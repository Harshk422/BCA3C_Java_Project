import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * MODULE 4: User Interface Module (Harsh)
 * ------------------------------------------------------------
 * The main window (opens MAXIMISED / full screen). It shows the menu,
 * the game board and the score history, and CONNECTS all the other
 * modules together:
 *
 *   click -> InputValidator checks the move -> Board is updated ->
 *   GameLogic checks win/draw -> PlayerManager switches turn
 *   (or ComputerPlayer plays in its own thread) ->
 *   ScoreHistory saves the result -> next round starts AUTOMATICALLY.
 *
 * Extra features: best-of match series with confetti, Hint and Undo
 * power-ups, sound effects, Medium difficulty and a player leaderboard.
 * Look: NEON theme (gradient background, glowing board, neon buttons, score cards).
 */
public class GameWindow extends JFrame implements BoardPanel.CellClickListener {

    private static final int TURN_SECONDS = 10;      // FEATURE: turn timer

    private static final int NEXT_ROUND_SECONDS = 3;
    private static final int NEW_MATCH_SECONDS = 6;
    private static final int MAX_UNDOS = 2;
    private static final int MAX_HINTS = 3;

    // ----- modules -----
    private final Board board = new Board();
    private final InputValidator validator = new InputValidator();
    private final ScoreHistory history = new ScoreHistory();
    private final Achievements achievements = new Achievements();   // FEATURE: badges & streaks
    private PlayerManager pm;
    private ComputerPlayer ai;

    // ----- GUI parts -----
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final BoardPanel boardPanel = new BoardPanel(board);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final ScoreCard card1 = new ScoreCard("X", Theme.CYAN);
    private final ScoreCard cardDraw = new ScoreCard("=", Theme.GOLD);
    private final ScoreCard card2 = new ScoreCard("O", Theme.PINK);
    private final JLabel infoLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel toastLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel badgeSummary = new JLabel(" ", SwingConstants.CENTER);
    private final DefaultTableModel badgeModel = new DefaultTableModel(
            new String[]{"Badge", "How to unlock", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JProgressBar turnBar = new JProgressBar(0, TURN_SECONDS * 1000);
    private RoundedButton timerButton;
    private final JLabel historySummary = new JLabel(" ", SwingConstants.CENTER);
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Date & Time", "Player 1", "Player 2", "Result"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final DefaultTableModel statsModel = new DefaultTableModel(
            new String[]{"Player", "Rounds", "Wins", "Draws", "Losses", "Win %"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private RoundedButton undoButton;
    private RoundedButton hintButton;
    private RoundedButton soundButton;

    // ----- game state -----
    private boolean roundOver = true;
    private int roundToken = 0;          // changes whenever a round starts/ends, to ignore late AI moves & timers
    private Thread computerThread;
    private Timer autoTimer;             // counts down to the automatic next round
    private Timer hintTimer;
    private int targetWins = 0;          // 0 = endless, otherwise "first to N wins the match"
    private int undosLeft, hintsLeft;
    private boolean timerOn = false;     // FEATURE: turn timer (off until the player switches it on)
    private Timer turnTimer;
    private long turnDeadline;
    private boolean usedHelp;            // Undo or Hint used this round (for the Flawless badge)
    private boolean inGame;              // true while the board screen is shown (keyboard shortcuts)
    private final Random random = new Random();
    private final Deque<Integer> moveStack = new ArrayDeque<>();   // moves of this round (for Undo)

    public GameWindow() {
        super("Tic Tac Toe - Java Mini Project");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        root.add(buildMenuScreen(), "menu");
        root.add(buildGameScreen(), "game");
        root.add(buildHistoryScreen(), "history");
        root.add(buildBadgeScreen(), "badges");
        setContentPane(root);
        setIconImage(createIcon());
        setSize(900, 760);                          // size used if the user "restores" the window
        setMinimumSize(new Dimension(640, 640));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);    // open full screen straight away
        boardPanel.setCellClickListener(this);
        turnTimer = new Timer(100, e -> onTurnTick());
        setupKeyboardShortcuts();
        showMenu();
    }

    // =====================================================================
    //  SCREENS
    // =====================================================================
    private JPanel buildMenuScreen() {
        JPanel outer = new GradientPanel(new GridBagLayout());

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        TitlePanel title = new TitlePanel(64f, new Dimension(760, 130));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(title);
        col.add(Box.createVerticalStrut(14));

        addMenuButton(col, "Player vs Player", Theme.VIOLET, e -> startSession(null));
        addMenuButton(col, "Computer - Easy", Theme.MINT,
                e -> startSession(ComputerPlayer.Difficulty.EASY));
        addMenuButton(col, "Computer - Medium", Theme.ORANGE,
                e -> startSession(ComputerPlayer.Difficulty.MEDIUM));
        addMenuButton(col, "Computer - Hard", Theme.PINK,
                e -> startSession(ComputerPlayer.Difficulty.HARD));
        addMenuButton(col, "Achievements", Theme.GOLD, e -> showBadges());
        addMenuButton(col, "Leaderboard & History", Theme.CYAN, e -> showHistory());
        addMenuButton(col, "Exit", Theme.ERROR, e -> System.exit(0));

        outer.add(col);
        return outer;
    }

    private JPanel buildGameScreen() {
        JPanel p = new GradientPanel(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createEmptyBorder(0, 24, 14, 24));

        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        statusLabel.setForeground(Color.WHITE);
        infoLabel.setFont(Theme.SMALL_FONT);
        infoLabel.setForeground(Theme.LAVENDER);
        toastLabel.setFont(Theme.SMALL_FONT);
        toastLabel.setForeground(Theme.GOLD);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(new TitlePanel(38f, new Dimension(100, 74)));
        JPanel cardRow = new JPanel(new GridLayout(1, 3, 14, 0));
        cardRow.setOpaque(false);
        cardRow.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        cardRow.add(card1);
        cardRow.add(cardDraw);
        cardRow.add(card2);
        top.add(cardRow);
        p.add(top, BorderLayout.NORTH);

        turnBar.setBorderPainted(false);
        turnBar.setBackground(Theme.alpha(Color.WHITE, 25));
        turnBar.setForeground(Theme.CYAN);
        turnBar.setValue(0);
        turnBar.setPreferredSize(new Dimension(100, 6));
        JPanel centre = new JPanel(new BorderLayout(0, 4));
        centre.setOpaque(false);
        centre.add(turnBar, BorderLayout.NORTH);
        centre.add(boardPanel, BorderLayout.CENTER);
        p.add(centre, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new GridLayout(1, 5, 12, 0));
        buttonRow.setOpaque(false);
        undoButton = new RoundedButton("Undo", Theme.VIOLET);
        undoButton.addActionListener(e -> undoMove());
        hintButton = new RoundedButton("Hint", Theme.GOLD);
        hintButton.addActionListener(e -> giveHint());
        soundButton = new RoundedButton("Sound: ON", Theme.MINT);
        soundButton.addActionListener(e -> {
            SoundManager.setEnabled(!SoundManager.isEnabled());
            soundButton.setText(SoundManager.isEnabled() ? "Sound: ON" : "Sound: OFF");
        });
        timerButton = new RoundedButton("Timer: OFF", Theme.ORANGE);
        timerButton.addActionListener(e -> toggleTimer());
        RoundedButton menuButton = new RoundedButton("Main Menu", Theme.PINK);
        menuButton.addActionListener(e -> {
            cancelPending();
            showMenu();
        });
        buttonRow.add(undoButton);
        buttonRow.add(hintButton);
        buttonRow.add(soundButton);
        buttonRow.add(timerButton);
        buttonRow.add(menuButton);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        for (JLabel l : new JLabel[]{statusLabel, infoLabel, toastLabel}) {
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height + 4));
            bottom.add(l);
            bottom.add(Box.createVerticalStrut(3));
        }
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(buttonRow);
        JLabel keys = makeLabel("Keys:  1-9 place   \u2022   U undo   \u2022   H hint", Font.PLAIN, 12f, Theme.MUTED);
        keys.setAlignmentX(Component.CENTER_ALIGNMENT);
        bottom.add(Box.createVerticalStrut(6));
        bottom.add(keys);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildHistoryScreen() {
        JPanel p = new GradientPanel(new BorderLayout(0, 15));
        p.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.setOpaque(false);
        top.add(makeLabel("Leaderboard & Score History", Theme.HEADING_FONT, Theme.CYAN));
        historySummary.setFont(Theme.SMALL_FONT);
        historySummary.setForeground(Theme.LAVENDER);
        top.add(historySummary);
        p.add(top, BorderLayout.NORTH);

        JPanel centre = new JPanel(new GridLayout(2, 1, 0, 14));
        centre.setOpaque(false);
        centre.add(tableSection("Leaderboard (most wins first)", statsModel));
        centre.add(tableSection("Recent rounds (newest first)", historyModel));
        p.add(centre, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 14, 0));
        bottom.setOpaque(false);
        RoundedButton back = new RoundedButton("Back", Theme.VIOLET);
        back.addActionListener(e -> showMenu());
        RoundedButton clear = new RoundedButton("Clear History", Theme.PINK);
        clear.addActionListener(e -> clearHistory());
        bottom.add(back);
        bottom.add(clear);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel tableSection(String heading, DefaultTableModel model) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setOpaque(false);
        JLabel h = new JLabel(heading);
        h.setFont(Theme.BUTTON_FONT);
        h.setForeground(Theme.LAVENDER);
        section.add(h, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.TEXT);
        table.setFont(Theme.BODY_FONT);
        table.setRowHeight(32);
        table.setGridColor(Theme.alpha(Theme.VIOLET, 70));
        table.setSelectionBackground(Theme.CELL);
        table.setSelectionForeground(Theme.CYAN);
        table.setFillsViewportHeight(true);
        if (model == badgeModel) {                               // wide middle column for the descriptions
            table.getColumnModel().getColumn(0).setPreferredWidth(170);
            table.getColumnModel().getColumn(1).setPreferredWidth(520);
            table.getColumnModel().getColumn(2).setPreferredWidth(110);
        }
        table.getTableHeader().setBackground(Theme.CELL);
        table.getTableHeader().setForeground(Theme.CYAN);
        table.getTableHeader().setFont(Theme.BUTTON_FONT.deriveFont(Font.BOLD, 15f));
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.alpha(Theme.VIOLET, 140)));
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    // =====================================================================
    //  SESSION / ROUND FLOW
    // =====================================================================
    /** One session = a match of several rounds between the same players. difficulty == null means 2 humans. */
    private void startSession(ComputerPlayer.Difficulty difficulty) {
        String name1 = askName("Player 1 name (plays X):", "Player 1");
        if (name1 == null) return;
        Player p1 = new HumanPlayer(name1, 'X');
        Player p2;
        if (difficulty == null) {
            String name2 = askName("Player 2 name (plays O):", "Player 2");
            if (name2 == null) return;
            if (name2.equalsIgnoreCase(name1)) name2 = name2 + " 2";
            p2 = new HumanPlayer(name2, 'O');
            ai = null;
        } else {
            ai = new ComputerPlayer("Computer", 'O', 'X', difficulty);
            p2 = ai;                                   // polymorphism: a ComputerPlayer IS a Player
        }
        int target = askMatchType();
        if (target < 0) return;
        targetWins = target;
        pm = new PlayerManager(p1, p2);
        cards.show(root, "game");
        inGame = true;
        startRound();
    }

    private String askName(String prompt, String fallback) {
        String text = fallback;
        while (true) {
            Object input = JOptionPane.showInputDialog(this, prompt, "Enter Name",
                    JOptionPane.QUESTION_MESSAGE, null, null, text);
            if (input == null) return null;                       // cancelled
            try {
                return validator.validateName(input.toString(), fallback);
            } catch (InvalidInputException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid name",
                        JOptionPane.WARNING_MESSAGE);
                text = input.toString();
            }
        }
    }

    /** Returns 0 for endless play, 3 or 5 for "first to N wins", or -1 if cancelled. */
    private int askMatchType() {
        String[] options = {"First to 3 wins", "First to 5 wins", "Endless (no limit)"};
        Object choice = JOptionPane.showInputDialog(this, "Choose the match type:", "Match Type",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) return -1;
        if (choice.equals(options[0])) return 3;
        if (choice.equals(options[1])) return 5;
        return 0;
    }

    private void startRound() {
        cancelPending();
        board.reset();
        boardPanel.reset();
        moveStack.clear();
        pm.startNewRound();
        undosLeft = MAX_UNDOS;
        hintsLeft = MAX_HINTS;
        usedHelp = false;
        toastLabel.setText(" ");
        roundOver = false;
        boardPanel.setInteractive(true);
        updateScore();
        infoLabel.setForeground(Theme.LAVENDER);
        infoLabel.setText("Round " + (pm.getRoundsPlayed() + 1) + "   |   "
                + (targetWins > 0 ? "First to " + targetWins + " wins the match" : "Endless mode"));
        updateTurn();
        if (pm.getCurrent().isComputer()) scheduleComputerMove();
    }

    /** Called by BoardPanel when the user clicks a box. */
    @Override
    public void onCellClicked(int cell) {
        if (roundOver || pm == null || pm.getCurrent().isComputer()) return;
        try {
            validator.validateMove(cell, board);       // Module 6
        } catch (InvalidMoveException ex) {
            SoundManager.error();
            boardPanel.invalid();
            showError(ex.getMessage());
            return;
        }
        applyMove(cell);
    }

    private void applyMove(int cell) {
        clearHint();
        Player current = pm.getCurrent();
        board.place(cell, current.getSymbol());         // Module 1
        moveStack.push(cell);
        boardPanel.markPlaced(cell);
        if (current.getSymbol() == 'X') SoundManager.placeX(); else SoundManager.placeO();
        if (GameLogic.isGameOver(board)) {              // Module 2
            finishRound();
        } else {
            pm.switchTurn();                            // Module 3
            updateTurn();
            if (pm.getCurrent().isComputer()) scheduleComputerMove();
        }
    }

    private void scheduleComputerMove() {
        boardPanel.setInteractive(false);
        final int token = roundToken;
        computerThread = ai.chooseMoveAsync(board, move -> {   // Module 5 (runs in its own thread)
            if (token != roundToken || roundOver) return;      // round was cancelled meanwhile
            applyMove(move);
            if (!roundOver) boardPanel.setInteractive(true);
        });
    }

    /** Stops everything that may still be waiting: computer thread, countdown and hint timers. */
    private void cancelPending() {
        roundToken++;
        if (autoTimer != null) autoTimer.stop();
        if (turnTimer != null) turnTimer.stop();
        clearHint();
        if (computerThread != null && computerThread.isAlive()) {
            computerThread.interrupt();                 // ask the thread to stop ...
            try {
                computerThread.join(300);               // ... and wait for it
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        computerThread = null;
    }

    private void finishRound() {
        roundOver = true;
        roundToken++;
        turnTimer.stop();
        turnBar.setValue(0);
        boardPanel.setInteractive(false);
        clearHint();
        Player p1 = pm.getPlayer1();
        Player p2 = pm.getPlayer2();
        char w = GameLogic.getWinner(board);
        String result;
        boolean matchWon = false;
        if (w != Board.EMPTY) {
            Player winner = pm.getPlayerBySymbol(w);
            pm.recordWin(winner);
            result = winner.getName() + " won";
            boardPanel.showWin(GameLogic.getWinningLine(board));
            matchWon = targetWins > 0 && winner.getScore() >= targetWins;
            statusLabel.setForeground(Theme.WIN);
            if (matchWon) {
                statusLabel.setText(winner + " WINS THE MATCH!");
                boardPanel.startConfetti();
                SoundManager.matchWin();
            } else {
                statusLabel.setText(winner + " WINS THE ROUND!");
                SoundManager.win();
            }
        } else {
            pm.recordDraw();
            result = "Draw";
            statusLabel.setForeground(Theme.LAVENDER);
            statusLabel.setText("It's a DRAW!");
            boardPanel.showDraw();
            SoundManager.draw();
        }
        // FEATURE: achievements, badges and win streaks (only in games against the computer)
        if (ai != null) {
            Achievements.Outcome outcome = (w == Board.EMPTY) ? Achievements.Outcome.DRAW
                    : (pm.getPlayerBySymbol(w) == ai ? Achievements.Outcome.LOSS : Achievements.Outcome.WIN);
            Achievements.Update up = achievements.recordRound(outcome, ai.getDifficulty(), usedHelp, matchWon,
                    outcome == Achievements.Outcome.WIN ? countSymbol(w) : 0);
            showToast(up);
        }
        if (!history.saveResult(p1.getName(), p2.getName(), result)) {   // Module 7
            JOptionPane.showMessageDialog(this, "Could not save the result to scores.txt",
                    "Save error", JOptionPane.WARNING_MESSAGE);
        }
        updateScore();
        updateButtons();
        scheduleAutoStart(matchWon);
    }

    /** FEATURE: the next round (or a brand new match) starts by itself after a short countdown. */
    private void scheduleAutoStart(final boolean newMatch) {
        final int token = roundToken;
        final int[] left = {newMatch ? NEW_MATCH_SECONDS : NEXT_ROUND_SECONDS};
        final String text = newMatch ? "New match starts in " : "Next round starts in ";
        infoLabel.setForeground(Theme.LAVENDER);
        infoLabel.setText(text + left[0] + "...");
        autoTimer = new Timer(1000, null);
        autoTimer.addActionListener(e -> {
            if (token != roundToken) {                  // player went back to the menu, etc.
                autoTimer.stop();
                return;
            }
            left[0]--;
            if (left[0] <= 0) {
                autoTimer.stop();
                if (newMatch) pm.resetScores();
                startRound();
            } else {
                infoLabel.setText(text + left[0] + "...");
            }
        });
        autoTimer.start();
    }

    // =====================================================================
    //  FEATURES: UNDO and HINT
    // =====================================================================
    private boolean isHumanTurn() {
        return !roundOver && pm != null && !pm.getCurrent().isComputer();
    }

    /** Takes back your last move (against the computer it also removes the computer's reply). */
    private void undoMove() {
        if (!isHumanTurn() || undosLeft == 0) return;
        int needed = (ai != null) ? 2 : 1;
        if (moveStack.size() < needed) {
            flashInfo("Nothing to undo yet");
            return;
        }
        clearHint();
        for (int i = 0; i < needed; i++) {
            int cell = moveStack.pop();
            board.clear(cell);
            boardPanel.unmark(cell);
        }
        if (ai == null) pm.switchTurn();                // two humans: previous player plays again
        undosLeft--;
        usedHelp = true;
        SoundManager.undo();
        updateTurn();
    }

    /** Uses the Hard AI (Minimax) to point at the best box for the current player. */
    private void giveHint() {
        if (!isHumanTurn() || hintsLeft == 0) return;
        Player c = pm.getCurrent();
        char opponent = (c.getSymbol() == 'X') ? 'O' : 'X';
        ComputerPlayer helper = new ComputerPlayer("Hint", c.getSymbol(), opponent,
                ComputerPlayer.Difficulty.HARD);
        int cell = helper.chooseMove(board.copy());
        boardPanel.setHint(cell);
        hintsLeft--;
        usedHelp = true;
        SoundManager.hint();
        updateButtons();
        if (hintTimer != null) hintTimer.stop();
        hintTimer = new Timer(2500, e -> boardPanel.setHint(-1));
        hintTimer.setRepeats(false);
        hintTimer.start();
    }

    private void clearHint() {
        if (hintTimer != null) hintTimer.stop();
        boardPanel.setHint(-1);
    }

    // =====================================================================
    //  SCORE HISTORY / LEADERBOARD SCREEN
    // =====================================================================
    private void showHistory() {
        historyModel.setRowCount(0);
        statsModel.setRowCount(0);
        List<String[]> rows = history.loadHistory();
        for (int i = rows.size() - 1; i >= 0; i--) {            // newest first
            historyModel.addRow(rows.get(i));
        }
        for (String[] row : history.getStats()) {
            statsModel.addRow(row);
        }
        historySummary.setText(rows.size() + " round(s) saved in scores.txt");
        cards.show(root, "history");
    }

    private void clearHistory() {
        int answer = JOptionPane.showConfirmDialog(this, "Delete all saved scores?",
                "Clear History", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            history.clearHistory();
            showHistory();
        }
    }

    private void showMenu() {
        inGame = false;
        cards.show(root, "menu");
    }

    // =====================================================================
    //  FEATURE: ACHIEVEMENTS SCREEN
    // =====================================================================
    private JPanel buildBadgeScreen() {
        JPanel p = new GradientPanel(new BorderLayout(0, 15));
        p.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 4));
        top.setOpaque(false);
        top.add(makeLabel("Achievements", Theme.HEADING_FONT, Theme.GOLD));
        badgeSummary.setFont(Theme.SMALL_FONT);
        badgeSummary.setForeground(Theme.LAVENDER);
        top.add(badgeSummary);
        p.add(top, BorderLayout.NORTH);

        p.add(tableSection("Badges (earned in games against the computer)", badgeModel), BorderLayout.CENTER);

        RoundedButton back = new RoundedButton("Back", Theme.VIOLET);
        back.addActionListener(e -> showMenu());
        JPanel bottom = new JPanel(new GridLayout(1, 1));
        bottom.setOpaque(false);
        bottom.add(back);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private void showBadges() {
        badgeModel.setRowCount(0);
        for (Achievements.Badge b : Achievements.Badge.values()) {
            badgeModel.addRow(new Object[]{b.title, b.description,
                    achievements.isUnlocked(b) ? "UNLOCKED" : "Locked"});
        }
        badgeSummary.setText("Unlocked " + achievements.getUnlockedCount() + "/" + Achievements.Badge.values().length
                + "   |   Rounds won vs computer: " + achievements.getTotalWins()
                + "   |   Current streak: " + achievements.getStreak()
                + "   |   Best streak: " + achievements.getBestStreak());
        cards.show(root, "badges");
    }

    /** Shows the win streak and new badges under the status text (does not block the auto next round). */
    private void showToast(Achievements.Update up) {
        StringBuilder sb = new StringBuilder();
        if (up.streak >= 2) {
            sb.append("Win streak x").append(up.streak);
        }
        for (Achievements.Badge b : up.unlocked) {
            if (sb.length() > 0) sb.append("     |     ");
            sb.append("Badge unlocked: ").append(b.title);
        }
        toastLabel.setText(sb.length() == 0 ? " " : sb.toString());
    }

    private int countSymbol(char symbol) {
        int n = 0;
        for (int i = 1; i <= Board.CELLS; i++) {
            if (board.getCell(i) == symbol) n++;
        }
        return n;
    }

    // =====================================================================
    //  FEATURE: TURN TIMER (10 seconds per move, random move when time is up)
    // =====================================================================
    private void toggleTimer() {
        timerOn = !timerOn;
        timerButton.setText("Timer: " + (timerOn ? "ON" : "OFF"));
        restartTurnTimer();
    }

    private void restartTurnTimer() {
        turnTimer.stop();
        turnBar.setValue(0);
        if (!timerOn || !isHumanTurn()) return;
        turnDeadline = System.currentTimeMillis() + TURN_SECONDS * 1000L;
        turnBar.setValue(TURN_SECONDS * 1000);
        turnBar.setForeground(Theme.CYAN);
        turnTimer.start();
    }

    private void onTurnTick() {
        long left = turnDeadline - System.currentTimeMillis();
        if (left <= 0) {
            turnTimer.stop();
            turnBar.setValue(0);
            playRandomMove();
            return;
        }
        turnBar.setValue((int) left);
        turnBar.setForeground(left < 3000 ? Theme.ERROR : Theme.CYAN);
    }

    private void playRandomMove() {
        if (!isHumanTurn()) return;
        List<Integer> free = board.getEmptyCells();
        if (free.isEmpty()) return;
        SoundManager.error();
        applyMove(free.get(random.nextInt(free.size())));
        if (!roundOver) showError("Time's up! A random move was played.");
    }

    // =====================================================================
    //  FEATURE: KEYBOARD SHORTCUTS  (1-9 = play that box, U = undo, H = hint)
    // =====================================================================
    private void setupKeyboardShortcuts() {
        for (int i = 1; i <= 9; i++) {
            final String digit = String.valueOf(i);
            bindKey(digit.charAt(0), "cell" + digit, () -> playTyped(digit));
        }
        bindKey('u', "undoLower", () -> { if (inGame) undoMove(); });
        bindKey('U', "undoUpper", () -> { if (inGame) undoMove(); });
        bindKey('h', "hintLower", () -> { if (inGame) giveHint(); });
        bindKey('H', "hintUpper", () -> { if (inGame) giveHint(); });
    }

    private void bindKey(char key, String name, final Runnable action) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        root.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /** A box number typed on the keyboard goes through the same validator as a mouse click. */
    private void playTyped(String text) {
        if (!inGame || roundOver || pm == null || pm.getCurrent().isComputer()) return;
        try {
            applyMove(validator.validateMove(text, board));      // overloaded String version
        } catch (InvalidMoveException ex) {
            SoundManager.error();
            boardPanel.invalid();
            showError(ex.getMessage());
        }
    }

    // =====================================================================
    //  SMALL HELPERS
    // =====================================================================
    private void updateTurn() {
        refreshTurnText();
        restartTurnTimer();
    }

    private void refreshTurnText() {
        Player c = pm.getCurrent();
        statusLabel.setForeground(c.getSymbol() == 'X' ? Theme.X_COLOR : Theme.O_COLOR);
        statusLabel.setText(c.isComputer() ? c + " is thinking..." : c + " - your turn");
        boardPanel.setCurrentMark(c.getSymbol());
        updateScore();
        updateButtons();
    }

    private void updateButtons() {
        undoButton.setText("Undo (" + undosLeft + ")");
        hintButton.setText("Hint (" + hintsLeft + ")");
        undoButton.setEnabled(isHumanTurn() && undosLeft > 0);
        hintButton.setEnabled(isHumanTurn() && hintsLeft > 0);
    }

    private void updateScore() {
        boolean live = !roundOver;
        Player p1 = pm.getPlayer1(), p2 = pm.getPlayer2();
        card1.setData(p1.getName(), p1.getScore(), live && pm.getCurrent() == p1);
        cardDraw.setData("Draws", pm.getDraws(), false);
        card2.setData(p2.getName(), p2.getScore(), live && pm.getCurrent() == p2);
    }

    private void showError(String message) {
        statusLabel.setForeground(Theme.ERROR);
        statusLabel.setText(message);
        final int token = roundToken;
        Timer t = new Timer(1600, e -> {
            if (token == roundToken && !roundOver) refreshTurnText();
        });
        t.setRepeats(false);
        t.start();
    }

    private void flashInfo(String message) {
        final int token = roundToken;
        infoLabel.setForeground(Theme.ERROR);
        infoLabel.setText(message);
        Timer t = new Timer(1500, e -> {
            if (token == roundToken && !roundOver) {
                infoLabel.setForeground(Theme.LAVENDER);
                infoLabel.setText("Round " + (pm.getRoundsPlayed() + 1) + "   |   "
                        + (targetWins > 0 ? "First to " + targetWins + " wins the match" : "Endless mode"));
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private static JLabel makeLabel(String text, int style, float size, Color color) {
        return makeLabel(text, Theme.BODY_FONT.deriveFont(style, size), color);
    }

    private static JLabel makeLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private static RoundedButton addMenuButton(JPanel col, String text, Color color, ActionListener action) {
        RoundedButton b = new RoundedButton(text, color);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setPreferredSize(new Dimension(380, 46));
        b.setMaximumSize(new Dimension(380, 46));
        b.addActionListener(action);
        col.add(b);
        col.add(Box.createVerticalStrut(8));
        return b;
    }

    // =====================================================================
    //  NEON LOOK: gradient background, title and window icon
    // =====================================================================
    /** A panel painted with the dark navy -> violet gradient used on every screen. */
    private static class GradientPanel extends JPanel {
        GradientPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, Theme.BG1, 0, getHeight(), Theme.BG2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    /** "TIC TAC TOE" in glowing cyan-to-pink letters with the project line under it. */
    private static class TitlePanel extends JComponent {
        private final float fontSize;

        TitlePanel(float fontSize, Dimension size) {
            this.fontSize = fontSize;
            setPreferredSize(size);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, size.height));
            setMinimumSize(new Dimension(100, size.height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Font font = new Font("SansSerif", Font.BOLD, 38).deriveFont(fontSize)
                    .deriveFont(java.util.Map.of(TextAttribute.TRACKING, 0.14f));
            Shape shape = font.createGlyphVector(g2.getFontRenderContext(), "TIC TAC TOE").getOutline();
            Rectangle2D box = shape.getBounds2D();
            double baseline = fontSize * 1.16;
            g2.translate((getWidth() - box.getWidth()) / 2.0 - box.getX(), baseline);
            for (int i = 5; i >= 1; i--) {                     // glow
                g2.setStroke(new java.awt.BasicStroke(i * 3f * (fontSize / 38f), java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND));
                g2.setColor(Theme.alpha(Theme.PINK, 14));
                g2.draw(shape);
            }
            g2.setPaint(new GradientPaint((float) box.getX(), 0, Theme.CYAN,
                    (float) (box.getX() + box.getWidth()), 0, Theme.PINK));
            g2.fill(shape);
            g2.dispose();

            Graphics2D g3 = (Graphics2D) g.create();
            g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g3.setFont(new Font("SansSerif", Font.PLAIN, fontSize > 50 ? 16 : 12));
            g3.setColor(Theme.LAVENDER);
            String sub = "Java Mini Project  \u2022  Dharmit & Harsh";
            g3.drawString(sub, (getWidth() - g3.getFontMetrics().stringWidth(sub)) / 2,
                    (int) (baseline + (fontSize > 50 ? 26 : 22)));
            g3.dispose();
        }
    }

    /** Small neon X and O icon for the window / task bar. */
    private static BufferedImage createIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, Theme.BG1, 64, 64, Theme.BG2));
        g2.fillRoundRect(0, 0, 64, 64, 16, 16);
        g2.setStroke(new java.awt.BasicStroke(7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.CYAN);
        g2.drawLine(12, 12, 30, 30);
        g2.drawLine(30, 12, 12, 30);
        g2.setColor(Theme.PINK);
        g2.drawOval(34, 34, 18, 18);
        g2.dispose();
        return img;
    }
}
