package Punto7;

import java.rmi.RemoteException;

public class ServerImplementer implements RemoteInt {

	public Object getResult(Tarea t) throws RemoteException {
		return t.ejecutar();
	}
}

