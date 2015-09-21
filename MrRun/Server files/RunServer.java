
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class RunServer {

	public static final int PORT = 2100;
	public static DatagramSocket socket;
	public static ArrayList<Thread> threadList = new ArrayList<Thread>();
	public static InetAddress localAddress = null;

	public static void main(String[] args) {
	
		
		try {
		
			// Get local address info
			try {
				localAddress = InetAddress.getLocalHost();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// Set up socket
			socket = new DatagramSocket(PORT);			
			System.out.println("Listening on port " + PORT);		
			
			// Set up sending packet
			byte rxBuf[] = new byte [1500];
			DatagramPacket rxPacket = new DatagramPacket(rxBuf, rxBuf.length);

			
			// Waits for packet and prints packet information
			while (true) {
				System.out.println("Entering accept loop.");
				
				RunServer.socket.receive(rxPacket);
				
				WorkerThread w = new WorkerThread(rxPacket);
			
				synchronized(RunServer.threadList) {
					RunServer.threadList.add(w);
				}
				w.start();
				
			}
		} catch (IOException e) {
			System.err.println("Error setting up server socket: " + e.getMessage());
		}
	}
}
				

