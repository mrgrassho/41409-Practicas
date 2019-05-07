package punto3;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;

import javax.sound.midi.MidiDevice.Info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.AMQP.Queue;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class Dispatcher {
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String inputQueueName = "inputQueue";
	private String processQueueName = "processQueue";
	private String nextNodeQueueName = "nextNodeQueue";
	private String notificationQueueName = "notificationQueue";
	private String activesQueueName = "activeQueue";
	private String ip;
	private int port;
	private Node nodoActual;
	private ArrayList<Node> nodosActivos;
	private ArrayList<Node> nodos;
	private Iterator<Node> iterNodos;
	private static final String EXCHANGE_NAME = "queueProcess";
	private static final String EXCHANGE_NOTIFICATION = "queueNotification";
	private static final int MAX_RETRIES_IN_NODE = 80;
	Gson googleJson;
	public GetResponse response;
	private GlobalState globalState;

	public Dispatcher(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.username = "admin";
		this.password = "admin";
		this.configureConnectionToRabbit();
		log.info(" RabbitMQ - Connection established");
		this.iterNodos = nodos.iterator();
	}

	private void loadNodeConfiguration(String string) {
		// TODO Auto-generated method stub
		nodos= new ArrayList<>();
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
			this.queueChannel.queueDeclare(this.nextNodeQueueName, true, true, false, null);
			this.queueChannel.queueDeclare(this.notificationQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.activesQueueName, true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	private Node getNextNode() {
		int i = nodosActivos.indexOf(nodoActual);
		i++;
		if (i > nodosActivos.size()) {
			i = 0;
		}
		return nodosActivos.get(i);
	}
	
	private Node getNextNodeSafe() {
		Node n; 
		do {
			n = getNextNode();
		} while (n.getNodeState() != NodeState.CRITICAL);
		return n;
	}

	public void msgDispatch() throws IOException {
		queueChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
		int MAX_RETRY = nodosActivos.size();
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			Message message = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Message.class);
			log.info(" [+] Dispatcher received msg from '" + delivery.getEnvelope().getRoutingKey());
			boolean msjSent = false;
			int cntRetries = 0;
			while (!msjSent && cntRetries < MAX_RETRY) {
				// NAck the msg
				GetResponse data = this.queueChannel.basicGet(nextNodeQueueName, false);
				nodoActual = googleJson.fromJson(new String(data.getBody(),"UTF-8"), Node.class);
				// Check if nextNode is active
				if (nodosActivos.contains(nodoActual)) {
					String mString = googleJson.toJson(message);
					queueChannel.basicPublish("", nodoActual.getName(), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
					log.info(" [+] Dispatcher sent msg to " +  nodoActual.getName());
					this.queueChannel.exchangeDeclare(EXCHANGE_NOTIFICATION, BuiltinExchangeType.DIRECT);
					this.queueChannel.queueBind(notificationQueueName, EXCHANGE_NOTIFICATION, message.getHeader("token-id"));
					log.info(" [+] Dispatcher waiting for notification");
					int retriesInNode = 0;
					boolean flag = false;
					while (retriesInNode < MAX_RETRIES_IN_NODE && !flag) {
						try {
							if (this.queueChannel.messageCount(notificationQueueName) >= 1) {
								flag = true;
							} else {
								Thread.sleep(125);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						retriesInNode++;
					}

					// Updates next Node
					Node n = getNextNodeSafe();
					// Ack the msg == Deletes the msg from the Queue
					this.queueChannel.basicGet(nextNodeQueueName, true);
					// Writes the next Node in NextNodeQueue
					String JsonMsg = googleJson.toJson(n);
					this.queueChannel.basicPublish("", nextNodeQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, JsonMsg.getBytes("UTF-8"));

					if (flag) {
						// Proceso Msj
						response = this.queueChannel.basicGet(notificationQueueName, true);
						msjSent = true;
						log.info(" [+] Msg notification arrived!");
					} else {
						log.info(" [+] Re-sending msg to " + n.getName()); 
					}

					this.queueChannel.queueUnbind(notificationQueueName, EXCHANGE_NOTIFICATION, message.getHeader("token-id"));
					cntRetries++;
				}
			}
		};
		this.queueChannel.basicConsume(inputQueueName, true, deliverCallback, consumerTag -> {});
	}

	/*
	 * Checks Node's health and creates/remove nodes when necessary.  
	 * */	
	private void healthChecker() throws IOException {
		new Thread()
		{
		    public void run() {
		    		
		    }
		}.start();
	}

	public void startServer() {
		log.info(" Dispatcher Started");
		try {
			// Init RabbitMQ Thread which listens to InputQueue 
			this.msgDispatch();
			// Init RabbitMQ Thread that listens to ActiveQueue
			this.healthChecker();
			this.queueChannel.basicConsume(inputQueueName, false, cntInputDeliverCallback, consumerTag -> {});
			// Listens node's activesQueue
			this.queueChannel.basicConsume(activesQueueName, false, ActiveNodeDeliverCallback, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void updateGlobalState(int cnt){
		for (Node node : nodosActivos) {
			
		}
	}
	
	DeliverCallback cntInputDeliverCallback = (consumerTag, delivery) -> {
		long cnt = this.queueChannel.messageCount(inputQueueName);
		//updateGlobalState(cnt);
	};

	DeliverCallback ActiveNodeDeliverCallback = (consumerTag, delivery) -> {
		Node n = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Node.class);
		int indx = nodosActivos.indexOf(n);
		if (indx != -1) {
			if (n.getNodeState() == NodeState.IDLE) {
				nodosActivos.remove(n);
				if (this.queueChannel.messageCount(n.getName()) == 0) {
					this.queueChannel.queueDelete(n.getName());
				}
			} else {
				nodosActivos.set(indx, n);
			}
		} else {
			this.queueChannel.queueDeclare(n.getName(), true, false, false, null);
			nodosActivos.add(n);
		}
	};
	

	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = Dispatcher.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		Dispatcher ss = new Dispatcher("localhost",9090);
		ss.startServer();
	}

}
