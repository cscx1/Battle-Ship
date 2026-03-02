import java.awt.Point;
import java.util.ArrayList;

/**
 * one ship is just a list of cell positions on the grid.
 * each position is a point (row, col). the full game uses this for
 * placement and for drag-to-move (comparing old vs new coords).
 */
public class Ship {
    // list of grid cells this ship occupies (as row, col)
    public ArrayList<Point> xy_coords = new ArrayList<>();

    // used when dragging: tentative new positions before we check if the move is valid
    public ArrayList<Point> new_xy_coords = new ArrayList<>();

    public void setShipPoint(int x, int y, int index) {
        xy_coords.set(index, new Point(x, y));
    }

    // for drag: set one of the proposed new positions. skeleton keeps this so the
    // structure matches the full game.
    public void setNewCoords(int index, Point p) {
        if (index >= new_xy_coords.size()) {
            new_xy_coords.add(p);
        } else {
            new_xy_coords.set(index, p);
        }
    }
}
