import java.awt.Point;
import java.util.ArrayList;

/**
 * One ship on your grid: which cells it spans and how many times it's been hit.
 */
public class Ship {
    public final Model.ShipType kind;
    public final boolean horizontal;
    public final ArrayList<Point> xy_coords = new ArrayList<>();
    public int hitsTaken;

    public Ship(Model.ShipType kind, boolean horizontal) {
        this.kind = kind;
        this.horizontal = horizontal;
    }

    public boolean occupies(int row, int col) {
        for (Point p : xy_coords) {
            if (p.x == row && p.y == col) {
                return true;
            }
        }
        return false;
    }

    public boolean isSunk() {
        return hitsTaken >= xy_coords.size();
    }
}
