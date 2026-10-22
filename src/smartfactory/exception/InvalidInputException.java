package smartfactory.exception;

/**
 * Thrown when the user (or a data file) gives wrong input:
 * empty fields, numbers out of range, duplicate IDs, unknown IDs etc.
 *
 * Unit 3 concept: user-defined (custom) checked exception.
 */
public class InvalidInputException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
