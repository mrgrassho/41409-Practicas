package punto3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class ThreadServer implements Runnable{
	private Socket client;
	private Channel queueChannel;
	private String inputQueueName;
	private String outputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private static final String EXCHANGE_NAME = "directOutput";
	
	public ThreadServer(Socket client, Long routingKey, Channel queueChannel, String inputQueueName, String outputQueueName, Logger log) {
		this.client = client;
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.inputQueueName = inputQueueName;
		this.outputQueueName = outputQueueName;
		this.googleJson = new Gson();
		this.log = log;
	}

	public void run() {
		try {
			ObjectOutputStream outputChannel = new ObjectOutputStream (this.client.getOutputStream());
			ObjectInputStream inputChannel = new ObjectInputStream (this.client.getInputStream());
			queueChannel.exchangeDeclare(EXCHANGE_NAME, "direct");
			while (true) {
				Message decodedMsg = (Message) inputChannel.readObject();
				decodedMsg.addHeader("token-id", String.valueOf(routingKey));
				this.log.info("Client has sent a msg");
				String body = (String) decodedMsg.getBody();
				this.queueChannel.basicPublish("", this.inputQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, body.getBytes());
				
				// Funcion que se ejecuta cada vez que hay un mensaje disponible.
				DeliverCallback deliverCallback = (consumerTag, delivery) -> {
		            String str = new String(delivery.getBody(), "UTF-8");
		            Message message = new Message(str);
		            outputChannel.writeObject(message);
		            log.info(" [+] Msg sent. - '" + delivery.getEnvelope().getRoutingKey() + "':'" + message.getBody() + "'");
				};
				// Bind Queue 
				queueChannel.queueBind(this.outputQueueName, EXCHANGE_NAME, this.routingKey);
				
				this.queueChannel.basicConsume(outputQueueName, true, deliverCallback, consumerTag -> {
					// Unbind Queue
					queueChannel.queueUnbind(this.outputQueueName, EXCHANGE_NAME, this.routingKey);
				});
			}
			
		} catch (IOException e) {
			log.info("Error -> ",e);
		} catch (ClassNotFoundException e) {
			log.info("Error -> ",e);
		}
	}
}
