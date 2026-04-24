# Code Review — Product Catalog (Spring Boot + MongoDB)

> Reviewed by: Senior Software Engineer (10 YoE, Java/Spring ecosystem)
> Scope: Spring Data Mongo best practices · SOLID · Layered architecture · General Java craftsmanship

---

## Overall Impression

This is a solid learning project. The layer separation is coherent, the DTO boundary is respected, validation groups are a nice touch, and the aggregation query in the repository shows you understand MongoDB beyond basic CRUD. That said, there are several issues ranging from **critical bugs** to **style and design improvements** that you should address before calling this production-ready.

---

## 🔴 Critical Issues

### 1. Broken `application.properties` — wrong key prefix

```
# What you wrote (WRONG — does NOT work for Spring Data MongoDB)
spring.mongodb.host=localhost
spring.mongodb.port=27017
spring.mongodb.database=product_catalog

# Correct prefix
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=product_catalog
```

The correct namespace is `spring.data.mongodb.*`. Your current keys are silently ignored, meaning the app falls back to Spring Boot defaults (`localhost:27017/test`). The app happens to "work" locally only because those defaults match your setup. In any other environment it will connect to the wrong database. This is a classic silent misconfiguration bug.

**Tip:** Never rely on coincidental defaults for infrastructure config. Always verify the actual resolved value with the Actuator `/actuator/env` endpoint.

---

### 2. `Slice` leaking into the service layer

In `ProductRepository`, queries correctly return `Slice<Product>` (good — avoids a COUNT query). However, in `ProductService` you immediately call `.getContent()` and throw away the pagination metadata:

```java
// ProductService
public List<Product> findProductsByTag(String tag, Pageable pageable) {
    return repo.findByTagsContaining(tag, pageable).getContent(); // Slice is discarded
}
```

The controller then re-wraps this in a plain `List<ProductDTO>` with no pagination metadata at all in the response. This means your client has no way to know if there is a next page, how many items exist, etc. You added `Pageable` support but then swallowed all its value.

**Fix:** Return `Slice<Product>` (or `Page<Product>`) from the service, map it in the controller, and return a proper paginated response body — either Spring's built-in `PagedModel` (via Spring HATEOAS) or a custom wrapper DTO with `content`, `hasNext`, `pageNumber`, etc.

---

### 3. `findByPriceBetween` type mismatch

```java
// Repository
Slice<Product> findByPriceBetween(int min, int max, Pageable pageable);

// Service
public List<Product> findProductInPriceRange(int min, int max, Pageable pageable) { ... }

// Controller
@GetMapping("range")
public ResponseEntity<...> findByRange(@RequestParam Integer min, @RequestParam Integer max, Pageable pageable)
```

`Product.price` is a `BigDecimal`. Spring Data MongoDB's derived query for `findByPriceBetween` will attempt to compare a `BigDecimal` field against `int` parameters. MongoDB will receive `NumberInt` vs `Decimal128` and the comparison may yield wrong or no results depending on the driver version and MongoDB storage type. Use `BigDecimal` consistently throughout the chain.

---

## 🟠 Significant Design Issues

### 4. No service interface — Dependency Inversion Principle (DIP) violated

```
ProductController → ProductService (concrete class)
```

`ProductController` depends directly on the concrete `ProductService`. This violates the **D in SOLID** (Dependency Inversion). The controller should depend on an abstraction:

```
ProductController → IProductService (interface)
                        ↑
                  ProductService (implementation)
```

**Why does this matter?**
- You cannot swap implementations without touching the controller.
- You cannot mock the service cleanly in unit tests without a mocking framework reaching into concrete classes.
- It signals that the controller "knows" about implementation details.

The fix is straightforward: extract a `ProductService` interface (or rename the interface `ProductService` and rename the class `ProductServiceImpl`).

---

### 5. Mapper placed inside `controller` package — wrong package responsibility

```
controller/
  mapper/
    ProductDTOMapper.java   ← sits here
```

The mapper is a `@Component` used to translate between domain objects and DTOs. It belongs to a **shared or infrastructure layer**, not inside `controller`. By placing it in `controller/mapper`, you're suggesting that mapping is a controller concern — but the service could also benefit from it in a more complex scenario, and it creates an uncomfortable layering smell.

**Better package structures:**
- `mapper/` at the top level (sibling of `controller`, `service`, `repository`)
- Or `shared/mapper/`

---

### 6. `ProductDTOMapper` is a stateless `@Component` — consider making it a utility or use an interface

The mapper has no state and no Spring dependencies injected into it. Annotating it with `@Component` registers it as a Spring bean, which is fine but adds overhead. Two alternatives worth knowing:

- Make it a **`@Component` that implements a `ProductMapper` interface** — allows substitution and clean mocking.
- Or make the methods **`static`** and drop the `@Component`, since it has zero dependencies. This is actually valid for pure mapping functions.

In a real project you'd likely use MapStruct, but for learning purposes, at least extract an interface for it.

---

### 7. Guard clauses in the service that should live in validation, not code

```java
// ProductService
public Product createProduct(Product p) {
    if (p == null) {
        throw new IllegalArgumentException("The product entity cannot be null");
    }
    ...
}
```

This null check is **redundant** given that:
1. The controller already validates the DTO with `@Validated(OnPost.class)` before calling the service.
2. The mapper will never return null.
3. Spring MVC will reject a missing `@RequestBody` before it even reaches your method.

The service is trying to protect itself from inputs it will never actually receive in this architecture. This is the kind of "defensive code everywhere" anti-pattern that makes the codebase noisy. The single source of truth for input validation should be the DTO + Bean Validation layer.

The `id` null check in `updateProduct` is similarly unnecessary since `@PathVariable` would have already caused a 4xx before reaching the service.

**Remove those guards and trust your validation layer.**

---

### 8. `deleteProduct` fetches the document just to delete it — unnecessary read

```java
public void deleteProduct(String id) {
    Product found = findProductById(id);  // SELECT round-trip
    repo.delete(found);                   // DELETE round-trip
}
```

This issues **two round-trips** to MongoDB for every delete. If the product does not exist, you want a 404 — that check is valid. But if it does exist, you don't need to load it just to delete it by ID.

The correct approach:

```java
public void deleteProduct(String id) {
    if (!repo.existsById(id)) {
        throw new ProductNotFoundException("Product with id " + id + " not found");
    }
    repo.deleteById(id);
}
```

This still produces the correct 404, but replaces a full document fetch with a cheap existence check (`count` query), and uses `deleteById` directly.

---

## 🟡 Style and Craftsmanship Issues

### 9. `@ResponseStatus` is redundant alongside `ResponseEntity`

```java
// GlobalExceptionHandler
@ExceptionHandler(value = IllegalArgumentException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)  // ← redundant
public ResponseEntity<ProblemDetail> handleIllegalArgumentException(...) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(p);
}
```

When you return a `ResponseEntity`, Spring uses its status. `@ResponseStatus` on the method is ignored. Pick one pattern and be consistent:
- Return `ResponseEntity<>` for full control (your current path — keep this).
- Or return `ProblemDetail` directly with `@ResponseStatus` (simpler, less explicit).

Remove all `@ResponseStatus` annotations from the handler methods.

---

### 10. `@GetMapping("range")` — missing leading slash

```java
@GetMapping("range")   // ← missing leading /
```

While Spring MVC normalizes this in most cases, it is inconsistent with the rest of your mappings (`"/word"`, `"/tag"`, `"/specifications"` all use a leading slash). Consistency matters for readability and for tools that parse routes statically (OpenAPI spec generators, documentation).

---

### 11. `FieldError` objects leaked in the validation error response

```java
p.setProperty("errors", ex.getBindingResult().getFieldErrors());
```

`FieldError` is a Spring internal class. Putting raw `FieldError` objects into your JSON response exposes Spring's internal error model to your clients — a leaky abstraction. It will also serialize with many internal fields that the client doesn't need (like `bindingFailure`, `objectName`, etc.).

Create a small response DTO like `ValidationErrorDetail(String field, String message)` and map the field errors to it:

```java
List<ValidationErrorDetail> errors = ex.getBindingResult().getFieldErrors()
    .stream()
    .map(fe -> new ValidationErrorDetail(fe.getField(), fe.getDefaultMessage()))
    .toList();
p.setProperty("errors", errors);
```

---

### 12. `auto-index-creation=true` — dangerous for production

```properties
spring.data.mongodb.auto-index-creation=true
```

This is acceptable for development but must be disabled in production. Automatic index creation on startup can cause significant performance issues if the collection is large, and can silently create wrong indexes if your `@TextIndexed` annotations change. In production, indexes should be managed through a migration tool (e.g., Mongock) or applied manually by a DBA.

At minimum, document this with a comment:

```properties
# Development only — disable in production and manage indexes via Mongock
spring.data.mongodb.auto-index-creation=true
```

---

### 13. Virtually no tests

Your test folder contains only the context-loads smoke test. For a project of this size, at minimum you should have:

| Test type | What to test |
|---|---|
| Unit tests (`@ExtendWith(MockitoExtension.class)`) | `ProductService` logic — not-found, update partial fields, etc. |
| Slice tests (`@DataMongoTest`) | Repository queries — especially `findProductsWithWord` and the aggregation |
| Web layer tests (`@WebMvcTest`) | Controller validation — ensure `@Validated(OnPost.class)` rejects bad input, correct HTTP status codes |

The aggregation query in particular is a prime candidate for a `@DataMongoTest` with an embedded MongoDB (`flapdoodle` — already declared in your pom as `spring-boot-starter-mongodb-test`).

---

### 14. `pom.xml` has duplicate / conflicting MongoDB dependencies

```xml
<dependency>spring-boot-starter-mongodb</dependency>          <!-- line 41 -->
<dependency>spring-boot-starter-data-mongodb</dependency>     <!-- line 86 -->
```

`spring-boot-starter-mongodb` is **not** a standard Spring Boot starter — it does not exist in the official catalog. The correct and only dependency you need is `spring-boot-starter-data-mongodb`. Having both listed likely means one is being resolved to an unexpected artifact or is being silently ignored. Clean this up to a single, correct entry.

Similarly for the test starters — `spring-boot-starter-mongodb-test`, `spring-boot-starter-validation-test`, and `spring-boot-starter-webmvc-test` are **not** official Spring Boot starters. The correct test dependencies are:
- `spring-boot-starter-test` (covers JUnit 5, Mockito, MockMvc)
- `de.flapdoodle.embed:de.flapdoodle.embed.mongo` or the embedded mongo auto-config

This is a significant issue — your pom may not even compile correctly in a clean environment.

---

## ✅ What You Did Well

- **DTO boundary is clean:** The controller never exposes `Product` directly; it always maps through `ProductDTO`. The `READ_ONLY` annotation on `id` is a nice touch.
- **Validation groups** (`OnPost.class`) for differentiating POST vs PUT validation is a pattern many seniors miss — well done.
- **`ProblemDetail`** (RFC 7807) is the right choice for structured error responses. Using a modern Spring Boot feature correctly.
- **`GlobalExceptionHandler`** is correctly using `@ControllerAdvice` with specific exception types — no catch-all that would swallow unknown errors.
- **`ProductNotFoundException` extends `RuntimeException`** — correct. Checked exceptions in a Spring MVC pipeline are an anti-pattern.
- **Text index usage** (`@TextIndexed`, `$text` query) shows real MongoDB knowledge beyond basic `findById`.
- **Constructor injection** throughout — you're not using `@Autowired` on fields. This is the correct approach for testability and immutability.
- **`Pageable` as a controller parameter** — Spring MVC's built-in `Pageable` resolver (via `PageableHandlerMethodArgumentResolver`) is exactly the right way to handle pagination parameters.
- **`BigDecimal` for price** — never use `float`/`double` for monetary values. Correct choice.

---

## Summary Scorecard

| Area | Score | Notes |
|---|---|---|
| Layered architecture | 7/10 | Layers exist and are respected; mapper misplaced; no service interface |
| Spring Data Mongo usage | 6/10 | Config key bug; type mismatch; Slice discarded |
| SOLID principles | 6/10 | DIP violated (no service interface); SRP mostly good |
| Validation & error handling | 8/10 | Groups are smart; FieldError leak; redundant `@ResponseStatus` |
| Testing | 2/10 | Only a smoke test |
| General Java craftsmanship | 7/10 | Unnecessary guards; redundant delete fetch; pom issues |

