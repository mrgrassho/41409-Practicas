package Practico1.ServidorSimple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTCP {
	int port;
	
	public ServerTCP (int i) {
		this.port = i;
		this.startServer();
		
	}
	private void startServer() {
		// TODO Auto-generated method stub
		try {
			ServerSocket ss = new ServerSocket (this.port);
			System.out.println(" Server started on port: "+this.port);
			
		
		
			while (true) {
				
				Socket client = ss.accept();
				System.out.println("Client connected from port: "+client.getInetAddress().getCanonicalHostName()+" : "+client.getPort());
				
				ThreadServer ts = new ThreadServer(client);
				Thread tsThread = new Thread (ts);
				tsThread.start();
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(" port in use");
		} 
	}
	public static void main(String[] args) {
		ServerTCP stcp = new ServerTCP(9000);

	}

}
