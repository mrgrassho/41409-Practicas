package Punto4;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class ServerMain {

	public static void main(String[] args) {
		
		
		try {
			int port=80;
			
			ServerImplementerSobel si = new ServerImplementerSobel();
			System.out.println("----- Implementador Sobel instanciado -----");
			
			Registry server = LocateRegistry.createRegistry(port);
			System.out.println("----- Servicio RMI Iniciado -----");
			
			RemoteInt serviceSobel = (RemoteInt) UnicastRemoteObject.exportObject(si, 8000);
			System.out.println("----- serviceSobel asociado a un puerto -----");
			
			server.rebind("sobelImageV1", serviceSobel);
			System.out.println("----- bind de servicio JNDI realizado -----");
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}