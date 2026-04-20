package com.javatraining.springdata.projection;

/**
 * DTO projection using a Java record.
 *
 * <p>Used in JPQL {@code new} expressions:
 * <pre>
 *   SELECT new com.javatraining.springdata.projection.EmployeeNameDto(e.name, e.salary)
 *   FROM Employee e WHERE ...
 * </pre>
 *
 * Records are immutable and work perfectly as DTO projections — no proxy overhead.
 */
public record EmployeeNameDto(String name, java.math.BigDecimal salary) {}
