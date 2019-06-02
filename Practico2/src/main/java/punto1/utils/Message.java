package punto1.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;
	private String cmd;
	private Map<String, String> parametros;
	private byte[] body;
	
	public Message(String cmd) {
		super();
		this.parametros = new HashMap<String,String>();
		this.setCmd(cmd);
		this.body = null;
	}
	
	public void setParametro(String key, String value) {
		this.parametros.put(key, value);
		
	}
	
	public String getParametro(String key) {
		return this.parametros.get(key);
	}
	
	public void delParametro(String key) {
		this.parametros.remove(key);
	}

	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}
	
}