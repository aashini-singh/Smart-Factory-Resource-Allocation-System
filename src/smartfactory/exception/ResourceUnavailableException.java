package smartfactory.exception;

/**
 * Thrown when a task cannot get the resources it needs because they are
 * busy, out of stock or do not exist.
 *
 * Unit 3 concept: user-defined (custom) checked exception.
 */
public class ResourceUnavailableException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResourceUnavailableException(String message) {
        super(message);
    }
}
