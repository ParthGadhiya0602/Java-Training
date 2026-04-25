package com.javatraining.lombokstruct.entity;

import lombok.*;

/**
 * Demonstrates fine-grained Lombok annotations instead of @Data.
 *
 * <p>Prefer individual annotations over @Data for entities because:
 * <ul>
 *   <li>@Data's @EqualsAndHashCode includes ALL fields by default - on a JPA entity
 *       this traverses lazy collections and breaks equals/hashCode contracts when the
 *       id is null (two unsaved instances are equal even if they represent different rows)</li>
 *   <li>@Data's @ToString can trigger lazy-load exceptions in JPA sessions</li>
 *   <li>Fine-grained control lets you exclude specific fields</li>
 * </ul>
 *
 * <p>@EqualsAndHashCode(onlyExplicitlyIncluded = true) + @EqualsAndHashCode.Include
 * on the id field is the recommended JPA pattern.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "address")             // exclude nested object to avoid verbose/circular output
@EqualsAndHashCode(of = {"id", "email"})   // stable identity fields only
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Address address;
}
