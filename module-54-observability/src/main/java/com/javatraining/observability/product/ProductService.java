package com.javatraining.observability.product;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates three Micrometer instrument types:
 *
 *  Counter  — monotonically increasing count of lookups
 *  Timer    — latency distribution for each findById call
 *  Gauge    — point-in-time snapshot of how many products have been created
 *
 * For automatic span + metric creation, annotate the class or individual methods
 * with @Observed(name = "product.service") — requires spring-boot-starter-aop so
 * Spring proxies the class and fires ObservationAspect around each call.
 */
@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;

    private final Counter lookupCounter;
    private final Timer   lookupTimer;
    private final AtomicInteger activeCount = new AtomicInteger(0);

    public ProductService(ProductRepository productRepository, MeterRegistry meterRegistry) {
        this.productRepository = productRepository;

        this.lookupCounter = Counter.builder("products.lookups")
                .description("Total number of product lookup calls")
                .register(meterRegistry);

        this.lookupTimer = Timer.builder("products.lookup.duration")
                .description("Time taken to look up a product by id")
                .register(meterRegistry);

        Gauge.builder("products.active", activeCount, AtomicInteger::get)
                .description("Number of products created in this application instance")
                .register(meterRegistry);
    }

    public Optional<Product> findById(Long id) {
        return lookupTimer.record(() -> {
            lookupCounter.increment();
            log.debug("Looking up product id={}", id);
            return productRepository.findById(id);
        });
    }

    public Product save(Product product) {
        Product saved = productRepository.save(product);
        activeCount.incrementAndGet();
        log.info("Created product id={} name={}", saved.getId(), saved.getName());
        return saved;
    }
}
