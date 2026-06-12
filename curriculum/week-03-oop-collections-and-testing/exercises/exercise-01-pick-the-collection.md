# Exercise 1 — Pick the Collection

**Goal:** Stop reaching for `ArrayList` by reflex. For six concrete Crunch Tracker scenarios, choose the right collection *interface* and *implementation*, and justify the choice from the access pattern and Big-O. This is a thinking-and-writing exercise, not a coding one — but every later exercise and the mini-project depend on getting it right.

**Estimated time:** 35 minutes.

---

## How to work this

For each scenario below, write down three things:

1. The **interface** you'd type as the field/return type (`List`, `Set`, `Map`, `Deque`, `NavigableMap`, …).
2. The **implementation** you'd construct (`ArrayList`, `HashMap`, `LinkedHashMap`, `TreeMap`, `ArrayDeque`, …).
3. A **one-sentence justification** naming the access pattern and the operation whose Big-O you care about.

Put your answers in a file `notes/week-03-collections.md` and commit it. The point is to be able to *defend* the choice in review, not to find "the" answer — though most of these have a clearly best answer.

Use the table from Lecture 1 §3 and the four questions: order? uniqueness? lookup by key/index/scan? add/remove where?

---

## The scenarios

**Scenario A — The habit catalog.**
Crunch Tracker holds all of a user's habits. You add habits over time, you iterate them to render a list, and you occasionally remove one by position. Duplicates can't happen because each has a unique id, but you don't look them up *by* id here — you just iterate in the order created.

**Scenario B — Fast lookup by id.**
The service frequently needs "give me the habit with this `UUID`." Thousands of habits; this lookup happens on every check-in. You never need them in any particular order here.

**Scenario C — The set of category tags a user has used.**
Each habit has a category string (`"fitness"`, `"reading"`, …). You want the *distinct* set of categories the user has ever used, to populate a filter dropdown. The dropdown should show them in alphabetical order.

**Scenario D — Recently viewed habits (most-recent first, capped at 10).**
The UI shows the last 10 habits the user opened, newest first. When a habit is opened, it goes to the front; if the list exceeds 10, the oldest falls off the back.

**Scenario E — Check-ins grouped by date, for a calendar view.**
You render a month calendar. For each day you need that day's check-ins, and you iterate days in chronological order. You also do range queries: "all check-ins from the 1st to the 15th."

**Scenario F — Counting points per habit.**
You're tallying total points per habit id across thousands of check-ins, then printing the result. Order doesn't matter for the tally itself; you'll sort for display separately.

---

## The two gotchas

**Gotcha 1 — the quadratic loop.**
A teammate wrote this to filter out archived habits:

```java
List<String> archivedNames = loadArchivedNames();   // could be thousands
List<Habit> visible = new ArrayList<>();
for (Habit h : allHabits) {                          // also thousands
    if (!archivedNames.contains(h.name())) {
        visible.add(h);
    }
}
```

What is the time complexity of this loop, and what one-line change makes it linear? Write the fixed code.

**Gotcha 2 — the silently mutable return.**
A service exposes its internal list:

```java
public List<Habit> habits() {
    return this.habits;     // returns the internal ArrayList directly
}
```

Why is this a bug waiting to happen, and what should the method return instead? Give the corrected one-liner.

---

## Expected outcome

Your `notes/week-03-collections.md` should contain:

- A six-row table (A–F) with interface, implementation, and a one-sentence justification each.
- The Gotcha 1 complexity answer (`O(?)`) and the fixed code.
- The Gotcha 2 explanation and the corrected return statement.

There's no `mvn test` here — this is the design-judgment muscle. Get it right and exercises 2, 3, and the mini-project get noticeably easier.

---

## Self-check (don't peek until you've written your answers)

<details>
<summary>Reveal the intended answers</summary>

- **A — Habit catalog:** `List` → `ArrayList`. Ordered, indexed, append-and-iterate; O(1) get and amortized O(1) append. (If you also needed to remove-by-position a lot, still `ArrayList` — `LinkedList` doesn't help because you have an index.)
- **B — Lookup by id:** `Map<UUID, Habit>` → `HashMap`. The access pattern is key lookup; O(1) `get`. A `List` here would be O(n) per lookup — quadratic across many check-ins.
- **C — Distinct categories, sorted:** `Set<String>` → `TreeSet` (or `SortedSet`). Uniqueness + sorted iteration; O(log n) ops and free alphabetical order. (`HashSet` if you didn't need the ordering.)
- **D — Recently viewed, capped:** `Deque<Habit>` → `ArrayDeque`. Add to front, evict from back; O(1) at both ends. After `push`, if `size() > 10` then `pollLast()`. Never `Stack`, never `LinkedList`.
- **E — Check-ins by date, range queries:** `NavigableMap<LocalDate, List<CheckIn>>` → `TreeMap`. Sorted-by-key iteration plus `subMap(from, to)` range queries for free; O(log n).
- **F — Points per habit:** `Map<UUID, Integer>` → `HashMap`, built with `merge(id, points, Integer::sum)`. O(1) per update; order irrelevant for the tally.

- **Gotcha 1:** It's **O(n·m)** — `List.contains` is O(m) and runs n times. Fix: make the membership-tested side a `Set`. `Set<String> archived = new HashSet<>(archivedNames);` then `if (!archived.contains(h.name()))` — now O(n) overall.

- **Gotcha 2:** Returning the internal list lets any caller mutate your service's private state (`service.habits().clear()`), breaking encapsulation and inviting `ConcurrentModificationException`. Return a copy/unmodifiable view: `return List.copyOf(this.habits);`.

</details>
