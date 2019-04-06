package Punto4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MQClient {
	Socket sock;
	int ClientId; 

	public MQClient(int ClientId, String ip, int port) throws UnknownHostException, IOException {
		this.sock = new Socket (ip, port);
		this.ClientId = ClientId;
	}
	
	public String readMsg() throws IOException {
		BufferedReader inputChannel = new BufferedReader (new InputStreamReader (sock.getInputStream()));
		return inputChannel.readLine();
	}
	
	public void writeMsg(int DestinationId, String msg) throws IOException {
		PrintWriter outputChannel = new PrintWriter (this.sock.getOutputStream(),true);
		String completeMsg = new String();
		completeMsg = String.valueOf(DestinationId) + "|" + msg ;
		outputChannel.println(completeMsg);
	}
	
	public void writeMsg(String msg) throws IOException {
		PrintWriter outputChannel = new PrintWriter (this.sock.getOutputStream(),true);
		String completeMsg = new String();
		completeMsg = String.valueOf(this.ClientId) + "|" + msg ;
		outputChannel.println(completeMsg);
	}

	public void end() throws IOException {
		this.sock.close();
	}
	
	
	public static void main(String[] args) {
		
		
		try {
			MQClient mqClient = new MQClient (100, "localhost", 9000);
			mqClient.writeMsg("SEND");
			mqClient.writeMsg(101, "Hola bro");
			MQClient mqClient1 = new MQClient (101, "localhost", 9000);
			mqClient1.writeMsg("RECV");
			System.out.println(mqClient1.readMsg());
			mqClient1.writeMsg("ACK");
			// No hay mensajes
			mqClient1 = new MQClient (101, "localhost", 9000);
			mqClient1.writeMsg("RECV");
			System.out.println(mqClient1.readMsg());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
