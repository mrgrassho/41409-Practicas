package punto3.node;

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

import punto3.core.Message;

public class ThreadNode implements Runnable {
	
	private Channel queueChannel;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private Message task;
	private Node node;
	private Long id;
	private String EXCHANGE_OUTPUT;
	
	public ThreadNode(Long idThread,Node node, Long routingKey, Message message, Channel queueChannel, String EXCHANGE_OUTPUT, Logger log) {
		this.id = idThread;
		this.node = node;
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.EXCHANGE_OUTPUT = EXCHANGE_OUTPUT ;
		this.task = message;
		this.googleJson = new Gson();
		this.log = log;
	}

	public void run() {
		try {
			Service s = this.node.findServiceByName(this.task.getFunctionName());
			if (s!=null) {
				log.info("TASK - "+ s.getName());
				log.info("["+ this.node.getName()+ " - Thread "+this.id+"] :" +this.task.parametros.values());
				
				this.task.setResultado(s.execute(this.task.parametros.values().toArray()));
				log.info("["+ this.node.getName()+ " - Thread "+this.id+"] RESULTADO TAREA: "+ this.task.getResultado());
				//-------------
				
				Message res = this.task;
				// Envio resultado a outputQueue
				String mString =  googleJson.toJson(res); 	
				queueChannel.queueBind(res.getHeader("token-id"), EXCHANGE_OUTPUT, "");
				//queueChannel.basicPublish("", res.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				queueChannel.basicPublish(EXCHANGE_OUTPUT, "", MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				log.info("["+ this.node.getName()+ " - Thread "+this.id+"]  Sent response "+ googleJson.toJson(res).toString());
				queueChannel.queueUnbind(res.getHeader("token-id"), EXCHANGE_OUTPUT, "");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
