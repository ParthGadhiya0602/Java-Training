package com.javatraining.thymeleaf.controller;

import com.javatraining.thymeleaf.dto.ProductForm;
import com.javatraining.thymeleaf.exception.ProductNotFoundException;
import com.javatraining.thymeleaf.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Controller — returns view names (template paths), not serialized response bodies.
 *
 * Key patterns:
 *   Model — a Map passed to the template; attributes are accessible via ${name} expressions
 *   @ModelAttribute — binds request parameters to a bean and adds it to the model
 *   BindingResult — holds validation errors; MUST immediately follow @ModelAttribute param
 *   redirect: prefix — tells Spring MVC to send an HTTP 302 redirect instead of rendering
 *
 * POST-Redirect-GET pattern:
 *   Always redirect after a successful POST to prevent duplicate submission on browser refresh.
 *   If the form has errors, return the form view directly (no redirect) so errors are visible.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        // Thymeleaf requires the model attribute to exist before rendering th:object
        model.addAttribute("productForm", new ProductForm());
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productForm", productService.toForm(id));
        model.addAttribute("productId", id);
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ProductForm productForm,
                         BindingResult result) {
        // BindingResult MUST be the parameter immediately after @ModelAttribute
        // If it were separated, Spring would throw a 400 before the method body runs
        if (result.hasErrors()) {
            // Return the form view — errors are already in the BindingResult in the model
            return "products/form";
        }
        productService.create(productForm);
        return "redirect:/products";    // POST-Redirect-GET
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ProductForm productForm,
                         BindingResult result,
                         Model model) {
        if (result.hasErrors()) {
            model.addAttribute("productId", id);
            return "products/form";
        }
        productService.update(id, productForm);
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/products";
    }

    // Scoped to this controller — redirects to list on not-found rather than showing an error page
    @ExceptionHandler(ProductNotFoundException.class)
    public String handleNotFound() {
        return "redirect:/products";
    }
}
