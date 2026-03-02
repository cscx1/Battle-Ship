import java.awt.BorderLayout;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 * the server doesn't run the game logic — it just waits for two clients and
 * relays messages between them. each client sends objects (e.g. shot coordinates)
 * to the server, and the server forwards them to the other client. that way
 * the two players never talk directly; the server is in the middle.
 */
public class Server extends JFrame {
    private JTextArea displayArea;
    private ServerSocket server;
    private Socket[] clients = new Socket[2];
    private ObjectOutputStream[] outputs = new ObjectOutputStream[2];
    private ObjectInputStream[] inputs = new ObjectInputStream[2];

    public Server() {
        super("Battleship server");
        displayArea = new JTextArea();
        add(new JScrollPane(displayArea), BorderLayout.CENTER);
        setSize(300, 150);
        setVisible(true);
    }

    /**
     * listen on port 12345, accept exactly two clients, send each one their
     * player index (0 or 1). then start a thread per client that reads objects
     * from that client and writes them to the other (relay).
     */
    public void runServer() {
        try {
            server = new ServerSocket(12345, 2);
            displayMessage("Server started. Waiting for two players...\n");

            for (int i = 0; i < 2; i++) {
                clients[i] = server.accept();
                displayMessage("Player " + (i + 1) + " connected from: " + clients[i].getInetAddress().getHostName() + "\n");
                outputs[i] = new ObjectOutputStream(clients[i].getOutputStream());
                outputs[i].flush();
                inputs[i] = new ObjectInputStream(clients[i].getInputStream());
                int[] clientIndex = { i };
                outputs[i].writeObject(clientIndex);
                outputs[i].flush();
            }

            for (int i = 0; i < 2; i++) {
                final int clientIndex = i;
                new Thread(() -> {
                    try {
                        while (true) {
                            Object message = inputs[clientIndex].readObject();
                            int opponentIndex = (clientIndex + 1) % 2;
                            displayMessage("Player " + (clientIndex + 1) + " sent a message to Player " + (opponentIndex + 1) + "\n");
                            outputs[opponentIndex].writeObject(message);
                            outputs[opponentIndex].flush();
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        displayMessage("Error handling client " + (clientIndex + 1) + ": " + e.getMessage() + "\n");
                    }
                }).start();
            }
        } catch (IOException e) {
            displayMessage("Server error: " + e.getMessage() + "\n");
        }
    }

    private void displayMessage(final String message) {
        SwingUtilities.invokeLater(() -> displayArea.append(message));
    }

    public static void main(String[] args) {
        Server serverApp = new Server();
        serverApp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverApp.runServer();
    }
}
