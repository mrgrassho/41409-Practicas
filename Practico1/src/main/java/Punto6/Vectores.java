package Punto6;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

public class Vectores implements Serializable{
	ArrayList<Integer> v1;
	ArrayList<Integer> v2;
	ArrayList<Integer> v3;
	String nombre;
	public Vectores(ArrayList<Integer> v1, ArrayList<Integer> v2, ArrayList<Integer> v3) {
		super();
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.nombre = " PEPITO ";
		this.guardarComoSerializable();
	}
	
	public void guardarComoSerializable () {
		
		
		try {
			File f = new File ("src/main/java/serializado");
			FileOutputStream fos = new FileOutputStream(f);
			PrintWriter pw = new PrintWriter(fos);
			pw.println(this);
			pw.close();
			fos.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
}
