# Week 7 — Exercises

Short, focused drills. Each one should take 30–50 minutes. Do them in order; later ones assume earlier ones.

## Index

1. **[Exercise 1 — Scaffold and run](exercise-01-scaffold-and-run.md)** — `create-expo-app`, run on a simulator and a real device, write your first typed component, see `0 tsc errors`. (~40 min)
2. **[Exercise 2 — The HabitCard component](exercise-02-habit-card.tsx)** — fill in TODOs in a typed, reusable `HabitCard` using props, conditional rendering, `Pressable`, and `StyleSheet`. (~45 min)
3. **[Exercise 3 — The habit-list screen](exercise-03-habit-list-screen.tsx)** — fill in TODOs for a `FlatList` screen with `useState`, a load-on-mount `useEffect`, immutable updates, and loading/empty states. (~50 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. Muscle memory is the entire point of these drills.
- Run it on a device or simulator. See it render. Read the LogBox error if it threw.
- Keep `npx tsc --noEmit` running in a second terminal. A type error means you're not done.
- If you get stuck for more than 15 minutes, peek at the inline hints at the bottom of each file.
- Every exercise must end with:
  - `npx tsc --noEmit` printing **0 errors**.
  - **No `any` types** anywhere you wrote.
  - **No LogBox warnings** (yellow or red) when the screen renders.

## Checklist before you call the week's exercises done

- [ ] Exercise 1: a `blank-typescript` Expo app runs on a simulator **and** a real device via Expo Go.
- [ ] Exercise 1: your first typed component renders, and `npx tsc --noEmit` is clean.
- [ ] Exercise 2: `HabitCard` is fully typed, renders all three states (done / not done / has-streak), and uses `Pressable` with a 44pt touch target and an `accessibilityLabel`.
- [ ] Exercise 3: the screen loads mock data through an effect, shows a spinner while loading, an empty state when there are none, and toggles a habit immutably.
- [ ] All three: zero `tsc` errors, zero `any`, zero LogBox warnings.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-07` to compare.
