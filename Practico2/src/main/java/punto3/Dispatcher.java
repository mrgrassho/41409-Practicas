package punto3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
	private String nextNodeQueueName = "nextNodeQueue";
	private String notificationQueueName = "notificationQueue";
	private String GlobalStateQueueName = "GlobalStateQueue";
	private String activesQueueName = "activeQueue";
	private String ip;
	private int port;
	private Node nodoActual;
	private ArrayList<Node> nodosActivos;
	private static final String EXCHANGE_NAME = "XCHNG-queueProcess";
	private static final String EXCHANGE_OUTPUT = "XCHNG-OUT";
	private static final String EXCHANGE_GLOBAL_LOAD = "XCHNG-GLOBAL_LOAD";
	// 80 trys in a interval of 125ms => 10 seconds per node
	private static final int RETRY_SLEEP_TIME = 125;
	private static final int MAX_RETRIES_IN_NODE = 240; 
	public Gson googleJson = new Gson();
	public GetResponse response;
	private GlobalState globalState;
	private int globalMaxLoad;
	private int globalCurrentLoad;

	public Dispatcher(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.username = "admin";
		this.password = "admin";
		this.globalState = GlobalState.GLOBAL_IDLE; 
		this.globalMaxLoad = 0;
		this.globalCurrentLoad = 0;
		this.configureConnectionToRabbit();
		this.loadNodeConfiguration();
		log.info(" RabbitMQ - Connection established");
		this.nodoActual = nodosActivos.get(0);
		this.purgeQueues();
	}

	private void loadNodeConfiguration() {
		try {
			nodosActivos = new ArrayList<Node>();
			nodosActivos.add(new Node("NodoA", "localhost", 8899, 20));
			nodosActivos.add(new Node("NodoB", "localhost", 8890, 20));
			nodosActivos.add(new Node("NodoC", "localhost", 8891, 20));
			for (Node node : nodosActivos) {
				this.queueChannel.queueDeclare(node.getName(), true, false, false, null);
			}
			String mString = googleJson.toJson(nodosActivos.get(0));
			if (this.queueChannel.messageCount(nextNodeQueueName) == 0) {
				queueChannel.basicPublish("", nextNodeQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			this.queueChannel.queueDeclare(this.GlobalStateQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.nextNodeQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.notificationQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.activesQueueName, true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}
	
	private void purgeQueues() {
		try {
			this.queueChannel.queuePurge(this.inputQueueName);
			this.queueChannel.queuePurge(this.GlobalStateQueueName);
			this.queueChannel.queuePurge(this.nextNodeQueueName);
			this.queueChannel.queuePurge(this.notificationQueueName);
			this.queueChannel.queuePurge(this.activesQueueName);
			for (Node node : nodosActivos) {
				this.queueChannel.queuePurge(node.getName());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Node getNextNode() {
		int i = nodosActivos.indexOf(nodoActual);
		i++;
		if (i == nodosActivos.size()) {
			i = 0;
		}
		return nodosActivos.get(i);
	}
	
	private Node getNextNodeSafe() {
		Node n; 
		do {
			n = getNextNode();
		} while (n.getNodeState() == NodeState.CRITICAL);
		return n;
	}

	public void msgDispatch() throws IOException {
		queueChannel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
		int MAX_RETRY = nodosActivos.size();
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			log.info(" [+] Dispatcher received msg from " + delivery.getEnvelope().getRoutingKey());
			Message message = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Message.class);
			boolean msjSent = false;
			int cntRetries = 0;
			Node n = nodoActual;
			while (!msjSent && cntRetries < MAX_RETRY) {
				// NAck the msg
				if (this.queueChannel.messageCount(nextNodeQueueName) > 0) {
					GetResponse data = this.queueChannel.basicGet(nextNodeQueueName, false);
					nodoActual = googleJson.fromJson(new String(data.getBody(),"UTF-8"), Node.class);
					log.info(" [+] Current Node -> *"+nodoActual.getName());
				}
				boolean flag = false;
				// Check if nextNode is active
				if (nodosActivos.contains(nodoActual)) {
					String mString = googleJson.toJson(message);
					queueChannel.basicPublish("", nodoActual.getName(), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
					log.info(" [+] Dispatcher sent msg to " +  nodoActual.getName());
					// Update Node Load 
					nodoActual.increaseCurrentLoad(1);
					String nString = googleJson.toJson(nodoActual);
					queueChannel.basicPublish("", activesQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, nString.getBytes("UTF-8"));;
					// Update Total Load		
					increaseGlobalCurrentLoad(1);
					this.queueChannel.exchangeDeclare(EXCHANGE_OUTPUT, BuiltinExchangeType.DIRECT);
					this.queueChannel.queueBind(notificationQueueName, EXCHANGE_OUTPUT, message.getHeader("token-id"));
					log.info(" [+] Dispatcher waiting for outputQueue notify");
					int retriesInNode = 0;
					while (retriesInNode < MAX_RETRIES_IN_NODE && !flag) {
						try {
							if (this.queueChannel.messageCount(notificationQueueName) >= 1) {
								flag = true;
							} else {
								Thread.sleep(RETRY_SLEEP_TIME);
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						retriesInNode++;
					}

					// Updates next Node
					n = getNextNodeSafe();
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
						// Update Node Load 
						nodoActual.decreaseCurrentLoad(1);
						String xString = googleJson.toJson(nodoActual);
						queueChannel.basicPublish("", activesQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, xString.getBytes("UTF-8"));;
						// Update Total Load		
						increaseGlobalCurrentLoad(1);
					} else {
						
					}
					this.queueChannel.queueUnbind(notificationQueueName, EXCHANGE_OUTPUT, message.getHeader("token-id"));	
				}
				cntRetries++;
				if (!flag) {
					if (cntRetries != MAX_RETRY) log.info(" [!] Re-sending msg to " + n.getName());
					else log.info(" [-] All Nodes are down.");
				}
			}
		};
		this.queueChannel.basicConsume(inputQueueName, true, deliverCallback, consumerTag -> {});
	}

	/*
	 * Checks Node's health and creates/remove nodes when necessary.  
	 * */
	private void healthChecker() throws IOException {
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			GlobalState gs = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), GlobalState.class);
			switch (gs) {
				case GLOBAL_CRITICAL:
					// Calculate how many Nodes need to be created to be in GLOBAL_ALERT
					// Definir nuevos Nodos
					// Agregar queueDeclares
	
					break;
				case GLOBAL_ALERT:
					// Calculate how many Nodes need to be created to be in GLOBAL_NORMAL
					// Definir nuevos Nodos
					// Agregar queueDeclares
	
					break;
				case GLOBAL_IDLE:
					// Calculate how many Nodes need to be removed to be in GLOBAL_NORMAL
					// Remover Nodos aleatoriamente
					// --> queueDelete
	
					break;
				default:
					break;
			}
		};
		queueChannel.basicConsume(GlobalStateQueueName, false, deliverCallback, consumerTag -> { });
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
	
	private void updateGlobal() throws UnsupportedEncodingException, IOException{
		// if GlobalCurrentLoad represents less than a 20% of GlobalLoad --> GlobalState is IDLE
		if (globalCurrentLoad  < ((int) globalMaxLoad * 0.2)) {
			this.globalState = GlobalState.GLOBAL_IDLE;
		// if GlobalCurrentLoad represents less than a 60% of GlobalLoad --> GlobalState is NORMAL
		} else if (globalCurrentLoad < ((int) globalMaxLoad * 0.6)) {
			this.globalState = GlobalState.GLOBAL_NORMAL;
		// if GlobalCurrentLoad represents less than a 80% of GlobalLoad --> GlobalState is ALERT
		} else if (globalCurrentLoad < ((int) globalMaxLoad * 0.8)) {
			this.globalState = GlobalState.GLOBAL_ALERT;
			// if GlobalCurrentLoad represents more than a 80% of GlobalLoad --> GlobalState is CRITICAL
		} else {
			this.globalState = GlobalState.GLOBAL_CRITICAL;
		}
		String JsonMsg = googleJson.toJson(globalState);
		if (this.queueChannel.messageCount(GlobalStateQueueName) > 0) this.queueChannel.basicGet(GlobalStateQueueName, true);
		this.queueChannel.basicPublish("", GlobalStateQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, JsonMsg.getBytes("UTF-8"));
	}
	
	public GlobalState getGlobalState() {
		return this.globalState;
	}

	private void increaseGlobalCurrentLoad(int currentLoad) {
		this.globalCurrentLoad += currentLoad;
	}
	
	private void decreaseGlobalCurrentLoad(int currentLoad) {
		this.globalCurrentLoad -= currentLoad;	
	}
	
	private void increaseGlobalMaxLoad(int maxLoad) {
		this.globalMaxLoad += maxLoad;
	}
	
	private void decreaseGlobalMaxLoad(int maxLoad) {
		this.globalMaxLoad -= maxLoad;
	}
	
	DeliverCallback cntInputDeliverCallback = (consumerTag, delivery) -> {
		long cnt = this.queueChannel.messageCount(inputQueueName);
	};

	DeliverCallback ActiveNodeDeliverCallback = (consumerTag, delivery) -> {
		Node n = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Node.class);
		int indx = nodosActivos.indexOf(n);
		if (indx != -1) {
			if (n.getNodeState() == NodeState.IDLE) {
				nodosActivos.remove(n);
				if (this.queueChannel.messageCount(n.getName()) == 0) {
					this.queueChannel.queueDelete(n.getName());
					decreaseGlobalMaxLoad(n.getMaxLoad());
				}
			} else {
				nodosActivos.set(indx, n);
				increaseGlobalMaxLoad(n.getMaxLoad());
			}
		} else {
			this.queueChannel.queueDeclare(n.getName(), true, false, false, null);
			nodosActivos.add(n);
			increaseGlobalMaxLoad(n.getMaxLoad());
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
