
import java.io.IOException;
import java.net.DatagramPacket;


public class WorkerThread extends Thread {

	private DatagramPacket rxPacket;


	public WorkerThread(DatagramPacket rxPacket) {
		// store the socket in a member variable
		this.rxPacket = rxPacket;
		System.out.println("Worker thread created (thread id=" + this.getId() + ")");
	}

	@Override
	public void run() {

		System.out.println("Worker thread started running (thread id=" + this.getId() + ")");


		try {

			byte rxBuf[] = new byte [1500];

			String sentence = new String(rxPacket.getData(), 0, rxPacket.getLength());

			System.out.println("Received: " + sentence);
			System.out.println("from address: " + rxPacket.getAddress());
			System.out.println("from port: " + rxPacket.getPort());
			System.out.println("My adress is: " + RunServer.localAddress);

			// reset packet length
			rxPacket.setLength(rxBuf.length);

			// Set up packet
			System.out.println("Echoing packet");
			try {
				RunServer.socket.send(rxPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} finally {
			RunServer.socket.close();;
		}			


		System.out.println("Worker thread terminated (thread id=" + this.getId() + ")");

		// Remove this thread from the threadList since it is finished
		synchronized (RunServer.threadList) {
			RunServer.threadList.remove(this);
		}

	}

	/**
	 * readInput -- 
	 * This method reads the input from the user and performs the
	 * necessary actions based on the input
	 * Precondition: the input is either in the form of
	 * int int int OR
	 * SHUTDOWN
	 * @param line -- the input line from the user
	 * @return reply  -- the output message to the user
	 */

	public String readInput(String line) {
		String reply;

		if (line.contains("SHUTDOWN")) {

			if (RunServer.socket.getInetAddress().isLoopbackAddress()) { 
				shutdown();
				reply = "";
			}
			else {
				reply = "Invalid server";
			}

		}

		else {
			String[] inputs = line.split(" ");
			int amount = Integer.parseInt(inputs[0]);
			int srcAccountNum = Integer.parseInt(inputs[1]);
			int destAccountNum = Integer.parseInt(inputs[2]);

			reply = amount + " transfered from account " + srcAccountNum + " to account " + destAccountNum;

		}
		return reply; 
	}

	public void shutdown() {
		RunServer.socket.close();

		synchronized (RunServer.threadList) {
			for (Thread w : RunServer.threadList) {
				try {
					if (w!=this)
						w.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}


