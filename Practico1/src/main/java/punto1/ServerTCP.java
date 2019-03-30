package punto1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTCP {
	int port;
	
	public ServerTCP (int i) {
		this.port = i;
		this.startServer();
		
	}
	private void startServer() {
		// TODO Auto-generated method stub
		try {
			ServerSocket ss = new ServerSocket (this.port);
			System.out.println("---- Servidor iniciado en el puerto "+this.port+ ". ----");
				
			Socket client = ss.accept();
			System.out.println("Se ha conectado un cliente> "+client.getInetAddress().getCanonicalHostName()+" : "+client.getPort());

			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (client.getOutputStream(),true);
			
			String msg = inputChannel.readLine();
			System.out.println("El cliente ha enviado> "+ msg);
			msg= "'"+msg+"'"+" , soy el Servidor.)";
			outputChannel.println(msg);
			System.out.println("El servidor ha respondido.");
			client.close();
			System.out.println("----Se ha cerrado la conexion----");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(" ERROR> port in use");
		} 
	}
	
	public static void main(String[] args) {
		ServerTCP server = new ServerTCP(9000);
	}

}
