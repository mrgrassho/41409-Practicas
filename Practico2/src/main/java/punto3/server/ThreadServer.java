package punto3.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

import punto3.core.Message;

public class ThreadServer implements Runnable{
	private static int SLEEP_TIME = 200;
	private Socket client;
	private Channel queueChannel;
	private String inputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private static final String EXCHANGE_NAME = "XCHNG-OUT";
	private HashMap<String, Message> results;

	public ThreadServer(Socket client, Long routingKey, Channel queueChannel, String inputQueueName, Logger log, HashMap<String, Message> results) {
		this.client = client;
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.inputQueueName = inputQueueName;
		this.googleJson = new Gson();
		this.log = log;
		this.results = results;
	}

	public void run() {
		try {
			System.out.println("");
			ObjectOutputStream outputChannel = new ObjectOutputStream (this.client.getOutputStream());
			ObjectInputStream inputChannel = new ObjectInputStream (this.client.getInputStream());
			
			// Thread que lee socket de cliente
			while(true) {
				try {
					this.log.info("["+routingKey.substring(8)+"] Read client Socket");
					Message decodedMsg = (Message) inputChannel.readObject();
					switch (decodedMsg.getHeader("tipo")) {
						case "query":
							decodedMsg.setHeader("token-id", String.valueOf(routingKey));
							this.log.info("["+routingKey.substring(8)+"] Client has sent a msg --> \n||" + (new Gson()).toJson(decodedMsg).toString());
							String mString = googleJson.toJson(decodedMsg);
							this.queueChannel.basicPublish("", this.inputQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes());
							boolean ready = false;
							log.info("["+routingKey.substring(8)+"] Waiting for response");
							while (!ready) {
								if (results.get(routingKey) != null)
									ready = true;
								Thread.sleep(SLEEP_TIME);
							}
							Message resultMsg = results.get(routingKey);
							results.remove(routingKey);
							resultMsg.delHeader("token-id");
							outputChannel.writeObject(resultMsg);
							log.info("["+routingKey.substring(8)+"] Msg sent to client - "  + (new Gson()).toJson(resultMsg).toString());
							break;
						case "response":
							String key = decodedMsg.getHeader("token-id");
							results.put(key, decodedMsg);
						default:
							break;
					}
					
				}
			    catch (EOFException exc) {
			        break;
			    } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			log.info("Error1 -> ",e);
			try {
				queueChannel.queueUnbind(String.valueOf(routingKey), EXCHANGE_NAME, "");
				queueChannel.queueDelete(routingKey);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			log.info("Error2 -> ",e);
		}
	}
}
