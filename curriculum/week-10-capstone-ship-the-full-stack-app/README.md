# Week 10 — Capstone: Ship the Full-Stack App

Welcome to the last week of **C3 · Crunch Labs Portfolio**. You do not learn a new framework this week. You ship.

Everything you have built since Week 01 — the JDK 21 toolchain and the team repo, the records-and-sealed-types domain model, the JUnit-tested services behind interfaces, the Spring Boot REST surface, the JPA/Postgres persistence with Flyway, the JWT auth with per-user data ownership, the Expo + TypeScript mobile client, the navigation and Zustand state, and the TanStack Query integration against the live API — gets assembled into one running, deployed product: **Crunch Tracker**, reachable on the public internet, with an installable mobile build pointed at it. Then you defend it.

"Defend it" is not a metaphor. This week you run a real architecture review: you stand in front of peers (or a cohort lead, or a recorded camera a hiring manager will eventually watch), you walk a single check-in from the phone tap all the way to the row in Postgres and back, and you answer the questions a senior engineer asks when they decide whether to trust your system in production. You produce a 5-minute demo video, a `RUNBOOK.md` another engineer could deploy from cold, a one-paragraph portfolio writeup linking the live demo, and a containerized API that goes from a clean `git clone` to a live, signed-in, working app through one documented GitHub Actions pipeline run.

The week has a rhythm: containerize and deploy early, wire CI/CD and prove the pipeline mid-week, then build the installable mobile artifact and deliver the live review at the end. If your Week 09 integration did not actually talk to the API over the network — if it only worked against `localhost` on your laptop — fix that first, because this week the API runs on someone else's machine and the phone has to find it.

The capstone is graded on a **working end-to-end slice**, not on slides. A grader will clone your repo, run your pipeline, install your build, sign in, create a habit, check in, and watch the row appear in your managed database. If that slice works and tears down cleanly, you pass. This is the week the course has been building toward. Treat it like a release.

## Learning objectives

By the end of this week, you will be able to:

- **Containerize** a Spring Boot 3 application with a multi-stage Dockerfile that produces a small, layered, non-root image that starts in seconds and reads all config from the environment.
- **Deploy** that container to a managed container host, backed by a managed PostgreSQL instance, with Flyway migrations running on startup against the real database.
- **Wire** a GitHub Actions pipeline that builds, tests, scans, pushes the image, and deploys on a push to `main` — using OIDC, not a long-lived cloud key checked into the repo.
- **Produce** an installable Expo/EAS build (a real `.apk`/`.ipa` or an internal-distribution link) configured at build time to point at the live API rather than `localhost`.
- **Configure** production concerns that were toy-grade until now: externalized secrets, CORS for the deployed origin, a JWT signing key that is not committed, a health check the platform can probe, and structured logs you can read after the fact.
- **Write** a `RUNBOOK.md` that another engineer can deploy from cold — environment variables, the deploy command, the rollback command, and the "it's broken, what do I look at first" decision tree.
- **Present** the system in a live review, trace one request through the whole stack on screen, and answer the senior-engineer questions about failure modes, data safety, secrets, cost, and rollback without flinching.
- **Record** a 5-minute walkthrough a hiring manager can watch and a peer can reproduce, and write the portfolio paragraph that links the live demo.

## Prerequisites

This week assumes you have completed Weeks 01–09 of C3 and that those mini-projects produced a working, version-controlled Crunch Tracker. Specifically, you need:

- A Spring Boot 3.x API (Java 21) with CRUD for goals, habits, and check-ins, validated request bodies, and `ProblemDetail` errors. (Weeks 04, 02–03.)
- Spring Data JPA over PostgreSQL with **Flyway migrations** that build the schema from empty. (Week 05.) If your schema only exists because Hibernate `ddl-auto` created it, fix that this week — production runs `validate`, not `update`.
- JWT auth: registration, login, BCrypt-hashed passwords, per-user data ownership enforced and tested, and CORS configured. (Week 06.)
- An Expo + TypeScript mobile client with navigation, a Zustand session store, and `expo-secure-store` for the token. (Weeks 07–08.)
- A TanStack Query integration layer that logs in, loads, and mutates the signed-in user's data against the API — with a single place where the API base URL is configured. (Week 09.)

If any of those is missing or broken, this week will expose it. That is the point. The most common gap is a base URL hard-coded to `http://localhost:8080` in five files; the integration that survives deployment reads it from one config value.

## Topics covered

- How a real architecture review runs: the agenda, the artifacts, the questions, and the move that reads most senior — naming your own biggest risk before anyone asks.
- The multi-stage Dockerfile for Spring Boot: build stage with the JDK, runtime stage with a slim JRE, layered jars for fast rebuilds, a non-root user, and a `HEALTHCHECK`.
- Twelve-factor config: every secret and connection string comes from the environment, not from `application.yml`; the same image runs in dev, staging, and prod.
- Managed Postgres: provisioning, the connection string, running Flyway against it on startup, and why `ddl-auto: validate` is the only safe production setting.
- The CI/CD pipeline: build and test on every PR, then on a push to `main` build the image, scan it, push it to a registry, and deploy — authenticated with OIDC so no cloud key lives in the repo.
- Releasing the mobile app with EAS: `eas build`, build profiles, the `extra` config that bakes the live API URL into the binary, and internal distribution for the demo.
- Production hardening of things that were fine on `localhost`: CORS for the real origin, the JWT secret as an env var, the health endpoint, and structured JSON logs.
- The runbook discipline: what a cold-start deploy needs, the rollback command, and the "3am, it's down, what do I look at" first-five-minutes tree.
- The 5-minute demo video and the portfolio writeup: what to show, what to skip, and how to trace one check-in on camera.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a contract; the capstone deserves whatever it takes.

| Day       | Focus                                                       | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|------------------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | Containerize the API; the architecture-review playbook     |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     1.5h     |    0h      |     6.5h    |
| Tuesday   | Deploy: managed Postgres + container host, Flyway on start |    1h    |    2h     |     0.5h   |    0.5h   |   1h     |     1.5h     |    0h      |     6.5h    |
| Wednesday | Wire the GitHub Actions deploy pipeline (CI/CD)            |    1h    |    2h     |     1h     |    0.5h   |   1h     |     1.5h     |    0h      |     7h      |
| Thursday  | EAS build pointed at the live API; the runbook             |    0h    |    1.5h   |     0.5h   |    0.5h   |   1h     |     2h       |    0.5h    |     6h      |
| Friday    | Record the demo video; deliver the live architecture review|    0h    |    0h     |     0h     |    0.5h   |   0h     |     3h       |    0.5h    |     4h      |
| Saturday  | Portfolio writeup; mock interview; clean redeploy drill    |    0h    |    0h     |     0h     |    0h     |   1h     |     2h       |    0.5h    |     3.5h    |
| Sunday    | Quiz, retrospective, course wrap                           |    0h    |    0h     |     0h     |    1h     |   0h     |     0.5h     |    0.5h    |     2h      |
| **Total** |                                                            | **4h**   | **7h**    | **2.5h**   | **4h**    | **5h**   | **12h**      | **2.5h**   | **36h**     |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./README.md) | This overview (you are here) |
| [resources.md](./resources.md) | Docker, GitHub Actions, EAS, managed-Postgres references; deploy and runbook templates |
| [lecture-notes/01-from-repo-to-running.md](./lecture-notes/01-from-repo-to-running.md) | Dockerizing Spring Boot, deploying the API to a managed host, and the CI/CD pipeline |
| [lecture-notes/02-shipping-the-mobile-app-and-the-runbook.md](./lecture-notes/02-shipping-the-mobile-app-and-the-runbook.md) | EAS builds, release config, the demo, the runbook, and what "done" means |
| [exercises/README.md](./exercises/README.md) | Index of the three exercises |
| [exercises/exercise-01-multi-stage-dockerfile.md](./exercises/exercise-01-multi-stage-dockerfile.md) | Containerize the API with a multi-stage Dockerfile and run it against managed Postgres |
| [exercises/exercise-02-deploy-workflow.yml](./exercises/exercise-02-deploy-workflow.yml) | A complete, runnable GitHub Actions build-test-scan-push-deploy pipeline |
| [exercises/exercise-03-app-config.ts](./exercises/exercise-03-app-config.ts) | The typed Expo config layer that points the EAS build at the live API |
| [challenges/README.md](./challenges/README.md) | Index of the weekly challenge |
| [challenges/challenge-01-clone-to-live-demo.md](./challenges/challenge-01-clone-to-live-demo.md) | Take the full stack from a clean clone to a live sign-in-and-use demo with one pipeline run |
| [quiz.md](./quiz.md) | 12 questions, answer key at the bottom |
| [homework.md](./homework.md) | The week's deliverables with a rubric |
| [mini-project/README.md](./mini-project/README.md) | The full capstone delivery brief |

## The "it runs from a clean clone" promise

C3 has one recurring marker, and Week 10 is where it cashes out:

```
build pushed · deploy succeeded · health 200 · sign-in OK · 1m48s
```

The grader will clone your repo, set the documented secrets, push to `main` (or re-run your pipeline), watch the API come up healthy against managed Postgres, install your EAS build, sign in, create a habit, check in, and confirm the row landed in the database. Then they will read your `RUNBOOK.md` and confirm they could have done the deploy from cold without asking you a single question. If the clone-to-live path is not documented and reproducible, the capstone does not pass — regardless of how clean the code looks. A system only you can deploy, from your laptop, with steps that live in your head, is not a shipped product; it is a demo that dies when you close the lid.

## Stretch goals

If you finish the regular work early and want to push further:

- Add a **staging** environment: the pipeline deploys to staging on every `main` push and to prod only on a tagged release, so you never ship straight to prod.
- Put a **custom domain + HTTPS** in front of the API and the EAS build's URL, instead of the platform-assigned hostname.
- Add a **smoke test** to the pipeline that hits `/actuator/health` and a real authenticated endpoint after deploy, and rolls back automatically if either fails.
- Wire **structured request logging + a basic dashboard** (the host's built-in metrics, or a free tier of a log aggregator) so you can answer "what's the p95 latency" with a number, not a guess.
- Ship an **over-the-air (OTA) update** with `eas update` and demonstrate pushing a JS-only change to the installed app without a new store build.

## Up next

There is no Week 11. After you ship the capstone and deliver the review, you are done with C3. The intended next track is **C18 Crunch GCP** — take the containerized service you just deployed and learn to run it at scale on a real cloud with infrastructure as code, observability, and a capstone of its own. Read the [Crunch Labs Charter](../../../CRUNCH-LABS-CHARTER.md) for the full pathway.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
