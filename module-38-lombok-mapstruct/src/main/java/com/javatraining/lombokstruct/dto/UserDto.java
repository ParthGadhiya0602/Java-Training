package com.javatraining.lombokstruct.dto;

import lombok.*;

/**
 * Response DTO — fields differ from the User entity:
 * <ul>
 *   <li>{@code fullName} combines firstName + lastName (MapStruct expression)</li>
 *   <li>{@code city} is flattened from the nested Address object</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String fullName;   // mapped from firstName + " " + lastName
    private String email;
    private String role;
    private String city;       // flattened from address.city
}
