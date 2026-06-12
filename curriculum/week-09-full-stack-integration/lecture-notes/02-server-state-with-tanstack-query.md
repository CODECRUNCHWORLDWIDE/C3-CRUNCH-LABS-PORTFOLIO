# Lecture 2 — Server State with TanStack Query

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can fetch server data with `useQuery`, reason about query keys and the cache lifecycle (`staleTime` vs `gcTime`), mutate with `useMutation` and choose correctly between invalidation and direct cache writes, implement a type-safe optimistic update with rollback, and render the four states every networked screen must handle: loading, empty, error, offline.

If you only remember one thing from this lecture, remember this:

> **Server state is not your state.** It lives on the backend; you hold a cached copy that is always, by definition, a little out of date. The entire job of TanStack Query is managing that lie gracefully — fetching, caching, invalidating, and reconciling — so you stop writing `useState` + `useEffect` + a loading flag + an error flag + a refetch function by hand, badly, in every component.

---

## 1. Why not just `useEffect`?

You already know how to fetch in a component. It looks like this, and it is wrong in about six ways:

```typescript
// The hand-rolled pattern. Don't.
function HabitList() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getHabits()
      .then(setHabits)
      .catch((e) => setError(toUserMessage(e)))
      .finally(() => setLoading(false));
  }, []);
  // ...
}
```

What's missing, every time:

- **No caching.** Navigate away and back, it refetches from scratch and flashes a spinner. Two screens that need habits fetch twice.
- **No deduplication.** Three components mount that each need the user — three identical in-flight requests.
- **No background refresh.** The data is fetched once and rots. The user pulls down to refresh and you write *that* by hand too.
- **No request cancellation.** The screen unmounts mid-fetch; the `setHabits` fires on an unmounted component (or you write the cleanup by hand).
- **Stale closures and race conditions.** Two fetches in flight, the slow one resolves last, you render stale data. Now you're tracking request IDs by hand.
- **It's copy-pasted into every screen.** Loading flag, error flag, the same five lines, slightly different each time, each with its own subtle bug.

TanStack Query solves all of that once, correctly, for the whole app. The hand-rolled version is the thing you stop writing in week 9.

---

## 2. Setup

```bash
npm install @tanstack/react-query
```

Create one `QueryClient` and wrap the app once, at the root:

```typescript
// src/api/queryClient.ts
import { QueryCache, QueryClient } from "@tanstack/react-query";
import { ApiError } from "./client";
import { useAuthStore } from "../auth/store";

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,          // 30s: data is "fresh" this long; no refetch on remount
      gcTime: 5 * 60_000,         // 5min: unused cache entries live this long
      retry: (failureCount, error) => {
        // Don't retry auth or client errors — only flaky network/5xx, twice.
        if (error instanceof ApiError && (error.kind === "unauthorized" || error.status === 404)) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: { retry: 0 },
  },
  queryCache: new QueryCache({
    onError: (error) => {
      if (error instanceof ApiError && error.kind === "unauthorized") {
        useAuthStore.getState().signOut();
      }
    },
  }),
});
```

```tsx
// App.tsx
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "./src/api/queryClient";

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      {/* your week-8 navigation tree */}
    </QueryClientProvider>
  );
}
```

That's the whole setup. One client, one provider, sensible defaults. Notice we wired the **`401` → sign-out** behavior here (from Lecture 1, §5) so it's truly global.

---

## 3. `useQuery` — fetching, the right way

Here is the hand-rolled `HabitList` from §1, rewritten:

```tsx
// src/screens/HabitListScreen.tsx
import { useQuery } from "@tanstack/react-query";
import { getHabits, type Habit } from "../api/habits";
import { toUserMessage } from "../api/errorMessage";

export function HabitListScreen() {
  const { data, isPending, isError, error, refetch, isRefetching } = useQuery({
    queryKey: ["habits"],
    queryFn: getHabits,
  });

  if (isPending) return <FullScreenSpinner />;
  if (isError) return <ErrorState message={toUserMessage(error)} onRetry={refetch} />;
  if (data.length === 0) return <EmptyState message="No habits yet. Add your first." />;

  return (
    <FlatList
      data={data}
      keyExtractor={(h) => h.id}
      onRefresh={refetch}
      refreshing={isRefetching}
      renderItem={({ item }) => <HabitRow habit={item} />}
    />
  );
}
```

Three lines of `useQuery` replaced a `useState` × 3 + `useEffect` + error string juggling — and gained caching, dedup, background refresh, cancellation, and pull-to-refresh for free.

The status fields you actually use:

| Field | Meaning |
|-------|---------|
| `isPending` | No cached data yet *and* a fetch is happening. The "first load" state. (v4 called this `isLoading`.) |
| `isError` / `error` | The `queryFn` threw. `error` is your typed `ApiError`. |
| `data` | The cached result. After `isPending` is false and `isError` is false, this is non-null — TypeScript narrows it. |
| `isRefetching` | A background refetch is in flight while `data` is already shown. Wire it to `FlatList`'s `refreshing`. |
| `refetch` | Imperatively trigger a refetch (retry button, pull-to-refresh). |

> **v5 naming gotcha.** If a tutorial uses `isLoading`, it's pre-v5. In v5, the "no data yet, fetching" state is **`isPending`**. `isLoading` still exists but now means `isPending && isFetching`, which is almost always what `isPending` already gives you. Use `isPending`.

---

## 4. Query keys — the one concept to get right

A query key is the array that **identifies a cache entry**. `['habits']` is one entry. `['habits', userId]` is a different entry per user. `['habit', habitId]` is one entry per habit. Same key → same cache → shared data, shared refetch, automatic dedup.

```typescript
// A small key factory keeps keys consistent and typo-free across the app.
export const habitKeys = {
  all: ["habits"] as const,
  list: () => [...habitKeys.all, "list"] as const,
  detail: (id: string) => [...habitKeys.all, "detail", id] as const,
};
```

Three rules:

1. **Keys must be serializable and stable.** Arrays of strings/numbers/plain objects. Don't put a `Date` or a function in a key.
2. **Everything that changes the data must be in the key.** If habits are per-user, the user id belongs in the key. If they're filtered, the filter belongs in the key. Otherwise two different result sets fight over one cache slot.
3. **Keys are hierarchical for invalidation.** `invalidateQueries({ queryKey: ['habits'] })` invalidates `['habits']`, `['habits','list']`, *and* `['habits','detail', x]` — anything that starts with `['habits']`. That partial-match behavior is why the factory above nests everything under `all`.

Get keys right and caching, dedup, and invalidation all fall out for free. Get them wrong and you'll fight phantom stale data all week.

---

## 5. `staleTime` vs `gcTime` — the lifecycle

These two are the most-confused settings in the library. They control different clocks.

- **`staleTime`** — how long fetched data is considered **fresh**. While fresh, remounting a component or refocusing the app will **not** refetch; Query serves the cache instantly. Default is `0` — meaning data is stale immediately, so Query refetches in the background on every remount (you still see cached data instantly; it just revalidates). We set `30_000` so navigating habits → goals → habits within 30s doesn't refetch.

- **`gcTime`** (garbage-collection time, was `cacheTime` in v4) — how long an **unused** cache entry (no component is observing it) survives before Query deletes it. Default 5 minutes. If you leave a screen and come back within `gcTime`, the old data is still cached (possibly stale, triggering a background refetch); after `gcTime`, it's gone and you get a fresh `isPending`.

A concrete way to feel the difference: `staleTime` is "how long before I bother asking the server again," `gcTime` is "how long I keep the answer around after nobody's looking." They're orthogonal. Tune `staleTime` for freshness/UX; leave `gcTime` near its default unless you have a memory reason to change it.

---

## 6. `useMutation` — changing server state

Reads use `useQuery`; writes use `useMutation`. Creating a habit:

```tsx
// src/hooks/useCreateHabit.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createHabit, type NewHabit } from "../api/habits";
import { habitKeys } from "../api/keys";

export function useCreateHabit() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: NewHabit) => createHabit(body),
    onSuccess: () => {
      // The list on the server changed; tell Query its cache is stale.
      qc.invalidateQueries({ queryKey: habitKeys.all });
    },
  });
}
```

```tsx
// In the add-habit screen:
const { mutate, isPending } = useCreateHabit();

function onSubmit(form: NewHabit) {
  mutate(form, {
    onSuccess: () => navigation.goBack(),
    onError: (e) => Alert.alert("Couldn't add habit", toUserMessage(e)),
  });
}
// ...
<Button title="Add" onPress={() => onSubmit(values)} disabled={isPending} />
```

The mutation gives you `isPending` (disable the button while saving), `isError`/`error`, and `mutate`/`mutateAsync`. The important architectural move is `onSuccess`: after the server confirms, **invalidate** the affected queries so the list refetches and shows the new habit.

---

## 7. Invalidation vs `setQueryData` — which and when

You have two ways to update the cache after a mutation, and choosing correctly is a senior-level distinction.

**`invalidateQueries`** — "the server's truth changed; go re-ask." Query marks the matching entries stale and refetches them. Costs a round trip; guarantees correctness. Use this as your **default** — it's the honest move.

```typescript
qc.invalidateQueries({ queryKey: habitKeys.all });
```

**`setQueryData`** — "I already know the new state; write it into the cache directly, no fetch." Costs nothing; risks drift if your local computation disagrees with what the server actually did. Use this when the mutation **response gives you the authoritative new object** and you want to avoid a refetch, or for **optimistic updates** (next section).

```typescript
// After createHabit resolves, the response IS the new habit. Splice it in, skip the refetch.
onSuccess: (created) => {
  qc.setQueryData<Habit[]>(habitKeys.list(), (old = []) => [...old, created]);
}
```

Rule of thumb: **invalidate when in doubt; `setQueryData` when you have the authoritative object and care about the round trip.** Mixing them is fine — a common pattern is `setQueryData` for instant feedback *and* `invalidateQueries` to reconcile shortly after.

---

## 8. Optimistic updates — the week's headline skill

The challenge and a chunk of the mini-project hinge on this. The scenario: the user taps "check in" on a habit. We want the UI to flip to "checked in" **instantly** — not after a 200ms round trip — and to **roll back** if the server rejects it.

The pattern has four callbacks, and the order matters:

```typescript
// src/hooks/useCheckIn.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "../api/client";
import { habitKeys } from "../api/keys";
import type { Habit } from "../api/habits";

interface CheckInVars { habitId: string; date: string }

// The server endpoint; returns the updated habit (with today's check-in recorded).
const postCheckIn = (v: CheckInVars) =>
  apiFetch<Habit>(`/api/v1/habits/${v.habitId}/check-ins`, { method: "POST", body: { date: v.date } });

export function useCheckIn() {
  const qc = useQueryClient();

  return useMutation({
    mutationFn: postCheckIn,

    // 1. BEFORE the request: snapshot + apply the optimistic change.
    onMutate: async (vars) => {
      // Cancel in-flight refetches so they don't clobber our optimistic write.
      await qc.cancelQueries({ queryKey: habitKeys.list() });

      // Snapshot the current cache so we can roll back.
      const previous = qc.getQueryData<Habit[]>(habitKeys.list());

      // Optimistically mark this habit checked-in for today.
      qc.setQueryData<Habit[]>(habitKeys.list(), (old = []) =>
        old.map((h) =>
          h.id === vars.habitId ? { ...h, checkedInToday: true } : h,
        ),
      );

      // Pass the snapshot to onError via context.
      return { previous };
    },

    // 2. ON FAILURE: roll back to the snapshot.
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        qc.setQueryData(habitKeys.list(), context.previous);
      }
    },

    // 3. ALWAYS (success or error): reconcile with the server's authoritative truth.
    onSettled: () => {
      qc.invalidateQueries({ queryKey: habitKeys.list() });
    },
  });
}
```

Read the lifecycle out loud, because the mental model is the whole point:

1. **`onMutate` runs synchronously before the request fires.** It cancels in-flight refetches (so a slow GET can't overwrite the optimistic write a moment later), snapshots the current cache, applies the optimistic change, and returns the snapshot as `context`.
2. **`onError` runs if the request rejects.** It restores the snapshot from `context.previous`. The UI snaps back to truth. The user sees their tap "undo" itself — that's correct; the server said no.
3. **`onSettled` runs after either outcome.** It invalidates the list, forcing a refetch of the *authoritative* state. On success this reconciles any difference between our optimistic guess and what the server actually recorded (e.g. a server-computed streak count). On error it confirms the rollback against reality.

Why `cancelQueries` matters: imagine a background refetch was already in flight when the user tapped. Without `cancelQueries`, that older GET could resolve *after* your optimistic write and overwrite it with stale "not checked in" data — the tap would visibly flicker back. Cancelling in `onMutate` closes that race. This is the single most common optimistic-update bug, and it's why the line is there.

> **Type safety end to end.** `CheckInVars` types the mutation input. `Habit[]` types every `setQueryData` updater. `context` is inferred from `onMutate`'s return. There is no `any` anywhere in this hook, and that's the bar for the challenge: a reviewer should be able to hover any variable and get a real type.

---

## 9. The four states every networked screen owes the user

A mocked-data screen has one state: data. A networked screen has four, and skipping any of them is the difference between a demo and a product.

1. **Loading** — `isPending`. Show a spinner or skeletons. Never a blank screen.
2. **Empty** — `data.length === 0`. A real list with zero items is *not* an error and *not* loading. Show an empty state with a call to action ("No habits yet — add your first"). The most-skipped state, and the one reviewers notice immediately.
3. **Error** — `isError`. Show `toUserMessage(error)` and a **retry button wired to `refetch`**. An error with no way to recover is a dead end.
4. **Offline** — the device has no connection. Distinct from "error" because the fix is "reconnect," not "retry now."

For offline, integrate Query's `onlineManager` with React Native's net-info so Query knows when the device is actually online (it can't detect this on its own in RN):

```typescript
// src/api/onlineManager.ts — run once at startup
import NetInfo from "@react-native-community/netinfo";
import { onlineManager } from "@tanstack/react-query";

onlineManager.setEventListener((setOnline) =>
  NetInfo.addEventListener((state) => {
    setOnline(Boolean(state.isConnected));
  }),
);
```

With that wired, Query **pauses** mutations and queries while offline and **resumes** them automatically when the connection returns — which is exactly the behavior the challenge asks you to demonstrate with an optimistic check-in performed in airplane mode.

A reusable wrapper keeps screens honest:

```tsx
function QueryStates<T>({
  query,
  empty,
  children,
}: {
  query: UseQueryResult<T[]>;
  empty: React.ReactNode;
  children: (data: T[]) => React.ReactNode;
}) {
  if (query.isPending) return <FullScreenSpinner />;
  if (query.isError) return <ErrorState message={toUserMessage(query.error)} onRetry={query.refetch} />;
  if (query.data.length === 0) return <>{empty}</>;
  return <>{children(query.data)}</>;
}
```

If every list screen routes through `QueryStates`, you cannot *forget* a state — the type won't let you fall through.

---

## 10. Putting it together — a habits screen with create and check-in

```tsx
function HabitsScreen() {
  const habits = useQuery({ queryKey: habitKeys.list(), queryFn: getHabits });
  const checkIn = useCheckIn();

  return (
    <QueryStates query={habits} empty={<EmptyState message="No habits yet." />}>
      {(data) => (
        <FlatList
          data={data}
          keyExtractor={(h) => h.id}
          onRefresh={habits.refetch}
          refreshing={habits.isRefetching}
          renderItem={({ item }) => (
            <HabitRow
              habit={item}
              onCheckIn={() =>
                checkIn.mutate({ habitId: item.id, date: todayIso() })
              }
            />
          )}
        />
      )}
    </QueryStates>
  );
}
```

Tap a row: the check-in flips instantly (optimistic), the request fires, and either the server confirms (reconcile, no visible change) or rejects (roll back, the row snaps off). Pull down: background refetch. Open the screen with no habits: empty state. Lose connection mid-tap: the mutation pauses and resumes when you're back. That is a production-grade networked screen, and it's maybe forty lines because Query carries the weight.

---

## 11. Recap

You should now be able to:

- Explain why server state needs a dedicated cache and what `useEffect`-fetching gets wrong.
- Set up a `QueryClient` with sensible `staleTime`, `gcTime`, retry, and a global `401` handler.
- Fetch with `useQuery` and consume `isPending` / `isError` / `data` / `refetch` correctly (v5 naming).
- Design hierarchical query keys and reason about partial-match invalidation.
- Distinguish `staleTime` from `gcTime`.
- Mutate with `useMutation` and choose between `invalidateQueries` and `setQueryData`.
- Implement a type-safe optimistic update with `onMutate` snapshot, `onError` rollback, and `onSettled` reconcile — and explain why `cancelQueries` prevents the flicker.
- Render loading, empty, error, and offline states so no screen ever shows a silent failure.

Next, do the exercises — three drills that build the client, the queries, and the optimistic mutation in isolation before the mini-project assembles them.

---

## References

- *TanStack Query — Queries*: <https://tanstack.com/query/latest/docs/framework/react/guides/queries>
- *TanStack Query — Mutations*: <https://tanstack.com/query/latest/docs/framework/react/guides/mutations>
- *TanStack Query — Optimistic Updates*: <https://tanstack.com/query/latest/docs/framework/react/guides/optimistic-updates>
- *TanStack Query — Query Keys*: <https://tanstack.com/query/latest/docs/framework/react/guides/query-keys>
- *TanStack Query — Important Defaults*: <https://tanstack.com/query/latest/docs/framework/react/guides/important-defaults>
- *TanStack Query — React Native*: <https://tanstack.com/query/latest/docs/framework/react/react-native>
- *TkDodo — Practical React Query*: <https://tkdodo.eu/blog/practical-react-query>
