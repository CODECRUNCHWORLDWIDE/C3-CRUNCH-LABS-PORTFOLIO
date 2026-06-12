# Mini-Project — Connect Crunch Tracker to the Live Backend

> Delete the mock data. For real this time. The week-8 Expo app logs in against your week-6 Spring Boot + Postgres backend, loads the signed-in user's habits and goals through TanStack Query, creates and checks in habits with optimistic updates, and handles auth, errors, refresh, and offline like a product — not a demo.

This is the week the course converges. You are not building anything new from scratch; you are **wiring two things you already built** into one working application. That sounds easier than it is. Integration is where assumptions go to die, and the deliverable bar is correspondingly concrete: a reviewer logs in on a fresh clone and uses your app against a live backend, and it just works.

**Estimated time:** ~9 hours (split across Wednesday through Sunday in the suggested schedule).

---

## What you will build

The week-8 app, with every mocked array replaced by live, authenticated, cached server data:

- **Login** against `POST /api/v1/auth/login`, token stored in `expo-secure-store`, attached to every subsequent request.
- **Habits tab** — lists the signed-in user's habits via `useQuery`, with pull-to-refresh, an add-habit form (`useMutation` + invalidate), and **optimistic check-in** (the exercise-3 hook) on each row.
- **Goals tab** — lists the user's goals via `useQuery`, with create and an optimistic progress update.
- **Profile tab** — shows the signed-in user's email (from the login response / a `/me` endpoint) and a working **logout** that clears the token and the query cache and routes back to login.
- **Honest states everywhere** — every networked screen renders loading, empty, error (with retry), and offline correctly. No blank screens. No silent failures.
- **Clean auth lifecycle** — a 401 anywhere clears the session and routes to login from one place; you never see a 401 leak into a screen as a crash.

By the end you'll have a public GitHub repo where the mobile client and the Spring backend genuinely talk, and a short demo you can show.

---

## How this compounds on prior weeks

This is week 9 of a sequential course. You are *standing on* prior weeks, not restarting:

- **Weeks 4–6 (backend)** gave you the API: REST endpoints, validated bodies, `ProblemDetail` errors, JWT login, per-user data scoping, Postgres persistence. This week you consume it.
- **Week 7 (RN basics)** gave you typed components and `FlatList`. The habit row, the empty state, the form fields — those are week-7 components, now fed by real data.
- **Week 8 (navigation + state)** gave you the navigable shell: stack + tabs, typed routes, the Zustand session store, the auth-gated flow. This week you flip the gate based on a *real* token and feed the tabs *real* data.
- **This week (9)** is the seam. Next week (10) deploys it.

Concretely: **do not rebuild screens.** Open your week-8 app, and for each screen that reads `MOCK_*`, replace the mock with a query hook. The visual layer barely changes; the data layer changes entirely. If you find yourself rewriting UI, stop — that's a sign you're avoiding the integration work, which is the actual assignment.

---

## Rules

- **You must** consume the week-6 backend you (or the cohort) built. No new mock data layer. If your backend is missing an endpoint this project needs (e.g. goals, or `/me`), **add it to the backend** — that's legitimate integration work — rather than mocking it on the client.
- **You may** use: `@tanstack/react-query` (v5), `zustand` (client state only), `expo-secure-store`, `@react-native-community/netinfo`, and optionally `zod` for response validation.
- **You may NOT** store fetched server data in Zustand "to be safe." Server state lives in the Query cache; client state (current tab, draft form, UI toggles) lives in Zustand/`useState`. Mixing them is the anti-pattern this week exists to break.
- **Zero `any`** at the API boundary. `npx tsc --noEmit` is clean.
- The base URL is **never hardcoded** — it comes from `getApiBaseUrl()` (env → extra → LAN fallback).

---

## Acceptance criteria

- [ ] A public GitHub repo (the same one you've extended since week 7, on a `week-09` branch or tag).
- [ ] One module — `src/api/client.ts` — is the **only** place that calls `fetch`. (`grep -rn 'fetch(' src` shows hits only in `client.ts`.)
- [ ] `getApiBaseUrl()` resolves per environment; no hardcoded host/port anywhere else.
- [ ] **Login works against the live backend.** The token persists across an app restart (don't have to log in twice).
- [ ] The Habits tab loads the signed-in user's real habits via `useQuery`, with working **pull-to-refresh**.
- [ ] Adding a habit creates it on the server and the list reflects it (invalidate or `setQueryData`).
- [ ] **Optimistic check-in** works: the row flips instantly, rolls back on a forced server error, and reconciles via `onSettled`. Verified on a real device/simulator with no flicker.
- [ ] The Goals tab loads real goals and supports create + an optimistic update.
- [ ] **Logout** clears the token, clears the query cache (`queryClient.clear()`), and routes to login. Logging in as a *different* user shows *that* user's data, not a stale cache.
- [ ] Every networked screen handles **loading, empty, error (with retry), and offline**. Demonstrate the empty state by deleting all habits, and the error state by stopping the backend.
- [ ] A **401 anywhere** routes to login cleanly (no crash, no blank screen) via the global `QueryCache.onError` handler.
- [ ] `npx tsc --noEmit`: 0 errors. `grep -rn ': any' src` is empty.
- [ ] Per-user scoping holds end to end: user A never sees user B's habits (the backend enforces it; confirm the client respects it).
- [ ] `README.md` documents setup (both processes), the env var, and includes the "real request" markers for the key flows.

---

## Suggested order of operations

Build incrementally. Get the wire working before the cache; get one tab working before all three.

### Phase 1 — The client and login (~2h)

This is exercise 1, promoted into the app for real.

1. Add `src/api/config.ts`, `src/api/client.ts`, `src/auth/tokenStore.ts`, `src/auth/login.ts`, and resource modules `src/api/habits.ts`, `src/api/goals.ts`.
2. Replace the week-8 mock login with the real `login()`. On success, store the token and flip the auth gate.
3. Smoke it: log in, confirm the token persists across a reload, confirm `getApiBaseUrl()` is correct on your device.

Commit: `Live login against the JWT endpoint`.

### Phase 2 — Query setup + the Habits tab (~2h)

1. `npm install @tanstack/react-query`; add `QueryClientProvider` and the `queryClient` with the global 401 handler (Lecture 2, §2).
2. Add `habitKeys`, `useHabits`, `useCreateHabit` (exercise 2).
3. Replace the Habits screen's mock with `useHabits()`. Wire loading/empty/error/refetch through a `QueryStates` wrapper.
4. Wire the add-habit form to `useCreateHabit`.

Commit: `Habits tab on live data with create`.

### Phase 3 — Optimistic check-in (~1.5h)

1. Add `useCheckIn` (exercise 3) with `onMutate`/`onError`/`onSettled`.
2. Wire each habit row's check-in control to it.
3. Verify the happy path (no flicker), the rollback (force a server error), and the reconcile (server-computed streak updates).

Commit: `Optimistic habit check-in`.

### Phase 4 — Goals tab (~1.5h)

1. Mirror the habits work for goals: `goalKeys`, `useGoals`, `useCreateGoal`, and an optimistic `useUpdateGoalProgress`.
2. Replace the Goals screen's mock.
3. If the backend lacks a goals endpoint, **add it server-side** (controller + service + repository + Flyway migration, the week-4/5/6 pattern) and note it in the README.

Commit: `Goals tab on live data`.

### Phase 5 — Profile, logout, and the auth lifecycle (~1h)

1. Profile shows the signed-in user (from login response or a `/me` query).
2. Logout: `clearToken()`, `queryClient.clear()`, flip the auth gate to login. Test the **user-switch** case: log out, log in as a different user, confirm no stale data.
3. Confirm a forced 401 (clear the token while a screen is open, then refetch) routes to login, not a crash.

Commit: `Profile + logout + clean auth lifecycle`.

### Phase 6 — States, polish, and the README (~1h)

1. Audit every networked screen for all four states. Demonstrate empty (no items) and error (backend down).
2. Add React Query Devtools (dev only) and watch the cache while you navigate.
3. Write the README: setup for both processes, the `EXPO_PUBLIC_API_URL` env var, and the "real request" markers for login, list, create, and check-in.

Commit: `States, devtools, and README`.

---

## Example expected behavior

The "done" markers, as real requests (your network panel should show these):

```
# Login (no token yet → no Authorization header)
POST /api/v1/auth/login            → 200 · token issued · 120 ms

# Habits tab mounts
GET  /api/v1/habits                → 200 · 4 items · 86 ms · Authorization: Bearer eyJ…

# Add a habit
POST /api/v1/habits                → 201 · Authorization: Bearer eyJ…
GET  /api/v1/habits                → 200 · 5 items   (invalidation refetch)

# Optimistic check-in (UI flips BEFORE this request returns)
POST /api/v1/habits/{id}/check-ins → 200 · Authorization: Bearer eyJ…
GET  /api/v1/habits                → 200   (onSettled reconcile; streak now correct)

# Logout, then log in as a different user → that user's data, not a cached mix
POST /api/v1/auth/login            → 200
GET  /api/v1/habits                → 200 · different user's habits
```

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Wire works on a fresh clone | 25% | Login + load real data against a live backend with no hand-holding; base URL resolves per env |
| Server-state architecture | 20% | One `fetch` site; query hooks; no fetched data in Zustand; correct keys + invalidation |
| Optimistic update correctness | 15% | Instant flip, rollback on failure, reconcile on settle, no flicker, fully typed |
| Auth lifecycle | 15% | Token persists; 401 → login from one place; logout clears cache; user-switch shows no stale data |
| The four states | 15% | Every networked screen handles loading/empty/error(retry)/offline; demonstrated, not claimed |
| Type & code hygiene | 10% | `tsc --noEmit` clean, zero `any`, hooks not smeared into screens, README lets a stranger run it |

---

## Stretch (optional)

- Add **offline resilience** from challenge 1 (onlineManager + paused mutations) to the check-in flow.
- Add **Zod validation** at the API boundary for habits and goals; deliberately break a field name on the backend and watch it fail loudly with a located error.
- Add a **`/me` query** and use it as the source of truth for the profile, with its own cache entry and `staleTime`.
- Add a **request-id** header per request and correlate a client error to a backend log line.
- Wire **`AppState`-based refetch-on-foreground** via Query's `focusManager` so the app refreshes when the user returns to it.

---

## What this prepares you for

- **Week 10 (capstone)** deploys this exact app against a *hosted* backend with real latency and cold starts. Every base-URL, auth, and offline decision you made here gets exercised against a network that is no longer `localhost`.
- The **server-state vs client-state** discipline is the single most transferable idea in modern frontend work — you'll apply it in every React/React Native job you ever take.
- The **integration debugging muscle** — read the real request, isolate the layer — is what makes you useful on day one of a real team, where "both halves pass their tests but the product is broken" is a Tuesday.

---

## Resources

- *TanStack Query — Overview*: <https://tanstack.com/query/latest/docs/framework/react/overview>
- *TanStack Query — Optimistic Updates*: <https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates>
- *TanStack Query — React Native*: <https://tanstack.com/query/latest/docs/framework/react/react-native>
- *Expo — Environment variables*: <https://docs.expo.dev/guides/environment-variables/>
- *Spring Security — CORS*: <https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html>
- *RFC 9457 — Problem Details*: <https://www.rfc-editor.org/rfc/rfc9457>

---

## Submission

When done:

1. Push to GitHub on a `week-09` branch or tag, public URL.
2. Make sure `README.md` documents both processes' setup, the `EXPO_PUBLIC_API_URL` env var, and the "real request" markers.
3. Record a 90-second demo: log in, list, add a habit, optimistic check-in (show the instant flip), logout, log in as a different user (show no stale data). Link it in the README — the demo proves the integration in a way a screenshot can't.
4. Post the repo URL in your cohort tracker. This is the week the app became real. Show it.
