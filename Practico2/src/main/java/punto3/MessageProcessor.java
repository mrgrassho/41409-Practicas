package punto3;

import java.io.IOException;
import java.util.ArrayList;

import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.MessageProperties;

public class MessageProcessor implements Runnable {
	private Dispatcher dispatcher;
	private String queueName;
	
	MessageProcessor(Dispatcher dispatcher, String queueName){
		this.dispatcher = dispatcher;
		this.queueName = queueName;
	}

	@Override
	public void run() {
		dispatcher.log.info(" [msgProcess -"+queueName+"] MsgProcess Thread started.");
		DeliverCallback msgProcess = (consumerTag, delivery) -> { //when received message from InProcessQueue
			int MAX_RETRY = this.dispatcher.getNodosActivos().size();
			String json;
			Message message = this.dispatcher.getGoogleJson().fromJson(new String(delivery.getBody(),"UTF-8"), Message.class);
			dispatcher.log.info(" [msgProcess -"+queueName+"] waiting for notification...");
			int retriesInNode = 0;
			boolean flag = false;
			while (retriesInNode < dispatcher.MAX_RETRIES_IN_NODE && !flag) {
				if (dispatcher.getQueueChannel().messageCount(dispatcher.notificationQueueName) >= 1) {
					dispatcher.getQueueChannel().basicGet(dispatcher.notificationQueueName, true);
					flag = true;  // a ThreadNode finished his task
				} else {
					try {
						Thread.sleep(dispatcher.RETRY_SLEEP_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				retriesInNode++;
			}
			String nodeName = message.getHeader("to-node");
			synchronized (dispatcher.getNodosActivos()) {
				Node n = dispatcher.findNodeByName(nodeName);
				if (n != null) {
					if (flag) { //[node finished his task]
						dispatcher.log.info(" [msgProcess -"+queueName+"] Msg notification arrived from ["+ nodeName + "]!");
						// Update Node Load
						n.decreaseCurrentLoad();
						dispatcher.log.info(" [msgProcess -"+queueName+"] currentLoad ["+ nodeName + "]: " + n.getCurrentLoad());
						// Actualizamos Activos en memoria
						synchronized (dispatcher.getNodosActivos()) {
							dispatcher.setNodeByName(nodeName, n);
						}
					} else {
						if (null != message.getHeader("redir-qty")) {
							int redir = Integer.parseInt(message.getHeader("redir-qty"));
							message.setHeader("redir-qty", String.valueOf(redir+1));
						} else {
							message.setHeader("redir-qty", "1");
						}

						// Re send the message to inputQueue
						dispatcher.log.info(" [msgProcess -"+queueName+"] Msg notification NOT arrived  from ["+ nodeName + "] - TIMEOUT REACHED!");
						// Update Node Load
						n.decreaseCurrentLoad();
						dispatcher.decreaseGlobalMaxLoad(n.getMaxLoad());
						synchronized (dispatcher.getNodosActivos()) {
							dispatcher.getNodosActivos().remove(n);
						}
						//dispatcher.getQueueChannel().queueDelete(n.getName());
						
						while (dispatcher.getQueueChannel().messageCount(n.getName()) > 0) {
							GetResponse g = dispatcher.getQueueChannel().basicGet(n.getName(),true);
							Message message1 = this.dispatcher.getGoogleJson().fromJson(new String(g.getBody(),"UTF-8"), Message.class);
							json = dispatcher.googleJson.toJson(message1);
							dispatcher.getQueueChannel().basicPublish("", dispatcher.inputQueueName, MessageProperties.PERSISTENT_TEXT_PLAIN, json.getBytes("UTF-8"));;
						}
						dispatcher.getQueueChannel().queuePurge(n.getName()+"inProcess");
						dispatcher.createNodos(1);
					}

				}
				// Update Total Load
				dispatcher.decreaseGlobalCurrentLoad();
				dispatcher.updateGlobal();
				dispatcher.log.info(" [msgProcess -"+queueName+"] GlobalCurrentLoad [" + dispatcher.getGlobalCurrentLoad() + "]");
				dispatcher.getQueueChannel().queueUnbind(dispatcher.notificationQueueName, dispatcher.EXCHANGE_NOTIFY, message.getHeader("token-id"));
				dispatcher.log.info(" [msgProcess -"+queueName+"] Dispatcher unbind> notificationQueue | Exchange '" + dispatcher.EXCHANGE_NOTIFY + "' | RoutingKey '"+message.getHeader("token-id")+"'" );
			}
		};
		
		try {
			dispatcher.getQueueChannel().basicConsume(queueName, true, msgProcess, consumerTag -> {});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
