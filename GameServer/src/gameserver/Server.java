/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gameserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

/**
 *
 * @author Mohamed Elsayed
 */
public class Server extends Thread {

    ArrayList<Client> clients;
    ServerSocket serverSocket;

    PrintStream ps;
    Scene scene;
    ListView lvClients;
    TextArea txtArea;

    public Server(TextArea txtArea, ListView lvClients) {
        this.txtArea = txtArea;
        this.lvClients = lvClients;

        try {
            serverSocket = new ServerSocket(2020);
            clients = new ArrayList<>();
            start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                clients.add(new Client(clientSocket));
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        lvClients.getItems().add("IP: " + clientSocket.getInetAddress().getHostAddress() + " Port: " + clientSocket.getPort());
                    }
                });

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void closeAll() {
        int size = clients.size();
        Client client;
        for (int i = 0; i < size; i++) {
            try {
                client = clients.get(i);
                ps = new PrintStream(client.socket.getOutputStream());
                ps.println("");

                client.stop();
                client.dis.close();
                client.socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        try {
            this.stop();
            clients.clear();
            serverSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public class Client extends Thread {

        private Socket socket;
        private DataInputStream dis;
        private String name;
        private boolean inGame = false;
        private String inGameWith = null;

        public Client(Socket socket) {
            this.socket = socket;
            name = new String();
            try {
                dis = new DataInputStream(socket.getInputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            start();
        }

        public void run() {
            while (true) {
                String msg = new String();
                try {
                    msg += dis.readLine();

                    while (dis.available() > 0) {
                        msg += dis.readLine();
                    }

                    //txtArea.appendText(msg + "\n");
                    if (!msg.equals(Commands.NONE.toString()) && msg.contains("-")) {
                        List<String> info = new ArrayList<>();
                        info = Arrays.asList(msg.split("-"));

                        if (msg.startsWith(Commands.OPEN.toString())) {
                            // OPEN-Name
                            this.name = info.get(1);
                            notifyAllClients();
                        } else if (msg.startsWith(Commands.CLOSE.toString())) {
                            // CLOSE-Name
                            publishCloseMessage(msg);
                        } else if (msg.startsWith(Commands.REQUEST.toString())) {
                            // REQUEST-Name-SocketDestination
                            sendPlayRequest(msg, info.get(2));
                        } else if (msg.startsWith(Commands.REPLAY.toString())) {
                            // REPLAY-Name-Replay(Y/N)-MyTurn(Y/N)-Source-Destination
                            sendPlayReplay(msg, info.get(5));
                        } else if (msg.startsWith(Commands.INGAME.toString())) {
                            // INGAME-P1Socket-P2Socket
                            inGameMessage(info.get(1), info.get(2));
                        } else if (msg.startsWith(Commands.OUTGAME.toString())) {
                            // OUTGAME-Socket-Name
                            outGameMessage();
                        } else if (msg.startsWith(Commands.PLAY.toString())) {
                            // PLAY-(Location)
                            play(msg);
                        } else if (msg.startsWith(Commands.Withdraw.toString())) {
                            // Withdraw-(Name)
                            withdraw(msg);
                        }
                    } else {
                        break;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            close();
        }

        public void close() {
            try {
                this.socket.close();
                this.dis.close();
                int size = lvClients.getItems().size();
                String address = this.socket.getInetAddress().getHostAddress();
                String port = "" + this.socket.getPort();

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        //System.out.println(address + " "+port);
                        for (int i = 0; i < size; i++) {
                            if (lvClients.getItems().get(i).toString().contains(address) && lvClients.getItems().get(i).toString().contains(port)) {
                                lvClients.getItems().remove(i);
                                break;
                            }
                        }
                    }
                });

                clients.remove(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        private void notifyAllClients() throws IOException {
            boolean anyClientAvailable = false;

            // Send all available users to the new client
            ps = new PrintStream(this.socket.getOutputStream());
            for (Client client : clients) {
                if ((!client.socket.toString().equals(this.socket.toString())) && client.inGame == false) {
                    // Socket-Name
                    ps.println(client.socket.toString() + "-" + client.name.toString());
                }
            }
            
            // Send the new client to all available users
            for (Client client : clients) {
                if (!(client.socket.toString().equals(this.socket.toString()))) {
                    // Socket-Name
                    ps = new PrintStream(client.socket.getOutputStream());
                    ps.println(socket.toString() + "-" + name.toString());
                    anyClientAvailable = true;
                }
            }

            if (!anyClientAvailable) {
                ps.println("Socket" + "-" + Commands.NONE.toString());
            }
        }

        private void publishCloseMessage(String msg) throws IOException {
            for (Client client : clients) {
                if (!client.socket.toString().equals(this.socket)) {
                    ps = new PrintStream(client.socket.getOutputStream());
                    ps.println(msg + "-" + this.socket.toString());
                } else {
                    client.inGame = false;
                    client.inGameWith = null;
                }
            }
        }

        private void sendPlayRequest(String msg, String destination) throws IOException {
            for (Client client : clients) {
                if (client.socket.toString().equals(destination)) {
                    ps = new PrintStream(client.socket.getOutputStream());
                    // REQUEST-Name-SocketDestination-SocketSource
                    ps.println(msg + "-" + this.socket.toString());
                    break;
                }
            }
        }

        private void sendPlayReplay(String msg, String destination) throws IOException {
            for (Client client : clients) {
                if (client.socket.toString().equals(destination)) {
                    ps = new PrintStream(client.socket.getOutputStream());
                    // REPLAY-Name-Replay(Y/N)-MyTurn(Y/N)-Source-Destination
                    ps.println(msg);
                    break;
                }
            }
        }

        private void inGameMessage(String p1Socket, String p2Socket) throws IOException {
            for (Client client : clients) {
                if (client.socket.toString().equals(p1Socket)) {
                    client.inGame = true;
                    client.inGameWith = p2Socket;
                } else if (client.socket.toString().equals(p2Socket)) {
                    client.inGame = true;
                    client.inGameWith = p1Socket;
                } else {
                    ps = new PrintStream(client.socket.getOutputStream());
                    ps.println(Commands.CLOSE.toString() + "-" + client.name + "-" + p1Socket);
                    ps.println(Commands.CLOSE.toString() + "-" + client.name + "-" + p2Socket);
                }
            }
        }

        private void outGameMessage() throws IOException {

            for (Client client : clients) {
                if (!client.socket.toString().equals(this.socket.toString()) && !client.socket.toString().equals(this.inGameWith)) {
                    // This Client become online
                    ps = new PrintStream(client.socket.getOutputStream());
                    ps.println(this.socket.toString() + "-" + this.name);
                    //ps.println(info.get(1).toString() + "-" + info.get(2));
                }
            }

            this.inGame = false;
            this.inGameWith = null;
        }

        private void play(String msg) throws IOException {
            for (Client client : clients) {
                if (client.socket.toString().equals(inGameWith)) {
                    ps = new PrintStream(client.socket.getOutputStream());
                    // PLAY-(Location)
                    ps.println(msg);
                    break;
                }
            }
        }

        private void withdraw(String msg) throws IOException {
            for (Client client : clients) {
                if (client.socket.toString().equals(this.inGameWith)) {
                    ps = new PrintStream(client.socket.getOutputStream());
                    ps.println(msg);
                    this.inGame = false;
                    this.inGameWith = null;
                    break;
                }
            }
        }

    }
}
