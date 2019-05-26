package punto1.utils;

public class Peer {
	private String peerId;

	public Peer(String ip, String port) {
		super();
		peerId = ip+":"+port;	
	}
	
	public String getPeerId() {
		return peerId;
	}
	
	public void setPeerId(String ip, String port) {
		peerId = ip+":"+port;
	}
	
	public String getIp() {
		return peerId.split(":")[0];
	}
	
	public String getPort() {
		return peerId.split(":")[1];
	}
}
