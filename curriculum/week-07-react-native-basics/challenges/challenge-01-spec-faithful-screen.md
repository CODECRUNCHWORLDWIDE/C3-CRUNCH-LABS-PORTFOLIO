# Challenge 1 — Build a Spec-Faithful Screen

**Time estimate:** ~90 minutes.

## Problem statement

You're handed a design spec for a **"Habit Detail"** screen and asked to build it faithfully as a typed, reusable component — the kind of ticket you'll get on any real mobile team. The spec is precise. Build it to the numbers, fully typed, with proper touch targets and accessibility, and handle every state the data can be in. No `any`. No LogBox warnings.

This is the screen a user lands on when they tap a habit in the list. It shows the habit, its streak, a week-strip of the last 7 days, and a primary "Check in" button.

## The design spec

Build a `HabitDetailScreen` that takes a `habitId` prop, loads the matching habit from mock data (with a simulated delay), and renders:

```
┌─────────────────────────────────────────┐
│  ‹ Back                                  │   ← back row, 44pt tall, left-aligned
│                                          │
│   Drink water                            │   ← title, 28pt, weight 800, #111
│   Daily · 🔥 7-day streak                │   ← subtitle, 15pt, #666
│                                          │
│   ┌───┬───┬───┬───┬───┬───┬───┐          │   ← week strip: 7 cells, equal width
│   │ M │ T │ W │ T │ F │ S │ S │          │     each cell shows the weekday letter
│   │ ● │ ● │ ● │ ○ │ ● │ ● │ ○ │          │     and a filled (●) / empty (○) dot
│   └───┴───┴───┴───┴───┴───┴───┘          │
│                                          │
│   This week: 5 of 7 days                 │   ← derived count, 15pt, #444
│                                          │
│                                          │
│   ┌─────────────────────────────────┐    │
│   │          Check in today          │    │   ← primary button, full width minus
│   └─────────────────────────────────┘    │     32pt side margins, 52pt tall
└─────────────────────────────────────────┘
```

### Exact requirements

**Data.** Use this type and mock source:

```tsx
export interface HabitDetail {
  id: string;
  title: string;
  cadence: "daily" | "weekly";
  streak: number;
  // last 7 days, oldest first; true = completed that day
  week: [boolean, boolean, boolean, boolean, boolean, boolean, boolean];
  doneToday: boolean;
}

const MOCK: Record<string, HabitDetail> = {
  "1": {
    id: "1",
    title: "Drink water",
    cadence: "daily",
    streak: 7,
    week: [true, true, true, false, true, true, false],
    doneToday: false,
  },
};
```

**Layout numbers (build to these, not "close enough"):**

- Back row: `height: 44`, content left-aligned, `‹ Back` tappable with `accessibilityRole="button"` and `accessibilityLabel="Go back"`.
- Title: `fontSize: 28`, `fontWeight: "800"`, `color: "#111"`.
- Subtitle: `fontSize: 15`, `color: "#666"`, shows cadence (capitalized) and the streak; the streak segment renders **only if `streak > 0`**.
- Week strip: a `flexDirection: "row"` of **7 equal-width cells** (`flex: 1` each). Each cell shows a weekday letter (`["M","T","W","T","F","S","S"]`) above a `●` (completed) or `○` (not). Completed dots are `#0B5`; empty dots are `#CCC`.
- "This week" line: derived as `week.filter(Boolean).length` of 7. Must be **computed during render**, not stored in state.
- Primary button: side margins of `16` (so width is screen minus 32), `height: 52`, `borderRadius: 12`, centered label `"Check in today"` (or `"Checked in ✓"` when `doneToday`). `accessibilityRole="button"`.

**Behavior:**

- On mount, show an `ActivityIndicator` for ~500ms (simulated load), then the content.
- If `habitId` doesn't exist in `MOCK`, show a "Habit not found." empty/not-found state — **not** a crash and **not** a blank screen.
- Tapping "Check in today" toggles `doneToday` immutably, updates the button label, and (your choice) bumps the streak. Keep state updates immutable.
- The back button takes an `onBack: () => void` prop and calls it on press. (No real navigation this week — just the callback.)

## Acceptance criteria

- [ ] A `HabitDetailScreen.tsx` (plus any small sub-components you extract) in your week-7 app.
- [ ] `npx tsc --noEmit`: **0 errors**. **Zero `any`** in anything you wrote. The `week` field is a 7-tuple, typed as shown.
- [ ] All layout numbers match the spec (44, 28/800, 15/#666, 7 equal cells, 52pt button, 16 margins).
- [ ] The week strip renders 7 equal-width cells with correct filled/empty dots from the `week` tuple.
- [ ] "This week: N of 7" is **derived during render**, never stored in state.
- [ ] Loading, not-found, and content states all render correctly (try a bad `habitId`).
- [ ] Every interactive element has `accessibilityRole` and a meaningful `accessibilityLabel`, and a touch target of at least 44pt.
- [ ] **No LogBox warnings** when the screen renders or when you check in.
- [ ] Committed under `challenges/challenge-01/` in your Week 7 repo with a short `README.md` showing a screenshot.

## Stretch

- Extract a `WeekStrip` component that takes `week: boolean[]` and renders the row, fully typed and reusable.
- Make the week strip respond to `useWindowDimensions()` so the cells stay square on a small phone and a tablet.
- Add a subtle press animation to the primary button using `Pressable`'s `style={({ pressed }) => ...}` (scale or opacity).
- Add an `accessibilityState={{ selected: doneToday }}` to the check-in button so screen readers announce the toggled state.

## Hints

<details>
<summary>Typing the 7-day tuple and deriving the count</summary>

```tsx
type Week = [boolean, boolean, boolean, boolean, boolean, boolean, boolean];

// derived during render — no state, no effect:
const completedThisWeek = habit.week.filter(Boolean).length; // 0..7
```

A tuple type pins the length to exactly 7, so a malformed mock won't compile.

</details>

<details>
<summary>The equal-width week strip</summary>

```tsx
const LETTERS = ["M", "T", "W", "T", "F", "S", "S"] as const;

<View style={styles.strip}>
  {habit.week.map((done, i) => (
    <View key={i} style={styles.cell}>
      <Text style={styles.cellLetter}>{LETTERS[i]}</Text>
      <Text style={[styles.dot, done ? styles.dotOn : styles.dotOff]}>
        {done ? "●" : "○"}
      </Text>
    </View>
  ))}
</View>;

// styles:
strip: { flexDirection: "row", marginHorizontal: 16, marginTop: 16 },
cell:  { flex: 1, alignItems: "center" },   // flex:1 → 7 equal columns
```

The index key is acceptable here because the week is a fixed-length, non-reordering list.

</details>

<details>
<summary>Loading and not-found states</summary>

```tsx
if (loading) {
  return (
    <SafeAreaView style={styles.center}><ActivityIndicator size="large" /></SafeAreaView>
  );
}
if (!habit) {
  return (
    <SafeAreaView style={styles.center}>
      <Text style={styles.muted}>Habit not found.</Text>
    </SafeAreaView>
  );
}
// ...content...
```

`habit` is `HabitDetail | undefined` after the lookup; the `if (!habit)` branch both renders the not-found state and narrows the type for the content branch below.

</details>

## Submission

Commit your screen under `challenges/challenge-01/` in your Week 7 GitHub repo. Include a `README.md` with a screenshot (simulator screenshot or a photo of your device) and one paragraph on how you mapped the spec's numbers to `StyleSheet` values. Make sure `npx tsc --noEmit` is clean on a fresh clone.

## Why this matters

"Build this screen to the spec" is the most common mobile ticket there is. Designers hand you numbers; you turn them into `StyleSheet` values and Flexbox. The discipline you practice here — **type the data exactly, build to the numbers, handle every state, make it accessible** — is what separates a screen that looks right in the demo from one that survives a real device, a screen reader, and a tablet. The reusable `WeekStrip` and the loading/not-found/content pattern carry directly into Week 8's navigated screens and Week 9's live-data screens.
