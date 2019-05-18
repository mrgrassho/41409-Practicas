package punto3;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class ThreadServer implements Runnable{
	private Socket client;
	private Channel queueChannel;
	private String inputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private static final String EXCHANGE_NAME = "XCHNG-OUT";
	
	public ThreadServer(Socket client, Long routingKey, Channel queueChannel, String inputQueueName, Logger log) {
		this.client = client;
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.inputQueueName = inputQueueName;
		this.googleJson = new Gson();
		this.log = log;
	}

	public void run() {
		try {
			System.out.println("");
			ObjectOutputStream outputChannel = new ObjectOutputStream (this.client.getOutputStream());
			ObjectInputStream inputChannel = new ObjectInputStream (this.client.getInputStream());
			
			this.queueChannel.queueDeclare(routingKey, true, false, false, null);	
			log.info("Thread Queue [" + routingKey + "] created.");
			// Funcion que se ejecuta cada vez que hay un mensaje en la cola de salida disponible.
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				Message messageResp = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Message.class);
				messageResp.delHeader("token-id");
				outputChannel.writeObject(messageResp);
				log.info("["+routingKey.substring(8)+"] Msg sent to client - "  + (new Gson()).toJson(messageResp).toString());
			};
			this.queueChannel.basicConsume(routingKey, true, deliverCallback, consumerTag -> {});
			
			// Thread que lee socket de cliente
			while(true) {
				try {
					this.log.info("["+routingKey.substring(8)+"] Read client Socket");
					Message decodedMsg = (Message) inputChannel.readObject();
					decodedMsg.setHeader("token-id", String.valueOf(routingKey));
					this.log.info("["+routingKey.substring(8)+"] Client has sent a msg --> \n||" + (new Gson()).toJson(decodedMsg).toString());
					String mString = googleJson.toJson(decodedMsg);
					this.queueChannel.basicPublish("", this.inputQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes());
				}
			    catch (EOFException exc) {
			        break;
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
