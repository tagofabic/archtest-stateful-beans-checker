# Spring Stateful Beans Checker

> Catch mutable Spring Beans way before they reach production.

A lightweight, ArchUnit-style test library that detects stateful or mutable Spring Beans that are unsafe under high concurrency. 
Works with **package scanning** (no Spring context required) or **live `ApplicationContext` inspection** via `@SpringBootTest`.

--- 

## Contents

- [Why](#why)
- [Installation](#installation)
- [Usage](#usage)
  - [Option 1 — Package scan (no Spring context)](#option-1--package-scan-no-spring-context)
  - [Option 2 — Spring context (@SpringBootTest)](#option-2--spring-context-springboottest)
  - [Option 3 — Programmatic API](#option-3--programmatic-api)
- [What gets flagged](#what-gets-flagged)
- [Configuration reference](#configuration-reference)
- [Excluding a Specific Bean](#excluding-a-specific-bean)
- [Example failure output](#example-failure-output)
- [How it Works](#how-it-works)
- [Module Structure](#module-structure)
- [License](#license)
- [AI Disclosure](#ai-disclosure)

---

## Why

Spring Beans are singletons by default. A single mutable field — an `ArrayList`, a non-final counter, a Lombok `@Data` 
annotation — can silently cause race conditions under load. This library surfaces those issues at test time, before they 
become production incidents.

This just provides that extra guardrail that might be missed during the Code Review process.

---

## Installation

Add the modules you need to your Maven `pom.xml`.

```xml
<!--
  Annotations only — add as a compile dependency so your production beans
  can use @ExcludeFromStatefulCheck without pulling in test dependencies.
-->
<dependency>
    <groupId>dev.tagofabic</groupId>
    <artifactId>stateful-beans-checker-annotations</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

<!-- Core engine — test scope -->
<dependency>
    <groupId>dev.tagofabic</groupId>
    <artifactId>stateful-beans-checker-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>

<!-- JUnit 5 integration (package scan mode) — test scope -->
<dependency>
    <groupId>dev.tagofabic</groupId>
    <artifactId>stateful-beans-checker-junit5</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>

<!-- Spring Boot integration (@SpringBootTest mode) — test scope -->
<dependency>
    <groupId>dev.tagofabic</groupId>
    <artifactId>stateful-beans-checker-spring</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

**Requirements:** Java 21+, JUnit 5, Spring Framework 6 / Spring Boot 3.

---

## Usage

### Option 1 — Package scan (no Spring context)

Annotate a JUnit 5 test class and add a placeholder `@Test` method so your IDE discovers and runs it. The extension fires in `@BeforeAll`, before any test methods execute.

```java
@StatefulBeanCheck(
    packages    = {"com.myapp.service", "com.myapp.component"},
    lombokAware = true
)
class StatefulBeanArchTest {

    @Test
    void statefulBeansCheck() {
        // Violations are caught by @StatefulBeanCheck before this runs
    }
}
```

---

### Option 2 — Spring context (`@SpringBootTest`)

Combines with `@SpringBootTest` to inspect only the beans actually wired in your application context. Infrastructure beans (Spring internals, HikariCP, Micrometer, etc.) are automatically excluded.

```java
@SpringStatefulBeanTest(lombokAware = true)
@SpringBootTest
class StatefulBeanArchTest {}
```

---

### Option 3 — Programmatic API

For custom integrations or non-JUnit runners:

```java
StatefulBeanCheckConfig config = StatefulBeanCheckConfig.builder()
    .packages(List.of("com.myapp.service", "com.myapp.component"))
    .lombokAware(true)
    .allowInjectedFields(true)   // exempt @Autowired / @Value fields
    .allowThreadLocalFields(true)
    .build();

AnalysisResult result = new StatefulBeanChecker(config).checkPackages();

if (!result.getViolations().isEmpty()) {
    throw new AssertionError(result.toFailureMessage());
}
```

---

## What gets flagged

| Violation | Description |
|---|---|
| `NON_FINAL_FIELD` | Instance field not declared `final` |
| `MUTABLE_SETTER` | Public or package-private setter on an instance field |
| `MUTABLE_COLLECTION_FIELD` | Field of a mutable collection type (`ArrayList`, `HashMap`, `HashSet`, `List`, `Map`, `Set`, …) |
| `LOMBOK_DATA` | Class annotated with `@lombok.Data` (generates setters for all fields) |
| `LOMBOK_SETTER` | Class or field annotated with `@lombok.Setter` |
| `LOMBOK_SINGULAR_BUILDER` | Builder with `@Singular` — accumulates mutable state |

### What is automatically allowed

- Fields annotated with `@Autowired`, `@Value`, `@Inject`, or `@Resource` (injection points).
- `ThreadLocal` fields (opt-out via `allowThreadLocalFields = false`).
- `static` fields and compiler-generated synthetic fields.
- Classes annotated with `@lombok.Value` (all fields are effectively final).

---

## Configuration reference

All options are available on both `@StatefulBeanCheck`, `@SpringStatefulBeanTest`, and `StatefulBeanCheckConfig`.

| Option | Default | Description |
|---|---|---|
| `packages` | — | Packages to scan (required for package-scan mode) |
| `excludePackages` | `[]` | Skip any class whose package starts with these prefixes |
| `excludeClasses` | `[]` | Fully-qualified class names to skip |
| `excludeAnnotations` | `[]` | Skip classes carrying any of these annotation FQCNs |
| `lombokAware` | `false` | Detect `@Data` / `@Setter` as violations |
| `allowInjectedFields` | `true` | Exempt fields annotated with injection annotations |
| `allowThreadLocalFields` | `true` | Exempt `ThreadLocal<?>` fields |
| `failOnViolation` | `true` | Fail the test on violations (set to `false` to log warnings only) |

---

## Excluding a Specific Bean

In cases (hopefully rare) where a bean or field is intentionally mutable, you can exclude it from the check using `@ExcludeFromStatefulCheck` from the `stateful-beans-checker-annotations` dependency.

**Class-level** — skips the entire bean:

```java
@ExcludeFromStatefulCheck(reason = "Thread-safe via ConcurrentHashMap internally")
@Service
public class MetricsCache {
    private final Map<String, Long> counters = new ConcurrentHashMap<>();
}
```

**Field-level** — skips a single field while the rest of the bean is still checked:

```java
@Service
public class OrderService {

    @ExcludeFromStatefulCheck(reason = "Intentional cache, access is synchronised externally")
    private List<Order> recentOrders;

    private final OrderRepository repository; // still checked
}
```

The `reason` field forces an explicit justification at the call site.

---

## Example failure output

```
StatefulBeanChecker found 2 violation(s) across 47 beans:

  Bean: com.myapp.service.OrderService
    - [NON_FINAL_FIELD] Field 'cache' (java.util.ArrayList) is not final
    - [MUTABLE_COLLECTION_FIELD] Field 'cache' is a mutable collection type

  Bean: com.myapp.component.ReportBuilder
    - [LOMBOK_DATA] Class is annotated with @Data which generates mutable setters
```

---

## How it Works

```
@StatefulBeanCheck / @SpringStatefulBeanTest
        │
        ▼
StatefulBeanCheckExtension  ──(BeforeAll)──►  StatefulBeanChecker
                                                      │
                              ┌───────────────────────┤
                              ▼                       ▼
                    ClasspathPackageScanner    SpringBeanExtractor
                    (package scan mode)       (Spring context mode)
                              │
                              ▼
                      BeanClassAnalyzer
                       ├── FieldAnalyzer          → NON_FINAL_FIELD
                       │                          → MUTABLE_COLLECTION_FIELD
                       │                          → LOMBOK_SETTER (field-level)
                       ├── SetterMethodAnalyzer   → MUTABLE_SETTER
                       └── LombokClassAnalyzer    → LOMBOK_DATA
                                                  → LOMBOK_SETTER (class-level)
```

- **No Lombok compile dependency** — Lombok annotations are detected by name string, so the library works whether or not Lombok is on the classpath.
- **CGLIB proxy unwrapping** — proxied beans are unwrapped automatically before analysis.
- **No Spring context required** — package scan mode uses ASM-backed classpath scanning and works in any JUnit 5 environment.

---

## Module Structure

| Module | Scope | Purpose |
|---|---|---|
| `stateful-beans-checker-annotations` | compile | Zero-dep annotations (`@ExcludeFromStatefulCheck`) for production code |
| `stateful-beans-checker-core` | test | Pure analysis engine — no JUnit or Spring context dependency |
| `stateful-beans-checker-junit5` | test | `@StatefulBeanCheck` annotation + JUnit 5 extension |
| `stateful-beans-checker-spring` | test | `@SpringStatefulBeanTest` + `@SpringBootTest` integration |

---

## License

[Apache 2.0](LICENSE)

---

## AI Disclosure

This project was kickstarted with the assistance of [Claude](https://claude.ai) (Anthropic's AI assistant). All code has been reviewed and is maintained by the project author.