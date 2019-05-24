package punto2.synch.sin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ExtraccionServer {
	int port;
	private final Gson gson = new Gson();
	private final Logger log = LoggerFactory.getLogger(ExtraccionServer.class);
	private static String FILENAME = "extras/saldo.txt"; 
	
	public ExtraccionServer(int i) {
		this.port = i;
		this.startServer();
	}
	
	private void startServer() {
		try {
			ServerSocket ss = new ServerSocket (this.port);
			System.out.println(" Server started on port: "+this.port);
			while (true) {
				Socket client = ss.accept();
				ExtraccionThread ts = new ExtraccionThread(FILENAME, log, gson, client);
				Thread tsThread = new Thread(ts);
				tsThread.start();
			}
		} catch (IOException e) {
			System.out.println(" port in use");
		} 
	}
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName=ExtraccionServer.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		ExtraccionServer stcp = new ExtraccionServer(9000);
	}
}
