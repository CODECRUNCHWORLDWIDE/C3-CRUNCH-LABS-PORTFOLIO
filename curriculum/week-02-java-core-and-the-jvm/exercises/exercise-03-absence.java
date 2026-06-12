// Exercise 3 — Representing absence: null, Optional, and exceptions
//
// Goal: This file is intentionally full of null-returning and null-dereferencing
//       code — the 2008-Java way to say "no value." Replace every "return null"
//       that means "absent" with Optional<T>, and decide deliberately where an
//       EXCEPTION is the right answer instead of an Optional. The point is to
//       make absence a TYPED, visible thing the caller cannot ignore — not a
//       null landmine that blows up later as a NullPointerException.
//
// Estimated time: 35 minutes.
//
// HOW TO USE THIS FILE
//
//   In a Maven project targeting Java 21 (see exercise 1), drop this file at:
//
//     src/main/java/com/crunch/tracker/HabitDirectory.java
//
//   Fix the methods marked `// TODO`. Then run:
//
//     mvn -q compile exec:java -Dexec.mainClass=com.crunch.tracker.HabitDirectory
//
//   The final program must:
//     - Build with BUILD SUCCESS and 0 warnings (-Xlint:all is on).
//     - Run and print the expected lines at the bottom.
//     - Contain ZERO `return null` and ZERO methods that can NPE the caller.
//
// ACCEPTANCE CRITERIA
//
//   [ ] mvn compile: BUILD SUCCESS, 0 warnings.
//   [ ] mvn exec:java prints the expected lines (see bottom).
//   [ ] `findById` returns Optional<Habit>, never null.
//   [ ] `displayName` consumes an Optional WITHOUT calling .get() blindly
//       (use map / orElse / orElseGet / ifPresentOrElse).
//   [ ] `requireById` THROWS for a missing id (this is the deliberate
//       exception-vs-Optional choice — see WHY THIS MATTERS at the bottom).
//   [ ] No `optional.isPresent()` followed by `optional.get()` — use the
//       combinators instead.

package com.crunch.tracker;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class HabitDirectory {

    record Habit(long id, String name, int currentStreak) {}

    private final List<Habit> habits;

    HabitDirectory(List<Habit> habits) {
        this.habits = List.copyOf(habits);   // defensive, immutable copy
    }

    // -------------------------------------------------------------------------
    // Lookups
    // -------------------------------------------------------------------------

    /**
     * Find a habit by id. "Not found" is a NORMAL, expected outcome here, so it
     * should be represented as an EMPTY Optional, not null and not an exception.
     *
     * TODO: change the return type to Optional<Habit> and return the first
     *       matching habit wrapped in an Optional, or Optional.empty().
     */
    Habit findById(long id) {
        for (Habit h : habits) {
            if (h.id() == id) return h;
        }
        return null;   // TODO: this null is the bug. Make absence typed.
    }

    /**
     * Look up a habit you EXPECT to exist (a programming error if it doesn't).
     * Here, absence is NOT a normal outcome — it means the caller asked for an
     * id that should be valid. This is the case where throwing is correct.
     *
     * TODO: implement by delegating to findById(...) and throwing a
     *       NoSuchElementException with a useful message if absent.
     *       Hint: Optional has orElseThrow(...).
     */
    Habit requireById(long id) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement requireById");
    }

    // -------------------------------------------------------------------------
    // Presentation — must consume Optional WITHOUT a blind .get()
    // -------------------------------------------------------------------------

    /**
     * Return a display string for a habit id:
     *   - found    -> the habit's name
     *   - not found -> "(unknown habit)"
     *
     * TODO: consume findById(id) using Optional combinators (map + orElse),
     *       NOT isPresent()/get().
     */
    String displayName(long id) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement displayName");
    }

    /**
     * Return the streak for a habit id, defaulting to 0 when the habit is
     * absent. Use Optional.map(...).orElse(...).
     *
     * TODO: implement with Optional combinators.
     */
    int streakOf(long id) {
        // TODO
        throw new UnsupportedOperationException("TODO: implement streakOf");
    }

    // -------------------------------------------------------------------------
    // Driver
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        var dir = new HabitDirectory(List.of(
            new Habit(1L, "Read 20 pages", 8),
            new Habit(2L, "Run 5k", 3),
            new Habit(3L, "Meditate", 0)
        ));

        System.out.println("findById(2) present? " + dir.findById(2L).isPresent()); // true
        System.out.println("findById(99) present? " + dir.findById(99L).isPresent()); // false

        System.out.println("displayName(1):  " + dir.displayName(1L));   // Read 20 pages
        System.out.println("displayName(99): " + dir.displayName(99L));  // (unknown habit)

        System.out.println("streakOf(1):  " + dir.streakOf(1L));         // 8
        System.out.println("streakOf(99): " + dir.streakOf(99L));        // 0

        // requireById on a known id returns it; on an unknown id it throws.
        System.out.println("requireById(2): " + dir.requireById(2L).name()); // Run 5k
        try {
            dir.requireById(99L);
        } catch (NoSuchElementException ex) {
            System.out.println("requireById(99) threw: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Expected output
    // -------------------------------------------------------------------------
    //
    // findById(2) present? true
    // findById(99) present? false
    // displayName(1):  Read 20 pages
    // displayName(99): (unknown habit)
    // streakOf(1):  8
    // streakOf(99): 0
    // requireById(2): Run 5k
    // requireById(99) threw: no habit with id 99
    //
    // -------------------------------------------------------------------------
    // HINTS (read only if stuck > 15 min)
    // -------------------------------------------------------------------------
    //
    // findById:
    //   Optional<Habit> findById(long id) {
    //       for (Habit h : habits) if (h.id() == id) return Optional.of(h);
    //       return Optional.empty();
    //   }
    //
    // requireById:
    //   Habit requireById(long id) {
    //       return findById(id)
    //           .orElseThrow(() -> new NoSuchElementException("no habit with id " + id));
    //   }
    //
    // displayName:
    //   String displayName(long id) {
    //       return findById(id).map(Habit::name).orElse("(unknown habit)");
    //   }
    //
    // streakOf:
    //   int streakOf(long id) {
    //       return findById(id).map(Habit::currentStreak).orElse(0);
    //   }
    //
    // -------------------------------------------------------------------------
    // WHY THIS MATTERS — Optional vs exception, the actual decision
    // -------------------------------------------------------------------------
    //
    // Use Optional<T> when absence is a NORMAL, expected outcome the caller
    // must handle — "is there a habit with this id?" Often there isn't, and
    // that's fine. The empty Optional forces the caller to deal with it; they
    // cannot accidentally dereference a null.
    //
    // Use an EXCEPTION when absence means a PROGRAMMING ERROR or a broken
    // invariant — "give me the habit I already know exists." If it's missing,
    // something upstream is wrong, and you want a loud failure with a stack
    // trace, not a silently-ignored empty value.
    //
    // Never use `null` to mean "absent" in an API you control. A method that
    // can return null is a method every caller can forget to null-check, and
    // the failure shows up far from the cause as a NullPointerException. The
    // helpful NPE message ("because X is null") tells you WHERE it blew up, not
    // WHO returned the null. Optional makes absence part of the type — the
    // compiler and the reader both see it. That is the whole point.
    // -------------------------------------------------------------------------
}
