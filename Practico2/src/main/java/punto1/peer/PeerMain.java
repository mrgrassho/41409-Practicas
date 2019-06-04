package punto1.peer;

import java.util.Scanner;

public class PeerMain {
	
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = SeederServer.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		Scanner sc = new Scanner(System.in);
		System.out.println("Ingrese puerto del SeederServer: ");
		int peerPort;
		do {
			peerPort = sc.nextInt();
			if (peerPort < 1000 || peerPort > 65536) System.out.println("Error ingrese puerto mayor a 1000 o menor a 65536");
		} while(peerPort < 1000 || peerPort > 65535);
		SeederServer ss = new SeederServer(peerPort);
		Thread tServer = new Thread(ss);
		PeerClient pc = new PeerClient(peerPort, "localhost");
		Thread tClient = new Thread(pc);
		tServer.start();
		tClient.start();
		try {
			tClient.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
