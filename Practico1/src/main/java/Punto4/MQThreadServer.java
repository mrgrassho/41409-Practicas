package Punto4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class MQThreadServer implements Runnable{
	Socket client;
	private ArrayList<String> messageQueue;
	public volatile static String msg = null;
	public volatile static ArrayList<String> elemToRemove;
	
	public MQThreadServer (Socket client, ArrayList<String> messageQueue) {
		this.client = client;
		this.messageQueue =  messageQueue;
		this.elemToRemove = new ArrayList<String>();
	}
	
	public String getMessage() {
		return this.msg;
	}
	
	public ArrayList<String> getElemToRemove() {
		return this.elemToRemove;
	}
	
	public void run() {		
		try {
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (this.client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (this.client.getOutputStream(),true);
			String opt = inputChannel.readLine();
			if (opt.substring(opt.indexOf("|")+1, opt.length()).compareTo("SEND") == 0) {
				this.msg = inputChannel.readLine();
				System.out.println(msg);
			} else if (opt.substring(opt.indexOf("|")+1, opt.length()).compareTo("RECV") == 0) {
				boolean f = false;
				String srcId = opt.substring(0, opt.indexOf("|"));
				if (!this.messageQueue.isEmpty()) {
					for (String str : messageQueue) {
						String destId = str.substring(0, str.indexOf("|"));
						String sendMsg = str.substring(str.indexOf("|")+1, str.length());
						if (destId.compareTo(srcId) == 0) {
							outputChannel.println("To: " + destId + " - Msg : " + sendMsg);
							String ack = inputChannel.readLine();
							if (ack.substring(ack.indexOf("|")+1, ack.length()).compareTo("ACK") == 0) {
								f = true;
								this.elemToRemove.add(str);
							}
						}
					}
				}
				
				if (!f) {
					outputChannel.println("No messages.");
				}
			}
			this.client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
