package io.github.statefulbeans.core.model;

import java.util.Objects;

/** Represents a single field-level statefulness violation detected on a Spring Bean class. */
public final class FieldViolation {

    private final String fieldName;
    private final String fieldTypeName;
    private final ViolationType violationType;
    private final String description;

    public FieldViolation(
            String fieldName,
            String fieldTypeName,
            ViolationType violationType,
            String description) {
        this.fieldName = Objects.requireNonNull(fieldName, "fieldName");
        this.fieldTypeName = Objects.requireNonNull(fieldTypeName, "fieldTypeName");
        this.violationType = Objects.requireNonNull(violationType, "violationType");
        this.description = Objects.requireNonNull(description, "description");
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public ViolationType getViolationType() {
        return violationType;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format(
                "  Field '%s' (%s) — %s: %s", fieldName, fieldTypeName, violationType, description);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldViolation that)) return false;
        return fieldName.equals(that.fieldName) && violationType == that.violationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, violationType);
    }
}
