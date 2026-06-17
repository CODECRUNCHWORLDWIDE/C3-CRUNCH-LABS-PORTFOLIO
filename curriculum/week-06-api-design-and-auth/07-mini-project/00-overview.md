# Mini-Project — Accounts and Per-User Scoping for Crunch Tracker

> Turn Crunch Tracker from a single-user demo into a multi-user product. Add registration, JWT login, BCrypt-hashed passwords, CORS for the coming mobile client, and per-user data scoping so every goal, habit, and check-in belongs to its owner — enforced at the query layer, declared with method security, and **proven by tests**. You extend the week-5 Spring Boot 3 + Postgres + Flyway codebase. You do not start over.

This is the week Crunch Tracker stops being "an API on my laptop that anyone who finds it can read" and becomes "a service multiple people can use safely, at the same time, without seeing each other's data." Every later week depends on it: week 9's mobile app logs in against the endpoint you build here.

**Estimated time:** ~11.5 hours (split across Thursday through Sunday in the suggested schedule).

---

## What you will build

Add three new endpoints and secure all the existing ones:

```bash
# Register a new account; returns a JWT.
POST /api/v1/auth/register   { "email": "...", "password": "...", "displayName": "..." }

# Log in; returns a JWT.
POST /api/v1/auth/login      { "email": "...", "password": "..." }

# (optional) Who am I? — echoes the authenticated user. Handy for the mobile client.
GET  /api/v1/auth/me         (requires Bearer token)
```

And the change that matters most: **every existing endpoint now requires a token and only ever touches the caller's own data.**

```bash
# Before this week: returned every habit in the database, to anyone.
# After this week: returns only the signed-in user's habits, and 401s without a token.
GET  /api/v1/habits          (requires Bearer token; scoped to the caller)
```

By the end you have a secured, multi-user API that the mobile client (week 7+) can register against, log in to, and trust.

---

## Rules

- **You may** read the Spring Security docs, the jjwt README, the RFCs, OWASP, the lecture notes, and the source of the libraries below.
- **You may** depend on these and only these new dependencies:
  - `spring-boot-starter-security` (the filter chain, BCrypt, method security).
  - `io.jsonwebtoken:jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.x — the token library).
  - `spring-security-test` (for `MockMvc` auth helpers in tests).
- **You may NOT** pull in an OAuth2 server, Keycloak, Auth0 SDK, or any external identity provider. We issue our own tokens against our own user table. (Swapping to OIDC later is a known, deliberate future step — not this week.)
- **You must** keep the week-5 contract working: existing endpoints keep their shapes (now under `/api/v1` and now secured). Existing Testcontainers integration tests must still pass, updated to send a token.
- Target: Java 21, Spring Boot 3.x, Spring Security 6.x, PostgreSQL 16 in Docker, Flyway migrations.
- **The signing secret is config, never code.** A committed secret fails review automatically.

---

## Acceptance criteria

- [ ] The repo builds: `./mvnw verify` is green (unit + Testcontainers integration).
- [ ] A new Flyway migration (`V6__...` or next in sequence) creates `app_user` and adds `owner_id` foreign keys to `goals` and `habits` (and indexes them).
- [ ] `POST /api/v1/auth/register` creates a user, hashes the password with BCrypt, returns `201` + a JWT, and returns `409` for a duplicate email.
- [ ] `POST /api/v1/auth/login` returns `200` + a JWT for correct credentials and **one** `401` for either wrong password *or* unknown email (no enumeration leak).
- [ ] Passwords are **never** stored in plaintext, **never** logged, and **never** present in any response DTO.
- [ ] Every non-auth endpoint returns `401` (RFC 9457 `ProblemDetail`) without a valid token.
- [ ] Every owned query is scoped by `owner_id`; a logged-in user sees and mutates **only** their own goals/habits/check-ins.
- [ ] A wrong-owner request returns your chosen `403`/`404` (documented), enforced by both owner-scoped queries and `@PreAuthorize`, and **does not leak** the other user's data.
- [ ] CORS is configured for `http://localhost:8081` (Expo dev) and a placeholder web origin, with `allowCredentials(true)` and **no** wildcard origin.
- [ ] OpenAPI docs include the bearer-auth scheme so the Swagger UI "Authorize" button works.
- [ ] At least **18** tests total, including the security negative paths: no token `401`, expired `401`, tampered `401`, wrong owner `403`/`404`, duplicate email `409`.
- [ ] `README.md` documents setup, the auth flow (with a copy-pasteable `curl`/HTTPie sequence), and a "Things I learned" section with at least 3 specific items.

---

## Suggested order of operations

Build incrementally. Get one slice green end-to-end before adding the next.

### Phase 1 — The user table and entity (~1.5h)

1. Write the Flyway migration: `app_user` (id, email unique, password_hash, display_name, created_at) and `ALTER TABLE goals/habits ADD COLUMN owner_id ... REFERENCES app_user(id)`, with an index on each `owner_id`.
   - For existing rows from week 5, either truncate the dev DB or backfill a seed user — note which you did. A multi-user table can't have null owners.
2. Add the `AppUser` JPA entity and `AppUserRepository` with `findByEmail`, `existsByEmail`.
3. Commit: `users table + entity`.

### Phase 2 — Passwords and the JWT service (~2h)

1. Add `spring-boot-starter-security` and jjwt to `pom.xml`.
2. Add the `PasswordEncoder` bean (`DelegatingPasswordEncoder`).
3. Implement `JwtService` from exercise 2 (issue + validate, secret from config). Bring its unit tests over and confirm they pass.
4. Commit: `JwtService + BCrypt encoder`.

### Phase 3 — The filter chain (~2h)

1. Implement `JwtAuthFilter` (`OncePerRequestFilter`) — read the bearer header, validate, load the user, set the principal.
2. Implement `SecurityConfig` from exercise 3 — stateless, CSRF off, `/api/v1/auth/**` open, everything else authenticated, `ProblemDetail` 401/403 handlers, filter wired before `UsernamePasswordAuthenticationFilter`.
3. Add the `CorsConfigurationSource` bean (Lecture 1 §9).
4. Smoke test by hand: `GET /api/v1/habits` → `401`; you can reach `/api/v1/auth/**`.
5. Commit: `Spring Security 6 filter chain + CORS`.

### Phase 4 — Register, login, me (~1.5h)

1. `AuthController` with `register` (`201` + token, `409` on dup), `login` (`200` + token, single `401`), and optional `me` (`GET`, returns a `UserResponse` with **no** password field).
2. DTOs with bean validation: `@Email`, `@NotBlank`, `@Size(min = 12)` on the password.
3. Add the `EmailAlreadyUsedException` handler to your `@RestControllerAdvice` (`409` `ProblemDetail`).
4. Drive the full dance with HTTPie (register → login → call a secured endpoint with the token).
5. Commit: `register/login/me endpoints`.

### Phase 5 — Per-user scoping + ownership (~2.5h)

1. Add `owner_id` to the `Goal`/`Habit` entities; set it from `caller.id()` on every create.
2. Convert every repository read to owner-scoped (`findByIdAndOwnerId`, `findAllByOwnerId`, `existsByIdAndOwnerId`).
3. Add the `@owns` evaluator and `@PreAuthorize` on id-taking endpoints (this is the challenge, integrated).
4. Update **every** controller to take `@AuthenticationPrincipal AppUser caller` and pass `caller.id()` down.
5. Commit: `per-user data scoping + ownership rules`.

### Phase 6 — Tests (~1.5h)

1. Update existing integration tests to authenticate (mint a token in setup, send it as a header).
2. Add the security negative-path tests: no token, expired, tampered, wrong owner, duplicate email.
3. Add the "wrong owner can't delete and the data survives" regression test.
4. **Watch one fail first:** temporarily un-scope a query, see a wrong-owner test go red, revert.
5. Commit: `security integration tests`.

### Phase 7 — OpenAPI + polish (~0.5h)

1. Configure springdoc with a bearer-auth `SecurityScheme` so Swagger UI's "Authorize" works.
2. Run `./mvnw verify`. Write the README auth-flow section with real copy-pasteable commands.
3. Open a PR (or push to your week-6 repo). Confirm green on a fresh clone with `docker compose up -d db`.

---

## Example auth flow (put this in your README)

```bash
# 1. Bring up the database.
docker compose up -d db

# 2. Run the API (set a real secret).
export CRUNCH_JWT_SECRET="$(openssl rand -base64 64)"
./mvnw spring-boot:run

# 3. Register.
http POST :8080/api/v1/auth/register \
    email=ada@crunch.dev password=correcthorsebattery displayName=Ada
# -> 201 { "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 900 }

# 4. Log in and capture the token.
TOKEN=$(http POST :8080/api/v1/auth/login \
    email=ada@crunch.dev password=correcthorsebattery -b | jq -r .accessToken)

# 5. No token -> 401 ProblemDetail.
http :8080/api/v1/habits

# 6. With the token -> 200, only Ada's habits.
http :8080/api/v1/habits "Authorization: Bearer $TOKEN"

# 7. Create a habit (owner set from the token, not the body).
http POST :8080/api/v1/habits "Authorization: Bearer $TOKEN" \
    name="Morning run" targetPerWeek:=5
```

---

## Rubric

| Criterion | Weight | What "great" looks like |
|----------|-------:|-------------------------|
| Builds and runs | 15% | `./mvnw verify`, `docker compose up -d db`, the curl flow above all clean on a fresh clone |
| Auth flow correctness | 20% | register/login return correct codes and tokens; one `401` for both login failures; `409` on dup email |
| Password hygiene | 10% | BCrypt via `PasswordEncoder`; never stored plaintext, logged, or returned; verified in a test |
| Per-user scoping | 20% | every owned query filtered by `owner_id`; the owner comes from the token, never the URL/body |
| Ownership enforcement + proof | 20% | `@PreAuthorize` + scoped queries; wrong-owner negative-path tests; a regression test that fails if enforcement is removed |
| CORS + error contract | 10% | explicit origins, no wildcard-with-credentials; auth errors are `ProblemDetail`, not HTML/stack traces |
| README quality | 5% | someone unfamiliar can clone, run, register, and call a secured endpoint in <10 minutes |

---

## What this prepares you for

- **Week 7–8** build the React Native client. The **CORS config** you wrote (the Expo dev origin) and the **login endpoint** are the very first thing it talks to.
- **Week 9** wires the client to this API for real: the mobile app logs in against `/api/v1/auth/login`, stores the JWT, and sends it on every request via a typed fetch client. The `401`/`403` behavior you tested here is exactly what the app's error UX is built around.
- **Week 10** ships it. The `CRUNCH_JWT_SECRET`-from-environment discipline you practiced is what lets the same image run in dev and prod with different secrets and no code change.

The seam you built — "the owner is the authenticated principal, enforced at the query and declared with method security" — is the single most important security decision in the whole course. Every feature added after this inherits it for free.

---

## Resources

- *Spring Security — Architecture*: <https://docs.spring.io/spring-security/reference/servlet/architecture.html>
- *Spring Security — Method Security*: <https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html>
- *jjwt (Java JWT)*: <https://github.com/jwtk/jjwt>
- *RFC 7519 — JSON Web Token*: <https://www.rfc-editor.org/rfc/rfc7519.html>
- *OWASP API Security Top 10 (2023)*: <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>
- *springdoc-openapi (bearer auth in Swagger UI)*: <https://springdoc.org/#how-can-i-define-securityscheme>

---

## Submission

When done:

1. Push your repo (or open a PR against your week-6 branch).
2. Make sure `README.md` includes the setup commands and the full auth-flow `curl` sequence.
3. Make sure `./mvnw verify` is green on a freshly cloned copy with only `docker compose up -d db` and `CRUNCH_JWT_SECRET` set.
4. In your PR description, name the wrong-owner test and confirm you watched it fail before it passed. Post the repo URL in your cohort tracker. You shipped real auth; show it.
