# Week 2 — Exercises

Short, focused drills. Each one should take 25–45 minutes. Do them in order; later ones assume earlier ones.

## Index

1. **[Exercise 1 — Records and immutability](./exercise-01-records-and-immutability.md)** — scaffold a Maven project, model the tracker's data with records, run it, and prove the immutability and value-equality you get for free. (~40 min)
2. **[Exercise 2 — Sealed types and the pattern-matching switch](./exercise-02-sealed-and-switch.java)** — fill in the TODOs in a `.java` file that models a closed hierarchy of check-in events and processes it with one exhaustive switch — no `instanceof`, no `default`. (~45 min)
3. **[Exercise 3 — Representing absence](./exercise-03-absence.java)** — replace `null`-returning code with `Optional`, and choose correctly between an exception and an `Optional` for "not found." (~35 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. Muscle memory is the entire point of these drills.
- Run it. See the output. Read the error and the stack trace if it crashed — Java's stack traces and helpful NPE messages name the culprit; learn to read them.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.
- Every exercise must end with `mvn compile` (or `mvn test`) printing **`BUILD SUCCESS`** with **zero warnings**. We compile with `-Xlint:all` this week — a warning about a raw type, an unchecked operation, or an unnecessary `instanceof` cast is a bug you must fix.

## The two file types

- **`.md` exercises** (exercise 1) are guided walkthroughs: you run commands and write small code as directed.
- **`.java` exercises** (exercises 2 and 3) are real, compilable source files with `// TODO` markers. The instructions for using them are in a comment block at the top of each file. Drop the file into a Maven project's `src/main/java`, fill in the TODOs, and run it.

## Checklist

Mark the week's exercises done when:

- [ ] Exercise 1: a Maven project compiles and runs; you've demonstrated record value-equality and immutability in your own `main`.
- [ ] Exercise 2: every TODO filled; the switch is exhaustive with **no `default`** because the interface is `sealed`; `mvn compile` is clean.
- [ ] Exercise 3: every `null`-return replaced with `Optional`; you can justify in one sentence where you chose an exception instead; build is warning-free.
- [ ] You ran `javap -c -p` on at least one of your compiled records and saw the generated `equals`/`hashCode`/`toString`.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-02` to compare.
