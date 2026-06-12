# Week 5 Homework

Six practice problems that revisit the week's topics. The full set should take about **6 hours**. Work in your Crunch Tracker repository so each problem produces at least one commit you can point to later. `docker compose up -d` must be running for all of them.

Each problem includes a **problem statement**, **acceptance criteria**, a **hint**, and an **estimated time**.

A grading rubric is at the bottom — read it before you start so you know what "great" looks like.

---

## Problem 1 — Read the generated SQL

**Problem statement.** Turn on `org.hibernate.SQL: debug` and `org.hibernate.orm.jdbc.bind: trace`. Through your API, perform: create a habit, list habits, fetch one habit by id, and delete it. Capture the SQL Hibernate logs for each operation into `notes/week-05-sql.md`. For each of the four operations, paste the SQL and write one sentence explaining it (which table, which columns, which bound parameters).

Then answer in one sentence: *when you called the "rename habit" service method that only did `setName` inside a `@Transactional`, what SQL fired and why — given you never called `save`?*

**Acceptance criteria.**

- `notes/week-05-sql.md` exists with four logged operations, each annotated.
- The dirty-checking answer is correct (an `UPDATE` fires at flush via automatic dirty checking).
- Committed.

**Hint.** The `insert into habits (...)`, `select ... from habits`, and `delete from habits where id=?` lines are what you're looking for. Bound parameter values appear on the `binding parameter` trace lines.

**Estimated time.** 30 minutes.

---

## Problem 2 — A second migration, the right way

**Problem statement.** Add a `description` column (nullable `VARCHAR(500)`) to the `habits` table. Do it as a **new** migration `V3__add_habit_description.sql` — do **not** edit `V2`. Add the field to the `Habit` entity and surface it in the create/response DTOs. Boot the app and confirm Flyway applies V3 and Hibernate validation passes.

**Acceptance criteria.**

- `V3__add_habit_description.sql` exists; `V2` is unchanged (prove it with `git log --follow -p` showing no edit to V2).
- `flyway_schema_history` shows version 3, `success = t`.
- The entity, the DTOs, and the schema agree (validation passes).
- A test posts a habit with a description and reads it back.
- Committed.

**Hint.** The migration is one line: `ALTER TABLE habits ADD COLUMN description VARCHAR(500);`. Map it with `@Column(length = 500)` (nullable, so no `nullable = false`).

**Estimated time.** 45 minutes.

---

## Problem 3 — Derived query vs `@Query`

**Problem statement.** Implement the same finder two ways and test both: "all habits with a target of at least N per week, ordered by name."

1. A **derived** query: `findByTargetPerWeekGreaterThanEqualOrderByNameAsc(int min)`.
2. A **`@Query`** (JPQL): `@Query("select h from Habit h where h.targetPerWeek >= :min order by h.name")`.

Write a Testcontainers test that seeds four habits and asserts both methods return identical results.

**Acceptance criteria.**

- Both methods exist and return the same rows for the same input.
- A passing integration test against real Postgres covers both.
- You can explain in a code comment which you'd ship and why (readability vs. method-name length).
- Committed.

**Hint.** Derived queries are great until the method name gets unreadable; past about three conditions, switch to `@Query`. Both compile to the same SQL here.

**Estimated time.** 45 minutes.

---

## Problem 4 — Prove an N+1, then kill it

**Problem statement.** Add an endpoint `GET /api/habits/with-counts` that returns each habit's name and check-in count. Implement it *naively* first (`findAll()` + `getCheckIns().size()`). Write a query-count test (Hibernate `Statistics`) and record the count for a 10-habit dataset. Then fix it with a constructor projection (`count(c)` in SQL) and re-run the test, asserting the count dropped to 1.

**Acceptance criteria.**

- A `FINDINGS.md` (or test comment) records the before count (11) and after count (1).
- The fixed version uses a constructor projection or `JOIN FETCH`/`@EntityGraph`.
- A query-count test asserts `getPrepareStatementCount() == 1` after the fix.
- The JSON response is unchanged between the two implementations (habits with zero check-ins still appear with count 0 — use a `left join`).
- Committed.

**Hint.** `@Query("select new ...HabitCount(h.id, h.name, count(c)) from Habit h left join h.checkIns c group by h.id, h.name")`. The `left join` is what keeps zero-check-in habits in the result.

**Estimated time.** 1 hour.

---

## Problem 5 — Optimistic locking returns 409

**Problem statement.** Demonstrate optimistic locking end to end. Load a habit (note its `version`). Simulate a concurrent update by updating the same row from a second transaction so its version increments. Then attempt to save your stale copy and confirm an `OptimisticLockException` (Spring wraps it as `ObjectOptimisticLockingFailureException`). Add an `@ExceptionHandler` that maps it to a `409 Conflict` ProblemDetail, and write an integration test that asserts the 409.

**Acceptance criteria.**

- The `Habit` entity has `@Version` (it should already).
- A test reproduces the stale-version update and asserts the 409 with a ProblemDetail body.
- The handler is in a `@RestControllerAdvice` and returns RFC-9457 ProblemDetail (consistent with Week 4).
- Committed.

**Hint.** In the test, use two separate transactions (or `TestEntityManager` / a second `save` with a stale detached copy) to bump the version, then save the stale one and assert the exception. Map it: `@ExceptionHandler(ObjectOptimisticLockingFailureException.class)` returning `ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ...)`.

**Estimated time.** 1 hour 15 minutes.

---

## Problem 6 — Reflection + ERD

**Problem statement.** Write a 300–400 word reflection at `notes/week-05-reflection.md` answering:

1. Before this week, what did you *think* "saving an object to the database" did? What's the more accurate picture now (entity lifecycle, dirty checking, the persistence context)?
2. Which was hardest: the entity mapping, Flyway discipline, or the N+1 hunt? Why?
3. In one paragraph, explain to a teammate why you'd never set `ddl-auto: update` in a real project.
4. What's one thing about persistence you want to learn next that this week didn't cover (connection pooling? second-level cache? read replicas?).

Also include a tiny ASCII or Mermaid ERD of the `goals` / `habits` / `check_ins` schema.

**Acceptance criteria.**

- File exists, 300–400 words, each numbered question in its own paragraph.
- Includes a small ERD (ASCII or Mermaid).
- Committed.

**Hint.** This is for *you*. Be honest about what clicked and what didn't. Future-you, debugging a `LazyInitializationException` in Week 9, will be glad you wrote down the lifecycle model.

**Estimated time.** 30 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 30 min |
| 2 | 45 min |
| 3 | 45 min |
| 4 | 1 h 0 min |
| 5 | 1 h 15 min |
| 6 | 30 min |
| **Total** | **~4 h 45 min** |

(The remaining budget is buffer — N+1 hunting and optimistic-lock tests always take longer than you expect the first time.)

---

## Grading rubric

Each problem is scored out of the weight below. Total 100.

| Problem | Weight | "Full marks" means |
|--------:|-------:|--------------------|
| P1 — Read the SQL | 15 | Four operations logged and correctly explained; the dirty-checking answer is right |
| P2 — Second migration | 20 | New `V3`, `V2` untouched, validation passes, round-trip test; demonstrates the never-edit rule |
| P3 — Derived vs `@Query` | 15 | Both finders return identical rows, tested on real Postgres, with a reasoned choice documented |
| P4 — Kill the N+1 | 25 | Before/after counts recorded, fix is correct, query-count test passes, JSON unchanged incl. zero-check-in habits |
| P5 — Optimistic lock 409 | 15 | Reproduces the conflict, maps to a ProblemDetail 409, integration test proves it |
| P6 — Reflection + ERD | 10 | Thoughtful answers, correct ERD, all four questions addressed |

**Cross-cutting deductions (apply to any problem):**

- **−10** if any work used H2 or a mock instead of Testcontainers/real Postgres where an integration test was required.
- **−10** if `ddl-auto` is anything but `validate`, or if any applied migration was edited.
- **−5** if a `@ManyToOne` is left `EAGER`, or an enum column is `ORDINAL`.
- **−5** if `./mvnw test` is red on a fresh clone with `docker compose up -d`.

When you've finished all six, push your repo and start the [mini-project](./mini-project/README.md).
