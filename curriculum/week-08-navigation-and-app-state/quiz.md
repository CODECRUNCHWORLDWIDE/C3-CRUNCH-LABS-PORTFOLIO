# Week 8 — Quiz

Ten questions. Take it with your lecture notes closed. Aim for 9/10 before moving to Week 9. Answer key at the bottom — don't peek.

---

**Q1.** What is a `ParamList` in React Navigation's TypeScript setup?

- A) A runtime array of the params currently on the navigation stack.
- B) A TypeScript type mapping each route name to the params that route accepts.
- C) A component that renders the list of available routes.
- D) A config object passed to `NavigationContainer` to enable deep links.

---

**Q2.** You want a list → detail flow *inside* the Habits tab, with a back button, while the bottom tab bar stays visible. What structure gives you that?

- A) Put both `HabitList` and `HabitDetail` as sibling tabs.
- B) Make the Habits tab's `component` a `createNativeStackNavigator` containing `HabitList` and `HabitDetail`.
- C) Conditionally render `HabitDetail` with `useState` inside `HabitList`.
- D) Use `navigation.replace` to swap `HabitList` for `HabitDetail`.

---

**Q3.** Lecture 1 says to pass `navigation.navigate('HabitDetail', { habitId: 42 })` rather than passing the whole habit object. Why?

- A) Params can't hold objects at all — only primitives.
- B) Passing the id keeps params small/serializable and lets the detail screen read the *current* habit by id, avoiding a stale snapshot.
- C) Objects in params are slower to render.
- D) It's purely a style preference with no functional difference.

---

**Q4.** Which statement about React Context's performance is correct?

- A) Only components that read the changed field re-render.
- B) Context never causes re-renders; it's a pure read.
- C) Every component calling `useContext` re-renders whenever the context *value* changes, even if it only uses an unchanged slice.
- D) Context re-renders the entire app on every change, including non-consumers.

---

**Q5.** In Zustand, what does subscribing with a *selector* — `useStore(s => s.token)` — buy you over reading the whole store?

- A) Nothing; it's the same as `useStore()`.
- B) The component re-renders only when the selected slice (`token`) changes, not on every store update.
- C) It makes the store globally mutable.
- D) It persists `token` to disk automatically.

---

**Q6.** Why does the session store use `status: 'loading' | 'authenticated' | 'unauthenticated'` instead of a boolean `isAuthenticated`?

- A) Booleans aren't allowed in Zustand.
- B) The third state expresses "we don't know yet" (while reading SecureStore on launch), which is what lets the app show a splash instead of flashing the login screen.
- C) Three states render faster than two.
- D) It's required by `expo-secure-store`.

---

**Q7.** Where should a JWT be stored on the device, and why?

- A) In `AsyncStorage`, because it's the fastest key-value store.
- B) In a Zustand store only, because in-memory is most secure.
- C) In `expo-secure-store`, because it's encrypted and OS-backed (iOS Keychain / Android Keystore), unlike plain `AsyncStorage`.
- D) In a global variable, so it survives reloads.

---

**Q8.** What makes the conditional-screens auth gate (rendering only `Login` *or* only `Main` inside one navigator) leak-proof against the back button after logout?

- A) A `useEffect` redirect fires fast enough that the user can't see the protected screen.
- B) The protected screen is removed from the navigator entirely, so there's no back-stack entry pointing at it — there is nothing to go back to.
- C) The hardware back button is disabled while logged out.
- D) `navigation.reset()` is called on every render.

---

**Q9.** You add the `persist` middleware to your Zustand store and the types collapse. What's the most likely cause?

- A) `persist` doesn't support TypeScript.
- B) You wrote `create<T>((set) => ...)` instead of the curried `create<T>()((set) => ...)`.
- C) You forgot to install `expo-secure-store`.
- D) You used a selector instead of reading the whole store.

---

**Q10.** Which value is **server state** (and therefore belongs in TanStack Query next week, not in a client store)?

- A) Whether the add-habit modal is currently open.
- B) The user's list of habits, owned by the backend and shared across the user's devices.
- C) The current value of the name field in the add-habit form.
- D) Which bottom tab is selected.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — A `ParamList` is a compile-time type mapping route names to their param shapes. It's the spine of typed navigation; nothing about it exists at runtime.
2. **B** — A stack navigator *inside* the tab's `component` gives push/pop with a back button while the tab bar persists. Siblings (A) have no back relationship; conditional `useState` (C) gives you no back stack or native transition; `replace` (D) swaps in place.
3. **B** — Passing the id keeps params serializable (they go into navigation state and deep-link URLs) and lets the detail screen read the *live* record from the store, so it doesn't show a stale snapshot if the habit changes. (Objects technically *can* go in params, but you shouldn't — so A is wrong.)
4. **C** — Every `useContext` consumer re-renders when the context value reference changes, regardless of which slice it actually reads. This is the footgun that pushes high-frequency state to Zustand.
5. **B** — Selectors are Zustand's whole performance story: a component subscribes to a slice and re-renders only when that slice changes, avoiding the Context-style re-render storm.
6. **B** — The `'loading'` state represents "still checking SecureStore." It lets the app render an intentional splash until auth resolves, instead of flashing the login screen for a frame (the classic flicker bug). A boolean can't express the unknown state.
7. **C** — `expo-secure-store` is encrypted, OS-backed storage (Keychain / Keystore). `AsyncStorage` is plaintext on disk — never put a token there. In-memory only (B) means re-login every launch.
8. **B** — Conditionally rendering screens removes the protected screen from the navigator; with no back-stack entry, there's nothing to back into. The `useEffect`-redirect approach (A) still mounts the protected screen briefly — that's both the flash and the leak.
9. **B** — Middleware like `persist` requires the curried `create<T>()(...)` form for TypeScript inference to work. The non-curried form compiles for plain stores but breaks under middleware. (Lecture 2 §4, §6.)
10. **B** — The habit list is owned by the server, shared across the user's devices, and can be stale the instant you read it — the defining traits of server state. The other three are client/UI/ephemeral state the app fully owns.

</details>

---

If you scored under 7, re-read the lectures for the questions you missed — especially the "four kinds of state" table and the auth-gate mechanism. If you scored 9 or 10, you're ready for the [homework](./homework.md).
