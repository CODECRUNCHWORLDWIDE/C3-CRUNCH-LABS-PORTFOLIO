# Lecture 1 — Spring Boot 3 from First Principles

> **Duration:** ~2 hours of reading + hands-on.
> **Outcome:** You can describe what Spring Boot 3 actually is — a dependency-injection container plus auto-configuration — without calling it magic. You can scaffold a Boot service, read every line of the generated `pom.xml` and `application.yml`, wire a bean with constructor injection, and trace a single HTTP request from the socket to your controller and back.

If you only remember one thing from this lecture, remember this:

> **Spring is a container. Spring Boot is sensible defaults for that container.** The container creates your objects ("beans"), hands each one its collaborators ("dependency injection"), and manages their lifecycle. Boot adds "if `spring-web` is on the classpath and you didn't configure a web server, start an embedded Tomcat on 8080." Nothing here is magic. It is plain Java run for you at startup, and you can override every default.

---

## 1. The three things people call "Spring"

Walk into a Java shop and ask "are you a Spring shop?" and you'll get an enthusiastic yes that papers over three distinct things.

| Layer | Example | What it is |
|------|---------|-----------|
| Core framework | **Spring Framework 6.2** | The IoC container, the web stack (Spring MVC), the transaction and data abstractions. The foundation. |
| Convention layer | **Spring Boot 3.5** | Auto-configuration, starters, an embedded server, `application.yml`, the `spring-boot-maven-plugin`. Boot *is not* a separate framework — it configures Spring for you. |
| Module ecosystem | **Spring Data, Spring Security, Spring Cloud** | Optional add-ons that each auto-configure when you add their starter. Spring Data JPA is Week 5; Spring Security is Week 6. |

The relationship matters because newcomers say "Spring Boot does X" when they mean "Spring Framework does X and Boot turned it on by default." When you hit a wall later — a bean that won't wire, a default you need to override — you fix it at the **Spring Framework** level, not the Boot level. Boot just chose the default; the machinery underneath is core Spring.

> **Why "Boot."** Before Boot (pre-2014), standing up a Spring web app meant hand-writing `web.xml`, a `dispatcher-servlet.xml`, a bean-definition XML file, picking a servlet container, and deploying a WAR to it. Boot collapsed all of that into "add a starter, write a `main` method, run a JAR." The name is "bootstrap" — it bootstraps a fully-wired Spring application from almost nothing. That is the entire value proposition: **convention over configuration.**

---

## 2. The IoC container, concretely

"Inversion of Control" sounds abstract; it is not. Here is the inversion, in code.

**Without a container** (the way you wrote Java in Weeks 1–3), an object creates its own collaborators:

```java
public class GoalService {
    private final GoalRepository repository = new InMemoryGoalRepository();
    // GoalService decides what implementation it gets. It is in control.
}
```

**With a container**, the object *declares what it needs* and the container provides it:

```java
@Service
public class GoalService {
    private final GoalRepository repository;

    public GoalService(GoalRepository repository) {  // "give me a GoalRepository"
        this.repository = repository;
    }
}
```

The control is *inverted*: `GoalService` no longer decides which `GoalRepository` it gets. The container — Spring's `ApplicationContext` — looks at the constructor, finds a bean that satisfies `GoalRepository`, and passes it in. That is **dependency injection**, the most common form of IoC.

Why does this matter?

1. **Testability.** In a JUnit test you construct `new GoalService(new FakeGoalRepository())` and pass a stub. The class never hard-codes its dependency, so you can swap it. This is exactly the pattern you set up in Week 3 with interfaces — Spring just automates the wiring in production.
2. **Swappability.** Next week the `GoalRepository` implementation changes from in-memory to JPA. `GoalService` does not change one line. The container wires the new implementation.
3. **Lifecycle.** The container creates each bean once (by default — singletons), wires them in dependency order, and tears them down at shutdown. You never call `new` on a service, and you never manage its lifetime.

The container is an object. You almost never touch it directly, but it is real: it is the `ApplicationContext` Boot creates in your `main` method.

---

## 3. Beans and the stereotype annotations

A **bean** is just an object the container manages. You tell the container "manage this class" with a stereotype annotation:

| Annotation | Means | Use it on |
|------------|-------|-----------|
| `@Component` | Generic managed bean | Anything you want the container to own |
| `@Service` | A component holding business logic | `GoalService`, `HabitService` |
| `@Repository` | A component that talks to a data store | `GoalRepository` implementations |
| `@RestController` | A component that handles HTTP and returns JSON | `GoalController` |
| `@Configuration` | A source of bean definitions (`@Bean` methods) | Wiring config |

`@Service`, `@Repository`, and `@RestController` are all `@Component` underneath with extra meaning (and, for `@Repository`, exception translation; for `@RestController`, `@ResponseBody` semantics). Use the most specific one — it documents intent and some tooling keys off it.

**Component scanning** is how the container finds them. Your main class:

```java
@SpringBootApplication
public class CrunchTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrunchTrackerApplication.class, args);
    }
}
```

`@SpringBootApplication` is three annotations in one: `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. The `@ComponentScan` part says "scan this package and everything below it for `@Component`-annotated classes and register them as beans." This is why **package layout matters**: put your main class at the root package (`com.crunchcrunch.tracker`) and everything under it gets scanned. A controller in a sibling package the main class can't see will silently not be registered.

---

## 4. Dependency injection: constructor injection and nothing else

There are three ways to inject a dependency. Only one is right.

**Constructor injection (do this):**

```java
@Service
public class GoalService {
    private final GoalRepository repository;
    private final Clock clock;

    public GoalService(GoalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }
}
```

Since Spring 4.3, if a bean has exactly one constructor you don't even need `@Autowired` — the container uses it automatically. Note the dependencies are `final`: once constructed, they cannot change. The object is fully initialized the moment it exists.

**Field injection (avoid):**

```java
@Service
public class GoalService {
    @Autowired
    private GoalRepository repository;  // smell
}
```

This works, but: the field can't be `final`, you cannot construct the object in a unit test without reflection, and you can hide an unbounded number of dependencies without the constructor screaming at you. A class with eight `@Autowired` fields looks fine; the same class with an eight-argument constructor looks like the problem it is. **Field injection hides design smells. Constructor injection surfaces them.** That is a feature.

**Setter injection (rare):** only for genuinely optional dependencies, which you almost never have.

> **C3 coding standard:** every Spring bean uses constructor injection with `final` fields. With Java 21 records and Lombok off the table for our domain, a hand-written constructor is fine; many teams use Lombok's `@RequiredArgsConstructor` to generate it. We write it by hand this week so you see exactly what's happening.

What if two beans satisfy the same interface? The container can't choose, and startup fails with `NoUniqueBeanDefinitionException`. You disambiguate with `@Primary` (pick a default) or `@Qualifier("name")` (pick by name) at the injection point. You'll hit this in Week 6 when you have two `UserDetailsService` candidates.

---

## 5. Auto-configuration: the heart of Boot

Here is the single idea that makes Boot *Boot*.

When the container starts, `@EnableAutoConfiguration` loads a long list of `*AutoConfiguration` classes shipped inside the Spring Boot JARs. Each one is guarded by `@Conditional` annotations that ask questions about your application:

```java
@AutoConfiguration
@ConditionalOnClass(DispatcherServlet.class)        // is Spring MVC on the classpath?
@ConditionalOnWebApplication(type = SERVLET)         // is this a servlet web app?
public class WebMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(HttpMessageConverters.class)  // only if YOU didn't define one
    public HttpMessageConverters messageConverters(...) {
        // sensible default Jackson + String converters
    }
}
```

Read those conditions:

- **`@ConditionalOnClass(DispatcherServlet.class)`** — only configure web MVC if the web classes are actually present. They are, because you added `spring-boot-starter-web`. If you hadn't, this whole block is skipped.
- **`@ConditionalOnMissingBean`** — only provide the default *if you haven't provided your own*. This is the override mechanism. Define your own `ObjectMapper` bean and Boot's default steps aside.

That is the whole trick. **Auto-configuration is hundreds of "if the classpath has X and you didn't define Y, wire a default Y."** Add `spring-boot-starter-web` and you get an embedded Tomcat, a `DispatcherServlet`, a Jackson `ObjectMapper`, content negotiation, and an error page — none of which you asked for explicitly, all of which you can override.

You can see exactly what fired. Run with `--debug` or set `debug=true` in `application.properties`, and Boot prints a **conditions report**: every auto-configuration, whether it matched, and why. When something doesn't wire the way you expect, this report is the first place you look.

---

## 6. Starters: curated dependency bundles

A **starter** is a `pom.xml` dependency that pulls in a coherent set of libraries so you don't assemble them yourself. `spring-boot-starter-web` is not code — it is a dependency-only POM whose transitive dependencies are Spring MVC, the embedded Tomcat, Jackson for JSON, and validation hooks, all at versions known to work together.

The starters you'll add this week:

| Starter | Pulls in | Why |
|---------|----------|-----|
| `spring-boot-starter-web` | Spring MVC, embedded Tomcat, Jackson | Build a REST API |
| `spring-boot-starter-validation` | Hibernate Validator (Jakarta Bean Validation) | `@Valid`, `@NotBlank`, `@Positive` |
| `spring-boot-starter-test` | JUnit 5, Mockito, AssertJ, Spring Test, `MockMvc` | Test the API (test scope) |
| `springdoc-openapi-starter-webmvc-ui` | springdoc + Swagger UI | Generate OpenAPI docs |

The version numbers are governed by the **Spring Boot BOM** (bill of materials), which the parent POM imports. You add `spring-boot-starter-web` with no `<version>` tag — the BOM decides the version, and it's guaranteed compatible with your Boot version. This is why you should never pin Spring library versions by hand; let the BOM do it.

---

## 7. Scaffold a real service from scratch

Let's make one. Use [start.spring.io](https://start.spring.io) in the browser, or generate it from the command line with `curl` so it's reproducible:

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
```

You now have:

```
crunch-tracker-api/
├── pom.xml
├── mvnw  mvnw.cmd            (the Maven wrapper — pins the Maven version)
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/crunchcrunch/tracker/
    │   │   └── CrunchTrackerApplication.java
    │   └── resources/
    │       ├── application.properties
    │       ├── static/        (static web assets)
    │       └── templates/     (server-rendered views — unused for a REST API)
    └── test/
        └── java/com/crunchcrunch/tracker/
            └── CrunchTrackerApplicationTests.java
```

Build and run with the wrapper (use `mvnw`, not a system `mvn`, so everyone is on the same Maven):

```bash
./mvnw spring-boot:run
```

You should see Boot's banner and then:

```
Started CrunchTrackerApplication in 1.732 seconds (process running for 2.011)
Tomcat started on port 8080 (http) with context path '/'
```

That's a running HTTP server. It serves nothing useful yet — there are no controllers — but the embedded Tomcat is live on 8080. `curl localhost:8080/` returns a default JSON error (a `404` as `ProblemDetail`, courtesy of auto-configuration). Stop it with `Ctrl+C`.

---

## 8. Reading the `pom.xml` line by line

Open `pom.xml`. Every element earns its place.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
</parent>
```

The **parent POM** imports the Boot BOM (so starters need no version), sets Java compiler defaults, and configures the `spring-boot-maven-plugin`. It is why your `pom.xml` is short.

```xml
<properties>
    <java.version>21</java.version>
</properties>
```

The Java version. The parent reads this and configures the compiler for `--release 21`.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Note: **no `<version>` tags.** The BOM supplies them. `spring-boot-starter-test` is `test` scope — it's on the classpath only when compiling and running tests, never shipped in the JAR.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

The Boot plugin gives you `./mvnw spring-boot:run` and, crucially, repackages your JAR into an **executable fat JAR** at `./mvnw package` — a single `target/crunch-tracker-api-0.0.1-SNAPSHOT.jar` that contains your code, all dependencies, and the embedded Tomcat. You ship that one file and run it with `java -jar`. That is the deployment artifact Week 10 puts in a container.

To add springdoc later:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

This one *does* need a version — it's not a Boot-managed dependency, so the BOM doesn't know it. Check springdoc.org for the version current with your Boot release.

---

## 9. Configuration: `application.yml` and the property hierarchy

Boot reads `application.properties` (or `application.yml`) from `src/main/resources`. The starter generates `.properties`; most teams rename to `.yml` for nesting. Either works. Convert and add a couple of values:

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: crunch-tracker-api

server:
  port: 8080

logging:
  level:
    org.springframework.web: INFO
    com.crunchcrunch.tracker: DEBUG
```

Every Spring component reads its configuration from here through `@ConfigurationProperties` or `@Value`. You almost never hard-code a port, a URL, or a feature flag — you put it here and override it per environment.

The override hierarchy (highest priority wins) is worth knowing now because it bites people in Week 10:

1. Command-line arguments (`--server.port=9000`).
2. Environment variables (`SERVER_PORT=9000`). Boot maps `SERVER_PORT` → `server.port` automatically (relaxed binding).
3. Profile-specific files (`application-prod.yml` when the `prod` profile is active).
4. `application.yml`.

This is why your container in Week 10 sets `SPRING_DATASOURCE_URL` as an env var and it Just Works without touching the YAML: env vars override the file. Internalize the order; you'll lean on it.

**Profiles** let you have different config per environment. `application-test.yml` is read only when the `test` profile is active. You'll use `@ActiveProfiles("test")` on integration tests in Week 5.

---

## 10. The request lifecycle: from socket to controller and back

When `curl localhost:8080/api/goals` hits your running app, here is the path the request walks. Knowing it turns "why did it 404?" from a mystery into a checklist.

```
HTTP request
     │
     ▼
Embedded Tomcat (the servlet container, listening on 8080)
     │
     ▼
DispatcherServlet  ── the front controller; every request goes through it
     │
     ▼
HandlerMapping  ── matches the URL + verb to a @RequestMapping method
     │   (GET /api/goals → GoalController.list())
     ▼
Your @RestController method runs
     │   returns a Java object (List<GoalResponse>)
     ▼
HttpMessageConverter (Jackson) ── serializes the object to JSON
     │
     ▼
HTTP response (200, Content-Type: application/json, the JSON body)
```

Each stage is a place a request can go wrong:

- **404 "No mapping"** — `HandlerMapping` found no method for that URL+verb. Check your `@RequestMapping` path and that the controller is in a scanned package.
- **415 Unsupported Media Type** — you `POST`ed without `Content-Type: application/json`, so the converter refused the body.
- **406 Not Acceptable** — the client's `Accept` header asks for a format you can't produce.
- **500** — your controller method threw something nobody handled. (After this week, that should almost never happen for client mistakes — Lecture 2 makes you handle them deliberately.)

You can watch this happen. Set `logging.level.org.springframework.web=DEBUG` and curl an endpoint; the log shows the `DispatcherServlet` receiving the request, the mapping it chose, and the converter it used. The first time a request 404s and you read the log and see "no handler," the framework stops being a black box.

---

## 11. A first endpoint, end to end

Enough scaffolding. Write the smallest real controller so you've seen the whole loop once:

```java
package com.crunchcrunch.tracker.web;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "crunch-tracker-api",
            "time", Instant.now().toString()
        );
    }
}
```

Run, then:

```bash
$ curl -i localhost:8080/api/health
HTTP/1.1 200 OK
Content-Type: application/json

{"status":"UP","service":"crunch-tracker-api","time":"2026-06-12T14:03:11.882Z"}
```

What happened: component scanning found `HealthController` (it's under the main package), registered it as a bean, the `HandlerMapping` mapped `GET /api/health` to `health()`, the method returned a `Map`, and Jackson serialized it to JSON with a `200`. You wrote nine lines and got a working JSON endpoint. *That* is what Boot's defaults bought you.

> **Don't return `Map` in real code.** We did it here for the smallest possible example. Returning an unstructured `Map` gives you no compile-time shape, no OpenAPI schema, and no contract. From Lecture 2 on, every response is a typed **DTO record**. The `Map` was training wheels; take them off now.

> **Health, for free.** Boot's Actuator (`spring-boot-starter-actuator`) gives you a production-grade `/actuator/health` endpoint without writing the controller above. We wrote our own once to see the loop; in the mini-project you may add Actuator instead. Either is fine this week.

---

## 12. What's *not* in Week 4

- **No database.** Everything is backed by the in-memory repository from Week 3. JPA + Postgres is Week 5.
- **No authentication.** Every endpoint is wide open. Spring Security + JWT is Week 6. Don't add a login this week; you'd be guessing at next week's design.
- **No reactive stack.** We use blocking Spring MVC (servlet, Tomcat), not WebFlux. WebFlux is a different model you don't need for this course.
- **No microservices, no Spring Cloud.** One service, one JAR. Resist the urge.

Week 4 is "put an honest HTTP surface on the domain you already built and tested." That's plenty.

---

## 13. Recap

You should now be able to:

- State what Spring Framework, Spring Boot, and the Spring module ecosystem each are without conflating them.
- Explain dependency injection and why constructor injection with `final` fields is the standard.
- Describe auto-configuration as "conditional default beans you can override," and find the conditions report with `--debug`.
- Scaffold a Boot 3.5 / Java 21 service from `start.spring.io` and read its `pom.xml` and `application.yml`.
- Trace an HTTP request from Tomcat through the `DispatcherServlet` to a controller and back through Jackson.
- Write a one-method `@RestController` and curl it.

Next up: the HTTP surface done properly — controllers, DTOs, validation, status codes, `ProblemDetail`, and OpenAPI. Continue to [Lecture 2 — A Clean HTTP Surface](./02-clean-http-surface.md).

---

## References

- *Spring Boot reference* — <https://docs.spring.io/spring-boot/index.html>
- *The IoC container* — <https://docs.spring.io/spring-framework/reference/core/beans.html>
- *Auto-configuration* — <https://docs.spring.io/spring-boot/reference/using/auto-configuration.html>
- *Spring Web MVC* — <https://docs.spring.io/spring-framework/reference/web/webmvc.html>
- *Externalized configuration* — <https://docs.spring.io/spring-boot/reference/features/external-config.html>
- *Spring Initializr* — <https://start.spring.io>
