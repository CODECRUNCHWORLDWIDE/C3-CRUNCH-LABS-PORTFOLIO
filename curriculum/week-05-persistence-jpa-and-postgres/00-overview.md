# Week 5 — Persistence: JPA and Postgres

Welcome to **C3 · Crunch Labs Portfolio**, Week 5. Last week the Crunch Tracker API went live — controllers, DTOs, Bean Validation, ProblemDetail errors, generated OpenAPI docs — but every byte of data lived in a `HashMap` that vanished the moment the JVM stopped. This week we fix that. By Friday the same REST surface you shipped in Week 4 is backed by **PostgreSQL**, mapped with **Spring Data JPA / Hibernate**, versioned with **Flyway**, and proven against a **real database in a container** via Testcontainers — not a mock, not H2, the actual Postgres your production app will run on.

This is the week "works on my machine" starts meaning something. A teammate clones the repo, runs `docker compose up`, runs `./mvnw test`, and gets the exact same schema and the exact same green suite you do. That reproducibility is the entire point of treating schema as code.

We assume you finished Weeks 1–4: you can drive a sprint board, you write Java 21 with records and sealed types, your domain is behind interfaces with a JUnit 5 suite, and you have a Spring Boot 3 REST API with a `HabitRepository` *interface* that currently has an in-memory implementation. That interface is the seam. This week we slot a JPA-backed implementation behind it without the controllers noticing.

The thing to internalize early: **JPA is a specification, Hibernate is the implementation, Spring Data JPA is the convenience layer on top, and none of them are the database.** People conflate all four. We will not. PostgreSQL is the database. Hibernate generates the SQL. JPA is the annotations and the `EntityManager` contract Hibernate implements. Spring Data JPA generates repository implementations so you write `findByOwnerId(...)` instead of a query. Four layers, four jobs.

## Learning objectives

By the end of this week, you will be able to:

- **Distinguish** JPA (the spec), Hibernate (the provider), Spring Data JPA (the repository abstraction), and PostgreSQL (the database) — four things people regularly collapse into one.
- **Map** a domain record/class to a table with `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, and the right type mappings (`UUID`, `Instant`, `enum`, `BigDecimal`).
- **Model** relationships with `@ManyToOne`, `@OneToMany`, `mappedBy`, and choose the correct `FetchType` and cascade.
- **Explain** the JPA entity lifecycle — transient, managed, detached, removed — and why a dirty check writes to the DB without you calling `save`.
- **Write** Spring Data repositories: derived query methods, `@Query` (JPQL and native), pagination with `Pageable`, and projections.
- **Diagnose and fix** the N+1 select problem using logged SQL, a `JOIN FETCH`, and an `@EntityGraph` — and *prove* the query count dropped.
- **Author** idempotent, forward-only **Flyway** migrations and understand why you never edit a migration that has already run.
- **Run** PostgreSQL locally with **Docker Compose** and wire Spring Boot's datasource to it.
- **Write** integration tests against a throwaway Postgres container with **Testcontainers**, so tests hit real SQL, not an in-memory imposter.

## Prerequisites

This week assumes you have completed **C3 weeks 1–4** or have equivalent experience. Specifically:

- You can scaffold and run a Spring Boot 3 app with Maven (`./mvnw spring-boot:run`).
- You understand controllers, `@Service`, `@Repository`, DTOs, and constructor injection.
- You write Java 21: records, `Optional`, the streams API, sealed types.
- You have a passing JUnit 5 + AssertJ suite and know what a `@SpringBootTest` slice is.
- **Docker Desktop (or Colima / Rancher Desktop / Podman) is installed and running.** Verify with `docker run --rm hello-world`. Testcontainers and the Compose file both need a working container runtime; if Docker isn't running, half of this week stalls. Fix that first.

You do **not** need prior SQL fluency beyond `SELECT`, `INSERT`, `JOIN`. We build the schema with Flyway and let Hibernate generate most queries; the SQL you read this week, you'll be able to read by Friday.

## Topics covered

- The four-layer stack: PostgreSQL → JDBC → Hibernate (JPA provider) → Spring Data JPA.
- `spring-boot-starter-data-jpa`, the PostgreSQL driver, and the autoconfigured `DataSource` / `EntityManagerFactory` / `JpaTransactionManager`.
- Entity mapping: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated`, `@CreationTimestamp`, optimistic locking with `@Version`.
- Why a JPA entity is **not** a DTO, and why we keep mapping the two even though it feels like boilerplate.
- Relationships: `@ManyToOne`, `@OneToMany(mappedBy=...)`, owning vs inverse side, `LAZY` vs `EAGER`, cascade types, orphan removal.
- The entity lifecycle and the persistence context (the "first-level cache"); automatic dirty checking; `flush` timing.
- Spring Data `JpaRepository`: derived queries, `@Query`, `@Modifying`, `Pageable`, `Slice`, and DTO projections.
- The **N+1 select problem**: how it happens, how to see it in logs, and three fixes (`JOIN FETCH`, `@EntityGraph`, batch fetching).
- Transaction boundaries: `@Transactional`, read-only transactions, `LazyInitializationException` and where it comes from.
- **Flyway**: versioned migrations (`V1__...sql`), repeatable migrations, naming, `flyway_schema_history`, and the rule of never editing applied migrations.
- **Docker Compose** for a local Postgres, including a named volume so data survives restarts and `spring-boot-docker-compose` for zero-config local dev.
- **Testcontainers**: `@Testcontainers`, `@ServiceConnection` (Spring Boot 3.1+), and why your integration tests should hit real Postgres.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract.

| Day       | Focus                                              | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|----------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | JPA vs Hibernate vs Spring Data; entity mapping    |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | Relationships, lifecycle, the N+1 trap             |    2h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0h      |     6.5h    |
| Wednesday | Postgres in Docker; Flyway migrations              |    1h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0.5h    |     6h      |
| Thursday  | Repository queries, pagination, Testcontainers     |    1h    |    1h     |     0h     |    0.5h   |   1h     |     2h       |    0.5h    |     6h      |
| Friday    | Swap the in-memory repo; mini-project work         |    0h    |    1h     |     0h     |    0.5h   |   1h     |     3h       |    0.5h    |     6h      |
| Saturday  | Mini-project deep work                             |    0h    |    0h     |     0h     |    0h     |   1h     |     3h       |    0h      |     4h      |
| Sunday    | Quiz, review, polish                               |    0h    |    0h     |     0h     |    1h     |   0h     |     0.5h     |    0h      |     1.5h    |
| **Total** |                                                    | **6h**   | **7.5h**  | **2h**     | **3.5h**  | **6h**   | **8.5h**     | **2h**     | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./00-overview.md) | This overview (you are here) |
| [resources.md](./01-resources.md) | Curated Spring, Hibernate, Postgres, Flyway, and Testcontainers links |
| [lecture-notes/01-jpa-hibernate-and-the-n-plus-1-trap.md](./02-lecture-notes/01-jpa-hibernate-and-the-n-plus-1-trap.md) | The four-layer stack, entity mapping, relationships, the lifecycle, and the N+1 problem |
| [lecture-notes/02-schema-as-code-flyway-postgres-and-queries.md](./02-lecture-notes/02-schema-as-code-flyway-postgres-and-queries.md) | Postgres in Docker, Flyway migrations, repository queries, pagination, Testcontainers |
| [exercises/README.md](./03-exercises/00-overview.md) | Index of the week's drills + checklist |
| [exercises/exercise-01-postgres-and-first-entity.md](./03-exercises/exercise-01-postgres-and-first-entity.md) | Stand up Postgres in Docker, add the starter, map your first entity end to end |
| [exercises/exercise-02-habit-entity.java](./03-exercises/exercise-02-habit-entity.java) | Fill-in-the-TODO JPA entity + Spring Data repository for `Habit` |
| [exercises/exercise-03-V2__habits_and_checkins.sql](./03-exercises/exercise-03-V2__habits_and_checkins.sql) | Author a real, idempotent-by-construction Flyway migration |
| [challenges/README.md](./04-challenges/00-overview.md) | What the challenge is and how it's graded |
| [challenges/challenge-01-hunt-the-n-plus-1.md](./04-challenges/challenge-01-hunt-the-n-plus-1.md) | Diagnose a slow endpoint, fix the N+1, and prove the query count fell |
| [mini-project/README.md](./07-mini-project/00-overview.md) | Full spec: swap Crunch Tracker's in-memory repo for JPA + Postgres + Flyway |
| [quiz.md](./05-quiz.md) | 12 questions with an answer key |
| [homework.md](./06-homework.md) | Six practice problems with a grading rubric |

## The "query count" promise

C3 uses a recurring marker for every persistence task that ends in working code:

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
Hibernate: select ... (1 query, not 1 + N)
```

If the same logical request fires one query per row instead of one query total, you are not done — you have an N+1 bug. We treat a surprise query count the way Week 1 treated a build warning: a defect to fix, not a curiosity to admire. The point of Week 5 is to make a deliberate, *counted* query the normal case.

## Stretch goals

If you finish the regular work early and want to push further:

- Read the official **Spring Data JPA reference**, section "Defining Query Methods": <https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html>.
- Skim **Vlad Mihalcea's** "The best way to map a `@OneToMany` relationship with JPA and Hibernate": <https://vladmihalcea.com/the-best-way-to-map-a-onetomany-relationship-with-jpa-and-hibernate/>. He is the reference voice on Hibernate performance.
- Turn on `spring.jpa.properties.hibernate.generate_statistics=true` and read what Hibernate reports about query counts and cache hits after a test run.
- Read the **Flyway "Concepts"** page and write a `R__` repeatable migration that seeds reference data: <https://documentation.red-gate.com/fd/flyway-concepts-184127471.html>.
- Run `EXPLAIN ANALYZE` against the query behind your slowest endpoint in `psql` and read the plan. You don't need to optimize it yet — just learn to read it.

## Up next

Continue to **Week 6 — API Design and Auth** once you have pushed the mini-project. Next week the API gets users, JWT login, and per-user data ownership — and the entities you map this week gain an `owner` column that every query will filter on.

---

*If you find errors in this material, open an issue or send a PR. Future learners will thank you.*
