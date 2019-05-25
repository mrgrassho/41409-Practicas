package Punto4;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;

import ch.qos.logback.classic.Logger;

public class ServerSobelThread implements Runnable{

	private int id;
	private String ipRMI;
	private int portRMI;
	private Logger log;
	private SobelRequest request;
	
	public ServerSobelThread(int id, String ipRMI, int portRMI,String QueueRespones , SobelRequest request) throws UnknownHostException, IOException{
		this.id = id;
		this.ipRMI = ipRMI;
		this.portRMI = portRMI;
		this.request = request;
	}
	
	public void run() {
		
	}
	

	/*
	BufferedImage inImg;
	
	int sectorSiz
	  getSubimage(actualSector, oriImg.getMinY(), imageEnd, oriImg.getHeight()); //funcion para partir la imagen.			
	}else {// caso normal
		inImg = oriImg.getSubimage(actualSector, oriImg.getMinY(), qSector, oriImg.getHeight()); 
	}e= actualSector+qSector;
	if (sectorSize>oriImg.getWidth()) {
		int imageEnd = oriImg.getWidth() - sectorSize;
		inImg = oriImg.
	
		ByteArrayOutputStream imgSend = new ByteArrayOutputStream();
		ImageIO.write(inImg, "JPG", imgSend);
		byte[] imgPartResult = ri.sobel(imgSend.toByteArray());
		outImg = ImageIO.read(new ByteArrayInputStream(imgPartResult));
	*/
	
	/* PROBANDO UNION DE DOS IMAGENES. BORRAR LUEGO
	  BufferedImage inImg1 = inImg.getSubimage(inImg.getMinX(), inImg.getMinY(), 300, 300);
	BufferedImage inImg2 = inImg.getSubimage(inImg.getMinX(), inImg.getMinY(), 300, 300);
	
	int w = Math.max(image.getWidth(), overlay.getWidth()); 
	int h = Math.max(image.getHeight(), overlay.getHeight());

	BufferedImage ima = new BufferedImage();
	Graphics g = ima.getGraphics(); 
	g.drawImage(image, 0, 0, null); 
	g.drawImage(overlay, 0, 0, null);*/
	
	//--------- proceso --------------------------
	
	// inImg = inImg.getSubimage(inImg.getMinX(), inImg.getMinY(), 300, 300); //funcion para partir la imagen.





}



