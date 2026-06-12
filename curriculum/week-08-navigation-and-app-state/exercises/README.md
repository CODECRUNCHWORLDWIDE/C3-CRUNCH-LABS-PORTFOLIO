# Week 8 — Exercises

Three focused drills. The first is a guided, step-by-step walkthrough; the second and third are runnable code files with TODOs you fill in. Do them in order — later ones reuse types and patterns from earlier ones.

## Index

1. **[Exercise 1 — Typed stack + bottom tabs](exercise-01-stack-and-tabs.md)** — wire a typed native-stack navigator with a nested bottom-tab navigator, from a fresh Expo app. (~50 min)
2. **[Exercise 2 — A controlled, validated habit form](exercise-02-habit-form.tsx)** — fill in the TODOs to build a multi-field add-habit form with per-field validation, a disabled-until-valid submit, and keyboard handling. (~45 min)
3. **[Exercise 3 — A persisted Zustand session store](exercise-03-session-store.ts)** — implement a session store with a three-state `status`, SecureStore persistence, and rehydration on launch. (~40 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. Muscle memory is the entire point of these drills.
- Run it on a simulator or Expo Go (`npx expo start`, then `i`/`a`). See the screens. Tap the tabs. Watch the keyboard behave.
- Keep `tsc` honest: run `npx tsc --noEmit` and treat a type error as a failing build. **Zero `any` this week.** If you reach for `as any` to make a navigation call compile, you skipped a typing step — go back to Lecture 1 §4 and fix the types.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.

## The working checklist

By the time all three exercises are done, you should be able to tick every box:

- [ ] A fresh Expo + TypeScript app runs with React Navigation installed (`@react-navigation/native`, `native-stack`, `bottom-tabs`, plus the native peers via `npx expo install`).
- [ ] A `RootStackParamList` and a `MainTabParamList` exist; every screen reads `route.params` through a typed `Props`.
- [ ] `npx tsc --noEmit` passes with **zero errors and zero `any`**.
- [ ] Tapping a tab switches screens; tapping a habit pushes a detail screen with a working back button.
- [ ] The add-habit form disables submit until valid, shows per-field errors only after a field is touched, and the keyboard never covers the inputs.
- [ ] The session store starts in `status: 'loading'`, resolves to `authenticated`/`unauthenticated` after reading SecureStore, and `logout()` deletes the token.
- [ ] You can explain, for every piece of state in the app, which of the four kinds it is.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-08` to compare approaches.
