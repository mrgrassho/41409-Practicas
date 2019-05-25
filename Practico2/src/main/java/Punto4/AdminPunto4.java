package Punto4;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class AdminPunto4 {

	public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, TimeoutException {

		System.out.println("");
		System.out.println("Ingrese cantidad de servidores [fijos]>");
		Scanner scanner = new Scanner(System.in);
		int qServers =  scanner.nextInt();
		ArrayList<Integer> portServers = new ArrayList<Integer>();
		
		for (int i=0; i<qServers;i++) {
			ServerMain sm = new ServerMain(i,"localhost", 80+i);
			sm.startServer("sobelImagenes", 8000+i);
			portServers.add(80+i);
		}
		
		SobelClient.main(portServers);	
	}

}
