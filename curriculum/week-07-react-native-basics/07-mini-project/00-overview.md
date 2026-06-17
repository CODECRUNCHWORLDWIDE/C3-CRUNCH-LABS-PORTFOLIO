# Mini-Project — Crunch Tracker Mobile Client (Habit List + Add Form)

> Build the first screen of the Crunch Tracker mobile app: an Expo + TypeScript app with a habit-list screen and an add-habit form, working entirely against **mocked local data**. No backend wiring this week — the Spring Boot API you built in weeks 1–6 sits untouched until Week 9. This week is about a clean, typed, performant, accessible single-screen UI.

This is the front-end counterpart to everything you've built so far. For six weeks the tracker had no face; now it gets one. By the end you'll have a public GitHub repo with an Expo app that runs on a real phone, lets a user add habits, check them off, and see a running "done today" count — all in strict TypeScript, all on local mock data designed to match the shape your API already returns.

**Estimated time:** ~8.5 hours (split across Thursday, Friday, Saturday in the suggested schedule).

---

## What you will build

An Expo app, `crunch-tracker-mobile`, with **one screen** that:

1. Loads a list of habits from a mock data module (with a simulated ~600ms delay so the loading state is real).
2. Shows a **loading** spinner, an **empty** state, and a **content** list — each rendered deliberately.
3. Renders habits in a `FlatList` of reusable, typed `HabitCard`s, each showing the title, cadence, a streak badge, and a check/uncheck control.
4. Has an **add-habit form** (a controlled `TextInput` + a button) that prepends a new habit to the list, with basic validation (no blank titles, no duplicate titles).
5. Lets the user **toggle** a habit's done-state and **delete** a habit, both updating state immutably.
6. Shows a small **summary header** — "N of M done today" — derived during render, never stored in state.

It runs on the iOS Simulator, the Android Emulator, and a real device via Expo Go. It type-checks with zero errors and shows zero LogBox warnings.

---

## Why mock data this week

The shape of the mock data is **deliberately the shape your API returns**. In Week 9 you'll replace the mock module with a typed `fetch` client against the live Spring Boot endpoint, and ideally *nothing else in the UI changes*. Designing the seam now — a single `mockData.ts` that the screen imports — is the whole point. Keep all "where does data come from" logic behind that one module so Week 9 is a swap, not a rewrite.

```ts
// data/mockData.ts — matches GET /api/v1/habits from your week-6 API
export interface Habit {
  id: string;
  title: string;
  cadence: "daily" | "weekly";
  streak: number;
  doneToday: boolean;
}

export const MOCK_HABITS: Habit[] = [
  { id: "1", title: "Drink water",        cadence: "daily",  streak: 3, doneToday: false },
  { id: "2", title: "Stretch",            cadence: "daily",  streak: 0, doneToday: false },
  { id: "3", title: "Read 20 minutes",    cadence: "daily",  streak: 7, doneToday: true  },
  { id: "4", title: "Review weekly goals",cadence: "weekly", streak: 2, doneToday: false },
];

// A function so the screen can "load" it asynchronously, mimicking a fetch.
export function fetchHabits(): Promise<Habit[]> {
  return new Promise((resolve) => setTimeout(() => resolve(MOCK_HABITS), 600));
}
```

---

## Rules

- **You may** read the Expo docs, the React Native docs, the React docs, the TypeScript handbook, and your own lecture notes and exercises.
- **You may NOT** wire up the real backend, `fetch` a network URL, add navigation libraries, or add a global state library (Context/Zustand). Those are Weeks 8 and 9. One screen, local state, mock data.
- **You may NOT** use `any`, disable `strict`, or `// @ts-ignore` your way past a type error. If a type is hard, the design is off.
- **You may NOT** render a long list with `.map()` inside a `ScrollView`. Use `FlatList`.
- **You must** keep `strict: true` in `tsconfig.json` (the Expo template's default — leave it on).
- **You must** keep all data access behind `data/mockData.ts` so Week 9 can swap it.
- Target: Expo SDK 53+ / React Native 0.79+ / React 19 / TypeScript 5.x (whatever `create-expo-app@latest` gives you in 2026).

---

## Acceptance criteria

- [ ] A new public GitHub repo named `c3-week-07-crunch-tracker-mobile-<yourhandle>`.
- [ ] Project layout:
  ```
  crunch-tracker-mobile/
  ├── App.tsx                        # renders <HabitListScreen />
  ├── app.json
  ├── tsconfig.json                  # strict: true
  ├── package.json
  ├── data/
  │   └── mockData.ts                # Habit type + MOCK_HABITS + fetchHabits()
  ├── components/
  │   ├── HabitCard.tsx              # one reusable, typed card
  │   ├── AddHabitForm.tsx           # controlled TextInput + Add button
  │   └── SummaryHeader.tsx          # "N of M done today"
  └── screens/
      └── HabitListScreen.tsx        # owns state, wires it all together
  ```
- [ ] `npx tsc --noEmit` prints **0 errors**.
- [ ] **Zero `any`** in any file you wrote. **Zero LogBox warnings** when the app runs.
- [ ] The app runs on a real device via Expo Go **and** at least one simulator/emulator.
- [ ] On open: a spinner shows (~600ms), then the list.
- [ ] If the list is empty (delete them all, or start from `[]`), a friendly empty state appears — not a blank screen, not a crash.
- [ ] Adding a habit: a non-blank, non-duplicate title prepends a new `HabitCard`; the input clears; the Add button is disabled while the input is blank.
- [ ] Toggling a habit flips its done state and tint; deleting removes it. Both update state immutably.
- [ ] The summary header reads "N of M done today" and is **derived during render** (no extra state, no effect).
- [ ] Every interactive control has a 44pt+ touch target, `accessibilityRole`, and an `accessibilityLabel`.
- [ ] The list uses `FlatList` with `keyExtractor` and a `ListEmptyComponent`.
- [ ] Your `README.md` includes: one paragraph describing the app; exact setup/run commands from a fresh clone; a screenshot or device photo; and a "Things I learned" section with at least 3 specific items.

---

## Suggested order of operations

Build incrementally. Each phase ends in a runnable app and a commit.

### Phase 1 — Scaffold (~0.5h)

```bash
npx create-expo-app@latest crunch-tracker-mobile --template blank-typescript
cd crunch-tracker-mobile
git init && git add . && git commit -m "Initial Expo blank-typescript app"
```

Confirm it runs on a device before writing a line of your own code. First commit: `Initial Expo blank-typescript app`.

### Phase 2 — The data seam (~0.5h)

Create `data/mockData.ts` exactly as shown above: the `Habit` interface, `MOCK_HABITS`, and `fetchHabits()`. This is the only file Week 9 will replace. Commit: `Mock data module (Habit type + fetchHabits)`.

### Phase 3 — The HabitCard (~1.5h)

Create `components/HabitCard.tsx`: a presentational component taking `habit: Habit`, `onToggle: (id) => void`, and `onDelete: (id) => void`. Render the check control (`Pressable`, 44pt, `accessibilityLabel`), title, cadence, a streak badge (only when `streak > 0`), and a delete control. Style it with `StyleSheet`; tint the card when `doneToday`. Reuse what you built in Exercise 2; add the delete control. Commit: `HabitCard component`.

### Phase 4 — The AddHabitForm (~1.5h)

Create `components/AddHabitForm.tsx`: a controlled `TextInput` (`value`/`onChangeText`) plus an Add `Pressable`. Take an `onAdd: (title: string) => void` prop and an `existingTitles: string[]` prop for duplicate-checking. Derive `canSubmit` during render: non-blank **and** not already present (case-insensitive). Disable the button when `!canSubmit`. Clear the field after a successful add. Commit: `AddHabitForm with validation`.

### Phase 5 — The SummaryHeader (~0.5h)

Create `components/SummaryHeader.tsx`: takes `done: number` and `total: number`, renders "N of M done today" plus the date. Pure presentational; no state. Commit: `SummaryHeader`.

### Phase 6 — The screen wires it together (~2h)

Create `screens/HabitListScreen.tsx`:

- State: `habits: Habit[]` (starts `[]`), `loading: boolean` (starts `true`).
- Effect: call `fetchHabits()` on mount; set state when it resolves; guard with a `cancelled` flag.
- Handlers: `addHabit(title)` (prepend a new habit with a generated id), `toggle(id)` (immutable map), `remove(id)` (immutable filter).
- Derive `doneCount = habits.filter(h => h.doneToday).length` during render.
- Render: loading branch (spinner), then `SafeAreaView` → `SummaryHeader` → `AddHabitForm` → `FlatList` of `HabitCard`s with a `ListEmptyComponent`.

Wire `App.tsx` to render `<HabitListScreen />`. Commit: `HabitListScreen wires the app together`.

### Phase 7 — Polish (~1.5h)

- Run `npx tsc --noEmit` and clear every error.
- Open the app and clear every LogBox warning (read each one; don't suppress).
- Walk the screen with VoiceOver/TalkBack on for two minutes — every control should announce something sensible.
- Test the empty state (delete all habits) and the loading state (it should always show on cold open).
- Write the `README.md` (paragraph, setup commands, screenshot, "Things I learned").
- Push to GitHub.

Commit: `Polish: a11y pass, README, screenshot`.

---

## Example expected behavior

On cold open:

```
[spinner ~600ms]
────────────────────────────
 Today, Jun 12          1 of 4 done
 ┌──────────────────────────────┐
 │ [＋ New habit…       ] [ Add ]│
 └──────────────────────────────┘
 ○  Drink water        daily   🔥 3   🗑
 ○  Stretch            daily          🗑
 ✓  Read 20 minutes    daily   🔥 7   🗑   (green tint)
 ○  Review weekly goals weekly  🔥 2   🗑
```

Type "Meditate", tap Add: a new `○ Meditate  daily  🗑` prepends, the input clears, and the header updates to "1 of 5 done". Tap "Drink water"'s ○: it becomes ✓, tints green, and the header reads "2 of 5 done". Delete a card: it disappears and the header recounts.

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Builds and runs | 20% | Fresh clone → `npm install` → `npx expo start` → runs on device; `tsc --noEmit` clean |
| Type hygiene | 15% | Zero `any`, zero `@ts-ignore`, `strict` on; props and data fully typed |
| State correctness | 20% | Add/toggle/delete all immutable; summary derived not stored; no stale-state bugs |
| Lists & states | 15% | `FlatList` (not `.map`) with `keyExtractor`; loading, empty, and content all handled |
| Styling & layout | 15% | Clean Flexbox layout, `SafeAreaView`, consistent spacing, survives small + large screens |
| Accessibility & touch | 10% | 44pt targets, `accessibilityRole`/`accessibilityLabel` on every control |
| README quality | 5% | Someone unfamiliar can clone and run in <5 minutes; screenshot present |

---

## Stretch (optional)

- Add a "filter" segmented control (All / Active / Done) above the list, deriving the visible list during render from a `filter` state — no second copy of the data.
- Persist habits across app restarts with `expo-secure-store` or `AsyncStorage` (this previews Week 8's persistence — keep it behind the same data seam).
- Add pull-to-refresh to the `FlatList` (`refreshing` + `onRefresh` re-calling `fetchHabits()`).
- Add a long-press on a card to reveal the delete control instead of always showing it.
- Add a light/dark theme using `useColorScheme()` and a small theme object.

---

## What this prepares you for

- **Week 8 (Navigation and App State)** turns this single screen into a tabbed, navigable app. The `HabitListScreen` becomes one tab; `HabitCard`'s `onPress` becomes a navigation to a detail screen; the screen-level state lifts into a Zustand store.
- **Week 9 (Full-Stack Integration)** replaces `data/mockData.ts` with a typed `fetch` client hitting your week-6 Spring Boot API, with JWT auth headers and TanStack Query for caching. Because you hid data access behind one module, this is a swap, not a rewrite.
- **Week 10 (Capstone)** ships this app — built with EAS, pointed at the deployed backend. The `HabitListScreen` you write this week is in the demo you give.

---

## Resources

- *Expo — Create a project*: <https://docs.expo.dev/get-started/create-a-project/>
- *React Native — FlatList*: <https://reactnative.dev/docs/flatlist>
- *React Native — TextInput*: <https://reactnative.dev/docs/textinput>
- *React — Updating Arrays in State*: <https://react.dev/learn/updating-arrays-in-state>
- *React Native — Layout with Flexbox*: <https://reactnative.dev/docs/flexbox>
- *React + TypeScript Cheatsheet*: <https://react-typescript-cheatsheet.netlify.app/>

---

## Submission

When done:

1. Push your repo to GitHub with a public URL.
2. Make sure `README.md` includes setup commands, a screenshot/photo, and the "Things I learned" section.
3. Make sure `npm install` then `npx expo start` runs on a freshly cloned copy, `npx tsc --noEmit` is clean, and the app shows no LogBox warnings.
4. Post the repo URL in your cohort tracker. You built the face of the product you've been working on for six weeks — show it.
