# Week 6 Homework

Six practice problems that revisit the week's topics. The full set should take about **5 hours** in total. Work in your week-6 Crunch Tracker repository so each problem produces at least one commit you can point to later.

Each problem includes:

- A short **problem statement**.
- **Acceptance criteria** so you know when you're done.
- A **hint** if you get stuck.
- An **estimated time**.

---

## Problem 1 — Decode a token by hand

**Problem statement.** Mint a token from your running API (register + login). Take the token and, **without** any JWT library, decode the header and payload yourself and write them into `notes/jwt-anatomy.md`. Then answer: which claim is the user id? What is the `exp` value as a human-readable time? Is the signature readable as JSON, and why not?

```bash
# split on the dots; the first two parts are base64url JSON
echo "$TOKEN" | cut -d. -f1 | base64 -d 2>/dev/null    # header
echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null    # payload
```

**Acceptance criteria.**

- `notes/jwt-anatomy.md` shows the decoded header and payload and answers all three questions.
- You correctly identify `sub` as the user id and convert `exp` (Unix seconds) to a readable time.
- You explain that the third part is an HMAC (raw bytes), not JSON.
- Committed.

**Hint.** base64url uses `-` and `_` instead of `+` and `/` and may need padding; if `base64 -d` complains, that's the padding. The payload is plain JSON once decoded — proof that a JWT is signed, not encrypted.

**Estimated time.** 25 minutes.

---

## Problem 2 — One `401` for both login failures

**Problem statement.** Audit your `login` endpoint. Confirm that submitting (a) an unknown email and (b) a known email with the wrong password both return the **same** status and **same** message. Write a `@SpringBootTest` test, `login_does_not_leak_which_emails_exist`, that asserts both cases return `401` with identical bodies.

**Acceptance criteria.**

- A test that exercises both failure cases and asserts identical `401` responses.
- If your code currently distinguishes them, fix it (collapse into one `BadCredentialsException`).
- `./mvnw test` green.
- Committed.

**Hint.** `users.findByEmail(email).filter(u -> encoder.matches(pw, u.passwordHash())).orElseThrow(() -> new BadCredentialsException("Invalid email or password"))` — both the missing-user and wrong-password branches fall through to the same exception.

**Estimated time.** 40 minutes.

---

## Problem 3 — Prove the wrong owner gets blocked

**Problem statement.** Write an integration test `user_a_cannot_read_user_b_habit`: seed two users and a habit owned by B, mint A's token, request B's habit as A, and assert the rejected status (`403` or `404` per your documented choice). Then **temporarily** revert the owner-scoped query to a plain `findById`, re-run the test, observe it now returns `200` with B's data, write one sentence in `notes/bola.md` describing what you saw, and revert.

**Acceptance criteria.**

- The test exists and passes with scoping in place.
- `notes/bola.md` describes the failure you observed when scoping was removed (the actual leaked status/body).
- The scoping is back in place and the test is green.
- Committed (the note, not the temporary break).

**Hint.** This is the heart of the week. The only way to trust a `403` is to have seen the endpoint return the wrong thing first. Use `findByIdAndOwnerId(id, caller.id())` for the real version.

**Estimated time.** 50 minutes.

---

## Problem 4 — Paginate and cap a list endpoint

**Problem statement.** Pick a list endpoint (`GET /api/v1/check-ins` is a good one). Make it return a `Page<T>`, default page size 20, cap at 100, default sort `performedAt,desc`, scoped to the caller. Write tests proving: (1) the response is a `Page` shape, (2) `size=1000000` is capped, (3) only the caller's check-ins appear (seed both users).

**Acceptance criteria.**

- The endpoint returns `Page<CheckInResponse>` and is owner-scoped.
- A test asserts the page-size cap (the returned `size`/`numberOfElements` is not a million).
- A test asserts cross-user isolation on the *list* endpoint (BOLA hides here too).
- `./mvnw test` green.
- Committed.

**Hint.** Set `spring.data.web.pageable.max-page-size: 100`. The cross-user list test is the easy one to forget — the single-resource endpoint may be secured while the list quietly returns everyone's rows.

**Estimated time.** 1 hour.

---

## Problem 5 — Configure CORS for the mobile client

**Problem statement.** Add a `CorsConfigurationSource` bean allowing `http://localhost:8081` (Expo dev) and one placeholder web origin, methods `GET/POST/PUT/PATCH/DELETE/OPTIONS`, headers `Authorization`/`Content-Type`/`Idempotency-Key`, credentials enabled. Then write `notes/cors.md` answering: why does the mobile app's *native* HTTP client not trigger CORS, but the Expo dev server does? And why does Spring refuse `allowedOrigins("*")` together with `allowCredentials(true)`?

**Acceptance criteria.**

- The CORS bean exists, wired into the filter chain, with explicit origins (no wildcard).
- A preflight `OPTIONS /api/v1/habits` from an allowed origin returns the `Access-Control-Allow-*` headers (test with `curl -X OPTIONS -H "Origin: http://localhost:8081" -H "Access-Control-Request-Method: GET" -i`).
- `notes/cors.md` answers both questions correctly.
- Committed.

**Hint.** CORS is a *browser* mechanism. Native mobile HTTP clients aren't browsers, so they ignore it. The wildcard-with-credentials combination is forbidden by the CORS spec because it would let any site make authenticated requests on a user's behalf.

**Estimated time.** 45 minutes.

---

## Problem 6 — Threat-model write-up

**Problem statement.** Write a 350–450 word note at `notes/threat-model.md` answering:

1. Map three items from the OWASP API Security Top 10 (2023) to specific places in your Crunch Tracker code. For each, name the file/method and how your code addresses (or still fails to address) it.
2. Your access tokens are stateless and live 15 minutes. Describe what happens if one is stolen, and what your refresh-token-or-not decision costs in that scenario.
3. If you had to add "Sign in with Google" next month, which single component from this week is the seam it would plug into, and roughly how?

**Acceptance criteria.**

- File exists, 350–450 words, each numbered question in its own section.
- The OWASP mappings cite real files/methods in your repo (not generic descriptions).
- File is committed.

**Hint.** Start with BOLA (#1) — you already have the code and the test. Broken Authentication (#2) is your login/JWT flow. "Unrestricted Resource Consumption" (#4) is your pagination cap. The OIDC seam is your filter chain: a Google token would be validated by a different provider but populate the same `SecurityContext`.

**Estimated time.** 30 minutes.

---

## Grading rubric

This homework is graded out of **100 points**.

| Problem | Points | What earns full marks |
|--------:|-------:|-----------------------|
| 1 — Decode a token | 12 | Header + payload decoded correctly; `sub`/`exp` identified; signature-isn't-JSON explained |
| 2 — One `401` | 16 | Identical `401` for both failures, proven by a passing test; any leak fixed |
| 3 — Prove BOLA blocked | 22 | Wrong-owner test passes; note documents the *observed* leak when scoping was removed; scoping restored |
| 4 — Paginate + cap | 18 | `Page<T>` shape, size cap tested, cross-user list isolation tested, owner-scoped |
| 5 — CORS | 16 | Explicit-origin CORS bean, preflight headers verified, both conceptual questions answered correctly |
| 6 — Threat model | 16 | 350–450 words; OWASP items mapped to *real* files/methods; token-theft and OIDC-seam answered |

**Point deductions (apply across the whole submission):**

- **−15** if any password is stored in plaintext, logged, or present in a response DTO anywhere in the repo.
- **−10** if the JWT signing secret is hard-coded/committed instead of read from config.
- **−10** if `./mvnw test` is not green on a fresh clone.
- **−5** per problem whose required note/file is missing or under-length.

**Scoring guide:**

- **90–100:** Production-shop quality. Security negative paths are tested, secrets are external, nothing leaks.
- **75–89:** Solid. Minor gaps in tests or write-ups, but the auth and scoping are correct.
- **60–74:** Passing. The happy path works but the negative paths are thin or a note is missing.
- **Below 60:** Revisit Lecture 2 (filter chain, BCrypt, ownership) and the challenge before moving on. Shipping un-scoped queries to week 7 will hurt.

---

## Time budget recap

| Problem | Estimated time |
|--------:|--------------:|
| 1 | 25 min |
| 2 | 40 min |
| 3 | 50 min |
| 4 | 1 h 0 min |
| 5 | 45 min |
| 6 | 30 min |
| **Total** | **~4 h 30 min** |

When you've finished all six, push your repo and make sure the [mini-project](./07-mini-project/00-overview.md) is on track — much of this homework feeds straight into it.
