package com.javatraining.webflux.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Spring Data R2DBC entity.
 *
 * Key differences from JPA:
 *   @Id comes from org.springframework.data.annotation (not javax/jakarta.persistence)
 *   @Table comes from org.springframework.data.relational.core.mapping
 *   No @GeneratedValue - R2DBC uses the database's auto-increment and reads back the generated id
 *   No lazy loading, no EntityManager, no first-level cache - all fetches are explicit reactive calls
 *   No @OneToMany / @ManyToOne - R2DBC does not support ORM-level joins (use queries or separate repos)
 */
@Table("products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private Long id;

    private String name;
    private String category;
    private BigDecimal price;

    @Builder.Default
    private boolean active = true;
}
