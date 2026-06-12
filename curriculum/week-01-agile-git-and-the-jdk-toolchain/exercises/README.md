# Week 1 — Exercises

Short, focused drills. Each one should take 30–60 minutes. Do them in order; later ones assume earlier ones. Exercise 1 sets up the toolchain everything else needs.

## Index

1. **[Exercise 1 — JDK 21 + Maven setup, then ship a PR](exercise-01-jdk-and-maven-setup.md)** — install JDK 21 with SDKMAN, scaffold a Maven project, branch, commit with Conventional Commits, push, and open a pull request. Also: carve one user story into INVEST tasks. (~60 min)
2. **[Exercise 2 — Hello, JDK 21](exercise-02-hello-jdk21.java)** — a runnable single-file Java 21 program. Fill in the TODOs and run it directly with `java exercise-02-hello-jdk21.java` (no `javac` step). (~35 min)
3. **[Exercise 3 — A real CI workflow](exercise-03-ci.yml)** — a GitHub Actions workflow that builds and tests your Maven project on every push and pull request. Read it, drop it into `.github/workflows/`, and make it go green. (~30 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the commands yourself.** Do not copy-paste blindly. The muscle memory for the Git loop is the entire point of this week.
- Run it. See the output. Read the error if it failed — Git and Maven errors are unusually informative once you slow down.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.
- Every exercise that produces code must end with a **green build**: `mvn verify` reaching `BUILD SUCCESS` with zero test failures, and CI green where applicable. A red build is a stop-the-line event this week.

## Definition of Done for these exercises

A drill is done when:

- [ ] The stated commands run clean on a fresh terminal.
- [ ] Your work is committed with sensible Conventional Commit messages.
- [ ] Exercise 1 ends with an open pull request on your GitHub repo.
- [ ] Exercise 2 prints the expected output.
- [ ] Exercise 3's workflow shows a green check on your PR.
- [ ] You can explain, in your own words, what each command did and why.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-01` to compare approaches.
