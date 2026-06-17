# Week 1 Homework

Six practice problems that revisit the week's topics. The full set should take about **5 hours**. Work in your Week 1 Git repository so each problem produces at least one commit (ideally a PR) you can point to later.

Each problem includes a short **problem statement**, **acceptance criteria** so you know when you're done, a **hint**, and an **estimated time**. The grading rubric is at the bottom.

---

## Problem 1 — Toolchain audit

**Problem statement.** Run the toolchain commands and capture what you see in `notes/toolchain.md`. For each, state the value and whether it's what you expected:

1. `java -version` — the JDK version and vendor (must be Temurin 21).
2. `mvn -version` — Maven version *and* the "Java version" line it reports (must also be 21).
3. `sdk current java` — what SDKMAN has set as your active JDK.
4. The contents of your project's `.sdkmanrc`.
5. One sentence: *if a teammate clones your repo and runs `sdk env`, what JDK will they get and why?*

**Acceptance criteria.**

- [ ] `notes/toolchain.md` exists with all five items.
- [ ] Committed with a Conventional Commit message.

**Hint.** If `mvn -version` reports a different Java than `java -version`, your `JAVA_HOME` is stale — re-run `sdk default java 21.0.5-tem` and open a fresh terminal.

**Estimated time.** 20 minutes.

---

## Problem 2 — Carve two stories into INVEST tasks

**Problem statement.** Take two Crunch Tracker stories — "create a goal" and "check in on a habit for today." For each, in `notes/stories.md`:

1. Write it in the **As a / I want / so that** form.
2. Run it through **INVEST** — one line per letter, noting any letter it fails and how you'd fix it.
3. Write **Given/When/Then** acceptance criteria: at least one happy path and one failure each.
4. Break each into **4–6 day-sized tasks** as checkboxes.

**Acceptance criteria.**

- [ ] Two complete stories with INVEST checks, acceptance criteria, and tasks.
- [ ] At least one story includes a deliberate failure-case acceptance criterion.
- [ ] Committed.

**Hint.** A check-in story's failure case might be "checking in twice on the same day shouldn't double-count." Naming that *now* saves you a bug in Week 9.

**Estimated time.** 45 minutes.

---

## Problem 3 — Run the full team Git loop, twice

**Problem statement.** In your Week 1 repo, ship two separate small changes the right way — each on its own short-lived branch, with Conventional Commits, opened as a PR.

1. PR 1: add a `LICENSE` (MIT is fine) on branch `chore/add-license`.
2. PR 2: add a `CONTRIBUTING.md` with your branch-naming and commit conventions on branch `docs/contributing`.

For each: branch off an up-to-date `main`, commit, push, `gh pr create`, then merge.

**Acceptance criteria.**

- [ ] Two merged PRs visible in the repo's PR list.
- [ ] Each came from a correctly-named feature/chore/docs branch.
- [ ] Every commit message follows Conventional Commits.
- [ ] After merging, the feature branches are deleted.

**Hint.** `gh pr create --fill` uses your branch's commits to pre-fill the PR. `gh pr merge --squash --delete-branch` merges and cleans up in one shot.

**Estimated time.** 45 minutes.

---

## Problem 4 — A switch expression and a record in JDK 21

**Problem statement.** In a single file `homework/Streak.java` (runnable with `java homework/Streak.java`), define:

```java
record CheckIn(java.time.LocalDate date, boolean done) {}
```

Then write a method `String describe(CheckIn c)` that uses a **switch expression** (not if/else) over a derived value to return:

- `"done today"` when `done` is true and `date` equals `LocalDate.now()`.
- `"done earlier"` when `done` is true and `date` is before today.
- `"missed"` when `done` is false.

Drive it from `main` with three example check-ins and print the results.

**Acceptance criteria.**

- [ ] `java homework/Streak.java` runs with no exception and prints three lines.
- [ ] The classification logic is a switch expression, not an if/else ladder.
- [ ] `CheckIn` is a record.
- [ ] Committed.

**Hint.** Switch on a small computed key, e.g. `switch (key(c)) { case TODAY -> ...; }`, where `key` returns an enum or an int derived from the booleans/date comparison. Use `LocalDate.now()` and `isBefore`.

**Estimated time.** 1 hour.

---

## Problem 5 — Add CI and make it required

**Problem statement.** Add the Exercise 3 workflow to your Week 1 Maven repo (if you haven't already), open a PR, and turn the resulting status check into a **required** check in branch protection. Then prove it works: open a PR that *deliberately* breaks a test, confirm CI goes red and the merge button is blocked, then fix it and confirm it merges.

**Acceptance criteria.**

- [ ] `.github/workflows/ci.yml` runs on push and pull_request and is green on a passing PR.
- [ ] The CI job is listed as a **required status check** in `main`'s branch protection.
- [ ] You have evidence (screenshot or a closed "broken" PR) that a red CI blocked the merge.
- [ ] Committed/merged.

**Hint.** Break a test by changing an expected value in `AppTest`, push, watch CI fail, confirm "Merge" is disabled, then revert. Solo learners can document the protection with a screenshot.

**Estimated time.** 1 hour.

---

## Problem 6 — Reflection essay

**Problem statement.** Write a 300–400 word reflection at `notes/week-01-reflection.md` answering, one paragraph each:

1. Which felt easiest this week — the Agile/Scrum concepts, the JDK/Maven toolchain, or the Git workflow? Which felt hardest, and why?
2. Did anything you believed about Git (or "Agile," or build tools) turn out to be wrong this week? What?
3. In one paragraph, how would you explain "merge vs rebase" to someone who's only ever used `git pull`?
4. What's one thing you want to get better at before Week 2's Java deep-dive?

**Acceptance criteria.**

- [ ] File exists, 300–400 words.
- [ ] Each numbered question addressed in its own paragraph.
- [ ] Committed.

**Hint.** This is for *you*. Be honest about what was confusing — future-you, debugging a rebase in Week 5, will be glad you wrote down what clicked.

**Estimated time.** 30 minutes.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 20 min |
| 2 | 45 min |
| 3 | 45 min |
| 4 | 1 h |
| 5 | 1 h |
| 6 | 30 min |
| **Total** | **~4 h 20 min** |

---

## Grading rubric

Total: 100 points.

| Problem | Points | What earns full marks |
|--------:|-------:|-----------------------|
| 1 — Toolchain audit | 10 | All five items captured; Java 21 confirmed in both `java` and `mvn`; the `sdk env` sentence is correct. |
| 2 — INVEST stories | 20 | Both stories in proper form; honest INVEST check per letter; happy *and* failure acceptance criteria; 4–6 real tasks each. |
| 3 — Git loop ×2 | 20 | Two merged PRs from correctly-named branches; every commit Conventional; branches deleted after merge. |
| 4 — Switch + record | 15 | Runs clean; classification is a switch expression (no if/else ladder); `CheckIn` is a record; output correct. |
| 5 — Required CI | 20 | Workflow green on a passing PR; CI is a required check; demonstrated that red CI blocks the merge. |
| 6 — Reflection | 15 | 300–400 words; all four questions addressed thoughtfully in their own paragraphs. |

**Penalties.**

- −5 if any commit on a protected `main` bypassed the PR loop (direct push).
- −5 for non-Conventional commit messages on merged work.
- −10 if `mvn verify` is red on a fresh clone.

**Bonus (up to +5).**

- +3 for a CI build matrix (JDK 21 + 23) that's green.
- +2 for a `dependabot.yml` or a `commitlint` check that actually gates a PR.

To submit: push your repo, ensure a fresh clone builds green, and post the repo URL (plus links to your two merged PRs from Problem 3) in your cohort tracker.

When you've finished, push your repo and open the [mini-project](./07-mini-project/00-overview.md).
