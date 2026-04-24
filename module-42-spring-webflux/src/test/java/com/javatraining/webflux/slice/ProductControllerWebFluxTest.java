package com.javatraining.webflux.slice;

import com.javatraining.webflux.controller.ProductController;
import com.javatraining.webflux.dto.ProductRequest;
import com.javatraining.webflux.dto.ProductResponse;
import com.javatraining.webflux.exception.GlobalExceptionHandler;
import com.javatraining.webflux.exception.ProductNotFoundException;
import com.javatraining.webflux.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * @WebFluxTest — WebFlux web layer slice.
 *
 * Loads: controllers, @ControllerAdvice, Jackson, WebFlux configuration.
 * Does NOT load: services, repositories, R2DBC, Netty server.
 *
 * WebTestClient:
 *   - Auto-configured by @WebFluxTest against the slice context (no real TCP)
 *   - Fluent DSL: .get().uri(...).exchange() → .expectStatus() → .expectBody()
 *   - Can also be bound to a running server port for full integration tests
 *
 * Compared to MockMvc (Spring MVC):
 *   - MockMvc simulates the servlet dispatch cycle in-process
 *   - WebTestClient (in @WebFluxTest) simulates the reactive dispatch in-process
 *   - Both avoid real TCP; real TCP testing uses WebTestClient.bindToServer(url)
 */
@WebFluxTest({ProductController.class, GlobalExceptionHandler.class})
class ProductControllerWebFluxTest {

    @Autowired WebTestClient webTestClient;
    @MockBean ProductService productService;

    @Test
    void getAll_returns_product_list_as_json() {
        given(productService.findAll()).willReturn(Flux.just(
                new ProductResponse(1L, "Laptop",  new BigDecimal("999.00"), "Electronics", true),
                new ProductResponse(2L, "Mouse",   new BigDecimal("29.00"),  "Accessories", true)
        ));

        webTestClient.get().uri("/api/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .hasSize(2);
    }

    @Test
    void getById_found_returns_product() {
        given(productService.findById(1L)).willReturn(
                Mono.just(new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics", true)));

        webTestClient.get().uri("/api/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductResponse.class)
                .consumeWith(result -> {
                    ProductResponse body = result.getResponseBody();
                    assertThat(body.id()).isEqualTo(1L);
                    assertThat(body.name()).isEqualTo("Laptop");
                });
    }

    @Test
    void getById_not_found_returns_404_problem_detail() {
        given(productService.findById(99L)).willReturn(Mono.error(new ProductNotFoundException(99L)));

        webTestClient.get().uri("/api/products/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Product Not Found")
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void create_valid_request_returns_201_with_body() {
        given(productService.create(any())).willReturn(
                Mono.just(new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics", true)));

        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ProductRequest("Laptop", new BigDecimal("999.00"), "Electronics"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .consumeWith(result -> assertThat(result.getResponseBody().id()).isEqualTo(1L));
    }

    @Test
    void stream_endpoint_responds_with_text_event_stream() {
        given(productService.findAll()).willReturn(Flux.just(
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics", true)
        ));

        webTestClient.get().uri("/api/products/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(ProductResponse.class)
                .hasSize(1);
    }
}
