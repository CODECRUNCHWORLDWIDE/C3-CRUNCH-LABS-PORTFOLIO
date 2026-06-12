# Week 9 — Resources

Every resource on this page is **free**. The TanStack Query docs are open. Expo's documentation is free. The Spring docs are free. The HTTP RFCs are published openly. No paywalled books are linked. If a link 404s, open an issue so we can replace it.

## Required reading (work it into your week)

- **TanStack Query — Overview** — what "server state" is and why it needs its own tool:
  <https://tanstack.com/query/latest/docs/framework/react/overview>
- **TanStack Query — Queries** — `useQuery`, query keys, the status state machine:
  <https://tanstack.com/query/latest/docs/framework/react/guides/queries>
- **TanStack Query — Mutations** — `useMutation` and the mutation lifecycle:
  <https://tanstack.com/query/latest/docs/framework/react/guides/mutations>
- **TanStack Query — Optimistic Updates** — the `onMutate`/`onError`/`onSettled` pattern we lean on Thursday:
  <https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates>
- **Expo — Environment variables** — `EXPO_PUBLIC_*`, `app.config.ts`, and the secrets warning:
  <https://docs.expo.dev/guides/environment-variables/>

## TanStack Query — the parts you'll use most

- **Important Defaults** — read this once; it explains `staleTime`, refetch-on-focus, retries:
  <https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults>
- **Query Keys** — how cache identity works; get this right and everything else follows:
  <https://tanstack.com/query/latest/docs/framework/react/guides/query-keys>
- **Query Invalidation** — `invalidateQueries`, partial matching:
  <https://tanstack.com/query/latest/docs/framework/react/guides/query-invalidation>
- **`QueryClient` API reference** — `setQueryData`, `getQueryData`, `cancelQueries`:
  <https://tanstack.com/query/latest/docs/reference/QueryClient>
- **React Native usage notes** — `onlineManager`, `focusManager`, `AppState` integration:
  <https://tanstack.com/query/latest/docs/framework/react/react-native>
- **Devtools** — watch the cache live (works with `@dev-plugins/react-query` in Expo):
  <https://tanstack.com/query/latest/docs/framework/react/devtools>

## The HTTP and Fetch surface

- **MDN — Using the Fetch API**: <https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch>
- **MDN — `AbortController`** (how we add request timeouts): <https://developer.mozilla.org/en-US/docs/Web/API/AbortController>
- **MDN — CORS** — the canonical explainer for preflights and the headers involved:
  <https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS>
- **RFC 9457 — Problem Details for HTTP APIs** — the JSON error shape your Spring backend returns:
  <https://www.rfc-editor.org/rfc/rfc9457>
- **RFC 6750 — Bearer Token Usage** — the `Authorization: Bearer` scheme:
  <https://www.rfc-editor.org/rfc/rfc6750>

## Expo and React Native config

- **Expo — `app.config.ts`** (dynamic config, reading env into `extra`):
  <https://docs.expo.dev/workflow/configuration/>
- **`expo-constants`** — reading the config `extra` at runtime: <https://docs.expo.dev/versions/latest/sdk/constants/>
- **`expo-secure-store`** — encrypted token storage (recap from week 8): <https://docs.expo.dev/versions/latest/sdk/securestore/>
- **Expo — Development builds and the LAN/tunnel URL** — why a real device can't reach `localhost`:
  <https://docs.expo.dev/more/expo-cli/#develop>

## Spring side (the backend you're integrating against)

- **Spring Security — CORS** — the server half of the CORS conversation:
  <https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html>
- **Spring Framework — CORS support** (`CorsConfigurationSource`):
  <https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html>
- **Spring Web — `ProblemDetail`** — the RFC-9457 errors your client must parse:
  <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html>

## Libraries we touch this week

- **`@tanstack/react-query`** (v5) — the server-state library at the center of the week:
  <https://www.npmjs.com/package/@tanstack/react-query>
- **`zustand`** (recap) — client state only this week; server state moves to Query:
  <https://github.com/pmndrs/zustand>
- **`zod`** (optional but recommended) — runtime-validate API responses so a contract drift fails loudly:
  <https://zod.dev/>

## Tools you'll use this week

- **HTTPie or `curl`** — sanity-check every endpoint from the terminal before you touch the app. `http :8080/api/v1/habits "Authorization:Bearer $TOKEN"`.
- **`jwt.io`** — paste a token to read its `exp` and `sub` claims while debugging. Never paste a production token: <https://jwt.io/>.
- **Expo Dev Tools / the in-app network inspector** — see the real request/response, the actual source of truth this week.
- **React Query Devtools** — the cache visualizer; indispensable once mutations get complicated.

## Videos (free, no signup)

- **TanStack Query official "Let's learn" playlist** — short, current to v5: <https://www.youtube.com/@Tanstack>
- **TkDodo's blog (Dominik, a Query maintainer)** — the single best free writing on Query patterns:
  <https://tkdodo.eu/blog/practical-react-query>
- **Expo on YouTube** — config, dev builds, EAS (looking ahead to week 10): <https://www.youtube.com/@expo_io>

## Open-source projects to read this week

You learn more from one hour reading a well-built Query integration than from three hours of tutorials. Pick one and scroll:

- **TanStack Query examples (React Native)** — official, runnable:
  <https://github.com/TanStack/query/tree/main/examples/react>
- **`TkDodo/react-query-fundamentals`** — the workshop repo, patterns annotated:
  <https://github.com/TkDodo>
- **Expo examples** — production-shaped Expo apps with API integration:
  <https://github.com/expo/examples>

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Server state** | Data you don't own — it lives on the backend, you cache a copy. Habits, goals, the user. |
| **Client state** | Data the app owns — UI toggles, the current tab, a draft form. Lives in Zustand/`useState`. |
| **Query key** | The array that identifies a cache entry, e.g. `['habits', userId]`. Same key = same cache. |
| **`staleTime`** | How long fetched data is considered fresh. Within it, no refetch on remount/focus. Default `0`. |
| **`gcTime`** | How long an unused (no observers) cache entry lives before garbage collection. Default 5 min. |
| **`isPending`** | v5 name for "the query has no data yet and is fetching." (Was `isLoading` in v4.) |
| **`invalidateQueries`** | Mark cached data stale and refetch — the "tell me the truth from the server" move. |
| **`setQueryData`** | Write directly into the cache without a fetch — the "I already know the answer" move. |
| **Optimistic update** | Update the UI before the server confirms; roll back if it fails. |
| **`onMutate`** | Mutation callback that runs before the request — where you snapshot and apply the optimistic write. |
| **CORS preflight** | An automatic `OPTIONS` request the platform sends before a "non-simple" cross-origin request. |
| **`ProblemDetail`** | The RFC-9457 JSON error shape Spring returns: `type`, `title`, `status`, `detail`, `instance`. |
| **JWT** | JSON Web Token — the signed, stateless credential you attach as `Authorization: Bearer <token>`. |
| **`EXPO_PUBLIC_*`** | Env vars Expo inlines into the client bundle. Public by definition — never secrets. |

---

*If a link 404s, please open an issue so we can replace it.*
