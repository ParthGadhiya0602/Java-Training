package com.javatraining.thymeleaf.repository;

import com.javatraining.thymeleaf.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
