# Week 8 Homework

Six practice problems that revisit the week's topics. The full set should take about **5 hours**. Work in your Week 8 Crunch Tracker repo so each problem produces at least one commit you can point to later.

Each problem includes a **problem statement**, **acceptance criteria**, a **hint**, and an **estimated time**. A grading rubric is at the bottom.

Keep the bar from the week: `npx tsc --noEmit` clean, **zero `any`**.

---

## Problem 1 — Type a loosely-typed screen

**Problem statement.** You're handed a screen that compiles but reads its params untyped:

```tsx
export function GoalDetailScreen({ route }: any) {
  const goalId = route.params.goalId; // untyped
  return <Text>Goal #{goalId}</Text>;
}
```

Rewrite it properly: add a `GoalsStackParamList` with `GoalDetail: { goalId: number }`, type the props with `NativeStackScreenProps<GoalsStackParamList, 'GoalDetail'>`, and remove every `any`. Then add a typed `navigate('GoalDetail', { goalId })` call in the goal list that the compiler validates.

**Acceptance criteria.**

- No `any` anywhere in the file or the navigate call.
- `route.params.goalId` is typed as `number` (hover to confirm).
- A deliberate wrong call — `navigate('GoalDetail', { goalId: '3' })` — is a compile error.
- `npx tsc --noEmit` clean. Committed.

**Hint.** Mirror Exercise 1's `HabitsStackParamList`/`HabitDetailScreen` exactly; only the names change.

**Estimated time.** 30 minutes.

---

## Problem 2 — A `useReducer` form vs the controlled-state form

**Problem statement.** You built the add-habit form with `useState` per field (Exercise 2). Rebuild *just the state management* of a three-field form with `useReducer` instead: one `formReducer(state, action)` handling `setField`, `touch`, and `reset` actions, with a discriminated-union `Action` type. The UI can stay the same; only the state mechanism changes.

Then write 2–3 sentences in `notes/usestate-vs-usereducer.md`: when is `useReducer` worth the extra ceremony over `useState`?

**Acceptance criteria.**

- A `formReducer` with a discriminated-union `Action` type — no `any`, exhaustive `switch`.
- The form behaves identically to the `useState` version (validation, touched, reset).
- The note exists and answers the question concretely.
- Committed.

**Hint.** `type Action = { type: 'setField'; field: K; value: string } | { type: 'touch'; field: K } | { type: 'reset' };`. The reducer's `switch` on `action.type` narrows the union so each branch is typed.

**Estimated time.** 1 hour.

---

## Problem 3 — Selectors and re-render counting

**Problem statement.** Add a tiny render counter to two components that read your session store: one that reads the whole store (`const s = useSessionStore()`) and one that reads a selector (`useSessionStore(selectUser)`). Trigger an unrelated store update (e.g. toggle a UI flag in the same store, or call `setState` on an unrelated field) and observe which component re-renders.

Write your finding in `notes/selectors.md` (2–3 sentences) and then fix the wasteful component to use a selector.

**Acceptance criteria.**

- A demonstrable difference: the whole-store reader re-renders on unrelated updates; the selector reader does not.
- The note records the observed counts.
- The wasteful component is fixed to use a selector.
- Committed.

**Hint.** A simple counter: `const renders = useRef(0); renders.current++;` and render `renders.current`. Or use React DevTools' "Highlight updates when components render."

**Estimated time.** 45 minutes.

---

## Problem 4 — Persist with the `persist` middleware

**Problem statement.** Refactor your session store to use Zustand's `persist` middleware with a SecureStore adapter, replacing the hand-rolled `SecureStore.setItemAsync`/`deleteItemAsync` calls inside the actions. Use `partialize` to persist only `token` and `user` (never `status`), and `onRehydrateStorage` to resolve `status` after rehydration.

**Acceptance criteria.**

- The store uses `persist(createJSONStorage(() => secureStorage), { name, partialize, onRehydrateStorage })`.
- `status` is **not** persisted; it's recomputed on launch.
- The five adversarial checks from the challenge still pass.
- You used the curried `create<T>()(...)` form (required for middleware).
- `npx tsc --noEmit` clean. Committed.

**Hint.** The SecureStore adapter is three methods — `getItem`, `setItem`, `removeItem` — each delegating to the matching `SecureStore.*Async`. Lecture 2 §6 has the skeleton.

**Estimated time.** 1 hour.

---

## Problem 5 — A working deep link with a parsed param

**Problem statement.** Configure deep linking so `crunchtracker://habits/:habitId` opens the matching Habit detail, and add a `parse` to the linking config so `habitId` arrives as a **number**, not a string. Test it from the terminal and capture the command + result.

**Acceptance criteria.**

- `app.json` has `"scheme": "crunchtracker"`.
- The `linking.config` mirrors your navigator nesting (root → Main → Habits → HabitDetail).
- `habitId` is a `number` in `route.params` (a `parse: { habitId: Number }` on that route).
- `npx uri-scheme open crunchtracker://habits/2 --ios` (or `--android`) lands on Habit #2; paste the command and a screenshot into `notes/deep-link.md`.
- Committed.

**Hint.** The linking `config.screens` tree must match your navigator tree level for level — that's the #1 deep-link bug. Per-route parse: `HabitDetail: { path: 'habits/:habitId', parse: { habitId: Number } }`.

**Estimated time.** 45 minutes.

---

## Problem 6 — The state-map essay

**Problem statement.** Write a 300–400 word note at `notes/week-08-state-map.md` answering:

1. List every significant piece of state in your Crunch Tracker app and classify each as navigation / ephemeral / shared-client / server.
2. For the one piece you labeled "server state," explain why it's server state and what specifically changes about it in Week 9.
3. Did you catch yourself putting any state in the wrong place this week? What was it, and how did you know it was wrong?
4. In one sentence: why does keeping these four kinds separate make Week 9 a wiring job instead of a rewrite?

**Acceptance criteria.**

- File exists, 300–400 words, each numbered question in its own paragraph.
- The classification in (1) is accurate for *your* app (a table is fine).
- Committed.

**Hint.** This is the same state map your mini-project README needs — do this first and reuse it. Future-you reading it at the start of Week 9 will be grateful.

**Estimated time.** 30 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 30 min |
| 2 | 1 h 0 min |
| 3 | 45 min |
| 4 | 1 h 0 min |
| 5 | 45 min |
| 6 | 30 min |
| **Total** | **~4 h 30 min** |

---

## Grading rubric

Each problem is worth points toward 100. "Complete" means the acceptance criteria are met *and* `npx tsc --noEmit` is clean with no `any`.

| Problem | Points | Full credit requires |
|--------:|-------:|----------------------|
| 1 — Type the screen | 15 | Typed props + params; wrong call is a compile error; no `any` |
| 2 — `useReducer` form | 20 | Discriminated-union actions; exhaustive switch; identical behavior; the note answers the question |
| 3 — Selectors | 15 | Demonstrated re-render difference; wasteful component fixed; note records counts |
| 4 — `persist` middleware | 20 | `persist` + SecureStore adapter; `status` not persisted; five checks still pass; curried form |
| 5 — Deep link | 15 | Scheme set; config mirrors nesting; `habitId` parsed to number; terminal test captured |
| 6 — State-map essay | 15 | Accurate four-way classification; specific Week-9 change identified |

**Scoring bands.** 90–100 distinction · 75–89 strong pass · 60–74 pass · under 60 revise and resubmit.

**Automatic deductions** (apply across the whole submission):

- Any `any` in checked-in code: **−10**.
- `npx tsc --noEmit` fails on a fresh clone: **−15**.
- A piece of state classified into the wrong bucket in Problem 6: **−5** each.

When you've finished all six, push your repo and open the [mini-project](./07-mini-project/00-overview.md) if you haven't already.
