package com.javatraining.batch.repository;

import com.javatraining.batch.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {}
