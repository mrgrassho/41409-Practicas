package punto2.syn.without;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class DepositoServer {
	int port;
	private final Gson gson = new Gson();
	private final Logger log = LoggerFactory.getLogger(DepositoServer.class);
	private static String FILENAME = "extras/saldo.txt";

	public DepositoServer(int i) {
		this.port = i;
		this.startServer();
	}

	private void startServer() {
		try {
			ServerSocket ss = new ServerSocket (this.port);
			System.out.println(" Server started on port: "+this.port);
			while (true) {
				Socket client = ss.accept();
				DepositoThread ts = new DepositoThread(FILENAME, log, gson, client);
				Thread tsThread = new Thread(ts);
				tsThread.start();
			}
		} catch (IOException e) {
			System.out.println(" port in use");
		}
	}
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName=DepositoServer.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		DepositoServer stcp = new DepositoServer(9001);
	}

}
