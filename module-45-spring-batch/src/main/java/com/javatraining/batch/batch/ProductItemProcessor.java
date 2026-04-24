package com.javatraining.batch.batch;

import com.javatraining.batch.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ItemProcessor — transforms a CSV row into a domain entity.
 *
 * Two outcomes per item:
 *
 *   Return a Product   — item is passed to the writer (written to DB).
 *
 *   Return null        — item is silently FILTERED (not written, not counted as a skip).
 *                        Spring Batch increments filterCount on the StepExecution.
 *                        Use this for logically invalid data that should be dropped
 *                        without marking the step as failed.
 *
 *   Throw an exception — item is a SKIP candidate if the exception type is configured
 *                        with .faultTolerant().skip(...).skipLimit(...) on the step.
 *                        Spring Batch increments processSkipCount and retries the chunk
 *                        without the failing item.
 *
 * Distinction: filter (null return) vs skip (exception):
 *   Filter: "this row is not relevant to us" — expected, silent
 *   Skip:   "this row is broken in a way we tolerate" — logged, counted
 */
@Component
public class ProductItemProcessor implements ItemProcessor<ProductCsvRow, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Override
    public Product process(ProductCsvRow item) {
        // --- Filter: silently drop rows with a blank name ---
        // Returning null increments filterCount; the item is not written.
        if (item.getName() == null || item.getName().isBlank()) {
            log.debug("Filtering row with blank name: {}", item);
            return null;
        }

        // --- Skip trigger: invalid price throws an exception ---
        // If .faultTolerant().skip(IllegalArgumentException.class) is configured on the
        // step, Spring Batch skips this item and increments processSkipCount.
        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Invalid price for product '" + item.getName() + "': " + item.getPrice());
        }

        // --- Transform: build domain entity ---
        return Product.builder()
                .name(item.getName().trim())
                // Normalise category to uppercase at the processing boundary
                .category(item.getCategory() != null
                        ? item.getCategory().trim().toUpperCase()
                        : "UNKNOWN")
                .price(item.getPrice())
                .build();
    }
}
