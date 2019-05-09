package punto3;

import java.util.ArrayList;

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
	
	public Object execute (ArrayList<Object> lista) {
		int result=0;
		for(Object o:lista) {
			result+= (int)o;
		}
		return result;
	}
}
