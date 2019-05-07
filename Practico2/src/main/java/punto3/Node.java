package punto3;

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
		this.currentLoad -= currentLoad;
		this.updatePercentageLoad();
		this.updateState();
	}

	private void updateState() {
		if (getPercentageLoad() == 0) {
			this.setNodeState(NodeState.IDLE);
		} else if (getPercentageLoad() <= 0.40) {
			this.setNodeState(NodeState.NORMAL);
		} else if (getPercentageLoad() <= 0.60) {
			this.setNodeState(NodeState.ALERT);
		} else if (getPercentageLoad() <= 0.60) {
			this.setNodeState(NodeState.CRITICAL);
		}
	}

	public NodeState getNodeState() {
		return nodeState;
	}

	private void setNodeState(NodeState nodeState) {
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

}
