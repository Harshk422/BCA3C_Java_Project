import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;
import javax.swing.SwingUtilities;

/**
 * MODULE 5: Computer Player (AI) Module (Harsh)
 * ------------------------------------------------------------
 * Lets a single player play against the computer.
 *   EASY   - picks a random empty box.
 *   MEDIUM - plays the best move about 60% of the time, otherwise random
 *            (so a clever human can beat it).
 *   HARD   - uses the Minimax algorithm (with alpha-beta pruning)
 *          and never loses.
 *
 * Syllabus topics shown here:
 *   - Inheritance: extends Player
 *   - Multi-threading: the move is calculated in its own Thread
 *     so the window never freezes
 */
public class ComputerPlayer extends Player {

    public enum Difficulty { EASY, MEDIUM, HARD }

    private final Difficulty difficulty;
    private final char opponentSymbol;
    private final Random random = new Random();

    public ComputerPlayer(String name, char symbol, char opponentSymbol, Difficulty difficulty) {
        super(name, symbol);
        this.opponentSymbol = opponentSymbol;
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public boolean isComputer() {
        return true;
    }

    @Override
    public String getType() {
        return "Computer (" + difficulty + ")";
    }

    /** Returns the chosen box number (1-9). */
    public int chooseMove(Board board) {
        switch (difficulty) {
            case EASY:   return randomMove(board);
            case MEDIUM: return random.nextInt(100) < 60 ? bestMove(board) : randomMove(board);
            default:     return bestMove(board);
        }
    }

    /**
     * Calculates the move in a background Thread (with a short "thinking" pause),
     * then hands the result back to the Swing event thread through the callback.
     * The caller can interrupt() and join() the returned thread to cancel it.
     */
    public Thread chooseMoveAsync(Board board, IntConsumer callback) {
        final Board snapshot = board.copy();
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;                                   // cancelled
            }
            final int move = chooseMove(snapshot);
            SwingUtilities.invokeLater(() -> callback.accept(move));
        }, "Computer-Thread");
        worker.setDaemon(true);
        worker.start();
        return worker;
    }

    // ---------------- EASY ----------------
    private int randomMove(Board board) {
        List<Integer> free = board.getEmptyCells();
        return free.get(random.nextInt(free.size()));
    }

    // ---------------- HARD (Minimax) ----------------
    private int bestMove(Board board) {
        int bestScore = Integer.MIN_VALUE;
        int best = -1;
        for (int cell : board.getEmptyCells()) {
            board.place(cell, getSymbol());
            int score = minimax(board, false, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            board.clear(cell);
            if (score > bestScore) {
                bestScore = score;
                best = cell;
            }
        }
        return best;
    }

    private int minimax(Board board, boolean computerTurn, int depth, int alpha, int beta) {
        char winner = GameLogic.getWinner(board);
        if (winner == getSymbol())  return 10 - depth;    // faster win is better
        if (winner == opponentSymbol) return depth - 10;  // slower loss is better
        if (board.isFull()) return 0;

        if (computerTurn) {
            int best = Integer.MIN_VALUE;
            for (int cell : board.getEmptyCells()) {
                board.place(cell, getSymbol());
                best = Math.max(best, minimax(board, false, depth + 1, alpha, beta));
                board.clear(cell);
                alpha = Math.max(alpha, best);
                if (beta <= alpha) break;                 // pruning
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int cell : board.getEmptyCells()) {
                board.place(cell, opponentSymbol);
                best = Math.min(best, minimax(board, true, depth + 1, alpha, beta));
                board.clear(cell);
                beta = Math.min(beta, best);
                if (beta <= alpha) break;
            }
            return best;
        }
    }
}
