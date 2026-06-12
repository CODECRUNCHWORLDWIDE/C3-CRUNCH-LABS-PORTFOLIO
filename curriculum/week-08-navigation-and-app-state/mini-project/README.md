# Mini-Project — Crunch Tracker, Navigable and Stateful

> Grow last week's single-screen prototype into a real, navigable app: a login screen, a bottom-tab shell (Habits / Goals / Profile), typed routes throughout, a Zustand session store with the JWT persisted to SecureStore, and an auth gate that lands on the right place with no flicker. Still on **mock data** — the network arrives next week. This is the last "no backend" milestone, and it's the one that turns a demo into an app.

This mini-project compounds directly on **Week 7**, where you built the habit-list screen and the add-habit form against local mock data. You'll lift that work into a multi-screen, stateful structure. By Friday you'll have a TypeScript Expo app of ~700–900 lines that a user can log into, navigate, fill forms in, and log out of — and that's wired so that Week 9 only has to swap mock data for real API calls.

**Estimated time:** ~13 hours (split across Thursday, Friday, Saturday, Sunday in the suggested schedule).

---

## What you will build

Crunch Tracker, mobile client, milestone 2:

```
Login  ──(submit)──▶  Main (tabs)
                       ├─ Habits  ──▶  HabitList ──▶ HabitDetail (id)
                       │                     ▲
                       │                AddHabit (modal)
                       ├─ Goals   ──▶  GoalList  ──▶ GoalDetail (id)
                       └─ Profile ──▶  current user + Log out
```

- **Login screen** — controlled email/password form, validated, disabled-until-valid submit. On submit it logs in with a mock user/token (Week 9 calls the real endpoint).
- **Habits tab** — a stack: a `FlatList` of habits (from a mock store standing in for server state), tap to push a detail screen, an "Add habit" modal reusing Exercise 2's form.
- **Goals tab** — the same shape as Habits, one level shallower is fine (list + detail), proving you can reuse the pattern.
- **Profile tab** — shows the signed-in user (from the session store) and a **Log out** button.
- **Session store** — Exercise 3's Zustand store: three-state `status`, token in SecureStore, rehydrate on launch.
- **Auth gate** — the challenge's flicker-free, leak-free conditional-screens pattern.

By the end you'll have a public GitHub repo that runs on a simulator and a real device via Expo Go, with zero `any` and a clean state map.

---

## Rules

- **You may** read the React Navigation, Zustand, Expo, and React docs, the lecture notes, and your own Week 7 code.
- **You must** stay on **mock data** for habits and goals. Do **not** call a real network this week. The point is to get navigation and state right in isolation so Week 9 is a wiring job, not a rewrite. Put mock habits/goals in a clearly-named store (e.g. `useMockHabitsStore`) with a comment: `// SERVER STATE STAND-IN — replaced by TanStack Query in Week 9`.
- **You must** keep the four kinds of state separate (Lecture 2 §8). Session/UI state in Zustand or Context; habits/goals in the labeled mock store; ephemeral state local; navigation state in React Navigation.
- **You must** type everything. `npx tsc --noEmit` clean, **zero `any`**, every screen reads `route.params` through a typed `Props`.
- Target the current Expo SDK (52+, React Native 0.76+) and TypeScript 5.x.

---

## Acceptance criteria

- [ ] A new public GitHub repo named `c3-week-08-crunch-tracker-<yourhandle>` (or a `week-08` tag/branch on your ongoing Crunch Tracker mobile repo).
- [ ] Project layout is organized, not one giant `App.tsx`:
  ```
  crunch-tracker/
  ├── App.tsx                       (auth gate + NavigationContainer)
  ├── app.json                      (scheme: "crunchtracker")
  ├── src/
  │   ├── navigation/
  │   │   ├── types.ts              (RootStackParamList, MainTabParamList, HabitsStackParamList, GoalsStackParamList + global override)
  │   │   ├── RootNavigator.tsx
  │   │   ├── MainTabs.tsx
  │   │   ├── HabitsStack.tsx
  │   │   └── GoalsStack.tsx
  │   ├── screens/
  │   │   ├── Splash.tsx
  │   │   ├── LoginScreen.tsx
  │   │   ├── HabitListScreen.tsx
  │   │   ├── HabitDetailScreen.tsx
  │   │   ├── AddHabitScreen.tsx     (from Exercise 2)
  │   │   ├── GoalListScreen.tsx
  │   │   ├── GoalDetailScreen.tsx
  │   │   └── ProfileScreen.tsx
  │   ├── store/
  │   │   ├── useSessionStore.ts     (from Exercise 3)
  │   │   ├── useUiStore.ts          (modal open/close, toasts)
  │   │   └── useMockHabitsStore.ts  (SERVER STATE STAND-IN)
  │   └── domain/
  │       └── types.ts              (Habit, Goal, User, HabitDraft)
  └── README.md
  ```
- [ ] `npx tsc --noEmit` passes with **zero errors and zero `any`**.
- [ ] The auth gate passes the challenge's five adversarial checks (cold-launch logged-out, cold-launch logged-in, logout-then-back, kill-during-splash, corrupted-token relaunch).
- [ ] All three tabs render with icons; the Habits and Goals tabs each push a typed detail screen with a working back button.
- [ ] The "Add habit" modal opens, validates, and on submit adds a habit to the mock store and dismisses — the list updates.
- [ ] The Profile tab shows the signed-in user's `displayName` and `email` from the session store, and **Log out** clears the token and routes back to Login.
- [ ] At least one deep link works: `crunchtracker://habits/<id>` opens the matching Habit detail. Demonstrate with `npx uri-scheme`.
- [ ] Your `README.md` includes:
  - One paragraph describing the app and how to run it from a fresh clone.
  - **The state map**: a table listing every significant piece of state, which of the four kinds it is, and where it lives.
  - A note on **what changes in Week 9** — which mock store gets deleted and which screens start fetching.
  - A screen recording (GIF/video) of login → tabs → add habit → detail → logout.

---

## Suggested order of operations

Build incrementally. Each phase ends with a commit and a runnable app.

### Phase 1 — Scaffold + navigation shell (~2h)

Start from Exercise 1's navigators (reuse them). Add the Goals stack as a sibling of the Habits stack. Define all four param lists in `src/navigation/types.ts` with the global `RootParamList` override. Get the tabs rendering with placeholder screens.

Commit: `Navigation shell: typed root stack, tabs, per-tab stacks`.

### Phase 2 — Session store + auth gate (~3h)

Drop in Exercise 3's `useSessionStore` and `bootstrapSession`. Build `Splash.tsx` and `LoginScreen.tsx`. Wire the conditional-screens auth gate in `App.tsx` (the challenge). Run the five adversarial checks until all pass.

Commit: `Session store + flicker-free auth gate`.

### Phase 3 — Domain types + mock habits store (~1.5h)

In `src/domain/types.ts`:

```ts
export type Frequency = 'daily' | 'weekly';

export type Habit = {
  id: number;
  name: string;
  frequency: Frequency;
  target: number | null;
  streak: number;
};

export type Goal = {
  id: number;
  title: string;
  targetDate: string; // ISO date
  done: boolean;
};
```

In `src/store/useMockHabitsStore.ts` (clearly labeled as the server-state stand-in), seed 5–6 habits and expose `habits`, `addHabit(draft)`, and `getById(id)`. Keep it dead simple — it's deliberately throwaway.

Commit: `Domain types + mock habits store (server-state stand-in)`.

### Phase 4 — Habits tab: list, detail, add modal (~3h)

- `HabitListScreen`: a `FlatList` over `useMockHabitsStore`, each row taps to `navigate('HabitDetail', { habitId })`. A header button opens the Add modal.
- `HabitDetailScreen`: reads `habitId` from typed params, looks the habit up in the store by id (not from params — Lecture 1 §7), shows its detail. Set the header title from the loaded habit with `setOptions` in `useLayoutEffect`.
- `AddHabitScreen`: drop in your completed Exercise 2 form. Present it as a modal (`presentation: 'modal'`). On submit, call `addHabit(draft)`, close the modal, and confirm the list updated.

Commit: `Habits tab: list, typed detail, add-habit modal`.

### Phase 5 — Goals tab + Profile tab (~1.5h)

- `GoalList` / `GoalDetail`: mirror the Habits pattern with the `Goal` type. This phase exists to prove you internalized the pattern — it should feel mechanical now.
- `ProfileScreen`: read the user from `useSessionStore(selectUser)`, render `displayName` + `email`, and a Log out button that calls `logout()`.

Commit: `Goals tab + Profile with logout`.

### Phase 6 — Deep links + polish (~1.5h)

- Add the `linking` config so `crunchtracker://habits/:habitId` resolves. Add a `parse` so `habitId` arrives as a `number`. Test with `npx uri-scheme open crunchtracker://habits/2 --ios`.
- Run `npx tsc --noEmit`; fix any `any`.
- Write the README, including the **state map** table and the **Week 9 changes** note.
- Record the demo GIF.

Commit: `Deep links + README + state map`.

---

## The state map (you must produce this in your README)

This table is half the grade on "understands state." Fill it in for *your* app:

| State | Kind (nav / ephemeral / shared-client / server) | Where it lives | Week 9 fate |
|-------|------------------------------------------------|----------------|-------------|
| current tab / screen | navigation | React Navigation | unchanged |
| `status` / `token` / `user` | shared-client (session) | `useSessionStore` + SecureStore | login calls real API |
| add-habit modal open | shared-client (UI) | `useUiStore` | unchanged |
| add-habit form draft | ephemeral | `AddHabitScreen` local | unchanged |
| habit list / goal list | **server** | `useMockHabitsStore` (stand-in) | **deleted → TanStack Query** |
| habit-list scroll pos | ephemeral | screen local | unchanged |

If you can produce this table accurately, you've achieved the week's core learning objective.

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Runs + navigates | 20% | Fresh clone runs; all tabs, details, and the modal work on a simulator |
| Auth gate | 20% | All five adversarial checks pass; conditional-screens pattern; three-state status |
| Typed navigation | 15% | Every screen typed; no `any`; `tsc --noEmit` clean; typed params and navigate calls |
| State discipline | 20% | Four kinds kept separate; mock store clearly labeled; the state map is accurate |
| Forms | 10% | Add-habit form validates, disables submit, handles the keyboard, resets on submit |
| README + Week 9 readiness | 15% | State map present; the "what changes in Week 9" note is specific and correct |

---

## Stretch (optional)

- Split the Zustand store into a `sessionSlice` and a `uiSlice` using the slices pattern, combined into one store.
- Add the `persist` middleware to the session store (with the SecureStore adapter and `partialize`) instead of hand-rolling SecureStore calls in the actions.
- Add a `react-hook-form` + `zod` version of the add-habit form and compare the re-render count to your hand-rolled version (React DevTools' "highlight updates").
- Add a deep-link guard: a `crunchtracker://habits/2` link while logged out routes to login, then resumes to Habit #2 after a successful login.
- Add an optimistic "check in" button on the habit detail that bumps `streak` in the mock store instantly — this is the exact interaction Week 9 makes optimistic against the real API.

---

## What this prepares you for

- **Week 9 — Full-Stack Integration.** The `login` action, `bootstrapSession`, and the mock habits store are the three insertion points. Login calls `POST /api/v1/auth/login`; bootstrap validates against `GET /api/v1/auth/me`; the mock store is *deleted* and the Habits/Goals screens fetch from your Spring API via TanStack Query — with the JWT you persisted this week attached as a `Bearer` header.
- **Week 10 — Capstone.** This navigable, authenticated app is what you build, sign, and ship installable against the live backend.

Because you kept the four kinds of state separate and labeled the server-state stand-in, none of the navigation, forms, or session code has to change next week. That's the payoff of doing state on purpose.

---

## Resources

- *React Navigation — TypeScript*: <https://reactnavigation.org/docs/typescript/>
- *React Navigation — Auth flow*: <https://reactnavigation.org/docs/auth-flow>
- *Zustand — Introduction*: <https://zustand.docs.pmnd.rs/getting-started/introduction>
- *Expo SecureStore*: <https://docs.expo.dev/versions/latest/sdk/securestore/>
- *Expo — Linking / deep links*: <https://docs.expo.dev/guides/linking/>

---

## Submission

When done:

1. Push your repo to GitHub with a public URL.
2. Make sure `npx tsc --noEmit` is clean and `npx expo start` runs on a fresh clone.
3. Make sure the README has the **state map** and the **Week 9 changes** note, plus the demo recording.
4. Post the repo URL in your cohort tracker. You turned a prototype into an app — show it.
