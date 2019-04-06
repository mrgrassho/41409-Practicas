package Punto2;

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
			int numClient = (int)(Math.random() * 1000) + 1; // para diferenciar los diferentes mensajes impresos en el Server
			String msj;
			
			Socket s = new Socket (ip, port);
			System.out.println("-----Cliente "+numClient+" iniciado----");
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (s.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (s.getOutputStream(),true);
			
			System.out.println("Ingrese un mensaje>");
			Scanner scannerMSJ = new Scanner(System.in);
			msj =  scannerMSJ.nextLine();
			//msj= "Hola";  //Para tests rapidos (con un mensaje por defecto) descomentar esta linea; comentar las tres lineas anteriores.
			
			msj+= ". {Usuario Aleatorio n�"+numClient +"}"; 
			outputChannel.println(msj);
			outputChannel.println ("----Mensaje enviado a Servidor----");
			String response = inputChannel.readLine();
			
			System.out.println("El servidor ha respondido> "+response);
			s.close();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
        boolean esIP = false;
        Scanner scanner;
        String ip="";
        int port;
        
        //inicio seccion ingreso manual
		while (!esIP) { 
			System.out.println("Indique servidor [IP o localhost]>");
			scanner = new Scanner(System.in);
			ip =  scanner.nextLine(); 
			 esIP= validarIP(ip);
			 if (!esIP) {System.out.println("Lo ingresado NO es una IP");}
		 }
		System.out.println("Indique puerto>");
		scanner = new Scanner(System.in);
		port =  scanner.nextInt(); 
        //fin seccion Ingreso Manual	
		
		//port=9000; ip= "localhost"; //para tests rapidos, descomentar esta linea; Comentar la seccion Ingreso Manual
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

