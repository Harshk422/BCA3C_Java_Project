import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * FEATURE: ACHIEVEMENTS, BADGES AND WIN STREAKS
 * ------------------------------------------------------------
 * Every round played against the computer is checked for badges, and
 * winning several rounds in a row builds a win streak.
 * Progress is saved in achievements.txt.
 */
public class Achievements {

    public enum Outcome { WIN, DRAW, LOSS }

    /** All badges. Each one has a title and a description. */
    public enum Badge {
        FIRST_WIN("First Blood", "Win your first round against the computer"),
        HAT_TRICK("Hat-Trick", "Win 3 rounds in a row against the computer"),
        UNSTOPPABLE("Unstoppable", "Win 5 rounds in a row against the computer"),
        LIGHTNING("Lightning", "Win a round using only 3 of your own moves"),
        FLAWLESS("Flawless", "Win a round without using Undo or Hint"),
        CRUSHER("Computer Crusher", "Beat the Medium computer"),
        SURVIVOR("Survivor", "Draw against the Hard computer"),
        MATCH_WINNER("Match Winner", "Win a whole match (first to 3 or 5)"),
        VETERAN("Veteran", "Win 10 rounds against the computer");

        public final String title;
        public final String description;

        Badge(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    /** What happened after one round: new badges and the current streak. */
    public static class Update {
        public final List<Badge> unlocked;
        public final int streak;

        Update(List<Badge> unlocked, int streak) {
            this.unlocked = unlocked;
            this.streak = streak;
        }
    }

    private static final Path FILE = Paths.get("achievements.txt");

    private final Set<Badge> unlocked = EnumSet.noneOf(Badge.class);
    private int totalWins;
    private int streak;
    private int bestStreak;

    public Achievements() {
        load();
    }

    // ---------------- queries ----------------
    public synchronized boolean isUnlocked(Badge b) { return unlocked.contains(b); }
    public synchronized int getUnlockedCount()      { return unlocked.size(); }
    public synchronized int getTotalWins()          { return totalWins; }
    public synchronized int getStreak()             { return streak; }
    public synchronized int getBestStreak()         { return bestStreak; }

    // ---------------- main logic ----------------
    /**
     * Call once after every round against the computer.
     * @param level        difficulty of the computer
     * @param usedHelp     true if Undo or Hint was used in this round
     * @param matchWon     true if this round won the whole match
     * @param winnerMoves  how many symbols the winner had on the board (only used on a WIN)
     */
    public synchronized Update recordRound(Outcome outcome, ComputerPlayer.Difficulty level,
                                           boolean usedHelp, boolean matchWon, int winnerMoves) {
        List<Badge> got = new ArrayList<>();

        if (outcome == Outcome.WIN) {
            totalWins++;
            streak++;
            bestStreak = Math.max(bestStreak, streak);
            tryUnlock(Badge.FIRST_WIN, got);
            if (streak >= 3) tryUnlock(Badge.HAT_TRICK, got);
            if (streak >= 5) tryUnlock(Badge.UNSTOPPABLE, got);
            if (!usedHelp) tryUnlock(Badge.FLAWLESS, got);
            if (winnerMoves > 0 && winnerMoves <= 3) tryUnlock(Badge.LIGHTNING, got);
            if (level == ComputerPlayer.Difficulty.MEDIUM) tryUnlock(Badge.CRUSHER, got);
            if (matchWon) tryUnlock(Badge.MATCH_WINNER, got);
            if (totalWins >= 10) tryUnlock(Badge.VETERAN, got);
        } else if (outcome == Outcome.LOSS) {
            streak = 0;                                          // a loss breaks the streak
        } else if (level == ComputerPlayer.Difficulty.HARD) {
            tryUnlock(Badge.SURVIVOR, got);                      // a draw keeps the streak alive
        }

        save();
        return new Update(got, streak);
    }

    private void tryUnlock(Badge b, List<Badge> list) {
        if (unlocked.add(b)) list.add(b);
    }

    // ---------------- file handling ----------------
    private void load() {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            p.load(in);
            totalWins = Integer.parseInt(p.getProperty("totalWins", "0"));
            streak = Integer.parseInt(p.getProperty("streak", "0"));
            bestStreak = Integer.parseInt(p.getProperty("bestStreak", "0"));
            for (String name : p.getProperty("unlocked", "").split(",")) {
                try {
                    unlocked.add(Badge.valueOf(name.trim()));
                } catch (IllegalArgumentException ignored) {
                    // unknown or empty name -> skip
                }
            }
        } catch (IOException | RuntimeException e) {
            unlocked.clear();
            totalWins = streak = bestStreak = 0;
        }
    }

    private void save() {
        Properties p = new Properties();
        StringBuilder names = new StringBuilder();
        for (Badge b : unlocked) {
            if (names.length() > 0) names.append(',');
            names.append(b.name());
        }
        p.setProperty("unlocked", names.toString());
        p.setProperty("totalWins", String.valueOf(totalWins));
        p.setProperty("streak", String.valueOf(streak));
        p.setProperty("bestStreak", String.valueOf(bestStreak));
        try (Writer out = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            p.store(out, "Achievements data");
        } catch (IOException e) {
            // cannot save -> progress is kept until the game is closed
        }
    }
}
