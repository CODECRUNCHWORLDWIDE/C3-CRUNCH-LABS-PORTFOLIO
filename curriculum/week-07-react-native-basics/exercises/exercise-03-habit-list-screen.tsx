// Exercise 3 — The habit-list screen
//
// Goal: Build a screen that loads mock data through an effect, renders it with
//       a FlatList, and handles the three states every screen has: loading,
//       empty, and content. Practice useState, useEffect (with cleanup),
//       immutable array updates, and the FlatList API.
//
// Estimated time: 50 minutes.
//
// HOW TO USE THIS FILE
//
// 1. Save this as `screens/HabitListScreen.tsx` in your week-7 app. You also
//    need the HabitCard from Exercise 2 at `components/HabitCard.tsx` (it is
//    imported below). Render <HabitListScreen /> from App.tsx.
//
// 2. Fill in the bodies marked `// TODO`. Do not change the imports, the
//    MOCK_HABITS data, or the component's default-export signature.
//
// 3. Type-check with zero errors:  npx tsc --noEmit
//    Run it; confirm: a spinner for ~600ms, then the list; no LogBox warnings.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `npx tsc --noEmit`: 0 errors. No `any` anywhere you wrote.
//   [ ] On open: an ActivityIndicator shows, THEN the list of habits appears.
//   [ ] Tapping a card's control toggles its done state (immutably).
//   [ ] If you set MOCK_HABITS to [] the screen shows the empty state, not a
//       blank screen and not a crash.
//   [ ] The effect cleans up its timer (no "set state on unmounted" warning).
//   [ ] The list is rendered with FlatList + keyExtractor — NOT .map().
//   [ ] No LogBox warnings.
//
// Inline hints are at the bottom of the file. Don't peek until you've tried
// for at least 15 minutes.

import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from "react-native";

import { HabitCard, type Habit } from "../components/HabitCard";

// ----------------------------------------------------------------------------
// Mock data (this is our "backend" for week 7)
// ----------------------------------------------------------------------------

const MOCK_HABITS: Habit[] = [
  { id: "1", title: "Drink water", cadence: "daily", streak: 3, doneToday: false },
  { id: "2", title: "Stretch", cadence: "daily", streak: 0, doneToday: false },
  { id: "3", title: "Read 20 minutes", cadence: "daily", streak: 7, doneToday: true },
  { id: "4", title: "Review weekly goals", cadence: "weekly", streak: 2, doneToday: false },
];

// ----------------------------------------------------------------------------
// Screen
// ----------------------------------------------------------------------------

export default function HabitListScreen() {
  // TODO (1): two pieces of state:
  //   - habits: Habit[]   (starts as an empty array)
  //   - loading: boolean  (starts true)

  // TODO (2): a load-on-mount effect.
  //   - Use a `cancelled` flag and a setTimeout(~600ms) to simulate loading.
  //   - When the timer fires (and not cancelled): set habits to MOCK_HABITS
  //     and loading to false.
  //   - Return a cleanup that sets cancelled = true and clears the timeout.
  //   - Dependency array: [] (run once).

  // TODO (3): a `toggle(id: string)` function that flips doneToday for the
  //   matching habit, IMMUTABLY (use setHabits with prev => prev.map(...)).

  // TODO (4): while loading, return an early branch with an ActivityIndicator
  //   centered on screen (use styles.center).

  // TODO (5): otherwise render the content branch:
  //   <SafeAreaView style={styles.screen}>
  //     <Text style={styles.heading}>Today</Text>
  //     <FlatList
  //       data={habits}
  //       keyExtractor={(item) => item.id}
  //       renderItem={({ item }) => <HabitCard habit={item} onToggle={toggle} />}
  //       ListEmptyComponent={() => <Text style={styles.empty}>No habits yet.</Text>}
  //       contentContainerStyle={styles.listContent}
  //     />
  //   </SafeAreaView>

  return (
    // TODO: replace this placeholder with the loading/content branches above.
    <SafeAreaView style={styles.center}>
      <Text>TODO: implement HabitListScreen</Text>
    </SafeAreaView>
  );
}

// ----------------------------------------------------------------------------
// Styles
// ----------------------------------------------------------------------------

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#F8F9FA",
  },
  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#F8F9FA",
  },
  heading: {
    fontSize: 28,
    fontWeight: "800",
    color: "#111",
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 4,
  },
  listContent: {
    paddingVertical: 8,
    flexGrow: 1, // so ListEmptyComponent can center in an otherwise-short list
  },
  empty: {
    textAlign: "center",
    color: "#888",
    marginTop: 48,
    fontSize: 15,
  },
});

// ----------------------------------------------------------------------------
// Expected behavior
// ----------------------------------------------------------------------------
//
//  - On mount: a large spinner, centered, for ~600ms.
//  - Then: a "Today" heading and four habit cards in a scrolling FlatList.
//  - Tapping a card's ○/✓ control toggles that card (and its green tint).
//  - If MOCK_HABITS is [] (try it): after the spinner, "No habits yet."
//  - Navigating away mid-load (or hot-reloading) does NOT warn about setting
//    state on an unmounted component, because of the cleanup guard.
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// State:
//   const [habits, setHabits] = useState<Habit[]>([]);
//   const [loading, setLoading] = useState(true);
//
// Effect:
//   useEffect(() => {
//     let cancelled = false;
//     const t = setTimeout(() => {
//       if (!cancelled) {
//         setHabits(MOCK_HABITS);
//         setLoading(false);
//       }
//     }, 600);
//     return () => {
//       cancelled = true;
//       clearTimeout(t);
//     };
//   }, []);
//
// toggle:
//   function toggle(id: string) {
//     setHabits((prev) =>
//       prev.map((h) => (h.id === id ? { ...h, doneToday: !h.doneToday } : h)),
//     );
//   }
//
// Loading branch:
//   if (loading) {
//     return (
//       <SafeAreaView style={styles.center}>
//         <ActivityIndicator size="large" />
//       </SafeAreaView>
//     );
//   }
//
// Content branch: see TODO (5) above — it's already written out for you there.
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// This screen is the skeleton of the mini-project, and the skeleton of nearly
// every data screen you will ever build: load on mount, show a spinner, render
// a virtualized list, handle empty, mutate immutably. In Week 9 the only thing
// that changes is the data source — the setTimeout becomes a real fetch to the
// Spring Boot API, and `loading`/empty grow an `error` sibling. The SHAPE you
// build here carries all the way to the capstone.
//
// ----------------------------------------------------------------------------
