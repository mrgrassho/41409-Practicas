package punto3.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class ServerMain {
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	public static final String RABBITMQ_CONFIG_FILE="src/main/java/punto3/resources/rabbitmq.properties";
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String inputQueueName;
	private String outputQueueName;
	private String ip;
	private int port;
	private String ipRabbit;
	private int portRabbit;
	private ServerSocket ss;
	
	public ServerMain(String ip, int port) {
		configRabbitParms();
		this.port = port;
		this.ip = ip;
		this.inputQueueName = "inputQueue";
		this.outputQueueName = "outputQueue";
		this.configureConnectionToRabbit();
		log.info(" RabbitMQ - Connection established");
	}
	
	void configRabbitParms() {
		try (InputStream input = new FileInputStream(RABBITMQ_CONFIG_FILE)) {
            Properties prop = new Properties();
            // load a properties file
            prop.load(input);
            this.ipRabbit = prop.getProperty("IP");
            this.portRabbit = Integer.parseInt(prop.getProperty("PORT"));
            this.username = prop.getProperty("USER");
            this.password = prop.getProperty("PASS");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
	}
	
	private void configureConnectionToRabbit() {
		try {
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost(this.ipRabbit);
			this.connectionFactory.setPort(this.portRabbit);
			this.connectionFactory.setUsername(this.username);
			this.connectionFactory.setPassword(this.password);
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare(this.inputQueueName, true, false, false, null);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}	
	}
	
	public void startServer() {
		try {
			ServerSocket ss = new ServerSocket (this.port);
			log.info("Server started on " + this.port);
			Random r = new Random();
			while (true) {
				Socket client = ss.accept();
				log.info("Client connected from " + client.getInetAddress().getCanonicalHostName()+":"+client.getPort());
				// Genera un ID Random de para el Thread -> TODO: Reemplazar por funcion mas potente.
				Long routingKey =  r.nextLong();
				if (routingKey < 0) routingKey *= -1;
				ThreadServer ts = new ThreadServer(client, routingKey, this.queueChannel, this.inputQueueName, log);
				log.info("Nuevo TrheadServer: "+ routingKey);
				Thread tsThread = new Thread(ts);
				tsThread.start();
			}
		} catch (IOException e) {
			log.info("Port in use!");
		}
	}

	public static void main(String[] args) {
		int thread = (int) Thread.currentThread().getId();
		String packetName = ServerMain.class.getSimpleName().toString()+"-"+thread;
		System.setProperty("log.name",packetName);
		ServerMain ss = new ServerMain("localhost",8090);
		ss.startServer();
	}
}
