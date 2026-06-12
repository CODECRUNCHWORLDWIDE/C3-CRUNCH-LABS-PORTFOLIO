# Mini-Project — Stand Up the Crunch Tracker Team Repo and Board

> Create the repository, board, and CI that every later week of C3 extends. By the end you have a public GitHub repo with a README, a populated backlog of 6+ INVEST stories on a project board, branch protection on `main`, a PR template, and a green "hello, JDK 21" build running in GitHub Actions. No business logic yet — this is the **team foundation** the rest of Crunch Tracker is built on.

This is the only mini-project where the deliverable is mostly *process infrastructure* rather than features. That's deliberate. Weeks 2–10 each add a real slice of Crunch Tracker — domain model, REST API, persistence, auth, mobile client — and every one of them assumes this foundation exists and works. Get it right now and the rest of the course flows through it. Get it wrong and you'll be fighting your own tooling in Week 5.

**Estimated time:** ~13 hours (split across Thursday, Friday, Saturday, and Sunday in the suggested schedule).

---

## What "Crunch Tracker" is (the product you build all course)

Crunch Tracker is a habit and goal tracker. A user creates **goals** ("read 20 books this year"), tracks **habits** ("drink water daily"), and logs **check-ins** ("did it today"). By Week 10 it's a deployed Spring Boot + PostgreSQL API with an installable React Native mobile client. This week you don't build any of that — you build the *project* that will hold it.

---

## What you will deliver

A single public GitHub repo, `crunch-tracker`, containing:

1. A **README** that introduces the product, the tech stack, and how to build/run.
2. A **JDK 21 Maven project** that compiles, tests, and produces a runnable "hello, JDK 21" artifact.
3. A **GitHub Actions CI workflow** that builds and tests on every push and PR — and is **green**.
4. **Branch protection** on `main`: PR required, one approving review required, CI required to pass.
5. A **PR template** (`.github/PULL_REQUEST_TEMPLATE.md`).
6. A **populated backlog**: 6+ INVEST user stories as GitHub issues, on a **GitHub Projects board** with columns.
7. A **CONTRIBUTING.md** capturing the team's working agreements (branch naming, commit format, Definition of Done).

---

## Rules

- **Public GitHub repo.** Portfolio work is public work.
- **JDK 21 / Maven.** Target `maven.compiler.release` = 21. No Gradle this week (we standardize on Maven for the backend; you may use Gradle in a stretch).
- **JUnit 5** for the one test. JUnit 4 is not accepted — it's the 2026 default and what Week 3 builds on.
- **No business logic.** The Java is intentionally a one-method "hello" plus its test. Resist the urge to start building features; that's Week 2's job, and it'll land *through this pipeline*.
- **`main` must be protected before you finish.** After protection is on, every change — including your own — goes through a PR. Demonstrate that you actually used the loop (your history should show feature branches merging via PRs, not direct pushes to `main`).

---

## Acceptance criteria

- [ ] A public GitHub repo named `crunch-tracker` (under your account or the cohort org).
- [ ] Repo layout matches the C3 standard:
  ```
  crunch-tracker/
  ├── README.md
  ├── CONTRIBUTING.md
  ├── .gitignore                       (excludes target/, IDE files)
  ├── .sdkmanrc                        (pins temurin-21)
  ├── pom.xml                          (maven.compiler.release = 21, JUnit 5)
  ├── .github/
  │   ├── workflows/ci.yml             (build + test on push and PR)
  │   └── PULL_REQUEST_TEMPLATE.md
  └── src/
      ├── main/java/dev/crunch/tracker/App.java
      └── test/java/dev/crunch/tracker/AppTest.java
  ```
- [ ] `mvn verify` from the repo root prints `BUILD SUCCESS` with passing tests, locally and in CI.
- [ ] `java -jar target/crunch-tracker-1.0-SNAPSHOT.jar` (or `mvn exec:java` / running `App`) prints a greeting and the Java version.
- [ ] The CI workflow shows a **green** check on at least one pull request.
- [ ] **Branch protection** on `main` requires: a PR, ≥1 approving review, and the CI status check to pass. (Solo learners: you can self-approve via a second account or document the setting and have a peer review; the *configuration* must be present.)
- [ ] A GitHub **Projects board** exists with at least the columns *Backlog → Ready → In Progress → In Review → Done*.
- [ ] **6+ issues**, each a proper INVEST user story with a "so that" and Given/When/Then acceptance criteria, placed on the board.
- [ ] `PULL_REQUEST_TEMPLATE.md` prompts for What / Why / How to test / linked issue.
- [ ] `CONTRIBUTING.md` documents branch naming, Conventional Commits, and the Definition of Done.
- [ ] The repo's commit/PR history shows the **team loop in use** — feature branches merged via PRs, not commits straight to a protected `main`.

---

## Suggested order of operations

Build it incrementally. Each phase ends in a commit (and from Phase 4 on, a PR).

### Phase 1 — Repo + Maven skeleton (~2h)

1. Scaffold the Maven project exactly as in Exercise 1 (`maven-archetype-quickstart`, groupId `dev.crunch.tracker`, artifactId `crunch-tracker`).
2. Pin Java 21 in `pom.xml`:
   ```xml
   <properties>
     <maven.compiler.release>21</maven.compiler.release>
     <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
     <junit.jupiter.version>5.11.3</junit.jupiter.version>
   </properties>
   ```
   and depend on JUnit 5:
   ```xml
   <dependency>
     <groupId>org.junit.jupiter</groupId>
     <artifactId>junit-jupiter</artifactId>
     <version>${junit.jupiter.version}</version>
     <scope>test</scope>
   </dependency>
   ```
3. Write `App.greeting(String)` plus a real `main`, and an `AppTest` with two assertions (named + blank). See Exercise 1 for the exact code.
4. `sdk env init` to write `.sdkmanrc`. Add a `.gitignore` (`target/`, `.idea/`, `.vscode/`, `.DS_Store`).
5. `mvn verify` → `BUILD SUCCESS`.
6. `git init`, commit: `chore: scaffold JDK 21 Maven project`.

### Phase 2 — Push to GitHub (~0.5h)

```bash
gh repo create crunch-tracker --public --source=. --remote=origin
git push -u origin main
```

### Phase 3 — CI workflow (~1.5h)

1. Drop Exercise 3's workflow into `.github/workflows/ci.yml`.
2. Do this on a branch and via a PR, so you see CI run *on the PR*:
   ```bash
   git switch -c ci/add-build-workflow
   git add .github/workflows/ci.yml
   git commit -m "ci: build and test on push and pull_request"
   git push -u origin ci/add-build-workflow
   gh pr create --fill
   ```
3. Watch the "build" job go green in the PR's Checks tab. Fix it if it's red (read the log — it's just `mvn verify` on a clean machine). Merge the PR.

### Phase 4 — PR template + CONTRIBUTING (~1.5h)

1. Add `.github/PULL_REQUEST_TEMPLATE.md`:
   ```markdown
   ## What
   <!-- One or two sentences: what does this PR do? -->

   ## Why
   <!-- The reason / the story this advances. Link the issue. -->

   ## How to test
   <!-- Commands a reviewer runs to verify. `mvn verify` should be green. -->

   ## Checklist
   - [ ] `mvn verify` is green
   - [ ] Tests cover new behavior
   - [ ] No new warnings
   - [ ] Acceptance criteria met

   Closes #
   ```
2. Add `CONTRIBUTING.md` documenting: branch naming (`feature/…`, `fix/…`, `chore/…`), Conventional Commits, the review process, and the **Definition of Done** (merged, CI-green, tested, criteria met).
3. Ship both via a branch + PR (using your own template now). Commit: `docs: add PR template and contributing guide`.

### Phase 5 — Branch protection (~1h)

In **Settings → Branches → Add branch ruleset** (or "Branch protection rules") for `main`, require:

- Pull request before merging (no direct pushes).
- At least 1 approving review.
- Status checks to pass before merging → select your CI job (`Build and test (JDK 21)`).
- Branches up to date before merging.

> Solo learners: GitHub won't let you approve your own PR on a personal repo. Options: (a) use a cohort organization where a peer reviews; (b) create a second GitHub account to act as reviewer; (c) document the ruleset with a screenshot and have a classmate review out-of-band. The *configuration must exist* either way.

Take a screenshot of the ruleset and add it to the README under "Working agreements."

### Phase 6 — Backlog + board (~4h)

1. Create a **GitHub Projects** board with columns *Backlog → Ready → In Progress → In Review → Done*. Add a WIP note (≤2) on the working columns.
2. Write **6+ issues**, each a real INVEST story. Suggested set (write them out fully with acceptance criteria — don't just paste titles):
   - As a user, I want to **add a habit** with a name and frequency, so that I can start tracking it.
   - As a user, I want to **see my list of habits**, so that I know what I'm tracking.
   - As a user, I want to **check in on a habit for today**, so that I can record that I did it.
   - As a user, I want to **create a goal** with a title and target, so that I can work toward it.
   - As a user, I want to **see my current streak** for a habit, so that I stay motivated.
   - As a user, I want to **edit or delete a habit**, so that I can fix mistakes.
   - As a developer, I want **the app to persist data**, so that my habits survive a restart. *(a deliberately fuzzy one — note in the issue that it needs slicing before it's Ready.)*
3. For each, include the "so that" benefit and at least one happy-path + one failure Given/When/Then.
4. Place the most-refined two or three in **Ready**; the rest in **Backlog**. Put your already-done items (README, Maven skeleton, CI) in **Done** so the board reflects reality.
5. Point each story with a rough story-point estimate (1/2/3/5/8) — practice relative sizing.

### Phase 7 — README + polish (~2h)

Write the README so a stranger can clone and build in under five minutes. It must include:

- One paragraph on **what Crunch Tracker is** and the **10-week arc** (one line per week is plenty).
- The **tech stack** (JDK 21, Maven, JUnit 5, GitHub Actions; later: Spring Boot 3, PostgreSQL, React Native/Expo).
- **Setup + build** commands from a fresh clone (SDKMAN, `sdk env`, `mvn verify`, how to run `App`).
- A **status badge** for the CI workflow:
  ```markdown
  ![CI](https://github.com/<you>/crunch-tracker/actions/workflows/ci.yml/badge.svg)
  ```
- A **"Working agreements"** section linking CONTRIBUTING.md and showing the branch-protection screenshot.
- A link to the **project board**.

Final polish: confirm a fresh clone builds, confirm the badge is green, push everything through the protected-PR loop, and make sure `main`'s history is clean.

---

## Example: a board-ready story (use this as your template)

```markdown
### Add a habit  (#7) — 3 points

**As a** user
**I want** to add a habit with a name and a target frequency
**so that** I can start tracking it.

#### Acceptance criteria
Given I am adding a habit
When  I enter the name "Drink water" and frequency "daily" and save
Then  "Drink water" appears in my habit list.

Given I am adding a habit
When  I save with an empty name
Then  I see a validation error and no habit is created.

#### Tasks
- [ ] Define the Habit shape (name, frequency)
- [ ] Reject blank names
- [ ] Surface a validation error
- [ ] Add a happy-path acceptance test
```

---

## Rubric

| Criterion | Weight | What "great" looks like |
|-----------|-------:|-------------------------|
| Repo + build | 20% | `mvn verify` green on a fresh clone; clean layout; Java 21 pinned in `pom.xml` and `.sdkmanrc` |
| CI | 20% | Workflow green on a real PR; caches Maven; triggers on push *and* pull_request |
| Branch protection + PR loop | 20% | `main` protected (PR + review + CI required); history shows feature branches merged via PRs, not direct pushes |
| Backlog quality | 20% | 6+ stories, each genuinely INVEST with "so that" + Given/When/Then; sensible board placement |
| Docs | 15% | README lets a stranger build in <5 min; CONTRIBUTING captures working agreements + DoD; PR template is real |
| Conventional Commits | 5% | History reads as a changelog; no `wip`/`asdf` on `main` |

---

## Stretch (optional)

- Add a `dependabot.yml` so Maven and Actions dependencies get update PRs automatically — and watch your CI gate them.
- Add a CODEOWNERS file so PRs auto-request the right reviewer.
- Add a Spotless/Checkstyle plugin and a separate `lint` CI job (you'll lean on this in Week 3).
- Mirror the build in **Gradle** in a branch and compare the `build.gradle.kts` to the `pom.xml`. Keep Maven as the default.
- Configure a `commitlint` GitHub Action that fails a PR whose commits aren't Conventional.

---

## What this prepares you for

- **Week 2** adds the real domain model (Goal, Habit, CheckIn as records and sealed types) — landing through *this* PR + CI loop.
- **Week 4** turns the project into a Spring Boot service; the CI workflow already there just gets a richer build.
- **Week 5** adds Postgres to CI as a service container — a few lines on top of the workflow you wrote this week.
- **Week 10** extends `ci.yml` into a deploy pipeline. Every later week is an *edit* to the foundation you stand up now.

---

## Resources

- *GitHub Projects*: <https://docs.github.com/en/issues/planning-and-tracking-with-projects>
- *Protected branches*: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
- *PR templates*: <https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository>
- *GitHub Actions for Java with Maven*: <https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-java-with-maven>
- *Conventional Commits*: <https://www.conventionalcommits.org/en/v1.0.0/>

---

## Submission

When done:

1. Push everything to GitHub, with a public URL.
2. Confirm the CI badge is green and a fresh clone builds with `mvn verify`.
3. Confirm `main` is protected and your history shows the PR loop in use.
4. Post the repo URL **and** the project board URL in your cohort tracker. You stood up a real team foundation — show it.
