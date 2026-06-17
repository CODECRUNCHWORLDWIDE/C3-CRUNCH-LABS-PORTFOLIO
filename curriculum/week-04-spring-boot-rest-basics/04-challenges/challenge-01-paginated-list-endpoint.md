# Challenge 1 — A Paginated, Filterable, Sortable List Endpoint

**Time estimate:** ~120 minutes.

## Problem statement

Replace the simple `GET /api/goals` from Exercise 2 with a real-world list endpoint that supports **pagination**, **filtering**, and **sorting** — and that returns a structured `400 ProblemDetail` (never a stack trace) when the query is malformed.

You are implementing this contract:

```
GET /api/goals?page=0&size=20&category=fitness&minTarget=2&sort=createdAt,desc
```

| Query param | Type | Rules | Default |
|-------------|------|-------|---------|
| `page` | int | `>= 0` | `0` |
| `size` | int | `1..100` | `20` |
| `category` | string | optional; one of the known categories if present | none (no filter) |
| `minTarget` | int | optional; `>= 0`; filters `targetPerWeek >= minTarget` | none |
| `q` | string | optional; case-insensitive substring match on `title` | none |
| `sort` | string | `field,direction`; field ∈ {`title`, `targetPerWeek`, `createdAt`}; direction ∈ {`asc`, `desc`} | `createdAt,desc` |

The response is a **paged envelope** — not a bare array — because the client needs the total count to render "Page 2 of 7":

```json
{
  "content": [ { "id": "...", "title": "Run 5k", "category": "fitness", "targetPerWeek": 3, "createdAt": "2026-06-12T..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false,
  "sort": "createdAt,desc"
}
```

### Required behaviours

1. **Happy path.** A well-formed query returns `200` with the envelope above, correctly paged, filtered, and sorted.
2. **Defaults.** `GET /api/goals` with no params returns page 0, size 20, sorted `createdAt,desc`.
3. **Validation — out-of-range page/size.** `page=-1` or `size=0` or `size=101` returns `400 ProblemDetail` naming the offending param. **Not** a `500`. **Not** a clamped silent default.
4. **Validation — bad sort.** `sort=color,sideways` returns `400` explaining the allowed fields and directions.
5. **Validation — unknown category.** `category=telepathy` returns `400` (reuse your `@KnownCategory` constraint).
6. **Empty result.** A filter that matches nothing returns `200` with `content: []`, `totalElements: 0`, `last: true` — not a `404`. An empty list is a valid result, not an error.
7. **Stable ordering.** Two goals with the same `createdAt` still come back in a deterministic order (tie-break on `id`).

## Implementation guidance

You are still on the **in-memory repository** this week — there is no database, so you implement paging/sorting/filtering in Java with the Streams API over the Week 3 `GoalService`. (Next week Spring Data's `Pageable` does this for you against Postgres, and your *contract* stays identical — that's the point.)

Suggested shape:

- A `GoalQuery` record that captures the validated, parsed inputs (`page`, `size`, `category`, `minTarget`, `q`, `sortField`, `sortDirection`).
- A `PagedResponse<T>` record for the envelope.
- Bind the raw query params with `@RequestParam`, validate ranges with Bean Validation (annotate the params and add `@Validated` to the controller class), and parse `sort` yourself — throwing a constraint-style failure on a bad value so it lands in your existing `@RestControllerAdvice`.
- Do the filter → sort → page pipeline as one readable Streams chain in the service or a small `GoalListService`.

> **Validating `@RequestParam`, not a body.** Parameter-level validation (`@Min`, `@Max` on a `@RequestParam`) throws `ConstraintViolationException`, *not* `MethodArgumentNotValidException`. Add a second `@ExceptionHandler(ConstraintViolationException.class)` to your advice that maps it to the same `400 ProblemDetail` shape, and remember `@Validated` on the controller class is what turns param validation on.

## Acceptance criteria

- [ ] `GET /api/goals` with the full query string returns the documented envelope.
- [ ] All six validation behaviours (page, size, sort field, sort direction, category, minTarget) return `400 ProblemDetail` with `Content-Type: application/problem+json` and name the offending param.
- [ ] An empty match returns `200` with an empty `content` array — never a `404`.
- [ ] Sorting is correct and stable (tie-break on `id`).
- [ ] `./mvnw test` passes with **at least 10** `@WebMvcTest` tests covering: each happy-path sort field, both directions, the empty case, and each of the six validation failures.
- [ ] The controller is thin — parsing/validation at the edge, the filter/sort/page logic in a service, no business logic in the handler.
- [ ] springdoc shows the endpoint in Swagger UI with all six params documented (`@Parameter` descriptions).
- [ ] Committed under `challenges/challenge-01/` in your Week 4 repo with a short `README.md` showing three example `curl` calls and their output.

## Stretch

- Add `?fields=id,title` projection so the client can ask for a subset of the response fields. Decide and document how you handle an unknown field name.
- Support multi-sort: `sort=category,asc&sort=createdAt,desc` (sort by category, then recency). Spring binds repeated params to a `List<String>` — parse each.
- Add an `ETag`/`If-None-Match` so an unchanged page returns `304 Not Modified`. (Compute a weak ETag from the page contents.)
- Write one `@SpringBootTest` end-to-end test that seeds ~50 goals through the real service and asserts `totalPages` and the boundary pages.

## Hints

<details>
<summary>The paged-response envelope record</summary>

```java
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort) {

    public static <T> PagedResponse<T> of(List<T> all, List<T> pageSlice,
                                          int page, int size, String sort) {
        long total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        return new PagedResponse<>(pageSlice, page, size, total,
                Math.max(totalPages, 1), page == 0, (page + 1) >= totalPages, sort);
    }
}
```

</details>

<details>
<summary>Validating <code>@RequestParam</code> ranges</summary>

```java
@RestController
@RequestMapping("/api/goals")
@Validated   // <-- REQUIRED for @Min/@Max on @RequestParam to be enforced
public class GoalController {

    @GetMapping
    public PagedResponse<GoalResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @KnownCategory String category,
            @RequestParam(required = false) @PositiveOrZero Integer minTarget,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        // parse sort, build GoalQuery, delegate
    }
}
```

And in the advice:

```java
@ExceptionHandler(ConstraintViolationException.class)
public ProblemDetail handleParamViolation(ConstraintViolationException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid query parameter.");
    pd.setTitle("Validation failed");
    pd.setType(URI.create("https://crunchtracker.dev/problems/validation"));
    List<Map<String, String>> errors = ex.getConstraintViolations().stream()
            .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
            .toList();
    pd.setProperty("errors", errors);
    return pd;
}
```

</details>

<details>
<summary>The filter → sort → page pipeline</summary>

```java
Comparator<Goal> base = switch (sortField) {
    case "title"         -> Comparator.comparing(Goal::title, String.CASE_INSENSITIVE_ORDER);
    case "targetPerWeek" -> Comparator.comparingInt(Goal::targetPerWeek);
    default              -> Comparator.comparing(Goal::createdAt);
};
Comparator<Goal> cmp = (direction == Sort.DESC ? base.reversed() : base)
        .thenComparing(Goal::id);   // stable tie-break

List<Goal> filtered = service.findAll(null).stream()
        .filter(g -> category == null || g.category().equalsIgnoreCase(category))
        .filter(g -> minTarget == null || g.targetPerWeek() >= minTarget)
        .filter(g -> q == null || g.title().toLowerCase().contains(q.toLowerCase()))
        .sorted(cmp)
        .toList();

List<Goal> slice = filtered.stream().skip((long) page * size).limit(size).toList();
```

</details>

## Submission

Commit your work under `challenges/challenge-01/` in your Week 4 GitHub repo. Make sure `./mvnw test` and a fresh `./mvnw spring-boot:run` both work on a clean clone, and that your `README.md` shows the three `curl` examples (a happy query, an empty match, and a validation failure) with their actual output.

## Why this matters

Pagination, filtering, and sorting are not advanced features — they are table stakes. The first time a list grows past a few hundred rows, an endpoint that returns the whole array falls over. By building the validated, enveloped version now, against the in-memory repository, you've written the contract once. When Week 5 swaps in `Pageable` and a JPA query, the client sees no difference — and that invisible swap is exactly the payoff of designing the HTTP surface as a contract this week.
