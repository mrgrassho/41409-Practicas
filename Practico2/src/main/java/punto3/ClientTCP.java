package punto3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

import org.slf4j.Logger;

import com.google.gson.Gson;

public class ClientTCP implements Runnable {
	Socket s;
	int numClient;
	static int REQUESTS = 30; 
	private Logger log;
	
	public ClientTCP (String ip, int port, Logger log) {
		try {
			this.s = new Socket (ip, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.log = log;
		this.numClient = (int)(Math.random() * 10000) + 1; 	
	}
	
	@Override
	public void run() {
		try {
			log.info("-----Cliente "+numClient+" iniciado----");
			ObjectOutputStream outputChannel = new ObjectOutputStream (s.getOutputStream());
			ObjectInputStream inputChannel = new ObjectInputStream (s.getInputStream());
			// Parametrizar -->
			int c = REQUESTS;
			while (c-- > 0) {
				String llamada = (Math.random() * 10 > 5) ? "resta" : "suma";
				Message funcion = new Message(llamada);
				funcion.addParametro("num1", (int) (Math.random() * 10));
				funcion.addParametro("num2", (int) (Math.random() * 10));
				funcion.setHeader("client",String.valueOf(numClient));
				//write to server 
				outputChannel.writeObject(funcion);
				log.info("["+numClient+"] Mensaje enviado> " + (new Gson()).toJson(funcion).toString());
				//read 
				Message response = (Message) inputChannel.readObject();
				log.info("["+numClient+"] El servidor ha respondido> "+response.getResultado());
			}
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

