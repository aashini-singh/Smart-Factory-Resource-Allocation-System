package smartfactory.model;

import smartfactory.exception.InvalidInputException;

/**
 * Priority level of a production task. A higher level is served first.
 */
public enum Priority {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /** Accepts HIGH / MEDIUM / LOW or the short forms H / M / L (any case). */
    public static Priority fromString(String text) throws InvalidInputException {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidInputException("Priority cannot be empty.");
        }
        String value = text.trim().toUpperCase();
        if (value.equals("H") || value.equals("HIGH")) {
            return HIGH;
        }
        if (value.equals("M") || value.equals("MEDIUM")) {
            return MEDIUM;
        }
        if (value.equals("L") || value.equals("LOW")) {
            return LOW;
        }
        throw new InvalidInputException("Invalid priority '" + text.trim() + "'. Use HIGH, MEDIUM or LOW.");
    }
}
