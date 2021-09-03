package com.alibaba.rcm;

/**
 * Factory class for {@link ResourceContainer}.
 * <p>
 * Each ResourceContainer implementation needs to provide a public
 * ResourceContainerFactory instance to allow users to choose a specific
 * ResourceContainer implementation:
 *
 * <pre>
 * ResourceContainerFactory FACTORY_INSTANCE = new ResourceContainerFactory() {
 *     protected ResourceContainer createContainer(Iterable<Constraint> constraints) {
 *         return new AbstractResourceContainer() {
 *             // implement abstract methods
 *         }
 *     }
 * }
 * </pre>
 *
 * Then API users can create ResourceContainer by
 * {@code FACTORY_INSTANCE.createContainer(...)}
 */
public interface ResourceContainerFactory {
    /**
     * Builds ResourceContainer with constraints.
     *
     * @param constraints the target {@code Constraint}s
     * @return a newly-created ResourceContainer
     */
    ResourceContainer createContainer(Iterable<Constraint> constraints);
}