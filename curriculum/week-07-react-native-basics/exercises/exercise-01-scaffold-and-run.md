# Exercise 1 — Scaffold and Run

**Goal:** Scaffold a real Expo + TypeScript app from a blank folder, run it on a simulator and a physical device, write your first typed function component, and see `0` TypeScript errors and no LogBox warnings. No IDE wizards. No template repos. Just you, `npx`, and your phone.

**Estimated time:** 40 minutes.

---

## Setup

You need **Node.js LTS** (20.x or 22.x) and a phone. Verify Node:

```bash
node --version    # v20.x or v22.x
npm --version
```

Install **Expo Go** on your phone from the App Store (iOS) or Play Store (Android). If you're on macOS and want the iOS Simulator, install Xcode and run `xcode-select --install`. For Android, install Android Studio and create a virtual device. The simulator/emulator is optional — Expo Go on a real phone covers everything.

You also need Git. `git --version` should print a real version.

---

## Step 1 — Scaffold the app

```bash
npx create-expo-app@latest CrunchTracker --template blank-typescript
cd CrunchTracker
```

This downloads the template and installs dependencies. When it finishes, initialize Git and make a first commit:

```bash
git init
git add .
git commit -m "Initial Expo blank-typescript app"
```

Look at what you got:

```bash
ls
# App.tsx  app.json  assets  babel.config.js  package.json  tsconfig.json  ...
```

Notice there is **no** `ios/` or `android/` folder. The native projects are generated on demand by Expo. You live in `App.tsx`.

---

## Step 2 — Run it

```bash
npx expo start
```

Metro boots and prints a QR code plus a menu (`a` = Android, `i` = iOS simulator, `w` = web).

Run it **two ways** this exercise — you must see both:

1. **On a real device:** open Expo Go and scan the QR code (iOS: use the Camera app; Android: use Expo Go's scanner). Your phone and laptop must share a Wi-Fi network. If nothing loads after ~30s, stop and run `npx expo start --tunnel`, then scan the new QR — tunnel mode works on locked-down networks.
2. **On a simulator/emulator:** press `i` (macOS, iOS) or `a` (Android).

You should see the template's "Open up App.tsx to start working on your app!" screen.

---

## Step 3 — Prove Fast Refresh works

Open `App.tsx` and change the text:

```tsx
<Text>Open up App.tsx to start working on your app!</Text>
```

to:

```tsx
<Text>Crunch Tracker — coming together this week.</Text>
```

Save. The device updates in well under a second **without** a full reload. That loop — edit, save, see it — is the whole point of this stack.

---

## Step 4 — Write your first typed component

Create a new file `components/Banner.tsx`:

```tsx
import { StyleSheet, Text, View } from "react-native";

interface BannerProps {
  title: string;
  subtitle?: string;
}

export function Banner({ title, subtitle }: BannerProps) {
  return (
    <View style={styles.banner}>
      <Text style={styles.title}>{title}</Text>
      {subtitle ? <Text style={styles.subtitle}>{subtitle}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    backgroundColor: "#0B5",
    paddingVertical: 24,
    paddingHorizontal: 16,
    borderRadius: 12,
    margin: 16,
  },
  title: { color: "#fff", fontSize: 22, fontWeight: "700" },
  subtitle: { color: "#eaffea", fontSize: 14, marginTop: 4 },
});
```

Now use it from `App.tsx`:

```tsx
import { StatusBar } from "expo-status-bar";
import { SafeAreaView, StyleSheet } from "react-native";

import { Banner } from "./components/Banner";

export default function App() {
  return (
    <SafeAreaView style={styles.screen}>
      <Banner title="Crunch Tracker" subtitle="Week 7 — React Native basics" />
      <StatusBar style="auto" />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#fff" },
});
```

Save. The green banner with a title and subtitle appears at the top, below the notch.

---

## Step 5 — Type-check and prove it's clean

In a second terminal:

```bash
npx tsc --noEmit
```

It should print **nothing** — zero errors. Now deliberately break a type to feel the safety net. In `App.tsx`, change:

```tsx
<Banner title="Crunch Tracker" subtitle="Week 7 — React Native basics" />
```

to:

```tsx
<Banner subtitle="Week 7 — React Native basics" />
```

Re-run `npx tsc --noEmit`. You should see:

```
App.tsx:8:8 - error TS2741: Property 'title' is missing in type ...
```

That's TypeScript catching a missing required prop *before* the app runs. Put `title` back; confirm `tsc` is clean again.

---

## Step 6 — Commit

```bash
git add .
git commit -m "Add typed Banner component, render it in App"
```

---

## Acceptance criteria

You can mark this exercise done when:

- [ ] You have a `CrunchTracker/` folder created by `create-expo-app --template blank-typescript`.
- [ ] `npx expo start` runs and you opened the app on **both** a real device (Expo Go) **and** a simulator/emulator.
- [ ] You changed text in `App.tsx` and watched Fast Refresh update the device without a full reload.
- [ ] A typed `Banner` component renders with a title and conditional subtitle, inside a `SafeAreaView`.
- [ ] `npx tsc --noEmit` prints **0 errors**.
- [ ] No LogBox warnings (yellow/red) appear on the device.
- [ ] You have at least 3 Git commits with sensible messages.
- [ ] You can explain, in your own words, why there's no `ios/`/`android/` folder and what Expo Go is doing.

---

## Stretch

- Add a `tone?: "success" | "warning"` prop to `Banner` that switches `backgroundColor`. Use a union type and a conditional style array.
- Add a third optional prop and render it conditionally with `&&` instead of a ternary — then make sure you don't accidentally render a `0`.
- Run `npx expo start --tunnel` and notice the difference in QR/connection behavior versus LAN mode. Write one sentence on when you'd need tunnel mode.

---

## Hints

<details>
<summary>If the QR code won't connect on a real device</summary>

Your phone and laptop must be on the same Wi-Fi, and that network must allow device-to-device traffic. Campus, office, and some home mesh networks block it. The fix is `npx expo start --tunnel`, which routes through Expo's servers (slower, but works anywhere). You may be prompted to install `@expo/ngrok` the first time — accept.

</details>

<details>
<summary>If `npx tsc --noEmit` says it can't find a config</summary>

Run it from the project root (where `tsconfig.json` lives). The `blank-typescript` template ships a `tsconfig.json` that extends `expo/tsconfig.base`; if it's missing, your scaffold failed — re-run `create-expo-app`.

</details>

<details>
<summary>If you see "Text strings must be rendered within a &lt;Text&gt; component"</summary>

You put a bare string inside a `<View>`. Every string must be wrapped in `<Text>`. This is the most common week-7 error; it's RN telling you there's no implicit text node like the web has.

</details>

---

When this exercise feels comfortable, move to [Exercise 2 — The HabitCard component](exercise-02-habit-card.tsx).
