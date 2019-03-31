package Punto5;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import punto1.ClientTCP;

public class ClientRMI {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			
			Registry clientRMI = LocateRegistry.getRegistry("localhost", 80);
		    System.out.println("----- Cliente conectado al servidor por RMI -----");
			
			RemoteInt ri = (RemoteInt) clientRMI.lookup("info-clima");
			String infoClima = ri.getClima();
			System.out.println("Clima en region del servidor: "+infoClima);
			System.out.println("----- Programa Finalizado -----");
			
			
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

}
