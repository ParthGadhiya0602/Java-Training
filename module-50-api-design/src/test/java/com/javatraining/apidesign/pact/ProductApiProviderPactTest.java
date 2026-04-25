package com.javatraining.apidesign.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verifies the running Spring Boot application against the pact file produced by
 * ProductApiConsumerPactTest.
 *
 * @PactFolder reads every *.json file in target/pacts/ and generates one
 * @TestTemplate invocation per interaction, making each interaction a separate
 * test case.
 *
 * Maven Surefire is configured with runOrder=alphabetical so the consumer test
 * (Consumer < Provider) always runs first, ensuring the pact file exists.
 */
@Provider("ProductProvider")
@PactFolder("target/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiProviderPactTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("product with id 1 exists")
    void productWithId1Exists() {
        // ProductRepository pre-loads product 1 — no action needed
    }
}
