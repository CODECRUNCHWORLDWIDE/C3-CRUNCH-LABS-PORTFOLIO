# Week 4 — Exercises

Short, focused drills. Each one should take 35–60 minutes. Do them in order; later ones assume earlier ones. All three build toward the same `crunch-tracker-api` service, so keep working in one project.

## Index

1. **[Exercise 1 — Bootstrap a service](./exercise-01-bootstrap-a-service.md)** — scaffold a Spring Boot 3.5 / Java 21 project, read the generated files, add a typed health endpoint, and curl it. (~40 min)
2. **[Exercise 2 — Goal controller](./exercise-02-goal-controller.java)** — fill in the TODOs in a CRUD controller that maps DTOs and returns the right status codes. (~55 min)
3. **[Exercise 3 — Validation and ProblemDetail](./exercise-03-validation-and-problemdetail.java)** — add Jakarta Bean Validation, a custom constraint, and a `@RestControllerAdvice` that emits RFC-9457 `ProblemDetail` errors. (~50 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. Muscle memory is the entire point of these drills — especially the annotations, which you must learn to recognize on sight.
- Run it. Curl the endpoint. **Check the status code first** (`curl -i`), then the body.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.
- Every exercise must end with `./mvnw test` green and `./mvnw spring-boot:run` starting cleanly. A failing test or a `500` for a client mistake means you're not done.

## The starting point

These exercises wrap the **Week 3 Crunch Tracker domain** — the `Goal`, `Habit`, `GoalService`, `HabitService`, and in-memory `GoalRepository`/`HabitRepository` you built and tested. If you have that code, drop it into the new project under `com.crunchcrunch.tracker.domain`. If you don't, the Week 3 reference solution is on the course GitHub; clone it first. Exercise 1 shows exactly where the domain files go.

## The checklist

Tick these off as you finish each exercise:

- [ ] **Ex 1:** `./mvnw spring-boot:run` starts and `curl -i localhost:8080/api/health` returns `200` with a typed JSON body (not a `Map`).
- [ ] **Ex 1:** You can name what each generated file does — `pom.xml`, `application.yml`, the `@SpringBootApplication` class, `mvnw`.
- [ ] **Ex 2:** `POST /api/goals` returns `201` with a `Location` header; `GET /api/goals/{unknown-id}` returns `404`; `DELETE` returns `204`.
- [ ] **Ex 2:** No domain entity is ever serialized to the wire — every response is a DTO record.
- [ ] **Ex 3:** A blank `title` returns `400` with `Content-Type: application/problem+json` and a per-field `errors` array.
- [ ] **Ex 3:** An unknown `category` is rejected by your custom constraint, not by an `if` in the controller.
- [ ] **All:** `./mvnw test` is green, including at least a couple of `@WebMvcTest` slice tests.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-04` to compare.
