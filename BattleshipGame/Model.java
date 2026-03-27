import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Game rules and state. No Swing drawing here — the view reads this and the
 * controller forwards user actions into these methods.
 */
public class Model {

    public enum CellStatus {
        HIT,
        MISS,
        DONTKNOW
    }

    public enum ShipType {
        EMPTY,
        CARRIER,
        BATTLESHIP,
        CRUISER,
        SUBMARINE,
        DESTROYER
    }

    public enum SoundCue {
        NONE,
        HIT,
        MISS,
        SINK,
        VICTORY,
        DEFEAT
    }

    private static int lengthOf(ShipType t) {
        switch (t) {
            case CARRIER:
                return 5;
            case BATTLESHIP:
                return 4;
            case CRUISER:
            case SUBMARINE:
                return 3;
            case DESTROYER:
                return 2;
            default:
                return 0;
        }
    }

    private static int sinkCodeFor(ShipType t) {
        switch (t) {
            case DESTROYER:
                return 1;
            case SUBMARINE:
                return 2;
            case CRUISER:
                return 3;
            case BATTLESHIP:
                return 4;
            case CARRIER:
                return 5;
            default:
                return 0;
        }
    }

    private static ShipType typeFromSinkCode(int code) {
        switch (code) {
            case 1:
                return ShipType.DESTROYER;
            case 2:
                return ShipType.SUBMARINE;
            case 3:
                return ShipType.CRUISER;
            case 4:
                return ShipType.BATTLESHIP;
            case 5:
                return ShipType.CARRIER;
            default:
                return null;
        }
    }

    private final int boardSize;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    private final ShipType[][] yourBoard;
    private final CellStatus[][] theirBoard;
    private final CellStatus[][] incomingOnYou;

    private boolean playerMove;
    private String logMessage = "Drag each ship from the fleet onto your ocean.";
    private boolean canMoveShips = true;
    private int hitsLandedOnEnemy;
    private int enemyShipsSunk;
    private int yourShipsSunk;

    private boolean gameOver;
    private boolean youWon;

    private final ArrayList<Ship> fleetOnBoard = new ArrayList<>();
    private final EnumSet<ShipType> typesStillInDock = EnumSet.of(
            ShipType.CARRIER, ShipType.BATTLESHIP, ShipType.CRUISER,
            ShipType.SUBMARINE, ShipType.DESTROYER);
    private final EnumSet<ShipType> enemyShipsSunkTypes = EnumSet.noneOf(ShipType.class);

    private SoundCue pendingSound = SoundCue.NONE;

    private Runnable onUpdate;

    public Model(int boardSize, int playerNumber, ObjectOutputStream out, ObjectInputStream in) {
        this.boardSize = boardSize;
        this.out = out;
        this.in = in;
        this.playerMove = (playerNumber == 1);
        this.yourBoard = new ShipType[boardSize][boardSize];
        this.theirBoard = new CellStatus[boardSize][boardSize];
        this.incomingOnYou = new CellStatus[boardSize][boardSize];
        clearTargetingBoards();
        clearYourOcean();
    }

    public void setOnUpdate(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    private void notifyUi() {
        if (onUpdate != null) {
            javax.swing.SwingUtilities.invokeLater(onUpdate);
        }
    }

    private void queueSound(SoundCue cue) {
        if (cue != SoundCue.NONE) {
            pendingSound = cue;
        }
    }

    public synchronized SoundCue takeSoundCue() {
        SoundCue s = pendingSound;
        pendingSound = SoundCue.NONE;
        return s;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized boolean isYouWon() {
        return youWon;
    }

    public synchronized boolean isEnemyShipTypeSunk(ShipType t) {
        return enemyShipsSunkTypes.contains(t);
    }

    public synchronized boolean isYourShipSunk(ShipType t) {
        for (Ship s : fleetOnBoard) {
            if (s.kind == t) {
                return s.isSunk();
            }
        }
        return false;
    }

    public synchronized void setLog(String logMessage) {
        this.logMessage = logMessage;
    }

    public synchronized String getLog() {
        return logMessage;
    }

    public synchronized boolean getCanMoveShips() {
        return canMoveShips;
    }

    public synchronized void setCanMoveShips(boolean canMoveShips) {
        this.canMoveShips = canMoveShips;
    }

    public synchronized int getScore() {
        return hitsLandedOnEnemy;
    }

    public synchronized int getYourShipsLeft() {
        return 5 - yourShipsSunk;
    }

    public synchronized int getEnemyShipsLeft() {
        return 5 - enemyShipsSunk;
    }

    public synchronized CellStatus incomingAt(int row, int col) {
        return incomingOnYou[row][col];
    }

    public synchronized CellStatus theirCell(int row, int col) {
        return theirBoard[row][col];
    }

    public synchronized ShipType yourCell(int row, int col) {
        return yourBoard[row][col];
    }

    public synchronized boolean isPlayersTurn() {
        return playerMove;
    }

    public synchronized boolean isFleetReady() {
        return typesStillInDock.isEmpty() && fleetOnBoard.size() == 5;
    }

    public synchronized boolean shipTypeAvailable(ShipType t) {
        return typesStillInDock.contains(t);
    }

    private void clearTargetingBoards() {
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                theirBoard[r][c] = CellStatus.DONTKNOW;
                incomingOnYou[r][c] = CellStatus.DONTKNOW;
            }
        }
    }

    private void clearYourOcean() {
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                yourBoard[r][c] = ShipType.EMPTY;
            }
        }
    }

    /**
     * Random valid layout for all five ships. Clears manual placement first.
     */
    public synchronized void autoPlaceFleet() {
        if (!canMoveShips || gameOver) {
            return;
        }
        Random rnd = new Random();
        ShipType[] types = {
                ShipType.CARRIER, ShipType.BATTLESHIP, ShipType.CRUISER,
                ShipType.SUBMARINE, ShipType.DESTROYER
        };
        for (int attempt = 0; attempt < 80; attempt++) {
            fleetOnBoard.clear();
            typesStillInDock.clear();
            typesStillInDock.addAll(EnumSet.of(
                    ShipType.CARRIER, ShipType.BATTLESHIP, ShipType.CRUISER,
                    ShipType.SUBMARINE, ShipType.DESTROYER));
            clearYourOcean();
            List<ShipType> order = new ArrayList<>();
            Collections.addAll(order, types);
            Collections.shuffle(order, rnd);
            boolean failed = false;
            for (ShipType t : order) {
                boolean placed = false;
                for (int tries = 0; tries < 400 && !placed; tries++) {
                    boolean horiz = rnd.nextBoolean();
                    int len = lengthOf(t);
                    int row;
                    int col;
                    if (horiz) {
                        row = rnd.nextInt(boardSize);
                        col = rnd.nextInt(boardSize - len + 1);
                    } else {
                        row = rnd.nextInt(boardSize - len + 1);
                        col = rnd.nextInt(boardSize);
                    }
                    if (fits(row, col, len, horiz) && !overlapsExisting(row, col, len, horiz, null)) {
                        Ship s = new Ship(t, horiz);
                        applyShipCells(s, row, col, len, horiz);
                        fleetOnBoard.add(s);
                        typesStillInDock.remove(t);
                        placed = true;
                    }
                }
                if (!placed) {
                    failed = true;
                    break;
                }
            }
            if (!failed && isFleetReady()) {
                logMessage = "Fleet placed automatically.";
                notifyUi();
                return;
            }
        }
        logMessage = "Auto-place failed — try again or place manually.";
        notifyUi();
    }

    public synchronized boolean placeShipFromDock(ShipType type, int row, int col, boolean horizontal) {
        if (!canMoveShips || gameOver || !typesStillInDock.contains(type)) {
            return false;
        }
        int len = lengthOf(type);
        if (!fits(row, col, len, horizontal)) {
            return false;
        }
        if (overlapsExisting(row, col, len, horizontal, null)) {
            return false;
        }
        Ship s = new Ship(type, horizontal);
        applyShipCells(s, row, col, len, horizontal);
        fleetOnBoard.add(s);
        typesStillInDock.remove(type);
        logMessage = named(type) + " placed.";
        notifyUi();
        return true;
    }

    private static String named(ShipType t) {
        return t.name().charAt(0) + t.name().substring(1).toLowerCase();
    }

    private boolean fits(int row, int col, int len, boolean horizontal) {
        if (horizontal) {
            return row >= 0 && row < boardSize && col >= 0 && col + len <= boardSize;
        }
        return col >= 0 && col < boardSize && row >= 0 && row + len <= boardSize;
    }

    private boolean overlapsExisting(int row, int col, int len, boolean horizontal, Ship ignore) {
        for (int i = 0; i < len; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;
            ShipType cell = yourBoard[r][c];
            if (cell != ShipType.EMPTY && (ignore == null || !ignore.occupies(r, c))) {
                return true;
            }
        }
        return false;
    }

    private void applyShipCells(Ship ship, int row, int col, int len, boolean horizontal) {
        ship.xy_coords.clear();
        for (int i = 0; i < len; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;
            yourBoard[r][c] = ship.kind;
            ship.xy_coords.add(new Point(r, c));
        }
    }

    private Ship shipAt(int row, int col) {
        for (Ship s : fleetOnBoard) {
            if (s.occupies(row, col)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Which segment this cell is along the ship (0 = start of the ship: left end if horizontal,
     * top end if vertical). Matches how the reference project maps Carrier1…N onto cells.
     */
    public synchronized int segmentIndexAt(int row, int col) {
        Ship s = shipAt(row, col);
        if (s == null) {
            return 0;
        }
        if (s.horizontal) {
            int minCol = Integer.MAX_VALUE;
            for (Point p : s.xy_coords) {
                minCol = Math.min(minCol, p.y);
            }
            return col - minCol;
        }
        int minRow = Integer.MAX_VALUE;
        for (Point p : s.xy_coords) {
            minRow = Math.min(minRow, p.x);
        }
        return row - minRow;
    }

    public synchronized Ship getShipCovering(int row, int col) {
        return shipAt(row, col);
    }

    public synchronized boolean movePlacedShip(int fromRow, int fromCol, int toRow, int toCol) {
        if (!canMoveShips || gameOver) {
            return false;
        }
        Ship s = shipAt(fromRow, fromCol);
        if (s == null) {
            return false;
        }
        int dr = toRow - fromRow;
        int dc = toCol - fromCol;
        ArrayList<Point> oldCells = new ArrayList<>(s.xy_coords);
        ArrayList<Point> nextCells = new ArrayList<>();
        for (Point p : oldCells) {
            nextCells.add(new Point(p.x + dr, p.y + dc));
        }
        for (Point p : nextCells) {
            if (p.x < 0 || p.x >= boardSize || p.y < 0 || p.y >= boardSize) {
                return false;
            }
        }
        clearShipFromBoard(s);
        for (Point p : nextCells) {
            if (yourBoard[p.x][p.y] != ShipType.EMPTY) {
                for (Point o : oldCells) {
                    yourBoard[o.x][o.y] = s.kind;
                }
                s.xy_coords.clear();
                s.xy_coords.addAll(oldCells);
                return false;
            }
        }
        s.xy_coords.clear();
        s.xy_coords.addAll(nextCells);
        for (Point p : nextCells) {
            yourBoard[p.x][p.y] = s.kind;
        }
        logMessage = "Moved your " + named(s.kind) + ".";
        notifyUi();
        return true;
    }

    private void clearShipFromBoard(Ship s) {
        for (Point p : s.xy_coords) {
            yourBoard[p.x][p.y] = ShipType.EMPTY;
        }
    }

    public synchronized void beginBattle() {
        if (gameOver) {
            return;
        }
        if (!isFleetReady()) {
            logMessage = "Place all five ships first.";
            notifyUi();
            return;
        }
        canMoveShips = false;
        logMessage = playerMove ? "Your turn — click the enemy board to fire." : "Waiting for the other player…";
        notifyAll();
        notifyUi();
    }

    public synchronized void shoot(int row, int col) {
        if (gameOver || canMoveShips || !playerMove || theirBoard[row][col] != CellStatus.DONTKNOW) {
            return;
        }
        int[] shot = { row, col };
        try {
            out.writeObject(shot);
            out.flush();
            int[] reply = (int[]) in.readObject();
            int code = reply[0];
            if (code >= 0) {
                theirBoard[row][col] = CellStatus.HIT;
                hitsLandedOnEnemy++;
                if (code >= 1 && code <= 5) {
                    ShipType sunk = typeFromSinkCode(code);
                    if (sunk != null) {
                        enemyShipsSunkTypes.add(sunk);
                    }
                    enemyShipsSunk++;
                    logMessage = sinkMessageForCode(code, false);
                    if (enemyShipsSunk >= 5) {
                        gameOver = true;
                        youWon = true;
                        queueSound(SoundCue.VICTORY);
                    } else {
                        queueSound(SoundCue.SINK);
                    }
                } else {
                    logMessage = "Hit.";
                    queueSound(SoundCue.HIT);
                }
            } else {
                theirBoard[row][col] = CellStatus.MISS;
                logMessage = "Miss.";
                queueSound(SoundCue.MISS);
            }
            playerMove = false;
            notifyAll();
        } catch (IOException | ClassNotFoundException e) {
            logMessage = "Connection problem: " + e.getMessage();
            notifyUi();
            return;
        }
        notifyUi();
    }

    private static String sinkMessageForCode(int code, boolean opponentSunkUs) {
        String who = opponentSunkUs ? "They sank your " : "You sank their ";
        String name;
        switch (code) {
            case 1:
                name = "destroyer";
                break;
            case 2:
                name = "submarine";
                break;
            case 3:
                name = "cruiser";
                break;
            case 4:
                name = "battleship";
                break;
            case 5:
                name = "carrier";
                break;
            default:
                name = "ship";
        }
        return who + name + "!";
    }

    public void waitForOpponent() {
        Thread t = new Thread(this::listenLoop, "battleship-incoming");
        t.setDaemon(true);
        t.start();
    }

    private void listenLoop() {
        while (true) {
            try {
                synchronized (this) {
                    while (playerMove && !gameOver) {
                        wait();
                    }
                    if (gameOver) {
                        return;
                    }
                }
                int[] incoming = (int[]) in.readObject();
                int r = incoming[0];
                int c = incoming[1];
                int result;
                synchronized (this) {
                    if (incomingOnYou[r][c] != CellStatus.DONTKNOW) {
                        result = incomingOnYou[r][c] == CellStatus.HIT ? 0 : -1;
                    } else {
                        result = resolveIncomingHit(r, c);
                        incomingOnYou[r][c] = (result >= 0) ? CellStatus.HIT : CellStatus.MISS;
                    }
                }
                out.writeObject(new int[] { result });
                out.flush();
                synchronized (this) {
                    if (!gameOver) {
                        playerMove = true;
                    }
                    notifyAll();
                }
                notifyUi();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException | ClassNotFoundException e) {
                break;
            }
        }
    }

    private int resolveIncomingHit(int row, int col) {
        ShipType cell = yourBoard[row][col];
        if (cell == ShipType.EMPTY) {
            logMessage = "They missed.";
            queueSound(SoundCue.MISS);
            return -1;
        }
        Ship s = shipAt(row, col);
        if (s == null) {
            logMessage = "They hit.";
            queueSound(SoundCue.HIT);
            return 0;
        }
        s.hitsTaken++;
        if (s.isSunk()) {
            yourShipsSunk++;
            logMessage = sinkMessageForCode(sinkCodeFor(s.kind), true);
            if (yourShipsSunk >= 5) {
                gameOver = true;
                youWon = false;
                queueSound(SoundCue.DEFEAT);
            } else {
                queueSound(SoundCue.SINK);
            }
            return sinkCodeFor(s.kind);
        }
        logMessage = "They hit your " + named(s.kind) + ".";
        queueSound(SoundCue.HIT);
        return 0;
    }
}
