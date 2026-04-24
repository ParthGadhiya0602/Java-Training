package com.javatraining.batch.batch;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO that mirrors one CSV row.
 *
 * Must be a mutable JavaBean (getters + setters + no-arg constructor):
 *   FlatFileItemReader's BeanWrapperFieldSetMapper creates an instance via the no-arg
 *   constructor, then populates each field by name using the setter. Records have no
 *   setters and cannot be used here.
 *
 * Spring's ConversionService converts the String tokens from the CSV to the
 * declared field types (BigDecimal, int, etc.) automatically.
 */
@Data
@NoArgsConstructor
public class ProductCsvRow {
    private String name;
    private String category;
    private BigDecimal price;
}
