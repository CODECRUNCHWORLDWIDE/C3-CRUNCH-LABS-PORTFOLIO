# C3 · Crunch Labs Portfolio — Syllabus

A 10-week program. Each week ships the standard 13-artifact format (README, resources, 2 lecture notes, 3 exercises, a challenge, a mini-project, quiz, homework).

## Week 01 — Agile, Git, and the JDK Toolchain

We set the working agreements before we write a line of business logic: how a Scrum/Kanban team actually moves, and how Git keeps that team honest. By Friday you can scaffold a JDK 21 project, drive a board through a sprint, and ship a trunk-based feature branch with a clean PR. This is the floor every later week stands on.

- **Lectures:** How a Real Sprint Runs: Scrum, Kanban, and the Ceremonies That Earn Their Keep; Git for Teams: Trunk-Based Development, Feature Branches, and PR Review
- **Exercises:** Terminal-first JDK 21 setup with SDKMAN; create a Maven/Gradle project skeleton; branch, commit with conventional messages, open and review a PR; carve one user story into INVEST tasks on a board.
- **Challenge:** Resolve a deliberately gnarly three-way merge conflict and rebase a messy branch into a clean, reviewable history without losing work.
- **Mini-project:** Stand up the team repo and board for 'Crunch Tracker' — a habit/goal tracker that every later week extends. Deliver the README, a populated backlog with 6+ INVEST stories, branch protection, a PR template, and a green 'hello, JDK 21' build in CI.
- **Key tech:** JDK 21, SDKMAN, Maven, Git, GitHub Actions, Scrum/Kanban

## Week 02 — Java Core and the JVM

The language tour for engineers who already know how to program. We cover the type system, control flow, and the modern Java idioms (records, sealed types, pattern-matching switch) so you write Java 21, not Java 8. You also learn what the JVM is actually doing underneath, because later weeks debug it.

- **Lectures:** Java 21 the Language: Types, Records, Sealed Interfaces, and Pattern-Matching switch; What the JVM Actually Does: Bytecode, the Heap/Stack, and Garbage Collection
- **Exercises:** Fill-in-the-TODO drills on primitives vs objects, immutability with records, exhaustive sealed-type switches, and exceptions vs Optional for absence.
- **Challenge:** Model a small domain (a transaction ledger) with sealed interfaces and records, then exhaustively process it with a pattern-matching switch — no instanceof chains, no nulls.
- **Mini-project:** Add the core domain model to Crunch Tracker: immutable records and sealed types for Goal, Habit, and CheckIn, with a small in-memory service that creates and queries them. Pure Java, no framework yet.
- **Key tech:** Java 21, records, sealed types, pattern matching, JVM, Optional

## Week 03 — OOP, Collections, and Testing

We turn the in-memory model into well-factored objects and prove it works. Interfaces, composition over inheritance, and the right collection for the job, then JUnit 5 + AssertJ so 'it compiles' is never mistaken for 'it works'. Testing becomes a habit here so the Spring weeks aren't terrifying.

- **Lectures:** OOP That Survives Review: Interfaces, Composition over Inheritance, and the Collections Framework; Testing as Default: JUnit 5, AssertJ, and Test-Driven Slices with Mockito
- **Exercises:** Pick and justify List/Map/Set/Deque choices; write equals/hashCode correctly; convert procedural code to small testable units; red-green-refactor a feature with JUnit 5.
- **Challenge:** Inherit a 200-line untested 'God class' and refactor it behind interfaces with a passing characterization test suite written before you touch the logic.
- **Mini-project:** Refactor the Crunch Tracker domain into clean services behind interfaces and back it with a JUnit 5 + AssertJ suite (80%+ on domain logic). Introduce a repository interface so persistence can slot in next week.
- **Key tech:** Java Collections, JUnit 5, AssertJ, Mockito, interfaces, TDD

## Week 04 — Spring Boot REST Basics

The backend goes live. We stand up a Spring Boot 3.x service, expose the tracker domain over HTTP, and learn the request lifecycle: controllers, DTOs, validation, and proper status codes. By the end you have a documented JSON API you can curl.

- **Lectures:** Spring Boot 3 from First Principles: Auto-Configuration, Beans, and the DI Container; A Clean HTTP Surface: Controllers, DTOs, Bean Validation, and ProblemDetail Errors
- **Exercises:** Build CRUD endpoints; map entities to DTOs; add jakarta.validation constraints; return RFC-9457 ProblemDetail errors; document with springdoc OpenAPI and exercise it via curl/HTTPie.
- **Challenge:** Take a spec for a paginated, filterable, sortable list endpoint and implement it correctly — including validation failures returning structured 400s, not stack traces.
- **Mini-project:** Wrap the Crunch Tracker domain in a Spring Boot REST API: CRUD for goals and habits, validated request bodies, ProblemDetail errors, and generated OpenAPI docs — backed by the in-memory repository from week 3.
- **Key tech:** Spring Boot 3, Spring Web, Bean Validation, springdoc-openapi, DTOs, ProblemDetail

## Week 05 — Persistence: JPA and Postgres

In-memory dies; real data lives. We add Spring Data JPA over PostgreSQL, learn the entity lifecycle and the queries Hibernate generates, and version the schema with Flyway. This is where 'works on my machine' starts meaning something reproducible.

- **Lectures:** Spring Data JPA and Hibernate: Entities, Relationships, and the N+1 Trap; Schema as Code: Flyway Migrations, Postgres in Docker, and Repository Queries
- **Exercises:** Map entities and @OneToMany/@ManyToOne relationships; write derived and @Query methods; spot and fix an N+1 with a fetch join; author idempotent Flyway migrations against a Dockerized Postgres.
- **Challenge:** Given a slow endpoint, use logged SQL to diagnose an N+1 query, fix it without breaking the API contract, and prove the query count dropped.
- **Mini-project:** Swap Crunch Tracker's in-memory repository for Spring Data JPA on PostgreSQL (Docker Compose), with Flyway migrations for the goals/habits/check-ins schema. Same API contract, real persistence, integration tests via Testcontainers.
- **Key tech:** Spring Data JPA, Hibernate, PostgreSQL, Flyway, Docker Compose, Testcontainers

## Week 06 — API Design and Auth

We make the API safe to expose: versioning, sane resource design, and stateless authentication with Spring Security and JWT. Users, registration, login, and per-user data ownership turn the tracker from a demo into a multi-user product.

- **Lectures:** REST API Design That Ages Well: Versioning, Idempotency, Pagination, and CORS; Stateless Auth with Spring Security 6: JWT, Password Hashing, and Method-Level Authorization
- **Exercises:** Add register/login issuing JWTs; configure the Spring Security 6 filter chain; hash passwords with BCrypt; enforce resource ownership; configure CORS for a mobile client.
- **Challenge:** Implement a @PreAuthorize ownership rule so user A can never read or mutate user B's habits, and write the integration tests that prove the 403s actually fire.
- **Mini-project:** Add accounts to Crunch Tracker: registration, JWT login, BCrypt-hashed passwords, CORS for the mobile app, and per-user data scoping so every goal/habit belongs to its owner — enforced and tested.
- **Key tech:** Spring Security 6, JWT, BCrypt, CORS, method security, API versioning

## Week 07 — React Native Basics

The frontend begins. We pivot to TypeScript and React Native via Expo, covering components, props, state, hooks, and styling for mobile. By the end you can build and run a typed multi-screen UI on a simulator and a real device.

- **Lectures:** React Native with Expo and TypeScript: Components, Props, JSX, and the Render Model; State and Effects on Mobile: useState, useEffect, Lists, and Styling for Touch
- **Exercises:** Scaffold an Expo + TypeScript app; build typed function components; manage local state with hooks; render performant FlatLists; style with Flexbox for varied screen sizes.
- **Challenge:** Rebuild a provided design-spec screen pixel-faithfully as a typed, reusable component with proper touch targets and an empty/loading state — no any types.
- **Mini-project:** Start the Crunch Tracker mobile client: an Expo + TypeScript app with a habit-list screen and an add-habit form, working entirely against mocked local data for now.
- **Key tech:** React Native, Expo, TypeScript, React hooks, FlatList, Flexbox

## Week 08 — Navigation and App State

A real app is more than one screen. We add stack and tab navigation, typed routes, forms, and a deliberate state strategy — local vs server state — so the UI scales past a toy. This sets up the integration week cleanly.

- **Lectures:** Multi-Screen Apps: React Navigation, Typed Routes, Tabs, and Deep Links; Managing State on Purpose: Context, Zustand, and Why Server State Is Different
- **Exercises:** Wire stack + bottom-tab navigators with typed params; build a controlled multi-field form with validation; lift shared state into Context/Zustand; persist a token to secure storage.
- **Challenge:** Implement an auth-gated navigation flow: unauthenticated users see login, authenticated users see the tabs, and a logout clears state and routes back — with no flicker or back-button leaks.
- **Mini-project:** Grow the mobile client into a navigable app: login screen, tabbed habits/goals/profile, typed routes, a Zustand store for session/UI state, and forms ready to talk to a backend — still on mock data.
- **Key tech:** React Navigation, Zustand, React Context, Expo SecureStore, TypeScript, forms

## Week 09 — Full-Stack Integration

The two halves meet. The React Native app talks to the live Spring API: typed fetch clients, JWT-authenticated requests, server-state caching with TanStack Query, and honest loading/error/offline handling. This is the week the whole course pays off.

- **Lectures:** Wiring the Client to the API: Typed Fetch, Auth Headers, and Environment Config; Server State Done Right: TanStack Query, Caching, Optimistic Updates, and Error UX
- **Exercises:** Build a typed API client; attach and refresh JWTs; fetch and mutate with TanStack Query; handle loading/error/empty states; debug CORS and 401s across the real stack.
- **Challenge:** Implement an optimistic 'check in to a habit' mutation that updates the UI instantly, rolls back on server failure, and reconciles with the authoritative response — all type-safe end to end.
- **Mini-project:** Connect the Crunch Tracker mobile app to the Spring Boot + Postgres backend for real: log in against the JWT endpoint, load and mutate the signed-in user's habits/goals via TanStack Query, with proper auth, error, and refresh handling.
- **Key tech:** TanStack Query, Fetch API, JWT, TypeScript, Spring Boot, CORS

## Week 10 — Capstone: Ship the Full-Stack App

You ship. The final week converts the integrated app into a deployed, demoable product: the API in a container on a host, the database managed, the mobile build installable, and a runbook plus a portfolio writeup. The capstone is graded on a working end-to-end slice, not slides.

- **Lectures:** From Repo to Running: Dockerizing Spring Boot, Deploying the API, and CI/CD; Shipping the Mobile App: EAS Builds, Release Config, the Demo, and the Runbook
- **Exercises:** Containerize the API with a multi-stage Dockerfile; deploy to a managed Postgres + container host; wire a GitHub Actions deploy pipeline; produce an installable Expo/EAS build pointed at the live API.
- **Challenge:** Take the full stack from a clean clone to a live, sign-in-and-use demo with one documented pipeline run — then write the runbook another engineer could deploy from cold.
- **Mini-project:** Ship Crunch Tracker: the Spring Boot API deployed and reachable, Postgres managed and migrated, the React Native app built and installable against the live backend, a RUNBOOK.md, and a one-paragraph portfolio writeup linking the live demo.
- **Key tech:** Docker, GitHub Actions, Expo EAS, managed Postgres, CI/CD, Spring Boot

