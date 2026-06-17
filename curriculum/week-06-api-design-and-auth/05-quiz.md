# Week 6 — Quiz

Ten multiple-choice questions. Take it with your lecture notes closed. Aim for 9/10 before moving to Week 7. Answer key at the bottom — don't peek.

---

**Q1.** A request arrives with a valid JWT for user 42, asking to read habit 9999 which belongs to user 99. What status code should the API return (using the C3 default posture)?

- A) `200 OK` — the token is valid, so the request is authorized.
- B) `401 Unauthorized` — the caller isn't allowed to see this resource.
- C) `403 Forbidden` — the caller is authenticated but not authorized for this resource.
- D) `500 Internal Server Error` — cross-user access is a server fault.

---

**Q2.** Which statement about a JWT is correct?

- A) A JWT is encrypted, so the claims inside it are secret from anyone who holds the token.
- B) A JWT is signed but not encrypted; anyone holding it can read the claims, but the signature proves it wasn't tampered with.
- C) A JWT stores the user's password in the payload so the server can re-verify it on each request.
- D) A JWT must be looked up in a server-side session store to be validated.

---

**Q3.** Why is `BCrypt` the right tool for password storage and a plain `SHA-256(password)` the wrong one?

- A) SHA-256 produces shorter hashes, which take more storage.
- B) BCrypt is deliberately slow and salted with a tunable work factor; SHA-256 is fast and unsalted, making it cheap to brute-force.
- C) SHA-256 is reversible, so an attacker can decrypt the password.
- D) BCrypt encrypts the password so it can be decrypted at login.

---

**Q4.** In a Spring Security 6 stateless JWT API, why do we set `SessionCreationPolicy.STATELESS` and disable CSRF?

- A) Because CSRF protection slows down the filter chain measurably.
- B) Because we authenticate each request from its bearer token, store no server-side session, and the browser never auto-sends the token — so there's no session to protect and no CSRF vector.
- C) Because Spring Security 6 removed both features.
- D) Because stateless APIs cannot use HTTPS.

---

**Q5.** Which HTTP method is **not** idempotent by contract?

- A) `PUT`
- B) `DELETE`
- C) `GET`
- D) `POST`

---

**Q6.** Your `GET /api/v1/habits?sort=passwordHash,asc` returns a `500` with a Hibernate exception. What's the correct fix?

- A) Catch the exception and return `200` with an empty list.
- B) Whitelist the sortable fields and return `400 ProblemDetail` for anything not on the list.
- C) Add `passwordHash` to the list of sortable columns.
- D) Disable sorting on the endpoint entirely.

---

**Q7.** A teammate writes this controller method. What's the security bug?

```java
@GetMapping("/habits/{id}")
HabitResponse get(@PathVariable Long id) {
    return habitRepository.findById(id).map(HabitResponse::from).orElseThrow();
}
```

- A) It should use `@PostMapping`, not `@GetMapping`.
- B) It fetches by id alone with no owner check, so any authenticated user can read any user's habit (BOLA).
- C) It will throw a `NullPointerException` because `findById` can return null.
- D) Nothing — the security filter chain already prevents cross-user access automatically.

---

**Q8.** Your API runs at `https://api.crunch.dev`. The Expo dev server (a browser context at `http://localhost:8081`) gets a CORS error, but the same request from `curl` works fine. What's true?

- A) The API is broken; fix the controller.
- B) CORS is a browser mechanism; the API works, but its `Access-Control-Allow-Origin` headers don't include `http://localhost:8081`. Add that origin to the CORS config.
- C) `curl` is bypassing authentication, which is why it works.
- D) You must disable CORS entirely to let the browser through.

---

**Q9.** At login, a user submits an email that isn't registered. To avoid leaking which emails have accounts, what should the API return?

- A) `404 Not Found` with `"No account for that email"`.
- B) `200 OK` with `{ "exists": false }`.
- C) The same `401` and message as a wrong password — e.g. `401 "Invalid email or password"`.
- D) `409 Conflict`, since the account is missing.

---

**Q10.** Why does the C3 design put the owner in the authenticated principal (`@AuthenticationPrincipal AppUser caller`) rather than in the URL path (`/users/{userId}/habits`)?

- A) Because path variables are slower to parse than headers.
- B) Because a user id in the path invites a caller to pass *someone else's* id, exactly the cross-user access bug we're preventing — the owner must come from the verified token, not untrusted input.
- C) Because Spring can't bind path variables to entities.
- D) Because REST forbids path variables.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **C** — `403 Forbidden`. The token is valid (authenticated), so it's not `401`; the caller simply isn't allowed to touch user 99's resource (not authorized). The `404`-instead-of-`403` "hide existence" posture is a defensible alternative, but the C3 default is `403`.
2. **B** — A JWT is signed, not encrypted. The payload is base64url and readable by anyone (try jwt.io). The signature guarantees integrity and authenticity, not confidentiality. Never put secrets in it.
3. **B** — BCrypt is adaptive: a built-in salt defeats rainbow tables, and a tunable work factor makes each hash slow enough that brute-forcing billions becomes infeasible. SHA-256 is fast and unsalted — the opposite of what you want for passwords.
4. **B** — Stateless means each request re-authenticates from its token with no server session. CSRF protects cookie/session apps from forged cross-site submits; since the browser doesn't auto-attach a bearer token, there's no CSRF vector, so disabling it is correct, not lazy.
5. **D** — `POST` is not idempotent: retrying it creates duplicate resources. `PUT`, `DELETE`, and `GET` are all idempotent by contract (`GET` is also safe). This is why a retried `PO/check-in` needs an `Idempotency-Key`.
6. **B** — Never pass an untrusted string to the query builder as a column name. Whitelist sortable fields and return a `400 ProblemDetail` for anything else. Adding `passwordHash` (C) would *leak hashes via sorting/serialization* and is exactly wrong.
7. **B** — Broken Object Level Authorization. `findById(id)` with no owner filter lets any authenticated user read any habit by guessing ids. Fix: `findByIdAndOwnerId(id, caller.id())`. The filter chain authenticates but does **not** enforce per-object ownership — that's your job.
8. **B** — CORS is enforced by the browser, not the server. `curl` ignores it. The fix is to add `http://localhost:8081` to `allowedOrigins`. The API itself is fine.
9. **C** — Return the same `401` and message for "unknown email" and "wrong password." Distinguishing them lets an attacker enumerate which emails are registered. Collapse both into one `BadCredentialsException`.
10. **B** — The owner must come from the verified token (trusted) rather than the URL (attacker-controlled). A `userId` in the path is an open invitation to pass someone else's id — the precise BOLA bug. The principal is the source of truth for "whose data."

</details>

---

If you scored under 7, re-read the lectures for the questions you missed — especially anything about `401` vs `403` and BOLA. If you scored 9 or 10, you're ready for the [homework](./06-homework.md).
