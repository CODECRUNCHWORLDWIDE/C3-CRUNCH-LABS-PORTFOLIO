# Challenge 1 — Resolve a Gnarly Conflict and Rebase a Messy Branch Clean

**Time estimate:** ~90 minutes.

## Problem statement

You're going to manufacture the exact situation that makes people panic — a three-way merge conflict on the same lines of the same file — resolve it *correctly* (keeping the intent of both sides), and then take a branch with embarrassing, half-baked commits and rebase it into a history you'd be proud to put in a PR.

This is a closed exercise: you create the mess yourself with the steps below, so everyone hits the same conflict. The skill is in the resolution, not the setup.

You'll work in a tiny throwaway repo. No Maven needed — a couple of Java files are enough to make a real conflict.

---

## Part A — Set up the battlefield

```bash
mkdir conflict-arena && cd conflict-arena
git init

# A trivial domain file we'll fight over.
cat > Habit.java <<'EOF'
package dev.crunch.tracker;

public record Habit(String name, String frequency) {
    public Habit {
        // baseline: no validation yet
    }
}
EOF

git add Habit.java
git commit -m "feat(habit): add baseline Habit record"
```

Now create **two branches that change the same region differently** — this is what guarantees a conflict (a one-sided change would auto-merge).

```bash
# Branch 1: strict, fail-fast validation.
git switch -c feature/strict-validation
cat > Habit.java <<'EOF'
package dev.crunch.tracker;

import java.util.Objects;

public record Habit(String name, String frequency) {
    public Habit {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
EOF
git commit -am "feat(habit): reject null/blank names"

# Branch 2 off main: lenient, normalize instead of reject.
git switch main
git switch -c feature/normalize-names
cat > Habit.java <<'EOF'
package dev.crunch.tracker;

public record Habit(String name, String frequency) {
    public Habit {
        name = name == null ? "" : name.trim();
        frequency = frequency == null ? "daily" : frequency;
    }
}
EOF
git commit -am "feat(habit): normalize names and default frequency"
```

You now have two branches that both rewrote the canonical constructor. They cannot both land cleanly.

---

## Part B — Trigger and resolve the conflict

Pretend `feature/strict-validation` merged to `main` first. Bring it in, then try to integrate your `feature/normalize-names` work on top via **rebase** (the realistic "main moved under me" scenario):

```bash
git switch main
git merge feature/strict-validation        # fast-forwards main to the strict version
git switch feature/normalize-names
git rebase main                             # <-- this conflicts
```

Git stops with a conflict in `Habit.java`. Open it; you'll see all three regions (HEAD = the strict version now on main, the incoming = your normalize commit).

**Resolve it by combining intent, not by picking a winner.** The correct final behavior: *normalize* whitespace AND *reject* a name that is blank after trimming, and default the frequency. That respects both authors:

```java
package dev.crunch.tracker;

import java.util.Objects;

public record Habit(String name, String frequency) {
    public Habit {
        Objects.requireNonNull(name, "name");
        name = name.trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        frequency = frequency == null || frequency.isBlank() ? "daily" : frequency.trim();
    }
}
```

Then mark it resolved and continue:

```bash
# remove ALL conflict markers first, then:
git add Habit.java
git rebase --continue
```

If you panic at any point: `git rebase --abort` puts you exactly back where you started. Nothing is lost.

> **Verify your resolution is real:** there must be zero `<<<<<<<`, `=======`, `>>>>>>>` lines left anywhere. `grep -rn '<<<<<<<\|=======\|>>>>>>>' .` should print nothing.

---

## Part C — Make a mess, then clean it with interactive rebase

Real feature work produces sloppy local commits. Manufacture some on a fresh branch, then reshape them.

```bash
git switch -c feature/streak-counter

echo "// streak: count consecutive check-ins" >> Habit.java
git commit -am "wip"

echo "public int placeholder() { return 0; }" >> Habit.java
git commit -am "more wip"

# oops, a typo fix on the previous line
git commit -am "fix typo" --allow-empty

echo "// TODO real streak logic next week" >> Habit.java
git commit -am "asdf"
```

Four garbage commits (`wip`, `more wip`, `fix typo`, `asdf`). Reshape them into **one clean commit** with a real message using an interactive rebase over the four commits since `main`:

```bash
git rebase -i main
```

In the editor, `squash` (or `fixup`) the lower three into the first, then rewrite the combined message to something Conventional, e.g.:

```
feat(habit): scaffold streak-counter placeholder

Adds a placeholder for the consecutive check-in streak; real logic
lands in Week 2 once CheckIn exists.
```

> **Note:** because interactive rebase is blocked in some automated/CI shells, you can achieve the same result non-interactively with a soft reset:
>
> ```bash
> git reset --soft main
> git commit -m "feat(habit): scaffold streak-counter placeholder"
> ```
>
> Both approaches collapse the four messy commits into one clean one. Use whichever your environment allows; understand that they produce the same outcome.

Confirm the history is now linear and clean:

```bash
git log --oneline main..feature/streak-counter
```

You should see exactly **one** well-named commit.

---

## Part D — Write the recovery narrative

Create `RECOVERY.md` in the repo explaining, in your own words:

1. **What conflicted and why** — which lines, and why Git couldn't auto-merge them (hint: both branches changed the same region; the common ancestor had neither change).
2. **How you resolved it** — why you combined both intents rather than picking one side.
3. **The cleanup** — what the four messy commits were and how you collapsed them.
4. **The safety net** — name two commands that would have rescued you if a step went wrong (`git rebase --abort`, `git reflog`).

Commit it:

```bash
git add RECOVERY.md
git commit -m "docs: narrate the conflict resolution and history cleanup"
```

---

## Acceptance criteria

- [ ] `Habit.java` on `feature/normalize-names` reflects the **combined** behavior (normalize *and* reject blank *and* default frequency) — verifiable by reading it.
- [ ] No conflict markers remain anywhere (`grep` check passes).
- [ ] `feature/streak-counter` has exactly **one** commit since `main`, with a Conventional message.
- [ ] `git log --oneline` on each branch is linear and readable — no `wip`/`asdf` survivors.
- [ ] `RECOVERY.md` exists and addresses all four points above.
- [ ] You can `git rebase --abort` from memory and explain what `git reflog` shows.

---

## Stretch

- Re-run Part B but resolve it with a **merge** instead of a rebase, and compare the resulting `git log --graph` shapes. Which history would you rather review?
- Deliberately make a *worse* resolution (pick only one side), commit it, then use `git reflog` + `git reset --hard <sha>` to undo it and redo it correctly. Now you trust the safety net.
- Add a JUnit 5 test that proves the combined `Habit` behavior: a name `"  water  "` becomes `"water"`, a blank name throws, and a null frequency defaults to `"daily"`. Wire it under a real Maven project and confirm `mvn verify` is green.

## Submission

Commit the `conflict-arena` repo (or just `Habit.java` + `RECOVERY.md`) under `challenges/challenge-01/` in your Week 1 GitHub repo. The graders read `RECOVERY.md` and the `git log` of each branch.

## Why this matters

Every week from here, `main` moves while you work. You will rebase onto it, you will hit conflicts, and you will sometimes make a local mess you need to clean before review. Engineers who fear this `git pull` blindly and ship broken merges; engineers who've done this once stay calm, read both sides, and reshape history on purpose. Ninety minutes now buys you a year of not panicking.
