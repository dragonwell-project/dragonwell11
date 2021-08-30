package com.alibaba.rcm;

import java.util.Arrays;

/**
 * A {@code Constraint} is a pair of {@link ResourceType} and {@code values}.
 * The constrained resource and specification of parameter values are documented
 * in enum constants in {@link ResourceType}
 * <p>
 * A {@code Constraint} must be associated with one {@code ResourceType}, so we
 * provide factory method {@link ResourceType#newConstraint(long...)} to implement
 * this restriction. For example:
 * <pre>
 *   Constraint cpuConstraint = ResourceType.CPU_PERCENT.newConstraint(30);
 * </pre>
 * <p>
 * {@code ResourceContainer} and  {@code Constraint} follow the one-to-many relationship.
 * {@link ResourceContainer#getConstraints()} can fetch the Constraint associated with
 * ResourceContainer.
 */
public class Constraint {
    private final ResourceType type;
    private final long[] values;

    /**
     * Constraint should be instantiated by {@link ResourceType#newConstraint(long...)}
     */
    protected Constraint(ResourceType type, long[] values) {
        this.type = type;
        this.values = values;
    }

    /**
     * Returns the currently restricted resource type.
     *
     * @return resource type
     */
    public ResourceType getResourceType() {
        return type;
    }

    /**
     * Returns the constraint value of ResourceType described by a long[],
     * which is documented on the ResourceType enums.
     * <p>
     * The returned value is a copy of the internal storage array to prevent
     * modification.
     *
     * @return constraint values
     */
    public long[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public String toString() {
        return "Constraint{" +
                "type=" + type +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
