import java.io.*;
import java.net.Socket;

/**
 * entry point for a player. we connect to the server, read our player number
 * (0 or 1), then build the model and view. the model uses the network
 * streams to send shots and receive the opponent's shots. we start the
 * "wait for opponent" thread in the model so when it's not our turn we're
 * listening for incoming shots.
 */
class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private View gameView;
    private Model gameState;

    public Client(String serverIP, int port) {
        try {
            socket = new Socket(serverIP, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            int[] playerNumberContainer = (int[]) in.readObject();

            gameState = new Model(10, playerNumberContainer[0], out, in);
            gameView = new View(gameState, out);
            gameState.waitForOpponent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String serverIP = (args.length > 0) ? args[0] : "localhost";
        new Client(serverIP, 12345);
    }
}
