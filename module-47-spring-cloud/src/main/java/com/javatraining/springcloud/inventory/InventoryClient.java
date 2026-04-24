package com.javatraining.springcloud.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Raw HTTP client — propagates RestClientException on any 4xx/5xx or network error.
 * Resilience4j aspects on InventoryService intercept those exceptions to apply
 * circuit-breaking and retry logic before they reach the caller.
 */
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${inventory.service.url:http://localhost:8081}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean checkAvailability(Long productId, int quantity) {
        restClient.get()
                .uri("/inventory/{id}?quantity={qty}", productId, quantity)
                .retrieve()
                .toBodilessEntity();
        return true;
    }

    public boolean reserve(Long productId, int quantity) {
        restClient.post()
                .uri("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReservationRequest(productId, quantity))
                .retrieve()
                .toBodilessEntity();
        return true;
    }
}
