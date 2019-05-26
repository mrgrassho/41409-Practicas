package Punto4.withDownWorkers;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

import ch.qos.logback.classic.Logger;

public class ServerSobelThread implements Runnable{

	private int id;
	private String ipRMI;
	private int portRMI;
	private Logger log;
	private SobelRequest request;
	private ArrayList<BufferedImage> partsImg;

	private String queueName;
	private Channel queueChannel;
	private RemoteInt ri;
	
	public ServerSobelThread(int idT, String queueName,Channel queueChannel, SobelRequest request, RemoteInt ri) throws NotBoundException, IOException, InterruptedException {
		this.id = idT;
		this.request = request;
		this.queueName = queueName;
		this.queueChannel = queueChannel;
		this.ri = ri;
	}
	
	public void run() {
		//llamo al servicio por RMI
		try {
			//ByteArrayOutputStream imgSend = new ByteArrayOutputStream();
			//ImageIO.write(this.request.getInParcialImg(), "jpg", imgSend);
			
			byte[] imgResult = this.ri.sobelDistribuido(this.request.getInParcialImg());
			this.request.setOutImg(imgResult);
			//envia a Worker 
			Gson googleJson = new Gson();
			String response = googleJson.toJson(request);
			this.queueChannel.basicPublish("", this.queueName, MessageProperties.PERSISTENT_TEXT_PLAIN,response.getBytes("UTF-8") );
			
			
			/*grabo en carpeta el resultado
			BufferedImage outParcialImg = ImageIO.read(new ByteArrayInputStream(imgResult));
			String outputParcialPath = "images/sobelPart"+this.id+".JPG";
			FileOutputStream outParcialFile = new FileOutputStream(outputParcialPath);
			ImageIO.write(outParcialImg, "JPG", outParcialFile);
			System.out.println("Grabo imagen en el thread "+ this.id);
			*/
		} catch (IOException e) {e.printStackTrace();
		} catch (InterruptedException e) {e.printStackTrace();
		} catch (NotBoundException e) {e.printStackTrace();
		} catch (ClassNotFoundException e) {e.printStackTrace();
		}
		
		

		 
	}



}



