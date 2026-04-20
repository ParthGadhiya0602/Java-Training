package com.javatraining.springdata.repository;

import com.javatraining.springdata.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Department}.
 *
 * <p>Extends {@link JpaRepository} which provides:
 * <ul>
 *   <li>{@code save}, {@code saveAll}, {@code saveAndFlush}</li>
 *   <li>{@code findById}, {@code findAll}, {@code findAllById}</li>
 *   <li>{@code existsById}, {@code count}</li>
 *   <li>{@code delete}, {@code deleteById}, {@code deleteAll}</li>
 *   <li>{@code flush}, {@code deleteAllInBatch}</li>
 * </ul>
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);

    boolean existsByName(String name);
}
