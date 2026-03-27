import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Enemy radar, fleet dock, auto-place, start battle, exit.
 */
class SidePanel extends JPanel {
    private final GameController controller;
    private final Model model;
    private final FleetTile[] fleetTiles;
    private final JButton startButton;
    private final JButton autoButton;

    SidePanel(GameController controller, JLabel orientLabel) {
        this.controller = controller;
        this.model = controller.getModel();

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
        gbc.insets = new Insets(0, 0, 10, 0);
        add(radar, gbc);

        JPanel fleetPanel = new JPanel();
        fleetPanel.setOpaque(false);
        fleetPanel.setLayout(new BoxLayout(fleetPanel, BoxLayout.Y_AXIS));

        JLabel fleetTitle = new JLabel("Your fleet (drag onto ocean)");
        fleetTitle.setForeground(Color.LIGHT_GRAY);
        fleetTitle.setFont(new Font("Arial", Font.BOLD, 12));
        fleetTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        fleetPanel.add(fleetTitle);
        fleetPanel.add(Box.createVerticalStrut(6));

        Model.ShipType[] order = {
                Model.ShipType.CARRIER, Model.ShipType.BATTLESHIP, Model.ShipType.CRUISER,
                Model.ShipType.SUBMARINE, Model.ShipType.DESTROYER
        };
        fleetTiles = new FleetTile[order.length];
        for (int i = 0; i < order.length; i++) {
            FleetTile tile = new FleetTile(model, order[i], orientLabel);
            tile.setAlignmentX(Component.LEFT_ALIGNMENT);
            tile.setMaximumSize(new Dimension(220, 80));
            fleetTiles[i] = tile;
            fleetPanel.add(tile);
            fleetPanel.add(Box.createVerticalStrut(6));
        }

        fleetPanel.add(Box.createVerticalGlue());

        autoButton = new JButton("Auto-place fleet");
        autoButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoButton.setMaximumSize(new Dimension(220, 32));
        autoButton.addActionListener(e -> controller.autoPlaceFleet());
        fleetPanel.add(autoButton);
        fleetPanel.add(Box.createVerticalStrut(6));

        startButton = new JButton("Start battle");
        startButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(220, 36));
        startButton.addActionListener(e -> controller.beginBattle());
        fleetPanel.add(startButton);
        fleetPanel.add(Box.createVerticalStrut(8));

        JButton exit = new JButton("Exit");
        exit.setAlignmentX(Component.LEFT_ALIGNMENT);
        exit.setMaximumSize(new Dimension(220, 32));
        exit.addActionListener(e -> System.exit(0));
        fleetPanel.add(exit);

        JScrollPane fleetScroll = new JScrollPane(fleetPanel);
        fleetScroll.setBorder(null);
        fleetScroll.getViewport().setOpaque(false);
        fleetScroll.setOpaque(false);
        fleetScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        fleetScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // Cap preferred height so pack() does not use the full fleet panel height; GridBag weighty expands this area.
        fleetScroll.setPreferredSize(new Dimension(240, 320));
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(fleetScroll, gbc);
    }

    void syncFromModel() {
        boolean setup = model.getCanMoveShips() && !model.isGameOver();
        for (FleetTile t : fleetTiles) {
            t.refreshEnabled();
        }
        autoButton.setEnabled(setup);
        startButton.setEnabled(setup && model.isFleetReady());
    }

    private static int length(Model.ShipType t) {
        switch (t) {
            case CARRIER:
                return 5;
            case BATTLESHIP:
                return 4;
            case CRUISER:
            case SUBMARINE:
                return 3;
            case DESTROYER:
                return 2;
            default:
                return 0;
        }
    }

    private static class FleetTile extends JPanel {
        private final Model model;
        private final Model.ShipType type;
        private final JLabel orientHint;
        private boolean horizontal = true;
        private boolean dragStarted;

        FleetTile(Model model, Model.ShipType type, JLabel orientLabel) {
            this.model = model;
            this.type = type;

            setLayout(new BorderLayout(4, 2));
            setOpaque(true);
            setBackground(new Color(35, 50, 85));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(80, 100, 140)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));

            String name = type.name().charAt(0) + type.name().substring(1).toLowerCase();
            JLabel title = new JLabel(name + " (" + length(type) + ")");
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Arial", Font.BOLD, 12));
            add(title, BorderLayout.NORTH);

            orientHint = new JLabel("H");
            orientHint.setForeground(new Color(150, 220, 150));
            orientHint.setFont(new Font("Arial", Font.PLAIN, 11));
            add(orientHint, BorderLayout.SOUTH);

            JButton rotateBtn = new JButton("Rotate");
            rotateBtn.setFont(new Font("Arial", Font.PLAIN, 10));
            rotateBtn.setMargin(new Insets(1, 6, 1, 6));
            rotateBtn.addActionListener(ev -> {
                if (!model.shipTypeAvailable(type)) {
                    return;
                }
                horizontal = !horizontal;
                orientHint.setText(horizontal ? "H" : "V");
                orientLabel.setText("Orientation: " + (horizontal ? "HORIZONTAL" : "VERTICAL")
                        + "  (Rotate button or right-click)");
            });
            add(rotateBtn, BorderLayout.EAST);

            setTransferHandler(new TransferHandler() {
                @Override
                public int getSourceActions(JComponent c) {
                    return COPY;
                }

                @Override
                protected java.awt.datatransfer.Transferable createTransferable(JComponent c) {
                    if (!model.shipTypeAvailable(type)) {
                        return null;
                    }
                    return new StringSelection(type.name() + "," + horizontal);
                }
            });

            MouseAdapter pressAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragStarted = false;
                    if (!model.shipTypeAvailable(type)) {
                        return;
                    }
                    if (SwingUtilities.isRightMouseButton(e)
                            || SwingUtilities.isMiddleMouseButton(e)) {
                        horizontal = !horizontal;
                        orientHint.setText(horizontal ? "H" : "V");
                        orientLabel.setText("Orientation: " + (horizontal ? "HORIZONTAL" : "VERTICAL")
                                + "  (Rotate button or right-click)");
                    }
                }
            };
            MouseMotionAdapter dragAdapter = new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!model.getCanMoveShips() || !model.shipTypeAvailable(type)) {
                        return;
                    }
                    if (!dragStarted) {
                        dragStarted = true;
                        TransferHandler th = getTransferHandler();
                        if (th != null) {
                            th.exportAsDrag(FleetTile.this, e, TransferHandler.COPY);
                        }
                    }
                }
            };

            // Clicks land on child labels unless we listen there too (otherwise rotate/drag never fire).
            addMouseListener(pressAdapter);
            addMouseMotionListener(dragAdapter);
            title.addMouseListener(pressAdapter);
            title.addMouseMotionListener(dragAdapter);
            orientHint.addMouseListener(pressAdapter);
            orientHint.addMouseMotionListener(dragAdapter);
            // Don't attach dragAdapter to rotateBtn — dragging from Rotate would start a ship drag.
        }

        void refreshEnabled() {
            boolean avail = model.shipTypeAvailable(type) && model.getCanMoveShips() && !model.isGameOver();
            setEnabled(avail);
            setBackground(avail ? new Color(35, 50, 85) : new Color(25, 28, 40));
        }
    }

    private static class EnemyRadar extends JComponent {
        private static final int CELLS = 10;
        private static final int CELL = 22;
        private final GameController controller;

        EnemyRadar(GameController controller) {
            this.controller = controller;
            Dimension d = new Dimension(CELLS * CELL + 2, CELLS * CELL + 2);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Model model = controller.getModel();
                    int c = e.getX() / CELL;
                    int r = e.getY() / CELL;
                    if (r < 0 || r >= CELLS || c < 0 || c >= CELLS) {
                        return;
                    }
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
