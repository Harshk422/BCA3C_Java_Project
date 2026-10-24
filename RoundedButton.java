import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/**
 * MODULE 4 (part): User Interface Module (Harsh)
 * A neon "pill" button: translucent gradient fill, glowing outline on hover
 * (inheritance: extends JButton, method overriding: paintComponent).
 */
public class RoundedButton extends JButton {
    private final Color accent;
    private boolean hover = false;

    public RoundedButton(String text, Color accent) {
        super(text);
        this.accent = accent;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        setForeground(Color.WHITE);
        setFont(Theme.BUTTON_FONT);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int strength;
        if (!isEnabled())                strength = 8;
        else if (getModel().isPressed()) strength = 100;
        else if (hover)                  strength = 75;
        else                             strength = 28;

        RoundRectangle2D.Double shape = new RoundRectangle2D.Double(1.5, 1.5, w - 3, h - 3, h, h);
        g2.setPaint(new GradientPaint(0, 0, Theme.alpha(accent, strength + 35),
                                      0, h, Theme.alpha(accent, strength)));
        g2.fill(shape);

        if (isEnabled() && hover) {                       // soft neon glow
            for (int i = 3; i >= 1; i--) {
                g2.setStroke(new BasicStroke(1.6f + i * 2.4f));
                g2.setColor(Theme.alpha(accent, 20));
                g2.draw(shape);
            }
        }
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(Theme.alpha(accent, isEnabled() ? 215 : 60));
        g2.draw(shape);
        g2.dispose();

        setForeground(isEnabled() ? Color.WHITE : new Color(255, 255, 255, 90));
        super.paintComponent(g);
    }
}
