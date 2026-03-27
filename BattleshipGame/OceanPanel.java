import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Your ocean grid: drop ships here, drag to reposition before battle.
 */
class OceanPanel extends JPanel {
    private static final int GRID_PIXELS = 520;
    private static final Point GRID_ORIGIN = new Point(12, 12);

    private final GameController controller;
    private final Model model;
    private GameGrid grid;

    OceanPanel(GameController controller) {
        this.controller = controller;
        this.model = controller.getModel();
        setOpaque(false);
        setLayout(null);

        rebuildGrid();
    }

    private void rebuildGrid() {
        removeAll();
        grid = new GameGrid(10, GRID_PIXELS, GRID_PIXELS, GRID_ORIGIN);
        grid.setLayout(null);
        int pad = GRID_ORIGIN.x * 2 + GRID_PIXELS;
        grid.setBounds(0, 0, pad, pad);

        grid.setDropTarget(new DropTarget(grid, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    String data = (String) e.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    String[] parts = data.split(",");
                    Model.ShipType type = Model.ShipType.valueOf(parts[0]);
                    boolean horiz = Boolean.parseBoolean(parts[1]);
                    Point loc = e.getLocation();
                    int[] cell = grid.getCellInside(loc);
                    if (cell[0] < 0) {
                        e.dropComplete(false);
                        return;
                    }
                    boolean ok = controller.placeShipFromDock(type, cell[0], cell[1], horiz);
                    e.dropComplete(ok);
                } catch (Exception ex) {
                    e.dropComplete(false);
                }
            }
        }));

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Model.CellStatus st = model.incomingAt(row, col);
                if (st != Model.CellStatus.DONTKNOW) {
                    grid.add(new Shot(st, new int[] { row, col }, grid));
                }
            }
        }

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (model.yourCell(row, col) != Model.ShipType.EMPTY) {
                    ShipSquare sq = new ShipSquare(grid, model, row, col);
                    sq.setCellSquare(row, col);
                    wireShipDrag(sq, row, col);
                    grid.add(sq);
                }
            }
        }

        add(grid);
        revalidate();
    }

    private int dragStartRow = -1;
    private int dragStartCol = -1;

    private void wireShipDrag(ShipSquare sq, int row, int col) {
        sq.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!model.getCanMoveShips() || model.isGameOver()) {
                    return;
                }
                int[] cell = grid.getCellInside(SwingUtilities.convertPoint(sq, e.getPoint(), grid));
                if (cell[0] >= 0) {
                    dragStartRow = cell[0];
                    dragStartCol = cell[1];
                } else {
                    dragStartRow = row;
                    dragStartCol = col;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!model.getCanMoveShips() || dragStartRow < 0 || model.isGameOver()) {
                    dragStartRow = -1;
                    return;
                }
                Point onGrid = SwingUtilities.convertPoint(sq, e.getPoint(), grid);
                int[] cell = grid.getCellInside(onGrid);
                if (cell[0] >= 0) {
                    controller.movePlacedShip(dragStartRow, dragStartCol, cell[0], cell[1]);
                }
                dragStartRow = -1;
            }
        });
    }

    void rebuildPieces() {
        rebuildGrid();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int w = GRID_ORIGIN.x * 2 + GRID_PIXELS;
        return new Dimension(w, w);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}
