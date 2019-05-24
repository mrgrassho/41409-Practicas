package punto2.synch.con;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import com.google.gson.Gson;

public class Cliente {
	Gson gson = new Gson();
	private String ipExtraccion;
	private int portExtraccion;
	private String ipDeposito;
	private int portDeposito;
	Socket s;
	Socket s2 ;
	BufferedReader inputChannel;
	PrintWriter outputChannel;
	BufferedReader inputChannel2 ;
	PrintWriter outputChannel2;

	public Cliente (String ipExtraccion, int portExtraccion,String ipDeposito, int portDeposito) {
		this.ipExtraccion = ipExtraccion;
		this.portExtraccion = portExtraccion;
		this.ipDeposito = ipDeposito;
		this.portDeposito = portDeposito;
	}

	public void openSocketExtraccion() throws UnknownHostException, IOException {
		s = new Socket (ipExtraccion, portExtraccion);
		inputChannel = new BufferedReader (new InputStreamReader (s.getInputStream()));
		outputChannel = new PrintWriter (s.getOutputStream(),true);
	}

	public void openSocketDeposito() throws UnknownHostException, IOException {
		s2 = new Socket (ipDeposito, portDeposito);
		inputChannel2 = new BufferedReader (new InputStreamReader (s2.getInputStream()));
		outputChannel2 = new PrintWriter (s2.getOutputStream(),true);
	}

	public void extraccion(Double monto) throws UnknownHostException, IOException {

			System.out.println(" Client connection ok");
			String json = gson.toJson(monto);
			System.out.println(json);
			outputChannel.println(json);
			//String response = inputChannel.readLine();
			//System.out.println("> " + gson.fromJson(response, String.class));
	}

	public void deposito(Double monto) throws UnknownHostException, IOException {

		System.out.println(" Client connection ok");
		String json = gson.toJson(monto);
		System.out.println(json);
		outputChannel2.println(json);
		//String response = inputChannel2.readLine();
		//System.out.println("> " + gson.fromJson(response, String.class));
	}
}
