# Week 2 — Java Core and the JVM

Welcome to week 2 of **C3 · Crunch Labs Portfolio**. Week 1 set the working agreements — Scrum, Git, the JDK 21 toolchain, a green CI build. This week we write the language.

This is the language tour for engineers who already know how to program. The C1 graduate is the target: comfortable with functions, classes, exceptions, and collections in Python or JavaScript. If that's you, this week is not "learn to program in Java." It's "learn the Java 21 dialect and what the JVM is actually doing underneath it." We move fast, and we write **Java 21, not Java 8**.

That distinction matters more in Java than in almost any other language. Java has eleven years of accumulated tutorials, Stack Overflow answers, and enterprise codebases written for Java 6, 7, and 8. A huge amount of "how to do X in Java" advice you'll find online is correct *and* obsolete: it works, it compiles, and it is two language generations behind what a 2026 team ships. You will write a `record` where a 2010 tutorial writes a 40-line POJO with hand-rolled getters, `equals`, `hashCode`, and `toString`. You will write a pattern-matching `switch` where a 2010 tutorial writes an `instanceof`-and-cast ladder. The whole week is about building the reflex to reach for the modern idiom first.

The second half of the week goes one level down: **what the JVM actually does**. Where your objects live (heap vs stack), what `javac` produces (bytecode, not machine code), what the JIT does at run time, and how garbage collection reclaims memory without you ever calling `free()`. You don't need this to write a `record`. You need it because weeks 5 through 10 will hand you a slow endpoint, an `OutOfMemoryError`, or an N+1 query, and "the JVM is a black box" is not a debugging strategy.

## Learning objectives

By the end of this week, you will be able to:

- **Distinguish** the four things people call "Java": the language, the bytecode, the JVM (the runtime), and the JDK (the toolchain) — and say which one a given sentence is about.
- **Read** the difference between a primitive (`int`, `long`, `double`, `boolean`) and its boxed object form (`Integer`, `Long`, `Double`, `Boolean`), and explain where each lives and why `==` on two `Integer`s is a trap.
- **Model** small domains with `record` types — immutable, with compiler-generated constructor, accessors, `equals`, `hashCode`, and `toString` — and know when a record is the wrong tool.
- **Design** closed type hierarchies with `sealed` interfaces and records, so the compiler knows every possible case.
- **Process** those hierarchies with a **pattern-matching `switch`** that the compiler verifies is exhaustive — no `instanceof` chains, no `default: throw`.
- **Choose** between an exception and an `Optional<T>` to represent "no value," and justify the choice.
- **Trace** a `.java` file through `javac` to bytecode to JIT-compiled machine code, and read a `javap` disassembly at a high level.
- **Explain** the heap/stack split, object allocation, references, and what a generational garbage collector does — well enough to reason about an `OutOfMemoryError` later.
- **Build and run** a pure-Java program with Maven, no framework, from the terminal.

## Prerequisites

This week assumes you completed **week 1 — Agile, Git, and the JDK Toolchain**, or have the equivalent:

- JDK 21 installed and on your `PATH`. Verify with `java -version` — it must print `21`. We use SDKMAN to manage this (week 1); if `java -version` shows 17 or 11, switch with `sdk use java 21.0.x-tem`.
- A Maven project skeleton you can build with `mvn compile` and `mvn test`.
- Comfort in a terminal: `cd`, run a command, read an error and a stack trace.
- Basic Git: `branch`, `add`, `commit`, `push`, open a PR.
- You've written and tested a small program end-to-end in *some* language.

You do **not** need any prior Java. We start at the type system. If you learned older Java (pre-records, pre-`var`, pre-pattern-matching), you'll need to unlearn a few habits, and we flag them as we go.

## Topics covered

- The four things people call "Java": the language (Java 21), the bytecode (`.class` files), the JVM (HotSpot, the runtime), and the JDK (the toolchain — `javac`, `java`, `jar`, `javap`).
- LTS releases and the six-month cadence: why "Java 21" is the 2026 baseline and what an LTS is.
- Primitives vs objects: the eight primitives, their boxed counterparts, autoboxing, and the `Integer` cache `==` trap.
- `var` for local type inference — where it helps readability and where it hurts.
- Immutability and `final`; why mutable shared state is the root of most bugs.
- `record` types: positional components, compiled members, compact constructors, validation, and when *not* to use a record.
- `sealed` interfaces and classes: `permits`, the closed hierarchy, and what the compiler can prove once a type is sealed.
- Pattern matching: `instanceof` patterns, the pattern-matching `switch`, record deconstruction patterns, guards (`when`), and compiler-checked exhaustiveness.
- `enum` types as a closed set of constants with behavior.
- Representing absence: `null`, the `NullPointerException`, `Optional<T>`, and when each is appropriate.
- Exceptions: checked vs unchecked, `try`/`catch`/`finally`, try-with-resources, and exceptions vs `Optional` for "not found."
- The JVM execution model: `javac` → bytecode → classloading → interpretation → JIT (C1/C2) → native code.
- Memory: the heap, the stack, references, object headers, and where a `record`'s data actually lives.
- Garbage collection: generational GC, the young/old split, and what G1 (the Java 21 default) does at a high level.
- Building and running pure Java with Maven: `mvn compile`, `mvn exec:java`, `mvn test`.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract.

| Day       | Focus                                              | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|----------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | Java the language: types, primitives, records      |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | Sealed types, pattern-matching switch, enums       |    2h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0h      |     6.5h    |
| Wednesday | Absence: null, Optional, exceptions                |    1h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0.5h    |     6h      |
| Thursday  | The JVM: bytecode, JIT, heap/stack, GC             |    2h    |    1h     |     0h     |    0.5h   |   1h     |     1h       |    0.5h    |     6h      |
| Friday    | Build pure Java with Maven; mini-project work      |    0h    |    1h     |     0h     |    0.5h   |   1h     |     3h       |    0.5h    |     6h      |
| Saturday  | Mini-project deep work                             |    0h    |    0h     |     0h     |    0h     |   1h     |     3h       |    0h      |     4h      |
| Sunday    | Quiz, review, polish                               |    0h    |    0h     |     0h     |    1h     |   0h     |     0.5h     |    0h      |     1.5h    |
| **Total** |                                                    | **7h**   | **7.5h**  | **2h**     | **3.5h**  | **6h**   | **10.5h**    | **2h**     | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./README.md) | This overview (you are here) |
| [resources.md](./resources.md) | Curated, current docs, JEPs, tools, and reading |
| [lecture-notes/01-java-the-language.md](./lecture-notes/01-java-the-language.md) | Types, primitives vs objects, records, sealed interfaces, pattern-matching switch, enums |
| [lecture-notes/02-what-the-jvm-actually-does.md](./lecture-notes/02-what-the-jvm-actually-does.md) | Bytecode, classloading, JIT, the heap/stack, references, garbage collection |
| [exercises/README.md](./exercises/README.md) | Index of short coding drills |
| [exercises/exercise-01-records-and-immutability.md](./exercises/exercise-01-records-and-immutability.md) | Scaffold a Maven project; model immutable data with records; run it |
| [exercises/exercise-02-sealed-and-switch.java](./exercises/exercise-02-sealed-and-switch.java) | Fill-in-the-TODO sealed hierarchy + exhaustive pattern-matching switch |
| [exercises/exercise-03-absence.java](./exercises/exercise-03-absence.java) | Replace null-returning code with `Optional`; choose exception vs `Optional` |
| [challenges/README.md](./challenges/README.md) | Index of weekly challenges |
| [challenges/challenge-01-transaction-ledger.md](./challenges/challenge-01-transaction-ledger.md) | Model a transaction ledger with sealed types + records, process it with one exhaustive switch |
| [quiz.md](./quiz.md) | 10 multiple-choice questions with an answer key |
| [homework.md](./homework.md) | Six practice problems with a grading rubric |
| [mini-project/README.md](./mini-project/README.md) | Full spec for the Crunch Tracker core domain model |

## The "build succeeded, tests green" promise

C3 uses a small recurring marker in every exercise that ends in working code. After `mvn test` you want to see:

```
[INFO] BUILD SUCCESS
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

If your build shows failures, errors, or — just as bad this week — **warnings about unchecked operations, raw types, or unnecessary `instanceof` casts**, you are not done. We treat compiler warnings as bugs. The point of week 2 is to make that green line ordinary.

We compile with `-Xlint:all` on this week so the compiler tells you when you've written 2008 Java by accident. A raw `List` (instead of `List<Goal>`), an `instanceof` followed by a cast (instead of an `instanceof` pattern), an unreachable `default` in an exhaustive switch — the compiler will flag them, and so will we.

## Stretch goals

If you finish the regular work early and want to push further:

- Read **JEP 395: Records** and **JEP 409: Sealed Classes** — the design documents that introduced these features. They explain the *why*, not just the *how*: <https://openjdk.org/jeps/395> and <https://openjdk.org/jeps/409>.
- Read **JEP 441: Pattern Matching for switch** (finalized in Java 21): <https://openjdk.org/jeps/441>.
- Disassemble one of your own `.class` files with `javap -c -p target/classes/...` and read the bytecode for a small method. You don't need to understand every opcode — just see that it's real.
- Run your mini-project with `java -Xlog:gc -jar ...` and watch the garbage collector log a collection. You'll come back to this in week 5.
- Write a short note for your future self comparing how Python and Java each model "no value": `None` vs `null` vs `Optional[T]` vs `Optional<T>`. The differences are subtle and they matter.

## Up next

Continue to **Week 3 — OOP, Collections, and Testing** once you have pushed this week's mini-project (the Crunch Tracker core domain) to your team repo. Week 3 takes the records and services you build this week and refactors them behind interfaces with a real JUnit 5 + AssertJ test suite. Everything you write this week, you keep.

---

*If you find errors in this material, open an issue or send a PR. Future learners will thank you.*
