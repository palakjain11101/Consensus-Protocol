import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPLoggerClient {
	
	private final int loggerServerPort;
	private final int processId;
	private final int timeout;
	int count = 0;

	public UDPLoggerClient(int loggerServerPort, int processId, int timeout) {
		this.loggerServerPort = loggerServerPort;
		this.processId = processId;
		this.timeout = timeout;
	}
	
	public int getLoggerServerPort() {
		return loggerServerPort;
	}

	public int getProcessId() {
		return processId;
	}
	
	public int getTimeout() {
		return timeout;
	}

	public void logToServer(String message) throws IOException {
		try {
			InetAddress address = InetAddress.getLocalHost();
			DatagramSocket socket = new DatagramSocket();
			System.out.println("Message received" + message);
			while (true) {
				DatagramPacket req = new DatagramPacket(message.getBytes(), message.getBytes().length, address, loggerServerPort);
				socket.send(req);

				byte[] buffer = new byte[256];
				DatagramPacket res = new DatagramPacket(buffer, buffer.length);
				try{
					socket.receive(res);
					String ack = new String(buffer, 0, res.getLength());
					System.out.println("Ack: " + ack);
					socket.close();
					break;
				}
				catch(InterruptedIOException e){
					count ++;
					if(count == 3){
						throw new IOException("Timeout error");
					}
				}

			}
		} catch (Exception e) {
			System.out.println("error " + e);
		}
		
	}
}
