package punto3.node;

import java.util.ArrayList;

public class Node {
	ArrayList<Service> services;
	private String name;
	private String ip;
	private int port;
	private int maxLoad;
	private int currentLoad;
	private float percentageLoad;
	private NodeState nodeState;

	public Node(String name, String ip, int port, int maxLoad) {
		this.name = name;
		this.ip = ip;
		this.port = port;
		services = new ArrayList<>();
		this.maxLoad = maxLoad;
		this.currentLoad = 0;
		this.percentageLoad = 0;
		this.nodeState = NodeState.IDLE;
	}
	
	
	public int getPort() {
		return this.port;
	}
	public int getMaxLoad() {
		return maxLoad;
	}

	public void setMaxLoad(int maxLoad) {
		this.maxLoad = maxLoad;
	}

	public int getCurrentLoad() {
		return currentLoad;
	}

	public void increaseCurrentLoad(int currentLoad) {
		this.currentLoad += currentLoad;
		this.updatePercentageLoad();
		this.updateState();
	}

	public void decreaseCurrentLoad(int currentLoad) {
		if (getCurrentLoad() > 0) {
			this.currentLoad -= currentLoad;
			this.updatePercentageLoad();
			this.updateState();
		}
	}

	private void updateState() {
		if (getPercentageLoad() == 0) {
			this.setNodeState(NodeState.IDLE);
		} else if (getPercentageLoad() < 0.60) {
			this.setNodeState(NodeState.NORMAL);
		} else if (getPercentageLoad() < 0.80) {
			this.setNodeState(NodeState.ALERT);
		} else {
			this.setNodeState(NodeState.CRITICAL);
		}
	}

	public NodeState getNodeState() {
		return nodeState;
	}

	public void setNodeState(NodeState nodeState) {
		this.nodeState = nodeState;
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

	public float getPercentageLoad() {
		return percentageLoad;
	}

	private void updatePercentageLoad() {
		this.percentageLoad = getCurrentLoad()/getMaxLoad();
	}
	
	public boolean hasService(String name){
		boolean find = false;
		for (Service s : services) {
			if (s.getName().equals(name)) {
				find = true;
				break;
			}
		}
		return find;
	}

	public Service findServiceByName(String name) {
		Service find = null;
		int i = 0;
		boolean salir = false;
		while (!salir && i<this.getServices().size()){
			if (this.services.get(i).getName().equals(name)) {
					find = this.services.get(i);
					salir = true;
			}
			i++;
		}
		System.out.println(" {FIND} " + find + " - " + name);
		return find;
	}


	@Override
	public boolean equals(Object node) {
		if(node instanceof Node)
			return ((Node) node).getName().equals(this.getName());
		return false;
	}

	public void increaseCurrentLoad() {
		this.increaseCurrentLoad(1);
	}

	public void decreaseCurrentLoad() {
		this.decreaseCurrentLoad(1);
	}
}
