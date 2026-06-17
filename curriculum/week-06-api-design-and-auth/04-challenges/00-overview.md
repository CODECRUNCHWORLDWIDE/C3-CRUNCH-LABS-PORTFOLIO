# Week 6 — Challenges

The exercises drill the pieces. **Challenges stretch you** and produce the part of the week that actually keeps users' data safe. Each one takes 60–120 minutes and produces something you commit to your portfolio.

## Index

1. **[Challenge 1 — Ownership authorization that you can prove](./challenge-01-ownership-authorization.md)** — implement a `@PreAuthorize` ownership rule across goals, habits, and check-ins, layer it on top of owner-scoped queries, and write the integration tests that prove user A gets a `403` (not a `404`, not a leak) reaching for user B's data. (~110 min)

## How challenges are assessed

Challenges are optional for *passing* the week, but this one is the spine of the mini-project — if you skip it you'll write it anyway, just without the scaffolding. It is assessed on four things, in priority order:

1. **The negative path is tested.** The single most important deliverable is an integration test that *fails* if ownership enforcement is removed. A green happy-path suite earns nothing here; we want to see the `403` asserted, and we want to see (in a comment or a deliberate experiment) that you watched the endpoint return something *wrong* before you fixed it.
2. **Defense is structural, not cosmetic.** Ownership is enforced at the repository (owner-scoped queries) *and* declared with `@PreAuthorize`. A check that lives only in the controller and can be bypassed by another endpoint does not count.
3. **No information leak.** The wrong-owner response must not reveal the contents — or, where you've chosen the "hide existence" posture, even the existence — of another user's resource. You make the `403`-vs-`404` choice on purpose and document it.
4. **It still builds clean.** `./mvnw verify` is green, including the Testcontainers integration tests from week 5.

There is no partial credit for "it works when I tried it in Postman." Security that isn't tested isn't security; it's a hope. The challenge exists to turn the hope into a test.
