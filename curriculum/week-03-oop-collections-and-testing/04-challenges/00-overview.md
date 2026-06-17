# Week 3 — Challenges

The exercises drill basics. **Challenges stretch you.** This one takes 90–150 minutes and produces something you can commit to your portfolio — and, more importantly, teaches the single most valuable skill of a working engineer: **safely changing code you didn't write and don't trust.**

## Index

1. **[Challenge 1 — Tame the God class](./challenge-01-tame-the-god-class.md)** — inherit a 200-line untested "God class," pin its current behavior with a characterization test suite *written before you touch the logic*, then refactor it behind interfaces with the tests still green. (~120 min)

## How it's assessed

Challenges are optional — you can pass the week without them. But this one is the closest thing in the whole course to what your first job actually feels like, so do it if you can. It's assessed on four things, in order of weight:

| Criterion | Weight | What "great" looks like |
|-----------|-------:|-------------------------|
| **Tests written first** | 30% | Your git history shows the characterization suite committed *before* any refactor. The tests describe the *current* behavior, warts and all — they are not aspirational. |
| **Behavior preserved** | 30% | The same suite is green before and after the refactor. You changed the structure, not the observable behavior. No test was weakened or deleted to make the refactor pass. |
| **Design after** | 25% | The God class is gone: responsibilities are split behind interfaces, composition replaces the tangle, collections are chosen deliberately, and the public surface is small. |
| **Hygiene** | 15% | `mvn test` and a JaCoCo report run clean on a fresh clone; commits are conventional and atomic; the README explains what you did and why. |

The deliverable is a public repo (or a folder in your week-3 repo) with the before/after code, the test suite, a green build, and a short write-up. Full rubric and starter code are in the challenge file.

Why this matters beyond the grade: in five years almost none of your time will be spent writing greenfield code. It will be spent changing systems that are already running, already load-bearing, and already untested. The characterization-test-then-refactor technique is how professionals do that without breaking production. Learn it now on a 200-line toy; you'll use it on a 200,000-line system later.
