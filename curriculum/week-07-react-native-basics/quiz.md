# Week 7 — Quiz

Ten multiple-choice questions. Take it with your lecture notes closed. Aim for 9/10 before moving to Week 8. Answer key at the bottom — don't peek.

---

**Q1.** Which of the following best describes the relationship between React, React Native, and Expo?

- A) They are three names for the same product, shipped together for marketing reasons.
- B) React is the render model, React Native is a renderer that targets native views, and Expo is the toolchain/SDK on top of React Native.
- C) Expo is a subset of React; you need Expo to write any React component.
- D) React Native is the language, React is the runtime, and Expo is the IDE.

---

**Q2.** What is wrong with this React Native snippet?

```tsx
<View>Drink water</View>
```

- A) Nothing — `<View>` renders text just like a `<div>`.
- B) `<View>` is not a valid component; it should be `<div>`.
- C) A bare string can't live directly in a `<View>` — text must be inside a `<Text>` component.
- D) `View` must be imported from `expo`, not `react-native`.

---

**Q3.** Given:

```tsx
const [habits, setHabits] = useState<Habit[]>([]);

function add(h: Habit) {
  habits.push(h);
}
```

Why does calling `add(...)` not update the screen?

- A) `push` is asynchronous and hasn't finished yet.
- B) Mutating the array in place keeps the same reference, so React detects no change and does not re-render. You must call `setHabits` with a new array.
- C) `useState` arrays are read-only and `push` silently throws.
- D) The component is missing a `useEffect` to watch `habits`.

---

**Q4.** Which call correctly increments `count` by 2 when run twice in a row?

```tsx
const [count, setCount] = useState(0);
```

- A) `setCount(count + 1); setCount(count + 1);`
- B) `setCount((c) => c + 1); setCount((c) => c + 1);`
- C) `count += 2;`
- D) `setCount(count++); setCount(count++);`

---

**Q5.** What does the dependency array `[]` mean in `useEffect(() => { ... }, [])`?

- A) Run the effect after every render.
- B) Run the effect once, after the first render (on mount).
- C) Never run the effect.
- D) Run the effect only when the component unmounts.

---

**Q6.** Why prefer `FlatList` over `.map()` inside a `ScrollView` for a long list?

- A) `.map()` doesn't work inside JSX.
- B) `FlatList` virtualizes — it renders only the rows near the viewport and recycles them — while `.map()` renders every item up front, hurting memory and performance.
- C) `FlatList` is required to use TypeScript.
- D) `ScrollView` can't render `<Text>` elements.

---

**Q7.** In a React Native `StyleSheet`, what is the default `flexDirection`, and how does it differ from the web?

- A) `row` — same as the web's CSS default.
- B) `column` — the opposite of the web's CSS default of `row`.
- C) There is no default; you must always set it.
- D) `inline` — there is no flex layout in React Native.

---

**Q8.** What's the bug here, and how do you fix it?

```tsx
{streak && <Text>🔥 {streak}-day streak</Text>}
```

- A) No bug — this always renders correctly.
- B) When `streak` is `0`, the expression renders the literal `0` (which crashes outside a `<Text>`). Fix it with `{streak > 0 && ...}`.
- C) `&&` is not allowed in JSX; use a ternary.
- D) `streak` must be wrapped in `String()` first.

---

**Q9.** A screen loads data in a `useEffect` with a `setTimeout`. Why return a cleanup function that clears the timer and flips a `cancelled` flag?

- A) It's required syntax; effects without a return don't compile.
- B) To avoid setting state after the component has unmounted (a leak/warning) and to cancel the pending timer.
- C) To make the effect run on every render instead of once.
- D) To convert the effect into a synchronous function.

---

**Q10.** Which is the correct, idiomatic way to type a function component's props in 2026?

- A) `const C: React.FC = (props) => { ... }` with no prop types.
- B) `function C(props: any) { ... }`
- C) Define an `interface CProps { ... }` and write `function C({ x, y }: CProps) { ... }`.
- D) Don't type props; TypeScript infers them from usage.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — React is the renderer-agnostic model (components/props/state/hooks); React Native is the renderer that targets native iOS/Android views; Expo is the toolchain/SDK/runtime (create-expo-app, Metro, Expo Go) on top. They ship aligned but are distinct.
2. **C** — There is no implicit text node in React Native like the web has. Every string must be wrapped in `<Text>`; a bare string in a `<View>` throws "Text strings must be rendered within a `<Text>` component."
3. **B** — React detects change by reference. `push` mutates in place and keeps the same array reference, so React sees nothing to do. You must produce a new array (`setHabits(prev => [...prev, h])`).
4. **B** — Inside one render `count` is a fixed snapshot, so `count + 1` twice computes the same number. The functional updater `(c) => c + 1` always receives the latest value, so the two updates compose to +2.
5. **B** — An empty dependency array means the effect runs once, after the first render (on mount). Its cleanup, if any, runs on unmount.
6. **B** — `FlatList` virtualizes the list, rendering only what's near the viewport and recycling rows. `.map()` inside a `ScrollView` renders the entire dataset eagerly, which janks and bloats memory for long lists.
7. **B** — React Native's default `flexDirection` is `column` (children stack vertically), the opposite of the web CSS default of `row`. This trips up every web developer exactly once.
8. **B** — When `streak` is `0`, `0 && <Text>...</Text>` evaluates to `0`, and React renders that `0` — which, outside a `<Text>`, crashes. Make the left side a real boolean: `streak > 0 && ...`.
9. **B** — The cleanup cancels the pending timer and the `cancelled` flag prevents calling `setState` after unmount, which would leak and warn. It runs on unmount (and before the effect re-runs).
10. **C** — The 2026 idiom is a plain function with a typed props parameter via an `interface`. `React.FC` (A) is discouraged for historical reasons; `any` (B) defeats type safety; (D) loses the explicit prop contract.

</details>

---

If you scored under 7, re-read the lectures for the questions you missed — especially the render model (Q3, Q4) and the JSX gotchas (Q2, Q8). If you scored 9 or 10, you're ready to dive into the [homework](./homework.md).
