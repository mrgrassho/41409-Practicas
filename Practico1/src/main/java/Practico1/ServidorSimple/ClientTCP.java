package Practico1.ServidorSimple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientTCP {

	public ClientTCP (String ip, int port) {
		try {
			Socket s = new Socket (ip, port);
			System.out.println(" Client connection ok");
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (s.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (s.getOutputStream(),true);
			System.out.println("Ingrese data: ");
			Scanner scanner = new Scanner(System.in);
			String param =  scanner.nextLine(); 
			outputChannel.println(param);

			String response = inputChannel.readLine();

			System.out.println(" Respons from server: "+response);
			s.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public static void main(String[] args) {
		ClientTCP ctcp = new ClientTCP ("localhost", 9000);

	}

}
