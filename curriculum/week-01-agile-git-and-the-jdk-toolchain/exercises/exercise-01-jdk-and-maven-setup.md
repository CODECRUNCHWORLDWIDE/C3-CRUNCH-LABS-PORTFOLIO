# Exercise 1 — JDK 21 + Maven Setup, Then Ship a PR

**Goal:** Go from a blank machine to a building JDK 21 Maven project, then run the full team Git loop once — branch, commit with Conventional Commits, push, and open a pull request. By the end you've installed your toolchain, scaffolded a project, and shipped a reviewable change. You'll also slice a user story into INVEST tasks.

**Estimated time:** 60 minutes.

---

## Setup

You need three things: a terminal, Git, and a GitHub account.

```bash
git --version      # any modern Git (2.30+) is fine
```

If you don't have a GitHub account, create one now: <https://github.com/signup>. Then make sure your local Git identity is set:

```bash
git config --global user.name  "Your Name"
git config --global user.email "you@example.com"
```

---

## Step 1 — Install SDKMAN and JDK 21

SDKMAN manages JDK versions per project, the way `nvm` manages Node. Install it:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk version
```

Now install a JDK 21 LTS build — we standardize on Eclipse Temurin:

```bash
sdk install java 21.0.5-tem
sdk default java 21.0.5-tem
```

(If `21.0.5-tem` is no longer the latest patch when you do this, run `sdk list java` and pick the newest `21.x.y-tem` identifier.)

Verify:

```bash
java -version
```

You should see something like:

```
openjdk version "21.0.5" 2024-10-15 LTS
OpenJDK Runtime Environment Temurin-21.0.5+11 (build 21.0.5+11-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.5+11 (build 21.0.5+11-LTS, mixed mode, sharing)
```

If `java -version` does not say `21`, stop and fix that before going further. Every later week assumes JDK 21.

Also install Maven:

```bash
sdk install maven
mvn -version
```

`mvn -version` should report Maven 3.9+ running on the Java 21 you just installed.

---

## Step 2 — Scaffold a Maven project

Generate a standard project skeleton with the Maven quickstart archetype:

```bash
mkdir crunch-tracker-warmup && cd crunch-tracker-warmup

mvn -B archetype:generate \
  -DgroupId=dev.crunch.tracker \
  -DartifactId=crunch-tracker \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DarchetypeVersion=1.5 \
  -DinteractiveMode=false

cd crunch-tracker
```

You now have the canonical Maven layout:

```
crunch-tracker/
├── pom.xml
└── src/
    ├── main/
    │   └── java/dev/crunch/tracker/App.java
    └── test/
        └── java/dev/crunch/tracker/AppTest.java
```

Open `pom.xml`. Find the `<properties>` block (or add one) and pin Java 21 so the compiler targets it:

```xml
<properties>
  <maven.compiler.release>21</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

Pin a `.sdkmanrc` so anyone who clones gets the same JDK:

```bash
sdk env init        # writes a .sdkmanrc with your current java version
```

Build and test:

```bash
mvn verify
```

You should see, at the bottom:

```
[INFO] BUILD SUCCESS
```

That's the "green build" marker. The archetype ships with one passing JUnit test, so `mvn verify` compiles, tests, and packages a `target/crunch-tracker-1.0-SNAPSHOT.jar`.

---

## Step 3 — Initialize Git and a .gitignore

```bash
git init
```

Create a `.gitignore` so you never commit build output or IDE noise:

```
target/
*.class
.idea/
*.iml
.vscode/
.DS_Store
```

First commit:

```bash
git add .
git commit -m "chore: scaffold JDK 21 Maven project"
```

---

## Step 4 — Create the GitHub repo and push

Using the GitHub CLI (install from <https://cli.github.com/> if needed):

```bash
gh auth login            # one-time, follow the prompts
gh repo create crunch-tracker-warmup --public --source=. --remote=origin
git push -u origin main
```

If you prefer the web UI: create an empty public repo named `crunch-tracker-warmup` (no README), then:

```bash
git remote add origin https://github.com/<you>/crunch-tracker-warmup.git
git branch -M main
git push -u origin main
```

---

## Step 5 — Run the team Git loop on a feature branch

Now ship a small change the *right* way: on a branch, behind a PR.

```bash
git switch -c feature/greeting
```

Edit `src/main/java/dev/crunch/tracker/App.java` so `main` prints a real greeting:

```java
package dev.crunch.tracker;

public class App {
    public static String greeting(String name) {
        return name == null || name.isBlank()
            ? "Hello, Crunch Tracker!"
            : "Hello, " + name.trim() + "!";
    }

    public static void main(String[] args) {
        String who = args.length > 0 ? args[0] : "";
        System.out.println(greeting(who));
        System.out.println("Running on Java " + System.getProperty("java.version"));
    }
}
```

Replace the generated `AppTest.java` body with real assertions:

```java
package dev.crunch.tracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void greetsByName() {
        assertEquals("Hello, Ada!", App.greeting("Ada"));
    }

    @Test
    void greetsGenericWhenBlank() {
        assertEquals("Hello, Crunch Tracker!", App.greeting("  "));
    }
}
```

> If the archetype generated JUnit 4 (`org.junit.Test`), upgrade `pom.xml` to JUnit 5 (`org.junit.jupiter:junit-jupiter:5.11.3`, test scope) — see the hint at the bottom. JUnit 5 is the 2026 default and what Week 3 uses.

Build, then commit each logical change:

```bash
mvn verify
git add src/main/java/dev/crunch/tracker/App.java
git commit -m "feat(app): add greeting that handles blank names"
git add src/test/java/dev/crunch/tracker/AppTest.java
git commit -m "test(app): cover named and blank greeting cases"
git push -u origin feature/greeting
```

Open a pull request:

```bash
gh pr create --title "feat(app): add greeting" \
  --body "Adds a greeting method with blank-name handling and tests. mvn verify is green."
```

Open the PR in your browser (`gh pr view --web`). Read your own diff top to bottom as if you were the reviewer. That habit — reviewing your own PR before asking a human — will make you the engineer teammates *want* to review.

---

## Step 6 — Carve a story into INVEST tasks

On paper or in a `STORY.md` file in the repo, take this story:

> **As a** user, **I want** to add a habit with a name and a target frequency, **so that** I can start tracking it.

Do three things:

1. **Check it against INVEST.** For each letter (Independent, Negotiable, Valuable, Estimable, Small, Testable), write one line: does it pass? If "Small" is shaky, note how you'd split it.
2. **Write Given/When/Then acceptance criteria** — at least one happy path and one failure (empty name).
3. **Break it into 4–6 day-sized tasks**, each a checkbox.

Commit it:

```bash
git add STORY.md
git commit -m "docs: slice add-habit story into INVEST tasks"
git push
```

---

## Acceptance criteria

You can mark this exercise done when:

- [ ] `java -version` and `mvn -version` both report Java 21.
- [ ] You have a `crunch-tracker` Maven project where `mvn verify` prints `BUILD SUCCESS` with passing tests.
- [ ] The repo is pushed to GitHub and has a `.gitignore` excluding `target/`.
- [ ] You have at least **3 commits** with Conventional Commit messages.
- [ ] An **open pull request** exists from `feature/greeting` into `main`.
- [ ] `STORY.md` contains an INVEST review, Given/When/Then criteria, and 4–6 tasks.
- [ ] You can describe what `pom.xml`, `src/main/java`, `src/test/java`, and `target/` are for.

---

## Stretch

- Add a `--upper` argument that uppercases the greeting. Test it.
- Add `git config --global commit.template` pointing at a file that reminds you of the Conventional Commit format.
- Configure your `pom.xml` to fail the build on compiler warnings (`-Werror`). Rebuild and confirm nothing breaks.

---

## Hints

<details>
<summary>If <code>sdk: command not found</code> after install</summary>

The installer adds a line to your shell profile, but the current shell hasn't sourced it. Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

Open a new terminal and it'll be permanent.

</details>

<details>
<summary>Switching the archetype to JUnit 5</summary>

Replace the JUnit 4 dependency in `pom.xml` with:

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.11.3</version>
  <scope>test</scope>
</dependency>
```

and ensure `maven-surefire-plugin` is 3.2+ (the Maven 3.9 default is fine). Then `mvn verify` runs JUnit 5 tests.

</details>

<details>
<summary>If <code>gh repo create</code> fails with auth</summary>

Run `gh auth login` first and choose HTTPS + a browser login. Then re-run the create command. If you must use the web UI, create the empty repo there and add the remote manually as shown in Step 4.

</details>

<details>
<summary>If <code>mvn verify</code> downloads "the whole internet"</summary>

That's Maven populating your local repository (`~/.m2/repository`) the first time. It's a one-time cost; subsequent builds are fast. Don't interrupt it.

</details>

---

When this exercise feels comfortable, move to [Exercise 2 — Hello, JDK 21](exercise-02-hello-jdk21.java).
