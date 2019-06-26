package punto3.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import org.slf4j.Logger;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

import punto3.core.Message;

public class ThreadServer implements Runnable{
	public static final int RETRY_SLEEP_TIME = 10;
	public static final int MAX_RETRIES_LISTEN = 1000;
	public static final int	MAX_RETRIES_BALANCER = 10;
	public static final String nodeDownQueue = "nodeDownQueue";
	private Socket client;
	private Channel queueChannel;
	private String inputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private HashMap<Long, String> routingKeys;

	public ThreadServer(Socket client, Long routingKey, HashMap<Long, String> routingKeys, Channel queueChannel, String inputQueueName, Logger log) {
		this.client = client;
		this.routingKey = String.valueOf(routingKey);
		this.routingKeys = routingKeys;
		this.queueChannel = queueChannel;
		this.inputQueueName = inputQueueName;
		this.googleJson = new Gson();
		this.log = log;
	}

	public void run() {
		try {
			ObjectOutputStream outputChannel = new ObjectOutputStream (this.client.getOutputStream());
			ObjectInputStream inputChannel = new ObjectInputStream (this.client.getInputStream());
			routingKeys.put(Long.parseLong(routingKey), "BEING_USED");
			
			log.info("Thread Queue [" + routingKey + "] created.");
			// Thread que lee socket de cliente
			while(true) {
				try {
					this.log.info("["+routingKey.substring(8)+"] Read client Socket");
					Message decodedMsg = (Message) inputChannel.readObject();
					decodedMsg.setHeader("token-id", String.valueOf(routingKey));
					this.log.info("["+routingKey.substring(8)+"] Client has sent a msg --> \n||" + (new Gson()).toJson(decodedMsg).toString());
					String mString = googleJson.toJson(decodedMsg);
					int retriesInNode = 0;
					boolean flag = false;
					for (int i = 0; i < MAX_RETRIES_BALANCER; i++) {
						this.queueChannel.basicPublish("", this.inputQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes());
						retriesInNode = 0;
						flag = false;
						this.log.info("["+routingKey.substring(8)+"] Waiting for response...");
						while (retriesInNode < MAX_RETRIES_LISTEN && !flag) {
							if (this.queueChannel.messageCount(routingKey) >= 1) {
								this.log.info("["+routingKey.substring(8)+"] Response arrived.");
								flag = true;  // a ThreadNode finished his task
							} else {
								try {
									Thread.sleep(RETRY_SLEEP_TIME);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							retriesInNode++;
						}
						if (flag) break;
					}
					Message messageResp ;
					if (flag) {
						GetResponse r = this.queueChannel.basicGet(routingKey, true);
						messageResp = googleJson.fromJson(new String(r.getBody(),"UTF-8"), Message.class);
						messageResp.delHeader("token-id");
					} else {
						this.log.info("[!] Service NOT Available.");
						messageResp = new Message("");
						messageResp.setResultado("Service NOT Available.");
						queueChannel.basicPublish("", nodeDownQueue, MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes());
					}
					outputChannel.writeObject(messageResp);
					log.info("["+routingKey.substring(8)+"] Msg sent to client - "  + (new Gson()).toJson(messageResp).toString());
				}
			    catch (EOFException exc) {
			        break;
			    }
			}

		} catch (IOException e) {
			log.info("Error1 -> ",e);
			purgeQueueAndCleanMap();
		} catch (ClassNotFoundException e) {
			log.info("Error2 -> ",e);
			purgeQueueAndCleanMap();
		}
		purgeQueueAndCleanMap();
	}
	
	void purgeQueueAndCleanMap(){
		try {
			this.queueChannel.queuePurge(routingKey);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		routingKeys.put(Long.parseLong(routingKey), "AVAILABLE");
		
	}
	
}
