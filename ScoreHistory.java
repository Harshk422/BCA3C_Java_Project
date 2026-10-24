import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MODULE 7: Save / Score History Module (Shared)
 * ------------------------------------------------------------
 * Saves every finished round to scores.txt, so results are
 * remembered even after the program is closed and reopened.
 * (File handling + exception handling + synchronized methods.)
 */
public class ScoreHistory {
    private static final Path FILE = Paths.get("scores.txt");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Appends one line per finished round. Returns false if the file could not be written. */
    public synchronized boolean saveResult(String player1, String player2, String result) {
        String line = LocalDateTime.now().format(FMT) + "|" + player1 + "|" + player2 + "|" + result
                + System.lineSeparator();
        try {
            Files.write(FILE, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Each row = {date & time, player 1, player 2, result}. Oldest first. */
    public synchronized List<String[]> loadHistory() {
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(FILE)) {
            return rows;
        }
        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\|", -1);
                if (parts.length == 4) {
                    rows.add(parts);
                }
            }
        } catch (IOException e) {
            // unreadable file -> treat as empty history
        }
        return rows;
    }

    public synchronized boolean clearHistory() {
        try {
            Files.deleteIfExists(FILE);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * FEATURE: leaderboard. Reads the saved rounds and counts, for every player name,
     * rounds played, wins, draws and losses. Best player (most wins) comes first.
     * Each row = {name, rounds, wins, draws, losses, win %}.
     */
    public synchronized List<String[]> getStats() {
        Map<String, int[]> map = new LinkedHashMap<>();       // name -> {rounds, wins, draws, losses}
        for (String[] r : loadHistory()) {
            String p1 = r[1].trim(), p2 = r[2].trim(), result = r[3].trim();
            int[] a = map.computeIfAbsent(p1, k -> new int[4]);
            int[] b = map.computeIfAbsent(p2, k -> new int[4]);
            a[0]++;
            b[0]++;
            if (result.equals("Draw")) {
                a[2]++;
                b[2]++;
            } else if (result.endsWith(" won")) {
                String winner = result.substring(0, result.length() - 4);
                if (winner.equals(p1))      { a[1]++; b[3]++; }
                else if (winner.equals(p2)) { b[1]++; a[3]++; }
            }
        }
        List<Map.Entry<String, int[]>> entries = new ArrayList<>(map.entrySet());
        entries.sort((x, y) -> y.getValue()[1] - x.getValue()[1]);
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, int[]> e : entries) {
            int[] v = e.getValue();
            rows.add(new String[]{e.getKey(), String.valueOf(v[0]), String.valueOf(v[1]),
                    String.valueOf(v[2]), String.valueOf(v[3]), (v[1] * 100 / v[0]) + "%"});
        }
        return rows;
    }
}
