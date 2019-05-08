package punto3;

import java.net.Socket;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;

public class ThreadNode {
	private Channel queueChannel;
	private String inputQueueName;
	private String outputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private static final String EXCHANGE_NAME = "directOutput";
	
	public ThreadNode(Long routingKey, Channel queueChannel, String inputQueueName, String outputQueueName, Logger log) {
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.inputQueueName = inputQueueName;
		this.outputQueueName = outputQueueName;
		this.googleJson = new Gson();
		this.log = log;
	}

	public void run() {
		try {
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
