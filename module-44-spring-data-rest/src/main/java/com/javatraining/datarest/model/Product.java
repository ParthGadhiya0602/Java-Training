package com.javatraining.datarest.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * JPA entity exposed by Spring Data REST.
 *
 * active defaults to false here (no @Builder.Default) - the ProductEventHandler
 * sets it to true in @HandleBeforeCreate, centralising the creation policy in one place.
 * This is intentional: event handlers are a natural place to enforce invariants.
 */
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    private boolean active;
}
