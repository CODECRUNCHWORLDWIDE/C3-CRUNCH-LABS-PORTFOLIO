# Week 6 — Exercises

Short, focused drills. Each one should take 35–60 minutes. Do them in order; later ones assume earlier ones. They all extend the week-5 Crunch Tracker codebase — you are not starting a new project.

## Index

1. **[Exercise 1 — Design the v1 surface](exercise-01-design-the-v1-surface.md)** — design and document the versioned, paginated, error-consistent v1 API, then wire the central `/api/v1` prefix and a `Page<T>` list endpoint. (~50 min)
2. **[Exercise 2 — The JWT service](exercise-02-jwt-service.java)** — fill in the TODOs in a `JwtService` that issues and validates HS256 tokens with jjwt, then prove it round-trips and rejects tampered/expired tokens. (~45 min)
3. **[Exercise 3 — The security config](exercise-03-security-config.java)** — wire the Spring Security 6 `SecurityFilterChain`, BCrypt encoder, and the bearer filter so `/auth/**` is open and everything else needs a token. (~50 min)

## How to work the exercises

- Read the prompt. Skim, don't memorize.
- **Type the code yourself.** Do not copy-paste. The muscle memory of wiring a filter chain by hand is the entire point.
- Run it. Hit it with `curl`/HTTPie. See the `401`, then the `200` once you send the token.
- If you get stuck for more than 10 minutes, peek at the inline hints at the bottom of each file.
- Every exercise must end with `./mvnw test` green **and** a deliberate negative check: a request that *should* be rejected actually is.

## The checklist

You can consider the week's exercises done when:

- [ ] Every endpoint lives under `/api/v1`, applied by a central path prefix (not repeated per controller).
- [ ] At least one list endpoint returns a `Page<T>` with a capped page size and a validated `sort`.
- [ ] Errors — including auth errors — return an RFC 9457 `ProblemDetail`, not a stack trace or an HTML page.
- [ ] `JwtService.issue` mints a token whose `sub` is the user id, with `iat` and `exp` set.
- [ ] `JwtService.validateAndGetUserId` returns the id for a good token and **empty** for a tampered, expired, or malformed one.
- [ ] The signing secret is read from configuration (`crunch.jwt.secret`), never hard-coded.
- [ ] The `SecurityFilterChain` is stateless, CSRF disabled, `/api/v1/auth/**` permitted, everything else authenticated.
- [ ] The `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter` and populates the `SecurityContext`.
- [ ] A request with no token gets `401`; a valid token gets through; you have *seen* both happen.
- [ ] `./mvnw test`: `Failures: 0, Errors: 0`.

There are no solutions checked in. The course is open source — solutions live in forks. After you finish, search GitHub for `c3-week-06` to compare.
