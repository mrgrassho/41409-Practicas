package Practico1.ServidorSimple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ThreadServer implements Runnable{
	Socket client;
	
	public ThreadServer (Socket client) {
		this.client = client;
	}
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (this.client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (this.client.getOutputStream(),true);
			
			String msg = inputChannel.readLine();
			System.out.println(" MSG from client: " + msg);
			msg+=" from server";
			outputChannel.println(msg);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
