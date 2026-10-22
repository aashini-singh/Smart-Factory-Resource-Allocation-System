package smartfactory.model;

import smartfactory.exception.InvalidInputException;

/**
 * Life cycle of a production task:
 * PENDING (waiting in queue) -> ALLOCATED (has resources) -> COMPLETED.
 */
public enum TaskStatus {
    PENDING,
    ALLOCATED,
    COMPLETED;

    /** Used while reading the tasks file. */
    public static TaskStatus fromString(String text) throws InvalidInputException {
        try {
            return TaskStatus.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidInputException("Invalid task status '" + text + "'.");
        }
    }
}
