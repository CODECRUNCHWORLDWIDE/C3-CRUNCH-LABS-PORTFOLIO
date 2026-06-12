# Week 3 Homework

Six practice problems that revisit the week's topics. The full set should take about **5.5 hours**. Work in your Week 3 Git repository so each problem produces at least one commit you can point to later. Each problem ends with a test or a written deliverable; from this week on, code without a test does not count as done.

Each problem includes a **problem statement**, **acceptance criteria**, a **hint**, and an **estimated time**. The grading rubric is at the bottom — read it before you start so you know what "great" looks like.

---

## Problem 1 — Collection choices, written and defended

**Problem statement.** In `notes/hw1-collections.md`, answer these four design questions for Crunch Tracker. For each, name the interface, the implementation, and a one-sentence Big-O justification:

1. A leaderboard that must always be iterable in descending-points order, updated as points change.
2. The set of distinct habit categories a user has used, shown in a dropdown in alphabetical order.
3. A cache of the last 20 habits a user opened, newest-first, oldest evicted.
4. Counting how many check-ins each habit has, across tens of thousands of check-ins.

Then add a short paragraph: *one collection mistake you've personally made (or seen) and how the access-pattern framework from lecture 1 would have caught it.*

**Acceptance criteria.**
- `notes/hw1-collections.md` exists with four answers (interface + impl + justification) and the paragraph.
- Committed.

**Hint.** Leaderboard → `TreeSet`/`TreeMap` with a comparator, or a list you re-sort. Categories → `TreeSet`. Recent → `ArrayDeque` (or a `LinkedHashMap` with access-order eviction). Counts → `HashMap` with `merge(id, 1, Integer::sum)`.

**Estimated time.** 30 minutes.

---

## Problem 2 — Refactor a subclass into composition

**Problem statement.** You're given this fragile design:

```java
class LoggingList<E> extends ArrayList<E> {
    int writes = 0;
    @Override public boolean add(E e) { writes++; return super.add(e); }
    @Override public boolean addAll(Collection<? extends E> c) { writes += c.size(); return super.addAll(c); }
}
```

Demonstrate the bug with a failing test (`addAll` double-counts), then **refactor `LoggingList` to use composition** (hold a `List<E>`, delegate, count around the delegation) so the test passes. Keep the public methods `add`, `addAll`, `get`, `size`, and `writes()`.

**Acceptance criteria.**
- A test that *fails* against the inheritance version (commit it first, red).
- A composition-based `LoggingList` that makes the test pass.
- `mvn test`: green.
- The final class does **not** use `extends` on a collection type.
- Committed (history shows red → green).

**Hint.** `private final List<E> delegate = new ArrayList<>();` then `public boolean addAll(Collection<? extends E> c) { writes += c.size(); return delegate.addAll(c); }` — the delegate calling its own `add` no longer touches your counter.

**Estimated time.** 45 minutes.

---

## Problem 3 — Get `equals`/`hashCode` right (two ways)

**Problem statement.** Model a `MoneyTag` that pairs a category name (compared case-insensitively) with a currency code (compared case-sensitively). Implement it **twice**:

1. As a hand-written `final class` with correct `equals` and `hashCode`.
2. As a `record` with a compact constructor that normalizes the category to lowercase.

Write tests proving, for *both* implementations, that two "equal" tags share a hash code and that a `HashSet` de-duplicates them.

**Acceptance criteria.**
- Both implementations exist and pass the same set of tests (parameterize over both if you like).
- The hand-written `equals` and `hashCode` use the *same* fields and the *same* normalization.
- `mvn test`: green.
- Committed.

**Hint.** Hand-written: `Objects.hash(category.toLowerCase(Locale.ROOT), currency)` and matching `equals`. Record: `MoneyTag { category = category.toLowerCase(Locale.ROOT); }` then equality is generated for you.

**Estimated time.** 1 hour.

---

## Problem 4 — Test-drive a `WeeklyGoalTracker`

**Problem statement.** Build, **test-first**, a `WeeklyGoalTracker` with one method: `int completedThisWeek(List<LocalDate> completedDates, LocalDate weekStart)` — the count of completed dates that fall within the 7-day window `[weekStart, weekStart+6]`, counting duplicates once. Drive it from at least five failing-then-passing tests covering: empty input, all-in-window, some-before, some-after, and duplicates.

**Acceptance criteria.**
- A buildable, runnable class built incrementally (commits show red → green).
- At least five tests, all passing under `mvn test`.
- The window check is inclusive on both ends; duplicates counted once (use a `Set`).
- `mvn test`: green.
- Committed.

**Hint.** `Set<LocalDate> inWindow = new HashSet<>(); for (LocalDate d : completedDates) if (!d.isBefore(weekStart) && !d.isAfter(weekStart.plusDays(6))) inWindow.add(d); return inWindow.size();`

**Estimated time.** 1 hour.

---

## Problem 5 — A Mockito interaction test, and a critique of it

**Problem statement.** Take the `HabitService.create(...)` from the mini-project (or write a small equivalent). Write a Mockito test that:

1. Stubs `HabitRepository.save(...)` to return its argument.
2. Verifies `save` is called exactly once with a habit whose name matches the input (use an `ArgumentCaptor`).

Then write a **second** test of the *same* behavior using a real in-memory fake repository instead of a mock. In `notes/hw5-mock-vs-fake.md`, write one paragraph: which test do you trust more, and why? Reference lecture 2's "when not to mock."

**Acceptance criteria.**
- One Mockito-based test (stub + `verify` + `ArgumentCaptor`), passing.
- One fake-based test of the same behavior, passing.
- `notes/hw5-mock-vs-fake.md` with the comparison paragraph.
- `mvn test`: green.
- Committed.

**Hint.** `var captor = ArgumentCaptor.forClass(Habit.class); when(repo.save(captor.capture())).thenAnswer(i -> i.getArgument(0));` then after the call, `assertThat(captor.getValue().name()).isEqualTo("Read");` and `verify(repo, times(1)).save(any());`.

**Estimated time.** 1 hour.

---

## Problem 6 — Coverage report and reflection

**Problem statement.** Wire JaCoCo into your week-3 homework project (the plugin config is in the mini-project README). Run `mvn test`, open `target/site/jacoco/index.html`, and find the class with the *lowest* coverage. Add tests to lift it, then write `notes/week-03-reflection.md` (300–400 words) answering:

1. Which uncovered lines did JaCoCo reveal, and were any of them genuine bugs hiding behind untested branches?
2. Did composition-over-inheritance feel natural or forced this week? Where did you almost reach for `extends` and stop yourself?
3. After test-driving a feature, do you believe the "write the test first" discipline or do you still resist it? Be honest.
4. What's one thing about testing you'd want to learn next that this week didn't cover (e.g. integration tests, property-based testing, Testcontainers)?

**Acceptance criteria.**
- JaCoCo report generates on `mvn test`.
- At least one class's coverage measurably improved by added tests (note the before/after %).
- `notes/week-03-reflection.md` exists, 300–400 words, each numbered question in its own paragraph.
- Committed.

**Hint.** This is for *you*, not for a grade. Future-you, meeting Spring next week, will be glad the testing reflex is already there.

**Estimated time.** 30 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|---------------:|
| 1 | 30 min |
| 2 | 45 min |
| 3 | 1 h 0 min |
| 4 | 1 h 0 min |
| 5 | 1 h 0 min |
| 6 | 30 min |
| **Total** | **~4 h 45 min** |

---

## Grading rubric

Your homework is graded out of 100 points across these dimensions:

| Dimension | Points | What earns full marks |
|-----------|-------:|-----------------------|
| **Correctness** | 30 | Every problem's code does what's asked; all tests pass on a fresh clone (`mvn test` green). |
| **Test-first discipline** | 20 | Problems 2 and 4 show red-before-green in the git history; tests are real assertions, not `assertTrue(true)`. |
| **Design quality** | 20 | Composition replaces inheritance (P2); `equals`/`hashCode` are correct and consistent (P3); collections are chosen from the access pattern (P1), not by habit. |
| **Testing depth** | 15 | A mix of `@Test` and `@ParameterizedTest`; both happy and unhappy paths; the mock-vs-fake critique (P5) shows real understanding, not just "mocks are bad." |
| **Coverage & honesty** | 10 | JaCoCo wired and read honestly (P6); coverage improved meaningfully, not gamed with assertion-free tests. |
| **Communication** | 5 | The written notes (P1, P5, P6) are specific and reference the lecture concepts by name. |

**Passing is 70/100.** Below 70, revisit the lectures for the dimensions you lost points on and resubmit. The point of this week is the *habit* — if the test-first reflex isn't forming yet, that's the thing to keep drilling before Spring.

When you've finished all six, push your repo and open the [mini-project](./mini-project/README.md).
