# Week 9 — Challenges

The exercises drill the pieces in isolation. **Challenges stretch you** by putting them under pressure — the kind a real network applies. Each takes 90–150 minutes and produces something you can commit to your portfolio.

## Index

1. **[Challenge 1 — Offline-resilient optimistic check-in](./challenge-01-offline-optimistic-checkin.md)** — extend the optimistic check-in so it survives airplane mode, queues while offline, resumes on reconnect, and recovers cleanly from a mid-session token expiry. (~120 min)

## How it's assessed

Challenges are optional. If you skip them, you can still pass the week. If you do them, you'll be measurably ahead — and the offline-and-reconnect pattern here is exactly what separates a demo app from one a person would actually use on a subway.

Assessment is hands-on, not multiple-choice. A reviewer (a peer, a mentor, or you against the rubric) will:

1. **Run it.** Clean clone, `npm install`, point at a live backend, log in. The happy path must work without a tour.
2. **Pull the plug.** Toggle airplane mode mid-check-in and watch what the UI does. The challenge passes only if the optimistic state holds, the mutation pauses, and it resumes and reconciles on reconnect — with no lost taps and no duplicate check-ins.
3. **Read the types.** Hover any variable in the mutation. If anything is `any`, or `context` is untyped, it fails the type bar.
4. **Read the diff.** The optimistic logic must be one cohesive hook, not check-in code smeared across a screen component.

The detailed rubric lives in the challenge file. The headline: **it has to actually work offline**, demonstrated live, not described in a README.

## Why this matters

Optimistic-plus-offline is the pattern behind every good mobile app you've used — the message that appears instantly and sends when you're back in signal, the like that registers before the request lands. TanStack Query gives you the machinery (`onlineManager`, paused mutations, `onMutate`/`onError`/`onSettled`); the skill is wiring it so the user never sees a lie they can't recover from. Nail it here and the week-10 capstone demo writes itself.
