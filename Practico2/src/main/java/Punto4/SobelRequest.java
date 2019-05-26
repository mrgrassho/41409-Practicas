package Punto4;

import java.awt.image.BufferedImage;

public class SobelRequest {

		private int idWorker;
		private int qSector;
		private int currentSector;
		private int portWorker;
		private BufferedImage inParcialImg;		
		private RemoteInt ri;
		//private BufferedImage outImg;				
		
		public SobelRequest (int idWorker,int portWorker, int qSector, int currentSector, BufferedImage inParcialImg, RemoteInt ri) {
			this.idWorker = idWorker;
			this.portWorker = portWorker;
			this.qSector = qSector;
			this.currentSector = currentSector;
			this.inParcialImg = inParcialImg;
			this.ri = ri;
		}

		public int getIdWorker() {return this.idWorker;}
		public int getPortWorker() {return this.portWorker;}
		public int getqSection() {return this.qSector;}
		public int getActualSection() {return this.currentSector;}
		public BufferedImage getInParcialImg() {return this.inParcialImg;}
		public RemoteInt getRI() {return this.ri;}
		
		//public BufferedImage getOutImg() {return this.outImg;}
		//public void setOutImg(BufferedImage outImg) {this.outImg = outImg;}

}
