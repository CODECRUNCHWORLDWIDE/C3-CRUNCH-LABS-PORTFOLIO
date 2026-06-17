# Week 1 — Resources

Every resource on this page is **free**. The Scrum Guide and Pro Git are published openly. Oracle, Adoptium, the Apache Maven project, and GitHub publish their docs without a paywall. No paid books are linked.

## Required reading (work it into your week)

- **The Scrum Guide** — the 13-page source of truth, by the people who created Scrum. Read it once this week, all the way through:
  <https://scrumguides.org/scrum-guide.html>
- **Pro Git (free book), "Git Branching" chapter** — branches, merging, and the model that underpins everything else:
  <https://git-scm.com/book/en/v2/Git-Branching-Branches-in-a-Nutshell>
- **Pro Git, "Git Branching - Rebasing"** — read this before the challenge:
  <https://git-scm.com/book/en/v2/Git-Branching-Rebasing>
- **Trunk-Based Development** — the canonical explanation of the workflow we use all course:
  <https://trunkbaseddevelopment.com/>
- **Maven in Five Minutes** — the official quickstart for the build tool:
  <https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html>

## Agile, Scrum, and Kanban

- **Agile Manifesto** — four values, twelve principles, two minutes to read. Everything else is commentary:
  <https://agilemanifesto.org/>
- **Kanban Guide** — the equivalent of the Scrum Guide, for Kanban:
  <https://kanbanguides.org/>
- **Atlassian Agile Coach** — practical, vendor-neutral-ish write-ups of standups, sprints, and retros:
  <https://www.atlassian.com/agile>
- **Mountain Goat Software — INVEST in good stories** — Bill Wake's original INVEST criteria, explained:
  <https://www.mountaingoatsoftware.com/blog/invest-in-good-stories-and-smart-tasks>
- **GitHub Projects docs** — the board we actually use for the mini-project:
  <https://docs.github.com/en/issues/planning-and-tracking-with-projects>

## The JDK toolchain

- **Adoptium / Eclipse Temurin** — the free, open-source JDK build we standardize on (`temurin-21`):
  <https://adoptium.net/temurin/releases/?version=21>
- **Oracle JDK 21 documentation** — the language and tool reference, free to read:
  <https://docs.oracle.com/en/java/javase/21/>
- **JDK 21 release notes / JEPs** — what shipped in the 21 LTS:
  <https://openjdk.org/projects/jdk/21/>
- **SDKMAN!** — install and switch JDK versions per project:
  <https://sdkman.io/>
- **`java` single-file source-code launch (JEP 330 / JEP 458)** — run a `.java` file directly, no `javac` step:
  <https://openjdk.org/jeps/458>

## Maven

- **Maven — Introduction to the Build Lifecycle** — `validate` → `compile` → `test` → `package` → `verify` → `install`:
  <https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html>
- **Standard Directory Layout** — where `src/main/java` and `src/test/java` come from:
  <https://maven.apache.org/guides/introduction/introduction-to-the-standard-directory-layout.html>
- **Maven Central** — where dependency coordinates resolve from:
  <https://central.sonatype.com/>
- **`pom.xml` POM reference** — the full schema, for when you need an element you don't recognize:
  <https://maven.apache.org/pom.html>

## Git, deeper

- **Pro Git (whole book, free)** — the reference for the year. Bookmark it:
  <https://git-scm.com/book/en/v2>
- **`git` reference manual** — `git help <command>` offline, or online:
  <https://git-scm.com/docs>
- **Oh Shit, Git!?!** — a plain-English recovery cookbook for when you've made a mess (you will):
  <https://ohshitgit.com/>
- **Conventional Commits 1.0.0** — the commit-message spec we follow all course:
  <https://www.conventionalcommits.org/en/v1.0.0/>
- **Atlassian — Merging vs Rebasing** — the clearest side-by-side of the two:
  <https://www.atlassian.com/git/tutorials/merging-vs-rebasing>

## GitHub: PRs, protection, and Actions

- **About pull requests** — the GitHub PR model end to end:
  <https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/about-pull-requests>
- **About protected branches** — required reviews and required status checks:
  <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
- **GitHub Actions — quickstart and workflow syntax**:
  <https://docs.github.com/en/actions/writing-workflows/quickstart>
  <https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions>
- **`actions/setup-java`** — the action that installs and caches a JDK in CI:
  <https://github.com/actions/setup-java>
- **Pull request template support** — where `PULL_REQUEST_TEMPLATE.md` lives:
  <https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/creating-a-pull-request-template-for-your-repository>

## Tools you'll use this week

- **`git`** — version control. `git --version` to confirm (need 2.40+ for a few niceties; any modern Git is fine).
- **`sdk`** — the SDKMAN CLI. `sdk version` after install.
- **`java` / `javac` / `mvn`** — installed via SDKMAN. `java -version` and `mvn -version` should both report JDK 21.
- **`gh`** — the GitHub CLI, for opening PRs and managing repos from the terminal: <https://cli.github.com/>.

## Videos (free, no signup)

- **"Scrum in under 10 minutes"** — search the official Scrum.org channel; the overview videos are short and accurate:
  <https://www.youtube.com/@Scrumorg>
- **"Git for Professionals" / "Git internals"** — Fireship and The Net Ninja both have solid, short Git explainers:
  <https://www.youtube.com/@Fireship>
- **GitHub Actions in ~15 minutes** — the official GitHub channel:
  <https://www.youtube.com/@GitHub>

## Open-source projects to read this week

You learn more from one hour reading a real repo's workflow and history than from three tutorials. Pick one and read its `.github/workflows/` and recent merged PRs:

- **`spring-projects/spring-petclinic`** — a clean, small Spring/Maven repo with readable CI you'll meet again in Week 4:
  <https://github.com/spring-projects/spring-petclinic>
- **`junit-team/junit5`** — well-run PRs, Conventional-ish commits, mature Actions:
  <https://github.com/junit-team/junit5>
- **`actions/setup-java`** — read the action you'll depend on; it's TypeScript but the README is the part that matters:
  <https://github.com/actions/setup-java>

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Agile** | A set of values (the Manifesto). Not a process you can "do." |
| **Scrum** | A framework with fixed roles, events, and artifacts on a sprint cadence. |
| **Kanban** | A flow method: a board with WIP limits, optimizing for cycle time. |
| **Sprint** | A fixed time-box (often 2 weeks) producing a potentially shippable increment. |
| **Backlog** | The ordered list of everything that might get built; the Product Owner owns its order. |
| **INVEST** | Independent, Negotiable, Valuable, Estimable, Small, Testable — a good-story checklist. |
| **JDK** | Java Development Kit — `javac`, `java`, `jar`, the standard library. What you install. |
| **JRE** | Java Runtime Environment — runs Java but can't compile it. Subset of the JDK. |
| **LTS** | Long-Term Support release. JDK 21 is the 2026 LTS baseline; the next is 25. |
| **SDKMAN** | A version manager for JDKs and JVM tools, like `nvm` for Node. |
| **Maven** | A build tool and dependency manager driven by `pom.xml`. |
| **POM** | Project Object Model — the `pom.xml` describing one Maven project. |
| **Trunk** | The shared mainline branch (`main`). Trunk-based dev integrates into it often. |
| **PR** | Pull request — a proposed, reviewable change before it merges to `main`. |
| **CI** | Continuous Integration — building and testing automatically on every change. |
| **HEAD** | A pointer to your current commit/branch — "you are here" in the history. |

---

*If a link 404s, please open an issue so we can replace it.*
