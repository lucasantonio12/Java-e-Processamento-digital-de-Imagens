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
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

/**
 * Teste de segmentação em lote - v10102019
 * Alessandra Mendes
**/
public class Teste_Lote implements PlugIn {


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
			ImageProcessor ipColor, ipGray;
			ColorProcessor cpRGB;
			
			int lin, col, i, j;
			float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
			int[] RGB = new int[3];
			int[] GRAY = new int[3];
			float hsb[] = new float[3];
			int R = 0, G = 1, B = 2; // indices dos componentes RGB


			//construir o vetor de imagens de entrada
			//for (k=0; k<qtd; k++) {
			//	IJ.open(result.get(k));
			//	temp[k] = IJ.getImage();
			//	impColor[k] = (ImagePlus) temp[k].clone();
			//	
			//	temp[k].changes = false;
			//	temp[k].close(); 
			//}
			
			for (k=0; k<qtd; k++) {

				//nome do arquivo
				fileName = (result.get(k).substring(result.get(k).lastIndexOf("\\") + 1));
	            fileName = (fileName.substring(0, fileName.indexOf(".")));
	            //IJ.log("Nome do arquivo("+k+") = "+fileName);
	            fileNames[k] = fileName;
	            
	            // BACK_BLUE
				
				//IJ.open(""); //abrir a janela para escolher a imagem

				IJ.open(result.get(k));
				impColor[k] = (ImagePlus) IJ.getImage(); //Imagem colorida original

				ipColor = impColor[k].getProcessor();
				col = ipColor.getWidth(); //largura
				lin = ipColor.getHeight(); //altura
				IJ.run("8-bit");

				impGray[k] = (ImagePlus) IJ.getImage(); //Imagem em tons de cinza
				ipGray = impGray[k].getProcessor();

				//bordas vermelhas na imagem original
				ipGray.findEdges();
				for (j=0; j<col; j++) { //colunas
					for (i=0; i<lin; i++) { //linhas
						ipGray.getPixel(j, i, GRAY);
						ipColor.getPixel(j, i, RGB);
						if(GRAY[0]>100){ //bordas ressaltadas na imagem
							RGB[R] = 255;
							RGB[G] = 0;
							RGB[B] = 0;
							ipColor.putPixel(j, i, RGB); //escreve a intensidade 255 na imagem RGB - background
							//IJ.log("valores: R-"+RGB[R]+"G-"+RGB[G]+"B-"+RGB[B]);
							//ipGray.putPixel(j, i, 126); //escreve a intensidade 255 na imagem RGB - background
						}
					}
				}
				//imp.show();
				//IJ.saveAs("Jpeg", "\\Results\\edgeRedLote_"+k);
				cpRGB = (ColorProcessor) ipColor; //imagem RGB original
				
				for (j=0; j<col; j++) { //colunas
					for (i=0; i<lin; i++) { //linhas
						cpRGB.getPixel(j, i, RGB); //seleciona o pixel da imagem RGB

						//Código inserido modificando a condicional para acrescentar o modelo HSB
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
						//valor fixo "10" - automatizar se for possível
						if( ( ((RGB[B]>RGB[G])&&(RGB[B]>RGB[R]))
								&&( (RGB[B]-RGB[R])>10) ) // se o canal azul é o mais alto e a distância entre o canal azul e os demais é de pelo menos 10 intensidades
								&&( ((hue >= 181)&&(hue <= 300)) //se o tom é de azul (do ciano ao azul marinho)
								//&&(((sat>=25)&&(bri>=80)) // se (saturação >= 25% e brilho >= 80%) OU (saturação >= 35% e brilho >=20 e brilho < 80) 
								//||((sat>=20)&&(bri>=20)&&(bri<=80)) ) ) ){ 
								&&((sat>=20)&&(bri>=20) ) ) ){ 
						//condicional primeira versão - somente modelo RGB
						//if(((RGB[B]>RGB[G])&&(RGB[G]>RGB[R]))&&((RGB[B]-RGB[G])>10)) { // se o canal azul é o mais alto e a distância entre os canais azul e verde é de pelo menos 20 intensidades
							ipGray.putPixel(j, i, 255); //escreve a intensidade 255 na imagem RGB - background
						}else {
							ipGray.putPixel(j, i, 0); //escreve a intensidade 0 na imagem RGB - foreground
						}
					}
				} 

				impColor[k].updateAndDraw(); //redesenhar a imagem modificada; impRGB.updateAndRepaintWindow();
				//ipColor[k].setAutoThreshold("Otsu"); //segmentação Otsu
				ipColor.setAutoThreshold("Otsu"); //segmentação Otsu
				IJ.run("Convert to Mask"); //conversão para máscara
				IJ.run("Make Binary"); //transformar para binária
				IJ.run("Fill Holes"); //preenchimento de regiões
				IJ.run("Watershed"); //Transformada watersheed - desconecção de sementes conectadas (distância de 1 pixel)
				IJ.run("Set Measurements...", "area"); 
				//Ruídos: retirar objetos que são menores que a metade da área do maior objeto (04/09/19) 
		 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
				TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
			    ResultsTable rt = tw.getTextPanel().getResultsTable(); 
			    float areas[] = rt.getColumn(0);
				maior = 0;
			    //IJ.log("maior = "+maior);
			    for (i=0; i<areas.length; i++)
			    	if (areas[i]>maior)
			    		maior = areas[i];
			    IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");
			    tw.close(false); //fecha a janela de resultados sem salvar

			    impSegm[k] = IJ.getImage();
				IJ.saveAs("Jpeg", "\\Results\\"+fileName);

				//para contagem de elementos
				tw = (TextWindow)WindowManager.getFrame("Summary"); 
			    //rt = tw.getTextPanel().getResultsTable(); 
				//IJ.log("rt: "+rt.getStringValue(1, 0));

				impGray[k].changes = false;
				impGray[k].close(); 
				impSegm[k].changes = false;
				impSegm[k].close();
				
				//IJ.log("Color: "+impSegm[k]);
				//IJ.log("Gray: "+impGray[k]);
				//IJ.log("Segment: "+impSegm[k]);
				//IJ.log("Name: "+fileNames[k]);
				
				
			}				
			
		} catch (IOException e) {
			e.printStackTrace();
		}		


		/*
        GenericDialog gd = new GenericDialog("Lote de Imagens");
        gd.addStringField("Quantas imagens deseja processar?", "");
        gd.showDialog();
        if (gd.wasCanceled()){
            IJ.error("PlugIn canceled!");
            return;
        }
        final int qtd = Integer.parseInt(gd.getNextString()); // indices dos componentes RGB
        //String op = gd.getNextString(); 
        //IJ.showMessage("qtd = "+op);
		*/
		
		IJ.showMessage("End of the plugin");

   }
}
