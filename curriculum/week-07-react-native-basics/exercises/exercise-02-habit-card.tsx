// Exercise 2 — The HabitCard component
//
// Goal: Build a typed, reusable presentational component using props,
//       conditional rendering, Pressable with a proper touch target, and
//       StyleSheet. No `any`, no LogBox warnings.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
// 1. In your Exercise 1 app (or a fresh `blank-typescript` app), save this as
//    `components/HabitCard.tsx`.
//
// 2. Fill in the bodies marked `// TODO`. Do not change the public surface
//    (the Habit interface, the HabitCardProps interface, or the component
//    signature). The HabitCardDemo at the bottom exercises the component;
//    render <HabitCardDemo /> from App.tsx to see it.
//
// 3. Type-check with zero errors:  npx tsc --noEmit
//    Run it and confirm there are no LogBox warnings on the device.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `npx tsc --noEmit`: 0 errors. No `any` anywhere.
//   [ ] Rendering <HabitCardDemo /> shows three cards: a not-done daily habit,
//       a done daily habit (visibly distinct), and a habit with a streak badge.
//   [ ] Pressing a card's check control calls onToggle with the habit id.
//   [ ] The check control is at least 44x44 and has an accessibilityLabel.
//   [ ] The streak badge renders ONLY when streak > 0 (and never renders a 0).
//   [ ] No LogBox warnings when the demo renders.
//
// Inline hints are at the bottom of the file. Don't peek until you've tried
// for at least 15 minutes.

import { StyleSheet, Text, View, Pressable, SafeAreaView } from "react-native";

// ----------------------------------------------------------------------------
// Domain type
// ----------------------------------------------------------------------------

export interface Habit {
  id: string;
  title: string;
  cadence: "daily" | "weekly";
  streak: number;
  doneToday: boolean;
}

// ----------------------------------------------------------------------------
// Component
// ----------------------------------------------------------------------------

export interface HabitCardProps {
  habit: Habit;
  onToggle: (id: string) => void;
}

export function HabitCard({ habit, onToggle }: HabitCardProps) {
  // TODO (1): Derive a boolean `done` from habit.doneToday for readability.

  // TODO (2): Return a card. Structure:
  //   <View style={[styles.card, done && styles.cardDone]}>
  //     <Pressable ...the check control... >
  //       <Text>{done ? "✓" : "○"}</Text>
  //     </Pressable>
  //     <View style={styles.body}>
  //       <Text style={styles.title}>{habit.title}</Text>
  //       <Text style={styles.cadence}>{habit.cadence}</Text>
  //     </View>
  //     {/* streak badge — only when streak > 0 */}
  //   </View>
  //
  // Requirements for the Pressable check control:
  //   - onPress calls onToggle(habit.id)
  //   - style uses styles.check (which is >= 44x44)
  //   - accessibilityRole="button"
  //   - accessibilityLabel describes the action, e.g.
  //       `Mark ${habit.title} ${done ? "not done" : "done"}`
  //
  // Requirement for the streak badge:
  //   - render <View style={styles.badge}><Text style={styles.badgeText}>🔥 {habit.streak}</Text></View>
  //     ONLY when habit.streak > 0. Use `habit.streak > 0 && (...)` — never a
  //     bare number on the left of &&.

  return (
    // TODO: replace this placeholder with the card described above.
    <View style={styles.card}>
      <Text>TODO: implement HabitCard</Text>
    </View>
  );
}

// ----------------------------------------------------------------------------
// Styles
// ----------------------------------------------------------------------------

const styles = StyleSheet.create({
  card: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#fff",
    borderRadius: 12,
    padding: 12,
    marginHorizontal: 16,
    marginVertical: 6,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 3,
    elevation: 2,
  },
  cardDone: {
    backgroundColor: "#EAFBEF",
  },
  check: {
    minWidth: 44,
    minHeight: 44,
    borderRadius: 22,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#F1F3F5",
  },
  checkText: {
    fontSize: 20,
    color: "#0B5",
  },
  body: {
    flex: 1,
    marginHorizontal: 12,
  },
  title: {
    fontSize: 17,
    fontWeight: "600",
    color: "#111",
  },
  cadence: {
    fontSize: 13,
    color: "#888",
    marginTop: 2,
    textTransform: "capitalize",
  },
  badge: {
    backgroundColor: "#FFF1E6",
    borderRadius: 10,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  badgeText: {
    fontSize: 13,
    fontWeight: "600",
    color: "#C2410C",
  },
  screen: {
    flex: 1,
    backgroundColor: "#F8F9FA",
  },
});

// ----------------------------------------------------------------------------
// Demo driver — render <HabitCardDemo /> from App.tsx
// ----------------------------------------------------------------------------

import { useState } from "react";

export function HabitCardDemo() {
  const [habits, setHabits] = useState<Habit[]>([
    { id: "1", title: "Drink water", cadence: "daily", streak: 0, doneToday: false },
    { id: "2", title: "Stretch", cadence: "daily", streak: 4, doneToday: true },
    { id: "3", title: "Review goals", cadence: "weekly", streak: 12, doneToday: false },
  ]);

  function toggle(id: string) {
    setHabits((prev) =>
      prev.map((h) => (h.id === id ? { ...h, doneToday: !h.doneToday } : h)),
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      {habits.map((h) => (
        <HabitCard key={h.id} habit={h} onToggle={toggle} />
      ))}
    </SafeAreaView>
  );
}

// ----------------------------------------------------------------------------
// Expected behavior (what you should see and be able to do)
// ----------------------------------------------------------------------------
//
//  - Three cards stacked vertically inside the safe area.
//  - Card 1 "Drink water": circle (○) on the left, no streak badge.
//  - Card 2 "Stretch": check (✓), tinted green background, 🔥 4 badge.
//  - Card 3 "Review goals": circle (○), 🔥 12 badge, "weekly" cadence.
//  - Tapping the left control on any card flips ✓/○ AND toggles the green
//    tint, because state updates immutably and the component re-renders.
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// done:
//   const done = habit.doneToday;
//
// The full return:
//   return (
//     <View style={[styles.card, done && styles.cardDone]}>
//       <Pressable
//         onPress={() => onToggle(habit.id)}
//         style={styles.check}
//         accessibilityRole="button"
//         accessibilityLabel={`Mark ${habit.title} ${done ? "not done" : "done"}`}
//         hitSlop={8}
//       >
//         <Text style={styles.checkText}>{done ? "✓" : "○"}</Text>
//       </Pressable>
//
//       <View style={styles.body}>
//         <Text style={styles.title}>{habit.title}</Text>
//         <Text style={styles.cadence}>{habit.cadence}</Text>
//       </View>
//
//       {habit.streak > 0 && (
//         <View style={styles.badge}>
//           <Text style={styles.badgeText}>🔥 {habit.streak}</Text>
//         </View>
//       )}
//     </View>
//   );
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// HabitCard is a "presentational" component: it owns no state, takes its data
// as a prop, and reports interaction through a callback (onToggle). That makes
// it trivially reusable and testable. The parent (HabitCardDemo here; the
// real screen in the mini-project) owns the state and decides what a toggle
// MEANS. This props-down / events-up split is the spine of every React app —
// internalize it now and the mini-project writes itself.
//
// ----------------------------------------------------------------------------
