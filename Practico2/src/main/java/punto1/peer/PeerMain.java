package punto1.peer;

import java.util.ArrayList;
import java.util.Random;
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
			if (peerPort < 8000) System.out.println("Error ingrese puerto mayor a 8000");
		} while(peerPort < 8000);
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
