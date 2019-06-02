package punto1.master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class ServerMain {
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	private static final String PEERS_INFO="src/main/java/punto1/master/resources/peers-info.json";
	private static final String FILES_INFO="src/main/java/punto1/master/resources/files-info.json";
	private Gson gson;
	private int port;
	
	public ServerMain(int port) {
		super();
		this.port = port;
		this.gson = new Gson();
	}

	public void startServer() {
		try {
			ServerSocket ss = new ServerSocket (this.port);
			log.info("Server started on " + this.port);
			boolean flag = true;
			while (flag) {
				Socket client = ss.accept();
				log.info("Client connected from " + client.getInetAddress().getCanonicalHostName()+":"+client.getPort());
				ServerThread ts = new ServerThread(log, gson, client, PEERS_INFO, FILES_INFO);
				Thread tsThread = new Thread(ts);
				tsThread.start();
			}
			ss.close();
		} catch (IOException e) {
			log.info("Port in use!");
		}
	}

	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		ServerMain ss = new ServerMain(8090);
		ss.startServer();
	}
}
