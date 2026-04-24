package com.javatraining.lombokstruct.dto;

import lombok.*;

/**
 * Incoming request DTO.
 *
 * <p>Using @Data + @Builder + @NoArgsConstructor + @AllArgsConstructor:
 * <ul>
 *   <li>@Data: getters, setters, equals, hashCode, toString</li>
 *   <li>@Builder: fluent construction in tests and application code</li>
 *   <li>@NoArgsConstructor: required by Jackson for JSON deserialization</li>
 *   <li>@AllArgsConstructor: required by @Builder when @NoArgsConstructor is present</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String role;
}
