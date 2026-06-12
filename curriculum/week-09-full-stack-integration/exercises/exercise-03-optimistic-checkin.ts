// Exercise 3 — Optimistic check-in
//
// Goal: Implement the headline skill of the week — an optimistic mutation that
//       updates the UI instantly, rolls back on server failure, and reconciles
//       with the authoritative response. Type-safe end to end, no `any`.
//
// Estimated time: 50 minutes.
//
// HOW TO USE THIS FILE
//
// 1. Drops into your week-8 Expo project as:
//        src/hooks/useCheckIn.ts
//    Assumes exercise 1 (`apiFetch`) and exercise 2 (`habitKeys`) are in place,
//    and the backend has a check-in endpoint:
//        POST /api/v1/habits/{id}/check-ins   body: { date: "YYYY-MM-DD" }
//        → returns the updated Habit (checkedInToday: true, streak recomputed)
//
// 2. Fill in the FOUR TODOs inside useMutation: mutationFn, onMutate, onError,
//    onSettled. Do NOT change the exported signature.
//
// 3. `npx tsc --noEmit` must be clean. Zero `any`.
//
// 4. Test the three paths against the REAL backend:
//      a) Happy path: tap → row flips instantly → server confirms (no flicker).
//      b) Rollback:   point at a habit that 409s (already checked in today) or
//         temporarily make the backend return 500 → row flips, then snaps back.
//      c) Reconcile:  after success, the streak count updates to the server's
//         value via the onSettled invalidation, even though onMutate only set
//         checkedInToday.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All four TODOs implemented; signature unchanged.
//   [ ] `npx tsc --noEmit`: 0 errors, no `any`. `context` is typed (not any).
//   [ ] onMutate cancels in-flight list queries before snapshotting.
//   [ ] On server failure the optimistic change is rolled back.
//   [ ] onSettled invalidates so the UI reconciles with server truth.
//   [ ] You verified the no-flicker happy path on a real device/simulator.
//
// Hints at the bottom. Don't peek for 15 minutes.

import {
  useMutation,
  useQueryClient,
  type UseMutationResult,
} from "@tanstack/react-query";
import { apiFetch } from "../api/client";
import type { ApiError } from "../api/client";
import type { Habit } from "../api/habits";
import { habitKeys } from "./useHabits";

// ----------------------------------------------------------------------------
// Types
// ----------------------------------------------------------------------------

export interface CheckInVars {
  habitId: string;
  date: string; // "YYYY-MM-DD"
}

// The context we pass from onMutate -> onError so we can roll back.
interface CheckInContext {
  previous: Habit[] | undefined;
}

// The server call. Returns the authoritative updated habit.
const postCheckIn = (vars: CheckInVars): Promise<Habit> =>
  apiFetch<Habit>(`/api/v1/habits/${vars.habitId}/check-ins`, {
    method: "POST",
    body: { date: vars.date },
  });

// ----------------------------------------------------------------------------
// The hook
// ----------------------------------------------------------------------------

/**
 * Optimistic check-in. The four callbacks, in lifecycle order:
 *   mutationFn — fire the request.
 *   onMutate   — BEFORE the request: cancelQueries, snapshot, optimistic write,
 *                return the snapshot as context.
 *   onError    — on failure: restore the snapshot from context.
 *   onSettled  — always: invalidate the list to reconcile with server truth.
 */
export function useCheckIn(): UseMutationResult<
  Habit,
  ApiError,
  CheckInVars,
  CheckInContext
> {
  const qc = useQueryClient();

  return useMutation<Habit, ApiError, CheckInVars, CheckInContext>({
    // TODO 1: mutationFn — call postCheckIn.
    mutationFn: postCheckIn,

    // TODO 2: onMutate — async (vars) => { ... return { previous }; }
    //   - await qc.cancelQueries on the list key (so a slow GET can't clobber us)
    //   - snapshot: const previous = qc.getQueryData<Habit[]>(habitKeys.list())
    //   - optimistic write: setQueryData mapping the matching habit to
    //       { ...h, checkedInToday: true }
    //   - return { previous }
    onMutate: async (_vars: CheckInVars): Promise<CheckInContext> => {
      throw new Error("TODO 2: implement onMutate (cancel, snapshot, optimistic write)");
    },

    // TODO 3: onError — (_err, _vars, context) => { roll back from context.previous }
    onError: (_err, _vars, _context) => {
      throw new Error("TODO 3: implement onError rollback");
    },

    // TODO 4: onSettled — invalidate habitKeys.list() to reconcile with the server.
    onSettled: () => {
      throw new Error("TODO 4: implement onSettled reconcile");
    },
  });
}

// ----------------------------------------------------------------------------
// DRIVER NOTES (not runnable here).
// In a row component:
//
//   const checkIn = useCheckIn();
//   <Pressable onPress={() => checkIn.mutate({ habitId: habit.id, date: todayIso() })}>
//     <CheckMark filled={habit.checkedInToday} />
//   </Pressable>
//
// HAPPY PATH (network panel + UI):
//   - UI: the checkmark fills INSTANTLY (onMutate setQueryData), before the request.
//   - POST /api/v1/habits/{id}/check-ins → 200  Authorization: Bearer eyJ…
//   - onSettled fires: GET /api/v1/habits → 200 (streak now matches server).
//
// ROLLBACK PATH:
//   - UI: checkmark fills instantly, then EMPTIES when the 409/500 returns.
//   - onError restored the snapshot; onSettled's refetch confirms the rollback.
// ----------------------------------------------------------------------------

// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// onMutate:
//   onMutate: async (vars) => {
//     await qc.cancelQueries({ queryKey: habitKeys.list() });
//     const previous = qc.getQueryData<Habit[]>(habitKeys.list());
//     qc.setQueryData<Habit[]>(habitKeys.list(), (old = []) =>
//       old.map((h) => (h.id === vars.habitId ? { ...h, checkedInToday: true } : h)),
//     );
//     return { previous };
//   },
//
// onError:
//   onError: (_err, _vars, context) => {
//     if (context?.previous) {
//       qc.setQueryData(habitKeys.list(), context.previous);
//     }
//   },
//
// onSettled:
//   onSettled: () => {
//     qc.invalidateQueries({ queryKey: habitKeys.list() });
//   },
//
// ----------------------------------------------------------------------------
// WHY cancelQueries MATTERS
// ----------------------------------------------------------------------------
//
// If a background refetch of the list was already in flight when the user
// tapped, it could resolve AFTER your optimistic setQueryData and overwrite it
// with stale "not checked in" data — the checkmark visibly flickers off then on.
// cancelQueries in onMutate kills that in-flight GET so your optimistic write
// is the last word until onSettled deliberately refetches. This is the #1
// optimistic-update bug; the one line prevents it.
//
// ----------------------------------------------------------------------------
// WHY onSettled INVALIDATES INSTEAD OF TRUSTING onMutate
// ----------------------------------------------------------------------------
//
// onMutate only set checkedInToday: true. But the server may also recompute a
// streak count, a "best streak," a completion percentage — fields you cannot
// derive correctly on the client. Invalidating in onSettled pulls the
// authoritative object so those server-computed fields are correct. Optimistic
// = instant guess; reconcile = eventual truth. You need both.
// ----------------------------------------------------------------------------
