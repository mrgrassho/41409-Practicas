package punto1.peer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class SeederServer implements Runnable{
	private final Logger log = LoggerFactory.getLogger(SeederServer.class);
	private static final String FILES_INFO="src/main/java/punto1/peer/resources/local-files-info.json";
	private Gson gson;
	private int port;
	private ServerSocket ss;

	public SeederServer(int port) {
		super();
		this.port = port;
		this.gson = new Gson();
	}

	public void startServer() {
		try {
			ss = new ServerSocket (this.port);
			log.info("PeerServer started on " + this.port);
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

	@Override
	public void run() {
		// TODO Auto-generated method stub
		startServer();
	}
}
