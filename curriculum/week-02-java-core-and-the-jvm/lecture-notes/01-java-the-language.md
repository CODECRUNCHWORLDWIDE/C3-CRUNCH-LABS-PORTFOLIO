# Lecture 1 — Java the Language: Types, Records, Sealed Interfaces, and Pattern-Matching switch

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can write Java 21, not Java 8. You can model a domain with records and sealed interfaces, process it with an exhaustive pattern-matching switch, and explain primitives vs objects without hand-waving.

If you only remember one thing from this lecture, remember this:

> **Modern Java is record-first, sealed-where-the-set-is-closed, and pattern-matching-first.** If you write a 40-line class with hand-rolled getters, `equals`, and `hashCode`, you are writing 2010 Java. If you write an `instanceof`-and-cast ladder where a pattern-matching `switch` would do, you are leaving safety on the table. Write 2026 Java from day one.

This lecture covers a lot of surface on purpose: the rest of the course assumes you've seen all of it once. We go deeper on collections (week 3), Spring (week 4+), and the JVM (lecture 2) later.

---

## 1. The four things people call "Java"

Walk into a Java shop and ask "what version of Java are you on?" and you'll get answers that are about different layers of the stack. Sort them out now.

| Layer | Example | What it is |
|------|---------|-----------|
| Language | **Java 21** | The syntax and semantics you type. `javac` reads `.java` files. |
| Bytecode | **`.class` files** | The portable instruction set `javac` emits. Not machine code. |
| Runtime | **the JVM (HotSpot)** | The virtual machine that loads and runs bytecode. JIT + GC live here. |
| Toolchain | **the JDK** | `javac`, `java`, `jar`, `javap`, and the standard library. What you install. |

Lecture 2 lives in the bottom two rows — the JVM and bytecode. This lecture is the top row: the language. But it helps to know up front that **the language and the platform share a version number**. "Java 21" is both the language edition (the syntax `javac` accepts) and the platform release (the JVM, the libraries). That's different from C# (the language) vs .NET (the runtime), which version separately. In Java, they move together.

> **Why Java 21 specifically.** Java ships a feature release every six months (21, then 22, 23, 24…). Every few releases is an **LTS** — Long-Term Support — that gets years of patches: Java 8, 11, 17, 21, and 25. Teams stand on LTS releases. In 2026, **Java 21 is the dominant production baseline**: new enough to have records, sealed types, and pattern matching finalized; old enough that every tool, library, and cloud runtime supports it. (Java 25 LTS shipped in late 2025 and adoption is climbing, but 21 is still where the bulk of production lives, and everything this week is identical on 25.) When this course says "Java," it means Java 21.

You installed the JDK in week 1 via SDKMAN. Confirm:

```bash
java -version
```

You want to see something like:

```
openjdk version "21.0.x" 2026-xx-xx
OpenJDK Runtime Environment Temurin-21.0.x (build 21.0.x+y)
OpenJDK 64-Bit Server VM Temurin-21.0.x (build 21.0.x+y, mixed mode, sharing)
```

If that says `17` or `11`, stop and fix it: `sdk use java 21.0.x-tem`. Every example this week assumes 21.

---

## 2. Primitives vs objects — the distinction that bites Python developers

Java has two kinds of types, and the line between them is sharp in a way Python and JavaScript don't have.

**Primitives** are not objects. They are raw values: `int`, `long`, `short`, `byte`, `double`, `float`, `boolean`, `char`. There are exactly eight. They have no methods, they can't be `null`, and they live directly where they're declared (on the stack for a local, inline in an object for a field — lecture 2 covers where).

```java
int count = 42;          // a 32-bit integer value, not an object
double rate = 0.05;      // a 64-bit floating point value
boolean done = false;    // true or false, never null
```

**Reference types** are everything else: every class, every array, every `record`, every interface. A variable of a reference type holds a *reference* (a pointer) to an object on the heap, or `null`.

```java
String name = "Crunch";  // a reference to a String object
Integer boxed = 42;      // a reference to an Integer object wrapping the value 42
```

Every primitive has a **boxed** counterpart — an object form:

| Primitive | Boxed type |
|-----------|-----------|
| `int` | `Integer` |
| `long` | `Long` |
| `double` | `Double` |
| `boolean` | `Boolean` |
| `char` | `Character` |
| `byte` | `Byte` |
| `short` | `Short` |
| `float` | `Float` |

**Autoboxing** converts between them implicitly:

```java
Integer boxed = 42;      // autoboxing: int 42 → Integer
int unboxed = boxed;     // auto-unboxing: Integer → int 42
```

Why do boxed types exist? Because generics and collections only work with objects. You can't write `List<int>` — you must write `List<Integer>`. (Project Valhalla is working on changing this, but it isn't here yet in 2026.) So whenever a value needs to live in a `List`, a `Map`, or an `Optional`, it gets boxed.

### The `Integer` cache trap

Here is the single most important gotcha. **Never compare boxed types with `==`.**

```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b);   // true  (?!)

Integer c = 128;
Integer d = 128;
System.out.println(c == d);   // false (?!?!)
```

`==` on reference types compares *references*, not values. The JDK caches small `Integer` objects (−128 to 127 by default), so `a` and `b` are the *same cached object* and `==` is `true`. But `128` is outside the cache, so `c` and `d` are two different objects, and `==` is `false`. This is a real, recurring production bug.

**The rule: compare objects with `.equals()`, compare primitives with `==`.**

```java
Integer a = 128, b = 128;
System.out.println(a.equals(b));   // true — correct
System.out.println(a.intValue() == b.intValue());  // true — also correct (unboxed)
```

For our Crunch Tracker domain, the lesson is concrete: a goal's `id` should be a primitive `long` (or a value object), and you compare two of them with `==` when they're primitives, `.equals()` when they're boxed or wrapped. We'll lean on records to make `.equals()` correct for free in section 4.

---

## 3. `var` — local type inference, used with judgment

Since Java 10 you can declare a local variable with `var` and let the compiler infer the type:

```java
var count = 42;                          // int
var name = "Crunch";                     // String
var goals = new ArrayList<Goal>();       // ArrayList<Goal>
```

`var` is **static typing with inference**, not dynamic typing. `count` is still an `int` forever; you cannot reassign a `String` to it. It's purely a way to avoid repeating a type the reader can already see.

Use `var` when the right-hand side makes the type obvious:

```java
var checkIns = new HashMap<LocalDate, CheckIn>();   // good — type is right there
```

Avoid `var` when it hides the type the reader needs:

```java
var result = service.process(input);     // bad — what type is result? unclear
List<CheckIn> result = service.process(input);  // better — say it
```

**C3 standard: `var` for `new X(...)` and obvious literals; explicit types when the right-hand side is a method call whose return type isn't obvious.** Readability is the only criterion. `var` is not "more modern," it's a tool with a right and wrong place.

---

## 4. Records — the default way to model data

Before records (Java 16), an immutable data type was pages of ceremony. A `Goal` with three fields meant: private final fields, a constructor, three getters, an `equals`, a `hashCode`, a `toString`. Forty lines, every one a chance for a bug (forget a field in `equals` and `HashSet` breaks silently).

With a **record**, the same type is one line:

```java
public record Goal(long id, String title, int targetPerWeek) {}
```

That single declaration gives you:

- Three `private final` fields: `id`, `title`, `targetPerWeek`.
- A **canonical constructor**: `new Goal(1L, "Read daily", 7)`.
- Three **accessors**, named after the components: `goal.id()`, `goal.title()`, `goal.targetPerWeek()`. (Note: `id()`, not `getId()` — records use the component name.)
- A correct, value-based **`equals`** and **`hashCode`** — two goals with equal components are `.equals()`, and they hash to the same bucket. This is the whole point: you can put records in a `HashSet` and it just works.
- A useful **`toString`**: `Goal[id=1, title=Read daily, targetPerWeek=7]`.

Records are **immutable**. There are no setters. To "change" a field, you build a new record:

```java
Goal g = new Goal(1L, "Read daily", 7);
Goal updated = new Goal(g.id(), g.title(), 5);   // a new Goal, same id and title, new target
```

Java doesn't have C#'s `with` expression (yet — a `withers` JEP is in progress), so you call the constructor. A common pattern is a small "wither" method on the record:

```java
public record Goal(long id, String title, int targetPerWeek) {
    public Goal withTargetPerWeek(int newTarget) {
        return new Goal(id, title, newTarget);
    }
}
```

### Compact constructors and validation

You can validate in a **compact constructor** — the canonical constructor without the parameter list:

```java
public record Goal(long id, String title, int targetPerWeek) {
    public Goal {                                 // compact constructor — no (...) here
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title must not be blank");
        if (targetPerWeek < 1 || targetPerWeek > 7)
            throw new IllegalArgumentException("targetPerWeek must be 1..7");
        title = title.strip();                    // you can normalize, too
    }
}
```

The compact constructor runs before the fields are assigned. You validate, you may reassign the parameters to normalize them, and then the compiler assigns them to the fields for you. This is where invariants live: a `Goal` that exists is, by construction, valid. You never have a half-built `Goal` floating around.

### Records can have behavior

A record isn't limited to data. It can have extra methods, static factories, and implement interfaces:

```java
public record Habit(long id, String name, int currentStreak) {
    public boolean isOnFire() {
        return currentStreak >= 7;
    }
    public static Habit start(long id, String name) {
        return new Habit(id, name, 0);
    }
}
```

What a record **cannot** do: extend another class (it implicitly extends `java.lang.Record`), have mutable fields, or have non-final instance fields. If you need inheritance or mutable state, a record is the wrong tool — use a class.

> **Use records by default for domain data.** If your type is mostly data with little behavior — a `Goal`, a `CheckIn`, a `Money` amount, a DTO in week 4 — it should be a record. If it has identity beyond its values and significant mutable behavior — a service, a repository, a connection — it should be a class. A plain class with private fields and public getters/setters is almost never the right answer in 2026; usually you wanted a record and didn't know the keyword existed.

---

## 5. Enums — a closed set of constants with behavior

An `enum` is a type with a fixed set of instances. Use it whenever a value is one of a small, known set:

```java
public enum CheckInResult { DONE, SKIPPED, PARTIAL }
```

Enums in Java are real objects — they can have fields, constructors, and methods:

```java
public enum Cadence {
    DAILY(1), WEEKLY(7), MONTHLY(30);

    private final int days;
    Cadence(int days) { this.days = days; }
    public int days() { return days; }
}

int d = Cadence.WEEKLY.days();   // 7
```

Enums matter for the next section because, like sealed types, they represent a *closed set* the compiler can reason about. A `switch` over an enum that covers every constant needs no `default` — the compiler knows there are no other cases.

---

## 6. Sealed interfaces and classes — closing the hierarchy

Sometimes a type genuinely has a fixed set of subtypes, and you want the compiler to *know* that. A `CheckIn` event in our tracker is one of: a completion, a skip, or a partial. A payment is one of cash, card, or transfer. There is no fourth kind, and if someone adds one, you want every place that processes these to stop compiling until it's handled.

That's what `sealed` is for. A sealed type declares exactly which types may extend or implement it:

```java
public sealed interface CheckInEvent
        permits Completed, Skipped, Partial {}

public record Completed(long habitId, LocalDate date) implements CheckInEvent {}
public record Skipped(long habitId, LocalDate date, String reason) implements CheckInEvent {}
public record Partial(long habitId, LocalDate date, int amountDone, int target) implements CheckInEvent {}
```

The `permits` clause lists the *only* types allowed to implement `CheckInEvent`. Try to write a fourth implementer in another file and `javac` rejects it. The set is closed, on purpose, and the compiler enforces it.

(If all permitted subtypes live in the same file, you can omit `permits` — the compiler infers it. Records are the natural partners for sealed interfaces: small, immutable, and they implement the interface in one line each.)

Why is this powerful? Because once the compiler knows the *complete* list of subtypes, it can verify that a `switch` over them handles every case. That's the next section, and it's the payoff for the whole hierarchy.

---

## 7. Pattern matching — the feature of the week

Java has, as of 21, a genuinely good pattern-matching system. Three pieces, building on each other.

### `instanceof` patterns (Java 16)

The old way to test-and-cast:

```java
// 2010 Java — don't write this
if (event instanceof Completed) {
    Completed c = (Completed) event;   // redundant cast
    System.out.println(c.habitId());
}
```

The modern way binds the variable in the test:

```java
// 2026 Java
if (event instanceof Completed c) {
    System.out.println(c.habitId());   // c is in scope, already the right type
}
```

`instanceof Completed c` both checks the type *and* introduces `c`, already cast. If you ever write `instanceof` followed by a cast on the next line, you've written it the old way — fix it.

### The pattern-matching `switch` (finalized Java 21, JEP 441)

Here's the centerpiece. Process a sealed hierarchy with a `switch` that returns a value:

```java
public String describe(CheckInEvent event) {
    return switch (event) {
        case Completed c            -> "done: habit " + c.habitId();
        case Skipped s              -> "skipped: " + s.reason();
        case Partial p              -> "partial: " + p.amountDone() + "/" + p.target();
    };
}
```

Read it carefully. Several things are happening that the old `switch` couldn't do:

1. **It's an expression.** `switch (...) { ... }` *returns a value* (note the `->` arrows and no `break`). You assign it, return it, pass it.
2. **It matches on type.** `case Completed c` matches when `event` is a `Completed` and binds it to `c`.
3. **It is exhaustive — and the compiler proves it.** Because `CheckInEvent` is `sealed` and we covered all three permitted subtypes, **we need no `default` branch**. If a teammate adds a fourth subtype to the sealed interface, *this switch stops compiling* with "switch does not cover all possible input values." The compiler hands you a checklist of every place that needs updating. This is the single best argument for sealed types.

Compare that to the `instanceof` ladder it replaces:

```java
// the bad old way — no exhaustiveness, easy to forget a case
public String describe(CheckInEvent event) {
    if (event instanceof Completed c) return "done: habit " + c.habitId();
    else if (event instanceof Skipped s) return "skipped: " + s.reason();
    else if (event instanceof Partial p) return "partial: " + p.amountDone() + "/" + p.target();
    else throw new IllegalStateException("unhandled: " + event);  // runtime failure, not compile-time
}
```

The ladder throws *at run time* when you forget a case. The switch fails *at compile time*. That's the whole game: move bugs from production to your editor.

### Record deconstruction patterns (Java 21, JEP 440)

You can destructure a record right in the pattern, binding its components directly:

```java
public String describe(CheckInEvent event) {
    return switch (event) {
        case Completed(long id, var date)           -> "done: habit " + id + " on " + date;
        case Skipped(var id, var date, var reason)  -> "skipped: " + reason;
        case Partial(var id, var date, var done, var target) when done == 0
                                                    -> "nothing done";
        case Partial(var id, var date, var done, var target)
                                                    -> "partial: " + done + "/" + target;
    };
}
```

Two new things:

- **Deconstruction:** `case Completed(long id, var date)` pulls the record apart into its components. No `.habitId()` calls — the names are right there in the pattern.
- **Guards (`when`):** `case Partial(...) when done == 0` matches only when the guard is also true. Guards let you split one type into multiple cases by a condition. The compiler still checks exhaustiveness: because the guarded `Partial` case isn't total, you must (and we do) provide an unguarded `Partial` case to cover the rest.

> **C3 coding standard: prefer pattern-matching `switch` to `if`/`else if` chains when computing a value from a type, and prefer sealed hierarchies so the switch is exhaustive without a `default`.** A `default` that throws is a code smell on a sealed type — it means you've thrown away the compiler's exhaustiveness check. Reach for it only when matching on a genuinely open type (like `Object`).

---

## 8. Putting it together — the Crunch Tracker domain, in miniature

Here is a single self-contained example that uses every idea from this lecture: records, a sealed interface, an enum, a compact constructor with validation, and an exhaustive pattern-matching switch with deconstruction and a guard.

```java
import java.time.LocalDate;
import java.util.List;

public class TrackerDemo {

    // --- domain: a closed set of check-in events ---
    sealed interface CheckInEvent permits Completed, Skipped, Partial {}

    record Completed(long habitId, LocalDate date) implements CheckInEvent {}
    record Skipped(long habitId, LocalDate date, String reason) implements CheckInEvent {}
    record Partial(long habitId, LocalDate date, int done, int target) implements CheckInEvent {
        Partial {                                  // compact constructor: validate
            if (done < 0 || target <= 0 || done > target)
                throw new IllegalArgumentException("0 <= done <= target, target > 0");
        }
    }

    enum Score { FULL, NONE, PARTIAL }

    // --- behavior: one exhaustive switch, no default, no instanceof ---
    static Score score(CheckInEvent event) {
        return switch (event) {
            case Completed c                              -> Score.FULL;
            case Skipped s                               -> Score.NONE;
            case Partial(var id, var date, var d, var t) when d == 0 -> Score.NONE;
            case Partial p                               -> Score.PARTIAL;
        };
    }

    public static void main(String[] args) {
        List<CheckInEvent> log = List.of(
            new Completed(1L, LocalDate.parse("2026-06-10")),
            new Skipped(2L, LocalDate.parse("2026-06-10"), "sick"),
            new Partial(3L, LocalDate.parse("2026-06-10"), 2, 5),
            new Partial(4L, LocalDate.parse("2026-06-10"), 0, 5)
        );

        for (CheckInEvent e : log) {
            System.out.println(e + "  ->  " + score(e));
        }
        // Completed[...]  ->  FULL
        // Skipped[...]    ->  NONE
        // Partial[...d=2] ->  PARTIAL
        // Partial[...d=0] ->  NONE
    }
}
```

Read it slowly. In about thirty lines you have a validated, immutable domain and a total function over it. **No mutable state. No null checks. No `instanceof` ladder. No `default` that throws.** Add a fourth `CheckInEvent` and `score` won't compile until you handle it. That is the modern-Java baseline, and the mini-project this week extends this exact pattern into the real Crunch Tracker model.

---

## 9. A glance at what's *not* in this lecture

- **Collections** (`List`, `Map`, `Set`, the right one for the job) — that's week 3.
- **Interfaces and composition over inheritance as a design discipline** — also week 3. This week we use interfaces only as sealed-hierarchy roots.
- **Streams and lambdas** — we use a touch of `List.of` and a `for` loop this week; week 3 brings the Stream API.
- **Generics in depth** — we use `List<Goal>` and `Optional<Goal>` but don't write our own generic types yet.
- **Spring, JPA, anything web** — weeks 4 and beyond. This week is pure Java, no framework.

---

## 10. Recap

You should now be able to:

- Name the four things called "Java" (language, bytecode, JVM, JDK) and say which a sentence is about.
- Explain primitives vs objects, autoboxing, and why `==` on `Integer` is a trap.
- Use `var` where it aids readability and not where it hides intent.
- Model immutable data with records, including compact-constructor validation and small behavior methods.
- Close a hierarchy with a sealed interface and records.
- Write an exhaustive pattern-matching `switch` — with deconstruction and guards — that the compiler verifies, with no `default` and no `instanceof` ladder.

Next, we drop one level down: what the JVM does with all of this. Continue to [Lecture 2 — What the JVM Actually Does](./02-what-the-jvm-actually-does.md).

---

## References

- *JEP 441: Pattern Matching for switch*: <https://openjdk.org/jeps/441>
- *JEP 440: Record Patterns*: <https://openjdk.org/jeps/440>
- *JEP 395: Records*: <https://openjdk.org/jeps/395>
- *JEP 409: Sealed Classes*: <https://openjdk.org/jeps/409>
- *Records reference — JDK 21*: <https://docs.oracle.com/en/java/javase/21/language/records.html>
- *Sealed classes reference — JDK 21*: <https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html>
- *Autoboxing and the Integer cache* — Java Language Specification §5.1.7: <https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.7>
