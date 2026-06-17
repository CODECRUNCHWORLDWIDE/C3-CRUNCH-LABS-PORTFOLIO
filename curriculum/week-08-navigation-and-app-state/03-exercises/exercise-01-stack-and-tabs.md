# Exercise 1 — Typed Stack + Bottom Tabs

**Goal:** Stand up the Crunch Tracker navigation shell from a blank Expo app: a root native-stack with a Login screen and a `Main` screen, where `Main` is a bottom-tab navigator (Habits / Goals / Profile), and the Habits tab is itself a stack (list → detail). Every route, every param, every navigate call fully typed. No `any`.

**Estimated time:** 50 minutes.

---

## Setup

You need Node 20+ and the Expo tooling. Verify:

```bash
node --version   # v20.x or newer
npx expo --version
```

Create a fresh TypeScript app (or use your Week 7 app — but starting clean here is cleaner):

```bash
npx create-expo-app@latest crunch-nav --template expo-template-blank-typescript
cd crunch-nav
```

Install React Navigation and its native peers:

```bash
npm install @react-navigation/native @react-navigation/native-stack @react-navigation/bottom-tabs
npx expo install react-native-screens react-native-safe-area-context
```

Sanity check: `npx expo start`, press `i` (iOS) or `a` (Android). The blank app should load.

---

## Step 1 — The param-list types

Create `src/navigation/types.ts`. This file is the spine of the whole exercise.

```ts
// src/navigation/types.ts
import type { NavigatorScreenParams } from '@react-navigation/native';

export type HabitsStackParamList = {
  HabitList: undefined;
  HabitDetail: { habitId: number };
};

export type MainTabParamList = {
  Habits: NavigatorScreenParams<HabitsStackParamList>;
  Goals: undefined;
  Profile: undefined;
};

export type RootStackParamList = {
  Login: undefined;
  Main: NavigatorScreenParams<MainTabParamList>;
};

// Make bare useNavigation() typed everywhere.
declare global {
  namespace ReactNavigation {
    interface RootParamList extends RootStackParamList {}
  }
}
```

`NavigatorScreenParams<T>` is the type that lets a parent route accept "navigate to me, and here's which child screen and child params" — it's what makes the nested `navigation.navigate('Main', { screen: 'Habits', params: { ... } })` call type-check.

---

## Step 2 — Five trivial screens

You're wiring navigation, not building UI, so keep the screens to a few lines each. Create `src/screens/`:

```tsx
// src/screens/LoginScreen.tsx
import { View, Text, Button } from 'react-native';
import { useNavigation } from '@react-navigation/native';

export function LoginScreen() {
  const navigation = useNavigation(); // typed via the global override
  return (
    <View style={{ flex: 1, justifyContent: 'center', padding: 24 }}>
      <Text style={{ fontSize: 24, marginBottom: 16 }}>Crunch Tracker</Text>
      <Button title="Log in" onPress={() => navigation.navigate('Main', { screen: 'Habits' })} />
    </View>
  );
}
```

```tsx
// src/screens/HabitListScreen.tsx
import { View, Text, Button, FlatList } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { HabitsStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<HabitsStackParamList, 'HabitList'>;

const MOCK = [
  { id: 1, name: 'Read 20 min' },
  { id: 2, name: 'Drink water' },
  { id: 3, name: 'Ship code' },
];

export function HabitListScreen({ navigation }: Props) {
  return (
    <FlatList
      contentContainerStyle={{ padding: 16 }}
      data={MOCK}
      keyExtractor={(h) => String(h.id)}
      renderItem={({ item }) => (
        <View style={{ paddingVertical: 12 }}>
          <Text style={{ fontSize: 18 }}>{item.name}</Text>
          <Button title="Open" onPress={() => navigation.navigate('HabitDetail', { habitId: item.id })} />
        </View>
      )}
    />
  );
}
```

```tsx
// src/screens/HabitDetailScreen.tsx
import { View, Text } from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import type { HabitsStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<HabitsStackParamList, 'HabitDetail'>;

export function HabitDetailScreen({ route }: Props) {
  const { habitId } = route.params; // typed as number
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text style={{ fontSize: 22 }}>Habit #{habitId}</Text>
    </View>
  );
}
```

Make `GoalsScreen.tsx` and `ProfileScreen.tsx` the same trivial shape — a centered `<Text>Goals</Text>` and `<Text>Profile</Text>`.

---

## Step 3 — The Habits stack (a stack *inside* a tab)

```tsx
// src/navigation/HabitsStack.tsx
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { HabitsStackParamList } from './types';
import { HabitListScreen } from '../screens/HabitListScreen';
import { HabitDetailScreen } from '../screens/HabitDetailScreen';

const Stack = createNativeStackNavigator<HabitsStackParamList>();

export function HabitsStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="HabitList" component={HabitListScreen} options={{ title: 'Habits' }} />
      <Stack.Screen
        name="HabitDetail"
        component={HabitDetailScreen}
        options={({ route }) => ({ title: `Habit #${route.params.habitId}` })}
      />
    </Stack.Navigator>
  );
}
```

---

## Step 4 — The tab navigator

```tsx
// src/navigation/MainTabs.tsx
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import type { MainTabParamList } from './types';
import { HabitsStack } from './HabitsStack';
import { GoalsScreen } from '../screens/GoalsScreen';
import { ProfileScreen } from '../screens/ProfileScreen';

const Tab = createBottomTabNavigator<MainTabParamList>();

export function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: route.name !== 'Habits', // the Habits stack owns its own header
        tabBarActiveTintColor: '#7c3aed',
        tabBarIcon: ({ color, size }) => {
          const name =
            route.name === 'Habits' ? 'checkbox-outline'
            : route.name === 'Goals' ? 'flag-outline'
            : 'person-outline';
          return <Ionicons name={name} size={size} color={color} />;
        },
      })}
    >
      <Tab.Screen name="Habits" component={HabitsStack} />
      <Tab.Screen name="Goals" component={GoalsScreen} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
```

Notice `headerShown: route.name !== 'Habits'` — the Habits tab is a stack that draws its own header, so we hide the tab navigator's header for that one tab to avoid the doubled-header problem from Lecture 1 §6.

---

## Step 5 — The root and the App entry

```tsx
// src/navigation/RootNavigator.tsx
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import type { RootStackParamList } from './types';
import { LoginScreen } from '../screens/LoginScreen';
import { MainTabs } from './MainTabs';

const Root = createNativeStackNavigator<RootStackParamList>();

export function RootNavigator() {
  return (
    <Root.Navigator screenOptions={{ headerShown: false }}>
      <Root.Screen name="Login" component={LoginScreen} />
      <Root.Screen name="Main" component={MainTabs} />
    </Root.Navigator>
  );
}
```

```tsx
// App.tsx
import { NavigationContainer } from '@react-navigation/native';
import { RootNavigator } from './src/navigation/RootNavigator';
import './src/navigation/types'; // ensure the global declare-merge runs

export default function App() {
  return (
    <NavigationContainer>
      <RootNavigator />
    </NavigationContainer>
  );
}
```

---

## Step 6 — Run it and prove the types

```bash
npx expo start
```

Walk the flow on the simulator:

1. Login screen → tap **Log in** → lands on the Habits tab.
2. Tap a habit → pushes **Habit #N** with a working back chevron.
3. Tap the **Goals** and **Profile** tabs → they switch; no back stack between tabs.

Now prove the types hold:

```bash
npx tsc --noEmit
```

It should pass with zero errors. To *feel* the type safety, temporarily break something and watch the compiler catch it:

- Change `navigation.navigate('HabitDetail', { habitId: item.id })` to `{ habitId: String(item.id) }` → type error (string not number).
- Change it to `navigation.navigate('HabitDetial', ...)` → type error (no such route).
- Remove the `{ habitId }` param entirely → type error (params required).

Undo those after you've seen each error.

---

## Acceptance criteria

You can mark this exercise done when:

- [ ] The app runs and you can navigate Login → Habits tab → Habit detail → back.
- [ ] All three tabs switch correctly with icons and a tinted active color.
- [ ] `npx tsc --noEmit` passes with **zero errors and zero `any`**.
- [ ] `route.params.habitId` is typed as `number` in `HabitDetailScreen` (hover it in your editor to confirm).
- [ ] The Habits tab shows exactly **one** header (the stack's), not two.
- [ ] You can explain why navigating from Login uses `navigate('Main', { screen: 'Habits' })` and not `navigate('Habits')`.

---

## Stretch

- Add a deep-link config so `crunchtracker://habits/2` opens Habit #2. Set `"scheme": "crunchtracker"` in `app.json`, add the `linking` prop to `NavigationContainer`, and test with `npx uri-scheme open crunchtracker://habits/2 --ios`.
- Add a `presentation: 'modal'` "Add habit" screen to the Habits stack and a header button that opens it.
- Add a `tabBarBadge` to the Habits tab showing the mock habit count (`3`).

---

## Hints

<details>
<summary>If <code>useNavigation()</code> isn't typed</summary>

Make sure `src/navigation/types.ts` (with the `declare global { namespace ReactNavigation ... }` block) is imported somewhere in the module graph that runs at startup — importing it in `App.tsx` is enough. The declaration merge only takes effect if the file is part of the compilation.

</details>

<details>
<summary>If the nested navigate call won't type-check</summary>

The parent route's param type must be `NavigatorScreenParams<ChildParamList>`, not the child param list directly. That's what lets `{ screen, params }` flow through. Re-check Step 1.

</details>

<details>
<summary>If you see two headers on the Habits tab</summary>

That's the doubled-header problem. The tab navigator and the inner stack both render a header. Hide the tab navigator's header for the Habits route with `headerShown: route.name !== 'Habits'` (Step 4) and let the inner stack own it.

</details>

---

When this feels comfortable, move to [Exercise 2 — A controlled, validated habit form](./exercise-02-habit-form.tsx).
