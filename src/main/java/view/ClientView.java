package view;

import client.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class ClientView extends JFrame {
    private Client client;
    private boolean isHost;
    private String username;
    private int gamePin;
    private int score = 0;
    private int timeLimit = 5;
    private boolean questionInProgress = false;  // Flag to track question state

    public ClientView() {
        setTitle("Kahoot Game");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            client = new Client("localhost", 5000); // connect to server
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Can't connect to server.");
            System.exit(1);
        }

        askCreateOrJoin(); // pick what to do
    }

    private void askCreateOrJoin() {
        String[] options = {"Create Game", "Join Game"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Do you want to create a game or join a game?",
                "Choose Option",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            startCreateFlow();
        } else if (choice == 1) {
            startJoinFlow();
        } else {
            System.exit(0);
        }
    }

    private void startCreateFlow() {
        username = JOptionPane.showInputDialog(this, "Enter your username:");

        if (username == null || username.trim().isEmpty()) System.exit(0);

        String[] topics = {"history", "geography", "math"};
        String topic = (String) JOptionPane.showInputDialog(this, "Select a topic:", "Choose Topic", JOptionPane.PLAIN_MESSAGE, null, topics, topics[0]);

        if (topic == null || topic.isEmpty()) System.exit(0);

        client.sendCommand("CREATE_GAME");
        client.sendLine(username);
        client.sendLine(topic);

        try {
            String response = client.readLine();
            if (response.equals("CREATE_OK")) {
                isHost = true;
                gamePin = Integer.parseInt(client.readLine());
                showHostLobby(topic);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create game.");
                System.exit(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error creating game.");
            System.exit(0);
        }

        String[] timeOptions = {"5", "10", "15"};
        String timeChoice = (String) JOptionPane.showInputDialog(
                this,
                "Set time (seconds) for each question:",
                "Choose Time Limit",
                JOptionPane.PLAIN_MESSAGE,
                null,
                timeOptions,
                timeOptions[0]
        );

        if (timeChoice == null || timeChoice.isEmpty()) System.exit(0);
        timeLimit = Integer.parseInt(timeChoice);

        client.sendLine(timeChoice);
    }

    private void startJoinFlow() {
        username = JOptionPane.showInputDialog(this, "Enter your username:");
        if (username == null || username.trim().isEmpty()) System.exit(0);

        String pinInput = JOptionPane.showInputDialog(this, "Enter Game PIN:");
        if (pinInput == null || pinInput.trim().isEmpty()) System.exit(0);

        client.sendCommand("JOIN_GAME");
        client.sendLine(pinInput);
        client.sendLine(username);

        try {
            String response = client.readLine();
            if (response.equals("JOIN_OK")) {
                isHost = false;
                gamePin = Integer.parseInt(pinInput);
                showWaitingScreen();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to join game.");
                System.exit(0);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error joining game.");
            System.exit(0);
        }
    }

    private void showHostLobby(String topic) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Game PIN: " + gamePin + " | Topic: " + topic, SwingConstants.CENTER);
        JButton startButton = new JButton("Start Game");

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                client.sendCommand("START_GAME");
                waitForGameStart();
            }
        });

        panel.add(label, BorderLayout.NORTH);
        panel.add(startButton, BorderLayout.CENTER);

        setContentPane(panel);
        setVisible(true);
        revalidate();
        repaint();
    }

    private void showWaitingScreen() {
        JLabel label = new JLabel("Waiting for host to start... (PIN: " + gamePin + ")", SwingConstants.CENTER);
        setContentPane(label);
        setVisible(true);
        revalidate();
        repaint();
        waitForGameStart();
    }

    private void waitForGameStart() {
        new Thread(() -> {
            try {
                while (true) {
                    String line = client.readLine();
                    if (line == null) break;

                    if (line.equals("QUESTION")) {
                        String prompt = client.readLine();
                        ArrayList<String> options = new ArrayList<>();
                        for (int i = 0; i < 4; i++) {
                            options.add(client.readLine());
                        }
                        showQuestion(prompt, options);
                    }

                    if (line.equals("SCORES")) {
                        ArrayList<String> scores = new ArrayList<>();
                        while (!(line = client.readLine()).equals("END_SCORES")) {
                            scores.add(line);
                        }
                        showScoreboard(scores);
                        break;
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Game connection lost.");
            }
        }).start();
    }

    private Timer countdown = null; // Declare the countdown timer at the class level to manage it globally

    private void showQuestion(String prompt, ArrayList<String> options) {
        // Prevent showing a new question while one is in progress
        if (questionInProgress) return;
        questionInProgress = true;

        JPanel panel = new JPanel(new BorderLayout());

        JLabel timerLabel = new JLabel("Time left: " + timeLimit, SwingConstants.CENTER);
        panel.add(timerLabel, BorderLayout.NORTH);

        JLabel qLabel = new JLabel("<html><h3>" + prompt + "</h3></html>", SwingConstants.CENTER);
        panel.add(qLabel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        boolean[] answered = {false};
        int[] timeLeft = {timeLimit};

        for (int i = 0; i < options.size(); i++) {
            JButton btn = new JButton(options.get(i));
            int idx = i;

            btn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (answered[0]) return; // Prevent double answers
                    answered[0] = true;
                    if (countdown != null && countdown.isRunning()) {
                        countdown.stop(); // Stop the timer when an answer is selected
                    }
                    submitAnswer(prompt, idx); // Submit the selected answer
                }
            });

            btnPanel.add(btn);
        }

        panel.add(btnPanel, BorderLayout.SOUTH);
        setContentPane(panel);
        setVisible(true);
        revalidate();
        repaint();

        // Stop any running timer before starting a new one
        if (countdown != null && countdown.isRunning()) {
            countdown.stop();
        }

        // Create and start the countdown timer
        countdown = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeLeft[0]--;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        timerLabel.setText("Time left: " + timeLeft[0]);
                    }
                });

                // When time runs out, automatically submit no answer
                if (timeLeft[0] == 0 && !answered[0]) {
                    answered[0] = true;
                    countdown.stop();
                    submitAnswer(prompt, -1);
                }
            }
        });

        countdown.start(); // Start the countdown timer
    }

    private void submitAnswer(String prompt, int index) {
        // Send the answer to the server
        client.sendCommand("ANSWER");
        client.sendLine(String.valueOf(prompt.hashCode()));
        client.sendLine(String.valueOf(index));

        // Display Waiting message
        setContentPane(new JLabel("Waiting for next question...", SwingConstants.CENTER));
        revalidate();
        repaint();

        // Wait for the server to send the next question
        questionInProgress = false;
    }

    private void showScoreboard(ArrayList<String> scores) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Final Scores", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(title, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(scores.size(), 1));
        for (String score : scores) {
            list.add(new JLabel(score, SwingConstants.CENTER));
        }

        panel.add(list, BorderLayout.CENTER);
        setContentPane(panel);
        setVisible(true);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientView());
    }
}
