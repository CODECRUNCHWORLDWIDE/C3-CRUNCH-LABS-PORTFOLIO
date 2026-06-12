# Week 4 — Challenges

The exercises drill basics. **Challenges stretch you.** Each one takes 90–150 minutes and produces something you can commit to your portfolio.

## Index

1. **[Challenge 1 — A paginated, filterable, sortable list endpoint](challenge-01-paginated-list-endpoint.md)** — implement `GET /api/goals` to a real spec: pagination, multi-field filtering, validated sorting, and a structured `400` (never a stack trace) when the query is malformed. (~120 min)

## How challenges are assessed

Challenges are optional — you can pass the week without them — but doing them puts you measurably ahead. Each challenge is assessed on four axes:

| Axis | What we look for |
|------|------------------|
| **Correctness** | The endpoint behaves exactly as the spec says for every listed case, including the unhappy ones. |
| **Contract honesty** | Bad input returns a `400` `ProblemDetail`, not a `500` and not a silent default. The response envelope is consistent and documented. |
| **Test coverage** | `@WebMvcTest` slices that prove each behaviour, including every validation failure. A behaviour without a test doesn't count. |
| **Code quality** | Thin controller, validation at the edge, no business logic leaking into the web layer, no `!`-style "trust me" shortcuts. |

A challenge is "done" when a peer can clone your repo, run `./mvnw test` green, start the app, and reproduce every example in the spec with `curl`.

## Why this one matters

The paginated/filterable/sortable list endpoint is the single most common real-world REST endpoint there is — every list screen in every app is backed by one. The pattern you build here (validated query params → a typed query object → a consistent paged response envelope) reappears directly in:

- **Week 5**, where the same endpoint gets backed by Spring Data's `Pageable` and a JPA repository — and your contract must not change.
- **Week 6**, where the list is scoped to the authenticated user's own data.
- **Week 9**, where the React Native app consumes the paged envelope with TanStack Query's infinite-scroll.

Get the envelope and the validation right now, once, and three later weeks inherit it for free.
