# Week 6 — Resources

Every resource on this page is **free**. Spring's reference docs are open. The relevant RFCs are public. OWASP is free and CC-licensed. The jjwt library is open source. No paywalled books are linked.

## Required reading (work it into your week)

- **Spring Security reference — Architecture** — how the filter chain actually fits together; read this before you write a line of config:
  <https://docs.spring.io/spring-security/reference/servlet/architecture.html>
- **Spring Security — Authentication** — `AuthenticationManager`, `UserDetailsService`, providers:
  <https://docs.spring.io/spring-security/reference/servlet/authentication/index.html>
- **Spring Security — Method Security** — `@EnableMethodSecurity`, `@PreAuthorize`, custom expressions:
  <https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html>
- **RFC 9457 — Problem Details for HTTP APIs** — the error contract every endpoint (including auth) should speak:
  <https://www.rfc-editor.org/rfc/rfc9457.html>
- **OWASP API Security Top 10 (2023)** — read at least #1 (Broken Object Level Authorization) and #2 (Broken Authentication):
  <https://owasp.org/API-Security/editions/2023/en/0x11-t10/>

## The specifications (skim, don't memorize)

You will not read these cover to cover, but the first time a teammate writes "that's not idempotent per RFC 9110 §9.2.2" you should know what they mean.

- **RFC 9110 — HTTP Semantics** — methods, status codes, idempotency, safety; the modern HTTP bible:
  <https://www.rfc-editor.org/rfc/rfc9110.html>
- **RFC 7519 — JSON Web Token (JWT)** — claims, structure, the `exp`/`iat`/`sub` registered claims:
  <https://www.rfc-editor.org/rfc/rfc7519.html>
- **RFC 7617 — The 'Basic' HTTP Authentication Scheme** — what we are deliberately *not* using, and why:
  <https://www.rfc-editor.org/rfc/rfc7617.html>
- **CORS — Fetch Standard (WHATWG)** — the normative source for preflight and `Access-Control-*`:
  <https://fetch.spec.whatwg.org/#http-cors-protocol>

## Official Spring docs

- **Spring Boot 3.x reference**: <https://docs.spring.io/spring-boot/index.html>
- **Spring Security 6.x reference (top)**: <https://docs.spring.io/spring-security/reference/index.html>
- **Spring Security — CORS**: <https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html>
- **Spring Security — Password storage / `PasswordEncoder`**: <https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>
- **Spring Data — Paging and Sorting (`Pageable`/`Page`)**: <https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html#repositories.special-parameters>

## Libraries we touch this week

- **jjwt (Java JWT)** — the library we use to sign and parse tokens; the README is the best quick reference for the builder/parser API:
  <https://github.com/jwtk/jjwt>
- **Spring Security Crypto** — `BCryptPasswordEncoder`, `DelegatingPasswordEncoder` (ships with Spring Security; no extra dependency):
  <https://docs.spring.io/spring-security/reference/features/integrations/cryptography.html>
- **springdoc-openapi** — keeps your OpenAPI docs in sync, including the bearer-auth scheme so "Authorize" works in Swagger UI:
  <https://springdoc.org/>

## Tools you'll use this week

- **HTTPie / curl** — to drive the API by hand. `http POST :8080/api/v1/auth/login email=... password=...` is faster than Swagger for the auth dance.
- **jwt.io** — paste a token to inspect its header and claims (decode only — never paste a *production* token into a website). <https://jwt.io/>
- **Docker / Docker Compose** — Postgres from week 5; nothing new to install this week.
- **`openssl rand -base64 64`** — generate a real HMAC signing secret instead of committing `"secret"` to Git.

## Spring Security 6 migration notes (read if you've seen old tutorials)

A lot of Spring Security material online is for version 5 and earlier. It will steer you wrong. The big breaking changes you must know:

- **`WebSecurityConfigurerAdapter` is gone.** You now declare a `SecurityFilterChain` `@Bean`. Any tutorial that extends `WebSecurityConfigurerAdapter` is obsolete.
- **The DSL is lambda-based.** `http.csrf(csrf -> csrf.disable())`, not `http.csrf().disable()`. The old non-lambda chain is removed in 6.x.
- **`authorizeHttpRequests`**, not `authorizeRequests` (the latter is removed).
- **Method security**: `@EnableMethodSecurity` (Spring Security 6), not the old `@EnableGlobalMethodSecurity`.

Spring's official migration guide is the canonical source:
<https://docs.spring.io/spring-security/reference/migration/index.html>

## Free, focused reading

- **Baeldung — Spring Security with a JWT** (free articles; verify against the current Spring Security 6 docs, as some posts lag):
  <https://www.baeldung.com/spring-security-oauth-jwt> and <https://www.baeldung.com/java-json-web-tokens-jjwt>
- **Spring guides — "Securing a Web Application"** (official, free, runnable):
  <https://spring.io/guides/gs/securing-web>
- **Auth0 — "JWT Handbook"** (free PDF after a no-cost signup; the best single explanation of token structure and pitfalls):
  <https://auth0.com/resources/ebooks/jwt-handbook>

## Videos (free, no signup)

- **Spring Developer channel** — official talks, including security deep-dives from SpringOne:
  <https://www.youtube.com/@SpringSourceDev>
- **Dan Vega — Spring Security 6 with JWT** — community channel that tracks the current API closely:
  <https://www.youtube.com/@danvega>

## Open-source projects to read this week

You learn more from one hour reading a real secured API than from three hours of tutorials. Pick one and scroll:

- **`spring-projects/spring-security`** — the framework itself; the `web/.../jwt` and `crypto/bcrypt` packages are readable:
  <https://github.com/spring-projects/spring-security>
- **`jwtk/jjwt`** — the JWT library, with a thorough README that doubles as documentation:
  <https://github.com/jwtk/jjwt>
- **`spring-petclinic/spring-petclinic-rest`** — a canonical Spring Boot REST sample with security wired in:
  <https://github.com/spring-petclinic/spring-petclinic-rest>

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Authentication (authn)** | *Who are you?* Proving identity. The JWT does this. |
| **Authorization (authz)** | *Are you allowed?* Deciding access. Ownership rules and roles do this. |
| **JWT** | JSON Web Token — a signed, base64url string carrying claims like `sub` and `exp`. Not encrypted, just signed. |
| **Claim** | A key/value inside the JWT payload, e.g. `sub` (subject/user id), `exp` (expiry). |
| **Bearer token** | A token where merely possessing it grants access. Sent as `Authorization: Bearer <token>`. |
| **BCrypt** | An adaptive password-hashing function with a built-in salt and a tunable work factor. |
| **Work factor / cost** | BCrypt's `2^cost` iteration count. Higher = slower = harder to brute-force. Default 10–12. |
| **`SecurityFilterChain`** | The Spring Security 6 bean that defines which filters run and what is permitted. |
| **`SecurityContext`** | Per-request holder of the authenticated principal. Your filter populates it. |
| **`OncePerRequestFilter`** | A servlet filter guaranteed to run once per request; where the bearer filter lives. |
| **CORS** | Cross-Origin Resource Sharing — browser rules that gate cross-origin requests via preflight + headers. |
| **Preflight** | The automatic `OPTIONS` request a browser sends before certain cross-origin calls. |
| **Idempotent** | Doing it twice has the same effect as once. `PUT`/`DELETE` are; `POST` is not, by default. |
| **`ProblemDetail`** | The RFC 9457 JSON error body (`type`, `title`, `status`, `detail`, `instance`). |
| **BOLA** | Broken Object Level Authorization — OWASP API #1; the "user A reads user B's data" bug. |

---

*If a link 404s, please open an issue so we can replace it.*
