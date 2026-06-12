# Week 4 — Spring Boot REST Basics

Welcome to the week the **backend goes live**. For three weeks Crunch Tracker has been a pile of well-tested Java objects living in memory and reachable only from a JUnit test. This week we put an HTTP front door on it. By Friday you can start a Spring Boot 3.5 service from a blank folder, expose the goal/habit domain over JSON, validate every request body, return RFC-9457 `ProblemDetail` errors instead of stack traces, and hand someone a generated OpenAPI page they can click through and curl.

This is the hinge of the whole 10-week program. Weeks 1–3 made you a Java engineer who can model a domain and prove it works. Week 4 makes that domain *reachable*. Week 5 puts it on Postgres, Week 6 locks it down with auth, and Weeks 7–9 build the React Native client that talks to exactly the API you design this week. The contract you write now is the contract the mobile app consumes later — so we treat it as a contract, not a sketch.

The first thing to internalize: **Spring Boot is not magic, it is defaults.** Everything it does for you — starting an embedded Tomcat, wiring a Jackson `ObjectMapper`, mapping `@RestController` methods to URLs — is plain Java you could have written by hand, run for you by a dependency-injection container at startup. Auto-configuration is "sensible defaults you can override," nothing more. We spend Lecture 1 making sure you never see the framework as a black box.

## Learning objectives

By the end of this week, you will be able to:

- **Explain** what Spring Boot auto-configuration actually does — beans, the `ApplicationContext`, component scanning, `@ConditionalOnMissingBean` — without hand-waving "it's magic."
- **Scaffold** a Spring Boot 3.5 / Java 21 project from [start.spring.io](https://start.spring.io) or the CLI, and read every line of the generated `pom.xml` and `application.yml`.
- **Wire** dependencies with constructor injection and understand why field injection is discouraged.
- **Expose** a domain over HTTP with `@RestController`, `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`, `@PathVariable`, and `@RequestParam`.
- **Separate** the wire format from the domain with **DTOs** — request records, response records, and explicit mapping — so the API contract and the internal model can evolve independently.
- **Validate** request bodies with Jakarta Bean Validation (`@NotBlank`, `@Positive`, `@Size`, `@Valid`) and custom constraints.
- **Return** correct HTTP status codes (`200`, `201` + `Location`, `204`, `400`, `404`, `409`) on purpose, not by accident.
- **Produce** structured errors as `ProblemDetail` (RFC 9457) through `@RestControllerAdvice`, so clients get machine-readable JSON instead of a 500 and a stack trace.
- **Document** the API with springdoc-openapi and exercise it from Swagger UI, `curl`, and HTTPie.
- **Write** `@WebMvcTest` slice tests and full `@SpringBootTest` + `MockMvc` tests that assert status codes and JSON bodies.

## Prerequisites

This week assumes you have completed **C3 weeks 1–3**, or have equivalent Java fluency. Specifically:

- You can scaffold and build a JDK 21 project (Week 1) and drive Git/PR review on a feature branch.
- You write Java 21, not Java 8: records, sealed interfaces, and pattern-matching `switch` are reflexes (Week 2).
- You factor logic behind interfaces and back it with a JUnit 5 + AssertJ suite (Week 3). **You already have a `GoalService`, `HabitService`, and an in-memory `GoalRepository`/`HabitRepository` interface from the Week 3 mini-project — this week wraps exactly that code in HTTP.** If you skipped Week 3, clone the reference solution first; this week extends it, it does not replace it.
- You are comfortable in a terminal: `cd`, run a JAR, `curl` a URL, read a JSON response.

You do **not** need prior Spring experience. We start at the container. If you have learned old Spring (XML config, `web.xml`, `dispatcher-servlet.xml`), unlearn it — Spring Boot 3 is annotation-and-convention-driven and we will flag the legacy habits as we go.

## Topics covered

- The Spring ecosystem in 2026: Spring Framework 6, Spring Boot 3.5, Spring Web MVC, and where Spring Security / Spring Data fit (next weeks).
- The IoC container: beans, the `ApplicationContext`, `@Component`/`@Service`/`@RestController`, component scanning.
- Dependency injection: constructor injection (preferred), why field injection is a smell, `@Autowired` and when you can omit it.
- Auto-configuration: starters, `spring-boot-starter-web`, `@ConditionalOnMissingBean`, the embedded Tomcat, the `application.yml`/`application.properties` hierarchy.
- The request lifecycle: `DispatcherServlet` → handler mapping → controller → message conversion (Jackson) → response.
- REST mapping annotations: `@RestController`, `@RequestMapping`, the HTTP-verb shortcuts, `@PathVariable`, `@RequestParam`, `@RequestBody`, `ResponseEntity`.
- DTOs vs domain: why you never serialize a domain entity directly, request/response record DTOs, and mapping by hand.
- HTTP status codes that mean what they say: `201 Created` + `Location`, `204 No Content`, `404`, `409 Conflict`.
- Jakarta Bean Validation: `@Valid`, `@NotBlank`, `@NotNull`, `@Positive`, `@Size`, `@Pattern`, validation groups, and writing a custom constraint.
- RFC 9457 `ProblemDetail` and global error handling with `@RestControllerAdvice` + `@ExceptionHandler`.
- springdoc-openapi: the generated `/v3/api-docs` and Swagger UI; annotating with `@Operation` and `@Schema`.
- Testing: `@WebMvcTest` slices with `MockMvc`, `@SpringBootTest` end-to-end, and asserting JSON with `jsonPath`.
- Exercising the API by hand: `curl`, HTTPie, and the `.http` file format.

## Weekly schedule

The schedule below adds up to approximately **36 hours**. Treat it as a target, not a stopwatch.

| Day       | Focus                                                  | Lectures | Exercises | Challenges | Quiz/Read | Homework | Mini-Project | Self-Study | Daily Total |
|-----------|--------------------------------------------------------|---------:|----------:|-----------:|----------:|---------:|-------------:|-----------:|------------:|
| Monday    | Spring Boot from first principles: beans, DI, auto-config |    2h    |    1.5h   |     0h     |    0.5h   |   1h     |     0h       |    0.5h    |     5.5h    |
| Tuesday   | Controllers, request mapping, DTOs, status codes       |    2h    |    2h     |     0h     |    0.5h   |   1h     |     0h       |    0h      |     5.5h    |
| Wednesday | Bean Validation + ProblemDetail error handling         |    1h    |    2h     |     1.5h   |    0.5h   |   1h     |     0h       |    0.5h    |     6.5h    |
| Thursday  | OpenAPI docs, MockMvc testing, curl/HTTPie             |    1h    |    1h     |     0h     |    0.5h   |   1h     |     2h       |    0.5h    |     6h      |
| Friday    | Mini-project: wrap Crunch Tracker in REST              |    0h    |    0h     |     0h     |    0.5h   |   1h     |     3h       |    0.5h    |     5h      |
| Saturday  | Mini-project deep work                                 |    0h    |    0h     |     0h     |    0h     |   0h     |     3.5h     |    0h      |     3.5h    |
| Sunday    | Quiz, review, polish, OpenAPI export                   |    0h    |    0h     |     0h     |    1h     |   0h     |     1.5h     |    0h      |     2.5h    |
| **Total** |                                                        | **6h**   | **6.5h**  | **1.5h**   | **3.5h**  | **5h**   | **13.5h**    | **2h**     | **34.5h**   |

## How to navigate this week

| File | What's inside |
|------|---------------|
| [README.md](./README.md) | This overview (you are here) |
| [resources.md](./resources.md) | Curated Spring, Jakarta, and OpenAPI links — all free |
| [lecture-notes/01-spring-boot-from-first-principles.md](./lecture-notes/01-spring-boot-from-first-principles.md) | What Spring Boot 3 actually is; the IoC container; beans; DI; auto-configuration; the request lifecycle |
| [lecture-notes/02-clean-http-surface.md](./lecture-notes/02-clean-http-surface.md) | Controllers, DTOs, status codes, Bean Validation, `ProblemDetail`, OpenAPI, MockMvc |
| [exercises/README.md](./exercises/README.md) | Index of short coding exercises |
| [exercises/exercise-01-bootstrap-a-service.md](./exercises/exercise-01-bootstrap-a-service.md) | Scaffold a Boot service, read the generated files, add a health endpoint |
| [exercises/exercise-02-goal-controller.java](./exercises/exercise-02-goal-controller.java) | Fill-in-the-TODO CRUD controller with DTOs and status codes |
| [exercises/exercise-03-validation-and-problemdetail.java](./exercises/exercise-03-validation-and-problemdetail.java) | Add Bean Validation and a `@RestControllerAdvice` that emits `ProblemDetail` |
| [challenges/README.md](./challenges/README.md) | Index of weekly challenges |
| [challenges/challenge-01-paginated-list-endpoint.md](./challenges/challenge-01-paginated-list-endpoint.md) | Implement a paginated, filterable, sortable list endpoint with validated 400s |
| [quiz.md](./quiz.md) | 10 multiple-choice questions with an answer key |
| [homework.md](./homework.md) | Six practice problems with a grading rubric |
| [mini-project/README.md](./mini-project/README.md) | Full spec for the "Crunch Tracker REST API" mini-project |

## The "green build, honest status code" promise

C3 uses a recurring marker in every exercise that ends in working code. When you start the app you should see Boot's banner and then:

```
Started CrunchTrackerApplication in 1.732 seconds (process running for 2.011)
Tomcat started on port 8080 (http) with context path '/'
```

And when you curl an endpoint, the **status code is the first thing you check** — not the body:

```bash
$ curl -i -X POST localhost:8080/api/goals -H 'Content-Type: application/json' -d '{"title":""}'
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json
```

If a malformed request returns `500` and a stack trace, you are not done. A REST API that returns `500` for a client mistake is lying about whose fault it is. The point of Week 4 is to make `400`/`404`/`201`-with-`Location` the ordinary, boring outcome.

## Stretch goals

If you finish the regular work early and want to push further:

- Read the official **"Building a RESTful Web Service"** guide and notice how much of it you now understand: <https://spring.io/guides/gs/rest-service>.
- Skim **RFC 9457 (Problem Details for HTTP APIs)** — it is short and readable: <https://www.rfc-editor.org/rfc/rfc9457>.
- Turn on `logging.level.org.springframework.web=DEBUG` and watch the `DispatcherServlet` route a single request end to end. Trace it in the log.
- Browse the **spring-projects/spring-boot** repo on GitHub and open one `*AutoConfiguration.java` file (e.g. `WebMvcAutoConfiguration`). Read the `@Conditional` annotations: <https://github.com/spring-projects/spring-boot>.
- Write a short note for your future self comparing how you'd return "not found" in three styles: throwing an exception handled by advice, returning `ResponseEntity.notFound()`, and returning `Optional` from a `@GetMapping`. Which reads best? Which is most testable?

## Up next

Continue to **Week 5 — Persistence: JPA and Postgres** once you have pushed the mini-project to your GitHub. Next week the in-memory repository you wrapped this week gets swapped for Spring Data JPA on a Dockerized Postgres — **and your API contract must not change**. The DTO boundary you build this week is what makes that swap invisible to the mobile client.

---

*If you find errors in this material, please open an issue or send a PR. Future learners will thank you.*
