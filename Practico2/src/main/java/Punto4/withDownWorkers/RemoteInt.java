package Punto4.withDownWorkers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RemoteInt extends Remote{
	
	public byte[] sobelDistribuido (byte[] image) throws IOException, InterruptedException, NotBoundException, ClassNotFoundException;
}
