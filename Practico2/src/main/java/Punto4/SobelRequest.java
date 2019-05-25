package Punto4;

import java.awt.image.BufferedImage;

public class SobelRequest {

		private int qSection;
		private int actualSection;
		private BufferedImage inImg;		
		
		public SobelRequest ( int qSection, int actualSection, BufferedImage inImg) {
			this.qSection = qSection;
			this.actualSection = actualSection;
			this.inImg = inImg;
		}

}
