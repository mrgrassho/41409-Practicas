package Punto5;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class ServerMain {

	public static void main(String[] args) {
		
		
		try {
			ServerImplementer si = new ServerImplementer();
			System.out.println("----- Implementador instanciado -----");
			
			Registry server = LocateRegistry.createRegistry(80);
			System.out.println("----- Servicio RMI Iniciado -----");
			
			RemoteInt serviceClima = (RemoteInt) UnicastRemoteObject.exportObject(si, 8000);
			System.out.println("----- serviceClima asociado a un puerto -----");
			
			server.rebind("info-clima", serviceClima);
			System.out.println("----- bind de servicio JNDI realizado -----");
	
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
