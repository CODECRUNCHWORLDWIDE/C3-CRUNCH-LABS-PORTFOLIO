# Exercise 1 — Postgres in Docker + Your First Entity

**Goal:** Take the Crunch Tracker Spring Boot app from Week 4 (in-memory) and give it a real database for the `Goal` aggregate: Postgres in a container, the JPA + Flyway dependencies, a `V1` migration, a mapped `@Entity`, and a Spring Data repository — proven with a round-trip you can see in `psql` and in the SQL logs.

**Estimated time:** 50 minutes.

---

## Setup

You need:

- The Crunch Tracker repo from Week 4 (your own, with the `GoalController`, DTOs, and the `GoalRepository` *interface* + in-memory impl).
- A working container runtime. Verify:

```bash
docker run --rm hello-world
```

If that fails, fix Docker before continuing — every step below needs it.

---

## Step 1 — Stand up Postgres

Create `compose.yaml` at the repo root:

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

Bring it up and confirm it's healthy:

```bash
docker compose up -d
docker compose ps          # STATUS column should read "healthy" within ~10s
```

Connect with `psql` to prove the DB exists (it'll be empty):

```bash
psql "postgresql://crunch:crunch@localhost:5432/crunch_tracker" -c '\dt'
# Output: "Did not find any relations." — correct, no tables yet.
```

---

## Step 2 — Add the dependencies

In `pom.xml`, add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Configure `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crunch_tracker
    username: crunch
    password: crunch
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
    open-in-view: false
  flyway:
    enabled: true
logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.bind: trace
```

---

## Step 3 — Write the `V1` migration

Create `src/main/resources/db/migration/V1__create_goals.sql`:

```sql
CREATE TABLE goals (
    id          UUID PRIMARY KEY,
    title       VARCHAR(120) NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    target_date DATE         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_goals_status ON goals (status);
```

---

## Step 4 — Map the `Goal` entity

Create the entity. The `status` is an enum (`GoalStatus { ACTIVE, ACHIEVED, ABANDONED }` — reuse your Week 2 enum).

```java
package tech.crunch.tracker.goal;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Goal() { }

    public Goal(String title, GoalStatus status, LocalDate targetDate) {
        this.title = title;
        this.status = status;
        this.targetDate = targetDate;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }
    public LocalDate getTargetDate() { return targetDate; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
```

---

## Step 5 — Replace the repository

Delete your hand-written in-memory `GoalRepository` implementation. Change the interface to extend `JpaRepository`:

```java
package tech.crunch.tracker.goal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByStatus(GoalStatus status);
}
```

Your `GoalService` already calls `save`, `findById`, `findAll`, `deleteById` — `JpaRepository` provides all of them. The controller and DTOs do **not** change. That's the seam paying off.

---

## Step 6 — Boot it and watch the magic

```bash
./mvnw spring-boot:run
```

In the startup logs you should see, in order:

1. Flyway connect, then `Migrating schema "public" to version "1 - create goals"`, then `Successfully applied 1 migration`.
2. Hibernate validation pass (no "missing column" / "wrong type" errors).
3. Tomcat start on 8080.

If Hibernate complains the schema doesn't match the entity, your migration and entity disagree — reconcile them (the migration is the source of truth for the DB; fix whichever is wrong).

---

## Step 7 — Round-trip a goal and watch the SQL

In another terminal, create a goal through the API:

```bash
curl -s -X POST localhost:8080/api/goals \
  -H 'Content-Type: application/json' \
  -d '{"title":"Ship the capstone","targetDate":"2026-09-01"}'
```

In the app logs you should see Hibernate's `insert into goals (...) values (...)`. Now read it back:

```bash
curl -s localhost:8080/api/goals | jq
```

And confirm directly in the database:

```bash
psql "postgresql://crunch:crunch@localhost:5432/crunch_tracker" \
  -c 'SELECT id, title, status, version FROM goals;'
psql "postgresql://crunch:crunch@localhost:5432/crunch_tracker" \
  -c 'SELECT version, description, success FROM flyway_schema_history;'
```

You should see your row in `goals` and `1 | create goals | t` in the history table.

---

## Step 8 — Prove persistence survives a restart

Stop the app (`Ctrl+C`). Do **not** run `docker compose down`. Restart:

```bash
./mvnw spring-boot:run
curl -s localhost:8080/api/goals | jq
```

The goal is still there. In-memory died; real data lives. That's the whole week in one observation.

---

## Acceptance criteria

You can mark this exercise done when:

- [ ] `docker compose ps` shows the Postgres container `healthy`.
- [ ] `./mvnw spring-boot:run` boots with Flyway applying `V1` and Hibernate `validate` passing.
- [ ] `POST /api/goals` then `GET /api/goals` round-trips through Postgres.
- [ ] `psql` shows the row in `goals` and a `success = t` row in `flyway_schema_history`.
- [ ] Data survives an app restart (without `docker compose down`).
- [ ] You can point to the `insert into goals ...` line in the SQL logs.

---

## Stretch

- Add `findByStatusAndTargetDateBefore(GoalStatus status, LocalDate date)` and an "overdue active goals" endpoint. Test it.
- Add `spring-boot-docker-compose` (dev scope) and remove the hardcoded datasource URL from dev config. Confirm `./mvnw spring-boot:run` now starts the container for you.
- Add a `@Version`-driven `409 Conflict`: edit the same goal from two requests with stale versions and watch the `OptimisticLockException`.

---

## Hints

<details>
<summary>If Flyway says "found non-empty schema without history table"</summary>

You have tables Hibernate created earlier (a leftover `ddl-auto: update` run). Reset cleanly: `docker compose down -v && docker compose up -d`, then re-run. Going forward, `ddl-auto: validate` means Hibernate never creates tables.

</details>

<details>
<summary>If Hibernate fails validation on the enum column</summary>

Check the `varchar` length is at least as long as your longest enum name and that you used `@Enumerated(EnumType.STRING)`. An `ORDINAL` enum maps to an integer column, not a varchar — mismatch.

</details>

<details>
<summary>If the app can't connect to Postgres</summary>

Confirm the container is up (`docker compose ps`), the port mapping is `5432:5432`, and nothing else is squatting on 5432 (`lsof -i :5432`). The healthcheck must read `healthy`, not just `running`.

</details>

---

When this exercise feels comfortable, move to [Exercise 2 — The Habit entity and repository](./exercise-02-habit-entity.java).
