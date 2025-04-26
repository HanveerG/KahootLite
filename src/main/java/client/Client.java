package client;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // connect to server
    public Client(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // send a command like JOIN, START, etc.
    public void sendCommand(String command) {
        out.println(command);
    }

    // send data (like username, pin, answer)
    public void sendLine(String line) {
        out.println(line);
    }

    // get a message from server
    public String readLine() throws IOException {
        return in.readLine();
    }

    // close everything
    public void close() throws IOException {
        socket.close();
    }
}
