# Week 9 — Quiz

Twelve questions on the full-stack integration material. Take it with your lecture notes closed. Aim for 10/12 before moving to the capstone. Answer key at the bottom — don't peek.

---

**Q1.** Your app works on the iOS **simulator** but a physical phone shows a spinner forever, then a timeout. The base URL is `http://localhost:8080`. Why?

- A) The simulator is faster, so the request completes there but not on the slower phone.
- B) On a physical device, `localhost` means the device itself, which has no server — it must use the laptop's LAN IP or a tunnel.
- C) The phone needs a different JWT than the simulator.
- D) Physical devices block port 8080 by default; you must use 443.

---

**Q2.** In the typed `apiFetch<T>` wrapper, why do we call `res.ok` / check `res.status` **before** calling `res.json()`?

- A) `res.json()` is slower than `res.ok`, so we short-circuit for performance.
- B) An error response body is usually a `ProblemDetail`, not the data shape `T`; parsing it as `T` would crash downstream with `undefined.map`-style errors.
- C) `res.json()` can only be called once, so we must check status first to avoid consuming the stream twice.
- D) You cannot read `res.ok` after `res.json()` resolves.

---

**Q3.** Which of these belongs behind `EXPO_PUBLIC_`?

- A) The JWT signing secret.
- B) The database password.
- C) The API base URL.
- D) A third-party API key with write access.

---

**Q4.** A "non-simple" cross-origin request (one with `Content-Type: application/json` or an `Authorization` header) triggers what, and where is the fix?

- A) A retry storm; fix it on the client with exponential backoff.
- B) An automatic `OPTIONS` preflight; the fix is server-side CORS config that returns the right `Access-Control-Allow-*` headers.
- C) A 401; fix it by refreshing the token.
- D) Nothing special; CORS only applies to GET requests.

---

**Q5.** In TanStack Query v5, which field means "no cached data yet, and a fetch is in progress" (the first-load state)?

- A) `isLoading`
- B) `isFetching`
- C) `isPending`
- D) `isStale`

---

**Q6.** You call `invalidateQueries({ queryKey: ['habits'] })`. Given the key factory `{ all: ['habits'], list: () => ['habits','list'], detail: (id) => ['habits','detail',id] }`, which entries are invalidated?

- A) Only the exact entry `['habits']`.
- B) `['habits']`, `['habits','list']`, and every `['habits','detail', x]` — partial prefix match.
- C) Every query in the entire cache.
- D) None; you must invalidate each key individually.

---

**Q7.** What is the difference between `staleTime` and `gcTime`?

- A) They're aliases for the same setting.
- B) `staleTime` is how long data is considered fresh (no auto-refetch on remount); `gcTime` is how long an **unused** cache entry survives before deletion.
- C) `staleTime` controls retries; `gcTime` controls timeouts.
- D) `gcTime` is for queries; `staleTime` is for mutations.

---

**Q8.** After a successful create mutation whose **response is the new object**, you want the list to reflect it **without a refetch**. Which is the most direct tool?

- A) `invalidateQueries`
- B) `refetch`
- C) `setQueryData`, splicing the returned object into the cached list
- D) `useEffect` that re-reads the list

---

**Q9.** In an optimistic update, why does `onMutate` call `cancelQueries` before snapshotting and writing?

- A) To cancel the mutation if it's slow.
- B) So an already-in-flight background refetch can't resolve *after* the optimistic write and overwrite it with stale data (the flicker bug).
- C) Because `setQueryData` requires no queries to be running.
- D) To reduce the number of network requests.

---

**Q10.** In the optimistic check-in, `onSettled` invalidates the list even on **success**. Why not just trust the optimistic write?

- A) Query requires an invalidation after every mutation or it throws.
- B) The server may compute fields the client can't derive (streak, best-streak, completion %); invalidating pulls the authoritative object so those are correct.
- C) `onSettled` only runs on failure, so it's the rollback.
- D) It's redundant; you should remove it.

---

**Q11.** Which piece of data should live in the **TanStack Query cache** rather than in Zustand?

- A) The currently selected bottom-tab.
- B) A half-typed "new habit" form draft.
- C) The signed-in user's list of habits fetched from the backend.
- D) Whether a modal is open.

---

**Q12.** Your app gets a `401` while the user is mid-session. Which is the cleanest architecture for handling it?

- A) Add a `try/catch` around every `useQuery` call in every screen and route to login from each.
- B) `apiFetch` clears the token on 401 and throws a typed `ApiError('unauthorized')`; a single global `QueryCache.onError` handler signs out and routes to login.
- C) Ignore it; the user will figure out they need to log in again.
- D) Reload the entire app via a native restart.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — `localhost` resolves to the device. On the simulator that's your Mac (where the server runs); on a phone it's the phone (no server). Use the LAN IP or tunnel. This is the single most common week-9 blocker.
2. **B** — A non-2xx body is typically a `ProblemDetail`, not your data type. Parsing it as `T` and handing it to a `.map` crashes a screen. Check status first; parse the problem body separately.
3. **C** — `EXPO_PUBLIC_*` values are inlined into the shipped bundle and are readable by anyone with the app. The base URL is fine to expose; secrets (A, B, D) must never use that prefix — they live on the server.
4. **B** — Non-simple cross-origin requests trigger an automatic `OPTIONS` preflight. If the server doesn't answer with the right `Access-Control-Allow-*` headers, the real request never fires. The fix is server-side CORS config (and permitting `OPTIONS` in the Spring filter chain).
5. **C** — In v5, `isPending` is "no data yet, fetching." `isLoading` was the v4 name and now means `isPending && isFetching`. `isFetching` is true for *any* fetch including background refetch.
6. **B** — Query keys are hierarchical; invalidation does a prefix match. Invalidating `['habits']` invalidates everything that starts with it. That's exactly why the factory nests under `all`.
7. **B** — `staleTime` = freshness window (no refetch on remount/focus while fresh). `gcTime` (was `cacheTime`) = how long an *unobserved* entry lives before garbage collection. Orthogonal clocks.
8. **C** — `setQueryData` writes directly into the cache, no round trip. Use it when the mutation response *is* the authoritative new object. `invalidateQueries` is the safe default but costs a refetch.
9. **B** — Without `cancelQueries`, an in-flight GET can resolve after your optimistic `setQueryData` and clobber it, making the UI flicker back. Cancelling closes that race — the #1 optimistic-update bug.
10. **B** — `onMutate` only set the fields you know (e.g. `checkedInToday`). The server may recompute streaks and percentages you can't derive client-side. `onSettled` runs on both success and error; invalidating reconciles with authoritative truth.
11. **C** — Fetched backend data is *server state* and belongs in the Query cache. A, B, D are *client state* (UI the app owns) and belong in Zustand/`useState`. Don't duplicate server state into Zustand.
12. **B** — Centralize it: `apiFetch` clears the token and throws a typed unauthorized error; one global handler signs out and routes to login. A (per-screen try/catch) is the un-maintainable anti-pattern; D is a sledgehammer; C is broken UX.

</details>

---

If you scored under 8, re-read the lectures for the questions you missed — especially the difference between `staleTime`/`gcTime` and the optimistic-update lifecycle. If you scored 10 or higher, you're ready for the [homework](./homework.md).
