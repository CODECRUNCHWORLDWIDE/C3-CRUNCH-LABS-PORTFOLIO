# Week 7 — React Native Basics

Welcome to the front half of **C3 · Crunch Labs Portfolio**. For six weeks we built a backend: a Spring Boot 3 REST API over PostgreSQL, secured with Spring Security 6 and JWTs, that knows about users, goals, habits, and check-ins. It is a real, multi-user product — but it has no face. This week the frontend begins, and it begins on a phone.

We pivot to **TypeScript and React Native via Expo**. By Friday you should be able to scaffold an Expo + TypeScript app from a blank folder, write typed function components, manage local state with hooks, render a performant scrolling list, lay it out with Flexbox so it survives a small phone and a tablet, and run the whole thing on both the iOS Simulator (or Android Emulator) and a real device over the network. No native Xcode/Android Studio project files to hand-edit, no `any` types, no class components.

The first thing to internalize is that **React Native is not "React in a browser"**. There is no DOM. There are no `<div>`s, no CSS files, no `px` units that mean CSS pixels. You write `<View>` and `<Text>` and `<Pressable>`, and Expo/React Native turns them into real native `UIView`/`android.view.View` widgets. You write styles as JavaScript objects, and layout is Flexbox — but a Flexbox whose default `flexDirection` is `column`, not `row`. The mental model is "React's component and hooks model, rendering to native primitives instead of HTML." Get that distinction now and the rest of the week is downhill.

We move fast. This is software engineering for people who already shipped a backend.

## Learning objectives

By the end of this week, you will be able to:

- **Distinguish** React (the library/render model), React Native (the renderer that targets native views), and Expo (the toolchain, SDK, and runtime on top) — three things people regularly conflate.
- **Scaffold** an Expo + TypeScript app with `npx create-expo-app`, run it on a simulator and a physical device with Expo Go, and read the Metro bundler output.
- **Write** typed function components with explicit `Props` interfaces — no implicit `any`, `strict` mode on.
- **Read** JSX correctly: expressions in braces, conditional rendering, list rendering with stable `key`s, and why `<Text>` is mandatory for strings.
- **Explain** the render model: what triggers a re-render, what `props` and `state` are, and why mutating state in place does nothing.
- **Manage** local component state with `useState`, and run side effects with `useEffect` — including the dependency array and the cleanup function.
- **Render** long lists performantly with `FlatList` (not `.map()` inside a `ScrollView`), with `keyExtractor`, `ListEmptyComponent`, and a typed `renderItem`.
- **Style** for mobile with `StyleSheet.create`, Flexbox layout, `SafeAreaView`, and touch targets that meet the 44pt minimum.
- **Handle** the three states every screen has — loading, empty, and content — deliberately, instead of rendering `undefined`.

## Prerequisites

This week assumes you have completed **C3 weeks 1–6**, or have equivalent experience. Specifically:

- You shipped the Crunch Tracker Spring Boot API in week 6: registration, JWT login, per-user habits and goals. You don't touch it this week, but you know its shape.
- You're comfortable in a terminal — `cd`, `npm`/`npx`, installing a package, reading a stack trace.
- You can read and write basic Git (`clone`, `add`, `commit`, `push`, `branch`).
- You have written and tested a small project end-to-end at least once.

You do **not** need prior React, React Native, or TypeScript experience. We start at the component model. If you have learned **React for the web** before, you'll need to unlearn a couple of habits (there is no `<div>`, `className` doesn't exist, navigation is not URLs yet); we flag them as we go.

**Hardware:** any laptop (macOS, Linux, or Windows) plus a phone. macOS gets the iOS Simulator for free; everyone can use the Android Emulator; and everyone can run on a real device with the **Expo Go** app over Wi-Fi. You do **not** need a Mac to do this week.

## Topics covered

- The three things people call "React Native": the **render model** (React), the **native renderer** (React Native), and the **toolchain/SDK** (Expo, currently SDK 53 on React Native 0.79 / React 19 in 2026).
- The Expo CLI: `npx create-expo-app`, `npx expo start`, the Metro bundler, Expo Go vs development builds, and `npx expo start --tunnel`.
- Project layout: `app.json`/`app.config.ts`, `package.json`, `tsconfig.json`, `App.tsx`, `assets/`, and (later) the `app/` directory for Expo Router.
- TypeScript essentials for RN: `interface` vs `type`, typing props, `React.FC` vs explicit return types, union types for state, `as const`, and why `strict: true` is non-negotiable.
- Core components: `View`, `Text`, `Image`, `TextInput`, `Pressable`, `ScrollView`, `FlatList`, `SafeAreaView`, `StatusBar`.
- JSX: expressions in `{}`, conditional rendering (`&&`, ternary), list rendering, fragments, and the `key` rule.
- The render model: props down, state up, re-render on state change, referential identity, and immutable updates.
- Hooks: `useState` (including functional updates and lazy init), `useEffect` (dependency array, cleanup, the empty-array case), and the rules of hooks.
- `FlatList` deeply: `data`, `renderItem`, `keyExtractor`, `ListEmptyComponent`, `ItemSeparatorComponent`, and why it beats `.map()` for long lists.
- Styling: `StyleSheet.create`, the style object model, Flexbox (with `column` as the default axis), `flex`, `justifyContent`, `alignItems`, spacing, and platform-adaptive layout with `Dimensions`/`useWindowDimensions`.
- Touch and accessibility basics: `Pressable` states, 44pt hit targets, `accessibilityLabel`, `accessibilityRole`.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract.

| Day       | Focus                                              | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|----------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | RN/Expo/TS stack, scaffold, run on device          |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | Components, props, JSX, the render model           |    2h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0h      |     6.5h    |
| Wednesday | useState, useEffect, immutable updates             |    1h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0.5h    |     6h      |
| Thursday  | FlatList, styling, Flexbox, touch targets          |    1h    |    1h     |     0h     |    0.5h   |   1h     |     2h       |    0.5h    |     6h      |
| Friday    | Loading/empty states; mini-project work            |    0h    |    1h     |     0h     |    0.5h   |   1h     |     3h       |    0.5h    |     6h      |
| Saturday  | Mini-project deep work                             |    0h    |    0h     |     0h     |    0h     |   1h     |     3h       |    0h      |     4h      |
| Sunday    | Quiz, review, polish                               |    0h    |    0h     |     0h     |    1h     |   0h     |     0.5h     |    0h      |     1.5h    |
| **Total** |                                                    | **6h**   | **7.5h**  | **2h**     | **3.5h**  | **6h**   | **8.5h**     | **2h**     | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./00-overview.md) | This overview (you are here) |
| [resources.md](./01-resources.md) | Curated Expo, React Native, React, and TypeScript docs and tools |
| [lecture-notes/01-react-native-expo-and-typescript.md](./02-lecture-notes/01-react-native-expo-and-typescript.md) | What RN, Expo, and TS each are; the toolchain; components, props, JSX, the render model |
| [lecture-notes/02-state-effects-lists-and-styling.md](./02-lecture-notes/02-state-effects-lists-and-styling.md) | `useState`, `useEffect`, `FlatList`, `StyleSheet`, Flexbox, touch targets |
| [exercises/README.md](./03-exercises/00-overview.md) | Index of short coding exercises |
| [exercises/exercise-01-scaffold-and-run.md](./03-exercises/exercise-01-scaffold-and-run.md) | `create-expo-app`, run on simulator + device, first typed component |
| [exercises/exercise-02-habit-card.tsx](./03-exercises/exercise-02-habit-card.tsx) | Fill-in-the-TODO typed component drill — a reusable `HabitCard` |
| [exercises/exercise-03-habit-list-screen.tsx](./03-exercises/exercise-03-habit-list-screen.tsx) | A `FlatList` screen with state, an effect, and loading/empty states |
| [challenges/README.md](./04-challenges/00-overview.md) | Index of weekly challenges |
| [challenges/challenge-01-spec-faithful-screen.md](./04-challenges/challenge-01-spec-faithful-screen.md) | Rebuild a design-spec screen pixel-faithfully, fully typed |
| [quiz.md](./05-quiz.md) | 10 multiple-choice questions with an answer key |
| [homework.md](./06-homework.md) | Six practice problems for the week |
| [mini-project/README.md](./07-mini-project/00-overview.md) | Full spec for the Crunch Tracker mobile client (habit list + add form, mock data) |

## The "no `any`, no warnings" promise

C3's frontend half uses a recurring marker on every exercise that ends in working code:

```
✔ TypeScript: 0 errors   ·   ✔ Metro bundled   ·   ✔ 0 LogBox warnings
```

If `npx tsc --noEmit` doesn't print zero errors, or the app shows a yellow/red LogBox warning, you are not done. We treat `any` as a bug and a runtime warning as a bug. `tsconfig.json` ships with `strict: true` and we do not turn it off. The point of week 7 is to make that clean line ordinary.

## Stretch goals

If you finish the regular work early and want to push further:

- Read the official **"React Native: Core Components and APIs"** page end to end: <https://reactnative.dev/docs/components-and-apis>.
- Skim the **React docs on "Render and Commit"** and **"State as a Snapshot"** — the two pages that most cleanly explain the render model: <https://react.dev/learn/render-and-commit>.
- Run your app on a real device over `npx expo start --tunnel` and feel the difference between LAN and tunnel mode.
- Read the **`FlatList` performance guide** and try a 10,000-item list to feel `windowSize` and `getItemLayout` matter: <https://reactnative.dev/docs/optimizing-flatlist-configuration>.
- Write a short note for your future self comparing how the web DOM and React Native's native views differ — `<div>`/`<span>` vs `<View>`/`<Text>`, CSS vs `StyleSheet`, `px` vs density-independent units.

## Up next

Continue to **Week 8 — Navigation and App State** once you have pushed the mini-project to your GitHub. Week 8 adds React Navigation, typed routes, tabs, and a deliberate state strategy (Context/Zustand), turning this single-screen client into a navigable app — still on mock data, so the real backend wiring in Week 9 is the only new variable.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
