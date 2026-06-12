# Week 1 — Quiz

Twelve questions. Take it with your lecture notes closed. Aim for 10/12 before moving to Week 2. Answer key at the bottom — don't peek.

---

**Q1.** Which best describes the relationship between Agile, Scrum, and Kanban?

- A) They're three names for the same process, used interchangeably.
- B) Agile is a set of values; Scrum is a framework that implements them; Kanban is a flow-based method — three different things.
- C) Scrum is the values, Kanban is the framework, and Agile is the board tool.
- D) Kanban is a stricter version of Scrum with more required ceremonies.

---

**Q2.** Halfway through a two-week sprint, a stakeholder asks the team to add an urgent new feature "before Friday." In classic Scrum, the healthy response is to:

- A) Silently cram it into the current sprint backlog and hope it fits.
- B) Extend the sprint by three days so the feature fits.
- C) Add it to the product backlog for the PO to order into a future sprint (or, for a true emergency, cancel the sprint) — the sprint length and committed scope don't bend casually.
- D) Tell the stakeholder Agile forbids new features.

---

**Q3.** Which Scrum event is specifically about inspecting **how the team worked** (process), not the product?

- A) Sprint Review
- B) Sprint Planning
- C) Daily Scrum
- D) Sprint Retrospective

---

**Q4.** A story reads: "As a developer, I want to refactor the data layer." Which INVEST letter does it most clearly fail, and why?

- A) Independent — it depends on nothing.
- B) Valuable — there's no user-visible value; "valuable to whom?" has no answer.
- C) Small — refactors are always small.
- D) Testable — refactors can't be tested.

---

**Q5.** In Kanban, what is the primary purpose of a **WIP limit** on the "In Progress" column?

- A) To cap how many people the team can hire.
- B) To force the team to finish work before starting more, smoothing flow and reducing half-done inventory.
- C) To make sure nobody is ever idle.
- D) To set the sprint length.

---

**Q6.** In Git, a **branch** is best described as:

- A) A full copy of the repository's files.
- B) A named, movable pointer to a commit.
- C) A diff between two versions of a file.
- D) A snapshot of the staging area.

---

**Q7.** You're on a feature branch. `main` has advanced. You run `git rebase main`. What happens?

- A) `main` is rewritten to include your commits.
- B) A merge commit with two parents is created on your branch.
- C) Your branch's commits are replayed on top of the latest `main` as new commits, producing a linear history.
- D) Your uncommitted changes are permanently deleted.

---

**Q8.** Which of these is the **one situation you must never** do?

- A) Rebase your own un-pushed feature branch to clean up its commits.
- B) Rebase a shared branch (like `main`) that teammates have already based work on.
- C) Squash-merge a feature branch into `main`.
- D) Run `git rebase --abort` when a rebase goes wrong.

---

**Q9.** You hit a merge conflict. The file shows `<<<<<<< HEAD ... ======= ... >>>>>>> feature/x`. What's the correct way to finish?

- A) Delete the whole file and commit an empty one.
- B) Keep only the `HEAD` side every time; it's always right.
- C) Edit the file to the correct combined result, remove all three marker lines, `git add` it, then continue the merge/rebase.
- D) Run `git push --force` to overwrite the conflict.

---

**Q10.** Which is a well-formed Conventional Commit message?

- A) `fixed the thing`
- B) `feat(habit): reject blank names`
- C) `WIP`
- D) `Update Habit.java and some other files too`

---

**Q11.** Why install the **JDK** rather than the **JRE** for this course?

- A) The JRE is faster at runtime.
- B) The JDK includes the compiler (`javac`) and dev tools; the JRE can only *run* Java, not compile it.
- C) The JRE doesn't support Java 21.
- D) There is no difference; the names are interchangeable.

---

**Q12.** In a GitHub Actions workflow, the `on:` key with `push` and `pull_request` controls:

- A) Which JDK version is installed.
- B) The triggers — *when* the workflow runs.
- C) The name shown in the Actions tab.
- D) The permissions granted to the job's token.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **B** — Agile = values (the Manifesto), Scrum = a framework implementing them, Kanban = a flow method. Conflating them is the classic beginner error.
2. **C** — The sprint's length and committed scope are protected. New work goes to the product backlog for the PO to order; a genuine emergency means *canceling* the sprint, not quietly cramming. Extending the sprint (B) breaks the cadence.
3. **D** — The Retrospective inspects *how the team worked* and produces one or two process improvements. The Review (A) demos the *product*.
4. **B** — It has no user-visible value; "valuable to whom?" can't be answered. (It may also be hard to estimate/test, but the clearest failure is **V**.) Stories should describe value; reframe a refactor as the user-facing capability it enables.
5. **B** — WIP limits force "stop starting, start finishing," which smooths flow and reduces stale half-done work. Keeping everyone "busy" (C) is the anti-pattern WIP limits exist to prevent.
6. **B** — A branch is literally a small file containing a commit SHA — a movable pointer. That's why branching is cheap.
7. **C** — Rebase replays your commits as *new* commits on top of the latest `main`, yielding a linear history. It doesn't touch `main` (A) or create a merge commit (B).
8. **B** — The Golden Rule of Rebasing: never rewrite shared/public history others may have based work on. Rebasing your own un-pushed branch (A) is fine and encouraged.
9. **C** — Resolve by editing to the correct (often combined) result, delete the markers, `git add`, then `--continue`. Never blindly keep one side; understand both.
10. **B** — `type(scope): imperative summary`. The others are exactly the messages Conventional Commits exists to eliminate.
11. **B** — The JDK ships `javac` and the dev tools; the JRE can only run compiled Java. You need to compile, so you install the JDK.
12. **B** — `on:` defines the triggers. `name:` (C) is the label, `permissions:` (D) is token scope, and the JDK (A) is set by `setup-java`.

</details>

---

If you scored under 8, re-read the lectures for the questions you missed. If you scored 10 or higher, you're ready for the [homework](./homework.md) and the [mini-project](./mini-project/README.md).
