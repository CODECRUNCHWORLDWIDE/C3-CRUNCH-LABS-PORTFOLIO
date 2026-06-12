# Challenge 1 — Tame the God Class

**Time estimate:** ~120 minutes.

## The situation

You've inherited `TrackerManager` — a 200-line class a previous developer wrote for Crunch Tracker before this course's standards existed. It *works* (the app uses it), but it does everything: it stores habits and check-ins in raw fields, computes streaks, tallies points, formats reports, validates input, and prints to `System.out`. There are **no tests**. Your tech lead wants it refactored behind clean interfaces so week 4's Spring layer can use it — but the app depends on its current behavior, so **you must not change what it does, only how it's built.**

This is the most important skill in the job: changing untrusted code without breaking it. The technique is **characterization testing**, from Michael Feathers' *Working Effectively with Legacy Code*:

> A characterization test pins down the code's *current* behavior — bugs and all — so that when you refactor, any change in behavior shows up as a red test. You write the tests **first**, against the messy code, **before** you touch the logic.

## The God class

Drop this into `src/main/java/com/crunch/tracker/legacy/TrackerManager.java`. It compiles and runs. It is also a mess. **Do not "fix" it yet.**

```java
package com.crunch.tracker.legacy;

import java.time.LocalDate;
import java.util.*;

// A "God class": stores data, computes stats, formats output, validates,
// and prints — all in one type, with no tests. Your job is to tame it.
public class TrackerManager {

    private final List<String[]> habits = new ArrayList<>();   // [id, name, target]
    private final List<String[]> checkIns = new ArrayList<>(); // [habitId, date, status]

    public String addHabit(String name, int target) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("name required");
        if (target <= 0)
            throw new IllegalArgumentException("target must be positive");
        String id = UUID.randomUUID().toString();
        habits.add(new String[] { id, name.trim(), String.valueOf(target) });
        return id;
    }

    public void checkIn(String habitId, LocalDate date, String status) {
        boolean found = false;
        for (String[] h : habits) if (h[0].equals(habitId)) found = true;
        if (!found) throw new IllegalArgumentException("no such habit: " + habitId);
        if (!status.equals("COMPLETED") && !status.equals("SKIPPED") && !status.equals("PARTIAL"))
            throw new IllegalArgumentException("bad status: " + status);
        checkIns.add(new String[] { habitId, date.toString(), status });
    }

    public int points(String habitId) {
        int total = 0;
        for (String[] c : checkIns) {
            if (!c[0].equals(habitId)) continue;
            if (c[2].equals("COMPLETED")) total += 10;
            else if (c[2].equals("PARTIAL")) total += 5;
            // SKIPPED adds 0
        }
        return total;
    }

    public int longestStreak(String habitId) {
        List<LocalDate> done = new ArrayList<>();
        for (String[] c : checkIns)
            if (c[0].equals(habitId) && c[2].equals("COMPLETED"))
                done.add(LocalDate.parse(c[1]));
        Collections.sort(done);
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate day : done) {
            if (prev != null && day.equals(prev)) continue; // skip dup
            if (prev != null && day.equals(prev.plusDays(1))) run++;
            else run = 1;
            if (run > best) best = run;
            prev = day;
        }
        return best;
    }

    public String report(String habitId) {
        String name = null;
        for (String[] h : habits) if (h[0].equals(habitId)) name = h[1];
        if (name == null) throw new IllegalArgumentException("no such habit: " + habitId);
        StringBuilder sb = new StringBuilder();
        sb.append("Habit: ").append(name).append("\n");
        sb.append("Points: ").append(points(habitId)).append("\n");
        sb.append("Longest streak: ").append(longestStreak(habitId)).append("\n");
        return sb.toString();
    }

    public void printReport(String habitId) {
        System.out.println(report(habitId));   // side effect: hard to test
    }

    public int habitCount() { return habits.size(); }
    public int checkInCount() { return checkIns.size(); }
}
```

## Your mission, in order

### Phase 1 — Characterize (write tests FIRST) — ~45 min

Before changing a single line of `TrackerManager`, write a JUnit 5 + AssertJ suite that pins down its **current** behavior. At minimum, characterize:

- Adding a valid habit returns a non-null id and increments `habitCount()`.
- Adding a habit with a blank name throws; with a non-positive target throws.
- Checking in against an unknown habit throws; with a bad status throws.
- `points(...)`: COMPLETED = 10, PARTIAL = 5, SKIPPED = 0, summed correctly across multiple check-ins.
- `longestStreak(...)`: consecutive days, a gap resetting the run, duplicate dates counted once.
- `report(...)`: returns a string containing the habit name, the points line, and the streak line. (Assert on the substrings — don't hard-code the whole blob; that makes the test brittle.)

**Commit this suite now, before any refactor.** The commit message should make the order obvious, e.g. `test: characterize legacy TrackerManager before refactor`. This commit is graded.

> Tip: `printReport` writes to `System.out`. You don't need to test the printing — you already test `report()` which produces the same string. This is a hint about where the seam goes.

### Phase 2 — Refactor behind interfaces (tests stay green) — ~60 min

Now restructure. Run the characterization suite after *every* small step — it must stay green the whole way. Split `TrackerManager`'s responsibilities into clean types under `com.crunch.tracker`:

- A proper `Habit` **record** and a `CheckIn` model (use a `sealed` interface or an enum `Status` — your call; justify it).
- A `HabitRepository` **interface** and an in-memory implementation (a `Map<UUID, Habit>`-backed *fake* — exactly the kind you met in lecture 2).
- A `CheckInRepository` interface and in-memory implementation.
- A `PointPolicy` interface (composition!) so the 10/5/0 scoring is swappable, not hard-coded in a method.
- A `StreakCalculator` (reuse exercise 3's, behind its `Streaks` interface).
- A `ReportService` that *composes* the repositories, the point policy, and the streak calculator, and returns the report **string** (no `System.out` inside it — printing is the caller's job).

Replace the raw `String[]` arrays with typed collections chosen deliberately (a `HashMap` for id lookup, etc. — name your reasons).

When you're done, the old `TrackerManager` should either be gone or be a thin facade delegating to the new pieces. Either way, the characterization suite — **unchanged** — must still pass.

## Acceptance criteria

- [ ] A repo (or `challenges/challenge-01/` folder in your week-3 repo) with the legacy class, the new design, and the test suite.
- [ ] **The characterization suite was committed before the refactor** (visible in git history).
- [ ] The same suite is green both before and after the refactor. No assertion was weakened or deleted to make the refactor "pass."
- [ ] `mvn test`: `BUILD SUCCESS`, zero failures, **at least 12 tests**.
- [ ] The refactored design has: typed `Habit`/`CheckIn` models, two repository **interfaces** with in-memory fakes, a composed `PointPolicy`, a `StreakCalculator` behind an interface, and a `ReportService` with no `System.out` inside it.
- [ ] Collections are chosen deliberately — no raw `String[]` arrays survive; the README names each non-obvious choice.
- [ ] A JaCoCo report shows **80%+ line coverage** on the new domain types.
- [ ] A short `README.md` with: the before/after class diagram (ASCII is fine), what each new type is responsible for, and a "behavior I deliberately preserved (including this quirk)" note.

## Stretch

- The legacy `points` method silently ignores check-ins for nonexistent habits (it never validates). Decide whether that's a bug to fix or a behavior to preserve. **Whatever you decide, write a test that documents the decision** and note it in the README. (This is the heart of legacy work: deciding which quirks are load-bearing.)
- Add a `DoublePointPolicy` (every score doubled) and a test proving `ReportService` produces different totals when constructed with it — demonstrating that composition bought you swappable behavior with zero changes to `ReportService`.
- Use Mockito to write *one* interaction test on `ReportService`: verify it calls `findById` on the habit repository exactly once per report. Then ask yourself whether that test earns its keep versus the outcome-based tests — write your answer in the README.

## Hints

<details>
<summary>What a characterization test of the streak quirk looks like</summary>

```java
@Test
@DisplayName("CHARACTERIZATION: duplicate completed dates count once")
void streak_dedupesDuplicates() {
    var mgr = new TrackerManager();
    String id = mgr.addHabit("Read", 7);
    mgr.checkIn(id, LocalDate.of(2026, 6, 1), "COMPLETED");
    mgr.checkIn(id, LocalDate.of(2026, 6, 1), "COMPLETED"); // duplicate
    mgr.checkIn(id, LocalDate.of(2026, 6, 2), "COMPLETED");
    assertThat(mgr.longestStreak(id)).isEqualTo(2);   // pin the CURRENT behavior
}
```

You are not asserting what the code *should* do — you're asserting what it *does* right now, so the refactor can't change it by accident.

</details>

<details>
<summary>The shape of the composed ReportService after refactor</summary>

```java
public final class ReportService {
    private final HabitRepository habits;
    private final CheckInRepository checkIns;
    private final PointPolicy policy;
    private final Streaks streaks;

    public ReportService(HabitRepository habits, CheckInRepository checkIns,
                         PointPolicy policy, Streaks streaks) {
        this.habits = habits;
        this.checkIns = checkIns;
        this.policy = policy;
        this.streaks = streaks;
    }

    public String report(UUID habitId) {
        Habit habit = habits.findById(habitId)
            .orElseThrow(() -> new IllegalArgumentException("no such habit: " + habitId));
        int points = checkIns.findByHabit(habitId).stream()
            .mapToInt(policy::pointsFor).sum();
        int streak = streaks.longest(completedDates(habitId));
        return """
            Habit: %s
            Points: %d
            Longest streak: %d
            """.formatted(habit.name(), points, streak);
    }
    // ...
}
```

Notice: four interfaces injected, nothing extended, no `System.out`. That's the target.

</details>

## Submission

Commit under `challenges/challenge-01/` in your week-3 GitHub repo. Make sure `mvn test` and the JaCoCo report run clean on a fresh clone. Your git history is part of the grade for this one — atomic commits that tell the story (characterize → refactor in small steps) beat one giant "refactored everything" commit.

## Why this matters

The fluent skill here — *pin behavior, then change structure without changing behavior* — is what separates engineers who can be trusted near production from those who can't. Every framework you meet later (Spring next week, JPA the week after) sits on top of code someone else wrote. The day you're handed a flaky legacy service and asked to "just add one field," the characterization-first reflex you build here is what keeps you from being the person who broke prod.
