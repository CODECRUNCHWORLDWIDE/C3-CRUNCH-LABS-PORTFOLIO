# Week 8 — Resources

Every resource on this page is **free**. React Navigation, Zustand, and Expo are all open-source with free public docs. No paywalled courses are linked. When a library has shifted its API across versions, we say which version we mean — this matters more in the JS ecosystem than almost anywhere else.

## Required reading (work it into your week)

- **React Navigation — Getting started** — the canonical setup, current to v7:
  <https://reactnavigation.org/docs/getting-started>
- **React Navigation — Type checking with TypeScript** — read this twice; it's the spine of the week:
  <https://reactnavigation.org/docs/typescript/>
- **Zustand — Getting started** — the whole "intro" page is ~10 minutes and you'll use all of it:
  <https://zustand.docs.pmnd.rs/getting-started/introduction>
- **Expo SecureStore** — the API you persist the token with:
  <https://docs.expo.dev/versions/latest/sdk/securestore/>
- **React — "You Might Not Need an Effect"** — the mental reset that makes state-vs-server-state click:
  <https://react.dev/learn/you-might-not-need-an-effect>

## React Navigation (the navigator half of the week)

- **Native Stack Navigator** — the stack we use; it's backed by the platform's native navigation:
  <https://reactnavigation.org/docs/native-stack-navigator>
- **Bottom Tabs Navigator**:
  <https://reactnavigation.org/docs/bottom-tab-navigator>
- **Nesting navigators** — the rules for a stack-inside-a-tab and a tab-inside-an-auth-stack:
  <https://reactnavigation.org/docs/nesting-navigators>
- **Authentication flows** — the official version of this week's challenge:
  <https://reactnavigation.org/docs/auth-flow>
- **Deep linking** — URL-to-route configuration and testing:
  <https://reactnavigation.org/docs/deep-linking>
- **Expo Router** (the file-based alternative; we use React Navigation directly, but you should know this exists):
  <https://docs.expo.dev/router/introduction/>

## State management (the state half of the week)

- **Zustand — TypeScript guide** — the `create<T>()(...)` curried-call gotcha is documented here:
  <https://zustand.docs.pmnd.rs/guides/typescript>
- **Zustand — `persist` middleware** — how we sync the store to SecureStore:
  <https://zustand.docs.pmnd.rs/integrations/persisting-store-data>
- **Zustand — Slices pattern** — splitting one store into `session` and `ui` slices:
  <https://zustand.docs.pmnd.rs/guides/slices-pattern>
- **React — Passing data deeply with Context** — when Context is the right tool (and when it isn't):
  <https://react.dev/learn/passing-data-deeply-with-context>
- **React — Scaling Up with Reducer and Context** — the `useReducer` + Context pattern, for comparison:
  <https://react.dev/learn/scaling-up-with-reducer-and-context>
- **"Why React Query / TanStack Query"** — a preview of *why* server state is different (we use this in Week 9):
  <https://tanstack.com/query/latest/docs/framework/react/overview>

## Forms

- **React Native — Handling Text Input** — controlled `TextInput` basics:
  <https://reactnative.dev/docs/handling-text-input>
- **React Native — KeyboardAvoidingView** — the component that keeps the keyboard off your inputs:
  <https://reactnative.dev/docs/keyboardavoidingview>
- **react-hook-form** — the form library most teams reach for once forms get big:
  <https://react-hook-form.com/get-started>
- **Zod** — schema validation you can share between form and (next week) API client:
  <https://zod.dev/>

## Expo and the toolchain

- **Expo — Develop overview** — `npx expo start`, dev builds, Expo Go:
  <https://docs.expo.dev/develop/tools/>
- **Expo — `uri-scheme` CLI** — opening deep links from the terminal:
  <https://docs.expo.dev/guides/linking/#testing-urls>
- **Expo SDK 52 release notes** — the SDK these examples target (React Native 0.76, the New Architecture on by default):
  <https://expo.dev/changelog/2024/11-12-sdk-52>

## TypeScript (you'll lean on these patterns all week)

- **TypeScript Handbook — Object Types & generics** (route param lists are just generic objects):
  <https://www.typescriptlang.org/docs/handbook/2/objects.html>
- **TypeScript — Declaration Merging** (the `RootParamList` global-override trick relies on this):
  <https://www.typescriptlang.org/docs/handbook/declaration-merging.html>

## Tools you'll use this week

- **Node 20+** — `node --version` to confirm. Expo SDK 52 needs Node 18.18+; we standardize on 20 LTS.
- **`npx expo start`** — the dev server. Press `i` for iOS sim, `a` for Android emulator, or scan the QR with Expo Go.
- **React Native DevTools** (ships with RN 0.76) — open with `j` in the Expo CLI; inspect components and network.
- **`npx uri-scheme`** — open deep links into your app from the terminal.

## Videos (free, no signup)

- **"React Navigation v7"** — the official channel walks the v7 API; search "React Navigation" on the Software Mansion / Expo channels:
  <https://www.youtube.com/@expo>
- **Jack Herrington — Zustand** — a clear, fast tour of the store and selectors:
  <https://www.youtube.com/@jherr>
- *(If a link rots, search the title on YouTube; the official channels repost.)*

## Open-source projects to read this week

You learn more from one hour reading a well-typed Expo app than from three hours of tutorials. Pick one and scroll:

- **`expo/expo`** — the `apps/` and `packages/` trees are real, current React Native + TypeScript:
  <https://github.com/expo/expo>
- **`pmndrs/zustand`** — the store is ~1 KB; read `src/vanilla.ts` and `src/react.ts` once:
  <https://github.com/pmndrs/zustand>
- **`react-navigation/react-navigation`** — the navigators and the TypeScript types:
  <https://github.com/react-navigation/react-navigation>

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Navigator** | A component that owns a set of screens and the transitions between them (stack, tabs, drawer). |
| **Stack navigator** | Screens pushed on top of each other; a back button pops. `createNativeStackNavigator`. |
| **Tab navigator** | Sibling screens switched by a bottom bar; no back stack between tabs. `createBottomTabNavigator`. |
| **Navigation state** | The tree React Navigation keeps: which navigators exist, which screen is focused, the params. |
| **ParamList** | A TypeScript type mapping each route name to the params it accepts. The spine of typed navigation. |
| **Deep link** | A URL (`crunchtracker://habits/42`) that opens a specific screen with specific params. |
| **Client state** | State the app owns: which tab is open, a form draft, theme. You are the source of truth. |
| **Server state** | State the API owns: the user's habits. It's cached on the client but the server is the source of truth. |
| **Zustand** | A tiny (~1 KB) state store; you `create` a store with state + actions and subscribe with selectors. |
| **Selector** | `useStore(s => s.token)` — subscribe to a *slice* so the component only re-renders when that slice changes. |
| **SecureStore** | Expo's encrypted key-value store: iOS Keychain, Android Keystore-backed. Where the JWT lives. |
| **Rehydrate** | On launch, read persisted state (the token) back into the store before deciding which navigator to show. |
| **Auth gate** | The conditional that shows login vs the app based on whether a valid session exists. |

---

*If a link 404s, please open an issue so we can replace it. The JS ecosystem moves fast — version drift is the most likely cause.*
