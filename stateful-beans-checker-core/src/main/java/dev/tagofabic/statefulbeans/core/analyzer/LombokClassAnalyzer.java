package dev.tagofabic.statefulbeans.core.analyzer;

import dev.tagofabic.statefulbeans.core.model.ViolationType;
import dev.tagofabic.statefulbeans.core.util.AnnotationNames;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects Lombok annotations at the class level that imply generated mutability.
 *
 * <p>Detection is done by annotation type name so the library can operate independently of whether
 * Lombok is on the test classpath.
 */
public class LombokClassAnalyzer {

    /** Returns class-level {@link ViolationType}s related to Lombok on the given class. */
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

            if (AnnotationNames.LOMBOK_BUILDER.equals(name) && hasSingularField(clazz)) {
                violations.add(ViolationType.LOMBOK_SINGULAR_BUILDER);
            }
        }

        return violations;
    }

    private boolean hasSingularField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                if (AnnotationNames.LOMBOK_SINGULAR.equals(annotation.annotationType().getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the class carries {@code @lombok.Value}, which Lombok uses to
     * generate an immutable value class (all fields final, no setters). Such beans are considered
     * safe.
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
