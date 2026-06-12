# Week 10 — Quiz

Twelve questions. This is the last quiz of the course; it mixes capstone-week material (containers, deploy, CI/CD, EAS, runbooks) with the synthesis questions a reviewer or interviewer actually asks. Take it with your notes closed. Aim for 10/12. Answer key at the bottom — don't peek.

---

**Q1.** In a multi-stage Dockerfile for the Spring Boot API, why is the runtime stage `FROM eclipse-temurin:21-jre` instead of `...-jdk`?

- A) The JRE builds faster than the JDK.
- B) The runtime only needs to *run* the jar, not compile it; shipping the JRE (not the full JDK + Maven) yields a much smaller image with a smaller attack surface.
- C) The JDK can't run Spring Boot.
- D) It's a style preference with no real effect.

---

**Q2.** Your container starts, but the API can't reach the database, even though it works locally. The connection string is a managed Postgres URL. What is the most likely cause?

- A) The container is too small.
- B) The JDBC URL is missing `?sslmode=require` (or the JDBC `jdbc:` prefix), so the TLS-required managed database refuses the connection.
- C) Docker doesn't support Postgres.
- D) The JRE can't open sockets.

---

**Q3.** Why does production set `spring.jpa.hibernate.ddl-auto: validate` rather than `update`?

- A) `validate` is faster at runtime.
- B) `update` guesses schema changes (adds columns, never drops/renames safely) and silently diverges from intent; `validate` makes Flyway the single source of truth for the schema and refuses to let Hibernate modify it.
- C) `update` doesn't work with Postgres.
- D) They're identical in production.

---

**Q4.** A teammate puts the JWT signing secret and the database password directly in `application.yml`. What's the problem, and the fix?

- A) No problem if the repo is private.
- B) The secrets are now in git history forever and ship in the image; the fix is `${JWT_SECRET}` / `${DATABASE_PASSWORD}` placeholders set as environment variables, never committed.
- C) YAML can't hold secrets; use JSON.
- D) Encrypt the YAML file and commit it.

---

**Q5.** In the CI/CD pipeline, why does the `ship` job carry `if: github.ref == 'refs/heads/main'`?

- A) To make the pipeline run faster.
- B) So pull requests run the tests (showing green/red in review) but only a merge to `main` actually builds the image and deploys — the CI/CD separation.
- C) Because `main` is the only branch GitHub Actions supports.
- D) To skip the tests on `main`.

---

**Q6.** The pipeline pushes the image to GHCR using `${{ secrets.GITHUB_TOKEN }}`. Why is that better than storing a registry username and password as a secret?

- A) It isn't; a stored password is simpler.
- B) `GITHUB_TOKEN` is a built-in, automatically-scoped, short-lived token for *this* repo — there is no long-lived registry credential to store, rotate, or leak.
- C) `GITHUB_TOKEN` is free and passwords cost money.
- D) It pushes faster.

---

**Q7.** You ship an EAS build and the installed app shows "network request failed" on every screen, even though the API is healthy. The phone is not on your Wi-Fi. What did you most likely get wrong?

- A) The phone's battery is low.
- B) `EXPO_PUBLIC_API_URL` in the build profile still points at `localhost` (or your LAN IP), which on a real device the app cannot reach — the API URL must be the live host, baked in at build time.
- C) EAS builds can't make network requests.
- D) The JWT expired.

---

**Q8.** Why must the mobile app's API URL be decided at *build time* rather than read from a server at runtime?

- A) Runtime config is illegal in React Native.
- B) A standalone EAS build has no dev server to ask; the URL is frozen into the JS bundle at build time (via the `EXPO_PUBLIC_` env var), so it must be correct when you build.
- C) Build-time config is faster to type.
- D) It doesn't matter; both work identically.

---

**Q9.** A reviewer asks "where does the auth token live on the device, and what happens on logout?" For the capstone, what's the correct answer?

- A) In a global variable; logout reloads the app.
- B) In `expo-secure-store` (the platform Keychain/Keystore, encrypted at rest), loaded on launch and deleted on logout.
- C) In plain `AsyncStorage`; it's fine because the repo is private.
- D) In the JWT itself; tokens store themselves.

---

**Q10.** What does "clean rollback" require for the capstone, and why are migrations relevant?

- A) Delete the host and start over.
- B) Redeploy the previous image SHA (one action, ~1–2 min); migrations must be expand/contract (forward-only, backward-compatible) so a code rollback never strands the schema.
- C) Re-run all migrations in reverse.
- D) Rollback isn't needed if tests pass.

---

**Q11.** A reviewer asks "it's 3am, the app won't load data — what's the first thing you look at?" Which answer reads as senior?

- A) "I'd look at the logs."
- B) "Check `/actuator/health`; if it's not UP the container is down or unhealthy (usually a missing env var or a failed migration); if it is UP, reproduce with curl and read the structured logs filtered to the failing endpoint."
- C) "I'd redeploy and hope."
- D) "I'd ask a teammate."

---

**Q12.** What does "done" mean for the capstone, stated the way the course defines it?

- A) Every planned feature is implemented and the test coverage is 100%.
- B) A working end-to-end slice someone *else* can stand up and use from a cold start (clone → deploy → install → sign in → check in → row in DB), plus an honest "known limitations" section — depth of *it actually ships* over breadth of *it almost does five things*.
- C) It compiles and runs on your laptop.
- D) It has a polished UI and an architecture diagram.

---
---

## Answer key

**Q1 — B.** The runtime stage only runs the jar, so it needs the JRE, not the JDK + build tools. Smaller image, smaller attack surface, faster pulls on every deploy. (Lecture 1, §1.3; Exercise 1.)

**Q2 — B.** Managed Postgres almost always requires TLS; a connection that works locally and fails in prod is usually a missing `?sslmode=require` (or a missing `jdbc:` prefix / wrong URL shape). (Lecture 1, §1.5.)

**Q3 — B.** `validate` makes Flyway the schema's single source of truth and forbids Hibernate from modifying it; `update` is a guessing algorithm that silently diverges from intent. (Lecture 1, §1.6.)

**Q4 — B.** Secrets in `application.yml` enter git history and ship in the image. The fix is environment-variable placeholders (`${JWT_SECRET}`), set as secrets, never committed. A private repo is not a secrets store. (Lecture 1, §1.4, §1.10.)

**Q5 — B.** The `if` on the `ship` job is the CI/CD separation: PRs test, only `main` deploys. That's what keeps a pull request from shipping straight to prod. (Lecture 1, §1.9.)

**Q6 — B.** `GITHUB_TOKEN` is built-in, scoped to the repo, and short-lived — no long-lived registry credential to store, rotate, or leak. (Lecture 1, §1.9, §1.10.)

**Q7 — B.** The classic capstone bug: the shipped build's `EXPO_PUBLIC_API_URL` points at `localhost`/LAN, which a real device can't reach. The URL must be the live host, baked in at build time. (Lecture 2, §2.1, §2.3; Exercise 3.)

**Q8 — B.** A standalone build has no dev server to query; the URL is inlined into the bundle at build time, so it must be set correctly when you build. (Lecture 2, §2.2, §2.3.)

**Q9 — B.** The token lives in `expo-secure-store` (Keychain/Keystore, encrypted), loaded on launch and deleted on logout. Plain `AsyncStorage` and "the repo is private" are wrong answers. (Lecture 2, §2.4.)

**Q10 — B.** Rollback is "redeploy the previous SHA," made safe by expand/contract migrations so a code rollback never strands the database. (Lecture 1, §1.11.)

**Q11 — B.** The senior answer is concrete and starts at the health endpoint, then reproduces and reads the *structured* logs filtered to the endpoint. "I'd look at the logs" is too vague. (Lecture 2, §2.7.)

**Q12 — B.** Done = a working end-to-end slice someone else can stand up and use, plus an honest limitations section; depth of "it ships" beats breadth of "it almost does five things." (Lecture 2, §2.8.)

---

*Score 10+/12 and you're ready for the live review. Below that, re-read the lecture section cited next to each one you missed before Friday.*
