package com.javatraining.thymeleaf.service;

import com.javatraining.thymeleaf.dto.ProductForm;
import com.javatraining.thymeleaf.exception.ProductNotFoundException;
import com.javatraining.thymeleaf.model.Product;
import com.javatraining.thymeleaf.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** Converts an entity to a form-backing bean for the edit form. */
    public ProductForm toForm(Long id) {
        Product p = findById(id);
        return new ProductForm(p.getName(), p.getPrice(), p.getCategory());
    }

    @Transactional
    public Product create(ProductForm form) {
        return repository.save(Product.builder()
                .name(form.getName())
                .category(form.getCategory())
                .price(form.getPrice())
                .build());
    }

    @Transactional
    public Product update(Long id, ProductForm form) {
        Product product = findById(id);
        product.setName(form.getName());
        product.setCategory(form.getCategory());
        product.setPrice(form.getPrice());
        return repository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
