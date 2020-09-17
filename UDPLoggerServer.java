import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

public class UDPLoggerServer{

    public static void main (String[] args){
        if(args.length != 1){
            System.out.println("Usage: UDPLoggerServer <port>");
            return;
        }
        try{
            PrintStream writer = new PrintStream("logger_server_" + System.currentTimeMillis() + ".log");
            new UDPLoggerServer().listen(Integer.parseInt(args[0]), writer);
        }
        catch(IOException e){
        }
    }

    void listen(int port, PrintStream writer){
        try{
            DatagramSocket socket = new DatagramSocket(port);
            byte[] buffer = new byte[512];
            DatagramPacket req = new DatagramPacket(buffer, buffer.length);
            while(true){
                socket.receive(req);
                System.out.println("Message received" + req);
                acknowledge(req, socket);
                String msg = message(req, buffer);
                writer.println(msg);
            }

        } catch (IOException e){
            System.out.println("error");
        }
    }

    String message(DatagramPacket packet, byte[] buffer){
        String msg = new String(buffer, 0, packet.getLength());
        String[] messageParts = msg.trim().split(" ", 2);
        String result = messageParts[0] + " " + System.currentTimeMillis() + " " + messageParts[1];
        System.out.println(result);
        return result;
    }

    void acknowledge(DatagramPacket req, DatagramSocket socket){
        try{
            InetAddress clientAddress = req.getAddress();
            int clientPort = req.getPort();
            String ack = "ACK";
            DatagramPacket res = new DatagramPacket(ack.getBytes(), ack.getBytes().length, clientAddress, clientPort);
            socket.send(res);
        } catch (IOException e){

        }
    }

}