package Punto7;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Tarea extends Remote{
	public Object ejecutar() throws RemoteException;
}
