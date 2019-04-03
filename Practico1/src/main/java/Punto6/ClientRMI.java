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
		
		int nPrueba =1;
		boolean salir1=false;
		do  {  
		    ArrayList<Integer> v1 = new ArrayList<Integer>();
		    ArrayList<Integer> v2 = new ArrayList<Integer>();
			
			for (int i = 0; i<5; i++) {
				int n1=(int)(Math.random()*10);
				int n2=(int)(Math.random()*10);		
				v1.add(n1); 
				v2.add(n2);
			}
			System.out.println("VECTORES ANTES DE ENVIARLOS POR PARAMETRO:");
			System.out.println("V1: " +v1.toString());
			System.out.println("V2: " +v2.toString());
			
			RemoteInt ri = (RemoteInt) clientRMI.lookup("vectores");
			
			boolean salir2= false;
			
			do {
				System.out.println("MENU");
				System.out.println("1. Suma");
				System.out.println("2. Resta");
				System.out.println("0. Terminar");
				System.out.println("Ingrese una opcion>");
				Scanner scanner = new Scanner(System.in);
				Integer op =  scanner.nextInt();
				ArrayList<Integer> vResult;
			
				switch(op) {
				case 1:
					vResult = ri.sumaVectores(v1, v2);
					System.out.println("VECTORES AL VOLVER DEL SERVIDOR:");
					System.out.println("V1: " +v1.toString());
					System.out.println("V2: " +v2.toString());
					System.out.println("VECTOR SUMA: " +vResult.toString()); 
					System.out.println("----- Prueba " +nPrueba+" Finalizada -----");
					salir2=true;
					break;
				case 2:
					vResult = ri.restaVectores(v1, v2);
					System.out.println("AL VOLVER DEL SERVIDOR:");
					System.out.println("V1: " +v1.toString());
					System.out.println("V2: " +v2.toString());
					System.out.println("VECTOR RESTA: " +vResult.toString());
					System.out.println("----- Prueba " +nPrueba+" Finalizada -----");
					salir2=true;
					break;	
				case 0:
					System.out.println("----- Programa Finalizado -----");
					salir2=true;
					salir1=true;
					break;		
				default:
					System.out.println("La opcion ingresada no corresponde.");
						salir2=false;
				}
			}while (!salir2);	
			System.out.println("");
			System.out.println("///////////////////////////////////////////////////////////////");
			System.out.println("");
			nPrueba++;
		}while (!salir1);	
			
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}

}
