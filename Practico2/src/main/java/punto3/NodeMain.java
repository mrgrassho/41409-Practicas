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
import com.rabbitmq.client.AMQP.Queue;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

import ch.qos.logback.core.joran.action.NewRuleAction;


public class NodeMain {
	private static final String EXCHANGE_OUTPUT = "XCHNG-OUT";
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String outputQueueName;
	private String activesQueueName;
	private String ipRabbitMQ;
	private Node node;
	private Gson googleJson;
	
	public NodeMain(Node node, String ipRabbitMQ) {
		this.node = node;
		this.ipRabbitMQ = ipRabbitMQ;
		this.username = "admin";
		this.password = "admin";
		this.activesQueueName = "activeQueue";
		this.outputQueueName = "outputQueue";
		googleJson = new Gson();
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
			this.queueChannel.queueDeclare(this.activesQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(node.getName(), true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}	
	}
	
	public void startServerA() {
		try {
			log.info(" Node A started.");
			Random r = new Random();
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				Message message = googleJson.fromJson(new String(delivery.getBody(), "UTF-8"), Message.class);
				log.info(" [+] Node received '" + delivery.getEnvelope().getRoutingKey() + "': '" + message.getBody() + "'");
				//Asigna Tarea a Thread
				log.info(" [+] Working...");
				// Thread Termina
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Envio resultado a outputQueue
				Message res = new Message("RESULTADO X");
				res.addHeader("token-id",  message.getHeader("token-id"));
				String mString =  googleJson.toJson(res); 
				queueChannel.basicPublish(EXCHANGE_OUTPUT, message.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				log.info(" [+] Node A Sent response!");
			};
			queueChannel.basicConsume("NodoA", true, deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startServerB() {
		try {
			log.info(" Node B started.");
			Random r = new Random();
			queueChannel.exchangeDeclare(EXCHANGE_OUTPUT, "direct");
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				Message message = googleJson.fromJson(new String(delivery.getBody(), "UTF-8"), Message.class);
				log.info(" [+] Node received '" + delivery.getEnvelope().getRoutingKey() + "': '" + message.getBody() + "'");
				//Asigna Tarea a Thread
				log.info(" [+] Working...");
				// Thread Termina
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Envio resultado a outputQueue
				Message res = new Message("RESULTADO X");
				res.addHeader("token-id",  message.getHeader("token-id"));
				String mString =  googleJson.toJson(res); 
				queueChannel.basicPublish(EXCHANGE_OUTPUT, message.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				log.info(" [+] Node B Sent response!");
			};
			queueChannel.basicConsume("NodoB", true, deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startServerC() {
		try {
			log.info(" Node C started.");
			Random r = new Random();
			queueChannel.exchangeDeclare(EXCHANGE_OUTPUT, "direct");
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				Message message = googleJson.fromJson(new String(delivery.getBody(), "UTF-8"), Message.class);
				log.info(" [+] Node received '" + delivery.getEnvelope().getRoutingKey() + "': '" + message.getBody() + "'");
				//Asigna Tarea a Thread
				log.info(" [+] Working...");
				// Thread Termina
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// Envio resultado a outputQueue
				Message res = new Message("RESULTADO X");
				res.addHeader("token-id",  message.getHeader("token-id"));
				String mString =  googleJson.toJson(res); 
				queueChannel.basicPublish(EXCHANGE_OUTPUT, message.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				log.info(" [+] Node C Sent response!");
			};
			queueChannel.basicConsume("NodoC", true, deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		NodeMain ss = new NodeMain(new Node("Nodo1", "localhost", 8071,10), "localhost");
		ss.startServerA();
		ss.startServerB();
		ss.startServerC();
	}

}
