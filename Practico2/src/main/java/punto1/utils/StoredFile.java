package punto1.utils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
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
	
	public StoredFile(String pathname,  String checksum) throws NoSuchAlgorithmException, IOException {
		String[] f = pathname.split("/");
		this.setName(f[f.length-1]);
		this.setPathname(pathname);
		this.setChecksum(checksum);
	}

	private String calculateChecksum(String pathname) throws NoSuchAlgorithmException, IOException {
		MessageDigest md = null;
		DigestInputStream dis = null;
		md = MessageDigest.getInstance("SHA-256");
		InputStream is = Files.newInputStream(Paths.get(pathname));
		dis = new DigestInputStream(is, md);
		return getMessageDigest(dis);
	}

	private static String getMessageDigest(DigestInputStream digestInputStream) {
		MessageDigest digest = digestInputStream.getMessageDigest();
		byte[] digestBytes = digest.digest();
		String digestStr = getHexaString(digestBytes);
		return digestStr;
	}

	private static String getHexaString(byte[] data) {
		String result = new BigInteger(1, data).toString(16);
		return result;
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
