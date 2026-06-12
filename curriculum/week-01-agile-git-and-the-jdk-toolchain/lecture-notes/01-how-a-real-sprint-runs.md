# Lecture 1 — How a Real Sprint Runs: Scrum, Kanban, and the Ceremonies That Earn Their Keep

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can explain Agile vs Scrum vs Kanban without conflating them, name every Scrum ceremony and what it's *for*, describe the three roles and two artifacts, and carve a vague feature into INVEST user stories with acceptance criteria.

If you only remember one thing from this lecture, remember this:

> **Agile is a set of values. Scrum is a framework. Kanban is a method. They are three different things, and most teams that say "we do Agile" actually mean "we run a watered-down Scrum on a Kanban board." Knowing the difference is the difference between cargo-culting ceremonies and using them on purpose.**

We are going to spend Week 1 on process before we write business logic on purpose. The single biggest predictor of whether a junior engineer ships is not how fast they type — it's whether they can take a fuzzy request, slice it into something deliverable, and move it across a board without surprising their team. That's a learnable skill, and this is where you learn it.

---

## 1. The three things people call "Agile"

Walk into three different shops and ask "how do you work?" You'll hear "we're Agile," "we do Scrum," and "we're on Kanban" — sometimes from people sitting next to each other. They are describing different layers.

| Layer | Example | What it is |
|-------|---------|-----------|
| Values | **Agile** | Four values and twelve principles. A philosophy, not a process. You cannot "install" it. |
| Framework | **Scrum** | A concrete framework: fixed roles, events, artifacts, on a sprint cadence. |
| Method | **Kanban** | A flow-based method: visualize work, limit work-in-progress, optimize cycle time. |

**Agile** is the 2001 Manifesto: 68 words of values ("individuals and interactions over processes and tools," "working software over comprehensive documentation," "customer collaboration over contract negotiation," "responding to change over following a plan") plus twelve principles. That's it. It deliberately does not tell you *how* to work. Anyone selling you "the Agile process" is selling you something the Manifesto's authors explicitly didn't write.

**Scrum** is the most popular *implementation* of those values. It's a lightweight framework — the entire Scrum Guide is about 13 pages — that prescribes a small set of roles, events, and artifacts and then gets out of the way. Scrum is intentionally incomplete: it tells you to hold a retrospective but not how to run it.

**Kanban** comes from lean manufacturing (Toyota) by way of David Anderson. It doesn't prescribe roles or time-boxes. It prescribes a board that makes work visible, explicit limits on how much work can be in progress at once, and a relentless focus on *flow* — how fast a card travels from "to do" to "done."

> **Why this matters in code review.** When a teammate says "let's add a WIP limit to the in-progress column," they're pulling a Kanban idea. When they say "this isn't ready for the sprint, it's not refined," that's Scrum vocabulary. Knowing which framework a word comes from tells you what problem they're trying to solve.

Most real teams are hybrids — "Scrumban." They run sprints (Scrum) on a board with WIP limits (Kanban). That's fine. The goal is never purity; it's shipping.

---

## 2. The Agile values, made concrete

The Manifesto sounds like a motivational poster until you connect each value to a daily decision. Here's the translation table we actually use.

| Manifesto value | What it means on a Tuesday |
|-----------------|----------------------------|
| Individuals and interactions over processes and tools | If two people are blocked, they talk — they don't file a ticket and wait. |
| Working software over comprehensive documentation | A merged, tested feature beats a 20-page design doc for a feature that doesn't exist. |
| Customer collaboration over contract negotiation | When the requirement is ambiguous, you ask the person who wants it — you don't guess and ship. |
| Responding to change over following a plan | A sprint plan is a hypothesis, not a promise. New information changes the plan. |

Note the word "over." The Manifesto values the things on the right too — it just values the things on the left *more* when they conflict. "We're Agile so we don't write docs" is a misread; "we write the docs that help and skip the ones that don't" is the point.

---

## 3. Scrum: the sprint

The heartbeat of Scrum is the **sprint** — a fixed time-box, usually one to four weeks (two is the default and what we use in C3). At the end of every sprint, the team has a **potentially shippable increment**: software that *could* go to production, even if the business chooses not to ship it yet.

Three properties make a sprint a sprint:

1. **Fixed length.** You don't extend a sprint because work isn't done. Unfinished work goes back to the backlog. The time-box is sacred precisely because it forces the team to slice work small enough to fit.
2. **No scope changes mid-sprint** (in classic Scrum). Once the sprint starts, the sprint backlog is the team's to manage; the Product Owner doesn't drop new "urgent" work into the middle of it. Genuine emergencies are handled by *canceling* the sprint, not quietly cramming.
3. **A shippable increment at the end.** "We made progress" is not the deliverable. Something *done* is.

> **The mistake juniors make:** treating a sprint as a deadline ("the feature is due Friday"). A sprint is a *cadence*, not a deadline. If the feature doesn't fit, you slice it. The sprint length never moves.

---

## 4. The two Scrum artifacts

Scrum has exactly two primary artifacts you need this week (plus the increment itself, which is the third).

### Product Backlog

The single, ordered list of everything that might ever get built — features, fixes, tech debt, spikes. It is:

- **Owned by the Product Owner**, who alone decides the *order*.
- **Ordered by value/priority**, not by who shouted loudest.
- **Emergent** — it's never "finished." It grows and re-orders as you learn.
- **Refined continuously** — "backlog refinement" (a.k.a. grooming) is the ongoing act of adding detail, estimates, and acceptance criteria to upcoming items so they're ready when a sprint needs them.

The top of the backlog should always be the most ready, most valuable, most clearly-specified work. The bottom can be one-line ideas.

### Sprint Backlog

The subset of the Product Backlog the team commits to *this* sprint, plus a plan for delivering it. The team pulls items off the top of the product backlog during sprint planning until they've taken on as much as they believe they can finish. Once chosen, the *team* owns it.

For Crunch Tracker, your product backlog this week will hold 6+ INVEST stories. Your first sprint backlog (in Week 2) will pull a few of them in.

---

## 5. The three Scrum roles (now "accountabilities")

The 2020 Scrum Guide renamed "roles" to "accountabilities," but the substance is the same. There are three, and exactly three.

### Product Owner (PO)

Owns *what* gets built and in what order. Maximizes the value of the product. Maintains and orders the Product Backlog. Says no a lot. One person, not a committee — a backlog with two owners has zero owners.

### Scrum Master (SM)

Owns *how well the team runs the process*. Not a manager; a coach and an impediment-remover. Facilitates the events, protects the team from mid-sprint scope creep, and helps the team improve. In small teams this is often a rotating or part-time hat.

### Developers

Own *how* the work gets built and *the quality of the increment*. "Developers" in Scrum means everyone doing the building — backend, frontend, QA, design. They self-organize: nobody outside the team tells a developer which task to pick up next. The team is cross-functional and small (the Guide says 10 or fewer).

> **In C3:** you'll often play all three hats on a solo or pair project. That's normal for a portfolio repo. The point is to *understand* the separation so that when you join a real team, you know whose decision is whose. The PO decides *whether* to build the dark-mode toggle. The developers decide *how*. Confusing those is how teams thrash.

---

## 6. The five Scrum events (the "ceremonies")

People roll their eyes at "ceremonies" because they've sat through bad ones. A good ceremony earns its time by replacing a worse, ad-hoc version of the same conversation. Here's each one, what it's *for*, and how it fails.

### 6.1 The Sprint (the container)

Technically the sprint is the event that contains the other four. Length is fixed; we use two weeks.

### 6.2 Sprint Planning

- **When:** Start of the sprint.
- **Time-box:** Up to ~4 hours for a two-week sprint.
- **Who:** The whole team.
- **For:** Answering two questions — *what* can we deliver this sprint (the PO presents the top of the backlog; the team pulls what it can commit to), and *how* will we do it (the team breaks stories into tasks).
- **Output:** A sprint goal and a sprint backlog.
- **How it fails:** Planning with un-refined stories. If the team is discovering for the first time what "add a habit" even means, planning becomes design-by-committee and runs four hours over. Fix: refine *before* planning.

### 6.3 Daily Scrum (standup)

- **When:** Every day, same time. 15 minutes, time-boxed, hard stop.
- **Who:** The developers (PO/SM optional).
- **For:** Re-planning the next 24 hours toward the sprint goal. Surfacing blockers *today*, not Friday.
- **The classic three questions** (a useful default, not a mandate): What did I do yesterday? What will I do today? What's in my way?
- **How it fails:** It becomes a status report *to a manager* instead of coordination *among peers*. Tell: everyone talks to the SM instead of each other. Another failure: solving the blocker *in* the standup for 20 minutes while six people watch. Fix: "let's take that offline after."

### 6.4 Sprint Review

- **When:** End of the sprint.
- **Time-box:** Up to ~2 hours for a two-week sprint.
- **Who:** Team + stakeholders.
- **For:** *Demonstrating the working increment* and gathering feedback that feeds the backlog. The operative word is **demo** — running software, not slides.
- **How it fails:** Death by PowerPoint. If you're showing slides instead of a running app, you're hiding that the increment isn't done.

### 6.5 Sprint Retrospective

- **When:** After the review, before the next planning.
- **Time-box:** Up to ~1.5 hours.
- **Who:** The team (a safe space — usually just the team).
- **For:** Inspecting *how the team worked* (not the product) and committing to one or two concrete improvements for next sprint. Process, tools, communication, friction.
- **A simple format:** What went well? What didn't? What will we change? Pick at most **one or two** action items — a retro that produces fifteen action items produces zero.
- **How it fails:** No action items, or the same complaints every sprint with nothing changing. A retro that doesn't change behavior is theater.

> **Memory aid for the cadence:** **Plan** the sprint → **standup** daily → **review** the product → **retro** the process. Plan-build-show-improve, on repeat.

---

## 7. Kanban: when flow beats cadence

Scrum is great when work arrives in plannable chunks. It's a poor fit for work that's interrupt-driven and unpredictable — think a platform/on-call team, or a support queue. That's where **Kanban** shines.

Kanban has no sprints, no required roles, no estimation ritual. It has these practices:

1. **Visualize the workflow.** A board with columns for each real stage: *Backlog → Ready → In Progress → In Review → Done*. Every piece of work is a card. You can't manage invisible work.
2. **Limit work-in-progress (WIP).** Each column gets a number. "In Progress: max 3." When it's full, you can't start new work — you must *finish* something first. This is the heart of Kanban and the part teams skip and then wonder why nothing ships.
3. **Manage flow.** Measure **cycle time** (how long a card takes from "started" to "done") and **lead time** (from "requested" to "done"). Make flow smooth and predictable, not maximally busy.
4. **Make policies explicit.** What does "Done" mean? When can a card move to "In Review"? Write it on the board.
5. **Improve collaboratively.** Use the data (cycle time, WIP) to find and fix bottlenecks.

> **Why WIP limits feel wrong and are right.** A team's instinct is "start everything so nobody's idle." But a card half-done helps no one — it's inventory, not value. WIP limits force you to *finish* before you *start*, which counterintuitively makes the whole team faster, because half-finished work isn't piling up waiting and going stale. "Stop starting, start finishing."

**Pull, not push.** In Kanban you *pull* the next card when you have capacity, rather than having work *pushed* onto you. A developer finishing a card looks at the board and pulls the top of the next column — they don't get assigned.

### Scrum vs Kanban at a glance

| | Scrum | Kanban |
|---|-------|--------|
| Cadence | Fixed sprints | Continuous flow |
| Roles | PO, SM, Developers | None prescribed |
| Key limit | Sprint backlog (per sprint) | WIP limit (per column) |
| Key metric | Velocity (points/sprint) | Cycle time |
| Change mid-flight | Avoided in-sprint | Anytime |
| Best for | Plannable feature work | Interrupt-driven / support |

For Crunch Tracker we use a **Scrumban** board: a GitHub Projects board with columns and a WIP limit on *In Progress*, planned in two-week sprints. You get Kanban's visibility and Scrum's rhythm.

---

## 8. The board, concretely

Here's the board you'll stand up for the mini-project. Columns flow left to right; a WIP limit on the working columns keeps it honest.

```
┌────────────┬────────────┬──────────────┬──────────────┬──────────┐
│  Backlog   │   Ready    │ In Progress  │  In Review   │   Done   │
│            │            │  (WIP ≤ 2)   │  (WIP ≤ 2)   │          │
├────────────┼────────────┼──────────────┼──────────────┼──────────┤
│ #12 Goals  │ #7 Add a   │ #4 Repo +    │ #3 PR        │ #1 Init  │
│   reminder │   habit    │   CI build   │   template   │   README │
│ #11 Export │ #6 List    │              │              │ #2 Maven │
│   CSV      │   habits   │              │              │   skel.  │
└────────────┴────────────┴──────────────┴──────────────┴──────────┘
```

- A card enters at **Backlog** (an idea, maybe one line).
- It moves to **Ready** only when it's *refined*: has acceptance criteria, is INVEST-clean, and a developer could start it cold.
- **In Progress** has a WIP limit. Hit the limit? Finish something before pulling more.
- **In Review** means a PR is open and waiting on a reviewer.
- **Done** means merged, CI-green, and meeting the Definition of Done.

> **Definition of Done (DoD):** a team-wide checklist that every card must satisfy to be "Done." Ours for C3: *code merged to `main`, CI green, tests for new logic, no new warnings, and the acceptance criteria demonstrably met.* "Done" is not "it works on my branch."

---

## 9. User stories and INVEST

A **user story** is a small, user-centered slice of value, traditionally phrased:

> **As a** \<role\>, **I want** \<capability\>, **so that** \<benefit\>.

For Crunch Tracker:

> As a user, I want to add a habit with a name and a target frequency, so that I can start tracking it.

The "so that" is not decoration — it's the *why*, and it's what lets you cut scope intelligently when time runs short. If you know *why* a story exists, you know which parts are essential.

A good story passes **INVEST** (Bill Wake):

| Letter | Criterion | Smell that it fails |
|--------|-----------|---------------------|
| **I** | **Independent** | "Story B can't start until A and C and D are done." |
| **N** | **Negotiable** | The story dictates the exact UI/implementation, leaving no room to collaborate. |
| **V** | **Valuable** | "Refactor the data layer" — valuable to *whom*? No user sees it. |
| **E** | **Estimable** | The team has no idea how big it is because it's too vague. |
| **S** | **Small** | "Build the entire app." Can't finish in a sprint, let alone a day or two. |
| **T** | **Testable** | No acceptance criteria; you can't tell when it's done. |

### Acceptance criteria

Every Ready story needs **acceptance criteria** — the testable conditions that make it "done." A common, clean format is **Given/When/Then**:

```
Story: Add a habit

Given I am on the Add Habit screen
When  I enter the name "Drink water" and frequency "daily" and tap Save
Then  the habit "Drink water" appears in my habit list
And   the list persists if I reopen the app

Given I am on the Add Habit screen
When  I tap Save with an empty name
Then  I see a validation error and the habit is not created
```

Notice this story doesn't say *how*. It doesn't mention React Native, Spring, or a database. It describes observable behavior. The *how* is the developers' call — that's the "Negotiable" in INVEST.

### Story vs task

A **story** is user-visible value. A **task** is an engineering step toward it, sized to finish in a day or less.

> Story: *Add a habit* (#7)
> - [ ] Task: design the `Habit` form fields (name, frequency)
> - [ ] Task: add the `POST /habits` endpoint contract (stub)
> - [ ] Task: wire the Save button to local state
> - [ ] Task: add validation for empty name
> - [ ] Task: write the acceptance test for the happy path

Tasks live *inside* a story. You'll do this slicing in Exercise 1 and again in the mini-project.

---

## 10. Estimation, briefly

Two questions come up immediately: how big is this, and how much can we take on?

- **Story points** are a *relative* size estimate (often a Fibonacci-ish scale: 1, 2, 3, 5, 8, 13). A 5-point story is roughly five times a 1-pointer. Points fold together complexity, effort, and uncertainty — deliberately *not* hours, because humans are bad at hour estimates and good at "this is bigger than that."
- **Velocity** is how many points a team actually completes per sprint, averaged over recent sprints. It's a *forecasting* tool for the team, not a productivity score for managers (the fastest way to ruin velocity is to weaponize it — teams just inflate their points).
- **Planning poker** is the ritual: everyone privately picks a size, reveals simultaneously, and the outliers explain. The disagreement is the value — it surfaces a hidden assumption.

For Week 1 you don't need velocity yet (you have no history). You *will* point your six stories, just to practice relative sizing. Don't agonize — the conversation matters more than the number.

---

## 11. Putting it together: a day in a sprint

Concretely, here's what your week *as a team member* looks like once a sprint is running (Week 2 onward):

1. **9:30 standup (15 min).** "Yesterday I finished the habit form; today I'll wire validation; I'm blocked on the endpoint contract — can someone pair after?" Coordination, not status.
2. **Pull a card.** You finished yours; In Progress is under its WIP limit; you pull the top Ready card.
3. **Branch, build, commit.** (Lecture 2 — the Git half of this week.) Short-lived branch, small commits, open a PR.
4. **Request review.** Card moves to In Review. A teammate reviews; CI runs.
5. **Merge.** Green CI + an approval → merge to `main`. Card → Done. DoD satisfied.
6. **Repeat** until the sprint ends, then **review** (demo the increment) and **retro** (improve the process).

Every later week of C3 runs on this loop. Week 1 is where you learn the loop with a trivial increment ("hello, JDK 21" in CI) so that in Week 4, when the increment is a real REST API, the *process* is muscle memory and you can spend your brain on the code.

---

## 12. Agile anti-patterns you'll meet in the wild

You will join teams that say "we're Agile" and aren't. Recognizing the failure modes early lets you ask the right question instead of absorbing dysfunction as "just how it works."

- **Cargo-cult Scrum.** Running every ceremony by rote with no understanding of *why*. Tell: a 45-minute standup where everyone reads status to a manager. The fix is always to return to the event's purpose — a standup exists to *re-plan the day and surface blockers*, full stop.
- **The backlog of everything.** A 600-item backlog nobody has ordered. If the top isn't clearly the most valuable, refined work, the backlog isn't doing its one job. A backlog you can't act on is a junk drawer.
- **Velocity as a productivity score.** The moment a manager rewards high velocity, teams inflate their points and the number becomes meaningless. Velocity is a *forecasting* tool *for the team*, never a performance metric *about* the team.
- **Stories that are really tasks.** "Add a database column" is not a user story — no user wants a column. It's a task inside a story. If there's no "so that \<benefit\>," you're probably looking at a task wearing a story costume.
- **Done that isn't done.** "It's done, I just need to write the tests / merge it / handle the error case." That's not done — that's "done-ish," and done-ish work is how a sprint that looked finished ships nothing. The Definition of Done exists precisely to kill the word "just."
- **The retro that changes nothing.** Same complaints every two weeks, zero behavior change. A retrospective that produces no acted-on improvement is a venting session with a calendar invite. One real change per sprint beats fifteen aspirations.

The throughline: every Agile practice is a tool aimed at a problem. When you see a practice that's become ritual, ask "what problem was this *for*?" — and you'll usually find the team drifted from it.

## 13. Recap

You should now be able to:

- Distinguish **Agile** (values), **Scrum** (framework), and **Kanban** (method).
- Name the **five Scrum events**, what each is for, and how each fails.
- Name the **three roles** and **two artifacts**, and say whose decision is whose.
- Explain a **WIP limit** and why "stop starting, start finishing" makes a team faster.
- Write a **user story** with a "so that," check it against **INVEST**, give it **Given/When/Then** acceptance criteria, and slice it into day-sized **tasks**.
- Describe the **Definition of Done** and why "works on my branch" isn't it.

Next up: the other half of how a team stays coordinated — Git. Continue to [Lecture 2 — Git for Teams](./02-git-for-teams.md).

---

## References

- *The Scrum Guide* (2020): <https://scrumguides.org/scrum-guide.html>
- *The Agile Manifesto*: <https://agilemanifesto.org/>
- *Kanban Guide*: <https://kanbanguides.org/>
- *INVEST in Good Stories* — Bill Wake: <https://www.mountaingoatsoftware.com/blog/invest-in-good-stories-and-smart-tasks>
- *GitHub Projects* docs: <https://docs.github.com/en/issues/planning-and-tracking-with-projects>
