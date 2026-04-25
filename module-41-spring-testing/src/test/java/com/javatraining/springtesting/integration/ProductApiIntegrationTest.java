package com.javatraining.springtesting.integration;

import com.javatraining.springtesting.client.PricingClient;
import com.javatraining.springtesting.dto.ProductRequest;
import com.javatraining.springtesting.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Full integration test - @SpringBootTest(RANDOM_PORT).
 *
 * Starts the complete Spring context including the embedded web server on a random port.
 * Uses TestRestTemplate which makes real HTTP calls to the running server.
 *
 * Scope: the entire stack from HTTP → Controller → Service → Repository (H2 in-memory).
 *   PricingClient is @MockBean-ed: it depends on an external service that is not available
 *   here, and its behaviour is covered by the dedicated WireMock test.
 *
 * Why RANDOM_PORT (not MOCK)?
 *   TestRestTemplate requires a real port - it calls the server over TCP.
 *   RANDOM_PORT avoids conflicts with other running services.
 *   MOCK (MockMvc) is sufficient for slice and security tests but does not exercise
 *   the servlet container, filter chains at the container level, or connection handling.
 *
 * Transaction isolation: @SpringBootTest does NOT roll back transactions between tests
 * (unlike @DataJpaTest). Each test must leave the database in a known state - here that
 * means creating new objects and not relying on cross-test state.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    // PricingClient talks to an external service - stub it so tests are self-contained
    @MockBean PricingClient pricingClient;

    @Test
    void create_then_retrieve_roundtrip() {
        ProductRequest request = new ProductRequest("Laptop", new BigDecimal("999.00"), "Electronics");

        ResponseEntity<ProductResponse> createResponse =
                restTemplate.postForEntity("/api/products", request, ProductResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductResponse created = createResponse.getBody();
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Laptop");
        assertThat(created.active()).isTrue();

        // Retrieve by ID
        ResponseEntity<ProductResponse> getResponse =
                restTemplate.getForEntity("/api/products/" + created.id(), ProductResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().name()).isEqualTo("Laptop");
        assertThat(getResponse.getBody().price()).isEqualByComparingTo("999.00");
    }

    @Test
    void get_all_products_returns_ok() {
        // Create one product so the list is non-empty
        restTemplate.postForEntity("/api/products",
                new ProductRequest("Keyboard", new BigDecimal("79.00"), "Accessories"),
                ProductResponse.class);

        ResponseEntity<ProductResponse[]> response =
                restTemplate.getForEntity("/api/products", ProductResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void get_nonexistent_product_returns_404() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/products/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void get_live_price_returns_mocked_price() {
        when(pricingClient.getPrice(1L)).thenReturn(new BigDecimal("149.99"));

        // Create the product first
        ProductRequest request = new ProductRequest("Mouse", new BigDecimal("50.00"), "Accessories");
        ProductResponse created = restTemplate.postForEntity("/api/products", request, ProductResponse.class).getBody();

        // Stub the pricing client for this product's ID
        when(pricingClient.getPrice(created.id())).thenReturn(new BigDecimal("45.00"));

        ResponseEntity<String> priceResponse =
                restTemplate.getForEntity("/api/products/" + created.id() + "/price", String.class);

        assertThat(priceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
