package io.github.statefulbeans.core.model;

/** Describes why a field or class was flagged as a statefulness violation. */
public enum ViolationType {

    /**
     * Field is not {@code final} and is not a recognised injection point ({@code @Autowired},
     * {@code @Inject}, {@code @Value}, {@code @Resource}).
     */
    NON_FINAL_FIELD,

    /**
     * Field has a public or package-private setter method that is not a Spring setter-injection
     * method.
     */
    MUTABLE_SETTER,

    /**
     * Field type is a mutable collection ({@code List}, {@code Map}, {@code Set}, etc.) and is not
     * wrapped in an unmodifiable view or declared as an immutable type.
     */
    MUTABLE_COLLECTION_FIELD,

    /** Class carries Lombok {@code @Data} which generates setters for every field. */
    LOMBOK_DATA,

    /** Class or field carries Lombok {@code @Setter}. */
    LOMBOK_SETTER,

    /**
     * Class carries Lombok {@code @Builder} with {@code @Singular} mutable accumulator fields left
     * accessible after construction.
     */
    LOMBOK_SINGULAR_BUILDER
}
