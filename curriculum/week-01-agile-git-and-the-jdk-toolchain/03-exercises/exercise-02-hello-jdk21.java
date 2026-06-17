// Exercise 2 — Hello, JDK 21
//
// Goal: Prove your JDK 21 toolchain works end to end by running a SINGLE Java
//       file directly with `java`, no `javac` step and no Maven. Along the way
//       you meet a few Java 21 idioms you'll lean on all course: records,
//       a switch expression, text blocks, and the enhanced `main`.
//
// Estimated time: 35 minutes.
//
// HOW TO USE THIS FILE
//
//   1. Make sure JDK 21 is active:
//
//        java -version          # must say 21.x
//
//   2. Run this file DIRECTLY — single-file source-code launch (JEP 330/458)
//      compiles it in memory and runs it; there is no .class file:
//
//        java exercise-02-hello-jdk21.java
//        java exercise-02-hello-jdk21.java Ada
//
//   3. Fill in the bodies marked `// TODO`. Do NOT change the method
//      signatures or the record shapes — the main() below exercises exactly
//      what you implement. When all TODOs are correct, the program prints the
//      expected output shown at the bottom of this file.
//
//   4. There must be no compiler warnings and no exceptions at runtime.
//
// ACCEPTANCE CRITERIA
//
//   [ ] `java -version` reports 21.
//   [ ] All TODOs implemented; no signature or record shape changed.
//   [ ] `java exercise-02-hello-jdk21.java` runs with no exception.
//   [ ] Output matches the "Expected output" block at the bottom.
//   [ ] `classify` is a SWITCH EXPRESSION (arrow form), not an if/else ladder.
//   [ ] `summary` uses a TEXT BLOCK (the triple-quoted """ ... """ form).
//
// Inline hints are at the bottom of the file. Don't peek until you've tried
// for at least 15 minutes.

import java.util.List;

public class HelloJdk21 {

    // ------------------------------------------------------------------------
    // Domain: a tiny preview of the Crunch Tracker model you build for real
    // in Week 2. A Habit has a name and how often you intend to do it.
    // ------------------------------------------------------------------------

    enum Frequency { DAILY, WEEKLY, MONTHLY }

    // A record is an immutable data carrier: the compiler generates the
    // constructor, accessors (name(), frequency()), equals/hashCode, toString.
    record Habit(String name, Frequency frequency) {

        // Compact canonical constructor: validate invariants once, here.
        Habit {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("habit name must not be blank");
            }
        }
    }

    // ------------------------------------------------------------------------
    // Functions to implement
    // ------------------------------------------------------------------------

    /**
     * Turn a Frequency into a human cadence phrase using a SWITCH EXPRESSION
     * (the arrow form, no `break`). Map:
     *   DAILY   -> "every day"
     *   WEEKLY  -> "every week"
     *   MONTHLY -> "every month"
     *
     * The switch must be exhaustive over the enum (no `default` needed when
     * you cover every constant — that's the point of switching on an enum).
     */
    static String classify(Frequency f) {
        // TODO: replace this with a switch expression on f.
        throw new UnsupportedOperationException("classify not implemented");
    }

    /**
     * Greet a name, falling back to a generic greeting when blank/null.
     *   "Ada"  -> "Hello, Ada!"
     *   "  "   -> "Hello, Crunch Tracker!"
     *   null   -> "Hello, Crunch Tracker!"
     */
    static String greeting(String name) {
        // TODO: handle null/blank, otherwise greet the trimmed name.
        throw new UnsupportedOperationException("greeting not implemented");
    }

    /**
     * Produce a multi-line summary of the habits using a TEXT BLOCK.
     * For the two habits in main(), it must produce EXACTLY:
     *
     * Tracking 2 habits:
     *   - Drink water (every day)
     *   - Review goals (every week)
     *
     * Build the two body lines from the list (one per habit, two-space indent,
     * "- <name> (<cadence>)"), then place them into a text block.
     */
    static String summary(List<Habit> habits) {
        // TODO: build the per-habit lines (use classify() for the cadence),
        //       then return a text block "Tracking N habits:\n<lines>".
        throw new UnsupportedOperationException("summary not implemented");
    }

    // ------------------------------------------------------------------------
    // Driver
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
        String who = args.length > 0 ? args[0] : "";

        System.out.println(greeting(who));
        System.out.println("Running on Java " + System.getProperty("java.version"));
        System.out.println();

        List<Habit> habits = List.of(
            new Habit("Drink water", Frequency.DAILY),
            new Habit("Review goals", Frequency.WEEKLY)
        );

        System.out.print(summary(habits));
    }
}

// ----------------------------------------------------------------------------
// Expected output  (with NO command-line argument)
// ----------------------------------------------------------------------------
//
// Hello, Crunch Tracker!
// Running on Java 21.0.5
//
// Tracking 2 habits:
//   - Drink water (every day)
//   - Review goals (every week)
//
// (With an argument, e.g. `java exercise-02-hello-jdk21.java Ada`, the first
//  line becomes "Hello, Ada!". Your Java patch version may differ from .0.5.)
//
// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// classify:
//   static String classify(Frequency f) {
//       return switch (f) {
//           case DAILY   -> "every day";
//           case WEEKLY  -> "every week";
//           case MONTHLY -> "every month";
//       };
//   }
//
// greeting:
//   static String greeting(String name) {
//       return (name == null || name.isBlank())
//           ? "Hello, Crunch Tracker!"
//           : "Hello, " + name.trim() + "!";
//   }
//
// summary (one approach using a StringBuilder + a text block frame):
//   static String summary(List<Habit> habits) {
//       StringBuilder lines = new StringBuilder();
//       for (Habit h : habits) {
//           lines.append("  - ")
//                .append(h.name())
//                .append(" (").append(classify(h.frequency())).append(")")
//                .append(System.lineSeparator());
//       }
//       return """
//              Tracking %d habits:
//              %s""".formatted(habits.size(), lines.toString());
//   }
//
// Note on text blocks: the closing """ position sets the left margin that the
// compiler strips. Keep the closing delimiter aligned with the content.
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// Single-file launch (JEP 330/458) is the fastest way to confirm a JDK is
// healthy and to try a snippet — no project, no build tool. Records, switch
// expressions, and text blocks are not exotic; they are the default modern
// Java you write from Week 2 on. Getting them under your fingers now means
// the Spring weeks read like Java, not like a foreign dialect.
//
// ----------------------------------------------------------------------------
