package com.javatraining.microservices.inventory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP client for the inventory-service.
 *
 * Uses Spring Boot 3.2's RestClient - the synchronous successor to RestTemplate.
 * RestClient.Builder is auto-configured as a prototype bean, allowing each client
 * to set its own baseUrl without affecting others.
 *
 * reserve() returns false instead of throwing on any HTTP or network error so the
 * saga can react with a compensating transaction rather than an unhandled exception.
 */
@Component
@Slf4j
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${inventory.service.url:http://localhost:8081}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean reserve(Long productId, int quantity) {
        try {
            restClient.post()
                    .uri("/inventory/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReservationRequest(productId, quantity))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Inventory reservation failed for productId={}, quantity={}: {}",
                    productId, quantity, e.getMessage());
            return false;
        }
    }

    public void release(Long productId, int quantity) {
        restClient.post()
                .uri("/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReservationRequest(productId, quantity))
                .retrieve()
                .toBodilessEntity();
    }
}
