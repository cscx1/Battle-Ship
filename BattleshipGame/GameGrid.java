import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Draws the water grid and converts between pixel coords and cell indices.
 * Child components (ship cells, hit markers) use the same coordinate system.
 */
class GameGrid extends JComponent {
    final int numOfCells;
    final int boardWidth;
    final int boardHeight;
    private final int top;
    private final int left;
    private final int cellWidth;
    private final int cellHeight;
    private final int strokeSize = 2;

    private int previewCount;
    private final int[] previewRow = new int[5];
    private final int[] previewCol = new int[5];
    private boolean previewValid;
    private int previewStamp;

    GameGrid(int numOfCells, int boardWidth, int boardHeight, Point topLeft) {
        this.numOfCells = numOfCells;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        left = topLeft.x + strokeSize;
        top = topLeft.y + strokeSize;
        cellHeight = boardHeight / numOfCells;
        cellWidth = boardWidth / numOfCells;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(strokeSize));

        Color water = new Color(160, 200, 240);
        for (int x = 0; x < numOfCells; x++) {
            for (int y = 0; y < numOfCells; y++) {
                g2d.setColor(water);
                g2d.fillRect(left + x * cellWidth, top + y * cellHeight, cellWidth, cellHeight);
            }
        }

        g2d.setColor(new Color(30, 50, 80));
        for (int i = 0; i <= numOfCells; i++) {
            g2d.drawLine(left + i * cellWidth, top, left + i * cellWidth, boardHeight + top);
        }
        for (int i = 0; i <= numOfCells; i++) {
            g2d.drawLine(left, top + i * cellHeight, boardWidth + left, top + i * cellHeight);
        }
    }

    int getCellWidth() {
        return cellWidth;
    }

    int getCellHeight() {
        return cellHeight;
    }

    /** Returns [row, col], or [-1,-1] if the point isn't on the grid. */
    int[] getCellInside(Point mousePosition) {
        int col = (mousePosition.x - left) / cellWidth;
        int row = (mousePosition.y - top) / cellHeight;
        if (row < 0 || row >= numOfCells || col < 0 || col >= numOfCells) {
            return new int[] { -1, -1 };
        }
        return new int[] { row, col };
    }

    /** Top-left pixel of a cell inside this component. */
    int[] getCellPosition(int[] cellIndex) {
        int row = cellIndex[0];
        int col = cellIndex[1];
        if (row < 0 || row >= numOfCells || col < 0 || col >= numOfCells) {
            return new int[] { -1, -1 };
        }
        return new int[] { left + col * cellWidth, top + row * cellHeight };
    }

    /**
     * Highlights up to five board cells for drag placement preview (painted above children).
     * Pass count 0 to clear. Returns false if nothing changed (throttles repaints).
     */
    boolean setPlacementPreviewCells(int count, int[] rows, int[] cols, boolean valid) {
        if (count < 0 || count > 5 || rows == null || cols == null) {
            count = 0;
        }
        if (count == 0) {
            if (previewCount == 0) {
                return false;
            }
            previewCount = 0;
            previewStamp = 0;
            previewValid = false;
            Arrays.fill(previewRow, 0);
            Arrays.fill(previewCol, 0);
            repaint();
            return true;
        }
        int stamp = 17;
        for (int i = 0; i < count; i++) {
            stamp = 31 * stamp + rows[i];
            stamp = 31 * stamp + cols[i];
        }
        stamp = 31 * stamp + (valid ? 1 : 0);
        if (stamp == previewStamp && count == previewCount && previewValid == valid) {
            boolean same = true;
            for (int i = 0; i < count; i++) {
                if (previewRow[i] != rows[i] || previewCol[i] != cols[i]) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return false;
            }
        }
        previewStamp = stamp;
        previewCount = count;
        previewValid = valid;
        Arrays.fill(previewRow, 0);
        Arrays.fill(previewCol, 0);
        for (int i = 0; i < count; i++) {
            previewRow[i] = rows[i];
            previewCol[i] = cols[i];
        }
        repaint();
        return true;
    }

    void clearPlacementPreview() {
        setPlacementPreviewCells(0, previewRow, previewCol, false);
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (previewCount <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = previewValid
                ? new Color(40, 200, 90, 110)
                : new Color(230, 50, 50, 115);
        Color edge = previewValid
                ? new Color(0, 120, 40, 220)
                : new Color(140, 0, 0, 230);
        for (int i = 0; i < previewCount; i++) {
            int[] pix = getCellPosition(new int[] { previewRow[i], previewCol[i] });
            if (pix[0] < 0) {
                continue;
            }
            int inset = 2;
            g2.setColor(fill);
            g2.fillRoundRect(pix[0] + inset, pix[1] + inset,
                    cellWidth - 2 * inset, cellHeight - 2 * inset, 6, 6);
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(pix[0] + inset, pix[1] + inset,
                    cellWidth - 2 * inset, cellHeight - 2 * inset, 6, 6);
        }
        g2.dispose();
    }
}
