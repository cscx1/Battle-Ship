import javax.swing.*;
import java.awt.*;

/**
the grid is a square of cells. we need two kinds of conversion: from
mouse position (pixels) to which cell was clicked, and from cell index
to pixel position so we can draw things (like shots or ship segments) in
the right place.
 */
class GameGrid extends JComponent {
    int numOfCells;
    int boardWidth, boardHeight;
    int top, left;
    int cellWidth, cellHeight;
    int strokeSize = 2;
    Model gameState;

    GameGrid(int numOfCells, int boardWidth, int boardHeight, Point topLeft, Model gameState) {
        this.numOfCells = numOfCells;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        left = topLeft.x + strokeSize;
        top = topLeft.y + strokeSize;
        this.cellHeight = boardHeight / numOfCells;
        this.cellWidth = boardWidth / numOfCells;
        this.gameState = gameState;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(strokeSize));

        // fill each cell with a light blue so it looks like water (no image needed)
        Color water = new Color(180, 220, 255);
        for (int x = 0; x < numOfCells; x++) {
            for (int y = 0; y < numOfCells; y++) {
                g2d.setColor(water);
                g2d.fillRect(left + x * cellWidth, top + y * cellHeight, cellWidth, cellHeight);
            }
        }

        // draw grid lines
        g2d.setColor(Color.BLACK);
        for (int i = 0; i <= numOfCells; i++) {
            g2d.drawLine(left + i * cellWidth, top, left + i * cellWidth, boardHeight + top);
        }
        for (int i = 0; i <= numOfCells; i++) {
            g2d.drawLine(left, top + i * cellHeight, boardWidth + left, top + i * cellHeight);
        }
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    /**
    converts a mouse position (in pixels) to the grid cell index [row, col].
    returns [-1, -1] if the point is outside the grid.
     */
    public int[] getCellInside(Point mousePosition) {
        int col = (mousePosition.x - left) / cellWidth;
        int row = (mousePosition.y - top) / cellHeight;
        if (row < 0 || row >= numOfCells || col < 0 || col >= numOfCells) {
            return new int[] { -1, -1 };
        }
        return new int[] { row, col };
    }

    /**
    converts a cell index [row, col] to the pixel position (top-left of that
    cell) so we can place a Shot or ShipSquare there.
     */
    public int[] getCellPosition(int[] cellIndex) {
        int row = cellIndex[0];
        int col = cellIndex[1];
        if (row < 0 || row >= numOfCells || col < 0 || col >= numOfCells) {
            return new int[] { -1, -1 };
        }
        return new int[] { left + col * cellWidth, top + row * cellHeight };
    }
}
