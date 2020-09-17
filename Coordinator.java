import java.net.ServerSocket;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;

public class Coordinator {

    CoordinatorLogger coordLogger;
    CoordinatorLogger coordlogger;
    Map<Integer, PrintWriter> participantConnectionsMap;
    ServerSocket ss = null;
    List<String> options = new ArrayList<>();
    List<Integer> participantsList = new ArrayList<>();
    int count;
    int port;
    int lport;
    int parts;
    int timeout;


    private Coordinator(String[] args) {
        int Port = Integer.parseInt(args[0]);
        int Lport = Integer.parseInt(args[1]);
        int Parts = Integer.parseInt(args[2]);
        int Timeout = Integer.parseInt(args[3]);

        this.port = Port;
        this.lport = Lport;
        this.parts = Parts;
        this.timeout = Timeout;

        options.addAll(Arrays.asList(args).subList(4, args.length));
        System.out.println("Hello");

        try {
            CoordinatorLogger.initLogger(lport, port, timeout);
            coordLogger = CoordinatorLogger.getLogger();
            ss = new ServerSocket(port);
            participantConnectionsMap = Collections.synchronizedMap(new HashMap<>(parts));
            coordLogger.startedListening(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void establishConnections() throws IOException {
        Socket conn;
        try {
            while (count <= parts) {
                conn = ss.accept();
                coordLogger.connectionAccepted(conn.getPort());
                Thread thread = new ServerThread(conn);
                thread.start();
                thread.sleep(500);
            }
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    private synchronized  boolean register(Integer name, PrintWriter out) {
        if (count >= parts)
            return false;
        if (participantConnectionsMap.containsKey(name)) {
            System.err.println("Client already joined.");
            return false;
        }
        try {
            participantConnectionsMap.put(name, out);
        } catch (NullPointerException e) {
            return false;
        }
        count++;
        return true;
    }

    public synchronized void sendDetails(int participantPort, PrintWriter _clientOut) {
        StringBuilder message = new StringBuilder("DETAILS");
        List<Integer> otherPorts = new ArrayList<>((participantConnectionsMap.keySet()));
        for (Integer port : otherPorts) {
            if (port != participantPort) {
                String newPort = Integer.toString(port);
                message.append(" ").append(newPort);
            }
        }
        _clientOut.println(message);
        _clientOut.flush();
        coordLogger.detailsSent(participantPort, participantsList);
        coordLogger.messageSent(participantPort, message.toString());

    }

    private void sendOptions(int participantPort, PrintWriter _clientOut) {
        StringBuilder message = new StringBuilder("VOTE OPTIONS");
        for (String option : options){
            message.append(" " + option);
        }
        System.out.println(message);
        _clientOut.println(message);
        _clientOut.flush();
        coordLogger.voteOptionsSent(participantPort, options);
        coordLogger.messageSent(participantPort, message.toString());

    }


    public static void main(String[] args) {
        try {
            Coordinator coordinator = new Coordinator(args);
            coordinator.establishConnections();
            coordinator.sendDetailsVoteOptions();
        } catch (IOException e) {
            System.err.println("COORD: Unable to connect to participants");
            e.printStackTrace();
        }
    }

    private void sendDetailsVoteOptions() {
        if (count >= parts) {
            for (Integer part : participantConnectionsMap.keySet()) {
                this.sendDetails(part, participantConnectionsMap.get(part));
            }
            for (Integer part : participantConnectionsMap.keySet()) {
                this.sendOptions(part, participantConnectionsMap.get(part));
            }
        }
    }



    private class ServerThread extends Thread {
        private Socket _clientSocket;
        private BufferedReader _clientIn;
        private PrintWriter _clientOut;
        private int participantPort;

        ServerThread(Socket client) throws IOException {
            _clientSocket = client;
            _clientIn = new BufferedReader(new InputStreamReader(client.getInputStream()));
            _clientOut = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
        }

        public void run() {
            try {
                _clientSocket.setSoTimeout(timeout);

                String receivedMessage;
                String[] messageParts;

                receivedMessage = _clientIn.readLine();
                messageParts = receivedMessage.split(" ");
                String g = messageParts[0];

                if (!(g.equals("JOIN"))) {
                    _clientSocket.close();
                    return;
                }

                participantPort = Integer.valueOf(messageParts[1]);
                coordLogger.joinReceived(participantPort);
                coordLogger.messageReceived(participantPort, receivedMessage);
                _clientSocket.setSoTimeout(0);

                if (!(register (participantPort, (_clientOut)))) {
                    _clientSocket.close();
                    return;
                }


                sendDetailsVoteOptions();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                receivedMessage = _clientIn.readLine();

                messageParts = receivedMessage.split(" ");

                String a = messageParts[0];

                if (a.equals("OUTCOME")) {
                    System.out.println("Outcome received");
                    coordLogger.outcomeReceived(participantPort, messageParts[1]);
                    coordLogger.messageReceived(participantPort, receivedMessage);
                }

            } catch (SocketTimeoutException e) {
                coordlogger.participantCrashed(this.participantPort);
                try {
                    _clientSocket.close();
                } catch (IOException exception) {

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }



}

