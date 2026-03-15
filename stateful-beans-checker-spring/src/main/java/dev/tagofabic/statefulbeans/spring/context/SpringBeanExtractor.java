package dev.tagofabic.statefulbeans.spring.context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Extracts a {@code beanName → beanClass} map from a live Spring {@link ApplicationContext},
 * filtering out infrastructure beans that are not user-authored (framework internals, Spring Boot
 * auto-configuration, etc.).
 */
public class SpringBeanExtractor {

    private static final Logger log = LoggerFactory.getLogger(SpringBeanExtractor.class);

    /**
     * Package prefixes considered Spring / Jakarta EE infrastructure that should not be subjected
     * to the statefulness check.
     */
    private static final Set<String> INFRASTRUCTURE_PREFIXES =
            Set.of(
                    "org.springframework.",
                    "org.apache.",
                    "com.sun.",
                    "sun.",
                    "java.",
                    "javax.",
                    "jakarta.",
                    "com.zaxxer.", // HikariCP
                    "io.micrometer.",
                    "ch.qos.logback.",
                    "org.slf4j.");

    /**
     * Extracts all user-defined bean classes from the context.
     *
     * @param applicationContext the live Spring context
     * @return map of {@code beanName → concrete class (proxy unwrapped)}
     */
    public Map<String, Class<?>> extractUserBeans(ApplicationContext applicationContext) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Map<String, Class<?>> result = new HashMap<>(beanNames.length);

        Arrays.stream(beanNames)
                .forEach(
                        name -> {
                            try {
                                Object bean = applicationContext.getBean(name);
                                Class<?> clazz = bean.getClass();

                                if (isInfrastructureClass(clazz)) {
                                    log.debug(
                                            "Skipping infrastructure bean: {} ({})",
                                            name,
                                            clazz.getName());
                                    return;
                                }

                                result.put(name, clazz);
                            } catch (Exception e) {
                                log.debug("Could not retrieve bean '{}': {}", name, e.getMessage());
                            }
                        });

        log.info(
                "Extracted {} user bean(s) from ApplicationContext (total definitions: {}).",
                result.size(),
                beanNames.length);
        return result;
    }

    // -------------------------------------------------------------------------

    private boolean isInfrastructureClass(Class<?> clazz) {
        String name = clazz.getName();
        for (String prefix : INFRASTRUCTURE_PREFIXES) {
            if (name.startsWith(prefix)) return true;
        }
        return false;
    }
}
