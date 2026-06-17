# Week 3 — Quiz

Ten multiple-choice questions. Take it with your lecture notes closed. Aim for 9/10 before moving to Week 4. Answer key at the bottom — don't peek.

---

**Q1.** "Favor composition over inheritance" is advice to prefer which of the following?

- A) Copying the parent class's code into the child to avoid the dependency.
- B) Building behavior by *holding* another object as a field and delegating to it, rather than `extends`-ing it.
- C) Making every class `final` so it can never be subclassed.
- D) Using interfaces only, never classes.

---

**Q2.** What is the "fragile base class problem"?

- A) Base classes throw exceptions more often than subclasses.
- B) A subclass can break when the superclass changes its internal implementation, because the subclass depended on those internals.
- C) Abstract classes cannot be instantiated.
- D) `final` classes cannot be extended, which makes them fragile.

---

**Q3.** You're choosing a collection to answer "give me the habit with this `UUID`," called on every check-in, with no ordering requirement. The right choice is:

- A) `ArrayList<Habit>`, scanning for the matching id.
- B) `LinkedList<Habit>`, for fast removal.
- C) `HashMap<UUID, Habit>`, for O(1) lookup by key.
- D) `TreeSet<Habit>`, for sorted access.

---

**Q4.** Why is `ArrayList` almost always preferred over `LinkedList` in modern Java?

- A) `LinkedList` cannot store `null`.
- B) `ArrayList` is thread-safe and `LinkedList` is not.
- C) `ArrayList` has O(1) indexed access and contiguous, cache-friendly memory; `LinkedList`'s indexed access is O(n) and its pointer-chasing loses to the array in practice.
- D) `LinkedList` was removed from the JDK in Java 17.

---

**Q5.** A `Tag` class overrides `equals()` to compare names case-insensitively but does **not** override `hashCode()`. What happens when you put two equal tags (`"Fitness"` and `"fitness"`) into a `HashSet`?

- A) The set throws an exception at the second `add`.
- B) The set correctly keeps only one — `equals` is all that matters.
- C) The set may keep both, because the two equal objects get different hash codes and land in different buckets, violating the equals/hashCode contract.
- D) The code does not compile.

---

**Q6.** Which collection is the modern, recommended choice for a LIFO stack or a FIFO queue?

- A) `java.util.Stack`
- B) `LinkedList`
- C) `ArrayDeque`
- D) `PriorityQueue`

---

**Q7.** In the TDD red-green-refactor loop, what does the "red" step require?

- A) Refactoring the code until the tests are clean.
- B) Writing a test that *fails* for the next small behavior — and watching it fail — before writing the code.
- C) Deleting any test that currently passes.
- D) Running the code in production and watching for errors.

---

**Q8.** Given this AssertJ assertion, what is it checking?

```java
assertThat(habits)
    .extracting(Habit::name)
    .containsExactly("Read", "Run", "Meditate");
```

- A) That `habits` contains exactly three elements whose `name` values are `"Read"`, `"Run"`, `"Meditate"`, **in that order**.
- B) That `habits` contains those three names in any order.
- C) That every habit's name is one of those three strings.
- D) That the `habits` list is sorted alphabetically by name.

---

**Q9.** When is reaching for Mockito the **wrong** choice?

- A) To stub a not-yet-implemented payment gateway at a system boundary.
- B) To verify that a service called `repository.save(...)` exactly once.
- C) To "mock" a `LocalDate` or a record so you don't have to construct a real one.
- D) To stub a clock so a time-dependent test is deterministic.

---

**Q10.** Why does this method, exposed by a service, invite bugs?

```java
public List<Habit> habits() {
    return this.habits;   // returns the internal ArrayList directly
}
```

- A) It can't compile because `habits` is private.
- B) Returning the internal list lets any caller mutate the service's private state and risks `ConcurrentModificationException`; it should return `List.copyOf(this.habits)` or an unmodifiable view.
- C) `List` is an interface and can't be returned.
- D) `ArrayList` is not serializable, so returning it is unsafe.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — Composition means *has-a*: hold the other object and delegate to it, adding your behavior around the delegation. It avoids the white-box coupling that `extends` creates.
2. **B** — Inheritance is white-box reuse: the subclass depends on the superclass's *internal* implementation (e.g. `HashSet.addAll` calling `add`). When those internals change, the subclass breaks invisibly. That's the fragile base class problem.
3. **C** — The access pattern is key lookup, so a `Map` keyed by `UUID` gives O(1) `get`. A `List` scan (A) is O(n) per lookup — quadratic across many check-ins.
4. **C** — `ArrayList`'s contiguous memory and O(1) indexed access beat `LinkedList`'s O(n) indexing and per-node pointer-chasing in essentially all real workloads. `LinkedList` is almost never the right answer.
5. **C** — Breaking the equals/hashCode contract doesn't throw; it silently corrupts hash-based collections. Two "equal" objects with different hash codes go to different buckets, so the set never notices the duplicate. (The fix: override `hashCode` consistently — or use a record.)
6. **C** — `ArrayDeque` is O(1) at both ends and is the modern stack/queue. `java.util.Stack` is legacy (extends `Vector`, needlessly synchronized, surprising iteration order); `LinkedList` loses on cache locality.
7. **B** — Red means writing a failing test *first* and confirming it fails. A test that passes before you write the code is testing nothing; watching it fail proves the test can actually detect the behavior.
8. **A** — `extracting(Habit::name)` projects each habit to its name, and `containsExactly(...)` requires those values in the **given order**. (`containsExactlyInAnyOrder` is the order-independent variant.)
9. **C** — Don't mock value objects (records, `String`, `LocalDate`) — just construct real ones; mocking them is a smell. A, B, and D are all legitimate uses (boundary stub, interaction verification, deterministic clock).
10. **B** — Handing out the internal list breaks encapsulation: callers can mutate or clear your private state, and concurrent iteration risks `ConcurrentModificationException`. Return an unmodifiable copy (`List.copyOf`) instead.

</details>

---

If you scored under 7, re-read the lectures for the questions you missed. If you scored 9 or 10, you're ready for the [homework](./06-homework.md).
