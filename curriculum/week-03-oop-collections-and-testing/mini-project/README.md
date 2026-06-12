# Mini-Project — Crunch Tracker: Clean Services Behind Interfaces, Fully Tested

> Take the in-memory Crunch Tracker domain you built in week 2 — records and sealed types for `Goal`, `Habit`, and `CheckIn`, plus a service that creates and queries them — and turn it into well-factored services behind interfaces, backed by a JUnit 5 + AssertJ suite hitting **80%+ coverage on the domain logic**. Introduce a `Repository` interface so week 4's Spring layer and week 5's JPA can slot in without touching a line of domain code.

This is the compounding mini-project: week 2 gave you the model, week 3 makes it *engineering*, week 4 puts HTTP in front of it, week 5 gives it a real database. Everything you build here is load-bearing for the rest of the course. Pure Java still — **no Spring, no database, no web.** Just JDK 21, Maven, and tests.

**Estimated time:** ~11.5 hours (split across Thursday, Friday, Saturday in the suggested schedule).

---

## Where you're starting from

You should have the week-2 domain. If you don't (or want a clean baseline), the starting model is roughly:

```java
// Week 2 deliverable — immutable records + sealed types + one in-memory service.
public record Goal(UUID id, String name, int targetPerWeek) {}
public record Habit(UUID id, String name, Cadence cadence, int target) {}
public enum Cadence { DAILY, WEEKLY }

public sealed interface CheckIn permits Completed, Skipped, Partial {
    UUID habitId();
    LocalDate date();
}
public record Completed(UUID habitId, LocalDate date) implements CheckIn {}
public record Skipped(UUID habitId, LocalDate date, String reason) implements CheckIn {}
public record Partial(UUID habitId, LocalDate date, int percent) implements CheckIn {}

// ...and a single TrackerService that holds ArrayLists and does everything.
```

The model is fine. The *service* is the junior draft we're going to professionalize.

---

## What you will build

A `crunch-tracker-core` Maven module with:

1. **The domain model**, tidied: records for `Goal`, `Habit`; the sealed `CheckIn` hierarchy; validation in compact constructors (no blank names, no non-positive targets, `0 <= percent <= 100`).
2. **Repository interfaces** — `GoalRepository`, `HabitRepository`, `CheckInRepository` — each with an **in-memory `Map`-backed implementation** (a *fake* you'll reuse in every test, and that week 5 replaces with JPA).
3. **Composed services behind interfaces** — `HabitService`, `GoalService`, and a `StatsService` that computes points-per-habit and longest streaks, depending on a swappable `PointPolicy` (composition, not inheritance).
4. **A JUnit 5 + AssertJ test suite** that drives the domain to **80%+ line coverage**, measured with JaCoCo, mixing `@Test`, `@ParameterizedTest`, and at least one Mockito-based isolation test.

By the end you'll have a public repo of ~400–600 lines of Java (excluding tests) plus a test suite that is *larger* than the production code — which is normal and good.

---

## Rules

- **You may** read the lecture notes, the Java docs, and the JUnit/AssertJ/Mockito docs.
- **You may NOT** add Spring, a database driver, or any web framework. This is the last pure-Java week on purpose.
- Production dependencies: the JDK only. Test dependencies (all `<scope>test</scope>`): `junit-jupiter`, `assertj-core`, `mockito-junit-jupiter`. JaCoCo as a build plugin.
- **Every public service method must depend on an interface**, never a concrete repository class. If a constructor parameter names `InMemoryHabitRepository`, you did it wrong — it names `HabitRepository`.
- **No mutable state leaks.** Collections returned from services are unmodifiable copies; records holding collections copy defensively.
- **Zero raw arrays as data holders.** Use typed collections, chosen deliberately.
- Target: Java 21 (`<maven.compiler.release>21</maven.compiler.release>`).

---

## The `pom.xml` you'll need

Reuse from week 1, with these test dependencies and the JaCoCo plugin (versions current as of 2026):

```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.26.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.14.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.1</version>
        </plugin>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution><goals><goal>prepare-agent</goal></goals></execution>
                <execution>
                    <id>report</id><phase>test</phase>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Acceptance criteria

- [ ] A new public GitHub repo (or a module in your existing Crunch Tracker repo) named `c3-week-03-crunch-tracker-core-<yourhandle>`.
- [ ] Project layout follows the standard:
  ```
  crunch-tracker-core/
  ├── pom.xml
  ├── .gitignore                       (excludes target/)
  ├── README.md
  └── src/
      ├── main/java/com/crunch/tracker/
      │   ├── domain/
      │   │   ├── Goal.java
      │   │   ├── Habit.java
      │   │   ├── Cadence.java
      │   │   ├── CheckIn.java         (sealed) + Completed/Skipped/Partial
      │   │   └── PointPolicy.java     (interface) + StandardPointPolicy
      │   ├── repository/
      │   │   ├── GoalRepository.java          (interface)
      │   │   ├── HabitRepository.java         (interface)
      │   │   ├── CheckInRepository.java       (interface)
      │   │   ├── InMemoryGoalRepository.java
      │   │   ├── InMemoryHabitRepository.java
      │   │   └── InMemoryCheckInRepository.java
      │   └── service/
      │       ├── GoalService.java
      │       ├── HabitService.java
      │       └── StatsService.java
      └── test/java/com/crunch/tracker/
          ├── domain/        (model + validation tests)
          ├── repository/    (fake-repository tests)
          └── service/       (service tests, incl. one Mockito test)
  ```
- [ ] `mvn test` from the root prints `BUILD SUCCESS` with **0 failures, 0 errors**.
- [ ] The JaCoCo report at `target/site/jacoco/index.html` shows **≥ 80% line coverage** on the `domain` and `service` packages.
- [ ] **At least 25 tests**, including:
  - Validation tests for every compact-constructor rule (blank name, non-positive target, percent out of range) using `assertThatThrownBy`.
  - At least three `@ParameterizedTest` methods.
  - At least one Mockito test that isolates a service from a repository (stub + `verify`).
  - Streak and points tests covering consecutive days, gaps, duplicates, and an empty case.
- [ ] **Every service depends on a repository interface**, injected via constructor. No service constructs its own repository.
- [ ] `PointPolicy` is composed into `StatsService` — swapping a `DoublePointPolicy` changes totals with no change to `StatsService`. Include a test proving it.
- [ ] No service returns a mutable view of internal state; no record leaks a mutable collection (defensive copies in compact constructors).
- [ ] Zero raw `String[]`/`Object[]` used as data holders anywhere.
- [ ] Your `README.md` includes the project description, fresh-clone setup + `mvn test` command, the coverage number you achieved, the collection choice for each repository (and why), and a "Things I learned" section with at least three specific items.

---

## Suggested order of operations

### Phase 1 — Tidy the domain + validation (~2h)

Move the records into `domain/` and add validation in **compact constructors**:

```java
package com.crunch.tracker.domain;

import java.util.Objects;
import java.util.UUID;

public record Habit(UUID id, String name, Cadence cadence, int target) {
    public Habit {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cadence, "cadence");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
        if (target <= 0)
            throw new IllegalArgumentException("target must be positive");
    }
}
```

Add the same discipline to `Goal` and to `Partial` (`0 <= percent <= 100`). Write the validation tests **first** (TDD) — one `@Test` or `@ParameterizedTest` per rule, each using `assertThatThrownBy`. Commit: `feat(domain): records with validated compact constructors + tests`.

### Phase 2 — The PointPolicy seam (~1h)

```java
public interface PointPolicy {
    int pointsFor(CheckIn checkIn);
}

public final class StandardPointPolicy implements PointPolicy {
    @Override public int pointsFor(CheckIn checkIn) {
        return switch (checkIn) {
            case Completed c          -> 10;
            case Partial(_, _, var p) -> p / 10;     // 0..10 points
            case Skipped s            -> 0;
        };
    }
}
```

Test each arm of the policy with a `@ParameterizedTest`. Commit: `feat(domain): swappable PointPolicy + tests`.

### Phase 3 — Repository interfaces + in-memory fakes (~2h)

Define each repository as an interface, then implement an in-memory one with a `Map<UUID, ...>` for O(1) lookup:

```java
public interface HabitRepository {
    Habit save(Habit habit);
    Optional<Habit> findById(UUID id);
    List<Habit> findAll();
    void deleteById(UUID id);
}

public final class InMemoryHabitRepository implements HabitRepository {
    private final Map<UUID, Habit> store = new LinkedHashMap<>();  // insertion order for deterministic findAll

    @Override public Habit save(Habit habit) { store.put(habit.id(), habit); return habit; }
    @Override public Optional<Habit> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public List<Habit> findAll() { return List.copyOf(store.values()); }  // unmodifiable copy
    @Override public void deleteById(UUID id) { store.remove(id); }
}
```

Note the deliberate choices: `LinkedHashMap` so `findAll()` is deterministic (you'll thank yourself when a test asserts order), and `List.copyOf` so callers can't mutate the store. Write tests for save/find/findAll/delete on each fake. Commit: `feat(repository): repository interfaces + in-memory fakes + tests`.

### Phase 4 — Services behind interfaces (~2.5h)

```java
public final class HabitService {
    private final HabitRepository repository;          // interface, injected

    public HabitService(HabitRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public Habit create(String name, Cadence cadence, int target) {
        return repository.save(new Habit(UUID.randomUUID(), name, cadence, target));
    }

    public List<Habit> active() {
        return repository.findAll().stream().filter(h -> h.target() > 0).toList();
    }
}
```

`StatsService` composes the check-in repository, the `PointPolicy`, and a `StreakCalculator` (reuse exercise 3). Test the services against the in-memory fakes for the happy paths, and write **one Mockito test** that isolates a service from its repository to verify an interaction (e.g. `create` calls `save` exactly once). Commit: `feat(service): habit/goal/stats services + tests`.

### Phase 5 — Coverage + polish (~1.5h)

- Run `mvn test` and open `target/site/jacoco/index.html`. If domain/service coverage is under 80%, the report shows you the exact uncovered lines — add tests for them (usually an unhappy path you skipped).
- Add the `DoublePointPolicy` test proving composition bought you swappable behavior.
- Write the README: setup, coverage number, collection-choice rationale, "things I learned."
- Commit: `test: lift domain coverage above 80%; docs: README`.

### Phase 6 — Wire the seam for next week (~0.5h)

Confirm that **nothing in `service/` or `domain/` imports anything from `repository/InMemory*`** — services see only the interfaces. That decoupling is the whole point: next week Spring constructs your services and injects the in-memory repos via DI, and the week after, JPA repos replace the in-memory ones with **zero changes to services or their tests**. Add a one-line note in the README pointing at that seam. Commit: `docs: note the repository seam for week 4/5`.

---

## Example: what a great test file looks like

```java
@DisplayName("StatsService")
class StatsServiceTest {

    private InMemoryCheckInRepository checkIns;
    private StatsService stats;

    @BeforeEach
    void setUp() {
        checkIns = new InMemoryCheckInRepository();
        stats = new StatsService(checkIns, new StandardPointPolicy());
    }

    @Test
    @DisplayName("sums points across a habit's check-ins")
    void points_areSummedPerPolicy() {
        UUID habit = UUID.randomUUID();
        checkIns.save(new Completed(habit, LocalDate.of(2026, 6, 1)));   // 10
        checkIns.save(new Partial(habit, LocalDate.of(2026, 6, 2), 50)); // 5
        checkIns.save(new Skipped(habit, LocalDate.of(2026, 6, 3), "sick")); // 0

        assertThat(stats.pointsFor(habit)).isEqualTo(15);
    }

    @Test
    @DisplayName("a DoublePointPolicy doubles totals with no change to StatsService")
    void compositionMakesScoringSwappable() {
        var doubled = new StatsService(checkIns, c -> 2 * new StandardPointPolicy().pointsFor(c));
        UUID habit = UUID.randomUUID();
        checkIns.save(new Completed(habit, LocalDate.of(2026, 6, 1)));   // 20 now

        assertThat(doubled.pointsFor(habit)).isEqualTo(20);
    }
}
```

---

## Rubric

| Criterion | Weight | What "great" looks like |
|-----------|-------:|-------------------------|
| Builds and tests clean | 20% | `mvn test` is green on a fresh clone; JaCoCo report generates |
| Interface discipline | 20% | Every service depends on a repository *interface*; no service builds its own repo; the domain/service packages don't import the in-memory impls |
| Composition over inheritance | 10% | `PointPolicy` is composed and swappable; no inheritance used to vary behavior; the swap test passes |
| Test quality & coverage | 25% | ≥ 25 tests, ≥ 80% domain/service line coverage, a mix of `@Test`/`@ParameterizedTest`, at least one Mockito isolation test, both happy and unhappy paths |
| Immutability & collections | 15% | Defensive copies; unmodifiable returns; deliberate, justified collection choices; zero raw arrays as data holders |
| README quality | 10% | A stranger can clone and run in < 5 minutes; coverage number stated; collection rationale and "things I learned" present |

---

## Stretch (optional)

- Add a `@Nested`, fully `@ParameterizedTest`-driven version of your streak tests, sourcing cases from `@MethodSource`.
- Add a `GoalProgressService` that joins goals to habits and check-ins to report "this week's progress toward each goal" — a richer `groupingBy` pipeline. Test it.
- Wire a JaCoCo coverage **gate** (`<rule>` with `<minimum>0.80</minimum>`) into the build so `mvn verify` *fails* if coverage drops below 80%. Watch it fail, then make it pass — that's CI doing its job.
- Add `equals`/`hashCode` *characterization* tests proving your records' generated equality behaves as you expect across the sealed `CheckIn` types.

---

## What this prepares you for

- **Week 4 (Spring Boot REST):** Spring's DI container will construct your `HabitService` and inject a repository — the exact constructor-injection shape you used here, now done by the framework. Your services don't change.
- **Week 5 (JPA + Postgres):** the `HabitRepository` interface gets a JPA implementation. Because your services and tests depend only on the interface, they don't change — and your in-memory fakes stay useful for fast unit tests forever.
- **The whole rest of the course:** the test-first reflex you build here is the difference between Spring being boring (good) and Spring being terrifying (bad).

---

## Submission

When done:

1. Push your repo to GitHub with a public URL.
2. Make sure `README.md` has the setup commands, the achieved coverage number, and the collection rationale.
3. Make sure `mvn test` is green and the JaCoCo report generates on a freshly cloned copy.
4. Confirm CI is green (your week-1 GitHub Actions build should run `mvn test`).
5. Post the repo URL in your cohort tracker. You turned a draft into engineering — show it.
