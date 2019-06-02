package punto1.peer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
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
import punto1.utils.PeerException;
import punto1.utils.StoredFile;

public class PeerClient implements Runnable{
	private final Logger log = LoggerFactory.getLogger(PeerClient.class);
	private static final String MASTER_INFO = "src/main/java/punto1/peer/resources/master-info.json";
	private static final String LOCAL_FILES_INFO = "src/main/java/punto1/peer/resources/local-files-info.json";
	private Gson gson = new Gson();
	private Set<Master> masters;
	private Set<StoredFile> storedFiles;
	private Master master;
	private BufferedReader inputChannel;
	private PrintWriter outputChannel;
	private Socket socketMaster;
	private Iterator<Master> iteratorMaster;
	static Scanner scanner = new Scanner(System.in);
	private static final Type SET_MASTER_TYPE = new TypeToken<Set<Master>>(){}.getType();
	private static final Type SET_STOREDFILE_TYPE = new TypeToken<Set<StoredFile>>(){}.getType();
	private static final int TRYS_IN_PEER = 3;
	private static final long RETRY_SLEEP_INTERVAL = 3000;
	private int portPeerServer;
	private String ipPeerServer;


	PeerClient(int portPeerServer, String ipPeerServer){
		try {
			this.portPeerServer = portPeerServer;
			this.ipPeerServer = ipPeerServer;
			JsonReader br;
			br = new JsonReader(new FileReader(MASTER_INFO));
			masters = null;
			if (br.hasNext()) {
				masters = gson.fromJson(br, SET_MASTER_TYPE);
			}
			br = new JsonReader(new FileReader(LOCAL_FILES_INFO));
			if (br != null) {
				storedFiles = gson.fromJson(br, SET_STOREDFILE_TYPE);
			}
			this.iteratorMaster = masters.iterator();
			this.master = getNextMaster();
			configMaster();
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.err.println("El archivo NO existe.");
		} catch (JsonIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonSyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		sf = (sf == null)?new HashSet<>(): sf;
		if (!sf.isEmpty()) {
			for (StoredFile s : sf) {
				tmp += s.getChecksum() +",";
			}
			tmp = tmp.substring(0, tmp.length()-1);
		}
		return tmp;
	}

	private String extractFileNames(Set<StoredFile> sf) {
		String tmp = "";
		sf = (sf == null)?new HashSet<>(): sf;
		if (!sf.isEmpty()) {
			for (StoredFile s : sf) {
				tmp += s.getName() +",";
			}
			tmp = tmp.substring(0, tmp.length()-1);
		}
		return tmp;
	}

	public void addFileToShare(StoredFile f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(LOCAL_FILES_INFO));
		FileWriter wr = new FileWriter(LOCAL_FILES_INFO);
		// Process
		if (br.ready() ) {
			Set<StoredFile> fp = gson.fromJson(br, new TypeToken<Set<StoredFile>>(){}.getType());
			fp.add(f);
			String str = gson.toJson(fp);
			wr.write(str);
		}
		wr.close();
		br.close();
	}

	public void showFilesToShare() throws IOException {
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

	public void delFileToShare(StoredFile f) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(LOCAL_FILES_INFO));
		FileWriter wr = new FileWriter(LOCAL_FILES_INFO);
		// Process
		if (br.ready() ) {
			Set<StoredFile> fp = gson.fromJson(br, new TypeToken<Set<StoredFile>>(){}.getType());
			fp.remove(f);
			String str = gson.toJson(fp);
			wr.write(str);
		}
		wr.close();
		br.close();
	}

	public void announce() throws IOException {
		Message m = new Message("announce");
		
		String fileChcks = extractFileChecksums(storedFiles);
		String fileNames = extractFileNames(storedFiles);
		if (fileChcks != "" && fileNames != "") {
			String[] arr1 = fileChcks.split(",");
			String[] arr2 = fileNames.split(",");
			System.out.println("Anunciando al master (archivos):");
			System.out.println("\t[  MD5  ]\t[      FILENAME      ]");
			for (int i = 0; i < arr2.length; i++) {
				System.out.println("\t"+arr1[i].substring(0, 8) + "\t" + arr2[i]);
			}
			m.setParametro("file-checksums", fileChcks);
			m.setParametro("file-names", fileNames);
			m.setParametro("port", String.valueOf(portPeerServer));
			m.setParametro("ip", String.valueOf(ipPeerServer));
			String json = gson.toJson(m);
			this.outputChannel.print(json);
			json = this.inputChannel.readLine();
			Message msg = gson.fromJson(json, Message.class);
			log.info(" " + msg.getParametro("ack"));
		} else {
			log.info("No hay archivos para anunciar al master.");
		}
	}

	private Set<Peer> findPeerWithFile(String name) throws IOException{
		return findPeerWithFile(name, null);
	}

	private Set<Peer> findPeerWithFile(String name, String chksum) throws IOException{
		Message m = new Message("find");
		if (name != null) m.setParametro("name", name);
		if (chksum != null) m.setParametro("checksum", chksum);
		String json = gson.toJson(m);
		outputChannel.println(json);
		json = inputChannel.readLine();
		Message message = gson.fromJson(json, Message.class);
		String[] peerId;
		Set<Peer> s = new HashSet<>();
		if (message.getParametro("peers") != null) { 
			peerId = message.getParametro("peers").split(",");
			for (String str : peerId) {
				String ip = str.split(":")[0];
				String port = str.split(":")[1];
				s.add(new Peer(ip, port));
			}
		}
		return s;
	}
	
	private void findFiles(String name) throws IOException{
		Message m = new Message("find");
		if (name != null) m.setParametro("name", name);
		String json = gson.toJson(m);
		outputChannel.println(json);
		json = inputChannel.readLine();
		Message message = gson.fromJson(json, Message.class);
		String[] fileNames;
		String[] chcks;
		Set<Peer> s = new HashSet<>();
		if (message.getParametro("file-names") != null) { 
			fileNames = message.getParametro("file-names").split(",");
			chcks = message.getParametro("file-checksums").split(",");
			System.out.println("Archivos disponibles para descargar: ");
			System.out.println("\t[  MD5  ]\t[      FILENAME      ]");
			for (int i = 0; i < fileNames.length; i++) {
				System.out.println("\t"+fileNames[i].substring(0, 8) + "\t" + chcks[i]);
			}
		}
	}


	public void downloadFile(String pathname, Peer peer) throws ConnectException, NumberFormatException, UnknownHostException, IOException, PeerException {
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
					System.out.print("#");
				}
				in.close();
				out.close();
			} catch (Exception e) {
				s.close();
				throw new PeerException("Error en la descarga!");
			}
		}
		json = inPeer.readLine();
		m = gson.fromJson(json, Message.class);
		System.out.println("Estado descarga: " + m.getParametro("status"));
		s.close();
	}

	public void menuCli() {
		System.out.println();
		System.out.println("Opciones sobre Red P2P:\n");
		System.out.println("\tdownload [filename]\t\tDescarga archivos por nombre o checksum.");
		System.out.println("\tfind [*|filename|chksum]\t\tBusca archivos por nombre o checksum.");
		System.out.println("\nOpciones para administrar archivos (locales) a compartir sobre red P2P:\n");
		System.out.println("\tadd [path-file]\t\t\tAgrega el archivo a la lista de archivos compartidos.");
		System.out.println("\tdelete [path-file]\t\tBorra el archivo de la lista de archivos compartidos.");
		System.out.println("\tshow-files\t\t\tMuestra todos los archivos disponibles para compartir.");
		System.out.println("\n\thelp\t\t\t\tMuestra este mensaje.");
		System.out.println("\texit\t\t\t\tSalir.");
		System.out.println();
	}

	public String[] splitArgs(String command) {
		command = command.trim();
		return command.split(" ");
	}

	public void end() throws IOException {
	}

	public void interpretCmd(String line) throws Exception  {
		String[] args = splitArgs(line);
		String command = args[0];
		if (command.equals("download")) {
			String filename = args[1];
			Set<Peer> set = findPeerWithFile(filename);
			Peer[] p = set.toArray(new Peer[set.size()]);
			boolean downloadOk = false;
			int i = 0;
			int intento = 0;
			if (p.length > 0) {
				while (!downloadOk) {
					try {
						if (intento++ > TRYS_IN_PEER) {
							i = (i==p.length)?0:i++;
							intento = 0;
						}
						downloadFile(filename, p[i]);
						downloadOk = true;
					} catch (PeerException e) {
						System.err.println(" [!] Error en la descarga. Intentando con Peer:" + p[i].getPeerId());
						i++;
						if (i==p.length) break;
						Thread.sleep(RETRY_SLEEP_INTERVAL);
					} catch (ConnectException e) {
						System.err.println(" [!] No se pudo conectar. Peer ("+p[i].getPeerId()+") caido!");
						i++;
						if (i==p.length) break;
						Thread.sleep(RETRY_SLEEP_INTERVAL);
					}  catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				System.out.println(" [!] No se encontró el archivo especificado.");
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
			showFilesToShare();
		} else if (command.equals("help")) {
			menuCli();
		} else if (command.equals("exit")) {
			throw new Exception("Ha salido correctamente.");
		} else {
			System.err.println("Opcion incorrecta!");
		}
	}
	
	void startClient() {
		try {
			this.announce();
			scanner = new Scanner(System.in);
			System.out.println("Ingrese - help - para ver las opciones.");
			while(true) {
				System.out.print("> ");
				this.interpretCmd(scanner.nextLine());
			}
		} catch (ConnectException e) {
			System.err.println(" [!] No se pudo conectar, Master caido!");
		} catch (Exception f) {
			System.err.println(f);
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		startClient();
	}

}
