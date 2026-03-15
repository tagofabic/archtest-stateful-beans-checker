package dev.tagofabic.statefulbeans.core.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** The complete result of a stateful-bean analysis run across one or more packages. */
public final class AnalysisResult {

    private final List<BeanViolation> violations;
    private final int totalBeansScanned;

    public AnalysisResult(List<BeanViolation> violations, int totalBeansScanned) {
        this.violations =
                Collections.unmodifiableList(Objects.requireNonNull(violations, "violations"));
        this.totalBeansScanned = totalBeansScanned;
    }

    public List<BeanViolation> getViolations() {
        return violations;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public int getTotalBeansScanned() {
        return totalBeansScanned;
    }

    /** Formats a human-readable failure message suitable for test assertion output. */
    public String toFailureMessage() {
        if (!hasViolations()) {
            return "No stateful bean violations found.";
        }
        String details =
                violations.stream()
                        .map(BeanViolation::toString)
                        .collect(Collectors.joining("\n\n"));
        return String.format(
                "Found %d stateful bean(s) out of %d scanned:%n%n%s",
                violations.size(), totalBeansScanned, details);
    }
}
