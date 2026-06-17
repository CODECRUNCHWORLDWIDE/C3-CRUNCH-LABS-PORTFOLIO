# Challenge 1 — Ownership Authorization That You Can Prove

**Time estimate:** ~110 minutes.

## Problem statement

Your API now authenticates: a valid JWT proves who the caller is. But authentication is not authorization. Right now, if your repositories still query by raw id, **user A can read and mutate user B's habits** — they just need a valid token of their own and someone else's habit id. This is OWASP API Security #1, **Broken Object Level Authorization (BOLA)**, and it is the bug this challenge exists to kill.

You will:

1. Enforce per-user ownership across `goals`, `habits`, and a habit's `check-ins` — at the **repository** layer (owner-scoped queries) *and* with a **`@PreAuthorize`** rule.
2. Make the wrong-owner case return a deliberate, documented status (`403` or `404` — you choose and justify).
3. Write the integration tests that **prove** the rule fires: a `403` for the wrong owner, and a regression test that would fail if someone deleted the enforcement.

This is the part of the week that makes Crunch Tracker a real multi-user product instead of a shared notebook.

## Build on

The week-5 Crunch Tracker plus this week's exercises: the `JwtService` (exercise 2), the `SecurityConfig` and `JwtAuthFilter` (exercise 3), and the `app_user` table with `owner_id` foreign keys on `goals`/`habits` (Lecture 2 §3). If you haven't done those, do them first — this challenge assumes a request arrives with `@AuthenticationPrincipal AppUser caller` populated.

## Requirements

### 1. Owner-scoped queries (the real defense)

Every repository read of an owned resource must take the owner id and filter on it. No method that fetches an owned row by id alone may remain reachable from a controller.

```java
public interface HabitRepository extends JpaRepository<Habit, Long> {
    Optional<Habit> findByIdAndOwnerId(Long id, Long ownerId);
    Page<Habit> findAllByOwnerId(Long ownerId, Pageable pageable);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
```

Services pass `caller.id()` into every query. A read that doesn't match the owner returns empty, which maps to your chosen not-allowed status.

### 2. A `@PreAuthorize` ownership rule (defense in depth)

Add an `@Component("owns")` evaluator with a method per owned type, and annotate the controller methods that take an id:

```java
@PreAuthorize("@owns.habit(#id)")
@DeleteMapping("/habits/{id}")
ResponseEntity<Void> delete(@PathVariable Long id) { ... }
```

The rule must cover `goals`, `habits`, and `check-ins` (a check-in is owned transitively through its habit's owner). Use SpEL `#paramName` to reference the path variable.

### 3. The status choice, on purpose

Decide whether a wrong-owner request returns `403 Forbidden` (honest: "this exists, you can't have it") or `404 Not Found` (hides existence, prevents enumeration). Either is defensible. Write your choice and a one-paragraph justification in `docs/authz-decisions.md`. Be consistent across all three resources.

### 4. The tests that prove it

This is the graded heart of the challenge. Using `@SpringBootTest` + `MockMvc` (against Testcontainers Postgres, as in week 5), write at least these:

- `own_data_is_reachable` — user A reads/deletes A's own habit → `200`/`204`.
- `wrong_owner_is_rejected` — user A reads B's habit by id → your chosen `403`/`404`, and the body does **not** contain B's habit name.
- `wrong_owner_cannot_delete` — user A `DELETE`s B's habit → rejected, and B's habit **still exists** afterward (assert it).
- `no_token_is_unauthorized` — no `Authorization` header → `401`.
- `tampered_token_is_unauthorized` — corrupted token → `401`.
- `check_in_ownership_is_transitive` — user A tries to add a check-in to B's habit → rejected.

```java
@Test
void wrong_owner_cannot_delete_and_the_habit_survives() throws Exception {
    AppUser b = users.save(new AppUser("b@crunch.dev", encoder.encode("longpassword12"), "Bea"));
    Habit bHabit = habits.save(Habit.forOwner(b.id(), "Bea's run"));
    String tokenA = jwt.issue(userA.id());

    mockMvc.perform(delete("/api/v1/habits/{id}", bHabit.id())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
           .andExpect(status().isForbidden());            // or .isNotFound() per your choice

    assertThat(habits.findById(bHabit.id())).isPresent(); // the proof: B's data survived
}
```

## Acceptance criteria

- [ ] No controller path can fetch or mutate an owned resource without the owner id flowing from `@AuthenticationPrincipal`.
- [ ] An `@owns` evaluator covers `goals`, `habits`, and `check-ins`; the relevant controller methods carry `@PreAuthorize`.
- [ ] `@EnableMethodSecurity` is present (verify `@PreAuthorize` actually runs — temporarily break the rule and watch a test go red).
- [ ] `docs/authz-decisions.md` states and justifies the `403`-vs-`404` choice.
- [ ] At least **6** integration tests, including the wrong-owner negative paths and the "survives" assertion.
- [ ] **Regression proof:** comment out the ownership check, run the suite, and confirm a test fails. Re-enable. Note this in your PR description.
- [ ] `./mvnw verify` is green (unit + Testcontainers integration tests).
- [ ] Committed under `challenges/challenge-01/` (or as a feature branch + PR) in your week-6 repo.

## Stretch

- **Bulk endpoint leak.** Add `GET /api/v1/check-ins` that lists the caller's check-ins across all habits. Write a test that seeds check-ins for both A and B and asserts A's list contains *only* A's — the classic place BOLA hides (the single-resource endpoint is secured but the list endpoint forgets the filter).
- **Method-level `@PostAuthorize`.** For one read endpoint, use `@PostAuthorize("returnObject.ownerId == authentication.principal.id")` instead of a pre-check, and explain when post-authorization is appropriate (and its danger: the work already happened).
- **Audit log.** On every `403`, write a structured log line (`who`, `what resource`, `when`). Note why this matters for incident response — a quiet `403` is a probe you'd want to see in a dashboard.

## Hints

<details>
<summary>The ownership evaluator</summary>

```java
@Component("owns")
public class OwnershipEvaluator {

    private final HabitRepository habits;
    private final GoalRepository goals;
    private final CheckInRepository checkIns;

    // constructor omitted

    private Long callerId() {
        return ((AppUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).id();
    }

    public boolean habit(Long habitId) {
        return habits.existsByIdAndOwnerId(habitId, callerId());
    }

    public boolean goal(Long goalId) {
        return goals.existsByIdAndOwnerId(goalId, callerId());
    }

    public boolean checkIn(Long habitId) {
        // a check-in is owned through its habit
        return habits.existsByIdAndOwnerId(habitId, callerId());
    }
}
```

`existsByIdAndOwnerId` is one indexed query and avoids loading the whole entity just to check ownership.

</details>

<details>
<summary>Watching the test fail first</summary>

The only way to trust a `403` is to have seen the endpoint do the wrong thing first. Temporarily change `findByIdAndOwnerId(id, callerId)` back to `findById(id)` and run `wrong_owner_is_rejected`. It should now return `200` with B's data — a red test. *That* is the bug. Revert, and the test goes green. Keep that experience; it's why the test exists.

</details>

## Why this matters

BOLA is the most common serious API vulnerability in the wild — it tops the OWASP API Security list precisely because it's so easy to ship: the happy path works, the demo works, the bug is invisible until someone enumerates ids. Every later week assumes this is solid: week 9's mobile client loads *the signed-in user's* habits and trusts that the API will never hand it someone else's. The tests you write here are the contract that promise is built on.
