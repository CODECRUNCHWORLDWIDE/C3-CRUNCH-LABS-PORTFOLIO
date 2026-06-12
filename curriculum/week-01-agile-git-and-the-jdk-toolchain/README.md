# Week 1 — Agile, Git, and the JDK Toolchain

Welcome to **C3 · Crunch Labs Portfolio**. This is a 10-week, full-stack Java program. Every week extends the same product — a habit and goal tracker called **Crunch Tracker** — until, by Week 10, you have shipped a deployed Spring Boot API and an installable React Native app you can put in your portfolio.

Week 1 builds the floor every later week stands on. We do not write a line of business logic. Instead we set the **working agreements**: how a real Scrum/Kanban team actually moves through a sprint, how Git keeps that team honest, and how the JDK 21 toolchain turns a blank folder into a building, testing, CI-checked project. By Friday you can scaffold a JDK 21 Maven project from the terminal, drive a board through one sprint, and ship a trunk-based feature branch behind a clean pull request that a teammate can review in five minutes.

We assume you can already program. The C1/C2 graduate is the target: comfortable with functions, classes, collections, and exceptions in *some* language, and able to read basic Java. If that's you, this week is less "learn to code" and more "learn how a team of engineers ships software without stepping on each other." We move fast and we are opinionated, because the habits you form this week are the ones you will still be using in Week 10 and in your first job.

The first thing to internalize: **the tools are not the point — the discipline is.** Git is not "save with a comment." A sprint board is not a to-do list. CI is not "it builds on my machine." Each of these is a *contract* between you and the rest of the team (including future-you). This week is where you sign those contracts.

## Learning objectives

By the end of this week, you will be able to:

- **Explain** how a real Scrum team runs a sprint — the cadence, the five ceremonies, the three roles, the two key artifacts — and where Kanban differs and why a team would choose it.
- **Carve** a vague feature request into **INVEST** user stories and break a story into tasks small enough to finish in a day.
- **Install and pin** JDK 21 with SDKMAN, and explain the difference between a JDK, a JRE, and the `java`/`javac`/`jar` tools.
- **Scaffold** a Maven project skeleton from the terminal, read its `pom.xml` line by line, and run `mvn verify` to a green build.
- **Use** trunk-based development with short-lived feature branches: branch, commit with Conventional Commit messages, push, and open a pull request.
- **Review** a pull request like a senior engineer — read the diff, leave specific comments, request changes, and approve.
- **Configure** branch protection so `main` can only change through a reviewed, CI-passing PR.
- **Author** a GitHub Actions workflow that builds and tests the project on every push and pull request.
- **Recover** from a gnarly three-way merge conflict and rebase a messy branch into a clean, reviewable history without losing work.

## Prerequisites

This week assumes you have completed **C1 · Code Crunch Convos** (or equivalent programming fundamentals) and can:

- Work in a terminal — `cd`, `ls`, `mkdir`, run a program, set an environment variable.
- Read basic Java or another C-family language (you do **not** need Java fluency yet — that's Week 2).
- Use Git at the "I've made a few commits" level (`clone`, `add`, `commit`, `push`). We deepen this hard.
- Hold a free GitHub account. If you don't have one, create it before Monday: <https://github.com/signup>.

You do **not** need any prior Maven, CI, or Scrum experience. We start from the toolchain.

## Topics covered

- The three Agile frameworks people conflate: **Agile** (the values), **Scrum** (a framework), **Kanban** (a method). What each actually prescribes.
- Scrum mechanics: the sprint, the backlog, sprint planning, daily standup, sprint review, retrospective; Product Owner, Scrum Master, Developers.
- Kanban mechanics: the board, work-in-progress (WIP) limits, pull vs push, cycle time, and when to prefer it over Scrum.
- **INVEST** user stories, acceptance criteria, story points, and the difference between a story and a task.
- The JDK toolchain: `javac`, `java`, `jar`, `javadoc`; JDK vs JRE; LTS releases and why JDK 21 is the 2026 baseline.
- **SDKMAN** for installing and pinning JDK versions per project with `.sdkmanrc`.
- Maven: `pom.xml`, the build lifecycle (`compile`, `test`, `package`, `verify`, `install`), the standard directory layout, and dependency coordinates.
- Git internals you actually need: commits as a DAG, branches as pointers, `HEAD`, the three trees (working tree, index, HEAD), and what `merge` vs `rebase` really do.
- **Trunk-based development**: short-lived branches, frequent integration, and why long-lived feature branches rot.
- **Conventional Commits**: `type(scope): summary`, and why a machine-readable history pays off.
- Pull requests: a good description, a focused diff, the PR template, and code-review etiquette.
- Branch protection rules and required status checks on GitHub.
- **GitHub Actions**: workflows, jobs, steps, triggers, the `setup-java` action, and caching.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract — some days you'll move faster.

| Day       | Focus                                            | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|--------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | Agile vs Scrum vs Kanban; ceremonies & roles     |    2h    |    1h     |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5h      |
| Tuesday   | INVEST stories; JDK 21 + SDKMAN setup            |    1h    |    2h     |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5h      |
| Wednesday | Git for teams: trunk-based, branches, merges     |    2h    |    1.5h   |     1h     |    0.5h   |   1h     |     0h       |    0h      |     6h      |
| Thursday  | Pull requests, review, branch protection         |    1h    |    1.5h   |     1h     |    0.5h   |   1h     |     1h       |    0.5h    |     6.5h    |
| Friday    | GitHub Actions CI; mini-project work             |    0h    |    1h     |     0h     |    0.5h   |   1h     |     3h       |    0.5h    |     6h      |
| Saturday  | Mini-project deep work (repo + board + CI)        |    0h    |    0h     |     0h     |    0h     |   0h     |     4h       |    0h      |     4h      |
| Sunday    | Quiz, review, polish                             |    0h    |    0h     |     0h     |    1h     |   0h     |     1h       |    0.5h    |     2.5h    |
| **Total** |                                                  | **6h**   | **7h**    | **2h**     | **3.5h**  | **5h**   | **13h**      | **3h**     | **35h**     |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./README.md) | This overview (you are here) |
| [resources.md](./resources.md) | Curated, free, current references — Scrum Guide, Pro Git, JDK/Maven/Actions docs |
| [lecture-notes/01-how-a-real-sprint-runs.md](./lecture-notes/01-how-a-real-sprint-runs.md) | Agile vs Scrum vs Kanban; the ceremonies, roles, and artifacts; INVEST stories |
| [lecture-notes/02-git-for-teams.md](./lecture-notes/02-git-for-teams.md) | Git internals, trunk-based development, branches, merge vs rebase, PR review |
| [exercises/README.md](./exercises/README.md) | Index of the short, focused drills |
| [exercises/exercise-01-jdk-and-maven-setup.md](./exercises/exercise-01-jdk-and-maven-setup.md) | Install JDK 21 with SDKMAN, scaffold a Maven project, branch + commit + open a PR |
| [exercises/exercise-02-hello-jdk21.java](./exercises/exercise-02-hello-jdk21.java) | Runnable JDK 21 single-file program — fill in the TODOs, run with `java` |
| [exercises/exercise-03-ci.yml](./exercises/exercise-03-ci.yml) | A real GitHub Actions workflow that builds and tests on every push and PR |
| [challenges/README.md](./challenges/README.md) | What the challenge is and how it's graded |
| [challenges/challenge-01-merge-conflict-rebase.md](./challenges/challenge-01-merge-conflict-rebase.md) | Resolve a deliberate three-way conflict and rebase a messy branch clean |
| [mini-project/README.md](./mini-project/README.md) | Full spec for "Stand up the Crunch Tracker team repo and board" |
| [quiz.md](./quiz.md) | 12 questions with an answer key |
| [homework.md](./homework.md) | Six practice problems with a grading rubric |

## The "green build" promise

C3 uses a small recurring marker in every exercise that ends in working code:

```
BUILD SUCCESS · 0 errors · 0 test failures
```

If your `mvn verify` doesn't reach `BUILD SUCCESS` with no failing tests, you are not done. From Week 1 we treat a red build as a stop-the-line event. The point of this week is to make that green line — locally *and* in CI — ordinary.

## Stretch goals

If you finish the regular work early and want to push further:

- Read the **Scrum Guide** end to end (it's only ~13 pages): <https://scrumguides.org/scrum-guide.html>. Notice how little it actually prescribes.
- Read the **Pro Git** chapter on "Git Branching - Rebasing": <https://git-scm.com/book/en/v2/Git-Branching-Rebasing>. Then re-read it after the challenge.
- Add a second job to your CI workflow that runs `mvn -B verify` on both JDK 21 and JDK 23 using a matrix, and read what fails (probably nothing — but now you know how).
- Read Martin Fowler on **Trunk-Based Development** and long-lived branches: <https://martinfowler.com/articles/branching-patterns.html>.
- Configure a commit-message linter (`commitlint`) locally and watch it reject `fixed stuff` as a commit message.

## Up next

Continue to **Week 2 — Java Core and the JVM** once you have pushed your mini-project repo, board, branch protection, and a green CI run.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
