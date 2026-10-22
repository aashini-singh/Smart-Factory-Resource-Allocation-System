package smartfactory.util;

import smartfactory.exception.InvalidInputException;

/**
 * Small helper with common input checks used by the model classes.
 * All methods are static, so no object of this class is ever created.
 */
public final class Validator {

    /** Marker used everywhere in the project for "nothing / not required". */
    public static final String NONE = "-";

    private Validator() {
        // utility class - no objects
    }

    /** Text must not be empty, must not contain a comma (used as file separator) and must not be "-". */
    public static String requireText(String value, String fieldName) throws InvalidInputException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidInputException(fieldName + " cannot be empty.");
        }
        String text = value.trim();
        if (text.contains(",")) {
            throw new InvalidInputException(fieldName + " cannot contain a comma (,).");
        }
        if (text.equals(NONE)) {
            throw new InvalidInputException(fieldName + " cannot be '" + NONE + "'.");
        }
        return text;
    }

    /** Same as requireText, but empty input or "-" is allowed and means "not required". */
    public static String optionalText(String value, String fieldName) throws InvalidInputException {
        if (value == null || value.trim().isEmpty() || value.trim().equals(NONE)) {
            return NONE;
        }
        return requireText(value, fieldName);
    }

    /** Number must be between min and max (both included). */
    public static int requireRange(int value, int min, int max, String fieldName) throws InvalidInputException {
        if (value < min || value > max) {
            throw new InvalidInputException(fieldName + " must be between " + min + " and " + max + ".");
        }
        return value;
    }
}
