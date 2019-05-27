package punto1.master;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import punto1.utils.FilesAtPeers;
import punto1.utils.Message;
import punto1.utils.Peer;

public class ServerThread implements Runnable {
	private String PEER_INFO;
	private String FILES_INFO;
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
					Peer crntPeer = new Peer(ipToAdd, portToAdd);
					log.info(" [MASTER] - [ANNOUNCE] " + crntPeer.getPeerId() + " has "+ filesChkToAdd.length + " files");
					BufferedReader br = new BufferedReader(new FileReader(FILES_INFO));
					BufferedReader br2 = new BufferedReader(new FileReader(PEER_INFO));
					FileWriter wr = new FileWriter(FILES_INFO);
					FileWriter wr2 = new FileWriter(PEER_INFO);
					ArrayList<FilesAtPeers> fp = gson.fromJson(br, new TypeToken<ArrayList<FilesAtPeers>>(){}.getType());
					ArrayList<Peer> fp2 = gson.fromJson(br2, new TypeToken<ArrayList<Peer>>(){}.getType());
					for (int i = 0; i < filesNameToAdd.length; i++) {
						log.info(" [MASTER] - [ANNOUNCE] " + crntPeer.getPeerId() + " announce "+ filesNameToAdd[i].substring(0, 8));
						// Busco si el archivo existe en el Json
						if ((index = findChk(filesNameToAdd[i], fp)) != -1) {
							// SI existe -> agarro el Element y le agrego el IP y PORT to Add
							fp.get(index).addPeer(crntPeer);
						} else {
							// SINO -> Creo el elemento y le agrego el IP y PORT to Add
							fp.add(new FilesAtPeers(filesNameToAdd[i], filesChkToAdd[i]));
							fp.get(fp.size()-1).addPeer(crntPeer);;
						}
					}
					// TODO: Hacer funcion que borre archivos que no estan más mantenidos
					String str = gson.toJson(fp);
					wr.write(str);
					if (fp2.indexOf(new Peer(ipToAdd, portToAdd)) == -1) {
						fp2.add(new Peer(ipToAdd, portToAdd));
					}
					str = gson.toJson(fp2);
					wr2.write(str);
					wr.close();
					wr2.close();
					br.close();
					br2.close();
					log.info(" [MASTER] - [ANNOUNCE] Files closed.");
					Message m = new Message("ack");
					String json = gson.toJson(m);
					outputChannel.print(json);
					log.info(" [MASTER] - [ANNOUNCE] Send ACK to client.");
				} else if (decodedMsg.getCmd().equals("find")) {
					// Find file in PEER INFO file
					log.info(" [MASTER] - [FIND] Msg arrived.");
					String list = findFile(decodedMsg);
					Message m = new Message("peer-info");
					m.setParametro("peers", list);
					log.info(" [MASTER] - [FIND] File found in [" + list + "]");
					String json = gson.toJson(m);
					outputChannel.print(json);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private int findChk(String fa, ArrayList<FilesAtPeers> fp) {
		int i = -1;
		for (int j = 0; j < fp.size(); j++) {
			if (fp.get(j).getChecksum().equals(fa)) {
				i = j;
			}
		}
		return i;
	}

	private String findFile(Message m) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(FILES_INFO));
		ArrayList<FilesAtPeers> fp = gson.fromJson(br, new TypeToken<ArrayList<FilesAtPeers>>(){}.getType());
		Set<Peer> p = null;
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
		String tmp = "";
		for (Peer peer : p) {
			tmp += peer.getPeerId() +",";
		}
		tmp = tmp.substring(0, tmp.length()-1);
		return tmp;
	}

}
