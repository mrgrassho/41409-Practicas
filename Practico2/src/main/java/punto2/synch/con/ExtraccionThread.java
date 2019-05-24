package punto2.synch.con;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;

import com.google.gson.Gson;

public class ExtraccionThread implements Runnable {
	private String filename;
	private Logger log;
	private Socket client;
	private Gson gson;

	ExtraccionThread(String filename, Logger log, Gson gson, Socket client){
		this.filename = filename;
		this.log = log;
		this.client = client;
		this.gson = gson;
	}

	@Override
	public void run() {
		try {

			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (this.client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (this.client.getOutputStream(),true);
			log.info("Esperando extracción...");
			String str;
			while((str = inputChannel.readLine()) != null) {
				Double monto = gson.fromJson(str, Double.class);
				log.info(" [-] Nueva extracion por $" + monto );
				BufferedReader br = new BufferedReader(new FileReader(filename));;
				synchronized (br) {
					Double saldo = new Double(br.readLine());
					log.info(" [-] *Antes* de Extraccion -> Monto:" + monto + ", Saldo:" + saldo);
					if (saldo >= monto) {
						saldo -= monto;
						try {
							Thread.sleep(80);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						FileWriter writer = new FileWriter(filename);
						writer.write(String.valueOf(saldo), 0, String.valueOf(saldo).length());
						String json = gson.toJson("Transaccion Exitosa! Saldo Actual: "+saldo);
						outputChannel.print(json);
						log.info(" [-] Extraccion Exitosa!");
						writer.close();
					} else {
						String json = gson.toJson("Transaccion Rechazada! Saldo Actual: "+saldo);
						outputChannel.print(json);
						log.info(" [-] Extraccion Rechazada! Saldo insuficiente!");
					}
					log.info(" [-] *Despues* de Extraccion -> Monto:" + monto + ", Saldo:" + saldo);
					log.info("Esperando extracción...");
					br.close();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
