
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

/**
 * Teste de segmentação em lote (blue background) - v200120
 * Alessandra Mendes
 * Salvamento da máscara em Tiff - 07/01/2020
 * Deve existir uma pasta de saída c:\Results para armazenamento dos aquivos de saída
**/
public class backBlue_v2 implements PlugIn {


	public void run(String args) {
		IJ.run("Close All");
			
		ImagePlus imp;
//		ImagePlus impColor, impGray, impSegm;
		ImageProcessor ipColor, ipGray;
		ColorProcessor cpRGB;
		
		int lin, col, i, j;
		float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
		int[] RGB = new int[3];
		int[] GRAY = new int[3];
		float hsb[] = new float[3];
		int R = 0, G = 1, B = 2; // indexes
		
        // BACK_BLUE
		IJ.open();
		imp = (ImagePlus) IJ.getImage(); 
		// salvando imagem com bordas
		IJ.saveAs(imp, "Jpeg", "\\Imagens\\_0_orig"); // Original color images
//		impColor = (ImagePlus) imp.clone();
		ipColor = imp.getProcessor();
		col = ipColor.getWidth(); 
		lin = ipColor.getHeight(); 
		IJ.run("8-bit");
//		impGray = new Duplicator().run(imp);
		ipGray = imp.getProcessor();
		// salvando imagem em tons de cinza
		IJ.saveAs(imp, "Jpeg", "\\Imagens\\_1_gray"); // Original color images

		// flag - red edges
		ipGray.findEdges();
		for (j=0; j<col; j++) { 
			for (i=0; i<lin; i++) { 
				ipGray.getPixel(j, i, GRAY);
				ipColor.getPixel(j, i, RGB);
				if(GRAY[0]>100){ 
					RGB[R] = 255;
					RGB[G] = 0;
					RGB[B] = 0;
					ipColor.putPixel(j, i, RGB); 
				}
			}
		}
		// salvando imagem com bordas
		IJ.saveAs(imp, "Jpeg", "\\Imagens\\_2_edges"); // Original color images
		
		// background extraction
		cpRGB = (ColorProcessor) ipColor;
		for (j=0; j<col; j++) { 
			for (i=0; i<lin; i++) { 
				cpRGB.getPixel(j, i, RGB); 
				//Color HUE (model HLS);
				Color.RGBtoHSB(RGB[R], RGB[G], RGB[B], hsb);
				sat = (int) Math.ceil(hsb[1]*100);
				bri = (int) Math.ceil(hsb[2]*100);
				// Minimum and Maximum RGB values are used in the HUE calculation
				min = Math.min(RGB[R], Math.min(RGB[G], RGB[B]));
				max = Math.max(RGB[R], Math.max(RGB[G], RGB[B]));
				// Calculate the Hue
				if (max == min)
					hue = 0;
				else if (max == RGB[R])
					hue = ((60 * (RGB[G] - RGB[B]) / (max - min)) + 360) % 360;
				else if (max == RGB[G])
					hue = (60 * (RGB[B] - RGB[R]) / (max - min)) + 120;
				else if (max == RGB[B])
					hue = (60 * (RGB[R] - RGB[G]) / (max - min)) + 240;
				//valor fixo "10" - automatizar...
				if( ( ((RGB[B]>RGB[G])&&(RGB[B]>RGB[R]))
						&&( (RGB[B]-RGB[R])>10) ) // se o canal azul é o mais alto e a distância entre o canal azul e os demais é de pelo menos 10 intensidades
						&&( ((hue >= 181)&&(hue <= 300)) //se o tom é de azul (do ciano ao azul marinho)
//								 ) ){ // se (saturação >= 20% e brilho >= 20%)
					&&((sat>=20)&&(bri>=20) ) ) ){ // se (saturação >= 20% e brilho >= 20%)
					ipGray.putPixel(j, i, 255); 
				}else {
					ipGray.putPixel(j, i, 0);
				}
			}
		} 

		imp.updateAndDraw(); 
		// salvando imagem com bordas
		IJ.saveAs(imp, "Jpeg", "\\imagens\\_3_color"); // Original color images

	//	ipColor.setAutoThreshold("Otsu");  comentado em 13/05/20 e não replicado para a UniCA - redundante
	//	IJ.run("Convert to Mask"); comentado em 13/05/20 e não replicado para a UniCA - redundante
	//	IJ.run("Make Binary"); comentado em 13/05/20 e não replicado para a UniCA - redundante
		IJ.run("Fill Holes"); 
		IJ.saveAs(imp, "Jpeg", "\\Imagens\\_4_filling"); // Original color images
		IJ.run("Set Measurements...", "area"); 
 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
 		TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
	    ResultsTable rt = tw.getTextPanel().getResultsTable(); 
	    float areasMedia[] = rt.getColumn(0);
	    maior = 0;
	    for (i=0; i<areasMedia.length; i++)
	    	if (areasMedia[i]>maior)
	    		maior = areasMedia[i];
		tw.close(false); 
		IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");
		IJ.run("Convert to Mask"); 

		imp = IJ.getImage();
//		impSegm = new Duplicator().run(imp);
		// salvando imagem segmentada
		IJ.saveAs(imp, "Jpeg", "\\Imagens\\_5_segm"); // Original color images
		
		// closing...
//		IJ.getImage().changes = false;
//		IJ.getImage().close();
//		IJ.getImage().changes = false;
//		IJ.getImage().close();
			
		TextWindow tw2 = (TextWindow)WindowManager.getFrame("Summary"); 
		//tw2.close(false); 

   }
}