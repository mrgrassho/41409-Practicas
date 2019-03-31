package Practico1.Punto7;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Random;

public class RandomVal implements Tarea, Serializable{
	private static final long serialVersionUID = 1L;
	private int rango;
	private int value;

	public RandomVal(int rango) {
		this.setValue(0);
		this.setRango(rango);
	}

	public int getRango() {
		return rango;
	}

	public void setRango(int rango) {
		this.rango = rango;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public Object ejecutar() throws RemoteException {
		if  ( Integer.valueOf(this.getRango()) >= 0) {
			this.setRango(100);
		}
		Random random = new Random();
		this.setValue(random.nextInt(Integer.valueOf(this.getRango())));
		return this;
	}

}
