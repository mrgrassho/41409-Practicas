package punto3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Map<String, String> header;
	private String body;
	
	public Message(String body) {
		super();
		this.body = body;
		this.header = new HashMap<String,String>();
	}
	
	public String getFullHeader() {
		String str = new String();
		for (Map.Entry me : header.entrySet()) {
			str = me.getKey() + ": "+ me.getValue() + "\n";
		}
		return str;
	}
	
	public String getHeader(String key) {
		return this.header.get(key);
	}
	
	public void addHeader(String key, String value) {
		this.header.put(key, value);
	}
	
	public void delHeader(String key) {
		this.header.remove(key);
	}
	
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
}