# Lecture 1 — Wiring the Client to the API

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can build a single typed `fetch` wrapper for the whole app, point it at the right base URL per environment, attach a JWT to every request, map a non-2xx response (including a Spring `ProblemDetail`) into a typed error, and debug the three integration failures that block every team in week 9 — CORS, 401, and a misconfigured base URL.

If you only remember one thing from this lecture, remember this:

> **There is exactly one place in your app that calls `fetch`.** Every screen, every hook, every mutation goes through it. The base URL lives there once. The auth header is attached there once. Error mapping happens there once. The day you have `fetch(` scattered across twelve components is the day integration bugs become un-debuggable. Centralize the wire.

---

## 1. The integration boundary

For eight weeks the two halves of Crunch Tracker lived in separate universes. The backend had its own truth: a Postgres row, a JWT, a `@PreAuthorize` rule, an integration test that passed. The mobile app had its own truth: a `const MOCK_HABITS = [...]` array, a Zustand store, a screen that rendered.

This week those two truths have to agree across a wire, and the wire is unforgiving. Three things change the moment you delete the mock array:

1. **Data arrives asynchronously and can fail.** The mock was always there, instantly. The real list is a `Promise` that can reject, time out, or come back `401`.
2. **The two processes disagree about the world.** The server is the authority. Your cached copy is a guess that goes stale. Managing that disagreement *is* the work.
3. **Configuration becomes load-bearing.** Which URL? Which token? Which CORS origin? Get any of these wrong and nothing renders, with an error message that rarely points at the real cause.

A senior engineer's mental model is: **the network is a function that takes a typed request and returns, eventually, either typed data or a typed error — and might do neither for a while.** Everything in this lecture builds that function.

---

## 2. The smallest honest fetch is already wrong

Here is what most people write first. It works in the demo and breaks in production.

```typescript
// DON'T ship this.
async function getHabits() {
  const res = await fetch("http://localhost:8080/api/v1/habits");
  return res.json();
}
```

Count the problems:

- **`localhost` is hardcoded.** It will not reach your laptop from a physical phone (more on this in §4). It is also the wrong URL in production.
- **No auth header.** Your week-6 backend scopes habits per user; this returns `401`.
- **`res.json()` is called unconditionally.** A `401` or `500` is not JSON-of-habits; it's a `ProblemDetail`. You'll parse the error body *as if it were data* and crash a screen with `undefined.map`.
- **The return type is `any`.** Nothing downstream is type-checked. The whole point of TypeScript evaporates at the one boundary that matters most.
- **No timeout.** A dead backend hangs the request forever; the user stares at a spinner with no end.

Every one of those is a real failure that a real cohort hits in week 9. We fix all of them in one wrapper.

---

## 3. A typed fetch wrapper

Here is the centerpiece of the week. Read it slowly; we build it up piece by piece below.

```typescript
// src/api/client.ts
import { getApiBaseUrl } from "./config";
import { getToken, clearToken } from "../auth/tokenStore";

export type ApiErrorKind = "network" | "timeout" | "unauthorized" | "http" | "parse";

export class ApiError extends Error {
  constructor(
    readonly kind: ApiErrorKind,
    message: string,
    readonly status?: number,
    readonly problem?: ProblemDetail,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

// RFC 9457 — the shape Spring returns for every error.
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  // Spring lets you add fields; bean-validation errors come back under `errors`.
  errors?: Array<{ field: string; message: string }>;
}

interface ApiOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  signal?: AbortSignal;
  auth?: boolean; // default true; set false for /auth/login and /auth/register
  timeoutMs?: number; // default 10_000
}

export async function apiFetch<T>(path: string, opts: ApiOptions = {}): Promise<T> {
  const {
    method = "GET",
    body,
    auth = true,
    timeoutMs = 10_000,
  } = opts;

  const url = `${getApiBaseUrl()}${path}`;

  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";

  if (auth) {
    const token = await getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  // Timeout via AbortController, merged with any caller-supplied signal.
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const signal = opts.signal
    ? anySignal([opts.signal, controller.signal])
    : controller.signal;

  let res: Response;
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (cause) {
    clearTimeout(timer);
    if (signal.aborted) {
      throw new ApiError("timeout", `Request to ${path} timed out after ${timeoutMs}ms`);
    }
    throw new ApiError("network", `Network request to ${path} failed`, undefined);
  }
  clearTimeout(timer);

  if (res.status === 401) {
    // Token missing/expired/invalid. Clear it so the app re-routes to login.
    await clearToken();
    throw new ApiError("unauthorized", "Your session has expired. Please sign in again.", 401);
  }

  if (!res.ok) {
    const problem = await safeParseProblem(res);
    throw new ApiError("http", problem?.detail ?? problem?.title ?? `HTTP ${res.status}`, res.status, problem);
  }

  // 204 No Content has no body — don't try to parse it.
  if (res.status === 204) return undefined as T;

  try {
    return (await res.json()) as T;
  } catch {
    throw new ApiError("parse", `Could not parse JSON response from ${path}`, res.status);
  }
}

async function safeParseProblem(res: Response): Promise<ProblemDetail | undefined> {
  try {
    const json = await res.json();
    if (json && typeof json === "object" && "status" in json) return json as ProblemDetail;
  } catch {
    /* body wasn't JSON — fine, return undefined */
  }
  return undefined;
}

// Combine multiple AbortSignals into one (older RNs lack AbortSignal.any).
function anySignal(signals: AbortSignal[]): AbortSignal {
  const controller = new AbortController();
  for (const s of signals) {
    if (s.aborted) {
      controller.abort();
      break;
    }
    s.addEventListener("abort", () => controller.abort(), { once: true });
  }
  return controller.signal;
}
```

That is ~90 lines and it is the only place the app touches the network. Everything else — `getHabits`, `createGoal`, `checkIn` — is a one-liner on top of it:

```typescript
// src/api/habits.ts
import { apiFetch } from "./client";

export interface Habit {
  id: string;
  name: string;
  cadence: "DAILY" | "WEEKLY";
  createdAt: string; // ISO-8601 from the backend
  archived: boolean;
}

export interface NewHabit {
  name: string;
  cadence: "DAILY" | "WEEKLY";
}

export const getHabits = () => apiFetch<Habit[]>("/api/v1/habits");
export const getHabit = (id: string) => apiFetch<Habit>(`/api/v1/habits/${id}`);
export const createHabit = (body: NewHabit) =>
  apiFetch<Habit>("/api/v1/habits", { method: "POST", body });
export const archiveHabit = (id: string) =>
  apiFetch<void>(`/api/v1/habits/${id}`, { method: "DELETE" });
```

Notice what these functions return: a `Promise<Habit[]>`, a `Promise<Habit>`. Typed, narrow, honest. No `Response` ever escapes the client module. **This is the single most important refactor in week 9.**

> **Optional but recommended: validate the response with Zod.** TypeScript types are erased at runtime. `apiFetch<Habit[]>` *trusts* that the JSON matches `Habit[]`. If the backend renames `cadence` to `frequency`, TypeScript is happy and your UI breaks silently. Wrapping the parse in a Zod schema (`HabitSchema.array().parse(json)`) turns a silent contract drift into a loud, located error. We make this a homework option, not a requirement, because it adds ceremony — but on a real team you'd do it.

---

## 4. Environment config — and the `localhost` trap

The hardcoded `http://localhost:8080` is the most common week-9 blocker, and it deserves its own section because the failure is so confusing: *it works in the simulator and fails on your phone*, with no error that explains why.

Here's the why. `localhost` means "this device." On the iOS simulator, "this device" is your Mac, so `localhost:8080` reaches the Spring server running on your Mac. On a **physical phone**, "this device" is the phone — and there is no server on the phone. The request goes nowhere.

The fix: the phone must reach your laptop by its **LAN IP** (e.g. `http://192.168.1.42:8080`) or through an **Expo tunnel**. And you should never type that IP into source — it changes per network, per coworker, per CI.

### The config module

```typescript
// src/api/config.ts
import Constants from "expo-constants";

/**
 * Resolution order:
 *   1. EXPO_PUBLIC_API_URL  — explicit override (CI, production builds)
 *   2. app.config.ts `extra.apiUrl`
 *   3. Derive the LAN host from the Expo dev server, dev only
 */
export function getApiBaseUrl(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  if (fromEnv) return fromEnv;

  const fromExtra = Constants.expoConfig?.extra?.apiUrl as string | undefined;
  if (fromExtra) return fromExtra;

  // Dev fallback: reuse the host Metro is served from, swap the port.
  // hostUri looks like "192.168.1.42:8081" — Metro's port, not the API's.
  const hostUri = Constants.expoConfig?.hostUri;
  if (hostUri) {
    const host = hostUri.split(":")[0];
    return `http://${host}:8080`;
  }

  throw new Error(
    "No API base URL. Set EXPO_PUBLIC_API_URL or extra.apiUrl in app.config.ts.",
  );
}
```

The dev fallback is the clever bit: Expo already knows the LAN IP your phone used to load the bundle (`Constants.expoConfig.hostUri`). We reuse that host and swap Metro's port (`8081`) for the API's (`8080`). On a physical device this Just Works, because the same network path that delivered the JS bundle delivers the API call.

### Setting `extra.apiUrl` and env vars

```typescript
// app.config.ts
import { ExpoConfig } from "expo/config";

const config: ExpoConfig = {
  name: "Crunch Tracker",
  slug: "crunch-tracker",
  // ...the week-7/8 config...
  extra: {
    apiUrl: process.env.EXPO_PUBLIC_API_URL, // undefined in dev → triggers LAN fallback
  },
};

export default config;
```

```bash
# .env.local (gitignored) — point at a deployed backend for a teammate without one running
EXPO_PUBLIC_API_URL=https://crunch-tracker-api.fly.dev
```

> **`EXPO_PUBLIC_` is public.** Expo inlines any `EXPO_PUBLIC_*` variable into the JavaScript bundle that ships to the device. Anyone with the app can read it. That is correct for an API base URL. It is catastrophic for a secret. **Never** put a database password, an API key with write scope, or a JWT signing secret behind `EXPO_PUBLIC_`. Secrets live on the server, period.

---

## 5. The token store and attaching auth

Week 8 already gave you secure storage. This week we just need a tiny module the client imports, so the wrapper can attach the token and clear it on `401`.

```typescript
// src/auth/tokenStore.ts
import * as SecureStore from "expo-secure-store";

const KEY = "crunch.jwt";

export async function getToken(): Promise<string | null> {
  return SecureStore.getItemAsync(KEY);
}

export async function setToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(KEY, token);
}

export async function clearToken(): Promise<void> {
  await SecureStore.deleteItemAsync(KEY);
}
```

Login is a single `apiFetch` call with `auth: false` (you don't have a token yet), and it writes the result into the store:

```typescript
// src/auth/login.ts
import { apiFetch } from "../api/client";
import { setToken } from "./tokenStore";

interface LoginResponse {
  token: string;
  expiresAt: string; // ISO-8601
  user: { id: string; email: string };
}

export async function login(email: string, password: string) {
  const res = await apiFetch<LoginResponse>("/api/v1/auth/login", {
    method: "POST",
    body: { email, password },
    auth: false, // no Authorization header on the login request itself
  });
  await setToken(res.token);
  return res.user;
}
```

### Recovering from 401

Notice that `apiFetch` already calls `clearToken()` on any `401`. That is deliberate and it is half of session recovery. The other half is **reacting** to the cleared token. The clean way: a small auth gate (you built the navigation half in week 8) that reads the token on mount and on a global "session expired" signal, and routes to login when it's gone.

A pragmatic v1 that works today:

```typescript
// In your QueryClient setup, treat ApiError(unauthorized) globally.
import { QueryCache, QueryClient } from "@tanstack/react-query";
import { ApiError } from "./api/client";
import { useAuthStore } from "./auth/store"; // your week-8 Zustand store

export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      if (error instanceof ApiError && error.kind === "unauthorized") {
        useAuthStore.getState().signOut(); // flips the auth gate → login screen
      }
    },
  }),
});
```

Now any query or mutation that gets a `401` clears the token *and* kicks the user to login, from one place, no matter which screen triggered it. **Full token refresh** (using a refresh token to get a new access token transparently) is a real-world extension — we sketch it in the challenge, but the recover-to-login flow above is the week-9 baseline and it is honest.

---

## 6. Error modeling — `ProblemDetail` to a message a human reads

Your week-6 backend returns RFC-9457 `ProblemDetail` bodies. A validation failure looks like this on the wire:

```json
{
  "type": "https://crunch-tracker/errors/validation",
  "title": "Bad Request",
  "status": 400,
  "detail": "name must not be blank",
  "instance": "/api/v1/habits",
  "errors": [{ "field": "name", "message": "must not be blank" }]
}
```

The `apiFetch` wrapper already parses this into `ApiError.problem`. The job now is mapping it to something a user can act on. Centralize that too:

```typescript
// src/api/errorMessage.ts
import { ApiError } from "./client";

export function toUserMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return "Something went wrong. Please try again.";

  switch (error.kind) {
    case "network":
      return "Can't reach the server. Check your connection.";
    case "timeout":
      return "The request took too long. Try again.";
    case "unauthorized":
      return "Your session expired. Please sign in again.";
    case "parse":
      return "The server sent something unexpected.";
    case "http": {
      // Prefer the most specific field-level message when present.
      const fieldError = error.problem?.errors?.[0];
      if (fieldError) return `${fieldError.field}: ${fieldError.message}`;
      if (error.status === 404) return "We couldn't find that.";
      if (error.status === 409) return "That conflicts with something that already exists.";
      return error.problem?.detail ?? error.problem?.title ?? "Request failed.";
    }
  }
}
```

The discriminated union on `ApiError.kind` is doing real work here: TypeScript forces you to handle every error kind, and adding a new kind to the union later produces a compile error at this switch until you handle it. That is the whole point of modeling errors as types instead of catching `unknown` and stringifying it.

---

## 7. Debugging integration failure #1 — CORS

Symptom: in the browser/web target or sometimes in dev tooling, a request fails with a console message about "preflight" or "Access-Control-Allow-Origin," and the request never reaches your controller (the breakpoint never hits).

What is actually happening: when a client makes a "non-simple" cross-origin request — anything with a `Content-Type: application/json` or an `Authorization` header, which describes *every* authenticated request you make — the platform first sends an automatic `OPTIONS` preflight asking the server "are you OK with this origin, method, and headers?" If the server doesn't answer with the right `Access-Control-Allow-*` headers, the real request is never sent.

The fix is **on the server**, in your Spring Security config. You configured CORS in week 6; week 9 is where you find out if you did it right:

```java
// SecurityConfig.java (Spring Security 6)
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
        "http://localhost:8081",      // Expo web / Metro
        "exp://192.168.1.42:8081"     // Expo Go dev client (adjust to your LAN)
    ));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    cfg.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
}
```

And wire it into the filter chain (and make sure `OPTIONS` is permitted, not blocked by auth):

```java
http.cors(Customizer.withDefaults())
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/v1/auth/**").permitAll()
        .anyRequest().authenticated());
```

> **How to *read* a CORS failure instead of guessing.** Open your network panel. Find the `OPTIONS` request that precedes the failed call. Look at its response headers. If `Access-Control-Allow-Origin` is missing or doesn't match your origin, that's your bug — on the server. If the `OPTIONS` returns `401`, you forgot to permit `OPTIONS` in the filter chain. The error is always in the preflight response; the real request is a victim, not the cause.

A subtlety worth knowing: native iOS/Android (Expo Go on a device, not web) does **not** enforce CORS — CORS is a *browser* security model. So a request can work on a physical device and fail in the web target. Don't let that fool you into thinking CORS "doesn't matter"; the moment you run the web build or a reviewer opens it in a browser, it does.

---

## 8. Debugging integration failure #2 — the 401

Symptom: every authenticated request comes back `401 Unauthorized`, even though you "logged in."

Walk the token end to end — it's almost always one of four things:

1. **No token was attached.** Check the actual request in the network panel. Is the `Authorization: Bearer …` header present? If not, your `apiFetch` ran with `auth: false`, or `getToken()` returned `null` because login didn't persist the token. Log in again and confirm `getToken()` resolves to a string.
2. **The token is malformed or truncated.** Paste it into `jwt.io`. Does it decode into three dot-separated segments? A common bug: storing `Bearer eyJ…` (with the prefix) and then prefixing it again, sending `Bearer Bearer eyJ…`. Store the raw token; add `Bearer ` only in the header.
3. **The token expired.** Decode it and read `exp` (Unix seconds). If it's in the past, your session is stale — that's the `clearToken()` → login flow doing its job. If tokens expire absurdly fast, check the backend's expiry config.
4. **The backend rejects the signature.** If you restarted the backend with a freshly generated signing secret (a dev-mode default in some setups), every previously issued token is now invalid. Log in again to get one signed with the current secret.

The discipline: **read the real request and the real response.** The header that's present or absent, the token that decodes or doesn't, the `exp` that's future or past. "I think I'm logged in" is not data.

---

## 9. Debugging integration failure #3 — the wrong base URL

Symptom: requests hang and then time out, or fail with a generic network error, and nothing reaches the server (no log line on the backend at all).

This is almost always the `localhost` trap from §4, or a port mismatch (you pointed at Metro's `8081` instead of the API's `8080`), or the backend simply isn't running.

The 30-second triage, in order:

1. **Is the backend up?** `curl http://localhost:8080/api/v1/habits` from the same machine. If that fails, fix the backend first; the app can't reach what isn't there.
2. **What URL is the app actually using?** Temporarily `console.log(getApiBaseUrl())` at startup. Read it. Is the host your LAN IP (good for a device) or `localhost` (broken on a device)? Is the port `8080`?
3. **Can the device reach the host?** From the phone's browser, open `http://<lan-ip>:8080/api/v1/habits`. If the phone's browser can't reach it, the app never will — it's a network/firewall problem (a VPN, a "guest" WiFi that isolates clients, or macOS firewall blocking inbound 8080), not a code problem.

Each step isolates a layer. Skip the triage and you'll spend an hour editing TypeScript to fix a backend that isn't running.

---

## 10. Putting it together — a login-then-load smoke test

Before TanStack Query (that's Lecture 2), prove the raw wire works with a throwaway script you can run in a dev screen or a quick test:

```typescript
import { login } from "./src/auth/login";
import { getHabits, createHabit } from "./src/api/habits";

async function smoke() {
  const user = await login("ada@crunch.dev", "correct-horse-battery");
  console.log("Logged in as", user.email);

  const before = await getHabits();
  console.log(`${before.length} habits before`);

  const created = await createHabit({ name: "Read 20 pages", cadence: "DAILY" });
  console.log("Created", created.id, created.name);

  const after = await getHabits();
  console.log(`${after.length} habits after`); // before.length + 1
}

smoke().catch((e) => console.error("Smoke failed:", e));
```

If that prints the expected lines — login, count, create, count+1 — your client, config, auth, and error handling are correct. Now, and only now, do you put TanStack Query on top of it. **The wire works first; the cache works second.** Reverse that order and you'll debug two layers at once and learn neither.

---

## 11. Recap

You should now be able to:

- Explain why exactly one module in the app should call `fetch`.
- Build a typed `apiFetch<T>` with base URL, JSON handling, auth, timeout, and typed errors.
- Resolve the API base URL per environment and explain the `localhost`-from-device trap.
- Attach a JWT and recover cleanly from a `401` by clearing the token and routing to login.
- Map a Spring `ProblemDetail` into a user-facing message via a discriminated-union error type.
- Diagnose CORS, 401, and base-URL failures by reading the real request/response rather than guessing.

Next up: the cache layer. Continue to [Lecture 2 — Server State with TanStack Query](./02-server-state-with-tanstack-query.md).

---

## References

- *MDN — Using the Fetch API*: <https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch>
- *MDN — CORS*: <https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS>
- *RFC 9457 — Problem Details for HTTP APIs*: <https://www.rfc-editor.org/rfc/rfc9457>
- *Expo — Environment variables*: <https://docs.expo.dev/guides/environment-variables/>
- *expo-constants*: <https://docs.expo.dev/versions/latest/sdk/constants/>
- *Spring Security — CORS*: <https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html>
