# Week 5 — Challenges

The exercises drill the basics. **Challenges stretch you.** Each one takes 60–120 minutes and produces something you can commit to your portfolio and talk about in an interview.

## Index

1. **[Challenge 1 — Hunt the N+1](./challenge-01-hunt-the-n-plus-1.md)** — you're handed a deliberately slow endpoint. Use logged SQL to diagnose an N+1, fix it without breaking the API contract, and *prove* with a query-count assertion that the count dropped. (~90 min)

## How challenges are assessed

Challenges are optional — you can pass the week without them — but they are where the real signal is. A graded challenge submission is assessed on four axes:

| Axis | What we look for |
|------|------------------|
| **Diagnosis** | You located the N+1 from evidence (logged SQL, query counts), not by guessing. You can state how many queries fired before and why. |
| **Fix correctness** | The fix is the *right* tool (`JOIN FETCH` / `@EntityGraph` / batch fetch) for the situation, and the **API response is byte-for-byte unchanged** — same JSON, same status, same pagination. |
| **Proof** | A test asserts the query count fell (Hibernate `Statistics` or an assertion library). "It feels faster" is not proof. |
| **No regressions** | The full suite — unit + Testcontainers integration — is still green. You didn't fix N+1 by breaking laziness elsewhere or by eager-loading the world. |

The pattern you practice here — *observe, count, fix, re-count, prove* — is exactly what you'll do on a real team the first time a "the dashboard is slow" ticket lands. The N+1 you fix here reappears the moment Week 6 adds per-user filtering and Week 9 has the mobile app hammering the list endpoint. Learn the loop now.

## Submission

Commit your work under `challenges/challenge-01/` in your Week 5 repo. The branch must contain:

- The before/after SQL logs (paste them into the challenge's `FINDINGS.md`).
- The fix, as a focused diff.
- A passing query-count test.
- A one-paragraph writeup: what the count was, what it is now, which fix you chose and why.

Make sure `./mvnw test` is green on a fresh clone with `docker compose up -d` running.
