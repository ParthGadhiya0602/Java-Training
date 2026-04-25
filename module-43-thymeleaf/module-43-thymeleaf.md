---
title: "Module 43 — Thymeleaf"
parent: "Phase 5 — Spring Ecosystem"
nav_order: 43
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-43-thymeleaf/src){: .btn .btn-outline }

# Module 43 — Thymeleaf

## Overview

Thymeleaf is Spring Boot's default server-side HTML templating engine. Templates are valid HTML
files — they can be opened directly in a browser (static prototype) or rendered by Spring on the
server. Thymeleaf attributes (`th:*`) add dynamic behaviour without breaking the HTML structure.

---

## 1. Expression types

| Expression | Syntax | Resolves against |
|---|---|---|
| Variable | `${product.name}` | Model attributes |
| Selection | `*{name}` | Object selected by `th:object` |
| Message | `#{key}` | `.properties` message files (i18n) |
| URL | `@{/products/{id}(id=${p.id})}` | Context-relative URL with params |
| Fragment | `~{template :: fragment}` | Template fragment reference |

```html
<!-- Variable expression — reads from the Model -->
<td th:text="${product.name}">Placeholder</td>

<!-- URL expression — path variable + query param -->
<a th:href="@{/products/{id}/edit(id=${product.id})}">Edit</a>

<!-- Ternary inside th:text -->
<td th:text="${product.active} ? 'Yes' : 'No'">Yes</td>
```

---

## 2. Iteration and conditionals

```html
<!-- th:each — iterates any Iterable -->
<tr th:each="product : ${products}">
    <td th:text="${product.id}">1</td>
    <td th:text="${product.name}">Name</td>
</tr>

<!-- Status variable — index, count, first, last, odd, even -->
<tr th:each="product, stat : ${products}"
    th:classappend="${stat.odd} ? 'odd-row'">
    <td th:text="${stat.count}">1</td>
</tr>

<!-- th:if / th:unless — conditional rendering (element is NOT rendered at all) -->
<p th:if="${products.empty}">No products yet.</p>
<table th:unless="${products.empty}"> ... </table>
```

---

## 3. Fragments and layouts

Fragments let you define reusable HTML blocks once and include them in multiple templates.

```html
<!-- fragments/layout.html — defines the fragments -->
<head th:fragment="page-head(title)">          <!--/* parameterized fragment */-->
    <title th:text="${title}">Page</title>
    <link rel="stylesheet" th:href="@{/css/app.css}"/>
</head>

<nav th:fragment="nav">
    <a th:href="@{/products}">Products</a>
</nav>
```

```html
<!-- products/list.html — consumes the fragments -->

<!--/*
  th:replace — replaces this element entirely with the fragment content.
  The <head> tag here disappears; the fragment's <head> takes its place.
*/-->
<head th:replace="~{fragments/layout :: page-head('Products')}"></head>

<!--/*
  th:insert — inserts the fragment INSIDE this element (host element is kept).
  <div th:insert="~{fragments/layout :: nav}">  →  <div><nav>...</nav></div>
*/-->
<div th:insert="~{fragments/layout :: nav}"></div>
```

**`th:replace` vs `th:insert`:**

| | `th:replace` | `th:insert` |
|---|---|---|
| Host element | Removed | Kept |
| Fragment position | Replaces host | Inside host |
| Common use | `<head>`, `<nav>`, `<footer>` | Embedding a widget inside a `<div>` |

---

## 4. Form handling

### Controller side

```java
// @Controller — returns view names, not response bodies
@Controller
@RequestMapping("/products")
public class ProductController {

    @GetMapping("/new")
    public String newForm(Model model) {
        // Must add the form object to the model BEFORE rendering th:object
        model.addAttribute("productForm", new ProductForm());
        return "products/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ProductForm productForm,
                         BindingResult result) {
        // BindingResult MUST be the parameter immediately after @ModelAttribute
        if (result.hasErrors()) {
            return "products/form";    // redisplay with errors
        }
        productService.create(productForm);
        return "redirect:/products";  // POST-Redirect-GET
    }
}
```

**POST-Redirect-GET pattern:** always redirect after a successful POST. Without it, pressing F5
resubmits the POST request, creating duplicate records.

**`BindingResult` must immediately follow `@ModelAttribute`:** if they are separated by another
parameter, Spring throws a 400 before the method body runs.

### Form-backing bean

```java
// Must be a mutable JavaBean (with getters AND setters) — not a record
@Data @NoArgsConstructor @AllArgsConstructor
public class ProductForm {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull @DecimalMin("0.01")
    private BigDecimal price;

    @NotBlank
    private String category;
}
```

Records have no setters, so Spring's `DataBinder` cannot populate them from POST parameters.
`@NoArgsConstructor` is required — the binder creates an empty instance then sets fields.

### Template side

```html
<form th:action="@{/products}" th:object="${productForm}" method="post">

    <label for="name">Name</label>
    <!--/*
      th:field="*{name}" expands to THREE attributes:
        id="name"        — for the <label for="name"> association
        name="name"      — POST parameter name (mapped by Spring's DataBinder)
        value="..."      — pre-filled with productForm.name (blank for new, populated for edit)
      *{} works within the th:object scope (selection variable expression)
    */-->
    <input type="text"
           th:field="*{name}"
           th:classappend="${#fields.hasErrors('name')} ? 'field-error'"/>
    <!--/* th:errors renders all error messages for the field; absent if no errors */-->
    <span class="error" th:errors="*{name}"></span>

    <button type="submit">Save</button>
</form>
```

**`#fields` utility:** provides `hasErrors(field)`, `errors(field)` (as a list), and
`allErrors()` for global errors.

---

## 5. URL expressions

```html
<!-- Simple path -->
<a th:href="@{/products}">All Products</a>

<!-- Path variable -->
<a th:href="@{/products/{id}(id=${product.id})}">View</a>

<!-- Multiple query parameters -->
<a th:href="@{/products(page=${page},size=${size})}">Next</a>

<!-- Combined path variable + query param -->
<a th:href="@{/products/{id}(id=${p.id},ref='list')}">View</a>
```

Thymeleaf automatically URL-encodes parameter values and prepends the application context path.

---

## 6. Utility objects

```html
<!-- #numbers — number formatting -->
<td th:text="${#numbers.formatDecimal(product.price, 1, 2)}">0.00</td>
<td th:text="${#numbers.formatInteger(count, 1, 'COMMA')}">1,000</td>

<!-- #strings — string utilities -->
<span th:text="${#strings.toUpperCase(product.category)}">CATEGORY</span>
<span th:if="${#strings.isEmpty(product.description)}">No description</span>

<!-- #dates / #temporals — date formatting (use #temporals for java.time) -->
<td th:text="${#temporals.format(product.createdAt, 'yyyy-MM-dd')}">2024-01-01</td>

<!-- #lists, #sets, #maps — collection utilities -->
<span th:text="${#lists.size(products)}">0</span>
```

---

## 7. Testing Thymeleaf controllers

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean  ProductService productService;

    @Test
    void list_page_renders_products() throws Exception {
        given(productService.findAll()).willReturn(List.of(
                Product.builder().id(1L).name("Laptop").category("Electronics")
                       .price(new BigDecimal("999.00")).build()
        ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/list"))          // logical view name
                .andExpect(model().attributeExists("products"))   // model has attribute
                .andExpect(content().string(containsString("Laptop"))); // template was rendered
    }

    @Test
    void create_invalid_name_shows_form_errors() throws Exception {
        mockMvc.perform(post("/products")
                        .param("name", "")          // blank — fails @NotBlank
                        .param("price", "999.00")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(model().attributeHasFieldErrors("productForm", "name"));
    }

    @Test
    void create_valid_product_redirects() throws Exception {
        given(productService.create(any())).willReturn(Product.builder().id(1L).build());

        mockMvc.perform(post("/products")
                        .param("name", "Laptop")
                        .param("price", "999.00")
                        .param("category", "Electronics"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));
    }
}
```

**What `@WebMvcTest` loads for Thymeleaf:** the named controller, `@ControllerAdvice` beans,
the Thymeleaf `TemplateEngine`, `ViewResolver`, and Jackson. Templates ARE rendered — the
`content().string(...)` assertion reads the actual HTML output.

---

## 8. Thymeleaf Security dialect (with Spring Security)

When `thymeleaf-extras-springsecurity6` is on the classpath, templates gain `sec:*` attributes:

```xml
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    <!-- version managed by Spring Boot BOM -->
</dependency>
```

```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">

<!-- Show content only to authenticated users with ROLE_ADMIN -->
<div sec:authorize="hasRole('ADMIN')">
    <a th:href="@{/products/new}">+ Add Product</a>
</div>

<!-- Display the logged-in username -->
<span sec:authentication="name">Username</span>

<!-- Show different content based on authentication status -->
<a th:href="@{/login}"  sec:authorize="isAnonymous()">Login</a>
<a th:href="@{/logout}" sec:authorize="isAuthenticated()">Logout</a>
```

---

## Key takeaways

- Thymeleaf templates are valid HTML — static fallback text (e.g., `>Placeholder</td>`) is shown
  in browser preview but replaced at runtime by `th:text`
- `th:replace` removes the host element; `th:insert` keeps it — use `th:replace` for structural
  fragments like `<head>`, `<nav>`, `<footer>`
- Form-backing beans must be mutable JavaBeans (with setters and a no-arg constructor) — records
  cannot be used with `th:field`
- Always redirect after a successful POST (POST-Redirect-GET) to prevent duplicate submissions
- `BindingResult` must immediately follow `@ModelAttribute` in the method signature
- `@WebMvcTest` renders Thymeleaf templates — assert on `view()`, `model()`, and `content()` for
  complete controller + template coverage
{% endraw %}
