package Punto5;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInt extends Remote{
	public String getClima() throws RemoteException;
	
}
