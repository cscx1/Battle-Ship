import javax.swing.*;
import java.awt.*;

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
}
