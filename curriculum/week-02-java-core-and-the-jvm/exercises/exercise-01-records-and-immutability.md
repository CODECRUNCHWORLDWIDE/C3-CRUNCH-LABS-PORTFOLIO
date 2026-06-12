# Exercise 1 — Records and Immutability

**Goal:** Scaffold a real Maven project from a blank folder, model the Crunch Tracker's core data with `record` types, run it from the terminal, and prove to yourself the value-equality and immutability that records hand you for free. No IDE wizards. No framework. Just you, `mvn`, and JDK 21.

**Estimated time:** 40 minutes.

---

## Setup

You need JDK 21 and Maven. Verify both:

```bash
java -version     # must print 21.x
mvn -version      # must print Maven 3.9.x or 4.x, running on Java 21
```

If `java -version` shows 17 or 11, switch with `sdk use java 21.0.x-tem` (week 1 set this up). Every line below assumes Java 21.

---

## Step 1 — Generate a Maven project

From a clean folder:

```bash
mvn -q archetype:generate \
  -DgroupId=com.crunch.tracker \
  -DartifactId=tracker-core \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.4 \
  -DinteractiveMode=false

cd tracker-core
```

You now have a standard Maven layout:

```
tracker-core/
├── pom.xml
└── src/
    ├── main/java/com/crunch/tracker/App.java
    └── test/java/com/crunch/tracker/AppTest.java
```

Initialize Git and commit the skeleton:

```bash
git init
printf "target/\n" > .gitignore
git add .
git commit -m "Generate tracker-core Maven skeleton"
```

---

## Step 2 — Set the project to Java 21 with lint on

Open `pom.xml`. Replace its `<properties>` and add a compiler-plugin block so we target Java 21 and treat the language as 2026 Java. Inside `<project>`:

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.13.0</version>
      <configuration>
        <compilerArgs>
          <arg>-Xlint:all</arg>
        </compilerArgs>
      </configuration>
    </plugin>
  </plugins>
</build>
```

The `release` of `21` is the line that lets records, sealed types, and the pattern-matching `switch` compile. The `-Xlint:all` makes the compiler nag you about 2008-Java habits.

Confirm it still builds:

```bash
mvn -q compile
```

You want `BUILD SUCCESS` and zero warnings.

---

## Step 3 — Model the domain with records

Create `src/main/java/com/crunch/tracker/Goal.java`:

```java
package com.crunch.tracker;

public record Goal(long id, String title, int targetPerWeek) {
    public Goal {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title must not be blank");
        if (targetPerWeek < 1 || targetPerWeek > 7)
            throw new IllegalArgumentException("targetPerWeek must be 1..7");
        title = title.strip();
    }
}
```

Create `src/main/java/com/crunch/tracker/Habit.java`:

```java
package com.crunch.tracker;

public record Habit(long id, long goalId, String name, int currentStreak) {
    public Habit {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
        if (currentStreak < 0)
            throw new IllegalArgumentException("currentStreak must be >= 0");
        name = name.strip();
    }

    public boolean isOnFire() {
        return currentStreak >= 7;
    }

    /** A "wither": return a copy with the streak incremented. Records are immutable. */
    public Habit withIncrementedStreak() {
        return new Habit(id, goalId, name, currentStreak + 1);
    }
}
```

---

## Step 4 — Drive it from `main` and observe what records give you

Replace `src/main/java/com/crunch/tracker/App.java` with:

```java
package com.crunch.tracker;

import java.util.HashSet;
import java.util.Set;

public class App {
    public static void main(String[] args) {
        var read = new Goal(1L, "  Read every day  ", 7);
        System.out.println("toString is free:  " + read);
        System.out.println("title was trimmed: '" + read.title() + "'");

        // Value equality: two records with equal components are .equals().
        var sameRead = new Goal(1L, "Read every day", 7);
        System.out.println("equals (value):    " + read.equals(sameRead));   // true
        System.out.println("== (reference):    " + (read == sameRead));      // false

        // Because equals/hashCode are correct, records work in a HashSet.
        Set<Goal> goals = new HashSet<>();
        goals.add(read);
        goals.add(sameRead);                 // a "duplicate" by value
        System.out.println("set size:          " + goals.size());            // 1, not 2

        // Immutability: withIncrementedStreak returns a NEW Habit; the original is unchanged.
        var habit = new Habit(10L, 1L, "Read 20 pages", 6);
        var advanced = habit.withIncrementedStreak();
        System.out.println("original streak:   " + habit.currentStreak());   // 6 (unchanged)
        System.out.println("advanced streak:   " + advanced.currentStreak()); // 7
        System.out.println("advanced on fire?  " + advanced.isOnFire());      // true

        // Validation runs in the compact constructor.
        try {
            new Goal(2L, "  ", 7);
        } catch (IllegalArgumentException ex) {
            System.out.println("rejected blank:    " + ex.getMessage());
        }
    }
}
```

Run it:

```bash
mvn -q compile exec:java -Dexec.mainClass="com.crunch.tracker.App"
```

(If `exec:java` complains the plugin isn't found, add the `org.codehaus.mojo:exec-maven-plugin` to your `pom.xml`, or just run it with plain tools: `mvn -q compile` then `java -cp target/classes com.crunch.tracker.App`.)

Expected output:

```
toString is free:  Goal[id=1, title=Read every day, targetPerWeek=7]
title was trimmed: 'Read every day'
equals (value):    true
== (reference):    false
set size:          1
original streak:   6
advanced streak:   7
advanced on fire?  true
rejected blank:    title must not be blank
```

---

## Step 5 — Look at the bytecode the compiler generated

Records aren't magic — the compiler really does write `equals`, `hashCode`, `toString`, and accessors for you. Prove it:

```bash
javap -c -p target/classes/com/crunch/tracker/Goal.class
```

Scroll the output. You'll find `equals`, `hashCode`, `toString`, and `id`/`title`/`targetPerWeek` accessor methods — none of which you typed. That one-line `record` declaration expanded into all of it.

---

## Acceptance criteria

You can mark this exercise done when:

- [ ] You have a `tracker-core/` Maven project that builds with `mvn compile` — `BUILD SUCCESS`, **zero warnings**.
- [ ] `Goal` and `Habit` are `record` types with compact-constructor validation.
- [ ] Running `App` prints the expected output above — including `set size: 1` (value equality dedups in a `HashSet`) and the unchanged original streak (immutability).
- [ ] You ran `javap -c -p` on `Goal.class` and saw the generated `equals`/`hashCode`/`toString`.
- [ ] You have at least 2 Git commits with sensible messages.

---

## Stretch

- Add a `CheckIn` record: `record CheckIn(long id, long habitId, java.time.LocalDate date, boolean completed) {}`. Validate that `date` is not in the future.
- Add a JUnit 5 test (`src/test/java/...`) asserting `new Goal(1, "x", 7).equals(new Goal(1, "x", 7))` and that `new Goal(1, "", 7)` throws. (Add the `junit-jupiter` dependency from the resources page.)
- Override `toString` on `Habit` to print a 🔥 when `isOnFire()`. Note: once you write your own `toString`, the compiler stops generating one — that's expected.

---

## Hints

<details>
<summary>If <code>exec:java</code> says the plugin is missing</summary>

The `exec-maven-plugin` isn't bundled with every archetype. Either add it under `<build><plugins>` (`groupId org.codehaus.mojo`, `artifactId exec-maven-plugin`, a recent version), or skip it entirely: `mvn -q compile` then `java -cp target/classes com.crunch.tracker.App`. The plain `java -cp` route always works.

</details>

<details>
<summary>If you see "records are not supported in -source 8"</summary>

Your `<maven.compiler.release>` isn't 21. That single property controls the language level. Set it to `21` and rebuild. Confirm `java -version` is also 21 — Maven uses the JDK on your `PATH`.

</details>

<details>
<summary>Why is <code>read == sameRead</code> false but <code>.equals</code> true?</summary>

`==` on objects compares references — are these the *same object*? They aren't; they're two separate allocations. `.equals()` on a record compares *values* — do all components match? They do. This is exactly the lecture-1 point about comparing objects with `.equals()`, never `==`.

</details>

---

When this exercise feels comfortable, move to [Exercise 2 — Sealed types and the pattern-matching switch](exercise-02-sealed-and-switch.java).
