package Punto6;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import punto1.ClientTCP;

public class ClientRMI {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			
			Registry clientRMI = LocateRegistry.getRegistry("localhost", 80);
		    System.out.println("----- Cliente conectado -----");
			
		    ArrayList<Integer> v1 = new ArrayList<Integer>();
		    ArrayList<Integer> v2 = new ArrayList<Integer>();
			
			for (int i = 0; i<5; i++) {
				int n1=(int)(Math.random()*10);
				int n2=(int)(Math.random()*10);		
				v1.add(n1); 
				v2.add(n2);
			}
			System.out.println("ANTES DE ENVIARLOS POR PARAMETRO:");
			System.out.println("V1: " +v1.toString());
			System.out.println("V2: " +v2.toString());
			
			RemoteInt ri = (RemoteInt) clientRMI.lookup("vectores");
			
			//ACA TENDRIA QUE DARLE A ELEGIR SI SUMAR O RESTAR 
			ArrayList<Integer> vResult = ri.sumaVectores(v1, v2);
			
			System.out.println("AL VOLVER DEL SERVIDOR:");
			System.out.println("V1: " +v1.toString());
			System.out.println("V2: " +v2.toString());
			
			System.out.println("VECTOR SUMA: " +vResult.toString());
			
			System.out.println("----- Programa Finalizado -----");
			
			
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

}
