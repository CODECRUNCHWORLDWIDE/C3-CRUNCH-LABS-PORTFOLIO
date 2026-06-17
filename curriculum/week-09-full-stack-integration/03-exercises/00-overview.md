# Week 9 — Exercises

Short, focused drills. Each one should take 35–55 minutes. Do them in order; later ones assume earlier ones. Together they build the three pieces the mini-project assembles: a typed client, query hooks, and an optimistic mutation.

## Index

1. **[Exercise 1 — Typed API client](./exercise-01-typed-api-client.md)** — build the `apiFetch` wrapper, point it at the live week-6 backend, log in, and curl-verify a real authenticated request. (~50 min)
2. **[Exercise 2 — `useHabits` query hooks](./exercise-02-use-habits-query.ts)** — fill in TODOs to wrap the client in TanStack Query hooks for listing and creating habits. (~45 min)
3. **[Exercise 3 — Optimistic check-in](./exercise-03-optimistic-checkin.ts)** — implement an optimistic check-in mutation with snapshot, rollback, and reconcile. (~50 min)

## What you need running

Unlike most weeks, exercise 1 needs **two processes alive at once**:

- Your **week-6 Spring Boot + Postgres backend**, reachable at `http://localhost:8080`, with at least one registered user. Confirm with:
  ```bash
  curl -s -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"ada@crunch.dev","password":"correct-horse-battery"}'
  ```
  You should get back a JSON object containing a `token`. If you don't, fix the backend before starting — there is no mock that teaches the same lessons.
- Your **week-8 Expo app**, started with `npx expo start`, running on a simulator or a physical device.

The `.ts` exercise files (2 and 3) are written to drop into your week-8 Expo project. They assume the `apiFetch` client from exercise 1 and `@tanstack/react-query` installed (`npm install @tanstack/react-query`).

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste from the hints. Muscle memory is the point.
- Run it against the **real backend**. The marker for "done" this week is a real request in the network panel, not a green checkmark in a mock.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.
- Every exercise must end with **`npx tsc --noEmit` clean** (zero type errors) and **zero `any`**. A loose `any` at the API boundary is a bug this week.

## The "done" checklist

For each exercise, you are done when:

- [ ] `npx tsc --noEmit` reports zero errors.
- [ ] There is no `any` in the code you wrote (run `grep -n ': any' your-file.ts` — it should be empty).
- [ ] You can point at the **real HTTP request** the code made: method, path, status, and `Authorization` header present.
- [ ] The behavior matches the "expected outcome" section of the exercise.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-09` to compare approaches.
