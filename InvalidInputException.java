/**
 * MODULE 6 (part): Input Validation Module (Harsh)
 * Custom CHECKED exception for any wrong input (for example a bad name).
 */
public class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}
