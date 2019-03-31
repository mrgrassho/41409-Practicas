package Punto6;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RemoteInt extends Remote{
	
	public ArrayList<Integer> sumaVectores (ArrayList<Integer> v1, ArrayList<Integer> v2) throws RemoteException;
}
