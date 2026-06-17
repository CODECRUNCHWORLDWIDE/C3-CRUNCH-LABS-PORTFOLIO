# Lecture 1 — REST API Design That Ages Well

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can design a versioned REST surface for Crunch Tracker that survives change — sane resources, URI versioning, idempotent writes, paginated and sortable lists, a consistent RFC 9457 error contract, and CORS configured for exactly the clients you want and no others.

If you only remember one thing from this lecture, remember this:

> **Your API contract is a promise to clients you will never meet.** The mobile app, a future web dashboard, a partner's integration — they all depend on the shapes you ship. You can change your database, your services, your language; you cannot casually change the contract without breaking someone. Design it like it's permanent, because for your clients it is.

Last week Crunch Tracker got real persistence: JPA, Postgres, Flyway. This week we make it safe to *expose*. Before we add auth (Lecture 2), we get the surface right, because a well-designed surface is what auth attaches to. A `/api/v1/habits/{id}` that returns a paginated, owner-scoped list is far easier to secure than a sprawling, inconsistent set of endpoints.

---

## 1. Two questions people conflate

Beginners say "I added authentication" when they mean three different things. Keep them separate:

- **Authentication (authn):** *Who are you?* — proving identity. A JWT does this (Lecture 2).
- **Authorization (authz):** *Are you allowed to do this?* — deciding access. Ownership and roles do this (Lecture 2).
- **API design:** *What is the shape of the thing you're allowed to do?* — resources, methods, status codes, errors. **This** lecture.

You cannot secure a mess. So we design the surface first. The order matters: a clean, versioned, consistent surface is a small attack surface and a predictable thing to write authorization rules against.

---

## 2. Resource modeling for Crunch Tracker

REST is about **resources** (nouns) manipulated with a fixed set of **methods** (verbs). The verbs are HTTP's; you don't invent them. Your job is to pick the nouns and map them to URIs.

Crunch Tracker's domain from weeks 2–5: a user has **goals**, a goal has **habits**, a habit accrues **check-ins**. That hierarchy suggests the URIs:

```
GET    /api/v1/goals                       list the caller's goals
POST   /api/v1/goals                       create a goal
GET    /api/v1/goals/{goalId}              one goal
PUT    /api/v1/goals/{goalId}              replace a goal
PATCH  /api/v1/goals/{goalId}              partial update
DELETE /api/v1/goals/{goalId}              delete a goal

GET    /api/v1/habits                      list the caller's habits
POST   /api/v1/habits                      create a habit
GET    /api/v1/habits/{habitId}            one habit
DELETE /api/v1/habits/{habitId}            delete a habit

POST   /api/v1/habits/{habitId}/check-ins  record a check-in for a habit
GET    /api/v1/habits/{habitId}/check-ins  list a habit's check-ins
```

A few design choices worth defending:

- **Plural collection nouns.** `/goals`, not `/goal`. The collection is plural; the item is the collection plus an id. This is the near-universal convention; consistency beats cleverness.
- **No verbs in the path.** Not `/createGoal`, not `/getHabits`. The verb is the HTTP method. `POST /goals` *is* "create a goal." If you find yourself wanting `/habits/{id}/archive`, that's a signal you might model archival as a state transition — `PATCH /habits/{id}` with `{"status":"archived"}` — though a dedicated sub-resource action (`POST /habits/{id}/archive`) is a legitimate, common exception when the action isn't a simple field set.
- **Nest only one level.** `/habits/{habitId}/check-ins` is fine. `/goals/{g}/habits/{h}/check-ins/{c}` is too deep — once you have a check-in id, address it directly at `/check-ins/{c}`. Deep nesting bakes the hierarchy into every URL and hurts when it changes.
- **The caller is implicit.** Notice there is **no** `/users/{userId}/goals`. The user is *whoever the token says they are*. Putting the user id in the path invites exactly the bug we spend Lecture 2 preventing (user A passing user B's id). The owner comes from the authenticated principal, never from the URL.

---

## 3. Status codes that mean something

The status code is part of the contract. Clients branch on it. Get them right:

| Situation | Status | Notes |
|-----------|-------:|-------|
| `GET` succeeded | `200 OK` | The body is the resource(s). |
| `POST` created a resource | `201 Created` | Include a `Location: /api/v1/goals/42` header. |
| `PUT`/`PATCH`/`DELETE` succeeded, no body | `204 No Content` | Or `200` if you return the updated resource. |
| Request body failed validation | `400 Bad Request` | Body is a `ProblemDetail` listing the field errors. |
| No/invalid credentials | `401 Unauthorized` | "I don't know who you are." (authn) |
| Known user, not allowed | `403 Forbidden` | "I know who you are; you can't do this." (authz) |
| Resource doesn't exist | `404 Not Found` | |
| Conflict (e.g. email taken) | `409 Conflict` | Registration with a used email. |
| Unhandled server error | `500 Internal Server Error` | Never leak a stack trace in the body. |

The `401` vs `403` distinction trips up everyone. Mnemonic: **401 = "log in"; 403 = "you're logged in, but no."** A request with a missing or expired token gets `401`. A request with a *valid* token for user A asking for user B's habit gets `403`.

> **A deliberate exception.** Some teams return `404` instead of `403` for "you're not allowed to see this" so that an attacker can't even confirm the resource exists (it prevents enumeration). That's a defensible security posture. For C3 we use `403` because it's clearer for learning and our IDs are not secrets. Know the tradeoff; pick on purpose.

---

## 4. Idempotency: which methods are safe to retry

A method is **idempotent** if making the same request twice has the same effect as making it once. This isn't a nice-to-have; it's how clients survive flaky networks. A mobile app on a subway loses connectivity mid-request constantly — it *will* retry.

Per RFC 9110, the contract is:

| Method | Safe? | Idempotent? |
|--------|:-----:|:-----------:|
| `GET` | yes | yes |
| `HEAD` | yes | yes |
| `PUT` | no | **yes** |
| `DELETE` | no | **yes** |
| `PATCH` | no | no (not guaranteed) |
| `POST` | no | **no** |

"Safe" means it doesn't change server state (reads). "Idempotent" means repeating it is harmless.

- `PUT /goals/42 {whole goal}` is idempotent: sending it five times leaves goal 42 in the same final state.
- `DELETE /goals/42` is idempotent: the first deletes; the rest are no-ops that still return `204` (or `404` — your choice, but be consistent).
- `POST /goals {new goal}` is **not** idempotent: five retries create five goals. This is the dangerous one.

### Making `POST` safe to retry with an idempotency key

The standard fix: the client generates a unique `Idempotency-Key` (a UUID) per logical operation and sends it as a header. The server remembers keys it has seen and, on a repeat, returns the *original* result instead of creating a duplicate.

```java
@PostMapping("/api/v1/check-ins")
public ResponseEntity<CheckInResponse> create(
        @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
        @Valid @RequestBody CreateCheckInRequest body,
        @AuthenticationPrincipal AppUser caller) {

    // If we've processed this key before, replay the stored response.
    if (key != null) {
        Optional<CheckInResponse> prior = idempotencyStore.find(caller.id(), key);
        if (prior.isPresent()) {
            return ResponseEntity.ok(prior.get());
        }
    }

    CheckInResponse created = checkInService.record(caller.id(), body);

    if (key != null) {
        idempotencyStore.save(caller.id(), key, created);
    }
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
}
```

In production the `idempotencyStore` is backed by a table (or Redis) with a TTL, scoped per user so keys can't collide across accounts. For Crunch Tracker, recording the same check-in twice is the exact scenario this prevents: a user taps "done," the response is lost, they tap again — without an idempotency key that's two check-ins and a corrupted streak.

You don't have to implement this for every endpoint. Implement it where a duplicate is *harmful* — creating check-ins, anything that affects a count or a charge. It's a stretch goal for this week's mini-project, not a requirement, but you should be able to explain it.

---

## 5. Pagination: never return an unbounded list

`GET /api/v1/check-ins` for a user who's been tracking for a year could return thousands of rows. Returning all of them is a bug waiting to happen — slow, memory-hungry, and a denial-of-service vector. **Every list endpoint must be paginated.**

### Offset pagination (page/size)

The common, simple approach: the client asks for a page number and a page size.

```
GET /api/v1/check-ins?page=0&size=20&sort=performedAt,desc
```

Spring Data gives you this almost for free with `Pageable` and `Page<T>`:

```java
@GetMapping("/api/v1/check-ins")
public Page<CheckInResponse> list(
        @AuthenticationPrincipal AppUser caller,
        @PageableDefault(size = 20, sort = "performedAt", direction = Sort.Direction.DESC)
        Pageable pageable) {

    return checkInRepository
            .findAllByHabit_OwnerId(caller.id(), pageable)
            .map(CheckInResponse::from);
}
```

A `Page<T>` serializes to JSON with the content plus metadata:

```json
{
  "content": [ { "id": 1, "performedAt": "2026-06-10T08:00:00Z" } ],
  "totalElements": 412,
  "totalPages": 21,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

> **Cap the page size.** A client asking for `size=1000000` should not get a million rows. Spring Boot's `spring.data.web.pageable.max-page-size` property (default 2000) caps it; set it lower (say 100) for Crunch Tracker. Without a cap, pagination is theater.

### Cursor (keyset) pagination — the grown-up option

Offset pagination has two real problems at scale: `OFFSET 100000` makes Postgres scan and discard 100,000 rows (slow), and if rows are inserted while a user pages, items shift and they see duplicates or skips.

**Cursor pagination** fixes both: instead of "page 5," the client says "everything after *this* item." The cursor is typically the sort key of the last item seen.

```
GET /api/v1/check-ins?after=2026-06-10T08:00:00Z&size=20
```

```sql
SELECT * FROM check_ins
WHERE habit_owner_id = :ownerId AND performed_at < :after
ORDER BY performed_at DESC
LIMIT :size;
```

This is `WHERE ... < cursor LIMIT n` — it uses the index and never scans-and-discards. For Crunch Tracker's volumes, offset is fine and simpler. Use cursor pagination when a list can grow without bound and is hit by an infinite-scroll UI (which is exactly what the mobile feed becomes by week 9). Know both; default to offset until you measure a problem.

---

## 6. Filtering and sorting — and validating them

Clients want subsets. Expose filters as query parameters and **validate them**, because a bad parameter must be a `400`, never a `500`.

```
GET /api/v1/habits?status=active&sort=createdAt,desc
```

The trap: if you let `sort` map straight onto an arbitrary column name, a client can sort by a column that doesn't exist (`sort=DROP`) and you get an ugly `500` or, worse, leak internals. Whitelist sortable fields:

```java
private static final Set<String> SORTABLE = Set.of("createdAt", "name", "status");

private Sort parseSort(String raw) {
    String[] parts = raw.split(",", 2);
    String field = parts[0];
    if (!SORTABLE.contains(field)) {
        throw new InvalidSortException(field, SORTABLE);
    }
    Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
    return Sort.by(dir, field);
}
```

`InvalidSortException` maps to a `400` `ProblemDetail` (next section). The rule: **never pass an untrusted string to a query builder as a column name.** Whitelist, or use a typed enum.

For richer filtering, Spring Data's `Specification` API or Querydsl lets clients combine predicates safely. For this week, a couple of `@RequestParam` filters with explicit handling is plenty.

---

## 7. The error contract: RFC 9457 ProblemDetail

You met `ProblemDetail` in week 4. We make it non-negotiable this week, **including for auth errors**. A consistent error body is part of the contract; clients should never have to parse a free-text stack trace.

RFC 9457 defines a JSON shape with `type`, `title`, `status`, `detail`, and `instance`. Spring Boot 3 has first-class support via `ProblemDetail` and `@ExceptionHandler`.

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidSortException.class)
    public ProblemDetail onInvalidSort(InvalidSortException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid sort field");
        pd.setType(URI.create("https://crunch.dev/problems/invalid-sort"));
        pd.setProperty("allowedFields", ex.allowed());
        return pd;
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ProblemDetail onDuplicateEmail(EmailAlreadyUsedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "An account with that email already exists.");
        pd.setTitle("Email already in use");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        pd.setTitle("Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }
}
```

A validation failure now returns:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "errors": { "name": "must not be blank", "targetPerWeek": "must be at least 1" }
}
```

Auth failures should speak the same dialect. Spring Security's default `401`/`403` are bare; in Lecture 2 we plug in an `AuthenticationEntryPoint` and `AccessDeniedHandler` that emit `ProblemDetail` bodies so a `401` looks like every other error, not like a servlet container's HTML page.

> **Never leak internals in `detail`.** "Could not connect to database `crunch_prod` at 10.0.4.2:5432" is a gift to an attacker. The `detail` should be safe for a client to read. Log the real exception server-side; return a sanitized message.

---

## 8. Versioning: design for the day you must break the contract

You *will* eventually need to change a response shape incompatibly — rename a field, change a type, restructure a resource. When that day comes, you cannot break existing clients. **Versioning** buys you the ability to ship v2 while v1 keeps working.

There are three common strategies:

1. **URI versioning** — `/api/v1/goals`, `/api/v2/goals`. Explicit, cache-friendly, trivially visible in logs and browser bars. The most common choice and what we use in C3.
2. **Header versioning** — `Accept: application/vnd.crunch.v2+json`. "Purer" REST (the URI identifies the resource, the header negotiates representation) but harder to test, debug, and cache.
3. **Query-param versioning** — `/api/goals?version=2`. Easy but ugly and easy to forget; rarely a good default.

We use **URI versioning**. Every endpoint lives under `/api/v1`. Code it so the version prefix is centralized, not repeated in every controller:

```java
@RestController
@RequestMapping("/api/v1/goals")
public class GoalController { /* ... */ }
```

Better, set a base path once so you never typo `v1`:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1",
                clazz -> clazz.isAnnotationPresent(RestController.class));
    }
}
```

Now controllers map `@RequestMapping("/goals")` and the `/api/v1` prefix is applied centrally. When v2 arrives, you introduce a `/api/v2` prefix for the new controllers and the old ones keep serving v1.

> **Don't version prematurely.** v1 is your *first* version, not a promise of a second. Most APIs never ship a v2. The discipline is: start at v1, never put unversioned endpoints in production, and version *additively* (adding a field is not breaking; removing or renaming one is). You version when you must break, not on a schedule.

---

## 9. CORS from first principles

Here is the scenario that confuses everyone: your API runs at `https://api.crunch.dev`, your web dashboard at `https://app.crunch.dev`. The browser blocks `app` from calling `api` unless `api` explicitly says it's allowed. That's **CORS** — Cross-Origin Resource Sharing — and it is a *browser* security mechanism, not a server one.

Key facts that clear up the confusion:

- **CORS is enforced by the browser, not your server.** `curl`, Postman, and your mobile app's native HTTP client ignore it entirely. If your API works from `curl` but fails from a browser with a CORS error, the API is fine; the *CORS headers* are missing.
- **An "origin" is scheme + host + port.** `https://app.crunch.dev` and `http://app.crunch.dev` are different origins. So are `:443` and `:3000`.
- **The preflight.** Before certain requests (anything with a custom header like `Authorization`, or methods beyond simple `GET`/`POST`), the browser sends an automatic `OPTIONS` request asking "am I allowed?" Your server must answer it with the right `Access-Control-Allow-*` headers, *or the real request never fires*.

### Configuring CORS in Spring

Do it centrally, in the security config, so it's consistent and you don't sprinkle `@CrossOrigin` annotations everywhere:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
            "http://localhost:8081",          // Expo dev server (mobile)
            "https://app.crunch.dev"          // future web dashboard
    ));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(Duration.ofMinutes(30));   // cache the preflight

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Then wire it into the filter chain (Lecture 2 shows the whole chain):

```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
```

> **Never ship `allowedOrigins("*")` with `allowCredentials(true)`.** The spec forbids it, and for good reason: a wildcard origin that also sends credentials means *any* website can make authenticated requests on your users' behalf. Spring will throw at startup if you try. List your real origins explicitly. The mobile app's native HTTP client doesn't trigger CORS at all, but the **Expo dev server** runs in a browser context during development (`http://localhost:8081`), so you genuinely need it there.

---

## 10. Putting the surface together

Here's a controller that uses most of this lecture — versioned (via the central prefix), paginated, sorted, validated, owner-scoped, returning `201` with a `Location`:

```java
@RestController
@RequestMapping("/habits")    // /api/v1 added centrally
@Validated
class HabitController {

    private final HabitService habits;

    HabitController(HabitService habits) {
        this.habits = habits;
    }

    @GetMapping
    Page<HabitResponse> list(
            @AuthenticationPrincipal AppUser caller,
            @RequestParam(required = false) HabitStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return habits.listForOwner(caller.id(), status, pageable);
    }

    @PostMapping
    ResponseEntity<HabitResponse> create(
            @AuthenticationPrincipal AppUser caller,
            @Valid @RequestBody CreateHabitRequest body) {
        HabitResponse created = habits.create(caller.id(), body);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    HabitResponse one(@AuthenticationPrincipal AppUser caller, @PathVariable Long id) {
        return habits.getForOwner(caller.id(), id);   // throws 404/403, never returns another user's habit
    }
}
```

Notice every method takes `@AuthenticationPrincipal AppUser caller` and passes `caller.id()` into the service. The service scopes every query by owner. That's the seam where Lecture 2's authorization lives — the controller never trusts an id from the URL to decide *whose* data to touch; it trusts only the authenticated principal.

---

## 11. Recap

You should now be able to:

- Model Crunch Tracker's domain as REST resources without verbs in the path or the user id in the URL.
- Choose the right status code, especially `401` vs `403`.
- Explain which methods are idempotent and make a `POST` safe to retry with an `Idempotency-Key`.
- Paginate every list endpoint with `Pageable`/`Page`, cap the page size, and explain when to reach for cursor pagination.
- Validate `sort`/filter parameters so a bad one is a `400`, not a `500`.
- Return a consistent RFC 9457 `ProblemDetail` for every error.
- Version under `/api/v1` centrally and explain when (and when not) to ship a v2.
- Configure CORS for exactly your clients, and explain why a wildcard origin with credentials is forbidden.

Next: we make the API know *who* is calling. Continue to [Lecture 2 — Stateless Auth with Spring Security 6](./02-stateless-auth-with-spring-security-6.md).

---

## References

- *Spring Security — Architecture*: <https://docs.spring.io/spring-security/reference/servlet/architecture.html>
- *RFC 9110 — HTTP Semantics (methods, idempotency, status codes)*: <https://www.rfc-editor.org/rfc/rfc9110.html>
- *RFC 9457 — Problem Details for HTTP APIs*: <https://www.rfc-editor.org/rfc/rfc9457.html>
- *Spring Data — Paging and Sorting*: <https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html>
- *Spring Security — CORS*: <https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html>
- *OWASP API Security Top 10 (2023)*: <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>
