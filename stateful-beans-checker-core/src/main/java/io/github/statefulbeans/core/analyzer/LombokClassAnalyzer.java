package io.github.statefulbeans.core.analyzer;

import io.github.statefulbeans.core.model.ViolationType;
import io.github.statefulbeans.core.util.AnnotationNames;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects Lombok annotations at the class level that imply generated mutability.
 *
 * <p>Detection is done by annotation type name so the library can operate
 * independently of whether Lombok is on the test classpath.</p>
 */
public class LombokClassAnalyzer {

    /**
     * Returns class-level {@link ViolationType}s related to Lombok on the given class.
     */
    public List<ViolationType> analyzeClass(Class<?> clazz) {
        List<ViolationType> violations = new ArrayList<>();

        for (Annotation annotation : clazz.getAnnotations()) {
            String name = annotation.annotationType().getName();

            if (AnnotationNames.LOMBOK_DATA.equals(name)) {
                violations.add(ViolationType.LOMBOK_DATA);
            }

            if (AnnotationNames.LOMBOK_SETTER.equals(name)) {
                violations.add(ViolationType.LOMBOK_SETTER);
            }

            // @lombok.Value makes the class immutable — not a violation.
            // @lombok.Builder alone is OK; @Singular within a @Builder
            // is checked separately at field level if needed.
        }

        return violations;
    }

    /**
     * Returns {@code true} if the class carries {@code @lombok.Value},
     * which Lombok uses to generate an immutable value class (all fields final,
     * no setters). Such beans are considered safe.
     */
    public boolean isLombokImmutable(Class<?> clazz) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (AnnotationNames.LOMBOK_VALUE.equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }
}
