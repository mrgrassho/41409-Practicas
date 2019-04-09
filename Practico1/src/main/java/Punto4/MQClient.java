package Punto4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.json.JSONObject;

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
		System.out.println("send [id-client]\t\tEnviar un mensaje a un cliente.");
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
		int idDestino;
		if (command.equals("send")) {
			if (cliente != null && args.length == 2) {
				idDestino = Integer.parseInt(args[1]);
				System.out.println("Ingrese el Asunto del msj: ");
				String asunto = scanner.nextLine();
				System.out.println("Ingrese el cuerpo del msj: ");
				String body = scanner.nextLine();
				String msg = new JSONObject()
		                  .put("asunto", asunto)
						  .put("body", body).toString();
				if (msg == null) System.err.println("Debe ingresar un mensaje.");
				cliente.writeMsg("SEND");
				cliente.writeMsg(idDestino, msg);
				System.out.println("Msg sent!");
			} else {
				System.err.println("Msg no enviado");
			}
		} else if (command.equals("read")) {
			if (cliente != null && args.length == 1) {
				cliente.writeMsg("RECV");
				String msgX = cliente.readMsg();
				int c = 1;
				while(!msgX.trim().equals("No messages.")) {
					JSONObject jsonMsg = new JSONObject(msgX);
					System.out.println("\t----- MENSAJE " + c + "-----");
					System.out.println("ASUNTO:\n\t" + jsonMsg.get("asunto"));
					System.out.println("BODY:\n\t" + jsonMsg.get("body"));
					System.out.println("\n\t> Desea marcarlo como leido? Y/N");
					String opt = scanner.nextLine();
					if (opt.equals("Y") || opt.equals("y")) {
						cliente.writeMsg("ACK");
					} else {
						cliente.writeMsg("NACK");
					}
					msgX = cliente.readMsg();
					c++;
				}
				if (c > 1) {
					System.out.println("[!] No hay mas mensajes.");
				} else {
					System.out.println("[!] No hay mensajes.");
				}
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
		System.out.print("Ingrese el ID del nuevo cliente -> ");
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
