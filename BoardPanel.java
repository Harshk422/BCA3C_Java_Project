import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * MODULE 4 (part): User Interface Module (Harsh)
 * ------------------------------------------------------------
 * The clickable game board, drawn with Java 2D in a NEON style:
 * floating glow dots, glowing grid lines, glowing animated X and O,
 * a hover preview of the next symbol and a glowing gold line through
 * the winning cells. Mouse clicks are reported to the window through
 * the CellClickListener interface.
 *
 * Extra features drawn here: pulsing HINT outline and the
 * CONFETTI celebration when someone wins the whole match.
 */
public class BoardPanel extends JPanel {

    /** Callback interface: the window implements this to receive clicks. */
    public interface CellClickListener {
        void onCellClicked(int cell);
    }

    private static final Color[] CONFETTI_COLORS =
            {Theme.GOLD, Theme.CYAN, Theme.PINK, Color.WHITE, Theme.VIOLET};

    private final Board board;
    private final float[] progress = new float[Board.CELLS + 1];   // 0..1 animation per cell
    private final Timer animator;
    private final Random random = new Random();
    private final List<float[]> confetti = new ArrayList<>();      // {x, y, vx, vy, size, colour, spin}
    private final List<double[]> dots = new ArrayList<>();         // {x, y, vx, vy, size, phase, colourIndex}
    private final Color[] dotColors = {Theme.CYAN, Theme.PINK, Color.WHITE, Theme.VIOLET};

    private CellClickListener listener;
    private int hoverCell = -1;
    private int[] winLine;
    private float winProgress;
    private boolean interactive = true;
    private char currentMark = 'X';
    private int hintCell = -1;
    private float hintPhase;
    private double shake;
    private double time;
    private long lastTick = System.nanoTime();

    private double originX, originY, side, cellSize;

    public BoardPanel(Board board) {
        this.board = board;
        setOpaque(false);
        setPreferredSize(new Dimension(460, 460));
        setMinimumSize(new Dimension(300, 300));

        for (int i = 0; i < 46; i++) {
            dots.add(new double[]{random.nextDouble(), random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02,
                    2 + random.nextDouble() * 4, random.nextDouble() * 6.28,
                    random.nextInt(dotColors.length)});
        }

        animator = new Timer(16, e -> tick());
        animator.start();

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                hoverCell = cellAt(e.getX(), e.getY());
                setCursor(java.awt.Cursor.getPredefinedCursor(
                        interactive && hoverCell != -1 ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
            }
            @Override public void mouseExited(MouseEvent e) {
                hoverCell = -1;
                setCursor(java.awt.Cursor.getDefaultCursor());
            }
            @Override public void mousePressed(MouseEvent e) {
                int c = cellAt(e.getX(), e.getY());
                if (c != -1 && listener != null) {
                    listener.onCellClicked(c);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // ---------------- public API used by GameWindow ----------------
    public void setCellClickListener(CellClickListener l) { this.listener = l; }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (!interactive) setCursor(java.awt.Cursor.getDefaultCursor());
    }

    /** The symbol that is shown as a faint preview when the mouse is over an empty box. */
    public void setCurrentMark(char mark) { this.currentMark = mark; }

    /** Call after a symbol was placed on the board so it animates in. */
    public void markPlaced(int cell) { progress[cell] = 0f; }

    public void showWin(int[] line) {
        winLine = line;
        winProgress = 0f;
    }

    /** Removes the animation of one cell (used by Undo). */
    public void unmark(int cell) { progress[cell] = 0f; }

    /** Shows a pulsing outline on the suggested cell; pass -1 to remove it. */
    public void setHint(int cell) {
        hintCell = cell;
        hintPhase = 0f;
    }

    /** Short sideways shake of the board: a draw. */
    public void showDraw() { shake = 0.7; }

    /** Shorter shake: an invalid click. */
    public void invalid() { shake = 0.4; }

    /** Rains confetti over the board (match victory). */
    public void startConfetti() {
        confetti.clear();
        int w = Math.max(getWidth(), 400);
        for (int i = 0; i < 170; i++) {
            confetti.add(new float[]{
                random.nextFloat() * w, -random.nextFloat() * 220f,
                (random.nextFloat() - 0.5f) * 3f, 2f + random.nextFloat() * 4f,
                7f + random.nextFloat() * 8f, random.nextInt(CONFETTI_COLORS.length),
                random.nextFloat() * 6.28f});
        }
    }

    public void reset() {
        java.util.Arrays.fill(progress, 0f);
        winLine = null;
        winProgress = 0f;
        hintCell = -1;
        shake = 0;
        confetti.clear();
        repaint();
    }

    // ---------------- geometry ----------------
    private void computeGeometry() {
        side = Math.min(getWidth(), getHeight()) - 36;
        cellSize = side / 3.0;
        originX = (getWidth() - side) / 2.0;
        originY = (getHeight() - side) / 2.0;
    }

    private int cellAt(int x, int y) {
        computeGeometry();
        double dx = x - originX, dy = y - originY;
        if (dx < 0 || dy < 0 || dx >= side || dy >= side) return -1;
        int col = Math.min(2, (int) (dx / cellSize));
        int row = Math.min(2, (int) (dy / cellSize));
        return row * 3 + col + 1;
    }

    private RoundRectangle2D cellShape(int cell) {
        int r = (cell - 1) / 3, c = (cell - 1) % 3;
        return new RoundRectangle2D.Double(originX + c * cellSize + 8, originY + r * cellSize + 8,
                cellSize - 16, cellSize - 16, 22, 22);
    }

    private double[] center(int cell) {
        int r = (cell - 1) / 3, c = (cell - 1) % 3;
        return new double[]{originX + (c + 0.5) * cellSize, originY + (r + 0.5) * cellSize};
    }

    // ---------------- animation ----------------
    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - lastTick) / 1e9);
        lastTick = now;
        time += dt;

        for (int i = 1; i <= Board.CELLS; i++) {
            progress[i] = board.getCell(i) != Board.EMPTY ? Math.min(1f, progress[i] + (float) (dt * 3.2)) : 0f;
        }
        if (winLine != null && winProgress < 1f) winProgress = Math.min(1f, winProgress + (float) (dt * 1.8));
        if (hintCell != -1) hintPhase += (float) (dt * 7);
        if (shake > 0) shake = Math.max(0, shake - dt);

        for (double[] d : dots) {                           // drifting background dots
            d[0] = (d[0] + d[2] * dt + 1) % 1.0;
            d[1] = (d[1] + d[3] * dt + 1) % 1.0;
        }
        for (Iterator<float[]> it = confetti.iterator(); it.hasNext(); ) {
            float[] c = it.next();
            c[0] += c[2] + (float) Math.sin(c[6]) * 0.9f;
            c[1] += c[3];
            c[6] += 0.12f;
            if (c[1] > getHeight() + 20) it.remove();
        }
        repaint();
    }

    private static double ease(double t) {
        double u = 1 - Math.max(0, Math.min(1, t));
        return 1 - u * u * u;
    }

    private static Color markColor(char c) { return c == 'X' ? Theme.CYAN : Theme.PINK; }

    // ---------------- painting ----------------
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        computeGeometry();
        int w = getWidth(), h = getHeight();

        for (double[] d : dots) {                           // twinkling dots
            double pulse = 0.5 + 0.5 * Math.sin(time * 1.5 + d[5]);
            g2.setColor(Theme.alpha(dotColors[(int) d[6]], (int) (30 + 60 * pulse)));
            g2.fill(new Ellipse2D.Double(d[0] * w, d[1] * h, d[4], d[4]));
        }

        Graphics2D base = g2;
        g2 = (Graphics2D) base.create();
        if (shake > 0) g2.translate(Math.sin(time * 70) * 9 * Math.min(1, shake / 0.35), 0);

        // glass card behind the board
        RoundRectangle2D back = new RoundRectangle2D.Double(originX - 10, originY - 10, side + 20, side + 20, 36, 36);
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fill(back);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255, 255, 255, 45));
        g2.draw(back);

        // hover preview of the next symbol
        if (interactive && hoverCell != -1 && board.isCellEmpty(hoverCell)) {
            Color mc = markColor(currentMark);
            Shape cs = cellShape(hoverCell);
            g2.setColor(Theme.alpha(mc, 34));
            g2.fill(cs);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Theme.alpha(mc, 120));
            g2.draw(cs);
            double[] c = center(hoverCell);
            drawMark(g2, currentMark, c[0], c[1], 1.0, true);
        }

        // pulsing hint
        if (hintCell != -1 && board.isCellEmpty(hintCell)) {
            Shape cs = cellShape(hintCell);
            g2.setColor(Theme.alpha(Theme.GOLD, (int) (70 + 60 * Math.sin(hintPhase))));
            g2.fill(cs);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Theme.GOLD);
            g2.draw(cs);
        }

        // small box numbers (for the keyboard shortcuts 1-9)
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (cellSize * 0.12))));
        int asc = g2.getFontMetrics().getAscent();
        for (int i = 1; i <= Board.CELLS; i++) {
            if (!board.isCellEmpty(i)) continue;
            int r = (i - 1) / 3, c = (i - 1) % 3;
            g2.setColor(new Color(255, 255, 255, 55));
            g2.drawString(String.valueOf(i), (float) (originX + c * cellSize + 20),
                    (float) (originY + r * cellSize + 18 + asc * 0.6));
        }

        // glowing grid lines
        double pad = 10;
        for (int n = 1; n <= 2; n++) {
            glow(g2, new Line2D.Double(originX + n * cellSize, originY + pad, originX + n * cellSize, originY + side - pad), Theme.VIOLET, 5f);
            glow(g2, new Line2D.Double(originX + pad, originY + n * cellSize, originX + side - pad, originY + n * cellSize), Theme.VIOLET, 5f);
        }

        // placed symbols
        for (int i = 1; i <= Board.CELLS; i++) {
            char s = board.getCell(i);
            if (s == Board.EMPTY) continue;
            double[] c = center(i);
            drawMark(g2, s, c[0], c[1], progress[i], false);
        }

        if (winLine != null) drawWin(g2);
        g2.dispose();
        g2 = base;
        drawConfetti(g2);
        g2.dispose();
    }

    private void drawMark(Graphics2D g2, char symbol, double cx, double cy, double p, boolean ghost) {
        if (p <= 0.001) return;
        double r = cellSize * 0.24;
        double e = ease(p);
        float width = (float) Math.max(6, cellSize * 0.07);
        Color color = markColor(symbol);
        List<Shape> parts = new ArrayList<>();
        if (symbol == 'X') {
            double a = Math.min(1, e * 2), b = Math.max(0, e * 2 - 1);
            parts.add(new Line2D.Double(cx - r, cy - r, cx - r + 2 * r * a, cy - r + 2 * r * a));
            if (b > 0) parts.add(new Line2D.Double(cx + r, cy - r, cx + r - 2 * r * b, cy - r + 2 * r * b));
        } else {
            parts.add(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, 90, -360 * e, Arc2D.OPEN));
        }
        for (Shape s : parts) {
            if (ghost) {
                g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(Theme.alpha(color, 75));
                g2.draw(s);
            } else {
                glow(g2, s, color, width);
            }
        }
    }

    private void drawWin(Graphics2D g2) {
        double pulse = 0.5 + 0.5 * Math.sin(time * 6);
        for (int cell : winLine) {
            g2.setColor(Theme.alpha(Theme.GOLD, (int) (28 + 40 * pulse)));
            g2.fill(cellShape(cell));
        }
        double[] a = center(winLine[0]);
        double[] b = center(winLine[2]);
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double len = Math.hypot(dx, dy);
        double ux = dx / len, uy = dy / len;
        double ext = cellSize * 0.3;
        double x1 = a[0] - ux * ext, y1 = a[1] - uy * ext;
        double x2 = b[0] + ux * ext, y2 = b[1] + uy * ext;
        double p = ease(winProgress);
        glow(g2, new Line2D.Double(x1, y1, x1 + (x2 - x1) * p, y1 + (y2 - y1) * p), Theme.GOLD, 10f);
    }

    private void drawConfetti(Graphics2D g2) {
        for (float[] c : confetti) {
            Graphics2D gp = (Graphics2D) g2.create();
            gp.rotate(c[6], c[0] + c[4] / 2, c[1] + c[4] / 3);
            gp.setColor(CONFETTI_COLORS[(int) c[5]]);
            gp.fill(new RoundRectangle2D.Float(c[0], c[1], c[4], c[4] * 0.5f, 3, 3));
            gp.dispose();
        }
    }

    /** Neon effect: wide faint strokes, then the solid colour, then a thin white core. */
    private static void glow(Graphics2D g2, Shape shape, Color color, float width) {
        for (int i = 5; i >= 1; i--) {
            g2.setStroke(new BasicStroke(width + i * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Theme.alpha(color, 16));
            g2.draw(shape);
        }
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(color);
        g2.draw(shape);
        g2.setStroke(new BasicStroke(Math.max(1.5f, width * 0.35f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.alpha(Color.WHITE, 170));
        g2.draw(shape);
    }
}
