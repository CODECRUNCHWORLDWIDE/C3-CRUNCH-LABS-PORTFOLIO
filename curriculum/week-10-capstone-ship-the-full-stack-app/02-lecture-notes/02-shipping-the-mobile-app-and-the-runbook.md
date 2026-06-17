# Lecture 2 — Shipping the Mobile App: EAS Builds, Release Config, the Demo, and the Runbook

> **Reading time:** ~70 minutes. **Hands-on time:** ~50 minutes (you produce an installable EAS build pointed at your live API and draft your runbook).

Lecture 1 put the API on the internet. This lecture puts the *app* in someone's hand. So far your Crunch Tracker mobile client has lived inside the Expo dev server — `npx expo start`, scan a QR code, run it through Expo Go on your own phone, pointed at your laptop's IP. That is a development loop. It is not a product. A product is a binary a grader, a friend, or a hiring manager can install on a phone they own and use against your live API without your laptop being on the same Wi-Fi — or on at all. By the end of this lecture you can produce that binary with EAS, configure it so the shipped app talks to your deployed API instead of `localhost`, run the demo that proves the whole stack works, and write the runbook that lets another engineer deploy the entire system from cold.

This lecture also closes the loop on the architecture review. Lecture 1 gave you the deploy to defend; this one gives you the *runbook* — the artifact that answers the most operational question a reviewer asks: "it's 3am, it's down, what's the first thing you look at?" If you cannot answer that in one sentence, your observability is decorative. By the end you'll have the sentence.

## 2.1 — Why Expo Go isn't shipping, and what EAS does

Expo Go is a host app: it loads *your* JavaScript at runtime over the network. That's wonderful for development and useless for shipping, for three reasons. It requires the dev server to be reachable. It can't include custom native code. And it isn't *your* app — it's Expo's app running your code, with Expo's icon, that you can't hand someone as "Crunch Tracker."

**EAS Build** is the answer. It compiles your project into a real, standalone binary in the cloud — an Android `.apk`/`.aab` or an iOS `.ipa` — without you owning an Android SDK or an Xcode toolchain locally. The output is a file (or an install link) that runs on its own, with your icon and your name, talking to whatever API URL you baked in. That last clause is the whole game: a standalone binary has *no* dev server to ask for the API URL, so the URL has to be decided at **build time** and frozen into the binary. Get that wrong and you ship an app that points at `http://localhost:8080` — which, on a phone, means the phone itself, where there is no API. This is the single most common capstone failure, and §2.3 is entirely about avoiding it.

## 2.2 — `eas.json`: build profiles

EAS configures builds through `eas.json` at the project root. A **build profile** is a named set of build settings; you'll use three:

```json
{
  "cli": { "version": ">= 12.0.0", "appVersionSource": "remote" },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "env": { "EXPO_PUBLIC_API_URL": "http://192.168.1.20:8080" }
    },
    "preview": {
      "distribution": "internal",
      "android": { "buildType": "apk" },
      "env": { "EXPO_PUBLIC_API_URL": "https://crunch-tracker-api.onrender.com" }
    },
    "production": {
      "distribution": "store",
      "autoIncrement": true,
      "env": { "EXPO_PUBLIC_API_URL": "https://crunch-tracker-api.onrender.com" }
    }
  },
  "submit": { "production": {} }
}
```

What each profile is for:

- **`development`** — a build with the dev client for fast iteration on real hardware; points at your laptop's LAN IP. You won't demo this.
- **`preview`** — `distribution: internal` and `buildType: apk`. This is **the capstone build**: an installable `.apk` (or an iOS internal-distribution link) pointed at your *live* API, that anyone can install without going through an app store. This is what the grader installs.
- **`production`** — `distribution: store`, for an actual App Store / Play Store submission. Out of scope for the capstone, but worth having so the path is real.

The line that matters most is the `env.EXPO_PUBLIC_API_URL` in `preview` and `production`: it is your deployed API hostname from Lecture 1. The `EXPO_PUBLIC_` prefix is significant — Expo inlines those variables into the JS bundle at build time, so the value is *in the binary*, available with no network call. Build with `eas build --profile preview --platform android`, and the resulting `.apk` is hard-wired to your live backend.

## 2.3 — One config seam: the API URL the app reads

The deploy-killer is a base URL hard-coded in five different fetch calls. The fix is the same discipline Lecture 1 applied on the backend: **the API URL is read from one place, and that place reads it from build-time config.** Week 09 should already have a single `apiClient` — this week you make sure its base URL comes from config, not from a literal.

Exercise 3 is the complete, typed config module. The shape:

```typescript
// config/env.ts
import Constants from "expo-constants";

function resolveApiUrl(): string {
  // EXPO_PUBLIC_API_URL is inlined at build time by EAS (2.2). In the dev
  // server it comes from the shell / eas.json env; in a standalone build it is
  // frozen into the bundle. We also read it back from expo-constants `extra`
  // as a fallback so a misconfigured build fails loudly, not silently.
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  const fromExtra = (Constants.expoConfig?.extra as { apiUrl?: string } | undefined)?.apiUrl;
  const url = fromEnv ?? fromExtra;
  if (!url) {
    throw new Error(
      "API URL is not configured. Set EXPO_PUBLIC_API_URL in eas.json for this build profile.",
    );
  }
  return url.replace(/\/+$/, ""); // normalize trailing slash
}

export const API_URL = resolveApiUrl();
```

Then the one and only `apiClient` consumes it:

```typescript
// api/client.ts
import { API_URL } from "../config/env";
import { useSession } from "../store/session";

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = useSession.getState().token;
  const res = await fetch(`${API_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });
  if (!res.ok) {
    throw new ApiError(res.status, await res.text());
  }
  return (await res.json()) as T;
}
```

The win: there is exactly one string in the whole app that knows the backend's address, and it comes from build config. To point the app at a different backend — staging, a teammate's deploy, a fresh prod URL — you change one `env` value in `eas.json` and rebuild. Nothing in the screens or the TanStack Query hooks changes. A reviewer who asks "how does the shipped app know where the API is?" gets a clean answer: "one config seam, set per build profile, frozen into the binary at build time, and it throws on startup if it's missing so a misconfigured build can't silently point at nothing."

## 2.4 — Token storage in a shipped build

One thing that's been quietly fine in development can bite in a shipped build: where the JWT lives. In Week 08 you put it in `expo-secure-store`, which is correct — it's the platform keychain/keystore, encrypted at rest. Confirm it still is in the production build, because a token in plain `AsyncStorage` is readable by anything that can get at the app's storage. The session store loads the token from secure storage on launch and clears it on logout:

```typescript
// store/session.ts (the persistence seam)
import * as SecureStore from "expo-secure-store";

const TOKEN_KEY = "crunch.jwt";

export async function persistToken(token: string) {
  await SecureStore.setItemAsync(TOKEN_KEY, token);
}
export async function loadToken(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}
export async function clearToken() {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
}
```

The reviewer question here is "where does the auth token live on the device, and what happens to it on logout?" The answer: "the platform secure store — Keychain on iOS, Keystore-backed encrypted prefs on Android — and logout deletes it." That, plus "the token is a short-lived JWT the API validates with a secret that isn't in the app," is the mobile-security story for the capstone.

## 2.5 — Building and distributing the artifact

With `eas.json` set and the config seam in place, the build is two commands:

```bash
npm install -g eas-cli
eas login                 # one-time, free Expo account

# Build the installable APK pointed at the live API.
eas build --profile preview --platform android
```

EAS runs the build in the cloud (a few minutes) and prints an install link plus a QR code. That link *is* your demo artifact: open it on an Android phone, install the `.apk`, and the app runs standalone against your deployed backend. For iOS, internal distribution requires registering the device's UDID (Apple's rule, not Expo's), so for a grader the Android `.apk` is the path of least friction — note in your README that the install link is the Android build and iOS is available on request.

Put the install link in your README and your runbook. The grader's flow is: open the link, install, sign in, create a habit, check in. If that works against your live API, the capstone's core slice is proven. Test it yourself first on a phone that has *never* been on your dev network — that's the only way to be sure you didn't accidentally ship a `localhost` URL or rely on your laptop being reachable.

## 2.6 — The demo: trace one check-in end to end

The single most effective thing you do in the review — and the core of your 5-minute video — is trace one real action through the live system, on screen. Not a slide of the architecture. The actual app, the actual API, the actual database row. Here is the walk, and the commands you run live to prove each hop landed.

**On the phone:** open the installed app, sign in as a real user, tap a habit, tap "Check in." The UI updates instantly (your Week 09 optimistic update). Narrate: "this tap fires a `POST /api/habits/42/check-ins` with the user's JWT in the Authorization header, to my live API on Render."

**At the edge / API:** show the request arrived. Tail the deployed API's structured logs (the host's log view, or `curl` the health endpoint to show it's the live one):

```bash
curl -s https://crunch-tracker-api.onrender.com/actuator/health
# {"status":"UP"}  -- this is the live, deployed API, not localhost
```

**In the database:** the proof that closes the loop. Connect to managed Postgres and show the row that the tap created:

```bash
psql "$DATABASE_URL" -c \
  "SELECT id, habit_id, user_id, checked_at
     FROM check_ins
    ORDER BY id DESC
    LIMIT 1;"
```

When the row's `checked_at` matches the moment you tapped — and the `user_id` matches the signed-in user, proving your Week 06 ownership scoping held — you've demonstrated the entire stack in one motion: phone → HTTPS → deployed Spring Boot → JPA → managed Postgres, authenticated and scoped to the user. That is more convincing than any diagram. A reviewer who sees the tap produce a real row stops wondering whether the system works and starts asking the *interesting* questions (what breaks, what's the SPOF) — which is exactly where you want the conversation.

**Rehearse this walk three times before Friday.** Networks fail during demos; have a screen recording of a successful trace ready to play if the live system hiccups. "The live demo is having a moment, here's a recording of the same walk from an hour ago" is completely acceptable and far better than freezing. The 5-minute video is essentially this walk, edited; rehearse the live version well and the recording is a thirty-minute session, not a thirty-take ordeal.

## 2.7 — The runbook: deploy from cold

The runbook is the artifact that turns "only I can deploy this" into "anyone can deploy this." It is graded, and it is what a reviewer means when they ask whether you've operated a system. A good `RUNBOOK.md` answers four questions, concretely:

1. **What does a cold deploy need?** Every environment variable, every account, every one-time setup step.
2. **How do I deploy?** The exact command or pipeline trigger, start to finish.
3. **How do I roll back?** One command, with a time.
4. **It's broken — what do I look at first?** The decision tree.

Here is the shape of the capstone `RUNBOOK.md`. Notice it is concrete enough that someone who has never seen your project could follow it:

```markdown
# Crunch Tracker — Runbook

## Cold deploy: prerequisites
- A managed Postgres instance. Copy its connection string.
- A container host (Render/Railway/Fly) project.
- Set these environment variables on the API service:
  | Var | Example | Notes |
  |---|---|---|
  | DATABASE_URL | jdbc:postgresql://...:5432/crunch?sslmode=require | JDBC shape; SSL required |
  | DATABASE_USER | crunch_user | from the DB provider |
  | DATABASE_PASSWORD | (secret) | from the DB provider; never committed |
  | JWT_SECRET | (32+ random bytes) | `openssl rand -base64 32`; never committed |
  | CORS_ALLOWED_ORIGINS | https://app.example.com | the web origin; not `*` |
  | PORT | 8080 | the host usually injects this |
- GitHub repo secrets: RENDER_DEPLOY_HOOK (the host's deploy URL).

## Deploy
1. Merge to `main`. The GitHub Actions pipeline runs tests, builds the image,
   scans it, pushes to ghcr.io, and POSTs the deploy hook.
2. Watch: `gh run watch`. Then confirm health:
   `curl -s https://<api-host>/actuator/health`  -> expect {"status":"UP"}.
3. First deploy only: confirm Flyway built the schema:
   `psql "$DATABASE_URL" -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;"`

## Mobile build
- `eas build --profile preview --platform android` -> install link.
- Verify EXPO_PUBLIC_API_URL in eas.json points at the live API host.

## Rollback
- Redeploy the previous image SHA: in the host UI select the prior deploy,
  or re-run the pipeline at the previous commit. Takes ~1-2 minutes.
- DB note: migrations are expand/contract; a code rollback never strands the schema.

## It's broken: first five minutes
PAGE / report: "the app won't load data" or "sign-in fails"
  1. Is the API up? `curl -s https://<api-host>/actuator/health`
     - Not UP / no response: the container is down or unhealthy.
       Check the host's deploy log -> did the latest deploy fail to start?
       Most common: a missing/changed env var (JWT_SECRET, DATABASE_URL).
     - UP: continue.
  2. Is it auth or data? Reproduce with curl:
       curl -s -X POST https://<api-host>/api/auth/login \
         -H 'Content-Type: application/json' \
         -d '{"email":"test@example.com","password":"..."}'
     - 401: auth path. Did JWT_SECRET change between build and deploy?
       (A rotated secret invalidates all existing tokens -- expected; users re-login.)
     - 500: check the structured logs filtered to the failing endpoint.
       Most common 500: DB connection (DATABASE_URL/SSL) or a failed migration.
  3. If a recent deploy correlates with the breakage: roll back first (above),
     investigate after. Mitigate, then diagnose.
```

The point of putting the runbook in the review is not that reviewers read all of it — they read the *first line of the "first five minutes" section* and judge whether your observability lets you answer "what's wrong" in one look. "Check `/actuator/health`, then reproduce with curl, then read the structured logs filtered to the endpoint" is an operable system. "I'd look at the logs" is too vague and the reviewers will say so.

## 2.8 — What "done" actually means

This is the question that separates a capstone from a homework assignment, and it's worth stating plainly because students chronically get it wrong in both directions — some declare "done" at "it compiles," others never declare it because there's always one more feature.

For the capstone, **done is a working end-to-end slice that someone else can stand up and use, plus the honest list of what you'd do next.** Concretely:

- A grader clones the repo, sets the documented env vars, triggers the pipeline, and the API comes up healthy against managed Postgres. ✅ Reproducible deploy.
- They install your EAS build, sign in, create a habit, check in, and the row lands in your database, scoped to their user. ✅ Working slice.
- They read your `RUNBOOK.md` and could have done all of that without asking you anything. ✅ Operable by someone else.
- They read your "Known limitations and next steps" and find an honest, prioritized list. ✅ You know where the bodies are.

Note what "done" does **not** require: every feature, perfect test coverage, zero rough edges, multi-region failover, a custom domain. The capstone is graded on a *slice that works end to end and ships*, not on breadth. A small app that genuinely deploys, signs in, and persists a check-in — with a runbook and an honest limitations section — beats a sprawling app that only runs on your laptop. **Depth of "it actually ships" beats breadth of "it almost does five things."**

The "Known limitations and next steps" section deserves emphasis because beginners think it's an admission of failure. It's the opposite. A README that says "here are the three things I'd fix before this took real traffic, in priority order" — single-region database is the SPOF, no refresh-token rotation so sessions are fixed-length, no rate limiting on the auth endpoint — is *dramatically* more credible than one that pretends the system is perfect. Hiring managers read the limitations section first, because it's where they learn whether you can think. Naming your own biggest risk before anyone asks is the single most senior move you make all week.

## 2.9 — The portfolio writeup

The last deliverable is a single paragraph and a link, and it's the thing a hiring manager actually reads. The writeup answers, in a few sentences: what is it, what's it built with, what did *you* do, and where can I see it live. The link is the install link or the live API plus a short demo video. A strong example:

> **Crunch Tracker** — a full-stack habit and goal tracker. A Spring Boot 3 / Java 21 REST API with JWT auth and per-user data ownership, persisted in PostgreSQL with Flyway-versioned migrations, containerized and deployed to a managed host through a GitHub Actions CI/CD pipeline (test → build → scan → push → deploy, no credentials in the repo). The client is a React Native (Expo + TypeScript) app with typed navigation and TanStack Query server-state caching, shipped as an installable EAS build pointed at the live API. I designed the schema, built the API and the mobile client, and wired the deploy pipeline and the runbook. [Live demo: install link] · [5-min walkthrough: video] · [Source]

What makes that paragraph work: it names the stack precisely (so a recruiter's keyword scan hits), it names *what you did* in the first person (so it's clearly your work), and it links a *live* artifact (so the claim is verifiable). A portfolio entry a hiring manager can install and use in ninety seconds is worth more than ten that are screenshots of localhost.

## 2.9b — The architecture review: agenda and the question bank

The Friday review is a structured hour, not a hostile interrogation. It exists to surface, in one sitting, the risks that would otherwise surface in production over months. It is *not* a status update or a "here's what I built, please clap." The deliverable of the meeting is a **risk list** — each item tagged accept / mitigate-now / mitigate-later, with an owner. A review that ends without that list didn't do its job, no matter how good the demo was.

Run the hour like this:

- **Minutes 0–5 — Context.** What is Crunch Tracker *for*, who uses it, and what's the consequence if it's down? One sentence of product context, one of scale ("a few hundred users, a few requests per second"), one of cost ("free tier, single managed DB"). No architecture yet.
- **Minutes 5–15 — The diagram walk.** Put the one-page diagram on screen and walk it: phone → API → database, plus the CI/CD path. Establish the shape before the detail.
- **Minutes 15–30 — Trace one check-in.** The heart of the meeting: the live trace-an-event walk from §2.6. Tap, request, JWT validation, ownership check, the row in Postgres. Name the failure mode at each hop as you go and let the reviewers interrupt.
- **Minutes 30–45 — Failure modes and blast radius.** Off the happy path. What happens when the database is down? When a deploy is bad? What's the data-loss window?
- **Minutes 45–55 — Cost, secrets, and rollback.** The cheap-but-real questions: what does it cost, where are the secrets, how do you roll back.
- **Minutes 55–60 — Risk list and sign-off.** The reviewers state the risks, tag each, assign owners. You write them down. That list becomes your README's "known limitations" section.

The question bank a senior engineer draws from — have an answer to every one *before* Friday, so the five they pick are easy:

**Failure modes and blast radius.**
- *"What's the single point of failure?"* The honest answer is the single-region managed Postgres. Name it, say why it's the right call at this scale, and state the fix (a replica / a multi-AZ tier). "There isn't one" is the wrong answer.
- *"The database is slow — not down, just slow. What happens to a request?"* HikariCP's pool blocks up to its connection-timeout, then the request fails with a 500; the user sees an error state (your Week 09 error UX), not a hung spinner. If your answer is "it hangs forever," you found a real bug in the meeting — which is the point of the meeting.
- *"What's the blast radius of a bad deploy?"* The health check keeps traffic off an unhealthy container; the smoke test catches a broken deploy; rollback is the previous SHA in a minute or two.

**Data and durability.**
- *"Where can you lose data?"* A request that fails before the insert is lost, but the client can retry (the check-in is idempotent-ish on `(habit, user, checked_at)`). Past the insert, Postgres is durable. Name the window.
- *"How is the schema managed and could you rebuild it from empty?"* Flyway, forward-only, and yes — that's the whole `validate` discipline.

**Security.**
- *"Show me one credential in the repo."* There should be none. `git grep` proves it. WIF/`GITHUB_TOKEN` for the pipeline, env-var secrets for the runtime.
- *"What can a stolen JWT do, and for how long?"* It acts as that user until it expires; you don't have refresh-token rotation yet (name it as a limitation), so the window is the token lifetime. Set that lifetime deliberately.

**Operations and cost.**
- *"It's 3am, it's down — first five minutes?"* The §2.7 runbook answer: `/actuator/health`, reproduce with curl, structured logs filtered to the endpoint.
- *"What does this cost, and what's the first thing you'd cut?"* A sentence: free tier or a few dollars, mostly the always-on container and the managed DB. At this scale cost isn't the constraint; say so.

The move that reads most senior across all of these is to **name your own biggest risk before anyone asks** (§2.8). Juniors think their design is flawless; seniors know exactly where the bodies are. Say "the single-region database is my biggest risk and here's the fix and its cost" and the reviewers spend their energy confirming you understand it rather than hunting for a gotcha.

## 2.9c — A worked review exchange

Reading the question bank is one thing; hearing how a good answer *sounds* is another. Here's a reconstructed exchange from a capstone-style review, lightly edited. The candidate is presenting; "R" is the reviewer.

> **R:** Walk me through what happens when your managed Postgres goes down — not slow, fully down.
>
> **Candidate:** Two things. The API stays up — the container is healthy, `/actuator/health` would actually flip to `DOWN` because I wired the DB into the health indicator, so the host stops routing to it and the app sees a clean error state rather than a hang. No request silently corrupts anything because there's no write path that half-commits. The user sees "couldn't load your habits, retry," which is my Week 09 error UX. What I *lose* is availability: there's one database, single region, so until it's back, the app can't read or write. That's my biggest risk and it's the first thing I'd fix with money — a managed replica or a multi-AZ tier — but at a few hundred users on a free tier it's the right call, and I documented the upgrade path.
>
> **R:** So you lose availability. Do you lose data?
>
> **Candidate:** No. Anything committed is durable — managed Postgres handles backups and point-in-time recovery on the paid tiers. The only loss window is a request in flight when the DB dropped, and that's a failed insert the client can retry; the check-in row has a unique constraint on habit-plus-user-plus-timestamp so a retry can't double-insert.
>
> **R:** How would you even know it's down at 3am?
>
> **Candidate:** Honestly, right now I'd know because a user reported it — I don't have an external uptime monitor yet, and that's a named limitation in my README. If I were taking real traffic the first thing I'd add is a synthetic check hitting `/actuator/health` from outside, paging me on two consecutive failures. My runbook's first line is already "check `/actuator/health`," so the response is ready; what's missing is the *page*, and I know exactly what it is.

That answer wins because it separates availability from durability, gives concrete mechanisms (the health indicator, the unique constraint, the error UX), names the biggest risk with its fix and cost before being pushed, and is honest about the gap (no external monitor) while showing the candidate knows precisely how to close it. That's the shape of every strong review answer: specific, honest about the weakness, and connected to what you'd actually do.

## 2.10 — The review and the interview are the same skill

The capstone week runs a mock interview alongside the architecture review, and it's worth seeing why they sit together: they're two views of one competence. The review defends a system *you built*. The interview asks you to design or debug one *on the spot*. Both reward the same muscles: tracing data flow, naming failure modes, defending tradeoffs with reasons (not taste), and being honest about what you don't know.

The most common mistake in both is the same: **jumping to a solution before establishing the requirements.** A candidate who hears "design a habit tracker's backend" and immediately starts naming tables has skipped the questions that matter — how many users, what's the consistency requirement, what happens if the database is down, what's the auth model. Spend the first minutes on requirements and assumptions, *then* design. That's the same "context first" opening as the review.

The second most common mistake is bluffing on a deep-dive. When the interviewer asks "what does `ddl-auto: validate` actually do" and you're not sure, "I believe it checks the schema matches the entities and refuses to modify it — let me reason about why that's the safe production setting" is a *strong* answer. A confident wrong answer is a *weak* one, and interviewers can always tell. Honesty about the edge of your knowledge, paired with the reasoning you *can* do, is what distinguishes the senior candidate — and it's the same honesty the review rewards when you name your own biggest risk.

Write the retrospective afterward (Homework Problem 6): the two questions you answered well, the one you fumbled, what you'd say differently. The fumble is the valuable part — naming your own weak answer and articulating the better one is the exact metacognition that makes the *next* interview go better.

## Summary

Shipping the mobile app means producing a standalone binary with EAS, not running through Expo Go — and the binary's API URL is decided at build time and frozen in, so the deploy-killer is a `localhost` URL in a shipped build. You avoid it with one config seam (`EXPO_PUBLIC_API_URL` per `eas.json` profile, read through a single typed module that throws if it's missing) and one `apiClient` that consumes it. The token lives in `expo-secure-store`, cleared on logout. The demo traces one real check-in from the tap to the row in managed Postgres, proving the whole stack and the user-scoping in one motion — rehearse it, with a recorded fallback. The `RUNBOOK.md` lets someone deploy from cold: the env vars, the deploy command, the one-command rollback (safe because migrations are expand/contract), and the "first five minutes" tree that starts at `/actuator/health`. "Done" is a working end-to-end slice someone else can stand up and use, plus an honest "known limitations" section — depth of *it actually ships* over breadth of *it almost does five things*. The portfolio writeup is one precise paragraph and a live link. And the review and the mock interview are the same skill: trace the flow, name the failure modes, defend the tradeoffs with reasons, and be honest about what you don't know. Now ship it.
