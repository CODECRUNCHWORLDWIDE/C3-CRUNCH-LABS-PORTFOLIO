# Lecture 2 — A Clean HTTP Surface

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can build a CRUD controller backed by a service, separate the wire format from the domain with DTO records, return the right status code on purpose, validate request bodies with Jakarta Bean Validation, turn every error into an RFC-9457 `ProblemDetail`, document the API with springdoc-openapi, and prove it all with `MockMvc` tests.

If you only remember one thing from this lecture, remember this:

> **The API is a contract, and DTOs are where you write it.** Never serialize a domain entity straight to the wire. Define a request DTO and a response DTO as Java records, map between them explicitly, validate at the edge, and return a status code that tells the truth. The mobile app in Week 9 consumes exactly the JSON your DTOs produce — so the DTOs *are* the public surface of your system. Design them like one.

This lecture covers a lot of surface area because the rest of the backend weeks assume you've seen all of it once. We go deeper on persistence (Week 5) and auth (Week 6); here we make the HTTP layer correct.

---

## 1. The controller, and what it should and shouldn't do

A controller is a **thin** translation layer. Its job: receive an HTTP request, pull out the inputs, hand them to a service, and turn the result into an HTTP response. **No business logic in controllers.** The `GoalService` you wrote in Week 3 owns the rules; the controller just speaks HTTP on its behalf.

```java
package com.crunchcrunch.tracker.web;

import com.crunchcrunch.tracker.domain.GoalService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) {  // constructor injection, from Lecture 1
        this.service = service;
    }

    // handler methods below...
}
```

Two annotations carry the load:

- **`@RestController`** = `@Controller` + `@ResponseBody`. Every method's return value is serialized to the response body (as JSON by default) rather than treated as a view name.
- **`@RequestMapping("/api/goals")`** at the class level sets a base path. Method-level mappings append to it. So `@GetMapping` on a method maps `GET /api/goals`; `@GetMapping("/{id}")` maps `GET /api/goals/{id}`.

The HTTP-verb shortcut annotations — `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` — are each `@RequestMapping(method = ...)` with the verb baked in. Use them; they read better than the long form.

---

## 2. Binding inputs: path variables, query params, request bodies

Spring binds three kinds of input into your method parameters.

**`@PathVariable`** — a segment of the URL path:

```java
@GetMapping("/{id}")
public GoalResponse get(@PathVariable UUID id) { ... }
// GET /api/goals/3fa85f64-...  →  id = that UUID
```

**`@RequestParam`** — a query-string parameter:

```java
@GetMapping
public List<GoalResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) String category) { ... }
// GET /api/goals?page=2&category=fitness  →  page = 2, category = "fitness"
```

`required = false` makes a param optional (binds `null` if absent); `defaultValue` supplies a fallback. Spring converts the string to the declared type — `int`, `UUID`, `LocalDate`, an enum — and returns a clean `400` if the conversion fails (no work from you).

**`@RequestBody`** — the JSON request body, deserialized by Jackson into an object:

```java
@PostMapping
public GoalResponse create(@RequestBody CreateGoalRequest request) { ... }
// POST /api/goals  with  {"title":"Run 5k","category":"fitness"}  →  request populated
```

That `CreateGoalRequest` is a DTO, which is the next section.

---

## 3. DTOs: the wire format is not your domain

Here is the mistake every Spring beginner makes, and why you won't:

```java
// DON'T: returning the domain entity directly
@GetMapping("/{id}")
public Goal get(@PathVariable UUID id) {   // Goal is your domain type
    return service.findById(id);
}
```

It works on day one. Then:

- Week 5 adds a JPA `@Entity` with a lazy `@OneToMany` of check-ins, and serializing it triggers a lazy-load explosion or a `LazyInitializationException`.
- Week 6 adds an `ownerId` and a password hash to the user-linked entity, and now you're leaking internal fields onto the wire.
- The mobile team wants `progressPercent` (computed) but not `internalVersion` (an optimistic-lock column), and you can't give them one without the other because the entity *is* the contract.

The fix is a hard boundary. Define a **request DTO** for input and a **response DTO** for output, both as records, and map explicitly:

```java
package com.crunchcrunch.tracker.web.dto;

import jakarta.validation.constraints.*;

// What a client may SEND to create a goal. Only the fields they control.
public record CreateGoalRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 500) String description,
        @NotBlank String category,
        @Positive int targetPerWeek) {
}
```

```java
package com.crunchcrunch.tracker.web.dto;

import java.time.Instant;
import java.util.UUID;

// What a client RECEIVES. Only the fields they should see, plus computed ones.
public record GoalResponse(
        UUID id,
        String title,
        String description,
        String category,
        int targetPerWeek,
        Instant createdAt) {

    public static GoalResponse from(Goal goal) {  // explicit mapping in one place
        return new GoalResponse(
                goal.id(), goal.title(), goal.description(),
                goal.category(), goal.targetPerWeek(), goal.createdAt());
    }
}
```

Records are ideal DTOs: immutable, concise, and Jackson knows how to (de)serialize them via the canonical constructor. The mapping (`from`) lives in one place; when the domain and the wire format diverge — and they will — you change the mapper, not fifty call sites.

> **Why not a mapping library (MapStruct)?** Plenty of teams use MapStruct to generate mappers. For a domain this size, hand-written `from`/`toDomain` static methods are clearer and have zero magic. We hand-map this week; you can adopt MapStruct later when the mapping count justifies it.

---

## 4. Status codes you choose on purpose

HTTP status codes are part of your contract. Returning the right one is not pedantry — the mobile client *branches on the status code* (Week 9: `if (response.status === 201) ...`). Get these reflexes:

| Situation | Status | How to return it |
|-----------|--------|------------------|
| Read succeeded | `200 OK` | Return the body; default for a non-void method |
| Resource created | `201 Created` + `Location` header | `ResponseEntity.created(uri).body(dto)` |
| Update/delete succeeded, no body | `204 No Content` | `@ResponseStatus(NO_CONTENT)` or `ResponseEntity.noContent()` |
| Client sent invalid data | `400 Bad Request` | Bean Validation does it; or throw |
| Resource not found | `404 Not Found` | Throw a `NotFoundException` handled by advice |
| Conflict (duplicate, version) | `409 Conflict` | Throw a `ConflictException` handled by advice |

The two beginners get wrong most often:

**`201 Created` with a `Location` header.** When a `POST` creates a resource, return `201`, not `200`, and include a `Location` header pointing at the new resource. `ResponseEntity` makes this clean:

```java
@PostMapping
public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
    Goal created = service.create(request.title(), request.description(),
                                  request.category(), request.targetPerWeek());
    GoalResponse body = GoalResponse.from(created);
    URI location = URI.create("/api/goals/" + created.id());
    return ResponseEntity.created(location).body(body);
}
```

```bash
$ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' \
    -d '{"title":"Run 5k","category":"fitness","targetPerWeek":3}'
HTTP/1.1 201 Created
Location: /api/goals/3fa85f64-5717-4562-b3fc-2c963f66afa6
Content-Type: application/json

{"id":"3fa85f64-...","title":"Run 5k", ... }
```

**`204 No Content` for a delete.** A successful delete returns nothing; say so:

```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable UUID id) {
    service.delete(id);   // throws NotFoundException if it's not there → 404
}
```

A void method with `@ResponseStatus(NO_CONTENT)` returns `204` and an empty body — exactly the contract a delete should have.

---

## 5. Bean Validation: reject bad input at the edge

You never trust a request body. Jakarta Bean Validation lets you declare the rules as annotations on the DTO and have Spring enforce them before your controller method body runs.

Annotate the DTO (you saw this above):

```java
public record CreateGoalRequest(
        @NotBlank(message = "title is required") @Size(max = 120) String title,
        @Size(max = 500) String description,
        @NotBlank String category,
        @Positive int targetPerWeek) {
}
```

The constraints you'll reach for:

| Annotation | Asserts |
|------------|---------|
| `@NotNull` | Not null (may be empty) |
| `@NotBlank` | Not null and not whitespace-only (strings) |
| `@NotEmpty` | Not null and not empty (strings, collections) |
| `@Size(min=, max=)` | Length/size in range |
| `@Positive` / `@PositiveOrZero` | Numeric sign |
| `@Min` / `@Max` | Numeric bounds |
| `@Email` | Valid email format |
| `@Pattern(regexp=)` | Matches a regex |
| `@Past` / `@Future` | Temporal constraints on dates |

Then **trigger** validation with `@Valid` on the parameter:

```java
@PostMapping
public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) { ... }
```

Without `@Valid`, the annotations are decoration that does nothing. With it, Spring validates the bound object *before* your method body runs. On failure it throws `MethodArgumentNotValidException` and never enters your method — so your method body can assume valid input. That assumption is the entire point: validation at the edge means clean code in the core.

A validation failure produces a `400`. But the *default* `400` body is generic. Making it a precise, machine-readable `ProblemDetail` is the next section.

> **Custom constraints.** When a built-in annotation can't express your rule (e.g. "category must be one of a known set"), write a custom constraint: an annotation plus a `ConstraintValidator<YourAnnotation, String>`. The exercise file walks you through one. Reach for it only when the built-ins genuinely can't express the rule — most rules are a `@Pattern` or `@Size` away.

---

## 6. RFC 9457 ProblemDetail: errors clients can parse

When something goes wrong, a REST API must return a body the *client* can act on — not a stack trace, and not a 500 for what was the client's mistake. The standard for this is **RFC 9457, "Problem Details for HTTP APIs,"** and Spring has first-class support via the `ProblemDetail` class.

A `ProblemDetail` body looks like:

```json
{
  "type": "https://crunchtracker.dev/problems/validation",
  "title": "Validation failed",
  "status": 400,
  "detail": "title is required",
  "instance": "/api/goals",
  "errors": [
    { "field": "title", "message": "title is required" },
    { "field": "targetPerWeek", "message": "must be greater than 0" }
  ]
}
```

It's served with `Content-Type: application/problem+json`. The `type`, `title`, `status`, `detail`, and `instance` fields are standard; you can attach extra properties (`errors` here) for structured detail.

You centralize error handling in one class with `@RestControllerAdvice` — a global exception handler that applies to every controller:

```java
package com.crunchcrunch.tracker.web;

import com.crunchcrunch.tracker.domain.NotFoundException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — Bean Validation failures, with per-field detail
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        pd.setTitle("Validation failed");
        pd.setType(URI.create("https://crunchtracker.dev/problems/validation"));
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                                  "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
                .toList();
        pd.setProperty("errors", errors);
        return pd;
    }

    // 404 — domain "not found"
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource not found");
        pd.setType(URI.create("https://crunchtracker.dev/problems/not-found"));
        return pd;
    }
}
```

Spring reads the `ProblemDetail`'s status, sets the response code, serializes the body as `application/problem+json`, and you're done. Return type is `ProblemDetail`; Spring handles the rest.

The discipline this enforces: **the controller and service never `try/catch` for HTTP.** The service throws a domain `NotFoundException`; the advice translates it to a `404`. The validation layer throws `MethodArgumentNotValidException`; the advice translates it to a `400`. Cross-cutting error handling lives in exactly one place. When you need a new error mapping, you add one `@ExceptionHandler` method — you don't sprinkle `try/catch` through the codebase.

> **Don't let a 500 stand for a client mistake.** If `curl`-ing a malformed body returns `500`, your error handling has a gap. Every *expected* failure mode — bad input, missing resource, conflict — should map to a 4xx `ProblemDetail`. A `500` should mean "the server genuinely broke," and those should be rare and alarming.

---

## 7. Documenting the API with springdoc-openapi

A REST API nobody can discover is half-built. **springdoc-openapi** reads your controllers, DTOs, and validation annotations at startup and generates an **OpenAPI 3.1** document plus an interactive **Swagger UI** — for free, just by adding the dependency.

Add it to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

Restart and visit:

- **`http://localhost:8080/v3/api-docs`** — the raw OpenAPI 3.1 JSON. This is the machine-readable contract; the mobile team can generate a typed client from it.
- **`http://localhost:8080/swagger-ui.html`** — an interactive page listing every endpoint, its parameters, request/response schemas (derived from your DTO records), and a "Try it out" button that fires real requests.

springdoc infers most of the doc from your code: the paths from `@RequestMapping`, the schemas from your DTO records, the required fields from `@NotNull`/`@NotBlank`. You enrich it with annotations where the inference isn't enough:

```java
@Operation(summary = "Create a goal", description = "Creates a new goal for the tracker.")
@ApiResponse(responseCode = "201", description = "Goal created")
@ApiResponse(responseCode = "400", description = "Validation failed")
@PostMapping
public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) { ... }
```

The payoff is real: the Swagger page *is* your API documentation, it can never drift from the code because it's generated from the code, and the `/v3/api-docs` JSON is the artifact you hand the front-end team in Week 7. The mini-project requires you to export it.

---

## 8. Testing the HTTP surface

You tested the domain in Week 3. Now test the HTTP layer. Two tools, two altitudes.

**`@WebMvcTest` — the controller slice.** Loads *only* the web layer (your controller, the advice, the converters) and mocks the service. Fast, focused, no embedded server:

```java
package com.crunchcrunch.tracker.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GoalController.class)
class GoalControllerTest {

    @Autowired MockMvc mvc;
    @MockBean GoalService service;   // the service is mocked; we test only the web layer

    @Test
    void rejects_blank_title_with_400_problemdetail() throws Exception {
        mvc.perform(post("/api/goals")
                .contentType("application/json")
                .content("""
                    { "title": "", "category": "fitness", "targetPerWeek": 3 }
                    """))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentType("application/problem+json"))
           .andExpect(jsonPath("$.title").value("Validation failed"))
           .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void returns_201_with_location_on_create() throws Exception {
        var goal = new Goal(/* a valid goal with a known id */);
        when(service.create("Run 5k", null, "fitness", 3)).thenReturn(goal);

        mvc.perform(post("/api/goals")
                .contentType("application/json")
                .content("""
                    { "title": "Run 5k", "category": "fitness", "targetPerWeek": 3 }
                    """))
           .andExpect(status().isCreated())
           .andExpect(header().exists("Location"))
           .andExpect(jsonPath("$.title").value("Run 5k"));
    }
}
```

`MockMvc` calls your controller through the full Spring MVC machinery — mapping, binding, validation, advice, Jackson — *without* a real socket. You assert the status, headers, content type, and JSON body with `jsonPath`. This is the test you write most.

**`@SpringBootTest` — the full stack.** Boots the entire `ApplicationContext` (real service, real in-memory repository) and, with `webEnvironment = RANDOM_PORT`, a real embedded server you hit with `TestRestTemplate` or `WebTestClient`. Slower; use it for the handful of true end-to-end paths:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GoalApiEndToEndTest {
    @Autowired TestRestTemplate rest;

    @Test
    void create_then_fetch_round_trips() {
        var created = rest.postForEntity("/api/goals",
                new CreateGoalRequest("Run 5k", null, "fitness", 3), GoalResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var fetched = rest.getForObject("/api/goals/" + created.getBody().id(), GoalResponse.class);
        assertThat(fetched.title()).isEqualTo("Run 5k");
    }
}
```

> **The test pyramid for this week:** many fast `@WebMvcTest` slices that assert status codes and JSON bodies, a few `@SpringBootTest` round-trips for the critical happy paths, and the Week 3 domain unit tests still running underneath. The mini-project rubric requires both slice and end-to-end tests.

---

## 9. Putting it together — a full CRUD controller

Here is the whole controller with every idea from this lecture, against the Week 3 `GoalService`:

```java
package com.crunchcrunch.tracker.web;

import com.crunchcrunch.tracker.domain.Goal;
import com.crunchcrunch.tracker.domain.GoalService;
import com.crunchcrunch.tracker.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService service;

    public GoalController(GoalService service) {
        this.service = service;
    }

    @GetMapping
    public List<GoalResponse> list(@RequestParam(required = false) String category) {
        return service.findAll(category).stream().map(GoalResponse::from).toList();
    }

    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable UUID id) {
        return GoalResponse.from(service.findById(id));   // throws NotFoundException → 404
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest req) {
        Goal created = service.create(req.title(), req.description(), req.category(), req.targetPerWeek());
        return ResponseEntity
                .created(URI.create("/api/goals/" + created.id()))
                .body(GoalResponse.from(created));
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateGoalRequest req) {
        return GoalResponse.from(service.update(id, req.title(), req.description(), req.targetPerWeek()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);   // throws NotFoundException → 404
    }
}
```

Read it slowly. Every Week 4 idea is here in forty lines:

- **Constructor injection** of the service (Lecture 1).
- **DTOs** in and out (`CreateGoalRequest`, `UpdateGoalRequest`, `GoalResponse`) — never the entity.
- **`@Valid`** so bad bodies are rejected before the method runs.
- **Status codes on purpose:** `200` for reads, `201`+`Location` for create, `204` for delete.
- **No error handling here** — `NotFoundException` propagates to the `@RestControllerAdvice` and becomes a `404` `ProblemDetail`.

That is the modern Spring Boot 3 REST baseline. It compiles on Java 21, it's covered by `@WebMvcTest` slices, and the mobile app in Week 9 consumes exactly this contract.

---

## 10. Recap

You should now be able to:

- Write a thin `@RestController` that delegates to a service and holds no business logic.
- Bind path variables, query params, and request bodies, and know which annotation does which.
- Separate the wire format from the domain with request/response DTO records and explicit mapping.
- Return `200`, `201`+`Location`, `204`, `400`, `404`, and `409` deliberately.
- Validate request bodies with Jakarta Bean Validation and `@Valid`.
- Translate every error into an RFC-9457 `ProblemDetail` from a single `@RestControllerAdvice`.
- Generate OpenAPI docs + Swagger UI with springdoc, and enrich them with annotations.
- Test the controller with `@WebMvcTest` + `MockMvc` and the full stack with `@SpringBootTest`.

Next, do the exercises — three drills that exercise bootstrapping, CRUD with DTOs, and validation + `ProblemDetail` in isolation.

---

## References

- *Spring Web MVC controllers* — <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html>
- *Bean Validation in Spring* — <https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html>
- *Error handling with `@ExceptionHandler`* — <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html>
- *RFC 9457 — Problem Details* — <https://www.rfc-editor.org/rfc/rfc9457>
- *springdoc-openapi* — <https://springdoc.org/>
- *Testing Spring Boot apps* — <https://docs.spring.io/spring-boot/reference/testing/index.html>
