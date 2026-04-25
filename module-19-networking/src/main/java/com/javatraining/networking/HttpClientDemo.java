package com.javatraining.networking;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Module 19 - HttpClient (Java 11+)
 *
 * java.net.http.HttpClient replaces the clunky HttpURLConnection.
 * Key design:
 *   - Immutable, fluent builder for client and requests
 *   - Supports HTTP/1.1 and HTTP/2
 *   - Both synchronous (send) and asynchronous (sendAsync) APIs
 *   - Built-in redirect following, authentication, cookie handling
 *
 * Also covers HttpURLConnection for legacy context and URL/URI utilities.
 */
public class HttpClientDemo {

    // ── Build a shared client ─────────────────────────────────────────────────

    /**
     * HttpClient instances should be reused - they manage connection pools.
     * Build once, share across requests.
     */
    public static HttpClient buildClient(Duration connectTimeout) {
        return HttpClient.newBuilder()
                         .connectTimeout(connectTimeout)
                         .followRedirects(HttpClient.Redirect.NORMAL)
                         .version(HttpClient.Version.HTTP_1_1)
                         .build();
    }

    // ── Synchronous GET ───────────────────────────────────────────────────────

    /**
     * send() blocks the calling thread until response arrives.
     * BodyHandlers.ofString() buffers the entire response body.
     */
    public static HttpResponse<String> get(HttpClient client, String url)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(url))
                                         .timeout(Duration.ofSeconds(10))
                                         .header("Accept", "application/json")
                                         .GET()
                                         .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** GET with custom headers. */
    public static HttpResponse<String> getWithHeaders(HttpClient client, String url,
                                                        Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                                                  .uri(URI.create(url))
                                                  .timeout(Duration.ofSeconds(10))
                                                  .GET();
        headers.forEach(builder::header);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    // ── Synchronous POST ──────────────────────────────────────────────────────

    public static HttpResponse<String> postJson(HttpClient client, String url, String jsonBody)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(url))
                                         .timeout(Duration.ofSeconds(10))
                                         .header("Content-Type", "application/json")
                                         .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                         .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public static HttpResponse<String> postForm(HttpClient client, String url,
                                                  Map<String, String> formParams)
            throws IOException, InterruptedException {
        String body = formParams.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(url))
                                         .header("Content-Type", "application/x-www-form-urlencoded")
                                         .POST(HttpRequest.BodyPublishers.ofString(body))
                                         .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ── Asynchronous requests ─────────────────────────────────────────────────

    /**
     * sendAsync() returns a CompletableFuture immediately.
     * The response is processed on the HttpClient's internal thread pool.
     */
    public static CompletableFuture<String> getAsync(HttpClient client, String url) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(url))
                                         .timeout(Duration.ofSeconds(10))
                                         .GET()
                                         .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                     .thenApply(HttpResponse::body);
    }

    /** Fan-out: send N requests concurrently, collect all bodies. */
    public static List<String> getAll(HttpClient client, List<String> urls)
            throws Exception {
        List<CompletableFuture<String>> futures = urls.stream()
            .map(url -> getAsync(client, url))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()))
            .get();
    }

    // ── Response utilities ────────────────────────────────────────────────────

    /** Extract all header values for a given name (case-insensitive). */
    public static List<String> getHeaders(HttpResponse<?> response, String headerName) {
        return response.headers().allValues(headerName);
    }

    /** Check if response has a success status (2xx). */
    public static boolean isSuccess(HttpResponse<?> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    // ── URI utilities ─────────────────────────────────────────────────────────

    /**
     * URI vs URL:
     *   URI - identifies a resource (may be abstract, relative, or opaque)
     *   URL - a URI that also includes how to locate/retrieve it
     *
     * Prefer URI in APIs; use URL.toURI() when you must interop.
     */
    public static URI buildUri(String scheme, String host, String path,
                                Map<String, String> queryParams) {
        String query = queryParams.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        String uriStr = scheme + "://" + host + path
                      + (query.isEmpty() ? "" : "?" + query);
        return URI.create(uriStr);
    }

    public static Map<String, String> parseQueryString(String query) {
        if (query == null || query.isBlank()) return Map.of();
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                           URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return params;
    }

    // ── HttpURLConnection (legacy reference) ──────────────────────────────────

    /**
     * HttpURLConnection predates HttpClient and requires more boilerplate.
     * Kept here as a reference - prefer HttpClient for new code.
     */
    public static String legacyGet(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try {
            int status = conn.getResponseCode();
            InputStream stream = status < 400
                ? conn.getInputStream()
                : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append('\n');
                return sb.toString().trim();
            }
        } finally {
            conn.disconnect();
        }
    }
}
