# Lecture 2 — Managing State on Purpose: Context, Zustand, and Why Server State Is Different

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can name the four kinds of state in a mobile app and pick the right tool for each; lift shared state into React Context or a Zustand store without causing needless re-renders; persist a JWT to `expo-secure-store` and rehydrate it on launch; and articulate why server state needs a different tool (TanStack Query, Week 9) than client state.

If you only remember one thing from this lecture, remember this:

> **Not all state is the same, and reaching for one tool to hold all of it is the most expensive mistake in a React app.** Which screen you're on, whether you're logged in, a form's draft values, and what the server says your habits are — those are four different problems. Put them in the same `useState` blob or the same Context and you get re-render storms, stale data, and impossible-to-debug flicker. Sort them first; then the tools are obvious.

---

## 1. The four kinds of state

Before you choose a library, classify the state. Almost every value in an app falls into one of four buckets:

| Kind | Examples | Who owns the truth | Right tool (this course) |
|------|----------|--------------------|--------------------------|
| **Navigation state** | current screen, back stack, route params | React Navigation | React Navigation (Lecture 1) |
| **Ephemeral / local** | a toggle's open/closed, a single input's value, a "is this row expanded" flag | the component | `useState` / `useReducer` |
| **Shared client state** | is-logged-in, the JWT, the current user, theme, a global toast | the app | **Zustand** (high-frequency) or **Context** (low-frequency) |
| **Server state** | the user's habits, goals, check-ins | the **server** | **TanStack Query** (Week 9) |

The discipline is: **classify, then choose.** If you ask "where should the habit list live?" the answer isn't "in a store" — it's "habits are *server state*, so they live in TanStack Query's cache next week; this week they're mock data in a store *standing in for* server state." That distinction is the whole reason Week 9 is a wiring exercise and not a rewrite.

Why server state is genuinely different, in one breath: server state is **shared with other clients**, can be **stale the instant you read it**, needs **caching, deduplication, background refetch, and retry**, and is **owned by something you don't control**. Client state has none of those properties — you are the single source of truth and it's never stale because nothing else can change it. Treating server state like client state (fetch into `useState`, never revalidate) is how you ship an app that shows yesterday's data. We'll handle it properly in Week 9; this week we just keep the seam clean.

---

## 2. Ephemeral state: `useState`, and *not* over-lifting

The default for any value is `useState` in the component that uses it. Don't lift state until you have a reason. The reason is: **two or more sibling components need the same value, or a value must survive the component unmounting.**

A common beginner anti-pattern is lifting everything to the top of the app "to be safe." That makes the root component re-render on every keystroke in any form. Keep ephemeral state local:

```tsx
function SearchBar({ onSubmit }: { onSubmit: (q: string) => void }) {
  const [query, setQuery] = useState(''); // local — nobody else needs it mid-typing
  return (
    <TextInput
      value={query}
      onChangeText={setQuery}
      onSubmitEditing={() => onSubmit(query)}
    />
  );
}
```

Only the *result* (the submitted query) needs to travel upward, and you do that with a callback prop, not by hoisting `query` itself.

---

## 3. React Context: the right tool for low-frequency shared state

Context solves "prop drilling": passing a value through ten layers of components that don't use it just to reach the one that does. You create a context, provide a value at the top, and any descendant reads it with `useContext`.

```tsx
// src/theme/ThemeContext.tsx
import { createContext, useContext, useState, type ReactNode } from 'react';

type Theme = 'light' | 'dark';
type ThemeContextValue = { theme: Theme; toggle: () => void };

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>('light');
  const toggle = () => setTheme((t) => (t === 'light' ? 'dark' : 'light'));
  return (
    <ThemeContext.Provider value={{ theme, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside <ThemeProvider>');
  return ctx;
}
```

The `if (!ctx) throw` pattern is non-negotiable: it turns "you forgot the Provider" from a silent `null` into a loud, immediate error.

### The Context re-render footgun

Here is why Context is the *low-frequency* tool. **Every component that calls `useContext` re-renders whenever the context value changes — even if it only uses a slice that didn't change.** If you put your theme *and* your current user *and* a counter that ticks every second all in one context, every `useContext` consumer re-renders every second.

The fixes are: (a) split unrelated concerns into separate contexts, (b) memoize the value object so it isn't a new reference every render, and (c) for genuinely high-frequency state, **don't use Context at all** — use Zustand, which lets each component subscribe to exactly the slice it needs. That's the next section.

> **Rule of thumb:** Context for things that change rarely and that most of the tree reads — theme, locale, "who is the current user." A store (Zustand) for things that change often or that only a few components read — UI flags, the session token, a draft, a toast queue.

---

## 4. Zustand: a 1-KB store that fixes the re-render problem

Zustand is a tiny state-management library. You `create` a store — a plain object with state and the functions that change it — and components subscribe to it with **selectors**. The selector is the magic: a component re-renders *only* when the slice it selected changes.

### A minimal store

```ts
// src/store/useUiStore.ts
import { create } from 'zustand';

type UiState = {
  isAddHabitOpen: boolean;
  openAddHabit: () => void;
  closeAddHabit: () => void;
};

export const useUiStore = create<UiState>((set) => ({
  isAddHabitOpen: false,
  openAddHabit: () => set({ isAddHabitOpen: true }),
  closeAddHabit: () => set({ isAddHabitOpen: false }),
}));
```

### Subscribing with a selector

```tsx
function AddHabitButton() {
  // subscribes to ONLY the action; this component never re-renders on state change
  const openAddHabit = useUiStore((s) => s.openAddHabit);
  return <Button title="Add habit" onPress={openAddHabit} />;
}

function AddHabitModal() {
  // re-renders only when isAddHabitOpen flips
  const isOpen = useUiStore((s) => s.isAddHabitOpen);
  return isOpen ? <Modal>{/* ... */}</Modal> : null;
}
```

Compare to Context: with Context, both components would re-render whenever *anything* in the context value changed. With Zustand selectors, each component re-renders only for its own slice. That's the entire reason Zustand is the default for high-frequency client state in 2026.

### The TypeScript gotcha: the curried `create`

The single most common Zustand-in-TypeScript mistake. To get inference to work you must use the **curried** form `create<T>()(...)` — note the extra `()`:

```ts
// CORRECT — curried, lets TS infer the `set`/`get` types
export const useUiStore = create<UiState>()((set, get) => ({ /* ... */ }));

// WRONG-ish — works for simple stores, breaks with middleware
export const useUiStore = create<UiState>((set) => ({ /* ... */ }));
```

For a store with no middleware, the non-curried form compiles. The instant you add `persist` (next section), you **must** switch to the curried form or the types collapse. The Zustand TS guide documents exactly why: <https://zustand.docs.pmnd.rs/guides/typescript>. C3 standard: always write the curried form, even on simple stores, so adding middleware later is a one-line change.

### Selecting multiple values without a re-render storm

If you need several values, don't return a fresh object from the selector every render (that defeats the equality check). Either select them individually:

```ts
const token = useSessionStore((s) => s.token);
const user = useSessionStore((s) => s.user);
```

...or use `useShallow` for an object selection:

```ts
import { useShallow } from 'zustand/react/shallow';
const { token, user } = useSessionStore(useShallow((s) => ({ token: s.token, user: s.user })));
```

`useShallow` does a shallow comparison so the component re-renders only when one of those two fields actually changes.

---

## 5. The session store — the heart of this week

Crunch Tracker's auth gate (Lecture 1) needs to know: are we logged in, who is the user, and what's the token? That's shared client state that changes rarely but is read in many places. It's a textbook Zustand store.

```ts
// src/store/useSessionStore.ts
import { create } from 'zustand';

export type User = { id: number; email: string; displayName: string };

type SessionState = {
  status: 'loading' | 'authenticated' | 'unauthenticated';
  token: string | null;
  user: User | null;
  login: (token: string, user: User) => void;
  logout: () => void;
};

export const useSessionStore = create<SessionState>()((set) => ({
  status: 'loading',          // start "loading" until we check SecureStore on launch
  token: null,
  user: null,
  login: (token, user) => set({ status: 'authenticated', token, user }),
  logout: () => set({ status: 'unauthenticated', token: null, user: null }),
}));
```

Three design choices worth calling out:

- **`status` is a three-state union, not a boolean.** `'loading' | 'authenticated' | 'unauthenticated'`. The `'loading'` state exists so that on launch — while we're still reading the token from SecureStore — we show a splash, not a flash of the login screen. The challenge is entirely about getting this right; a boolean `isAuthenticated` cannot express "we don't know yet," and that's where flicker comes from.
- **`login(token, user)` is the seam for Week 9.** Today the login screen calls it with a fake token. Next week the login screen calls the real `POST /api/v1/auth/login`, gets back a real JWT and user, and calls this exact same action. The store doesn't change; only what feeds it does.
- **No habits in this store.** Habits are server state. They do not belong here. (This week they live in a separate mock-data store *clearly labeled* as a server-state stand-in; next week that store is deleted and TanStack Query takes over.)

The auth gate from Lecture 1 now reads the store:

```tsx
function App() {
  const status = useSessionStore((s) => s.status);
  if (status === 'loading') return <SplashScreen />;
  return (
    <NavigationContainer>
      <RootStack.Navigator screenOptions={{ headerShown: false }}>
        {status === 'authenticated'
          ? <RootStack.Screen name="Main" component={MainTabs} />
          : <RootStack.Screen name="Login" component={LoginScreen} />}
      </RootStack.Navigator>
    </NavigationContainer>
  );
}
```

---

## 6. Persisting the token: `expo-secure-store`

A JWT must survive an app restart — otherwise the user logs in every launch. But you must **never** put a token in plain `AsyncStorage`: that's unencrypted on-disk storage readable by anything with filesystem access on a rooted/jailbroken device. The token goes in **SecureStore**.

```bash
npx expo install expo-secure-store
```

`expo-secure-store` is encrypted, OS-backed storage:

- **iOS:** the Keychain.
- **Android:** `EncryptedSharedPreferences`, with the key in the Android Keystore.

Its API is three async functions:

```ts
import * as SecureStore from 'expo-secure-store';

await SecureStore.setItemAsync('crunch.token', token);
const token = await SecureStore.getItemAsync('crunch.token'); // string | null
await SecureStore.deleteItemAsync('crunch.token');
```

> **What "secure" does and doesn't mean.** SecureStore protects the token *at rest* against other apps and casual filesystem inspection. It does **not** make your token un-stealable from a fully compromised device, and it does **not** replace HTTPS in transit. It's the right place for a JWT; it's not a vault for secrets that should never be on the device at all (those don't belong on the client period). Values are also size-limited (~2 KB on iOS) — fine for a JWT, not for blobs.

### Wiring SecureStore into the session lifecycle

Two integration points. First, **rehydrate on launch** — read the token before deciding which navigator to show:

```ts
// src/store/sessionBootstrap.ts
import * as SecureStore from 'expo-secure-store';
import { useSessionStore } from './useSessionStore';
import { decodeUserFromToken } from '../auth/token'; // your helper (mock this week)

export async function bootstrapSession(): Promise<void> {
  const token = await SecureStore.getItemAsync('crunch.token');
  if (token) {
    const user = decodeUserFromToken(token); // Week 9: validate/refresh against the API
    useSessionStore.getState().login(token, user);
  } else {
    useSessionStore.setState({ status: 'unauthenticated' });
  }
}
```

Call it once at startup:

```tsx
useEffect(() => {
  void bootstrapSession();
}, []);
```

Second, **write on login and delete on logout.** You can do this inside the store actions so callers never forget:

```ts
login: async (token, user) => {
  await SecureStore.setItemAsync('crunch.token', token);
  set({ status: 'authenticated', token, user });
},
logout: async () => {
  await SecureStore.deleteItemAsync('crunch.token');
  set({ status: 'unauthenticated', token: null, user: null });
},
```

### The `persist` middleware alternative

Zustand ships a `persist` middleware that automates this. You give it a storage adapter and it saves/restores chosen slices for you:

```ts
import { persist, createJSONStorage } from 'zustand/middleware';
import * as SecureStore from 'expo-secure-store';

const secureStorage = {
  getItem: (name: string) => SecureStore.getItemAsync(name),
  setItem: (name: string, value: string) => SecureStore.setItemAsync(name, value),
  removeItem: (name: string) => SecureStore.deleteItemAsync(name),
};

export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({ /* ...state and actions... */ }),
    {
      name: 'crunch.session',
      storage: createJSONStorage(() => secureStorage),
      partialize: (s) => ({ token: s.token, user: s.user }), // never persist `status`
      onRehydrateStorage: () => (state) => {
        // after rehydrate, resolve the loading state
        useSessionStore.setState({ status: state?.token ? 'authenticated' : 'unauthenticated' });
      },
    },
  ),
);
```

`partialize` is important: you persist `token` and `user`, but **never** the transient `status` — that must be recomputed on each launch. Exercise 3 builds exactly this store. (Note `persist` requires the curried `create<T>()(...)` form from Section 4 — this is where it bites if you wrote the non-curried version.)

---

## 7. Controlled forms and validation (the form half of the week)

Crunch Tracker needs an add-habit form: a name, a frequency, an optional target. On mobile, you build it with **controlled inputs** — each `TextInput` has a `value` and an `onChangeText`, and the state lives in the component (or a store).

```tsx
const [name, setName] = useState('');
<TextInput value={name} onChangeText={setName} placeholder="Habit name" />;
```

"Controlled" means React is the source of truth for the input's value — you can validate it, transform it, and reset it programmatically. Uncontrolled inputs (let the native field hold its own value) fight you the moment you need validation, which on a real form is immediately.

### Validation without a library

For a three-field form, you don't need a library. Compute errors from the current values:

```ts
type Errors = Partial<Record<'name' | 'target', string>>;

function validate(values: { name: string; target: string }): Errors {
  const errors: Errors = {};
  if (values.name.trim().length === 0) errors.name = 'Name is required.';
  else if (values.name.trim().length > 60) errors.name = 'Keep it under 60 characters.';
  if (values.target.length > 0 && Number.isNaN(Number(values.target)))
    errors.target = 'Target must be a number.';
  return errors;
}
```

Then: disable submit when `Object.keys(errors).length > 0`, show each error under its field, and only show an error after the user has *touched* that field (tracking a `touched` set) so you're not yelling at an empty pristine form.

### When to reach for a library

Once a form has 5+ fields, cross-field rules, async validation, or you want to share the schema with your API layer, hand-rolling becomes a liability. The 2026 default is **`react-hook-form` + `zod`**: `react-hook-form` minimizes re-renders (it uses uncontrolled refs under the hood and only re-renders on submit/error), and `zod` gives you a schema you can reuse — the *same* schema can validate the form here and (Week 9) parse the API response. Exercise 2 hand-rolls a form so you understand what the library does for you; the mini-project lets you choose.

### Keyboard handling

The single most common mobile-form bug: the keyboard covers the input you're typing in. Wrap the form in `KeyboardAvoidingView` and, for scrollable forms, a `ScrollView` with `keyboardShouldPersistTaps="handled"`:

```tsx
<KeyboardAvoidingView
  behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
  style={{ flex: 1 }}
>
  <ScrollView keyboardShouldPersistTaps="handled">{/* fields */}</ScrollView>
</KeyboardAvoidingView>
```

`behavior` differs by platform because iOS and Android handle the keyboard inset differently. This is the kind of detail that's invisible until a user with a small phone can't see the submit button.

---

## 8. Where each Crunch Tracker value lives — the cheat sheet

Tape this to your monitor for the mini-project:

| Value | Kind | Home |
|-------|------|------|
| Which tab is selected | navigation | React Navigation |
| Habit-list scroll position | ephemeral | the screen's local state |
| Add-habit form draft | ephemeral (or UI store if it must survive nav) | `useState` / `useUiStore` |
| `isAddHabitModalOpen` | shared client (UI) | `useUiStore` (Zustand) |
| `status` / `token` / `user` | shared client (session) | `useSessionStore` (Zustand) + SecureStore |
| Theme / locale | shared client (low-freq) | Context |
| The user's habits & goals | **server** | mock store this week → **TanStack Query** in Week 9 |

If you can fill in this table for any value before you write a line of code, you've already done the hard part of state management. The libraries are easy once the classification is right.

---

## 9. A note on `useEffect` and "you might not need it"

A recurring junior mistake: fetching server data into `useState` inside a `useEffect`, then trying to keep it in sync by hand. React's own docs (<https://react.dev/learn/you-might-not-need-an-effect>) push hard against this, and it's the conceptual on-ramp to Week 9. The short version: an Effect is for **synchronizing with an external system** (a subscription, a native event, a timer). Fetching server state *looks* like that but has so many extra requirements — caching, dedup, retry, revalidation, race-condition handling — that you want a dedicated tool (TanStack Query) rather than a hand-rolled Effect. This week, because we're on mock data, you won't write those fetch Effects at all — and that's deliberate. We keep server state out so that when it arrives next week it arrives in the *right* tool.

---

## 10. Recap

You should now be able to:

- Name the four kinds of state and classify any value into one of them.
- Keep ephemeral state local and explain why over-lifting causes re-render storms.
- Use React Context for low-frequency shared state and explain its re-render footgun.
- Build a Zustand store, subscribe with selectors, use the curried `create<T>()(...)` form, and avoid the multi-select re-render trap with `useShallow`.
- Build the session store with a three-state `status` union and explain why the boolean version causes flicker.
- Persist a JWT to SecureStore and rehydrate it on launch, by hand or with the `persist` middleware.
- Build a controlled, validated form with keyboard handling, and know when to reach for `react-hook-form` + `zod`.
- Articulate *why* server state is different and why we deliberately keep it out until Week 9.

Next, do the exercises — wire the navigators, build the form, and stand up the session store.

---

## References

- *Zustand — Introduction*: <https://zustand.docs.pmnd.rs/getting-started/introduction>
- *Zustand — TypeScript guide*: <https://zustand.docs.pmnd.rs/guides/typescript>
- *Zustand — persist middleware*: <https://zustand.docs.pmnd.rs/integrations/persisting-store-data>
- *React — Passing data deeply with Context*: <https://react.dev/learn/passing-data-deeply-with-context>
- *React — You Might Not Need an Effect*: <https://react.dev/learn/you-might-not-need-an-effect>
- *Expo SecureStore*: <https://docs.expo.dev/versions/latest/sdk/securestore/>
- *React Native — KeyboardAvoidingView*: <https://reactnative.dev/docs/keyboardavoidingview>
- *react-hook-form*: <https://react-hook-form.com/get-started> · *Zod*: <https://zod.dev/>
