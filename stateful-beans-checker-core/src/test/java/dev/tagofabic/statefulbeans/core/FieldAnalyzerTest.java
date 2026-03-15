package dev.tagofabic.statefulbeans.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.tagofabic.statefulbeans.core.analyzer.FieldAnalyzer;
import dev.tagofabic.statefulbeans.core.config.StatefulBeanCheckConfig;
import dev.tagofabic.statefulbeans.core.model.FieldViolation;
import dev.tagofabic.statefulbeans.core.model.ViolationType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FieldAnalyzerTest {

    private final FieldAnalyzer analyzer = new FieldAnalyzer();

    private StatefulBeanCheckConfig defaultConfig() {
        return StatefulBeanCheckConfig.builder()
                .packages("dev.tagofabic.statefulbeans.core")
                .build();
    }

    // --- Fixture classes ---

    static class CleanBean {
        private final String name;
        private final int count;

        CleanBean(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    static class MutableBean {
        private String mutableName; // non-final
        private int mutableCount; // non-final
        private final String immutable = "x";
    }

    static class CollectionBean {
        private final List<String> items = new ArrayList<>(); // mutable collection
    }

    // --- Tests ---

    @Test
    void cleanBean_producesNoViolations() {
        List<FieldViolation> violations = analyzer.analyzeFields(CleanBean.class, defaultConfig());
        assertTrue(violations.isEmpty(), "Expected no violations for a fully immutable bean");
    }

    @Test
    void mutableBean_flagsNonFinalFields() {
        List<FieldViolation> violations =
                analyzer.analyzeFields(MutableBean.class, defaultConfig());

        long nonFinalCount =
                violations.stream()
                        .filter(v -> v.getViolationType() == ViolationType.NON_FINAL_FIELD)
                        .count();

        assertEquals(
                2,
                nonFinalCount,
                "Expected 2 NON_FINAL_FIELD violations (mutableName, mutableCount)");
    }

    @Test
    void collectionBean_flagsMutableCollection() {
        List<FieldViolation> violations =
                analyzer.analyzeFields(CollectionBean.class, defaultConfig());

        boolean hasMutableCollection =
                violations.stream()
                        .anyMatch(
                                v ->
                                        v.getViolationType()
                                                == ViolationType.MUTABLE_COLLECTION_FIELD);

        assertTrue(
                hasMutableCollection,
                "Expected a MUTABLE_COLLECTION_FIELD violation for ArrayList field");
    }
}
