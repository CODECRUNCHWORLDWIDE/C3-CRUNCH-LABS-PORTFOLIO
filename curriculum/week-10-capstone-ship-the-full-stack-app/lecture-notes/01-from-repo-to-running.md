# Lecture 1 — From Repo to Running: Dockerizing Spring Boot, Deploying the API, and CI/CD

> **Reading time:** ~75 minutes. **Hands-on time:** ~50 minutes (you containerize the API, run it against managed Postgres, and sketch your deploy pipeline).

This is the lecture that turns "it runs on my machine" into "it runs on a machine, and I can prove it, and someone else can deploy it without me." You have spent nine weeks building Crunch Tracker on `localhost`: the Spring Boot API on port 8080, Postgres in a Docker Compose file, the Expo app pointed at `http://localhost:8080` or your laptop's LAN IP. That is a development environment. This week you build a *deployment* — a container that carries the app and all its dependencies, a managed database it talks to over the network, and a pipeline that ships a new version when you push to `main`. By the end of this lecture you will understand the multi-stage Dockerfile, how config flows from the environment instead of from a file, how to deploy to a managed host, and how a CI/CD pipeline authenticates and ships without a secret ever touching the repo.

A note on framing before we start: this is also the week of the **architecture review**, and the review and the deploy are the same skill viewed from two angles. The deploy forces you to make every implicit dependency explicit — the database URL, the JWT secret, the CORS origin, the port — because a container has no `localhost` and no ambient state. The review then asks you to *defend* each of those choices. Build the deploy well and the review answers itself. So even where this lecture is about Docker and YAML, keep one eye on the question a senior engineer will ask: *what breaks, and how would you know?*

## 1.1 — What "shipping" actually means

A junior engineer hears "ship it" and thinks "make it work." A senior engineer hears "ship it" and thinks about six things, in this order:

1. **Reproducibility.** Can the exact same artifact run on a clean machine? A `jar` that needs "first install Java 21, then set these five env vars you remember, then…" is not reproducible. A container image is.
2. **Configuration.** Does the artifact read its database URL, its secret, and its allowed origins from the environment, so the *same* image runs in dev, staging, and prod with different config? Or is `localhost:5432` baked into a YAML file?
3. **State.** Where does the data live? Not in the container — containers are cattle, they get killed and replaced. The data lives in a managed database that survives the container.
4. **Observability.** When it breaks at 3am, what do you look at? A health endpoint, structured logs, and a metric. "I'd SSH in and tail a file" does not survive a platform that gives you no SSH.
5. **Rollback.** When the new version is bad, how fast can you get back to the old one? "Redeploy the previous git tag" is acceptable; "I'm not sure" is a failed review.
6. **Reproducibility, again, by someone else.** Can a teammate deploy it from a cold start, reading only the runbook? If the steps live in your head, you have built a pet, not a product.

The whole rest of this lecture is the mechanics that make those six true for Crunch Tracker. Hold them in mind; they are also the question bank for Friday's review.

## 1.2 — Why a container, and what a container is not

You already met Docker in Week 05, where you ran Postgres in a container so your local database matched everyone else's. This week you put *your own app* in a container. The reason is the first item above: reproducibility. An image is a frozen filesystem plus a command — the JRE, your jar, and `java -jar app.jar` — that runs identically on your laptop, in CI, and on the host. There is no "did you install the right Java?" because the right Java is *inside the image*.

A container is **not** a virtual machine. It shares the host kernel; it is a process with an isolated filesystem and network namespace. That is why it starts in seconds, not minutes, and why a slim image matters: every megabyte is pulled over the network on every deploy. A container is also **not** a place to keep data. The filesystem inside a running container is ephemeral — kill the container, lose the writes. This is the single most common beginner mistake: running Postgres *inside* the app container and wondering where the data went after a redeploy. Your data lives in a managed database, outside the container, reachable over the network. The container is stateless. Internalize that: **the API container can be destroyed and recreated at any moment, and the only thing that must survive is the database.**

## 1.3 — The multi-stage Dockerfile for Spring Boot

The naive Dockerfile copies your whole project in, installs Maven, builds, and runs — and ships a 900MB image full of build tools you do not need at runtime. The professional pattern is **multi-stage**: one stage with the full JDK and your source builds the jar; a second stage with only a slim JRE copies the *jar* out of the first stage and runs it. The build tools never make it into the shipped image.

Here is the Dockerfile for the Crunch Tracker API, annotated. This is the file Exercise 1 has you write and verify:

```dockerfile
# syntax=docker/dockerfile:1

# ---- Stage 1: build ----------------------------------------------------------
# Full JDK + Maven to compile and package. Nothing from this stage ships.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only the build descriptors first so Docker caches the dependency
# download layer. If pom.xml hasn't changed, this layer is reused and the
# (slow) dependency resolution is skipped on rebuild.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Now copy the source and build. Skip tests here -- the CI pipeline runs them
# as a separate, visible step (1.9); the image build should be fast and focused.
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests

# Spring Boot's layertools splits the fat jar into layers (deps, app code) so
# rebuilds of just-your-code don't re-ship the unchanged dependency layer.
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ---- Stage 2: runtime --------------------------------------------------------
# Slim JRE only -- no compiler, no Maven, much smaller attack surface.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user. If the process is compromised, it is not root inside
# the container. This is a free, expected hardening step.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

# Copy the extracted layers in cache-friendly order: the slowest-changing
# (dependencies) first, the fastest-changing (your code) last.
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

# The platform sets PORT; Spring reads it (1.4). Document the default.
EXPOSE 8080

# Let the orchestrator probe liveness. Actuator's health endpoint (1.8).
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# JVM flags worth setting in a container: respect the cgroup memory limit and
# fail fast on OOM rather than thrash. MaxRAMPercentage leaves headroom.
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "org.springframework.boot.loader.launch.JarLauncher"]
```

Three things to call out, because reviewers ask about all three:

- **Layer ordering is a performance decision, not a cosmetic one.** Docker caches layers; a change to your application code invalidates only the `application/` layer, so a rebuild re-ships a few kilobytes, not the 60MB of dependencies. Get the order wrong and every deploy re-uploads everything.
- **Non-root is not optional in a review.** "Why does your container run as root?" has no good answer. Two lines of Dockerfile fix it.
- **The healthcheck is how the platform knows you're alive.** Without it, the host cannot tell "starting up" from "wedged," and it will route traffic to a process that isn't ready. We wire the actual endpoint in §1.8.

Build and run it locally to confirm before you deploy anything:

```bash
docker build -t crunch-tracker-api:local .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/crunch" \
  -e DATABASE_USER="crunch" \
  -e DATABASE_PASSWORD="dev-only" \
  -e JWT_SECRET="local-dev-secret-not-for-prod-change-me-please-32b" \
  crunch-tracker-api:local
```

If that comes up healthy against your local Postgres, the image is correct. The only thing that changes for production is *where the env vars point* — which is the entire idea of the next section.

## 1.4 — Config comes from the environment (the twelve-factor turn)

On `localhost`, your `application.yml` probably has this:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/crunch
    username: crunch
    password: crunch
jwt:
  secret: my-dev-secret
```

That is fine for development and *catastrophic* for production, for two reasons. First, the password and the JWT secret are now in your git history forever. Second, the URL points at `localhost`, which inside a container means the container itself — there is no Postgres there. The fix is the twelve-factor **Config** principle: *every value that differs between environments comes from the environment*, and the image carries no environment-specific values at all.

So `application.yml` becomes a set of placeholders that read env vars, with safe-to-commit defaults only for genuinely non-secret values:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate   # production NEVER creates schema; Flyway owns it (1.6)
  flyway:
    enabled: true
server:
  port: ${PORT:8080}       # the host injects PORT; default for local
jwt:
  secret: ${JWT_SECRET}    # no default -- the app MUST fail to start without it
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:19006}
```

Notice `JWT_SECRET` has *no* default. That is deliberate: if someone deploys without setting it, you want the application to refuse to start loudly, not to silently boot with a guessable secret. A missing secret is a startup failure, not a runtime surprise. The same image — byte-for-byte identical — now runs in dev with your local env vars and in prod with the host's env vars. That is the property that makes the pipeline possible: CI builds *one* image and the environment differentiates it.

This also resolves a question reviewers love: *"show me one credential in the repo."* The answer is "there are none — the database password and the JWT secret are environment variables, set as secrets on the host and in GitHub Actions, never committed." If `git grep -i "password\|secret" -- '*.yml'` turns up a real value, you fail that question on the spot. Make it turn up only `${...}` placeholders.

## 1.5 — Managed Postgres: the database that outlives the container

Your local Postgres came from `docker compose up`. In production you do not run your own database in a container next to your app — you use a **managed** Postgres (Render, Railway, Fly, Neon, Supabase; see `resources.md`). The provider runs it, backs it up, patches it, and gives you a connection string. You provision it once, copy the URL, and set it as the `DATABASE_URL` env var on your API host.

The connection string looks like this (the provider gives you the exact one):

```
postgresql://crunch_user:long-random-password@dpg-xxxx.oregon-postgres.render.com:5432/crunch_db
```

A few production realities that bite people the first time:

- **JDBC URL shape.** Spring's JDBC driver wants `jdbc:postgresql://host:port/db`, not the bare `postgresql://...` the provider shows. You either reshape it or set `DATABASE_USER`/`DATABASE_PASSWORD` separately and keep the URL host-only. Decide which and document it in the runbook.
- **SSL is usually required.** Managed Postgres typically requires TLS. Append `?sslmode=require` to the JDBC URL (or the provider's equivalent). A connection that works locally and hangs in prod is almost always this.
- **Connection limits are small on free tiers.** A free managed Postgres might cap you at ~20 connections. HikariCP (Spring's default pool) defaults to 10, which is fine for one instance — but if you run two API instances pointed at the same small database, set `spring.datasource.hikari.maximum-pool-size` so two pools don't exhaust the cap.
- **The database is regional and singular.** One instance, in one region. That is your single point of failure, and naming it in the review is the senior move (more in Lecture 2's review prep). For the capstone's scale it is the correct, honest choice — you are not building multi-region failover for a portfolio habit tracker, but you should *say* that you know it's the SPOF.

## 1.6 — Flyway against the real database: the schema is code

This is where Week 05 pays off, and where people who skipped Flyway and let Hibernate `ddl-auto: update` build their schema get a nasty surprise. In production you set `ddl-auto: validate`, which tells Hibernate to *check* that the schema matches the entities and to **never modify it**. The schema is built and evolved by **Flyway migrations** — versioned SQL files in `src/main/resources/db/migration/` — that run on startup, in order, exactly once each, recorded in a `flyway_schema_history` table.

Why `validate` and not `update`? Because `update` is a guessing algorithm. It will add a column it thinks is missing but it will never drop one, never rename one safely, never add an index you wanted, and it will silently disagree with your intent in ways you discover in production. Flyway makes the schema an auditable, reviewable, repeatable artifact: the same migrations that built your local database build the managed one, in the same order, and the history table proves which ran. When the container starts against the fresh managed Postgres, Flyway sees an empty `flyway_schema_history`, runs `V1__init.sql` through `V7__add_checkin_index.sql`, and the schema exists. On the next deploy it sees they already ran and does nothing. That idempotency is the whole point.

A migration the capstone almost certainly has (the check-ins table that every later week extends):

```sql
-- V3__create_checkins.sql
CREATE TABLE check_ins (
    id          BIGSERIAL PRIMARY KEY,
    habit_id    BIGINT NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    checked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    note        TEXT,
    UNIQUE (habit_id, user_id, checked_at)
);
CREATE INDEX idx_checkins_user_habit ON check_ins (user_id, habit_id);
```

The thing to verify before you deploy: run the container against a *fresh, empty* managed database and confirm Flyway builds the whole schema from zero. If it only works against a database you already hand-shaped, you do not have a reproducible deploy — you have a database you'll be unable to recreate.

## 1.7 — CORS, for the origin that isn't localhost

On `localhost` your Expo dev server and your API are both on your machine, and CORS rarely bites. In production the mobile app makes requests from a real device to a real hostname, and any web origin you expose must be allowed explicitly. Spring Security 6 owns CORS; you configure the allowed origins from — you guessed it — an environment variable, so the same image allows different origins in different environments:

```java
@Bean
CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

A native mobile app (a real EAS build, not the web preview) is not subject to browser CORS at all — it is not a browser. But your demo and your stretch web build *are*, and a reviewer will ask "what's your CORS posture?" The answer "allowed origins come from `CORS_ALLOWED_ORIGINS`; in prod it's the deployed web origin only, not `*`" is the one you want to be able to give. Never ship `setAllowedOrigins(List.of("*"))` with `allowCredentials(true)` — the spec forbids it and the browser will reject it anyway.

## 1.8 — The health endpoint and structured logs

Two observability primitives the platform needs and the review checks.

**The health endpoint.** Spring Boot Actuator exposes `/actuator/health`. The container's `HEALTHCHECK` (§1.3) and the host both probe it; a `200` means "ready for traffic," anything else means "don't route to me." Wire it so it actually checks the database, not just that the process is alive:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health   # expose ONLY health publicly; not env, not beans
  endpoint:
    health:
      probes:
        enabled: true     # gives you /actuator/health/liveness and /readiness
      show-details: never # don't leak internals to anonymous callers
```

With this, `/actuator/health/readiness` returns `503` while Flyway is still migrating on a cold start and `200` once the app is ready — which is exactly when the host should start sending it traffic. That `start-period=40s` in the Dockerfile healthcheck exists to cover that migration window.

**Structured logs.** On `localhost` you read the console. In production the platform captures stdout, and you want to *search* it after an incident — by user, by request, by error. Spring Boot 3.4+ can emit JSON logs:

```yaml
logging:
  structured:
    format:
      console: ecs   # Elastic Common Schema JSON to stdout; the host aggregates it
```

Now a log line is `{"@timestamp":"...","log.level":"ERROR","message":"...","trace.id":"..."}` instead of free text, and "find every error for user 42 in the last hour" is a query, not a grep through scrollback. When a reviewer asks "it's broken, what do you look at first," the answer is "the structured logs, filtered to the failing endpoint" — concrete, not "I'd look at the logs."

## 1.9 — The CI/CD pipeline: build, test, scan, push, deploy

Now the pipeline that ties it together. The discipline: **on every pull request, build and test; on every push to `main`, also build the image, scan it, push it to a registry, and deploy.** Nothing is manual, and no secret lives in the repo — the pipeline authenticates to the registry with the built-in `GITHUB_TOKEN` and to the host with a deploy hook or OIDC.

The stages, and why each exists:

1. **Checkout + set up Java 21.** `actions/checkout` and `actions/setup-java` with caching, so Maven doesn't re-download the world every run.
2. **Build and test.** `./mvnw verify`. This is where your Week 03 JUnit suite and your Week 05 Testcontainers integration tests run. A red test stops the pipeline — you do not ship a failing build. This is *why* you wrote tests: so the machine can refuse a broken deploy.
3. **Build the image.** `docker/build-push-action` with Buildx. Tag it with the git SHA *and* `latest`, so every deploy is traceable to a commit and rollback is "deploy the previous SHA."
4. **Scan the image.** Trivy checks the image for known CVEs. A critical vulnerability in a base image is something you want to know *before* you ship, not after.
5. **Push to the registry.** GHCR (`ghcr.io/<owner>/crunch-tracker-api`), authenticated with `GITHUB_TOKEN` — no separate credential to manage.
6. **Deploy.** Trigger the host to pull the new image. On Render/Railway that's a deploy hook URL (a secret); with a cloud you'd use OIDC to assume a role and run the deploy with a short-lived token.

Exercise 2 is the complete, runnable workflow file. The shape, in brief:

```yaml
on:
  push:
    branches: [main]
  pull_request:

jobs:
  build-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: ./mvnw -B verify          # tests gate everything downstream

  ship:
    needs: build-test
    if: github.ref == 'refs/heads/main'   # PRs test; only main ships
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write                  # to push to GHCR
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}   # built-in, no stored key
      - uses: docker/build-push-action@v6
        with:
          push: true
          tags: |
            ghcr.io/${{ github.repository }}:${{ github.sha }}
            ghcr.io/${{ github.repository }}:latest
      - name: Deploy
        run: curl -fsS -X POST "${{ secrets.RENDER_DEPLOY_HOOK }}"
```

The `if: github.ref == 'refs/heads/main'` on the `ship` job is the rule that makes this safe: a pull request runs the tests (so review sees green or red) but does **not** deploy. Only a merge to `main` ships. That separation is the difference between "CI" (tests on every change) and "CD" (deploys on the trunk).

## 1.10 — Secrets in the pipeline, the right way

The pipeline needs three secrets: the registry credential, the deploy hook, and — if your deploy step talks to the database (it usually doesn't, the container does) — nothing else. These live in **GitHub Actions encrypted secrets**, set once with `gh secret set RENDER_DEPLOY_HOOK` (or in the repo settings UI), and referenced as `${{ secrets.NAME }}`. They are encrypted at rest, masked in logs, and never visible in the YAML.

The registry credential is the cleanest case: GitHub gives every workflow a built-in `GITHUB_TOKEN` scoped to the repo, so pushing to *that repo's* GHCR needs no stored credential at all. For a cloud deploy, the modern pattern is **OIDC**: the workflow requests a short-lived token from GitHub's identity provider, the cloud trusts that provider for your specific repo, and the deploy runs with a token that expires in minutes — so there is no long-lived cloud key to leak. The whole point: *the pipeline can deploy, but if the repo were compromised there is no permanent credential to steal.* When a reviewer asks "how does your pipeline authenticate, and what happens if your repo leaks," "OIDC, short-lived tokens, no stored cloud key" is the answer that reads as senior. "I put my AWS access key in a secret" is the answer that gets a follow-up about key rotation you don't want.

## 1.11 — Rollback: the deploy you'll actually need at 3am

Every deploy strategy is incomplete without a rollback, and rollback is the question reviewers ask when they want to know if you've operated anything. Because you tag every image with the git SHA, rollback is mechanical: redeploy the previous SHA's image. On Render/Railway that's selecting the previous deploy in the UI or re-running the pipeline at the prior commit; with a cloud it's `deploy --image ...:<prev-sha>`. Time it once so you can answer "how long does rollback take" with a number, not a shrug.

The subtle part is the **database**. Rolling back the *code* is easy; rolling back a *migration* is not. If `V8__drop_legacy_column.sql` ran and you roll the code back to a version that expected that column, you have a problem the image rollback cannot fix. The discipline: **migrations are forward-only and backward-compatible within a release window.** You do not drop a column in the same deploy that stops writing to it; you stop writing (deploy 1), confirm it's safe, then drop (deploy 2). This is the "expand/contract" pattern, and it is why your Flyway files are append-only and never edited after they've run. A reviewer who hears "code rollback is one command; schema changes are expand/contract so a code rollback never strands the database" knows you've thought past the happy path.

## 1.12 — Putting it together: the deploy you can defend

Step back and look at the whole path, because this is the trace you'll walk in Friday's review:

- A commit lands on `main`. The pipeline runs the test suite — your Week 03 unit tests and your Week 05 Testcontainers integration tests — and only proceeds if they're green.
- It builds the multi-stage image, scans it, tags it with the SHA, and pushes it to GHCR.
- It triggers the host, which pulls the new image and starts a container. The container reads `DATABASE_URL`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` from the environment. Flyway runs the migrations against managed Postgres. Actuator reports `200` on readiness once migrations finish.
- The host routes traffic to the healthy container. The old container is drained and killed. The mobile app — already pointed at this hostname (Lecture 2) — keeps working, now against the new version.
- If it's bad, you redeploy the previous SHA in under a minute, and because your schema changes are expand/contract, the database is fine.

Every arrow in that path is something you chose and can defend: why a container (reproducibility), why managed Postgres (state outlives the container, the provider operates it), why config from the environment (one image, many environments, no secrets in git), why Flyway `validate` (the schema is auditable code), why the test gate (the machine refuses a broken deploy), why OIDC (no permanent credential to steal), why expand/contract (rollback never strands the data). That list *is* your architecture review. Lecture 2 takes the other half — the mobile build, the runbook, and what "done" really means — and then you ship.

## Summary

Shipping is six properties: reproducibility, config-from-environment, externalized state, observability, rollback, and reproducibility-by-someone-else. A container delivers the first and is stateless by design; the data lives in a managed Postgres that outlives it. The multi-stage Dockerfile builds with the JDK and ships only the JRE plus a layered jar, as a non-root user, with a healthcheck. Config — the database URL, the JWT secret, the CORS origins — comes from environment variables, so one image runs everywhere and no secret touches the repo. Flyway owns the schema with `ddl-auto: validate`, building it from empty and proving idempotency through its history table. The CI/CD pipeline tests on every PR and ships only from `main`, building, scanning, pushing, and deploying with no long-lived credential — `GITHUB_TOKEN` for the registry, OIDC or a deploy hook for the host. Rollback is "redeploy the previous SHA," made safe by expand/contract migrations. Every one of those choices is something a reviewer will ask you to defend, and building the deploy well is how you prepare the defense.

Lecture 2 ships the other half of the product — the installable mobile build pointed at this live API — and writes the runbook that lets someone else deploy all of it from cold.
