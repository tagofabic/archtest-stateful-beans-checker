package io.github.statefulbeans.junit5.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring Bean class as deliberately excluded from the stateful-bean check.
 *
 * <p>Use this when a bean intentionally holds mutable state and you have taken
 * other measures (e.g. synchronisation, concurrent data structures) to make it
 * safe. Annotate the bean class itself:</p>
 *
 * <pre>{@code
 * @Service
 * @ExcludeFromCheck(reason = "Uses ConcurrentHashMap internally — safe by design")
 * public class MyCache {
 *     private final Map<String, Object> cache = new ConcurrentHashMap<>();
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcludeFromCheck {

    /**
     * Optional human-readable justification for excluding this class.
     */
    String reason() default "";
}
