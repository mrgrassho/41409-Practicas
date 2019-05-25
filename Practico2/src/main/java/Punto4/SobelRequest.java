package Punto4;

import java.awt.image.BufferedImage;

public class SobelRequest {

		private int idWorker;
		private int qSection;
		private int actualSection;
		private BufferedImage inImg;		
		//private BufferedImage outImg;				
		
		public SobelRequest (int idWorker, int qSection, int actualSection, BufferedImage inImg) {
			this.idWorker = idWorker;
			this.qSection = qSection;
			this.actualSection = actualSection;
			this.inImg = inImg;
		}

		public int getIdWorker() {return this.idWorker;}
		public int getqSection() {return this.qSection;}
		public int getActualSection() {return this.actualSection;}
		public BufferedImage getInImg() {return this.inImg;}
		//public BufferedImage getOutImg() {return this.outImg;}

		//public void setOutImg(BufferedImage outImg) {this.outImg = outImg;}

}
