import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main class: START THE GAME FROM HERE.
 * It only opens the window; GameWindow connects all 7 modules together.
 */
public class Main {

    public static void main(String[] args) {
        // make the pop-up dialogs match the dark neon theme
        UIManager.put("OptionPane.background", Theme.PANEL);
        UIManager.put("Panel.background", Theme.PANEL);
        UIManager.put("OptionPane.messageForeground", Theme.TEXT);

        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
