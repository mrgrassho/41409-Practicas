package Practico1.Punto7;

import java.io.Serializable;
import java.rmi.RemoteException;

public class Pi implements Tarea, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int precision;
	private double value;

	public Pi(int precision) {
		this.setValue(0);
		this.setPrecision(precision);
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public Object ejecutar() throws RemoteException {
		this.setValue(3.1423);
		return this;
	}
	
}
