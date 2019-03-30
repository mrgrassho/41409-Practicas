package punto1;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientTCP {
	
	static final Logger log = Logger.getLogger(ClientTCP.class);
	
	public ClientTCP (String ip, int port) {
		try {
			Socket s = new Socket (ip, port);
			System.out.println("-----Cliente iniciado----");
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (s.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (s.getOutputStream(),true);
			
			System.out.println("Ingrese un mensaje>");
			Scanner scannerMSJ = new Scanner(System.in);
			String msj =  scannerMSJ.nextLine(); 
			outputChannel.println(msj);
			System.out.println ("----Mensaje enviado----");
			String response = inputChannel.readLine();
			
			System.out.println("El servidor ha respondido> "+response);
			s.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		//BasicConfigurator.configure();
        boolean esIP = false;
        Scanner scanner;
        String ip="";
        
		 while (!esIP) { 
			System.out.println("Indique servidor [IP o localhost]>");
			scanner = new Scanner(System.in);
			ip =  scanner.nextLine(); 
			 esIP= validarIP(ip);
			 if (!esIP) {System.out.println("Lo ingresado NO es una IP");}
		 }
		System.out.println("Indique puerto>");
		scanner = new Scanner(System.in);
		int port =  scanner.nextInt(); 
		//port=9000; ip= "localhost"; //para tests
		ClientTCP ctcp = new ClientTCP (ip, port);

	}

	public static boolean validarIP(String ip) {
		Scanner scanner;
		if (ip.equals("localhost")) { 
			return true;
		}else {
			String [] sNumeros=ip.split("\\.");
        	if (sNumeros.length <4 || sNumeros.length >4) {
        		  return false;
        		 }else return true;
		}
	}

}

