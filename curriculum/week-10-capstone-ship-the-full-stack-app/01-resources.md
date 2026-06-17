# Week 10 — Resources

Capstone week. The reading here is split into six buckets: containerizing the Spring Boot API, deploying it against managed Postgres, wiring the CI/CD pipeline, releasing the mobile app with EAS, the production-hardening concerns that were toy-grade until now, and the runbook / portfolio references. Everything is free unless explicitly noted; nothing in this week requires a paid plan, though a small managed-Postgres and container-host bill (often inside a free tier or a few dollars) is expected.

## Containerizing the Spring Boot API

- **Docker — multi-stage builds** — the official guide to the two-stage pattern this week's exercise uses (build with the JDK, run with a slim JRE): <https://docs.docker.com/build/building/multi-stage/>
- **Spring Boot — container images & layered jars** — the reference section on `layertools`, the recommended Dockerfile, and Buildpacks: <https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html>
- **Eclipse Temurin (Adoptium) images** — the JDK/JRE base images we build on; `temurin:21-jdk` to build, `temurin:21-jre` to run: <https://hub.docker.com/_/eclipse-temurin>
- **Google `distroless` Java** — the smaller, no-shell runtime base for the stretch image; nothing to attack, nothing to `exec` into: <https://github.com/GoogleContainerTools/distroless>
- **Dive** — inspect your image layer by layer to see what you accidentally shipped: <https://github.com/wagoodman/dive>
- **The twelve-factor app — Config** — why every secret and connection string is an env var, not a file in the repo: <https://12factor.net/config>

## Deploying: managed Postgres + a container host

- **Render — deploy a Docker image / web service** — the default host for this week; free-tier web service + managed Postgres, deploy from a Dockerfile or an image: <https://render.com/docs/docker> · <https://render.com/docs/databases>
- **Railway — deploy from a Dockerfile** — an equally good alternative; provision Postgres and a service in the same project: <https://docs.railway.com/guides/dockerfiles>
- **Fly.io — `fly launch` / `fly deploy`** — run the container close to users, with a managed Postgres add-on: <https://fly.io/docs/launch/> · <https://fly.io/docs/postgres/>
- **Supabase / Neon — serverless Postgres** — managed Postgres on its own if your host doesn't bundle one; Neon scales to zero, Supabase gives you a dashboard: <https://neon.tech/docs> · <https://supabase.com/docs/guides/database>
- **Flyway — documentation** — your migrations are the schema; `flyway:validate` semantics and how Spring Boot runs them on startup: <https://documentation.red-gate.com/fd> · <https://docs.spring.io/spring-boot/how-to/data-initialization.html>
- **Testcontainers** — spin up a real Postgres in a container for your integration tests in CI, instead of an in-memory fake: <https://java.testcontainers.org/>

## CI/CD with GitHub Actions

- **GitHub Actions — quickstart & workflow syntax** — the reference for `on:`, `jobs:`, `steps:`, and the expression language: <https://docs.github.com/en/actions/writing-workflows/quickstart> · <https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions>
- **`actions/setup-java`** — install Temurin 21 and cache Maven/Gradle in CI: <https://github.com/actions/setup-java>
- **`docker/build-push-action` + `docker/login-action`** — build the image with Buildx and push it to a registry in the pipeline: <https://github.com/docker/build-push-action>
- **GitHub Container Registry (GHCR)** — the free registry that lives next to your repo; `ghcr.io/<owner>/<image>`: <https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry>
- **OIDC in GitHub Actions** — how the pipeline authenticates to your cloud with a short-lived token instead of a long-lived key you'd have to store: <https://docs.github.com/en/actions/security-for-github-actions/security-hardening-your-deployments/about-security-hardening-with-openid-connect>
- **Encrypted secrets & environments** — where `DATABASE_URL`, `JWT_SECRET`, and deploy tokens actually live; environments add required reviewers: <https://docs.github.com/en/actions/security-for-github-actions/security-guides/using-secrets-in-github-actions>
- **Trivy** — scan the image for known CVEs as a pipeline step before you ship it: <https://aquasecurity.github.io/trivy/>

## Releasing the mobile app with Expo EAS

- **EAS Build — introduction** — build a real `.apk`/`.aab`/`.ipa` in the cloud without a local Android/Xcode toolchain: <https://docs.expo.dev/build/introduction/>
- **`eas.json` build profiles** — `development`, `preview`, and `production` profiles, and how they differ: <https://docs.expo.dev/build-reference/eas-json/>
- **Internal distribution** — the install link you hand a grader or a phone for the demo, no app store required: <https://docs.expo.dev/build/internal-distribution/>
- **App config & `extra` / environment variables** — bake the live API URL into the binary at build time so the shipped app doesn't point at `localhost`: <https://docs.expo.dev/workflow/configuration/> · <https://docs.expo.dev/eas/environment-variables/>
- **`expo-constants`** — read the `extra` config back out at runtime, typed: <https://docs.expo.dev/versions/latest/sdk/constants/>
- **EAS Update (OTA)** — the stretch goal: push a JS-only change to installed apps without a new build: <https://docs.expo.dev/eas-update/introduction/>

## Production hardening (it was fine on localhost)

- **Spring Security 6 — CORS** — configure the deployed origin properly; the mobile build and any web origin must be allowed explicitly: <https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html>
- **Spring Boot Actuator** — the `/actuator/health` endpoint the platform probes, and how to expose only what you mean to: <https://docs.spring.io/spring-boot/reference/actuator/index.html>
- **Externalized configuration & `application.yml` profiles** — `application-prod.yml`, env-var overrides, and `${JWT_SECRET}` placeholders: <https://docs.spring.io/spring-boot/reference/features/external-config.html>
- **OWASP — Secrets Management Cheat Sheet** — why the JWT signing key and DB password never get committed, and what to do instead: <https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html>
- **Logback / Spring Boot structured logging** — emit JSON logs you can actually search after an incident (Spring Boot 3.4+ has first-class structured logging): <https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured>

## Runbook, demo, and portfolio

- **Google SRE Book — "Being On-Call" & the runbook discipline** — the model for the "first five minutes" section of your `RUNBOOK.md`: <https://sre.google/sre-book/being-on-call/>
- **Atlassian — incident runbook & playbook templates** — a clean structure to start from: <https://www.atlassian.com/incident-management/incident-response/playbooks>
- **The README as a product** — Make a README's checklist; your repo's README is the first thing a hiring manager reads: <https://www.makeareadme.com/>
- **A screen recorder for the 5-minute video** — QuickTime (macOS), OBS Studio (cross-platform, free), or Loom: <https://obsproject.com/>
- **Mermaid live editor** — draw the one-page architecture diagram you'll defend; it renders in GitHub: <https://mermaid.live/>

## Tools you'll use this week

- **`docker` (Engine + Buildx)** — `docker build`, `docker run`, `docker push`. Docker Desktop on macOS/Windows, or the Engine on Linux.
- **`gh`** — the GitHub CLI, for `gh secret set`, `gh run watch`, and triggering the pipeline.
- **`eas-cli`** — `npm install -g eas-cli`, then `eas login` and `eas build`.
- **`psql`** — connect to the managed database to verify a row landed: `psql "$DATABASE_URL"`.
- **`curl` / HTTPie** — hit the deployed API directly to test sign-in and health before the phone ever does.
- **A free Expo account and a free Render/Railway/Fly account** — the two sign-ups this week needs; both have no-cost tiers sufficient for the capstone.

## Career pack (cross-referenced)

The capstone is the headline portfolio artifact. The supporting docs live one level up in the track root:

- `interview-prep/` — the full-stack system-design round and the Java/Spring + React Native deep-dive drills used for the mock interview.
- `portfolio.md` — the template for the one-paragraph capstone writeup and the live-demo link.

---

*If a link 404s, please open an issue so we can replace it.*
