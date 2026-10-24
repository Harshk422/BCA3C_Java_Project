import java.util.ArrayList;
import java.util.List;

/**
 * MODULE 1: Game Board Module (Dharmit)
 * ------------------------------------------------------------
 * Creates the 3x3 grid, keeps track of which cell has X, O or is
 * empty, and resets the board when a new game starts.
 * Cells are numbered 1-9:   1 | 2 | 3
 *                           4 | 5 | 6
 *                           7 | 8 | 9
 */
public class Board {
    public static final char EMPTY = ' ';
    public static final int SIZE = 3;
    public static final int CELLS = SIZE * SIZE;

    private final char[] cells = new char[CELLS + 1];   // index 0 is unused

    public Board() {
        reset();
    }

    /** Clears every cell (used when a new round starts). */
    public void reset() {
        for (int i = 1; i <= CELLS; i++) {
            cells[i] = EMPTY;
        }
    }

    public boolean isValidCell(int cell) {
        return cell >= 1 && cell <= CELLS;
    }

    /** Places a symbol. Returns false if the cell does not exist or is already taken. */
    public boolean place(int cell, char symbol) {
        if (!isValidCell(cell) || cells[cell] != EMPTY) {
            return false;
        }
        cells[cell] = symbol;
        return true;
    }

    /** Removes a symbol (needed by the AI while it searches moves). */
    public void clear(int cell) {
        if (isValidCell(cell)) {
            cells[cell] = EMPTY;
        }
    }

    public char getCell(int cell) {
        return cells[cell];
    }

    public boolean isCellEmpty(int cell) {
        return isValidCell(cell) && cells[cell] == EMPTY;
    }

    public boolean isFull() {
        for (int i = 1; i <= CELLS; i++) {
            if (cells[i] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    public List<Integer> getEmptyCells() {
        List<Integer> free = new ArrayList<>();
        for (int i = 1; i <= CELLS; i++) {
            if (cells[i] == EMPTY) {
                free.add(i);
            }
        }
        return free;
    }

    /** Independent copy, so the AI thread never touches the board shown on screen. */
    public Board copy() {
        Board b = new Board();
        System.arraycopy(cells, 0, b.cells, 0, cells.length);
        return b;
    }
}
