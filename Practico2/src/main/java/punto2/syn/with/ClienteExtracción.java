package punto2.syn.with;

import java.io.IOException;
import java.util.ArrayList;

public class ClienteExtracción {
	public static String IP_EXTRACCION = "localhost";
	public static String IP_DEPOSITO = "localhost";
	public static int PORT_EXTRACCION = 9000;
	public static int PORT_DEPOSITO = 9001;

	public static void main(String[] args) {
		System.out.println("ClientGenerator started.");
		Cliente cl = new Cliente(IP_EXTRACCION, PORT_EXTRACCION, IP_DEPOSITO, PORT_DEPOSITO);
		try {
			cl.openSocketExtraccion();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		while(true) {
			try {
				cl.extraccion(new Double(50));
				Thread.sleep(200);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

}
