# Week 2 — Challenges

The exercises drill basics. **Challenges stretch you.** Each one takes 60–120 minutes and produces something you can commit to your portfolio.

## Index

1. **[Challenge 1 — The Transaction Ledger](./challenge-01-transaction-ledger.md)** — model a small financial domain (a transaction ledger) with sealed interfaces and records, then process it exhaustively with one pattern-matching switch — no `instanceof` chains, no nulls, no `default`. (~90 min)

## How it's assessed

Challenges are optional. If you skip them, you can still pass the week. If you do them, you'll be measurably ahead — and the sealed-types-plus-exhaustive-switch pattern here is the *exact* pattern the mini-project uses for the Crunch Tracker domain, so this challenge is the best possible warm-up for it.

A challenge submission is assessed on five things, in order of weight:

| Criterion | What "great" looks like |
|----------|-------------------------|
| **Modeling fidelity** | The domain is a `sealed` hierarchy of `record`s. Illegal states are unrepresentable. Invariants live in compact constructors. |
| **Exhaustiveness** | Every `switch` over the sealed type has **no `default`**. Adding a new subtype breaks compilation — and you can demonstrate that. |
| **No nulls** | Absence is modeled with `Optional`, never `null`. Zero methods can hand a caller a null. |
| **Correctness** | A JUnit 5 suite proves the behavior — happy paths and the validation/throw paths. |
| **Clarity** | Files are short, each type has one job, names read in plain English, no dead code. |

There is no partial credit for "it runs but uses an `instanceof` ladder." The whole point of the challenge is the modern modeling discipline. If your switch has a `default: throw`, you've thrown away the compiler's exhaustiveness check, and that's the one thing we're grading.

Commit your solution under `challenges/challenge-01/` in your week-2 repo with a `README.md` showing the example usage. Make sure `mvn test` is green on a fresh clone.
