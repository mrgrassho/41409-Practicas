package Punto7;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClienteMain {

	public static void main(String[] args) {
		try {
			Registry clientRMI = LocateRegistry.getRegistry("localhost", 9000);
			System.out.println("[+] Client connected - OK");
			RemoteInt ri = (RemoteInt) clientRMI.lookup("ejecutar-tarea");
			Pi pi = new Pi(3);
			pi = (Pi) ri.getResult(pi);
			System.out.println("Value: "+ String.valueOf(pi.getValue()));
			RandomVal ra = new RandomVal(600);
			ra = (RandomVal) ri.getResult(ra);
			System.out.println("Value: "+ String.valueOf(ra.getValue()));
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

}
