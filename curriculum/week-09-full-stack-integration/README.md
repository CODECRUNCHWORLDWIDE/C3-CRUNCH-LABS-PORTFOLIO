# Week 9 — Full-Stack Integration

Welcome to the week the whole course pays off. For eight weeks you have built two halves of **Crunch Tracker** that never met: a Spring Boot 3 + PostgreSQL backend that speaks JSON and issues JWTs (weeks 4–6), and a React Native + Expo mobile client that navigates between screens against mocked local data (weeks 7–8). This week the wire goes live. The app logs in against the real `/auth/login` endpoint, stores the token, attaches it to every request, and loads and mutates the signed-in user's habits and goals through a typed API client with TanStack Query sitting in front of it.

This is integration work, and integration work is where junior engineers discover that "both halves pass their own tests" is not the same thing as "the product works." The 401 that only fires on a real device. The CORS preflight that the simulator hides. The optimistic update that flickers because you reconciled against the wrong cache key. The token that expired mid-session and dumped the user back to a blank screen with no error. None of that shows up in a backend integration test or a mocked-data UI. It shows up here, and learning to see it is the point of the week.

We assume you finished weeks 7 and 8 with a navigable Expo app on mock data, and that you have the week-6 backend running locally with at least one registered user. If your backend isn't running, you cannot do this week — the first exercise is making the two processes talk, and there is no mock substitute that teaches the same lessons. We move fast and we stay honest about the failure modes.

## Learning objectives

By the end of this week, you will be able to:

- **Build** a typed `fetch` client in TypeScript that centralizes base-URL config, JSON serialization, error mapping, and auth headers — no `any`, no scattered `fetch` calls.
- **Configure** environment-specific API base URLs (`localhost`, LAN IP, tunnel, production) without hardcoding, and explain why `localhost` from a physical device does not reach your laptop.
- **Attach** a JWT to outgoing requests via an interceptor-style wrapper, and **refresh or recover** cleanly when the token is missing, malformed, or expired.
- **Map** a non-2xx HTTP response — including a Spring `ProblemDetail` body — into a typed `ApiError` your UI can render, instead of letting a raw `Response` leak into a component.
- **Fetch** server state with TanStack Query (`useQuery`): query keys, `staleTime`, `gcTime`, background refetch, and the `isPending` / `isError` / `data` state machine.
- **Mutate** server state with `useMutation`: invalidating queries, the difference between invalidation and `setQueryData`, and where each belongs.
- **Implement** an optimistic update with `onMutate` / `onError` / `onSettled` that updates the UI instantly, rolls back on failure, and reconciles with the server's authoritative response — fully type-safe.
- **Distinguish** server state from client state and stop storing fetched data in Zustand "just in case."
- **Debug** the three classic integration failures — CORS preflight, 401 auth, and base-URL misconfiguration — by reading the actual request/response, not by guessing.
- **Render** honest loading, empty, error, and offline states so the app never shows a blank screen or a silent failure.

## Prerequisites

This week assumes you have completed **C3 · Crunch Labs Portfolio** weeks 1–8, or have equivalent fluency:

- A running Crunch Tracker backend from week 6: Spring Boot 3.x, PostgreSQL in Docker, JWT login at `POST /api/v1/auth/login`, per-user data scoping enforced. You can `curl` it and get a token.
- A navigable Expo + TypeScript app from week 8: stack + tab navigation, a Zustand store for session/UI state, login and habit-list screens wired to **mock** data.
- Comfortable TypeScript: generics, discriminated unions, `async`/`await`, `Promise<T>`.
- React hooks fluency: `useState`, `useEffect`, custom hooks, the rules of hooks.
- You can read and write Git and open a PR.

You do **not** need any prior TanStack Query exposure. We start from `npm install`. If you've used React Query v4, note that we are on **v5** and a few APIs changed (`isLoading` → `isPending`, object-form arguments) — we flag them as we go.

## Topics covered

- The integration boundary: two processes, one contract; why "works in isolation" misleads.
- A typed `fetch` wrapper: `apiFetch<T>()`, base URL, JSON handling, timeouts via `AbortController`.
- Environment config in Expo: `app.config.ts`, `expo-constants`, `EXPO_PUBLIC_*` vars, and the `localhost`-from-device trap.
- JWT on the client: attaching `Authorization: Bearer`, reading expiry, recovering from 401, secure storage (recap from week 8).
- Error modeling: RFC-9457 `ProblemDetail`, a typed `ApiError`, mapping status codes to user-facing messages.
- TanStack Query v5: `QueryClient`, `QueryClientProvider`, `useQuery`, query keys, `staleTime` vs `gcTime`, `refetch`.
- `useMutation`: the mutation lifecycle, `invalidateQueries`, `setQueryData`, and when to use which.
- Optimistic updates: `onMutate` → snapshot → optimistic write → `onError` rollback → `onSettled` reconcile.
- Server state vs client state: the single most clarifying idea in modern frontend architecture.
- CORS from a mobile client: what triggers a preflight, why Spring Security needs explicit config, how to read a failed preflight.
- Loading / empty / error / offline UX: the four states every screen that touches the network must handle.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract.

| Day       | Focus                                              | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|----------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | Typed fetch client, env config, auth headers       |    2h    |    2h     |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     6h      |
| Tuesday   | Wiring login + token; debugging CORS and 401s      |    1h    |    2h     |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5h      |
| Wednesday | TanStack Query: useQuery, keys, caching            |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0.5h     |    0.5h    |     6.5h    |
| Thursday  | useMutation, invalidation, optimistic updates      |    1h    |    1.5h   |     1.5h   |    0.5h   |   1h     |     0.5h     |    0h      |     6h      |
| Friday    | Error/loading/offline UX; mini-project work        |    0h    |    1h     |     0.5h   |    0.5h   |   1h     |     2h       |    0.5h    |     5.5h    |
| Saturday  | Mini-project deep work                             |    0h    |    0h     |     0h     |    0h     |   0h     |     3.5h     |    0h      |     3.5h    |
| Sunday    | Quiz, review, polish                               |    0h    |    0h     |     0h     |    1h     |   0h     |     2h       |    0h      |     3h      |
| **Total** |                                                    | **6h**   | **8h**    | **3.5h**   | **3.5h**  | **5h**   | **9h**       | **2.5h**   | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./README.md) | This overview (you are here) |
| [resources.md](./resources.md) | TanStack Query docs, Expo config, Spring CORS, and the rest |
| [lecture-notes/01-wiring-the-client-to-the-api.md](./lecture-notes/01-wiring-the-client-to-the-api.md) | Typed fetch, env config, auth headers, error modeling, CORS/401 debugging |
| [lecture-notes/02-server-state-with-tanstack-query.md](./lecture-notes/02-server-state-with-tanstack-query.md) | TanStack Query v5: queries, caching, mutations, optimistic updates, error UX |
| [exercises/README.md](./exercises/README.md) | Index of the three drills + how they work |
| [exercises/exercise-01-typed-api-client.md](./exercises/exercise-01-typed-api-client.md) | Build and curl-verify a typed fetch client against the live backend |
| [exercises/exercise-02-use-habits-query.ts](./exercises/exercise-02-use-habits-query.ts) | Fill-in-the-TODO TanStack Query hooks for habits |
| [exercises/exercise-03-optimistic-checkin.ts](./exercises/exercise-03-optimistic-checkin.ts) | Implement an optimistic check-in mutation with rollback |
| [challenges/README.md](./challenges/README.md) | What the challenge is and how it's graded |
| [challenges/challenge-01-offline-optimistic-checkin.md](./challenges/challenge-01-offline-optimistic-checkin.md) | Optimistic check-in that survives offline and a token refresh |
| [quiz.md](./quiz.md) | 12 questions with an answer key |
| [homework.md](./homework.md) | Six practice problems with a rubric |
| [mini-project/README.md](./mini-project/README.md) | Connect the Crunch Tracker app to the live backend, for real |

## The "real request" promise

C3 uses one recurring marker in every exercise that ends in working integration. After you wire a screen, you confirm it against the network panel, not against a green checkmark in a mocked test:

```
GET /api/v1/habits → 200 · 4 items · 86 ms · Authorization: Bearer eyJ…
```

If you cannot point at the real request and its real response — the method, the path, the status, the latency, and the `Authorization` header actually present — you are not done. "It rendered" is not "it worked." The whole point of week 9 is making that line ordinary.

## Stretch goals

If you finish the regular work early and want to push further:

- Read the TanStack Query **"Important Defaults"** page end to end and write down which default surprised you: <https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults>.
- Add **React Query Devtools** to your app and watch the cache mutate live as you navigate: <https://tanstack.com/query/latest/docs/framework/react/devtools>.
- Read the Expo guide on **environment variables and `EXPO_PUBLIC_`** and explain why secrets must never use that prefix: <https://docs.expo.dev/guides/environment-variables/>.
- Skim **RFC 9457** (Problem Details for HTTP APIs) — the format your Spring backend already returns: <https://www.rfc-editor.org/rfc/rfc9457>.
- Write a short note for your future self comparing how week-8 Zustand and week-9 TanStack Query each "hold state," and which kind of state belongs in which.

## Up next

Continue to **Week 10 — Capstone: Ship the Full-Stack App** once you have pushed the mini-project and your app loads real, signed-in data from the live backend.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
