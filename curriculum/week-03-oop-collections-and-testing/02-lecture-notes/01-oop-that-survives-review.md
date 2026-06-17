# Lecture 1 — OOP That Survives Review

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can design small types behind interfaces, default to composition over inheritance and explain why, and pick the right collection for a job from its access pattern and Big-O — not from habit.

If you only remember one thing from this lecture, remember this:

> **Program to interfaces, build with composition, and pick the collection from the access pattern.** Three habits. Each one is the kind of thing a senior reviewer flags in a PR when it's missing, and silently approves when it's there. We are going to make all three automatic.

Week 2 gave you the Java *language*. This week is about Java *design* — the difference between code that compiles and code that survives a second engineer touching it six months from now. Crunch Tracker already has a working domain from week 2: records for `Goal`, `Habit`, and `CheckIn`, and one `TrackerService` that creates and queries them. It works. It is also a junior's first draft, and we're going to turn it into something you'd put in a portfolio.

---

## 1. Interfaces are contracts, not boilerplate

A Python developer coming to Java sometimes treats interfaces as Java ceremony — a tax you pay to make the compiler happy. They are the opposite. An interface is the single most valuable design tool Java gives you, because it lets you name a *capability* without naming an *implementation*.

Here is the week-2 service, lightly cleaned up:

```java
public final class TrackerService {
    private final List<Habit> habits = new ArrayList<>();

    public Habit addHabit(String name, int targetPerWeek) {
        var habit = new Habit(UUID.randomUUID(), name, targetPerWeek);
        habits.add(habit);
        return habit;
    }

    public List<Habit> allHabits() {
        return List.copyOf(habits);
    }
}
```

The problem is not that this is wrong. It's that everything is concrete. The storage is a hard-coded `ArrayList`. There is no seam to test against, no seam to swap for a database next week. Compare:

```java
public interface HabitRepository {
    Habit save(Habit habit);
    Optional<Habit> findById(UUID id);
    List<Habit> findAll();
    void deleteById(UUID id);
}
```

That interface says *what* a habit store can do and says *nothing* about how. An in-memory map can implement it. A JPA repository can implement it (week 5). A test fake can implement it. The service depends on the interface; the service no longer cares.

```java
public final class HabitService {
    private final HabitRepository repository;

    public HabitService(HabitRepository repository) {
        this.repository = repository;          // depend on the contract, not a class
    }

    public Habit create(String name, int targetPerWeek) {
        var habit = new Habit(UUID.randomUUID(), name, targetPerWeek);
        return repository.save(habit);
    }
}
```

### "Accept the interface, return the implementation"

This is a phrase you'll hear in review. The principle:

- **Accept** the most general type you can — `Collection<Habit>`, `List<Habit>`, `Iterable<Habit>` — as a *parameter*. The caller has more freedom; your method works with more inputs.
- **Return** a specific, useful type. Returning `List<Habit>` (not `Collection<Habit>`) tells the caller they can index into it.

```java
// Accept the general interface as input...
public int countActive(Collection<Habit> habits) {
    return (int) habits.stream().filter(Habit::isActive).count();
}

// ...return the concrete-enough interface that the caller can use.
public List<Habit> activeHabits(Collection<Habit> habits) {
    return habits.stream().filter(Habit::isActive).toList();
}
```

Note we still return `List`, the *interface*, not `ArrayList`, the *class*. You almost never want a method signature that names `ArrayList`. The implementation is your business; the contract is the caller's.

### `default` methods — adding behavior without breaking implementers

Interfaces can carry `default` method bodies (Java 8+). This is how you add a method to a published interface without breaking every class that already implements it:

```java
public interface HabitRepository {
    Habit save(Habit habit);
    Optional<Habit> findById(UUID id);
    List<Habit> findAll();
    void deleteById(UUID id);

    // Added later. Implementers get this for free; nothing breaks.
    default boolean existsById(UUID id) {
        return findById(id).isPresent();
    }

    default long count() {
        return findAll().size();
    }
}
```

`default` methods are for *convenience built on the abstract methods*, not for sneaking state into an interface (interfaces still cannot hold instance fields). Use them to keep implementers small.

### Sealed interfaces, revisited

You met `sealed` in week 2 for the domain model. It pairs beautifully with the pattern-matching `switch` you also learned. When the set of implementations is *closed and known* — a `CheckIn` is `Completed`, `Skipped`, or `Partial`, and nothing else — seal it:

```java
public sealed interface CheckIn permits Completed, Skipped, Partial {
    UUID habitId();
    LocalDate date();
}

public record Completed(UUID habitId, LocalDate date) implements CheckIn {}
public record Skipped(UUID habitId, LocalDate date, String reason) implements CheckIn {}
public record Partial(UUID habitId, LocalDate date, int percent) implements CheckIn {}
```

Now a `switch` over `CheckIn` is *exhaustive* — the compiler proves you handled every case:

```java
public int points(CheckIn checkIn) {
    return switch (checkIn) {
        case Completed c        -> 10;
        case Partial(_, _, var p) -> p / 10;   // record pattern, deconstructs
        case Skipped s          -> 0;
    };                                          // no default needed; compiler is satisfied
}
```

The rule of thumb: **seal it when the type hierarchy is part of your domain and you want exhaustive handling; leave it open (a plain interface) when third parties should be free to implement it** — like `HabitRepository`, which week 5 implements with JPA.

---

## 2. Composition over inheritance

Now the most-quoted, least-followed principle in object-oriented design. From *Effective Java*, Item 18: **favor composition over inheritance**.

Inheritance (`extends`) is seductive. You have a `Habit` and you want a `WeeklyHabit` that's "a Habit but weekly," so you write `class WeeklyHabit extends Habit`. It feels natural. It is also how codebases rot.

### Why inheritance hurts

Inheritance is **white-box reuse**: the subclass depends on the *internal* implementation of the superclass. When the superclass changes — and it will — subclasses break in ways the compiler can't see. The classic demonstration is a counting set:

```java
// BROKEN: a HashSet subclass that tries to count how many elements were ever added.
public class CountingHashSet<E> extends HashSet<E> {
    private int addedCount = 0;

    @Override public boolean add(E e) {
        addedCount++;
        return super.add(e);
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        addedCount += c.size();
        return super.addAll(c);       // BUG
    }

    public int addedCount() { return addedCount; }
}
```

Add three elements with `addAll` and `addedCount` reports **six**, not three. Why? Because `HashSet.addAll` is implemented by calling `add` internally — so each element is counted twice. The subclass made an assumption about the superclass's *internals* that happened to be false. This is the **fragile base class problem**, and it is invisible until it bites.

### Composition does the same job without the fragility

Hold the collection as a field; delegate to it; add your behavior around the delegation:

```java
// CORRECT: composition. We HOLD a Set, we don't extend it.
public final class CountingSet<E> {
    private final Set<E> delegate = new HashSet<>();
    private int addedCount = 0;

    public boolean add(E e) {
        addedCount++;
        return delegate.add(e);
    }

    public boolean addAll(Collection<? extends E> c) {
        addedCount += c.size();
        return delegate.addAll(c);   // delegate's internal calls are NOT our add()
    }

    public int addedCount() { return addedCount; }
    public boolean contains(E e) { return delegate.contains(e); }
    public int size() { return delegate.size(); }
}
```

Now `addAll` reports three, because `delegate.addAll` calling its own `add` doesn't touch *our* counter. The composed class doesn't depend on the delegate's internals — only its public contract. The delegate can be reimplemented entirely and our class is unaffected.

### The decision rule

Reach for inheritance **only** when *all* of these hold:

1. There is a genuine **"is-a"** relationship (a `Completed` *is a* `CheckIn`), not a "has-a" or "is-implemented-using."
2. The superclass was **designed and documented for extension** (it says so, and its self-use of overridable methods is documented).
3. You are inside the **same codebase / same module** — you control both classes.

In every other case, compose. In Crunch Tracker, `WeeklyHabit` should *not* extend `Habit`; it should be a `Habit` with a `Cadence` field:

```java
public enum Cadence { DAILY, WEEKLY }

public record Habit(UUID id, String name, Cadence cadence, int target) {
    public boolean isActive() { return target > 0; }
}
```

No subclassing. The variation that tempted you toward a subclass is just *data*. This is the single most common refactor you'll do this week: a class hierarchy that wanted to be a field.

### Interfaces + composition = the strategy you'll use all course

Composition shines when the held object is an *interface*. The held thing can be swapped:

```java
public interface PointPolicy {
    int pointsFor(CheckIn checkIn);
}

public final class StandardPointPolicy implements PointPolicy {
    @Override public int pointsFor(CheckIn checkIn) {
        return switch (checkIn) {
            case Completed c -> 10;
            case Partial(_, _, var p) -> p / 10;
            case Skipped s -> 0;
        };
    }
}

public final class StreakService {
    private final PointPolicy policy;        // composed interface — swappable

    public StreakService(PointPolicy policy) { this.policy = policy; }

    public int totalPoints(Collection<CheckIn> checkIns) {
        return checkIns.stream().mapToInt(policy::pointsFor).sum();
    }
}
```

`StreakService` doesn't extend anything. It *has a* `PointPolicy`. Want double points during a challenge week? Pass a different `PointPolicy`. No subclass, no `if`, no recompile of `StreakService`. This is the same shape as Spring's dependency injection next week — you're learning DI before you meet the container.

---

## 3. The Collections Framework

Now the workhorse. The Java Collections Framework is a small set of interfaces and a larger set of implementations. Know the interfaces cold; look up the implementations as needed.

```
                Iterable
                   │
               Collection
        ┌──────────┼───────────┐
       List       Set        Queue
                              │
                            Deque

   Map  (NOT a Collection — it's a separate root)
```

`Map` is deliberately *not* a `Collection`. A map is a set of *entries*, not a collection of values, so it sits on its own. Keep that straight.

### The four questions that pick a collection

Before you type `new ArrayList<>()`, answer:

1. **Do I need order?** Insertion order, sorted order, or no order?
2. **Do I need uniqueness?** Can duplicates exist?
3. **Do I look things up by key, by index, or by scanning?**
4. **Where do I add and remove — ends, front, or middle?**

Your answers map directly onto a type:

| You need… | Use | Why |
|-----------|-----|-----|
| Ordered, indexed, duplicates OK | `List` (→ `ArrayList`) | O(1) indexed get; fast append |
| Uniqueness, don't care about order | `Set` (→ `HashSet`) | O(1) `contains`/`add` |
| Uniqueness + insertion order | `Set` (→ `LinkedHashSet`) | predictable iteration order |
| Uniqueness + sorted order | `Set` (→ `TreeSet`) | O(log n) ops, sorted iteration |
| Key → value lookup | `Map` (→ `HashMap`) | O(1) get/put |
| Key → value, insertion order | `Map` (→ `LinkedHashMap`) | LRU caches, deterministic output |
| Key → value, sorted by key | `Map` (→ `TreeMap`) | range queries, sorted reports |
| FIFO queue or LIFO stack | `Deque` (→ `ArrayDeque`) | O(1) at both ends |

### `ArrayList` vs `LinkedList` — the answer is almost always `ArrayList`

This is the interview question everyone overthinks. The honest 2026 answer:

- **`ArrayList`** is a resizable array. Indexed `get(i)` is O(1). Appending is amortized O(1). It is cache-friendly (contiguous memory), so even operations that are theoretically O(n) are *fast* in practice.
- **`LinkedList`** is a doubly-linked list. Indexed `get(i)` is O(n) — it walks the chain. Its only theoretical wins are O(1) insert/remove *at a known node*, which you rarely have, because you usually have an *index*, and getting to the index is already O(n).

In practice **use `ArrayList` for lists and `ArrayDeque` for queues/stacks.** `LinkedList` is almost never the right answer; the pointer-chasing and per-node allocation lose to the array's cache locality. If a reviewer sees `LinkedList`, expect the question "why not `ArrayList`?" and have a real answer or change it.

```java
// Append-and-iterate? ArrayList.
List<Habit> habits = new ArrayList<>();

// Queue or stack? ArrayDeque — not Stack (legacy, synchronized), not LinkedList.
Deque<UUID> recentlyViewed = new ArrayDeque<>();
recentlyViewed.push(habitId);          // stack semantics
UUID last = recentlyViewed.poll();     // or queue semantics
```

> **Never use `java.util.Stack`.** It extends `Vector`, it's synchronized for no reason you want, and its iteration order is bottom-to-top, which surprises everyone. `ArrayDeque` is the modern stack.

### `HashMap` vs `LinkedHashMap` vs `TreeMap`

```java
// Default. No order guarantee. O(1) get/put. Use unless you need order.
Map<UUID, Habit> byId = new HashMap<>();

// Preserves insertion order. Use when output must be deterministic
// (e.g. you print habits in the order they were created).
Map<UUID, Habit> ordered = new LinkedHashMap<>();

// Sorted by key. O(log n). Use for range queries or sorted reports
// (e.g. "all check-ins between two dates").
NavigableMap<LocalDate, CheckIn> byDate = new TreeMap<>();
SortedMap<LocalDate, CheckIn> may = byDate.subMap(may1, june1);  // range query, free
```

The `Map` interface also gave us, since Java 8, three methods that delete a *lot* of boilerplate:

```java
// "Group check-ins by habit id" — the computeIfAbsent idiom.
Map<UUID, List<CheckIn>> byHabit = new HashMap<>();
for (CheckIn c : checkIns) {
    byHabit.computeIfAbsent(c.habitId(), k -> new ArrayList<>()).add(c);
}

// "Sum points per habit" — the merge idiom.
Map<UUID, Integer> points = new HashMap<>();
for (CheckIn c : checkIns) {
    points.merge(c.habitId(), pointsFor(c), Integer::sum);
}

// "Default if absent" — getOrDefault.
int p = points.getOrDefault(habitId, 0);
```

Learn `computeIfAbsent`, `merge`, and `getOrDefault`. They turn ten lines of null-checking into one.

### Big-O for the operations that matter

You don't need to memorize a table. You need to know, for the operation *your code does in a loop*, whether it's O(1) or O(n):

| Operation | `ArrayList` | `LinkedList` | `HashSet` | `TreeSet` | `HashMap` | `TreeMap` | `ArrayDeque` |
|-----------|:-----------:|:------------:|:---------:|:---------:|:---------:|:---------:|:------------:|
| get by index | O(1) | O(n) | — | — | — | — | — |
| get by key | — | — | — | — | O(1) | O(log n) | — |
| `contains` | O(n) | O(n) | O(1) | O(log n) | O(1) (key) | O(log n) | O(n) |
| add at end | O(1)* | O(1) | O(1) | O(log n) | O(1) | O(log n) | O(1) |
| add at front | O(n) | O(1) | — | — | — | — | O(1) |
| ordered iteration | insertion | insertion | none | sorted | none | sorted | insertion |

\* amortized — the array occasionally doubles, but averaged over many adds it's O(1).

The bug this table prevents: calling `list.contains(x)` inside a loop over `n` items is **O(n²)**. If you're doing membership checks in a loop, you wanted a `HashSet`, which makes the whole thing O(n). This is the single most common collections performance bug in code review, and now you can spot it.

```java
// O(n*m) — quadratic. Bad if both lists are large.
for (Habit h : habits) {
    if (archivedNames.contains(h.name())) { /* ... */ }   // archivedNames is a List
}

// O(n) — fix: make the membership-tested side a Set.
Set<String> archived = new HashSet<>(archivedNames);
for (Habit h : habits) {
    if (archived.contains(h.name())) { /* ... */ }        // O(1) per check
}
```

---

## 4. Immutability and defensive copies

Records are immutable in their *fields*, but a record holding a `List` is only as immutable as that list. This bites people:

```java
public record HabitGroup(String name, List<Habit> habits) {}

var list = new ArrayList<Habit>();
var group = new HabitGroup("Morning", list);
list.add(somethingNew);        // ☠️ the "immutable" record's list just changed
```

The record copied the *reference*, not the list. Fix it with a **compact constructor** that copies defensively:

```java
public record HabitGroup(String name, List<Habit> habits) {
    public HabitGroup {                          // compact canonical constructor
        habits = List.copyOf(habits);            // immutable, defensive copy
    }
}
```

`List.copyOf` returns an unmodifiable list *and* makes a copy, so neither the caller's later mutations nor a caller calling `.add()` on the accessor can corrupt the record. Use `List.of(...)` / `Set.of(...)` / `Map.of(...)` for literals and `List.copyOf(...)` for defensive copies. These collections throw `UnsupportedOperationException` on mutation — which is what you want: loud failure, not silent corruption.

---

## 5. A small worked refactor

Pull it together. The week-2 service queried check-ins with a hand-rolled loop. Here's the same logic, refactored to the principles above — interface dependency, composition, the right collections, immutability, and a touch of Stream:

```java
public interface CheckInRepository {
    CheckIn save(CheckIn checkIn);
    List<CheckIn> findByHabit(UUID habitId);
    List<CheckIn> findAll();
}

public final class StatsService {
    private final CheckInRepository checkIns;   // depend on the interface
    private final PointPolicy policy;           // composed, swappable

    public StatsService(CheckInRepository checkIns, PointPolicy policy) {
        this.checkIns = checkIns;
        this.policy = policy;
    }

    /** Points earned per habit, sorted by points descending. */
    public Map<UUID, Integer> pointsByHabit() {
        return checkIns.findAll().stream()
            .collect(Collectors.groupingBy(
                CheckIn::habitId,
                Collectors.summingInt(policy::pointsFor)));
    }

    /** The longest run of consecutive completed days for one habit. */
    public int longestStreak(UUID habitId) {
        // TreeSet gives us sorted, unique dates for free.
        NavigableSet<LocalDate> done = checkIns.findByHabit(habitId).stream()
            .filter(c -> c instanceof Completed)
            .map(CheckIn::date)
            .collect(Collectors.toCollection(TreeSet::new));

        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate day : done) {                 // iterates in sorted order
            run = (prev != null && day.equals(prev.plusDays(1))) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = day;
        }
        return best;
    }
}
```

Every choice here is deliberate: `CheckInRepository` is an interface (testable, swappable), `PointPolicy` is composed (not inherited), `groupingBy` does the grouping a hand loop used to, and `TreeSet` hands us sorted-unique dates so the streak loop is clean. That's the altitude we're shooting for all week — and exactly what `StreakCalculator` in exercise 3 will ask you to test-drive.

---

## 6. Recap

You should now be able to:

- Define an interface as a contract and depend on it ("accept the interface, return the implementation").
- Add a `default` method to an interface without breaking implementers.
- Explain the fragile base class problem and refactor a subclass into composition + delegation.
- State the three conditions under which inheritance is actually appropriate.
- Pick a collection from access pattern and Big-O, and know why `ArrayList`/`ArrayDeque`/`HashMap` are the sensible defaults.
- Use `computeIfAbsent`, `merge`, and `getOrDefault`.
- Make a record holding a collection genuinely immutable with a defensive copy.

Next we make all of this *provable*. Continue to [Lecture 2 — Testing as Default](./02-testing-as-default.md).

---

## References

- *The Java Tutorials — Interfaces*: <https://dev.java/learn/interfaces/>
- *The Java Tutorials — Collections*: <https://dev.java/learn/api/collections-framework/>
- *Composition over inheritance*: <https://refactoring.guru/design-patterns/composition-over-inheritance>
- *`Object` Javadoc (equals/hashCode contracts)*: <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html>
- *`java.util` package summary*: <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html>
- *Sealed classes and interfaces (JEP 409)*: <https://openjdk.org/jeps/409>
