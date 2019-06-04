package punto1.peer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import punto1.utils.FilesAtPeers;
import punto1.utils.Message;
import punto1.utils.Seeder;
import punto1.utils.StoredFile;

public class PeerServerThread implements Runnable {

	private Logger log;
	private Gson gson;
	private Socket client;
	private String LOCAL_FILES_INFO;


	public PeerServerThread(Logger log, Gson gson, Socket client, String LOCAL_FILES_INFO){
		this.log = log;
		this.gson = gson;
		this.client = client;
		this.LOCAL_FILES_INFO = LOCAL_FILES_INFO;
	}

	@Override
	public void run() {
		try {
			BufferedReader inputChannel = new BufferedReader (new InputStreamReader (client.getInputStream()));
			PrintWriter outputChannel = new PrintWriter (client.getOutputStream(),true);
			log.info(" [PEER_SERVER] Waiting for CMDs...");
			String msg;
			int index;
			InputStream in = null;
			while((msg = inputChannel.readLine()) != null) {
				Message decodedMsg = gson.fromJson(msg, Message.class);
				if (decodedMsg.getCmd().equals("get-file")) {
					log.info(" [PEER_SERVER] - [GET] Msg Arrived.");
					StoredFile sf = findFile(decodedMsg);
					if (sf != null) {
						log.info(" [PEER_SERVER] - [GET] File found.");
						Message m = new Message("peer-data");
						m.setParametro("status", "OK");
						m.setParametro("checksum", sf.getChecksum());
						m.setParametro("name", sf.getName());
						String json = gson.toJson(m);
						outputChannel.println(json);
						sendFile(sf);
					} else {
						log.info(" [PEER_SERVER] - [GET] File NOT found.");
						Message m = new Message("peer-error");
						m.setParametro("status", "FAILED - File Not Found!");
						m.setParametro("descrip", "File Not Found!");
						String json = gson.toJson(m);
						outputChannel.println(json);
					}

				}
			}
			client.close();
		}  catch (SocketException s) {
			log.info(" [!] - Client disconnected.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendFile(StoredFile sf) throws IOException {
		OutputStream out = client.getOutputStream();
		FileInputStream in = null;
		if (sf != null) {
			File file = new File(sf.getPathname());
			// Get the size of the file
			long length = file.length();
			byte[] bytes = new byte[16 * 1024];
			in = new FileInputStream(file);
			log.info(" [PEER_SERVER] - [GET] Sending file " + sf.getChecksum() + " (" + sf.getName()+")");
			int count;
			while ((count = in.read(bytes)) > 0) {
				out.write(bytes, 0, count);
			}
			log.info(" [PEER_SERVER] - [GET] File sent correctly.");
		}
		out.close();
		in.close();
	}

	private StoredFile findFile(Message m) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(LOCAL_FILES_INFO));
		ArrayList<StoredFile> fp = gson.fromJson(br, new TypeToken<ArrayList<StoredFile>>(){}.getType());
		StoredFile p = null;
		String nombre = m.getParametro("name");
		String chk = m.getParametro("checksum");
		if (nombre != null && chk != null) {
			for (StoredFile fap : fp) {
				if (fap.getName().equals(nombre) && fap.getChecksum().equals(chk)) {
					p = fap;
				}
			}
		} else if (nombre != null) {
			for (StoredFile fap : fp) {
				if (fap.getName().equals(nombre)) {
					p = fap;
				}
			}
		} else if (chk != null) {
			for (StoredFile fap : fp) {
				if (fap.getChecksum().equals(chk)) {
					p = fap;
				}
			}
		}
		br.close();
		return p;
	}

}
