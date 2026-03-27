/**
 * MVC controller: the view calls these methods instead of touching the model
 * directly for actions. The model still uses {@link Model#setOnUpdate} to push
 * incoming network events to the view.
 */
public class GameController {
    private final Model model;

    public GameController(Model model) {
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    public void beginBattle() {
        model.beginBattle();
    }

    public void shootEnemy(int row, int col) {
        if (model.isGameOver()) {
            return;
        }
        model.shoot(row, col);
    }

    public boolean placeShipFromDock(Model.ShipType type, int row, int col, boolean horizontal) {
        if (model.isGameOver()) {
            return false;
        }
        return model.placeShipFromDock(type, row, col, horizontal);
    }

    public boolean movePlacedShip(int fromRow, int fromCol, int toRow, int toCol) {
        if (model.isGameOver()) {
            return false;
        }
        return model.movePlacedShip(fromRow, fromCol, toRow, toCol);
    }

    public void autoPlaceFleet() {
        if (model.isGameOver()) {
            return;
        }
        model.autoPlaceFleet();
    }
}
