package com.javatraining.apidesign.api.v1;

import com.javatraining.apidesign.product.Product;
import com.javatraining.apidesign.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductControllerV1.class)
class ProductControllerV1Test {

    @Autowired MockMvc mockMvc;
    @MockBean  ProductRepository productRepository;

    @Test
    void getById_returns_summary_with_id_name_price_only() throws Exception {
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(new Product(1L, "Widget", new BigDecimal("9.99"), "Tools", true)));

        mockMvc.perform(get("/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.inStock").doesNotExist());
    }

    @Test
    void getById_returns_404_when_product_not_found() throws Exception {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/products/99"))
                .andExpect(status().isNotFound());
    }
}
