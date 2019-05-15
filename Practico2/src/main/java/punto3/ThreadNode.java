package punto3;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

public class ThreadNode implements Runnable {
	
	private Channel queueChannel;
	private String activeQueueName;
	private String outputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private Message task;
	private Node node;
	private static final String EXCHANGE_OUTPUT = "XCHNG-OUT";
	
	public ThreadNode(Node node, Long routingKey, Message message, Channel queueChannel, String activeQueueName, String outputQueueName, Logger log) {
		this.node = node;
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.activeQueueName = activeQueueName;
		this.outputQueueName = outputQueueName;
		this.task = message;
		this.googleJson = new Gson();
		this.log = log;
	}

	public void run() {
		try {
			//--------------harcodeado,  enrealidad tiene que buscar a que servicio corresponde llamar
			Service s = this.node.services.get(0); 
			ArrayList<Object> array = new ArrayList<Object>();//lista de objetos a sumar por ServiceSuma
			array.add(this.task.parametros.get("num1"));
			array.add(this.task.parametros.get("num2"));
			this.task.setResultado((int) s.execute(array));
			log.info("["+ this.node.getName()+ "] RESULTADO TAREA: "+ this.task.getResultado());
			//-------------
			
			Message res = this.task;
			// Envio resultado a outputQueue
			String mString =  googleJson.toJson(res); 	
			queueChannel.basicPublish(EXCHANGE_OUTPUT, this.task.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
			log.info("["+ this.node.getName()+ "] Sent response "+ googleJson.toJson(res).toString());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
