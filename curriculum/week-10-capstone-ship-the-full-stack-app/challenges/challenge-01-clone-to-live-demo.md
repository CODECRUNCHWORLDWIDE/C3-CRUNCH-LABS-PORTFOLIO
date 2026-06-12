# Challenge 1 — From a clean clone to a live demo, in one documented pipeline run

> **Estimated time:** the capstone (the bulk of the week's 12 mini-project hours, plus the Friday delivery slot). **This is the assessed capstone.** No solution is provided — only acceptance criteria, because in production nobody hands you the answer key.

You will take the full Crunch Tracker stack — the Spring Boot API, the managed PostgreSQL database, and the React Native mobile client you have been compounding since Week 01 — from a **clean `git clone`** to a **live, sign-in-and-use demo**, through **one documented pipeline run**, and then write the `RUNBOOK.md` another engineer could deploy from cold. "Live" and "documented" are the operative words. The grader does not read your code and award points. The grader clones your repo, sets the secrets your runbook lists, triggers your pipeline, watches the API come up healthy against managed Postgres, installs your EAS build, signs in, creates a habit, checks in, and confirms the row landed in the database. If any of those steps fails — or requires a step that isn't in your runbook — the capstone does not pass, regardless of how good the code is.

This challenge is harder than the mini-project brief in two specific ways: (1) the deploy must run from a **clean clone with only the documented secrets** — nothing that lives only on your laptop or in your head — and (2) it must be **operable by someone else**, proven by a runbook a stranger can follow without asking you a question.

## The system

The full stack from `SYLLABUS.md` (and the mini-project brief), shipped:

- **API:** Spring Boot 3 / Java 21, CRUD for goals/habits/check-ins, JWT auth with BCrypt and per-user data ownership, `ProblemDetail` errors, validated request bodies. Containerized with a multi-stage Dockerfile, non-root, config entirely from the environment.
- **Database:** Managed PostgreSQL, schema built and versioned by Flyway (`ddl-auto: validate`), reachable over SSL.
- **CI/CD:** A GitHub Actions pipeline — test on every PR; on `main`, build + scan + push the image to GHCR + deploy — with no long-lived credential in the repo (built-in `GITHUB_TOKEN` for the registry, a deploy hook or OIDC for the host).
- **Mobile:** Expo + TypeScript, typed navigation, Zustand session store, token in `expo-secure-store`, TanStack Query against the live API, shipped as an installable EAS `preview` build whose API URL is the deployed host (one config seam, frozen at build time).
- **Observability:** `/actuator/health` the platform probes, structured JSON logs you can search.

## Acceptance criteria (the grader runs every one of these)

### Clone to running

- [ ] **A clean `git clone` plus the documented secrets becomes a running API through one pipeline run.** The grader sets the env vars/secrets your `RUNBOOK.md` lists, pushes to `main` (or re-runs the pipeline), and the API deploys without a manual step that isn't documented.
- [ ] **The API comes up healthy against managed Postgres.** `GET /actuator/health` returns `{"status":"UP"}` within a couple of minutes of deploy, and Flyway built the schema from empty (the grader can check `flyway_schema_history`).
- [ ] **`terraform`/infra is not required** — but every manual host setting (env vars, the database, the deploy hook) is documented so the deploy is reproducible.

### The working slice

- [ ] **Install → sign in → create a habit → check in → row in the database.** The grader installs the EAS `preview` build, registers/logs in, creates a habit, checks in, and confirms the `check_ins` row exists in managed Postgres, scoped to their user (Week 06 ownership held).
- [ ] **The shipped app talks to the live API, not localhost.** The grader's phone is not on your network; the build works anyway because the API URL is baked in at build time (Exercise 3 / Lecture 2).

### CI/CD

- [ ] **Tests gate the deploy.** A PR runs the test suite and shows green/red; only a merge to `main` ships. The grader can see this in the Actions tab.
- [ ] **No long-lived credential in the repo.** `git grep` finds no database password, JWT secret, or cloud key. The image push uses `GITHUB_TOKEN`; secrets live in GitHub Actions secrets and the host.

### Operability

- [ ] **`RUNBOOK.md` lets a stranger deploy from cold.** It lists every env var, the deploy command, the rollback command (with a time), and the "first five minutes" decision tree. The grader follows it without asking you anything.
- [ ] **Rollback is one command/action** and is demonstrably safe (migrations are expand/contract, so a code rollback never strands the schema).

### Delivery artifacts

- [ ] **One-page architecture diagram** (Mermaid or PNG) in the repo, every arrow labeled.
- [ ] **5-minute video walkthrough** tracing one check-in from the tap to the database row.
- [ ] **Cost note**: a sentence on what the running system costs (free tier vs a few dollars) and the SPOF (single-region managed DB).
- [ ] **Portfolio paragraph** linking the live demo and the video.
- [ ] **Live architecture review delivered** (Friday slot): you present, you trace a check-in, you answer the senior-engineer questions, you produce the risk list.

## How you are graded

This challenge maps to the **Capstone delivery (25%)** line of the assessment matrix, plus the **Mock interview (5%)** and the **live review (5%)**. The single hardest gate is the **cold start**: a system you cannot deploy from a clean clone with documented secrets is not a system you operate, and it does not pass. Build the runbook from day one — every time you set an env var or run a deploy command by hand, write it down. The grader will follow only what's written, and a deploy that needs a tribal-knowledge step is an automatic gap.

## What "open-ended" means here

There is no single right deployment. Within the spec, you make and defend choices: which host (Render vs Railway vs Fly), whether the DB is bundled with the host or a separate Neon/Supabase instance, deploy-hook vs OIDC for the deploy step, `apk` vs internal iOS distribution for the demo, whether to add a staging environment. The challenge is not to match a reference; it is to make defensible choices, prove the slice works under a cold-start deploy, and explain the tradeoffs in the review. The runbook and the self-named risks (Lecture 2, §2.8) are where you demonstrate that you understand the choices you made — which is, in the end, the entire point of the capstone.
