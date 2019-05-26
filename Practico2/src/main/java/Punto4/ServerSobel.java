package Punto4;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AccessException;
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

import punto2.syn.with.DepositoThread;
import punto3.core.Message;

public class ServerSobel implements RemoteInt {

	private String ip;
	private int port;
	private final static Logger log = LoggerFactory.getLogger(ServerSobel.class);
	private Map<Integer, RemoteInt> workerConections; // par PuertoWorker-> RI 
	private ArrayList<WorkerSobel> workers;
	private Map<Integer,BufferedImage> finishedImgs; // par IdWorker -> ImagenSobel
	private ConnectionFactory connectionFactory;
	private Connection queueConnection;
	private Channel queueChannel;

	
	public ServerSobel(String ip, int port, ArrayList<WorkerSobel> workers) throws NotBoundException, IOException, InterruptedException {
		this.port = port;
		this.workerConections = new HashMap<Integer,RemoteInt>();
		this.workers= workers;
		this.finishedImgs = new HashMap<Integer,BufferedImage>();
		this.startSobelRMIforClient();
		this.startWorkerConections();
		configureConnectionToRabbit();
	}
	
	private void configureConnectionToRabbit() {
		try {
			this.connectionFactory = new ConnectionFactory();
			this.connectionFactory.setHost("localhost");
			this.connectionFactory.setUsername("admin");
			this.connectionFactory.setPassword("admin");
			this.queueConnection = this.connectionFactory.newConnection();
			this.queueChannel = this.queueConnection.createChannel();
			this.queueChannel.queueDeclare("QUEUE_SOBEL", true, false, false, null);
			this.queueChannel.basicConsume("QUEUE_SOBEL", true, recibirOutImages, consumerTag -> {});
			
		} catch (IOException e) {e.printStackTrace();
		} catch (TimeoutException e) {e.printStackTrace();
		}	
	}
	
	private DeliverCallback recibirOutImages = (consumerTag, delivery) -> {
        //byte[] imgQueue = delivery.getBody();
        Gson googleJson = new Gson();
		SobelRequest response = googleJson.fromJson(new String(delivery.getBody(),"UTF-8"), SobelRequest.class);
        System.out.println("ServerSobel agarro una imagen de la cola..");
        
        BufferedImage imgOut = ImageIO.read(new ByteArrayInputStream(response.getOutImg()));
        this.finishedImgs.put(response.getIdWorker(),imgOut);
        
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

	
	public void startWorkerConections() throws NotBoundException, IOException, InterruptedException {
		RemoteInt ri;
		for (int i =0 ; i<this.workers.size(); i++) {	
			Registry clientRMI = LocateRegistry.getRegistry("localhost", this.workers.get(i).getPortRMI());
			ri = (RemoteInt) clientRMI.lookup("sobelImagenes");
			log.info("----- ServerSobel conectado via RMI a worker"+this.workers.get(i).getIdWorker() +" en puerto "+this.workers.get(i).getPortRMI()+"-----");
			workerConections.put(this.workers.get(i).getPortRMI(), ri);
			
			String result = "inicialmente no anda";
			result = workerConections.get(this.workers.get(i).getPortRMI()).proceso();
			System.out.println(result);
			
		}
	}
	
	
	public byte[] sobelDistribuido(byte[] image) throws IOException, InterruptedException, NotBoundException, ClassNotFoundException {
		log.info("--INGRESO A PROCESO 'SOBELDISTRIBUIDO' ENTRE CLIENTE Y SERVERSOBEL  --");
		
		BufferedImage inImg = ImageIO.read(new ByteArrayInputStream(image));// imagen que recibo de SobelClient
		BufferedImage outImg; //imagen final que paso a SobelClient
		BufferedImage inParcialImg; //Parte de la imagen que paso al Worker
		BufferedImage outParcialImg;// Parte de la imagen que recibo del Worker	
		int qSector = (int)(inImg.getWidth()/this.workerConections.size());
		System.out.println("tamaño del sector> "+qSector);
		int currentSector = 0;
		int currentWorker = 0;
	
		// declaro variables para imagen resultado
		int w = inImg.getWidth(); 
		int h = inImg.getHeight();
		outImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		int qParts= 0;
		while (qParts < this.workers.size() ) {// una parte por cada worker
			int proxSectorSize = currentSector+(2*qSector);
			if (proxSectorSize>inImg.getWidth()) {// cuando la division en partes entre los workers no da exacta, se asigna el resto al ultimo Worker
				int proxSector = currentSector+qSector;
			    int untilImageEnd = qSector+(inImg.getWidth() - proxSector);
			    inParcialImg = inImg.getSubimage(currentSector, inImg.getMinY(), untilImageEnd, inImg.getHeight()); //funcion para partir la imagen.			
			}else {// caso normal
				inParcialImg = inImg.getSubimage(currentSector, inImg.getMinY(), qSector, inImg.getHeight()); 
			}
			
			ByteArrayOutputStream inImgParam = new ByteArrayOutputStream();
			ImageIO.write(inParcialImg, "jpg", inImgParam);
			SobelRequest request = new SobelRequest(this.workers.get(currentWorker).getIdWorker(),
													this.workers.get(currentWorker).getPortRMI(), 
													qSector, 
													currentSector, 
													inImgParam.toByteArray());
			
			ServerSobelThread sst = new ServerSobelThread(currentWorker,"QUEUE_SOBEL",this.queueChannel, request, workerConections.get(this.workers.get(currentWorker).getPortRMI()));
			Thread sstThread = new Thread(sst);
			sstThread.start();

			qParts++;
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
		
		return imgResult.toByteArray();
	}
	
	
	public BufferedImage joinImages(BufferedImage inImg) {	
		int w = inImg.getWidth();
		int h = inImg.getHeight();
		BufferedImage outImg = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
		Graphics unionImages = outImg.getGraphics();
		int sectorSize = 0;
		for (int i=0; i<this.workerConections.size();i++) {
			unionImages.drawImage(this.finishedImgs.get(this.workers.get(i).getIdWorker()), sectorSize, 0, null);
			sectorSize+=this.finishedImgs.get(this.workers.get(i).getIdWorker()).getWidth();
		}
		return outImg;
	}
	

	public void waitingImagesSobel() throws IOException, ClassNotFoundException, InterruptedException {
		boolean salir = false;
		while (!salir) {
			System.out.println("largo de finishedImgs: " + this.finishedImgs.size());
			if (this.finishedImgs.size()==this.workerConections.size()) {
				System.out.println("finishedImgs ya tiene todas las partes de la imagen resueltas en sobel.");
				salir = true;
			}
		}

	}

	
	public String proceso() throws IOException, InterruptedException {
		return "procesoRMI en ServerSobel (Dispatcher) funcionando";
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
		for (int i =0 ; i<qWorkers; i++) {	
			int portW = portInicialWorker + i;
			WorkerSobel ws = new WorkerSobel(i,portW);
			workerList.add(ws);
		}
		System.out.println("");
		int portRMIserver = 80;
		ServerSobel ss = new ServerSobel("localhost",portRMIserver,workerList);
					
	}
	
}
