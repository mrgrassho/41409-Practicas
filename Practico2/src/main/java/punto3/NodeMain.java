package punto3;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

import ch.qos.logback.core.joran.action.NewRuleAction;


public class NodeMain {
	private static final String EXCHANGE_NAME = "queueProcess";
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String outputQueueName;
	private String processQueueName;
	private String ipRabbitMQ;
	private Node node;
	
	public NodeMain(Node node, String ipRabbitMQ) {
		this.node = node;
		this.ipRabbitMQ = ipRabbitMQ;
		this.username = "admin";
		this.password = "admin";
		this.processQueueName = "processQueue";
		this.outputQueueName = "outputQueue";
		this.configureConnectionToRabbit();
		log.info(" RabbitMQ - Connection established");
	}
	
	private void configureConnectionToRabbit() {
		try {
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost(this.ipRabbitMQ);
			this.connectionFactory.setUsername(this.username);
			this.connectionFactory.setPassword(this.password);
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare(this.outputQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.processQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(node.getName(), true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}	
	}
	
	public void startServer() {
		try {
			log.info(" Node started.");
			Random r = new Random();
			queueChannel.queueBind(processQueueName, EXCHANGE_NAME, "Nodo1");
			queueChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), "UTF-8");
				log.info(" [+] Node received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");

				//Asigna Tarea a Thread
				
				// Thread Termina
				
				// Envio resultado a outputQueue
			};
			this.queueChannel.basicConsume(processQueueName, true, deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			log.info("Port in use!");
		}
	}
	
	
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		NodeMain ss = new NodeMain(new Node("Nodo1", "localhost", 8071), "localhost");
		ss.startServer();
	}

}
