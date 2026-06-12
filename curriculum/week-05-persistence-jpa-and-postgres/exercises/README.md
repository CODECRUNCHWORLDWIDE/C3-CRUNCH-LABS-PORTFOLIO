# Week 5 — Exercises

Short, focused drills. Each one should take 30–60 minutes. Do them in order; later ones assume earlier ones. They build directly on the Crunch Tracker codebase you have carried since Week 1 — you are extending the real app, not a throwaway sandbox.

## Index

1. **[Exercise 1 — Postgres in Docker + your first entity](exercise-01-postgres-and-first-entity.md)** — stand up Postgres with Docker Compose, add the JPA + Flyway dependencies, write a `V1` migration, map the `Goal` entity, and prove a round-trip with `psql`. (~50 min)
2. **[Exercise 2 — The Habit entity and repository](exercise-02-habit-entity.java)** — fill in the TODOs in a JPA entity and its Spring Data repository, including a relationship and a derived query. (~45 min)
3. **[Exercise 3 — Author a Flyway migration](exercise-03-V2__habits_and_checkins.sql)** — complete a real `V2` migration that creates the `habits` and `check_ins` tables with the right types, constraints, and indexes. (~30 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. The muscle memory of writing `@ManyToOne(fetch = FetchType.LAZY)` for the tenth time is the point.
- Run it against a **real Postgres**. `docker compose up -d` first; this week nothing runs against H2 or a mock.
- Turn on SQL logging (`org.hibernate.SQL: debug`) and *read the SQL Hibernate generates*. Half the learning this week is in watching the log.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.
- Every exercise must end with `./mvnw test` green **and** the app booting against Postgres with Flyway and Hibernate validation both passing. A failed schema validation is a bug this week.

## Checklist

You can mark the week's exercises complete when:

- [ ] `docker compose up -d` brings up a `healthy` Postgres container.
- [ ] `./mvnw spring-boot:run` boots: Flyway applies your migrations, Hibernate `validate` passes, no errors.
- [ ] `psql` shows your tables (`\dt`) and the `flyway_schema_history` table records each migration as `success = t`.
- [ ] The `Goal` and `Habit` entities map cleanly; `@Enumerated(EnumType.STRING)` everywhere an enum is stored.
- [ ] Your `@ManyToOne` is explicitly `LAZY`; no accidental eager fetch.
- [ ] At least one derived query method (`findBy...`) and one `@Query` exist and are tested.
- [ ] You read the generated SQL for at least one finder and can explain it.
- [ ] `./mvnw test` passes, including at least one Testcontainers integration test.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-05` to compare.
