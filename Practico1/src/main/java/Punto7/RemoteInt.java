package Practico1.Punto7;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInt extends Remote{
	public Object getResult(Tarea t) throws RemoteException;
}
