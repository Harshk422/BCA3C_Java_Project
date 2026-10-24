/**
 * MODULE 3 (part): Player Management Module (Dharmit)
 * ------------------------------------------------------------
 * Abstract parent class for every kind of player.
 * HumanPlayer and ComputerPlayer both EXTEND this class
 * (inheritance + abstraction).
 */
public abstract class Player {
    private final String name;
    private final char symbol;
    private int score;

    protected Player(String name, char symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    /** Each subclass says whether it is controlled by the computer. */
    public abstract boolean isComputer();

    /** Short description, e.g. "Human" or "Computer (HARD)". */
    public abstract String getType();

    public String getName()   { return name; }
    public char getSymbol()   { return symbol; }
    public int getScore()     { return score; }
    public void addWin()      { score++; }
    public void resetScore()  { score = 0; }

    @Override
    public String toString() {
        return name + " (" + symbol + ")";
    }
}
