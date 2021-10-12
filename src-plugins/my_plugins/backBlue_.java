package my_plugins;

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
import ij.text.TextWindow; 
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog; 

/**
 * Blue background segmentation - v04092019
 * Alessandra Mendes
**/
public class backBlue_ implements PlugIn {
	
	
	public void run(String args) {
		
		ImagePlus imp, impGray;
		ImagePlus finalImage, imClone, imGrande;
    	ImageProcessor ip, ipGray;

    	int lin, col, i, j, pos;
		float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
		int[] RGB = new int[3];
		int[] GRAY = new int[3];
		float hsb[] = new float[3];
		int R = 0, G = 1, B = 2; // indices dos componentes RGB
		
		IJ.open(""); //abrir a janela para escolher a imagem
		imp = (ImagePlus) IJ.getImage(); 
		ip = imp.getProcessor();
		col = ip.getWidth(); //largura
		lin = ip.getHeight(); //altura
		IJ.run("8-bit");
		impGray = (ImagePlus) IJ.getImage(); //Imagem em tons de cinza
		ipGray = impGray.getProcessor();
		
		//bordas vermelhas na imagem original
		ipGray.findEdges();
		for (j=0; j<col; j++) { //colunas
			for (i=0; i<lin; i++) { //linhas
				ipGray.getPixel(j, i, GRAY);
				ip.getPixel(j, i, RGB);
				if(GRAY[0]>100){ 
					RGB[R] = 255;
					RGB[G] = 0;
					RGB[B] = 0;
					ip.putPixel(j, i, RGB); //escreve a intensidade 255 na imagem RGB - background
					//IJ.log("valores: R-"+RGB[R]+"G-"+RGB[G]+"B-"+RGB[B]);
					//ipGray.putPixel(j, i, 126); //escreve a intensidade 255 na imagem RGB - background
				}
			}
		}
		//imp.getBufferedImage();
		//imp.updateAndRepaintWindow();
		
		//IJ.saveAs("Jpeg", "\\Results\\edgeRed");
		ColorProcessor cpRGB = (ColorProcessor) ip; //imagem RGB original
		
		for (j=0; j<col; j++) { //colunas
			for (i=0; i<lin; i++) { //linhas
				cpRGB.getPixel(j, i, RGB); //seleciona o pixel da imagem RGB
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
				// saturação e brilho
				//Color.RGBtoHSB(RGB[R], RGB[G], RGB[B], hsb);
				//IJ.log("hue = "+hue);
				//IJ.log("saturat = "+hsb[1]);
				//IJ.log("brilho = "+hsb[2]);
				//valor fixo "10" - automatizar se for possível
				//R 164
				//G 224
				//B 252  azul claro
				if( ( ((RGB[B]>RGB[G])&&(RGB[B]>RGB[R]))
						&&( (RGB[B]-RGB[R])>10) ) // se o canal azul é o mais alto e a distância entre o canal azul e vermelho é de pelo menos 10 intensidades
						&&( ((hue >= 181)&&(hue <= 300)) //se o tom é de azul (do ciano ao azul marinho)
						//&&(((sat>=25)&&(bri>=80)) // se (saturação >= 25% e brilho >= 80%) OU (saturação >= 35% e brilho >=20 e brilho < 80) 
						//||((sat>=20)&&(bri>=20)&&(bri<=80)) ) ) ){ 
						&&((sat>=20)&&(bri>=20)) ) ){ 
					ipGray.putPixel(j, i, 255); //escreve a intensidade 255 na imagem RGB - background
				}else {
					ipGray.putPixel(j, i, 0); //escreve a intensidade 0 na imagem RGB - foreground
				}
			}
		} 
		
		
		imp.updateAndDraw(); //redesenhar a imagem modificada; impRGB.updateAndRepaintWindow();
		ip.setAutoThreshold("Otsu"); //segmentação Otsu
		IJ.run("Convert to Mask"); //conversão para máscara
		
		imp.updateAndDraw(); //redesenhar a imagem modificada; impRGB.updateAndRepaintWindow();

		IJ.run("Make Binary"); //transformar para binária
		IJ.run("Fill Holes"); //preenchimento de regiões
		
		IJ.showMessage("pause");

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
		//IJ.log("mediana "+mediana+" na posição "+lin);
		
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

		finalImage = IJ.getImage();
		imClone = (ImagePlus) finalImage.clone(); // imagem original

		//guarda os objetos grandes (diferença maior que o tamanho da mediana (objeto) + 3/4)
		IJ.run("Analyze Particles...", "size="+(areas.get(pos).floatValue())+"-Infinity show=Masks summarize");
		IJ.run("Watershed"); //Transformada watersheed - desconecção de sementes conectadas (distância de 1 pixel)
		finalImage = IJ.getImage();
		imGrande = (ImagePlus) finalImage.clone(); // imagem com objetos grandes separados
		finalImage.changes = false;
		finalImage.close();
		
		//imGrande.show();
		
		//guarda os objetos menores que a maior diferença em uma imagem e os maiores em outra
		IJ.run("Analyze Particles...", "size=0-"+(areas.get(pos-1).floatValue())+" show=Masks summarize");
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
		IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");
		finalImage = IJ.getImage();
		imClone = (ImagePlus) finalImage.clone();
		finalImage.changes = false;
		finalImage.close();
		//IJ.log("grande: "+imGrande);
		//IJ.log("final: "+finalImage);
		//IJ.log("clone: "+imClone);


		//imGrande.show();
		//imClone.show();
		
		ImageCalculator ic = new ImageCalculator();
		finalImage = ic.run("XOR create", imGrande, imClone);
		//IJ.saveAs(imGrande, "Jpeg", "\\Results\\_g");
		//IJ.saveAs(imClone, "Jpeg", "\\Results\\_c");
		//IJ.saveAs(finalImage, "Jpeg", "\\Results\\_f");
		
		imGrande.changes = false;
		imGrande.close();
		imClone.changes = false;
		imClone.close();
		
	    finalImage.show();
	    
	    
		// Objetos grandes 
		//if(maior > (mediana/2)) { // se a maior diferença de tamanhos encontrada for maior que 2/3 da mediana, são objetos conectados
		//}

		//finalImage = IJ.getImage();
		finalImage.show();
		//IJ.showMessage("pausa");

		//finalImage = IJ.getImage();

		//para contagem de elementos
		tw = (TextWindow)WindowManager.getFrame("Summary"); 
	    rt = tw.getTextPanel().getResultsTable(); 
		
		//IJ.log("rt: "+rt.getStringValue(1, 0));

	    
	    
	    
	    
	    
	    ///////////////////////////
		imp.changes = false;
		imp.close(); 
		impGray.changes = false;
		impGray.close(); 

		IJ.selectWindow("Results"); 
		IJ.run("Close");

        //return finalImage;  // this is the final mask image

		IJ.showMessage("End of the plugin");

   }
}
