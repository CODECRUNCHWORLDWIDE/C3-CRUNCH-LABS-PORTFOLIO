# Week 4 — Resources

Every resource on this page is **free**. The Spring docs are open and free. The Jakarta EE specs are published openly. The RFCs are public. springdoc-openapi is MIT-licensed and the source is on GitHub. No paywalled books are linked.

A note on versions: as of mid-2026 the current line is **Spring Boot 3.5.x on Spring Framework 6.2 and Java 21**. Spring Boot 4.0 is in the pipeline but 3.5 is the stable LTS-grade line every example here uses. Always read the docs for the version in your `pom.xml`; the URLs below resolve to the current docs.

## Required reading (work it into your week)

- **Spring Boot reference documentation** — the canonical source; bookmark it:
  <https://docs.spring.io/spring-boot/index.html>
- **"Building a RESTful Web Service"** — the official getting-started guide; do it once even if it feels basic:
  <https://spring.io/guides/gs/rest-service>
- **Spring Web MVC reference** — controllers, mapping, the request lifecycle:
  <https://docs.spring.io/spring-framework/reference/web/webmvc.html>
- **RFC 9457 — Problem Details for HTTP APIs** — short, readable, and the format we emit for every error:
  <https://www.rfc-editor.org/rfc/rfc9457>
- **Jakarta Bean Validation specification** (the constraints you'll use):
  <https://jakarta.ee/specifications/bean-validation/3.1/>

## The "core concepts" docs (read once, refer back often)

- **The IoC container** — beans, the `ApplicationContext`, the lifecycle:
  <https://docs.spring.io/spring-framework/reference/core/beans.html>
- **Dependency injection** — constructor vs setter vs field, and why constructor wins:
  <https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html>
- **Auto-configuration** — how Boot decides what to wire:
  <https://docs.spring.io/spring-boot/reference/using/auto-configuration.html>
- **Externalized configuration** — the `application.yml`/properties hierarchy and `@ConfigurationProperties`:
  <https://docs.spring.io/spring-boot/reference/features/external-config.html>

## Validation and error handling

- **Validation in Spring** — `@Valid`, `@Validated`, the `Validator` SPI:
  <https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html>
- **Error handling for REST with `@RestControllerAdvice`** — Spring's guide to global handlers:
  <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html>
- **`ProblemDetail` Javadoc** — the class Spring gives you for RFC 9457:
  <https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ProblemDetail.html>
- **Hibernate Validator reference** — the reference implementation of Bean Validation; how to write a custom constraint:
  <https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/>

## OpenAPI / Swagger

- **springdoc-openapi** — the library that generates OpenAPI 3 + Swagger UI from your controllers:
  <https://springdoc.org/>
- **OpenAPI Specification 3.1** — the spec your `/v3/api-docs` conforms to:
  <https://spec.openapis.org/oas/v3.1.0>
- **Swagger UI** — the interactive docs page springdoc serves at `/swagger-ui.html`:
  <https://swagger.io/tools/swagger-ui/>

## Tools you'll use this week

- **start.spring.io (Spring Initializr)** — generate a project skeleton in your browser or via `curl`:
  <https://start.spring.io>
- **`curl`** — preinstalled on macOS and Linux; available on Windows. Use `-i` to see status codes.
- **HTTPie** — a friendlier HTTP client; `http POST :8080/api/goals title=Run`:
  <https://httpie.io/>
- **`.http` files** — IntelliJ HTTP Client / VS Code REST Client format; check your requests into the repo:
  <https://www.jetbrains.com/help/idea/exploring-http-syntax.html>
- **jq** — pretty-print and filter JSON responses on the command line:
  <https://jqlang.github.io/jq/>

## Testing

- **Testing Spring Boot applications** — `@SpringBootTest`, slices, test config:
  <https://docs.spring.io/spring-boot/reference/testing/index.html>
- **`@WebMvcTest` and `MockMvc`** — the controller-slice test you'll write most:
  <https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html#testing.spring-boot-applications.spring-mvc-tests>
- **AssertJ** — the fluent assertions you adopted in Week 3, used here too:
  <https://assertj.github.io/doc/>

## Free, long-form learning

- **"Spring Boot Tutorial"** — Baeldung's index; individual articles are free and current:
  <https://www.baeldung.com/spring-boot>
- **Spring Academy (free tier)** — VMware's official courses; the "Building a REST API with Spring Boot" path is free:
  <https://spring.academy/courses>
- **Dan Vega — Spring Developer Advocate** — free, current YouTube channel that tracks each Boot release:
  <https://www.youtube.com/@DanVega>

## Open-source projects to read this week

You learn more from one hour reading a well-built Spring app than from three hours of tutorials. Pick one and scroll:

- **spring-projects/spring-petclinic** — the canonical reference app; read the controllers and DTOs:
  <https://github.com/spring-projects/spring-petclinic>
- **spring-projects/spring-boot** — the framework itself; open one `*AutoConfiguration.java`:
  <https://github.com/spring-projects/spring-boot>
- **springdoc/springdoc-openapi** — how the OpenAPI doc gets generated from your annotations:
  <https://github.com/springdoc/springdoc-openapi>

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Spring Framework 6** | The core: the IoC container, the web stack, the abstractions. The foundation everything else builds on. |
| **Spring Boot 3.5** | The opinionated layer that auto-configures Spring with sensible defaults and an embedded server. |
| **Bean** | An object the Spring container creates, configures, and manages the lifecycle of. |
| **IoC container** | Inversion-of-Control container; the `ApplicationContext` that holds and wires your beans. |
| **DI** | Dependency Injection — the container hands a bean its collaborators instead of the bean `new`-ing them. |
| **Starter** | A curated dependency bundle, e.g. `spring-boot-starter-web` pulls in MVC + Tomcat + Jackson. |
| **Auto-configuration** | Boot's "if X is on the classpath and you haven't defined a bean, wire a sensible default." |
| **`DispatcherServlet`** | The front controller that routes every HTTP request to the right handler method. |
| **DTO** | Data Transfer Object — the record that defines the *wire* shape, separate from the domain entity. |
| **Jackson** | The JSON library Boot uses to (de)serialize request/response bodies. |
| **Bean Validation** | Jakarta's annotation-driven validation (`@NotBlank`, `@Positive`); Hibernate Validator implements it. |
| **`ProblemDetail`** | Spring's class for RFC 9457 machine-readable error bodies (`application/problem+json`). |
| **`@RestControllerAdvice`** | A class that handles exceptions across all controllers and turns them into HTTP responses. |
| **springdoc-openapi** | The library that reads your controllers and generates an OpenAPI 3 doc + Swagger UI. |
| **`MockMvc`** | A test harness that calls your controllers without starting a real server. |

---

*If a link 404s, please open an issue so we can replace it.*
