package server;

import model.Question;

import java.io.*;
import java.net.Socket;

//Handles communication between client and server
public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String username;
    private boolean isHost = false;
    private int score = 0;

    private GameSession session; // each player joins one session

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public void incrementScore() {
        score++;
    }

    public void sendLine(String line) {
        out.println(line);
    }

    //Listens for client commands and dispatches them
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                String command = in.readLine();
                if (command == null) break;

                switch (command) {
                    case "CREATE_GAME" -> handleCreate();
                    case "JOIN_GAME" -> handleJoin();
                    case "START_GAME" -> handleStart();
                    case "ANSWER" -> handleAnswer();
                    default -> sendLine("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + username);
        }
    }
    //handlers for each command from client
    private void handleCreate() throws IOException {
        String name = in.readLine();
        String topic = in.readLine();

        int pin = GameServer.createGameSession(topic, this, name);

        if (pin != -1) {
            sendLine("CREATE_OK");
            sendLine(String.valueOf(pin));
        } else {
            sendLine("CREATE_FAIL");
        }
    }

    private void handleJoin() throws IOException {
        int pin = Integer.parseInt(in.readLine());
        String name = in.readLine();

        boolean success = GameServer.joinSession(pin, this, name);
        if (success) {
            sendLine("JOIN_OK");
        } else {
            sendLine("JOIN_FAIL");
        }
    }

    private void handleStart() {
        if (isHost && session != null) {
            GameServer.startSession(session);
        }
    }

    private void handleAnswer() throws IOException {
        String questionID = in.readLine();
        int selected = Integer.parseInt(in.readLine());

        if (session == null) return;

        for (Question q : session.getQuestions()) {
            if (String.valueOf(q.getPrompt().hashCode()).equals(questionID)) {
                if (selected == q.getCorrectIndex()) {
                    incrementScore();
                }
                break;
            }
        }
    }
}
