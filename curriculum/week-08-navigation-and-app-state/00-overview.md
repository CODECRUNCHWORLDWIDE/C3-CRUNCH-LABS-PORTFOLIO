# Week 8 — Navigation and App State

Welcome to **C3 · Crunch Labs Portfolio**, Week 8. Last week you built your first React Native screens with Expo and TypeScript: a habit list, an add-habit form, all against mock data, all on one screen at a time. That was the "can I render a component" week. This is the "is this actually an app" week.

A real app is more than one screen. The moment you have a login screen, a tab bar, a detail view, and a back button, you are in the territory of **navigation** and **shared state** — and both of them will bite you if you wing it. By Friday you can wire a typed stack-and-tab navigator with React Navigation, build a controlled multi-field form that validates before it submits, lift session and UI state into a Zustand store, and persist a token to the device's secure keychain — all in TypeScript with no `any`.

We are building toward Week 9, where this app finally talks to the Spring Boot + Postgres backend you shipped in Weeks 4–6. So this week we draw a hard line that most tutorials blur: **local/UI state is not server state**. We manage the first deliberately (Context, Zustand, SecureStore) and we leave a clean seam where the second will slot in next week (TanStack Query). Getting that boundary right now is what keeps Week 9 from being a rewrite.

The one sentence to internalize before we start:

> **Navigation is state, and not all state is the same.** Which screen you're on, who's logged in, what a form's draft values are, and what the server thinks your habits are — those are four different kinds of state with four different owners. Confuse them and you get flicker, stale data, and back-button leaks. Keep them separate and the app scales.

## Learning objectives

By the end of this week, you will be able to:

- **Install and configure** React Navigation in an Expo Router or `@react-navigation/native` project, including the native stack and bottom-tab navigators.
- **Type your routes** end to end — a `RootStackParamList`, typed `navigation.navigate(...)` calls, and typed `route.params` with no casts.
- **Compose navigators**: a stack inside a tab, a tab inside an auth-gated stack, and understand why the nesting order matters.
- **Build a controlled, multi-field form** with per-field validation, error messages, a disabled-until-valid submit button, and keyboard handling that doesn't cover the inputs.
- **Distinguish** the four kinds of state — navigation state, ephemeral component state, shared client state, and server state — and pick the right tool for each.
- **Lift shared state** into React Context for low-frequency values and into a **Zustand** store for everything that changes often, without triggering needless re-renders.
- **Persist a JWT** to `expo-secure-store` (the Keychain on iOS, Keystore-backed `EncryptedSharedPreferences` on Android) and rehydrate it on launch.
- **Implement an auth-gated navigation flow**: unauthenticated users see login, authenticated users see the tabs, logout clears state and routes back — with no flicker and no way to swipe back into a protected screen.
- **Reason about deep links**: map a URL like `crunchtracker://habits/42` to a screen and its params, and test it from the terminal.

## Prerequisites

This week assumes you have completed **C3 Weeks 1–7**, or have equivalent experience. Specifically:

- You finished Week 7: you can scaffold an Expo + TypeScript app, write typed function components, manage local state with `useState`/`useEffect`, and render a `FlatList`.
- You can read and write modern TypeScript: union types, generics, discriminated unions, and `as const`. We use all four this week.
- You have Node 20+ and the Expo tooling working (`npx expo start` launches and the app loads on a simulator or Expo Go).
- You remember, conceptually, the JWT login endpoint you built in Week 6 (`POST /api/v1/auth/login` returning `{ token, user }`). We mock its response this week; Week 9 calls it for real.

You do **not** need any prior React Navigation or Zustand experience. We start from `npm install`.

## Topics covered

- The React Navigation 7 mental model: navigators, screens, the navigation state tree, and how `NavigationContainer` owns it.
- `createNativeStackNavigator` vs `createBottomTabNavigator` — what each is for and how they nest.
- Typed navigation: `ParamList` types, `NativeStackScreenProps<T, 'Screen'>`, the global `RootParamList` declaration-merge trick, and typed hooks (`useNavigation`, `useRoute`).
- Passing params, reading params, and updating params (`navigation.setParams`).
- Controlled inputs in React Native: `value` + `onChangeText`, why uncontrolled inputs fight you on mobile, and `KeyboardAvoidingView`.
- Lightweight form validation without a library, then a note on where `react-hook-form` + `zod` earn their keep.
- The four-quadrant state model: navigation / ephemeral / shared-client / server.
- React Context: the right tool for theme, locale, and rarely-changing session facts; the re-render footgun and how to avoid it.
- Zustand: a 1-KB store, selectors, actions, `persist` middleware, and why it beats Context for high-frequency state.
- `expo-secure-store`: writing, reading, and deleting a token; what "secure" actually means on each platform.
- The auth-gating pattern: a "hydrating" splash state, conditional navigators, and clearing state on logout.
- Deep linking: the `linking` config, URL-to-route mapping, and testing with `npx uri-scheme`.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract.

| Day       | Focus                                                | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|------------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | React Navigation: stacks, tabs, the nav state tree   |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | Typed routes, params, nesting, deep links            |    1h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0h      |     5.5h    |
| Wednesday | Controlled forms, validation, keyboard handling      |    1h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0.5h    |     6h      |
| Thursday  | State on purpose: Context, Zustand, server vs client |    2h    |    1h     |     0h     |    0.5h   |   1h     |     1.5h     |    0h      |     6h      |
| Friday    | SecureStore, auth gating; mini-project work          |    0h    |    1h     |     0h     |    0.5h   |   1h     |     3h       |    0.5h    |     6h      |
| Saturday  | Mini-project deep work                               |    0h    |    0h     |     0h     |    0h     |   0h     |     3.5h     |    0h      |     3.5h    |
| Sunday    | Quiz, review, polish                                 |    0h    |    0h     |     0h     |    1h     |   0h     |     1.5h     |    0h      |     2.5h    |
| **Total** |                                                      | **6h**   | **7.5h**  | **2h**     | **3.5h**  | **5h**   | **13h**      | **2h**     | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./00-overview.md) | This overview (you are here) |
| [resources.md](./01-resources.md) | Curated React Navigation, Zustand, Expo, and TypeScript links |
| [lecture-notes/01-multi-screen-navigation.md](./02-lecture-notes/01-multi-screen-navigation.md) | React Navigation, typed routes, stacks, tabs, nesting, deep links |
| [lecture-notes/02-managing-state-on-purpose.md](./02-lecture-notes/02-managing-state-on-purpose.md) | The four kinds of state; Context vs Zustand; SecureStore; why server state is different |
| [exercises/README.md](./03-exercises/00-overview.md) | Index of the three exercises + the working checklist |
| [exercises/exercise-01-stack-and-tabs.md](./03-exercises/exercise-01-stack-and-tabs.md) | Guided: wire a typed stack + bottom-tab navigator from scratch |
| [exercises/exercise-02-habit-form.tsx](./03-exercises/exercise-02-habit-form.tsx) | Runnable: a controlled, validated multi-field add-habit form |
| [exercises/exercise-03-session-store.ts](./03-exercises/exercise-03-session-store.ts) | Runnable: a Zustand session store with SecureStore persistence |
| [challenges/README.md](./04-challenges/00-overview.md) | What the challenge is and how it's assessed |
| [challenges/challenge-01-auth-gated-flow.md](./04-challenges/challenge-01-auth-gated-flow.md) | Build a flicker-free, leak-free auth-gated navigation flow |
| [mini-project/README.md](./07-mini-project/00-overview.md) | Grow Crunch Tracker into a navigable, stateful app |
| [quiz.md](./05-quiz.md) | 10 questions with an answer key |
| [homework.md](./06-homework.md) | Six practice problems with a grading rubric |

## The "no flicker, no leak" promise

C3's mobile weeks have a recurring quality bar that this week introduces:

```
App launch → splash → resolves to the RIGHT navigator, once, with no white flash,
and there is NO way to reach a protected screen without being authenticated.
```

If your app flashes the login screen for a frame before showing the tabs, you are not done. If a user can hardware-back or swipe-back into the habits list after logging out, you are not done. The auth gate is the single most-failed mobile interview question; we make getting it right ordinary.

## Where this sits in the Crunch Tracker arc

| Week | What the app could do |
|-----:|-----------------------|
| 7 | One screen: a habit list + add form, mock data |
| **8 (this week)** | **Login → tabs (Habits / Goals / Profile), typed routes, Zustand session store, token in SecureStore — still mock data** |
| 9 | The same UI, now wired to the live Spring API via TanStack Query: real login, real habits, real mutations |
| 10 | The whole stack deployed and installable |

This week is the last "mock data" week. We deliberately keep the network out so you can get navigation and state right in isolation. Next week the seams we leave — a `login(token, user)` action that today just stores a fake token, a habits screen that reads from a store — become the exact insertion points for the real API.

## Stretch goals

If you finish early and want to push further:

- Read the React Navigation **"Type checking with TypeScript"** guide end to end and convert any remaining loosely-typed screen: <https://reactnavigation.org/docs/typescript/>.
- Read the Zustand **"Slices pattern"** and split your store into a `sessionSlice` and a `uiSlice`: <https://zustand.docs.pmnd.rs/guides/slices-pattern>.
- Add a deep link that opens a specific habit (`crunchtracker://habits/42`) and verify it with `npx uri-scheme open crunchtracker://habits/42 --ios`.
- Write a one-paragraph note for your future self: which state in your app is "navigation state," which is "client state," and which will become "server state" in Week 9? Drawing that map now will make Week 9 click.

## Up next

Continue to **Week 9 — Full-Stack Integration** once you have pushed the mini-project to your GitHub. That's the week the two halves of the course finally meet.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
