package com.javatraining.apidesign.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Defines the consumer's expectations of the ProductProvider.
 *
 * PactConsumerTestExt starts a mock HTTP server that replays the interactions
 * defined in the @Pact method. The test validates that the consumer code can
 * handle the response. On success, the interaction is written to
 * target/pacts/ProductConsumer-ProductProvider.json.
 *
 * That file is then read by ProductApiProviderPactTest to verify the real
 * provider honours the same contract.
 *
 * pactVersion = V3 keeps the familiar RequestResponsePact + PactDslWithProvider API;
 * pact-jvm 4.6.x defaults to V4 which requires a different builder signature.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ProductProvider", pactVersion = PactSpecVersion.V3)
class ProductApiConsumerPactTest {

    @Pact(consumer = "ProductConsumer")
    RequestResponsePact productSummaryShape(PactDslWithProvider builder) {
        return builder
                .given("product with id 1 exists")
                .uponReceiving("a GET request for product 1 via v1 API")
                .path("/v1/products/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .numberType("id", 1L)
                        .stringType("name", "Widget")
                        .decimalType("price", 9.99))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "productSummaryShape")
    @SuppressWarnings("unchecked")
    void consumer_expects_product_summary_with_id_name_price(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body = restTemplate.getForObject(
                mockServer.getUrl() + "/v1/products/1", Map.class);

        assertThat(body).containsKeys("id", "name", "price");
    }
}
