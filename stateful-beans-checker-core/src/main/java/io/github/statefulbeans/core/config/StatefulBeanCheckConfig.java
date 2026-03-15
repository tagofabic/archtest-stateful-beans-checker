package io.github.statefulbeans.core.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable configuration for a stateful-bean check run.
 * Construct via {@link Builder}.
 *
 * <pre>{@code
 * StatefulBeanCheckConfig config = StatefulBeanCheckConfig.builder()
 *     .packages("com.myapp.service", "com.myapp.component")
 *     .lombokAware(true)
 *     .excludePackages("com.myapp.service.legacy")
 *     .build();
 * }</pre>
 */
public final class StatefulBeanCheckConfig {

    /** Packages to scan for Spring stereotype-annotated classes. */
    private final Set<String> packagesToScan;

    /** Packages whose classes are excluded from analysis. */
    private final Set<String> excludedPackages;

    /**
     * Fully-qualified annotation names whose presence on a class exempts it
     * from the check (e.g. "org.springframework.web.bind.annotation.RestController"
     * if you intentionally allow mutable state there).
     */
    private final Set<String> excludedAnnotations;

    /** Fully-qualified class names to skip entirely. */
    private final Set<String> excludedClasses;

    /**
     * When {@code true}, the analyser treats Lombok {@code @Data}, {@code @Setter},
     * and {@code @Singular} on a bean class as violations even if no explicit
     * Java setter method can be found in the compiled bytecode yet (annotation
     * processors run at compile time so the library user's test classpath may
     * already have the generated methods — but this flag provides an explicit
     * signal to check for them).
     */
    private final boolean lombokAware;

    /**
     * When {@code true}, non-final fields that are recognised Spring injection
     * annotations ({@code @Autowired}, {@code @Inject}, {@code @Value},
     * {@code @Resource}) are <em>not</em> flagged. Defaults to {@code true}.
     */
    private final boolean allowInjectedFields;

    /**
     * When {@code true}, {@code ThreadLocal} fields are not flagged because
     * they provide per-thread isolation. Defaults to {@code true}.
     */
    private final boolean allowThreadLocalFields;

    private StatefulBeanCheckConfig(Builder builder) {
        this.packagesToScan = Collections.unmodifiableSet(new HashSet<>(builder.packagesToScan));
        this.excludedPackages = Collections.unmodifiableSet(new HashSet<>(builder.excludedPackages));
        this.excludedAnnotations = Collections.unmodifiableSet(new HashSet<>(builder.excludedAnnotations));
        this.excludedClasses = Collections.unmodifiableSet(new HashSet<>(builder.excludedClasses));
        this.lombokAware = builder.lombokAware;
        this.allowInjectedFields = builder.allowInjectedFields;
        this.allowThreadLocalFields = builder.allowThreadLocalFields;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> getPackagesToScan() { return packagesToScan; }
    public Set<String> getExcludedPackages() { return excludedPackages; }
    public Set<String> getExcludedAnnotations() { return excludedAnnotations; }
    public Set<String> getExcludedClasses() { return excludedClasses; }
    public boolean isLombokAware() { return lombokAware; }
    public boolean isAllowInjectedFields() { return allowInjectedFields; }
    public boolean isAllowThreadLocalFields() { return allowThreadLocalFields; }

    // -------------------------------------------------------------------------

    public static final class Builder {

        private final Set<String> packagesToScan = new HashSet<>();
        private final Set<String> excludedPackages = new HashSet<>();
        private final Set<String> excludedAnnotations = new HashSet<>();
        private final Set<String> excludedClasses = new HashSet<>();
        private boolean lombokAware = false;
        private boolean allowInjectedFields = true;
        private boolean allowThreadLocalFields = true;

        private Builder() {}

        public Builder packages(String... packages) {
            this.packagesToScan.addAll(Arrays.asList(packages));
            return this;
        }

        public Builder excludePackages(String... packages) {
            this.excludedPackages.addAll(Arrays.asList(packages));
            return this;
        }

        /**
         * Exclude classes carrying any of the given annotation types.
         * Pass fully-qualified annotation class names as strings so the
         * core module does not need the annotations on its compile classpath.
         */
        public Builder excludeAnnotations(String... annotationClassNames) {
            this.excludedAnnotations.addAll(Arrays.asList(annotationClassNames));
            return this;
        }

        public Builder excludeClasses(Class<?>... classes) {
            for (Class<?> c : classes) {
                this.excludedClasses.add(c.getName());
            }
            return this;
        }

        public Builder excludeClassNames(String... classNames) {
            this.excludedClasses.addAll(Arrays.asList(classNames));
            return this;
        }

        /** Enable Lombok-aware detection of {@code @Data}, {@code @Setter}, etc. */
        public Builder lombokAware(boolean lombokAware) {
            this.lombokAware = lombokAware;
            return this;
        }

        /**
         * Control whether fields annotated with Spring/Jakarta injection
         * annotations are exempted. Default {@code true}.
         */
        public Builder allowInjectedFields(boolean allow) {
            this.allowInjectedFields = allow;
            return this;
        }

        /**
         * Control whether {@code ThreadLocal} fields are exempted.
         * Default {@code true}.
         */
        public Builder allowThreadLocalFields(boolean allow) {
            this.allowThreadLocalFields = allow;
            return this;
        }

        public StatefulBeanCheckConfig build() {
            return new StatefulBeanCheckConfig(this);
        }
    }
}
