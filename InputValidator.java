/**
 * MODULE 6: Input Validation Module (Harsh)
 * ------------------------------------------------------------
 * Makes sure the player enters a valid move (correct box number,
 * not already filled) and a valid name. Wrong input is reported
 * with a custom exception, and the GUI shows the message.
 */
public class InputValidator {
    public static final int MAX_NAME_LENGTH = 12;

    /** Checks a box number that came from a mouse click. */
    public int validateMove(int cell, Board board) throws InvalidMoveException {
        if (!board.isValidCell(cell)) {
            throw new InvalidMoveException("Box " + cell + " does not exist. Choose between 1 and 9.");
        }
        if (!board.isCellEmpty(cell)) {
            throw new InvalidMoveException("Box " + cell + " is already filled. Choose an empty box.");
        }
        return cell;
    }

    /** Checks a box number typed as text (overloaded version). */
    public int validateMove(String input, Board board) throws InvalidMoveException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidMoveException("Please enter a number from 1 to 9.");
        }
        int cell;
        try {
            cell = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new InvalidMoveException("'" + input.trim() + "' is not a number. Type 1 to 9.");
        }
        return validateMove(cell, board);
    }

    /** Cleans and checks a player name. An empty name becomes the fallback. */
    public String validateName(String raw, String fallback) throws InvalidInputException {
        String name = (raw == null) ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            return fallback;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidInputException("Name can have at most " + MAX_NAME_LENGTH + " characters.");
        }
        if (!name.matches("[\\p{L}\\p{M}\\p{N} _-]+")) {
            throw new InvalidInputException("Use only letters, numbers, spaces, _ or -.");
        }
        return name;
    }
}
