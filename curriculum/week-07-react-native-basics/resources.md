# Week 7 — Resources

Every resource on this page is **free**. The Expo docs, the React Native docs, the React docs, and the TypeScript handbook are all open and need no account. Open-source repos are public on GitHub. No paywalled books or courses are linked.

A note on versions: this week targets **Expo SDK 53** on **React Native 0.79** with **React 19** and **TypeScript 5.x**, which is the 2026 baseline. Expo ships a new SDK roughly every quarter; APIs shift. When a doc page disagrees with what `npx expo start` prints, trust the version selector at the top of the doc and your installed SDK.

## Required reading (work it into your week)

- **Expo — "Get started: Create a project"** — the canonical scaffold-and-run walkthrough:
  <https://docs.expo.dev/get-started/create-a-project/>
- **React Native — "Core Components and APIs"** — the catalogue of `View`, `Text`, `FlatList`, etc.:
  <https://reactnative.dev/docs/components-and-apis>
- **React — "Describing the UI"** (components, JSX, props, conditional and list rendering):
  <https://react.dev/learn/describing-the-ui>
- **React — "Adding Interactivity"** (state, events, the render-and-commit model):
  <https://react.dev/learn/adding-interactivity>
- **TypeScript — "TypeScript for JavaScript Programmers"** (the 10-minute orientation):
  <https://www.typescriptlang.org/docs/handbook/typescript-in-5-minutes.html>

## The React render model (read these twice)

The single biggest source of bugs for new React Native developers is misunderstanding *when* and *why* a component re-renders. These four short pages are the cure:

- **"Render and Commit"**: <https://react.dev/learn/render-and-commit>
- **"State as a Snapshot"**: <https://react.dev/learn/state-as-a-snapshot>
- **"Updating Objects in State"** (immutability): <https://react.dev/learn/updating-objects-in-state>
- **"Updating Arrays in State"**: <https://react.dev/learn/updating-arrays-in-state>

## Official Expo docs

- **Expo SDK reference** (every Expo module): <https://docs.expo.dev/versions/latest/>
- **"Expo Go" vs "development builds"** — which to use when: <https://docs.expo.dev/develop/development-builds/introduction/>
- **Running on a device**: <https://docs.expo.dev/get-started/start-developing/>
- **TypeScript in Expo** (how the template wires `tsconfig`): <https://docs.expo.dev/guides/typescript/>
- **Metro bundler config**: <https://docs.expo.dev/guides/customizing-metro/>

## Official React Native docs

- **`FlatList`**: <https://reactnative.dev/docs/flatlist>
- **`Optimizing FlatList configuration`**: <https://reactnative.dev/docs/optimizing-flatlist-configuration>
- **`StyleSheet`**: <https://reactnative.dev/docs/stylesheet>
- **Layout with Flexbox**: <https://reactnative.dev/docs/flexbox>
- **`Pressable`**: <https://reactnative.dev/docs/pressable>
- **`TextInput`**: <https://reactnative.dev/docs/textinput>
- **Height and Width / density-independent units**: <https://reactnative.dev/docs/height-and-width>

## TypeScript

- **The TypeScript Handbook** (the canonical reference): <https://www.typescriptlang.org/docs/handbook/intro.html>
- **"Everyday Types"** (the types you'll use 90% of the time): <https://www.typescriptlang.org/docs/handbook/2/everyday-types.html>
- **React + TypeScript Cheatsheet** (community-maintained, excellent): <https://react-typescript-cheatsheet.netlify.app/>
- **`tsconfig` reference** (what `strict` actually turns on): <https://www.typescriptlang.org/tsconfig/>

## Editors

- **VS Code** (primary) — first-class TypeScript and JSX support out of the box: <https://code.visualstudio.com/>
- **"ES7+ React/Redux/React-Native snippets"** extension (optional, speeds up boilerplate): search the VS Code marketplace.
- **WebStorm** (secondary; free non-commercial licence as of late 2024): <https://www.jetbrains.com/webstorm/>

The C3 curriculum does **not** depend on any IDE feature. Everything compiles, type-checks, and runs from the terminal with `npx expo start` and `npx tsc --noEmit`.

## Tools you'll use this week

- **Node.js LTS** (20.x or 22.x) — installed however you like (`nvm` recommended). Verify with `node --version`.
- **`npx` / `npm`** — ship with Node. `npx` runs a package without a global install.
- **Expo Go** — the app you install on your phone from the App Store / Play Store to run your project on a device with no native build.
- **iOS Simulator** (macOS only, via Xcode Command Line Tools) and/or **Android Emulator** (via Android Studio). Both optional — Expo Go on a real phone covers everything.
- **`npx tsc --noEmit`** — type-checks without producing output. Your "is it green?" command.

## Videos (free, no signup)

- **"Expo Go in 100 Seconds"** and the Expo team's official channel: <https://www.youtube.com/@expo>
- **React Native EU / App.js Conf** — community conferences; every talk is on YouTube. Search "App.js Conf 2025".
- **"React in 100 Seconds"** (Fireship) for the render model at a glance — short, accurate.

## Open-source projects to read this week

You learn more from one hour reading a well-built Expo app than from three hours of tutorials. Pick one and scroll:

- **`expo/examples`** — official, small, single-feature Expo apps in TypeScript:
  <https://github.com/expo/examples>
- **`obytes/react-native-template-obytes`** — an opinionated production RN/Expo starter; read its component and styling conventions:
  <https://github.com/obytes/react-native-template-obytes>
- **`callstack/react-native-paper`** — a Material component library; great source for typed RN components:
  <https://github.com/callstack/react-native-paper>
- **`react-native-community/...`** — the community org hosts many of the libraries you'll meet in weeks 8–9.

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **React** | The library and render model — components, props, state, hooks. Renderer-agnostic. |
| **React Native** | A renderer for React that targets native iOS/Android views instead of the DOM. |
| **Expo** | The toolchain, SDK, and runtime on top of React Native. `create-expo-app`, `expo start`, Expo Go. |
| **Expo Go** | A prebuilt app on your phone that runs your JS without a native build. Great for week 7. |
| **Metro** | React Native's JavaScript bundler. The thing `expo start` launches; serves your JS to the device. |
| **JSX** | The XML-like syntax (`<View>`) that compiles to `React.createElement(...)` calls. |
| **Component** | A function returning JSX. The unit of UI. We write function components only. |
| **Props** | Read-only inputs passed into a component from its parent. |
| **State** | A component's own changeable data, declared with `useState`. Changing it triggers a re-render. |
| **Hook** | A function starting with `use` that taps into React features (`useState`, `useEffect`). |
| **`FlatList`** | The performant, virtualized list component. Use it instead of `.map()` for long lists. |
| **Flexbox** | The layout system. In RN the default `flexDirection` is `column`, not `row`. |
| **`StyleSheet`** | RN's API for defining styles as JS objects. Styles are objects, not CSS files. |
| **dp / density-independent unit** | RN's unitless number for size. Not a CSS pixel; scaled by device density. |
| **LogBox** | The in-app overlay that surfaces warnings (yellow) and errors (red) at runtime. |
| **`strict`** | The `tsconfig` flag that turns on the full TypeScript safety net. We never turn it off. |

---

*If a link 404s, please open an issue so we can replace it.*
