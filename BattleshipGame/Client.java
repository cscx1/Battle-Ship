import javax.swing.*;
import java.io.*;
import java.net.Socket;

/**
 * Connects to the server, builds model + UI on the Swing thread, then listens
 * for the opponent's shots in the model.
 */
class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Model gameState;

    public Client(String serverIP, int port) {
        SwingUtilities.invokeLater(() -> {
            try {
                socket = new Socket(serverIP, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                int[] playerNumberContainer = (int[]) in.readObject();

                gameState = new Model(10, playerNumberContainer[0], out, in);
                new GameScreen(gameState);
                gameState.waitForOpponent();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not connect: " + e.getMessage(),
                        "Battleship",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static void main(String[] args) {
        String serverIP = (args.length > 0) ? args[0] : "localhost";
        new Client(serverIP, 12345);
    }
}
