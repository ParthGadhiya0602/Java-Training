package com.javatraining.apidesign.api.v2;

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

@WebMvcTest(ProductControllerV2.class)
class ProductControllerV2Test {

    @Autowired MockMvc mockMvc;
    @MockBean  ProductRepository productRepository;

    @Test
    void getById_returns_detail_with_category_and_inStock() throws Exception {
        when(productRepository.findById(2L))
                .thenReturn(Optional.of(new Product(2L, "Gadget", new BigDecimal("29.99"), "Electronics", true)));

        mockMvc.perform(get("/v2/products/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Gadget"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void getById_returns_404_when_product_not_found() throws Exception {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v2/products/99"))
                .andExpect(status().isNotFound());
    }
}
