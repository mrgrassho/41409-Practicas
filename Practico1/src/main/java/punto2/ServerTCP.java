package Punto2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;


public class ServerTCP {
	int port;
	int sleep;
	public ServerTCP (int i, int s) {
		this.port = i;
		this.sleep= s;
		this.startServer();
		
	}
	private void startServer() {
		// TODO Auto-generated method stub
		try {
			ServerSocket ss = new ServerSocket (this.port);
			System.out.println("---- Servidor iniciado en el puerto "+this.port+ ". ----");
				
		while (true) {
			Socket client = ss.accept();
			System.out.println("Se ha conectado un cliente> "+client.getInetAddress().getCanonicalHostName()+" : "+client.getPort());
			ThreadServer ts = new ThreadServer(client, this.sleep);
			Thread tsThread = new Thread (ts);
			tsThread.start();
		}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(" ERROR> port in use");
		} 
	}
	
	public static void main(String[] args) {
		System.out.println("Indique puerto a escuchar>");
		Scanner scanner = new Scanner(System.in);
		int port =  scanner.nextInt();
		
		System.out.println("Indique tiempo de sleep <seg> (0 si no desea)>");
		scanner = new Scanner(System.in);
		int sleep =  scanner.nextInt()*1000;
		
		ServerTCP server = new ServerTCP(port, sleep);
	}

}
