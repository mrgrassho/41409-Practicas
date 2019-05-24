package punto2.syn.con;
import java.io.IOException;

public class ClienteDeposito {
	public static String IP_EXTRACCION = "localhost";
	public static String IP_DEPOSITO = "localhost";
	public static int PORT_EXTRACCION = 9000;
	public static int PORT_DEPOSITO = 9001;
	
	public static void main(String[] args) {
		System.out.println("Cliente Deposito started.");
		Cliente cl = new Cliente(IP_EXTRACCION, PORT_EXTRACCION, IP_DEPOSITO, PORT_DEPOSITO);
		try {
			cl.openSocketDeposito();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		while(true) {
			try {
				cl.deposito(new Double(100));
				Thread.sleep(2000);
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
