package dev.tagofabic.statefulbeans.core.analyzer;

import dev.tagofabic.statefulbeans.core.config.StatefulBeanCheckConfig;
import dev.tagofabic.statefulbeans.core.model.FieldViolation;
import dev.tagofabic.statefulbeans.core.model.ViolationType;
import dev.tagofabic.statefulbeans.core.util.AnnotationNames;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects public/package-private setter methods on a bean class that are <em>not</em> Spring
 * setter-injection points.
 *
 * <p>A method is considered a state-mutation setter when it:
 *
 * <ul>
 *   <li>Is named {@code setXxx} (JavaBean convention)
 *   <li>Has exactly one parameter
 *   <li>Is public or package-private (not private — those are less risky)
 *   <li>Is <em>not</em> annotated with a Spring/Jakarta injection annotation
 *   <li>Corresponds to a field on the class (best-effort name matching)
 * </ul>
 */
public class SetterMethodAnalyzer {

    public List<FieldViolation> analyzeSetters(Class<?> clazz, StatefulBeanCheckConfig config) {
        List<FieldViolation> violations = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!isSetterCandidate(method)) continue;
            if (isInjectionSetter(method)) continue;

            String fieldName = deriveFieldName(method.getName());
            Field correspondingField = findField(clazz, fieldName);
            String typeName =
                    correspondingField != null
                            ? correspondingField.getType().getName()
                            : method.getParameterTypes()[0].getName();

            violations.add(
                    new FieldViolation(
                            fieldName,
                            typeName,
                            ViolationType.MUTABLE_SETTER,
                            String.format(
                                    "Method '%s' is a public setter for field '%s'. "
                                            + "Non-injection setters on shared Spring Beans allow state "
                                            + "mutation after construction, which is not thread-safe.",
                                    method.getName(), fieldName)));
        }

        return violations;
    }

    // -------------------------------------------------------------------------

    private boolean isSetterCandidate(Method method) {
        String name = method.getName();
        if (!name.startsWith("set") || name.length() <= 3) return false;
        if (method.getParameterCount() != 1) return false;
        // Only flag non-private setters; private setters are rarely an issue
        int mod = method.getModifiers();
        return !Modifier.isPrivate(mod) && !Modifier.isStatic(mod);
    }

    private boolean isInjectionSetter(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (AnnotationNames.INJECTION_ANNOTATIONS.contains(
                    annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private String deriveFieldName(String setterName) {
        // setMyField → myField
        String suffix = setterName.substring(3);
        return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
