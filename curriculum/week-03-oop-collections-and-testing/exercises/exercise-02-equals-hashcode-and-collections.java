// Exercise 2 — equals/hashCode and collections
//
// Goal: Two bugs hide in this file, both of the kind that compile cleanly,
//       pass a careless glance, and then lose your data at run time.
//
//        BUG 1: `Tag` overrides equals() but NOT hashCode(). This violates the
//               equals/hashCode contract, so a HashSet<Tag> treats two "equal"
//               tags as different and lets duplicates in.
//
//        BUG 2: `HabitGroup` is meant to be immutable, but it stores the caller's
//               List by reference. Mutating the original list corrupts the group.
//
//       Fix both. The tests below pin down the correct behavior. Make them green
//       WITHOUT changing the test file's assertions.
//
// Estimated time: 45 minutes.
//
// HOW TO USE THIS FILE
//
//   1. Use the Maven project from week 1 (or `mvn archetype:generate` a fresh one).
//      Ensure the pom has junit-jupiter and assertj-core at <scope>test</scope>
//      (versions are in the mini-project README).
//
//   2. Put the production classes (Tag, HabitGroup) under
//        src/main/java/com/crunch/tracker/
//      and this whole file's test class under
//        src/test/java/com/crunch/tracker/
//      OR, simplest: drop everything in src/test/java/com/crunch/tracker/ for the
//      drill — the classes are tiny and self-contained here on purpose.
//
//   3. Run `mvn test`. You'll see failures. Fix the two TODO sites in the
//      production classes until you get:
//        [INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
//        [INFO] BUILD SUCCESS
//
// ACCEPTANCE CRITERIA
//   [ ] mvn test: BUILD SUCCESS, 0 failures, 0 errors.
//   [ ] `Tag` obeys the equals/hashCode contract (equal Tags share a hashCode).
//   [ ] A HashSet<Tag> de-duplicates by tag name, case-insensitively.
//   [ ] `HabitGroup` is genuinely immutable: mutating the source list after
//       construction does NOT change the group, and group.habits() is unmodifiable.
//   [ ] You did NOT edit any assertion in HabitCollectionsTest.
//
// Inline hints are at the bottom. Don't peek until you've tried for 15 minutes.

package com.crunch.tracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ----------------------------------------------------------------------------
// PRODUCTION CLASS #1 — Tag
//
// A category tag, compared case-insensitively by name ("Fitness" == "fitness").
// ----------------------------------------------------------------------------

final class Tag {
    private final String name;

    Tag(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag other)) return false;
        return name.equalsIgnoreCase(other.name);
    }

    // BUG 1: equals() above compares case-insensitively, but there is NO
    //        hashCode() override here. The inherited Object.hashCode() is
    //        identity-based, so two equal Tags ("Fitness" / "fitness") get
    //        DIFFERENT hash codes and a HashSet stores both. That breaks the
    //        equals/hashCode contract: equal objects MUST have equal hashCodes.
    //
    // TODO: Add a hashCode() that is consistent with equals(). Hint: the field
    //       used in equals() is `name`, compared case-insensitively — so hash on
    //       a case-normalized form of name.
}

// ----------------------------------------------------------------------------
// PRODUCTION CLASS #2 — HabitGroup
//
// A named, IMMUTABLE bundle of habit names. Once constructed, it must never
// change, no matter what the caller does to the list they passed in.
// ----------------------------------------------------------------------------

final class HabitGroup {
    private final String name;
    private final List<String> habits;

    HabitGroup(String name, List<String> habits) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        // BUG 2: stores the caller's list by reference. If the caller mutates
        //        their list afterward, this "immutable" group changes too.
        //        And habits() below hands the internal list straight back, so a
        //        caller can mutate it directly.
        //
        // TODO: take a defensive, unmodifiable copy here. Hint: List.copyOf(...).
        this.habits = habits;
    }

    String name() {
        return name;
    }

    List<String> habits() {
        // TODO: once the constructor takes a List.copyOf, this returns an
        //       unmodifiable list automatically — no change needed here. But if
        //       you chose a different fix, make sure what you return cannot be
        //       mutated by the caller.
        return habits;
    }
}

// ----------------------------------------------------------------------------
// THE TESTS — do not edit the assertions. Make them pass by fixing the bugs.
// ----------------------------------------------------------------------------

@DisplayName("Tag and HabitGroup collection behavior")
class HabitCollectionsTest {

    // ---- Tag: the equals/hashCode contract ----

    @Test
    @DisplayName("equal Tags have equal hashCodes (the contract)")
    void equalTags_shareHashCode() {
        Tag a = new Tag("Fitness");
        Tag b = new Tag("fitness");

        assertThat(a).isEqualTo(b);                       // equals already passes
        assertThat(a.hashCode()).isEqualTo(b.hashCode()); // FAILS until hashCode is fixed
    }

    @Test
    @DisplayName("a HashSet de-duplicates case-insensitively equal Tags")
    void hashSet_dedupesTags() {
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag("Fitness"));
        tags.add(new Tag("fitness"));   // equal to the first — should NOT be added
        tags.add(new Tag("Reading"));

        // Without a correct hashCode, the set would contain 3 elements.
        assertThat(tags).hasSize(2);
    }

    @Test
    @DisplayName("Set.contains finds an equal-but-different-cased Tag")
    void hashSet_containsRespectsEquality() {
        Set<Tag> tags = new HashSet<>(List.of(new Tag("Fitness")));

        // contains() uses hashCode to find the bucket, THEN equals. A broken
        // hashCode means this returns false even though the Tag is "in" the set.
        assertThat(tags.contains(new Tag("FITNESS"))).isTrue();
    }

    // ---- HabitGroup: genuine immutability ----

    @Test
    @DisplayName("mutating the source list after construction does not change the group")
    void group_isInsulatedFromSourceMutation() {
        List<String> source = new ArrayList<>(List.of("Read", "Run"));
        HabitGroup group = new HabitGroup("Morning", source);

        source.add("Sabotage");          // mutate the original AFTER constructing

        assertThat(group.habits()).containsExactly("Read", "Run"); // unaffected
    }

    @Test
    @DisplayName("the list returned by habits() cannot be mutated by the caller")
    void group_returnsUnmodifiableList() {
        HabitGroup group = new HabitGroup("Morning", new ArrayList<>(List.of("Read")));

        assertThatThrownBy(() -> group.habits().add("Hack"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("two groups with the same name and habits behave as expected")
    void group_basicAccessors() {
        HabitGroup group = new HabitGroup("Evening", List.of("Stretch", "Journal"));

        assertThat(group.name()).isEqualTo("Evening");
        assertThat(group.habits()).containsExactly("Stretch", "Journal");
    }

    // A tiny helper kept for parity with how you'd normalize a tag name.
    static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}

// ----------------------------------------------------------------------------
// HINTS (read only if stuck >15 min)
// ----------------------------------------------------------------------------
//
// BUG 1 — hashCode consistent with a case-insensitive equals:
//
//   @Override
//   public int hashCode() {
//       return name.toLowerCase(java.util.Locale.ROOT).hashCode();
//   }
//
//   The rule: if equals() ignores case, hashCode() must ignore case too —
//   otherwise two "equal" objects can land in different buckets and the set
//   never notices they're duplicates. Hash on exactly the same normalized
//   value your equals() compares.
//
//   ASIDE: the cleanest fix of all would be to make Tag a record with a compact
//   constructor that normalizes the name, then equals/hashCode are generated for
//   you. Try that as a stretch:
//
//       record Tag(String name) {
//           Tag { name = name.toLowerCase(java.util.Locale.ROOT); }
//       }
//
//   (Note: that changes the stored case; adjust if you need the display case.)
//
// BUG 2 — defensive copy in the constructor:
//
//   HabitGroup(String name, List<String> habits) {
//       this.name = Objects.requireNonNull(name, "name must not be null");
//       this.habits = List.copyOf(habits);   // immutable + decoupled from caller
//   }
//
//   List.copyOf returns an unmodifiable list AND copies the elements, so neither
//   later mutation of the source nor a caller calling .add() on habits() can
//   corrupt the group. Both HabitGroup tests then pass with no change to habits().
//
// ----------------------------------------------------------------------------
// WHY THIS MATTERS
// ----------------------------------------------------------------------------
//
// The equals/hashCode contract is the load-bearing invariant under every
// HashSet, HashMap, and HashMap-backed cache in the JVM. Break it and you don't
// get an exception — you get silent, intermittent data loss that's brutal to
// debug. This is why we prefer records: they generate a correct, consistent pair
// for you. When you DO hand-write equals(), you MUST hand-write a matching
// hashCode(), every time.
//
// Defensive copies are the other silent corruptor. "Immutable" means nothing if
// you hold a reference someone else can mutate. List.copyOf / Set.copyOf /
// Map.copyOf in your constructors close that hole for good.
//
// ----------------------------------------------------------------------------
