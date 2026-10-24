import java.awt.Color;
import java.awt.Font;

/**
 * MODULE 4 (part): User Interface Module (Harsh)
 * One place for all colours and fonts, so the whole GUI looks consistent.
 * NEON THEME: deep navy-to-violet background with glowing cyan / pink / gold accents.
 */
public final class Theme {
    private Theme() { }

    // background gradient (top -> bottom)
    public static final Color BG1        = new Color(0x0A, 0x0E, 0x27);
    public static final Color BG2        = new Color(0x1B, 0x12, 0x45);

    // neon accents
    public static final Color CYAN       = new Color(0x00, 0xE5, 0xFF);   // X
    public static final Color PINK       = new Color(0xFF, 0x2E, 0x93);   // O
    public static final Color GOLD       = new Color(0xFF, 0xD1, 0x66);   // wins / hints
    public static final Color VIOLET     = new Color(0x8B, 0x7C, 0xFF);   // grid lines, secondary buttons
    public static final Color MINT       = new Color(0x5C, 0xFF, 0x9D);
    public static final Color ORANGE     = new Color(0xFF, 0x8A, 0x5C);
    public static final Color ERROR      = new Color(0xFF, 0x6B, 0x81);

    // text
    public static final Color TEXT       = new Color(0xEB, 0xEE, 0xFF);
    public static final Color LAVENDER   = new Color(0xB9, 0xB4, 0xE8);
    public static final Color MUTED      = new Color(0x7F, 0x86, 0xBF);

    // panels / tables (translucent look on top of the gradient)
    public static final Color PANEL      = new Color(0x14, 0x15, 0x3A);
    public static final Color CELL       = new Color(0x22, 0x1F, 0x56);

    // short names kept for the game code
    public static final Color X_COLOR    = CYAN;
    public static final Color O_COLOR    = PINK;
    public static final Color WIN        = GOLD;

    public static final Font TITLE_FONT   = new Font("SansSerif", Font.BOLD, 60);
    public static final Font HEADING_FONT = new Font("SansSerif", Font.BOLD, 30);
    public static final Font BUTTON_FONT  = new Font("SansSerif", Font.BOLD, 18);
    public static final Font BODY_FONT    = new Font("SansSerif", Font.PLAIN, 17);
    public static final Font SMALL_FONT   = new Font("SansSerif", Font.PLAIN, 16);

    /** Same colour with a different transparency (0-255). */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
