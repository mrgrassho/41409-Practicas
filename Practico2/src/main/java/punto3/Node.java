package punto3;

import java.util.ArrayList;

public class Node {
	ArrayList<Service> services;
	private String name;
	private String ip;
	private int port; 
	
	public Node(String name, String ip, int port) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		services = new ArrayList<Service>();
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
