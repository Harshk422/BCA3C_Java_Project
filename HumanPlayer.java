/**
 * MODULE 3 (part): Player Management Module (Dharmit)
 * A real person who plays by clicking on the board.
 */
public class HumanPlayer extends Player {

    public HumanPlayer(String name, char symbol) {
        super(name, symbol);
    }

    @Override
    public boolean isComputer() {
        return false;
    }

    @Override
    public String getType() {
        return "Human";
    }
}
