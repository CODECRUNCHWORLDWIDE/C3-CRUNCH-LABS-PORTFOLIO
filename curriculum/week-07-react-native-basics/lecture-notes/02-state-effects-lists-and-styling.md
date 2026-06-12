# Lecture 2 — State, Effects, Lists, and Styling

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can manage local state with `useState` (including functional updates and immutable array/object updates), run side effects with `useEffect` (dependency array + cleanup), render long lists performantly with `FlatList`, style screens with `StyleSheet` + Flexbox, size touch targets correctly, and handle the loading/empty/content states every screen has.

If you only remember one thing from this lecture, remember this:

> **State changes are requests to re-render with new, immutable data. Effects synchronize your component with the outside world. Lists are virtualized with `FlatList`, never `.map()` for long data. Layout is Flexbox with `column` as the default axis.** Those four ideas are 90% of mobile UI work.

This lecture has a lot of surface area on purpose: the mini-project uses every piece.

---

## 1. `useState` — the workhorse hook

A component's own changeable data is **state**. You declare it with `useState`:

```tsx
import { useState } from "react";

function Counter() {
  const [count, setCount] = useState(0);
  return (
    <Pressable onPress={() => setCount(count + 1)}>
      <Text>Pressed {count} times</Text>
    </Pressable>
  );
}
```

`useState(0)` returns a pair: the current value (`count`) and a setter (`setCount`). The `[count, setCount]` is array destructuring; the names are yours. **Calling the setter asks React to re-render the component with the new value.** It does not change `count` in the current render — `count` is a snapshot, fixed for this render.

### Functional updates — the snapshot trap

```tsx
// BUG: both reads see the SAME stale count. This increments by 1, not 2.
function addTwo() {
  setCount(count + 1);
  setCount(count + 1);
}

// FIX: the functional updater always receives the latest value.
function addTwo() {
  setCount((c) => c + 1);
  setCount((c) => c + 1);   // now genuinely +2
}
```

Inside a single render, `count` has one fixed value. `setCount(count + 1)` twice computes the same number twice. The **functional updater** `setCount((c) => c + 1)` hands you the most up-to-date value, so updates compose. **Rule of thumb: when the new state depends on the old state, use the functional form.**

### Lazy initial state

If computing the initial value is expensive (parsing a big mock dataset, reading a file), pass a *function* so it runs only once, on mount, instead of every render:

```tsx
const [habits, setHabits] = useState<Habit[]>(() => loadMockHabits());
```

`useState(loadMockHabits())` would call `loadMockHabits()` on every render and throw the result away every time but the first. `useState(() => loadMockHabits())` calls it once. Small thing, real difference on a hot list.

### Typing state

```tsx
const [count, setCount] = useState(0);                     // number, inferred
const [query, setQuery] = useState("");                    // string, inferred
const [selected, setSelected] = useState<Habit | null>(null);  // explicit: starts null
const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
```

Let inference work when the initial value tells the whole story. Annotate when the type is wider than the initial value — when it can be `null`, or when it's a union of states.

---

## 2. Immutable updates — arrays and objects

React detects change by comparing **references**. To trigger a re-render you must hand the setter a **new** object/array, never a mutated copy of the old one. This is the single most important discipline in React.

### Arrays

```tsx
// Add
setHabits((prev) => [...prev, newHabit]);

// Remove by id
setHabits((prev) => prev.filter((h) => h.id !== idToRemove));

// Update one item by id
setHabits((prev) =>
  prev.map((h) => (h.id === id ? { ...h, doneToday: !h.doneToday } : h)),
);
```

Note every one produces a **new array**. `prev.map(...)` returns a fresh array; the `{ ...h, doneToday: ... }` produces a fresh object for the one row that changed and reuses the rest. Never `prev.push(...)`, `prev[i].done = true`, or `prev.splice(...)` — those mutate in place and React won't notice.

### Objects

```tsx
setForm((prev) => ({ ...prev, title: newTitle }));   // spread, then override one field
```

The parentheses around the object are required — `(prev) => ({ ... })` — otherwise the `{` is read as a function body, not an object literal. A classic five-minute bug.

> **Why immutability?** Two reasons. (1) Reference comparison: a new reference is how React knows to re-render. (2) Predictability: if state is never mutated in place, you can reason about each render as a pure function of its inputs. Once this is a reflex you stop having "why didn't it update?" bugs.

---

## 3. `useEffect` — synchronizing with the outside world

State and props describe *what to render*. Sometimes you need to do something *outside* React when they change: fetch data, start a timer, subscribe to an event, set the screen title. That's a **side effect**, and it lives in `useEffect`.

```tsx
import { useEffect, useState } from "react";

function HabitListScreen() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    // Simulate loading mock data with a small delay so we can see the loading state.
    const timer = setTimeout(() => {
      if (!cancelled) {
        setHabits(MOCK_HABITS);
        setLoading(false);
      }
    }, 600);

    // Cleanup: runs when the component unmounts or before the effect re-runs.
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, []);   // empty deps → run once, on mount

  // ...
}
```

Three things to understand:

### The dependency array

The second argument controls *when* the effect re-runs:

| Deps | When the effect runs |
|------|----------------------|
| `[]` | Once, after the first render (mount). The "load on open" case. |
| `[query]` | After mount, and again whenever `query` changes. |
| *(omitted)* | After **every** render. Almost always a mistake. |

**Every value from props/state that the effect reads must be in the deps array.** Leaving one out gives you an effect that reads stale data — a "stale closure" bug. In 2026 the ESLint rule `react-hooks/exhaustive-deps` (on by default in the Expo template) will warn you. Listen to it.

### The cleanup function

If the effect returns a function, React runs it as cleanup — when the component unmounts, or right before the effect runs again. Use it to clear timers, cancel subscriptions, and (as above) guard against setting state after the component is gone. **Setting state on an unmounted component is a leak**; the `cancelled` flag is the standard guard.

### What NOT to use `useEffect` for

A common over-use: running an effect to compute a value from state. Don't. If you can compute it during render, just compute it:

```tsx
// BAD: an effect to derive filtered list into more state.
useEffect(() => { setVisible(habits.filter(h => !h.doneToday)); }, [habits]);

// GOOD: derive it during render. No effect, no extra state.
const visible = habits.filter((h) => !h.doneToday);
```

Effects are for **synchronizing with things outside React** (network, timers, native APIs, the device). Derived data is just a calculation — do it inline.

### The rules of hooks

Two rules, never broken:

1. **Only call hooks at the top level** of a component — never inside an `if`, a loop, or a nested function. React tracks hooks by call order; conditional calls scramble that.
2. **Only call hooks from React function components** (or custom hooks), never from plain functions.

The ESLint rule `react-hooks/rules-of-hooks` enforces both. If you find yourself wanting a hook inside an `if`, lift the condition *inside* the hook instead.

---

## 4. `FlatList` — performant lists

For a handful of items, `.map()` inside a `<ScrollView>` is fine. For a list that could be long (and a habit tracker's history can be), `.map()` is a trap: it renders **every** item up front, even the 900 below the fold, blowing up memory and jank. The fix is `FlatList`, which **virtualizes** — it only renders the rows near the viewport and recycles them as you scroll.

```tsx
import { FlatList, Text, View } from "react-native";

function HabitList({ habits, onToggle }: HabitListProps) {
  return (
    <FlatList
      data={habits}
      keyExtractor={(item) => item.id}
      renderItem={({ item }) => (
        <HabitRow habit={item} onToggle={onToggle} />
      )}
      ItemSeparatorComponent={() => <View style={styles.separator} />}
      ListEmptyComponent={() => (
        <Text style={styles.empty}>No habits yet. Add one to get started.</Text>
      )}
      contentContainerStyle={styles.listContent}
    />
  );
}
```

The props you'll use constantly:

| Prop | What it does |
|------|--------------|
| `data` | The array to render. Typed — `FlatList<Habit>` infers from `data`. |
| `renderItem` | `({ item }) => JSX` — renders one row. `item` is typed as your element type. |
| `keyExtractor` | Returns a stable unique key per item. Same role as `key` in `.map()`. |
| `ListEmptyComponent` | Rendered when `data` is empty. Your "empty state" lives here. |
| `ItemSeparatorComponent` | Rendered between rows (not above the first or below the last). |
| `ListHeaderComponent` / `ListFooterComponent` | Rendered above/below the whole list. |
| `contentContainerStyle` | Padding/styles for the scrollable content (not the outer frame). |
| `refreshing` + `onRefresh` | Pull-to-refresh. (We use these in Week 9 with real data.) |

> **Don't put a `FlatList` inside a `ScrollView`.** Both scroll vertically; nesting them breaks virtualization and warns in LogBox. If you need a scrollable header above a list, use `ListHeaderComponent`, not a wrapping `ScrollView`.

For very long lists, the perf knobs are `initialNumToRender`, `windowSize`, and `getItemLayout` — see the optimizing guide in resources. You won't need them in week 7's data sizes, but know they exist.

---

## 5. Styling: `StyleSheet`, the style object, and the cascade that isn't

Styles in React Native are **JavaScript objects**, not CSS. There is no cascade, no inheritance (except a little, within `<Text>`), no selectors. You attach a style object to a component via the `style` prop.

```tsx
import { StyleSheet } from "react-native";

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#fff",
    borderRadius: 12,
    padding: 16,
    marginVertical: 6,
  },
  title: {
    fontSize: 17,
    fontWeight: "600",
    color: "#111",
  },
});

<View style={styles.card}>
  <Text style={styles.title}>Drink water</Text>
</View>
```

### Why `StyleSheet.create` and not a plain object?

You *can* pass a plain inline object (`style={{ padding: 16 }}`), and it works. But `StyleSheet.create` gives you (a) type-checking of property names and values, (b) a small perf win (styles are registered once), and (c) a clean separation of layout from logic. Use `StyleSheet.create` for anything reused; reserve inline objects for one-off dynamic values.

### Combining styles

The `style` prop accepts an **array**; later entries override earlier ones:

```tsx
<View style={[styles.card, isSelected && styles.cardSelected]} />
```

A falsy entry (`false`, `null`, `undefined`) is ignored, which is why `isSelected && styles.cardSelected` is the idiom for conditional styling.

### Units and key differences from CSS

- Numbers are **density-independent units**, not CSS pixels. `padding: 16` scales sensibly across a cheap Android phone and an iPad.
- `fontWeight` is a string: `"400"`, `"600"`, `"bold"`.
- There's no `margin: "16px"` — just `margin: 16`. Percentages are strings: `width: "50%"`.
- Most CSS properties exist, but not all. `boxShadow` is `shadowColor`/`shadowOffset`/`shadowOpacity`/`shadowRadius` (iOS) plus `elevation` (Android).
- Text styling (`fontSize`, `color`, `fontWeight`) goes on `<Text>`, not `<View>`. A `<View>` has no text properties.

---

## 6. Flexbox — the layout system

**All layout in React Native is Flexbox.** If you know web Flexbox, two things differ:

1. **The default `flexDirection` is `column`, not `row`.** Children stack vertically unless you say otherwise. (On the web it defaults to `row`.) This trips up every web dev exactly once.
2. **Everything is `display: flex` already.** You don't write `display: flex`; it's the only layout model.

The core properties:

```tsx
const styles = StyleSheet.create({
  // A vertical container that fills the screen.
  screen: {
    flex: 1,                       // fill available space
    flexDirection: "column",       // (the default — stack children top to bottom)
  },
  // A horizontal row: icon on the left, text in the middle, chevron on the right.
  row: {
    flexDirection: "row",          // lay children left to right
    alignItems: "center",          // center them on the cross (vertical) axis
    justifyContent: "space-between", // spread them along the main (horizontal) axis
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  // The middle item that should grow to push the chevron to the edge.
  rowBody: {
    flex: 1,                       // take all leftover space
    marginHorizontal: 12,
  },
});
```

| Property | Controls |
|----------|----------|
| `flexDirection` | The **main axis**: `column` (default, vertical) or `row` (horizontal). |
| `justifyContent` | Distribution along the **main** axis: `flex-start`, `center`, `space-between`, `space-around`, `space-evenly`. |
| `alignItems` | Alignment along the **cross** axis: `flex-start`, `center`, `stretch`, `flex-end`. |
| `flex: 1` | "Grow to fill leftover space." The most-used layout property. |
| `gap` | Spacing between flex children (supported in modern RN — cleaner than margins). |

> **The `flex: 1` mental model.** `flex: 1` on a child means "take all the remaining space on the main axis." Put `flex: 1` on the screen container to fill the device; put `flex: 1` on a row's middle element to push its siblings to the edges. Master `flex: 1` and 80% of layout falls out.

---

## 7. `SafeAreaView` and the notch

Phones have notches, dynamic islands, rounded corners, and home indicators. Content drawn at the very top or bottom gets clipped. Wrap your screen so content stays inside the safe area:

```tsx
import { SafeAreaView } from "react-native-safe-area-context";

export function HabitListScreen() {
  return (
    <SafeAreaView style={styles.screen}>
      {/* your content */}
    </SafeAreaView>
  );
}
```

Use `SafeAreaView` from `react-native-safe-area-context` (Expo bundles it), not the deprecated one from `react-native` core — the community package handles Android and edge insets correctly. Wrap the **outermost** view of each screen.

---

## 8. Touch targets and accessibility

A button you can't reliably tap is a broken button. Apple's HIG and Android's Material guidelines both set a minimum touch target of about **44pt** (iOS) / **48dp** (Android). Size your pressables accordingly:

```tsx
const styles = StyleSheet.create({
  iconButton: {
    minWidth: 44,
    minHeight: 44,
    alignItems: "center",
    justifyContent: "center",
  },
});
```

And give every interactive element an accessible label and role:

```tsx
<Pressable
  onPress={onToggle}
  accessibilityRole="button"
  accessibilityLabel={`Mark ${habit.title} ${habit.doneToday ? "not done" : "done"}`}
  hitSlop={8}              // extends the tappable area without growing the visual
  style={styles.iconButton}
>
  <Text>{habit.doneToday ? "✓" : "○"}</Text>
</Pressable>
```

`hitSlop` is a cheap way to make a visually small control easier to hit. `accessibilityLabel` is what a screen reader announces; `accessibilityRole="button"` tells assistive tech this is tappable. The spec-faithful challenge this week checks all three.

---

## 9. The three states every screen has

A screen that only renders the happy path is half-built. Every data-driven screen has **at least three** states, and you must render each deliberately:

1. **Loading** — data is on the way (an effect is running). Show a spinner or skeleton.
2. **Empty** — the load finished and there's nothing. Show a friendly empty state, not a blank screen.
3. **Content** — you have data. Render it.

(In Week 9 we add a fourth, **error**, when the network fails. For mock data this week, three is enough.)

```tsx
function HabitListScreen() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const t = setTimeout(() => {
      if (!cancelled) { setHabits(MOCK_HABITS); setLoading(false); }
    }, 600);
    return () => { cancelled = true; clearTimeout(t); };
  }, []);

  if (loading) {
    return (
      <SafeAreaView style={styles.center}>
        <ActivityIndicator size="large" />
        <Text style={styles.muted}>Loading your habits…</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <FlatList
        data={habits}
        keyExtractor={(h) => h.id}
        renderItem={({ item }) => <HabitRow habit={item} onToggle={toggle} />}
        ListEmptyComponent={() => (
          <Text style={styles.empty}>No habits yet. Tap + to add one.</Text>
        )}
      />
    </SafeAreaView>
  );
}
```

`ActivityIndicator` is RN's built-in spinner. Notice the loading branch returns *early* — a clean way to render mutually-exclusive states. The empty case is handled by `FlatList`'s `ListEmptyComponent`, so we don't need a separate branch for it.

---

## 10. Controlled inputs — the add-habit form preview

The mini-project includes an add-habit form. Inputs in React are **controlled**: state is the source of truth, and the input reflects it.

```tsx
function AddHabitForm({ onAdd }: { onAdd: (title: string) => void }) {
  const [title, setTitle] = useState("");

  const trimmed = title.trim();
  const canSubmit = trimmed.length > 0;

  return (
    <View style={styles.form}>
      <TextInput
        value={title}
        onChangeText={setTitle}
        placeholder="New habit (e.g. Drink water)"
        style={styles.input}
        returnKeyType="done"
        onSubmitEditing={() => canSubmit && submit()}
      />
      <Pressable
        onPress={submit}
        disabled={!canSubmit}
        style={[styles.addButton, !canSubmit && styles.addButtonDisabled]}
        accessibilityRole="button"
        accessibilityLabel="Add habit"
      >
        <Text style={styles.addButtonText}>Add</Text>
      </Pressable>
    </View>
  );

  function submit() {
    if (!canSubmit) return;
    onAdd(trimmed);
    setTitle("");        // clear the field after adding
  }
}
```

`value={title}` + `onChangeText={setTitle}` is the controlled pattern: every keystroke updates state, and state drives the input. `canSubmit` is **derived during render** (no effect, no extra state) and gates both the button's `disabled` and the submit. After adding, we reset the field by setting state.

---

## 11. Putting it together — the shape of the mini-project screen

Here's the skeleton the mini-project fleshes out. It uses every idea from both lectures:

```tsx
import { useEffect, useState } from "react";
import { ActivityIndicator, FlatList, SafeAreaView, Text, View } from "react-native";

import { AddHabitForm } from "./AddHabitForm";
import { HabitRow } from "./HabitRow";
import { MOCK_HABITS, type Habit } from "./mockData";

export default function HabitListScreen() {
  const [habits, setHabits] = useState<Habit[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const t = setTimeout(() => {
      if (!cancelled) { setHabits(MOCK_HABITS); setLoading(false); }
    }, 600);
    return () => { cancelled = true; clearTimeout(t); };
  }, []);

  function addHabit(title: string) {
    const habit: Habit = {
      id: `${Date.now()}`, title, cadence: "daily", streak: 0, doneToday: false,
    };
    setHabits((prev) => [habit, ...prev]);
  }

  function toggle(id: string) {
    setHabits((prev) =>
      prev.map((h) => (h.id === id ? { ...h, doneToday: !h.doneToday } : h)),
    );
  }

  if (loading) {
    return (
      <SafeAreaView style={styles.center}>
        <ActivityIndicator size="large" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <Text style={styles.heading}>Today</Text>
      <AddHabitForm onAdd={addHabit} />
      <FlatList
        data={habits}
        keyExtractor={(h) => h.id}
        renderItem={({ item }) => <HabitRow habit={item} onToggle={toggle} />}
        ListEmptyComponent={() => <Text style={styles.empty}>No habits yet.</Text>}
      />
    </SafeAreaView>
  );
}
```

Read it slowly. There are eight week-7 ideas in forty lines: typed state, a load-on-mount effect with cleanup, immutable array updates (add prepends, toggle maps), props-down/events-up to the form and rows, a `FlatList` with `keyExtractor` and an empty state, an early-return loading branch, and `SafeAreaView`. **No `any`. No mutated state. No `.map()` for the long list.** That's the modern React Native baseline, and the mini-project is exactly this plus styling and polish.

---

## 12. Recap

You should now be able to:

- Declare and update state with `useState`, including functional updates and immutable array/object patterns.
- Run a load-on-mount effect with `useEffect`, get the dependency array right, and clean up timers/subscriptions.
- Explain why deriving data during render beats stuffing it into an effect.
- Render long lists with `FlatList`, with `keyExtractor`, `renderItem`, and `ListEmptyComponent`.
- Style with `StyleSheet.create`, combine styles with arrays, and lay out with Flexbox (remembering `column` is the default).
- Wrap screens in `SafeAreaView` and size touch targets to 44pt with accessible labels.
- Render loading, empty, and content states deliberately.
- Build a controlled `TextInput` form with derived validation.

Next, do the exercises — three drills that exercise each of these skills, then the mini-project that combines them into the Crunch Tracker mobile client.

---

## References

- *React — State: A Component's Memory*: <https://react.dev/learn/state-a-components-memory>
- *React — Synchronizing with Effects*: <https://react.dev/learn/synchronizing-with-effects>
- *React — You Might Not Need an Effect*: <https://react.dev/learn/you-might-not-need-an-effect>
- *React Native — FlatList*: <https://reactnative.dev/docs/flatlist>
- *React Native — StyleSheet*: <https://reactnative.dev/docs/stylesheet>
- *React Native — Layout with Flexbox*: <https://reactnative.dev/docs/flexbox>
- *React Native — Pressable*: <https://reactnative.dev/docs/pressable>
