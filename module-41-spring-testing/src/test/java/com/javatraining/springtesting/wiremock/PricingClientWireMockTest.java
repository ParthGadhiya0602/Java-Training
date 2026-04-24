package com.javatraining.springtesting.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.javatraining.springtesting.client.PricingClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock — HTTP stub server for external service dependencies.
 *
 * Why WireMock instead of @MockBean?
 *   @MockBean replaces the whole Java object — no real HTTP happens.
 *   WireMock starts a real HTTP server — the RestTemplate, headers, serialization,
 *   timeout settings, and retry logic all run exactly as in production.
 *   This catches bugs that @MockBean cannot: wrong URL construction, missing headers,
 *   incorrect JSON field names, timeout configuration errors.
 *
 * @RegisterExtension static WireMockExtension:
 *   - Starts a Jetty server on a random port before all tests
 *   - Resets stubs between tests (clean state for every test)
 *   - Stops the server after all tests
 *
 * @DynamicPropertySource:
 *   - Runs AFTER the WireMock extension starts (so wireMock.baseUrl() is available)
 *   - Runs BEFORE the Spring context is created
 *   - Registers pricing.service.url with the random WireMock port
 *   - Spring Boot includes dynamic properties in the context cache key, so different
 *     dynamic values → different Spring contexts (no accidental cross-test contamination)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PricingClientWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // wireMock::baseUrl is called WHEN the property is resolved, not immediately —
        // safe because WireMock has already started at this point.
        registry.add("pricing.service.url", wireMock::baseUrl);
    }

    @Autowired PricingClient pricingClient;

    @Test
    void getPrice_returns_stubbed_price() {
        wireMock.stubFor(get(urlEqualTo("/prices/1"))
                .willReturn(okJson("{\"productId\":1,\"price\":999.00}")));

        BigDecimal price = pricingClient.getPrice(1L);

        assertThat(price).isEqualByComparingTo("999.00");
    }

    @Test
    void getPrice_service_unavailable_returns_zero_fallback() {
        wireMock.stubFor(get(urlEqualTo("/prices/2"))
                .willReturn(aResponse().withStatus(503)));

        // PricingClient catches RestClientException and returns BigDecimal.ZERO
        BigDecimal price = pricingClient.getPrice(2L);

        assertThat(price).isEqualByComparingTo("0");
    }

    @Test
    void getPrice_verifies_request_sent_to_correct_endpoint() {
        wireMock.stubFor(get(urlEqualTo("/prices/42"))
                .willReturn(okJson("{\"productId\":42,\"price\":149.99}")));

        pricingClient.getPrice(42L);

        // Verify the HTTP request was actually sent (not just that a mock returned a value)
        wireMock.verify(1, getRequestedFor(urlEqualTo("/prices/42")));
    }
}
