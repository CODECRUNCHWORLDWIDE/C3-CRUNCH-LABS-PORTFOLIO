# Week 5 — Quiz

Twelve questions. Take it with your lecture notes closed. Aim for 10/12 before moving to Week 6. Answer key at the bottom — don't peek.

---

**Q1.** Which statement correctly describes the relationship between JPA, Hibernate, and Spring Data JPA?

- A) They are three names for the same library, bundled in one starter.
- B) JPA is the specification (annotations + `EntityManager` contract), Hibernate is an implementation of it, and Spring Data JPA generates repository implementations on top.
- C) Hibernate is the specification, JPA is Hibernate's implementation, and Spring Data JPA is the database driver.
- D) Spring Data JPA is the specification; Hibernate and JPA are competing databases.

---

**Q2.** Why must a JPA `@Entity` be a class with a no-arg constructor rather than a Java record?

- A) Records can't have fields.
- B) The JPA spec requires a no-arg constructor and mutable fields so the provider can instantiate and populate the entity; records are final and have no no-arg constructor.
- C) Records can't be annotated.
- D) There is no reason; you can map a record with `@Entity` directly.

---

**Q3.** You set `spring.jpa.hibernate.ddl-auto=validate` and use Flyway. On startup, what does Hibernate do?

- A) Generates and runs `CREATE TABLE` statements from your entities.
- B) Drops and recreates the schema to match your entities.
- C) Compares your entity mappings against the existing (Flyway-built) schema and fails fast if they disagree.
- D) Nothing; `validate` disables Hibernate entirely.

---

**Q4.** Given this mapping, which side is the *owning* side of the relationship?

```java
class Habit  { @OneToMany(mappedBy = "habit") List<CheckIn> checkIns; }
class CheckIn{ @ManyToOne @JoinColumn(name = "habit_id") Habit habit; }
```

- A) `Habit.checkIns`, because it's listed first.
- B) `CheckIn.habit`, because it has the `@JoinColumn` / foreign key.
- C) Neither; `mappedBy` makes them both owning.
- D) Both, equally.

---

**Q5.** What is the correct default fetch strategy to set on a `@ManyToOne`, and why?

- A) `EAGER`, because you always need the parent.
- B) `LAZY`, because eager many-to-ones are a leading cause of accidental N+1 queries; fetch explicitly when needed.
- C) `EAGER`, because `LAZY` doesn't work on `@ManyToOne`.
- D) It doesn't matter; Hibernate ignores the setting.

---

**Q6.** Inside a `@Transactional` method you load a managed `Habit`, call `habit.setName("New")`, and return — without calling `save`. What happens?

- A) Nothing is written; you forgot to call `save`.
- B) Hibernate's dirty checking detects the change and issues an `UPDATE` at flush (transaction commit).
- C) It throws because the entity is read-only.
- D) The change is written only if you also call `flush()` manually.

---

**Q7.** You see `LazyInitializationException: could not initialize proxy - no Session`. What's the usual cause?

- A) The database is down.
- B) You accessed a lazy association *after* the persistence context / transaction closed (e.g. in a controller with `open-in-view: false`).
- C) You used `@Enumerated(EnumType.STRING)`.
- D) Flyway hasn't run yet.

---

**Q8.** A Flyway migration `V2__add_check_ins.sql` ran successfully yesterday and was pushed. Today you need to add a column to that table. What do you do?

- A) Edit `V2__add_check_ins.sql` to add the column.
- B) Add a new migration `V3__add_column.sql`; never edit an applied migration.
- C) Delete `V2` and let Hibernate regenerate the schema.
- D) Manually `ALTER TABLE` in production and update `V2` to match.

---

**Q9.** Why store an enum with `@Enumerated(EnumType.STRING)` rather than the default `ORDINAL`?

- A) `STRING` is faster to query.
- B) `ORDINAL` stores the integer position; reordering or inserting enum constants silently changes the meaning of every stored row. `STRING` stores the name, which is stable.
- C) `ORDINAL` isn't supported on Postgres.
- D) `STRING` uses less storage.

---

**Q10.** This service method fires 1 query for the list plus 1 query per habit for its check-ins:

```java
habitRepository.findAll().stream()
    .map(h -> new Summary(h.getName(), h.getCheckIns().size()))
    .toList();
```

Which is **not** a valid fix?

- A) Use `@Query("select distinct h from Habit h left join fetch h.checkIns")`.
- B) Use `@EntityGraph(attributePaths = "checkIns")`.
- C) Use a constructor projection that does `count(c)` in SQL.
- D) Change `@ManyToOne` on `CheckIn` to `EAGER` so the collection loads automatically.

(Choose the option that is *not* a correct fix for *this* N+1.)

---

**Q11.** Why does C3 require Testcontainers (real Postgres) for integration tests instead of in-memory H2?

- A) H2 is slower than Postgres.
- B) H2's SQL dialect, constraint handling, and type behavior differ from Postgres, so tests can pass on H2 and fail in production; tests must run on the same engine.
- C) H2 can't run in Java.
- D) Testcontainers is required by the JPA spec.

---

**Q12.** In a Spring Boot 3.1+ test, what does `@ServiceConnection` on a `PostgreSQLContainer` field do?

- A) Nothing; it's decorative.
- B) It auto-configures Spring's `DataSource` to point at the running container, so you don't need `@DynamicPropertySource` or a hardcoded JDBC URL.
- C) It starts a second database for read replicas.
- D) It disables Flyway during tests.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — Four distinct layers (the database is the fourth). JPA = spec, Hibernate = provider/implementation, Spring Data JPA = repository abstraction on top. They ship aligned in one starter but do different jobs.
2. **B** — The spec needs a no-arg constructor and settable fields so the provider can build an empty instance and hydrate it. Records are final with no no-arg constructor, so they can't be entities. (Records remain perfect for DTOs.)
3. **C** — `validate` makes Hibernate *check* the entities against the existing schema and fail at startup on drift. Flyway owns DDL; Hibernate just verifies agreement. `update`/`create` are the ones that generate DDL — and we never use them where it matters.
4. **B** — The owning side has the foreign-key column, marked by `@JoinColumn` (or implied by `@ManyToOne`). `mappedBy` marks the *inverse* side and tells Hibernate not to create a join table.
5. **B** — Set `@ManyToOne(fetch = FetchType.LAZY)` explicitly. The default for `@ManyToOne` is `EAGER`, which silently loads parents and is the classic accidental-N+1 source. Everything lazy by default; fetch deliberately.
6. **B** — Automatic dirty checking. A managed entity's changes are detected and flushed (usually at commit) without an explicit `save`. This is correct and intended — and a reason to be deliberate about setters inside transactions.
7. **B** — A lazy association is a proxy that only loads while the session is open. Touch it after the transaction closed (common with `open-in-view: false`) and you get this exception. Fix: fetch what you need inside the transaction, map to a DTO, return the DTO.
8. **B** — The cardinal rule. Applied migrations are frozen; their checksums are recorded. Editing one causes a checksum mismatch and halts Flyway. Always add the next-numbered migration.
9. **B** — `ORDINAL` persists the integer index, so reordering/inserting constants reinterprets existing rows. `STRING` persists the stable name. Always `STRING`.
10. **D** — Making the `@ManyToOne` eager is backwards: that's the `CheckIn → Habit` direction, not the `Habit → checkIns` collection, and eager fetching is the *cause* of accidental N+1, not a fix. A, B, and C are all valid fixes for this collection N+1.
11. **B** — H2 (even in "Postgres mode") differs in dialect, constraints, and type handling; green-on-H2/red-in-prod is a real failure mode. C3's rule: integration tests run on the same engine production uses — Postgres via Testcontainers.
12. **B** — `@ServiceConnection` (Spring Boot 3.1+) wires the container's connection details straight into the app's datasource, eliminating manual property plumbing. Flyway then builds the schema in the container and your test hits real SQL.

</details>

---

If you scored under 9, re-read the lectures for the questions you missed — especially the entity lifecycle (Q6/Q7), Flyway discipline (Q8), and the N+1 fixes (Q5/Q10). If you scored 11 or 12, you're ready for the [homework](./homework.md).
