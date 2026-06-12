# Lecture 1 — Multi-Screen Apps: React Navigation, Typed Routes, Tabs, and Deep Links

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can install React Navigation in an Expo + TypeScript app, build a stack and a bottom-tab navigator, type every route and every param so the compiler catches a wrong `navigate(...)` call, nest navigators correctly, and map a deep-link URL to a screen.

If you only remember one thing from this lecture, remember this:

> **Navigation is application state, and React Navigation owns it.** A screen is not a page you visit; it's an entry in a state tree that React Navigation keeps and renders. Once you internalize that — that "go to the habit detail with id 42" is a state transition, not a page load — the typed-routes machinery stops feeling like ceremony and starts feeling like a type-checked state machine.

We are going to build, by the end of this lecture, the skeleton of the Crunch Tracker app: a login screen, then a bottom-tab bar with Habits, Goals, and Profile, where Habits is itself a stack (list → detail). Everything typed, no `any`.

---

## 1. Why you can't just conditionally render screens

In Week 7 you had one screen. To add a second, your instinct from web React might be: keep a `const [screen, setScreen] = useState<'list' | 'detail'>('list')` and render one or the other. It works for exactly two screens and then collapses.

What conditional rendering does **not** give you, and what a navigation library does:

- **A back stack.** The hardware back button on Android and the swipe-back gesture on iOS need to know what to return to. You'd have to model that history yourself.
- **Native transitions.** Push/pop animations, the iOS swipe-to-go-back, shared headers — the platform does these, and a navigation library hooks into the native APIs to get them for free.
- **Per-screen headers, titles, and gestures.** A tab bar that persists while the screen under it changes. A header that shows a back chevron on a pushed screen but not on a tab root.
- **Deep links.** A URL that opens a specific screen with specific params — impossible to do cleanly with ad-hoc `useState`.
- **Type-safe params.** "Open the detail screen for habit 42" should be a typed call that the compiler validates.

React Navigation gives you all of that. In 2026 it is at **version 7**, it works in plain React Native and in Expo, and it is what the overwhelming majority of production Expo apps use. (Expo Router is a file-based layer *on top of* React Navigation; we use React Navigation directly this week so you see the machinery, then you'll recognize it under Expo Router later.)

---

## 2. Installing it

From your Week 7 Expo app (or a fresh `npx create-expo-app@latest crunch-tracker -t expo-template-blank-typescript`):

```bash
npm install @react-navigation/native
npx expo install react-native-screens react-native-safe-area-context
npm install @react-navigation/native-stack @react-navigation/bottom-tabs
```

A few things worth knowing:

- `@react-navigation/native` is the core. It does nothing visible on its own — it's the state container.
- `react-native-screens` and `react-native-safe-area-context` are **native** dependencies, which is why we install them with `npx expo install` (it pins versions compatible with your Expo SDK) rather than plain `npm install`. `react-native-screens` lets the navigators use real native screen primitives instead of plain `View`s — it's what makes transitions smooth and memory sane.
- `native-stack` and `bottom-tabs` are the two navigators we use. There's also a JS `stack`, a `drawer`, and `material-top-tabs`; we don't need them this week.

> **Why `native-stack` and not `stack`?** `@react-navigation/native-stack` is backed by the platform's native navigation primitives (`UINavigationController` on iOS, the Fragment-based stack on Android). It's faster and feels native. The older `@react-navigation/stack` is pure JS — more customizable, less native-feeling. Default to native-stack; reach for the JS stack only when you need a custom transition the native one can't do.

---

## 3. The container and your first stack

Everything lives under a single `NavigationContainer` at the root of your app. It owns the navigation state for the whole tree.

```tsx
// App.tsx
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { LoginScreen } from './src/screens/LoginScreen';
import { HabitsScreen } from './src/screens/HabitsScreen';

const Stack = createNativeStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Habits" component={HabitsScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
```

That's a working two-screen app. `createNativeStackNavigator()` returns an object with two components: `Navigator` (the container for a set of screens) and `Screen` (one entry). The first `<Stack.Screen>` is the initial route unless you set `initialRouteName`.

Inside any screen component, you get a `navigation` object (via props or the `useNavigation` hook) to move around:

```tsx
import { useNavigation } from '@react-navigation/native';

function LoginScreen() {
  const navigation = useNavigation();
  return (
    <Button title="Log in" onPress={() => navigation.navigate('Habits')} />
  );
}
```

`navigation.navigate('Habits')` pushes the Habits screen. `navigation.goBack()` pops it. There's also `push` (always adds a new instance, even of the same route), `replace` (swap the current screen — useful after login), and `popToTop` (back to the root).

But notice the problem already: `navigation.navigate('Habits')` is a **string**. Typo it as `'Habbits'` and nothing complains until runtime. That's what the next section fixes.

---

## 4. Typed routes — the part that matters

This is the single most valuable thing in this lecture. We're going to make the compiler reject `navigate('Habbits')` and reject passing the wrong params.

### Step 1 — declare the param list

A **ParamList** is a TypeScript type that maps every route name to the params it accepts. `undefined` means "no params."

```ts
// src/navigation/types.ts
export type RootStackParamList = {
  Login: undefined;
  Habits: undefined;
  HabitDetail: { habitId: number };
};
```

This says: `Login` and `Habits` take no params; `HabitDetail` requires a `{ habitId: number }`.

### Step 2 — make the navigator generic

```tsx
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { RootStackParamList } from './navigation/types';

const Stack = createNativeStackNavigator<RootStackParamList>();
```

Now `<Stack.Screen name="...">` only accepts the three names you declared. Misspell one and it's a compile error. Already a win.

### Step 3 — type the screen's props

A screen receives `navigation` and `route` props. React Navigation gives you a helper to type both at once:

```tsx
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'HabitDetail'>;

export function HabitDetailScreen({ route, navigation }: Props) {
  const { habitId } = route.params; // typed as number — no cast
  return <Text>Habit #{habitId}</Text>;
}
```

`route.params` is now `{ habitId: number }`. There is no `as` anywhere. If you try `route.params.habbitId`, the compiler stops you.

### Step 4 — type the navigate calls

When you navigate to a screen that needs params, the compiler enforces them:

```tsx
navigation.navigate('HabitDetail', { habitId: 42 }); // ok
navigation.navigate('HabitDetail');                  // error: params required
navigation.navigate('HabitDetail', { habitId: '42' }); // error: string not number
```

### Step 5 — type the bare `useNavigation` hook

When you call `useNavigation()` in a deeply nested component that doesn't get props, type it explicitly:

```ts
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

type Nav = NativeStackNavigationProp<RootStackParamList>;
const navigation = useNavigation<Nav>();
```

### Step 6 (optional but recommended) — the global override

You can register your root param list globally so `useNavigation()` is typed *everywhere* without passing the generic each time. This uses TypeScript declaration merging:

```ts
// src/navigation/types.ts
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
```

Add that once, import the file anywhere in your entry path, and bare `useNavigation()` calls become typed against `RootStackParamList`. The official guide documents this; we use it in the mini-project. Read <https://reactnavigation.org/docs/typescript/>.

> **C3 coding standard:** no screen in your app may read `route.params` without a typed `Props`, and no `navigate(...)` call may use an untyped navigation object. If you find yourself writing `as any` to make a navigation call compile, you've skipped a step above — go back and fix the types, don't cast through them.

---

## 5. Bottom tabs

A stack is "screens on top of each other." Tabs are "sibling screens you switch between with a bar." Crunch Tracker's main surface is three tabs: Habits, Goals, Profile.

```tsx
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';

export type MainTabParamList = {
  Habits: undefined;
  Goals: undefined;
  Profile: undefined;
};

const Tab = createBottomTabNavigator<MainTabParamList>();

export function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: true,
        tabBarIcon: ({ color, size }) => {
          const name =
            route.name === 'Habits' ? 'checkbox-outline'
            : route.name === 'Goals' ? 'flag-outline'
            : 'person-outline';
          return <Ionicons name={name} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#7c3aed',
        tabBarInactiveTintColor: '#9ca3af',
      })}
    >
      <Tab.Screen name="Habits" component={HabitsStack} />
      <Tab.Screen name="Goals" component={GoalsScreen} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
```

Two things to notice:

- `screenOptions` can be a **function** of `{ route }`, which is how you give each tab a different icon from one place.
- `Ionicons` comes from `@expo/vector-icons`, which ships with Expo — no extra install.

Tabs do **not** maintain a back stack between each other. Tapping Goals then Habits doesn't "go back" from Habits to Goals — they're siblings. If you need a back stack *within* a tab (list → detail), the tab's `component` must itself be a stack navigator. Which is exactly the next section.

---

## 6. Nesting — a stack inside a tab inside an auth stack

Real apps nest navigators. The Crunch Tracker shape is:

```
RootStack (native-stack)
├── Login                         ← shown when logged out
└── Main                          ← shown when logged in
    └── MainTabs (bottom-tabs)
        ├── Habits → HabitsStack (native-stack)
        │           ├── HabitList
        │           └── HabitDetail   { habitId: number }
        ├── Goals  → GoalsScreen
        └── Profile→ ProfileScreen
```

The Habits tab is a stack so that list → detail gets a push animation and a back button, while the tab bar stays put.

```tsx
const HabitsStackNav = createNativeStackNavigator<HabitsStackParamList>();

export type HabitsStackParamList = {
  HabitList: undefined;
  HabitDetail: { habitId: number };
};

function HabitsStack() {
  return (
    <HabitsStackNav.Navigator>
      <HabitsStackNav.Screen name="HabitList" component={HabitListScreen} />
      <HabitsStackNav.Screen name="HabitDetail" component={HabitDetailScreen} />
    </HabitsStackNav.Navigator>
  );
}
```

### Nesting rules you must internalize

1. **Each navigator has its own param list.** `RootStackParamList`, `MainTabParamList`, `HabitsStackParamList` are three separate types. Don't try to flatten them into one.
2. **Navigating to a nested screen uses the nested syntax.** From outside the Habits stack, to open a specific habit:
   ```tsx
   navigation.navigate('Main', {
     screen: 'Habits',
     params: { screen: 'HabitDetail', params: { habitId: 42 } },
   });
   ```
   You navigate to the navigator (`Main`), tell it which screen (`Habits`), and pass through to the inner screen. Typed param lists make this verbose but compiler-checked.
3. **Headers can double up.** A stack inside a tab can show *two* headers (the tab's and the stack's). Usually you set `headerShown: false` on the outer one and let the inner stack own the header, or vice versa. Decide deliberately.
4. **Don't over-nest.** Each layer of nesting is a layer of complexity in your navigate calls and your types. Three levels (root → tabs → per-tab stack) is the practical ceiling for an app this size.

> **The "composing param lists" type trick.** To make a *child* navigator's screen also able to navigate to its *parent's* screens with full typing, React Navigation gives you `CompositeScreenProps`. For example, a screen inside `HabitsStack` that needs to navigate to a root-level `Login`:
> ```ts
> type Props = CompositeScreenProps<
>   NativeStackScreenProps<HabitsStackParamList, 'HabitDetail'>,
>   BottomTabScreenProps<MainTabParamList>
> >;
> ```
> You'll need this in the mini-project. The guide has the full pattern: <https://reactnavigation.org/docs/typescript/#nesting-navigators>.

---

## 7. Passing, reading, and updating params

Params are how one screen tells the next screen what to show. Three operations:

**Pass** (when navigating):
```tsx
navigation.navigate('HabitDetail', { habitId: 42 });
```

**Read** (in the target screen):
```tsx
const { habitId } = route.params;
```

**Update** (change the current screen's own params, e.g. after an edit):
```tsx
navigation.setParams({ habitId: 43 });
```

A few discipline points that separate juniors from seniors:

- **Params are for identifiers and small primitives, not for whole objects.** Pass `{ habitId: 42 }`, not the entire habit object. Why? Because params get serialized into the navigation state (and into deep-link URLs), and because the habit might change while the detail screen is open — if you passed a snapshot, it goes stale. Pass the id, then *read the current habit from your store* by that id. This is a Week 9 setup: the detail screen will pass an id and fetch the live record.
- **Never put functions or class instances in params.** They can't be serialized; React Navigation will warn you. If a screen needs to "call back" to its opener, use shared state (a Zustand action — Lecture 2), not a function-in-params.
- **Give optional params defaults at read time:** `const { sort = 'name' } = route.params ?? {};`.

---

## 8. Headers, options, and screen configuration

Each screen can configure its header, title, and behavior via `options`, either statically or as a function of the route:

```tsx
<HabitsStackNav.Screen
  name="HabitDetail"
  component={HabitDetailScreen}
  options={({ route }) => ({ title: `Habit #${route.params.habitId}` })}
/>
```

You can also set options imperatively from inside a screen with `navigation.setOptions(...)` — handy when the title depends on loaded data:

```tsx
useLayoutEffect(() => {
  navigation.setOptions({ title: habit?.name ?? 'Habit' });
}, [navigation, habit]);
```

Use `useLayoutEffect`, not `useEffect`, for header updates so the title is set before the first paint — otherwise the title visibly flickers from the default to the real value.

Common options you'll set this week: `title`, `headerShown`, `headerBackTitle` (iOS), `presentation: 'modal'` (slide a screen up as a modal — great for the add-habit form), and `tabBarBadge` (a count dot on a tab).

---

## 9. Deep links

A deep link is a URL that opens a specific screen with specific params. Two reasons you care: (1) push notifications and emails that "open the app to this habit," and (2) it's a clean way to *test* navigation from the terminal.

### Configure the scheme

In `app.json` (Expo):
```json
{ "expo": { "scheme": "crunchtracker" } }
```

### Configure the linking map

Tell `NavigationContainer` how URLs map to your nested navigators:

```tsx
import * as Linking from 'expo-linking';

const linking = {
  prefixes: [Linking.createURL('/'), 'crunchtracker://'],
  config: {
    screens: {
      Login: 'login',
      Main: {
        screens: {
          Habits: {
            screens: {
              HabitList: 'habits',
              HabitDetail: 'habits/:habitId',
            },
          },
          Goals: 'goals',
          Profile: 'profile',
        },
      },
    },
  },
};

<NavigationContainer linking={linking} fallback={<SplashScreen />}>
```

Now `crunchtracker://habits/42` opens the Habit detail with `habitId: 42`. The `:habitId` path segment becomes a param. (By default params come in as strings; you'll parse `Number(route.params.habitId)` or add a `parse` function to the linking config — the mini-project does the latter.)

### Test it from the terminal

```bash
npx uri-scheme open crunchtracker://habits/42 --ios
npx uri-scheme open crunchtracker://habits/42 --android
```

The app should foreground and land on Habit #42. If it doesn't, your nesting in the `config` doesn't match your navigator tree — they must mirror each other exactly. This is the most common deep-link bug: the linking config and the navigator nesting drift apart.

---

## 10. A subtle but important point: navigation state survives re-renders, not reloads

The navigation state (which screen, what params, the back stack) lives in `NavigationContainer`. It survives component re-renders. It does **not** survive an app reload by default — restart the app and you're back at the initial route. For most apps that's fine; for some you want state persistence. React Navigation supports it via the `onStateChange` / `initialState` props, persisting the serialized state to `AsyncStorage`. We don't need it for Crunch Tracker — when the app relaunches we want to re-check auth and land on the right navigator from scratch — but know it exists.

This connects to Lecture 2's central theme: navigation state is *one kind* of state, with its own owner (React Navigation) and its own lifetime (the session, not persisted). Your session token, by contrast, *must* persist across reloads — which is why it lives in SecureStore, not navigation state. Different state, different home.

---

## 11. Putting it together — the app shell

Here's the root that ties Sections 3–6 together. The auth conditional (`isAuthenticated`) is a placeholder this lecture — Lecture 2 wires it to a real Zustand session store, and the challenge makes it flicker-free.

```tsx
// App.tsx
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { RootStackParamList } from './src/navigation/types';

const RootStack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  const isAuthenticated = false; // ← Lecture 2 replaces this with useSession()

  return (
    <NavigationContainer linking={linking}>
      <RootStack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <RootStack.Screen name="Main" component={MainTabs} />
        ) : (
          <RootStack.Screen name="Login" component={LoginScreen} />
        )}
      </RootStack.Navigator>
    </NavigationContainer>
  );
}
```

Notice we conditionally render *which screens exist in the navigator* — not which navigator renders. When `isAuthenticated` flips, the `Login` screen leaves the navigator and `Main` enters. React Navigation animates the transition and, critically, there is **no `Login` in the back stack** once you're in `Main`, so the user cannot swipe or hardware-back into the login screen. That property — the protected screens are simply *not present* in the navigator while logged in — is the entire reason the auth gate is leak-proof. Memorize the pattern; the challenge is built on it.

---

## 12. Recap

You should now be able to:

- Explain why a navigation library beats ad-hoc conditional rendering: back stack, native transitions, deep links, typed params.
- Install React Navigation 7 and its native peers in an Expo app.
- Build a native-stack navigator and a bottom-tab navigator, and nest a stack inside a tab inside a root stack.
- Type the whole thing: `ParamList` types, `NativeStackScreenProps`, typed `navigate(...)`, the global `RootParamList` override, and `CompositeScreenProps` for nested screens.
- Pass, read, and update params — and explain why you pass an id, not a whole object.
- Configure deep links and test them from the terminal with `uri-scheme`.
- Explain why conditionally rendering screens (not navigators) is what makes the auth gate leak-proof.

Next: the other half of "is this an app" — managing the state those screens share. Continue to [Lecture 2 — Managing State on Purpose](./02-managing-state-on-purpose.md).

---

## References

- *React Navigation — Getting started*: <https://reactnavigation.org/docs/getting-started>
- *React Navigation — Type checking with TypeScript*: <https://reactnavigation.org/docs/typescript/>
- *Native Stack Navigator*: <https://reactnavigation.org/docs/native-stack-navigator>
- *Bottom Tabs Navigator*: <https://reactnavigation.org/docs/bottom-tab-navigator>
- *Nesting navigators*: <https://reactnavigation.org/docs/nesting-navigators>
- *Deep linking*: <https://reactnavigation.org/docs/deep-linking>
- *Expo — Linking*: <https://docs.expo.dev/guides/linking/>
