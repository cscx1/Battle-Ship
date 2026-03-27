import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Title row plus two columns: your fleet status and enemy ships you've sunk.
 */
class ScorePanel extends JPanel {
    private final Model model;
    private final JLabel shipsLabel;
    private final JLabel turnLabel;
    private final JLabel enemyLabel;
    private final JTextArea yourStatus;
    private final JTextArea enemyStatus;

    ScorePanel(Model model) {
        this.model = model;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setBackground(new Color(15, 25, 50));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        shipsLabel = new JLabel();
        shipsLabel.setForeground(new Color(180, 200, 255));
        shipsLabel.setFont(new Font("Arial", Font.BOLD, 14));

        turnLabel = new JLabel("BATTLESHIP", SwingConstants.CENTER);
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 16));

        enemyLabel = new JLabel();
        enemyLabel.setForeground(new Color(255, 180, 160));
        enemyLabel.setFont(new Font("Arial", Font.BOLD, 14));
        enemyLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        top.add(shipsLabel, BorderLayout.WEST);
        top.add(turnLabel, BorderLayout.CENTER);
        top.add(enemyLabel, BorderLayout.EAST);

        Font small = new Font("Monospaced", Font.PLAIN, 10);
        yourStatus = new JTextArea(5, 20);
        yourStatus.setEditable(false);
        yourStatus.setFont(small);
        yourStatus.setBackground(new Color(12, 18, 35));
        yourStatus.setForeground(new Color(200, 210, 240));
        yourStatus.setBorder(new EmptyBorder(4, 6, 4, 6));
        yourStatus.setLineWrap(false);
        yourStatus.setWrapStyleWord(false);

        enemyStatus = new JTextArea(5, 20);
        enemyStatus.setEditable(false);
        enemyStatus.setFont(small);
        enemyStatus.setBackground(new Color(12, 18, 35));
        enemyStatus.setForeground(new Color(240, 210, 200));
        enemyStatus.setBorder(new EmptyBorder(4, 6, 4, 6));
        enemyStatus.setLineWrap(false);
        enemyStatus.setWrapStyleWord(false);

        JPanel mid = new JPanel(new GridLayout(1, 2, 8, 0));
        mid.setOpaque(false);
        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setOpaque(false);
        JLabel yl = new JLabel("Your ships");
        yl.setForeground(Color.LIGHT_GRAY);
        leftWrap.add(yl, BorderLayout.NORTH);
        leftWrap.add(yourStatus, BorderLayout.CENTER);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        JLabel er = new JLabel("Enemy (what you know)");
        er.setForeground(Color.LIGHT_GRAY);
        rightWrap.add(er, BorderLayout.NORTH);
        rightWrap.add(enemyStatus, BorderLayout.CENTER);

        mid.add(leftWrap);
        mid.add(rightWrap);

        add(top, BorderLayout.NORTH);
        add(mid, BorderLayout.CENTER);

        syncFromModel();
    }

    void syncFromModel() {
        shipsLabel.setText("Ships left: " + model.getYourShipsLeft());
        enemyLabel.setText("Enemy left: " + model.getEnemyShipsLeft());
        if (model.getCanMoveShips()) {
            turnLabel.setText("BATTLESHIP — place fleet");
        } else if (model.isGameOver()) {
            turnLabel.setText(model.isYouWon() ? "You won" : "Game over");
        } else if (model.isPlayersTurn()) {
            turnLabel.setText("Your turn");
        } else {
            turnLabel.setText("Their turn");
        }

        Model.ShipType[] order = {
                Model.ShipType.CARRIER, Model.ShipType.BATTLESHIP, Model.ShipType.CRUISER,
                Model.ShipType.SUBMARINE, Model.ShipType.DESTROYER
        };

        StringBuilder y = new StringBuilder();
        boolean inBattle = !model.getCanMoveShips();
        for (Model.ShipType t : order) {
            y.append(label(t)).append(": ");
            if (!inBattle) {
                y.append(model.shipTypeAvailable(t) ? "not placed\n" : "placed\n");
            } else {
                y.append(model.isYourShipSunk(t) ? "SUNK\n" : "still afloat\n");
            }
        }
        yourStatus.setText(y.toString());
        yourStatus.setCaretPosition(0);

        StringBuilder e = new StringBuilder();
        for (Model.ShipType t : order) {
            e.append(label(t)).append(": ");
            if (!inBattle) {
                e.append("—\n");
            } else {
                e.append(model.isEnemyShipTypeSunk(t) ? "you sank it\n" : "not sunk yet\n");
            }
        }
        enemyStatus.setText(e.toString());
        enemyStatus.setCaretPosition(0);
    }

    private static String label(Model.ShipType t) {
        return t.name().charAt(0) + t.name().substring(1).toLowerCase();
    }
}
