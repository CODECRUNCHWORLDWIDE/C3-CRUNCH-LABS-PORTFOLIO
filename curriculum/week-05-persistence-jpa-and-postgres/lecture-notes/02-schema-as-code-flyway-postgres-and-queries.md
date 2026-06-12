# Lecture 2 — Schema as Code: Flyway Migrations, Postgres in Docker, and Repository Queries

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can run PostgreSQL locally with Docker Compose, version your schema with forward-only Flyway migrations, write derived and `@Query` repository methods (including pagination and projections), and write integration tests against a real Postgres container with Testcontainers.

If you remember one sentence from this lecture, remember this:

> **Your schema is code. It lives in version control, it changes only by adding a new migration, and you never edit a migration that has already run.** "Works on my machine" becomes "works on every machine" the moment the schema is reproducible from a clean checkout.

---

## 1. Why the schema must be code

In Lecture 1 we set `ddl-auto: validate` and said Flyway owns the schema. Here's why that's not negotiable.

Consider the alternatives. `ddl-auto: create-drop` rebuilds the schema from your entities on every boot and **drops all data on shutdown** — fine for a throwaway demo, catastrophic anywhere else. `ddl-auto: update` tries to *alter* the schema to match your entities — but it only ever *adds*; it never drops a column, never renames, never adds an index you need, and silently does the wrong thing in subtle cases. Neither is reviewable, neither is repeatable, and neither leaves a history of *how* the schema got to its current shape.

A migration tool fixes all of that. Each schema change is a numbered SQL file, committed to Git, code-reviewed like any other change, and applied exactly once, in order, on every environment. The history of your database becomes a readable, auditable sequence of files. That's "schema as code," and **Flyway** is the tool C3 uses.

---

## 2. Postgres in Docker Compose

You need a real Postgres to migrate against. Don't install it globally — run it in a container, scoped to this project, identical for every teammate. Create `compose.yaml` at the repo root:

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: crunch_tracker
      POSTGRES_USER: crunch
      POSTGRES_PASSWORD: crunch
    ports:
      - "5432:5432"
    volumes:
      - crunch_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U crunch -d crunch_tracker"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  crunch_pgdata:
```

Three things worth understanding:

- **The named volume `crunch_pgdata`.** Without it, `docker compose down` deletes your data with the container. With it, data survives `down`/`up` cycles and only vanishes on `docker compose down -v`. For local dev you want it to persist; for a clean reset, `down -v`.
- **The healthcheck.** `pg_isready` reports when Postgres is actually accepting connections, not just when the container started. Other services (and your app) can wait on `condition: service_healthy`.
- **`postgres:16`** — pin a major version. `postgres:latest` will surprise you the day a new major ships with a behavior change.

Bring it up:

```bash
docker compose up -d
docker compose ps          # STATUS should show "healthy"
psql "postgresql://crunch:crunch@localhost:5432/crunch_tracker" -c '\dt'
```

> **Spring Boot Docker Compose support.** Add `spring-boot-docker-compose` (dev scope) and Spring Boot will *start `compose.yaml` for you* when you run the app and wire the datasource automatically — no hardcoded URL needed in dev. Production reads real env vars. It's a genuinely nice developer experience; the manual `application.yml` datasource above is the fallback and what you'll use in CI.

---

## 3. Flyway: the mechanics

Flyway scans `src/main/resources/db/migration` for files named by a strict convention:

```
V1__create_habits.sql
V2__add_check_ins.sql
V3__add_habit_owner.sql
R__seed_reference_data.sql
```

- **`V`** = a *versioned* migration. Runs once, in version order, recorded permanently.
- The number (`1`, `2`, `3`) is the version. Flyway runs them ascending.
- **Two underscores** (`__`) separate the version from the description. One underscore won't parse.
- The description (`create_habits`) is human-readable; underscores become spaces in logs.
- **`R__`** = a *repeatable* migration. Re-runs whenever its checksum changes. Use for views, functions, and idempotent seed data — never for `CREATE TABLE`.

When the app boots (or you run `./mvnw flyway:migrate`), Flyway:

1. Creates a `flyway_schema_history` table if absent.
2. Reads which versions have already run from that table.
3. Applies any new versioned migrations, in order, inside transactions.
4. Records each one — version, description, checksum, timestamp, success — in the history table.

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
-- 1 | create habits  | t
-- 2 | add check ins   | t
```

---

## 4. The cardinal rule: never edit an applied migration

This is the one rule that, broken, ruins everyone's afternoon.

Flyway stores a **checksum** of each migration. On the next run, it re-checksums the files and compares. If `V2__add_check_ins.sql` ran on your machine yesterday, and today you *edit* that file to fix a typo, the checksum no longer matches the recorded one. Flyway halts with:

```
Migration checksum mismatch for migration version 2
-> Applied to database : 1234567890
-> Resolved locally    : 9876543210
```

It refuses to continue because it cannot know whether your database already has the *old* version of that change applied. The history is now inconsistent.

**The rule:** once a migration has run *anywhere a teammate or CI might have seen it*, it is frozen. To change the schema, you **add a new migration** (`V3__rename_column.sql`), never edit an old one. The only safe time to edit `V2` is before it has ever been committed/pushed and before anyone (including CI) has applied it. When in doubt: new file, next number.

> **What about a typo you just wrote and haven't pushed?** Fine to edit — drop your local DB (`docker compose down -v`), edit the file, `up` again, re-migrate from scratch. Once it's pushed, that option is gone.

---

## 5. A real migration

Here is `V1__create_habits.sql` — the schema the `Habit` entity from Lecture 1 expects:

```sql
CREATE TABLE habits (
    id              UUID PRIMARY KEY,
    name            VARCHAR(120) NOT NULL,
    target_per_week INTEGER      NOT NULL CHECK (target_per_week BETWEEN 1 AND 21),
    cadence         VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_habits_cadence ON habits (cadence);
```

And `V2__create_check_ins.sql`:

```sql
CREATE TABLE check_ins (
    id          UUID PRIMARY KEY,
    habit_id    UUID NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    checked_on  DATE NOT NULL,
    note        VARCHAR(280),
    UNIQUE (habit_id, checked_on)            -- one check-in per habit per day
);

CREATE INDEX idx_check_ins_habit ON check_ins (habit_id);
```

Things to notice, because each is a deliberate choice:

- **Column types match the entity.** `uuid` ↔ `UUID`, `timestamptz` ↔ `Instant`, `varchar` ↔ `String`, `bigint` ↔ `long version`. With `ddl-auto: validate`, Hibernate will fail startup if these drift — a useful safety net.
- **`TIMESTAMPTZ`, not `TIMESTAMP`.** Always store timestamps with a time zone. `timestamptz` maps cleanly to `Instant`; `timestamp` (without zone) is a recurring source of off-by-hours bugs.
- **The `REFERENCES ... ON DELETE CASCADE`** mirrors the `orphanRemoval`/cascade on the entity, so deleting a habit removes its check-ins at the database level too — defense in depth.
- **The `CHECK` and `UNIQUE` constraints** push invariants into the database. The app validates too (Week 4 Bean Validation), but the DB is the last line — it'll reject a bad row even if a bug bypasses the service.
- **Indexes on foreign keys.** Postgres does *not* auto-index FK columns. The `idx_check_ins_habit` index is what makes the N+1-fixing `WHERE habit_id IN (...)` queries from Lecture 1 actually fast.

> **"Idempotent by construction."** A `V` migration runs exactly once, so it doesn't need to be re-runnable — Flyway guarantees once-only execution. But authoring them defensively (e.g. `CREATE TABLE IF NOT EXISTS` in repeatable migrations, additive changes that don't assume prior state) keeps you out of trouble. For versioned migrations, the discipline is simpler: each file describes *one forward change*, and you never go back.

---

## 6. Wiring entities, Flyway, and validation together

The healthy boot sequence is:

1. Spring Boot starts. The `DataSource` connects to Postgres.
2. **Flyway runs first** (Spring Boot orders it before Hibernate). It applies any pending `V` migrations. The schema is now correct.
3. **Hibernate validates** (`ddl-auto: validate`). It compares the entity mappings against the now-migrated schema. If a column is missing or a type is wrong, the app **fails to start** with a clear message — better than a runtime error on the first query.
4. The app is up, schema and entities provably in agreement.

If step 3 fails ("missing column `target_per_week`"), the fix is a new migration, not an entity change — unless the entity is genuinely wrong. This two-sided check (Flyway builds it, Hibernate verifies it) is why we trust the schema in production.

---

## 7. Repository queries, deeper

Lecture 1 introduced derived queries and `@Query`. A few more patterns you'll use in the mini-project.

**Projections — fetch only the columns you need.** Selecting whole entities to compute a summary wastes I/O. A projection selects a narrow shape:

```java
public record HabitCount(UUID habitId, String name, long checkInCount) { }

@Query("""
       select new tech.crunch.tracker.habit.HabitCount(h.id, h.name, count(c))
       from Habit h left join h.checkIns c
       group by h.id, h.name
       """)
List<HabitCount> countCheckInsPerHabit();
```

That's a **constructor projection**: one query, one aggregate, no entities loaded, no N+1 possible. When you only need numbers, project — don't fetch graphs.

**Interface projections** (Spring picks the columns from getters):

```java
interface HabitView {
    UUID getId();
    String getName();
}
List<HabitView> findByCadence(Cadence cadence);
```

**Modifying queries.** Bulk updates skip the entity lifecycle and run as one SQL statement:

```java
@Modifying
@Query("update Habit h set h.targetPerWeek = :n where h.cadence = :c")
int bumpTargets(@Param("c") Cadence c, @Param("n") int n);
```

`@Modifying` is required for `UPDATE`/`DELETE` JPQL, and the call must be inside a `@Transactional`. Note bulk updates *bypass dirty checking and the persistence context* — managed entities in memory won't reflect the change. Use them for true bulk operations, not single-row edits.

**Pagination, again.** Every list endpoint takes a `Pageable`:

```java
Page<Habit> findByCadence(Cadence cadence, Pageable pageable);
```

Spring Boot binds `?page=0&size=20&sort=name,asc` from the request straight into a `Pageable` parameter on a controller method — no manual parsing.

---

## 8. Transactions, briefly

Spring's `@Transactional` defines a transaction boundary around a method. Defaults worth knowing:

- A method annotated `@Transactional` runs in a transaction; it commits on normal return and **rolls back on a runtime exception** (not on checked exceptions, by default — a historical quirk).
- `@Transactional(readOnly = true)` hints the driver and Hibernate that no writes happen; Hibernate skips dirty-checking flushes, a real performance win on read endpoints. Put it on every query-only service method.
- Transactions are **proxy-based**: calling a `@Transactional` method *from within the same class* bypasses the proxy and the annotation does nothing. Call across beans.
- Keep transactions short. A transaction holds a database connection from your pool the whole time. Don't do HTTP calls or sleep inside one.

The service pattern for the week: **service methods are `@Transactional`, they load and fetch everything needed (no lazy surprises), map entities to DTOs, and return DTOs.** The transaction closes when the method returns; the controller only ever touches DTOs. This is what `open-in-view: false` enforces.

---

## 9. Testcontainers: integration tests against real Postgres

Unit tests with mocks prove your *logic*. They prove nothing about your *SQL*. A derived query that compiles can still generate wrong SQL; a migration can be subtly incompatible with an entity; a `JOIN FETCH` can return duplicates. The only way to catch these is to run against **a real Postgres** — and Testcontainers makes that a throwaway container per test run.

```java
@SpringBootTest
@Testcontainers
class HabitRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired HabitRepository habitRepository;

    @Test
    void saves_and_finds_by_cadence() {
        habitRepository.save(new Habit("Read", 5, Cadence.DAILY));

        List<Habit> daily = habitRepository.findByCadence(Cadence.DAILY);

        assertThat(daily).extracting(Habit::getName).containsExactly("Read");
    }
}
```

The magic line is **`@ServiceConnection`** (Spring Boot 3.1+). It takes the running `PostgreSQLContainer` and *auto-configures Spring's datasource* to point at it — no `@DynamicPropertySource`, no hardcoded JDBC URL. Flyway runs against the container on startup, building the exact schema your migrations describe. Your test hits real SQL, real constraints, real `JOIN FETCH` behavior.

What this catches that mocks never will:

- A `UNIQUE (habit_id, checked_on)` violation when you double-check-in.
- A migration that builds a `varchar(20)` while the entity sends a 30-char enum name.
- An N+1 — assert the query count here (Lecture 1, section 12) against real Hibernate statistics.
- A `JOIN FETCH` returning duplicate parents without `distinct`.

> **Why not H2?** H2 in "Postgres compatibility mode" is a tempting shortcut — fast, in-process, no Docker. It also *lies*. Its SQL dialect, its constraint handling, its `JSON`/`uuid`/`timestamptz` behavior, and its query planner all differ from real Postgres. Tests pass on H2 and fail in production. C3's rule: **integration tests run on the same database engine production uses.** That's Testcontainers, and you already required Docker in the prerequisites.

A faster pattern for many tests: reuse one container across the whole suite (a `static` field, as above, is shared) and let each test clean up its own rows or run in a rolled-back transaction (`@Transactional` on the test rolls back after each method).

---

## 10. The full local loop

Putting the week together, here's the developer loop a teammate runs on a fresh clone:

```bash
git clone <repo> && cd crunch-tracker
docker compose up -d                      # Postgres, healthy
./mvnw spring-boot:run                     # Flyway migrates, Hibernate validates, app up
# ... in another terminal ...
curl localhost:8080/api/habits             # real data, real persistence
./mvnw test                                # unit tests + Testcontainers integration tests, green
docker compose down                        # data persists in the named volume
```

Every step is reproducible. Nobody installs Postgres by hand. Nobody applies schema by hand. The schema is a folder of SQL files; the data store is a container; the tests prove the SQL against the same engine. That's the whole point of "schema as code," and it's the baseline every later week builds on.

---

## 11. Recap

You should now be able to:

- Run PostgreSQL locally with Docker Compose, including a persistent named volume and a healthcheck.
- Explain why `ddl-auto: validate` + Flyway beats `ddl-auto: update` in every environment that matters.
- Author versioned Flyway migrations with the correct naming, and explain why you never edit an applied one.
- Match Postgres column types to entity field types and let Hibernate validate the agreement.
- Write repository projections, modifying queries, and paginated finders.
- Set transaction boundaries correctly with `@Transactional(readOnly = ...)` and `open-in-view: false`.
- Write Testcontainers integration tests with `@ServiceConnection`, and explain why H2 isn't an acceptable substitute.

Now do the exercises — three drills that take you from "Postgres is running" to "my entity, my migration, my repository, all green against a real database."

---

## References

- *Flyway — how it works*: <https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html>
- *Spring Boot — Docker Compose support*: <https://docs.spring.io/spring-boot/reference/features/dev-services.html>
- *Spring Boot — Testcontainers*: <https://docs.spring.io/spring-boot/reference/testing/testcontainers.html>
- *Testcontainers Postgres module*: <https://java.testcontainers.org/modules/databases/postgres/>
- *PostgreSQL 16 — data types*: <https://www.postgresql.org/docs/16/datatype.html>
- *Spring Data JPA — query methods*: <https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html>
- *Spring — `@Transactional`*: <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html>
