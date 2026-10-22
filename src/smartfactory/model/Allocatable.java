package smartfactory.model;

import smartfactory.exception.ResourceUnavailableException;

/**
 * Common allocation behaviour that every resource must provide.
 *
 * Unit 3 concept: interface. Because all resources are used only through
 * these methods, a new resource type (for example a Robot) can be added
 * later without changing the allocation logic.
 */
public interface Allocatable {

    /** true if the resource can be given to a task right now. */
    boolean isAvailable();

    /** true if this resource is free AND matches what the task needs. */
    boolean canFulfil(Task task);

    /** Gives this resource to the task. */
    void allocate(Task task) throws ResourceUnavailableException;

    /** Takes the resource back after the task is completed. */
    void release(Task task);
}
