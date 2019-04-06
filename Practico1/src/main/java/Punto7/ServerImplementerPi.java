package Punto7;

import java.rmi.RemoteException;

public class ServerImplementerPi implements Tarea {
	private Pi pi;

	public ServerImplementerPi(Pi pi) {
		this.pi = pi;
	}
	
	public Object ejecutar() throws RemoteException {
		return 3.1423;
	}

}
