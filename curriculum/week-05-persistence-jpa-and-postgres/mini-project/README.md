# Mini-Project — Persist Crunch Tracker on PostgreSQL

> Swap Crunch Tracker's in-memory repositories for **Spring Data JPA on PostgreSQL**, with the schema versioned by **Flyway** and the whole thing running on **Docker Compose**. The Week 4 REST contract stays *exactly* the same — same endpoints, same DTOs, same status codes, same OpenAPI docs — but the data is now real, durable, and reproducible. Prove it with **Testcontainers** integration tests that hit an actual Postgres, not a mock.

This is the week the app stops being a demo. By the end, a teammate clones your repo, runs `docker compose up -d`, runs `./mvnw test`, and gets your exact schema and your exact green suite. That reproducibility is the deliverable.

**Estimated time:** ~8.5 hours (split across Thursday, Friday, Saturday in the suggested schedule).

---

## What you're building on

You are **extending the same Crunch Tracker repo** you've carried since Week 1 — not starting over. Coming into this week you have:

- Java 21 domain types for `Goal`, `Habit`, `CheckIn` (Week 2: records and sealed types).
- Clean services behind interfaces with a JUnit 5 + AssertJ suite (Week 3).
- A Spring Boot 3 REST API: CRUD for goals and habits, validated request bodies, ProblemDetail errors, generated OpenAPI docs (Week 4).
- Crucially, **`GoalRepository` and `HabitRepository` *interfaces*** with in-memory implementations behind them.

This week you replace the in-memory implementations with JPA-backed ones — and because the controllers depend on the *interface*, the API surface doesn't change. That's the seam you built in Week 3 doing its job.

---

## What you will deliver

A Crunch Tracker backend where:

- `docker compose up -d` starts a PostgreSQL 16 container.
- The app boots, **Flyway** builds the `goals`, `habits`, and `check_ins` schema from versioned migrations, and **Hibernate validates** the entities against it.
- Every Week 4 endpoint works unchanged, now reading and writing real rows.
- The `Habit → CheckIn` one-to-many is mapped, lazy, and free of N+1 on every list endpoint.
- The list endpoints are **paginated** (`Page<...>`), not `findAll()`-everything.
- **Testcontainers** integration tests prove the repositories and the migrations against real Postgres.

---

## Rules

- **You may** read the Spring, Hibernate, Flyway, and Testcontainers docs, the lecture notes, and the source of the libraries below.
- **You must** keep the Week 4 API contract. If a Week 4 integration test or curl command worked before, it must work identically now. (Run your Week 4 tests against the new persistence — they should pass untouched.)
- **You must** use Flyway for *all* schema. `spring.jpa.hibernate.ddl-auto` is set to `validate` and stays there. No `update`, no `create`, in any profile.
- **You must NOT** use H2 anywhere — not in tests, not in dev. Integration tests run on Postgres via Testcontainers. Same engine as production.
- **Every** `@ManyToOne` is explicitly `FetchType.LAZY`. **Every** enum column is `@Enumerated(EnumType.STRING)`.
- `open-in-view: false`. Lazy access happens inside `@Transactional` service methods; controllers see only DTOs.
- Target: Java 21, Spring Boot 3.x (latest GA), PostgreSQL 16.

---

## Acceptance criteria

- [ ] The repo has a root `compose.yaml` defining a `postgres:16` service with a named volume and a healthcheck.
- [ ] `src/main/resources/db/migration/` contains versioned Flyway migrations:
  - `V1__create_goals.sql`
  - `V2__create_habits_and_check_ins.sql`
  - (and any later `V3__...` for refinements — never an edit to an applied one)
- [ ] `./mvnw spring-boot:run` boots cleanly: Flyway applies all migrations, Hibernate `validate` passes, no schema-drift errors.
- [ ] `GoalRepository` and `HabitRepository` extend `JpaRepository`; the hand-written in-memory implementations are **deleted**.
- [ ] `Goal`, `Habit`, `CheckIn` are mapped entities with UUID PKs, `@Version` optimistic locking, `STRING` enums, and `timestamptz` timestamps.
- [ ] `Habit → CheckIn` is a bidirectional one-to-many: lazy `@ManyToOne` owning side, `@OneToMany(mappedBy="habit", cascade=ALL, orphanRemoval=true)` inverse side, with a helper method that keeps both sides in sync.
- [ ] Every list endpoint is paginated and accepts `?page=&size=&sort=`.
- [ ] **Zero N+1** on any list endpoint. At least one query-count test asserts a list endpoint that touches check-ins runs a bounded number of queries, independent of row count.
- [ ] **Testcontainers** integration tests cover both repositories and the migrations, using `@ServiceConnection`. At least **12** integration/repository tests pass against real Postgres.
- [ ] All Week 4 tests still pass unchanged.
- [ ] `optimistic lock → 409`: a stale-version update returns `409 Conflict`, with a test that proves it.
- [ ] A malformed/empty request still returns the Week 4 ProblemDetail 400 (Bean Validation is untouched).
- [ ] `README.md` documents the local loop (clone → compose up → run → test) and lists the schema.

---

## Suggested order of operations

Build incrementally. Get one aggregate fully persisted before you touch the next.

### Phase 1 — Postgres + dependencies (~0.5h)

1. Add `compose.yaml` (from Lecture 2 / Exercise 1). `docker compose up -d`, confirm `healthy`.
2. Add `spring-boot-starter-data-jpa`, `postgresql` (runtime), `flyway-core`, `flyway-database-postgresql` to `pom.xml`.
3. Add the test dependencies: `spring-boot-testcontainers`, `org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`.
4. Configure `application.yml`: datasource, `ddl-auto: validate`, `open-in-view: false`, Flyway enabled, SQL logging on.
5. Commit: `chore(persistence): add JPA, Postgres driver, Flyway, Testcontainers`.

### Phase 2 — Goals on JPA (~1.5h)

1. Write `V1__create_goals.sql`.
2. Map the `Goal` entity (UUID PK, `STRING` status enum, `timestamptz` created_at, `@Version`).
3. Replace `GoalRepository` with a `JpaRepository<Goal, UUID>`; delete the in-memory impl.
4. Boot; confirm Flyway applies V1 and validation passes.
5. Round-trip a goal through the existing API; confirm Week 4 goal tests still pass.
6. Write a `GoalRepositoryIT` with Testcontainers: save/find, `findByStatus`, optimistic-lock 409.
7. Commit: `feat(goals): persist goals via Spring Data JPA on Postgres`.

### Phase 3 — Habits + check-ins on JPA (~2h)

1. Write `V2__create_habits_and_check_ins.sql` (habits + check_ins, FK with `ON DELETE CASCADE`, unique `(habit_id, checked_on)`, indexes on FK and cadence).
2. Map `Habit` (entity, `STRING` cadence enum, `@Version`) and `CheckIn` (lazy `@ManyToOne`, FK column).
3. Wire the bidirectional one-to-many with the sync helper (`habit.checkIn(day, note)`).
4. Replace `HabitRepository` with a `JpaRepository<Habit, UUID>`; delete the in-memory impl.
5. Add `findByCadence` (derived) and a `JOIN FETCH` finder for "habit with check-ins."
6. Confirm Week 4 habit tests pass unchanged.
7. Commit: `feat(habits): persist habits and check-ins with a lazy one-to-many`.

### Phase 4 — Pagination + N+1 sweep (~1.5h)

1. Convert every list endpoint from `List<...> findAll()` to `Page<...> findAll(Pageable)`. Bind `?page=&size=&sort=` in the controllers.
2. Find the endpoints that touch check-ins. Turn on SQL logging and *count the queries* (Lecture 1 §12).
3. Fix any N+1 with a `JOIN FETCH`, `@EntityGraph`, or a constructor projection. Re-count.
4. Write a query-count test that asserts the fix (Hibernate `Statistics`).
5. Commit: `perf(habits): paginate list endpoints and eliminate N+1 on the summary`.

### Phase 5 — Testcontainers + hardening (~2h)

1. Flesh out the integration suite: at least 12 tests across both repositories and the migrations.
2. Add the optimistic-lock → 409 mapping (exception handler) if not already done, with a test.
3. Confirm a malformed body still returns the Week 4 ProblemDetail 400.
4. Run the **whole** suite against Postgres: `./mvnw test`. Green.
5. Commit: `test(persistence): Testcontainers integration tests for goals and habits`.

### Phase 6 — Docs + smoke (~1h)

1. Update `README.md`: the local loop, the schema (a small table or ERD sketch), and how to run the tests.
2. Smoke the full loop on a *fresh clone* in a temp dir (`git clone . /tmp/ct-smoke && cd /tmp/ct-smoke && docker compose up -d && ./mvnw test`).
3. Commit: `docs(persistence): document the local Postgres + Flyway loop`.

---

## Example: the local loop your README must document

```bash
git clone <repo> && cd crunch-tracker
docker compose up -d                          # Postgres 16, healthy
./mvnw spring-boot:run                          # Flyway migrates, Hibernate validates, app on :8080

curl -s -X POST localhost:8080/api/habits \
  -H 'Content-Type: application/json' \
  -d '{"name":"Read","targetPerWeek":5,"cadence":"DAILY"}' | jq

curl -s "localhost:8080/api/habits?page=0&size=20&sort=name,asc" | jq

./mvnw test                                     # unit + Testcontainers integration, green
docker compose down                             # data persists in the named volume
```

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Builds, boots, migrates | 20% | Fresh clone: `compose up`, `spring-boot:run`, Flyway applies all migrations, Hibernate `validate` passes — zero manual schema steps |
| Contract preserved | 15% | Every Week 4 endpoint and test works unchanged; same JSON, same status codes |
| Entity mapping quality | 15% | UUID PKs, `STRING` enums, `timestamptz`, `@Version`; entity ≠ DTO; lazy `@ManyToOne`; correct owning/inverse sides |
| Flyway discipline | 10% | Versioned, forward-only migrations; no applied migration ever edited; `ddl-auto: validate` everywhere |
| N+1 eliminated + proven | 15% | No N+1 on any list endpoint, with a query-count test that proves it |
| Testcontainers coverage | 15% | 12+ integration tests on real Postgres via `@ServiceConnection`; no H2 anywhere |
| README + reproducibility | 10% | Someone unfamiliar clones and gets a green suite in <10 minutes |

---

## Stretch (optional)

- Add a `R__seed_demo_data.sql` repeatable migration that inserts a handful of demo habits and check-ins, gated to a `dev` profile, so a new contributor sees data immediately.
- Add a `category` aggregate with a `@ManyToOne` from `Habit`, and a constructor-projection report "check-ins per category this week."
- Add a database-level partial unique index (e.g. one *active* goal per title) and surface the constraint violation as a ProblemDetail 409.
- Wire a GitHub Actions job that runs `./mvnw test` with Testcontainers in CI (the runner has Docker). Green check on every push.
- Read `EXPLAIN ANALYZE` for your dashboard query in `psql` and note the plan in the README. Don't optimize yet — just learn to read it.

---

## What this prepares you for

- **Week 6 (Auth)** adds an `owner` column to every aggregate and a `WHERE owner_id = ?` to every query. The entities you mapped this week gain a `@ManyToOne User owner` and per-user data scoping. The migration discipline you built here is exactly how that column ships.
- **Week 9 (Integration)** has the React Native app hammering these exact endpoints. Pagination and the absence of N+1 are what keep that app responsive.
- **Week 10 (Capstone)** deploys this against a *managed* Postgres. The Flyway migrations you wrote run unchanged against the managed database — schema-as-code is what makes "deploy" boring.

---

## Resources

- *Spring Data JPA reference*: <https://docs.spring.io/spring-data/jpa/reference/>
- *Spring Boot — Testcontainers*: <https://docs.spring.io/spring-boot/reference/testing/testcontainers.html>
- *Flyway — getting started*: <https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html>
- *Hibernate User Guide — fetching*: <https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#fetching>
- *Vlad Mihalcea — N+1*: <https://vladmihalcea.com/n-plus-1-query-problem/>

---

## Submission

When done:

1. Push your repo to GitHub (`github.com/CODECRUNCHWORLDWIDE/...` for the org reference; your fork for the work).
2. Make sure `README.md` documents the local loop and the schema.
3. Make sure `docker compose up -d` + `./mvnw test` is green on a freshly cloned copy.
4. Post the repo URL in your cohort tracker. You turned a demo into a durable product — show it.
