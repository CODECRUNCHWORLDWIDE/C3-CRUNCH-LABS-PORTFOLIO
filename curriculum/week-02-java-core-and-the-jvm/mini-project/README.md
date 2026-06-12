# Mini-Project — Crunch Tracker: The Core Domain Model

> Build the immutable, framework-free heart of **Crunch Tracker** — the habit/goal tracker that every later week extends. This week you model the domain with `record`s and `sealed` types, validate every invariant in compact constructors, and put a small in-memory service on top that creates and queries goals, habits, and check-ins. Pure Java 21. No Spring. No database. No framework wallpaper hiding what's happening.

This is the second compounding deliverable in C3. In week 1 you stood up the repo, the board, the branch protection, and a green CI build for Crunch Tracker. **This week you put the first real code in it: the domain model.** Weeks 3 onward build directly on what you write here — week 3 refactors these services behind interfaces with a full test suite, week 4 wraps them in a REST API, week 5 swaps in-memory storage for Postgres. The records and services you write this week, you keep. So write them like you'll be reading them in week 10, because you will be.

**Estimated time:** ~10 hours (split across Thursday, Friday, Saturday in the suggested schedule).

---

## What you will build

A library — no `main`-driven CLI required, though a small demo `main` is encouraged — that models the Crunch Tracker domain and exposes an in-memory service to manage it.

The domain, in plain English:

- A **Goal** is something the user wants to do regularly (e.g. "Read every day"), with a weekly target (1–7 times).
- A **Habit** is a concrete recurring action tied to a Goal (e.g. "Read 20 pages"), and it has a current streak.
- A **CheckIn** records what happened for a habit on a given day. A check-in is one of a *closed set* of outcomes — completed, skipped, or partial — modeled as a `sealed` hierarchy.
- A **CrunchTrackerService** holds goals, habits, and check-ins **in memory** and lets you create and query them.

By the end you'll have ~300–400 lines of clean Java 21 (excluding tests) plus a JUnit 5 suite, all building green with `mvn test`, and committed to your week-2 repo.

---

## Rules

- **Pure Java 21.** The *only* dependency beyond the JDK is **JUnit 5** (test scope), already available from your week-1 skeleton. No Spring. No Lombok. No Guava. No Jackson. If you reach for a framework this week, you've missed the point.
- **Records for all domain data.** `Goal`, `Habit`, and every `CheckIn` variant are `record`s. There is no mutable domain object.
- **Sealed for the check-in outcomes.** `CheckIn` is a `sealed interface`; its variants are records that `permit`-list it. Processing it uses **exhaustive pattern-matching switches — no `default`, no `instanceof`.**
- **No `null` in the public API.** Lookups that can miss return `Optional<T>`. A method that can hand a caller `null` is a bug this week.
- **Money/quantities are exact.** Use `int`/`long` for counts and streaks. (There's no currency in the tracker yet; if you add one as a stretch, it's `BigDecimal`, never `double`.)
- **Validate invariants in compact constructors.** An object that exists is valid by construction. No half-built domain objects.
- **Target framework:** Java 21, compiled with `-Xlint:all`. **Warnings are bugs.**

---

## Acceptance criteria

- [ ] A repository (your week-2 Crunch Tracker repo) with a Maven module laid out to the C3 standard:
  ```
  tracker-core/
  ├── pom.xml                       (Java 21, JUnit 5, -Xlint:all)
  ├── .gitignore                    (excludes target/)
  └── src/
      ├── main/java/com/crunch/tracker/
      │   ├── domain/
      │   │   ├── Goal.java
      │   │   ├── Habit.java
      │   │   └── CheckIn.java       (sealed interface + Completed/Skipped/Partial records)
      │   ├── service/
      │   │   └── CrunchTrackerService.java
      │   └── App.java              (a small demo main — optional but encouraged)
      └── test/java/com/crunch/tracker/
          ├── domain/
          │   ├── GoalTest.java
          │   ├── HabitTest.java
          │   └── CheckInTest.java
          └── service/
              └── CrunchTrackerServiceTest.java
  ```
- [ ] `mvn test` from the module root prints `BUILD SUCCESS`, **0 warnings**, and **at least 15 passing tests** covering both the domain types and the service.
- [ ] `Goal`, `Habit`, and the three `CheckIn` variants are `record`s; `CheckIn` is a `sealed interface`.
- [ ] Every method that processes a `CheckIn` is an **exhaustive switch with no `default`**. Demonstrate (in the README) that adding a fourth variant breaks compilation.
- [ ] **Zero `null`** in the public API. Service lookups return `Optional`. **Zero `instanceof`-then-cast.**
- [ ] All domain invariants are enforced in compact constructors, and there are tests proving each one throws on bad input.
- [ ] A `README.md` for the module that includes:
  - One paragraph describing the domain.
  - The exact commands to build and test from a fresh clone.
  - A short usage example showing the service creating and querying data.
  - A "Things I learned" section with at least 3 specific items.

---

## Suggested order of operations

Build incrementally. Commit after each phase.

### Phase 1 — Module skeleton (~1h)

1. From your week-2 repo root, create the `tracker-core` Maven module (reuse exercise 1's `pom.xml` — Java 21, `-Xlint:all`, JUnit 5).
2. Add the `junit-jupiter` test dependency and the `maven-surefire-plugin` so `mvn test` runs JUnit 5:
   ```xml
   <dependency>
     <groupId>org.junit.jupiter</groupId>
     <artifactId>junit-jupiter</artifactId>
     <version>5.11.3</version>
     <scope>test</scope>
   </dependency>
   ```
3. Confirm `mvn test` runs (it'll find no tests yet — that's fine).
4. Commit: `tracker-core module skeleton`.

### Phase 2 — Goal and Habit records (~1.5h)

`domain/Goal.java`:

```java
package com.crunch.tracker.domain;

public record Goal(long id, String title, int targetPerWeek) {
    public Goal {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title must not be blank");
        if (targetPerWeek < 1 || targetPerWeek > 7)
            throw new IllegalArgumentException("targetPerWeek must be 1..7");
        title = title.strip();
    }
}
```

`domain/Habit.java` — a `Habit` belongs to a `Goal` (by `goalId`), has a `name` and a `currentStreak`, an `isOnFire()` (`streak >= 7`), and a `withIncrementedStreak()` wither. Validate `name` non-blank and `currentStreak >= 0`.

Write `GoalTest` and `HabitTest`: value equality, compact-constructor rejections, the wither's immutability (original unchanged).

Commit: `Goal and Habit records + tests`.

### Phase 3 — The sealed CheckIn hierarchy (~2h)

`domain/CheckIn.java`:

```java
package com.crunch.tracker.domain;

import java.time.LocalDate;

public sealed interface CheckIn permits CheckIn.Completed, CheckIn.Skipped, CheckIn.Partial {
    long habitId();
    LocalDate date();

    record Completed(long habitId, LocalDate date) implements CheckIn {
        public Completed {
            if (date == null) throw new IllegalArgumentException("date must not be null");
        }
    }

    record Skipped(long habitId, LocalDate date, String reason) implements CheckIn {
        public Skipped {
            if (date == null) throw new IllegalArgumentException("date must not be null");
            if (reason == null) throw new IllegalArgumentException("reason must not be null");
        }
    }

    record Partial(long habitId, LocalDate date, int done, int target) implements CheckIn {
        public Partial {
            if (date == null) throw new IllegalArgumentException("date must not be null");
            if (target <= 0) throw new IllegalArgumentException("target must be > 0");
            if (done < 0 || done > target)
                throw new IllegalArgumentException("done must be 0..target");
        }
    }

    /** Counts toward a streak if it represents real progress. Exhaustive switch, no default. */
    default boolean countsTowardStreak() {
        return switch (this) {
            case Completed c            -> true;
            case Skipped s              -> false;
            case Partial(var id, var d, var done, var t) -> done > 0;
        };
    }
}
```

Write `CheckInTest`: each variant's validation, and `countsTowardStreak()` for all three. **Then temporarily add a fourth variant and confirm `countsTowardStreak` stops compiling** — note this in your README.

Commit: `Sealed CheckIn hierarchy + exhaustive switch + tests`.

### Phase 4 — The in-memory service (~2.5h)

`service/CrunchTrackerService.java`. This is the in-memory heart. It holds goals, habits, and check-ins; assigns ids; and answers queries. Suggested surface:

```java
public final class CrunchTrackerService {

    // creation — return the created object with its assigned id
    public Goal createGoal(String title, int targetPerWeek);
    public Habit createHabit(long goalId, String name);   // throws if the goal doesn't exist
    public CheckIn recordCheckIn(CheckIn checkIn);          // throws if the habit doesn't exist

    // queries — Optional for "might miss", List for collections
    public Optional<Goal> findGoal(long goalId);
    public Optional<Habit> findHabit(long habitId);
    public List<Goal> allGoals();
    public List<Habit> habitsForGoal(long goalId);
    public List<CheckIn> checkInsForHabit(long habitId);

    // derived — uses the sealed switch from CheckIn
    public int streakForHabit(long habitId);              // counts consecutive countsTowardStreak() days
    public long completedCount(long habitId);             // how many Completed check-ins
}
```

Implementation notes:

- Store with `Map<Long, Goal>`, `Map<Long, Habit>`, and a `List<CheckIn>` (or `Map<Long, List<CheckIn>>` keyed by habit). An `AtomicLong` or a plain `long` counter assigns ids.
- `createHabit` and `recordCheckIn` must **validate their foreign keys**: creating a habit for a nonexistent goal, or recording a check-in for a nonexistent habit, throws (this is the "broken invariant ⇒ exception" case from exercise 3, not the "expected miss ⇒ Optional" case).
- `findGoal`/`findHabit` return `Optional` (expected misses).
- `streakForHabit` and `completedCount` use the sealed switch — `streakForHabit` counts how many of the most recent consecutive days have a check-in that `countsTowardStreak()`.
- Return defensive, **unmodifiable** copies from list queries (`List.copyOf(...)`) — callers must not be able to mutate your internal state.
- No `null` returns anywhere.

Write `CrunchTrackerServiceTest`: create a goal, create habits under it, record check-ins, and assert the queries and derived numbers. Cover the throw paths (habit for missing goal; check-in for missing habit) and the Optional-empty paths (find a goal that doesn't exist).

Commit: `In-memory CrunchTrackerService + tests`.

### Phase 5 — Demo main + README (~1h)

`App.java` — a small `main` that creates a goal, two habits, a few check-ins, and prints a streak. Useful as living documentation:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.crunch.tracker.App"
```

Write the module `README.md`: the domain paragraph, the build/test commands, the usage example, the exhaustiveness demonstration, and "Things I learned."

Commit: `Demo main + module README`.

### Phase 6 — Polish (~1h)

- Run `javap -c -p target/classes/com/crunch/tracker/domain/Goal.class` once and confirm the generated `equals`/`hashCode`/`toString` are there — mention it in "Things I learned."
- Confirm `mvn clean test` is green from a fresh `target/`.
- Confirm the CI build you set up in week 1 stays green with the new code (push and watch GitHub Actions).
- Open a PR into your week-2 branch following the week-1 PR conventions; self-review the diff before merging.

---

## Example expected behavior

A small driver and its output:

```java
var svc = new CrunchTrackerService();

Goal read = svc.createGoal("Read every day", 7);
Habit pages = svc.createHabit(read.id(), "Read 20 pages");

svc.recordCheckIn(new CheckIn.Completed(pages.id(), LocalDate.parse("2026-06-08")));
svc.recordCheckIn(new CheckIn.Completed(pages.id(), LocalDate.parse("2026-06-09")));
svc.recordCheckIn(new CheckIn.Partial(pages.id(), LocalDate.parse("2026-06-10"), 10, 20));
svc.recordCheckIn(new CheckIn.Skipped(pages.id(), LocalDate.parse("2026-06-11"), "travel"));

System.out.println("goals:          " + svc.allGoals().size());        // 1
System.out.println("habits for goal:" + svc.habitsForGoal(read.id()).size()); // 1
System.out.println("completed:      " + svc.completedCount(pages.id())); // 2
System.out.println("streak:         " + svc.streakForHabit(pages.id())); // 3 (Completed, Completed, Partial-with-progress; Skipped breaks it)
System.out.println("missing goal:   " + svc.findGoal(999L).isPresent());  // false
```

Adjust the exact numbers to your streak definition, but the output must be deterministic and the streak logic must match what your tests assert.

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Builds and tests | 25% | `mvn test` clean on a fresh clone — `BUILD SUCCESS`, 0 warnings, 15+ tests |
| Domain modeling | 25% | Records + sealed `CheckIn`; invariants in compact constructors; illegal states unrepresentable |
| Exhaustiveness & no-null | 20% | Every `CheckIn` switch has no `default`; absence is `Optional`; zero `instanceof`-cast |
| Service correctness | 20% | Queries, streak, and counts are right; foreign-key checks throw; list queries return unmodifiable copies |
| README quality | 10% | Someone unfamiliar can clone and run in <5 minutes; "Things I learned" is specific |

---

## What this prepares you for

- **Week 3** refactors these services behind interfaces (`GoalRepository`, `HabitRepository`) and backs them with a real JUnit 5 + AssertJ suite at 80%+ on domain logic. Your in-memory `Map`-based storage becomes the first implementation of a repository interface — so persistence can slot in next.
- **Week 4** wraps this exact domain in a Spring Boot REST API: the records become request/response DTOs, the service becomes a `@Service` bean, and the sealed validation becomes `ProblemDetail` errors.
- **Week 5** swaps the in-memory `Map`s for Spring Data JPA on Postgres — same domain, real persistence.

The discipline you build this week — records, sealed types, exhaustive switches, no nulls, invariants at construction — is the foundation every later week stands on. Get it right now and the framework weeks are about plumbing, not firefighting.

---

## Resources

- *Records reference (JDK 21)*: <https://docs.oracle.com/en/java/javase/21/language/records.html>
- *Sealed classes (JDK 21)*: <https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html>
- *JEP 441: Pattern Matching for switch*: <https://openjdk.org/jeps/441>
- *Optional API*: <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html>
- *JUnit 5 user guide*: <https://junit.org/junit5/docs/current/user-guide/>
- *Maven in five minutes*: <https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html>

---

## Submission

When done:

1. Push your week-2 branch and open a PR into your Crunch Tracker repo, following the week-1 PR conventions.
2. Make sure the module `README.md` includes the build/test commands and the usage example.
3. Make sure `mvn clean test` is green on a freshly cloned copy and CI passes.
4. Post the repo/PR link in your cohort tracker. This is the foundation of the app you ship in week 10 — show it.
