package Practico1.Punto7;

import java.rmi.RemoteException;
import java.util.Random;

public class ServerImplementerRandom implements Tarea  {
	private RandomVal randomVal;
	
	public ServerImplementerRandom(RandomVal randomVal) {
		this.randomVal = randomVal;
	}
	public Object ejecutar() throws RemoteException {
		if  ( Integer.valueOf(randomVal.getRango()) >= 0) {
			randomVal.setRango(100);
		}
		Random random = new Random();
		randomVal.setValue(random.nextInt(Integer.valueOf(randomVal.getRango())));
		return randomVal;
	}

}
