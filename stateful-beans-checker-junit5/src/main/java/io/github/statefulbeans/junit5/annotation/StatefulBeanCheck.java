package io.github.statefulbeans.junit5.annotation;

import io.github.statefulbeans.junit5.extension.StatefulBeanCheckExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JUnit 5 test class as a stateful-bean check.
 *
 * <p>Placing this annotation on a test class (with no test methods) is sufficient
 * to trigger analysis at class-initialisation time via the registered
 * {@link StatefulBeanCheckExtension}.</p>
 *
 * <h2>Package-scan usage (no Spring context)</h2>
 * <pre>{@code
 * @StatefulBeanCheck(
 *     packages = {"com.myapp.service", "com.myapp.component"},
 *     lombokAware = true
 * )
 * class StatefulBeanTest {}
 * }</pre>
 *
 * <h2>Combined with Spring Boot test</h2>
 * <p>When combined with {@code @SpringBootTest} and the
 * {@code stateful-beans-checker-spring} module is on the classpath, the
 * extension will automatically use the live {@code ApplicationContext} instead
 * of the classpath scanner.</p>
 * <pre>{@code
 * @SpringStatefulBeanTest     // composite annotation from the spring module
 * @SpringBootTest
 * class StatefulBeanTest {}
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(StatefulBeanCheckExtension.class)
public @interface StatefulBeanCheck {

    /**
     * Packages to scan for Spring-annotated bean classes.
     * At least one package is required when not using Spring context integration.
     */
    String[] packages() default {};

    /**
     * Packages to exclude from scanning (prefix match).
     */
    String[] excludePackages() default {};

    /**
     * Fully-qualified annotation names — classes carrying any of these
     * annotations will be skipped.
     *
     * <p>Example: {@code "org.springframework.stereotype.Repository"} to
     * allow repository classes to maintain internal state.</p>
     */
    String[] excludeAnnotations() default {};

    /**
     * Specific fully-qualified class names to exclude from the check.
     */
    String[] excludeClasses() default {};

    /**
     * When {@code true}, Lombok {@code @Data} and {@code @Setter} on a bean
     * class are treated as violations. Requires Lombok to be on the test
     * classpath (or at least the annotation names to be resolvable).
     */
    boolean lombokAware() default false;

    /**
     * When {@code true} (default), fields annotated with Spring/Jakarta
     * injection annotations ({@code @Autowired}, {@code @Value}, etc.) are
     * exempt from the non-final-field check.
     */
    boolean allowInjectedFields() default true;

    /**
     * When {@code true} (default), {@code ThreadLocal} fields are exempt.
     */
    boolean allowThreadLocalFields() default true;

    /**
     * When {@code true}, the test fails if any violations are found.
     * When {@code false}, violations are logged as warnings but the test passes.
     * Useful for incrementally introducing the check into an existing codebase.
     */
    boolean failOnViolation() default true;
}
