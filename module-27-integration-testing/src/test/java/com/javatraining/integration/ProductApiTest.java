package com.javatraining.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for {@link ProductApiServer} using REST-assured.
 *
 * What REST-assured demonstrates:
 *   given()           — build request: headers, content-type, body
 *   when()            — specify HTTP method and path
 *   then()            — assert status code and response body
 *   .body("field", …) — GPath expression + Hamcrest matcher for JSON assertions
 *   .extract()        — pull values from the response for use in subsequent steps
 *   path parameters   — /api/products/{id} binding
 *   query parameters  — ?category=X filtering
 *
 * The embedded {@link ProductApiServer} starts on port 0 (OS-assigned) in
 * {@code @BeforeAll} and is torn down in {@code @AfterAll}.
 * {@code @BeforeEach} resets the in-memory repository so every test starts clean.
 */
class ProductApiTest {

    static ProductApiServer   server;
    static InMemoryProductRepository repository;

    @BeforeAll
    static void startServer() {
        repository = new InMemoryProductRepository();
        server     = new ProductApiServer(new ProductService(repository));
        server.start(0);                   // OS assigns a free port
        RestAssured.port    = server.port();
        RestAssured.baseURI = "http://localhost";
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearRepository() {
        repository.clear();
    }

    // ── GET all ───────────────────────────────────────────────────────────────

    @Test
    void get_all_returns_empty_list_when_no_products_exist() {
        when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", empty());
    }

    @Test
    void get_all_returns_all_saved_products() {
        createProduct("Widget",  9.99, "gadgets");
        createProduct("Gizmo",  14.99, "gadgets");

        when()
            .get("/api/products")
        .then()
            .statusCode(200)
            .body("$",    hasSize(2))
            .body("name", hasItems("Widget", "Gizmo"));
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    void post_creates_product_and_returns_201_with_body() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Widget", "price", 9.99, "category", "gadgets"))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .body("id",       greaterThan(0))
            .body("name",     equalTo("Widget"))
            .body("category", equalTo("gadgets"));
    }

    @Test
    void post_returns_400_when_name_is_missing() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("price", 9.99))
        .when()
            .post("/api/products")
        .then()
            .statusCode(400);
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @Test
    void get_by_id_returns_product() {
        int id = createProduct("Widget", 9.99, "gadgets");

        when()
            .get("/api/products/{id}", id)
        .then()
            .statusCode(200)
            .body("id",   equalTo(id))
            .body("name", equalTo("Widget"));
    }

    @Test
    void get_by_id_returns_404_for_unknown_id() {
        when()
            .get("/api/products/{id}", 9999)
        .then()
            .statusCode(404);
    }

    // ── GET with query param ──────────────────────────────────────────────────

    @Test
    void get_by_category_returns_only_matching_products() {
        createProduct("Widget",  9.99, "gadgets");
        createProduct("Spanner", 4.99, "tools");
        createProduct("Gizmo",  14.99, "gadgets");

        when()
            .get("/api/products?category=gadgets")
        .then()
            .statusCode(200)
            .body("$",    hasSize(2))
            .body("name", hasItems("Widget", "Gizmo"))
            .body("name", not(hasItem("Spanner")));
    }

    @Test
    void get_by_category_returns_empty_for_unknown_category() {
        createProduct("Widget", 9.99, "gadgets");

        when()
            .get("/api/products?category=nonexistent")
        .then()
            .statusCode(200)
            .body("$", empty());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void delete_removes_product_and_returns_204() {
        int id = createProduct("Widget", 9.99, "gadgets");

        when()
            .delete("/api/products/{id}", id)
        .then()
            .statusCode(204);

        // Verify the product is gone
        when()
            .get("/api/products/{id}", id)
        .then()
            .statusCode(404);
    }

    @Test
    void delete_returns_404_for_nonexistent_product() {
        when()
            .delete("/api/products/{id}", 9999)
        .then()
            .statusCode(404);
    }

    // ── Content-Type ──────────────────────────────────────────────────────────

    @Test
    void response_has_json_content_type() {
        when()
            .get("/api/products")
        .then()
            .contentType(containsString("application/json"));
    }

    // ── Extract response values ───────────────────────────────────────────────

    @Test
    void can_extract_created_product_and_retrieve_by_id() {
        // REST-assured extract() pulls typed values from the response
        int id =
            given()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Gadget", "price", 24.99, "category", "tech"))
            .when()
                .post("/api/products")
            .then()
                .statusCode(201)
                .extract().path("id");

        // Use extracted id to GET the same product
        when()
            .get("/api/products/{id}", id)
        .then()
            .statusCode(200)
            .body("name",     equalTo("Gadget"))
            .body("category", equalTo("tech"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** POST a product and return the assigned id. */
    private int createProduct(String name, double price, String category) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "price", price, "category", category))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().path("id");
    }
}
