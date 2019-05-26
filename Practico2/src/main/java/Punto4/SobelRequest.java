package Punto4;

import java.awt.image.BufferedImage;
import java.io.Serializable;

public class SobelRequest implements Serializable{

		private int idWorker;
		private int qSector;
		private int currentSector;
		private int portWorker;
		private byte[] inParcialImg;		
		private byte[] outImg;
		
		public SobelRequest (int idWorker,int portWorker, int qSector, int currentSector, byte[] inParcialImg) {
			this.idWorker = idWorker;
			this.portWorker = portWorker;
			this.qSector = qSector;
			this.currentSector = currentSector;
			this.inParcialImg = inParcialImg;
		}

		public int getIdWorker() {return this.idWorker;}
		public int getPortWorker() {return this.portWorker;}
		public int getqSection() {return this.qSector;}
		public int getActualSection() {return this.currentSector;}
		public byte[] getInParcialImg() {return this.inParcialImg;}
		
		public byte[] getOutImg() {return this.outImg;}
		public void setOutImg(byte[] outImg) {this.outImg = outImg;}

}
