# Challenge 1 — A Flicker-Free, Leak-Free Auth-Gated Flow

**Time estimate:** ~100 minutes.

## Problem statement

Wire the session store (Exercise 3), the navigators (Exercise 1), and SecureStore into a complete authentication flow with **three** non-negotiable properties:

1. **Correct launch, every time.** On cold launch the app must resolve to the *right* navigator — login if there's no valid token, the tabs if there is — and it must do so *once*, with **no white flash and no flash of the wrong screen** in between.
2. **No back-button leak.** After logout, there must be **no way** to reach a protected screen — not by hardware back (Android), not by swipe-back (iOS), not by a stale deep link. The protected screens simply do not exist in the navigator when logged out.
3. **Resilience to the messy cases.** A corrupt or foreign token, a kill mid-splash, and a logout while a detail screen is open must all resolve cleanly — never a crash, never a stuck "loading" splash.

This is the single most-failed mobile interview exercise, and the failure is almost always property 1 or 2. The naive version "works" in the demo and breaks the instant someone relaunches or logs out.

## The shape you're building

```
App launch
  └─ status === 'loading'  → <Splash/>            (while bootstrapSession runs)
        ├─ no valid token  → status 'unauthenticated' → RootStack shows ONLY <Login/>
        └─ valid token     → status 'authenticated'   → RootStack shows ONLY <MainTabs/>

Login submit → useSessionStore.login(token, user) → status flips → RootStack swaps to <MainTabs/>
Logout       → useSessionStore.logout()           → status flips → RootStack swaps to <Login/>
                                                     (MainTabs and its whole back stack are GONE)
```

The key architectural decision — the one that makes property 2 free — is from Lecture 1 §11: **you conditionally render which screens exist inside one `RootStack.Navigator`, based on `status`.** You do not render two different `NavigationContainer`s. When `status` flips to `unauthenticated`, the `Main` screen (and its entire nested back stack) is removed from the navigator. There is nothing to back into.

## Required behavior

- A `<Splash />` component shows while `status === 'loading'`. It must look intentional (a logo or spinner centered), not a blank white screen.
- `bootstrapSession()` runs exactly once on mount (Exercise 3). The app shows the splash until it resolves.
- The login screen has a real-ish form: an email + password field (controlled, validated — reuse Exercise 2's patterns), a disabled-until-valid submit. On submit it calls `login(makeFakeToken(user), user)` with a mock user derived from the email. (Week 9 replaces this single call with the real `POST /api/v1/auth/login`.)
- The Profile tab has a **Log out** button that calls `logout()`.
- After logout, attempting hardware-back / swipe-back does **not** reveal the habits list. After login, hardware-back does **not** reveal the login screen.

## Acceptance criteria

- [ ] An Expo + TypeScript app with React Navigation and the session store wired together.
- [ ] `npx tsc --noEmit`: **0 errors, no `any`.**
- [ ] **The five adversarial checks all pass** (see "How it's assessed" below).
- [ ] The `status` union is `'loading' | 'authenticated' | 'unauthenticated'` — **not** a boolean. (A boolean cannot pass check 1.)
- [ ] The auth gate conditionally renders screens *inside one navigator*, not two `NavigationContainer`s.
- [ ] Logout clears the token from SecureStore (verify `getItemAsync` returns `null` after).
- [ ] A short `README.md` in the challenge folder with: the navigator tree as a diagram, a one-paragraph explanation of *why* the conditional-screens pattern prevents the back-button leak, and a GIF or screen recording of the five checks.
- [ ] Committed under `challenges/challenge-01/` in your Week 8 repo.

## How it's assessed — the five adversarial checks

Run these in order. All five must pass.

| # | Check | How to run it | Pass = |
|--:|-------|---------------|--------|
| 1 | **Cold launch, logged out** | Fresh install (or after a logout), kill the app, relaunch | Splash → Login, **no flash of the tabs** |
| 2 | **Cold launch, logged in** | Log in, kill the app, relaunch | Splash → Tabs, **no flash of Login**, no re-login required |
| 3 | **Logout-then-back** | Log in → open a habit detail → go to Profile → Log out → press hardware/ swipe back repeatedly | Stays on Login; **never** reveals habits or detail |
| 4 | **Kill during splash** | Throttle bootstrap (add a 1.5s delay), kill the app while the splash shows, relaunch | Resolves correctly to login or tabs; **never stuck on splash** |
| 5 | **Corrupted token relaunch** | Manually write a garbage value to the token key, relaunch | App clears it and lands on Login; **no crash** |

For checks 4 and 5 you'll want a way to manipulate SecureStore for testing — a hidden dev button that calls `SecureStore.setItemAsync(TOKEN_KEY, 'garbage')` is fine, just gate it behind `__DEV__`.

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Launch correctness (checks 1, 2, 4) | 35% | Always resolves to the right navigator, once, no flash, never stuck on splash |
| Leak prevention (check 3) | 30% | Zero paths back into a protected screen after logout; demonstrated on both platforms |
| Resilience (check 5) | 15% | Corrupt/foreign token handled gracefully; no crash, no infinite loading |
| Architecture | 10% | Conditional screens in one navigator; three-state status; clean `login`/`logout` seam |
| Type safety + README | 10% | No `any`; README explains the leak-prevention mechanism and shows the recording |

## Hints

<details>
<summary>The app shell</summary>

```tsx
// App.tsx
import { useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { RootStackParamList } from './src/navigation/types';
import { useSessionStore, selectStatus, bootstrapSession } from './src/store/useSessionStore';
import { Splash } from './src/screens/Splash';
import { LoginScreen } from './src/screens/LoginScreen';
import { MainTabs } from './src/navigation/MainTabs';

const Root = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  const status = useSessionStore(selectStatus);

  useEffect(() => {
    void bootstrapSession();
  }, []);

  if (status === 'loading') return <Splash />;

  return (
    <NavigationContainer>
      <Root.Navigator screenOptions={{ headerShown: false }}>
        {status === 'authenticated' ? (
          <Root.Screen name="Main" component={MainTabs} />
        ) : (
          <Root.Screen name="Login" component={LoginScreen} />
        )}
      </Root.Navigator>
    </NavigationContainer>
  );
}
```

Notice the splash renders *outside* `NavigationContainer`. That's fine and deliberate — there's nothing to navigate yet.

</details>

<details>
<summary>Why this prevents the back-button leak (put this in your README)</summary>

When `status` is `unauthenticated`, the `Main` screen is **not a child of the navigator** — it doesn't exist in the navigation state at all. There is no entry in any back stack pointing at it. So hardware-back, swipe-back, and `goBack()` have nothing to go back *to*. The leak is impossible by construction, not by guarding each screen. Contrast the naive approach (always render both screens, redirect in a `useEffect` if not authed): there, the protected screen briefly mounts before the redirect fires — that's the flash *and* the leak.

</details>

<details>
<summary>Forcing the splash race (check 4)</summary>

In `bootstrapSession`, temporarily add `await new Promise((r) => setTimeout(r, 1500));` before reading SecureStore. That widens the loading window so you can kill the app mid-splash and confirm it still resolves on relaunch. Remove the delay before you commit (or gate it behind `__DEV__`).

</details>

## Stretch

- Add a **token-expiry** check: store an `expiresAt` alongside the token; on bootstrap, treat an expired token as no token (clear it, land on login). This is the on-ramp to Week 9's refresh flow.
- Add a **deep-link guard**: a deep link to `crunchtracker://habits/42` while logged out should route to login, and resume to Habit #42 after a successful login. (Capture the pending link, replay it post-login.)
- Add an **inactivity logout**: if the app has been backgrounded for >15 minutes, require re-login on resume. (`AppState` + a timestamp.)

## Why this matters

The conditional-screens auth gate isn't a Crunch-Tracker quirk — it's *the* canonical React Navigation auth pattern, documented in their auth-flow guide (<https://reactnavigation.org/docs/auth-flow>). Every production Expo app you'll work on has some version of it. And the `login`/`logout`/`bootstrapSession` seam you build here is exactly where Week 9 injects the real backend: `login` will call your Spring `POST /api/v1/auth/login`, `bootstrapSession` will validate the token against `GET /api/v1/auth/me`, and nothing else in the navigation layer changes. Get it right once; reuse it for the rest of the course.

## Submission

Commit your work under `challenges/challenge-01/` in your Week 8 GitHub repo. Make sure `npx tsc --noEmit` is clean on a fresh clone, and include the screen recording of the five checks in the README — that recording *is* the proof the challenge passed.
