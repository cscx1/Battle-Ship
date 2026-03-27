import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Enemy targeting grid (radar column beside the ocean).
 */
class SidePanel extends JPanel {
    private final GameController controller;

    SidePanel(GameController controller) {
        this.controller = controller;

        setLayout(new GridBagLayout());
        setBackground(new Color(25, 35, 65));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;

        JLabel radarTitle = new JLabel("Enemy board (click to fire)");
        radarTitle.setForeground(Color.LIGHT_GRAY);
        radarTitle.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        add(radarTitle, gbc);

        EnemyRadar radar = new EnemyRadar(controller);
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(radar, gbc);
    }

    void syncFromModel() {
        // Radar repaints from model on each frame via parent refresh; no controls here.
    }

    private static class EnemyRadar extends JComponent {
        private static final int CELLS = 10;
        private static final int CELL = 22;
        /** Pixel extent of the 10×10 grid (matches painted cells; excludes 1px outer margin). */
        private static final int GRID_PX = CELLS * CELL;
        private final GameController controller;

        EnemyRadar(GameController controller) {
            this.controller = controller;
            Dimension d = new Dimension(CELLS * CELL + 2, CELLS * CELL + 2);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    Point p = e.getPoint();
                    if (p.x < 0 || p.y < 0 || p.x >= GRID_PX || p.y >= GRID_PX) {
                        return;
                    }
                    int c = p.x / CELL;
                    int r = p.y / CELL;
                    if (r < 0 || r >= CELLS || c < 0 || c >= CELLS) {
                        return;
                    }
                    Model model = controller.getModel();
                    if (!model.getCanMoveShips() && model.isPlayersTurn()
                            && model.theirCell(r, c) == Model.CellStatus.DONTKNOW
                            && !model.isGameOver()) {
                        controller.shootEnemy(r, c);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Model model = controller.getModel();
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(40, 55, 90));
            g2.fillRect(0, 0, getWidth(), getHeight());

            for (int r = 0; r < CELLS; r++) {
                for (int c = 0; c < CELLS; c++) {
                    int x = c * CELL;
                    int y = r * CELL;
                    Model.CellStatus st = model.theirCell(r, c);
                    if (st == Model.CellStatus.HIT) {
                        g2.setColor(Color.RED);
                    } else if (st == Model.CellStatus.MISS) {
                        g2.setColor(Color.WHITE);
                    } else {
                        g2.setColor(new Color(70, 90, 130));
                    }
                    g2.fillRect(x + 1, y + 1, CELL - 2, CELL - 2);
                    g2.setColor(new Color(20, 30, 50));
                    g2.drawRect(x, y, CELL, CELL);
                }
            }
        }
    }
}
