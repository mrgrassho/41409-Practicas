package punto3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
	public final Logger log = LoggerFactory.getLogger(Dispatcher.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	public String inputQueueName = "inputQueue";
	public String outputQueueName = "outputQueue";
	public String notificationQueueName = "notificationQueue";
	public String GlobalStateQueueName = "GlobalStateQueue";
	public String activesQueueName = "activeQueue";
	public String inprocessQueueName = "inProcessQueue";
	public String ip;
	private Map<String, Thread> threads;

	private int port;
	private Node nodoActual;
	private ArrayList<Node> nodosActivos;
	public static final String EXCHANGE_OUTPUT = "XCHNG-OUT";
	public static final String EXCHANGE_NOTIFY = "XCHNG-NOTIFY";
	public static final String EXCHANGE_GLOBAL_LOAD = "XCHNG-GLOBAL_LOAD";
	// 1000 trys in a interval of 10ms => 10 seconds per node
	public static final int RETRY_SLEEP_TIME = 10;
	public static final int MAX_RETRIES_IN_NODE = 1000;
	public Gson googleJson;
	public GetResponse response;
	private GlobalState globalState;
	private volatile int globalMaxLoad;
	private volatile int globalCurrentLoad;

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
		threads = new HashMap<String,Thread>();

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
				this.queueChannel.queueDeclare(node.getName()+"inProcess", true, false, false, null);
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
	
	public void setNodeByName(String name, Node n) {
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

	public Node findNodeByName(String name) {
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
			this.queueChannel.queueBind(notificationQueueName, EXCHANGE_NOTIFY, message.getHeader("token-id"));
			log.info(" [msgDispatch] Dispatcher declared new bind> notificationQueue | Exchange '" + EXCHANGE_NOTIFY + "' | RoutingKey '"+message.getHeader("token-id")+"'" );
			// Send Msg to InProcessQueue
			json = googleJson.toJson(message);
			queueChannel.basicPublish("", n.getName()+"inProcess", MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));;
			log.info(" [msgDispatch] sent msg to " + n.getName()+"inProcess");
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

	public void createNodos(int k) throws UnsupportedEncodingException, IOException {
		String json = "{}";
		int i = DICCIONARIO.indexOf(nodosActivos.get(nodosActivos.size()-1).getName());
		log.info("i -> " + i + ",k -> " + k);
		for (int j = i+1; j < k+1+i; j++) {
			if (DICCIONARIO.size() > j) {
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
				this.queueChannel.queueDeclare(n.getName()+"inProcess", true, false, false, null);
				MessageProcessor msg = new MessageProcessor(this, n.getName()+"inProcess");
				threads.put(n.getName(), new Thread(msg));
				threads.get(n.getName()).start();
			}
		}
		updateGlobal();
	}

	private void removeNodos(int k) throws UnsupportedEncodingException, IOException {
		String json = "{}";
		int i = nodosActivos.size()-1;
		int m = nodosActivos.size() - k;
		m = (m < 0) ? 0 : m;
		for (int j = i; j >= m; j--) {
			if (nodosActivos.get(j).getNodeState().equals(NodeState.IDLE)) {
				log.info(" [NODE-REMOVER] - Removing " + nodosActivos.get(j).getName());
				Node n = nodosActivos.get(j);
				// Al enviarlo en estado DEAD, el thread activesNode lo borra.
				decreaseGlobalMaxLoad(n.getMaxLoad());
				decreaseGlobalCurrentLoad(n.getCurrentLoad());
				synchronized (nodosActivos) {
						nodosActivos.remove(n);
				}
				this.queueChannel.queueDelete(n.getName());	
				this.queueChannel.queueDelete(n.getName()+"inProcess");
				//threads.get(n.getName()).interrupt();
			}
		}
		updateGlobal();
	}

	/*
	 * Checks Node's health and creates/remove nodes when necessary.
	 * */
	private void healthChecker() throws IOException {
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			log.info(" [HEALTH_CHECKER] Global State -> " + this.getGlobalState() + " | CURRNT_LOAD:" + this.getGlobalCurrentLoad() + " | MAX_LOAD:"+ this.getGlobalMaxLoad());
			if (estadoCreador.equals(EstadoCreador.IDLE)){
				estadoCreador = EstadoCreador.WORKING;
				int k = 0;
				switch (this.getGlobalState()) {
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
						//k = (nodosActivos.size() > 5) ? nodosActivos.size()/2 : 1;
						//removeNodos(k);
						removeNodos(1);
					}
					break;
				default:
					break;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				estadoCreador = EstadoCreador.IDLE;
			}

		};
		queueChannel.basicConsume(GlobalStateQueueName, true, deliverCallback, consumerTag -> { });
	}

	public void updateGlobal() throws UnsupportedEncodingException, IOException{
		// if GlobalCurrentLoad represents less than a 20% of GlobalLoad --> GlobalState is IDLE
		log.info(" [UPDATE_GLOBAL] CURR: " + getGlobalCurrentLoad() + " - MAX:" + getGlobalMaxLoad());
		if (getGlobalCurrentLoad()  < ((int) getGlobalMaxLoad() * 0.2)) {
			this.globalState = GlobalState.GLOBAL_IDLE;
		// if GlobalCurrentLoad represents less than a 50% of GlobalLoad --> GlobalState is NORMAL
		} else if (getGlobalCurrentLoad() < ((int) getGlobalMaxLoad() * 0.5)) {
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
	
	public Channel getQueueChannel() {
		return queueChannel;
	}

	public void setQueueChannel(Channel queueChannel) {
		this.queueChannel = queueChannel;
	}

	public Node getNodoActual() {
		return nodoActual;
	}

	public void setNodoActual(Node nodoActual) {
		this.nodoActual = nodoActual;
	}

	public ArrayList<Node> getNodosActivos() {
		return nodosActivos;
	}

	public void setNodosActivos(ArrayList<Node> nodosActivos) {
		this.nodosActivos = nodosActivos;
	}

	public Gson getGoogleJson() {
		return googleJson;
	}

	public void setGoogleJson(Gson googleJson) {
		this.googleJson = googleJson;
	}

	public synchronized GlobalState getGlobalState() {
		return this.globalState;
	}

	private void increaseGlobalCurrentLoad(int currentLoad) {
		this.globalCurrentLoad += currentLoad;
	}

	private void increaseGlobalCurrentLoad() {
		this.globalCurrentLoad ++;
	}

	private void decreaseGlobalCurrentLoad(int currentLoad) {
		this.globalCurrentLoad = (this.globalCurrentLoad > 0) ? this.globalCurrentLoad - currentLoad : 0;
	}

	public void decreaseGlobalCurrentLoad() {
		this.decreaseGlobalCurrentLoad(1);
	}

	private void increaseGlobalMaxLoad(int maxLoad) {
		this.globalMaxLoad += maxLoad;
	}

	private void increaseGlobalMaxLoad() {
		this.globalMaxLoad++;
	}

	public void decreaseGlobalMaxLoad(int maxLoad) {
		this.globalMaxLoad = (this.globalMaxLoad > 0) ? this.globalMaxLoad - maxLoad : 0;
	}

	private void decreaseGlobalMaxLoad() {
		this.decreaseGlobalMaxLoad(1);
	}

	public synchronized int getGlobalMaxLoad() {
		return globalMaxLoad;
	}

	public void setGlobalMaxLoad(int globalMaxLoad) {
		this.globalMaxLoad = globalMaxLoad;
	}

	public synchronized int getGlobalCurrentLoad() {
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
			// Init RabbitMQ Thread which listens to InputQueue
			this.queueChannel.basicConsume(inputQueueName, true, msgDispatch, consumerTag -> {});
			// Init RabbitMQ Thread which make sure that the message is attend it.
			//this.queueChannel.basicConsume(inprocessQueueName, true, msgProcess, consumerTag -> {});
			initMsgProcess();
			// Listens node's activesQueue
			this.queueChannel.basicConsume(activesQueueName, true, ActiveNodeDeliverCallback, consumerTag -> {});
			// Init RabbitMQ Thread that listens and updates to ActiveQueue
			this.healthChecker();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				queueChannel.queueUnbind(this.notificationQueueName, EXCHANGE_OUTPUT, "");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private void initMsgProcess() {
		for (Node n : nodosActivos) {
			MessageProcessor msg = new MessageProcessor(this, n.getName()+"inProcess");
			threads.put(n.getName(),new Thread(msg));
			threads.get(n.getName()).start();
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
