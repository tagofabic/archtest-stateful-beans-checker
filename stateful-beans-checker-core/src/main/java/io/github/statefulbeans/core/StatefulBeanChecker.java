package io.github.statefulbeans.core;

import io.github.statefulbeans.core.analyzer.BeanClassAnalyzer;
import io.github.statefulbeans.core.analyzer.ClasspathPackageScanner;
import io.github.statefulbeans.core.config.StatefulBeanCheckConfig;
import io.github.statefulbeans.core.model.AnalysisResult;
import io.github.statefulbeans.core.model.BeanViolation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Primary entry point for stateful-bean analysis.
 *
 * <p>Supports two scanning strategies:
 *
 * <ol>
 *   <li><b>Package scan</b> — Scans the classpath for classes carrying Spring stereotype
 *       annotations inside the configured packages. No Spring context is required.
 *   <li><b>Bean map</b> — Analyses a pre-collected {@code Map<String, Class<?>>} of {@code beanName
 *       → beanClass} pairs (typically sourced from a live Spring {@code ApplicationContext}).
 * </ol>
 *
 * <pre>{@code
 * StatefulBeanCheckConfig config = StatefulBeanCheckConfig.builder()
 *     .packages("com.myapp.service")
 *     .lombokAware(true)
 *     .build();
 *
 * AnalysisResult result = new StatefulBeanChecker(config).checkPackages();
 * if (result.hasViolations()) {
 *     throw new AssertionError(result.toFailureMessage());
 * }
 * }</pre>
 */
public class StatefulBeanChecker {

    private static final Logger log = LoggerFactory.getLogger(StatefulBeanChecker.class);

    private final StatefulBeanCheckConfig config;
    private final BeanClassAnalyzer beanClassAnalyzer;
    private final ClasspathPackageScanner packageScanner;

    public StatefulBeanChecker(StatefulBeanCheckConfig config) {
        this.config = config;
        this.beanClassAnalyzer = new BeanClassAnalyzer();
        this.packageScanner = new ClasspathPackageScanner();
    }

    /**
     * Scans configured packages on the classpath, filters to Spring-annotated classes, and analyses
     * each one.
     */
    public AnalysisResult checkPackages() throws IOException {
        if (config.getPackagesToScan().isEmpty()) {
            throw new IllegalStateException(
                    "No packages configured for scanning. "
                            + "Use StatefulBeanCheckConfig.builder().packages(...).build()");
        }

        List<Class<?>> candidates = new ArrayList<>();
        for (String pkg : config.getPackagesToScan()) {
            log.debug("Scanning package: {}", pkg);
            packageScanner.scanPackage(pkg).stream()
                    .filter(BeanClassAnalyzer::isSpringBeanClass)
                    .forEach(candidates::add);
        }

        return analyzeClasses(candidates, null);
    }

    /**
     * Analyses a pre-supplied map of bean names to bean classes (e.g. from a Spring {@code
     * ApplicationContext}).
     *
     * @param beanMap {@code beanName → beanClass}; class may be a CGLIB proxy — the analyser will
     *     unwrap it automatically
     */
    public AnalysisResult checkBeans(Map<String, Class<?>> beanMap) {
        List<Class<?>> classes = new ArrayList<>(beanMap.size());
        List<String> names = new ArrayList<>(beanMap.size());

        beanMap.forEach(
                (name, clazz) -> {
                    classes.add(unwrapProxy(clazz));
                    names.add(name);
                });

        return analyzeNamedClasses(classes, names);
    }

    // -------------------------------------------------------------------------

    private AnalysisResult analyzeClasses(List<Class<?>> classes, List<String> names) {
        List<BeanViolation> violations = new ArrayList<>();
        int total = classes.size();

        for (int i = 0; i < classes.size(); i++) {
            Class<?> clazz = classes.get(i);
            String name = (names != null && i < names.size()) ? names.get(i) : null;
            Optional<BeanViolation> violation = beanClassAnalyzer.analyze(clazz, name, config);
            violation.ifPresent(violations::add);
        }

        log.info(
                "Analysis complete. Scanned {} beans, found {} violations.",
                total,
                violations.size());
        return new AnalysisResult(violations, total);
    }

    private AnalysisResult analyzeNamedClasses(List<Class<?>> classes, List<String> names) {
        return analyzeClasses(classes, names);
    }

    /**
     * Unwraps CGLIB/Spring-generated proxy subclasses to their original target class. CGLIB proxies
     * have "$$" in their class name.
     */
    private Class<?> unwrapProxy(Class<?> clazz) {
        if (clazz.getName().contains("$$")) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return superclass;
            }
        }
        return clazz;
    }
}
