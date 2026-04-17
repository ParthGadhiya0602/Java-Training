package com.javatraining.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Lightweight HTTP server built on the JDK's built-in {@code com.sun.net.httpserver}.
 * Exposes a minimal JSON REST API for products so integration tests can exercise
 * REST-assured against a real HTTP stack without any external server process.
 *
 * Endpoints:
 *   GET    /api/products             — list all products (or filter by ?category=X)
 *   GET    /api/products/{id}        — get by id; 404 if not found
 *   POST   /api/products             — create; 201 with created product; 400 on bad input
 *   DELETE /api/products/{id}        — delete; 204 if deleted; 404 if not found
 */
public class ProductApiServer {

    private static final String BASE = "/api/products";
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProductService service;
    private HttpServer server;

    public ProductApiServer(ProductService service) {
        this.service = service;
    }

    /**
     * Starts the server on the given port.  Pass {@code 0} to let the OS pick
     * a free port; retrieve it afterwards with {@link #port()}.
     */
    public void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext(BASE, this::dispatch);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start ProductApiServer", e);
        }
    }

    /** Stops the server immediately. */
    public void stop() {
        if (server != null) server.stop(0);
    }

    /** Returns the port the server is actually bound to (useful when started on port 0). */
    public int port() {
        return server.getAddress().getPort();
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    private void dispatch(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path   = ex.getRequestURI().getPath();
        String query  = ex.getRequestURI().getQuery();

        try {
            if (BASE.equals(path)) {
                if ("GET".equals(method))    handleGetAll(ex, query);
                else if ("POST".equals(method)) handleCreate(ex);
                else send(ex, 405, "");
            } else if (path.startsWith(BASE + "/")) {
                String segment = path.substring((BASE + "/").length());
                long id = Long.parseLong(segment);
                if ("GET".equals(method))    handleGetById(ex, id);
                else if ("DELETE".equals(method)) handleDelete(ex, id);
                else send(ex, 405, "");
            } else {
                send(ex, 404, "");
            }
        } catch (NumberFormatException e) {
            send(ex, 400, json("error", "invalid id"));
        } catch (IllegalArgumentException e) {
            send(ex, 400, json("error", e.getMessage()));
        } catch (Exception e) {
            send(ex, 500, json("error", e.getMessage()));
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private void handleGetAll(HttpExchange ex, String query) throws IOException {
        if (query != null && query.startsWith("category=")) {
            String cat = URLDecoder.decode(
                query.substring("category=".length()), StandardCharsets.UTF_8);
            send(ex, 200, mapper.writeValueAsString(service.findByCategory(cat)));
        } else {
            send(ex, 200, mapper.writeValueAsString(service.findAll()));
        }
    }

    private void handleGetById(HttpExchange ex, long id) throws IOException {
        var opt = service.findById(id);
        if (opt.isPresent()) send(ex, 200, mapper.writeValueAsString(opt.get()));
        else                 send(ex, 404, json("error", "not found"));
    }

    private void handleCreate(HttpExchange ex) throws IOException {
        byte[] bytes = ex.getRequestBody().readAllBytes();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = mapper.readValue(bytes, Map.class);
        String name     = (String) body.get("name");
        double price    = ((Number) body.get("price")).doubleValue();
        String category = (String) body.getOrDefault("category", "");
        Product created = service.create(name, price, category);
        send(ex, 201, mapper.writeValueAsString(created));
    }

    private void handleDelete(HttpExchange ex, long id) throws IOException {
        if (service.deleteById(id)) {
            ex.sendResponseHeaders(204, -1);
            ex.getResponseBody().close();
        } else {
            send(ex, 404, json("error", "not found"));
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Produces {@code {"key":"value"}} without a full ObjectMapper invocation. */
    private static String json(String key, String value) {
        return "{\"" + key + "\":\"" + value.replace("\"", "\\\"") + "\"}";
    }
}
