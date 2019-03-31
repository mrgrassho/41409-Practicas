package Punto5;

import java.rmi.RemoteException;
import java.util.ArrayList;

public class ServerImplementer implements RemoteInt {
	
	public String getClima() throws RemoteException {
		
		ArrayList<String> climas = new ArrayList<String>();
        climas.add("Despejado"); climas.add("Nublado"); climas.add("Lluvioso"); climas.add("Tormentoso"); climas.add("Ventoso");
		int n = (int)Math.random()*climas.size();
		
		return climas.get(n);
	}

}
