# Week 8 — Challenges

The exercises drill the pieces in isolation: a navigator here, a form there, a store on its own. **Challenges make you assemble them under a real constraint.** This one takes 90–120 minutes and produces something you'll keep — the auth gate is the backbone of the mini-project and of Week 9.

## Index

1. **[Challenge 1 — A flicker-free, leak-free auth-gated flow](./challenge-01-auth-gated-flow.md)** — combine the session store, the navigators, and SecureStore into an authentication flow that launches to the right place with no white flash and gives a user no way to back-button into a protected screen after logout. (~100 min)

## How it's assessed

Challenges are graded on **behavior under adversarial conditions**, not on whether the happy path renders. A login screen that shows the tabs when you tap "log in" is the easy 60%; the challenge is in the other 40% — the launch race, the logout leak, the corrupt-token case. The rubric in the challenge file weights those explicitly.

You assess your own work against the rubric, then (if your cohort runs peer review) a partner runs the **same five adversarial checks** against your build: cold launch logged-out, cold launch logged-in, logout-then-back, kill-during-splash, and corrupted-token relaunch. If all five pass with no flicker and no leak, it's a pass.

Challenges are optional for passing the week, but this one is load-bearing: the mini-project's acceptance criteria assume a working auth gate, and Week 9 wires the real API into the exact `login`/`logout` seam you build here. Skipping it makes both harder. Do it.
