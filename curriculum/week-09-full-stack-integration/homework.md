# Week 9 Homework

Six practice problems that revisit the week's topics against your real, integrated stack. The full set should take about **5 hours**. Work in your Crunch Tracker repo (week-09 branch) so each problem produces at least one commit you can point to later.

Each problem includes a **problem statement**, **acceptance criteria**, a **hint**, and an **estimated time**. The grading rubric for the whole set is at the bottom.

---

## Problem 1 — Read your own wire

**Problem statement.** With your app pointed at the live backend, perform the login → list habits → add a habit flow, then write `notes/week-09-wire.md` recording, for each of the three requests:

1. The method and full path.
2. The status code.
3. Whether the `Authorization: Bearer` header was present (and for login, confirm it was **absent** — you had no token yet).
4. The approximate latency.

Then answer in one sentence: *what value does `getApiBaseUrl()` return on your test device, and why is it not `localhost`?*

**Acceptance criteria.**

- `notes/week-09-wire.md` exists with the three requests documented and the one-sentence answer.
- The login request is correctly noted as having **no** `Authorization` header.
- Committed.

**Hint.** Use the in-app network inspector or Expo dev tools. If you can't see headers there, `console.log` the resolved headers inside `apiFetch` temporarily (then remove it).

**Estimated time.** 25 minutes.

---

## Problem 2 — Harden the error mapper

**Problem statement.** Extend `toUserMessage(error)` (Lecture 1, §6) to handle these cases with specific, human messages, and write a small test file that asserts each:

1. A `429 Too Many Requests` → "You're going too fast. Try again in a moment."
2. A `403 Forbidden` (you tried to touch another user's habit) → "You don't have access to that."
3. A `ProblemDetail` with a field error → `"<field>: <message>"`.
4. A non-`ApiError` thrown value → a safe generic message.

**Acceptance criteria.**

- `toUserMessage` handles all four cases; the discriminated-union switch stays exhaustive.
- A test file (`errorMessage.test.ts`) with at least four cases, all passing under your test runner (Jest/Vitest).
- `npx tsc --noEmit` clean; no `any`.
- Committed.

**Hint.** Construct test `ApiError`s directly: `new ApiError("http", "...", 429)`. For the field-error case, pass a `problem` with an `errors: [{ field, message }]` array.

**Estimated time.** 45 minutes.

---

## Problem 3 — Query keys and a per-user cache

**Problem statement.** Right now habits are keyed `['habits','list']`. Refactor the key factory and `useHabits` so the list is keyed **per user**: `['habits', userId, 'list']`. Then verify the bug this fixes: log in as user A (see A's habits), log out, log in as user B, and confirm you see B's habits — **not a stale cache of A's**. Document the before/after in `notes/week-09-keys.md`.

**Acceptance criteria.**

- `habitKeys` includes the `userId` in the list (and detail) keys.
- `useHabits` derives `userId` from the session and passes it into the key.
- Logout calls `queryClient.clear()` (or you can prove the per-user key alone prevents the leak).
- `notes/week-09-keys.md` describes what you observed before and after.
- Committed.

**Hint.** `userId` comes from your week-8 session store (set at login). The key factory becomes `list: (userId: string) => ['habits', userId, 'list'] as const`. Remember: everything that changes the data belongs in the key.

**Estimated time.** 45 minutes.

---

## Problem 4 — An optimistic update you can defend

**Problem statement.** Implement an **optimistic "archive habit"** mutation (`useArchiveHabit`): tapping archive removes the habit from the list instantly, restores it if the server rejects, and reconciles on settle. Then write three tests (or a documented manual test plan) proving:

1. Happy path: archived habit disappears and stays gone after reconcile.
2. Rollback: forced server error → the habit reappears.
3. No flicker: an in-flight refetch can't resurrect the archived habit mid-mutation (the `cancelQueries` guard).

**Acceptance criteria.**

- `useArchiveHabit` implemented with `onMutate`/`onError`/`onSettled`, fully typed (`context` is not `any`).
- `cancelQueries` is called in `onMutate`.
- The three scenarios are demonstrated (tests or a written, reproducible plan with observed results).
- `npx tsc --noEmit` clean.
- Committed.

**Hint.** The optimistic write is a `filter`, not a `map`: `old.filter(h => h.id !== vars.id)`. Snapshot in `onMutate`, restore in `onError`, invalidate in `onSettled`. To force a server error for testing, temporarily make the endpoint return 500 or point at a non-existent id.

**Estimated time.** 1 hour.

---

## Problem 5 — The four states, demonstrated

**Problem statement.** Pick one networked screen (Habits or Goals). Make it render **all four** states correctly and capture a screenshot of each into `notes/week-09-states/`:

1. **Loading** — the first-load spinner/skeleton.
2. **Empty** — delete all items on the backend and reload; show the empty-state CTA.
3. **Error** — stop the backend and reload; show the message **and a working retry button**.
4. **Offline** — turn on airplane mode; show an offline-aware state distinct from a generic error.

**Acceptance criteria.**

- Four screenshots in `notes/week-09-states/`, one per state.
- The error state's retry button actually re-runs the query (restart the backend, tap retry, data appears).
- The screen never shows a blank white screen in any state.
- Committed.

**Hint.** Route the screen through a `QueryStates` wrapper (Lecture 2, §9) so you physically cannot skip a state. For offline, wire `onlineManager` to NetInfo and branch on connectivity, or at minimum distinguish a `network`-kind `ApiError`.

**Estimated time.** 1 hour.

---

## Problem 6 — Integration debugging writeup

**Problem statement.** Deliberately reproduce **two** of the three classic integration failures (CORS, 401, base-URL), then write a 300–400 word debugging writeup at `notes/week-09-debugging.md` covering, for each:

1. How you reproduced it (the exact change you made).
2. What the **symptom** looked like (what the user/screen saw).
3. How you **read** the real cause (which request/response/header told you the truth).
4. The fix, and which layer (client or server) it lived in.

**Acceptance criteria.**

- `notes/week-09-debugging.md` exists, 300–400 words, covering two distinct failures.
- Each failure includes reproduction, symptom, diagnosis, and fix.
- The writeup names the specific request/header/status you read to diagnose — not "I guessed and it worked."
- Committed.

**Hint.** For 401: clear the token mid-session, refetch, read the missing `Authorization` header. For base-URL: stop the backend or point at the wrong port, watch it time out, confirm no request reaches the server. For CORS (web target): tighten `allowedOrigins` to exclude your origin and read the failed `OPTIONS` preflight.

**Estimated time.** 40 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 25 min |
| 2 | 45 min |
| 3 | 45 min |
| 4 | 1 h 0 min |
| 5 | 1 h 0 min |
| 6 | 40 min |
| **Total** | **~4 h 35 min** |

---

## Grading rubric

The homework is graded out of **100 points**. Submit your week-09 branch with all commits and the `notes/` artifacts.

| Criterion | Points | What earns full marks |
|-----------|-------:|------------------------|
| **Wire literacy (P1)** | 10 | Three requests documented accurately, including the login-has-no-token detail and a correct base-URL explanation. |
| **Error modeling (P2)** | 15 | All four cases handled with specific messages; exhaustive switch; tests pass; no `any`. |
| **Query keys / cache correctness (P3)** | 20 | Per-user keys implemented; the user-switch leak is demonstrably fixed; before/after documented. |
| **Optimistic update (P4)** | 25 | Archive flips instantly, rolls back on failure, reconciles on settle, `cancelQueries` present, fully typed; all three scenarios shown. |
| **The four states (P5)** | 20 | All four states render correctly with screenshots; retry actually works; no blank screens. |
| **Debugging writeup (P6)** | 10 | Two failures with reproduction, symptom, evidence-based diagnosis, and the correct fix layer. |

**Deductions:**

- −10 if `npx tsc --noEmit` is not clean.
- −10 if any `: any` appears in code you wrote at the API/query boundary.
- −5 per missing `notes/` artifact.
- −5 if fetched server data is duplicated into Zustand (server-state-in-the-wrong-place).

**Grade bands:** 90–100 production-ready; 75–89 solid, minor gaps; 60–74 works but architecture is shaky; below 60 revisit the lectures and resubmit.

When you've finished all six, push your repo and open the [mini-project](./mini-project/README.md) if you haven't already started it.
