package punto3.node;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
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
	private String activeQueueName;
	private String outputQueueName;
	private String routingKey;
	private Gson googleJson;
	private Logger log;
	private Message task;
	private Node node;
	private Long id;

	private Socket ServerResult;

	private static final String EXCHANGE_OUTPUT = "XCHNG-OUT";

	public ThreadNode(Long idThread,Node node, Long routingKey, Message message, Channel queueChannel, String activeQueueName, String outputQueueName, Logger log, String ipServerResult, int portServerResult) {
		this.id = idThread;
		this.node = node;
		this.routingKey = String.valueOf(routingKey);
		this.queueChannel = queueChannel;
		this.activeQueueName = activeQueueName;
		this.outputQueueName = outputQueueName;
		this.task = message;
		this.googleJson = new Gson();
		this.log = log;
		try {
			this.ServerResult = new Socket(ipServerResult, portServerResult);
		} catch (ConnectException e) {
			System.err.println("[!] Server is down.");
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			//--------------harcodeado,  enrealidad tiene que buscar a que servicio corresponde llamar

			//log.info("["+ this.node.getName()+ " - Thread "+this.id+"] :" +this.task.parametros.values());
			//log.info("["+ this.node.getName()+ " - Thread "+this.id+"] cant de service: "+ this.node.getServices().size());
			//log.info("["+ this.node.getName()+ " - Thread "+this.id+"] service 1: "+ this.node.getServices().get(0).getName());
			ObjectOutputStream outputChannel = new ObjectOutputStream (this.ServerResult.getOutputStream());
			Service s = this.node.findServiceByName(this.task.getFunctionName());
			if (s!=null) {
				log.info("TASK - "+ s.getName());
				log.info("["+ this.node.getName()+ " - Thread "+this.id+"] :" +this.task.parametros.values());

				this.task.setResultado(s.execute(this.task.parametros.values().toArray()));
				log.info("["+ this.node.getName()+ " - Thread "+this.id+"] RESULTADO TAREA: "+ this.task.getResultado());
				//-------------

				Message res = this.task;
				res.setHeader("tipo", "response");
				// Envio resultado a outputQueue
				String mString =  googleJson.toJson(res);
				//queueChannel.queueBind("notificationQueue", EXCHANGE_OUTPUT, res.getHeader("token-id"));
				//log.info("["+ this.node.getName()+ "] declared bind> notificacionQueue | Exchange '"+EXCHANGE_OUTPUT+"' | routingKey '"+res.getHeader("token-id")+"'");
				//queueChannel.basicPublish("", res.getHeader("token-id"), MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				queueChannel.basicPublish(EXCHANGE_OUTPUT, "", MessageProperties.PERSISTENT_TEXT_PLAIN, mString.getBytes("UTF-8"));
				outputChannel.writeObject(res);
				log.info("["+ this.node.getName()+ " - Thread "+this.id+"]  Sent response "+ googleJson.toJson(res).toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
