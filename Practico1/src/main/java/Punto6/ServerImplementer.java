package Punto6;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ServerImplementer implements RemoteInt {

	public ArrayList<Integer> sumaVectores(ArrayList<Integer> v1, ArrayList<Integer> v2) throws RemoteException {
		ArrayList<Integer> v3 = new ArrayList<Integer>();
		
		for (int i =0; i<v1.size(); i++) {
			v3.add((v1.get(i)+v2.get(i)));
		}
		
		v2 = new ArrayList<Integer>(); //ERROR
		
		System.out.println("MODIFICACION EN EL SERVIDOR:");
		System.out.println("V1: " +v1.toString());
		System.out.println("V2: " +v2.toString());
		System.out.println("");
		System.out.println("SUMA: " +v3.toString());
		System.out.println("///////////////////////////////////////////////////////////////");
		System.out.println("");
		return v3;
	}

	
	public ArrayList<Integer> restaVectores(ArrayList<Integer> v1, ArrayList<Integer> v2) throws RemoteException {
		ArrayList<Integer> v3 = new ArrayList<Integer>();
		
		for (int i =0; i<v1.size(); i++) {
			v3.add((v1.get(i)-v2.get(i)));
		}
		
		v1 = new ArrayList<Integer>(); //ERROR introducido.
		
		System.out.println("MODIFICACION EN EL SERVIDOR:");
		System.out.println("V1: " +v1.toString());
		System.out.println("V2: " +v2.toString());
		System.out.println("");
		System.out.println("RESTA: " +v3.toString());
		System.out.println("///////////////////////////////////////////////////////////////");
		System.out.println("");
		return v3;
	}
}
