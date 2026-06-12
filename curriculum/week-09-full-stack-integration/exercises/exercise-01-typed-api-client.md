# Exercise 1 — Typed API Client

**Goal:** Build the one module in your app that talks to the network, point it at your live week-6 backend, log in for real, and confirm an authenticated request lands — by reading the actual request and response, not a mock. No TanStack Query yet; just the raw, typed wire.

**Estimated time:** 50 minutes.

---

## Setup

You need two things alive:

1. **The week-6 backend** at `http://localhost:8080`. Sanity-check it from the terminal first — never debug the app against a backend you haven't verified:

```bash
# Should return a token. If it doesn't, stop and fix the backend.
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@crunch.dev","password":"correct-horse-battery"}'
```

If you don't have a user yet, register one:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@crunch.dev","password":"correct-horse-battery"}'
```

2. **Your week-8 Expo app**. From its root: `npx expo start`. Keep it running.

Confirm `expo-secure-store` is installed (it was, in week 8): `npx expo install expo-secure-store`.

---

## Step 1 — The config module

Create `src/api/config.ts`. It resolves the base URL per environment, with the LAN fallback so a physical device works:

```typescript
import Constants from "expo-constants";

export function getApiBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  if (fromEnv) return fromEnv;

  const hostUri = Constants.expoConfig?.hostUri;
  if (hostUri) {
    const host = hostUri.split(":")[0];
    return `http://${host}:8080`;
  }
  throw new Error("No API base URL. Set EXPO_PUBLIC_API_URL.");
}
```

Add a temporary `console.log("API base:", getApiBaseUrl())` at app startup and read it. On a **simulator** you'll likely see `localhost` or a LAN IP; on a **device** you must see a LAN IP (e.g. `http://192.168.x.x:8080`), never `localhost`. If you see `localhost` on a device, that's your first lesson — it will never reach your laptop.

---

## Step 2 — The token store

Create `src/auth/tokenStore.ts`:

```typescript
import * as SecureStore from "expo-secure-store";

const KEY = "crunch.jwt";

export const getToken = () => SecureStore.getItemAsync(KEY);
export const setToken = (t: string) => SecureStore.setItemAsync(KEY, t);
export const clearToken = () => SecureStore.deleteItemAsync(KEY);
```

---

## Step 3 — The typed client

Create `src/api/client.ts`. Build `apiFetch<T>` so that it:

- Prefixes `path` with `getApiBaseUrl()`.
- Sends `Accept: application/json`, and `Content-Type: application/json` only when there's a body.
- Attaches `Authorization: Bearer <token>` when `auth !== false` and a token exists.
- Aborts after `timeoutMs` (default 10s) via `AbortController`.
- On `401`: clears the token and throws an `ApiError` of kind `"unauthorized"`.
- On any other non-2xx: parses the `ProblemDetail` body and throws an `ApiError` of kind `"http"` carrying the `status` and parsed `problem`.
- On `204`: returns `undefined`.
- Otherwise: returns `await res.json()` typed as `T`.

Use the full implementation from **Lecture 1, §3** as your reference, but type it yourself. Define `ApiError`, `ApiErrorKind`, and `ProblemDetail` exactly as in the lecture.

---

## Step 4 — The habits resource module

Create `src/api/habits.ts`:

```typescript
import { apiFetch } from "./client";

export interface Habit {
  id: string;
  name: string;
  cadence: "DAILY" | "WEEKLY";
  createdAt: string;
  archived: boolean;
  checkedInToday: boolean;
}

export interface NewHabit { name: string; cadence: "DAILY" | "WEEKLY" }

export const getHabits = () => apiFetch<Habit[]>("/api/v1/habits");
export const createHabit = (body: NewHabit) =>
  apiFetch<Habit>("/api/v1/habits", { method: "POST", body });
```

(Adjust field names to match what your week-6 controller actually returns — that reconciliation *is* part of integration. If your backend doesn't yet send `checkedInToday`, either add it server-side or compute it client-side from a check-ins list; note which you chose.)

---

## Step 5 — Login

Create `src/auth/login.ts`:

```typescript
import { apiFetch } from "../api/client";
import { setToken } from "./tokenStore";

interface LoginResponse {
  token: string;
  user: { id: string; email: string };
}

export async function login(email: string, password: string) {
  const res = await apiFetch<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email, password },
    auth: false,
  });
  await setToken(res.token);
  return res.user;
}
```

---

## Step 6 — Smoke it against the real backend

Wire a throwaway button on any screen (or a top-of-`App` effect) that runs:

```typescript
import { login } from "./src/auth/login";
import { getHabits, createHabit } from "./src/api/habits";

async function smoke() {
  const user = await login("ada@crunch.dev", "correct-horse-battery");
  console.log("Logged in as", user.email);

  const before = await getHabits();
  console.log(`${before.length} habits before`);

  const created = await createHabit({ name: "Read 20 pages", cadence: "DAILY" });
  console.log("Created", created.id);

  const after = await getHabits();
  console.log(`${after.length} habits after`); // before + 1
}
```

Run it. Open the network inspector. You must see, in order:

```
POST /api/v1/auth/login   → 200   (no Authorization header — correct, you had no token)
GET  /api/v1/habits       → 200   Authorization: Bearer eyJ…
POST /api/v1/habits       → 201   Authorization: Bearer eyJ…
GET  /api/v1/habits       → 200   Authorization: Bearer eyJ…  (one more item than the first GET)
```

---

## Step 7 — Break it on purpose

Integration skill is debugging skill. Reproduce each of the three classic failures and confirm you can read it:

1. **Force a 401.** After login, `await clearToken()` then call `getHabits()`. Confirm you get an `ApiError` of kind `"unauthorized"`, and that the request in the panel has **no** `Authorization` header.
2. **Force a base-URL failure.** Stop the backend. Call `getHabits()`. Confirm you get a `"network"` or `"timeout"` error after ~10s, and that no request reaches the (dead) server.
3. **Force a validation error.** Call `createHabit({ name: "", cadence: "DAILY" })`. Your week-6 bean validation should reject the blank name with a `400` and a `ProblemDetail`. Confirm `ApiError.problem.errors[0]` carries the field message.

---

## Expected outcome

- A `src/api/client.ts` that is the only place in the app calling `fetch`.
- Real, authenticated requests visible in the network panel, with the `Bearer` token present.
- You can reproduce and *read* a 401, a base-URL failure, and a validation error.
- `npx tsc --noEmit` is clean. There is no `any` in any file you wrote.

---

## Acceptance criteria

- [ ] `getApiBaseUrl()` resolves correctly and you can explain why `localhost` fails on a device.
- [ ] `apiFetch<T>` attaches the `Bearer` token, handles `401`, parses `ProblemDetail`, and times out.
- [ ] The smoke flow logs login → count → create → count+1, confirmed in the network panel.
- [ ] You reproduced all three failure modes and described what you saw for each.
- [ ] `npx tsc --noEmit`: 0 errors. `grep -rn ': any' src/api src/auth` is empty.

---

## Stretch

- Add a 10-line Zod schema for `Habit` and validate the response inside `apiFetch` for the habits endpoints. Rename a field on the backend and watch the parse fail loudly instead of silently.
- Add an `X-Request-Id` header (a random UUID per request) and log it server-side. Now you can correlate a client error with a backend log line — a real production debugging superpower.

---

## Hints

<details>
<summary>If every request 401s even right after login</summary>

Read the actual request header. Most common cause: you stored `"Bearer eyJ…"` (with prefix) in SecureStore and then the client adds `Bearer ` again, sending `Bearer Bearer eyJ…`. Store the **raw** token; add `Bearer ` only in the header. Second most common: the token didn't persist — `await setToken(...)` wasn't awaited, or login threw before reaching it.

</details>

<details>
<summary>If the simulator works but a physical device shows a spinner forever</summary>

The base URL is `localhost`, which on the device means the device itself. Confirm `getApiBaseUrl()` returns your laptop's LAN IP. From the phone's browser, open `http://<lan-ip>:8080/api/v1/habits` — if the phone can't reach it there, it's a network/firewall issue (guest WiFi isolation, macOS firewall blocking inbound 8080), not code.

</details>

<details>
<summary>If `res.json()` throws on an error response</summary>

You're calling `res.json()` before checking `res.ok`. An error body might be a `ProblemDetail` (JSON) or empty. Check `res.status`/`res.ok` first; parse the problem body with a try/catch (`safeParseProblem` in the lecture) so a non-JSON error body doesn't crash the client.

</details>

---

When this feels comfortable, move to [Exercise 2 — `useHabits` query hooks](exercise-02-use-habits-query.ts).
