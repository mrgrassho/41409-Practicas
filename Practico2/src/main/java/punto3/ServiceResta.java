package punto3;

import java.util.ArrayList;
import java.util.Map;

public class ServiceResta implements Service {

	private int port;
	private String name;
	
	public ServiceResta(int port, String name) {
		this.port = port;
		this.name = name;
	}
	
	
	public String getName() {
		return this.name;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public Object execute (Object[] lista) {
		double result = (double)lista[0];
		for (int i=0;i<lista.length;i++) {
			if (i!=0) {result-= (double)lista[i];}
		}
		
		try {
			// Hacemos que la tarea tarde mas
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}
}