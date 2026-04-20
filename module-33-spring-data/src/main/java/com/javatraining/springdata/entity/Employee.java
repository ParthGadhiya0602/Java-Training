package com.javatraining.springdata.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Employee entity — demonstrates:
 * <ul>
 *   <li>Bean Validation constraints on fields</li>
 *   <li>ManyToOne relationship to Department (LAZY)</li>
 *   <li>Spring Data auditing via {@code @CreatedDate} / {@code @LastModifiedDate}</li>
 * </ul>
 */
@Entity
@Table(name = "employees")
@EntityListeners(AuditingEntityListener.class)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotNull
    @DecimalMin("0.00")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Employee() {}

    public Employee(String name, String email, BigDecimal salary) {
        this.name   = name;
        this.email  = email;
        this.salary = salary;
    }

    // Getters
    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getEmail()             { return email; }
    public BigDecimal getSalary()        { return salary; }
    public boolean isActive()            { return active; }
    public Department getDepartment()    { return department; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }

    // Setters
    public void setName(String name)             { this.name = name; }
    public void setEmail(String email)           { this.email = email; }
    public void setSalary(BigDecimal salary)     { this.salary = salary; }
    public void setActive(boolean active)        { this.active = active; }
    public void setDepartment(Department dept)   { this.department = dept; }
}
