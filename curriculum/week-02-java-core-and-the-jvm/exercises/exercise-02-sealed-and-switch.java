// Exercise 2 — Sealed types and the pattern-matching switch
//
// Goal: Model a CLOSED set of check-in events with a sealed interface and
//       records, then process them with ONE exhaustive pattern-matching
//       switch — no instanceof ladders, no `default` branch. The compiler
//       must be able to prove your switch is total.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
//   1. In a Maven project targeting Java 21 (see exercise 1 for the pom.xml),
//      drop this file at:
//
//        src/main/java/com/crunch/tracker/CheckInEvents.java
//
//   2. Fill in every body marked `// TODO`. Do NOT change the public surface
//      (the sealed interface, the record shapes, the method signatures). The
//      main() at the bottom exercises the code you fill in; wire it correctly
//      and `mvn compile exec:java -Dexec.mainClass=com.crunch.tracker.CheckInEvents`
//      prints the expected output shown near the bottom of this file.
//
//   3. Build clean: `mvn -q compile` with -Xlint:all on. A warning is a bug.
//
// ACCEPTANCE CRITERIA
//
//   [ ] All TODOs implemented.
//   [ ] `mvn compile`: BUILD SUCCESS, 0 warnings.
//   [ ] `score(...)` is a single pattern-matching switch with NO `default`
//       branch. It compiles only because CheckInEvent is sealed and you
//       covered every permitted subtype.
//   [ ] `summarize(...)` uses a record DECONSTRUCTION pattern at least once.
//   [ ] At least one case uses a `when` GUARD.
//   [ ] No `instanceof` followed by a cast anywhere.
//
// PROVE EXHAUSTIVENESS: after it compiles, temporarily add a fourth record
// (e.g. `record Excused(...) implements CheckInEvent {}`) to the permits list
// and watch `score` FAIL TO COMPILE with "switch does not cover all possible
// input values." That compile error is the entire point of sealed types.
// Remove the fourth type to get back to green.

package com.crunch.tracker;

import java.time.LocalDate;
import java.util.List;

public final class CheckInEvents {

    // -------------------------------------------------------------------------
    // Domain: a CLOSED set of check-in outcomes.
    // -------------------------------------------------------------------------

    sealed interface CheckInEvent permits Completed, Skipped, Partial {
        long habitId();
        LocalDate date();
    }

    record Completed(long habitId, LocalDate date) implements CheckInEvent {}

    record Skipped(long habitId, LocalDate date, String reason) implements CheckInEvent {}

    record Partial(long habitId, LocalDate date, int done, int target) implements CheckInEvent {
        Partial {
            // TODO: validate in this compact constructor:
            //   - target must be > 0
            //   - done must be in the range 0..target (inclusive)
            // Throw IllegalArgumentException with a clear message otherwise.
            throw new UnsupportedOperationException("TODO: validate done/target");
        }

        double fraction() {
            return (double) done / target;
        }
    }

    enum Score { FULL, PARTIAL, NONE }

    // -------------------------------------------------------------------------
    // Behavior: total functions over the sealed hierarchy.
    // -------------------------------------------------------------------------

    /**
     * Score an event:
     *   - Completed            -> FULL
     *   - Skipped              -> NONE
     *   - Partial with done==0 -> NONE   (use a `when` guard)
     *   - any other Partial    -> PARTIAL
     *
     * MUST be a single switch expression. MUST have NO `default` branch —
     * exhaustiveness comes from CheckInEvent being sealed.
     */
    static Score score(CheckInEvent event) {
        // TODO: replace with a pattern-matching switch expression.
        //       Hint: `case Partial p when p.done() == 0 -> Score.NONE`
        throw new UnsupportedOperationException("TODO: implement score with a switch");
    }

    /**
     * One-line human-readable summary of an event. Use a record DECONSTRUCTION
     * pattern for at least one case, e.g.:
     *
     *   case Completed(long id, var date) -> "habit " + id + " done on " + date
     *
     * Examples (formats up to you, but make them readable and distinct):
     *   Completed -> "habit 1 completed on 2026-06-10"
     *   Skipped   -> "habit 2 skipped on 2026-06-10 (sick)"
     *   Partial   -> "habit 3 partial 2/5 on 2026-06-10"
     */
    static String summarize(CheckInEvent event) {
        // TODO: a switch expression using deconstruction patterns. No default.
        throw new UnsupportedOperationException("TODO: implement summarize");
    }

    /**
     * Count how many events in the log scored FULL. Reuse score(). A simple
     * loop is fine (week 3 brings the Stream API).
     */
    static long countFull(List<CheckInEvent> log) {
        // TODO: count events whose score(...) == Score.FULL
        throw new UnsupportedOperationException("TODO: implement countFull");
    }

    // -------------------------------------------------------------------------
    // Driver
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        LocalDate day = LocalDate.parse("2026-06-10");

        List<CheckInEvent> log = List.of(
            new Completed(1L, day),
            new Skipped(2L, day, "sick"),
            new Partial(3L, day, 2, 5),
            new Partial(4L, day, 0, 5),
            new Completed(5L, day)
        );

        for (CheckInEvent e : log) {
            System.out.printf("%-8s  %s%n", score(e), summarize(e));
        }
        System.out.println("FULL count: " + countFull(log));
    }

    // -------------------------------------------------------------------------
    // Expected output (your summarize format may differ slightly)
    // -------------------------------------------------------------------------
    //
    // FULL      habit 1 completed on 2026-06-10
    // NONE      habit 2 skipped on 2026-06-10 (sick)
    // PARTIAL   habit 3 partial 2/5 on 2026-06-10
    // NONE      habit 4 partial 0/5 on 2026-06-10
    // FULL      habit 5 completed on 2026-06-10
    // FULL count: 2
    //
    // -------------------------------------------------------------------------
    // HINTS (read only if stuck > 15 min)
    // -------------------------------------------------------------------------
    //
    // Partial compact constructor:
    //   Partial {
    //       if (target <= 0)            throw new IllegalArgumentException("target must be > 0");
    //       if (done < 0 || done > target)
    //                                   throw new IllegalArgumentException("done must be 0..target");
    //   }
    //
    // score:
    //   static Score score(CheckInEvent event) {
    //       return switch (event) {
    //           case Completed c                 -> Score.FULL;
    //           case Skipped s                   -> Score.NONE;
    //           case Partial p when p.done() == 0 -> Score.NONE;
    //           case Partial p                   -> Score.PARTIAL;
    //       };
    //   }
    //
    // summarize (deconstruction patterns):
    //   static String summarize(CheckInEvent event) {
    //       return switch (event) {
    //           case Completed(long id, var date)            -> "habit " + id + " completed on " + date;
    //           case Skipped(long id, var date, var reason)  -> "habit " + id + " skipped on " + date + " (" + reason + ")";
    //           case Partial(long id, var date, var d, var t) -> "habit " + id + " partial " + d + "/" + t + " on " + date;
    //       };
    //   }
    //
    // countFull:
    //   static long countFull(List<CheckInEvent> log) {
    //       long n = 0;
    //       for (CheckInEvent e : log) if (score(e) == Score.FULL) n++;
    //       return n;
    //   }
    //
    // -------------------------------------------------------------------------
    // WHY THIS MATTERS
    // -------------------------------------------------------------------------
    //
    // The combination "sealed interface + records + exhaustive switch" is the
    // backbone of safe domain modeling in modern Java. The compiler becomes
    // your checklist: add a new kind of event and EVERY switch that processes
    // events stops compiling until you handle it. An instanceof ladder gives
    // you none of that — it fails at run time, in production, when you forgot a
    // case. You will use this exact pattern in the mini-project and again in
    // every Spring week when you model API request/response variants.
    // -------------------------------------------------------------------------
}
