# Week 3 — Exercises

Short, focused drills. Each one should take 25–50 minutes. Do them in order; later ones assume earlier ones.

## Index

1. **[Exercise 1 — Pick the collection](exercise-01-pick-the-collection.md)** — for six concrete Crunch Tracker scenarios, choose `List`/`Set`/`Map`/`Deque` and the implementation, and justify it from access pattern and Big-O. (~35 min)
2. **[Exercise 2 — equals/hashCode and collections](exercise-02-equals-hashcode-and-collections.java)** — fix a broken `equals`/`hashCode` that makes a `HashSet` lose data, and plug a mutable-collection leak. Runnable JUnit 5 + AssertJ. (~45 min)
3. **[Exercise 3 — Red-green-refactor a StreakCalculator](exercise-03-red-green-refactor.java)** — drive a feature from a failing test to green, test-first, then refactor behind an interface. Runnable JUnit 5 + AssertJ. (~50 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. Muscle memory is the entire point.
- For the `.java` exercises, drop the file into a Maven project's `src/test/java/com/crunch/tracker/` (exercise 1 from week 1 has the layout) and run `mvn test`.
- Every coding exercise must end with `mvn test` printing **`BUILD SUCCESS`** and zero failures. A red bar is the next thing to fix, not a stopping point.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each `.java` file.

## What you need installed

- **JDK 21** — `java --version` prints `21.x`.
- **Maven** — `mvn --version` works.
- A `pom.xml` with `junit-jupiter`, `assertj-core`, and (for exercise 3's stretch) `mockito-junit-jupiter`, all at `<scope>test</scope>`. The mini-project README lists exact versions; reuse that `pom.xml` here.

## The exercise checklist

Mark each done when:

- [ ] **Exercise 1** — a written table with a chosen type *and a one-sentence justification* for all six scenarios, plus the two "gotcha" answers.
- [ ] **Exercise 2** — `mvn test` is green; the `HashSet` no longer loses duplicates; the leak test passes; you used a record OR a correct hand-written `equals`/`hashCode` pair.
- [ ] **Exercise 3** — `StreakCalculator` was built test-first (your commit history shows red before green), all provided tests pass, and the calculator sits behind an interface.
- [ ] You committed each exercise with a conventional message (`test: ...`, `refactor: ...`).

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-03` to compare.
