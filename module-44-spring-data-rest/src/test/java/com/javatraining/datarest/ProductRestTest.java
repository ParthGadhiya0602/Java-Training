package com.javatraining.datarest;

import com.javatraining.datarest.model.Product;
import com.javatraining.datarest.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for Spring Data REST auto-generated endpoints.
 *
 * @SpringBootTest(MOCK) + @AutoConfigureMockMvc — full Spring context, no real TCP.
 * Spring Data REST registers its endpoints in the DispatcherServlet, so MockMvc works.
 *
 * No @WebMvcTest — Spring Data REST is not a @Controller; it's auto-configured at startup.
 * @WebMvcTest only loads annotated controllers and would miss the SDR endpoints entirely.
 *
 * Base path /api is configured in application.properties.
 * Endpoint URLs follow the pattern: /api/{collectionResourceRel}/{id}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ProductRestTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository repository;

    @BeforeEach
    void cleanup() {
        repository.deleteAll();
    }

    // ── Collection ────────────────────────────────────────────────────────────

    @Test
    void get_all_returns_hal_embedded_collection() throws Exception {
        repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());
        repository.save(Product.builder().name("Mouse").category("ACCESSORIES")
                .price(new BigDecimal("29.00")).active(true).build());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                // HAL collection: items are nested under _embedded.{collectionResourceRel}
                .andExpect(jsonPath("$._embedded.products", hasSize(2)))
                .andExpect(jsonPath("$._embedded.products[0].name", notNullValue()))
                // HAL _links on the collection
                .andExpect(jsonPath("$._links.self.href", notNullValue()))
                // Pagination metadata (JpaRepository → PageAndSortingRepository)
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    void get_all_with_pagination_returns_page_subset() throws Exception {
        for (int i = 1; i <= 5; i++) {
            repository.save(Product.builder().name("Product " + i).category("X")
                    .price(BigDecimal.TEN).active(true).build());
        }

        mockMvc.perform(get("/api/products?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.products", hasSize(2)))
                .andExpect(jsonPath("$.page.totalElements", is(5)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                // HAL pagination links
                .andExpect(jsonPath("$._links.next.href", notNullValue()));
    }

    // ── Single resource ───────────────────────────────────────────────────────

    @Test
    void get_by_id_returns_product_with_self_link() throws Exception {
        Product saved = repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());

        mockMvc.perform(get("/api/products/" + saved.getId()))
                .andExpect(status().isOk())
                // id is in body because of DataRestConfig.exposeIdsFor(Product.class)
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Laptop")))
                // HAL _links on the item
                .andExpect(jsonPath("$._links.self.href", containsString("/api/products/" + saved.getId())));
    }

    @Test
    void get_nonexistent_returns_404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void post_creates_product_and_returns_201() throws Exception {
        String json = """
                {"name":"Keyboard","category":"accessories","price":89.00}
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Keyboard")))
                // Event handler uppercased the category (@HandleBeforeCreate)
                .andExpect(jsonPath("$.category", is("ACCESSORIES")))
                // Event handler set active = true (@HandleBeforeCreate)
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$._links.self.href", notNullValue()));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void put_replaces_product_and_normalises_category() throws Exception {
        Product saved = repository.save(Product.builder().name("Old Name").category("ELECTRONICS")
                .price(new BigDecimal("100.00")).active(true).build());

        String json = """
                {"name":"New Name","category":"electronics","price":150.00,"active":true}
                """;

        mockMvc.perform(put("/api/products/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")))
                // @HandleBeforeSave normalises the category
                .andExpect(jsonPath("$.category", is("ELECTRONICS")))
                .andExpect(jsonPath("$.price", is(150.00)));
    }

    @Test
    void patch_updates_only_supplied_fields() throws Exception {
        Product saved = repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());

        // PATCH: only send the fields that should change
        String patch = """
                {"price":849.00}
                """;

        mockMvc.perform(patch("/api/products/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(849.00)))
                // name and category unchanged
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.category", is("ELECTRONICS")));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_returns_204_and_resource_is_gone() throws Exception {
        Product saved = repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());

        mockMvc.perform(delete("/api/products/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Test
    void search_findByCategory_returns_matching_products() throws Exception {
        repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());
        repository.save(Product.builder().name("Keyboard").category("ACCESSORIES")
                .price(new BigDecimal("89.00")).active(true).build());
        repository.save(Product.builder().name("Monitor").category("ELECTRONICS")
                .price(new BigDecimal("399.00")).active(true).build());

        mockMvc.perform(get("/api/products/search/findByCategory?category=ELECTRONICS"))
                .andExpect(status().isOk())
                // Search results are also embedded under the collectionResourceRel
                .andExpect(jsonPath("$._embedded.products", hasSize(2)));
    }

    @Test
    void search_endpoint_lists_exported_methods() throws Exception {
        // GET /api/products/search shows which custom query methods are exposed
        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.findByCategory.href", notNullValue()))
                // findByActiveTrue has @RestResource(exported=false) — must NOT be listed
                .andExpect(jsonPath("$._links.findByActiveTrue").doesNotExist());
    }

    // ── Projection ────────────────────────────────────────────────────────────

    @Test
    void projection_summary_returns_subset_of_fields() throws Exception {
        Product saved = repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());

        mockMvc.perform(get("/api/products/" + saved.getId() + "?projection=summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.price", notNullValue()))
                // category and active are NOT part of the ProductSummary projection
                .andExpect(jsonPath("$.category").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    // ── Profile / ALPS ────────────────────────────────────────────────────────

    @Test
    void profile_endpoint_returns_alps_schema() throws Exception {
        // Spring Data REST auto-generates ALPS metadata at /api/profile/{resource}
        // ALPS describes the available actions and their input/output shapes
        mockMvc.perform(get("/api/profile/products")
                        .accept("application/alps+json"))
                .andExpect(status().isOk());
    }
}
