/**
 * MODULE 3: Player Management Module (Dharmit)
 * ------------------------------------------------------------
 * Keeps Player 1 and Player 2, switches turns after every move
 * and keeps the score across multiple rounds.
 */
public class PlayerManager {
    private final Player player1;
    private final Player player2;
    private Player current;
    private int draws = 0;
    private int roundsPlayed = 0;

    public PlayerManager(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.current = player1;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Player getCurrent() { return current; }
    public int getDraws()      { return draws; }
    public int getRoundsPlayed() { return roundsPlayed; }

    public void switchTurn() {
        current = (current == player1) ? player2 : player1;
    }

    /** The starter alternates every round so the game stays fair. */
    public void startNewRound() {
        current = (roundsPlayed % 2 == 0) ? player1 : player2;
    }

    public Player getPlayerBySymbol(char symbol) {
        return player1.getSymbol() == symbol ? player1 : player2;
    }

    public void recordWin(Player winner) {
        winner.addWin();
        roundsPlayed++;
    }

    public void recordDraw() {
        draws++;
        roundsPlayed++;
    }

    public void resetScores() {
        player1.resetScore();
        player2.resetScore();
        draws = 0;
        roundsPlayed = 0;
    }

    public String scoreLine() {
        return player1 + ": " + player1.getScore()
             + "    \u2022    " + player2 + ": " + player2.getScore()
             + "    \u2022    Draws: " + draws;
    }
}
