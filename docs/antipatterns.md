# Antipatterns — What the Library Flags

Each section below shows a bean pattern that will cause `StatefulBeanChecker` to fail the test, the violation type it raises, and a thread-safe alternative.

Spring Beans are singletons by default. Every request, every thread, shares the same instance. A single mutable field is all it takes to introduce a silent race condition under load.

---

## NON_FINAL_FIELD

A field that is not declared `final` can be reassigned at any point after construction.

```java
// BAD — field is not final; any thread can overwrite it
@Service
public class PricingService {

    private BigDecimal taxRate = new BigDecimal("0.20"); // mutable!

    public BigDecimal calculate(BigDecimal price) {
        return price.multiply(taxRate);
    }
}
```

**Violation raised:** `NON_FINAL_FIELD` on `taxRate`.

**Fix:** make the field `final` and set it in the constructor (or via `@Value`).

```java
@Service
public class PricingService {

    private final BigDecimal taxRate;

    public PricingService(@Value("${tax.rate:0.20}") BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
}
```

---

## MUTABLE_SETTER

A public (or package-private) setter allows any caller to swap the field's value after the bean is wired, destroying any thread-safety guarantee the constructor established.

```java
// BAD — a setter lets external code replace the dependency at runtime
@Service
public class NotificationService {

    private EmailClient emailClient;

    // Setter injection — opens the field to arbitrary mutation
    @Autowired
    public void setEmailClient(EmailClient emailClient) {
        this.emailClient = emailClient;
    }
}
```

**Violation raised:** `MUTABLE_SETTER` on `setEmailClient`.

**Fix:** switch to constructor injection and mark the field `final`.

```java
@Service
public class NotificationService {

    private final EmailClient emailClient;

    @Autowired
    public NotificationService(EmailClient emailClient) {
        this.emailClient = emailClient;
    }
}
```

---

## MUTABLE_COLLECTION_FIELD

Mutable collection types (`ArrayList`, `HashMap`, `HashSet`, `LinkedList`, …) accumulate state across requests. Because the singleton bean is shared, concurrent writes corrupt the collection without synchronisation.

```java
// BAD — shared mutable list grows with every request
@Component
public class AuditLogger {

    private List<String> log = new ArrayList<>(); // shared across threads!

    public void record(String entry) {
        log.add(entry); // race condition
    }
}
```

**Violation raised:** `NON_FINAL_FIELD` + `MUTABLE_COLLECTION_FIELD` on `log`.

**Fix:** use a thread-safe alternative or push state to a proper store.

```java
@Component
public class AuditLogger {

    // Thread-safe; no external synchronisation needed
    private final List<String> log = new CopyOnWriteArrayList<>();

    public void record(String entry) {
        log.add(entry);
    }
}
```

Or, for high-throughput counters, prefer `ConcurrentLinkedQueue`, `ConcurrentHashMap`, or a dedicated persistence layer.

> **Note:** `ConcurrentHashMap` / `ConcurrentLinkedQueue` fields are still reported as `MUTABLE_COLLECTION_FIELD` because the checker operates on declared types, not runtime semantics. Use `@ExcludeFromStatefulCheck` with an explicit `reason` when a concurrent collection is intentional.

---

## LOMBOK_DATA

`@Data` generates getters, setters, `equals`, `hashCode`, and `toString` for every field. The generated setters make every field publicly mutable — equivalent to writing `MUTABLE_SETTER` for every single field.

```java
// BAD — @Data generates a setter for every field
@Data
@Service
public class UserSessionService {

    private String currentUser;
    private Instant sessionStart;
    private List<String> roles;
}
```

**Violation raised:** `LOMBOK_DATA`.

**Fix:** replace `@Data` with `@Getter` + `@RequiredArgsConstructor` and mark all fields `final`.

```java
@Getter
@RequiredArgsConstructor
@Service
public class UserSessionService {
    // Stateless — all runtime data lives in a per-request context or DB
}
```

If you need a value object (not a Spring Bean), `@Value` (Lombok) is safe — all fields are effectively final.

---

## LOMBOK_SETTER

`@Setter` at the class level generates a setter for every field. Applied to a single field, it opens just that field — but it is still mutation in a shared singleton.

```java
// BAD — class-level @Setter generates setters for all fields
@Setter
@Service
public class FeatureFlagService {

    private boolean darkModeEnabled;
    private int maxRetries;
}
```

**Violation raised:** `LOMBOK_SETTER` (class-level).

```java
// BAD — field-level @Setter on a single field
@Service
public class FeatureFlagService {

    @Setter
    private boolean darkModeEnabled; // still mutable

    private final int maxRetries = 3;
}
```

**Violation raised:** `LOMBOK_SETTER` (field-level).

**Fix:** inject configuration via `@Value` or `@ConfigurationProperties` and keep fields `final`.

```java
@Service
public class FeatureFlagService {

    private final boolean darkModeEnabled;
    private final int maxRetries;

    public FeatureFlagService(
            @Value("${features.dark-mode:false}") boolean darkModeEnabled,
            @Value("${retry.max:3}") int maxRetries) {
        this.darkModeEnabled = darkModeEnabled;
        this.maxRetries = maxRetries;
    }
}
```

---

## LOMBOK_SINGULAR_BUILDER

`@Singular` inside a Lombok `@Builder` accumulates items into a mutable backing list on the builder. If the builder itself is stored as a bean field (e.g., to reuse a partially configured builder across requests), that list is shared state.

```java
// BAD — builder with @Singular stored as a bean field
@Component
public class QueryFactory {

    @Builder
    public static class QueryBuilder {
        @Singular
        private List<String> filters; // mutable accumulation
    }

    private QueryBuilder sharedBuilder = QueryBuilder.builder().build(); // shared!
}
```

**Violation raised:** `LOMBOK_SINGULAR_BUILDER` + `NON_FINAL_FIELD` on `sharedBuilder`.

**Fix:** never store builder instances in singleton fields. Create a fresh builder per request.

```java
@Component
public class QueryFactory {

    public QueryBuilder newQuery() {
        return QueryBuilder.builder(); // new builder per call, no shared state
    }
}
```

---

## Intentionally Mutable Beans

Sometimes a bean is purposefully mutable and that is fine — caches, connection pools, and metrics registries are examples. Use `@ExcludeFromStatefulCheck` to document the intent:

```java
@ExcludeFromStatefulCheck(reason = "All access is through ConcurrentHashMap; thread-safe by design")
@Service
public class LocalMetricsCache {

    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();

    public void increment(String key) {
        counters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }
}
```

The `reason` field is mandatory — it forces an explicit justification at the call site and keeps the exclusion reviewable in code review.
