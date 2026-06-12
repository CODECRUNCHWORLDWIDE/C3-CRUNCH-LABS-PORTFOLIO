# Exercise 1 — Design the v1 Surface

**Goal:** Before you write a line of security code, get the API surface right. You'll design Crunch Tracker's versioned REST contract on paper, then implement two pieces of it in the week-5 codebase: a central `/api/v1` path prefix and a paginated, sorted, validated list endpoint that returns a `Page<T>`.

**Estimated time:** 50 minutes.

---

## Setup

Work in your week-5 Crunch Tracker repo. Confirm it still runs:

```bash
docker compose up -d db
./mvnw spring-boot:run
# in another terminal:
http :8080/habits        # week-5 unversioned endpoint, still open
```

If that returns your habits, you're ready. If not, fix week 5 first — this week builds directly on it.

---

## Part A — Design on paper (15 min)

In a file `docs/api-v1.md`, write the contract for the resources below **before** coding. For each, give the method, the path under `/api/v1`, the success status, and the error statuses it can return.

Resources to cover: `goals`, `habits`, and a habit's `check-ins`. At minimum:

```
GET    /api/v1/habits            200  (paginated)          401
POST   /api/v1/habits            201 + Location            400, 401
GET    /api/v1/habits/{id}       200                       401, 403, 404
DELETE /api/v1/habits/{id}       204                       401, 403, 404
POST   /api/v1/habits/{id}/check-ins   201                 400, 401, 403, 404
```

Then answer these three design questions in prose (2–3 sentences each):

1. Why is there **no** `/api/v1/users/{userId}/habits` path? Where does the owner come from instead?
2. Which of your endpoints are idempotent, and which one would you protect with an `Idempotency-Key`? Why that one?
3. What's the difference between the `401` and the `403` on `GET /habits/{id}`? Give a concrete scenario for each.

This part is graded on whether your answers are *right*, not long. If you can't answer #1 cleanly, re-read Lecture 1 §2 before continuing — it's the crux of the whole week.

---

## Part B — Central version prefix (10 min)

Right now your controllers map paths like `/habits`. Add a config class that prefixes every `@RestController` with `/api/v1` so you never repeat the version and can't typo it.

Create `config/WebConfig.java`:

```java
package dev.crunch.tracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // TODO: add the prefix "/api/v1" to every class annotated @RestController.
        //       Hint: configurer.addPathPrefix(prefix, predicate)
    }
}
```

After this, your existing `@RequestMapping("/habits")` controller answers at `/api/v1/habits`. Confirm:

```bash
./mvnw spring-boot:run
http :8080/api/v1/habits     # works
http :8080/habits            # now 404 — good, the old path is gone
```

---

## Part C — Paginate, sort, and validate the list (25 min)

Replace your `GET /habits` "return everything" handler with a paginated one. Three requirements:

1. Return a `Page<HabitResponse>`, not a `List`.
2. Cap the page size. In `application.yml`:
   ```yaml
   spring:
     data:
       web:
         pageable:
           max-page-size: 100
           default-page-size: 20
   ```
3. Validate `sort`. Only `createdAt`, `name`, and `status` may be sorted on; any other field is a `400` `ProblemDetail`.

The controller:

```java
@GetMapping("/habits")
public Page<HabitResponse> list(
        @RequestParam(required = false) HabitStatus status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {

    // TODO: validate the sort fields in `pageable` against a whitelist.
    //       If a non-whitelisted field is requested, throw InvalidSortException.
    // TODO: call the repository's findAll...(status, pageable) and map to HabitResponse.
    return null;
}
```

Add the `InvalidSortException` handler to your `@RestControllerAdvice` so it produces a `400` with the allowed fields (see Lecture 1 §7).

### Verify

```bash
# good: paginated, sorted
http ':8080/api/v1/habits?page=0&size=5&sort=name,asc'
# bad sort: must be a 400 ProblemDetail, NOT a 500
http ':8080/api/v1/habits?sort=DROP_TABLE,asc'
# oversized page: server caps it at 100, does not return 1,000,000 rows
http ':8080/api/v1/habits?size=1000000'
```

---

## Acceptance criteria

- [ ] `docs/api-v1.md` exists with the contract table and answers to the three design questions.
- [ ] Every endpoint answers under `/api/v1`; the old unversioned paths `404`.
- [ ] `GET /api/v1/habits` returns a `Page<HabitResponse>` JSON shape (with `content`, `totalElements`, etc.).
- [ ] A request for `sort=<not-whitelisted>` returns a `400` `ProblemDetail` naming the allowed fields.
- [ ] A request for `size=1000000` is capped (check `totalPages`/`size` in the response — it's not a million).
- [ ] `./mvnw test` is green.
- [ ] At least 2 sensible Git commits.

---

## Stretch

- Add cursor (keyset) pagination as an alternative endpoint `GET /api/v1/check-ins?after=<timestamp>&size=20` and compare the generated SQL with the offset version (enable `spring.jpa.show-sql=true`). Notice the offset version's `OFFSET n` vs the cursor's `WHERE performed_at < ?`.
- Add an `Idempotency-Key` header to `POST /habits/{id}/check-ins` backed by a simple in-memory `Map` (per-user), and write a test that sends the same key twice and asserts only one check-in is created.

---

## Hints

<details>
<summary>Part B — the path-prefix predicate</summary>

```java
configurer.addPathPrefix("/api/v1",
        clazz -> clazz.isAnnotationPresent(RestController.class));
```

The predicate receives each handler's class; return `true` to apply the prefix.

</details>

<details>
<summary>Part C — validating the sort whitelist</summary>

```java
private static final Set<String> SORTABLE = Set.of("createdAt", "name", "status");

private void validateSort(Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
        if (!SORTABLE.contains(order.getProperty())) {
            throw new InvalidSortException(order.getProperty(), SORTABLE);
        }
    }
}
```

Call `validateSort(pageable)` at the top of the handler. `Pageable.getSort()` is iterable.

</details>

<details>
<summary>Why cap the page size at the framework level?</summary>

`spring.data.web.pageable.max-page-size` is enforced by Spring's `PageableHandlerMethodArgumentResolver` *before* your controller runs. You don't have to write a manual check — but you should *know* it's there, because without it a single `size=10000000` request can OOM your service.

</details>

---

When this feels comfortable, move to [Exercise 2 — The JWT service](exercise-02-jwt-service.java).
