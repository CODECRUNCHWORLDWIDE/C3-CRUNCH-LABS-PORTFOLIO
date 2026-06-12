/**
 * Exercise 3 - The build-time config seam for the Crunch Tracker mobile app.
 *
 * This is the ONE place in the app that knows the backend's address. Everything
 * else (the apiClient, every TanStack Query hook, every screen) imports `config`
 * from here and never hard-codes a URL. That single seam is what lets an EAS
 * build point at the LIVE API instead of `localhost`: you set EXPO_PUBLIC_API_URL
 * per build profile in eas.json, and it is frozen into the binary at build time.
 *
 * Drop this in `config/env.ts`. It is real TypeScript: `tsc --noEmit` is clean,
 * and it runs in both the Expo dev server and a standalone EAS build.
 *
 * Why two sources (process.env AND expo-constants `extra`)?
 *   - `process.env.EXPO_PUBLIC_API_URL` is inlined by Expo's bundler at build
 *     time for any var prefixed EXPO_PUBLIC_. This is the primary source.
 *   - `Constants.expoConfig.extra.apiUrl` is a fallback for setups that inject
 *     config through app.config.ts `extra` instead of an env var. Reading both
 *     means a misconfigured build FAILS LOUDLY at startup rather than silently
 *     pointing at nothing.
 *
 * Wire app.config.ts to forward the env var into `extra` so both paths agree:
 *
 *   // app.config.ts
 *   export default {
 *     expo: {
 *       name: "Crunch Tracker",
 *       slug: "crunch-tracker",
 *       extra: { apiUrl: process.env.EXPO_PUBLIC_API_URL },
 *     },
 *   };
 */

import Constants from "expo-constants";

/** Shape of the `extra` block we expect in app config. */
interface AppExtra {
  apiUrl?: string;
}

/** The resolved, validated runtime configuration the rest of the app reads. */
export interface AppConfig {
  /** Base URL of the Crunch Tracker API, no trailing slash. */
  readonly apiUrl: string;
  /** True when running against the Expo dev server (not a standalone build). */
  readonly isDev: boolean;
}

const EXPO_PUBLIC_API_URL = "EXPO_PUBLIC_API_URL";

/**
 * Pull the API URL from build-time config, preferring the inlined env var and
 * falling back to expo-constants `extra`. Returns `undefined` if neither is set
 * so the caller can throw a clear, actionable error.
 */
function readApiUrl(): string | undefined {
  // process.env.EXPO_PUBLIC_* is statically replaced by the bundler; reading it
  // via bracket access keeps it inlined and avoids `any`.
  const fromEnv = process.env[EXPO_PUBLIC_API_URL];
  const extra = (Constants.expoConfig?.extra ?? {}) as AppExtra;
  const fromExtra = extra.apiUrl;
  const raw = fromEnv ?? fromExtra;
  return raw && raw.trim().length > 0 ? raw.trim() : undefined;
}

/** Strip any trailing slashes so `${apiUrl}/api/x` never double-slashes. */
function normalizeBaseUrl(url: string): string {
  return url.replace(/\/+$/, "");
}

/**
 * Validate that we got a real http(s) URL. A common mistake is shipping a build
 * whose EXPO_PUBLIC_API_URL still points at localhost -- which, on a phone, is
 * the phone itself. We can't detect intent, but we CAN reject obviously-wrong
 * values (empty, non-URL) and warn loudly on a localhost URL in a release build.
 */
function assertUsableUrl(url: string, isDev: boolean): void {
  let parsed: URL;
  try {
    parsed = new URL(url);
  } catch {
    throw new Error(
      `[config] API URL "${url}" is not a valid URL. ` +
        `Set ${EXPO_PUBLIC_API_URL} to your deployed API (e.g. https://crunch-tracker-api.onrender.com).`,
    );
  }
  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    throw new Error(`[config] API URL must be http(s); got "${parsed.protocol}".`);
  }
  const isLocalhost =
    parsed.hostname === "localhost" ||
    parsed.hostname === "127.0.0.1" ||
    parsed.hostname.startsWith("192.168.") ||
    parsed.hostname.startsWith("10.");
  if (isLocalhost && !isDev) {
    // A release/standalone build pointed at localhost is the #1 capstone bug.
    // It won't reach anything on a real device. Surface it as loud as we can.
    throw new Error(
      `[config] This is a release build but the API URL is "${url}", which a ` +
        `phone cannot reach. Set ${EXPO_PUBLIC_API_URL} to your LIVE API host in ` +
        `the eas.json "preview"/"production" profile and rebuild.`,
    );
  }
}

/**
 * Build the validated config object once, at module load. If it throws, the app
 * fails fast at startup with a clear message -- far better than a confusing
 * "network request failed" on every screen of a shipped build.
 */
function buildConfig(): AppConfig {
  const isDev = __DEV__; // Expo/RN global: true in dev, false in a release build
  const raw = readApiUrl();
  if (!raw) {
    throw new Error(
      `[config] No API URL configured. Set ${EXPO_PUBLIC_API_URL} in eas.json ` +
        `for this build profile (or in your shell for the dev server).`,
    );
  }
  const apiUrl = normalizeBaseUrl(raw);
  assertUsableUrl(apiUrl, isDev);
  return Object.freeze({ apiUrl, isDev });
}

/**
 * The app-wide config. Import this everywhere a URL is needed:
 *
 *   import { config } from "../config/env";
 *   const res = await fetch(`${config.apiUrl}/api/habits`, ...);
 */
export const config: AppConfig = buildConfig();

/**
 * Convenience helper used by the single apiClient. Joins the base URL with a
 * path, guaranteeing exactly one slash between them.
 */
export function apiUrl(path: string): string {
  const suffix = path.startsWith("/") ? path : `/${path}`;
  return `${config.apiUrl}${suffix}`;
}
