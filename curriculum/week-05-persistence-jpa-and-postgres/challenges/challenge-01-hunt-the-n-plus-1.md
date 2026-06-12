# Challenge 1 — Hunt the N+1

**Time estimate:** ~90 minutes.

## The scenario

You've joined the Crunch Tracker team mid-sprint. There's a ticket in the board:

> **CRUNCH-412** — *The "habit dashboard" endpoint is slow.* `GET /api/habits/dashboard` returns every habit plus its check-in count and its most recent check-in date. With 12 demo habits it's fine; the QA database has ~400 habits with thousands of check-ins and the endpoint takes **3–6 seconds**. Profiling shows almost no CPU — it's all database round-trips. Figure out why and fix it. **Do not change the JSON the endpoint returns** — a mobile build (Week 9) is already coded against it.

This is a real N+1, planted on purpose. Your job is the full loop: **observe, count, diagnose, fix, prove.**

## The starting code

The endpoint is implemented like this (it's in the challenge starter under `challenges/challenge-01/before/`):

```java
// DashboardService.java
@Service
public class DashboardService {

    private final HabitRepository habits;

    public DashboardService(HabitRepository habits) {
        this.habits = habits;
    }

    @Transactional(readOnly = true)
    public List<HabitDashboardRow> dashboard() {
        return habits.findAll().stream()                       // (1)
            .map(h -> new HabitDashboardRow(
                    h.getId(),
                    h.getName(),
                    h.getCheckIns().size(),                     // (2)
                    h.getCheckIns().stream()                    // (3)
                        .map(CheckIn::getCheckedOn)
                        .max(Comparator.naturalOrder())
                        .orElse(null)))
            .toList();
    }
}
```

```java
public record HabitDashboardRow(UUID id, String name, int checkInCount, LocalDate lastCheckIn) { }
```

```java
// HabitRepository.java — findAll() is inherited from JpaRepository
public interface HabitRepository extends JpaRepository<Habit, UUID> { }
```

The `Habit.checkIns` association is `@OneToMany(mappedBy = "habit")` and **lazy** (correctly so).

## Step 1 — Observe

Turn on SQL logging if it isn't already:

```yaml
logging:
  level:
    org.hibernate.SQL: debug
```

Boot the app with a seeded database (use the provided `R__seed_dashboard_demo.sql` repeatable migration, or write a test that inserts 50 habits each with 10 check-ins). Hit the endpoint and **read the log**. You'll see:

```
Hibernate: select h.* from habits h
Hibernate: select c.* from check_ins c where c.habit_id = ?
Hibernate: select c.* from check_ins c where c.habit_id = ?
... (one per habit) ...
```

## Step 2 — Count

Don't eyeball it — *count* it. Write a test that wraps the call in Hibernate statistics:

```java
Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
stats.setStatisticsEnabled(true);
stats.clear();

dashboardService.dashboard();

long queries = stats.getPrepareStatementCount();
// With 50 habits this will be 51. That's your "before" number.
```

Record the number. For N habits, you should see **N + 1** queries: one for the habit list, one per habit for its check-ins (lines (2) and (3) both touch the lazy collection, but Hibernate loads it once per habit and caches it in the persistence context, so it's N, not 2N).

## Step 3 — Diagnose

Write down, in one or two sentences, *exactly* why. The `findAll()` loads habits with the lazy `checkIns` association uninitialized. The first access to `h.getCheckIns()` per habit triggers a separate `SELECT ... WHERE habit_id = ?`. The query count grows linearly with the number of habits — it does not scale.

## Step 4 — Fix

Choose the right tool and apply it. You have at least three correct options; pick one and justify it:

**Option A — `JOIN FETCH`.** Load habits and their check-ins in one query:

```java
@Query("select distinct h from Habit h left join fetch h.checkIns")
List<Habit> findAllWithCheckIns();
```

Then call `findAllWithCheckIns()` in the service. One query. (Watch the `distinct` — a collection fetch-join duplicates parents.)

**Option B — `@EntityGraph`.** Declarative, composes with pagination:

```java
@EntityGraph(attributePaths = "checkIns")
List<Habit> findAllProjectedBy();
```

**Option C — a constructor projection** (the best answer here, if you're willing to refactor). You don't actually need the entities — you need a count and a max date. Compute them in SQL:

```java
@Query("""
       select new tech.crunch.tracker.habit.HabitDashboardRow(
           h.id, h.name, count(c), max(c.checkedOn))
       from Habit h left join h.checkIns c
       group by h.id, h.name
       """)
List<HabitDashboardRow> dashboardRows();
```

One query, no entities loaded, the aggregation done by Postgres. This is the answer a senior engineer reaches for: the cheapest possible work. If you choose this, the service becomes a one-liner: `return habits.dashboardRows();`.

**The constraint that makes this a challenge:** the returned `HabitDashboardRow` JSON must be **identical** to before. Same fields, same order, same null handling for a habit with zero check-ins (`lastCheckIn` is `null`, `checkInCount` is `0`). Option C must produce `lastCheckIn = null` for a habit with no check-ins — confirm the `left join` and `max` do that (they do: `max` of an empty group is `null`).

## Step 5 — Prove

Re-run your query-count test against the fixed code:

```java
assertThat(stats.getPrepareStatementCount())
    .as("dashboard must be a bounded number of queries, independent of habit count")
    .isEqualTo(1);     // Option A or C: exactly 1. Option B: 1. (Batch-fetch: 2.)
```

Then prove the **contract is unchanged** with a serialization test: build a fixed dataset (3 habits — one with two check-ins, one with one, one with none), call the endpoint through `MockMvc`, and assert the JSON body matches a golden value byte-for-byte, including the `null` lastCheckIn for the empty habit.

## Acceptance criteria

- [ ] A `FINDINGS.md` stating the before count (e.g. "51 queries for 50 habits"), the after count, and which fix you chose and why.
- [ ] The fix is committed as a focused diff under `challenges/challenge-01/`.
- [ ] A query-count test proves the count dropped to a small constant.
- [ ] A `MockMvc` (or equivalent) test proves the JSON response is byte-for-byte unchanged, including the empty-habit edge case.
- [ ] `./mvnw test` is green — full suite, including the Testcontainers integration tests.
- [ ] `docker compose up -d` running; the test hits real Postgres, not H2.

## Stretch

- Seed 5,000 habits and time the before/after with a stopwatch around the service call. Put the two timings in `FINDINGS.md`. (You should see seconds → low double-digit milliseconds.)
- Add a *paginated* version, `GET /api/habits/dashboard?page=0&size=20`. Now discover why a collection `JOIN FETCH` + `LIMIT` paginates **incorrectly** in SQL (Hibernate 6 warns about in-memory pagination), and solve it with either the constructor projection (which paginates fine) or batch fetching. Document what you found.
- Turn on `hibernate.generate_statistics=true` and read the full stats dump (query count, collection fetch count, second-level cache hits). Explain three of the numbers.

## Why this matters

The N+1 is the single most common performance bug in JPA applications, and "the list page is slow" is the single most common performance ticket. The loop you practiced here — turn on SQL logging, count the queries, identify the lazy access, fetch deliberately, re-count, prove the contract held — is a senior-engineer reflex. Every framework that touches a database has its own version of this trap (TanStack Query over-fetching in Week 9 is the frontend cousin). Once you can *count queries on demand* and reason about why each one fires, you stop being surprised by your own database.

## Hints

<details>
<summary>If the JOIN FETCH count is 2, not 1</summary>

Something else is firing a query — often the `count(*)` from a `Page`, or a lazy `@ManyToOne` you forgot to fetch, or Hibernate initializing a second collection. Check the SQL log to see the second statement and fetch it too, or switch to the constructor projection which is unambiguously one query.

</details>

<details>
<summary>If your JSON differs by the empty-habit case</summary>

A habit with no check-ins: with `findAll()` + `.size()`, count is `0` and `max` of an empty stream is `null`. With an *inner* join projection, that habit *disappears* (no rows to join). Use a **`left join`** so habits with zero check-ins still appear, with `count = 0` and `max = null`. That's the subtle correctness bug this challenge is really testing.

</details>
