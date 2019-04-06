package Punto7;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ServerMain {

	public static void main(String[] args) {
		try {
			ServerImplementer si = new ServerImplementer();
			System.out.println("[+] ServiceImplementer - OK");
			Registry server = LocateRegistry.createRegistry(9000);
			System.out.println("[+] RMI service - OK");
			RemoteInt serviceA = (RemoteInt) UnicastRemoteObject.exportObject(si, 8001);
			server.rebind("ejecutar-tarea", serviceA);
			System.out.println("[+] Services Binding - OK");
		} catch (RemoteException e) {
			e.printStackTrace();
			System.err.println("[!] Error -> " + e.toString());
		}
	}

}