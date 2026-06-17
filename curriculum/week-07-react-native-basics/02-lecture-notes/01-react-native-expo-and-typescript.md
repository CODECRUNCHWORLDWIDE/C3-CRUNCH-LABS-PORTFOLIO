# Lecture 1 — React Native with Expo and TypeScript

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can describe what React, React Native, and Expo each are without confusing them, scaffold a typed Expo app from a blank folder, run it on a simulator and a real device, write typed function components, read JSX correctly, and explain what triggers a re-render.

If you only remember one thing from this lecture, remember this:

> **React is the render model. React Native is the renderer. Expo is the toolchain.** They ship aligned and are easily confused, but they are three different things. And React Native renders to **native views, not HTML** — there is no DOM, no `<div>`, no CSS file. Internalize that now.

---

## 1. The three things people call "React Native"

Walk into a mobile shop and ask "are you using React Native?" You'll get answers that all mean slightly different things. There are three layers, and people name the whole stack after whichever layer they care about.

| Layer | Example | What it is | Released cadence |
|-------|---------|-----------|------------------|
| Render model | **React 19** | Components, props, state, hooks, the reconciler. Renderer-agnostic. | ~yearly |
| Native renderer | **React Native 0.79** | The library that turns React elements into real `UIView` / `android.view.View` widgets. | ~every 2–3 months |
| Toolchain / SDK | **Expo SDK 53** | `create-expo-app`, the Metro dev server, the Expo Go runtime, and a large set of native modules (camera, filesystem, secure store). | ~quarterly |

**The same React** that powers a website powers your phone app. The component you write — a function that returns JSX — is identical in shape. What changes is the *renderer*: on the web, React reconciles to the DOM (`react-dom`); on a phone, it reconciles to native views (`react-native`). The famous line is "**learn once, write anywhere**" — not "write once, run anywhere." You reuse the *model*, not the *markup*.

**Expo** sits on top. You *can* build a bare React Native app with no Expo (the `react-native community CLI` path), but in 2026 the React Native team itself recommends Expo as the default starting point. Expo gives you: a zero-config TypeScript setup, the Metro bundler wired up, over-the-air JS updates, a huge library of vetted native modules, and **Expo Go** — an app on your phone that runs your JavaScript without you ever opening Xcode or Android Studio. For week 7, Expo Go is all we need.

> **Why "no native build" matters this week.** A bare React Native project requires you to compile a native iOS app (needs a Mac + Xcode) or a native Android app (needs Android Studio) every time you change native code. Expo Go sidesteps that: it's a *prebuilt* native shell that loads your JS over the network. You change a component, save, and the phone updates in under a second. You only need a real native build (a "development build") when you add a native module Expo Go doesn't bundle — and we don't this week.

---

## 2. React Native is not the web

This is the single most expensive misconception for someone coming from web React. There is **no DOM**. Burn the following table into your memory:

| Web (react-dom) | React Native | Notes |
|-----------------|--------------|-------|
| `<div>` | `<View>` | The generic container. Lays out children with Flexbox. |
| `<span>` / `<p>` | `<Text>` | **All** text must be inside a `<Text>`. A bare string in a `<View>` crashes. |
| `<img>` | `<Image>` | Takes a `source`, not `src`. Needs explicit width/height or `flex`. |
| `<input>` | `<TextInput>` | Controlled via `value` + `onChangeText`. |
| `<button>` / `onClick` | `<Pressable>` / `onPress` | There is no `onClick`. Touch, not click. |
| scrollable page | `<ScrollView>` / `<FlatList>` | The page does not scroll by default; you opt in. |
| `class="card"` + CSS file | `style={styles.card}` | Styles are JS objects. No CSS files, no `className`. |
| `12px` | `12` | A unitless number — a density-independent unit, not a CSS pixel. |

So a "hello world" on the web is `<div>Hello</div>`. In React Native it is:

```tsx
import { View, Text } from "react-native";

export default function App() {
  return (
    <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
      <Text>Hello, Crunch Tracker</Text>
    </View>
  );
}
```

If you wrote `<View>Hello</View>` you'd get a red screen: *"Text strings must be rendered within a `<Text>` component."* That error will catch you at least once this week. It's not a bug — it's RN telling you that, unlike a browser, there is no implicit text node.

---

## 3. Installing the toolchain

You need **Node.js LTS** (20.x or 22.x in 2026) and a phone. That's the floor.

```bash
node --version   # v20.x or v22.x
npm --version    # ships with node
```

Optional but nice:

- **iOS Simulator** (macOS only): install Xcode from the App Store, then `xcode-select --install`.
- **Android Emulator**: install Android Studio, create a virtual device in its Device Manager.
- **Expo Go on your phone**: install "Expo Go" from the App Store (iOS) or Play Store (Android). This is the path that works for everyone regardless of OS.

You do **not** install a global `expo` or `react-native` CLI. The modern pattern is `npx`, which runs the pinned version from your project. If a 2021-era tutorial tells you to `npm install -g expo-cli`, ignore it — that global CLI is deprecated.

---

## 4. Scaffold a real app from scratch

Let's make one now. This is the canonical layout you'll use for the mini-project.

```bash
npx create-expo-app@latest CrunchTracker --template blank-typescript
cd CrunchTracker
```

`--template blank-typescript` gives you a minimal app with TypeScript already wired and a `tsconfig.json` extending Expo's strict base. (The default template includes Expo Router and example screens; `blank-typescript` is deliberately bare so you understand every file.)

You now have, roughly:

```
CrunchTracker/
├── App.tsx                 # the root component — your entry point
├── app.json               # Expo project config (name, icon, splash, slug)
├── package.json           # dependencies and scripts
├── tsconfig.json          # extends expo/tsconfig.base; strict is on
├── babel.config.js        # Babel preset for Expo
├── assets/                # icon.png, splash, adaptive-icon
└── .gitignore             # excludes node_modules, .expo, etc.
```

Notice there is no `ios/` or `android/` folder. That's the point of a managed Expo app: the native projects are generated on demand. You live in `App.tsx` and the components you add.

Start the dev server:

```bash
npx expo start
```

Metro (the bundler) boots and prints a QR code and a menu:

```
› Metro waiting on exp://192.168.1.42:8081
› Scan the QR code above with Expo Go (Android) or the Camera app (iOS)

› Press a │ open Android
› Press i │ open iOS simulator
› Press w │ open web
› Press r │ reload app
› Press m │ toggle menu
```

To run it:

- **Real device (everyone):** open Expo Go, scan the QR code. Your phone and laptop must be on the same Wi-Fi. If your network blocks device-to-device traffic (common on campus/corporate Wi-Fi), run `npx expo start --tunnel` — it routes through Expo's servers, slower but reliable.
- **iOS Simulator (macOS):** press `i`.
- **Android Emulator:** press `a`.

You should see "Hello, Crunch Tracker" centered on the screen. Edit the `<Text>`, save, and watch **Fast Refresh** update the device in well under a second without losing state. That loop — edit, save, see it on the phone — is the entire reason this stack is pleasant to work in.

> **The "no `any`, no warnings" promise.** Run `npx tsc --noEmit` in a second terminal. It should print nothing (zero errors). If a yellow or red box appears on the device (LogBox), tap it, read it, fix the cause. We treat both a `tsc` error and a LogBox warning as bugs you are not allowed to ship.

---

## 5. Reading `App.tsx` and the config

Open `App.tsx`. The `blank-typescript` template gives you something like:

```tsx
import { StatusBar } from "expo-status-bar";
import { StyleSheet, Text, View } from "react-native";

export default function App() {
  return (
    <View style={styles.container}>
      <Text>Open up App.tsx to start working on your app!</Text>
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
    alignItems: "center",
    justifyContent: "center",
  },
});
```

Every piece matters:

- **`export default function App()`** — the root component Expo renders. A component is just a function that returns JSX.
- **`import { View, Text } from "react-native"`** — core components come from the `react-native` package, not from a `<...>` global. You import what you use.
- **`StyleSheet.create({ ... })`** — defines styles as JS objects. We dig into this in Lecture 2. For now: `flex: 1` means "fill the available space."
- **`<StatusBar style="auto" />`** — an Expo component that controls the OS status bar (the clock/battery row). It renders nothing visible itself.

Open `app.json`. The fields you'll actually touch:

```json
{
  "expo": {
    "name": "CrunchTracker",
    "slug": "crunch-tracker",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "userInterfaceStyle": "automatic",
    "splash": { "image": "./assets/splash.png", "resizeMode": "contain" }
  }
}
```

You won't change much here in week 7. Just know it exists: this is where the app's name, icon, splash screen, and (in week 10) build config live.

---

## 6. TypeScript: the minimum you need, the right way

We write **strict TypeScript** from line one. Not because it's fashionable — because a typed component is a component the compiler can defend. The `tsconfig.json` from the template extends `expo/tsconfig.base`, which sets `strict: true`. **We never turn that off.** If something is hard to type, that's a signal the design is off, not a signal to write `any`.

The TypeScript you need this week is small:

### Typing the shape of data

```ts
// A habit, as our mock data and (later) the API will return it.
export interface Habit {
  id: string;
  title: string;
  cadence: "daily" | "weekly";   // a union type — only these two strings allowed
  streak: number;                // consecutive completions
  doneToday: boolean;
}
```

`interface` describes the shape of an object. The `cadence` field is a **union type**: its only legal values are the literal strings `"daily"` and `"weekly"`. Try to assign `"monthly"` and the compiler stops you — that's the safety we're buying.

`interface` vs `type`: for object shapes, either works. Convention in this course: `interface` for object/props shapes, `type` for unions and aliases (`type Cadence = "daily" | "weekly"`). Don't agonize over it; be consistent.

### Typing component props

Every component that takes input declares a props interface:

```tsx
import { Text, View } from "react-native";

interface GreetingProps {
  name: string;
  excited?: boolean;   // the ? makes it optional
}

export function Greeting({ name, excited = false }: GreetingProps) {
  return (
    <View>
      <Text>Hello, {name}{excited ? "!" : "."}</Text>
    </View>
  );
}
```

Read it:

- **`interface GreetingProps`** — the input contract. `name` is required; `excited` is optional (the `?`).
- **`({ name, excited = false }: GreetingProps)`** — we destructure the props in the parameter list and give `excited` a default. The `: GreetingProps` annotation is what makes this typed: pass `<Greeting />` with no `name` and the compiler errors before the app ever runs.

> **Skip `React.FC`.** Older tutorials type components as `const Greeting: React.FC<GreetingProps> = (...)`. The React + TS community moved away from `React.FC` (it has historical baggage around implicit `children`). Write a plain function with a typed props parameter, as above. That's the 2026 idiom.

### Typing state

We'll cover hooks in depth in Lecture 2, but note now that `useState` infers its type:

```tsx
const [count, setCount] = useState(0);          // count: number, inferred
const [habit, setHabit] = useState<Habit | null>(null);  // explicit when it can be null
```

When the initial value tells the whole story (`0` → `number`), let inference do the work. When the type is wider than the initial value (it starts `null` but will hold a `Habit`), annotate it with `useState<Habit | null>`.

---

## 7. JSX, properly

JSX looks like HTML and is not HTML. It's syntax sugar that compiles to function calls. `<View><Text>Hi</Text></View>` becomes roughly `React.createElement(View, null, React.createElement(Text, null, "Hi"))`. You almost never see that compiled form; you just need to know the rules.

### Expressions live in braces

Anything in `{}` is a JavaScript expression, evaluated and inserted:

```tsx
const name = "Ada";
<Text>Hello, {name}</Text>             // Hello, Ada
<Text>{name.toUpperCase()}</Text>      // ADA
<Text>Total: {2 + 2}</Text>            // Total: 4
```

`{}` takes an **expression**, not a statement. You cannot put an `if` or a `for` loop directly inside JSX — there's no place for them. You use expressions instead.

### Conditional rendering

Two idioms, both expressions:

```tsx
// Ternary — when you render one thing OR another:
<Text>{doneToday ? "✓ Done" : "Not yet"}</Text>

// Logical && — when you render something OR nothing:
{streak > 0 && <Text>🔥 {streak}-day streak</Text>}
```

> **The `&&` gotcha.** `{count && <Badge />}` when `count` is `0` renders the literal `0` on screen (and in RN, a bare `0` outside a `<Text>` crashes). The fix: make the left side a real boolean — `{count > 0 && <Badge />}`. Never put a number on the left of `&&` in JSX.

### Rendering lists (and the `key` rule)

To render a collection, you `map` it to elements. Every element in a mapped list needs a **stable, unique `key`**:

```tsx
{habits.map((habit) => (
  <Text key={habit.id}>{habit.title}</Text>
))}
```

`key` lets React match elements between renders so it can update the right ones instead of rebuilding the whole list. **Use a stable id (`habit.id`), never the array index** if the list can reorder, insert, or delete — index keys cause subtle, maddening bugs where state attaches to the wrong row.

(For *long* lists you don't use `.map()` at all — you use `FlatList`, which virtualizes. That's Lecture 2. For a handful of items, `.map()` is fine.)

### Fragments and the single-root rule

A component returns **one** root element. To return siblings without a wrapping `<View>`, use a Fragment:

```tsx
import { Fragment } from "react";

return (
  <>
    <Text>Line one</Text>
    <Text>Line two</Text>
  </>
);
```

`<>...</>` is the Fragment shorthand. It groups children without adding a node to the layout tree.

---

## 8. The render model — the part that actually matters

Most "React is confusing" pain comes from not understanding *when components re-run*. Here is the whole model in four sentences:

1. A component is a function. React **calls** it to get JSX (this is a "render").
2. React calls it on first mount, and again **every time its state or props change**.
3. When you call a state setter (`setCount(1)`), you're asking React to re-run the component with the new value — you are **not** mutating a variable in place.
4. React compares the new JSX to the old, computes the minimal change, and **commits** it to the native views.

The classic beginner bug:

```tsx
// WRONG — mutating state in place does nothing.
const [habits, setHabits] = useState<Habit[]>([]);
function add(h: Habit) {
  habits.push(h);     // mutated the array, but React doesn't know — no re-render
}
```

```tsx
// RIGHT — produce a NEW array and hand it to the setter.
function add(h: Habit) {
  setHabits((prev) => [...prev, h]);   // new array → React re-renders
}
```

React decides "did anything change?" by comparing **references**, not deep contents. `habits.push(...)` keeps the same array reference, so React sees no change and skips the re-render. Spreading into a new array (`[...prev, h]`) creates a new reference, so React knows to re-render. **This immutable-update discipline is the heart of React and we'll hammer it in Lecture 2.**

> **State is a snapshot.** Inside one render, the value of a state variable is fixed — it's a snapshot from when that render started. Calling `setCount(count + 1)` twice in a row does **not** add 2; both reads see the same stale `count`. The fix is the functional updater `setCount((c) => c + 1)`, which always sees the latest. We return to this in Lecture 2; flag it now.

---

## 9. The component tree: props down, events up

Data flows **one way**: a parent passes data to children through props. Children don't reach up and change the parent; instead, the parent passes down a *callback*, and the child calls it.

```tsx
// Parent owns the state and the "what happens on press" logic.
function HabitListScreen() {
  const [habits, setHabits] = useState<Habit[]>(MOCK_HABITS);

  function toggle(id: string) {
    setHabits((prev) =>
      prev.map((h) => (h.id === id ? { ...h, doneToday: !h.doneToday } : h)),
    );
  }

  return (
    <View>
      {habits.map((h) => (
        <HabitRow key={h.id} habit={h} onToggle={toggle} />
      ))}
    </View>
  );
}

// Child receives data (habit) and a callback (onToggle). It owns neither.
interface HabitRowProps {
  habit: Habit;
  onToggle: (id: string) => void;
}

function HabitRow({ habit, onToggle }: HabitRowProps) {
  return (
    <Pressable onPress={() => onToggle(habit.id)}>
      <Text>{habit.doneToday ? "✓ " : "○ "}{habit.title}</Text>
    </Pressable>
  );
}
```

This pattern — **state lives in the parent, children are dumb and typed, events bubble up through callbacks** — is the spine of every React app. The `HabitRow` doesn't know what "toggle" means; it just reports "this row was pressed." The parent decides the consequence. That separation is what makes the row reusable.

---

## 10. Pressable, not Button

React Native ships a `<Button>` component, but it's deliberately limited — you can't style it much. The modern, flexible primitive is `<Pressable>`:

```tsx
<Pressable
  onPress={() => onToggle(habit.id)}
  style={({ pressed }) => [
    styles.row,
    pressed && styles.rowPressed,   // visual feedback while held
  ]}
  accessibilityRole="button"
  accessibilityLabel={`Toggle ${habit.title}`}
>
  <Text style={styles.title}>{habit.title}</Text>
</Pressable>
```

Note `style` can be a **function** of the press state — that's how you give touch feedback. And note `accessibilityRole`/`accessibilityLabel`: screen readers are not optional polish, and the spec-faithful challenge this week checks them. We'll size touch targets to the 44pt minimum in Lecture 2.

---

## 11. What `npx expo start` is actually doing

A quick mental model of the dev loop:

```
   your .tsx files
        │
        ▼
   ┌──────────────┐     bundles JS over HTTP      ┌─────────────┐
   │ Metro bundler │ ───────────────────────────▶ │  Expo Go     │
   │ (your laptop) │                               │  (your phone)│
   └──────────────┘ ◀─────────────────────────── └─────────────┘
        ▲                  Fast Refresh on save
        │
   you edit + save
```

1. Metro transpiles your TypeScript/JSX to JavaScript and bundles it.
2. Expo Go (or the simulator) downloads that bundle over your LAN and runs it in a JS engine (Hermes) embedded in the native shell.
3. On save, Metro recomputes just what changed and pushes a **Fast Refresh** update — your component re-renders, usually preserving state.

When something goes wrong, the error shows in two places: the **terminal** (Metro/bundler errors — syntax, missing imports) and the **device** (runtime errors and LogBox warnings). Learn to read both. A red screen on the device with a stack trace is your friend; tap it, read the top frame, fix it.

---

## 12. A glance at what's *not* in week 7

- **Navigation.** No multi-screen routing, no tabs, no React Navigation/Expo Router yet. One screen this week. That's Week 8.
- **Global state.** No Context, no Zustand yet. State lives in the screen component. Week 8.
- **The real backend.** No `fetch`, no JWT, no TanStack Query. We run entirely on **mock local data**. The backend you built weeks 1–6 sits untouched until Week 9.
- **Native modules.** No camera, no secure storage, no push. Pure JS + core components, runnable in Expo Go.

Week 7 is the language (TypeScript), the render model (React), and the primitives (React Native core components). By Friday you can build a typed, single-screen mobile UI without a tutorial.

---

## 13. Recap

You should now be able to:

- State what React 19, React Native 0.79, and Expo SDK 53 each are without conflating them.
- Explain why there's no DOM, and name the RN equivalent of `<div>`, `<span>`, `<img>`, `<input>`, and `<button>`.
- Scaffold a `blank-typescript` Expo app and run it on a simulator and a real device.
- Write a typed function component with a `Props` interface — no `React.FC`, no `any`.
- Read JSX: expressions in braces, ternary/`&&` conditionals, `.map()` with stable `key`s, fragments.
- Explain what triggers a re-render and why mutating state in place does nothing.
- Pass data down via props and events up via callbacks.

Next, the dynamic half: state, effects, lists, and styling. Continue to [Lecture 2 — State, Effects, Lists, and Styling](./02-state-effects-lists-and-styling.md).

---

## References

- *Expo — Create a project*: <https://docs.expo.dev/get-started/create-a-project/>
- *React Native — Core Components and APIs*: <https://reactnative.dev/docs/components-and-apis>
- *React — Describing the UI*: <https://react.dev/learn/describing-the-ui>
- *React — Render and Commit*: <https://react.dev/learn/render-and-commit>
- *TypeScript — Everyday Types*: <https://www.typescriptlang.org/docs/handbook/2/everyday-types.html>
- *React Native — Pressable*: <https://reactnative.dev/docs/pressable>
