package server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Question;

//Main server logic, listens for clients and manages game sessions
public class GameServer {
    public static final int PORT = 5000;

    // All active sessions are stored by their PIN
    private static Map<Integer, GameSession> sessions = new HashMap<>();

    // All topics with preloaded question lists
    private static Map<String, List<Question>> topics = new HashMap<>();

    public static void main(String[] args) {
        preloadTopics(); // load questions from CSVs

        System.out.println("[Server] Game server running on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket client = serverSocket.accept();
                ClientHandler handler = new ClientHandler(client);
                new Thread(handler).start(); // handle each client in its own thread
            }
        } catch (IOException e) {
            System.out.println("[Server] Error: " + e.getMessage());
        }
    }

    // load CSVs for each topic into memory
    private static void preloadTopics() {
        String[] topicNames = {"history", "geography", "math"};

        for (String topic : topicNames) {
            try {
                List<Question> list = loadQuestionsFromCSV("questions/" + topic + ".csv");
                topics.put(topic, list);
                System.out.println("[Server] Loaded: " + topic);
            } catch (IOException e) {
                System.out.println("[Server] Failed to load: " + topic);
            }
        }
    }

    private static List<Question> loadQuestionsFromCSV(String path) throws IOException {
        List<Question> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(path));

        reader.readLine(); // skip header

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",", -1);
            if (parts.length < 6) continue;

            String prompt = parts[0];
            List<String> options = List.of(parts[1], parts[2], parts[3], parts[4]);
            int correct = Integer.parseInt(parts[5].trim()) - 1;

            list.add(new Question(prompt, options, correct));
        }

        reader.close();
        return list;
    }

    // host creates a session
    public static synchronized int createGameSession(String topic, ClientHandler host, String name) {
        int pin = generatePIN();
        List<Question> qList = topics.get(topic);
        if (qList == null) return -1;

        GameSession session = new GameSession(pin, topic, qList);
        session.setHost(host);
        session.addPlayer(host);

        sessions.put(pin, session);

        host.setHost(true);
        host.setUsername(name);
        host.setSession(session);

        return pin;
    }

    // player joins existing session
    public static synchronized boolean joinSession(int pin, ClientHandler player, String name) {
        GameSession session = sessions.get(pin);
        if (session == null || session.getState() != GameSession.GameState.WAITING) {
            return false;
        }

        session.addPlayer(player);
        player.setUsername(name);
        player.setSession(session);
        return true;
    }

    // host starts the game
    public static synchronized void startSession(GameSession session) {
        if (session.getState() != GameSession.GameState.WAITING) return;

        session.setState(GameSession.GameState.IN_PROGRESS);
        sendNextQuestion(session, 0);
    }

    // sends one question to all players
    private static void sendNextQuestion(GameSession session, int index) {
        List<Question> list = session.getQuestions();
        if (index >= list.size()) {
            sendScores(session);
            session.setState(GameSession.GameState.COMPLETED);
            return;
        }

        Question q = list.get(index);
        for (ClientHandler p : session.getPlayers()) {
            p.sendLine("QUESTION");
            p.sendLine(q.getPrompt());
            for (String opt : q.getOptions()) {
                p.sendLine(opt);
            }
        }

        new Thread(() -> {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ignored) {}
            sendNextQuestion(session, index + 1);
        }).start();
    }

    private static void sendScores(GameSession session) {
        List<ClientHandler> players = session.getPlayers();

        // sort by score (highest first)
        players.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        for (ClientHandler player : players) {
            player.sendLine("SCORES");

            for (int i = 0; i < players.size(); i++) {
                ClientHandler p = players.get(i);
                String line = (i + 1) + ". " + p.getUsername() + ": " + p.getScore();
                player.sendLine(line);
            }

            player.sendLine("END_SCORES");
        }
    }

    private static int generatePIN() {
        Random rand = new Random();
        int pin;
        do {
            pin = 1000 + rand.nextInt(9000);
        } while (sessions.containsKey(pin));
        return pin;
    }

    public static GameSession getSession(int pin) {
        return sessions.get(pin);
    }
}
