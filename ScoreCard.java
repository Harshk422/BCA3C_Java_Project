import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * MODULE 4 (part): User Interface Module (Harsh)
 * A glowing score card (X / draws / O). The card of the player whose turn it is
 * pulses, and the number "bumps" in gold when the score goes up.
 */
public class ScoreCard extends JPanel {
    private final String symbol;
    private final Color accent;
    private final Timer anim;
    private String name = "";
    private int score;
    private boolean active;
    private long bumpStart = -1L;

    public ScoreCard(String symbol, Color accent) {
        this.symbol = symbol;
        this.accent = accent;
        setOpaque(false);
        setPreferredSize(new Dimension(210, 84));
        anim = new Timer(30, e -> repaint());
        anim.start();
    }

    public void setData(String name, int score, boolean active) {
        if (score > this.score) bumpStart = System.nanoTime();
        this.name = name;
        this.score = score;
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        double pulse = active ? 0.5 + 0.5 * Math.sin(System.nanoTime() / 1.6e8) : 0;

        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(3, 3, w - 6, h - 6, 28, 28);
        g2.setColor(Theme.alpha(accent, active ? (int) (42 + 34 * pulse) : 14));
        g2.fill(shape);
        if (active) {
            for (int i = 4; i >= 1; i--) {
                g2.setStroke(new BasicStroke(2f + i * 3f));
                g2.setColor(Theme.alpha(accent, 20));
                g2.draw(shape);
            }
        }
        g2.setStroke(new BasicStroke(active ? 2.6f : 1.2f));
        g2.setColor(Theme.alpha(accent, active ? 255 : 95));
        g2.draw(shape);

        // symbol
        g2.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Theme.alpha(accent, 70));
        g2.drawString(symbol, 22, h / 2 + fm.getAscent() / 2 - 2);
        g2.setColor(accent);
        g2.drawString(symbol, 24, h / 2 + fm.getAscent() / 2 - 4);

        // name
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        fm = g2.getFontMetrics();
        g2.setColor(Theme.TEXT);
        g2.drawString(ellipsize(name, fm, w - 92), 74, 30);

        // score (bumps in gold when it grows)
        double bump = bumpStart > 0 ? Math.max(0, 1 - (System.nanoTime() - bumpStart) / 5.5e8) : 0;
        g2.setFont(new Font("SansSerif", Font.BOLD, 34).deriveFont((float) (34 * (1 + 0.45 * bump))));
        g2.setColor(bump > 0 ? Theme.GOLD : Color.WHITE);
        g2.drawString(String.valueOf(score), 74, h - 16);

        if (active) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(Theme.alpha(accent, (int) (170 + 85 * pulse)));
            String s = "YOUR TURN";
            g2.drawString(s, w - 16 - g2.getFontMetrics().stringWidth(s), h - 18);
        }
        g2.dispose();
    }

    private static String ellipsize(String s, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "...") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }
}
