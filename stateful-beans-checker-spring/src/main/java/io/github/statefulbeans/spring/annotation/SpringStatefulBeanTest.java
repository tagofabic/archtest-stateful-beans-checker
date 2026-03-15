package io.github.statefulbeans.spring.annotation;

import io.github.statefulbeans.spring.extension.SpringStatefulBeanTestExtension;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Composite annotation for running stateful-bean checks against a live Spring {@code
 * ApplicationContext}.
 *
 * <p>Use together with {@code @SpringBootTest} (or any other Spring test annotation that loads an
 * {@code ApplicationContext}):
 *
 * <pre>{@code
 * @SpringStatefulBeanTest(lombokAware = true)
 * @SpringBootTest
 * class StatefulBeanTest {
 *     // No test methods needed — the check runs automatically before the suite
 * }
 * }</pre>
 *
 * <h2>What happens</h2>
 *
 * <ol>
 *   <li>{@code @SpringBootTest} loads the {@code ApplicationContext}.
 *   <li>{@link SpringStatefulBeanTestExtension} extracts all user beans from the context and runs
 *       the stateful-bean analysis using the attributes declared on this annotation.
 * </ol>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(SpringStatefulBeanTestExtension.class)
public @interface SpringStatefulBeanTest {

    /** Enable Lombok-aware detection ({@code @Data}, {@code @Setter}, etc.). */
    boolean lombokAware() default false;

    /**
     * When {@code true} (default), the test fails on violations. Set to {@code false} to log
     * warnings without failing — useful when introducing the check into an existing codebase.
     */
    boolean failOnViolation() default true;

    /** Fully-qualified annotation names — bean classes carrying any of these will be skipped. */
    String[] excludeAnnotations() default {};

    /** Fully-qualified class names to exclude from the check. */
    String[] excludeClasses() default {};

    /**
     * When {@code true} (default), injection-annotated fields ({@code @Autowired}, {@code @Value},
     * etc.) are not flagged.
     */
    boolean allowInjectedFields() default true;

    /** When {@code true} (default), {@code ThreadLocal} fields are not flagged. */
    boolean allowThreadLocalFields() default true;
}
