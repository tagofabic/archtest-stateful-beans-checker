package io.github.statefulbeans.core.analyzer;

import io.github.statefulbeans.core.config.StatefulBeanCheckConfig;
import io.github.statefulbeans.core.model.BeanViolation;
import io.github.statefulbeans.core.model.FieldViolation;
import io.github.statefulbeans.core.model.ViolationType;
import io.github.statefulbeans.core.util.AnnotationNames;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates all sub-analysers against a single bean class and produces an optional {@link
 * BeanViolation}.
 */
public class BeanClassAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(BeanClassAnalyzer.class);

    private final FieldAnalyzer fieldAnalyzer = new FieldAnalyzer();
    private final SetterMethodAnalyzer setterAnalyzer = new SetterMethodAnalyzer();
    private final LombokClassAnalyzer lombokClassAnalyzer = new LombokClassAnalyzer();

    /**
     * Analyses {@code clazz} and returns a {@link BeanViolation} if any statefulness issues are
     * found, or {@link Optional#empty()} if clean.
     *
     * @param clazz the Spring Bean class to inspect
     * @param beanName the logical Spring bean name (may be {@code null})
     * @param config analysis configuration
     */
    public Optional<BeanViolation> analyze(
            Class<?> clazz, String beanName, StatefulBeanCheckConfig config) {
        if (isExcluded(clazz, config)) {
            log.debug("Skipping excluded class: {}", clazz.getName());
            return Optional.empty();
        }

        List<ViolationType> classViolations = new ArrayList<>();
        List<FieldViolation> fieldViolations = new ArrayList<>();

        // --- Class-level Lombok checks ---
        if (config.isLombokAware()) {
            // If @lombok.Value is present the class is fully immutable — skip it
            if (lombokClassAnalyzer.isLombokImmutable(clazz)) {
                log.debug(
                        "Class {} carries @lombok.Value — considered immutable, skipping.",
                        clazz.getName());
                return Optional.empty();
            }
            classViolations.addAll(lombokClassAnalyzer.analyzeClass(clazz));
        }

        // --- Field-level checks ---
        fieldViolations.addAll(fieldAnalyzer.analyzeFields(clazz, config));

        // --- Setter-method checks ---
        fieldViolations.addAll(setterAnalyzer.analyzeSetters(clazz, config));

        if (classViolations.isEmpty() && fieldViolations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new BeanViolation(clazz.getName(), beanName, fieldViolations, classViolations));
    }

    // -------------------------------------------------------------------------

    private boolean isExcluded(Class<?> clazz, StatefulBeanCheckConfig config) {
        // Excluded by exact class name
        if (config.getExcludedClasses().contains(clazz.getName())) return true;

        // Excluded by package prefix
        String className = clazz.getName();
        for (String excludedPkg : config.getExcludedPackages()) {
            if (className.startsWith(excludedPkg)) return true;
        }

        // Excluded by annotation
        for (Annotation annotation : clazz.getAnnotations()) {
            if (config.getExcludedAnnotations().contains(annotation.annotationType().getName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if the class carries at least one Spring Bean stereotype annotation
     * (used when scanning packages without a live context).
     */
    public static boolean isSpringBeanClass(Class<?> clazz) {
        for (Annotation annotation : clazz.getAnnotations()) {
            String name = annotation.annotationType().getName();
            if (AnnotationNames.SPRING_BEAN_ANNOTATIONS.contains(name)) return true;

            // Meta-annotation check: e.g. @RestController is meta-annotated with @Controller
            for (Annotation metaAnnotation : annotation.annotationType().getAnnotations()) {
                if (AnnotationNames.SPRING_BEAN_ANNOTATIONS.contains(
                        metaAnnotation.annotationType().getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
