# Exercise 1 — Containerize the API with a multi-stage Dockerfile, run it against managed Postgres

> **Estimated time:** ~90 minutes (20 min writing the Dockerfile, 20 min building and shrinking it, 50 min wiring config and running against a database). **Cost:** free locally; a managed Postgres free tier if you point at one.

This is the exercise everyone gets *almost* right the first time, because the naive Dockerfile works — it just ships a 900MB image full of build tools, or it bakes `localhost:5432` into the image, or it runs as root, or Flyway can't build the schema because you were depending on a database you'd hand-shaped. You will do it *correctly*: a small, layered, non-root image whose every config value comes from the environment, that builds the schema from empty.

## Goal

Containerize the Crunch Tracker Spring Boot API into a multi-stage image, build it, and run it against a Postgres database (managed or local) with **all** config — the database URL, credentials, JWT secret, CORS origins, port — supplied through environment variables. Confirm it comes up healthy and that Flyway builds the schema from an empty database.

## What "correct" means here

- **Multi-stage:** the JDK and Maven build the jar in one stage; only a slim JRE and the jar ship in the runtime stage.
- **Small:** under ~250MB. If yours is 900MB, build tools leaked into the runtime image.
- **Non-root:** the process runs as an unprivileged user inside the container.
- **Config from the environment:** `git grep` finds no real database password or JWT secret in `application.yml` — only `${...}` placeholders.
- **Reproducible schema:** Flyway runs the migrations on startup against an *empty* database and builds the whole schema. `ddl-auto: validate`, never `update`.

## Step 1 — Make `application.yml` read the environment

Before the Dockerfile, fix config. Open `src/main/resources/application.yml` and replace any hard-coded values with environment placeholders:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate     # Flyway owns the schema; Hibernate only verifies
  flyway:
    enabled: true
server:
  port: ${PORT:8080}
jwt:
  secret: ${JWT_SECRET}      # no default: app MUST refuse to start without it
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:19006}
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
```

Verify there are no secrets left in the file:

```bash
git grep -nE "password:|secret:" -- '*.yml' | grep -v '\${'
# Expect NO output. Any line printed is a real value you must turn into ${...}.
```

## Step 2 — Write the multi-stage Dockerfile

Create `Dockerfile` at the API project root:

```dockerfile
# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT:-8080}/actuator/health || exit 1
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "org.springframework.boot.loader.launch.JarLauncher"]
```

If you use Gradle, the build stage runs `./gradlew bootJar` and the layertools line is the same (Spring Boot's Gradle plugin produces a layered jar too). The `JarLauncher` main class path is for Spring Boot 3.2+; older versions use `org.springframework.boot.loader.JarLauncher` (no `.launch`).

Add a `.dockerignore` so you don't ship the local build output and git history into the build context:

```
target/
build/
.git/
.gradle/
*.md
```

## Step 3 — Build the image and check its size

```bash
docker build -t crunch-tracker-api:local .
docker images crunch-tracker-api:local
# REPOSITORY                TAG     SIZE
# crunch-tracker-api        local   ~210MB   <-- under 250MB: good
```

If it's 900MB+, your runtime stage is `FROM ...-jdk` instead of `...-jre`, or you copied the whole jar plus build tools. Inspect what shipped with `dive crunch-tracker-api:local` (see `resources.md`) — you'll see the layers and the wasted megabytes.

## Step 4 — Run it against a database

**Option A — a local Postgres (fastest to iterate):**

```bash
docker run --rm -d --name crunch-db \
  -e POSTGRES_DB=crunch -e POSTGRES_USER=crunch -e POSTGRES_PASSWORD=dev-only \
  -p 5432:5432 postgres:16

docker run --rm -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/crunch" \
  -e DATABASE_USER="crunch" \
  -e DATABASE_PASSWORD="dev-only" \
  -e JWT_SECRET="$(openssl rand -base64 32)" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:19006" \
  crunch-tracker-api:local
```

(`host.docker.internal` resolves to the host from inside the container on Docker Desktop; on plain Linux, use `--network host` and `localhost`, or run both in a user-defined network.)

**Option B — a managed Postgres** (Render/Neon/Supabase; this is what production uses). Provision one, copy the connection string, reshape it to JDBC form, and append `?sslmode=require`:

```bash
docker run --rm -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://dpg-xxxx.oregon-postgres.render.com:5432/crunch_db?sslmode=require" \
  -e DATABASE_USER="crunch_user" \
  -e DATABASE_PASSWORD="$MANAGED_DB_PASSWORD" \
  -e JWT_SECRET="$(openssl rand -base64 32)" \
  -e CORS_ALLOWED_ORIGINS="https://app.example.com" \
  crunch-tracker-api:local
```

Watch the logs: you should see Flyway report it ran `V1` through `Vn`, then Spring report the app started, then the health probe go `UP`.

## Step 5 — Prove it's healthy and the schema is real

```bash
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}

# Sign up and log in to prove the whole API path works through the container.
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"correct-horse-battery"}'

# Confirm Flyway built the schema from empty (not Hibernate ddl-auto).
psql "postgresql://crunch:dev-only@localhost:5432/crunch" -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

The `flyway_schema_history` rows are the proof: each migration ran exactly once, `success = t`. Drop the database, recreate it empty, run the container again — Flyway should rebuild the whole schema from scratch. If it can't, you have a database you can't reproduce, which means you don't have a deployable system.

## Step 6 — Confirm non-root and write it up

```bash
docker run --rm --entrypoint id crunch-tracker-api:local
# uid=999(spring) gid=999(spring) groups=999(spring)   <-- NOT uid=0(root)
```

Write a short `containerize.md` in your repo with: the final image size, the run command (with secrets redacted), the `flyway_schema_history` output, and the `id` output proving non-root.

## Expected output

A healthy startup against an empty managed Postgres looks roughly like:

```
Flyway Community Edition by Redgate
Successfully validated 7 migrations (execution time 00:00.04s)
Migrating schema "public" to version "1 - init"
Migrating schema "public" to version "2 - create habits"
...
Successfully applied 7 migrations to schema "public" (execution time 00:00.31s)
Started CrunchTrackerApplication in 4.12 seconds
```

And the checks:

```
image size:         ~210 MB         (under 250MB: PASS)
runs as:            uid=999(spring) (non-root: PASS)
GET /actuator/health: {"status":"UP"}
flyway history:     7 rows, all success=t (schema reproducible: PASS)
secrets in yaml:    none (all ${...} placeholders: PASS)
```

## Acceptance criteria

- [ ] Multi-stage Dockerfile: build stage uses the JDK, runtime stage uses only the JRE.
- [ ] Image under ~250MB (confirmed with `docker images`).
- [ ] Container runs as a non-root user (confirmed with `id`).
- [ ] Every config value comes from an environment variable; `application.yml` has no real secrets (only `${...}`).
- [ ] `ddl-auto: validate`, and Flyway builds the schema from an empty database (confirmed via `flyway_schema_history`).
- [ ] `GET /actuator/health` returns `{"status":"UP"}`, and a register/login round-trip works through the container.
- [ ] `containerize.md` written with the size, run command, Flyway history, and non-root proof.
