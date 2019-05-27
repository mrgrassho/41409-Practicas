package punto1.peer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import punto1.utils.Master;
import punto1.utils.Message;
import punto1.utils.Peer;
import punto1.utils.StoredFile;

public class PeerClient {
	private final Logger log = LoggerFactory.getLogger(PeerClient.class);
	private static String MASTER_INFO = "src/main/java/punto1/peer/resources/master-info.json";
	private static String LOCAL_FILES_INFO = "src/main/java/punto1/peer/resources/local-files-info.json";
	private static Gson gson = new Gson();
	private Set<Master> masters;
	private Set<StoredFile> storedFiles;
	private Master master;
	private int masterIndex;
	private static BufferedReader inputChannel;
	private static PrintWriter outputChannel;
	private Socket socketMaster;
	private Iterator<Master> iteratorMaster;
	static Scanner scanner = new Scanner(System.in);
	private static final Type SET_MASTER_TYPE = new TypeToken<Set<Master>>(){}.getType();
	private static final Type SET_STOREDFILE_TYPE = new TypeToken<Set<StoredFile>>(){}.getType();


	PeerClient() throws JsonIOException, JsonSyntaxException, IOException{
			JsonReader br;
			br = new JsonReader(new FileReader(MASTER_INFO));
			masters = null;
			if (br.hasNext()) {
				masters = gson.fromJson(br, SET_MASTER_TYPE);
			}
			br = new JsonReader(new FileReader(LOCAL_FILES_INFO));
			if (br != null) {
				Set<StoredFile> storedFiles = gson.fromJson(br, SET_STOREDFILE_TYPE);
			}
			this.iteratorMaster = masters.iterator();
			this.master = getNextMaster();
			configMaster();
			br.close();
	}

	private Master getNextMaster() {
		if (iteratorMaster.hasNext())
			iteratorMaster = masters.iterator();
		return iteratorMaster.next();
	}


	private void configMaster() throws UnknownHostException, IOException, ConnectException {
		this.socketMaster = new Socket(this.master.getIp(), this.master.getPort());
		this.inputChannel = new BufferedReader (new InputStreamReader (socketMaster.getInputStream()));
		this.outputChannel = new PrintWriter (socketMaster.getOutputStream(),true);
	}

	private String extractFileChecksums(Set<StoredFile> sf) {
		String tmp = "";
		for (StoredFile s : sf) {
			tmp += s.getChecksum() +",";
		}
		tmp = tmp.substring(0, tmp.length()-1);
		return tmp;
	}

	private String extractFileNames(Set<StoredFile> sf) {
		String tmp = "";
		for (StoredFile s : sf) {
			tmp += s.getName() +",";
		}
		tmp = tmp.substring(0, tmp.length()-1);
		return tmp;
	}

	public static void addFileToShare(StoredFile f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(LOCAL_FILES_INFO));
		FileWriter wr = new FileWriter(LOCAL_FILES_INFO);
		// Process
		Set<StoredFile> fp = gson.fromJson(br, new TypeToken<Set<StoredFile>>(){}.getType());
		fp.add(f);
		String str = gson.toJson(fp);
		wr.write(str);
		wr.close();
		br.close();
	}

	public static void showFilesToShare() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(LOCAL_FILES_INFO));
		FileWriter wr = new FileWriter(LOCAL_FILES_INFO);
		// Process
		Set<StoredFile> fp = gson.fromJson(br, new TypeToken<Set<StoredFile>>(){}.getType());
		for (StoredFile storedFile : fp) {
			System.out.println("-> NAME:"+storedFile.getName() +" | MD5_CHECKSUM:" + storedFile.getChecksum());
		}
		String str = gson.toJson(fp);
		wr.write(str);
		wr.close();
		br.close();
	}

	public static void delFileToShare(StoredFile f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(LOCAL_FILES_INFO));
		FileWriter wr = new FileWriter(LOCAL_FILES_INFO);
		// Process
		Set<StoredFile> fp = gson.fromJson(br, new TypeToken<Set<StoredFile>>(){}.getType());
		fp.remove(f);
		String str = gson.toJson(fp);
		wr.write(str);
		wr.close();
		br.close();
	}

	public void announce() throws IOException {
		Message m = new Message("announce");
		String fileChcks = extractFileChecksums(storedFiles);
		String fileNames = extractFileNames(storedFiles);
		m.setParametro("file-checksums", fileChcks);
		m.setParametro("file-names", fileNames);
		String json = gson.toJson(m);
		outputChannel.print(json);
		json = inputChannel.readLine();
		Message msg = gson.fromJson(json, Message.class);
		log.info(" [PEER_CLIENT] - [ANNOUNCE] " + msg.getParametro("ack"));
	}

	private static Set<Peer> findFile(String name) throws IOException{
		return findFile(name, null);
	}

	private static Set<Peer> findFile(String name, String chksum) throws IOException{
		Message m = new Message("find");
		if (name != null) m.setParametro("name", name);
		if (chksum != null) m.setParametro("checksum", chksum);
		String json = gson.toJson(m);
		outputChannel.println(json);
		json = inputChannel.readLine();
		Message message = gson.fromJson(json, Message.class);
		String[] peerId = message.getParametro("peers").split(",");
		Set<Peer> s = new HashSet<>();
		for (String str : peerId) {
			String ip = str.split(":")[0];
			String port = str.split(":")[1];
			s.add(new Peer(ip, port));
		}
		return s;
	}

	public static void downloadFile(String pathname, Peer peer) throws IOException {
		Socket s = new Socket(peer.getIp(), Integer.parseInt(peer.getPort()));
		BufferedReader inPeer = new BufferedReader (new InputStreamReader (s.getInputStream()));
		PrintWriter outputPeer = new PrintWriter (s.getOutputStream(),true);
		
		// Comunicarse con el Peer con el comando GET
		Message m = new Message("get-file");
		m.setParametro("name", pathname);
		String json = gson.toJson(m);
		outputPeer.print(json);
		if (s.isConnected()) {
			try {
				InputStream in = s.getInputStream();
				OutputStream out = new FileOutputStream(pathname);
				byte[] bytes = new byte[16*1024];
				int count;
				while ((count = in.read(bytes)) > 0) {
					out.write(bytes, 0, count);
				}
				in.close();
				out.close();
			} catch (Exception e) {
				System.out.println("Error en la descarga!");
			}
		}
		json = inPeer.readLine();
		m = gson.fromJson(json, Message.class);
		System.out.println("Estado descarga: " + m.getParametro("status"));
	}

	public static void menuCli(int id) {
		System.out.println();
		System.out.println("Bandeja del Cliente <+=+> " + String.valueOf(id));
		System.out.println("download [filename]\t\tDescarga archivos por nombre o checksum.");
		System.out.println("add [path-file]\t\t\tAgrega el archivo a la lista de archivos compartidos.");
		System.out.println("delete [path-file]\t\t\tBorra el archivo de la lista de archivos compartidos.");
		System.out.println("find [filename|chksum]\t\tBusca archivos por nombre o checksum.");
		System.out.println("show-files\t\t\tMuestra todos los archivos disponibles.");
		System.out.println("help\t\t\tMuestra este mensaje.");
		System.out.println("exit\t\t\tSalir.");
		System.out.println();
	}

	public static String[] splitArgs(String command) {
		command = command.trim();
		return command.split(" ");
	}

	public static void end() throws IOException {
	}

	public static void interpretCmd(String line) throws Exception  {
		String[] args = splitArgs(line);
		String command = args[0];
		if (command.equals("download")) {
			String filename = args[1];
			Set<Peer> p = findFile(filename);
			try {
				downloadFile(filename, (Peer) p.toArray()[0]);
			} catch (Exception e) {

			}
		} else if (command.equals("find")) {

		}else if (command.equals("add")) {
			String pathname = args[1];
			try {
				addFileToShare(new StoredFile(pathname));
			} catch (IOException e) {
				System.out.println("El archivo especifado no existe.");
			}
		}else if (command.equals("del")) {
			String pathname = args[1];
			try {
				delFileToShare(new StoredFile(pathname));
			} catch (IOException e) {
				System.out.println("El archivo especifado no existe.");
			}
		} else if (command.equals("show-files")) {

		} else if (command.equals("help")) {
			menuCli(100);
		} else if (command.equals("exit")) {
			throw new Exception("Ha salido correctamente.");
		} else {
			System.err.println("Opcion incorrecta!");
		}
	}

	public static void main(String[] args) throws Exception {
		try {
			PeerClient pc = new PeerClient();
			pc.announce();
			scanner = new Scanner(System.in);
			System.out.println("Ingrese - help - para ver las opciones.");
			while(true) {
				System.out.print("> ");
				interpretCmd(scanner.nextLine());
			}
		} catch (ConnectException e) {
			System.err.println("No se pudo conectar, Master caido!");
		} catch (Exception f) {
			System.err.println(f);
		}
	}

}
