# Week 6 — API Design and Auth

Welcome to **C3 · Crunch Labs Portfolio**, week 6. Up to now Crunch Tracker has been a single-user demo: anyone who can reach the API can read and write every goal, habit, and check-in in the database. That ends this week. By Friday the API will know *who* is calling, refuse the ones it doesn't recognize, and make sure user A can never see — or touch — user B's data.

We are doing two related things at once, and it helps to keep them separate in your head:

1. **API design that ages well.** Versioning, idempotency, pagination, sorting, filtering, and CORS. These are the decisions that, if you get them wrong now, you live with for years because clients depend on them. The mobile app you build starting next week is your first real client; it should not have to be rewritten every time the backend changes.
2. **Stateless authentication and authorization with Spring Security 6.** Registration, login, BCrypt password hashing, signed JWTs, the security filter chain, and method-level authorization with `@PreAuthorize`. This is where "a service on the internet" becomes "a multi-user product."

The thing to internalize up front is that **authentication and authorization are different questions**. Authentication answers *who are you?* — a JWT proves identity. Authorization answers *are you allowed to do this?* — ownership rules and roles decide access. A JWT that proves you are user 42 does not, by itself, let you read user 99's habits. We enforce that separately, and we write tests that prove the 403s fire.

We assume the week-5 Crunch Tracker is working: Spring Boot 3.x, Spring Data JPA over PostgreSQL in Docker, Flyway migrations, Testcontainers integration tests. We extend that exact codebase. We do not start over.

## Learning objectives

By the end of this week, you will be able to:

- **Distinguish** authentication from authorization, and stateless (token) auth from stateful (session) auth — three things people regularly conflate.
- **Design** a REST surface that survives change: URI versioning, idempotent writes, cursor or offset pagination, and a consistent error contract (RFC 9457 `ProblemDetail`).
- **Configure** the Spring Security 6 filter chain with a `SecurityFilterChain` bean, `httpBasic`/`formLogin` disabled, sessions set to stateless, and a custom JWT filter.
- **Hash** passwords correctly with BCrypt through Spring Security's `PasswordEncoder`, and explain why you never store, log, or return a password.
- **Issue and validate** JSON Web Tokens — sign with HMAC-SHA256, set `sub`, `iat`, `exp`, validate the signature, and reject expired or tampered tokens.
- **Build** register and login endpoints that create a user and return a token, returning correct status codes (`201`, `200`, `401`, `409`).
- **Enforce** per-user data ownership so every goal/habit/check-in belongs to its owner, both with query scoping and with `@PreAuthorize` method security.
- **Configure** CORS deliberately for a mobile/web client without opening the API to the whole internet.
- **Write** integration tests that prove a logged-in user can reach their own data and is given a `403` (not a `404`, not a stack trace) when they reach for someone else's.

## Prerequisites

This week assumes you have completed **C3 weeks 1–5**, or have equivalent experience. Specifically:

- You can build and run the week-5 Crunch Tracker: `./mvnw spring-boot:run` brings up the API, `docker compose up -d db` brings up Postgres, Flyway migrates on boot.
- You are comfortable writing a Spring `@RestController`, a DTO, a `@Service`, and a Spring Data `JpaRepository`.
- You can write a JPA entity with `@OneToMany`/`@ManyToOne` and reason about the N+1 trap.
- You can write a JUnit 5 + AssertJ test and a `@SpringBootTest` integration test backed by Testcontainers.
- You can read and write basic Git and open a PR.

You do **not** need any prior Spring Security exposure. We start from the filter chain. If you have used the old Spring Security (the `WebSecurityConfigurerAdapter` style, removed in Spring Security 6), you will need to unlearn it; we flag the differences as we go.

## Topics covered

- Authentication vs authorization; stateless tokens vs server-side sessions; why mobile clients prefer tokens.
- REST design for longevity: resource modeling, URI versioning (`/api/v1/...`), and when *not* to version.
- Idempotency: which HTTP methods are idempotent by contract, and how an `Idempotency-Key` header makes `POST` safe to retry.
- Pagination: offset (`page`/`size`) vs cursor (keyset), and Spring Data's `Pageable`/`Page<T>`.
- Filtering and sorting as query parameters, with validation so a bad `sort` is a `400`, not a `500`.
- The error contract: RFC 9457 `ProblemDetail`, and why every error — including auth failures — should use it.
- CORS from first principles: the preflight `OPTIONS` request, `Access-Control-Allow-*` headers, and `CorsConfigurationSource`.
- Spring Security 6: the `SecurityFilterChain` bean, the filter chain order, `AuthenticationManager`, `UserDetailsService`.
- Password hashing: BCrypt, work factor, salts, and `DelegatingPasswordEncoder`.
- JSON Web Tokens: structure (header.payload.signature), HMAC signing, claims (`sub`, `iat`, `exp`, `jti`), and validation.
- A custom `OncePerRequestFilter` that reads the `Authorization: Bearer` header, validates the token, and populates the `SecurityContext`.
- Method security: `@EnableMethodSecurity`, `@PreAuthorize`, and a `@Bean` ownership evaluator (`@PreAuthorize("@owns.habit(#id)")`).
- Per-user data scoping at the repository layer (every query filtered by owner id) and why that is your real defense.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target.

| Day       | Focus                                              | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|----------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | REST design, versioning, idempotency, pagination   |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | CORS, error contract, the v1 surface               |    1h    |    2h     |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5h      |
| Wednesday | Spring Security 6 filter chain, BCrypt, register   |    2h    |    2h     |     1h     |    0.5h   |   1h     |     0h       |    0h      |     6.5h    |
| Thursday  | JWT issue/validate, the bearer filter, login       |    1h    |    1.5h   |     1h     |    0.5h   |   1h     |     1h       |    0.5h    |     6.5h    |
| Friday    | Method security, ownership rules; mini-project work |    0h    |    1h     |     0h     |    0.5h   |   1h     |     2.5h     |    0.5h    |     5.5h    |
| Saturday  | Mini-project deep work                             |    0h    |    0h     |     0h     |    0h     |   0h     |     3.5h     |    0h      |     3.5h    |
| Sunday    | Quiz, review, polish                               |    0h    |    0h     |     0h     |    1h     |   0h     |     1h       |    0h      |     2h      |
| **Total** |                                                    | **6h**   | **8h**    | **2h**     | **3.5h**  | **5h**   | **11.5h**    | **2h**     | **35.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./00-overview.md) | This overview (you are here) |
| [resources.md](./01-resources.md) | Curated Spring Security, JWT, REST, and OWASP references |
| [lecture-notes/01-rest-api-design-that-ages-well.md](./02-lecture-notes/01-rest-api-design-that-ages-well.md) | Versioning, idempotency, pagination, filtering, the error contract, CORS |
| [lecture-notes/02-stateless-auth-with-spring-security-6.md](./02-lecture-notes/02-stateless-auth-with-spring-security-6.md) | The filter chain, BCrypt, JWTs, the bearer filter, method security, ownership |
| [exercises/README.md](./03-exercises/00-overview.md) | Index of the week's exercises |
| [exercises/exercise-01-design-the-v1-surface.md](./03-exercises/exercise-01-design-the-v1-surface.md) | Design and document the versioned, paginated v1 API on paper and in code |
| [exercises/exercise-02-jwt-service.java](./03-exercises/exercise-02-jwt-service.java) | Fill-in-the-TODO JWT issue/validate service with jjwt |
| [exercises/exercise-03-security-config.java](./03-exercises/exercise-03-security-config.java) | Wire the Spring Security 6 filter chain, BCrypt, and the bearer filter |
| [challenges/README.md](./04-challenges/00-overview.md) | Index of weekly challenges |
| [challenges/challenge-01-ownership-authorization.md](./04-challenges/challenge-01-ownership-authorization.md) | Enforce a `@PreAuthorize` ownership rule and prove the 403s fire |
| [quiz.md](./05-quiz.md) | 10 multiple-choice questions |
| [homework.md](./06-homework.md) | Six practice problems for the week |
| [mini-project/README.md](./07-mini-project/00-overview.md) | Full spec for adding accounts and per-user scoping to Crunch Tracker |

## The "all green, all scoped" promise

C3 uses a recurring marker in every exercise that ends in working code. This week it is two lines:

```
BUILD SUCCESS · Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
SECURITY: 401 on no token · 403 on wrong owner · 200 on own data
```

If your tests are green but you have not written the test that proves user A gets a `403` reaching for user B's habit, you are not done. A passing happy path is not security. The whole point of week 6 is that the *unhappy* paths — no token, expired token, tampered token, wrong owner — behave correctly and are *tested*.

## A note on what we are not doing

- **OAuth2 / OpenID Connect / social login.** Real, important, and out of scope. We issue our own JWTs against our own user table. If you later swap in "Sign in with Google," the filter chain you build this week is exactly where it plugs in.
- **Refresh tokens and rotation.** We issue a single short-lived access token. We discuss refresh tokens conceptually and leave them as a stretch goal; the mobile-integration week (9) is where they earn their keep.
- **Spring Authorization Server.** That is for *issuing* tokens to *other* services as an identity provider. Overkill for one app with one client. We use it nowhere this week.

Keep the scope tight. A correct, tested, single-token auth flow with real ownership enforcement is worth far more than a half-finished OAuth integration.

## Stretch goals

If you finish early and want to push further:

- Read the **OWASP API Security Top 10 (2023)** and map each item to a place in your code: <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>. "Broken Object Level Authorization" is #1 — it is exactly the ownership bug this week exists to prevent.
- Add a `/api/v1/auth/refresh` endpoint that exchanges a valid (unexpired) token for a fresh one, and reason about the tradeoffs versus a separate refresh-token table.
- Add a rate limiter on `/auth/login` (e.g. Bucket4j) so password-guessing is expensive. Note what you would need to make it work across multiple API instances.
- Replace the symmetric HMAC signature with an asymmetric RS256 key pair and explain when you'd prefer it (hint: when the validator is a different service than the issuer).

## Up next

Continue to **Week 7 — React Native Basics** once you have pushed the mini-project and your auth integration tests are green. Next week the *client* arrives, and the CORS config and login endpoint you built this week are the first thing it talks to.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
