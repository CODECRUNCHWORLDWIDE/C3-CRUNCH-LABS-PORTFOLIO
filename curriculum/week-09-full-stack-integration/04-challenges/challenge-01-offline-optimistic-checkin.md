# Challenge 1 — Offline-Resilient Optimistic Check-In

**Time estimate:** ~120 minutes.

## Problem statement

Take the optimistic check-in from exercise 3 and harden it for the real world: a flaky, sometimes-absent network and a JWT that can expire mid-session. When a user taps "check in" on a habit, the app must:

1. **Update instantly** (optimistic), as in exercise 3.
2. **Survive going offline.** If the device is offline when the tap happens, the optimistic state must hold and the mutation must **pause** — not fail — and then **fire automatically** when connectivity returns, reconciling against the server.
3. **Never double-submit.** If the user taps the same habit twice while offline, or the app restarts before the queued mutation flushes, the server must not record two check-ins for the same habit/day. (Your week-6 endpoint should already be idempotent per habit+date; if it isn't, make it so, and rely on that.)
4. **Recover from a mid-session 401.** If the access token expires while a queued mutation is waiting (or while any request is in flight), the app must route to login cleanly — without losing the user's other data or crashing — and after re-login the queued check-in must still reconcile correctly (either it flushed before expiry, or it's safely retried after).

This is the difference between "works on my desk on good WiFi" and "works on a train."

## What you'll build on

You already have, from this week:

- `apiFetch` with `401` → `clearToken` (Lecture 1).
- The global `QueryCache.onError` → `signOut` handler (Lecture 2 / exercise driver).
- `useCheckIn` with `onMutate`/`onError`/`onSettled` (exercise 3).
- `habitKeys` (exercise 2).

You will add the **offline** dimension and prove the **token-expiry** recovery.

## Required pieces

### 1. Teach Query about connectivity

Query cannot detect online/offline in React Native on its own. Wire `onlineManager` to NetInfo once at startup:

```bash
npx expo install @react-native-community/netinfo
```

```typescript
// src/api/onlineManager.ts
import NetInfo from "@react-native-community/netinfo";
import { onlineManager } from "@tanstack/react-query";

export function wireOnlineManager() {
  onlineManager.setEventListener((setOnline) =>
    NetInfo.addEventListener((state) => setOnline(Boolean(state.isConnected))),
  );
}
```

Call `wireOnlineManager()` once before rendering. Now Query **pauses** mutations whose `networkMode` allows it while offline and **resumes** them on reconnect.

### 2. Make the check-in mutation pausable and persistent across the session

A paused mutation lives in memory. For the "app restarts before flush" requirement, you must either (a) persist the mutation cache, or (b) make the optimistic state durable enough that a fresh launch re-derives the right thing from the server. Choose one and justify it:

- **Option A — persist the mutation queue** with `@tanstack/query-async-storage-persister` + `persistQueryClient`, restoring paused mutations on launch and resuming them. More moving parts; truly offline-durable.
- **Option B — rely on server idempotency + invalidation.** Don't persist the queue; on launch, the `onSettled` refetch reconciles whatever the server actually recorded. Simpler; a check-in tapped offline and then killed before flush is lost, which may be acceptable. Document the tradeoff.

Either is a valid answer if you can defend it. Option A is the "great" answer.

### 3. Idempotency guard

Whatever you choose, the server must not double-count. Confirm (and if needed, fix) that `POST /api/v1/habits/{id}/check-ins` for a date already checked in returns the existing state (200) or a benign 409 you handle as success — never a second row. Add a backend integration test that proves it.

### 4. Token-expiry recovery

Force the scenario: set your backend's access-token TTL to ~60 seconds in dev, log in, wait it out, then tap check-in. The expired token yields a `401`; `apiFetch` clears it; the global handler signs out and routes to login. After re-login, confirm the habit's state is correct (the check-in either landed before expiry or is reconciled after). The app must never show a blank screen or a crash during this.

## Acceptance criteria

- [ ] `wireOnlineManager()` is called once; Query pauses/resumes with real connectivity.
- [ ] **Demonstrated live:** with airplane mode ON, tapping check-in updates the UI optimistically and the mutation is *paused* (visible in Query Devtools as `isPaused`), not failed.
- [ ] Turning airplane mode OFF fires the queued check-in automatically; the row reconciles to server truth.
- [ ] Double-tapping the same habit offline results in **exactly one** check-in on the server (idempotency verified by a backend test).
- [ ] A mid-session token expiry routes to login cleanly; after re-login no data is corrupted and the check-in state is correct.
- [ ] `npx tsc --noEmit`: 0 errors. Zero `any`. `context` in the mutation is fully typed.
- [ ] The offline/optimistic logic lives in hooks (`useCheckIn`, setup modules), not smeared into screen components.
- [ ] A short `challenges/challenge-01/README.md` explaining your Option A/B choice, how to reproduce the offline demo, and the token-expiry steps.

## Stretch

- Add a **subtle "pending sync" indicator** on rows whose check-in is queued but not yet confirmed (read `useMutationState` / `useIsMutating` filtered by the habit). Clear it on `onSettled`.
- Implement a real **refresh-token flow**: on `401`, attempt `POST /api/v1/auth/refresh` once to get a new access token and transparently retry the original request; only sign out if refresh also fails. This is the production answer to "token expired mid-session" and removes the forced re-login.
- **Queue multiple distinct check-ins offline** (three different habits), go online, and confirm all three flush and reconcile without races or lost updates.

## Hints

<details>
<summary>Why a paused mutation doesn't "fail" offline</summary>

With `onlineManager` wired and the default mutation `networkMode: 'online'`, Query sees the device is offline and **pauses** the mutation instead of running `mutationFn`. `onMutate` still runs (so your optimistic write happens), but the request waits. On reconnect, Query resumes paused mutations automatically. That's the whole mechanism — you mostly just have to wire `onlineManager` and not fight it.

</details>

<details>
<summary>Persisting the queue (Option A) sketch</summary>

```bash
npm install @tanstack/query-async-storage-persister @tanstack/react-query-persist-client @react-native-async-storage/async-storage
```

```typescript
import { persistQueryClient } from "@tanstack/react-query-persist-client";
import { createAsyncStoragePersister } from "@tanstack/query-async-storage-persister";
import AsyncStorage from "@react-native-async-storage/async-storage";

persistQueryClient({
  queryClient,
  persister: createAsyncStoragePersister({ storage: AsyncStorage }),
});
// Set a default mutation fn / mutation key so paused mutations can be resumed
// after a fresh launch via queryClient.resumePausedMutations().
```

Note: resuming paused mutations after a cold start requires a **default mutation function** registered for the mutation key, because the in-memory `mutationFn` closure is gone. Read the Query persistence docs carefully — this is the fiddly part.

</details>

<details>
<summary>Verifying idempotency on the backend</summary>

A clean approach: a unique constraint on `(habit_id, check_in_date)` in Postgres, and a controller that catches the duplicate and returns the existing check-in as `200` rather than `500`. Test:

```java
@Test
void checkingInTwiceForTheSameDayIsIdempotent() {
    var first = client.checkIn(habitId, today);
    var second = client.checkIn(habitId, today);
    assertThat(repo.countByHabitIdAndDate(habitId, today)).isEqualTo(1);
    assertThat(second.statusCode()).isIn(200, 409);
}
```

</details>

## Submission

Commit under `challenges/challenge-01/` in your week-9 repo. Record a 60-second screen capture of the **offline → tap → reconnect → reconcile** loop (the airplane-mode demo) and link it in the challenge README — this is the one part a written description can't prove. Make sure `npx tsc --noEmit` is clean on a fresh clone.

## Why this matters

- **Optimistic + offline** is the exact pattern behind every messaging app, every good to-do app, every habit tracker that doesn't infuriate users on bad connections.
- The **token-expiry recovery** is the integration failure that most often ships broken — apps that dump you to a blank screen when your session lapses. Handling it cleanly is a senior signal.
- In **week 10** you deploy this against a real hosted backend with real network latency and real cold starts. Everything you harden here pays off when the network is no longer `localhost`.
