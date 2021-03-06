package Punto3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Array;
import java.util.ArrayList;

public class MQThreadServer implements Runnable{
	Socket client;
	private ArrayList<String> messageQueue;
	
	public MQThreadServer (Socket client, ArrayList<String> messageQueue) {
		this.client = client;
		this.messageQueue =  messageQueue;
	}
	
	public void run() {	
		try {
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (this.client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (this.client.getOutputStream(),true);
			while(!client.isClosed()) {
				String opt = inputChannel.readLine();
				if (opt.substring(opt.indexOf("|")+1, opt.length()).equals("SEND")) {
					messageQueue.add(inputChannel.readLine());
				} else if (opt.substring(opt.indexOf("|")+1, opt.length()).equals("RECV")) {
					String srcId = opt.substring(0, opt.indexOf("|"));
					if (!this.messageQueue.isEmpty()) {
						for (String str : messageQueue) {
							String destId = str.substring(0, str.indexOf("|"));
							String sendMsg = str.substring(str.indexOf("|")+1, str.length());
							if (destId.equals(srcId)) {
								outputChannel.println(sendMsg);
							}
						}
					}
					outputChannel.println("No messages.");	
				}
			}
			this.client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

}
