package com.javatraining.springtesting.slice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.springtesting.controller.ProductController;
import com.javatraining.springtesting.dto.ProductRequest;
import com.javatraining.springtesting.dto.ProductResponse;
import com.javatraining.springtesting.exception.ProductNotFoundException;
import com.javatraining.springtesting.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest - web layer slice.
 *
 * Loads: ProductController, GlobalExceptionHandler, Jackson, Spring MVC config.
 * Does NOT load: ProductService, ProductRepository, DataSource, PricingClient.
 *
 * Benefits over @SpringBootTest:
 *   - Starts in ~400 ms instead of 2+ s (no JPA, no datasource)
 *   - Failure isolation: a broken service layer cannot fail controller tests
 *   - Forces the developer to think about controller responsibilities vs service responsibilities
 */
@WebMvcTest(ProductController.class)
class ProductControllerSliceTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;

    @Test
    void getAll_returns_list_of_products() throws Exception {
        given(productService.findAll()).willReturn(List.of(
                new ProductResponse(1L, "Laptop",  new BigDecimal("999.00"),  "Electronics", true),
                new ProductResponse(2L, "Headset", new BigDecimal("149.00"), "Accessories", true)
        ));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Headset"));
    }

    @Test
    void getById_found_returns_product() throws Exception {
        given(productService.findById(1L)).willReturn(
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics", true));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getById_not_found_returns_404_problem_detail() throws Exception {
        given(productService.findById(99L)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_valid_request_returns_201() throws Exception {
        given(productService.create(any())).willReturn(
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics", true));

        ProductRequest req = new ProductRequest("Laptop", new BigDecimal("999.00"), "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }
}
