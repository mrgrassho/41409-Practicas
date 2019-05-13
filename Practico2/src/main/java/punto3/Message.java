package punto3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
	/* PAYLOAD DE PRUEBA
	  {"header":{"token-id":"2222"},"body":"NUEVA TAREA DE PRUEBA"}
    */
	private static final long serialVersionUID = 1L;
	private Map<String, String> header;
	private String functionName;
	public Map<String, Integer> parametros;
	private int resultado;
	
	public Message(String functionName) {
		super();
		this.parametros = new HashMap<String,Integer>();
		this.functionName = functionName;
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
	
	public void setHeader(String key, String value) {
		this.header.put(key, value);
	}
	
	public void delHeader(String key) {
		this.header.remove(key);
	}
	
	public String getFunctionName() {
		return this.functionName;
	}
	
	public void addParametro(String key, Integer value) {
		this.parametros.put(key, value);
	}
	
	public void delParametro(String key) {
		this.parametros.remove(key);
	}
	
	public int getResultado() {
		return this.resultado;
	}
	
	public void setResultado(int result) {
		this.resultado = result;
	}
	
}