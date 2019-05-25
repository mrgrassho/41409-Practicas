package Punto4;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import Punto4.RemoteInt;


public class SobelClient {
				
	private static final int portSocket = 9000;
	private ArrayList<Integer> portServers;
				
	
	public SobelClient(ArrayList<Integer> portServers) {
		this.portServers = portServers;
	}
	
	
	public String sobelCall(String pathImage, String tipoSobel) throws IOException, NotBoundException, InterruptedException, TimeoutException {		

		String entryPath = pathImage;
		
		int pos = entryPath.indexOf(".");
		String nameImage = entryPath.substring(0, pos);
		String extensionImage = entryPath.substring(pos, entryPath.length());;		
		String outputPath= nameImage + "-SobelResult"+ extensionImage;
		String outputParcialPath= "";
		
		FileInputStream inFile = new FileInputStream(entryPath);
		BufferedImage inImg = ImageIO.read(inFile);
		
		BufferedImage outImg;
		
		long start = System.currentTimeMillis();
		
		Registry clientRMI = LocateRegistry.getRegistry("localhost", this.portServers.get(0));
		System.out.println("----- Cliente conectado a un 1 Server implementer en puerto"+this.portServers.get(0)+"-----");
		RemoteInt ri = (RemoteInt) clientRMI.lookup("sobelImagenes");
		//--------- proceso --------------------------
		if (tipoSobel.equals("local")) {
			ByteArrayOutputStream imgSend = new ByteArrayOutputStream();
			ImageIO.write(inImg, "jpg", imgSend);
			byte[] imgResult = ri.sobel(imgSend.toByteArray());
			outImg = ImageIO.read(new ByteArrayInputStream(imgResult));
		
		}else { // distribuido
			
			//declare queue donde me llegan las respuestas.
			 ConnectionFactory factory = new ConnectionFactory();
			 factory.setHost("localhost");
			 Connection connection = factory.newConnection();
			 Channel channel = connection.createChannel();
			 String QUEUE_NAME = "QueueSobel"; 
			 channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			
			int qSector = (int)(inImg.getWidth()/this.portServers.size());
			System.out.println("tamaño del sector> "+qSector);
			int actualSector = 0;
			int contServer = 0;
			BufferedImage inParcialImg;
			BufferedImage outParcialImg;			
			// declaro variables para imagen resultado
			int w = inImg.getWidth(); 
			int h = inImg.getHeight();
			outImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			Graphics unionImages = outImg.getGraphics();
		
			while (actualSector<inImg.getWidth()) {
				int sectorSize = actualSector+qSector;
				if (sectorSize>inImg.getWidth()) {
				   int imageEnd = inImg.getWidth() - actualSector;
				   inParcialImg = inImg.getSubimage(actualSector, inImg.getMinY(), imageEnd, inImg.getHeight()); //funcion para partir la imagen.			
				}else {// caso normal
					inParcialImg = inImg.getSubimage(actualSector, inImg.getMinY(), qSector, inImg.getHeight()); 
				}
				
				/* me guarda las imagenes partidas SIN SOBEL
				outputPath = nameImage + "-OriginalPart"+actualSector +extensionImage;
				FileOutputStream outFile = new FileOutputStream(outputPath);
				ImageIO.write(inParcialImg, "JPG", outFile);
				*/
				
				//llamo al servicio por RMI
				ByteArrayOutputStream imgSend = new ByteArrayOutputStream();
				ImageIO.write(inParcialImg, "jpg", imgSend);
				byte[] imgResult = ri.sobel(imgSend.toByteArray());
				outParcialImg = ImageIO.read(new ByteArrayInputStream(imgResult));
				
				/* me guarda las imagenes partidas CON SOBEL
    			outputParcialPath = nameImage + "-SobelPart"+actualSector +extensionImage;
				FileOutputStream outParcialFile = new FileOutputStream(outputParcialPath);
				ImageIO.write(outParcialImg, "JPG", outParcialFile);
				 */
				
				unionImages.drawImage(outParcialImg, actualSector, 0, null); 
			
				actualSector+= qSector;
				contServer++;
			}
			
		}
		//--------------------------------------------
		
		FileOutputStream outResultFile = new FileOutputStream(outputPath);
		ImageIO.write(outImg, "JPG", outResultFile);
		
		long end = System.currentTimeMillis();
		System.out.println(" ELAPSED TIME: "+(end-start));
		return outputPath;
	}			
			
	
	
	public static void main(ArrayList<Integer> args) throws IOException, NotBoundException, InterruptedException, TimeoutException {

		System.out.println("----- SobelClient iniciado -----");
		SobelClient sobel1  = new SobelClient(args);
				
		//System.out.println("Ingrese la ruta de la imagen a modificar: ");
		//Scanner scannerImage = new Scanner(System.in);
		//String pathImage =  scannerImage.nextLine();
		String pathImage = "images/pc1.jpg";  //Imagen existente en el proyecto. Descomentar esta linea y comentar las 3 anteriores para pruebas rapidas.
		
		boolean salir= false;
		do {
			System.out.println("MENU");
			System.out.println("1. Sobel Local (a)");
			System.out.println("2. Sobel Distribuido (b)");
			System.out.println("2. Sobel Distribuido mejorado (c)");					
			System.out.println("0. Terminar");
			System.out.println("Ingrese una opcion>");
			Scanner scanner = new Scanner(System.in);
			int op =  scanner.nextInt();
			//int op = 1;
			String pathResultado;
			switch(op) {
			case 1:
				pathResultado = sobel1.sobelCall(pathImage,"local");
				if (pathResultado!=null) {
					System.out.println("Se ha hecho la operacion exitosamente.");
					System.out.println("La nueva imagen se ubica dentro del proyecto en la ruta: " + pathResultado);
					salir=true;
				}
				break;
			case 2:
				pathResultado = sobel1.sobelCall(pathImage,"distribuido");
				if (pathResultado!=null) {
					System.out.println("Se ha hecho la operacion exitosamente.");
					System.out.println("La nueva imagen se ubica dentro del proyecto en la ruta: " + pathResultado);
					salir=true;
				}
				break;
			case 0:
				System.out.println("------- Programa finalizado -------");
				
				salir = true;
				break;		
			default:
				System.out.println("La opcion ingresada no corresponde.");
				System.out.println("");
			}
		}while (!salir);
	}
	
	
}
