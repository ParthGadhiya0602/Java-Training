package com.javatraining.springrest.controller;

import com.javatraining.springrest.dto.ProductRequest;
import com.javatraining.springrest.dto.ProductResponse;
import com.javatraining.springrest.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

// produces = APPLICATION_JSON_VALUE: this controller only serves application/json.
// Clients sending Accept: text/html get 406 Not Acceptable - content negotiation.
@RestController
@RequestMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET /products or GET /products?category=Electronics
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProductResponse>>> getAll(
            @RequestParam(required = false) String category) {

        List<ProductResponse> products = (category != null)
                ? productService.findByCategory(category)
                : productService.findAll();

        List<EntityModel<ProductResponse>> items = products.stream()
                .map(p -> EntityModel.of(p,
                        linkTo(methodOn(ProductController.class).getById(p.id())).withSelfRel()))
                .toList();

        // Self link points back to the collection (category ignored in link - always lists all)
        CollectionModel<EntityModel<ProductResponse>> collection = CollectionModel.of(items,
                linkTo(methodOn(ProductController.class).getAll(null)).withSelfRel());

        return ResponseEntity.ok(collection);
    }

    // GET /products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ProductResponse>> getById(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);

        // Self link + "products" rel back to the collection
        EntityModel<ProductResponse> model = EntityModel.of(product,
                linkTo(methodOn(ProductController.class).getById(id)).withSelfRel(),
                linkTo(methodOn(ProductController.class).getAll(null)).withRel("products"));

        return ResponseEntity.ok(model);
    }

    // POST /products  - 201 Created + Location header pointing to the new resource
    @PostMapping
    public ResponseEntity<EntityModel<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse created = productService.create(request);

        EntityModel<ProductResponse> model = EntityModel.of(created,
                linkTo(methodOn(ProductController.class).getById(created.id())).withSelfRel(),
                linkTo(methodOn(ProductController.class).getAll(null)).withRel("products"));

        return ResponseEntity
                .created(linkTo(methodOn(ProductController.class).getById(created.id())).toUri())
                .body(model);
    }

    // PUT /products/{id}  - full replacement
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse updated = productService.update(id, request);

        EntityModel<ProductResponse> model = EntityModel.of(updated,
                linkTo(methodOn(ProductController.class).getById(id)).withSelfRel(),
                linkTo(methodOn(ProductController.class).getAll(null)).withRel("products"));

        return ResponseEntity.ok(model);
    }

    // DELETE /products/{id}  - 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
