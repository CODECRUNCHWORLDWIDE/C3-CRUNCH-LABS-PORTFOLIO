# Week 2 — Quiz

Ten multiple-choice questions. Take it with your lecture notes closed. Aim for 9/10 before moving to week 3. Answer key at the bottom — don't peek.

---

**Q1.** Which best describes the relationship between "Java 21," bytecode, the JVM, and the JDK?

- A) They are four names for the same thing, shipped together for marketing reasons.
- B) Java 21 is the language/platform version, bytecode is what `javac` emits into `.class` files, the JVM is the runtime that executes that bytecode, and the JDK is the toolchain you install (`javac`, `java`, `javap`, libraries).
- C) The JVM compiles `.java` files directly to machine code; bytecode is only used for debugging.
- D) The JDK is the runtime and the JVM is the compiler.

---

**Q2.** Given:

```java
Integer a = 1000;
Integer b = 1000;
System.out.println(a == b);
```

What does this print, and why?

- A) `true` — `Integer` always compares by value.
- B) `false` — `==` on boxed `Integer`s compares references, and `1000` is outside the cached range, so `a` and `b` are different objects.
- C) `true` — small integers are cached, and `1000` is in the cache.
- D) It does not compile; you cannot use `==` on `Integer`.

---

**Q3.** You write `public record Goal(long id, String title, int targetPerWeek) {}`. Which of these does the compiler **not** generate for you?

- A) A canonical constructor `Goal(long, String, int)`.
- B) Accessor methods `id()`, `title()`, `targetPerWeek()`.
- C) A value-based `equals` and `hashCode`.
- D) Setter methods `setId(long)`, `setTitle(String)`, etc.

---

**Q4.** What is the purpose of a **compact constructor** in a record?

- A) To make the record smaller in memory.
- B) To validate and/or normalize the components before they're assigned to the fields, so an existing record is always valid by construction.
- C) To allow the record to extend another class.
- D) To generate the accessors; without it, a record has no accessors.

---

**Q5.** You have:

```java
sealed interface CheckIn permits Completed, Skipped, Partial {}
```

and a switch over a `CheckIn` that handles `Completed`, `Skipped`, and `Partial`. Why does this switch need **no `default`** branch?

- A) Because `switch` expressions never need a `default`.
- B) Because the interface is `sealed`, the compiler knows the *complete* set of permitted subtypes; covering all of them makes the switch provably exhaustive.
- C) Because records cannot appear in a `default` branch.
- D) It does need a `default`; the code won't compile without one.

---

**Q6.** A teammate adds a fourth variant, `record Excused(...) implements CheckIn {}`, to the `permits` list. What happens to your existing exhaustive switch that didn't handle `Excused`?

- A) Nothing; it silently falls through and returns `null`.
- B) It throws a `RuntimeException` the first time an `Excused` reaches it, at run time.
- C) It **fails to compile** with an error that the switch doesn't cover all possible input values — pointing you at the place to fix.
- D) The `Excused` variant is ignored because it was added later.

---

**Q7.** Which is the correct modern (Java 16+) way to test-and-cast, and why?

- A) `if (e instanceof Completed) { Completed c = (Completed) e; ... }` — the explicit cast is required.
- B) `if (e instanceof Completed c) { ... }` — the `instanceof` pattern tests the type *and* binds `c`, already cast, with no redundant cast line.
- C) `Completed c = (Completed) e;` then check for `ClassCastException`.
- D) `if (e.getClass() == Completed.class) { ... }` — comparing classes is the idiomatic way.

---

**Q8.** When should you return `Optional<T>` instead of throwing an exception?

- A) Always — exceptions are deprecated in modern Java.
- B) When absence is a **normal, expected outcome** the caller must handle (e.g. "find a habit by id; it might not exist"). Throw instead when absence means a **broken invariant or programming error**.
- C) Only inside `catch` blocks.
- D) Never; `Optional` is only for stream pipelines.

---

**Q9.** Where do a method's **local variables** and a newly-`new`-ed **object** live, respectively?

- A) Both on the heap.
- B) Both on the stack.
- C) Local variables (and the reference holding the object) live in the method's frame on the **stack**; the object itself lives on the **heap**.
- D) Local variables on the heap; objects in the metaspace.

---

**Q10.** A Java program runs the same hot method millions of times and gets faster after the first few thousand calls. What's happening?

- A) The garbage collector is freeing memory, which speeds up the method.
- B) The JVM starts by interpreting bytecode, then the JIT (C1, then C2) compiles the hot method to optimized native machine code — this is "warm-up."
- C) The operating system caches the `.java` source file.
- D) The method is being inlined into the `.class` file by `javac` at build time.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — Language/platform, bytecode, runtime, toolchain are four distinct layers that happen to share a version. (C is wrong: `javac` emits bytecode, not machine code; the JIT makes machine code at *run* time. D inverts JDK and JVM.)
2. **B** — `==` on reference types compares object identity. The `Integer` cache covers −128..127; `1000` is outside it, so the two autoboxed `Integer`s are distinct objects. Compare with `.equals()`, never `==`, for boxed types.
3. **D** — Records are **immutable**; there are no setters. They *do* generate the canonical constructor, the accessors (named `id()`, not `getId()`), and value-based `equals`/`hashCode`/`toString`.
4. **B** — The compact constructor is where you validate and normalize before assignment, so the invariant holds for every instance that exists. (It can't enable inheritance — records can't extend classes — and it doesn't generate accessors.)
5. **B** — `sealed` gives the compiler the complete subtype list, enabling a provable exhaustiveness check. Covering every permitted subtype means no `default` is needed (or wanted).
6. **C** — This is the entire payoff of sealed types: the switch fails to compile, turning a would-be runtime bug into a compile-time checklist. (A and B describe what an `instanceof` ladder or a non-sealed `default` would do — the failure modes sealed types prevent.)
7. **B** — The `instanceof` pattern (`instanceof Completed c`) tests and binds in one step, with no redundant cast. A works but is the old, verbose form. D is fragile and doesn't bind.
8. **B** — `Optional` for expected-and-handled absence; exception for broken-invariant/programming-error absence. The deliberate choice between them is the lesson of exercise 3.
9. **C** — Locals and references live in the per-call frame on the thread's stack; the `new`-ed object lives on the shared, GC-managed heap. (Lecture 2, §4.)
10. **B** — Tiered JIT compilation: interpret → C1 → C2. The method gets native-compiled once it's hot. This is why benchmarks must warm up before timing.

</details>

---

If you scored under 7, re-read the lectures for the questions you missed — most misses cluster on either the boxing/`==` trap (Q2) or exhaustiveness (Q5/Q6). If you scored 9 or 10, you're ready to dive into the [homework](./homework.md).
