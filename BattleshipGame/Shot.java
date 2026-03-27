import javax.swing.*;
import java.awt.*;

/**
 * Incoming fire on your ocean: red = hit on you, white = miss (per assignment).
 */
class Shot extends JComponent {
    Model.CellStatus hitMiss;
    int row, col;
    GameGrid gameGrid;

    Shot(Model.CellStatus hitMiss, int[] coords, GameGrid gameGrid) {
        this.hitMiss = hitMiss;
        this.row = coords[0];
        this.col = coords[1];
        this.gameGrid = gameGrid;
        int[] position = gameGrid.getCellPosition(coords);
        setBounds(position[0], position[1], gameGrid.getCellWidth(), gameGrid.getCellHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (hitMiss == Model.CellStatus.HIT) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.WHITE);
        }
        int w = gameGrid.getCellWidth() - 4;
        int h = gameGrid.getCellHeight() - 4;
        g.fillOval(2, 2, w, h);
    }
}
