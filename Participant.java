import java.io.*;
import java.net.*;
import java.util.*;

public class Participant {

    public static int cport;
    static int lport;
    static int pport;
    static int timeout;
    ParticipantLogger pLogger;
    private static ServerSocket server = null;

    private PrintWriter out;
    private BufferedReader in = null;
    private List<Integer> allOtherParticipants = new ArrayList<>();
    private List<String> voteOptions = new ArrayList<>();
    private String chosenVote; //Randomly chosen vote from this participant
    private List<String> mostSelectedOptions = new ArrayList<>(); //Participant votes with majority of votes (including ties)

    private Set<PeerParticipant> lowerConnections;
    private Set<PeerParticipant> higherConnections;
    private List<Vote> votesToSend;
    private HashSet<PeerParticipant> allConnections;

    private List<Vote> votes = Collections.synchronizedList(new ArrayList<>());


    private Participant(String[] args) {
        int cPort = Integer.parseInt(args[0]);
        int lPort = Integer.parseInt(args[1]);
        int pport = Integer.parseInt(args[2]);
        int timeout = Integer.parseInt(args[3]);
        this.cport = cPort;
        this.lport = lPort;
        this.pport = pport;
        this.timeout = timeout;

        try {
            ParticipantLogger.initLogger(lport, pport, timeout);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            //establishes TCP connection with coordinator
            Socket socket = new Socket("localhost", cport);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pLogger = ParticipantLogger.getLogger();
            pLogger.startedListening();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main (String[]args){
        try {
            Participant participant = new Participant(args);
            participant.sendJoin();
            participant.getDetails();
            participant.getOptions();
            participant.startRounds();
            System.out.println("Details received");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initialise() {
        if (server == null) {
            try {
                server = new ServerSocket(pport);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Set<PeerParticipant> getLowerPeers() {
        if (lowerConnections == null) {
            lowerConnections = new HashSet<>();
        }
        return lowerConnections;
    }

    private Set<PeerParticipant> getHigherPeers() {
        if (higherConnections == null) {
            higherConnections = new HashSet<>();
        }
        return higherConnections;
    }

    public void sendJoin(){
        out.println("JOIN " + pport);
        out.flush();
        pLogger.joinSent(cport);
        pLogger.messageSent(cport,"JOIN " + pport);
    }

    private void getDetails() {
        String details = "";
        try {
            details = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(details);
        pLogger.messageReceived(cport, details);
        String[] messageParts = details.split(" ");
        if (messageParts[0].equals("DETAILS")) {
            for (int i = 1; i < messageParts.length; i++) {
                allOtherParticipants.add(Integer.parseInt(messageParts[i]));
            }
            for (int port : allOtherParticipants) {
                this.establishConnection(port);
            }
            allConnections = new HashSet<>(getLowerPeers());
            allConnections.addAll(getHigherPeers());
            System.out.println(pport + ": Other participants: " + allOtherParticipants.toString());
            pLogger.detailsReceived(allOtherParticipants);
        }
    }

    public void getOptions() {
        String details = "";
        try {
            details = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(details);
        String[] messageParts = details.split(" ");
        if (messageParts[0].equals("VOTE")) {
            if (messageParts[1].equals("OPTIONS")) {
                for (int i = 2; i < messageParts.length; i++) {
                    voteOptions.add(messageParts[i]);
                }
                System.out.println(pport + ": Vote options: " + voteOptions);
                pLogger.voteOptionsReceived(voteOptions);
                Collections.shuffle(voteOptions);
                chosenVote = voteOptions.get(1);
                System.out.println("Chosen voteee" + chosenVote);
                votes.add(new Vote(pport, chosenVote));
                System.out.print("Vote chosen by this participant" + chosenVote);
                System.out.println();
            }
        }
    }



    private void establishConnection(int port) {
        try {
            if (port < pport) {
                initialise();
                Socket peer = server.accept();
                new Thread(new PeerThread(peer)).start();
                peer.setSoTimeout(timeout);
            } else if (port > pport) {
                Socket peer = null;
                while (peer == null) {
                    try {
                        peer = new Socket(InetAddress.getLocalHost(), port);
                        peer.setSoTimeout(timeout);
                    } catch (ConnectException e) {

                    }
                }
                PeerParticipant peerParticipant = new PeerParticipant(peer);
                peerParticipant.setId(port);
                getHigherPeers().add(peerParticipant);
                pLogger.connectionEstablished(peer.getPort());
            } 
        } catch (IOException e) {

        }
        System.out.println("All connections established");
    }

    private void sendVote(PeerParticipant peer, List<Vote> firstVote) {
        String voteMessage = "VOTE";
        for (Vote vote : votes) {
            voteMessage += " " + vote.getParticipantPort() + " " + vote.getVote();
        }
        peer.writer.println(voteMessage);
        System.out.println("Vote sent:" + voteMessage);
        peer.writer.flush();
        pLogger.messageSent(peer.remotePort, voteMessage);
        pLogger.votesSent(peer.remotePort, votes);
    }


    private void sendOutcome() {
        Map<String, Integer> votesList = new HashMap<>();
        String portsString = "";
        for (Vote vote : votes) {
            int count = votesList.getOrDefault(vote.getVote(), 0);
            votesList.put(vote.getVote(), count + 1);
            portsString = portsString + " " + vote.getParticipantPort();
        }
        int maxVotes = (Collections.max(votesList.values()));  //Find the maximum vote for any option

        for (Map.Entry<String, Integer> entry : votesList.entrySet()) {
            if (entry.getValue() == maxVotes) {
                mostSelectedOptions.add(entry.getKey());     //Add any option matching the maximum vote to the list (this will result in either 1 outcome, or tied outcomes)
            }
        }
        if (mostSelectedOptions.size() == 1) {
            System.out.println(pport + ": MAJORITY VOTE FOUND: " + mostSelectedOptions.get(0));
            pLogger.outcomeDecided(mostSelectedOptions.get(0));
            out.println("OUTCOME " + mostSelectedOptions.get(0) + portsString);
            out.flush();

            pLogger.outcomeNotified(mostSelectedOptions.get(0));
            pLogger.messageSent(cport, "OUTCOME " + mostSelectedOptions.get(0) + portsString);

        } else if (mostSelectedOptions.size() > 1) {
            System.out.println(pport + ": TIE BETWEEN: " + mostSelectedOptions.toString());
            TreeMap<String, Integer> treemap = new TreeMap<String, Integer>();
            for (Map.Entry<String, Integer> entry : votesList.entrySet()) {
                treemap.put(entry.getKey(), entry.getValue());
            }
            String key = String.valueOf(treemap.keySet().toArray()[0]);
            String msg = "OUTCOME " + key + portsString;
            out.println(msg);
            pLogger.outcomeNotified(key);
            pLogger.messageSent(cport, msg);
            out.flush();

        } else {
            mostSelectedOptions.clear();
            mostSelectedOptions.addAll(votesList.keySet());
        }

    }





    public void startRounds() {
        
        pLogger.beginRound(1);
        List<Vote> firstVote = Collections.synchronizedList(new ArrayList<>(votes));
        List<Vote> votesList = new ArrayList<>();
        
        for (PeerParticipant peer : getHigherPeers()) {
            sendVote(peer, firstVote);
            pLogger.votesSent(peer.id, firstVote);
        }
        

        for (PeerParticipant peer : getLowerPeers()) {
            try {
                System.out.println("Here1");
                String vote = peer.getReader().readLine();
                pLogger.messageReceived(peer.getRemotePort(), vote);
                String[] messageParts = vote.split(" ");
                if (messageParts[0].equals("VOTE")) {
                    for (int i = 1; i < messageParts.length; i += 2) {
                        Vote thisvote = new Vote(Integer.parseInt(messageParts[i]), messageParts[i + 1]);
                        votesList.add(thisvote);
                        getLowerPeers().stream().filter(p -> p.getPeer().equals(peer.getPeer())).findFirst().get().setId(Integer.parseInt(messageParts[i]));
                    }

                    if(votesToSend == null){
                        votesToSend = receiveVote(votesList);
                    }
                    else{
                        votesToSend.addAll(receiveVote(votesList));
                    }
                    pLogger.votesReceived(peer.getId(), votesList);

                }
            } catch(SocketTimeoutException e){
                if (peer.id > 0) {
                    pLogger.participantCrashed(peer.getId());
                }
                allConnections.remove(peer);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        

        votesList.clear();
        

        for (PeerParticipant peer : lowerConnections) {
            sendVote(peer, firstVote);
            pLogger.votesSent(peer.getId(), firstVote);
        }
        

        for (PeerParticipant peer : higherConnections) {
            try {
                String vote = peer.getReader().readLine();
                pLogger.messageReceived(peer.getRemotePort(), vote);
                String[] messageParts = vote.split(" ");
                if (messageParts[0].equals("VOTE")) {
                    for (int i = 1; i < messageParts.length; i += 2) {
                        Vote thisvote = new Vote(Integer.parseInt(messageParts[i]), messageParts[i + 1]);
                        votesList.add(thisvote);
                    }
                    if(votesToSend == null){
                        votesToSend = receiveVote(votesList);
                    }
                    else{
                        votesToSend.addAll(receiveVote(votesList));
                    }
                    pLogger.votesReceived(peer.getId(), votesList);

                }
            } catch(SocketTimeoutException e){
                if (peer.id > 0) {
                    pLogger.participantCrashed(peer.getId());
                }
                allConnections.remove(peer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        votesList.clear();
        pLogger.endRound(1);


        
        for (int j = 2; j <= allConnections.size() + 1; j++) {
            pLogger.beginRound(j);
            System.out.println("Round: "+ j);
            for (PeerParticipant peer : allConnections) {
                sendVote(peer, votesToSend);
                pLogger.votesSent(peer.getId(), votesToSend);
            }

            votesList.clear();

            for (PeerParticipant peer : allConnections) {
                try {
                    String vote = peer.getReader().readLine();
                    pLogger.messageReceived(peer.remotePort, vote);
                    String[] messageParts = vote.split(" ");
                    if (messageParts[0].equals("VOTE")) {
                        for (int i = 1; i < messageParts.length; i += 2) {
                            Vote thisvote = new Vote(Integer.parseInt(messageParts[i]), messageParts[i + 1]);
                            votesList.add(thisvote);
                        }
                        votesToSend = receiveVote(votesList);
                        pLogger.votesReceived(peer.remotePort, votes);
                    }
                } catch (SocketTimeoutException e) {
                    if (peer.id > 0) {
                        pLogger.participantCrashed(peer.id);
                    }
                    allConnections.remove(peer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            pLogger.endRound(j);
        }
        sendOutcome();
    }


    private List<Vote> receiveVote(List<Vote> voteList){
        List<Vote> newVotes = Collections.synchronizedList(new ArrayList<>());
        for(Vote v: voteList){
            boolean isVote = votes.stream().map(Vote::getParticipantPort).anyMatch(v1->v1 == v.getParticipantPort());
            if(!isVote){
                votes.add(v);
                newVotes.add(v);
            }
        }
        return newVotes;
    }





    private class PeerThread implements Runnable {
        private BufferedReader reader;
        private PrintWriter writer;
        private PeerParticipant peerParticipant;

        public PeerThread(Socket peer) throws IOException {
            peerParticipant = new PeerParticipant(peer);
            reader = peerParticipant.reader;
            writer = peerParticipant.writer;
            getLowerPeers().add(peerParticipant);
        }

        public void run() {
            pLogger.connectionAccepted(peerParticipant.peer.getPort());
            getLowerPeers().add(peerParticipant);
        }
    }




    private class PeerParticipant {
        public int id;
        public int remotePort;
        public Socket peer;
        public BufferedReader buffer;
        public PrintWriter writer;
        BufferedReader reader;

        public PeerParticipant(Socket peer) throws IOException {
            this.id = id;
            this.peer = peer;
            this.remotePort = peer.getPort();
            reader = new BufferedReader(new InputStreamReader(peer.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(peer.getOutputStream()));
        }

        public void setId(int port) {
            this.id = id;
        }

        public int getId(){
            return id;
        }

        public int getRemotePort(){
            return remotePort;
        }

        public void setRemotePort(int remotePort){
            this.remotePort = remotePort;
        }

        public Socket getPeer(){
            return peer;
        }

        public void setPeer(Socket peer){
            this.peer = peer;
        }

        public BufferedReader getReader(){
            return reader;
        }

        public void setReader(BufferedReader reader){
            this.reader = reader;

        }

        public PrintWriter getWriter(){
            return writer;
        }

        public void setWriter(){
            this.writer = writer;
        }

    }




}
