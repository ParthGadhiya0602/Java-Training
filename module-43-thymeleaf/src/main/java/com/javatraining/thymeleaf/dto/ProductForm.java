package com.javatraining.thymeleaf.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Form-backing bean for product create/edit.
 *
 * Must be a mutable JavaBean (with getters AND setters) — not a record.
 * Thymeleaf's th:field uses setters when binding submitted form values to the object.
 * Records have no setters, so Spring's DataBinder cannot populate them from POST params.
 *
 * @NoArgsConstructor is required: Spring's DataBinder creates the object with the no-arg
 * constructor and then populates fields via setters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductForm {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    private BigDecimal price;

    @NotBlank(message = "Category is required")
    private String category;
}
