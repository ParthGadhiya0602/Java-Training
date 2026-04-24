package com.javatraining.springtesting.slice;

import com.javatraining.springtesting.dto.ProductRequest;
import com.javatraining.springtesting.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @JsonTest — JSON slice.
 *
 * Loads: Jackson ObjectMapper (with Spring Boot auto-configuration — dates, modules, etc.)
 * Does NOT load: Spring MVC, JPA, services, or any application beans.
 *
 * Use @JsonTest to verify:
 *   - Field names in the serialized JSON (e.g., camelCase vs snake_case)
 *   - Null handling (@JsonInclude(NON_NULL), etc.)
 *   - Number formatting (BigDecimal → decimal in JSON)
 *   - Record deserialization (canonical constructor parameter mapping)
 *
 * JacksonTester.initFields(this, objectMapper) scans the test instance for
 * JacksonTester<T> fields and binds each one to the configured ObjectMapper.
 */
@JsonTest
class ProductJsonTest {

    @Autowired
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    JacksonTester<ProductResponse> responseJson;
    JacksonTester<ProductRequest> requestJson;

    @BeforeEach
    void setUp() {
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    void serializes_product_response_to_json() throws Exception {
        ProductResponse product = new ProductResponse(
                1L, "Laptop", new BigDecimal("999.00"), "Electronics", true);

        JsonContent<ProductResponse> content = responseJson.write(product);

        assertThat(content).hasJsonPathValue("$.id", 1);
        assertThat(content).hasJsonPathStringValue("$.name", "Laptop");
        assertThat(content).hasJsonPathStringValue("$.category", "Electronics");
        assertThat(content).hasJsonPathBooleanValue("$.active", true);
        // BigDecimal → decimal in JSON (not an int)
        // extractingJsonPathNumberValue returns Number (Double at runtime), so convert before comparing
        assertThat(content).extractingJsonPathNumberValue("$.price")
                .satisfies(n -> assertThat(new BigDecimal(n.toString())).isEqualByComparingTo("999.00"));
    }

    @Test
    void deserializes_product_request_from_json() throws Exception {
        String json = """
                {"name":"Laptop","price":999.00,"category":"Electronics"}
                """;

        ProductRequest request = requestJson.parseObject(json);

        assertThat(request.name()).isEqualTo("Laptop");
        assertThat(request.category()).isEqualTo("Electronics");
        assertThat(request.price()).isEqualByComparingTo("999.00");
    }

    @Test
    void serialized_json_does_not_include_unexpected_fields() throws Exception {
        ProductResponse product = new ProductResponse(
                2L, "Phone", new BigDecimal("499.99"), "Mobile", false);

        String json = responseJson.write(product).getJson();

        // Verify the exact field set — no extra fields sneak in
        assertThat(json)
                .contains("\"id\"")
                .contains("\"name\"")
                .contains("\"price\"")
                .contains("\"category\"")
                .contains("\"active\"");
        // Record components map 1:1 to JSON fields
        assertThat(json).contains("\"active\":false");
    }
}
