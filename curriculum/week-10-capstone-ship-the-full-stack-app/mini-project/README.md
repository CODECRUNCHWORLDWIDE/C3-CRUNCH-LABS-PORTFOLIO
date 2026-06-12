# Mini-Project — Ship Crunch Tracker: the deployed, installable, end-to-end product

> Assemble every prior week's compounding artifact — the JDK 21 toolchain and team repo, the records-and-sealed-types domain, the JUnit-tested services, the Spring Boot REST API, the JPA/Postgres persistence with Flyway, the JWT auth with per-user ownership, the Expo + TypeScript client, the navigation and Zustand state, and the TanStack Query integration — into one deployed product: the Spring Boot API reachable on the internet against managed Postgres, the React Native app built and installable against the live backend, a `RUNBOOK.md` another engineer could deploy from cold, and a one-paragraph portfolio writeup linking the live demo.

This is the capstone. It is not a new build; it is the **shipping** of ten weeks of compounding work into one system you can deploy, demo, defend, and redeploy from a clean clone. By the SYLLABUS note, the mini-projects compound — by Week 09 the mobile app was already talking to the live API, not mock data — and Week 10 is where the compounding pays off. If you kept your config externalized and your Flyway migrations honest every week, this week is packaging and proof. If you took shortcuts — a hard-coded `localhost`, a schema only Hibernate's `ddl-auto` ever built, a secret in `application.yml` — this is where you pay for them.

**Estimated time:** ~12 hours of the week's schedule (Monday through Saturday mini-project blocks), on top of the exercises and the live review.

---

## What you assemble

You already have, from the prior weeks, a working API and a working mobile client. The mini-project containerizes the API, deploys it against managed Postgres, wires the CI/CD pipeline, builds the installable mobile artifact, and proves the whole thing works end to end from a cold start.

### The pieces you compose (from prior weeks)

- **The domain + services** — Weeks 02–03. Records, sealed types, services behind interfaces, JUnit + AssertJ tests.
- **The REST API** — Week 04. CRUD, DTOs, validation, `ProblemDetail`, OpenAPI docs.
- **Persistence** — Week 05. Spring Data JPA, PostgreSQL, Flyway migrations, Testcontainers integration tests.
- **Auth** — Week 06. JWT login, BCrypt, per-user ownership, CORS.
- **The mobile client** — Weeks 07–08. Expo + TypeScript, navigation, Zustand, `expo-secure-store`.
- **The integration** — Week 09. The typed `apiClient`, TanStack Query, auth headers, error/loading UX.

### The repository shape this week adds

```
crunch-tracker/
├── api/
│   ├── src/main/resources/
│   │   ├── application.yml            # all ${ENV} placeholders, no secrets
│   │   └── db/migration/V1..Vn.sql    # Flyway: the schema is code (Week 05)
│   ├── Dockerfile                     # multi-stage, non-root (Exercise 1)
│   ├── .dockerignore
│   └── pom.xml
├── mobile/
│   ├── config/env.ts                  # the API-URL config seam (Exercise 3)
│   ├── api/client.ts                  # the single apiClient (Week 09)
│   ├── app.config.ts                  # forwards EXPO_PUBLIC_API_URL into extra
│   └── eas.json                       # preview profile -> live API (Lecture 2)
├── .github/workflows/deploy.yml       # CI/CD pipeline (Exercise 2)
├── diagram.md                         # one-page Mermaid architecture diagram
├── RUNBOOK.md                         # deploy-from-cold runbook (Lecture 2)
├── portfolio.md                       # the one-paragraph writeup + live link
└── README.md                          # how to deploy, demo, and roll back
```

The key wiring is that **one config value flows all the way through**: the deployed API's hostname is `EXPO_PUBLIC_API_URL` in `eas.json`, read by `config/env.ts`, consumed by the single `apiClient`. Change the host in one place, rebuild, and the whole mobile app points at the new backend. That is the seam that makes the shipped app independent of your laptop.

---

## The end-to-end data flow you must demonstrate

One check-in, traced through every hop. This is the trace-an-event walk from Lecture 2 §2.6, and it is what your 5-minute video shows:

1. **Phone.** The user taps "Check in" on a habit in the installed EAS build. The UI updates optimistically (Week 09). The `apiClient` fires `POST /api/habits/{id}/check-ins` with the JWT from `expo-secure-store` in the `Authorization` header, to the live API host.
2. **Edge / API.** The request reaches the deployed Spring Boot container over HTTPS. Spring Security validates the JWT (Week 06), the controller validates the body (Week 04), and the service enforces that the habit belongs to the signed-in user (ownership scoping).
3. **Persistence.** Spring Data JPA inserts a `check_ins` row in managed Postgres. The schema is exactly what Flyway built (Week 05).
4. **Response.** The API returns the created check-in; TanStack Query reconciles the optimistic update with the authoritative response.
5. **Proof.** You connect to managed Postgres (`psql "$DATABASE_URL"`) and show the new row, with `user_id` matching the signed-in user — closing the loop from tap to durable, user-scoped data.
6. **Observe.** The deployed API's structured logs show the request; `/actuator/health` confirms it's the live, deployed instance.

---

## Rules

- **You may** reuse every line of the API and the mobile client you wrote in Weeks 01–09. That is the point — this is shipping, not a rewrite.
- **You may NOT** depend on your laptop for the demo. The grader's phone is not on your network; the EAS build must reach the live API on its own.
- **Config from the environment only.** `git grep -iE "password|secret|jwt" -- '*.yml' '*.properties'` must return only `${...}` placeholders. No real secret in the repo, ever.
- **The schema is Flyway's.** `ddl-auto: validate` in production. The deploy must build the schema from an *empty* managed database. If it only works against a database you hand-shaped, fix it.
- **No long-lived credential in the repo.** `GITHUB_TOKEN` for the registry; secrets in GitHub Actions secrets and on the host. `git grep` for `BEGIN PRIVATE KEY`, access keys, and passwords must come up empty.
- **Cold-start discipline:** every manual host setting goes in `RUNBOOK.md` the moment you set it. The grader follows only what's written.

---

## Acceptance criteria

The rubric maps each box to a deliverable. This is the same bar as `challenges/challenge-01`, restated for the build.

### Containerize & deploy (25%)

- [ ] A multi-stage Dockerfile builds a non-root image under ~250MB (Exercise 1).
- [ ] The API is deployed to a managed host, reachable on a public URL, healthy at `/actuator/health`.
- [ ] Managed Postgres is provisioned; Flyway builds the schema from empty; `ddl-auto: validate`.
- [ ] All config comes from environment variables; no secret in the repo.

### CI/CD (20%)

- [ ] A GitHub Actions pipeline tests on every PR and ships only from `main` (Exercise 2).
- [ ] On `main`: build → scan → push to GHCR → deploy, with no long-lived credential in the repo.
- [ ] The post-deploy smoke test confirms the new version is healthy before calling the deploy a success.

### Mobile build (20%)

- [ ] An installable EAS `preview` build (`.apk` or internal-distribution link) in the README.
- [ ] The build points at the live API (one config seam, frozen at build time); it works on a phone not on your network.
- [ ] The token lives in `expo-secure-store`; logout clears it.

### The working slice (20%)

- [ ] Install → sign in → create a habit → check in → the row lands in managed Postgres, scoped to the user.
- [ ] One cross-stack trace of a single check-in, on demand (the video).

### Delivery (15%)

- [ ] `diagram.md` — one-page Mermaid architecture diagram, every arrow labeled.
- [ ] `RUNBOOK.md` — a stranger can deploy from cold: env vars, deploy, rollback, "first five minutes".
- [ ] A 5-minute video tracing one check-in end to end.
- [ ] `portfolio.md` — the one-paragraph writeup with the live-demo link.
- [ ] The live architecture review delivered (Friday slot).

---

## Suggested order of work

- **Monday.** Containerize the API (Exercise 1). Get it running locally against a database with config entirely from env vars, non-root, schema built by Flyway from empty. Do not move on until `docker run` comes up healthy and a register/login round-trips.
- **Tuesday.** Provision managed Postgres and a container host. Deploy the image by hand once (set the env vars, push the image, watch it come up). Confirm `/actuator/health` is `UP` on the public URL and a row lands in the managed DB. Start `RUNBOOK.md` as you go.
- **Wednesday.** Wire the CI/CD pipeline (Exercise 2). Open a PR and watch the tests run; merge to `main` and watch it build, scan, push, deploy, and smoke-test. Make the deploy fully hands-off.
- **Thursday.** Wire the mobile config seam (Exercise 3), set `EXPO_PUBLIC_API_URL` to the live host in `eas.json`, and run `eas build --profile preview --platform android`. Install the `.apk` on a phone *not* on your network and prove sign-in works. Finish `RUNBOOK.md`.
- **Friday.** Record the 5-minute video. Deliver the live architecture review. Capture the risk list.
- **Saturday.** Write `portfolio.md` and `diagram.md`. Mock interview. Do one final clean cold-start deploy *as the grader will*: fresh clone (or fresh host), set the documented secrets, one pipeline run, install, sign in, check in.

---

## What "done" looks like

A grader clones your repo, reads the README and `RUNBOOK.md`, sets the documented secrets, triggers your pipeline, and watches the API come up healthy against managed Postgres. They install your EAS build on their own phone, sign in, create a habit, check in, and confirm the row landed in your database scoped to their user. They open one trace of that check-in. They read your runbook and confirm they could have done all of it without asking you a single question. Then they read your "known limitations" and find an honest, prioritized list. Every one of those steps passes without you touching the keyboard to fix something. That is the capstone. That is C3.
