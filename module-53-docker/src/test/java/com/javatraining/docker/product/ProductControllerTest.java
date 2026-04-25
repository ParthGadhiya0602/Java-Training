package com.javatraining.docker.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;

    private Product widget;
    private Product gadget;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        List<Product> saved = productRepository.saveAll(List.of(
                new Product(null, "Widget", new BigDecimal("9.99"),  "Tools"),
                new Product(null, "Gadget", new BigDecimal("19.99"), "Electronics")
        ));
        widget = saved.get(0);
        gadget = saved.get(1);
    }

    @Test
    void get_all_returns_all_products() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Widget"))
                .andExpect(jsonPath("$[1].name").value("Gadget"));
    }

    @Test
    void get_by_id_returns_product() throws Exception {
        mockMvc.perform(get("/products/{id}", widget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.category").value("Tools"));
    }

    @Test
    void get_by_unknown_id_returns_404() throws Exception {
        mockMvc.perform(get("/products/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void post_creates_product_and_returns_201_with_location() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Sprocket","price":4.99,"category":"Parts"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Sprocket"))
                .andExpect(jsonPath("$.price").value(4.99))
                .andExpect(header().string("Location", containsString("/products/")));
    }
}
