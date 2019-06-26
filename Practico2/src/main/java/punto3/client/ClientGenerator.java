package punto3.client;

import java.io.IOException;
import java.net.ConnectException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import punto3.server.ServerMain;

public class ClientGenerator {
	private final static Logger log = LoggerFactory.getLogger(ClientGenerator.class);

	public static void main(String[] args) {
		int count = 100;
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);

		ClientTCP cliente;
		try {
			while (count > 0) {
				cliente = new ClientTCP("localhost", 8090, log);
				Thread tsThread = new Thread(cliente);
				tsThread.start(); count--;
			}
		} catch (ConnectException e) {
			System.err.println("[!] El servidor se encuentra caido.");
		} catch (IOException e) {
			System.err.println("[!] Ha ocurrido un error.");
		}
	}
}
