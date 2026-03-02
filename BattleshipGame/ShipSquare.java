import javax.swing.*;
import java.awt.*;

/**
 * represents one ship segment on the grid (one cell that has part of a ship).
 * in the full game this is draggable; here we just draw a gray rectangle so
 * you can see where the ship is. setCellSquare(row, col) positions it using
 * the grid's cell-to-pixel conversion.
 */
class ShipSquare extends JComponent {
    int xPos, yPos;
    int size;
    GameGrid gameGrid;
    Model gameState;

    public ShipSquare(GameGrid gameGrid, Model gameState) {
        this.gameGrid = gameGrid;
        this.gameState = gameState;
        this.size = gameGrid.getCellHeight();
    }

    /**
     * move this square to the given grid cell (row, col) by converting to pixels
     */
    public void setCellSquare(int row, int col) {
        int[] pos = gameGrid.getCellPosition(new int[] { row, col });
        if (pos[0] < 0) return;
        xPos = pos[0];
        yPos = pos[1];
        setBounds(xPos, yPos, size, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(2, 2, size - 4, size - 4);
    }
}
