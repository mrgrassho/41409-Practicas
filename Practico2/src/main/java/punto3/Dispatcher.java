package punto3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import javax.sound.midi.MidiDevice.Info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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
	private String outputQueueName = "outputQueue";
	private String notificationQueueName = "notificationQueue";
	private String GlobalStateQueueName = "GlobalStateQueue";
	private String activesQueueName = "activeQueue";
	private String inprocessQueueName = "inProcessQueue";
	private String ip;
	private int port;
	private Node nodoActual;
	private ArrayList<Node> nodosActivos;
	private static final String EXCHANGE_OUTPUT = "XCHNG-OUT";
	private static final String EXCHANGE_NOTIFY = "XCHNG-NOTIFY";
	private static final String EXCHANGE_GLOBAL_LOAD = "XCHNG-GLOBAL_LOAD";
	// 1000 trys in a interval of 10ms => 10 seconds per node
	private static final int RETRY_SLEEP_TIME = 10;
	private static final int MAX_RETRIES_IN_NODE = 1000;
	public Gson googleJson;
	public GetResponse response;
	private GlobalState globalState;
	private int globalMaxLoad;
	private int globalCurrentLoad;

	private static final ArrayList<String> DICCIONARIO = new ArrayList<String>( Arrays.asList(
			"NodoA", "NodoB", "NodoC", "NodoD", "NodoE", "NodoF", "NodoG", "NodoH",
			"NodoI", "NodoJ", "NodoK", "NodoL", "NodoM", "NodoN", "NodoO", "NodoP",
			"NodoQ", "NodoR", "NodoS", "NodoT", "NodoU", "NodoV", "NodoW", "NodoX",
			"NodoY", "NodoZ"
	));

	private EstadoCreador estadoCreador = EstadoCreador.IDLE;

	public Dispatcher(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.username = "admin";
		this.password = "admin";
		this.globalState = GlobalState.GLOBAL_IDLE;
		this.globalMaxLoad = 0;
		this.globalCurrentLoad = 0;
		googleJson = new GsonBuilder()
		        .registerTypeAdapter(Service.class, new InterfaceSerializer(ServiceResta.class))
		        .registerTypeAdapter(Service.class, new InterfaceSerializer(ServiceSuma.class))
		        .create();
		this.configureConnectionToRabbit();
		this.loadNodeConfiguration();
		log.info(" RabbitMQ - Connection established");
		this.purgeQueues();

	}

	/*CARGA CONFIGURACION RABBIT: Declara conexion, canal y colas que va a usar Dispatcher*/
	private void configureConnectionToRabbit() {
		try {
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost(this.ip);
			this.connectionFactory.setUsername(this.username);
			this.connectionFactory.setPassword(this.password);
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare(this.inputQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.outputQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.GlobalStateQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.notificationQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.inprocessQueueName, true, false, false, null);
			this.queueChannel.queueDeclare(this.activesQueueName, true, false, false, null);
			this.queueChannel.exchangeDeclare(EXCHANGE_NOTIFY, BuiltinExchangeType.DIRECT);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	/*CARGA NODOS: Los que empezaran como activos, con una cola de proceso para cada nodo, la cual se llama igual que el nodo*/
	private void loadNodeConfiguration() {
		try {
			nodosActivos = new ArrayList<Node>();
			//empieza con tres nodos activos, luego ira llamando a los otros bajo demanda
			nodosActivos.add(new Node("NodoA", "localhost", 8899, 20));
			nodosActivos.add(new Node("NodoB", "localhost", 8890, 20));
			nodosActivos.add(new Node("NodoC", "localhost", 8891, 20));
			for (int i = 0; i < nodosActivos.size(); i++) {
				nodosActivos.get(i).addService(new ServiceResta(0, "resta"));
				nodosActivos.get(i).addService(new ServiceSuma(0, "suma"));
			}
			for (Node node : nodosActivos) {
				this.queueChannel.queueDeclare(node.getName(), true, false, false, null);
				this.increaseGlobalCurrentLoad(node.getCurrentLoad());
				this.increaseGlobalMaxLoad(node.getMaxLoad());
			}
			updateGlobal();
			String mString = googleJson.toJson(nodosActivos.get(0));
			this.nodoActual = nodosActivos.get(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*PURGAR COLAS: Elimina todos los mensajes que puedan haber en ellas de antes.*/
	private void purgeQueues() {

		try {
			this.queueChannel.queuePurge(this.inputQueueName);
			this.queueChannel.queuePurge(this.GlobalStateQueueName);
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

	/*Devuelve el siguiente nodo al que se le intentará asignar una tarea*/
	private synchronized Node getNextNode() {
		int i = nodosActivos.indexOf(nodoActual);
		i = (i + 1) % nodosActivos.size();
		return nodosActivos.get(i);
	}

	/*Devuelve el siguiente nodo en condiciones de recibir la proxima tarea*/
	private Node getNextNodeSafe(String name) throws Exception {
		int len = nodosActivos.size();
		int i = nodosActivos.indexOf(nodoActual);
		Node n = getNextNode();
		do {
			n = nodosActivos.get(i);
			log.info("COMPARING " + name + " AND " + googleJson.toJson(n) + ", RESULT --> "+ n.hasService(name));
			log.info("NodosActivos[" + i + "] -> len=" + len);
			if (len < 0) throw new Exception("Servicio no disponible.");
			len--;
			i = (i + 1) % nodosActivos.size();
		//} while (n.getNodeState().equals(NodeState.CRITICAL) && !n.hasService(name) );
		} while (!n.hasService(name) );
		return n;
	}
	
	private void setNodeByName(String name, Node n) {
		boolean NotFound = true;
		synchronized (nodosActivos) {
			for (Node node : nodosActivos) {
				if (node.getName().equals(name)) {
					node = n;
					NotFound = false;
					break;
				} 
			}
		}
		if (NotFound) {log.info(" [+] Dispatcher NOT found ["+ name + "] in nodosActivos");}
	}

	private Node findNodeByName(String name) {
		Node n = null;
		synchronized (nodosActivos) {
			for (Node node : nodosActivos) {
				if (node.getName().equals(name)) {
					n = node;
					break;
				}
			}
		}
		if (n==null) {log.info(" [+] Dispatcher NOT found ["+ name + "] in nodosActivos");}
		return n;
	}

	private DeliverCallback msgProcess = (consumerTag, delivery) -> { //when received message from InProcessQueue
		int MAX_RETRY = nodosActivos.size();
		String json;
		Message message = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Message.class);
		//this.queueChannel.exchangeDeclare(EXCHANGE_OUTPUT, BuiltinExchangeType.DIRECT);
		//this.queueChannel.queueBind(notificationQueueName, EXCHANGE_OUTPUT, message.getHeader("token-id"));
		//log.info(" [+] Dispatcher declared new bind> notificationQueue | Exchange '" + EXCHANGE_NOTIFY + "' | RoutingKey '"+message.getHeader("token-id")+"'" );
		log.info(" [msgProcess] waiting for notification...");
		int retriesInNode = 0;
		boolean flag = false;
		while (retriesInNode < MAX_RETRIES_IN_NODE && !flag) {
			if (this.queueChannel.messageCount(notificationQueueName) >= 1) {
				this.queueChannel.basicGet(notificationQueueName, true);
				flag = true;  // a ThreadNode finished his task
			} else {
				try {
					Thread.sleep(RETRY_SLEEP_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			retriesInNode++;
		}
		String nodeName = message.getHeader("to-node");
		Node n = findNodeByName(nodeName);
		if (n != null) {
			if (flag) { //[node finished his task]
				log.info(" [msgProcess] Msg notification arrived from ["+ nodeName + "]!");
				// Update Node Load
				n.decreaseCurrentLoad();
				log.info(" [+] currentLoad ["+ nodeName + "]: " + n.getCurrentLoad());
				// Actualizamos Activos en memoria
				synchronized (nodosActivos) {
					this.setNodeByName(nodeName, n);
				}
			} else {

				if (null != message.getHeader("redir-qty")) {
					int redir = Integer.parseInt(message.getHeader("redir-qty"));
					message.setHeader("redir-qty", String.valueOf(redir+1));
				} else {
					message.setHeader("redir-qty", "1");
				}

				// Re send the message to inputQueue
				log.info(" [!] Msg notification NOT arrived  from ["+ nodeName + "] - TIMEOUT REACHED!");
				// Update Node Load
				n.decreaseCurrentLoad();
				n.setNodeState(NodeState.DEAD);
				json = googleJson.toJson(n);
				queueChannel.basicPublish("", activesQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));;
				json = googleJson.toJson(message);
				queueChannel.basicPublish("", inputQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));;
			}
			// Update Total Load
			decreaseGlobalCurrentLoad();
			log.info(" [+] GlobalCurrentLoad ["+ this.globalCurrentLoad);
			this.queueChannel.queueUnbind(notificationQueueName, EXCHANGE_NOTIFY, message.getHeader("token-id"));
			log.info(" [+] Dispatcher unbind> notificationQueue | Exchange '" + EXCHANGE_NOTIFY + "' | RoutingKey '"+message.getHeader("token-id")+"'" );
		}
	};


	private DeliverCallback msgDispatch = (consumerTag, delivery) -> {
		String json;
		log.info(" [msgDispatch] received msg from " + delivery.getEnvelope().getRoutingKey());
		log.info(new String(delivery.getBody(), "UTF-8"));
		Message message = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Message.class);//message from ThreadServer
		try {
			Node n = getNextNodeSafe(message.getFunctionName());
			synchronized (this.nodoActual) {
				this.nodoActual = getNextNode();
			}
			// Add destination Node
			message.setHeader("to-node", n.getName());
			// Send Msg to InProcessQueue
			json = googleJson.toJson(message);
			queueChannel.basicPublish("", inprocessQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));;
			log.info(" [msgDispatch] sent msg to inProcessQueue");

			this.queueChannel.queueBind(notificationQueueName, EXCHANGE_NOTIFY, message.getHeader("token-id"));
			log.info(" [msgDispatch] Dispatcher declared new bind> notificationQueue | Exchange '" + EXCHANGE_NOTIFY + "' | RoutingKey '"+message.getHeader("token-id")+"'" );
			// Send Msg to Node Queue
			json = googleJson.toJson(message);
			queueChannel.basicPublish("", n.getName(), MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));
			log.info(" [msgDispatch] sent msg to " +  n.getName());
			// Update Node Load
			n.increaseCurrentLoad();
			json = googleJson.toJson(n);
			log.info(" [+] currentLoad ["+ n.getName() + "]: " + n.getCurrentLoad());
			updateGlobal();
			setNodeByName(n.getName(), n);
			// Update Total Load
			increaseGlobalCurrentLoad();
			log.info(" [+] GlobalCurrentLoad ["+ this.globalCurrentLoad+"]");

		}  catch (Exception e) {
			log.info(" [-] Service Not found.");
			String xString = googleJson.toJson(new Message("Service Not found."));
			//queueChannel.basicPublish("", message.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, xString.getBytes("UTF-8"));
		}
	};

	private void createNodos(int k) throws UnsupportedEncodingException, IOException {
		String json = "{}";
		int i = DICCIONARIO.indexOf(nodosActivos.get(nodosActivos.size()-1).getName());
		log.info("i -> " + i + ",k -> " + k);
		for (int j = i+1; j < k+1+i; j++) {
			log.info(" [NODE-BUILDER] Creating Node -> " + DICCIONARIO.get(j));
			Random r = new Random();
			Node n = new Node(DICCIONARIO.get(j), null, r.nextInt(1000000), 20);
			log.info(" [NODE-BUILDER] " + n.getPort());
			n.addService(new ServiceResta(0, "resta"));
			n.addService(new ServiceSuma(0, "suma"));
			synchronized (nodosActivos) {
				nodosActivos.add(n);
			}
			increaseGlobalMaxLoad(n.getMaxLoad());
			this.queueChannel.queueDeclare(n.getName(), true, false, false, null);
		}
	}

	private void removeNodos(int k) throws UnsupportedEncodingException, IOException {
		String json = "{}";
		int i = 0;
		for (int j = i; j < k; j++) {
			if (nodosActivos.get(j).getNodeState() == NodeState.IDLE) {
				log.info(" [NODE-REMOVER] - Removing " + nodosActivos.get(j).getName());
				Node node3 = nodosActivos.get(j);
				// Al enviarlo en estado DEAD, el thread activesNode lo borra.
				node3.setNodeState(NodeState.DEAD);
				json = googleJson.toJson(node3);
				queueChannel.basicPublish("", activesQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));;
			}
		}
	}

	/*
	 * Checks Node's health and creates/remove nodes when necessary.
	 * */
	private void healthChecker() throws IOException {
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			GlobalState gs = getGlobalState();
			log.info(" [HEALTH_CHECKER] Global State -> " + gs + " | CURRNT_LOAD:" + this.getGlobalCurrentLoad() + " | MAX_LOAD:"+ this.getGlobalMaxLoad());
			if (estadoCreador.equals(EstadoCreador.IDLE)){
				log.info(" [HEALTH_CHECKER] ");
				int k = 0;
				switch (gs) {
				case GLOBAL_CRITICAL:
					// Calculate how many Nodes need to be created to be in GLOBAL_ALERT
					// Definir nuevos Nodos
					// Agregar queueDeclares

					k = nodosActivos.size()/2;
					k = (k < 1) ? 1: k;
					createNodos(k);
					break;
				case GLOBAL_ALERT:
					// Calculate how many Nodes need to be created to be in GLOBAL_NORMAL
					// Definir nuevos Nodos
					// Agregar queueDeclares
					k = nodosActivos.size()/4;
					k = (k < 1) ? 1: k;
					createNodos(k);
					break;
				case GLOBAL_IDLE:
					// Calculate how many Nodes need to be removed to be in GLOBAL_NORMAL
					// Remover Nodos aleatoriamente
					// --> queueDelete
					if (nodosActivos.size() > 3) {
						k = (nodosActivos.size() > 5) ? nodosActivos.size()/2 : 1;
						removeNodos(k);
					}
					break;
				default:
					break;
				}
				estadoCreador = EstadoCreador.IDLE;
				try {
					Thread.sleep(8000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		};
		queueChannel.basicConsume(GlobalStateQueueName, true, deliverCallback, consumerTag -> { });
	}

	private void updateGlobal() throws UnsupportedEncodingException, IOException{
		// if GlobalCurrentLoad represents less than a 20% of GlobalLoad --> GlobalState is IDLE
		log.info(" [UPDATE_GLOBAL] CURR: " + getGlobalCurrentLoad() + " - MAX:" + getGlobalMaxLoad());
		if (getGlobalCurrentLoad()  < ((int) getGlobalMaxLoad() * 0.2)) {
			this.globalState = GlobalState.GLOBAL_IDLE;
		// if GlobalCurrentLoad represents less than a 60% of GlobalLoad --> GlobalState is NORMAL
		} else if (getGlobalCurrentLoad() < ((int) getGlobalMaxLoad() * 0.6)) {
			this.globalState = GlobalState.GLOBAL_NORMAL;
		// if GlobalCurrentLoad represents less than a 80% of GlobalLoad --> GlobalState is ALERT
		} else if (getGlobalCurrentLoad() < ((int) getGlobalMaxLoad() * 0.8)) {
			this.globalState = GlobalState.GLOBAL_ALERT;
			// if GlobalCurrentLoad represents more than a 80% of GlobalLoad --> GlobalState is CRITICAL
		} else {
			this.globalState = GlobalState.GLOBAL_CRITICAL;
		}
		log.info(" [UPDATE_GLOBAL] NEW ESTATE +=> " + this.globalState);
		String JsonMsg = googleJson.toJson(globalState);
		if (this.queueChannel.messageCount(GlobalStateQueueName) > 0) this.queueChannel.basicGet(GlobalStateQueueName, true);
		this.queueChannel.basicPublish("", GlobalStateQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, JsonMsg.getBytes("UTF-8"));
	}

	private DeliverCallback ActiveNodeDeliverCallback = (consumerTag, delivery) -> {
		Node updateNode = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), Node.class);
		Node previousNode = findNodeByName(updateNode.getName());
		log.info("[AQ] - NEW -> " + updateNode.getName() + " - " + updateNode.getPort());
		if (previousNode != null) {
			log.info("[AQ] - OLD -> " + previousNode.getName());
			log.info("[AQ] - " + updateNode.getName() + " - Load:"+ updateNode.getPercentageLoad());
			if (previousNode.getName().equals(updateNode.getName())) {
				nodosActivos.set(nodosActivos.indexOf(previousNode), updateNode);
				decreaseGlobalMaxLoad(previousNode.getMaxLoad());
				increaseGlobalMaxLoad(updateNode.getMaxLoad());
				if (updateNode.getNodeState() == NodeState.DEAD) {
					synchronized (nodosActivos) {
						nodosActivos.remove(previousNode);
					}
					this.queueChannel.queueDelete(previousNode.getName());
				}
			}
		} else {
			log.info("[AQ] - OLD -> NULL");
			log.info("[AQ] - Creating Queue for " + updateNode.getName());
			this.queueChannel.queueDeclare(updateNode.getName(), true, false, false, null);
			synchronized (nodosActivos) {
				nodosActivos.add(updateNode);
			}
			increaseGlobalMaxLoad(updateNode.getMaxLoad());
		}
		updateGlobal();
	};

	public GlobalState getGlobalState() {
		return this.globalState;
	}

	private void increaseGlobalCurrentLoad(int currentLoad) {
		this.globalCurrentLoad += currentLoad;
	}

	private void increaseGlobalCurrentLoad() {
		this.globalCurrentLoad ++;
	}

	private void decreaseGlobalCurrentLoad(int currentLoad) {
		this.globalCurrentLoad -= currentLoad;
	}

	private void decreaseGlobalCurrentLoad() {
		this.globalCurrentLoad--;
	}

	private void increaseGlobalMaxLoad(int maxLoad) {
		this.globalMaxLoad += maxLoad;
	}

	private void increaseGlobalMaxLoad() {
		this.globalMaxLoad++;
	}

	private void decreaseGlobalMaxLoad(int maxLoad) {
		this.globalMaxLoad -= maxLoad;
	}

	private void decreaseGlobalMaxLoad() {
		this.globalMaxLoad--;
	}

	public int getGlobalMaxLoad() {
		return globalMaxLoad;
	}

	public void setGlobalMaxLoad(int globalMaxLoad) {
		this.globalMaxLoad = globalMaxLoad;
	}

	public int getGlobalCurrentLoad() {
		return globalCurrentLoad;
	}

	public void setGlobalCurrentLoad(int globalCurrentLoad) {
		this.globalCurrentLoad = globalCurrentLoad;
	}

	public void startServer() {
		log.info(" Dispatcher Started");
		try {
			queueChannel.exchangeDeclare(EXCHANGE_OUTPUT, "fanout");
			queueChannel.queueBind(this.notificationQueueName, EXCHANGE_OUTPUT, "");
			queueChannel.queueBind(this.outputQueueName, EXCHANGE_OUTPUT, "");
			// Init RabbitMQ Thread which listens to InputQueue
			this.queueChannel.basicConsume(inputQueueName, true, msgDispatch, consumerTag -> {});
			// Init RabbitMQ Thread which make sure that the message is attend it.
			this.queueChannel.basicConsume(inprocessQueueName, true, msgProcess, consumerTag -> {});
			// Listens node's activesQueue
			this.queueChannel.basicConsume(activesQueueName, true, ActiveNodeDeliverCallback, consumerTag -> {});
			// Init RabbitMQ Thread that listens and updates to ActiveQueue
			this.healthChecker();
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
