# Exercise 1 — Bootstrap a Service

**Goal:** Scaffold a real Spring Boot 3.5 / Java 21 service from a blank folder, read every generated file so nothing is mystery, drop in your Week 3 domain, add a typed health endpoint, and curl it. No IDE wizards strictly required — just you, the Spring Initializr, and the Maven wrapper.

**Estimated time:** 40 minutes.

---

## Setup

You need JDK 21 and `curl`. Verify:

```bash
java -version    # should print 21.x
curl --version
```

If `java -version` is not 21, set it with SDKMAN (`sdk use java 21...`) as you learned in Week 1.

---

## Step 1 — Generate the project

Generate from the Spring Initializr so it's reproducible:

```bash
curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.5.0 \
  -d javaVersion=21 \
  -d groupId=com.crunchcrunch \
  -d artifactId=crunch-tracker-api \
  -d packageName=com.crunchcrunch.tracker \
  -d dependencies=web,validation \
  -o crunch-tracker-api.zip

unzip crunch-tracker-api.zip -d crunch-tracker-api
cd crunch-tracker-api
git init && git add . && git commit -m "Initial Spring Boot skeleton"
```

(Or use the web UI at <https://start.spring.io> with the same choices — Maven, Java 21, Boot 3.5, dependencies: Spring Web + Validation.)

---

## Step 2 — Read every generated file

Before you write a line, understand what you got. Open each and answer the question:

- **`pom.xml`** — Which starters are listed? What's the parent? Why do the starters have no `<version>`? *(Answer: the parent POM imports the Boot BOM, which supplies versions.)*
- **`src/main/java/com/crunchcrunch/tracker/CrunchTrackerApplication.java`** — What three annotations does `@SpringBootApplication` combine? *(`@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`.)*
- **`src/main/resources/application.properties`** — It's nearly empty. Rename it to `application.yml`.
- **`mvnw` / `mvnw.cmd`** — The Maven wrapper. Why use `./mvnw` instead of a system `mvn`? *(Pins the Maven version for everyone.)*

Convert the properties file to YAML and add some config:

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: crunch-tracker-api
server:
  port: 8080
logging:
  level:
    com.crunchcrunch.tracker: DEBUG
```

Delete the now-empty `application.properties`.

---

## Step 3 — First run

```bash
./mvnw spring-boot:run
```

You should see the banner and:

```
Started CrunchTrackerApplication in 1.7 seconds (process running for 2.0)
Tomcat started on port 8080 (http) with context path '/'
```

In another terminal, confirm there are no endpoints yet:

```bash
$ curl -i localhost:8080/api/health
HTTP/1.1 404
Content-Type: application/problem+json
```

A `404` returned as `application/problem+json` — Boot's auto-configured error handling already speaks RFC 9457. Stop the app with `Ctrl+C`.

---

## Step 4 — Drop in the Week 3 domain

Copy your Week 3 domain into `src/main/java/com/crunchcrunch/tracker/domain/`:

```
domain/
├── Goal.java                      (the record)
├── Habit.java                     (the record)
├── GoalService.java               (@Service — add the annotation)
├── HabitService.java              (@Service)
├── GoalRepository.java            (interface)
├── HabitRepository.java           (interface)
├── InMemoryGoalRepository.java    (@Repository — add the annotation)
├── InMemoryHabitRepository.java   (@Repository)
└── NotFoundException.java         (a RuntimeException; create it if you didn't have one)
```

Two changes from pure Week 3 code:

1. Annotate the services with `@Service` and the in-memory repositories with `@Repository` so the container manages them.
2. Make sure `GoalService` and `HabitService` use **constructor injection** of their repository (they almost certainly already do — that was the Week 3 design).

If your Week 3 services threw a custom "not found" exception, keep it. If they returned `Optional`, add a small `NotFoundException extends RuntimeException` now; the controller weeks lean on throwing it.

```bash
./mvnw compile   # should compile clean; the container will wire these next exercise
git add . && git commit -m "Add Week 3 domain as Spring-managed beans"
```

---

## Step 5 — Add a typed health endpoint

Create `src/main/java/com/crunchcrunch/tracker/web/HealthController.java`. **Return a record, not a `Map`** — we want a typed contract from the very first endpoint:

```java
package com.crunchcrunch.tracker.web;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    public record HealthResponse(String status, String service, Instant time) {}

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "crunch-tracker-api", Instant.now());
    }
}
```

Run and curl:

```bash
./mvnw spring-boot:run
# in another terminal:
curl -i localhost:8080/api/health
```

Expect:

```
HTTP/1.1 200
Content-Type: application/json

{"status":"UP","service":"crunch-tracker-api","time":"2026-06-12T14:03:11.882Z"}
```

---

## Step 6 — A first slice test

Create `src/test/java/com/crunchcrunch/tracker/web/HealthControllerTest.java`:

```java
package com.crunchcrunch.tracker.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void health_returns_up() throws Exception {
        mvc.perform(get("/api/health"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"))
           .andExpect(jsonPath("$.service").value("crunch-tracker-api"));
    }
}
```

```bash
./mvnw test
```

You should see one passing test plus the default context-load test. Commit:

```bash
git add . && git commit -m "Typed health endpoint + slice test"
```

---

## Acceptance criteria

You can mark this exercise done when:

- [ ] You have a `crunch-tracker-api/` project that builds with `./mvnw compile`.
- [ ] `./mvnw spring-boot:run` starts and prints "Tomcat started on port 8080."
- [ ] `curl -i localhost:8080/api/health` returns `200` with a typed JSON body (status/service/time).
- [ ] The Week 3 domain is present under `domain/`, annotated with `@Service`/`@Repository`, and compiles.
- [ ] `./mvnw test` is green, including the `HealthController` slice test.
- [ ] You have at least 3 Git commits with sensible messages.
- [ ] You can explain, in your own words, what `pom.xml`, `application.yml`, `@SpringBootApplication`, and `mvnw` are for.

---

## Stretch

- Add `spring-boot-starter-actuator` and visit `/actuator/health`. Compare Boot's built-in health to your hand-written one. Which would you ship?
- Turn on `logging.level.org.springframework.web=DEBUG`, curl `/api/health`, and find in the log where the `DispatcherServlet` maps the request to `HealthController.health()`.
- Run `./mvnw package` and then `java -jar target/crunch-tracker-api-0.0.1-SNAPSHOT.jar`. You just ran the fat JAR — the exact artifact Week 10 deploys.

---

## Hints

<details>
<summary>If <code>./mvnw</code> says "permission denied"</summary>

Make it executable: `chmod +x mvnw`. On Windows, use `mvnw.cmd`.

</details>

<details>
<summary>If the app won't start: "Field ... required a bean of type ... that could not be found"</summary>

Your service's dependency isn't a managed bean. Check that the in-memory repository has `@Repository` and that it lives **under** the main package (`com.crunchcrunch.tracker.*`) so component scanning finds it. A class outside the scanned package is invisible to the container.

</details>

<details>
<summary>If the slice test can't find <code>MockMvc</code></summary>

`@WebMvcTest` auto-configures `MockMvc` — make sure you imported `org.springframework.test.web.servlet.MockMvc` and annotated the class with `@WebMvcTest(HealthController.class)`, not plain `@SpringBootTest`.

</details>

---

When this exercise feels comfortable, move to [Exercise 2 — Goal controller](exercise-02-goal-controller.java).
