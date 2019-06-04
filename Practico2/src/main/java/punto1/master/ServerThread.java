package punto1.master;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import punto1.utils.FilesAtPeers;
import punto1.utils.Message;
import punto1.utils.Seeder;
import punto1.utils.StoredFile;

public class ServerThread implements Runnable {
	private String PEER_INFO;
	private String FILES_INFO;
	private static final Type ARR_PEER_INFO = new TypeToken<ArrayList<Seeder>>(){}.getType();
	private static final Type ARR_FILES_INFO = new TypeToken<ArrayList<FilesAtPeers>>(){}.getType();
	private Socket client;
	private Gson gson;
	private Logger log;
	
	public ServerThread(Logger log, Gson gson, Socket client, String PEER_INFO, String FILES_INFO) {
		this.log = log;
		this.gson = gson;
		this.client = client;
		this.PEER_INFO = PEER_INFO;
		this.FILES_INFO = FILES_INFO;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (client.getOutputStream(),true);
			log.info(" [MASTER] Waiting for CMDs...");
			String msg;
			int index;
			while((msg = inputChannel.readLine()) != null) {
				Message decodedMsg = gson.fromJson(msg, Message.class);
				if (decodedMsg.getCmd().equals("announce")) {
					// Add peer to PEER INFO file
					// Get announced files from PEER
					log.info(" [MASTER] - [ANNOUNCE] Msg arrived.");
					String[] filesChkToAdd = decodedMsg.getParametro("file-checksums").split(",");
					String[] filesNameToAdd = decodedMsg.getParametro("file-names").split(",");
					String ipToAdd = decodedMsg.getParametro("ip");
					String portToAdd = decodedMsg.getParametro("port");
					Seeder crntPeer = new Seeder(ipToAdd, portToAdd);
					log.info(" [MASTER] - [ANNOUNCE] " + crntPeer.getPeerId() + " has "+ filesChkToAdd.length + " files");
					JsonReader br = new JsonReader(new FileReader(FILES_INFO));
					JsonReader br2 = new JsonReader(new FileReader(PEER_INFO));
					ArrayList<FilesAtPeers> fp = gson.fromJson(br, ARR_FILES_INFO);
					ArrayList<Seeder> fp2 = gson.fromJson(br2, ARR_PEER_INFO);
					log.info(" [MASTER] - [ANNOUNCE] Contenido de Archivo('"+ARR_FILES_INFO+"') ->"+gson.toJson(fp));
					log.info(" [MASTER] - [ANNOUNCE] Contenido de Archivo('"+ARR_PEER_INFO+"') ->"+gson.toJson(fp2));
					br.close();
					br2.close();
					FileWriter wr = new FileWriter(FILES_INFO);
					FileWriter wr2 = new FileWriter(PEER_INFO);
					fp = (fp==null)? new ArrayList<>(): fp;
					fp2 = (fp2==null)? new ArrayList<>(): fp2;
					for (int i = 0; i < filesNameToAdd.length; i++) {
						log.info(" [MASTER] - [ANNOUNCE] " + crntPeer.getPeerId() + " announce file. SHA-256:"+ filesChkToAdd[i]);
						// Busco si el archivo existe en el Json
						if ((index = findChk(filesChkToAdd[i], fp)) != -1) {
							// SI existe -> agarro el Element y le agrego el IP y PORT to Add
							fp.get(index).addPeer(crntPeer);
						} else {
							// SINO -> Creo el elemento y le agrego el IP y PORT to Add
							fp.add(new FilesAtPeers(filesChkToAdd[i],filesNameToAdd[i]));
							fp.get(fp.size()-1).addPeer(crntPeer);;
						}
					}
					// TODO: Hacer funcion que borre archivos que no estan más mantenidos
					String str = gson.toJson(fp);
					wr.write(str);
					if (fp2.indexOf(new Seeder(ipToAdd, portToAdd)) == -1) {
						fp2.add(new Seeder(ipToAdd, portToAdd));
					}
					str = gson.toJson(fp2);
					wr2.write(str);
					wr.close();
					wr2.close();
					log.info(" [MASTER] - [ANNOUNCE] Files closed.");
					Message m = new Message("ack");
					String json = gson.toJson(m);
					outputChannel.println(json);
					log.info(" [MASTER] - [ANNOUNCE] Send ACK to client.");
				} else if (decodedMsg.getCmd().equals("find-peer-with-file")) {
					// Find file in PEER INFO file
					log.info(" [MASTER] - [FIND_PEER] Msg arrived.");
					String list = findPeerWithFile(decodedMsg);
					Message m = new Message("peer-info");
					if (!list.isEmpty()) {
						log.info(" [MASTER] - [FIND_PEER] File found in [" + list + "]");
						m.setParametro("peers", list);
					} else log.info(" [MASTER] - [FIND_PEER] File NOT found.");
					String json = gson.toJson(m);
					outputChannel.println(json);
				} else if (decodedMsg.getCmd().equals("find-files-at-peer")) {
					try {
						log.info(" [MASTER] - [FIND_FILE] Msg arrived.");
						Message m = findFilesAtPeer(decodedMsg);
						String json = gson.toJson(m);
						outputChannel.println(json);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private int findChk(String fa, ArrayList<FilesAtPeers> fp) {
		int i = -1;
		if ((fp != null)  && (fa != null)){
			for (int j = 0; j < fp.size(); j++) {
				if (fp.get(j).getChecksum().equals(fa)) {
					i = j;
				}
			}
		}
		return i;
	}

	private Message findFilesAtPeer(Message message) throws Exception {
		JsonReader br = new JsonReader(new FileReader(FILES_INFO));
		Message m = new Message("file-info");
		Set<StoredFile> p = new HashSet<>();
		if (br.hasNext()) {
			ArrayList<FilesAtPeers> fp = gson.fromJson(br, ARR_FILES_INFO);
			fp = (fp == null)?new ArrayList<>():fp;
			String nombre = message.getParametro("name");
			if (nombre == null) throw new Exception("No argument name");
			for (FilesAtPeers fap : fp) {
				if (fap.getName().equals(nombre) || nombre.equals("*")) {
					p.add(new StoredFile(fap.getName(), fap.getChecksum()));
					if (fap.getName().equals(nombre)) break;
				}
			}
			br.close();
		}
		String tmpNames = "";
		String tmpChks = "";
		log.info(" [FIND_FILES_*] Preparing files...");
		for (StoredFile s : p) {
			tmpNames += s.getName()+",";
			tmpChks += s.getChecksum()+",";
			log.info(" [FIND_FILES_*] " + s.getName()+"\t\t"+s.getChecksum());
		}
		tmpNames = (tmpNames!="") ? tmpNames.substring(0, tmpNames.length()-1) : tmpNames;
		tmpChks = (tmpChks!="") ? tmpChks.substring(0, tmpChks.length()-1) : tmpChks;
		m.setParametro("file-names", tmpNames);
		m.setParametro("file-checksums", tmpChks);
		log.info(gson.toJson(m));
		return m;
	}
	
	private String findPeerWithFile(Message m) throws IOException {
		JsonReader br = new JsonReader(new FileReader(FILES_INFO));
		Set<Seeder> p = new HashSet<>();
		if (br.hasNext()) {
			ArrayList<FilesAtPeers> fp = gson.fromJson(br, ARR_FILES_INFO);
			fp = (fp == null)?new ArrayList<>():fp;
			String nombre = m.getParametro("name");
			String chk = m.getParametro("checksum");
			if (nombre != null && chk != null) {
				for (FilesAtPeers fap : fp) {
					if (fap.getName().equals(nombre) && fap.getChecksum().equals(chk)) {
						p = fap.getPeers();
					}
				}
			} else if (nombre != null) {
				for (FilesAtPeers fap : fp) {
					if (fap.getName().equals(nombre)) {
						p = fap.getPeers();
					}
				}
			} else if (chk != null) {
				for (FilesAtPeers fap : fp) {
					if (fap.getChecksum().equals(chk)) {
						p = fap.getPeers();
					}
				}
			}
			br.close();
		}
		String tmp = "";
		for (Seeder peer : p) {
			tmp += peer.getPeerId() +",";
		}
		tmp = (tmp!="") ? tmp.substring(0, tmp.length()-1) : tmp;
		return tmp;
	}

}
