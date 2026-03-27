import javax.swing.*;
import java.awt.*;

/**
 * Main window: score + fleet status, ocean, side controls, status bar.
 */
public class GameScreen extends JFrame {
    private final Model model;
    private final JLabel statusLabel;
    private final ScorePanel scorePanel;
    private final SidePanel sidePanel;
    private final FleetDockPanel fleetDockPanel;
    private final OceanPanel oceanPanel;
    private boolean gameOverDialogShown;

    public GameScreen(Model model) {
        super("Battleship");
        this.model = model;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        statusLabel = new JLabel("Drag ships from Your Fleet onto your ocean grid to begin.");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        statusLabel.setForeground(Color.LIGHT_GRAY);

        JLabel orientLabel = new JLabel("Orientation: HORIZONTAL  (Rotate button or right-click a ship)");
        orientLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        orientLabel.setForeground(new Color(100, 200, 100));

        GameController controller = new GameController(model);

        scorePanel = new ScorePanel(model);
        oceanPanel = new OceanPanel(controller);
        sidePanel = new SidePanel(controller);
        fleetDockPanel = new FleetDockPanel(controller, orientLabel);

        JPanel oceanWrapper = new JPanel(new BorderLayout(0, 4));
        oceanWrapper.setBackground(new Color(20, 30, 55));
        oceanWrapper.add(oceanPanel, BorderLayout.CENTER);
        oceanWrapper.add(orientLabel, BorderLayout.SOUTH);

        JPanel playRow = new JPanel(new BorderLayout());
        playRow.setBackground(new Color(20, 30, 55));
        playRow.add(oceanWrapper, BorderLayout.CENTER);
        playRow.add(sidePanel, BorderLayout.EAST);

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.setBackground(new Color(20, 30, 55));
        centerArea.add(playRow, BorderLayout.CENTER);
        centerArea.add(fleetDockPanel, BorderLayout.SOUTH);

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(10, 10, 25));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        statusBar.add(statusLabel, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(scorePanel, BorderLayout.NORTH);
        root.add(centerArea, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);

        add(root);
        pack();
        setMinimumSize(new Dimension(880, 760));
        setLocationRelativeTo(null);

        model.setOnUpdate(this::refreshFromModel);
        refreshFromModel();
        setVisible(true);
    }

    private void refreshFromModel() {
        SoundEffects.play(model.takeSoundCue());
        scorePanel.syncFromModel();
        sidePanel.syncFromModel();
        fleetDockPanel.syncFromModel();
        oceanPanel.rebuildPieces();
        sidePanel.repaint();
        statusLabel.setText(model.getLog());
        if (model.isGameOver() && !gameOverDialogShown) {
            gameOverDialogShown = true;
            JOptionPane.showMessageDialog(this,
                    model.isYouWon() ? "You sank their entire fleet — you win!" : "Your fleet is destroyed — you lose.",
                    "Game over",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
