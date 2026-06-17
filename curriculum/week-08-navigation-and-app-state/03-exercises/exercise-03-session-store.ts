// Exercise 3 — A persisted Zustand session store
//
// Goal: Implement the session store that drives the auth gate. It tracks a
//       three-state `status` ('loading' | 'authenticated' | 'unauthenticated'),
//       holds the JWT + user, persists the token to expo-secure-store, and
//       rehydrates on launch. This is the exact store the mini-project and the
//       challenge build on, and the seam Week 9 plugs the real API into.
//
// Estimated time: 40 minutes.
//
// HOW TO USE THIS FILE
//
//   1. In your Expo app:
//        npm install zustand
//        npx expo install expo-secure-store
//   2. Drop this in as `src/store/useSessionStore.ts`.
//   3. Fill in the bodies marked `// TODO`. Do NOT change the exported types or
//      the action signatures — the auth gate and Week 9 depend on them.
//   4. Run `npx tsc --noEmit` — zero errors, no `any`.
//   5. Verify with the behavior notes near the bottom.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `npx tsc --noEmit`: 0 errors, no `any`.
//   [ ] Store starts in status: 'loading'.
//   [ ] bootstrapSession() reads SecureStore and resolves to authenticated /
//       unauthenticated exactly once.
//   [ ] login() writes the token to SecureStore and sets status 'authenticated'.
//   [ ] logout() deletes the token from SecureStore and sets 'unauthenticated'.
//   [ ] You used the CURRIED create<T>()(...) form (required for middleware later).
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';

// ----------------------------------------------------------------------------
// Types  (do not change — the auth gate and Week 9 client depend on these)
// ----------------------------------------------------------------------------

export type User = {
  id: number;
  email: string;
  displayName: string;
};

export type SessionStatus = 'loading' | 'authenticated' | 'unauthenticated';

export type SessionState = {
  status: SessionStatus;
  token: string | null;
  user: User | null;

  /** Persist the token and mark the session authenticated. */
  login: (token: string, user: User) => Promise<void>;
  /** Clear the token and mark the session unauthenticated. */
  logout: () => Promise<void>;
};

// The SecureStore key. Namespaced so it never collides with other stored values.
export const TOKEN_KEY = 'crunch.session.token';

// ----------------------------------------------------------------------------
// Token decoding (mock this week; Week 9 validates against the API)
// ----------------------------------------------------------------------------
//
// A real JWT is `header.payload.signature`, each part base64url-encoded. This
// week we don't have the backend wired, so we mock the decode: in dev we issue
// fake tokens of the form `fake.<base64url(JSON user)>.sig` and read the user
// straight back out. In Week 9 you replace this with a real decode + a call to
// GET /api/v1/auth/me to confirm the token is still valid.

/** Build a fake token that carries a user, for use until the API is wired. */
export function makeFakeToken(user: User): string {
  const payload = base64UrlEncode(JSON.stringify(user));
  return `fake.${payload}.sig`;
}

/** Pull the User back out of a fake token, or null if it isn't one we made. */
export function decodeUserFromToken(token: string): User | null {
  const parts = token.split('.');
  if (parts.length !== 3 || parts[0] !== 'fake') return null;
  try {
    const json = base64UrlDecode(parts[1]);
    const parsed: unknown = JSON.parse(json);
    return isUser(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function isUser(value: unknown): value is User {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Record<string, unknown>).id === 'number' &&
    typeof (value as Record<string, unknown>).email === 'string' &&
    typeof (value as Record<string, unknown>).displayName === 'string'
  );
}

// Minimal base64url helpers (no Buffer dependency in RN).
function base64UrlEncode(input: string): string {
  // RN/Hermes provides global btoa in SDK 52+. encodeURIComponent handles unicode.
  const b64 = globalThis.btoa(unescape(encodeURIComponent(input)));
  return b64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
function base64UrlDecode(input: string): string {
  const b64 = input.replace(/-/g, '+').replace(/_/g, '/');
  return decodeURIComponent(escape(globalThis.atob(b64)));
}

// ----------------------------------------------------------------------------
// The store  (use the CURRIED create<T>()(...) form)
// ----------------------------------------------------------------------------

export const useSessionStore = create<SessionState>()((set) => ({
  status: 'loading',
  token: null,
  user: null,

  login: async (token, user) => {
    // TODO:
    //   1. await SecureStore.setItemAsync(TOKEN_KEY, token)
    //   2. set status 'authenticated', token, user
    throw new Error('login not implemented');
  },

  logout: async () => {
    // TODO:
    //   1. await SecureStore.deleteItemAsync(TOKEN_KEY)
    //   2. set status 'unauthenticated', token: null, user: null
    throw new Error('logout not implemented');
  },
}));

// ----------------------------------------------------------------------------
// Bootstrap — called once at app launch, before deciding which navigator to show
// ----------------------------------------------------------------------------

/**
 * Read the persisted token, decode the user, and resolve the loading state.
 * Must leave `status` as 'authenticated' or 'unauthenticated' — never 'loading'.
 * Idempotent: safe to call once on mount.
 */
export async function bootstrapSession(): Promise<void> {
  // TODO:
  //   1. const token = await SecureStore.getItemAsync(TOKEN_KEY)
  //   2. if no token -> setState status 'unauthenticated' and return.
  //   3. const user = decodeUserFromToken(token)
  //   4. if user is null (corrupt/foreign token) -> deleteItemAsync, then
  //      setState 'unauthenticated' and return.
  //   5. otherwise setState { status: 'authenticated', token, user }.
  throw new Error('bootstrapSession not implemented');
}

// ----------------------------------------------------------------------------
// Convenience selectors (so screens import these, not the whole store)
// ----------------------------------------------------------------------------

export const selectStatus = (s: SessionState): SessionStatus => s.status;
export const selectUser = (s: SessionState): User | null => s.user;
export const selectIsAuthed = (s: SessionState): boolean => s.status === 'authenticated';

// ----------------------------------------------------------------------------
// EXPECTED BEHAVIOR (verify in a screen or a quick test harness)
// ----------------------------------------------------------------------------
//
//   - On a fresh install, bootstrapSession() leaves status 'unauthenticated'.
//   - After login(makeFakeToken(u), u), getItemAsync(TOKEN_KEY) returns the
//     token, status is 'authenticated', and user deep-equals u.
//   - Kill and relaunch the app, call bootstrapSession(): status becomes
//     'authenticated' again and user is restored from the token — no re-login.
//   - After logout(), getItemAsync(TOKEN_KEY) returns null and status is
//     'unauthenticated'.
//   - A deliberately corrupted stored token causes bootstrapSession() to clear
//     it and land 'unauthenticated' (not crash, not 'loading').
//
// Quick manual check you can paste into a screen's onPress:
//
//   const demo: User = { id: 1, email: 'ada@crunch.dev', displayName: 'Ada' };
//   await useSessionStore.getState().login(makeFakeToken(demo), demo);
//   console.log(await SecureStore.getItemAsync(TOKEN_KEY)); // a fake.* token
//   console.log(useSessionStore.getState().status);          // 'authenticated'
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// login:
//   login: async (token, user) => {
//     await SecureStore.setItemAsync(TOKEN_KEY, token);
//     set({ status: 'authenticated', token, user });
//   },
//
// logout:
//   logout: async () => {
//     await SecureStore.deleteItemAsync(TOKEN_KEY);
//     set({ status: 'unauthenticated', token: null, user: null });
//   },
//
// bootstrapSession:
//   export async function bootstrapSession(): Promise<void> {
//     const token = await SecureStore.getItemAsync(TOKEN_KEY);
//     if (!token) { useSessionStore.setState({ status: 'unauthenticated' }); return; }
//     const user = decodeUserFromToken(token);
//     if (!user) {
//       await SecureStore.deleteItemAsync(TOKEN_KEY);
//       useSessionStore.setState({ status: 'unauthenticated', token: null, user: null });
//       return;
//     }
//     useSessionStore.setState({ status: 'authenticated', token, user });
//   }
//
// Why the curried create<T>()(...)? It lets TypeScript infer `set`/`get` and is
// REQUIRED the moment you wrap the store in persist(...) middleware (see
// Lecture 2 §6). Writing it now means adding persistence later is one line.
//
// ----------------------------------------------------------------------------
