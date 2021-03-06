package Punto4;

import java.awt.image.BufferedImage;
import java.io.Serializable;

public class SobelRequest implements Serializable{

		private int idWorker;

		private byte[] inParcialImg;		
		private byte[] outImg;
		
		public SobelRequest (int idWorker, byte[] inParcialImg) {
			this.idWorker = idWorker;
			this.inParcialImg = inParcialImg;
		}

		public int getIdWorker() {return this.idWorker;}
		public byte[] getInParcialImg() {return this.inParcialImg;}
		public byte[] getOutImg() {return this.outImg;}
		
		public void setOutImg(byte[] outImg) {this.outImg = outImg;}
		public void setIdWorker(int idWorker) {this.idWorker = idWorker;}
}
