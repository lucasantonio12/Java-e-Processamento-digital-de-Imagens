package my_plugins;

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
 * Teste de segmenta??o em lote (blue background) - v18102019
 * Alessandra Mendes
 * Sem Watersheed - 31/10/2019
 * Deve existir uma pasta c:\Results para armazenamento dos aquivos de sa?da
**/
public class backBlue_Lote_v3 implements PlugIn {


	public void run(String args) {

		IJ.run("Close All");
		String directory, fileName;
		List<String> result;
		int k, qtd = 0;
		
		OpenDialog od = new OpenDialog("Selezionare un file tra quelli da analizzare", null);
		directory = od.getDirectory();
		if (null == directory) 
			return;
		try {
			Stream<Path> walk = Files.walk(Paths.get(directory));
			result = walk.filter(Files::isRegularFile)
					.map(x -> x.toString()).collect(Collectors.toList());
			walk.close();

			qtd = result.size();
			
			ImagePlus imp;
			ImagePlus impColor[] = new ImagePlus[qtd];
			ImagePlus impGray[] = new ImagePlus[qtd];
			ImagePlus impSegm[] = new ImagePlus[qtd];
			String fileNames[] = new String[qtd];
			ImageProcessor ipColor, ipGray;
			ColorProcessor cpRGB;
			
			int lin, col, i, j;
			float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
			int[] RGB = new int[3];
			int[] GRAY = new int[3];
			float hsb[] = new float[3];
			int R = 0, G = 1, B = 2; // index
			
			for (k=0; k<qtd; k++) {

				//nome do arquivo
				fileName = (result.get(k).substring(result.get(k).lastIndexOf("\\") + 1));
	            fileName = (fileName.substring(0, fileName.indexOf(".")));
	            fileNames[k] = fileName;
	            
	            // BACK_BLUE
				IJ.open(result.get(k));
				imp = (ImagePlus) IJ.getImage(); 
				impColor[k] = (ImagePlus) imp.clone();
				ipColor = imp.getProcessor();
				col = ipColor.getWidth(); 
				lin = ipColor.getHeight(); 
				IJ.run("8-bit");
				impGray[k] = new Duplicator().run(imp);
				ipGray = imp.getProcessor();

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
								&&( (RGB[B]-RGB[R])>10) ) // se o canal azul ? o mais alto e a dist?ncia entre o canal azul e os demais ? de pelo menos 10 intensidades
								&&( ((hue >= 181)&&(hue <= 300)) //se o tom ? de azul (do ciano ao azul marinho)
								&&((sat>=20)&&(bri>=20) ) ) ){ // se (satura??o >= 20% e brilho >= 20%)
							ipGray.putPixel(j, i, 255); 
						}else {
							ipGray.putPixel(j, i, 0); 
						}
					}
				} 

				imp.updateAndDraw(); 
				ipColor.setAutoThreshold("Otsu"); 
				IJ.run("Convert to Mask"); 
				IJ.run("Make Binary"); 
				IJ.run("Fill Holes"); 
				IJ.run("Set Measurements...", "area"); 
		 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
		 		TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
			    ResultsTable rt = tw.getTextPanel().getResultsTable(); 
			    float areasMedia[] = rt.getColumn(0);
			    maior = 0;
			    for (i=0; i<areasMedia.length; i++)
			    	if (areasMedia[i]>maior)
			    		maior = areasMedia[i];
				tw.close(false); //sair sem salvar
				IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");

				impSegm[k] = new Duplicator().run(imp);

				// fechando as janelas abertas
				IJ.getImage().changes = false;
				IJ.getImage().close();
				IJ.getImage().changes = false;
				IJ.getImage().close();
				
			
			}				
			
			// Arrays - Output
			for (k=0; k<qtd; k++) {
//				IJ.saveAs(impColor[k], "Jpeg", "\\Results\\"+fileNames[k]+"_C");
//				IJ.saveAs(impGray[k], "Jpeg", "\\Results\\"+fileNames[k]+"_G");
				IJ.saveAs(impSegm[k], "Jpeg", "\\Results\\"+fileNames[k]+"_S");
			}		
			//TextWindow tw = (TextWindow)WindowManager.getFrame("Summary"); 
			//tw.close(false); //sair sem salvar
			IJ.showMessage("End of the plugin");

		} catch (IOException e) {
			e.printStackTrace();
		}		
   }
}
