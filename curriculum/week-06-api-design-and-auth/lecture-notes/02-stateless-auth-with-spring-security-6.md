# Lecture 2 — Stateless Auth with Spring Security 6

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can configure the Spring Security 6 filter chain for a stateless JWT API, hash passwords with BCrypt, issue and validate signed tokens, populate the `SecurityContext` from a bearer header in a custom filter, and enforce per-user ownership with `@PreAuthorize` — and you understand each piece well enough to debug it when it misbehaves at 2am.

If you only remember one thing from this lecture, remember this:

> **A JWT proves identity. It does not grant access.** A token that says "I am user 42" lets the server *know* you're user 42; it does **not**, by itself, let you read user 99's habits. Authentication and authorization are two separate gates, and the second one — ownership — is the one that's almost always implemented wrong. This lecture builds both gates and tests that both close.

We extend the exact Crunch Tracker codebase from Lecture 1 and week 5. By the end, every endpoint requires a valid token, every data query is scoped to the owner, and the integration tests prove that user A gets a `403` (not a leak) reaching for user B's data.

---

## 1. Stateless vs stateful auth — and why we go stateless

Two ways a server can remember who you are:

- **Stateful (session) auth.** On login, the server creates a session, stores it server-side (memory, Redis, DB), and hands the browser a session-id cookie. Every request sends the cookie; the server looks up the session. The *server* holds the state. This is the classic Spring `HttpSession` model.
- **Stateless (token) auth.** On login, the server issues a **signed token** containing the user's identity. The *client* holds it and sends it on every request (`Authorization: Bearer <token>`). The server validates the signature and trusts the claims. The server stores **nothing** between requests.

For a mobile-first API like Crunch Tracker, **stateless wins**:

- **No session store to scale.** Run five API instances behind a load balancer; any of them can validate any token. No sticky sessions, no shared Redis just for auth.
- **Mobile clients don't love cookies.** A native app sending a `Bearer` header is simpler and more explicit than juggling cookie jars.
- **The token is self-contained.** It carries the user id and expiry; the server doesn't hit the database to know who you are (though we *will* hit it to load the full user — more below).

The tradeoff: **you can't easily revoke a stateless token before it expires.** With sessions you delete the session and the user is out instantly. With a JWT, if it's stolen, it's valid until `exp`. We mitigate this by keeping access tokens **short-lived** (15 minutes is typical) and discussing refresh tokens as a stretch goal. Know the tradeoff; it's the price of statelessness.

---

## 2. The anatomy of a JWT

A JWT is three base64url-encoded parts joined by dots: `header.payload.signature`.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiIsImV4cCI6MTcxODAwMDAwMH0.x9f2...sig
└──── header ────┘ └────────── payload ──────────┘ └─ signature ─┘
```

Decode the first two parts and they're just JSON:

```json
// header
{ "alg": "HS256", "typ": "JWT" }

// payload (the "claims")
{ "sub": "42", "iat": 1717996400, "exp": 1717997300 }
```

The **registered claims** you'll use:

- `sub` — *subject*: who the token is about. We put the user id here.
- `iat` — *issued at*: when it was minted (Unix seconds).
- `exp` — *expiry*: when it stops being valid (Unix seconds). **Always set this.**
- `jti` — *JWT id*: a unique id per token, useful if you ever build a revocation list.

> **A JWT is signed, not encrypted.** Anyone who has the token can read the payload — paste it into jwt.io and see your claims in plaintext. The signature only guarantees the token *wasn't tampered with* and *was issued by you*. **Never put a secret in a JWT** (no passwords, no PII you wouldn't log). The `sub` is a user id, not a name or email.

The **signature** is the security. The server computes `HMAC-SHA256(header + "." + payload, secret)` and appends it. On every request it recomputes the HMAC over the received header+payload and checks it matches. Change one byte of the payload and the signature no longer matches — the token is rejected. The secret never leaves the server.

---

## 3. The user table and BCrypt

Before anyone can log in, they have to register. Add a `users` table via Flyway (week 5's tooling):

```sql
-- V6__create_users.sql
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(72)  NOT NULL,
    display_name  VARCHAR(120) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- every owned table now references the owner
ALTER TABLE goals  ADD COLUMN owner_id BIGINT NOT NULL REFERENCES app_user(id);
ALTER TABLE habits ADD COLUMN owner_id BIGINT NOT NULL REFERENCES app_user(id);
CREATE INDEX idx_habits_owner ON habits(owner_id);
```

Notice the column is `password_hash`, never `password`. **You never store a password.** You store a one-way hash, and at login you hash the attempt and compare. If your database is stolen, the attacker has hashes, not passwords — and with BCrypt those hashes are expensive to crack.

### Why BCrypt and not SHA-256

A naive `SHA-256(password)` is *wrong* for passwords. SHA-256 is fast — billions of guesses per second on a GPU — which is exactly what you *don't* want for password storage. **BCrypt is deliberately slow and adaptive:**

- It has a built-in **salt** (a random per-password value) so two users with the same password get different hashes and precomputed "rainbow tables" are useless.
- It has a **work factor** (cost). Each increment doubles the work. At cost 12 a single hash takes ~250ms — trivial for one login, ruinous for an attacker trying billions.

Spring Security gives you BCrypt through `PasswordEncoder`:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // DelegatingPasswordEncoder writes a {bcrypt}$2a$... prefix so you can
    // migrate algorithms later. Cost 12 is a good 2026 default.
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

Registration hashes; login matches:

```java
// register
String hash = passwordEncoder.encode(request.password());   // {bcrypt}$2a$12$...
userRepository.save(new AppUser(request.email(), hash, request.displayName()));

// login
if (!passwordEncoder.matches(attempt, user.passwordHash())) {
    throw new BadCredentialsException("Invalid email or password");
}
```

> **Two non-negotiables.** (1) Never log the raw password — not in a debug statement, not in an exception. (2) Never return it, even hashed, in any response DTO. The `AppUser` entity has a `passwordHash`; the `UserResponse` DTO does not. Map deliberately; a field can't leak if it isn't in the response shape.
>
> One more: at login, return the **same** error for "no such email" and "wrong password" — `401 "Invalid email or password"`. Distinguishing them tells an attacker which emails are registered (an enumeration leak).

---

## 4. The JWT service

A small service that does two things: mint a token for a user, and validate an incoming token. We use **jjwt** (`io.jsonwebtoken`).

```java
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(
            @Value("${crunch.jwt.secret}") String secret,
            @Value("${crunch.jwt.ttl:PT15M}") Duration ttl) {
        // The secret must be at least 256 bits for HS256. Load it from config,
        // never hard-code it. Generate one with: openssl rand -base64 64
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    /** Mint a signed token whose subject is the user id. */
    public String issue(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .id(UUID.randomUUID().toString())          // jti
                .signWith(key)
                .compact();
    }

    /** Validate signature + expiry and return the user id, or empty if invalid. */
    public Optional<Long> validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)                        // checks the signature
                    .clockSkewSeconds(30)                   // tolerate small clock drift
                    .build()
                    .parseSignedClaims(token)               // throws on tamper/expiry
                    .getPayload();
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException ex) {
            // expired, malformed, bad signature, wrong type — all land here
            return Optional.empty();
        }
    }
}
```

Two things to internalize:

- **`parseSignedClaims` does the work.** It recomputes the HMAC and compares it to the signature; if they differ, it throws. It checks `exp` against the clock; if expired, it throws. You don't validate by hand — you let the library throw and you catch.
- **The secret comes from config, never code.** `crunch.jwt.secret` lives in an environment variable in real deployments. Committing `secret: "changeme"` to Git is the classic auth bug — anyone with the repo can mint valid tokens for any user.

---

## 5. The bearer filter — where the token becomes a principal

Every request carries the token in `Authorization: Bearer <token>`. Something must read that header on every request, validate the token, and tell Spring Security "this request is authenticated as user 42." That something is a filter that runs **once per request**.

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final AppUserRepository users;

    public JwtAuthFilter(JwtService jwt, AppUserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            jwt.validateAndGetUserId(token)
               .flatMap(users::findById)
               .ifPresent(user -> {
                   var auth = new UsernamePasswordAuthenticationToken(
                           user,                 // the principal — our AppUser
                           null,                 // no credentials needed past this point
                           user.authorities());  // roles, if any
                   auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                   SecurityContextHolder.getContext().setAuthentication(auth);
               });
        }

        // Always continue the chain. If we did NOT authenticate, the request
        // proceeds unauthenticated and the authorization rules will reject it.
        chain.doFilter(request, response);
    }
}
```

The flow:

1. Pull the `Authorization` header. No header, or not a `Bearer`? Do nothing and continue — the request is unauthenticated and will hit the `401` wall later.
2. Validate the token. Invalid (expired, tampered)? Do nothing and continue. Same wall.
3. Valid? Load the `AppUser` and set it as the authenticated principal in the `SecurityContext`.

> **Why load the user from the DB if the token already has the id?** Because the token is a snapshot from minutes ago. The user might have been disabled, their roles changed. Loading fresh costs one indexed primary-key lookup and keeps authorization honest. It's a reasonable tradeoff; some high-scale systems skip it and trust the token's claims entirely (truly stateless). For Crunch Tracker, load it.
>
> Note this is also what makes `@AuthenticationPrincipal AppUser caller` work in your controllers (Lecture 1) — the principal you set here is exactly what Spring injects there.

---

## 6. The filter chain — Spring Security 6 config

This is the heart of the configuration. **Forget every pre-6 tutorial** — there is no `WebSecurityConfigurerAdapter` anymore. You declare a `SecurityFilterChain` bean.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity              // turns on @PreAuthorize
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsSource;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, CorsConfigurationSource corsSource) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsSource = corsSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS from Lecture 1.
            .cors(cors -> cors.configurationSource(corsSource))
            // No CSRF tokens: we're a stateless token API, not a cookie/session app.
            .csrf(csrf -> csrf.disable())
            // Stateless: never create or use an HttpSession.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Who can hit what, before authentication.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()       // register + login are open
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()                          // everything else needs a token
            )
            // We don't want the browser login form or basic-auth popup.
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            // Make auth failures speak ProblemDetail (Lecture 1's contract).
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(problemDetailEntryPoint())   // 401
                .accessDeniedHandler(problemDetailAccessDenied())      // 403
            )
            // Our filter runs BEFORE the username/password filter so the
            // SecurityContext is populated by the time authorization runs.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // problemDetailEntryPoint() and problemDetailAccessDenied() write an
    // RFC 9457 body — see the exercise for the full implementation.
}
```

Walk the config line by line; every line is load-bearing:

- **`csrf().disable()`** — CSRF protection defends cookie-based sessions from forged cross-site form submits. We don't use cookies; the token isn't sent automatically by the browser, so there's nothing to forge. Disabling CSRF here is correct, **not** lazy. (If you were using cookies, you'd leave it on.)
- **`SessionCreationPolicy.STATELESS`** — Spring won't create an `HttpSession`. Every request re-authenticates from its token. This is what "stateless" means concretely.
- **`authorizeHttpRequests`** — the rule order matters. `/api/v1/auth/**` is `permitAll()` (you can't require a token to *get* a token). Everything else is `authenticated()`. The `anyRequest()` rule must be **last**.
- **`addFilterBefore(...)`** — places our filter early enough that the `SecurityContext` is populated before authorization decisions are made. Get the order wrong and you're authenticated *after* the gate already rejected you.

---

## 7. Register and login endpoints

These two endpoints are the only ones that are `permitAll()`. Everything else needs the token they hand out.

```java
@RestController
@RequestMapping("/auth")    // /api/v1 added centrally (Lecture 1)
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthenticationManager authManager;

    // constructor omitted for brevity

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new EmailAlreadyUsedException(req.email());   // -> 409 ProblemDetail
        }
        AppUser saved = users.save(new AppUser(
                req.email(),
                encoder.encode(req.password()),                 // hash here
                req.displayName()));
        String token = jwt.issue(saved);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new TokenResponse(token, "Bearer", jwt.ttlSeconds()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        AppUser user = users.findByEmail(req.email())
                .filter(u -> encoder.matches(req.password(), u.passwordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return new TokenResponse(jwt.issue(user), "Bearer", jwt.ttlSeconds());
    }
}
```

`RegisterRequest` carries `@Email`, `@NotBlank`, and a `@Size(min = 12)` password constraint — validation from week 4, now protecting account creation. The response is a `TokenResponse(accessToken, tokenType, expiresIn)`; the client stores the token and sends it on every subsequent call.

Note `login` returns one `401` for both "unknown email" and "wrong password" — the `.filter(...).orElseThrow(...)` collapses both into the same `BadCredentialsException`. That's the enumeration defense from §3.

---

## 8. Authorization: the gate that's usually wrong

You're authenticated. Now: *are you allowed?* This is **Broken Object Level Authorization** territory — OWASP API Security #1 — and it's where most real breaches happen. The bug looks like this:

```java
// WRONG. Authenticated, but not authorized.
@GetMapping("/habits/{id}")
HabitResponse get(@PathVariable Long id) {
    return habitRepository.findById(id)           // any id, anyone's habit!
            .map(HabitResponse::from)
            .orElseThrow(NotFoundException::new);
}
```

User 42 sends a valid token and asks for habit 9999, which belongs to user 99. The token check *passes* — user 42 is authenticated. But nothing checks that habit 9999 belongs to user 42. **Boom.** Every secured app has shipped this bug at least once.

There are two layers of defense; use both.

### Layer 1 — scope every query by owner (your real defense)

Don't query by id and *then* check ownership. Query by id **and** owner together, so a mismatch simply returns nothing:

```java
public interface HabitRepository extends JpaRepository<Habit, Long> {
    Optional<Habit> findByIdAndOwnerId(Long id, Long ownerId);
    Page<Habit> findAllByOwnerId(Long ownerId, Pageable pageable);
}
```

```java
HabitResponse getForOwner(Long ownerId, Long habitId) {
    return habitRepository.findByIdAndOwnerId(habitId, ownerId)
            .map(HabitResponse::from)
            .orElseThrow(() -> new HabitNotFoundException(habitId));   // 404 or 403
}
```

This is the strongest defense because it's structural: there's no code path that returns another user's habit, because the query *can't* select it. Even if a controller forgets a check, the repository physically won't hand over the wrong row.

### Layer 2 — `@PreAuthorize` ownership rule (defense in depth + intent)

Method security adds a declarative gate that documents the rule and catches mistakes the query scoping might miss. Write a small bean that answers "does the caller own this?":

```java
@Component("owns")
public class OwnershipEvaluator {

    private final HabitRepository habits;

    public OwnershipEvaluator(HabitRepository habits) { this.habits = habits; }

    public boolean habit(Long habitId) {
        AppUser caller = (AppUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return habits.findById(habitId)
                .map(h -> h.ownerId().equals(caller.id()))
                .orElse(false);
    }
}
```

Now annotate the method. If `owns.habit(#id)` returns false, Spring throws `AccessDeniedException`, which your handler turns into a `403`:

```java
@PreAuthorize("@owns.habit(#id)")
@DeleteMapping("/habits/{id}")
ResponseEntity<Void> delete(@PathVariable Long id) {
    habitService.delete(id);
    return ResponseEntity.noContent().build();
}
```

The `@owns` refers to the bean named `"owns"`; `#id` is the method's `id` parameter. The expression runs **before** the method body, so an unauthorized caller never reaches `delete`.

> **Why both layers?** Query scoping is your guarantee; `@PreAuthorize` is your readable, testable statement of intent and a backstop for endpoints that don't go through the scoped query. Belt and suspenders. This week's **challenge** is to implement the `@PreAuthorize` rule and write the integration tests that prove the `403` fires for the wrong owner.

---

## 9. Proving it with tests

A passing happy path is not security. You must test the *unhappy* paths. With `@SpringBootTest` + `MockMvc` (or `WebTestClient`), assert the status codes:

```java
@Test
void user_a_cannot_read_user_b_habit() throws Exception {
    Long habitOfB = seedHabitOwnedBy(userB);
    String tokenA = jwt.issue(userA);

    mockMvc.perform(get("/api/v1/habits/{id}", habitOfB)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
           .andExpect(status().isForbidden());        // 403, NOT 200, NOT a leak
}

@Test
void no_token_is_unauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/habits"))
           .andExpect(status().isUnauthorized());     // 401
}

@Test
void expired_token_is_unauthorized() throws Exception {
    String expired = jwt.issueWithTtl(userA, Duration.ofSeconds(-1));
    mockMvc.perform(get("/api/v1/habits")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
           .andExpect(status().isUnauthorized());     // 401
}

@Test
void tampered_token_is_unauthorized() throws Exception {
    String good = jwt.issue(userA);
    String tampered = good.substring(0, good.length() - 3) + "xxx";  // break the signature
    mockMvc.perform(get("/api/v1/habits")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
           .andExpect(status().isUnauthorized());     // 401
}
```

These four tests — own data works, no token `401`, expired `401`, wrong owner `403` — are the minimum that earns the "SECURITY: ..." line from the README. The challenge and mini-project both require them. **Write the security tests, then watch them fail before they pass** — the only way to trust a `403` is to have seen the endpoint return something else first.

---

## 10. Common failure modes (read before you debug at 2am)

- **Everything returns `403` after login.** Usually the filter isn't populating the `SecurityContext` (check `addFilterBefore` order and that `validateAndGetUserId` isn't silently returning empty due to a clock-skew or secret mismatch).
- **`401` on the login endpoint itself.** You forgot `permitAll()` for `/api/v1/auth/**`, or the path prefix differs from what the matcher expects (`/auth/**` vs `/api/v1/auth/**`).
- **CORS error in the browser but `curl` works.** The API is fine; the CORS config is missing the origin or a header. Re-read Lecture 1 §9.
- **`The signing key's size is not secure enough`** at startup. Your `crunch.jwt.secret` is shorter than 256 bits for HS256. Generate a real one: `openssl rand -base64 64`.
- **Tokens work locally, fail in CI.** Different secret between environments. The secret is config, not code; make sure CI sets `CRUNCH_JWT_SECRET`.
- **Password matches fail for a user you just created.** You stored the raw password (or hashed twice). Hash exactly once, at registration; match (never re-hash) at login.

---

## 11. Recap

You should now be able to:

- Explain why a mobile-first API goes stateless, and the revocation tradeoff that costs.
- Describe a JWT's three parts and which claims you use; explain that it's signed, not encrypted.
- Hash passwords with BCrypt via `PasswordEncoder`, and say why SHA-256 is the wrong tool.
- Write a `JwtService` that issues and validates tokens, with the secret loaded from config.
- Write a `OncePerRequestFilter` that turns a bearer header into an authenticated principal.
- Configure the Spring Security 6 `SecurityFilterChain` for stateless JWT auth — CSRF off, sessions stateless, `/auth/**` open, everything else authenticated.
- Build register and login endpoints that return tokens with correct status codes.
- Enforce ownership with **both** owner-scoped queries and `@PreAuthorize`, and explain why both.
- Write the integration tests that prove `401` on no/expired/tampered token and `403` on the wrong owner.

Next, do the exercises — design the v1 surface, build the JWT service, and wire the filter chain — then take on the challenge and the mini-project.

---

## References

- *Spring Security — Architecture*: <https://docs.spring.io/spring-security/reference/servlet/architecture.html>
- *Spring Security — Method Security (`@PreAuthorize`)*: <https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html>
- *Spring Security — Password storage / `PasswordEncoder`*: <https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>
- *RFC 7519 — JSON Web Token*: <https://www.rfc-editor.org/rfc/rfc7519.html>
- *jjwt (Java JWT) library*: <https://github.com/jwtk/jjwt>
- *OWASP API #1 — Broken Object Level Authorization*: <https://owasp.org/API-Security/editions/2023/en/0xa1-broken-object-level-authorization/>
- *Spring Security 6 migration guide*: <https://docs.spring.io/spring-security/reference/migration/index.html>
