import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Ship dock (drag onto ocean), auto-place, start, exit. FlowLayout wraps without scroll bars.
 */
class FleetDockPanel extends JPanel {
    private final GameController controller;
    private final Model model;
    private final FleetTile[] fleetTiles;
    private final JButton startButton;
    private final JButton autoButton;

    FleetDockPanel(GameController controller, JLabel orientLabel) {
        this.controller = controller;
        this.model = controller.getModel();

        setLayout(new BorderLayout(8, 0));
        setBackground(new Color(25, 35, 65));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 80, 120)),
                BorderFactory.createEmptyBorder(6, 12, 8, 12)));

        JPanel flow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        flow.setOpaque(false);

        JLabel fleetTitle = new JLabel("Your fleet (drag onto ocean)");
        fleetTitle.setForeground(Color.LIGHT_GRAY);
        fleetTitle.setFont(new Font("Arial", Font.BOLD, 11));
        flow.add(fleetTitle);

        Model.ShipType[] order = {
                Model.ShipType.CARRIER, Model.ShipType.BATTLESHIP, Model.ShipType.CRUISER,
                Model.ShipType.SUBMARINE, Model.ShipType.DESTROYER
        };
        fleetTiles = new FleetTile[order.length];
        for (int i = 0; i < order.length; i++) {
            FleetTile tile = new FleetTile(model, order[i], orientLabel);
            tile.setPreferredSize(new Dimension(124, 64));
            tile.setMaximumSize(new Dimension(140, 68));
            fleetTiles[i] = tile;
            flow.add(tile);
        }

        add(flow, BorderLayout.CENTER);

        JPanel btnCol = new JPanel();
        btnCol.setOpaque(false);
        btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));
        autoButton = new JButton("Auto-place");
        autoButton.setFont(new Font("Arial", Font.PLAIN, 11));
        autoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        autoButton.setMaximumSize(new Dimension(118, 28));
        autoButton.addActionListener(e -> controller.autoPlaceFleet());
        btnCol.add(autoButton);
        btnCol.add(Box.createVerticalStrut(4));
        startButton = new JButton("Start battle");
        startButton.setFont(new Font("Arial", Font.BOLD, 11));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setMaximumSize(new Dimension(118, 30));
        startButton.addActionListener(e -> controller.beginBattle());
        btnCol.add(startButton);
        btnCol.add(Box.createVerticalStrut(4));
        JButton exit = new JButton("Exit");
        exit.setFont(new Font("Arial", Font.PLAIN, 11));
        exit.setAlignmentX(Component.CENTER_ALIGNMENT);
        exit.setMaximumSize(new Dimension(118, 26));
        exit.addActionListener(e -> System.exit(0));
        btnCol.add(exit);
        add(btnCol, BorderLayout.EAST);
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

            setLayout(new BorderLayout(2, 0));
            setOpaque(true);
            setBackground(new Color(35, 50, 85));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(80, 100, 140)),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)));

            String name = type.name().charAt(0) + type.name().substring(1).toLowerCase();
            JLabel title = new JLabel("<html><div style='width:68px'>" + name + " (" + length(type) + ")</div></html>");
            title.setForeground(Color.WHITE);
            title.setFont(new Font("Arial", Font.BOLD, 10));
            add(title, BorderLayout.CENTER);

            orientHint = new JLabel("H");
            orientHint.setForeground(new Color(150, 220, 150));
            orientHint.setFont(new Font("Arial", Font.PLAIN, 9));

            JButton rotateBtn = new JButton("↻");
            rotateBtn.setFont(new Font("Arial", Font.PLAIN, 11));
            rotateBtn.setMargin(new Insets(0, 4, 0, 4));
            rotateBtn.setToolTipText("Rotate orientation");
            rotateBtn.addActionListener(ev -> {
                if (!model.shipTypeAvailable(type)) {
                    return;
                }
                horizontal = !horizontal;
                orientHint.setText(horizontal ? "H" : "V");
                orientLabel.setText("Orientation: " + (horizontal ? "HORIZONTAL" : "VERTICAL")
                        + "  (Rotate button or right-click)");
            });

            JPanel east = new JPanel(new BorderLayout(0, 0));
            east.setOpaque(false);
            east.add(rotateBtn, BorderLayout.NORTH);
            east.add(orientHint, BorderLayout.SOUTH);
            add(east, BorderLayout.EAST);

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

            addMouseListener(pressAdapter);
            addMouseMotionListener(dragAdapter);
            title.addMouseListener(pressAdapter);
            title.addMouseMotionListener(dragAdapter);
            orientHint.addMouseListener(pressAdapter);
            orientHint.addMouseMotionListener(dragAdapter);
            rotateBtn.addMouseListener(pressAdapter);
        }

        void refreshEnabled() {
            boolean avail = model.shipTypeAvailable(type) && model.getCanMoveShips() && !model.isGameOver();
            setEnabled(avail);
            setBackground(avail ? new Color(35, 50, 85) : new Color(25, 28, 40));
        }
    }
}
