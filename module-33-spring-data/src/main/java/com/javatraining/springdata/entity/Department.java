package com.javatraining.springdata.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Department — parent side of the one-to-many relationship with Employee.
 *
 * <p>Kept intentionally simple so tests focus on Spring Data JPA concepts
 * rather than domain complexity.
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();

    protected Department() {}

    public Department(String name) {
        this.name = name;
    }

    // Bidirectional helper
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }

    // Getters
    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public List<Employee> getEmployees() { return employees; }

    // Setters
    public void setName(String name)     { this.name = name; }
}
