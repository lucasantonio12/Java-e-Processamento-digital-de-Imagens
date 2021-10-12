package my_plugins;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

/**
 * Teste de segmentação em lote (blue background) - v18102019
 * Alessandra Mendes
 * Retirada de sementes ligadas utilizando Watersheed e mediana
 * Deve existir uma pasta c:\Results para armazenamento dos aquivos de saída
**/
public class backBlue_Lote_v2 implements PlugIn {


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
			
			ImagePlus impColor[] = new ImagePlus[qtd];
			ImagePlus impGray[] = new ImagePlus[qtd];
			ImagePlus impSegm[] = new ImagePlus[qtd];
			String fileNames[] = new String[qtd];
			ImagePlus impOriginal, impClone, imGrande;
			ImageProcessor ipColor, ipGray;
//			ColorProcessor cpRGB;
			
			int lin, col, i, j, pos;
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
				impOriginal = (ImagePlus) IJ.getImage(); 
				impColor[k] = (ImagePlus) impOriginal.clone();
				ipColor = impOriginal.getProcessor();
				col = ipColor.getWidth(); 
				lin = ipColor.getHeight(); 
				IJ.run("8-bit");
				impClone = (ImagePlus) IJ.getImage();
				impGray[k] = new Duplicator().run(impClone);
				//impClone.show();
				ipGray = impClone.getProcessor();

				//IJ.log("gray primeira = "+impGray[k]);
				//IJ.saveAs(impGray[k], "Jpeg", "\\Results\\"+fileName+"_G1");
				//impGray[k].changes = false;
				//impGray[k].close(); 
				//impColor[k].changes = false;
				//impColor[k].close();
				
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
				//cpRGB = (ColorProcessor) ipColor; 
				ColorProcessor cpRGB = (ColorProcessor) ipColor;
				
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
								&&((sat>=20)&&(bri>=20) ) ) ){ // se (saturação >= 20% e brilho >= 20%)
							ipGray.putPixel(j, i, 255); 
						}else {
							ipGray.putPixel(j, i, 0); 
						}
					}
				} 

				impOriginal.updateAndDraw(); 
				ipColor.setAutoThreshold("Otsu"); 
				IJ.run("Convert to Mask"); 
				//impOriginal.updateAndDraw(); 

				IJ.run("Make Binary"); 
				IJ.run("Fill Holes"); 

				//condicional do Watershed
				// Insere as áreas dos objetos em um vetor
				ArrayList<Float> areas = new ArrayList<Float>();
				float mediana = 0, dif = 0;
				IJ.run("Set Measurements...", "area"); 
		 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
				TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
			    ResultsTable rt = tw.getTextPanel().getResultsTable(); 
			    float areasMediana[] = rt.getColumn(0);
			    for(i=0; i<areasMediana.length; i++)
			    	areas.add(areasMediana[i]); 
			    
			    Collections.sort(areas); //vetor de áreas ordenado no ArrayList

			    //mediana - elemento da posição 2/3 do vetor de áreas ordenado de forma crescente
				mediana = areas.get((areas.size()/3)*2).floatValue();
				lin = areas.indexOf(mediana);
				
				//busca por uma diferença grande entre as áreas dos objetos do vetor a partir da mediana calculada
			    maior = 0;
			    pos = areas.size()-1; //último elemento do vetor para não mostrar as sementes se não existirem grandes diferenças
				for(i=(lin+1); i<areas.size(); i++) {
			    	dif = (areas.get(i).floatValue() - areas.get(i-1).floatValue());
			    	if(dif > maior) {
			    		maior = dif;
			    		pos = i;
			    	}
			    }

				impOriginal = IJ.getImage();
				impClone = new Duplicator().run(impOriginal);
				//impClone.show();

				//guarda os objetos grandes (diferença maior que o tamanho da mediana (objeto) + 3/4)
				//condicional para saber se a área que teve a maior diferença calculada é maior que o tamanho da semente + 50%
				IJ.run("Analyze Particles...", "size="+(areas.get(pos).floatValue())+"-Infinity show=Masks");
				IJ.run("Watershed"); //Transformada watersheed - desconecção de sementes conectadas (distância de 1 pixel)
				impOriginal = IJ.getImage();
				imGrande = new Duplicator().run(impOriginal);
				impOriginal.changes = false;
				impOriginal.close();
				//imGrande.changes = false;
				//imGrande.close();
				
				//guarda os objetos menores que a maior diferença em uma imagem e os maiores em outra
				IJ.run("Analyze Particles...", "size=0-"+(areas.get(pos-1).floatValue())+" show=Masks");
				//retira o ruído
				IJ.run("Set Measurements...", "area"); 
		 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
				tw = (TextWindow)WindowManager.getFrame("Results"); 
			    rt = tw.getTextPanel().getResultsTable(); 
			    float areasMedia[] = rt.getColumn(0);
			    maior = 0;
			    for (i=0; i<areasMedia.length; i++)
			    	if (areasMedia[i]>maior)
			    		maior = areasMedia[i];
				IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks");

				// fechando as janelas abertas
				impOriginal = IJ.getImage();
				impClone = new Duplicator().run(impOriginal);
				impOriginal.changes = false;
				impOriginal.close();
				impOriginal = IJ.getImage();
				impOriginal.changes = false;
				impOriginal.close();
				impOriginal = IJ.getImage();
				impOriginal.changes = false;
				impOriginal.close();
				
				ImageCalculator ic = new ImageCalculator();

				impOriginal = ic.run("XOR create", imGrande, impClone);
				impOriginal.show();
				impSegm[k] = new Duplicator().run(impOriginal);

				IJ.run("Analyze Particles...", "size=0-Infinity show=Masks summarize");
				// elements counting
				tw = (TextWindow)WindowManager.getFrame("Summary"); 
			    rt = tw.getTextPanel().getResultsTable(); 
				//IJ.log("rt: "+rt.getStringValue(1, 0));
				tw = (TextWindow)WindowManager.getFrame("Results"); 
				tw.close(false); //sair sem salvar
				
				//IJ.saveAs(impColor[k], "Jpeg", "\\Results\\"+fileName+"_C");
				//IJ.saveAs(impGray[k], "Jpeg", "\\Results\\"+fileName+"_G");
				//IJ.saveAs(impSegm[k], "Jpeg", "\\Results\\"+fileName+"_S");
				IJ.saveAs(impSegm[k], "Jpeg", "\\Results\\"+fileName+"r");

				impOriginal = IJ.getImage();
				impOriginal.changes = false;
				impOriginal.close();
				impOriginal = IJ.getImage();
				impOriginal.changes = false;
				impOriginal.close();
				
				//liberando memória
				impOriginal = null;
				impClone = null;
				imGrande = null;
				ipColor = null;
				ipGray = null;
				ic = null;
				cpRGB = null;
				areas = null;
				areasMediana = null;
				areasMedia = null;
				tw = null;
				System.gc();
				
			}				
			
		} catch (IOException e) {
			e.printStackTrace();
		}		

		// colorRGBImageOutputArray impSegm[]
		// grayImageOutputArray impGray[]
		// segmentImageOutputArray impSegm[]
		// nameFilesOutputArray fileNames[]
		
		IJ.showMessage("End of the plugin");

   }
}
