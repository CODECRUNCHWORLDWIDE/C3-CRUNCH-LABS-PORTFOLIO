# Lecture 2 — Testing as Default

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can write a JUnit 5 + AssertJ test that reads like a specification, drive a feature red-green-refactor, and use Mockito to isolate a unit from its collaborators — knowing when a mock is the right tool and when it's a trap.

If you only remember one thing from this lecture, remember this:

> **A feature without a test does not exist.** From this week on, "it compiles" is never evidence that "it works." The test is the evidence. We're not adding tests as a chore at the end; we're making the test the thing you write *first*, or at worst *alongside*, the code.

This is the habit that makes the Spring weeks survivable. Spring is mostly configuration, and configuration is mostly invisible until it's wrong. Teams that test thrive in Spring; teams that don't, drown. We build the muscle now, in plain Java, where there's nothing to hide behind.

---

## 1. Why test, honestly

You will hear "tests slow you down." For the first afternoon, true. After that it inverts. Here's what tests actually buy you, in order of how much they matter:

1. **A safety net for refactoring.** Lecture 1 was all about refactoring. You *cannot* refactor safely without tests — you're just hoping. With a green suite, you change code, rerun, and the bar tells you instantly whether you broke something. This is the entire reason this lecture follows lecture 1.
2. **A spec you can run.** A good test name is documentation that can't go stale: `archivedHabit_isNotCountedAsActive`. Six months later it still tells the truth, because if it lied, it would be red.
3. **Design pressure.** Code that's hard to test is usually badly designed — too many responsibilities, hidden dependencies, static state. Testing surfaces those problems early, while they're cheap.
4. **Fewer 2 a.m. pages.** The bug you catch in a unit test costs a minute. The same bug in production costs a day and your weekend.

We're not chasing 100% coverage. We're building the reflex: *write the test, watch it fail, make it pass.*

---

## 2. JUnit 5 — the framework

JUnit 5 (the modern version, also called *Jupiter*) is the default Java test framework in 2026. Your week-1 Maven setup already has it; if not, the dependency is:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.3</version>
    <scope>test</scope>
</dependency>
```

`<scope>test</scope>` matters: test libraries are compiled and run for `mvn test` but are **not** shipped in your jar. Your production code can't accidentally depend on them.

### The smallest test

Tests live under `src/test/java`, mirroring the package of the thing they test:

```java
package com.crunch.tracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HabitTest {

    @Test
    void activeHabit_hasPositiveTarget() {
        Habit habit = new Habit(UUID.randomUUID(), "Read", Cadence.DAILY, 7);
        assertEquals(7, habit.target());
    }
}
```

`@Test` marks a method as a test. The class doesn't need to be `public` in JUnit 5 (a nice cleanup from JUnit 4). Run it:

```bash
mvn test
```

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

That `BUILD SUCCESS` with zeros across the board is the green bar. It's the line you're chasing in every exercise this week.

### The lifecycle annotations

```java
class StatsServiceTest {

    private InMemoryCheckInRepository repo;
    private StatsService service;

    @BeforeEach                       // runs before EACH @Test — fresh state every time
    void setUp() {
        repo = new InMemoryCheckInRepository();
        service = new StatsService(repo, new StandardPointPolicy());
    }

    @Test
    void emptyRepository_yieldsNoPoints() {
        assertTrue(service.pointsByHabit().isEmpty());
    }

    @AfterEach                        // runs after each test — usually for cleanup
    void tearDown() { /* close files, etc. */ }
}
```

`@BeforeEach` gives every test a **fresh, isolated** fixture. This is non-negotiable: tests that share mutable state pass or fail depending on *order*, which is a nightmare to debug. One fresh `service` per test, always. (`@BeforeAll`/`@AfterAll` exist for expensive one-time setup and must be `static` — use them rarely.)

### `@DisplayName` and `@Nested` — tests that read as a spec

```java
@DisplayName("StreakCalculator")
class StreakCalculatorTest {

    @Nested
    @DisplayName("when there are no check-ins")
    class Empty {
        @Test
        @DisplayName("reports a streak of zero")
        void zero() { /* ... */ }
    }

    @Nested
    @DisplayName("when check-ins are on consecutive days")
    class Consecutive {
        @Test
        @DisplayName("counts the full run")
        void fullRun() { /* ... */ }

        @Test
        @DisplayName("resets after a gap")
        void resetsAfterGap() { /* ... */ }
    }
}
```

The IDE/test report prints this as an outline:

```
StreakCalculator
├─ when there are no check-ins
│  └─ reports a streak of zero
└─ when check-ins are on consecutive days
   ├─ counts the full run
   └─ resets after a gap
```

That's a specification. Anyone — including future-you — reads it and understands the behavior without opening the source.

### `assertThrows` — testing the unhappy path

Half of good testing is the error cases. JUnit makes asserting an exception a first-class thing:

```java
@Test
void negativeTarget_isRejected() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new Habit(UUID.randomUUID(), "Read", Cadence.DAILY, -1));
    assertTrue(ex.getMessage().contains("target"));
}
```

If the lambda *doesn't* throw, the test fails. If it throws the *wrong* type, the test fails. Test the failures as deliberately as the successes.

### `@ParameterizedTest` — one test, many cases

When the same assertion holds for a table of inputs, don't copy-paste the test:

```java
@ParameterizedTest
@CsvSource({
    "10, 10",      // Completed → 10 points
    "0,  0",       // Skipped   → 0 points
})
void points_matchPolicy(int input, int expected) {
    // ...
}

@ParameterizedTest
@ValueSource(ints = {0, -1, -100})
void nonPositiveTarget_isRejected(int badTarget) {
    assertThrows(IllegalArgumentException.class,
        () -> new Habit(UUID.randomUUID(), "x", Cadence.DAILY, badTarget));
}
```

One method, many rows, each reported as a separate test. This is how you cover boundaries (0, -1, max) without three near-identical methods.

---

## 3. AssertJ — assertions that read like English

JUnit's built-in `assertEquals(expected, actual)` works but is clumsy: you have to remember the argument order, and the failure messages are terse. **AssertJ** is the fluent assertion library the Java world standardized on. The dependency:

```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.26.3</version>
    <scope>test</scope>
</dependency>
```

The whole API starts with one static import: `import static org.assertj.core.api.Assertions.*;` — and then `assertThat(...)`:

```java
assertThat(habit.target()).isEqualTo(7);
assertThat(habit.name()).isEqualTo("Read").isNotBlank();
assertThat(service.pointsByHabit()).isEmpty();
```

The win is two things: **IDE autocomplete drives you to the right assertion**, and the **failure messages are specific**. Compare:

```java
// JUnit: "expected: <7> but was: <0>"
assertEquals(7, habit.target());

// AssertJ on a richer type — the failure tells you exactly what diverged:
assertThat(habits)
    .hasSize(3)
    .extracting(Habit::name)
    .containsExactly("Read", "Run", "Meditate");
// Failure: "Expecting ["Read","Run"] to contain exactly ["Read","Run","Meditate"]
//           but could not find: ["Meditate"]"
```

### The collection assertions you'll use constantly

```java
assertThat(list).hasSize(3);
assertThat(list).contains(habitA, habitB);
assertThat(list).containsExactly(a, b, c);            // order matters
assertThat(list).containsExactlyInAnyOrder(c, a, b);  // order doesn't
assertThat(list).isEmpty();
assertThat(set).doesNotContain(removedHabit);
assertThat(map).containsEntry(habitId, 30);
assertThat(map).containsKeys(id1, id2);

// extracting — assert on one field across a collection
assertThat(habits).extracting(Habit::name)
                  .containsExactlyInAnyOrder("Read", "Run");
```

### Exception assertions — fluent and exhaustive

```java
assertThatThrownBy(() -> new Habit(id, "x", Cadence.DAILY, -1))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("target");

// Or the BDD-flavored alias, when you prefer reading "then":
assertThatExceptionOfType(IllegalArgumentException.class)
    .isThrownBy(() -> repo.save(null))
    .withMessageContaining("must not be null");
```

### `usingRecursiveComparison` — comparing whole objects

Records give you `equals` for free, so `isEqualTo` usually just works. But when you want to compare two objects field-by-field *ignoring* one field (like a random id or a timestamp), AssertJ has you:

```java
assertThat(actualHabit)
    .usingRecursiveComparison()
    .ignoringFields("id")
    .isEqualTo(expectedHabit);
```

**C3 coding standard: use AssertJ for all assertions.** Mixing JUnit `assertEquals` and AssertJ `assertThat` in one file is noise. Pick AssertJ and stay there.

---

## 4. Test-Driven Development: red-green-refactor

TDD is a discipline, not a religion. The loop, from Kent Beck:

1. **Red.** Write a *failing* test for the next small behavior. Run it. Watch it fail. (A test that passes before you write the code is testing nothing.)
2. **Green.** Write the *minimum* code to make it pass. Not the elegant version — the dumbest thing that turns the bar green.
3. **Refactor.** Now that you have a green safety net, clean up. Remove duplication, rename, extract. Rerun after each change; stay green.

Repeat in tiny cycles — minutes, not hours.

### A worked TDD slice: `StreakCalculator`

We want: "given a habit's completed dates, return the length of the longest run of consecutive days." Let's build it test-first. (This is exactly exercise 3 — here's the technique.)

**Red #1 — the empty case:**

```java
@Test
void noCheckIns_streakIsZero() {
    var calc = new StreakCalculator();
    assertThat(calc.longest(List.of())).isZero();
}
```

Run it. It doesn't even compile — `StreakCalculator` doesn't exist. That's a legitimate red. Create the minimum:

```java
public final class StreakCalculator {
    public int longest(List<LocalDate> dates) {
        return 0;        // dumbest thing that passes
    }
}
```

Green. Yes, `return 0` is "wrong" — but it passes *this* test, and we only write code that a test demands.

**Red #2 — a single day:**

```java
@Test
void oneDay_streakIsOne() {
    var calc = new StreakCalculator();
    assertThat(calc.longest(List.of(LocalDate.of(2026, 6, 1)))).isEqualTo(1);
}
```

`return 0` now fails this. Make both pass:

```java
public int longest(List<LocalDate> dates) {
    return dates.isEmpty() ? 0 : 1;
}
```

**Red #3 — consecutive days:**

```java
@Test
void threeConsecutiveDays_streakIsThree() {
    var calc = new StreakCalculator();
    var dates = List.of(
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 2),
        LocalDate.of(2026, 6, 3));
    assertThat(calc.longest(dates)).isEqualTo(3);
}
```

Now `? 0 : 1` fails. *Now* the real algorithm is justified:

```java
public int longest(List<LocalDate> dates) {
    NavigableSet<LocalDate> sorted = new TreeSet<>(dates);   // sorted + de-duped
    int best = 0, run = 0;
    LocalDate prev = null;
    for (LocalDate day : sorted) {
        run = (prev != null && day.equals(prev.plusDays(1))) ? run + 1 : 1;
        best = Math.max(best, run);
        prev = day;
    }
    return best;
}
```

**Red #4 — a gap resets the run, and duplicates don't double-count:**

```java
@Test
void gapResetsTheStreak() {
    var dates = List.of(d(6,1), d(6,2), d(6,5), d(6,6), d(6,7));  // run of 3 wins
    assertThat(new StreakCalculator().longest(dates)).isEqualTo(3);
}

@Test
void duplicateDates_areCountedOnce() {
    var dates = List.of(d(6,1), d(6,1), d(6,2));
    assertThat(new StreakCalculator().longest(dates)).isEqualTo(2);
}
```

Both pass already — the `TreeSet` handled sorting *and* dedup for free (that's why we chose it; collections knowledge from lecture 1 paying off). **Refactor** if needed, rerun, stay green. Notice the design *emerged* from the tests instead of being guessed up front, and every edge case is now permanently guarded.

That's the loop. Small steps, always green between them, the algorithm justified by a failing test rather than imagined.

---

## 5. Mockito — isolating a unit from its collaborators

Sometimes the thing you're testing depends on something slow, nondeterministic, or not-yet-built — a repository, a clock, an email sender. You don't want to spin up a database to test a *service*. You want a stand-in. That's a **test double**, and **Mockito** makes them.

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.14.2</version>
    <scope>test</scope>
</dependency>
```

### Stubbing — canned answers

```java
@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock HabitRepository repository;        // a fake HabitRepository
    @InjectMocks HabitService service;       // gets the mock injected via constructor

    @Test
    void create_savesAndReturnsTheHabit() {
        // Arrange: stub save() to return its argument.
        when(repository.save(any(Habit.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Habit created = service.create("Read", Cadence.DAILY, 7);

        // Assert
        assertThat(created.name()).isEqualTo("Read");
    }
}
```

`when(...).thenReturn(...)` (or `.thenAnswer(...)`) defines what the fake does when called. The real `HabitRepository` — JPA, a database, none of it — is involved. You're testing `HabitService` *in isolation*.

### Verification — asserting an interaction happened

A *mock* (in Fowler's precise sense) is a double whose **calls you assert on**:

```java
@Test
void create_persistsExactlyOnce() {
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

    service.create("Read", Cadence.DAILY, 7);

    verify(repository, times(1)).save(any(Habit.class));   // it was saved once
    verifyNoMoreInteractions(repository);                   // and nothing else happened
}
```

### `ArgumentCaptor` — inspect what was passed

```java
@Test
void create_assignsAnId() {
    var captor = ArgumentCaptor.forClass(Habit.class);
    when(repository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

    service.create("Read", Cadence.DAILY, 7);

    Habit saved = captor.getValue();
    assertThat(saved.id()).isNotNull();
    assertThat(saved.name()).isEqualTo("Read");
}
```

### When NOT to mock — the most important slide

Mockito is sharp. Over-mocking produces tests that pass while the system is broken, because you've asserted on *interactions* instead of *outcomes*. Rules:

- **Don't mock types you don't own** (the JDK, third-party libs). Wrap them in your own interface and mock that, or use a real instance.
- **Don't mock value objects** — records, `String`, `LocalDate`. Just construct real ones. Mocking a record is a code smell.
- **Prefer a real fake for repositories.** An in-memory `Map`-backed `HabitRepository` (a *fake*) is often clearer than five lines of `when(...)`. You'll write exactly such a fake for the mini-project, and reuse it everywhere.
- **Mock at the boundaries** — the clock, the email sender, the payment gateway, the not-yet-built collaborator. Use real objects in the middle.

The vocabulary, from Fowler's "Mocks Aren't Stubs":

| Double | What it is | Example |
|--------|-----------|---------|
| **Dummy** | Passed but never used | a `null` filler argument |
| **Stub** | Returns canned answers | `when(clock.now()).thenReturn(fixed)` |
| **Mock** | You verify its interactions | `verify(repo).save(any())` |
| **Fake** | Working but simplified | an in-memory `Map`-backed repository |

Reach for a **fake** first; reach for a **mock** when you specifically need to assert *that a call happened* and a real object can't tell you.

---

## 6. Coverage with JaCoCo — a smell detector, not a goal

Wire JaCoCo into your Maven build to measure which lines your tests actually run:

```xml
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
```

Run `mvn test` and open `target/site/jacoco/index.html`. You'll see line and branch coverage per class.

**Read it honestly.** 90% coverage with assertions that never check anything is worthless; 75% coverage where every branch of your domain logic is asserted is excellent. Coverage tells you what you *forgot* to test — a class at 20% is a flag. It does **not** tell you your tests are *good*. The mini-project's 80% target is a floor that forces you to test the domain; it is not a trophy. Chase *meaningful* assertions, and coverage follows.

---

## 7. Recap

You should now be able to:

- Write a JUnit 5 test with `@Test`, `@BeforeEach`, `@DisplayName`, and `@Nested` so it reads as a spec.
- Test the unhappy path with `assertThrows` / `assertThatThrownBy`.
- Cover a table of cases with `@ParameterizedTest`.
- Use AssertJ fluently — collection, map, and exception assertions.
- Drive a feature red-green-refactor, writing the failing test first.
- Use Mockito to stub and verify, and articulate when a real fake beats a mock.
- Wire JaCoCo and read coverage as a smell detector, not a score.

Now do the exercises — pick collections deliberately, fix a broken `equals`/`hashCode`, and test-drive `StreakCalculator` from red to green.

---

## References

- *JUnit 5 User Guide*: <https://docs.junit.org/current/user-guide/>
- *AssertJ Core*: <https://assertj.github.io/doc/>
- *Mockito Javadoc (the real manual)*: <https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html>
- *Martin Fowler — "Mocks Aren't Stubs"*: <https://martinfowler.com/articles/mocksArentStubs.html>
- *Martin Fowler — "The Practical Test Pyramid"*: <https://martinfowler.com/articles/practical-test-pyramid.html>
- *JaCoCo Maven plugin*: <https://www.jacoco.org/jacoco/trunk/doc/maven.html>
