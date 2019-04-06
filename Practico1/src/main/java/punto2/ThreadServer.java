package Punto2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ThreadServer implements Runnable{
	Socket client;
	int sleep;
	
	public ThreadServer (Socket client, int sleep) {
		this.client = client;
		this.sleep = sleep;
	}
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (client.getOutputStream(),true);
			
			String msg = inputChannel.readLine();
			System.out.println("Un cliente ha enviado> "+ msg);
			msg= "'"+msg+"'"+" . [Echo from Server]";
			outputChannel.println(msg);
			System.out.println("Respuesta enviada.");
			try {
				Thread.sleep(this.sleep);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			client.close();
			System.out.println("----Se ha cerrado una conexion----");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
