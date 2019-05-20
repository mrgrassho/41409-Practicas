package Punto4;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RemoteInt extends Remote{
	
	public byte[] sobel (byte[] image) throws IOException, InterruptedException;
}
