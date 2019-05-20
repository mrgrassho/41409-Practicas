package Punto4;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

import Punto4.RemoteInt;


public class SobelMain {
				
				
	public String sobel(String pathImage, String tipoSobel) throws IOException, NotBoundException, InterruptedException {		

		String entryPath = pathImage;
		
		int pos = entryPath.indexOf(".");
		String nameImage = entryPath.substring(0, pos);
		String extensionImage = entryPath.substring(pos, entryPath.length());;		
		
		String outputPath = nameImage + "-Sobel"+ extensionImage;
	
		long start = System.currentTimeMillis();
		
		FileInputStream inFile = new FileInputStream(entryPath);
		BufferedImage inImg = ImageIO.read(inFile);
		BufferedImage outImg=null;
		BufferedImage outImg2=null;
		
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
		if (tipoSobel.equals("distribuido")) {
			// Teniendo en cuenta el tamaño de imagen la parte en N pedazos.
			// inImg = inImg.getSubimage(inImg.getMinX(), inImg.getMinY(), 300, 300); //funcion para partir la imagen.
			
			
		}else {
			
			if (tipoSobel.equals("local")) {
				Registry clientRMI = LocateRegistry.getRegistry("localhost", 80);
				System.out.println("----- Cliente conectado a un 1 Server implementer-----");
				RemoteInt ri = (RemoteInt) clientRMI.lookup("sobelImageV1");
				ByteArrayOutputStream imgSend = new ByteArrayOutputStream();
				ImageIO.write(inImg, "jpg", imgSend);
				byte[] imgResult = ri.sobel(imgSend.toByteArray());
				outImg = ImageIO.read(new ByteArrayInputStream(imgResult));
			}
		}
		//--------------------------------------------
		
		FileOutputStream outFile = new FileOutputStream(outputPath);
		ImageIO.write(outImg, "JPG", outFile);
		
		long end = System.currentTimeMillis();
		System.out.println(" ELAPSED TIME: "+(end-start));
		return outputPath;
	}			
				
	
	
	
    /*
     *  SOBEL DISTRIBUIDO: 
     *  1. Teniendo en cuenta el tamaño de imagen la parte en N pedazos
     *  2. Por cada pedazo hace un nuevo ThreadSobel() 
     *  	3. Cada ThreadSobel aplica la funcion SobelLocal segun la parte de la imagen que corresponda.
     *  4. SobelDistribuido (o algun subproceso) agrupa las partes de nuevo. 
     */
	
	
	
	public static void main(String[] args) throws IOException, NotBoundException, InterruptedException {

		/*		
		System.out.println("Ingrese la ruta de la imagen a modificar: ");
		Scanner scannerImage = new Scanner(System.in);
		String pathImage =  scannerImage.nextLine();
		*/
		String pathImage = "images/pc1.jpg";  //Imagen existente en el proyecto. Descomentar esta linea y comentar las 3 anteriores para pruebas rapidas.
		SobelMain sobel1  = new SobelMain();
		boolean salir= false;
		do {
			System.out.println("MENU");
			System.out.println("1. Sobel Local (a)");
			System.out.println("2. Sobel Distribuido (b)");
			System.out.println("2. Sobel Distribuido mejorado (c)");					
			System.out.println("0. Terminar");
			System.out.println("Ingrese una opcion>");
			//Scanner scanner = new Scanner(System.in);
			//Int op =  scanner.nextInt();
			int op = 1;
			String pathResultado;
			switch(op) {
			case 1:
				pathResultado = sobel1.sobel(pathImage,"local");
				if (pathResultado!=null) {
					System.out.println("Se ha hecho la operacion exitosamente.");
					System.out.println("La nueva imagen se ubica dentro del proyecto en la ruta: " + pathResultado);
					salir=true;
					break;
				}	
			case 2:
				pathResultado = sobel1.sobel(pathImage,"distribuido");
				salir=true;
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
