package punto3;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import javax.sound.midi.MidiDevice.Info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

public class Dispatcher {
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String inputQueueName;
	private String processQueueName;
	private String ip;
	private int port;
	private int nodoActual = 0;
	private ArrayList<Node> nodos;
	private Iterator<Node> iterNodos;
	private static final String EXCHANGE_NAME = "queueProcess";

	public Dispatcher(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.username = "admin";
		this.password = "admin";
		this.inputQueueName = "inputQueue";
		this.processQueueName = "processQueue";
		this.configureConnectionToRabbit();
		log.info(" RabbitMQ - Connection established");
		loadNodeConfiguration("../resources/nodes.config");
		this.iterNodos = nodos.iterator();
	}

	private void loadNodeConfiguration(String string) {
		// TODO Auto-generated method stub
		nodos= new ArrayList<>();
		nodos.add(new Node("Nodo1", "localhost", 7871));
		nodos.add(new Node("Nodo2", "localhost", 7872));
	}

	private void configureConnectionToRabbit() {
		try {
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost(this.ip);
			this.connectionFactory.setUsername(this.username);
			this.connectionFactory.setPassword(this.password);
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare(this.inputQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.processQueueName, true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	public void startServer() {
		log.info(" Dispatcher Started");
		try {
			queueChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
			
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), "UTF-8");
				log.info(" [+] Dispatcher received '" + delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
				
				// Aplica Logica Round Robin
				// Chequea estado nodo Actual si esta por debajo de los rangos asigna tarea
				Node n = new Node("Nodo1", "localhost", 8071);
				// declara Cola del Nodo
				
				queueChannel.basicPublish(EXCHANGE_NAME, n.getName(), MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes("UTF-8"));
				log.info(" [+] Dispatcher sent a msg to " +  n.getName());
			};
			this.queueChannel.basicConsume(inputQueueName, true, deliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = Dispatcher.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		Dispatcher ss = new Dispatcher("localhost",9090);
		ss.startServer();
	}

}
