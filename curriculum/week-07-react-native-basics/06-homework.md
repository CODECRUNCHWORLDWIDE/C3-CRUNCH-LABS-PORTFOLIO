# Week 7 Homework

Six practice problems that revisit the week's topics. The full set should take about **6 hours** in total. Work in your Week 7 Git repository so each problem produces at least one commit you can point to later. Every problem must end with `npx tsc --noEmit` clean and no LogBox warnings.

Each problem includes:

- A short **problem statement**.
- **Acceptance criteria** so you know when you're done.
- A **hint** if you get stuck.
- An **estimated time**.

---

## Problem 1 — Environment audit

**Problem statement.** Get the toolchain working end to end and document it. Scaffold a throwaway `blank-typescript` app, run it on **both** a simulator/emulator and a real device via Expo Go, and write the relevant pieces into `notes/expo-info.md`:

1. Your `node --version` and `npm --version`.
2. The Expo SDK version (`package.json` → `dependencies.expo`) and the React Native version (`dependencies["react-native"]`).
3. Which device(s) you ran on, and whether you used LAN or `--tunnel` mode (and why).
4. One sentence: what is Expo Go doing that means you didn't need to open Xcode/Android Studio?

**Acceptance criteria.**

- File `notes/expo-info.md` exists with the four items.
- You actually ran the app on a real device at least once (note it).
- Committed.

**Hint.** `npx expo start` prints the connection mode. The Expo and RN versions are in `package.json`. Expo Go is a prebuilt native shell that loads your JS over the network.

**Estimated time.** 25 minutes.

---

## Problem 2 — Three typed components, one screen

**Problem statement.** In `homework/p2-components/`, build three small, fully typed presentational components and compose them in one screen:

- `Avatar` — props `{ initials: string; size?: number }`; renders a circle with the initials centered. Default size 40.
- `Pill` — props `{ label: string; tone?: "neutral" | "success" | "danger" }`; renders a rounded label whose background depends on `tone`.
- `StatRow` — props `{ label: string; value: number | string }`; renders the label on the left and the value on the right (use `justifyContent: "space-between"`).

Compose all three in a `ProfileCard` screen with realistic sample data.

**Acceptance criteria.**

- A runnable screen rendering all three components.
- `npx tsc --noEmit`: 0 errors. No `any`. `tone` is a union type, not `string`.
- `Avatar` honors an optional `size` with a default.
- The `Pill` background changes with `tone` (use a style array, not three copies).
- Committed.

**Hint.** Conditional style: `style={[styles.pill, tone === "success" && styles.pillSuccess, tone === "danger" && styles.pillDanger]}`. Default size in the destructure: `({ initials, size = 40 }: AvatarProps)`.

**Estimated time.** 1 hour.

---

## Problem 3 — Immutable state operations

**Problem statement.** In `homework/p3-state/`, build a screen with a `useState<Item[]>` where `Item = { id: string; label: string; selected: boolean }`. Implement and wire four buttons:

1. **Add** — prepend a new item (generate an id).
2. **Toggle** — flip `selected` on a tapped item.
3. **Remove** — delete a tapped item.
4. **Clear done** — remove every item where `selected` is `true`.

Every operation must be **immutable** (produce a new array; never mutate). Show a derived "X of Y selected" count in the header, computed during render.

**Acceptance criteria.**

- All four operations work and update the screen.
- Each operation uses an immutable pattern (`[...]`, `.map`, `.filter`) — verify you never call `.push`, `.splice`, or assign to an element.
- The selected count is derived during render, not stored in state.
- `npx tsc --noEmit`: 0 errors. No `any`. No LogBox warnings.
- Committed.

**Hint.** Toggle: `setItems(prev => prev.map(i => i.id === id ? { ...i, selected: !i.selected } : i))`. Clear done: `setItems(prev => prev.filter(i => !i.selected))`.

**Estimated time.** 1 hour.

---

## Problem 4 — A FlatList with all three states

**Problem statement.** In `homework/p4-flatlist/`, build a screen that loads a list of at least 12 items through a `useEffect` (use a `setTimeout` to simulate an ~800ms fetch). Render with a `FlatList`. Handle:

1. **Loading** — an `ActivityIndicator`, centered.
2. **Empty** — if the loaded data is `[]`, a friendly empty message via `ListEmptyComponent`.
3. **Content** — the list, with an `ItemSeparatorComponent` between rows and a `ListHeaderComponent` showing the count.

The effect must clean up its timer and guard against setting state after unmount.

**Acceptance criteria.**

- Loading → content transition is visible on open.
- Setting the mock data to `[]` shows the empty state (try it, then put it back).
- The effect returns a cleanup that clears the timer and flips a `cancelled` flag.
- Uses `FlatList` with `keyExtractor`, not `.map()`.
- `npx tsc --noEmit`: 0 errors. No `any`. No LogBox warnings (including no "set state on unmounted" warning when you hot-reload).
- Committed.

**Hint.** The cleanup pattern: `let cancelled = false; const t = setTimeout(...); return () => { cancelled = true; clearTimeout(t); };`. Only `setState` inside the timeout `if (!cancelled)`.

**Estimated time.** 1 hour.

---

## Problem 5 — A controlled form with validation

**Problem statement.** In `homework/p5-form/`, build an add-item form: a controlled `TextInput` plus an Add `Pressable`. Take an `existing: string[]` prop. Derive (during render, no extra state):

- `canSubmit` = the trimmed input is non-empty **and** not already in `existing` (case-insensitive).

The Add button is `disabled` when `!canSubmit`. On submit, call an `onAdd(title)` callback and clear the field. Show an inline hint ("Already exists" / "Enter a name") when input is present but invalid.

**Acceptance criteria.**

- The input is controlled (`value` + `onChangeText`).
- `canSubmit` is derived during render, not stored.
- The button is visibly disabled when the input is blank or a duplicate.
- Submitting clears the field; submitting is also wired to the keyboard "done" via `onSubmitEditing`.
- `npx tsc --noEmit`: 0 errors. No `any`. No LogBox warnings.
- Committed.

**Hint.** `const dup = existing.some(e => e.toLowerCase() === title.trim().toLowerCase());` and `const canSubmit = title.trim().length > 0 && !dup;`.

**Estimated time.** 1 hour.

---

## Problem 6 — Mini reflection essay

**Problem statement.** Write a 300–400 word reflection at `notes/week-07-reflection.md` answering:

1. Which felt easiest: the toolchain (Expo/TS), the render model (props/state/re-render), or styling/layout (Flexbox/`StyleSheet`)? Which felt hardest? Why?
2. Coming from the backend half of this course (or from web React, if you knew it), what surprised you most about React Native — no DOM, immutable state, `column` default, something else?
3. In one paragraph, how would you explain "why mutating state in place does nothing" to a teammate who's new to React?
4. What's one thing you want to learn next that this week didn't cover? (Navigation? Real data? Animations?)

**Acceptance criteria.**

- File exists, 300–400 words.
- Each numbered question is addressed in its own paragraph.
- File is committed.

**Hint.** This is for *you*, not for a grade. Be honest. Future-you reading it after the capstone will be grateful.

**Estimated time.** 30 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 25 min |
| 2 | 1 h 0 min |
| 3 | 1 h 0 min |
| 4 | 1 h 0 min |
| 5 | 1 h 0 min |
| 6 | 30 min |
| **Total** | **~4 h 55 min** |

---

## Grading rubric

Each problem is graded out of the points shown; the homework totals **100 points**.

| Problem | Points | What earns full marks |
|--------:|-------:|-----------------------|
| 1 — Environment audit | 10 | App ran on a real device; all four items documented accurately |
| 2 — Typed components | 20 | Three components fully typed (union `tone`, optional `size` default); composed; zero `any` |
| 3 — Immutable state | 20 | All four ops immutable (no `push`/`splice`/index assign); count derived during render |
| 4 — FlatList states | 20 | Loading/empty/content all handled; effect cleanup correct; `FlatList` not `.map()` |
| 5 — Controlled form | 20 | Controlled input; `canSubmit` derived; duplicate + blank both blocked; field clears |
| 6 — Reflection | 10 | 300–400 words; all four questions addressed in their own paragraphs |

**Cross-cutting deductions (applied to the total):**

- **−5 per `any`** you wrote (excluding generated/template code).
- **−5 per `@ts-ignore` or `@ts-expect-error`** used to dodge a real type error.
- **−5 if `npx tsc --noEmit` reports any error** on a fresh clone.
- **−3 per unaddressed LogBox warning** that appears during normal use.
- **−5 if any required commit is missing** (each problem must have at least one).

**Bonus (max +5):** a thoughtful stretch beyond the spec on any problem — e.g. extracting a reusable hook, adding accessibility labels everywhere, or a tasteful empty-state illustration — at the grader's discretion.

When you've finished all six, push your repo and open the [mini-project](./07-mini-project/00-overview.md).
