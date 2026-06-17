# Challenge 1 — The Transaction Ledger

**Time estimate:** ~90 minutes.

## Problem statement

Model a small **transaction ledger** with sealed interfaces and records, then process it with pattern-matching switches that the compiler proves exhaustive. No `instanceof` chains. No `null`. No `default` branches on the sealed type.

A ledger is a list of **entries**. An entry is exactly one of:

- **`Deposit`** — money in: an `amount` (positive `BigDecimal`), a `LocalDate`, a `memo`.
- **`Withdrawal`** — money out: an `amount` (positive `BigDecimal`, the magnitude), a `LocalDate`, a `memo`, and a `Category` (an enum: `FOOD`, `TRANSPORT`, `SUBSCRIPTIONS`, `OTHER`).
- **`Transfer`** — money moved between two accounts: an `amount`, a `LocalDate`, a `fromAccount` and `toAccount` (both `String`), and a `memo`.
- **`Adjustment`** — a correction that can be positive or negative: a signed `amount`, a `LocalDate`, and a `reason`.

This is a *closed* set. There is no fifth kind of entry. Model it so the compiler knows that, and so that **adding a fifth kind would break every place that processes entries** until you handle it.

Use `BigDecimal` for money, never `double` — floating point can't represent `0.10` exactly, and money math demands exactness. (This is itself a senior-engineer reflex worth building: money is `BigDecimal` or integer cents, never `double`.)

## What to build

A small library, no framework, with these types and operations.

### Domain (`src/main/java/.../ledger/`)

```java
sealed interface Entry permits Deposit, Withdrawal, Transfer, Adjustment {
    BigDecimal amount();
    LocalDate date();
}
```

Each entry is a `record` implementing `Entry`. Validate in compact constructors:

- `Deposit` / `Withdrawal` / `Transfer` amounts must be **strictly positive**.
- `Adjustment` amount may be positive or negative but **not zero**.
- `Transfer.fromAccount` and `toAccount` must be non-blank and **different from each other**.
- No `memo`/`reason` may be `null` (blank is allowed; null is not).

### Operations (`Ledger.java`)

Implement each with a pattern-matching switch over `Entry`. **No `default`. No `instanceof`.**

```java
/** The signed effect of an entry on the running balance:
 *    Deposit     -> +amount
 *    Withdrawal  -> -amount
 *    Adjustment  -> amount (already signed)
 *    Transfer    -> 0 (it nets to zero on a single-account ledger;
 *                      document this choice in your README) */
static BigDecimal signedEffect(Entry e);

/** Running balance over a list of entries, starting from a given opening balance. */
static BigDecimal balance(BigDecimal opening, List<Entry> entries);

/** A human-readable one-line description of an entry. Use record DECONSTRUCTION
 *  patterns. Examples:
 *    "2026-06-10  +1500.00  deposit: paycheck"
 *    "2026-06-11   -42.50  withdrawal [FOOD]: groceries"
 *    "2026-06-12   200.00  transfer checking -> savings"
 *    "2026-06-13   -10.00  adjustment: bank fee correction" */
static String describe(Entry e);

/** Total spent per Withdrawal Category. Returns a Map<Category, BigDecimal>.
 *  Entries that aren't Withdrawals contribute nothing. */
static Map<Category, BigDecimal> spendByCategory(List<Entry> entries);

/** Find the largest single withdrawal, if any. Returns Optional<Withdrawal>
 *  — empty when the ledger has no withdrawals. NO null. */
static Optional<Withdrawal> largestWithdrawal(List<Entry> entries);
```

A plain `for` loop is fine for the aggregations (the Stream API is week 3). The grading is on the modeling and the switches, not on stream golf.

## Acceptance criteria

- [ ] A Maven project (Java 21, `-Xlint:all`) with the domain under one package and a JUnit 5 test class.
- [ ] `mvn test`: **BUILD SUCCESS**, 0 warnings, **at least 10 passing tests**, covering:
  - `balance` over a mixed ledger produces the correct `BigDecimal`.
  - Each entry type's `describe` output.
  - `signedEffect` for all four entry kinds.
  - `spendByCategory` sums correctly and ignores non-withdrawals.
  - `largestWithdrawal` returns the right `Withdrawal`, and `Optional.empty()` on a ledger with no withdrawals.
  - Compact-constructor validation: a zero `Adjustment` throws; a non-positive `Deposit` throws; a `Transfer` to the same account throws.
- [ ] **Every** switch over `Entry` has **no `default`**. The hierarchy is `sealed`.
- [ ] **Zero `null`** in the public API. Absence is `Optional`. (`largestWithdrawal` is the obvious one — there will be others.)
- [ ] Money is `BigDecimal` everywhere. No `double`. No `float`.
- [ ] A `README.md` in the challenge folder with the example usage and **a short note documenting your `Transfer.signedEffect == 0` decision** (it's a deliberate modeling choice — own it).

## Prove exhaustiveness (required)

In your README, describe (or screenshot) what happens when you add a fifth entry type. Concretely:

1. Add `record Interest(BigDecimal amount, LocalDate date, BigDecimal rate) implements Entry {}` to the `permits` list.
2. Run `mvn compile`.
3. Observe that `signedEffect`, `describe`, and every other switch **fail to compile** with "the switch statement does not cover all possible input values."
4. That compile error — the compiler handing you a checklist of every place that needs updating — is the deliverable. Note it, then revert the fifth type.

## Stretch

- Add a `Transfer` that *does* affect balance: model two accounts and compute a per-account balance, so a `Transfer` is `-amount` on `from` and `+amount` on `to`. Document how this changes `signedEffect`.
- Add a `LedgerError` sealed result type so `balance` can return either a `Success(BigDecimal)` or a `Failure(reason)` instead of throwing — a taste of the "errors as values" style you'll meet again in week 4's `ProblemDetail`.
- Replace the `for`-loop aggregations with the Stream API (`Collectors.groupingBy`, `Collectors.reducing`). This is a week-3 preview; try it if you're ahead.

## Hints

<details>
<summary>Domain skeleton</summary>

```java
package com.crunch.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;

public enum Category { FOOD, TRANSPORT, SUBSCRIPTIONS, OTHER }

public sealed interface Entry permits Deposit, Withdrawal, Transfer, Adjustment {
    BigDecimal amount();
    LocalDate date();
}

public record Deposit(BigDecimal amount, LocalDate date, String memo) implements Entry {
    public Deposit {
        requirePositive(amount, "deposit amount");
        if (memo == null) throw new IllegalArgumentException("memo must not be null");
    }
}

public record Adjustment(BigDecimal amount, LocalDate date, String reason) implements Entry {
    public Adjustment {
        if (amount == null || amount.signum() == 0)
            throw new IllegalArgumentException("adjustment amount must be non-zero");
        if (reason == null) throw new IllegalArgumentException("reason must not be null");
    }
}

// ... Withdrawal, Transfer similar; share a requirePositive helper ...
```

</details>

<details>
<summary>signedEffect with a switch (no default, no instanceof)</summary>

```java
static BigDecimal signedEffect(Entry e) {
    return switch (e) {
        case Deposit d     -> d.amount();
        case Withdrawal w  -> w.amount().negate();
        case Adjustment a  -> a.amount();
        case Transfer t    -> BigDecimal.ZERO;
    };
}
```

No `default`. If you add `Interest`, this won't compile — exactly what you want.

</details>

<details>
<summary>describe with record deconstruction</summary>

```java
static String describe(Entry e) {
    return switch (e) {
        case Deposit(var amt, var date, var memo) ->
            "%s  +%s  deposit: %s".formatted(date, amt, memo);
        case Withdrawal(var amt, var date, var memo, var cat) ->
            "%s   -%s  withdrawal [%s]: %s".formatted(date, amt, cat, memo);
        case Transfer(var amt, var date, var from, var to, var memo) ->
            "%s   %s  transfer %s -> %s".formatted(date, amt, from, to);
        case Adjustment(var amt, var date, var reason) ->
            "%s   %s  adjustment: %s".formatted(date, amt, reason);
    };
}
```

</details>

<details>
<summary>An xUnit-style JUnit 5 test that asserts a throw</summary>

```java
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class LedgerTest {
    @Test
    void zeroAdjustmentIsRejected() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> new Adjustment(BigDecimal.ZERO, LocalDate.now(), "noop"));
        assertTrue(ex.getMessage().contains("non-zero"));
    }
}
```

</details>

## Why this matters

The "sealed hierarchy of records, processed by an exhaustive switch" pattern is the safest way to model a domain in modern Java, and it scales all the way up:

- **The Crunch Tracker mini-project** (this week) is this exact pattern applied to `Goal`/`Habit`/`CheckIn`.
- **Week 4's API layer** models request/response variants and validation outcomes the same way.
- **Week 9's client** models loading/error/success UI states as a closed set.

If you internalize "closed set ⇒ sealed + records + exhaustive switch" now, every later week reuses the muscle.

## Submission

Commit your `ledger` solution under `challenges/challenge-01/` in your week-2 GitHub repo. Make sure `mvn test` passes on a fresh clone — that means committing a `.gitignore` that excludes `target/`, and no checked-in build output. Include the README with the example usage and your `Transfer` modeling note.
