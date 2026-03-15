package io.github.statefulbeans.spring.extension;

import io.github.statefulbeans.core.StatefulBeanChecker;
import io.github.statefulbeans.core.config.StatefulBeanCheckConfig;
import io.github.statefulbeans.core.model.AnalysisResult;
import io.github.statefulbeans.spring.annotation.SpringStatefulBeanTest;
import io.github.statefulbeans.spring.context.SpringBeanExtractor;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * JUnit 5 extension that backs {@link SpringStatefulBeanTest}.
 *
 * <p>Reads the live {@code ApplicationContext} loaded by {@code @SpringBootTest}, extracts all
 * user-defined beans, and runs the stateful-bean analysis using the configuration declared on
 * {@link SpringStatefulBeanTest}.
 */
public class SpringStatefulBeanTestExtension implements BeforeAllCallback {

    private static final Logger log =
            LoggerFactory.getLogger(SpringStatefulBeanTestExtension.class);

    private final SpringBeanExtractor extractor = new SpringBeanExtractor();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        SpringStatefulBeanTest annotation = testClass.getAnnotation(SpringStatefulBeanTest.class);
        if (annotation == null) return;

        ApplicationContext applicationContext;
        try {
            applicationContext = SpringExtension.getApplicationContext(context);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "@SpringStatefulBeanTest requires a Spring ApplicationContext. "
                            + "Add @SpringBootTest (or another Spring test annotation) to your test class.",
                    e);
        }

        Map<String, Class<?>> beanMap = extractor.extractUserBeans(applicationContext);
        StatefulBeanCheckConfig config = buildConfig(annotation);
        StatefulBeanChecker checker = new StatefulBeanChecker(config);

        AnalysisResult result = checker.checkBeans(beanMap);

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

    private StatefulBeanCheckConfig buildConfig(SpringStatefulBeanTest annotation) {
        return StatefulBeanCheckConfig.builder()
                .lombokAware(annotation.lombokAware())
                .allowInjectedFields(annotation.allowInjectedFields())
                .allowThreadLocalFields(annotation.allowThreadLocalFields())
                .excludeAnnotations(annotation.excludeAnnotations())
                .excludeClassNames(annotation.excludeClasses())
                .build();
    }
}
