package io.github.statefulbeans.core.analyzer;

import io.github.statefulbeans.annotation.ExcludeFromStatefulCheck;
import io.github.statefulbeans.core.config.StatefulBeanCheckConfig;
import io.github.statefulbeans.core.model.FieldViolation;
import io.github.statefulbeans.core.model.ViolationType;
import io.github.statefulbeans.core.util.AnnotationNames;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Inspects the declared fields of a class and produces {@link FieldViolation}s for any field that
 * represents mutable state unsuitable for a shared Spring Bean.
 */
public class FieldAnalyzer {

    private static final Set<String> MUTABLE_COLLECTION_TYPES =
            Set.of(
                    "java.util.ArrayList",
                    "java.util.LinkedList",
                    "java.util.Vector",
                    "java.util.HashMap",
                    "java.util.LinkedHashMap",
                    "java.util.TreeMap",
                    "java.util.HashSet",
                    "java.util.LinkedHashSet",
                    "java.util.TreeSet",
                    "java.util.ArrayDeque",
                    "java.util.PriorityQueue");

    private static final Set<String> MUTABLE_COLLECTION_INTERFACES =
            Set.of(
                    "java.util.List",
                    "java.util.Map",
                    "java.util.Set",
                    "java.util.Collection",
                    "java.util.Deque",
                    "java.util.Queue");

    public List<FieldViolation> analyzeFields(Class<?> clazz, StatefulBeanCheckConfig config) {
        List<FieldViolation> violations = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            // Skip synthetic (compiler-generated) fields
            if (field.isSynthetic()) continue;

            // Skip static fields — they are class-level, not instance state
            if (Modifier.isStatic(field.getModifiers())) continue;

            // Skip ThreadLocal if configured to allow them
            if (config.isAllowThreadLocalFields()
                    && ThreadLocal.class.isAssignableFrom(field.getType())) {
                continue;
            }

            // Skip injection-annotated fields if configured to allow them
            if (config.isAllowInjectedFields() && isInjectionField(field)) {
                continue;
            }

            // Skip fields explicitly excluded via @ExcludeFromStatefulCheck
            if (field.isAnnotationPresent(ExcludeFromStatefulCheck.class)) {
                continue;
            }

            // Check: non-final field
            if (!Modifier.isFinal(field.getModifiers())) {
                violations.add(
                        new FieldViolation(
                                field.getName(),
                                field.getType().getName(),
                                ViolationType.NON_FINAL_FIELD,
                                String.format(
                                        "Field is not final. Non-final instance fields in a Spring Bean "
                                                + "can be mutated concurrently. Declare it final or use constructor injection.")));
            }

            // Check: mutable collection type (even if final, the collection itself is mutable)
            checkMutableCollection(field, violations);

            // Check: Lombok @Setter on individual field
            if (config.isLombokAware() && hasAnnotation(field, AnnotationNames.LOMBOK_SETTER)) {
                violations.add(
                        new FieldViolation(
                                field.getName(),
                                field.getType().getName(),
                                ViolationType.LOMBOK_SETTER,
                                "Field carries @lombok.Setter — a public setter is generated, "
                                        + "making this field mutable after construction."));
            }
        }

        return violations;
    }

    // -------------------------------------------------------------------------

    private boolean isInjectionField(Field field) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            if (AnnotationNames.INJECTION_ANNOTATIONS.contains(
                    annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation(Field field, String annotationClassName) {
        for (java.lang.annotation.Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationClassName)) {
                return true;
            }
        }
        return false;
    }

    private void checkMutableCollection(Field field, List<FieldViolation> violations) {
        String typeName = field.getType().getName();

        boolean isMutableCollectionImpl = MUTABLE_COLLECTION_TYPES.contains(typeName);
        boolean isMutableCollectionInterface = MUTABLE_COLLECTION_INTERFACES.contains(typeName);

        if (isMutableCollectionImpl || isMutableCollectionInterface) {
            violations.add(
                    new FieldViolation(
                            field.getName(),
                            typeName,
                            ViolationType.MUTABLE_COLLECTION_FIELD,
                            String.format(
                                    "Field is declared as a mutable collection type '%s'. "
                                            + "Consider using an unmodifiable wrapper or immutable collection. "
                                            + "Mutable shared collections are not thread-safe.",
                                    typeName)));
        }
    }
}
