package io.github.statefulbeans.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates all violations found on a single Spring Bean class.
 */
public final class BeanViolation {

    private final String beanClassName;
    private final String beanName;
    private final List<FieldViolation> fieldViolations;
    private final List<ViolationType> classLevelViolations;

    public BeanViolation(String beanClassName, String beanName,
                         List<FieldViolation> fieldViolations,
                         List<ViolationType> classLevelViolations) {
        this.beanClassName = Objects.requireNonNull(beanClassName, "beanClassName");
        this.beanName = beanName != null ? beanName : beanClassName;
        this.fieldViolations = Collections.unmodifiableList(
                Objects.requireNonNull(fieldViolations, "fieldViolations"));
        this.classLevelViolations = Collections.unmodifiableList(
                Objects.requireNonNull(classLevelViolations, "classLevelViolations"));
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public String getBeanName() {
        return beanName;
    }

    public List<FieldViolation> getFieldViolations() {
        return fieldViolations;
    }

    public List<ViolationType> getClassLevelViolations() {
        return classLevelViolations;
    }

    public boolean hasViolations() {
        return !fieldViolations.isEmpty() || !classLevelViolations.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Bean '").append(beanName).append("' (").append(beanClassName).append(")");
        if (!classLevelViolations.isEmpty()) {
            sb.append("\n  Class-level: ").append(classLevelViolations);
        }
        fieldViolations.forEach(v -> sb.append("\n").append(v));
        return sb.toString();
    }
}
