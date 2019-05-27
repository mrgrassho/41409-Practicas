package punto1.utils;

public class Master {
	private String ip;
	private int port;
	private int level;
	
	public Master(String ip, int port, int level) {
		super();
		this.ip = ip;
		this.port = port;
		this.level = level;
	}
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	
	@Override
	public String toString() {
		return "ip:"+ip+",port:"+String.valueOf(port)+",level:"+level;
	}

}
