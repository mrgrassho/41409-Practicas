package punto3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.slf4j.Logger;

public class ClientTCP implements Runnable {
	Socket s;
	int numClient;
	private Logger log;
	
	public ClientTCP (String ip, int port, Logger log) {
		try {
			this.s = new Socket (ip, port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.log = log;
		this.numClient = (int)(Math.random() * 1000) + 1; 	
	}
	
	@Override
	public void run() {
		try {
			log.info("-----Cliente "+numClient+" iniciado----");
			ObjectOutputStream outputChannel = new ObjectOutputStream (s.getOutputStream());
			ObjectInputStream inputChannel = new ObjectInputStream (s.getInputStream());
			// Parametrizar -->
			String llamada = "suma";  
			Message funcion = new Message(llamada);
			funcion.addParametro("num1", 5);
			funcion.addParametro("num2", 7);
			
			outputChannel.writeObject(funcion);
			Message response = (Message) inputChannel.readObject();
			log.info("El servidor ha respondido> "+response.getResultado());
			s.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

