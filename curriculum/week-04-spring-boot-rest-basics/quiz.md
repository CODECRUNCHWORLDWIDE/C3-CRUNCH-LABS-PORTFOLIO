# Week 4 — Quiz

Ten multiple-choice questions. Take it with your lecture notes closed. Aim for 9/10 before moving to Week 5. Answer key at the bottom — don't peek.

---

**Q1.** Which statement best describes the relationship between Spring Framework, Spring Boot, and Spring MVC?

- A) They are three names for the same library, shipped together for marketing.
- B) Spring Boot is the core; Spring Framework and Spring MVC are optional add-ons you install separately.
- C) Spring Framework is the core (the IoC container + abstractions), Spring MVC is its web stack, and Spring Boot auto-configures both with sensible defaults.
- D) Spring MVC is the language, Spring Boot is the runtime, and Spring Framework is the IDE.

---

**Q2.** Why does C3 require constructor injection with `final` fields instead of field injection?

- A) Field injection doesn't compile in Java 21.
- B) Constructor injection makes dependencies explicit and the object testable without reflection; `final` guarantees full initialization. Field injection hides design smells.
- C) Field injection is faster at runtime, so we avoid it for performance.
- D) There is no real difference; it's purely a style preference with no consequences.

---

**Q3.** What does `@ConditionalOnMissingBean` accomplish in an auto-configuration class?

- A) It deletes any bean you defined that conflicts with Spring's.
- B) It provides Spring's default bean **only if you have not defined your own** bean of that type — the override mechanism.
- C) It throws an error at startup if a required bean is missing.
- D) It marks a bean as lazily initialized.

---

**Q4.** A `POST /api/goals` successfully creates a resource. What should it return?

- A) `200 OK` with the created body.
- B) `201 Created` with a `Location` header pointing at the new resource, and the created body.
- C) `204 No Content` with an empty body.
- D) `302 Found` redirecting to the new resource.

---

**Q5.** Why do we map domain entities to DTO records instead of serializing the entity directly?

- A) Records serialize faster than classes.
- B) Spring refuses to serialize non-record types.
- C) The DTO is the public wire contract; keeping it separate lets the internal domain evolve (e.g. becoming a JPA entity in Week 5) without changing or leaking onto the API.
- D) DTOs are required for validation to work at all.

---

**Q6.** Given:

```java
public record CreateGoalRequest(@NotBlank String title, @Positive int targetPerWeek) {}

@PostMapping
public ResponseEntity<GoalResponse> create(@RequestBody CreateGoalRequest req) { ... }
```

A client posts `{"title":"","targetPerWeek":-1}`. What happens?

- A) The request is rejected with a `400` because Bean Validation runs automatically on any `@RequestBody`.
- B) The method runs with the invalid data, because `@Valid` is **missing** from the parameter — the annotations do nothing without it.
- C) The app throws a `500` at startup because the constraints conflict.
- D) Jackson refuses to deserialize the body.

---

**Q7.** What `Content-Type` does Spring use when you return a `ProblemDetail` from an `@ExceptionHandler`?

- A) `application/json`
- B) `text/plain`
- C) `application/problem+json`
- D) `application/xml`

---

**Q8.** What is the difference between `@WebMvcTest` and `@SpringBootTest`?

- A) `@WebMvcTest` loads only the web layer (controllers, advice, converters) and mocks the rest; `@SpringBootTest` boots the entire application context.
- B) They are aliases for the same annotation.
- C) `@WebMvcTest` starts a real server on port 8080; `@SpringBootTest` never does.
- D) `@SpringBootTest` is for unit tests; `@WebMvcTest` is for production.

---

**Q9.** A request to `GET /api/goals/{id}` for an id that doesn't exist should return:

- A) `200 OK` with `null` body.
- B) `204 No Content`.
- C) `404 Not Found` as a `ProblemDetail` (typically from a `NotFoundException` handled by the global advice).
- D) `500 Internal Server Error` with the stack trace, so the client can debug it.

---

**Q10.** Where does springdoc-openapi serve the raw machine-readable API document and the interactive UI by default?

- A) `/api-docs.json` and `/docs`.
- B) `/v3/api-docs` and `/swagger-ui.html`.
- C) `/openapi` and `/explorer`.
- D) `/actuator/openapi` and `/actuator/swagger`.

---

## Answer key

<details>
<summary>Click to reveal answers</summary>

1. **C** — Spring Framework is the foundation (IoC container, abstractions); Spring MVC is its web stack; Spring Boot is the convention/auto-config layer that wires both with defaults. They ship aligned but are distinct.
2. **B** — Constructor injection surfaces dependencies (an 8-arg constructor screams; 8 `@Autowired` fields whisper), makes the object constructible in a plain unit test, and `final` means fully initialized at construction. Field injection hides smells, which is the opposite of what you want.
3. **B** — `@ConditionalOnMissingBean` is precisely the "only if you didn't provide your own" guard. Define your own `ObjectMapper` bean and Boot's default steps aside. This is how auto-configuration stays overridable.
4. **B** — A successful create returns `201 Created` plus a `Location` header. `ResponseEntity.created(uri).body(dto)` does it. `200` loses the "created" semantics and the location; `204` would mean "no body," which a create should return.
5. **C** — The DTO is the contract. Separating it lets the domain become a JPA entity (Week 5) with lazy relationships and internal columns without any of that reaching the wire. Returning the entity directly couples your public API to your storage model.
6. **B** — Bean Validation does **not** run automatically on `@RequestBody`; you must add `@Valid`. Without it, the constraints are inert and the method runs with garbage input. This is the single most common Spring validation bug.
7. **C** — `ProblemDetail` is serialized as `application/problem+json` per RFC 9457. That media type is how a client knows the body is a structured problem, not a normal response.
8. **A** — `@WebMvcTest` is a slice: only the web layer, collaborators mocked, fast. `@SpringBootTest` boots the whole context and, with `RANDOM_PORT`, a real server. Use many slices and a few full-stack tests.
9. **C** — A missing resource is a `404`, returned as a `ProblemDetail`. The idiomatic path is the service throwing `NotFoundException`, caught by the global `@RestControllerAdvice`. A `500` would falsely blame the server; returning `null`/`200` lies about the outcome.
10. **B** — springdoc serves the OpenAPI 3.1 JSON at `/v3/api-docs` and Swagger UI at `/swagger-ui.html` by default. Both paths are configurable, but those are the defaults you'll hit.

</details>

---

If you scored under 7, re-read the lectures for the questions you missed — especially Q6 (the `@Valid` trap) and Q4/Q9 (status codes). If you scored 9 or 10, you're ready for the [homework](./homework.md).
