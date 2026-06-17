# Week 3 — OOP, Collections, and Testing

Welcome to week 3 of **C3 · Crunch Labs Portfolio**. Week 1 set up the team and the toolchain; week 2 gave you the Java 21 language and a first in-memory domain model for **Crunch Tracker** — immutable records and sealed types for `Goal`, `Habit`, and `CheckIn`, wired into a small service that creates and queries them. That model works, but it is a pile of records with one fat service stapled on top. This week we turn it into *engineering*.

Two things happen. First, we refactor the domain behind **interfaces** with **composition over inheritance** and pick the **right collection for each job** instead of reaching for `ArrayList` every time. Second — and this is the habit that makes the rest of the course survivable — we make **testing the default**. By Friday, "it compiles" will never again be mistaken for "it works." You will write JUnit 5 + AssertJ tests as a reflex, drive a small feature red-green-refactor, and learn where Mockito earns its keep and where it actively hurts.

This is the last pure-Java week. Week 4 puts Spring Boot in front of this domain, and Spring without tests is a nightmare. We build the test muscle now so the framework weeks are boring in the good way.

We assume you already have Java 21 fluency from week 2: records, sealed interfaces, pattern-matching `switch`, `Optional`. If any of those feel shaky, re-read week 2's lecture notes before Monday — we lean on all of them.

## Learning objectives

By the end of this week, you will be able to:

- **Design** small types behind **interfaces**, and explain *why* you'd accept an interface and return a concrete type ("program to the interface").
- **Choose composition over inheritance** by default, and recognize the handful of cases where inheritance actually pays.
- **Pick the right collection** — `List` vs `Set` vs `Map` vs `Deque`, and `ArrayList` vs `LinkedList`, `HashMap` vs `TreeMap` vs `LinkedHashMap` — and justify the choice from access patterns and Big-O.
- **Write `equals` and `hashCode` correctly** (and know when a `record` already did it for you), and articulate the contract that keeps `HashSet`/`HashMap` from silently losing your data.
- **Author a test** with **JUnit 5** and **AssertJ** that reads like a specification, not a debugging session.
- **Drive a feature test-first** through the red-green-refactor loop.
- **Use Mockito** to isolate a unit from its collaborators with stubs and verification — and know when a real object is the better call.
- **Introduce a repository interface** so persistence can slot in next week without touching domain logic.
- **Measure coverage** with JaCoCo and read the report honestly (coverage is a smell detector, not a goal).

## Prerequisites

This week assumes you have completed **C3 weeks 1–2**, or equivalent:

- Comfortable in a terminal with **JDK 21** on your PATH (`java --version` prints 21.x) and **Maven** working (`mvn --version`).
- You can read and write Java 21: records, sealed interfaces, `switch` with patterns, generics, `Optional`.
- You can branch, commit with conventional messages, and open a PR (week 1).
- You have the week-2 Crunch Tracker domain model in front of you — we extend it directly. If you skipped week 2, clone the reference tag `c3-week-02` and start from there.

You do **not** need any prior JUnit, AssertJ, or Mockito exposure. We start from `@Test`.

## Topics covered

- Interfaces as contracts: `default` methods, `sealed` interfaces revisited, why "accept the interface, return the implementation."
- Composition over inheritance: the fragile base class problem, the diamond it avoids, delegation, and the rare legitimate `extends`.
- The Collections Framework: `List`, `Set`, `Map`, `Queue`/`Deque`, and the `Collection` and `Iterable` roots.
- Implementation trade-offs: `ArrayList` vs `LinkedList`, `HashMap` vs `LinkedHashMap` vs `TreeMap`, `HashSet` vs `TreeSet`, `ArrayDeque`.
- Big-O for the operations that matter: indexed get, contains, insert-at-end, insert-at-front, ordered iteration.
- The `equals`/`hashCode`/`compareTo` contracts, and why records hand you the first two for free.
- Immutability of collections: `List.of`, `List.copyOf`, defensive copies, `Collections.unmodifiableList`.
- The Stream API as a query tool over collections (`filter`, `map`, `collect`, `groupingBy`) — enough to write readable reports.
- JUnit 5 (Jupiter): `@Test`, `@DisplayName`, `@BeforeEach`, `@Nested`, `@ParameterizedTest`, lifecycle, `assertThrows`.
- AssertJ: fluent assertions that fail with useful messages — `assertThat(x).isEqualTo(...)`, collection and exception assertions.
- Test-Driven Development: the red-green-refactor loop, what a "unit" is, and the test pyramid.
- Mockito: `mock`, `when(...).thenReturn(...)`, `verify`, `ArgumentCaptor` — and the test-double vocabulary (stub vs mock vs fake).
- The repository pattern: an interface that hides storage so week 5 can swap in JPA without breaking a single test.
- JaCoCo coverage: wiring it into Maven, reading the HTML report, and why 80% is a floor, not a trophy.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract.

| Day       | Focus                                            | Lectures | Exercises | Challenge | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|--------------------------------------------------|---------:|----------:|----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | Interfaces, composition over inheritance         |    2h    |    1.5h   |    0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | Collections framework, choosing the right one    |    2h    |    2h     |    0h     |    0.5h   |   1h     |     0h       |    0h      |     6.5h    |
| Wednesday | equals/hashCode, JUnit 5, AssertJ                |    1h    |    2h     |    1h     |    0.5h   |   1h     |     0h       |    0.5h    |     6h      |
| Thursday  | TDD red-green-refactor, Mockito                  |    1h    |    1h     |    1h     |    0.5h   |   1h     |     1.5h     |    0.5h    |     6.5h    |
| Friday    | Repository interface; mini-project work          |    0h    |    1h     |    0h     |    0.5h   |   1h     |     3h       |    0h      |     5.5h    |
| Saturday  | Mini-project deep work                           |    0h    |    0h     |    0h     |    0h     |   0.5h   |     3h       |    0h      |     3.5h    |
| Sunday    | Quiz, review, polish                             |    0h    |    0h     |    0h     |    1h     |   0h     |     0.5h     |    0h      |     1.5h    |
| **Total** |                                                  | **6h**   | **7.5h**  | **3h**    | **3.5h**  | **5.5h** | **11.5h**    | **2h**     | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./00-overview.md) | This overview (you are here) |
| [resources.md](./01-resources.md) | Curated, current docs and references for OOP, collections, JUnit 5, AssertJ, Mockito |
| [lecture-notes/01-oop-that-survives-review.md](./02-lecture-notes/01-oop-that-survives-review.md) | Interfaces, composition over inheritance, and the Collections Framework with real code |
| [lecture-notes/02-testing-as-default.md](./02-lecture-notes/02-testing-as-default.md) | JUnit 5, AssertJ, TDD slices, and Mockito |
| [exercises/README.md](./03-exercises/00-overview.md) | Index of the week's exercises |
| [exercises/exercise-01-pick-the-collection.md](./03-exercises/exercise-01-pick-the-collection.md) | Justify a `List`/`Set`/`Map`/`Deque` choice for six concrete scenarios |
| [exercises/exercise-02-equals-hashcode-and-collections.java](./03-exercises/exercise-02-equals-hashcode-and-collections.java) | Fix a broken `equals`/`hashCode` and a leaky-collection bug; runnable JUnit |
| [exercises/exercise-03-red-green-refactor.java](./03-exercises/exercise-03-red-green-refactor.java) | Drive a `StreakCalculator` from a failing test to green; runnable JUnit |
| [challenges/README.md](./04-challenges/00-overview.md) | What the challenge is and how it's assessed |
| [challenges/challenge-01-tame-the-god-class.md](./04-challenges/challenge-01-tame-the-god-class.md) | Characterization-test a 200-line untested God class, then refactor behind interfaces |
| [mini-project/README.md](./07-mini-project/00-overview.md) | Full spec for refactoring Crunch Tracker behind interfaces with an 80%+ test suite |
| [quiz.md](./05-quiz.md) | 10 multiple-choice questions with an answer key |
| [homework.md](./06-homework.md) | Six practice problems with a grading rubric |

## The "green bar" promise

C3 uses one recurring marker in every exercise that ends in working code:

```
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

If `mvn test` doesn't print `BUILD SUCCESS` with zero failures and zero errors, you are not done. From this week on, a feature without a test is a feature that does not exist. We treat a red bar the way week 1 treated a merge conflict: not a disaster, just the next thing to resolve.

## Stretch goals

If you finish the regular work early and want to push further:

- Read the JUnit 5 User Guide section on the **`@Nested`** and **`@ParameterizedTest`** features and convert one of your flat test classes into a nested, parameterized one: <https://docs.junit.org/current/user-guide/>.
- Skim the AssertJ **"core assertions"** guide and find three assertions you didn't know existed (try `extracting`, `satisfies`, `usingRecursiveComparison`): <https://assertj.github.io/doc/>.
- Read Martin Fowler's **"Mocks Aren't Stubs"** and write a paragraph on which style your mini-project uses: <https://martinfowler.com/articles/mocksArentStubs.html>.
- Browse the `spring-projects/spring-petclinic` test suite on GitHub. It is the canonical "well-tested Spring app" — see how they structure unit vs slice tests *before* you meet Spring next week: <https://github.com/spring-projects/spring-petclinic>.
- Generate a JaCoCo report on your week-2 code as-is. Note how low it is. That number is your starting line.

## Up next

Continue to **Week 4 — Spring Boot REST Basics** once you have pushed the mini-project to your team's GitHub and the CI build is green. The repository interface you introduce this week is the seam Spring plugs into next week — get it clean now.

---

*If you find errors in this material, open an issue or send a PR. Future learners will thank you.*
