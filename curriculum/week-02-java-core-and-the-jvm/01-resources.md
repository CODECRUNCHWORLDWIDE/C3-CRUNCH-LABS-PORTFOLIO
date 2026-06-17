# Week 2 — Resources

Every resource on this page is **free**. The OpenJDK documentation, the JDK API docs, and the JEPs are published openly. Open-source repos are public on GitHub. No paywalled books are linked. Where a vendor (Oracle, Eclipse Adoptium, Red Hat) hosts the canonical version, we link the vendor's free copy.

## Required reading (work it into your week)

- **The Java Tutorials — "Learning the Java Language"** — Oracle's canonical language overview. Read the "Classes and Objects" and "Generics" trails; skim the rest:
  <https://docs.oracle.com/javase/tutorial/java/index.html>
- **JEP 441: Pattern Matching for switch** — finalized in Java 21. This is *the* feature of the week; read the "Motivation" and "Description":
  <https://openjdk.org/jeps/441>
- **JEP 440: Record Patterns** — record deconstruction in `switch`, finalized in Java 21:
  <https://openjdk.org/jeps/440>
- **`record` reference** — the language spec section, readable:
  <https://docs.oracle.com/en/java/javase/21/language/records.html>
- **`sealed` classes reference**:
  <https://docs.oracle.com/en/java/javase/21/language/sealed-classes-and-interfaces.html>
- **`Optional` API docs** — read the class javadoc top-to-bottom; it tells you how it's *meant* to be used:
  <https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html>

## The JEPs that define modern Java (skim, don't memorize)

JEPs (JDK Enhancement Proposals) are how features land in Java. The first time a teammate says "that's a preview feature from JEP 445," you'll want to know what they mean.

- **JEP 395: Records** (finalized Java 16) — the design rationale: <https://openjdk.org/jeps/395>
- **JEP 409: Sealed Classes** (finalized Java 17): <https://openjdk.org/jeps/409>
- **JEP 394: Pattern Matching for instanceof** (finalized Java 16): <https://openjdk.org/jeps/394>
- **JEP 286: Local-Variable Type Inference** (`var`, Java 10): <https://openjdk.org/jeps/286>
- **The full JEP index** — everything, searchable: <https://openjdk.org/jeps/0>

## The JVM (lecture 2 backing material)

- **The Java Virtual Machine Specification (Java SE 21 Edition)** — the normative reference for bytecode and the runtime. You will *not* read it cover to cover; skim chapter 2 ("The Structure of the Java Virtual Machine"):
  <https://docs.oracle.com/javase/specs/jvms/se21/html/index.html>
- **HotSpot Garbage Collection Tuning Guide** — what the GC is actually doing, vendor docs:
  <https://docs.oracle.com/en/java/javase/21/gctuning/index.html>
- **"JVM Anatomy Quarks"** by Aleksey Shipilëv — short, deep, free essays on one JVM behavior each. The single best free JVM-internals resource on the web:
  <https://shipilev.net/jvm/anatomy-quarks/>
- **`javap` documentation** — the disassembler ships in your JDK; learn to read it:
  <https://docs.oracle.com/en/java/javase/21/docs/specs/man/javap.html>

## Official Java docs

- **JDK 21 documentation home**: <https://docs.oracle.com/en/java/javase/21/>
- **JDK 21 API specification (javadoc)** — the standard library; keep it open in a tab:
  <https://docs.oracle.com/en/java/javase/21/docs/api/index.html>
- **"What's new in JDK 21"** — the release notes: <https://www.oracle.com/java/technologies/javase/21-relnote-issues.html>
- **OpenJDK 21 project page**: <https://openjdk.org/projects/jdk/21/>

## The toolchain

- **Eclipse Adoptium (Temurin)** — the free, well-supported OpenJDK build we use (`-tem` in SDKMAN):
  <https://adoptium.net/temurin/releases/?version=21>
- **SDKMAN!** — installs and switches JDK versions; you set this up in week 1:
  <https://sdkman.io/>
- **Apache Maven** — the build tool for this week. The "Maven in Five Minutes" guide is enough to start:
  <https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html>
- **Maven Central** — where dependencies come from; search before you add:
  <https://central.sonatype.com/>

## Editors / IDEs (any of these is fine)

- **IntelliJ IDEA Community Edition** (free, primary) — the best free Java IDE; its inspections will teach you modern idioms by nagging:
  <https://www.jetbrains.com/idea/download/>
- **VS Code + Extension Pack for Java** (free, lighter): <https://code.visualstudio.com/docs/languages/java>
- **Eclipse IDE** (free, classic): <https://www.eclipse.org/downloads/packages/>

The C3 curriculum **does not depend on any IDE feature**. Every project compiles, runs, and tests from `mvn` on the command line. If you can use only a terminal and `vim`, you can complete this course.

## Libraries we touch this week

This is a pure-Java week — no framework. The only non-JDK dependency is the test framework, which is already in your week-1 skeleton:

- **JUnit 5 (Jupiter)** — the test framework we use all year. This week you write a handful of `@Test` methods; week 3 goes deep:
  <https://junit.org/junit5/docs/current/user-guide/>

## Free books and courses (chapter-level)

- **Dev.java — "Learn Java"** — Oracle's modern, free, official learning site. Far better than the dusty old tutorials; start here if you want a guided path:
  <https://dev.java/learn/>
- **"Modern Java in Action" sample chapters** (Manning, free chapters) — the lambda/stream/Optional chapters are excellent and freely previewable:
  <https://www.manning.com/books/modern-java-in-action>
- **Baeldung — Java records / sealed classes / Optional guides** — practical, current, free:
  <https://www.baeldung.com/java-record-keyword>, <https://www.baeldung.com/java-sealed-classes-interfaces>, <https://www.baeldung.com/java-optional>

## Videos (free, no signup)

- **"Inside Java" YouTube channel** — the actual JDK team explaining features. Search "Records" and "Pattern Matching":
  <https://www.youtube.com/@java>
- **JEP Café (Inside Java)** — José Paumard's deep-dives on records, sealed types, and pattern matching; one feature per episode:
  <https://www.youtube.com/playlist?list=PLX8CzqL3ArzVnxC6PYxMlVjMPCo3fNZmW>

## Open-source projects to read this week

You learn more from one hour reading well-written modern Java than from three hours of tutorials. Pick one and scroll:

- **`openjdk/jdk`** — the JDK itself; the `java.util` package is full of clean, modern code (`Optional`, the records under `java.lang.runtime`):
  <https://github.com/openjdk/jdk>
- **`spring-projects/spring-petclinic`** — the canonical Spring sample. You don't touch Spring until week 4, but its domain model is readable Java today:
  <https://github.com/spring-projects/spring-petclinic>

## Tools you'll use this week

- **`java`, `javac`, `jar`, `javap`** — all ship in the JDK. Verify with `java -version` (must say `21`).
- **`mvn`** — Apache Maven. `mvn -version` should print a 3.9.x or 4.x release running on JDK 21.
- **`git`** — version control. `git --version` to confirm.

## Glossary cheat sheet

Keep this open in a tab.

| Term | Plain English |
|------|---------------|
| **Java 21** | The language *and* the platform version. An LTS release, September 2023. The 2026 baseline. |
| **LTS** | Long-Term Support release. Every ~3rd release (8, 11, 17, 21, 25). Where teams stay. |
| **JDK** | Java Development Kit — the toolchain: `javac` (compiler), `java` (launcher), `jar`, `javap`, plus the libraries. |
| **JRE** | Java Runtime Environment — historically "JDK minus the compiler." Mostly folded into the JDK now. |
| **JVM** | Java Virtual Machine — the thing that runs bytecode. HotSpot is the reference implementation. |
| **HotSpot** | The standard JVM that ships in OpenJDK. Includes the interpreter, the JIT, and the GC. |
| **bytecode** | The instruction set in `.class` files. `javac` produces it; the JVM runs it. Platform-independent. |
| **JIT** | Just-In-Time compiler — turns hot bytecode into native machine code at run time (HotSpot's C1 and C2). |
| **GC** | Garbage Collector — reclaims unreachable heap objects so you never call `free()`. G1 is the Java 21 default. |
| **record** | A concise, immutable data carrier. Compiler generates the constructor, accessors, `equals`, `hashCode`, `toString`. |
| **sealed** | A type that restricts which classes may extend/implement it via a `permits` clause. Enables exhaustive switches. |
| **boxing** | Wrapping a primitive (`int`) in its object form (`Integer`). Autoboxing does it implicitly. |
| **Optional** | A container that holds a value or nothing — Java's typed alternative to returning `null`. |
| **POM** | `pom.xml` — Maven's Project Object Model. Declares your dependencies, plugins, and build config. |

---

*If a link 404s, please open an issue so we can replace it.*
