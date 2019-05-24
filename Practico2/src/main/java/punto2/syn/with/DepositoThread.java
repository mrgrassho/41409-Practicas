package punto2.syn.with;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;

import com.google.gson.Gson;

public class DepositoThread implements Runnable {
	private String filename;
	private Logger log;
	private Socket client;
	private Gson gson;

	DepositoThread(String filename, Logger log, Gson gson, Socket client){
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
			log.info("Esperando Deposito...");
			String k;
			while((k = inputChannel.readLine()) != null) {
				Double monto = gson.fromJson(k, Double.class);
				log.info(" [-] Nueva Deposito por $" + monto );
				BufferedReader br = new BufferedReader(new FileReader(filename));
				synchronized (br) {
					Double saldo = new Double(br.readLine());
					log.info(" [-] *Antes* de Deposito -> Monto:" + monto + ", Saldo:" + saldo);
					saldo += monto;
					try {
						Thread.sleep(40);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					FileWriter writer = new FileWriter(filename);
					writer.write(String.valueOf(saldo), 0, String.valueOf(saldo).length());
					String json = gson.toJson("Deposito Exitoso! Saldo Actual: "+saldo);
					outputChannel.print(json);
					log.info(" [-] Deposito Exitoso!");
					writer.close();
					log.info(" [-] *Despues* de Deposito -> Monto:" + monto + ", Saldo:" + saldo);
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
