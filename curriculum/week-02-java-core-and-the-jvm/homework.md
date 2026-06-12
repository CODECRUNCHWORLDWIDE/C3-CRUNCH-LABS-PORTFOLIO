# Week 2 Homework

Six practice problems that revisit the week's topics. The full set should take about **6 hours** in total. Work in your week-2 Git repository so each problem produces at least one commit you can point to later.

Each problem includes a **problem statement**, **acceptance criteria** so you know when you're done, a **hint** if you get stuck, and an **estimated time**. A grading rubric is at the bottom.

---

## Problem 1 — `java -version` and the toolchain audit

**Problem statement.** Run `java -version`, `javac -version`, and `mvn -version`, and write the relevant pieces into `notes/toolchain.md`. State:

1. The exact JDK version and vendor `java -version` reports (e.g. `Temurin-21.0.x`).
2. Whether `javac -version` matches `java -version` (it must — a mismatch causes confusing build errors).
3. The Maven version and which JDK Maven reports it's running on (`mvn -version` prints the Java home).
4. The default garbage collector your JVM uses. (Run `java -XX:+PrintFlagsFinal -version | grep -i Use.*GC` and find the `UseG1GC` flag set to `true`, or run a program with `-Xlog:gc` and read which collector logs.)
5. One sentence: *if `java -version` showed 17 instead of 21, which single SDKMAN command would fix it?*

**Acceptance criteria.**

- File `notes/toolchain.md` exists with the five items.
- Committed.

**Hint.** `mvn -version` prints "Java version" and "Java home" near the top. The default GC in Java 21 is G1 (`UseG1GC = true`). The SDKMAN fix is `sdk use java 21.0.x-tem`.

**Estimated time.** 20 minutes.

---

## Problem 2 — Three records, one switch expression

**Problem statement.** In `homework/p2-shapes/`, create a small Maven project. Define a `sealed interface Shape permits Circle, Rectangle, Triangle` and three records implementing it:

```java
public record Circle(double radius) implements Shape {}
public record Rectangle(double width, double height) implements Shape {}
public record Triangle(double base, double height) implements Shape {}
```

Write a single method `double area(Shape shape)` that uses a **pattern-matching switch with record deconstruction** to compute the area. **No `default`** (the interface is sealed). Validate in compact constructors that all dimensions are positive.

Drive it from a `main` with one of each shape and print the areas.

**Acceptance criteria.**

- A buildable Maven project under `homework/p2-shapes/`.
- `mvn compile`: `BUILD SUCCESS`, 0 warnings.
- `mvn exec:java` (or `java -cp target/classes ...`) prints three areas.
- The body of `area` is exactly one switch expression — **no `if`/`else`, no `instanceof`, no `default`**.
- A JUnit 5 test class with at least one test per shape and one test that a negative dimension throws.
- Committed.

**Hint.** Circle: `Math.PI * r * r`. Rectangle: `w * h`. Triangle: `0.5 * b * h`. Deconstruction case: `case Circle(var r) -> Math.PI * r * r;`.

**Estimated time.** 45 minutes.

---

## Problem 3 — `Optional` in a real scenario

**Problem statement.** Take the `HabitDirectory` from exercise 3 (or rebuild it). Add a method:

```java
public Optional<Habit> longestStreak();
```

It returns the habit with the highest `currentStreak`, or `Optional.empty()` when the directory is empty. The signature must make the empty case visible; the implementation must contain **no `null`** and must not call `.get()` blindly.

Write three JUnit 5 tests:

1. A directory with several habits returns the right one.
2. Two habits tied for the highest streak — document and test which one you return.
3. An empty directory returns `Optional.empty()`.

**Acceptance criteria.**

- The method is implemented and returns `Optional<Habit>`.
- `mvn test` passes.
- The implementation contains **zero `null`** and zero blind `.get()` (use `max`, `reduce`, or a guarded loop building an `Optional`).
- Build is warning-free.
- Committed.

**Hint.** A guarded loop works: track a `Habit best = null` internally if you must, but **return** `Optional.ofNullable(best)` so the *API* is null-free. Or, cleaner: `habits.stream().max(Comparator.comparingInt(Habit::currentStreak))` returns an `Optional<Habit>` directly (a small Stream preview — fine to use here).

**Estimated time.** 45 minutes.

---

## Problem 4 — Model a domain so illegal states are unrepresentable

**Problem statement.** Model a `Reminder` for a habit as a `sealed interface` with these variants:

- `NoReminder` — the habit has no reminder.
- `DailyAt(LocalTime time)` — remind every day at a time.
- `WeeklyAt(DayOfWeek day, LocalTime time)` — remind on a specific weekday at a time.

Write `String nextDescription(Reminder r)` as an exhaustive switch (no `default`) returning a human-readable description, e.g. `"daily at 08:00"`, `"every MONDAY at 18:30"`, `"no reminder set"`. Use record deconstruction.

Then write `boolean firesOn(Reminder r, DayOfWeek day)` — does this reminder fire on the given weekday? (`NoReminder` never fires; `DailyAt` always fires; `WeeklyAt` fires only on its day.) Also an exhaustive switch.

**Acceptance criteria.**

- `Reminder` is a `sealed interface` with three record variants. `NoReminder` may be a record with no components or a singleton — your choice, document it.
- Both methods are exhaustive switches with **no `default`**.
- `mvn compile`: 0 warnings. A JUnit 5 test covers each variant of each method.
- **Demonstrate exhaustiveness:** in a comment or your commit message, describe what happens if you add a `MonthlyAt` variant (both switches stop compiling).
- Committed.

**Hint.** `LocalTime` and `DayOfWeek` are in `java.time`. `NoReminder` with zero components is fine: `record NoReminder() implements Reminder {}`.

**Estimated time.** 1 hour.

---

## Problem 5 — Read the bytecode and the GC

**Problem statement.** Pick any record you wrote this week. In `notes/under-the-hood.md`, do two things:

1. **Bytecode:** run `javap -c -p` on its compiled `.class` file. Paste the *signatures* (not the full bytecode) of the methods the compiler generated that you did **not** write — `equals`, `hashCode`, `toString`, and the accessors. Write one sentence explaining what `invokedynamic`/`ObjectMethods` is doing for `equals`/`hashCode` if you see it.
2. **GC:** write a tiny program with a loop that allocates a few million short-lived objects (e.g. `new String[]` or a temporary `record` per iteration), run it with `java -Xlog:gc -cp target/classes ...`, and paste **one** GC log line. Annotate it: which generation was collected, how much live data survived, and the pause time in ms.

**Acceptance criteria.**

- `notes/under-the-hood.md` contains the generated method signatures and at least one annotated GC log line.
- Your annotation correctly identifies the collected generation (likely "Pause Young"), the before→after sizes, and the pause time.
- Committed.

**Hint.** A loop like `for (int i = 0; i < 5_000_000; i++) { var s = new String(new char[100]); }` allocates enough to trigger young collections. The log line format is `GC(n) Pause Young (...) 24M->5M(256M) 3.1ms` — the `24M->5M` is live-before → live-after.

**Estimated time.** 1 hour.

---

## Problem 6 — Mini reflection essay

**Problem statement.** Write a 300–400 word reflection at `notes/week-02-reflection.md` answering, each in its own paragraph:

1. Which felt easiest: records and immutability, sealed types and the pattern-matching switch, `Optional`-based absence, or the JVM internals? Which felt hardest? Why?
2. Did anything you previously believed about Java (or the JVM, or "Java is verbose/slow/old") turn out to be wrong this week? If so, what?
3. In one paragraph, how would you explain "why a sealed interface plus an exhaustive switch is safer than an `instanceof` ladder" to a teammate who's only written Java 8?
4. What's one thing you'd want to learn next that this week didn't cover?

**Acceptance criteria.**

- File exists, 300–400 words, each numbered question in its own paragraph.
- Committed.

**Hint.** This is for *you*, not for a grade beyond "did you do it." Be honest. Future-you, debugging a JVM heap dump in week 5, will be glad this exists.

**Estimated time.** 30 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 20 min |
| 2 | 45 min |
| 3 | 45 min |
| 4 | 1 h 0 min |
| 5 | 1 h 0 min |
| 6 | 30 min |
| **Total** | **~4 h 20 min** |

(The remaining time in the week's 6-hour homework budget is reading the linked JEPs and re-running anything that didn't go green the first time.)

---

## Grading rubric

Homework is graded out of **100 points**. The split:

| Area | Points | What earns full marks |
|------|-------:|-----------------------|
| **Builds clean** | 20 | Every problem's Maven project compiles with `mvn compile`/`mvn test`: `BUILD SUCCESS`, **0 warnings** (`-Xlint:all` on). A warning costs points. |
| **Modern idioms** | 25 | Records for data; `sealed` for closed sets; **pattern-matching switches with no `default`** on sealed types; `instanceof` patterns (never `instanceof`-then-cast). |
| **No-null discipline** | 15 | `Optional` for expected absence; no `null` returns in your APIs; no blind `.get()` / `isPresent()`-then-`get()`. The right exception-vs-`Optional` choice in P3. |
| **Correctness & tests** | 25 | Every problem with code has passing JUnit 5 tests covering happy paths **and** the validation/throw/empty paths. The numbers are actually right. |
| **Understanding (P5/P6)** | 15 | P5's bytecode and GC annotations are accurate; P6 is thoughtful, specific, and 300–400 words. |

**Automatic deductions:**

- Any `default` branch on a switch over a `sealed` type that exists only to satisfy the compiler: **−5** (you've discarded the exhaustiveness check).
- Any `return null` to mean "absent" in your own API: **−5** per occurrence.
- Any `instanceof X` immediately followed by a cast to `X`: **−3** per occurrence.
- Any use of `double`/`float` for money (if you do the BigDecimal stretch): **−5**.
- Missing commits (work that can't be pointed to): **−10**.

**Bonus (up to +5):** a clean exhaustiveness demonstration — actually adding a new variant, capturing the compile error, and reverting — documented in P2 or P4, beyond just describing it.

When you've finished all six, push your repo and start the [mini-project](./mini-project/README.md) if you haven't already.
