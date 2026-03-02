import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ObjectOutputStream;

/**
 * the view is the window the player sees. it holds a GameGrid (which draws
 * the board and converts clicks to cell indices), a label for score and log
 * messages, and a "play game" button. it gets all the data it needs from the
 * model — it doesn't decide the rules, it just displays and forwards clicks.
 */
public class View extends JFrame {
    Model gameState;
    ObjectOutputStream out;
    GameGrid gameGrid;
    JLabel scoreLabel;
    JLabel logBox;
    JButton startButton;

    int boardSize = 600;
    int sidePanelSize = 200;

    public View(Model gameState, ObjectOutputStream out) {
        this.gameState = gameState;
        this.out = out;
        setSize(boardSize + sidePanelSize + 20, boardSize + 80);
        getContentPane().setBackground(Color.decode("#003399"));
        gameState.setLog("place your ships");
        // when the model changes (e.g. opponent shot), refresh the display
        gameState.setOnUpdate(this::renderView);
        renderView();
    }

    /**
     * rebuild the window: grid, ship squares for our board, shot markers,
     * score, log, and play button. the model is the single source of truth.
     */
    public void renderView() {
        getContentPane().removeAll();
        setLayout(null);

        startButton = new JButton("Play Game");
        startButton.setBounds(20, 20, 120, 40);
        startButton.addActionListener(e -> {
            gameState.setCanMoveShips(false);
            startButton.setEnabled(false);
        });
        add(startButton);

        gameGrid = new GameGrid(10, boardSize, boardSize, new Point(sidePanelSize, 0), gameState);

        // add a ShipSquare for each cell that has a ship (so we see our ship on the grid)
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                if (gameState.getYourBoardIndex(row, col) != Model.ShipType.EMPTY) {
                    ShipSquare sq = new ShipSquare(gameGrid, gameState);
                    sq.setCellSquare(row, col);
                    int[] pos = gameGrid.getCellPosition(new int[] { row, col });
                    sq.setBounds(sidePanelSize + pos[0], pos[1], gameGrid.getCellWidth(), gameGrid.getCellHeight());
                    add(sq);
                }
            }
        }

        gameGrid.setBounds(sidePanelSize, 0, boardSize, boardSize);
        add(gameGrid);

        // mouse release on grid: either shoot (if game started and our turn) or do nothing
        gameGrid.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int[] idx = gameGrid.getCellInside(e.getPoint());
                if (idx[0] < 0) return;
                if (!gameState.getCanMoveShips()) {
                    if (gameState.isPlayersTurn() && gameState.getTheirBoardIndex(idx[0], idx[1]) == Model.CellStatus.DONTKNOW) {
                        gameState.shoot(idx[0], idx[1]);
                        renderView();
                    }
                }
            }
        });

        // draw shot markers (hit/miss) on the grid — add to gameGrid so coordinates match
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                Model.CellStatus status = gameState.isPlayersTurn()
                    ? gameState.getTheirBoardIndex(row, col)
                    : gameState.getHitIndex(row, col);
                if (status != Model.CellStatus.DONTKNOW) {
                    Shot shot = new Shot(status, new int[] { row, col }, gameGrid);
                    gameGrid.add(shot);
                }
            }
        }

        scoreLabel = new JLabel("score: " + gameState.getScore());
        scoreLabel.setBounds(20, 70, 200, 30);
        scoreLabel.setOpaque(true);
        scoreLabel.setBackground(Color.WHITE);
        add(scoreLabel);

        logBox = new JLabel(gameState.getLog());
        logBox.setBounds(20, 110, 200, 30);
        logBox.setOpaque(true);
        logBox.setBackground(Color.WHITE);
        add(logBox);

        setVisible(true);
        revalidate();
        repaint();
    }
}
