package com.javatraining.springtesting.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

// External HTTP client — calls a separate pricing microservice.
// Base URL is configurable so tests can point it at a WireMock stub server.
@Component
public class PricingClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PricingClient(RestTemplateBuilder builder,
                         @Value("${pricing.service.url}") String baseUrl) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl;
    }

    public BigDecimal getPrice(Long productId) {
        try {
            PriceResponse response = restTemplate.getForObject(
                    baseUrl + "/prices/" + productId, PriceResponse.class);
            return response != null ? response.price() : BigDecimal.ZERO;
        } catch (RestClientException e) {
            // Pricing service unavailable — return zero as a safe fallback
            return BigDecimal.ZERO;
        }
    }

    record PriceResponse(Long productId, BigDecimal price) {}
}
