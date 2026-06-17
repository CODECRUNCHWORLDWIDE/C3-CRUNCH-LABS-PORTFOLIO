# Week 3 — Resources

Every resource on this page is **free**. The official Java docs are free. JUnit, AssertJ, and Mockito are open-source projects with open documentation. No paywalled books are linked. If a link 404s, open an issue so we can replace it.

## Required reading (work it into your week)

- **The Java Tutorials — Interfaces** — what an interface is and how `default` methods work:
  <https://dev.java/learn/interfaces/>
- **The Java Tutorials — Collections** — the canonical tour of `List`/`Set`/`Map`/`Queue`:
  <https://dev.java/learn/api/collections-framework/>
- **`Object.equals` and `Object.hashCode` Javadoc** — read the *contract* paragraphs, not just the signatures:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#equals(java.lang.Object)>
- **JUnit 5 User Guide** — the writing-tests and assertions sections:
  <https://docs.junit.org/current/user-guide/>
- **AssertJ Core documentation** — fluent assertions you'll use every day:
  <https://assertj.github.io/doc/>
- **Mockito documentation (the class Javadoc is the real manual)**:
  <https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html>

## OOP and design

- **Composition over inheritance** — a clear, language-neutral explanation:
  <https://en.wikipedia.org/wiki/Composition_over_inheritance>
- **"Design Patterns: Elements of Reusable Software" — the GoF principle** — secondhand summary of "favor object composition over class inheritance":
  <https://refactoring.guru/design-patterns/composition-over-inheritance>
- **The Liskov Substitution Principle**, plainly explained — why a subclass must honor its parent's contract:
  <https://en.wikipedia.org/wiki/Liskov_substitution_principle>
- **Sealed classes and interfaces (JEP 409)** — the feature you met in week 2, applied to interface design here:
  <https://openjdk.org/jeps/409>

## Collections — the deep references

- **`java.util` package summary** — the index of every collection type, kept current per release:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/package-summary.html>
- **`List` Javadoc** — note `List.of`, `List.copyOf`, and the immutability rules:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html>
- **`Map` Javadoc** — `computeIfAbsent`, `merge`, `getOrDefault` are the ones you'll actually reach for:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html>
- **`Deque` Javadoc** — why `ArrayDeque` is the modern stack/queue, not `Stack` or `LinkedList`:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Deque.html>
- **Big-O cheat sheet for Java collections** (community reference, sanity-check against the Javadoc):
  <https://www.bigocheatsheet.com/>

## Streams (we use them lightly this week)

- **The Stream API tutorial** — `filter`/`map`/`collect`/`groupingBy`:
  <https://dev.java/learn/api/streams/>
- **`Collectors` Javadoc** — `groupingBy`, `toMap`, `partitioningBy`, `counting`:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Collectors.html>

## Testing — JUnit 5

- **JUnit 5 User Guide** (bookmark this; it's the whole manual):
  <https://docs.junit.org/current/user-guide/>
- **Writing Tests** — `@Test`, `@DisplayName`, `@BeforeEach`, `@Nested`:
  <https://docs.junit.org/current/user-guide/#writing-tests>
- **Parameterized tests** — `@ParameterizedTest`, `@ValueSource`, `@CsvSource`, `@MethodSource`:
  <https://docs.junit.org/current/user-guide/#writing-tests-parameterized-tests>

## Testing — AssertJ

- **AssertJ Core** — the entry point and quickstart:
  <https://assertj.github.io/doc/#assertj-core>
- **AssertJ assertion guide** — collections, exceptions, `extracting`, `satisfies`:
  <https://assertj.github.io/doc/#assertj-core-assertions-guide>

## Testing — Mockito

- **Mockito main docs (Javadoc)** — `mock`, `when`/`thenReturn`, `verify`, `ArgumentCaptor`:
  <https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html>
- **`mockito-junit-jupiter`** — the `@ExtendWith(MockitoExtension.class)` integration:
  <https://github.com/mockito/mockito/wiki/Using-with-JUnit-5>
- **Martin Fowler — "Mocks Aren't Stubs"** — the essay that gives you the vocabulary:
  <https://martinfowler.com/articles/mocksArentStubs.html>

## Coverage

- **JaCoCo** — the coverage tool we wire into Maven:
  <https://www.eclemma.org/jacoco/>
- **JaCoCo Maven plugin usage**:
  <https://www.jacoco.org/jacoco/trunk/doc/maven.html>

## Maven (you set this up in week 1)

- **Maven Surefire plugin** — runs your unit tests on `mvn test`:
  <https://maven.apache.org/surefire/maven-surefire-plugin/>
- **Maven dependency scopes** — why JUnit/AssertJ/Mockito are `<scope>test</scope>`:
  <https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html>

## Books — free chapters and references

- **"Effective Java," 3rd ed. — items most relevant this week** (read these specific items; many are summarized free online):
  - Item 17: Minimize mutability
  - Item 18: Favor composition over inheritance
  - Item 10/11: Obey the `equals`/`hashCode` contracts
  - Item 64: Refer to objects by their interfaces
  A solid free summary index: <https://github.com/HugoMatilla/Effective-JAVA-Summary>
- **"Test-Driven Development by Example" (Kent Beck) — the canonical TDD book**; the red-green-refactor loop comes straight from it. (Library / not linked; the *idea* is free — see the next link.)
- **Martin Fowler — "Test Pyramid"** — where unit, slice, and end-to-end tests sit:
  <https://martinfowler.com/articles/practical-test-pyramid.html>

## Open-source projects to read this week

You learn more from one hour reading a well-tested Java codebase than from three hours of tutorials. Pick one and scroll through its `src/test/java`:

- **`spring-projects/spring-petclinic`** — the canonical well-tested Spring sample; read it *before* week 4:
  <https://github.com/spring-projects/spring-petclinic>
- **`assertj/assertj`** — AssertJ tests itself with AssertJ; great assertion examples:
  <https://github.com/assertj/assertj>
- **`junit-team/junit5`** — the framework's own tests are a masterclass in JUnit 5 features:
  <https://github.com/junit-team/junit5>

## Tools you'll use this week

- **JDK 21** — `java --version` should print `21.x`. Installed in week 1 via SDKMAN.
- **Maven** — `mvn --version`. The `mvn test` and `mvn verify` lifecycle phases are your daily drivers.
- **A JUnit-aware editor** — IntelliJ IDEA Community (free) or VS Code with the Java extension pack. Neither is required; everything runs from `mvn`.

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Interface** | A contract: method signatures with no (or `default`) implementation. |
| **Composition** | Building behavior by *holding* other objects (a field) rather than *extending* them. |
| **Inheritance** | `extends`: subtyping that inherits implementation. Powerful, easy to misuse. |
| **`equals`/`hashCode` contract** | Equal objects must have equal hash codes; the rule `HashMap`/`HashSet` depend on. |
| **JUnit 5 (Jupiter)** | The default Java test framework in 2026. `@Test`, `@BeforeEach`, etc. |
| **AssertJ** | Fluent assertion library: `assertThat(x).isEqualTo(y)`. Better failures than JUnit's `assertEquals`. |
| **Mockito** | A mocking library: create fake collaborators, stub their answers, verify calls. |
| **Stub** | A test double that returns canned answers. |
| **Mock** | A test double whose *interactions* you assert on (`verify`). |
| **Fake** | A working but simplified implementation (e.g. an in-memory repository). |
| **TDD** | Test-Driven Development: write the failing test first, then the code. |
| **Red-green-refactor** | The TDD loop: failing test → minimal pass → clean up. |
| **Repository** | An interface that hides where/how data is stored. |
| **JaCoCo** | The coverage tool. Reports which lines/branches your tests exercised. |
| **SUT** | System Under Test — the thing a test is actually testing. |

---

*If a link 404s, please open an issue so we can replace it.*
