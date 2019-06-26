package punto3.node;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
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
import punto3.core.Message;
import punto3.server.ServerMain;


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
	private String myNodeQueueName;
	private String notificationQueueName;
	public static final String RABBITMQ_CONFIG_FILE="src/main/java/punto3/resources/rabbitmq.properties";

	private String ipRabbitMQ;
	private int portRabbitMQ;
	private Node node;
	private Gson googleJson;
	private int max_tasks;

	private static final ArrayList<String> DICCIONARIO = new ArrayList<String>(Arrays.asList(
			"NodoA", "NodoB", "NodoC", "NodoD", "NodoE", "NodoF", "NodoG", "NodoH",
			"NodoI", "NodoJ", "NodoK", "NodoL", "NodoM", "NodoN", "NodoO", "NodoP",
			"NodoQ", "NodoR", "NodoS", "NodoT", "NodoU", "NodoV", "NodoW", "NodoX",
			"NodoY", "NodoZ"
			));

	public Node getNode() {
		return this.node;
	}

	public NodeMain(Node node) {
		this.node = node;
		configRabbitParms();
		this.activesQueueName = "activeQueue";
		this.outputQueueName = "outputQueue";
		this.notificationQueueName = "notificationQueue";
		this.myNodeQueueName = this.node.getName();
		googleJson = new Gson();
		this.configureConnectionToRabbit();
	}

	void configRabbitParms() {
		try (InputStream input = new FileInputStream(RABBITMQ_CONFIG_FILE)) {
			Properties prop = new Properties();
			// load a properties file
			prop.load(input);
			this.ipRabbitMQ = prop.getProperty("IP");
			this.portRabbitMQ = Integer.parseInt(prop.getProperty("PORT"));
			this.username = prop.getProperty("USER");
			this.password = prop.getProperty("PASS");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void configureConnectionToRabbit() {
		try {
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost(this.ipRabbitMQ);
			this.connectionFactory.setPort(this.portRabbitMQ);
			this.connectionFactory.setUsername(this.username);
			this.connectionFactory.setPassword(this.password);
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare(this.outputQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.activesQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.myNodeQueueName, true, false, false, null);
		} catch (ConnectException e) {
			System.err.println("[!] RabbitMQ Server is down.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	public void startNode() {
		try {
			if (queueChannel != null) {
				log.info(this.node.getName()+" Started");
				queueChannel.exchangeDeclare(EXCHANGE_OUTPUT, "fanout");
				queueChannel.queueBind(notificationQueueName, EXCHANGE_OUTPUT, "");
				DeliverCallback deliverCallback = (consumerTag, delivery) -> {
					Message message = googleJson.fromJson(new String(delivery.getBody(), "UTF-8"), Message.class);
					log.info("["+ delivery.getEnvelope().getRoutingKey() + "] received: " + googleJson.toJson(message));
					//Asigna Tarea a Thread
					log.info("["+this.node.getName()+"] Working...");
					//asigno la tarea a un thread
					Random r = new Random();
					ThreadNode tn = new ThreadNode(r.nextLong(), this.node,Long.parseLong(message.getHeader("token-id")),message, queueChannel,EXCHANGE_OUTPUT,log);
					Thread nodeThread = new Thread(tn);
					nodeThread.start();
				};
				queueChannel.basicConsume(this.myNodeQueueName, true, deliverCallback, consumerTag -> {});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);


		//CREO LOS DIFERENTES SERVICIOS QUE VA A TENER CADA UNO (AGREGAR RESTA, MULTIPLICACION, MOD, etc )
		//Service suma = (Service) new ServiceSuma(8071,"suma"); // DUDA: cuando se le pide el puerto al servicio?
		//Service resta = (Service) new ServiceResta(8071,"resta");
		
		ArrayList<NodeMain> node = new ArrayList<NodeMain>();
		int i =0;
		for (String Nodo : DICCIONARIO) {
			node.add(new NodeMain(new Node(Nodo, "localhost", 8071,20)));
			node.get(i).node.addService(new ServiceSuma(8071,"suma"));
			node.get(i).node.addService(new ServiceResta(8072,"resta"));
			if (i % 3 != 0) {
				node.get(i).startNode();
			}
			i++;
		}
		

	}

}
