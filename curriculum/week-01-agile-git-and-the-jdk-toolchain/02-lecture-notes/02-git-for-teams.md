# Lecture 2 — Git for Teams: Trunk-Based Development, Feature Branches, and PR Review

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can describe Git's object model well enough to predict what a command does, work in short-lived feature branches off a protected trunk, write Conventional Commit messages, open a focused pull request, and review someone else's PR like a senior engineer.

If you only remember one thing from this lecture, remember this:

> **A branch is a pointer. A commit is an immutable snapshot. `merge` and `rebase` are two different ways to combine histories, with different trade-offs. Once you understand those three facts, every "scary" Git situation becomes a sequence of moving pointers — and nothing in Git that you haven't deleted is ever truly lost.**

You already know how to `add`, `commit`, and `push`. This lecture is about Git *for a team*: how to integrate your work with other people's continuously, keep `main` always shippable, and produce a history a reviewer can actually read. The mechanics are 20% of it. The discipline is 80%.

---

## 1. The mental model: commits, trees, pointers

Git is not "a pile of diffs." It's a content-addressed database of **snapshots**.

- A **commit** is an immutable snapshot of your whole tree at a moment, plus metadata: author, message, and a pointer to its **parent** commit(s). Each commit is named by a SHA hash of its content. Change anything — one byte, the message, the parent — and you get a *different* commit with a *different* hash. Commits are never edited; they're replaced.
- Because each commit points at its parent, the history is a **directed acyclic graph (DAG)** — a chain (or web, when branches merge) of snapshots pointing backward in time.
- A **branch** is just a *named, movable pointer to a commit*. `main` is a 40-byte file containing a SHA. That's the whole secret. Creating a branch is cheap because it's literally writing one pointer. This is why Git branches are nothing like the heavyweight branches of older version-control systems.
- **`HEAD`** is a pointer to "where you are" — usually pointing at a branch (which points at a commit). "You are here."

```
        A ── B ── C  ← main
                   \
                    D ── E  ← feature/add-habit  ← HEAD
```

Here `main` points at `C`. You branched `feature/add-habit` and made commits `D` and `E`. `HEAD` points at `feature/add-habit`. Nothing about `C` changed; you just added new snapshots and moved a pointer.

> **Why this matters:** almost every Git operation — `commit`, `merge`, `rebase`, `reset`, `checkout` — is, underneath, "move a pointer and/or create new commits." Once you see commands as pointer moves, "I'm terrified of rebasing" turns into "I'm moving this pointer; let me check where it lands."

---

## 2. The three trees

When you work, three "trees" are in play. Knowing which is which explains every `git status` line.

| Tree | What it is | Command that touches it |
|------|-----------|-------------------------|
| **Working tree** | The actual files on disk you edit. | Your editor. |
| **Index (staging area)** | A snapshot you're *building* for the next commit. | `git add`, `git restore --staged` |
| **HEAD** | The last commit on your current branch. | `git commit` (advances HEAD) |

The flow is: edit the **working tree** → `git add` to copy changes into the **index** → `git commit` to snapshot the index into a new commit that **HEAD** advances onto.

```
working tree   --git add-->   index   --git commit-->   HEAD (new commit)
```

`git status` is just reporting the differences between these three. "Changes not staged for commit" = working tree differs from index. "Changes to be committed" = index differs from HEAD. Internalize this and `git status` stops being noise.

---

## 3. Trunk-based development: why we don't keep long branches

There are two dominant team workflows. We use the first and you should know why.

**Trunk-based development (TBD)** — what we use:

- One shared mainline: `main` (the "trunk").
- Work happens on **short-lived** feature branches — hours to a couple of days, not weeks.
- You integrate into `main` *frequently* (at least daily for active work), through small PRs.
- `main` is *always* releasable. CI proves it on every change.

**GitFlow** — the alternative, which we *don't* use:

- Long-lived `develop` and `release` branches, separate from `main`.
- Feature branches can live for weeks before merging.
- More ceremony, more branches, more merge pain.

> **Why long-lived branches rot.** The longer your branch lives away from `main`, the more `main` moves underneath you. When you finally merge, you face a giant conflict-prone integration with code you wrote two weeks ago and barely remember. This is "merge hell." Worse, a giant PR is *unreviewable* — nobody reads a 2,000-line diff carefully; they skim it and approve, and bugs sail through. Small, frequent integration is not just tidy; it's how you keep the team's velocity from collapsing. Modern teams overwhelmingly favor trunk-based development for exactly this reason (it's also what the DORA research associates with high-performing teams).

The TBD rule of thumb: **if your branch can't merge to `main` within a day or two, it's too big — slice the story smaller** (back to INVEST's "Small" from Lecture 1).

---

## 4. The branch-to-merge loop

This is the loop you'll run dozens of times this course. Memorize the shape.

```bash
# 1. Start from an up-to-date trunk.
git switch main
git pull origin main

# 2. Branch for one unit of work. Name it for the work.
git switch -c feature/add-habit

# 3. Do the work in small commits.
#    ... edit files ...
git add src/main/java/dev/crunch/tracker/Habit.java
git commit -m "feat(habit): add Habit record with name and frequency"

# 4. Push the branch and set its upstream.
git push -u origin feature/add-habit

# 5. Open a pull request (UI or gh CLI).
gh pr create --fill

# 6. Address review feedback with more small commits, push again.
git commit -m "test(habit): cover empty-name validation"
git push

# 7. Once approved and CI is green, merge (squash) and delete the branch.
#    Then clean up locally:
git switch main
git pull origin main
git branch -d feature/add-habit
```

Note `git switch` and `git restore` — the modern, purpose-built commands (Git 2.23+). The old `git checkout` does both jobs and a dozen others, which is exactly why beginners shoot themselves in the foot with it. Prefer `switch` for branches and `restore` for files.

### Branch naming

Use a consistent prefix so the branch name tells you the work type at a glance:

```
feature/add-habit
fix/habit-empty-name-validation
chore/bump-junit-5.11
docs/readme-setup-steps
```

Many teams put the issue number in too: `feature/7-add-habit`. That makes the link between the board card and the branch obvious.

---

## 5. Conventional Commits

A commit message is documentation written for the next person to run `git log` — usually future-you, at 2 a.m., bisecting a regression. We follow **Conventional Commits**:

```
<type>(<optional scope>): <imperative summary, lower case, no period>

<optional body: what and why, not how>

<optional footer: BREAKING CHANGE, issue refs>
```

The common **types**:

| Type | Use for |
|------|---------|
| `feat` | A new user-facing feature |
| `fix` | A bug fix |
| `chore` | Tooling, deps, config — no production behavior change |
| `docs` | Documentation only |
| `test` | Adding or fixing tests |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `ci` | CI/workflow changes |
| `build` | Build system / dependency changes |

Good messages:

```
feat(habit): add daily/weekly frequency to Habit
fix(ci): pin temurin-21 so the build is reproducible
chore: add .gitignore for Maven target/ and IDE files
docs(readme): document SDKMAN setup steps
```

Bad messages (and why):

```
fixed stuff            ← fixed what? you'll never find this later
update                 ← updates everything and nothing
WIP                    ← never merge a "WIP"; squash it away first
asdfasdf               ← we have all done this; don't merge it
```

> **Why bother with the format?** Two payoffs. First, `git log --oneline` becomes a *readable changelog* you can hand to anyone. Second, it's machine-readable: tools can auto-generate release notes and bump semantic versions from `feat`/`fix`/`BREAKING CHANGE`. You get both for the cost of typing a five-character prefix.

### Commit hygiene

- **One logical change per commit.** Don't mix "add feature" and "reformat 40 files" — the diff becomes unreviewable.
- **Commit early and often locally**, then clean up before the PR (see §9, interactive rebase). Your local history is a draft; the merged history is the published copy.
- **The summary line is ≤ ~50 chars, imperative mood** ("add", not "added" or "adds"). Read it as "this commit will _____."

---

## 6. Merge vs rebase: the one that confuses everyone

Both combine work from two branches. They differ in the *shape of history* they produce.

Start here — `main` advanced while you worked on your feature:

```
A ── B ── C ── F  ← main
      \
       D ── E  ← feature
```

### Merge

`git switch feature && git merge main` (or merging the PR) creates a **merge commit** `M` with *two* parents, tying the histories together:

```
A ── B ── C ── F ──── M  ← main (after PR merge)
      \              /
       D ── E ──────
```

- **Pro:** Non-destructive. Every original commit is preserved exactly; the merge commit records "these two lines of history joined here."
- **Con:** History becomes a braided graph. On a busy repo, `git log` turns into a railway diagram.

### Rebase

`git switch feature && git rebase main` *replays* your commits `D` and `E` on top of `main`'s latest commit, creating *new* commits `D'` and `E'`:

```
A ── B ── C ── F ── D' ── E'  ← feature (linear)
```

- **Pro:** Linear, clean history — as if you'd written your feature *after* `F` all along. `git log` reads like a story.
- **Con:** It **rewrites history** — `D'` and `E'` are new commits with new hashes. The originals `D`/`E` are abandoned.

### The Golden Rule of Rebasing

> **Never rebase commits that you have already pushed and that others may have based work on (i.e., shared `main` or a shared branch).** Rebasing rewrites history; if a teammate has the old commits, you've now forked reality and their next `pull` is a nightmare. Rebase your *own*, *unpushed* (or solo) feature branch freely. Never rebase shared/public history.

### Our policy in C3

- **Locally, on your own feature branch:** rebase onto `main` to stay current and to clean up your commits before review. This is the challenge this week.
- **Merging a PR to `main`:** use **squash merge** (GitHub's button). Your messy 9-commit feature branch collapses into one clean Conventional Commit on `main`. You get a linear, readable trunk history *and* the freedom to commit sloppily while you work.

That combination — rebase locally to stay current, squash-merge into trunk — gives you a `main` history where every commit is one reviewed, CI-green, named change. That's the goal.

---

## 7. Conflicts: what they are and how to resolve them

A **merge conflict** happens when two branches changed the *same lines* of the *same file* differently, and Git can't decide which to keep. Git is conservative: it never guesses. It stops and asks you.

You'll see conflict markers in the file:

```java
public Habit(String name, Frequency frequency) {
<<<<<<< HEAD
    this.name = Objects.requireNonNull(name, "name");
=======
    this.name = name == null ? "" : name.trim();
>>>>>>> feature/normalize-names
    this.frequency = frequency;
}
```

- Everything between `<<<<<<< HEAD` and `=======` is the version on *your current branch* (HEAD).
- Everything between `=======` and `>>>>>>> feature/...` is the version from the *incoming* branch.

To resolve:

1. **Open the file. Understand both sides.** Don't blindly pick one — read what each is trying to do. Often the right answer is a *combination*, not either side verbatim.
2. **Edit the file** to the correct final state and **delete all three marker lines**.
3. `git add <file>` to mark it resolved.
4. `git merge --continue` (or `git rebase --continue`).

```bash
git status                 # lists "both modified" files — your conflict to-do list
# ... edit each conflicted file, remove markers ...
git add src/main/java/dev/crunch/tracker/Habit.java
git rebase --continue
```

> **Three-way merge** is *why* Git resolves most conflicts automatically. It looks at three versions: your branch, their branch, and the **common ancestor** (the last commit both branches shared). If only one side changed a region, Git takes that side, no conflict. A conflict only happens when *both* sides changed the *same* region. The common-ancestor view is what makes Git smart instead of just diffing two files blindly. This week's challenge makes you feel this firsthand.

**Escape hatch:** anything went sideways? `git merge --abort` or `git rebase --abort` returns you to exactly where you started. And `git reflog` is your time machine — it logs every position `HEAD` has been, so you can `git reset --hard <reflog-sha>` back to any prior state. Nothing you committed is lost just because a pointer moved.

---

## 8. Pull requests: the unit of teamwork

A **pull request (PR)** proposes merging your branch into `main`, and is where review and CI happen. A good PR is small, focused, and described well.

### Anatomy of a good PR description

```markdown
## What
Adds the `Habit` domain record with name and frequency, plus validation
that rejects an empty name.

## Why
First slice of story #7 "Add a habit." The form and endpoint come in
follow-up PRs; this lands the core type and its invariants.

## How to test
- `mvn verify` is green.
- New tests: HabitTest covers happy path + empty-name rejection.

## Screenshots / output
BUILD SUCCESS · 0 errors · 0 test failures

Closes #7
```

`Closes #7` (or `Fixes #7`) auto-closes the linked issue when the PR merges — that's how the board card moves to Done automatically.

### What makes a PR reviewable

- **Small.** Under ~400 lines of diff is a good target. A reviewer reads a small PR carefully and a huge one carelessly.
- **One concern.** "Add habit + refactor logging + bump deps" is three PRs.
- **Green CI before you ask for review.** Don't make a human find what a robot would've caught.
- **A description that answers *why*.** The diff shows *what* changed; you must supply *why*.

---

## 9. Reviewing a PR like a senior engineer

Review is a skill, and a kind one. You're not gatekeeping; you're helping a teammate ship something better and catching the bug *before* prod does. The tone is "let's make this good together," never "gotcha."

### What to actually look at, in order

1. **Does it do what the story asked?** Read the linked issue's acceptance criteria first. Reviewing code without knowing the goal is reviewing in the dark.
2. **Correctness.** Edge cases, null handling, off-by-ones, error paths. Is there a test for the empty-name case the description claims?
3. **Tests.** Are the new behaviors covered? Would the tests actually fail if the code were wrong?
4. **Readability.** Will the next person understand this? Names, structure, dead code.
5. **Scope.** Is there anything in here that doesn't belong? Sneaky unrelated changes are a red flag.
6. **Style last.** A formatter (`mvn spotless:apply`, Week 3+) handles whitespace. Don't spend review capital on tabs.

### How to leave good comments

- **Be specific and actionable.** Not "this is bad" — "this NPEs when `name` is null; can we `Objects.requireNonNull` here?"
- **Distinguish blockers from nits.** Prefix optional suggestions with `nit:` so the author knows what *must* change vs what's a preference.
- **Ask, don't command, when unsure.** "Is there a reason this isn't `final`?" invites a conversation; "make this final" might miss context you don't have.
- **Praise the good parts.** "Nice — this test name reads like a spec." Review isn't only criticism.

### The three verdicts

GitHub gives you three review outcomes:

- **Approve** — good to merge (possibly with nits the author can take or leave).
- **Request changes** — there's a blocker; the author must address it before merging.
- **Comment** — feedback without a verdict (e.g. you're not the required reviewer).

> **The reviewer's prime directive:** would you be comfortable being paged at 3 a.m. for this code? If yes, approve. If a real bug or missing test would let it break in production, request changes. Everything else is a `nit:`.

---

## 10. Branch protection: making the rules enforced, not aspirational

"Always review before merging to `main`" is a nice agreement until someone pushes straight to `main` at midnight. **Branch protection** makes the rule mechanical. On GitHub, protect `main` to require:

- **A pull request before merging** — no direct pushes to `main`.
- **At least one approving review.**
- **Status checks to pass** — your CI workflow must be green before the merge button enables.
- **Branches up to date before merging** — the PR must include the latest `main` (catches "passed in isolation, breaks combined").
- Optionally: **linear history** (forces squash/rebase merges, no merge commits) and **signed commits**.

You'll configure this in the mini-project. Once it's on, the workflow from §4 isn't just *recommended* — it's the *only* way code reaches `main`. That's the point: the process is now the path of least resistance.

---

## 11. A safety net you should know exists

Three commands turn "I broke everything" into "I'll just step back":

- **`git reflog`** — every place `HEAD` has pointed, with SHAs. Your undo history for the repo itself. Lost a commit after a bad rebase? It's in the reflog; `git reset --hard <sha>` brings it back.
- **`git restore`** / **`git restore --staged`** — discard working-tree changes, or unstage, without touching commits.
- **`git stash`** — pocket your uncommitted changes to switch branches, then `git stash pop` to get them back.

> **The reassurance:** in Git, a commit you've made is not gone just because a branch pointer moved off it. As long as you committed (and within the ~90-day reflog window), it's recoverable. The genuinely dangerous operations are the ones that touch *uncommitted* work (`git reset --hard`, `git checkout .` over edits you never committed) and force-pushing over *shared* history. Commit early; that's your seatbelt.

---

## 12. Recap

You should now be able to:

- Describe a commit (immutable snapshot), a branch (movable pointer), and `HEAD` (you are here), and the three trees (working tree, index, HEAD).
- Run the **branch → commit → push → PR → review → merge** loop from the terminal.
- Write **Conventional Commit** messages and explain why the format pays off.
- Explain **merge vs rebase**, the **Golden Rule of Rebasing**, and our "rebase locally, squash-merge to trunk" policy.
- Resolve a **merge conflict** by understanding both sides, and recover with `--abort` / `reflog`.
- Open a **focused, well-described PR** and **review** one like a senior engineer.
- Configure **branch protection** so the workflow is enforced, not just agreed.

Next: put it all to work. The exercises take you from a blank machine to a JDK 21 Maven project shipped through a reviewed PR, and the challenge throws you a deliberately gnarly conflict and a messy branch to clean up.

---

## References

- *Pro Git — Branching in a Nutshell*: <https://git-scm.com/book/en/v2/Git-Branching-Branches-in-a-Nutshell>
- *Pro Git — Rebasing*: <https://git-scm.com/book/en/v2/Git-Branching-Rebasing>
- *Trunk-Based Development*: <https://trunkbaseddevelopment.com/>
- *Conventional Commits 1.0.0*: <https://www.conventionalcommits.org/en/v1.0.0/>
- *Atlassian — Merging vs Rebasing*: <https://www.atlassian.com/git/tutorials/merging-vs-rebasing>
- *GitHub — About protected branches*: <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
