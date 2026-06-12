# Week 4 Homework

Six practice problems that revisit the week's topics. The full set should take about **5 hours** in total. Work in your Week 4 Git repository (the `crunch-tracker-api` project) so each problem produces at least one commit you can point to later.

Each problem includes a short **problem statement**, **acceptance criteria** so you know when you're done, a **hint** if you get stuck, and an **estimated time**.

---

## Problem 1 — Read the conditions report

**Problem statement.** Start your service with auto-configuration debugging on, and learn what Boot wired for you. Run:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--debug
```

(or add `debug=true` to `application.properties`). In the giant output, find the **"Positive matches"** and **"Negative matches"** sections. Write a short note in `notes/auto-config.md` answering:

1. Name **three** auto-configurations that **matched** (fired) and, for each, the condition that made it match (e.g. `WebMvcAutoConfiguration matched: @ConditionalOnClass found DispatcherServlet`).
2. Name **two** that did **not** match, and why (e.g. `DataSourceAutoConfiguration did not match: no DataSource class on the classpath`).
3. One sentence: what would change in this report if you added `spring-boot-starter-data-jpa`?

**Acceptance criteria.**

- `notes/auto-config.md` exists with the three matches, two non-matches, and the one-sentence answer.
- Committed.

**Hint.** The report is long; pipe it through `grep` or scroll to the `CONDITIONS EVALUATION REPORT` banner. Negative matches are the most instructive — they tell you what Boot *would* configure if a dependency were present.

**Estimated time.** 30 minutes.

---

## Problem 2 — A `409 Conflict` for duplicate titles

**Problem statement.** Add a rule: a goal `title` must be unique (case-insensitive). Creating a goal whose title already exists returns `409 Conflict` as a `ProblemDetail`, **not** a `400` and **not** a `500`.

Implement it properly: the **service** detects the duplicate and throws a new `ConflictException` (a `RuntimeException`); the **`@RestControllerAdvice`** maps `ConflictException` → `409`. The controller stays untouched.

**Acceptance criteria.**

- A `ConflictException` in the domain package.
- `GoalService.create` throws it on a duplicate (case-insensitive) title.
- A new `@ExceptionHandler(ConflictException.class)` in `GlobalExceptionHandler` returning `409 ProblemDetail`.
- A `@WebMvcTest` test proving a duplicate returns `409` with `application/problem+json`.
- `./mvnw test` green. Committed.

**Hint.** Status: `HttpStatus.CONFLICT`. The advice method is a near-copy of your `handleNotFound`, just a different status and `type` URI. Don't put the uniqueness check in the controller — it's business logic, it belongs in the service.

**Estimated time.** 45 minutes.

---

## Problem 3 — A custom `@Pattern`-based constraint, end to end

**Problem statement.** Habits in Crunch Tracker have a `schedule` field — a cron-ish day mask like `"MON,WED,FRI"`. Add validation so the field must be a comma-separated list of valid three-letter weekday codes (`MON,TUE,WED,THU,FRI,SAT,SUN`), in any order, no duplicates allowed.

Decide: can `@Pattern` alone express "no duplicates"? (No — a regex can't easily forbid repeats.) So write a custom `@WeekdayMask` constraint + validator that splits, uppercases, and checks membership and uniqueness.

**Acceptance criteria.**

- `@WeekdayMask` annotation + `WeekdayMaskValidator`.
- Applied to the habit `schedule` DTO field.
- Three tests: a valid mask passes; an unknown code (`"MON,XYZ"`) returns `400`; a duplicate (`"MON,MON"`) returns `400`.
- The validator returns `true` for `null` (let `@NotBlank` own null/blank).
- `./mvnw test` green. Committed.

**Hint.** Split on `,`, trim, uppercase each token. Check every token is in the known set **and** that `tokens.size() == new HashSet<>(tokens).size()` (no dupes). Model it on the `@KnownCategory` validator from Exercise 3.

**Estimated time.** 1 hour.

---

## Problem 4 — Slice tests for every status code

**Problem statement.** Pick your `GoalController` and write a focused `@WebMvcTest` class that proves **every** status code the controller can return, with the service mocked:

1. `GET /api/goals/{id}` found → `200` + correct JSON.
2. `GET /api/goals/{id}` missing (service throws `NotFoundException`) → `404` + `application/problem+json`.
3. `POST` valid → `201` + `Location` header.
4. `POST` blank title → `400` + `errors[].field == "title"`.
5. `DELETE` → `204` with empty body.

**Acceptance criteria.**

- A `GoalControllerStatusTest` (or similar) with at least these five tests.
- Each test asserts the status **and** at least one other fact (a header, a JSON field, the content type).
- The service is a `@MockBean`; no real repository is touched.
- `./mvnw test` shows them passing. Committed.

**Hint.** Use `when(service.findById(id)).thenThrow(new NotFoundException("..."))` to drive the 404 path. For the 404 assertion: `.andExpect(content().contentType("application/problem+json"))`. For the 201: `.andExpect(header().exists("Location"))`.

**Estimated time.** 1 hour.

---

## Problem 5 — Externalize a value and prove the override order

**Problem statement.** Add a configurable feature flag `crunch-tracker.max-goals-per-category` (default `25`). Bind it with `@ConfigurationProperties` (not scattered `@Value`), read it in `GoalService.create`, and reject creation past the limit with a `409`. Then **prove the property override order** from Lecture 1: set the value three ways and observe which wins.

In `notes/config-override.md`, record what value `max-goals-per-category` resolves to when:

1. Only `application.yml` sets it to `5`.
2. `application.yml` says `5` but you start with `--crunch-tracker.max-goals-per-category=2`.
3. `application.yml` says `5` but the env var `CRUNCH_TRACKER_MAX_GOALS_PER_CATEGORY=3` is set (no command-line arg).

**Acceptance criteria.**

- A `@ConfigurationProperties`-bound record/class holding the limit, default `25`.
- `GoalService` enforces it (throws `ConflictException` past the limit).
- `notes/config-override.md` records the three resolved values (expected: `5`, then `2`, then `3`).
- Committed.

**Hint.** Boot's relaxed binding maps `CRUNCH_TRACKER_MAX_GOALS_PER_CATEGORY` → `crunch-tracker.max-goals-per-category`. Override priority (highest first): command-line args > env vars > `application.yml`. So arg `2` beats env `3` beats file `5`.

**Estimated time.** 1 hour.

---

## Problem 6 — Mini reflection essay

**Problem statement.** Write a 300–400 word reflection at `notes/week-04-reflection.md` answering:

1. Which felt easiest this week: the DI/auto-config mental model, the controller/DTO mapping, or the validation + `ProblemDetail` error handling? Which felt hardest? Why?
2. Before this week, did you think of Spring Boot as "magic"? After reading the conditions report and tracing a request through the `DispatcherServlet`, what changed?
3. In one paragraph, explain to a teammate why you map domain objects to DTO records instead of serializing entities directly — and tie it to what's coming in Week 5.
4. What's one thing about REST APIs you want to understand better that this week didn't cover (auth, versioning, caching, rate limiting, ...)?

**Acceptance criteria.**

- File exists, 300–400 words.
- Each numbered question is addressed in its own paragraph.
- Committed.

**Hint.** This is for *you*, not a grade. Be honest about what was confusing. Future-you, debugging a Week 9 CORS-and-401 mess, will be glad you wrote down how the request lifecycle actually works.

**Estimated time.** 30 minutes.

---

## Grading rubric

Homework is graded out of 100. Each problem is weighted; partial credit applies.

| Problem | Weight | Full marks require |
|--------:|-------:|--------------------|
| 1 — Conditions report | 10 | Three real matches with conditions, two real non-matches with reasons, the JPA sentence |
| 2 — `409` for duplicates | 20 | Service throws, advice maps to `409 ProblemDetail`, controller untouched, passing test |
| 3 — `@WeekdayMask` constraint | 20 | Working custom constraint, handles unknown + duplicate + null, three passing tests |
| 4 — Status-code slice tests | 20 | All five status codes proven, each test asserts status + one more fact, service mocked |
| 5 — Externalized config + override order | 20 | `@ConfigurationProperties` binding, limit enforced, all three resolved values correct |
| 6 — Reflection | 10 | 300–400 words, all four questions, in your own voice |

**Automatic deductions:**

- Any client mistake that returns a `500` instead of a `4xx`: **−10**.
- A domain entity serialized directly to the wire anywhere: **−10**.
- Business logic (the uniqueness check, the limit check) placed in a controller instead of the service: **−5**.
- `./mvnw test` not green on a fresh clone: **−15**.

**Bands:** 90–100 production-ready; 75–89 solid, minor gaps; 60–74 works but cuts corners; below 60 revisit the lectures before Week 5.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 30 min |
| 2 | 45 min |
| 3 | 1 h 0 min |
| 4 | 1 h 0 min |
| 5 | 1 h 0 min |
| 6 | 30 min |
| **Total** | **~4 h 45 min** |

When you've finished all six, push your repo and open the [mini-project](./mini-project/README.md) if you haven't already started it.
