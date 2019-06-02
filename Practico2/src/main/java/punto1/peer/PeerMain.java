package punto1.peer;

import java.util.Random;

public class PeerMain {
	
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = PeerServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		Random r = new Random();
		int peerPort = r.nextInt(20000)+9000;
		PeerServerMain ss = new PeerServerMain(peerPort);
		Thread tServer = new Thread(ss);
		PeerClient pc = new PeerClient(peerPort, "localhost");
		Thread tClient = new Thread(pc);
		tServer.start();
		tClient.start();
	}

}
