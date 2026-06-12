# Mini-Project — Crunch Tracker REST API

> Wrap the Crunch Tracker domain you built and tested in Week 3 in a Spring Boot 3.5 REST API. CRUD for goals and habits, validated request bodies, RFC-9457 `ProblemDetail` errors, and a generated OpenAPI page — all backed by the in-memory repository from Week 3. No database yet. No auth yet. A documented JSON API you can curl, that the React Native app will consume in Week 9.

This is the week Crunch Tracker stops being a test-only library and becomes a *service*. Everything you wire this week is the contract the rest of the course builds on: Week 5 swaps the in-memory repository for Postgres **without changing this contract**, Week 6 adds auth **on top of** these endpoints, and Weeks 7–9 build the mobile client that talks to **exactly** the JSON you design here.

**Estimated time:** ~13 hours (split across Thursday, Friday, Saturday, Sunday in the suggested schedule).

---

## What you will build

A Spring Boot service exposing the goals and habits domain over HTTP:

```
GET    /api/goals                 list goals (optionally ?category=fitness)
GET    /api/goals/{id}            fetch one goal            (404 if absent)
POST   /api/goals                 create a goal             (201 + Location)
PUT    /api/goals/{id}            update a goal             (404 if absent)
DELETE /api/goals/{id}            delete a goal             (204; 404 if absent)

GET    /api/habits                list habits (optionally ?goalId=...)
GET    /api/habits/{id}           fetch one habit           (404 if absent)
POST   /api/habits                create a habit            (201 + Location)
PUT    /api/habits/{id}           update a habit            (404 if absent)
DELETE /api/habits/{id}           delete a habit            (204; 404 if absent)

GET    /api/health               liveness (200, typed body)
GET    /v3/api-docs              the generated OpenAPI 3.1 document
GET    /swagger-ui.html          interactive API docs
```

A habit belongs to a goal (`goalId`), matching the Week 3 domain. Creating a habit against a non-existent `goalId` returns `400` or `404` (your choice — document it).

By the end you'll have a public GitHub repo, ~600–800 lines of Java (excluding tests), that validates every request, never returns a `500` for a client mistake, ships as a single runnable fat JAR, and exports an OpenAPI spec you commit to the repo.

---

## Rules

- **You may** read the Spring docs, the lecture notes, the Week 3 domain code, and the source of the libraries below.
- **You must** reuse the Week 3 domain (`Goal`, `Habit`, the services, the in-memory repositories). **Do not** rewrite it. If you genuinely improve a domain method, note it in the README's "Changes from Week 3."
- **You may NOT** add a database, JPA, or any persistence library this week. The repository stays in-memory. (That's Week 5.)
- **You may NOT** add Spring Security or any auth this week. Every endpoint is open. (That's Week 6.)
- **You must** never serialize a domain entity to the wire. Every request and response is a DTO **record**.
- **You must** turn every error into a `ProblemDetail` from a single `@RestControllerAdvice`. A client mistake returning `500` fails the rubric.
- Target: **Spring Boot 3.5.x**, **Java 21**. Use the Maven wrapper (`./mvnw`).

---

## Acceptance criteria

- [ ] A new public GitHub repo named `c3-week-04-crunch-tracker-api-<yourhandle>`.
- [ ] Project layout follows the standard Boot/Crunch shape:
  ```
  crunch-tracker-api/
  ├── pom.xml
  ├── mvnw  mvnw.cmd
  ├── .gitignore
  ├── docs/
  │   └── openapi.json              (exported OpenAPI spec — committed)
  ├── http/
  │   └── crunch-tracker.http        (sample requests you can replay)
  └── src/
      ├── main/java/com/crunchcrunch/tracker/
      │   ├── CrunchTrackerApplication.java
      │   ├── domain/                (Week 3 code: Goal, Habit, services, repos, NotFoundException)
      │   └── web/
      │       ├── GoalController.java
      │       ├── HabitController.java
      │       ├── HealthController.java
      │       ├── GlobalExceptionHandler.java
      │       ├── KnownCategory.java  + KnownCategoryValidator.java
      │       └── dto/               (CreateGoalRequest, UpdateGoalRequest, GoalResponse, habit DTOs)
      ├── main/resources/
      │   └── application.yml
      └── test/java/com/crunchcrunch/tracker/
          ├── web/GoalControllerTest.java        (@WebMvcTest slices)
          ├── web/HabitControllerTest.java
          ├── web/GoalValidationTest.java
          └── CrunchTrackerApiEndToEndTest.java   (@SpringBootTest round-trip)
  ```
- [ ] `./mvnw test` passes with **at least 20** tests: a mix of `@WebMvcTest` slices and at least one `@SpringBootTest` end-to-end round-trip, covering happy paths AND error paths (400, 404).
- [ ] `./mvnw spring-boot:run` starts cleanly and prints "Tomcat started on port 8080."
- [ ] Every CRUD endpoint returns the correct status code: `200` reads, `201`+`Location` create, `204` delete, `404` not-found, `400` invalid body.
- [ ] Every request DTO is validated with Jakarta Bean Validation. A blank/oversized/invalid field returns `400 application/problem+json` with a per-field `errors` array.
- [ ] The custom `@KnownCategory` constraint rejects unknown goal categories.
- [ ] No domain entity appears in any controller signature — only DTO records.
- [ ] No `@ExceptionHandler` lives outside `GlobalExceptionHandler`; no controller has a `try/catch` for HTTP concerns.
- [ ] springdoc serves `/swagger-ui.html` and `/v3/api-docs`. You commit the exported `docs/openapi.json`.
- [ ] `./mvnw package` produces a runnable `target/*.jar`; `java -jar target/*.jar` starts the same service.
- [ ] Your `README.md` includes: one paragraph describing the project; setup + run commands from a fresh clone; an example `curl` for each endpoint with its response; a "Changes from Week 3" section; and a "Things I learned" section with at least 3 specific items.

---

## Suggested order of operations

Build incrementally. Each phase ends with a green build and a commit.

### Phase 1 — Project + domain (~1h)

You did most of this in Exercise 1. If you skipped it, do it now: generate the project (Web + Validation), drop in the Week 3 domain annotated with `@Service`/`@Repository`, convert `application.properties` to `application.yml`, and confirm `./mvnw compile` is clean. Commit: `Project skeleton + Week 3 domain`.

### Phase 2 — Goal CRUD with DTOs (~2.5h)

This is Exercise 2, finished and tested. Build `GoalController`, the goal DTO records, and the `GoalResponse.from` mapper. Get all five endpoints returning the right status codes. Write `@WebMvcTest` slices for create (201+Location), get-not-found (404), and delete (204). Commit: `Goal CRUD + slice tests`.

### Phase 3 — Validation + ProblemDetail (~2h)

This is Exercise 3, finished. Annotate `CreateGoalRequest`/`UpdateGoalRequest`, add `@Valid`, write `@KnownCategory`, and build `GlobalExceptionHandler` mapping `MethodArgumentNotValidException` → `400` and `NotFoundException` → `404`. Add validation slice tests. Commit: `Bean Validation + ProblemDetail error handling`.

### Phase 4 — Habit CRUD (~2.5h)

Now do it again for habits — this is where it sticks. `HabitController`, habit DTOs (`CreateHabitRequest`, `UpdateHabitRequest`, `HabitResponse`), and the `?goalId=` filter on the list. Decide and document what happens when a habit references a missing goal (validate it: a `400` or a `404`). Reuse the same advice. Slice-test the lot. Commit: `Habit CRUD + tests`.

### Phase 5 — OpenAPI docs (~1.5h)

Add `springdoc-openapi-starter-webmvc-ui`. Start the app, open `/swagger-ui.html`, click through every endpoint, and use "Try it out" to fire real requests. Enrich the docs with `@Operation`, `@ApiResponse`, and a top-level `@OpenAPIDefinition` (title, version, description). Export the spec:

```bash
curl -s localhost:8080/v3/api-docs | jq . > docs/openapi.json
```

Commit `docs/openapi.json`. This is the artifact the mobile team consumes in Week 7. Commit: `OpenAPI docs + exported spec`.

### Phase 6 — End-to-end test + .http file (~1.5h)

Write one `@SpringBootTest(webEnvironment = RANDOM_PORT)` test that creates a goal, creates a habit against it, lists, updates, and deletes — asserting status codes through `TestRestTemplate`. Then write `http/crunch-tracker.http` (REST Client / IntelliJ format) with a replayable request for every endpoint, so a reviewer can exercise the API in one click. Commit: `E2E round-trip test + .http requests`.

### Phase 7 — Polish + ship (~1.5h)

- Run `./mvnw verify` clean.
- `./mvnw package` and confirm `java -jar target/crunch-tracker-api-0.0.1-SNAPSHOT.jar` runs.
- Write the README (description, setup, per-endpoint curl examples, "Changes from Week 3", "Things I learned").
- Add a one-line CI workflow `.github/workflows/ci.yml` that runs `./mvnw -B verify` on push. (Required from Week 4 on — you set up Actions in Week 1.)
- Push to GitHub.

Commit: `README, CI, and OpenAPI export`.

---

## Example expected output

```bash
$ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' \
    -d '{"title":"Run 5k","description":"Couch to 5k","category":"fitness","targetPerWeek":3}'
HTTP/1.1 201
Location: /api/goals/3fa85f64-5717-4562-b3fc-2c963f66afa6
Content-Type: application/json

{"id":"3fa85f64-5717-4562-b3fc-2c963f66afa6","title":"Run 5k","description":"Couch to 5k","category":"fitness","targetPerWeek":3,"createdAt":"2026-06-12T14:03:11.882Z"}
```

```bash
$ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' -d '{"title":""}'
HTTP/1.1 400
Content-Type: application/problem+json

{"type":"https://crunchtracker.dev/problems/validation","title":"Validation failed","status":400,
 "detail":"One or more fields are invalid.","instance":"/api/goals",
 "errors":[{"field":"title","message":"must not be blank"},
           {"field":"category","message":"must not be blank"}]}
```

```bash
$ curl -i localhost:8080/api/goals/00000000-0000-0000-0000-000000000000
HTTP/1.1 404
Content-Type: application/problem+json

{"type":"https://crunchtracker.dev/problems/not-found","title":"Resource not found","status":404,
 "detail":"Goal 00000000-0000-0000-0000-000000000000 not found","instance":"/api/goals/00000000-..."}
```

```bash
$ curl -i -X DELETE localhost:8080/api/goals/3fa85f64-5717-4562-b3fc-2c963f66afa6
HTTP/1.1 204
```

Your formatting may differ; the status codes and the `application/problem+json` content type must not.

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Builds, runs, ships | 20% | `./mvnw verify`, `spring-boot:run`, and `java -jar` all clean on a fresh clone |
| Correct status codes | 20% | `201`+`Location`, `204`, `404`, `400` all on purpose; no `500` for client mistakes |
| DTO boundary | 15% | No entity on the wire; request/response records; mapping in one place |
| Validation + ProblemDetail | 20% | Every bad body is a `400 application/problem+json` with field-level errors; one `@RestControllerAdvice`; custom `@KnownCategory` works |
| Test coverage | 15% | ≥20 tests, slices + at least one E2E, happy and error paths both covered |
| OpenAPI + docs | 10% | Swagger UI works, `docs/openapi.json` committed, README lets a stranger run it in <5 min |

---

## Stretch (optional)

- Fold in **Challenge 1**: make `GET /api/goals` paginated, filterable, and sortable with the validated envelope.
- Add `spring-boot-starter-actuator` and expose `/actuator/health` and `/actuator/info`. Set the build info so `/actuator/info` reports the version.
- Add a `@ControllerAdvice`-level handler for `HttpMessageNotReadableException` (malformed JSON) so a truncated body returns a clean `400`, not a `500`.
- Add `springdoc` grouping so goals and habits appear as separate tags in Swagger UI.
- Write a tiny `bin/smoke.sh` that starts the JAR, curls every endpoint, asserts the status codes, and exits non-zero on any mismatch. (Foreshadows the Week 10 runbook.)

---

## What this prepares you for

- **Week 5** swaps the in-memory repository for Spring Data JPA on Postgres. Because you went through DTOs, the swap changes the `domain` package and nothing in `web`. Your tests stay green; your contract stays identical.
- **Week 6** adds Spring Security and JWT *in front of* these controllers, then scopes every goal/habit to its owner. The clean controller layer is what makes adding `@PreAuthorize` tractable.
- **Week 7** hands the mobile team your `docs/openapi.json` to build a typed client against.
- **Week 9** connects the React Native app to this exact API. The `ProblemDetail` shape you designed is what its error UI parses.

This is the spine of the whole course. Build it like the contract it is.

---

## Resources

- *Building a RESTful Web Service* — <https://spring.io/guides/gs/rest-service>
- *Spring Web MVC* — <https://docs.spring.io/spring-framework/reference/web/webmvc.html>
- *Bean Validation in Spring* — <https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html>
- *RFC 9457 — Problem Details* — <https://www.rfc-editor.org/rfc/rfc9457>
- *springdoc-openapi* — <https://springdoc.org/>
- *Testing Spring Boot applications* — <https://docs.spring.io/spring-boot/reference/testing/index.html>

---

## Submission

When done:

1. Push your repo to GitHub with a public URL.
2. Make sure the README has setup commands and a `curl` example for every endpoint.
3. Make sure `./mvnw verify`, `./mvnw spring-boot:run`, and `java -jar target/*.jar` are all green on a freshly cloned copy, and `docs/openapi.json` is committed.
4. Post the repo URL in your cohort tracker. You put an HTTP front door on three weeks of work; show it.
