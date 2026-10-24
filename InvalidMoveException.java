/**
 * MODULE 6 (part): Input Validation Module (Harsh)
 * Thrown when a move is not allowed (box does not exist or is already filled).
 * Extends InvalidInputException (exception inheritance).
 */
public class InvalidMoveException extends InvalidInputException {
    public InvalidMoveException(String message) {
        super(message);
    }
}
