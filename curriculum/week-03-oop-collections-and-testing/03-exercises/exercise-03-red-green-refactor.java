// Exercise 3 — Red-green-refactor a StreakCalculator
//
// Goal: Build a feature TEST-FIRST. The tests below are written for you and
//       describe a `StreakCalculator` that does not exist yet. Your job is to
//       make them pass the disciplined way:
//
//         1. RED:      run `mvn test` and watch it fail to compile / fail.
//         2. GREEN:    write the MINIMUM code to pass the next test.
//         3. REFACTOR: clean up behind the green bar, rerun, stay green.
//
//       Work the tests roughly top to bottom. Resist the urge to write the whole
//       algorithm at once — write only what the failing test in front of you
//       demands. That is the entire point of the drill.
//
//       The streak rule: given a set of dates on which a habit was COMPLETED,
//       `longest(...)` returns the length of the longest run of consecutive
//       calendar days. Duplicates count once. Order of input does not matter.
//
// Estimated time: 50 minutes.
//
// HOW TO USE THIS FILE
//
//   1. Reuse the week-1 Maven project (junit-jupiter + assertj-core, test scope).
//   2. Create an EMPTY production class to start the red phase:
//        src/main/java/com/crunch/tracker/StreakCalculator.java
//          package com.crunch.tracker;
//          public final class StreakCalculator { }
//      (You'll grow it test-by-test. Don't pre-write the algorithm.)
//   3. Put THIS file's test class under src/test/java/com/crunch/tracker/.
//   4. Run `mvn test` repeatedly. Goal:
//        [INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
//        [INFO] BUILD SUCCESS
//
// ACCEPTANCE CRITERIA
//   [ ] You built StreakCalculator incrementally (your git log shows commits
//       moving red -> green; e.g. "test: empty streak", "feat: handle gaps").
//   [ ] mvn test: BUILD SUCCESS, all 8 tests pass.
//   [ ] The final implementation uses a Set/SortedSet to handle dedup + ordering
//       (see the collections lecture) rather than hand-sorting a List.
//   [ ] REFACTOR step: extract a `Streaks` interface and have StreakCalculator
//       implement it (the last test pins this down).
//
// Hints at the bottom. Don't peek until you've tried for 15 minutes.

package com.crunch.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StreakCalculator")
class StreakCalculatorTest {

    // A small helper so the dates below read cleanly.
    private static LocalDate d(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    @Nested
    @DisplayName("base cases")
    class BaseCases {

        @Test
        @DisplayName("no completed days -> streak of 0")
        void empty() {
            // RED FIRST: StreakCalculator doesn't exist yet. Create it empty,
            // then add `public int longest(List<LocalDate> dates)`.
            var calc = new StreakCalculator();
            assertThat(calc.longest(List.of())).isZero();
        }

        @Test
        @DisplayName("one completed day -> streak of 1")
        void single() {
            var calc = new StreakCalculator();
            assertThat(calc.longest(List.of(d(6, 1)))).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("consecutive runs")
    class Consecutive {

        @Test
        @DisplayName("three consecutive days -> streak of 3")
        void threeInARow() {
            var calc = new StreakCalculator();
            assertThat(calc.longest(List.of(d(6, 1), d(6, 2), d(6, 3)))).isEqualTo(3);
        }

        @Test
        @DisplayName("input order does not matter")
        void unsortedInput() {
            var calc = new StreakCalculator();
            assertThat(calc.longest(List.of(d(6, 3), d(6, 1), d(6, 2)))).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("gaps and duplicates")
    class GapsAndDuplicates {

        @Test
        @DisplayName("a gap resets the run; the longest run wins")
        void gapResets() {
            var calc = new StreakCalculator();
            // run of 2 (6/1, 6/2), gap, then run of 3 (6/5, 6/6, 6/7)
            var dates = List.of(d(6, 1), d(6, 2), d(6, 5), d(6, 6), d(6, 7));
            assertThat(calc.longest(dates)).isEqualTo(3);
        }

        @Test
        @DisplayName("duplicate dates are counted once")
        void duplicatesCountOnce() {
            var calc = new StreakCalculator();
            var dates = List.of(d(6, 1), d(6, 1), d(6, 2));
            assertThat(calc.longest(dates)).isEqualTo(2);
        }

        @Test
        @DisplayName("a run that spans a month boundary still counts")
        void acrossMonthBoundary() {
            var calc = new StreakCalculator();
            // June has 30 days: 6/29, 6/30, 7/1 is a 3-day run.
            var dates = List.of(d(6, 29), d(6, 30), d(7, 1));
            assertThat(calc.longest(dates)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("refactor: program to an interface")
    class ProgramToInterface {

        @Test
        @DisplayName("StreakCalculator implements the Streaks interface")
        void implementsInterface() {
            // REFACTOR STEP. Extract this interface and have StreakCalculator
            // implement it, so callers (and next week's Spring service) can
            // depend on the contract, not the concrete class:
            //
            //   public interface Streaks {
            //       int longest(java.util.List<java.time.LocalDate> dates);
            //   }
            //
            // This test just proves the seam exists.
            Streaks streaks = new StreakCalculator();
            assertThat(streaks.longest(List.of(d(6, 1), d(6, 2)))).isEqualTo(2);
        }
    }
}

// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// The red-green progression, step by step:
//
//   STEP 1 (empty + single): the dumbest passing code is enough.
//       public int longest(List<LocalDate> dates) {
//           return dates.isEmpty() ? 0 : 1;
//       }
//
//   STEP 2 (consecutive + unsorted + gaps + dups): now the real algorithm is
//   justified. Use a TreeSet to get sorted, de-duplicated dates for free
//   (collections lecture, section 3) — that single choice kills the unsorted
//   case and the duplicate case at once:
//
//       import java.time.LocalDate;
//       import java.util.List;
//       import java.util.NavigableSet;
//       import java.util.TreeSet;
//
//       public final class StreakCalculator implements Streaks {
//           @Override
//           public int longest(List<LocalDate> dates) {
//               NavigableSet<LocalDate> days = new TreeSet<>(dates);
//               int best = 0, run = 0;
//               LocalDate prev = null;
//               for (LocalDate day : days) {              // iterates in date order
//                   run = (prev != null && day.equals(prev.plusDays(1))) ? run + 1 : 1;
//                   best = Math.max(best, run);
//                   prev = day;
//               }
//               return best;
//           }
//       }
//
//   STEP 3 (refactor): extract the interface.
//
//       public interface Streaks {
//           int longest(java.util.List<java.time.LocalDate> dates);
//       }
//
//   Note plusDays(1) handles the month-boundary case correctly (LocalDate knows
//   June has 30 days), so acrossMonthBoundary passes with no special handling.
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// You just watched a clean algorithm EMERGE from a sequence of failing tests
// instead of being guessed up front. Every edge case (unsorted input, gaps,
// duplicates, month boundaries) is now permanently guarded — change the code and
// the bar tells you instantly if you broke one. That safety net is what makes
// the refactor at the end (extracting Streaks) feel free rather than scary.
//
// The mini-project asks you to test-drive the whole Crunch Tracker domain this
// way. This exercise is the rep that makes that feel natural.
//
// ----------------------------------------------------------------------------
