import javax.swing.*;
import java.awt.*;

/**
 * One cell of a ship on your grid. Draws the matching PNG segment from
 * Battleship/images (Carrier1…5, Battleship1…4, etc.).
 */
class ShipSquare extends JComponent {
    private final GameGrid gameGrid;
    private final Model model;
    private final int row;
    private final int col;

    ShipSquare(GameGrid gameGrid, Model model, int row, int col) {
        this.gameGrid = gameGrid;
        this.model = model;
        this.row = row;
        this.col = col;
    }

    void setCellSquare(int row, int col) {
        int[] pos = gameGrid.getCellPosition(new int[] { row, col });
        if (pos[0] < 0) {
            return;
        }
        setBounds(pos[0], pos[1], gameGrid.getCellWidth(), gameGrid.getCellHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        ShipImages.paintSegment(g, model, row, col, w, h);
    }
}
