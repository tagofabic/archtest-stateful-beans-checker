package dev.tagofabic.statefulbeans.core.util;

import java.util.Set;

/**
 * Centralised string constants for annotation class names used across the library. Using strings
 * avoids hard compile-time dependencies on Spring / Lombok.
 */
public final class AnnotationNames {

    private AnnotationNames() {}

    // --- Spring configuration ---
    public static final String CONFIGURATION_PROPERTIES =
            "org.springframework.boot.context.properties.ConfigurationProperties";

    // --- Spring stereotypes ---
    public static final String COMPONENT = "org.springframework.stereotype.Component";
    public static final String SERVICE = "org.springframework.stereotype.Service";
    public static final String REPOSITORY = "org.springframework.stereotype.Repository";
    public static final String CONTROLLER = "org.springframework.stereotype.Controller";
    public static final String REST_CONTROLLER =
            "org.springframework.web.bind.annotation.RestController";
    public static final String CONFIGURATION =
            "org.springframework.context.annotation.Configuration";
    public static final String MANAGED_BEAN = "jakarta.annotation.ManagedBean";
    public static final String NAMED = "jakarta.inject.Named";

    /** All annotations that mark a class as a Spring-managed bean. */
    public static final Set<String> SPRING_BEAN_ANNOTATIONS =
            Set.of(
                    COMPONENT,
                    SERVICE,
                    REPOSITORY,
                    CONTROLLER,
                    REST_CONTROLLER,
                    CONFIGURATION,
                    MANAGED_BEAN,
                    NAMED);

    // --- Spring injection annotations (field-level) ---
    public static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    public static final String VALUE = "org.springframework.beans.factory.annotation.Value";
    public static final String RESOURCE = "jakarta.annotation.Resource";
    public static final String INJECT = "jakarta.inject.Inject";

    /** Annotations that indicate a field is an injection point, not mutable state. */
    public static final Set<String> INJECTION_ANNOTATIONS =
            Set.of(AUTOWIRED, VALUE, RESOURCE, INJECT);

    // --- Lombok annotations ---
    public static final String LOMBOK_DATA = "lombok.Data";
    public static final String LOMBOK_SETTER = "lombok.Setter";
    public static final String LOMBOK_GETTER = "lombok.Getter";
    public static final String LOMBOK_VALUE = "lombok.Value"; // immutable – OK
    public static final String LOMBOK_BUILDER = "lombok.Builder";
    public static final String LOMBOK_SINGULAR = "lombok.Singular";

    /** Lombok annotations that imply mutability at the class level. */
    public static final Set<String> LOMBOK_MUTABLE_CLASS_ANNOTATIONS =
            Set.of(LOMBOK_DATA, LOMBOK_SETTER);
}
