import java.awt.Point;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * the model holds all game state for this player. it doesn't draw anything;
 * the view reads from the model to know what to show. the model also talks
 * to the server (through the streams the client passed in) when we shoot or
 * when the opponent shoots at us.
 */
public class Model {
    // what we know about each cell on the opponent's board: hit, miss, or not shot yet
    public enum CellStatus {
        HIT,
        MISS,
        DONTKNOW
    }

    // what (if any) ship is on each cell of our board
    public enum ShipType {
        EMPTY,
        CARRIER,
        BATTLESHIP,
        CRUISER,
        SUBMARINE,
        DESTROYER
    }

    // our ships: which cells we occupy. the view uses this to draw ship squares
    private ShipType[][] yourBoard = new ShipType[10][10];
    // what we've learned about the opponent's grid (hit, miss, or not yet shot)
    private CellStatus[][] theirBoard = new CellStatus[10][10];
    // when the opponent shoots at us, we record hit/miss here so the view can show it
    private CellStatus[][] yourHits = new CellStatus[10][10];

    private int boardSize = 10;
    private boolean playerMove;  // true when it's our turn to shoot
    private String logMessage = "place your ships";
    private boolean canMoveShips = true;
    private int score = 0;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ArrayList<Ship> battleShips = new ArrayList<>();
    private Runnable onUpdate;  // optional: called when state changes so the view can refresh

    public Model(int boardSize, int playerNumber, ObjectOutputStream out, ObjectInputStream in) {
        this.boardSize = boardSize;
        this.out = out;
        this.in = in;
        // player 1 (number 1) goes first
        this.playerMove = (playerNumber == 1);
        setTheirBoard();
        emptyYourBoard();
        setYourBoard();
    }

    public void setLog(String logMessage) {
        this.logMessage = logMessage;
    }

    public String getLog() {
        return logMessage;
    }

    public boolean getCanMoveShips() {
        return canMoveShips;
    }

    public void setCanMoveShips(boolean canMoveShips) {
        this.canMoveShips = canMoveShips;
    }

    public int getScore() {
        return score;
    }

    public CellStatus getHitIndex(int x, int y) {
        return yourHits[x][y];
    }

    public ShipType getYourBoardIndex(int x, int y) {
        return yourBoard[x][y];
    }

    public CellStatus getTheirBoardIndex(int x, int y) {
        return theirBoard[x][y];
    }

    public boolean isPlayersTurn() {
        return playerMove;
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    // fill both "opponent" and "incoming shots" boards with unknown
    public void setTheirBoard() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                theirBoard[i][j] = CellStatus.DONTKNOW;
                yourHits[i][j] = CellStatus.DONTKNOW;
            }
        }
    }

    public void emptyYourBoard() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                yourBoard[i][j] = ShipType.EMPTY;
            }
        }
    }

    /**
     * skeleton version: place one small ship (destroyer, length 2) in fixed
     * cells so we don't need images or random placement. you can extend this
     * to add more ships or random placement later.
     */
    public void setYourBoard() {
        emptyYourBoard();
        // one ship of length 2 at (0,0) and (0,1)
        Ship destroyer = new Ship();
        destroyer.xy_coords.add(new Point(0, 0));
        destroyer.xy_coords.add(new Point(0, 1));
        destroyer.new_xy_coords.add(new Point(0, 0));
        destroyer.new_xy_coords.add(new Point(0, 1));
        battleShips.add(destroyer);
        yourBoard[0][0] = ShipType.DESTROYER;
        yourBoard[0][1] = ShipType.DESTROYER;
    }

    /**
     * send the shot coordinates to the server and read back hit/miss. then
     * update our view of the opponent's board and switch turns.
     */
    public void shoot(int row, int col) {
        int[] firePosition = { row, col };
        try {
            out.writeObject(firePosition);
            out.flush();
            int[] result = (int[]) in.readObject();
            if (result[0] >= 0) {
                theirBoard[row][col] = CellStatus.HIT;
                score++;
                logMessage = "you hit";
            } else {
                theirBoard[row][col] = CellStatus.MISS;
                logMessage = "you missed";
            }
            playerMove = false;
        } catch (IOException | ClassNotFoundException e) {
            logMessage = "error: " + e.getMessage();
        }
    }

    /**
     * run in a background thread: when it's not our turn, wait for the
     * opponent's shot (an int[] from the server), check if it hit us,
     * send the result back, then it becomes our turn again.
     */
    public void waitForOpponent() {
        new Thread(() -> {
            while (true) {
                if (playerMove) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) { }
                    continue;
                }
                try {
                    int[] firePosition = (int[]) in.readObject();
                    int hitResult = checkForHit(firePosition[0], firePosition[1]);
                    int[] result = { hitResult };
                    yourHits[firePosition[0]][firePosition[1]] = (hitResult >= 0) ? CellStatus.HIT : CellStatus.MISS;
                    out.writeObject(result);
                    out.flush();
                    playerMove = true;
                    if (onUpdate != null) {
                        javax.swing.SwingUtilities.invokeLater(onUpdate);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }
            }
        }).start();
    }

    // returns -1 for miss, 0 or higher for hit (skeleton doesn't care about sink)
    private int checkForHit(int row, int col) {
        if (yourBoard[row][col] != ShipType.EMPTY) {
            return 0;
        }
        return -1;
    }
}
