package com.javatraining.lombokstruct.entity;

import lombok.*;

/**
 * @Data = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor.
 * Combined with @Builder, @NoArgsConstructor, @AllArgsConstructor for full flexibility.
 *
 * <p>The @NoArgsConstructor + @AllArgsConstructor pair is required alongside @Builder:
 * @Builder generates an all-args constructor internally, which conflicts unless you
 * explicitly declare both so Lombok knows what you intend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String street;
    private String city;
    private String country;
}
