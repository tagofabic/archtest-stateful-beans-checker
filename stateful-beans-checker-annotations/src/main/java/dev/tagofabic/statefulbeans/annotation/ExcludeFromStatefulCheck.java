package dev.tagofabic.statefulbeans.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring Bean class or a specific field as deliberately excluded from the stateful-bean
 * check.
 *
 * <p><b>Class-level</b> — excludes the entire bean from analysis:
 *
 * <pre>{@code
 * @Service
 * @ExcludeFromStatefulCheck(reason = "Thread-safe via ConcurrentHashMap internally")
 * public class MetricsCache {
 *     private final Map<String, Long> counters = new ConcurrentHashMap<>();
 * }
 * }</pre>
 *
 * <p><b>Field-level</b> — excludes a single field while the rest of the bean is still checked:
 *
 * <pre>{@code
 * @Service
 * public class OrderService {
 *     @ExcludeFromStatefulCheck(reason = "Intentional mutable cache, access is synchronised")
 *     private List<Order> recentOrders;
 *
 *     private final OrderRepository repository; // still checked
 * }
 * }</pre>
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcludeFromStatefulCheck {

    /** Human-readable justification for the exclusion. */
    String reason() default "";
}
