/**
 * MODULE 2: Game Logic / Rules Module (Dharmit)
 * ------------------------------------------------------------
 * Checks after every move whether someone has won (3 in a row,
 * column or diagonal) or whether the game is a draw.
 */
public final class GameLogic {

    private GameLogic() { }   // only static helpers, no objects needed

    public static final int[][] LINES = {
        {1, 2, 3}, {4, 5, 6}, {7, 8, 9},    // rows
        {1, 4, 7}, {2, 5, 8}, {3, 6, 9},    // columns
        {1, 5, 9}, {3, 5, 7}                // diagonals
    };

    /** Returns the three winning cells, or null if nobody has won yet. */
    public static int[] getWinningLine(Board board) {
        for (int[] line : LINES) {
            char a = board.getCell(line[0]);
            if (a != Board.EMPTY && a == board.getCell(line[1]) && a == board.getCell(line[2])) {
                return line.clone();
            }
        }
        return null;
    }

    /** Returns 'X' or 'O' if that symbol has won, otherwise Board.EMPTY. */
    public static char getWinner(Board board) {
        int[] line = getWinningLine(board);
        return line == null ? Board.EMPTY : board.getCell(line[0]);
    }

    /** Draw = board is full and nobody has won. */
    public static boolean isDraw(Board board) {
        return board.isFull() && getWinningLine(board) == null;
    }

    public static boolean isGameOver(Board board) {
        return getWinningLine(board) != null || board.isFull();
    }
}
