package com.javatraining.thymeleaf.controller;

import com.javatraining.thymeleaf.dto.ProductForm;
import com.javatraining.thymeleaf.exception.ProductNotFoundException;
import com.javatraining.thymeleaf.model.Product;
import com.javatraining.thymeleaf.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * @WebMvcTest - web layer slice for an MVC (non-REST) controller.
 *
 * Thymeleaf IS included in @WebMvcTest: templates are actually rendered.
 * This lets us assert on the HTML output (view name, model attributes, rendered content).
 *
 * Assertions available for MVC controllers:
 *   view().name(...)            - the logical view name returned by the handler
 *   model().attributeExists(..) - model contains an attribute with the given name
 *   model().attribute(name, value) - model attribute equals the value
 *   model().attributeHasFieldErrors(attr, field) - BindingResult has errors for field
 *   redirectedUrl(url)          - response is a redirect to the given URL
 *   content().string(...)       - raw response body assertion (HTML)
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  ProductService productService;

    @Test
    void list_page_returns_products_in_model() throws Exception {
        List<Product> products = List.of(
                Product.builder().id(1L).name("Laptop").category("Electronics").price(new BigDecimal("999.00")).build(),
                Product.builder().id(2L).name("Mouse").category("Accessories").price(new BigDecimal("29.00")).build()
        );
        given(productService.findAll()).willReturn(products);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))
                .andExpect(model().attributeExists("products"))
                // Template is rendered - assert the HTML contains the product names
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Laptop")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mouse")));
    }

    @Test
    void list_page_returns_html_content_type() throws Exception {
        given(productService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void new_form_adds_empty_product_form_to_model() throws Exception {
        mockMvc.perform(get("/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeExists("productForm"));
    }

    @Test
    void edit_form_loads_existing_product_data() throws Exception {
        given(productService.toForm(1L)).willReturn(
                new ProductForm("Laptop", new BigDecimal("999.00"), "Electronics"));

        mockMvc.perform(get("/products/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeExists("productForm"))
                .andExpect(model().attribute("productId", 1L));
    }

    @Test
    void create_valid_product_redirects_to_list() throws Exception {
        given(productService.create(any())).willReturn(
                Product.builder().id(1L).name("Laptop").build());

        mockMvc.perform(post("/products")
                        .param("name", "Laptop")
                        .param("price", "999.00")
                        .param("category", "Electronics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(productService).create(any(ProductForm.class));
    }

    @Test
    void create_invalid_name_returns_form_with_binding_errors() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "")          // blank - fails @NotBlank
                        .param("price", "999.00")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "name"));
    }

    @Test
    void create_invalid_price_returns_form_with_binding_errors() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "Laptop")
                        .param("price", "0.00")     // below @DecimalMin("0.01")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "price"));
    }

    @Test
    void update_valid_redirects_to_list() throws Exception {
        given(productService.update(eq(1L), any())).willReturn(
                Product.builder().id(1L).name("Updated Laptop").build());

        mockMvc.perform(post("/products/1")
                        .param("name", "Updated Laptop")
                        .param("price", "899.00")
                        .param("category", "Electronics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void delete_redirects_to_list() throws Exception {
        mockMvc.perform(post("/products/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        verify(productService).delete(1L);
    }

    @Test
    void edit_nonexistent_product_redirects_to_list() throws Exception {
        given(productService.toForm(99L)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/products/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }
}
