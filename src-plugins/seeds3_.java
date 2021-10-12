
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextPanel;
import ij.text.TextWindow; 
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo; 

public class seeds3_ implements PlugIn {
	
	
	public void run(String args) {
		int R = 0, G = 1, B = 2;
		
		
		IJ.open(); //IMAGEM ORIGINAL

		IJ.run("Size...", "width=1080 height=1502 depth=1 constrain average interpolation=Bilinear");
		ImagePlus im = (ImagePlus) IJ.getImage();
		
		String name = im.getOriginalFileInfo().fileName;
		String path = im.getOriginalFileInfo().directory;
		
		
		ImageProcessor ip = im.getProcessor();
    	
    	
		ImagePlus imCopia = new ImagePlus("copia", ip.crop());
    	ImageProcessor ipCopia = imCopia.getProcessor();
    	
    	
    	
    	int width = im.getWidth();
    	int height = im.getHeight();
    	
    	IJ.run("8-bit"); //IMAGEM EM TONS DE CINZA

    	ImagePlus imGray = (ImagePlus) IJ.getImage();
    	ImageProcessor ipGray = imGray.getProcessor();
  
		
    	ipGray.findEdges();
    	
    	int [] RGB = new int[3];
    	int [] GRAY = new int[3];
    	
    	
		for (int j = 0; j < width; j++) { //colunas
			for (int i = 0; i < height; i++) { //linhas
				ipGray.getPixel(j, i, GRAY);
				ip.getPixel(j, i, RGB);
				if(GRAY[0]>100){ //
					RGB[R] = 255;
					RGB[G] = 0;
					RGB[B] = 0;
					ip.putPixel(j, i, RGB);
				}
			}
		}
		
		
    	
	
		ColorProcessor cpRGB = (ColorProcessor) ip; //imagem RGB original
		
		float[] HSB = new float[3];
		float sat, bri, min, max, hue=0;
		
		for (int j = 0; j < width; j++) { //colunas
			for (int i = 0; i < height; i++) { //linhas
				cpRGB.getPixel(j, i, RGB); //seleciona o pixel da imagem RGB
				//Color HUE (model HLS);
				Color.RGBtoHSB(RGB[R], RGB[G], RGB[B], HSB);
				sat = (int) Math.ceil(HSB[G]*100);
				bri = (int) Math.ceil(HSB[B]*100);
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
					
				if( ( ((RGB[B]>RGB[G])&&(RGB[B]>RGB[R]))
				  &&( (RGB[B]-RGB[R])>10) ) // se o canal azul é o mais alto e a distância entre o canal azul e vermelho é de pelo menos 10 intensidades
				  &&( ((hue >= 200)&&(hue <= 400)) //se o tom é de azul (do ciano ao azul marinho)
				//&&(((sat>=25)&&(bri>=80)) // se (saturação >= 25% e brilho >= 80%) OU (saturação >= 35% e brilho >=20 e brilho < 80) 
				//||((sat>=20)&&(bri>=20)&&(bri<=80)) ) ) ){ 
				  &&((sat>=20)&&(bri>=20)) ) ){
					ipGray.putPixel(j, i, 255); //escreve a intensidade 255 na imagem RGB - background
				}else {
					ipGray.putPixel(j, i, 0); //escreve a intensidade 0 na imagem RGB - foreground
				}
			}
		}
	   
		
		
		ImagePlus imEmpty = (ImagePlus)im.clone();
		ImageProcessor ipEmpty = imEmpty.getProcessor();
		
		for (int j = 0; j < width; j++) { //colunas
			for (int i = 0; i < height; i++) { //linhas
				if(ipGray.getPixel(j, i) == 0) {
					if(ipGray.getPixel(j, i+1) == 0 && ipGray.getPixel(j, i-1) == 0) {// lado da semente
						ipEmpty.putPixel(i, j, 255);
					}
				}
			}
		}
	
		imEmpty.show();
		
		IJ.run("Invert"); //transformar para binária
		IJ.run("Fill Holes"); //preenchimento de regiões
		imEmpty = IJ.getImage();
	
		
		ArrayList<Float> areas = new ArrayList<Float>();
		float mediana = 0, dif = 0;
		
		IJ.run("Set Measurements...", "area"); 
 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");
 		
		TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
	    ResultsTable rt = tw.getTextPanel().getResultsTable(); 
	    tw.close(false);
	    float areasMediana[] = rt.getColumn(0);
	    int soma = 0;
	    for(int i = 0; i < areasMediana.length; i++) {
	    	areas.add(areasMediana[i]); 
	    	soma+=areasMediana[i];
	    }
	    
	    Collections.sort(areas); //vetor de áreas ordenado no ArrayList
	    
		//mediana - elemento da posição 2/3 do vetor de áreas ordenado de forma crescente
		//mediana = areas.get((areas.size()/3)*2).floatValue();
		//int lim = areas.indexOf(mediana);
	    int lim = soma/areasMediana.length;
		//IJ.log("mediana "+mediana+" na posição "+lin);
		
		//busca por uma diferença grande entre as áreas dos objetos do vetor a partir da mediana calculada
		float maior = 0;
		int pos = areas.size()-1; //último elemento do vetor para não mostrar as sementes se não existirem grandes diferenças
		for(int i = (lim+1); i < areas.size(); i++) {
			dif = (areas.get(i).floatValue() - areas.get(i-1).floatValue());
			if(dif > maior) {
				maior = dif;
				pos = i;
			}
		}
		
		
		
		
		IJ.run("Analyze Particles...", "size="+lim+"-Infinity show=Outlines clear");	    
    	IJ.save(IJ.getImage(), path + name.split("\\.")[0] + "_Contagem.png");
	    IJ.getImage().close();
	    
	    ImagePlus Mascara = IJ.getImage();
	    ImageProcessor ipMascara = Mascara.getProcessor();
	    ImagePlus imCopia2 = imCopia.duplicate();
	    ImageProcessor ipCopia2 = imCopia2.getProcessor();
	    
	
	    
	    
	    for (int j = 0; j < width; j++) { //colunas
			for (int i = 0; i < height; i++) { //linhas
				ipCopia2.getPixel(j, i, RGB);
				ipMascara.getPixel(j, i, GRAY);
				if(GRAY[0]==0) {
					RGB[R] = 0;
					RGB[G] = 0;
					RGB[B] = 0;
					ipCopia2.putPixel(j, i, RGB); //escreve a intensidade 255 na imagem RGB - background
				}
			}
		}
	    
	    imCopia2.show();
	    
	    IJ.save(IJ.getImage(), path + name.split("\\.")[0] + "_segmentada.png");
	    
	    IJ.run("close");
	    IJ.run("close");
	    IJ.run("close");
	    IJ.run("close");
	    
	    /*
	    
	    IJ.run("8-bit");
	    
	    
	    IJ.run("Threshold...");
	    IJ.setThreshold(10, 255);
	    IJ.run("Close");
	    
    	//IJ.log("valores: "+(areas.get(pos).floatValue()));
		IJ.run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding fit shape feret's integrated median skewness kurtosis area_fraction"); 
		IJ.run("Analyze Particles...", "size="+lim+"-Infinity show=Masks display clear");
 		tw = (TextWindow)WindowManager.getFrame("Results"); 
	    rt = tw.getTextPanel().getResultsTable(); 
	    //tw.setVisible(false);
	    
	   
	    tw.close(false);
	    
	    
	    ImagePlus imMask = Mascara;
	    ImageProcessor ipMask = imMask.getProcessor();
	   
	    
	    for (int j = 0; j < width; j++) { //colunas
			for (int i = 0; i < height; i++) { //linhas
				ipCopia.getPixel(j, i, RGB);
				ipMask.getPixel(j, i, GRAY);
				if(GRAY[0]==0) {
					RGB[R] = 0;
					RGB[G] = 0;
					RGB[B] = 0;
					ipCopia.putPixel(j, i, RGB); //escreve a intensidade 255 na imagem RGB - background
				}
			}
		}
	
	    	    
	    ArrayList<ImagePlus> sementes = new ArrayList<ImagePlus>();
	    ImagePlus imAux, imCrop;
	    ImageProcessor ipAux;
	    
	    
	    float bbX[] = rt.getColumn(11);
    	float bbY[] = rt.getColumn(12);
    	float bbW[] = rt.getColumn(13);
    	float bbH[] = rt.getColumn(14);
    	for(int i = 0; i < rt.size(); i++) {
		    imAux = imCopia.duplicate();
	    	ipAux = imAux.getProcessor();
		    ipAux.setRoi((int)bbX[i], (int)bbY[i], (int)bbW[i], (int)bbH[i]);
		    imCrop = new ImagePlus("Cortada", ipAux.crop());
	
		    sementes.add(imCrop);
	    }
	    
	   
	    int sem_RGB_R[] = new int[sementes.size()];
	    int sem_RGB_G[] = new int[sementes.size()];
	    int sem_RGB_B[] = new int[sementes.size()];
	    int sem_HSB_H[] = new int[sementes.size()];
	    int sem_HSB_S[] = new int[sementes.size()];
	    int sem_HSB_B[] = new int[sementes.size()];
	    
	    for(int i = 0; i < sementes.size(); i++) {
	    	sementes.get(i).show();
	    	
	    	
	    	
	    	
	    	IJ.run("RGB Stack");
	    	IJ.selectWindow("Cortada");
	    	sem_RGB_R[i] = getModa(IJ.getImage());
	    	IJ.run("Next Slice [>]");
	    	sem_RGB_G[i] = getModa(IJ.getImage());
	    	IJ.run("Next Slice [>]");
	    	sem_RGB_B[i] = getModa(IJ.getImage());
	    	IJ.run("Stack to RGB");
	    	IJ.run("HSB Stack");
	    	sem_HSB_H[i] = getModa(IJ.getImage());
	    	IJ.run("Next Slice [>]");
	    	sem_HSB_S[i] = getModa(IJ.getImage());
	    	IJ.run("Next Slice [>]");
	    	sem_HSB_B[i] = getModa(IJ.getImage());
	    	IJ.run("Close All");
	    	sementes.get(i).close();
	    }
	    
	    
	    for(int i = 0; i < sementes.size(); i++) {
	    	rt.addValue("Rgb", sem_RGB_R[i]);
	    	rt.addValue("rGb", sem_RGB_G[i]);
	    	rt.addValue("rgB", sem_RGB_B[i]);
	    	rt.addValue("Hsv", sem_HSB_H[i]);
	    	rt.addValue("hSv", sem_HSB_S[i]);
	    	rt.addValue("hsV", sem_HSB_B[i]);
	    	
		    
	    	//IJ.log("Area: "+ sem_areas[i] + " R: "+ sem_RGB_R[i] + " G: "+ sem_RGB_G[i] + " B: "+ sem_RGB_B[i] + " H: "+ sem_HSB_H[i] + " S: "+ sem_HSB_S[i] + " B: "+ sem_HSB_B[i]);
	    }  
	    for(int i = 0; i < sementes.size(); i++) {
	    	rt.setValue("Rgb", i, sem_RGB_R[i]);
	    	rt.setValue("rGb", i, sem_RGB_G[i]);
	    	rt.setValue("rgB", i, sem_RGB_B[i]);
	    	rt.setValue("Hsv", i, sem_HSB_H[i]);
	    	rt.setValue("hSv", i, sem_HSB_S[i]);
	    	rt.setValue("hsV", i, sem_HSB_B[i]);
	    	
	    	//IJ.log("Area: "+ sem_areas[i] + " R: "+ sem_RGB_R[i] + " G: "+ sem_RGB_G[i] + " B: "+ sem_RGB_B[i] + " H: "+ sem_HSB_H[i] + " S: "+ sem_HSB_S[i] + " B: "+ sem_HSB_B[i]);
	    }  
	    rt.show("Resultados");
	    
	    IJ.saveAs("Results", path + name.split("\\.")[0] + "_Resultados.csv");
	    
	    IJ.showMessage("PAUSE");
   } 
	
	public int getModa(ImagePlus im) {
		int [] RGB = new int[3];
		int [] intensidades = new int[256];
		int maior = 0, indice = 0, valor;
		
		ImageProcessor imp = im.getProcessor();
		
		for (int j = 0; j < im.getWidth(); j++) { //colunas
			for (int i = 0; i < im.getHeight(); i++) { //linhas
				imp.getPixel(j, i, RGB);
				valor = RGB[0]+RGB[1]+RGB[2];
				intensidades[valor]++;
			}
		}
		
		for (int j = 1; j < 256; j++) {
			if(intensidades[j]>maior) {
				maior = intensidades[j];
				indice = j;
			}
		}
		
		return indice;
		
	*/
	}
}
