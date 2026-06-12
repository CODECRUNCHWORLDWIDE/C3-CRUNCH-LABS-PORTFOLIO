# Week 1 — Challenges

The exercises drill the happy path. **Challenges stretch you into the situations that actually scare people.** Each one takes 60–120 minutes and produces something you can point to as evidence you can handle a mess, not just a clean start.

## Index

1. **[Challenge 1 — Resolve a gnarly conflict and rebase a messy branch clean](challenge-01-merge-conflict-rebase.md)** — you'll set up a deliberate three-way merge conflict, resolve it by understanding *both* sides, then take a branch with sloppy, half-baked commits and rebase it into a clean, reviewable, linear history — without losing any work. (~90 min)

## How challenges are assessed

Challenges are optional for passing the week, but they're where the real learning compounds. This one is graded on **process and outcome**, not just "did it work":

| Criterion | Weight | What "great" looks like |
|-----------|-------:|-------------------------|
| Conflict resolved correctly | 30% | The final file keeps the *intent of both sides*, not just one verbatim; no leftover `<<<<<<<` markers; tests pass. |
| History is clean | 30% | Final branch is linear, every commit builds, messages are Conventional, no `WIP`/`fixup` noise left. |
| No work lost | 20% | Every intended change is present in the final tree; nothing silently dropped during the rebase. |
| You can explain it | 20% | A short `RECOVERY.md` narrates what conflicted, why, and each rebase step — in your own words. |

If you can resolve a real conflict calmly and reshape history on purpose, you are measurably ahead of most engineers with years of experience who only ever `git pull` and pray. This skill reappears every time `main` moves under your feet — which is every week from here.
