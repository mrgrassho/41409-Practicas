package punto1.peer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class PeerServerMain {
	private final Logger log = LoggerFactory.getLogger(PeerServerMain.class);
	private static final String FILES_INFO="src/main/java/punto1/master/resources/peers-info.xml";
	private Gson gson;
	private int port;
	private Random r;
	private ServerSocket ss;

	public PeerServerMain() {
		super();
		this.port = 8002;
		this.gson = new Gson();
	}

	public void startServer() {
		try {
			ss = new ServerSocket (this.port);
			log.info("PeerServer started on " + this.port);
			r = new Random();
			while (true) {
				Socket client = ss.accept();
				log.info("Client connected from " + client.getInetAddress().getCanonicalHostName()+":"+client.getPort());
				PeerServerThread ts = new PeerServerThread(log, gson, client, FILES_INFO);
				Thread tsThread = new Thread(ts);
				tsThread.start();
			}
		} catch (IOException e) {
			log.info("Port in use!");
		}
	}

	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = PeerServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		PeerServerMain ss = new PeerServerMain();
		ss.startServer();
	}

}
