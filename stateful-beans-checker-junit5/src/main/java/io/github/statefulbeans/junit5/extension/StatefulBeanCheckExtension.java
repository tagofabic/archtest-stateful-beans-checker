package io.github.statefulbeans.junit5.extension;

import io.github.statefulbeans.core.StatefulBeanChecker;
import io.github.statefulbeans.core.config.StatefulBeanCheckConfig;
import io.github.statefulbeans.core.model.AnalysisResult;
import io.github.statefulbeans.junit5.annotation.ExcludeFromCheck;
import io.github.statefulbeans.junit5.annotation.StatefulBeanCheck;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that executes the stateful-bean check before any test methods run in the
 * annotated class.
 *
 * <p>Registered automatically when {@link StatefulBeanCheck} is present on the test class (via
 * {@code @ExtendWith}).
 *
 * <p>If the {@code stateful-beans-checker-spring} module is on the classpath and a Spring {@code
 * ApplicationContext} has been loaded into the {@link ExtensionContext} store (e.g. by
 * {@code @SpringBootTest}), the extension delegates to the Spring-aware runner instead of the
 * package scanner.
 */
public class StatefulBeanCheckExtension implements BeforeAllCallback {

    private static final Logger log = LoggerFactory.getLogger(StatefulBeanCheckExtension.class);

    /**
     * ExtensionContext store key used by the Spring integration module to register the {@code
     * ApplicationContext} bean map so this extension can pick it up without a hard Spring
     * compile-time dependency.
     */
    public static final String STORE_NAMESPACE =
            "io.github.statefulbeans.junit5.StatefulBeanCheckExtension";

    public static final String BEAN_MAP_KEY = "springBeanMap";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        StatefulBeanCheck annotation = testClass.getAnnotation(StatefulBeanCheck.class);
        if (annotation == null) return;

        StatefulBeanCheckConfig config = buildConfig(annotation, testClass);
        StatefulBeanChecker checker = new StatefulBeanChecker(config);

        AnalysisResult result = runAnalysis(checker, context, annotation);

        if (result.hasViolations()) {
            String message = result.toFailureMessage();
            if (annotation.failOnViolation()) {
                throw new AssertionError(message);
            } else {
                log.warn("Stateful bean violations detected (failOnViolation=false):\n{}", message);
            }
        } else {
            log.info(
                    "Stateful bean check passed. {} bean(s) scanned, no violations found.",
                    result.getTotalBeansScanned());
        }
    }

    // -------------------------------------------------------------------------

    private StatefulBeanCheckConfig buildConfig(StatefulBeanCheck annotation, Class<?> testClass) {
        StatefulBeanCheckConfig.Builder builder =
                StatefulBeanCheckConfig.builder()
                        .packages(annotation.packages())
                        .excludePackages(annotation.excludePackages())
                        .excludeAnnotations(annotation.excludeAnnotations())
                        .excludeClassNames(annotation.excludeClasses())
                        .lombokAware(annotation.lombokAware())
                        .allowInjectedFields(annotation.allowInjectedFields())
                        .allowThreadLocalFields(annotation.allowThreadLocalFields());

        // Always exclude classes annotated with @ExcludeFromCheck
        builder.excludeAnnotations(ExcludeFromCheck.class.getName());

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private AnalysisResult runAnalysis(
            StatefulBeanChecker checker, ExtensionContext context, StatefulBeanCheck annotation)
            throws IOException {
        // Check if the Spring integration module registered a bean map
        ExtensionContext.Store store =
                context.getStore(ExtensionContext.Namespace.create(STORE_NAMESPACE));
        Object beanMapObj = store.get(BEAN_MAP_KEY);

        if (beanMapObj instanceof java.util.Map<?, ?> rawMap) {
            log.info(
                    "Using Spring ApplicationContext bean map for analysis ({} beans).",
                    rawMap.size());
            java.util.Map<String, Class<?>> beanMap = (java.util.Map<String, Class<?>>) rawMap;
            return checker.checkBeans(beanMap);
        }

        // Fall back to package scan
        if (annotation.packages().length == 0) {
            throw new IllegalStateException(
                    "@StatefulBeanCheck requires at least one package in 'packages' when "
                            + "not using Spring context integration. "
                            + "Add packages = {\"com.myapp\"} or use @SpringStatefulBeanTest.");
        }

        log.info(
                "Using classpath package scan for analysis. Packages: {}",
                Arrays.toString(annotation.packages()));
        return checker.checkPackages();
    }
}
