package punto3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientGenerator {
	private final static Logger log = LoggerFactory.getLogger(ClientGenerator.class);
	
	public static void main(String[] args) {
		int count = 2;
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		
		while (count > 0) {
			ClientTCP cliente =  new ClientTCP("localhost", 8090, log);
			Thread tsThread = new Thread(cliente);
			tsThread.start(); count--;
		}
	}
}
