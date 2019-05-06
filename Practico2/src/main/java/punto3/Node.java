package punto3;

import java.util.ArrayList;

public class Node {
	ArrayList<Service> services;
	private String name;
	private String ip;
	private int port; 
	private int cargaMax;
	private int cargaActual;
	
	public Node(String name, String ip, int port, int cargaMax) {
		this.name = name;
		this.ip = ip;
		this.port = port;
<<<<<<< HEAD
		services = new ArrayList<Service>();
=======
		services = new ArrayList<>();
		this.cargaMax = cargaMax;
		this.cargaActual = 0;
>>>>>>> master
	}
	
	public String getName() {
		return name;
	}
	
	public void addService(Service service) {
		this.services.add(service);
	}
	
	public void delService(Service service) {
		this.services.remove(service);
	}
	
	public ArrayList<Service> getServices() {
		return this.services;
	}

}
