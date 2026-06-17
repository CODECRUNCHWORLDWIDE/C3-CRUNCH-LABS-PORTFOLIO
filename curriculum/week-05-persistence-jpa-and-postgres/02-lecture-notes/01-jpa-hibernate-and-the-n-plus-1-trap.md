# Lecture 1 — Spring Data JPA and Hibernate: Entities, Relationships, and the N+1 Trap

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can describe the four-layer persistence stack without confusing the layers, map a domain type to a table, model `@ManyToOne` / `@OneToMany` relationships with the right fetch strategy, explain the entity lifecycle, and recognise — and fix — an N+1 query from logged SQL.

If you remember one sentence from this lecture, remember this:

> **JPA is a specification. Hibernate is the implementation. Spring Data JPA is the convenience layer. PostgreSQL is the database.** They ship together in one starter, they get conflated constantly, and they are four different things doing four different jobs.

---

## 1. The four things people call "the ORM"

Add `spring-boot-starter-data-jpa` plus the Postgres driver to a project and you have just pulled in four distinct layers. Ask a room of juniors "what saves my object to the database?" and you'll get four answers, all partly right, because they're naming different floors of the same building.

| Layer | Example artifact | What it actually is | Who wrote it |
|-------|------------------|---------------------|--------------|
| Database | **PostgreSQL 16** | The actual data store. Speaks SQL over a wire protocol. | The PostgreSQL Global Development Group |
| JPA (the spec) | `jakarta.persistence.*` | The *specification*: `@Entity`, `@Id`, `EntityManager`. Annotations and contracts, no behavior. | Jakarta EE (Eclipse Foundation) |
| JPA provider | **Hibernate ORM 6** | The *implementation* of the spec. Turns your annotated objects into SQL and back. | Red Hat / the Hibernate team |
| Repository layer | **Spring Data JPA** | Generates repository *implementations* from interfaces you declare. | The Spring team |

When you write `habitRepository.save(habit)`:

1. **Spring Data JPA** provides the `save` method (you never wrote it) and delegates to the JPA `EntityManager`.
2. **Hibernate** (the JPA provider) decides this is an `INSERT`, builds the SQL, manages the persistence context.
3. **The JDBC driver** (`org.postgresql.Driver`) ships that SQL over TCP to the database.
4. **PostgreSQL** executes the `INSERT` and writes a row.

Four layers, one method call. You can swap any layer in principle — EclipseLink instead of Hibernate, MySQL instead of Postgres — but in 2026 the default Spring Boot 3 stack is **Hibernate 6 over PostgreSQL**, and that's what every example here uses.

> **Why this matters.** When something goes wrong, the fix lives at a specific layer. A slow query is a Hibernate/SQL problem. A `findByOwnerId` that won't compile is a Spring Data problem. A "column does not exist" is a schema (Flyway/Postgres) problem. A `LazyInitializationException` is a JPA lifecycle problem. If you can't name the layer, you can't fix the bug.

---

## 2. Where we left Crunch Tracker

Recall Week 3 and 4. The domain is behind interfaces:

```java
public interface HabitRepository {
    Habit save(Habit habit);
    Optional<Habit> findById(UUID id);
    List<Habit> findAll();
    void deleteById(UUID id);
}
```

In Week 3 you wrote an in-memory implementation backed by a `ConcurrentHashMap`. In Week 4 your controllers called the service, the service called this interface, and everything worked — until the JVM stopped and all data evaporated.

**That interface is the seam.** This week we write a JPA-backed implementation and slot it behind the same contract. The controllers don't change. The DTOs don't change. The API contract from Week 4 holds. That's not an accident — it's the payoff of "introduce a repository interface so persistence can slot in next week," which you did deliberately in Week 3.

In practice, Spring Data JPA hands you the implementation for free. You'll *delete* your hand-written `HashMap` class and replace the interface with one that extends `JpaRepository<Habit, UUID>`. More on that in section 8.

---

## 3. Adding the starter

In your `pom.xml`:

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

The starter is a transitive bundle: it brings in Hibernate ORM, the JPA API, Spring Data JPA, Spring's JDBC support, and the transaction manager. The Postgres driver is `runtime` scope because your code never imports `org.postgresql.*` directly — Hibernate loads the driver by name.

Then point the datasource at Postgres in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crunch_tracker
    username: crunch
    password: crunch
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway owns the schema; Hibernate only checks it
    properties:
      hibernate:
        format_sql: true
    open-in-view: false           # see section 11 — turn this OFF
  flyway:
    enabled: true
logging:
  level:
    org.hibernate.SQL: debug      # log the SQL Hibernate generates
    org.hibernate.orm.jdbc.bind: trace   # log the bound parameter values
```

Two settings deserve their own callout right now.

**`ddl-auto: validate`** — Hibernate *can* generate your schema (`ddl-auto: update` or `create`). **We never let it in any environment that matters.** Flyway owns DDL. Hibernate's job is to *validate* that the entities and the Flyway-built schema agree, and to fail fast at startup if they drift. `update` is a footgun that silently alters production tables; you will never see it in a C3 mini-project.

**`open-in-view: false`** — Spring Boot defaults this to `true`, which keeps a database session open for the entire HTTP request. It hides `LazyInitializationException` by accident and encourages lazy-loading inside controllers. Turn it off so lazy mistakes surface in development, not production. We explain the mechanism in section 11.

---

## 4. Your first entity

Here is the `Habit` domain type as a JPA entity. Compare it to the Week 3 record — entities have constraints records don't.

```java
package tech.crunch.tracker.habit;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "habits")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "target_per_week", nullable = false)
    private int targetPerWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Cadence cadence;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;            // optimistic locking — see section 10

    protected Habit() { }            // JPA requires a no-arg constructor

    public Habit(String name, int targetPerWeek, Cadence cadence) {
        this.name = name;
        this.targetPerWeek = targetPerWeek;
        this.cadence = cadence;
    }

    // getters; setters only for the fields the app legitimately mutates
    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTargetPerWeek() { return targetPerWeek; }
    public void setTargetPerWeek(int targetPerWeek) { this.targetPerWeek = targetPerWeek; }
    public Cadence getCadence() { return cadence; }
    public Instant getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
```

Annotation by annotation:

- **`@Entity`** — this class is mapped to a table. Without it, Hibernate ignores the class entirely.
- **`@Table(name = "habits")`** — the table name. Without it Hibernate derives one from the class name; we are explicit because the entity name (`Habit`) and the table name (`habits`, plural, snake) follow different conventions.
- **`@Id`** + **`@GeneratedValue(strategy = GenerationType.UUID)`** — a UUID primary key generated by Hibernate. We prefer UUIDs over auto-increment `long`s: they're allocatable client-side, they don't leak row counts, and they don't collide across services. (Postgres stores them efficiently as the native `uuid` type.)
- **`@Column(nullable = false, length = 120)`** — maps to a `NOT NULL VARCHAR(120)`. Note: this annotation *documents intent* and feeds `ddl-auto`/validation, but **the actual constraint is created by Flyway**, not by this annotation, because we set `ddl-auto: validate`. Keep them in sync.
- **`@Enumerated(EnumType.STRING)`** — store the enum as its name (`"DAILY"`), not its ordinal (`0`). **Always `STRING`.** `ORDINAL` is a time bomb: reorder the enum constants and every stored row silently changes meaning.
- **`@CreationTimestamp`** — Hibernate sets this on insert. `updatable = false` means it's never overwritten.
- **`@Version`** — the optimistic-lock counter. Hibernate increments it on every update and checks it in the `WHERE` clause. Section 10.

> **Why is this not a record?** JPA entities can't be records. The spec requires a no-arg constructor and mutable fields so Hibernate can instantiate an empty instance and populate it via reflection (or, in Hibernate 6, via bytecode). Records are final and have no no-arg constructor. So entities are plain classes — and that's *fine*, because **an entity is not your API model.** The DTOs from Week 4 stay records; the entities are mutable persistence plumbing. We map between them. (Section 5.)

---

## 5. The entity is not the DTO (and that's deliberate)

It is tempting to annotate your Week 4 DTO records with `@Entity` and skip a layer. Don't. They have genuinely different jobs:

| | DTO (`HabitResponse`) | Entity (`Habit`) |
|--|----------------------|------------------|
| Shape | Exactly what the API exposes | Exactly what the table stores |
| Type | `record` (immutable) | `class` (mutable, Hibernate-managed) |
| Has | Only fields the client should see | DB columns, `@Version`, lazy associations |
| Changes when | The API contract changes | The schema changes |

Coupling them means a schema change breaks your API contract and vice versa. It also leaks JPA concerns (lazy proxies, the `version` field) straight to clients, and it invites the N+1 disaster of section 12 when Jackson serializes a lazy association during JSON rendering. Keep the mapping. It's "boilerplate" that buys you a clean seam between storage and contract — the same seam principle as the repository interface.

A trivial mapper:

```java
static HabitResponse toResponse(Habit h) {
    return new HabitResponse(h.getId(), h.getName(), h.getTargetPerWeek(),
                             h.getCadence(), h.getCreatedAt());
}
```

---

## 6. Relationships: `@ManyToOne` and `@OneToMany`

Crunch Tracker has check-ins: each time a user performs a habit, they record a `CheckIn`. One habit has many check-ins; each check-in belongs to one habit. That's a bidirectional one-to-many.

```java
@Entity
@Table(name = "check_ins")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;            // the OWNING side — has the FK column

    @Column(name = "checked_on", nullable = false)
    private LocalDate checkedOn;

    @Column(length = 280)
    private String note;

    protected CheckIn() { }
    // ... constructor + getters
}
```

And the inverse side on `Habit`:

```java
@OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
private List<CheckIn> checkIns = new ArrayList<>();
```

Three concepts you must get straight:

**Owning side vs inverse side.** The `@ManyToOne` side (`CheckIn.habit`) is the *owning* side — it has the actual foreign-key column (`habit_id`). The `@OneToMany(mappedBy = "habit")` side is the *inverse* — `mappedBy` tells Hibernate "the FK lives on the `habit` field of the other entity; don't create a join table." Forget `mappedBy` and Hibernate assumes a join table and creates a third table you didn't want.

**Cascade.** `CascadeType.ALL` means operations on the `Habit` propagate to its `checkIns`: persist the habit, its check-ins persist too; delete the habit, they delete too. `orphanRemoval = true` means removing a check-in from the list *deletes the row*. Use these only on true parent-child ownership, which a habit→check-ins relationship is.

**Fetch type — the one with teeth.** `@ManyToOne` defaults to `EAGER`; `@OneToMany` defaults to `LAZY`. **Override the `@ManyToOne` to `LAZY` explicitly,** as above. Eager many-to-ones are the most common cause of accidental N+1 (section 12). The rule: **everything lazy by default; fetch what you need, when you need it, explicitly.**

---

## 7. The entity lifecycle and the persistence context

This is the model that explains the surprising behaviors — why `setName` writes to the DB without a `save`, and where `LazyInitializationException` comes from.

Every entity is in one of four states relative to a **persistence context** (Hibernate's per-transaction cache of managed entities, also called the first-level cache):

```
  new Habit(...)            transient   — not associated with any context, no DB row
       │ persist / save
       ▼
   ┌────────────┐  load / find / save
   │  MANAGED   │ ◄──────────────────── tracked by the persistence context;
   └────────────┘                       changes are auto-flushed (dirty checking)
       │ tx commits / em.detach / context closes
       ▼
    detached                  — has a DB row, but no longer tracked
       │ remove
       ▼
    removed                   — scheduled for DELETE at flush
```

Two consequences that trip everyone up:

**Automatic dirty checking.** While an entity is *managed* (loaded inside an open transaction), Hibernate snapshots its state. At flush time — usually transaction commit — it compares the current state to the snapshot and issues an `UPDATE` for anything that changed. You do **not** call `save`:

```java
@Transactional
public void rename(UUID id, String newName) {
    Habit h = habitRepository.findById(id).orElseThrow();
    h.setName(newName);              // no save() call
    // on method return, the @Transactional commits, Hibernate flushes,
    // and an UPDATE habits SET name=? fires automatically.
}
```

This is correct and intended. It also means an accidental setter inside a transaction is an accidental write. Be deliberate.

**`LazyInitializationException`.** A lazy association is a *proxy*. It only loads when you access it *while the persistence context is open*. Access it after the transaction closed (e.g. in a controller, after the service returned, when `open-in-view` is `false`) and Hibernate throws `LazyInitializationException: could not initialize proxy — no Session`. The fix is not to make everything eager — it's to **fetch what you need inside the transaction**, with a `JOIN FETCH` or an `@EntityGraph` (sections 8 and 12), and map to a DTO before the transaction closes.

---

## 8. Spring Data repositories

Now the convenience layer. Replace your hand-written repository with an interface:

```java
public interface HabitRepository extends JpaRepository<Habit, UUID> {

    // Derived query: Spring parses the method name into a query.
    List<Habit> findByCadence(Cadence cadence);

    // Derived with sorting.
    List<Habit> findByTargetPerWeekGreaterThanEqualOrderByNameAsc(int min);

    boolean existsByName(String name);
}
```

You write **zero implementation**. `JpaRepository` gives you `save`, `findById`, `findAll`, `deleteById`, `count`, `existsById`, paged `findAll(Pageable)`, and more, for free. The derived methods (`findByCadence`) are parsed from the method name at startup into JPQL. Spring fails fast at boot if a method name references a property that doesn't exist — a misspelled `findByCadance` won't compile your context.

When the method name gets unreadable, write the query yourself:

```java
@Query("select h from Habit h where h.cadence = :cadence and h.targetPerWeek >= :min")
List<Habit> findActive(@Param("cadence") Cadence cadence, @Param("min") int min);
```

That's **JPQL** — Java Persistence Query Language. It looks like SQL but operates on *entities and fields* (`Habit h`, `h.cadence`), not tables and columns. Hibernate translates it to SQL. For genuinely Postgres-specific SQL, use `@Query(value = "...", nativeQuery = true)` — but reach for that rarely.

To fetch a `Habit` *with* its check-ins in one query (the N+1 fix), use a fetch join:

```java
@Query("select h from Habit h join fetch h.checkIns where h.id = :id")
Optional<Habit> findByIdWithCheckIns(@Param("id") UUID id);
```

Or, more declaratively, an entity graph:

```java
@EntityGraph(attributePaths = "checkIns")
Optional<Habit> findWithCheckInsById(UUID id);
```

---

## 9. Pagination — never `findAll()` on a real table

`findAll()` returns *every row*. On a table with a million rows, that's a million objects in heap and a multi-second query. Real list endpoints paginate:

```java
Page<Habit> page = habitRepository.findAll(
        PageRequest.of(0, 20, Sort.by("name").ascending()));

List<Habit> rows   = page.getContent();   // up to 20 habits
long totalElements = page.getTotalElements();
int  totalPages    = page.getTotalPages();
```

Spring Data turns a `Pageable` into `LIMIT 20 OFFSET 0 ORDER BY name` plus a `count(*)` query for the total. If you don't need the total (infinite scroll), return a `Slice` instead and skip the count query. Your Week 4 list endpoint becomes a `Page` endpoint this week.

---

## 10. Optimistic locking with `@Version`

Two users edit the same habit at the same time. Without protection, the second write silently clobbers the first ("lost update"). The `@Version` field prevents it:

```java
@Version
private long version;
```

Hibernate adds the version to every `UPDATE`'s `WHERE` clause:

```sql
UPDATE habits SET name = ?, version = 4 WHERE id = ? AND version = 3
```

If another transaction already bumped `version` to 4, this update matches **zero rows**, and Hibernate throws `OptimisticLockException`. You catch it and return `409 Conflict` to the client. Cheap, no database locks held, exactly right for a web app. We wire the 409 mapping in the mini-project.

---

## 11. `open-in-view` and where lazy loading should happen

Spring Boot's default `open-in-view: true` keeps the Hibernate session open for the whole HTTP request — including JSON serialization in the controller. It feels convenient: lazy associations "just work" when Jackson touches them. It is a trap.

- It hides `LazyInitializationException` in dev, then surprises you when you serialize a lazy collection and fire **dozens of queries during JSON rendering**, in the controller, outside any service transaction.
- It holds a database connection for the duration of view rendering, throttling your connection pool under load.

Set `open-in-view: false`. Now lazy access outside a transaction throws immediately, in development, where you'll see it. The discipline it forces — **load everything you need inside the service's `@Transactional` method, map to a DTO, return the DTO** — is exactly the discipline that prevents the N+1 problem we turn to next.

---

## 12. The N+1 select problem

This is the marquee bug of the week. Here's how it happens.

You have 50 habits, each with check-ins. You write an endpoint that returns every habit with its check-in count. Naively:

```java
@Transactional(readOnly = true)
public List<HabitSummary> summaries() {
    return habitRepository.findAll().stream()              // 1 query: SELECT * FROM habits
        .map(h -> new HabitSummary(h.getName(),
                                   h.getCheckIns().size())) // N queries: one per habit!
        .toList();
}
```

`getCheckIns()` is lazy. The first time you call `.size()` on each habit's check-in list, Hibernate fires a separate `SELECT * FROM check_ins WHERE habit_id = ?`. One query for the 50 habits, then 50 more for the check-ins. **51 queries — 1 + N.** Watch it in the logs (you enabled `org.hibernate.SQL: debug`):

```
Hibernate: select h.* from habits h
Hibernate: select c.* from check_ins c where c.habit_id = ?
Hibernate: select c.* from check_ins c where c.habit_id = ?
Hibernate: select c.* from check_ins c where c.habit_id = ?
... (47 more) ...
```

At 50 habits it's a slow endpoint. At 5,000 it's an outage. The query count grows linearly with the data — the definition of a query that doesn't scale.

### Fix 1 — `JOIN FETCH`

Load the habits and their check-ins in **one** query:

```java
@Query("select distinct h from Habit h left join fetch h.checkIns")
List<Habit> findAllWithCheckIns();
```

```
Hibernate: select distinct h.*, c.* from habits h left join check_ins c on c.habit_id = h.id
```

One query. (Note: a fetch-join of a collection can multiply parent rows, hence `distinct`. For *paginated* fetch joins of collections, prefer `@EntityGraph` or a two-query batch — fetch-joining a collection with `LIMIT` paginates incorrectly in SQL. Hibernate 6 warns you about this.)

### Fix 2 — `@EntityGraph`

```java
@EntityGraph(attributePaths = "checkIns")
@Query("select h from Habit h")
List<Habit> findAllGraph();
```

Same effect, declarative, and it composes with `Pageable` better than a fetch join.

### Fix 3 — batch fetching

Annotate the association or set a global default:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 25
```

Now the N follow-up queries collapse into `ceil(N/25)` queries using `WHERE habit_id IN (?, ?, ...)`. It doesn't get you to one query, but it turns 50 into 2 — often good enough, and it's the safe global default for paginated collections.

### Proving the fix

Don't trust your eyes. **Count the queries.** The cleanest way in a test is Hibernate's statistics:

```java
var stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
stats.setStatisticsEnabled(true);
stats.clear();

service.summaries();

assertThat(stats.getPrepareStatementCount()).isEqualTo(1); // was 51
```

That assertion is the "query count promise" from the README, made concrete. The challenge this week is exactly this loop: see the logs, count the queries, fix the fetch, re-count, prove it dropped — without breaking the API contract.

---

## 13. Recap

You should now be able to:

- Name the four layers — Postgres, JPA, Hibernate, Spring Data JPA — and which one to blame for which failure.
- Map a domain type to a table with `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated(STRING)`, `@Version`.
- Explain why an entity is a mutable class, not a record, and why we keep DTOs separate.
- Model `@ManyToOne` / `@OneToMany`, identify the owning side, and set everything `LAZY` by default.
- Trace the entity lifecycle and explain dirty checking and `LazyInitializationException`.
- Write Spring Data repositories: derived queries, `@Query`, `Pageable`.
- Recognise an N+1 from logged SQL and fix it three ways, then prove the count dropped.

Next we make it reproducible: Postgres in a container, schema as Flyway migrations, and integration tests against real SQL. Continue to [Lecture 2 — Schema as Code: Flyway, Postgres in Docker, and Repository Queries](./02-schema-as-code-flyway-postgres-and-queries.md).

---

## References

- *Spring Data JPA reference*: <https://docs.spring.io/spring-data/jpa/reference/>
- *Hibernate ORM User Guide*: <https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html>
- *Jakarta Persistence 3.1 spec*: <https://jakarta.ee/specifications/persistence/3.1/>
- *Vlad Mihalcea — the persistence context*: <https://vladmihalcea.com/jpa-persistence-context/>
- *Vlad Mihalcea — the N+1 query problem*: <https://vladmihalcea.com/n-plus-1-query-problem/>
- *Spring — `@Transactional`*: <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html>
