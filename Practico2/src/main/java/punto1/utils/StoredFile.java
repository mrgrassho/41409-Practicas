package punto1.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StoredFile {
	private String checksum;
	private String name;
	private String pathname;

	public StoredFile(String pathname) throws NoSuchAlgorithmException, IOException {
		String[] f = pathname.split("/");
		this.setName(f[f.length-1]);
		this.setPathname(pathname);
		this.setChecksum(calculateChecksum(pathname));
	}

	private String calculateChecksum(String pathname) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = null;
		DigestInputStream dis = null;
		md = MessageDigest.getInstance("MD5");
		InputStream is = Files.newInputStream(Paths.get(pathname));
		dis = new DigestInputStream(is, md);
		return dis.getMessageDigest().digest().toString();
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPathname() {
		return pathname;
	}

	public void setPathname(String pathname) {
		this.pathname = pathname;
	}
}
