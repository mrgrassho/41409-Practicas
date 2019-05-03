package punto3;

import java.io.Serializable;

public class Message implements Serializable {
	
	private static final long serialVersionUID = 1L;
	String header;
	String body;
	
	public Message(String header, String body) {
		super();
		this.header = header;
		this.body = body;
	}
	
	public String getHeader() {
		return header;
	}
	
	public void setHeader(String header) {
		this.header = header;
	}
	
	public Object getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
}