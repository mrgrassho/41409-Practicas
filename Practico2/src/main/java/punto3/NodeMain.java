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
	private final Logger log = LoggerFactory.getLogger(NodeMain.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String outputQueueName;
	private String activesQueueName;
	private String myProcessQueueName;
	
	private String ipRabbitMQ;
	private Node node;
	private Gson googleJson;
	private int max_tasks;
	
	public NodeMain(Node node, String ipRabbitMQ) {
		this.node = node;
		this.ipRabbitMQ = ipRabbitMQ;
		this.username = "admin";
		this.password = "admin";
		this.activesQueueName = "activeQueue";
		this.outputQueueName = "outputQueue";
		this.myProcessQueueName = this.node.getName();
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
			this.queueChannel.queueDeclare(this.myProcessQueueName, true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}	
	}
	
	public void startNode() {
		try {
			log.info(this.node.getName()+" Started");
			Random r = new Random();
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				Message message = googleJson.fromJson(new String(delivery.getBody(), "UTF-8"), Message.class);
				log.info(" [+] Node received '" + delivery.getEnvelope().getRoutingKey() + "': '" + message.getFunctionName() + "'");
				//Asigna Tarea a Thread
				log.info(" [+] Working...");
				
				//asigno la tarea a un thread
				ThreadNode tn = new ThreadNode(this.node,Long.parseLong(message.getHeader("token-id")),message, queueChannel,this.activesQueueName,outputQueueName,log);
				Thread nodeThread = new Thread(tn);
				nodeThread.start();
			};
			queueChannel.basicConsume(this.myProcessQueueName, true, deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		
		NodeMain node1 = new NodeMain(new Node("NodoA", "localhost", 8071,10), "localhost");
		
		//CREO LOS DIFERENTES SERVICIOS QUE VA A TENER CADA UNO (AGREGAR RESTA, MULTIPLICACION, MOD, etc 
		Service suma = (Service) new ServiceSuma(8071,"suma"); // DUDA: cuando se le pide el puerto al servicio?
		node1.node.addService(suma);
		node1.startNode();
		
		/*
		NodeMain node2 = new NodeMain(new Node("Nodo2", "localhost", 8072,10), "localhost");
		node2.startNode();
		
		NodeMain node3 = new NodeMain(new Node("Nodo3", "localhost", 8073,10), "localhost");
		node3.startNode();
		*/
	}

}
