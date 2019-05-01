package punto3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class ServerMain {
	private final Logger log = LoggerFactory.getLogger(ServerMain.class);
	private String username;
	private String password;
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;
	private String inputQueue;
	private String outputQueue;
	private Gson googleJson;
	private String ip;
	private int port;
	private ServerSocket ss;
	
	public ServerMain(String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.username = "admin";
		this.password = "admin";
		this.inputQueue = "InputQueue";
		this.outputQueue = "OutputQueue";
		this.configureConnectionToRabbit();
		log.info(" RabbitMQ - Connection established");
		this.configureServer();
	}
	
	private void configureConnectionToRabbit() {
		
		try {
			this.googleJson = new Gson();
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost(this.ip);
			this.connectionFactory.setUsername(this.username);
			this.connectionFactory.setPassword(this.password);
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare(this.inputQueue, true, false, false, null);
			this.queueChannel.queueDeclare(this.outputQueue, true, false, false, null);
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
			while (true) {
				Socket client = ss.accept();
				log.info("Client connected from " + client.getInetAddress().getCanonicalHostName()+" : "+client.getPort());
				ThreadServer ts = new ThreadServer(client, this.queueChannel, this.inputQueue, this.outputQueue);
				Thread tsThread = new Thread (ts);
				tsThread.start();
			}
		} catch (IOException e) {
			log.info("Port in use!");
		}
	}

	public static void main(String[] args) {
		ServerMain ss = new ServerMain("localhost",8090);
		ss.startServer();
	}
}
