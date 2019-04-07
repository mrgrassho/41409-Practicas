package Punto3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.plaf.synth.SynthSpinnerUI;

public class MQClient {
	static String IP = "localhost";
	static int PORT = 9000; 
	static int cantClientes = 10;
	static Scanner scanner = new Scanner(System.in);
	private int ClientId;
	private Socket sock;

	public int getClientId() {
		return ClientId;
	}

	public void setClientId(int clientId) {
		ClientId = clientId;
	}

	public MQClient(int ClientId, String ip, int port) throws UnknownHostException, IOException {
		this.sock = new Socket (ip, port);
		this.ClientId = ClientId;
	}
	
	public String readMsg() throws IOException {
		BufferedReader inputChannel = new BufferedReader (new InputStreamReader (sock.getInputStream()));
		return inputChannel.readLine();
	}
	
	public void writeMsg(int DestinationId, String msg) throws IOException {
		PrintWriter outputChannel = new PrintWriter (sock.getOutputStream(),true);
		String completeMsg = new String();
		completeMsg = String.valueOf(DestinationId) + "|" + msg ;
		outputChannel.println(completeMsg);
	}
	
	public void writeMsg(String msg) throws IOException {
		PrintWriter outputChannel = new PrintWriter (sock.getOutputStream(),true);
		String completeMsg = new String();
		completeMsg = String.valueOf(this.ClientId) + "|" + msg ;
		outputChannel.println(completeMsg);
	}
	
	public void end() throws IOException {
		
	}
	
	public static void menuCli(int id) {
		System.out.println();
		System.out.println("Bandeja del Cliente -> " + String.valueOf(id));
		System.out.println("send [id-client] [MSG]\t\tEnviar un mensaje, el MSG debe ser una sola palabra (sin espacios).");
		System.out.println("read\t\t\tLeer mensajes");
		System.out.println("help\t\t\tMuestra este mensaje.");
		System.out.println("exit\t\t\tSalir.");
		System.out.println();
	}
	
	public static String[] splitArgs(String command) {
		command = command.trim();
		return command.split(" ");
	}
	
	public static void interpretCmd(String line, MQClient cliente) throws IOException {
		String[] args = splitArgs(line);
		String command = args[0];
		int idOrigen , idDestino;
		if (command.equals("send")) {
			if (cliente != null && args.length == 3) {
				idDestino = Integer.parseInt(args[1]);
				String msg = args[2];
				if (msg == null) System.err.println("Debe ingresar un mensaje.");
				cliente.writeMsg("SEND");
				cliente.writeMsg(idDestino, msg);
				System.out.println("Msg sent!");
			}
		} else if (command.equals("read")) {
			if (cliente != null && args.length == 1) {
				cliente.writeMsg("RECV");
				String msgX = cliente.readMsg();
				while(!msgX.trim().equals("No messages.")) {
					System.out.println(msgX);
					msgX = cliente.readMsg();
				}
				System.out.println("No messages.");
			}
		} else if (command.equals("help")) {
				menuCli(cliente.getClientId());
		} else if (command.equals("exit")) {
			cliente.end();
		} else {
			System.err.println("Opcion incorrecta!");
		}
	}
		
	public static Integer obtenerOpcion() {
		int opcion;
		System.out.print("> ");
		while ((!scanner.hasNextInt()) || (1 < scanner.nextInt() || scanner.nextInt() > 4)) {scanner.next();}
		opcion = scanner.nextInt();
		return opcion;
	}

	
	public static void main(String[] args) throws IOException {
		int opt;
		int idOrigen;
		int idDestino;
		System.out.println("Ingrese el ID del nuevo cliente");
		while ((!scanner.hasNextInt())) {scanner.next();}
		int cc = scanner.nextInt();
		MQClient cliente = new MQClient(cc, IP, PORT);
		scanner = new Scanner(System.in);
		System.out.println("Ingrese - help - para ver las opciones.");
		while(true) {
			System.out.print("> ");
			interpretCmd(scanner.nextLine(), cliente);
		}
	}
}
