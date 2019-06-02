package Punto4.withDownWorkers;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.MessageProperties;

public class ServerSobel implements RemoteInt {

	private String ip;
	private int port;
	private final static Logger log = LoggerFactory.getLogger(ServerSobel.class);
	private Map<Integer, String> workersActivesQueues; // par ID Worke r-> Nombre de su Cola 
	private ArrayList<WorkerSobel> workers;
	private ArrayList<SobelRequest> workerRequests;
	//private ConnectionFactory connectionFactory;
	//private Connection queueConnection;
	private Channel queueChannel;
    private String OutQueueName;
	private static final long maxWaitTime = 3000; // 3 segundos. 
	private static final long periodWaitTime = 200; // 0.2 segundos. 
	private static final int sizeBorderSobel = 1;
    
	public ServerSobel(String ip, int port, ArrayList<WorkerSobel> workers, Channel queueChannel) throws NotBoundException, IOException, InterruptedException {
		this.port = port;
		this.workers= workers;
		this.workersActivesQueues = new HashMap<Integer,String>();
		this.workerRequests = new ArrayList<SobelRequest>();
		this.OutQueueName = "QUEUE_SOBEL_OUT";
		this.queueChannel = queueChannel;
		this.startSobelRMIforClient();
		configureConnectionToRabbit();
		this.startWorkersQueues();
	}
	
	private void configureConnectionToRabbit() {
		try {

			this.queueChannel.queueDeclare(this.OutQueueName, true, false, false, null);
			this.queueChannel.basicConsume(this.OutQueueName, true, receivedOutImages, consumerTag -> {});
			
		} catch (IOException e) {e.printStackTrace();
		}	
	}

	private DeliverCallback receivedOutImages = (consumerTag, delivery) -> {
        Gson googleJson = new Gson();
		SobelRequest response = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), SobelRequest.class);
        log.info("ServerSobel recibio una imagen del worker "+ response.getIdWorker());
    	
		boolean find = false;
        int i =0; int pos = -1;
        // la encuentro en mi copia local de Requests para actualizarle estado y OutImg.
        while ((i<this.workerRequests.size())&&(!find)) {
        	if (this.workerRequests.get(i).getIdWorker()==response.getIdWorker()) {
        		find = true;
        		pos = i;
        	}
        	i++;
        }
        
        if (find) {
    	    BufferedImage imageOut = ImageIO.read(new ByteArrayInputStream(response.getOutImg()));// imagen resultado parcial CON LOS BORDES
        	int w = imageOut.getWidth();
    		int h = imageOut.getHeight();
    		BufferedImage subImage = imageOut.getSubimage(this.sizeBorderSobel, imageOut.getMinY(),w-(this.sizeBorderSobel*2) , imageOut.getHeight());
    	    
    		/*Ejemplo:
    		   Si 
	    		   SizeBorderSobel = 10	
	    		   imgIn Original = 80
	    		   width imgOut (con bordes Sobel)= BorderIzq + 80 + BorderDer = 100  	
	    	   Entonces SubImage imgOut = 
	    		   						Ini X = 10
	    		   						size X = 100-(10*2) = 80;    
	    		   						0--10 -  IMAGEN ORIGINAL - 90--100 		 							
    		 */
    		
			ByteArrayOutputStream outParcialImg = new ByteArrayOutputStream();
			ImageIO.write(subImage, "jpg", outParcialImg);
			
        	this.workerRequests.get(pos).setOutImg(outParcialImg.toByteArray());
        	this.workerRequests.get(pos).setState(response.getState());
        }
    };
    
    
	private void startSobelRMIforClient() throws RemoteException {
		
		Registry server = LocateRegistry.createRegistry(this.port);
		log.info("-----ServerSobel Servicio RMI Iniciado en puerto "+this.port+"-----");
		
		RemoteInt serviceSobel = (RemoteInt) UnicastRemoteObject.exportObject(this, 8000);
		log.info("-----ServerSobel asociado a puerto "+8000+" -----");
		
		server.rebind("sobelImagenes", serviceSobel);
		log.info("-----Server bind de servicio JNDI realizado -----");
		
		log.info("-----conexion para cliente RMI finalizada correctamente.");
		System.out.println("");

	}

	
	public void startWorkersQueues() throws NotBoundException, IOException, InterruptedException {
		for (int i =0 ; i<this.workers.size(); i++) {	
			workersActivesQueues.put(this.workers.get(i).getIdWorker(),this.workers.get(i).getQueueName() );
		}

		log.info("-- ServerSobel Instancio las colas activas de todos sus workers");
	}
	
	
	public byte[] sobelDistribuido(byte[] image) throws IOException, InterruptedException, NotBoundException, ClassNotFoundException {
		log.info("--INGRESO A PROCESO 'SOBELDISTRIBUIDO' ENTRE CLIENTE Y SERVERSOBEL  --");
		
		BufferedImage inImg = ImageIO.read(new ByteArrayInputStream(image));// imagen que recibo de SobelClient
		BufferedImage outImg; //imagen final que paso a SobelClient
		BufferedImage inParcialImg; //Parte de la imagen que paso al Worker
		BufferedImage outParcialImg;// Parte de la imagen que recibo del Worker	
		int qSector = (int)(inImg.getWidth()/this.workers.size());
		System.out.println("tamaño del sector> "+qSector);
		int currentSector = 0;
		int currentWorker = 0;
	
		// declaro variables para imagen resultado
		int w = inImg.getWidth(); 
		int h = inImg.getHeight();
		outImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		
		int qPart= 1;
		while (qPart <= this.workers.size() ) {// una parte de la imagen original por cada worker
			int proxSectorSize = currentSector+(2*qSector);
			int borderIzq=0;
			// si no es el sector inicial> enviara una imagen mas grante para que el borde sobel no recorte parte de la imagen original
			if (currentSector!=0) {
				borderIzq = this.sizeBorderSobel;
			}
			if (proxSectorSize>inImg.getWidth()) {// cuando la division en partes entre los workers no da exacta, se asignara el resto al ultimo Worker
				int proxSector = currentSector+qSector;
			    int untilImageEnd = qSector+(inImg.getWidth() - proxSector);
			    inParcialImg = inImg.getSubimage(currentSector-borderIzq, inImg.getMinY(), untilImageEnd+(this.sizeBorderSobel), inImg.getHeight()); //funcion para partir la imagen.			
			}else {// caso normal
				inParcialImg = inImg.getSubimage(currentSector-borderIzq, inImg.getMinY(), qSector+(this.sizeBorderSobel*2), inImg.getHeight()); 
			}
			
			ByteArrayOutputStream inImgParam = new ByteArrayOutputStream();
			ImageIO.write(inParcialImg, "jpg", inImgParam);
			SobelRequest request = new SobelRequest(this.workers.get(currentWorker).getIdWorker(), inImgParam.toByteArray());
			
			sendImgToWorker(request);
			this.workerRequests.add(request);
			qPart++;
			currentSector+= qSector;
			currentWorker++;
			if (currentWorker==this.workers.size()) {
				currentWorker = 0;
			}
		}
		
		waitingImagesSobel();
		outImg = joinImages(inImg);
		
		ByteArrayOutputStream imgResult = new ByteArrayOutputStream();
		ImageIO.write(outImg, "jpg", imgResult);
		log.info("Resultado final de Sobel enviado al cliente (via RMI)");
		return imgResult.toByteArray();
	}
	
	
	public void sendImgToWorker(SobelRequest request) throws UnsupportedEncodingException, IOException {
		//envia a Worker 
		Gson googleJson = new Gson();
		String sRequest = googleJson.toJson(request);
		this.queueChannel.basicPublish("", this.workersActivesQueues.get(request.getIdWorker()), MessageProperties.PERSISTENT_TEXT_PLAIN,sRequest.getBytes("UTF-8") );
		log.info("Parte sobel enviada a worker "+ request.getIdWorker());
	}
	
	
	public BufferedImage joinImages(BufferedImage inImg) throws IOException {
		// cuando ya recibio todas las request con sus correspondientes imagenes Sobel.
		log.info("Uniendo las imagenes sobel parciales... ");
		int w = inImg.getWidth();//-(this.sizeBorderSobel*2*this.workerRequests.size());
		int h = inImg.getHeight();
		BufferedImage outImg = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		Graphics unionImages = outImg.getGraphics();
		int sectorSize = 0;
		for (int i=0; i<this.workerRequests.size();i++) {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(this.workerRequests.get(i).getOutImg()));
			unionImages.drawImage(image, sectorSize, 0, null);
			sectorSize+=image.getWidth();
		}
		return outImg;
	}
	

	public int imagesReceived() {
		int cont = 0;
		for (int i = 0 ; i < this.workerRequests.size(); i++ ){
			if (this.workerRequests.get(i).getState().equals(StateSobelRequest.DONE)) {cont++;}
		}
		log.info("Hay "+ cont + " imagenes recibidas de las "+this.workerRequests.size()+" enviadas");
		return cont;
	}
	
	
	public void waitingImagesSobel() throws IOException, ClassNotFoundException, InterruptedException {
		boolean salir = false;
		long currentTimeWait = 0;
		
		while (!salir) {
			if (imagesReceived()==this.workerRequests.size()) {// si llegaron todas las respuestas de los workers
				log.info("Han llegado todas las partes de la imagen resueltas en sobel.");
				salir = true;
				
			}else {
				if (maxWaitTime == currentTimeWait) {//si paso el maximo tiempo de espera para las respuestas
					log.info("Se sobrepaso el teimpo maximo de espera.");
					vefiryFallenWorkers();
					currentTimeWait = 0;
				}else {
					currentTimeWait+= this.periodWaitTime; // aumento para la proxima entrada al while
					Thread.sleep(this.periodWaitTime);
				}
			}
		}

	}

		
  private void vefiryFallenWorkers() throws IOException {
	  //solo lo llamo cuando haya pasado el tiempo maximo de espera y no tengo todas las respuestas.
	  log.info("Verificando Workers caidos...");
	for (int i = 0 ; i<this.workerRequests.size(); i++) {
		
		if (this.workerRequests.get(i).getState().equals(StateSobelRequest.PENDENT)){
			// elimino la cola correspondiente
			 log.info("Worker " + this.workerRequests.get(i).getIdWorker() + " no envio su respuesta a tiempo. Reasignando tarea...");
			log.info(this.workersActivesQueues.get(this.workerRequests.get(i).getIdWorker()) + " eliminada");
			this.queueChannel.queueDelete(this.workersActivesQueues.get(this.workerRequests.get(i).getIdWorker()));
			this.workersActivesQueues.remove(this.workerRequests.get(i).getIdWorker()); 
			//reasigno la request a otro worker. Para eso agarro el primer Worker que encuentre que ya haya cumplido su tarea.
			boolean reasigned = false;
			do { // va a intentar asignarle un nuevo worker hasta que alguno quede ocioso.
				int nextW = nextIdleWorker(this.workerRequests.get(i).getIdWorker());	
				if (nextW!=-1) {
					reasigned = true;
					log.info(" Tarea reasignada a Worker "+ nextW );
					this.workerRequests.get(i).setIdWorker(nextW);
					sendImgToWorker(this.workerRequests.get(i));
				}
			}while (!reasigned);	
		}
	}
  }		
  
  public int nextIdleWorker(int antWID) {
	  boolean find = false;
	  int i = 0;
	  int wID = -1;
	  while ((!find)&&(i<this.workerRequests.size())) { 
		  if (this.workerRequests.get(i).getState().equals(StateSobelRequest.DONE) 
		     && (this.workerRequests.get(i).getIdWorker()!=antWID)){// Worker ocioso, ya entrego su parte.
			  		find = true;
			  		wID = this.workerRequests.get(i).getIdWorker();
		  }
		  i++;
	  }
	  return wID;
  }
  

//-----------------------------------------------------------------------------------------------------------------------//
 //-----------------------------------------------------------------------------------------------------------------------//
	public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, TimeoutException {
		Logger log = LoggerFactory.getLogger(ServerSobel.class);
		log.info("============ Server Dispatcher iniciado ============");
		System.out.println("Ingrese cantidad de servidores [fijos]>");
		Scanner scanner = new Scanner(System.in);
		int qWorkers=  scanner.nextInt();
		int portInicialWorker = 90;
		ArrayList<WorkerSobel> workerList = new ArrayList<WorkerSobel>();
	
		ConnectionFactory connectionFactory = new ConnectionFactory();
		connectionFactory.setHost("localhost");
		connectionFactory.setUsername("admin");
		connectionFactory.setPassword("admin");
		Connection queueConnection = connectionFactory.newConnection();
		Channel queueChannel = queueConnection.createChannel();
		
		for (int i =0 ; i<qWorkers; i++) {	
			int portW = portInicialWorker + i;
			WorkerSobel ws = new WorkerSobel(i, queueChannel);
			workerList.add(ws);
			//Thread tW = new Thread(ws);
			//tW.start();
		}
		
		System.out.println("");
		int portRMIserver = 80;
		ServerSobel ss = new ServerSobel("localhost",portRMIserver,workerList, queueChannel);
					
	}
	
}
