package Punto4.withDownWorkers;

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

import Punto4.withDownWorkers.RemoteInt;


public class SobelClient {
				
	private int portServer; //para el server que funciona como dispatcher entre los Worker. Sera el 80.
				
	
	public SobelClient(int portServer) {
		this.portServer = portServer;
	}
	
	
	public String sobelCall(String pathImage, String tipoSobel) throws IOException, NotBoundException, InterruptedException, TimeoutException, ClassNotFoundException {		

		long start = System.currentTimeMillis(); // INICIO tiempo tarea
		
		String entryPath = pathImage;
		int pos = entryPath.indexOf(".");
		String nameImage = entryPath.substring(0, pos);
		String extensionImage = entryPath.substring(pos, entryPath.length());;		
		String outputPath= nameImage + "-SobelResult"+ extensionImage;
		String outputParcialPath= "";
		
		FileInputStream inFile = new FileInputStream(entryPath);
		BufferedImage inImg = ImageIO.read(inFile);
		BufferedImage resultImg;
		
		if (tipoSobel.equals("local")) {
			resultImg = sobelLocal(inImg);
		}else {
			Registry clientRMI = LocateRegistry.getRegistry("localhost", this.portServer);
			System.out.println("----- Cliente conectado a server principal en puerto "+this.portServer+"-----");
			RemoteInt ri = (RemoteInt) clientRMI.lookup("sobelImagenes");
			ByteArrayOutputStream imgSend = new ByteArrayOutputStream();
			ImageIO.write(inImg, "jpg", imgSend);
			byte[] outImg = ri.sobelDistribuido(imgSend.toByteArray());
			resultImg = ImageIO.read(new ByteArrayInputStream(outImg));
		}
		
		FileOutputStream outResultFile = new FileOutputStream(outputPath);
		ImageIO.write(resultImg, "JPG", outResultFile);
		
		long end = System.currentTimeMillis();
		System.out.println(" ELAPSED TIME: "+(end-start));
		return outputPath;
	}			
			
	// -----------------------------------------------------------------------------------------------------
	public BufferedImage sobelLocal(BufferedImage inImg) {
	    int[][] sobel_x = new int[][]{
			{-1, 0,1}, 
			{-2, 0,2}, 
			{-1, 0,1}	
		};
		
		int[][] sobel_y = new int[][]{
			{-1,-2,-1},
			{0, 0, 0},
			{1, 2, 1}				
		};
		
		int i, j;
		int  max= 0, min=99999;
		float[][] Gx;
		float[][] Gy;
		float[][] G;
		
		int width = inImg.getWidth();
		int height = inImg.getHeight();
		System.out.println("W:" + width);
		System.out.println("H:" + height);
		float[] pixels = new float[(int) width * (int) height];
		float[][] output = new float[(int) width][(int) height];
		
			int counter2 = 0;
	        for (int xx = 0; xx < width; xx++) {

	            for (int yy = 0; yy < height; yy++) {
	            	try{
	            		
	            		pixels[counter2] = inImg.getRGB(xx, yy);
	            		output[xx][yy] = inImg.getRGB(xx, yy);
	            	
	            	}catch(Exception e1){
	            	    System.out.println("Error: " + " x: "+ yy + " y: " + xx);
	            	    
	            	}
	                counter2++;
	            }
	        }
	        

		Gx = new float[width][height];
		Gy = new float[width][height];
		G = new float[width][height];

		for (i = 0; i < width; i++) {
			for (j = 0; j < height; j++) {
				if (i == 0 || i == width - 1 || j == 0 || j == height - 1)
					Gx[i][j] = Gy[i][j] = G[i][j] = 0;
				else {
					// Calculo x
			    	  
			    	  Gx[i][j] = ((sobel_x[0][0] * output[i-1][j-1]) + (sobel_x[0][1] * output[i][j-1]) + (sobel_x[0][2] * output[i+1][j-1]) + (sobel_x[1][0] * output[i-1][j]) + (sobel_x[1][1] * output[i][j]) + (sobel_x[1][2] * output[i+1][j]) + (sobel_x[2][0] * output[i-1][j+1]) + (sobel_x[2][1] * output[i][j+1]) + (sobel_x[2][2] * output[i+1][j+1])); 
			    	  // Calculo y
			    	  Gy[i][j] = ((sobel_y[0][0] * output[i-1][j-1]) + (sobel_y[0][1] * output[i][j-1]) + (sobel_y[0][2] * output[i+1][j-1]) + (sobel_y[1][0] * output[i-1][j]) + (sobel_y[1][1] * output[i][j]) + (sobel_y[1][2] * output[i+1][j]) + (sobel_y[2][0] * output[i-1][j+1]) + (sobel_y[2][1] * output[i][j+1]) + (sobel_y[2][2] * output[i+1][j+1]));
					
					G[i][j] = (Math.abs(Gx[i][j]) + Math.abs(Gy[i][j]));
					
				}
			}
		}
		
		// Saco los m�x y min para determinar LUEGO LAS DIVISIONES Y NIVEL DE DETALLE.
		int counter1 = 0;
		for (int yy = 0; yy < height; yy++) {
			for (int xx = 0; xx < width; xx++) {
				pixels[counter1] = (int) G[xx][yy];
				counter1 = counter1 + 1;
			}
		}
		
		BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		outImg.getRaster().setPixels(0, 0, width, height, pixels);

		return outImg;
	}
	
	//---------------------------------------------------------------------------------------------------------------------------------
	
	public static void main(String[] args) throws IOException, NotBoundException, InterruptedException, TimeoutException, ClassNotFoundException {

		int portServer = 80;
		SobelClient sobel1  = new SobelClient(portServer);
		System.out.println("----- SobelClient iniciado -----");
				
		//System.out.println("Ingrese la ruta de la imagen a modificar: ");
		//Scanner scannerImage = new Scanner(System.in);
		//String pathImage =  scannerImage.nextLine();
		String pathImage = "images/pc1.jpg";  //Imagen existente en el proyecto. Descomentar esta linea y comentar las 3 anteriores para pruebas rapidas.
		
		boolean salir= false;
		do {
			System.out.println("MENU");
			System.out.println("1. Sobel Local (a)");
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
