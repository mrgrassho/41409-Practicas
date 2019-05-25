package punto3.node;

import java.util.ArrayList;
import java.util.Map;

public class ServiceSuma implements Service {

	private int port;
	private String name;
	
	public ServiceSuma(int port, String name) {
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
		double result = 0;
		for (int i=0;i<lista.length;i++) {
			result+= (double)lista[i];
		}
		try {
			// Hacemos que la tarea tarde mas
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}
}
