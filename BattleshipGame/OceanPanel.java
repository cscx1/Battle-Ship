import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

/**
 * Your ocean grid: drop ships here, drag to reposition before battle.
 */
class OceanPanel extends JPanel {
    private static final int GRID_PIXELS = 520;
    private static final Point GRID_ORIGIN = new Point(12, 12);

    private final GameController controller;
    private final Model model;
    private GameGrid grid;

    private final int[] previewRows = new int[5];
    private final int[] previewCols = new int[5];

    private int dragStartRow = -1;
    private int dragStartCol = -1;

    OceanPanel(GameController controller) {
        this.controller = controller;
        this.model = controller.getModel();
        setOpaque(false);
        setLayout(null);

        rebuildGrid();
    }

    private static int shipLength(Model.ShipType t) {
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

    private void updateDockDragPreview(Point locOnGrid, Transferable tr) {
        if (tr == null || !model.getCanMoveShips() || model.isGameOver()) {
            grid.clearPlacementPreview();
            return;
        }
        String data;
        try {
            if (!tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                grid.clearPlacementPreview();
                return;
            }
            data = (String) tr.getTransferData(DataFlavor.stringFlavor);
        } catch (Exception ex) {
            grid.clearPlacementPreview();
            return;
        }
        String[] parts = data.split(",");
        if (parts.length < 2) {
            grid.clearPlacementPreview();
            return;
        }
        Model.ShipType type;
        try {
            type = Model.ShipType.valueOf(parts[0]);
        } catch (IllegalArgumentException ex) {
            grid.clearPlacementPreview();
            return;
        }
        boolean horiz = Boolean.parseBoolean(parts[1]);
        int len = shipLength(type);
        if (len <= 0 || !model.shipTypeAvailable(type)) {
            grid.clearPlacementPreview();
            return;
        }

        int[] cell = grid.getCellInside(locOnGrid);
        if (cell[0] < 0) {
            grid.clearPlacementPreview();
            return;
        }
        int row = cell[0];
        int col = cell[1];

        boolean valid = model.canPlaceShipFromDock(type, row, col, horiz);
        int n = 0;
        for (int i = 0; i < len; i++) {
            int rr = horiz ? row : row + i;
            int cc = horiz ? col + i : col;
            if (rr < 0 || rr >= 10 || cc < 0 || cc >= 10) {
                continue;
            }
            previewRows[n] = rr;
            previewCols[n] = cc;
            n++;
        }
        if (n == 0) {
            grid.clearPlacementPreview();
            return;
        }
        grid.setPlacementPreviewCells(n, previewRows, previewCols, valid);
    }

    private void updateRepositionPreview(Point locOnGrid) {
        if (dragStartRow < 0) {
            return;
        }
        int[] cell = grid.getCellInside(locOnGrid);
        if (cell[0] < 0) {
            grid.clearPlacementPreview();
            return;
        }
        boolean valid = model.canMovePlacedShip(dragStartRow, dragStartCol, cell[0], cell[1]);
        List<Point> coords = model.getShipCellCoordsForPreview(dragStartRow, dragStartCol);
        if (coords == null) {
            grid.clearPlacementPreview();
            return;
        }
        int dr = cell[0] - dragStartRow;
        int dc = cell[1] - dragStartCol;
        int n = 0;
        for (Point p : coords) {
            int rr = p.x + dr;
            int cc = p.y + dc;
            if (rr < 0 || rr >= 10 || cc < 0 || cc >= 10) {
                continue;
            }
            previewRows[n] = rr;
            previewCols[n] = cc;
            n++;
        }
        if (n == 0) {
            grid.clearPlacementPreview();
            return;
        }
        grid.setPlacementPreviewCells(n, previewRows, previewCols, valid);
    }

    private void rebuildGrid() {
        removeAll();
        grid = new GameGrid(10, GRID_PIXELS, GRID_PIXELS, GRID_ORIGIN);
        grid.setLayout(null);
        int pad = GRID_ORIGIN.x * 2 + GRID_PIXELS;
        grid.setBounds(0, 0, pad, pad);

        grid.setDropTarget(new DropTarget(grid, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
                if (e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    e.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    e.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent e) {
                if (!e.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    e.rejectDrag();
                    return;
                }
                e.acceptDrag(DnDConstants.ACTION_COPY);
                updateDockDragPreview(e.getLocation(), e.getTransferable());
            }

            @Override
            public void dragExit(DropTargetEvent e) {
                grid.clearPlacementPreview();
            }

            @Override
            public void drop(DropTargetDropEvent e) {
                grid.clearPlacementPreview();
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
                grid.clearPlacementPreview();
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
        sq.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!model.getCanMoveShips() || dragStartRow < 0 || model.isGameOver()) {
                    return;
                }
                Point onGrid = SwingUtilities.convertPoint(sq, e.getPoint(), grid);
                updateRepositionPreview(onGrid);
            }
        });
    }

    void rebuildPieces() {
        dragStartRow = -1;
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
