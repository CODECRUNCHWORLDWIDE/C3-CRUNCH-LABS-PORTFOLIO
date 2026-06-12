// Exercise 2 — useHabits query hooks
//
// Goal: Wrap the typed API client from exercise 1 in TanStack Query v5 hooks.
//       Practice query keys, useQuery, useMutation, and the invalidate-vs-
//       setQueryData decision. No optimistic updates yet — that's exercise 3.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
// 1. This file drops into your week-8 Expo project as:
//        src/hooks/useHabits.ts
//    It assumes:
//      - The `apiFetch` client + `habits.ts` resource module from exercise 1.
//      - `@tanstack/react-query` installed:  npm install @tanstack/react-query
//      - A <QueryClientProvider> wrapping your app (see Lecture 2, §2).
//
// 2. Fill in every body marked `// TODO`. Do NOT change the exported function
//    signatures — the screens call them by name.
//
// 3. Verify types: `npx tsc --noEmit` must be clean. Zero `any`.
//
// 4. Wire `useHabits()` into a real screen and confirm a real
//    GET /api/v1/habits → 200 in the network panel. Then add a habit through
//    `useCreateHabit()` and confirm the list refetches and shows it.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented; signatures unchanged.
//   [ ] `npx tsc --noEmit`: 0 errors, no `any`.
//   [ ] `useHabits()` returns typed habits from the live backend.
//   [ ] Creating a habit invalidates the list and the new item appears.
//   [ ] habitKeys is the single source of truth for query keys (no inline arrays).
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryResult,
  type UseMutationResult,
} from "@tanstack/react-query";
import { getHabits, createHabit, type Habit, type NewHabit } from "../api/habits";
import type { ApiError } from "../api/client";

// ----------------------------------------------------------------------------
// Query keys — the single source of truth for cache identity.
// Hierarchical so invalidating `all` also invalidates `list` and `detail`.
// ----------------------------------------------------------------------------

export const habitKeys = {
  all: ["habits"] as const,
  list: () => [...habitKeys.all, "list"] as const,
  detail: (id: string) => [...habitKeys.all, "detail", id] as const,
};

// ----------------------------------------------------------------------------
// Read: list the signed-in user's habits.
// ----------------------------------------------------------------------------

/**
 * Returns the TanStack Query result for the habit list. The screen consumes
 * `isPending`, `isError`, `error`, `data`, `refetch`, `isRefetching`.
 *
 * Requirements:
 *   - queryKey MUST be habitKeys.list()  (not an inline array)
 *   - queryFn MUST be getHabits
 *   - The error type is ApiError (Query infers it from getHabits' throw)
 */
export function useHabits(): UseQueryResult<Habit[], ApiError> {
  // TODO: return useQuery({ ... })
  throw new Error("TODO: implement useHabits with useQuery");
}

// ----------------------------------------------------------------------------
// Write: create a habit, then make the list reflect it.
// ----------------------------------------------------------------------------

/**
 * Returns a mutation that creates a habit. On success, the new habit must
 * appear in the list WITHOUT the caller doing anything.
 *
 * Implement it TWO ways and keep the one you prefer (comment out the other):
 *
 *   Approach A — invalidate (the safe default):
 *     onSuccess: () => qc.invalidateQueries({ queryKey: habitKeys.all })
 *
 *   Approach B — setQueryData (skip the round trip; the response IS the new habit):
 *     onSuccess: (created) =>
 *       qc.setQueryData<Habit[]>(habitKeys.list(), (old = []) => [...old, created])
 *
 * In a one-line comment, state which you kept and why.
 */
export function useCreateHabit(): UseMutationResult<Habit, ApiError, NewHabit> {
  const qc = useQueryClient();

  // TODO: return useMutation({ mutationFn: ..., onSuccess: ... })
  throw new Error("TODO: implement useCreateHabit with useMutation");
}

// ----------------------------------------------------------------------------
// DRIVER NOTES (not runnable here — this is a hook module).
// In a screen:
//
//   const habits = useHabits();
//   const create = useCreateHabit();
//
//   if (habits.isPending) return <Spinner/>;
//   if (habits.isError)  return <ErrorState message={toUserMessage(habits.error)} onRetry={habits.refetch}/>;
//   if (habits.data.length === 0) return <EmptyState/>;
//   // ...render FlatList of habits.data, with onRefresh={habits.refetch}
//
//   // On the add screen:
//   create.mutate({ name, cadence: "DAILY" }, { onSuccess: () => navigation.goBack() });
//
// EXPECTED NETWORK PANEL after adding a habit (Approach A):
//   POST /api/v1/habits  → 201  Authorization: Bearer eyJ…
//   GET  /api/v1/habits  → 200  Authorization: Bearer eyJ…   (now includes the new habit)
//
// With Approach B you'll see the POST but NO follow-up GET — the cache was
// updated directly. Both are correct; know the difference.
// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// useHabits:
//   export function useHabits(): UseQueryResult<Habit[], ApiError> {
//     return useQuery({
//       queryKey: habitKeys.list(),
//       queryFn: getHabits,
//     });
//   }
//
// useCreateHabit (Approach A — invalidate):
//   export function useCreateHabit(): UseMutationResult<Habit, ApiError, NewHabit> {
//     const qc = useQueryClient();
//     return useMutation({
//       mutationFn: (body: NewHabit) => createHabit(body),
//       onSuccess: () => {
//         qc.invalidateQueries({ queryKey: habitKeys.all });
//       },
//     });
//   }
//
// useCreateHabit (Approach B — setQueryData):
//   onSuccess: (created) => {
//     qc.setQueryData<Habit[]>(habitKeys.list(), (old = []) => [...old, created]);
//   }
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// The screens never touch fetch, never touch the cache, never manage loading
// flags. They consume hooks. That separation — resource module (exercise 1) →
// query hooks (this file) → screen — is the architecture the mini-project
// scales. Get the keys and the invalidate/setQueryData decision right here and
// the optimistic update in exercise 3 is a small extension, not a rewrite.
// ----------------------------------------------------------------------------
