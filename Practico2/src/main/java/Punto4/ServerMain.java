package Punto4;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {

	private String ip;
	private int port;
	private int idServer;
	private final static Logger log = LoggerFactory.getLogger(ServerMain.class);
	
	public ServerMain(int id, String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.idServer = id;
	}
	
		public void startServer(String nameService, int portService) throws RemoteException {

				log.info("Server ["+this.idServer+"] iniciado");
			
				ServerImplementerSobel si = new ServerImplementerSobel();
				log.info("-----Server ["+this.idServer+"] Implementador Sobel instanciado -----");
				
				Registry server = LocateRegistry.createRegistry(this.port);
				log.info("-----Server ["+this.idServer+"] Servicio RMI Iniciado en puerto "+this.port+"-----");
				
				RemoteInt serviceSobel = (RemoteInt) UnicastRemoteObject.exportObject(si, portService);
				log.info("-----Server ["+this.idServer+"] serviceSobel asociado a puerto "+portService+" -----");
				
				server.rebind(nameService, serviceSobel);
				log.info("-----Server ["+this.idServer+"] bind de servicio JNDI realizado -----");
				
				log.info("-----Server ["+this.idServer+"] proceso starServer finalizado correctamente.");
		}
		
		

}