package server;

import model.Question;

import java.util.ArrayList;
import java.util.List;

//Represents an active game session with players and questions
public class GameSession {
    public enum GameState {
        WAITING,
        IN_PROGRESS,
        COMPLETED
    }

    private final int pin;
    private final String topic;
    private final List<Question> questions;
    private final List<ClientHandler> players;
    private ClientHandler host;
    private GameState state;

    public GameSession(int pin, String topic, List<Question> questions) {
        this.pin = pin;
        this.topic = topic;
        this.questions = questions;
        this.players = new ArrayList<>();
        this.state = GameState.WAITING;
    }

    public int getPin() {
        return pin;
    }

    public String getTopic() {
        return topic;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public List<ClientHandler> getPlayers() {
        return players;
    }

    public void addPlayer(ClientHandler player) {
        players.add(player);
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public void setHost(ClientHandler host) {
        this.host = host;
    }

    public ClientHandler getHost() {
        return host;
    }
}
