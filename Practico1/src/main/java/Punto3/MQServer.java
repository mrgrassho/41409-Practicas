package Punto3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MQServer {
	int port;
	private ArrayList<String> messageQueue;
	
	public MQServer (int i) {
		this.port = i;
		this.messageQueue =  new ArrayList<String>();
		this.startServer();
	}
	
	private void startServer() {
		
		try {
			ServerSocket ss = new ServerSocket (this.port);
			System.out.println("Server started on port: " + this.port);
			while (true) {
				Socket client = ss.accept();
				System.out.println("Client connected from port: "+client.getInetAddress().getCanonicalHostName()+" : "+client.getPort());
				MQThreadServer ts = new MQThreadServer(client,  this.messageQueue);
				Thread tsThread = new Thread (ts);
				tsThread.start();
			}
		} catch (IOException e) {
			System.out.println("Port in use");
		}
	}
	public static void main(String[] args) {
		MQServer stcp = new MQServer(9000);

	}

}
