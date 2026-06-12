# Week 5 — Resources

Every resource on this page is **free**. The Spring docs, Hibernate User Guide, PostgreSQL manual, and Testcontainers docs are all open and free without an account. No paywalled books are linked. Where a community author (Vlad Mihalcea, Baeldung) is the clearest source, we link the free article, not the paid course.

## Required reading (work it into your week)

- **Spring Data JPA reference** — the canonical guide to repositories, derived queries, and `@Query`:
  <https://docs.spring.io/spring-data/jpa/reference/>
- **Accessing Data with JPA** — the official Spring guide, start-to-finish in 30 minutes:
  <https://spring.io/guides/gs/accessing-data-jpa>
- **Hibernate ORM User Guide** — the entity-mapping and fetching sections especially:
  <https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html>
- **Flyway: Getting Started** — versioned migrations, naming, the schema history table:
  <https://documentation.red-gate.com/fd/quickstart-how-flyway-works-184127223.html>
- **Spring Boot Testcontainers support** — `@ServiceConnection` and the integration story:
  <https://docs.spring.io/spring-boot/reference/testing/testcontainers.html>

## The N+1 problem (read at least one)

The N+1 select problem is the single most common JPA performance bug. Internalize it once.

- **Vlad Mihalcea — N+1 query problem**: <https://vladmihalcea.com/n-plus-1-query-problem/>
- **Baeldung — Hibernate `JOIN FETCH`**: <https://www.baeldung.com/jpa-join-types>
- **Spring Data — `@EntityGraph`**: <https://docs.spring.io/spring-data/jpa/reference/jpa/entity-graph.html>

## Official docs

- **PostgreSQL 16 documentation** — the manual; you'll dip into `CREATE TABLE`, types, and `EXPLAIN`:
  <https://www.postgresql.org/docs/16/index.html>
- **PostgreSQL data types** — pick the right column type (`uuid`, `timestamptz`, `numeric`):
  <https://www.postgresql.org/docs/16/datatype.html>
- **Spring Boot — SQL databases**: <https://docs.spring.io/spring-boot/reference/data/sql.html>
- **Spring Boot — Docker Compose support** (`spring-boot-docker-compose`):
  <https://docs.spring.io/spring-boot/reference/features/dev-services.html>
- **Jakarta Persistence (JPA) 3.1 specification** — the normative reference for the annotations:
  <https://jakarta.ee/specifications/persistence/3.1/>
- **Flyway documentation home**: <https://documentation.red-gate.com/fd/>

## Tools you'll use this week

- **Docker Desktop / Colima / Podman** — a container runtime. Verify with `docker run --rm hello-world`.
  - Docker Desktop: <https://www.docker.com/products/docker-desktop/>
  - Colima (lightweight macOS/Linux alternative): <https://github.com/abiosoft/colima>
- **`psql`** — the Postgres CLI, ships with the Postgres client tools. Connect with `psql "postgresql://crunch:crunch@localhost:5432/crunch_tracker"`.
- **Testcontainers for Java** — throwaway Docker containers from tests: <https://java.testcontainers.org/>
- **`./mvnw`** — the Maven wrapper checked into your repo; no global Maven install needed.

## Libraries we touch this week

- **`spring-boot-starter-data-jpa`** — Spring Data JPA + Hibernate + the JPA API + the JDBC/transaction plumbing:
  <https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.jpa-and-spring-data>
- **`org.postgresql:postgresql`** — the PostgreSQL JDBC driver:
  <https://jdbc.postgresql.org/>
- **`org.flywaydb:flyway-core` + `flyway-database-postgresql`** — migrations run automatically on boot:
  <https://documentation.red-gate.com/fd/spring-boot-184127627.html>
- **`org.testcontainers:postgresql`** + **`spring-boot-testcontainers`** — integration tests against real Postgres:
  <https://java.testcontainers.org/modules/databases/postgres/>

## Deep dives (skim, don't memorize)

- **Vlad Mihalcea — A beginner's guide to the JPA persistence context** (the entity lifecycle, explained well):
  <https://vladmihalcea.com/jpa-persistence-context/>
- **Vlad Mihalcea — The best way to map a `@OneToMany`**:
  <https://vladmihalcea.com/the-best-way-to-map-a-onetomany-relationship-with-jpa-and-hibernate/>
- **Thorben Janssen — FetchType.LAZY vs EAGER**:
  <https://thorben-janssen.com/entity-mappings-introduction-jpa-fetchtypes/>
- **Spring — Understanding `@Transactional`**:
  <https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html>

## Videos (free, no signup)

- **"Spring Data JPA — The good parts"** — Dan Vega (official Spring Developer Advocate) channel:
  <https://www.youtube.com/@danvega>
- **"Testcontainers in 100 seconds" / longer talks** — the official Testcontainers channel reposts conference talks:
  <https://www.youtube.com/@TestcontainersOfficial>
- **"What is Flyway?"** — Redgate's official channel; short and accurate:
  <https://www.youtube.com/@flywaydb>

## Open-source projects to read this week

You learn more from one hour reading a well-mapped Spring Data project than from three tutorials.

- **`spring-projects/spring-data-jpa`** — the abstraction itself; the `query` package is instructive:
  <https://github.com/spring-projects/spring-data-jpa>
- **`spring-projects/spring-petclinic`** — the canonical Spring sample; JPA entities and repositories done plainly:
  <https://github.com/spring-projects/spring-petclinic>
- **`testcontainers/testcontainers-java`** — read the `postgresql` module:
  <https://github.com/testcontainers/testcontainers-java>

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **JPA** | Jakarta Persistence API — the *specification*: the annotations and the `EntityManager` contract. |
| **Hibernate** | The most common *implementation* of JPA. It generates the SQL. The default in Spring Boot. |
| **Spring Data JPA** | A layer on top of JPA that generates repository implementations from interfaces. |
| **Entity** | A Java class mapped to a database table, annotated `@Entity`. |
| **Persistence context** | Hibernate's per-transaction cache of managed entities. The "first-level cache." |
| **Managed / detached / transient** | Lifecycle states of an entity relative to the persistence context. |
| **Dirty checking** | Hibernate auto-detects changes to managed entities and writes them at flush — no explicit `save`. |
| **N+1** | A bug where loading N rows fires 1 query for the list + N queries for each row's association. |
| **`JOIN FETCH`** | A JPQL clause that loads an association in the same query — the usual N+1 fix. |
| **Flyway migration** | A versioned `.sql` file (`V1__init.sql`) that evolves the schema, run once and recorded. |
| **`flyway_schema_history`** | The table Flyway uses to track which migrations have run. Never edit it by hand. |
| **Testcontainers** | A library that spins up throwaway Docker containers (e.g. Postgres) for tests. |
| **`@ServiceConnection`** | Spring Boot 3.1+ annotation that auto-wires a Testcontainer to the app's datasource. |
| **DDL** | Data Definition Language — `CREATE TABLE`, `ALTER TABLE`. The shape of the schema. |
| **`ddl-auto`** | The Hibernate setting that can auto-generate DDL. We set it to `validate` and let Flyway own DDL. |

---

*If a link 404s, please open an issue so we can replace it.*
