package com.javatraining.springrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.springrest.dto.ProductRequest;
import com.javatraining.springrest.dto.ProductResponse;
import com.javatraining.springrest.exception.ProductNotFoundException;
import com.javatraining.springrest.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test — @WebMvcTest loads only the web layer:
 *   ProductController, GlobalExceptionHandler, Jackson, HATEOAS converters.
 *   ProductService is excluded (only the web layer is loaded) and must be mocked.
 *
 * @MockBean replaces the real ProductService with a Mockito mock in the
 * Spring application context used by MockMvc.
 */
@WebMvcTest
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;

    // ── GET /products — returns HAL collection ────────────────────────────────

    @Test
    void getAll_returns_hal_collection_with_links() throws Exception {
        given(productService.findAll()).willReturn(List.of(
                new ProductResponse(1L, "Laptop",  new BigDecimal("999.00"),  "Electronics"),
                new ProductResponse(2L, "Headphones", new BigDecimal("199.00"), "Electronics")
        ));

        mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // HAL _embedded key driven by @Relation(collectionRelation = "products")
                .andExpect(jsonPath("$._embedded.products").isArray())
                .andExpect(jsonPath("$._embedded.products", hasSize(2)))
                .andExpect(jsonPath("$._embedded.products[0].name").value("Laptop"))
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    void getByCategory_filters_results() throws Exception {
        given(productService.findByCategory("Electronics")).willReturn(List.of(
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics")
        ));

        mockMvc.perform(get("/products").param("category", "Electronics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.products", hasSize(1)))
                .andExpect(jsonPath("$._embedded.products[0].category").value("Electronics"));
    }

    // ── GET /products/{id} ────────────────────────────────────────────────────

    @Test
    void getById_found_returns_entity_with_links() throws Exception {
        given(productService.findById(1L)).willReturn(
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics"));

        mockMvc.perform(get("/products/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.00))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.products").exists());
    }

    @Test
    void getById_not_found_returns_404_problem_detail() throws Exception {
        given(productService.findById(99L)).willThrow(new ProductNotFoundException(99L));

        // RFC 9457 ProblemDetail: title, status, detail are standard fields
        mockMvc.perform(get("/products/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product not found: 99"));
    }

    // ── POST /products ────────────────────────────────────────────────────────

    @Test
    void create_valid_request_returns_201_with_location() throws Exception {
        ProductResponse created =
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics");
        given(productService.create(any())).willReturn(created);

        ProductRequest request =
                new ProductRequest("Laptop", new BigDecimal("999.00"), "Electronics");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Location header must point to the new resource
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/products/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$._links.self").exists());
    }

    @Test
    void create_blank_name_returns_400_problem_detail_with_errors() throws Exception {
        // "" fails @NotBlank AND @Size(min=2)
        ProductRequest bad = new ProductRequest("", new BigDecimal("999.00"), "Electronics");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void create_null_price_returns_400() throws Exception {
        // JSON with null price
        String json = """
                {"name":"Laptop","price":null,"category":"Electronics"}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    // ── PUT /products/{id} ────────────────────────────────────────────────────

    @Test
    void update_existing_product_returns_200() throws Exception {
        ProductResponse updated =
                new ProductResponse(1L, "Laptop Pro", new BigDecimal("1099.00"), "Electronics");
        given(productService.update(eq(1L), any())).willReturn(updated);

        ProductRequest request =
                new ProductRequest("Laptop Pro", new BigDecimal("1099.00"), "Electronics");

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(1099.00));
    }

    @Test
    void update_not_found_returns_404() throws Exception {
        given(productService.update(eq(99L), any())).willThrow(new ProductNotFoundException(99L));

        ProductRequest request =
                new ProductRequest("Ghost", new BigDecimal("1.00"), "Unknown");

        mockMvc.perform(put("/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── DELETE /products/{id} ─────────────────────────────────────────────────

    @Test
    void delete_existing_product_returns_204() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_not_found_returns_404() throws Exception {
        willThrow(new ProductNotFoundException(99L)).given(productService).delete(99L);

        mockMvc.perform(delete("/products/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Content negotiation ───────────────────────────────────────────────────

    @Test
    void accept_text_html_returns_406_not_acceptable() throws Exception {
        // produces = APPLICATION_JSON_VALUE on the controller excludes text/html.
        // Spring MVC rejects the request before invoking the handler method.
        mockMvc.perform(get("/products").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotAcceptable());
    }
}
