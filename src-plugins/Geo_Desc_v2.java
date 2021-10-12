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
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

public class Geo_Desc_v2 implements PlugIn{
	
	public void run(String args) {
		int w=0;
		do {
			
		IJ.run("Close All");
		ImagePlus imMask, imMaskSmall;
		float[] areas;
		float[] percent;
		float[] simObj;
		int[] results; 
		String fileNames[] = null;
		
		int n, j, cont, k, qtd, i, countWindows, c;
		String newTitle, stringa;
		ImageProcessor ip, ipGeo;
		int widthSeed, heightSeed, widthObj, heightObj, widthDif, heightDif, widthNew, heightNew;
		boolean nova, ruido = false;
		ImagePlus imp, impGeoAux;
		ImagePlus impGeo[];
		TextWindow tw, twTmp;
		ResultsTable rt, rtTmp;
		
		float aux = 0, menor;

		// GEO_DESC
		// Abrir objetos geométricos - base para a construção dos descritores de formas
		String directory;
		List<String> result;
		qtd = 0;
		impGeo = null;
		OpenDialog od = new OpenDialog("Selezionare un file (oggetto geometrico) tra quelli da analizzare", null);
		directory = od.getDirectory();
//		directory = "c:\\UniCA\\BDGeo_v3\\";
		if (null == directory) 
			return;
		try {
			Stream<Path> walk = Files.walk(Paths.get(directory));
			result = walk.filter(Files::isRegularFile)
					.map(x -> x.toString()).collect(Collectors.toList());
			walk.close();
			qtd = result.size();
			fileNames = new String[qtd];
			impGeo = new ImagePlus[qtd];
			for (k=0; k<qtd; k++) {
	            // Abrir Objeto
				//nome do arquivo
				fileNames[k] = (result.get(k).substring(result.get(k).lastIndexOf("\\") + 1));
				fileNames[k] = (fileNames[k].substring(0, fileNames[k].indexOf(".")));
//				IJ.open(result.get(k));
//				imp = (ImagePlus) IJ.getImage();
//				impGeo[k] = imp.duplicate();
//				IJ.getImage().changes = false;
//				IJ.getImage().close();
			}
				
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
		if(impGeo!=null) { //se existe objeto geométrico aberto
			qtd = impGeo.length; // quantidade de objetos geométricos no vetor
			simObj = new float[qtd]; //vetor de diferenças entre áreas da semente e de cada um dos objetos
			percent = new float[qtd]; //vetor com os resultados percentuais para cada objeto geométrico que será utilizado
			
			/*
			 * INSERIR AQUI O CÓDIGO DO BACKBLUE
			 * 
			 * 
			 */
			
			//IJ.open(); // abrir a imagem (lote de sementes - segmentada) a ser analisada

    		ImageProcessor ipColor, ipGray;
    		ColorProcessor cpRGB;
    		
    		int lin, col;
    		float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
    		int[] RGB = new int[3];
    		int[] GRAY = new int[3];
    		float hsb[] = new float[3];
    		int R = 0, G = 1, B = 2; // indexes
    		
    		///// INÍCIO DO BACK_BLUE
    		IJ.open();
//    		IJ.open("c:\\UniCA\\Teste\\teste.jpg");
    		imp = (ImagePlus) IJ.getImage(); 
//    		impColor = (ImagePlus) imp.clone(); //image RGB
    		ipColor = imp.getProcessor();
    		col = ipColor.getWidth(); 
    		lin = ipColor.getHeight(); 
    		IJ.run("8-bit");
//    		impGray = new Duplicator().run(imp); //image GRAY
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
    						&&( (RGB[B]-RGB[R])>10) ) // se o canal azul é o mais alto e a distância entre o canal azul e os demais é de pelo menos 10 intensidades
    						&&( ((hue >= 181)&&(hue <= 300)) //se o tom é de azul (do ciano ao azul marinho)
    						&&((sat>=20)&&(bri>=20) ) ) ){ // se (saturação >= 20% e brilho >= 20%)
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
     		TextWindow tw2 = (TextWindow)WindowManager.getFrame("Results"); 
    	    ResultsTable rt2 = tw2.getTextPanel().getResultsTable(); 
    	    float areasMedia[] = rt2.getColumn(0);
    	    maior = 0;
    	    for (i=0; i<areasMedia.length; i++)
    	    	if (areasMedia[i]>maior)
    	    		maior = areasMedia[i];
    		tw2.close(false); 
    		IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");
    		IJ.run("Convert to Mask"); 

    		imp = IJ.getImage();
//    		impSegm = new Duplicator().run(imp); //image mask
    		
    		// closing...
//    		IJ.getImage().changes = false;
//   		IJ.getImage().close();
//    		IJ.getImage().changes = false;
//   		IJ.getImage().close();

    		TextWindow tw3 = (TextWindow)WindowManager.getFrame("Summary"); 
    		tw3.close(false);   
    		
    		///// FIM DO BACK_BLUE
    		
			imMask = (ImagePlus) IJ.getImage(); //retirar
			ip = imMask.getProcessor();
			IJ.setBackgroundColor(255, 255, 255);
		    IJ.run("Set Measurements...", "area centroid bounding fit display redirect=None decimal=3");
		    IJ.run("Analyze Particles...", "size=50-Infinity show=[Overlay Masks] display clear include in_situ");
	 		tw = (TextWindow)WindowManager.getFrame("Results"); 
		    rt = tw.getTextPanel().getResultsTable(); 
		    n = rt.size();
			results = new int[n]; // vetor de resultados, um para cada semente individual
		    
//			float[] centrX = new float[n];
//			float[] centrY = new float[n];
			float[] bX = new float[n];
			float[] bY = new float[n];
			float[] bW = new float[n];
			float[] bH = new float[n];
//			float[] majE = new float[n];
//			float[] minE = new float[n];
			float[] anglE = new float[n];
			
//			centrX = rt.getColumn(6); // centroid X
//			centrY = rt.getColumn(7); // centroid Y
			bX = rt.getColumn(11); // bounding rectangle X
			bY = rt.getColumn(12); // bounding rectangle Y
			bW = rt.getColumn(13); // width of the bounding rectangle
			bH = rt.getColumn(14); // height of the bounding rectangle
//			majE = rt.getColumn(15); // major axis of the fit ellipsis
//			minE = rt.getColumn(16); // minor axis of the fit ellipsis
			anglE = rt.getColumn(17); // angle of the major axis of the fit ellipsis

	    
		    //setBatchMode("hide");
	
			ImagePlus impUnique[] = new ImagePlus[n];
			
			if (n > 1)
			{ 
				IJ.setForegroundColor(193, 209, 221);
				for (j = 0; j<n; j++)  
				{
					
					// Aumento do bounding box
					bX[j] = bX[j] - 2;
					bY[j] = bY[j] - 2;
					bW[j] = bW[j] + 4;
					bH[j] = bH[j] + 4;
	
					IJ.run("Specify...", "width="+bW[j]+" height="+bH[j]+" x="+bX[j]+" y="+bY[j]); //se há semente perto, corta o pedaço... ERRO!
					IJ.run("Copy");
	
					// Criação da imagem nova e rotação...
					newTitle = "maskSmall_";
					if ((j + 1) < 100) newTitle = newTitle + "0";
					if ((j + 1) < 10) newTitle = newTitle + "0";
					newTitle = newTitle + j;
					IJ.newImage(newTitle, "8-bit  black", (int) bW[j], (int) bH[j], 1); //depth = 1 (camadas?)
					IJ.run("Paste");

					IJ.run("Invert LUT");
					stringa = "angle=" + (anglE[j] - 90) + " grid=1 interpolation=Bicubic fill enlarge";
					IJ.run("Rotate... ", stringa);
	
					// Para retirar as "bordas" geradas pela rotação do background...
					imMaskSmall = (ImagePlus) IJ.getImage();
					ip = imMaskSmall.getProcessor();
					ip.setAutoThreshold("Otsu"); //segmentação Otsu
					IJ.run("Convert to Mask"); //conversão para máscara
					IJ.run("Create Selection");
					IJ.run("To Bounding Box");
					IJ.run("Crop");
	
					// Para armazenar a imagem individual no vetor...
					imMaskSmall = (ImagePlus) IJ.getImage();
					impUnique[j] = imMaskSmall.duplicate();

					IJ.getImage().changes = false;
					IJ.getImage().close();

				}
				//tw = (TextWindow)WindowManager.getFrame("Results");
				IJ.run("Clear Results");
				//tw.close(false); //sair sem salvar
				IJ.getImage().changes = false;
				IJ.getImage().close();
				IJ.getImage().changes = false;
				IJ.getImage().close();
				
				// Laço para a remoção de erros nas imagens individuais 
				// caso exista mais de um objeto em uma mesma imagem - erro fruto do bounding box anterior
				for (j=0; j<n; j++)  
				{
					ruido = false; //marcador para saber se existe uma imagem a mais (semente individual com ruído) na hora de fechar
					// Abrir primeira imagem do vetor de sementes individuais
					//impUnique[j].getImage();
					impUnique[j].show();
					ip = impUnique[j].getProcessor();
					IJ.run("Clear Results");
			 		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
			 		twTmp = (TextWindow)WindowManager.getFrame("Results"); 
				    rtTmp = twTmp.getTextPanel().getResultsTable(); 
				    if(rtTmp.size()>1) {
				    	ruido = true;
					    areas = rtTmp.getColumn(0);
					    aux = 0;
					    for (i=0; i<areas.length; i++)
					    	if (areas[i]>aux)
					    		aux = areas[i];
						//twTmp.close(false); //sair sem salvar
						IJ.run("Clear Results");
						IJ.run("Analyze Particles...", "size="+(aux-1)+"-Infinity show=Masks");
//						IJ.run("Convert to Mask"); //conversão para máscara
						IJ.run("Create Selection");
						IJ.run("To Bounding Box");
						IJ.run("Crop");
						imMaskSmall = (ImagePlus) IJ.getImage();
						impUnique[j] = null;
						impUnique[j] = imMaskSmall.duplicate();
						ip = impUnique[j].getProcessor();
//						IJ.getImage().changes = false;
//						IJ.getImage().close();
				    }
					IJ.saveAs(impUnique[j], "jpeg", "\\Results\\semi_"+j); // Original color images
					//tw = (TextWindow)WindowManager.getFrame("Summary"); 
					IJ.run("Clear Results");
//					IJ.getImage().changes = false;
//					IJ.getImage().close();
//					tw = (TextWindow)WindowManager.getFrame("Results"); 
//					tw.close(false); //sair sem salvar

					// Comparação com os objetos geométricos...
					// Abrir primeira imagem do vetor de sementes individuais
//					impUnique[j].getProcessor();
//					impUnique[j].show();
					// Obter eixos: largura (widthSeed) e altura (heightSeed) da semente
					widthSeed = ip.getWidth(); 
					heightSeed = ip.getHeight(); 
				
					for (k=0; k<qtd; k++) {

						IJ.run("Create Selection");
						IJ.run("Copy");
						imMaskSmall = (ImagePlus) IJ.getImage();
						//imMaskSmall.updateAndRepaintWindow();
						//IJ.saveAs(imMaskSmall, "jpeg", "\\Results\\Copy_"+w+"_"+j+"_"+k); // Original color images

						IJ.open(directory+fileNames[k]+".jpg");
						//impGeo[k].show(); // ESTE COMANDO NÃO FUNCIONA!!!  ver o IJ.openImage
						impGeoAux = (ImagePlus) IJ.getImage();
						ipGeo = impGeoAux.getProcessor();

						//impGeoAux = impGeo[k].duplicate();
//						IJ.saveAs(impGeoAux, "jpeg", "\\Results\\Geo_"+w+"_"+j+"_"+k); // Original color images
						// Obter eixos: largura (widthObj) e altura (heightObj) do objeto
						widthObj = ipGeo.getWidth(); 
						heightObj = ipGeo.getHeight(); 
						
						// Diferenças de altura e largura: se dif>0, o objeto é maior; se dif<0, a semente é maior; se dif=0, não há diferença.
						heightDif = heightObj-heightSeed; // diferença de altura
						widthDif = widthObj-widthSeed; // diferença de largura
						
						// Condicionais para aumentar/diminuir o objeto a partir da comparação entre 
						// as diferenças de altura e largura, visando obter ao final a semente contida no objeto 
						// Neste caso o objeto terá o seu menor eixo igualado ao eixo da semente de tal forma que 
						// o maior eixo possa ser igual ou maior que o da semente
						countWindows = 4;
						nova = true;
						widthNew = widthObj;
						heightNew = heightObj;
						if((heightDif==0)&&(widthDif==0)) {
							countWindows = 2;
							nova = false;
							IJ.run("Internal Clipboard");
							//IJ.selectWindow("Clipboard-1");
						} else {
							if(((heightDif==0)&&(widthDif>0))||((heightDif>0)&&(widthDif==0))) {
								countWindows = 3;
							} else {
								if ((heightDif>0)&&(widthDif>0)&&(heightDif>=widthDif)){ //objeto mais alto e mais largo, com diferença de altura maior ou igual que dif de largura
									// Reduz (a largura) o objeto de tal forma que a largura dele coincida com a da semente e a altura coincida ou fique ultrapassando
									widthNew = widthObj-widthDif;
									heightNew = heightObj-widthDif;
									IJ.run("Scale...", "x=- y=- width="+widthNew+" height="+heightNew+" interpolation=Bicubic average create");
								} else {
									if ((heightDif>0)&&(widthDif>0)&&(heightDif<widthDif)){ //objeto mais alto e mais largo, com diferença de altura menor que dif de largura
										// Reduz (a altura) o objeto de tal forma que a altura dele coincida com a da semente e a largura coincida ou fique ultrapassando 
										widthNew = widthObj-heightDif;
										heightNew = heightObj-heightDif;
										IJ.run("Scale...", "x=- y=- width="+widthNew+" height="+heightNew+" interpolation=Bicubic average create");
									} else {
										if((heightDif<0)&&(heightDif<widthDif)) { // objeto mais baixo e com diferença de altura menor que diferença de largura
											// Aumenta (a altura) o objeto de tal forma que a altura dele coincida com a da semente e a largura coincida ou fique ultrapassando
											widthNew = widthObj+(heightDif*(-1));
											heightNew = heightObj+(heightDif*(-1));
											IJ.run("Scale...", "x=- y=- width="+widthNew+" height="+heightNew+" interpolation=Bicubic average create");
										} else {
											if((widthDif<0)&&(widthDif<=heightDif)) { // objeto mais estreito e com diferença de largura menor ou igual a diferença de altura
												// Aumenta (a largura) o objeto de tal forma que a largura dele coincida com a da semente e a altura coincida ou fique ultrapassando
												widthNew = widthObj+(widthDif*(-1));
												heightNew = heightObj+(widthDif*(-1));
												IJ.run("Scale...", "x=- y=- width="+widthNew+" height="+heightNew+" interpolation=Bicubic average create");
											}
										}
									}
								}
							}
						}
						
					    simObj[k] = 0;
					    impGeoAux = (ImagePlus) IJ.getImage();
					    //aqui
						// threshold
						if(nova) { 
							// problema que ocorre quando a imagem da semente não fica totalmente contida na forma
							IJ.newImage("Untitled", "8-bit black", widthNew, heightNew, 1);
//							IJ.showMessage("ok");
							IJ.run("Paste");
							//IJ.run("Invert LUT");
							imMaskSmall = (ImagePlus) IJ.getImage();
							ImageCalculator ic = new ImageCalculator();
							ImagePlus imp3 = ic.run("Transparent-zero", imMaskSmall, imMaskSmall);
							imMaskSmall = (ImagePlus) IJ.getImage();
							//IJ.wait(1000);
							//IJ.saveAs(imp3, "jpeg", "\\Results\\Invert_"+w+"_"+j+"_"+k); // Erro de delay do refresh screen (ImageJ)
							ic = new ImageCalculator();
//							IJ.saveAs(impGeoAux, "jpeg", "\\Results\\XorGEO_"+w+"_"+j+"_"+k); 
							//IJ.saveAs(imMaskSmall, "jpeg", "\\Results\\XorImMask_"+w+"_"+j+"_"+k); 
							imp3 = ic.run("XOR create", impGeoAux, imMaskSmall);
							ip = imp3.getProcessor();
							imp3.show();
							IJ.saveAs(imp3, "jpeg", "\\Results\\Xor_"+w+"_"+j+"_"+k); 
							ip.setAutoThreshold("Default"); //segmentação Otsu
							IJ.run("Convert to Mask");

							// CORREÇÃO DE ERRO NOS CÁLCULOS
							// GRAVAÇÃO EM DISCO E ABERTURA POSTERIOR
/*							imMaskSmall = (ImagePlus) IJ.getImage();
							
							IJ.saveAs(imMaskSmall, "jpeg", "\\Results\\TEMP"); 
							IJ.open("c:\\Results\\TEMP.jpg");
							imMaskSmall = (ImagePlus) IJ.getImage();
							IJ.run("Convert to Mask"); //conversão para máscara
*/							
							IJ.run("Clear Results");
							IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
					 		twTmp = (TextWindow)WindowManager.getFrame("Results"); 
						    rtTmp = twTmp.getTextPanel().getResultsTable();
						    areas = rtTmp.getColumn(0);
						    simObj[k] = 0;
//					    	System.out.println("areas.length = "+areas);
						    for (i=0; i<areas.length; i++) {
						    	simObj[k] += areas[i];
						    }
					    	if((simObj[1]==simObj[2])&&(simObj[2]>0.0))
								System.out.println("Erro - simObj = ["+simObj[0]+", "+simObj[1]+", "+simObj[2]+", "+simObj[3]+"]");

						} else 
							simObj[k] = 0;
						//IJ.showMessage("Similaridade da semente "+j+" com objeto "+k+" = "+simObj[k]+"pixels");
						    
					
						for(c=1; c<=countWindows; c++) {
							imMaskSmall = (ImagePlus) IJ.getImage();
//							impGeoAux = null;
//							impGeoAux = imMaskSmall.duplicate();
//							ipGeo = impGeoAux.getProcessor();
							IJ.getImage().changes = false;
							IJ.getImage().close();
						}

						
					} // fim do laço de objetos geométricos
					IJ.getImage().changes = false;
					IJ.getImage().close();
					
					// verificar qual critério de semelhança é menor						
					menor = simObj[0];
					cont = 0;
				    for (k=0; k<simObj.length; k++)
				    	if(simObj[k]<menor) {
							menor = simObj[k];
							cont = k;
						}

//					IJ.showMessage("menor = "+menor+" e simOBJ = ["+simObj[0]+", "+simObj[1]+", "+simObj[2]+", "+simObj[3]+"]");
//					IJ.showMessage("Objeto com maior similaridade com a semente "+(j+1)+": "+(cont+1));
					System.out.println("Semi "+w+"_"+j+"_"+k+" classe "+cont);
					results[j] = (cont+1);
					if(ruido) {
						IJ.getImage().changes = false;
						IJ.getImage().close();
					}
					
					// Para armazenar a imagem individual no vetor...
//					IJ.saveAs(impUnique[j], "tiff", "\\Results\\smal_"+j+"_C"); // Original color images
				}
			}
			//IJ.run("Clear Results");
			IJ.run("Clear Results");
			tw = (TextWindow)WindowManager.getFrame("Results");
		    rt = tw.getTextPanel().getResultsTable(); 
			tw.close(false); //sair sem salvar
			
			for(k=0; k<percent.length; k++) {
				aux=0;
				for(j=0; j<results.length; j++) {
					if(results[j]==(k+1))
						aux++;
				}
				percent[k] = ((aux*100)/results.length);
//				IJ.showMessage("Percentual de similaridade da amostra ao objeto "+(k+1)+": "+percent[k]);

			}
		
			ResultsTable table = new ResultsTable();
		    table.setPrecision(2);
		    for (i=0; i<percent.length; i++) { 
		    	table.setValue(0, i, fileNames[i]);
		    	table.setValue(1, i, percent[i]);
		    	System.out.println(fileNames[i]+" - "+percent[i]);
		    }
		    System.out.println();
		    table.renameColumn("C1", "Geometry");
		    table.renameColumn("C2", "Similarity Percentage");
		    
		    
		    table.show("Result2");
		    //tw = (TextWindow)WindowManager.getFrame("Results2");
		    //tw.close(false);
//			IJ.showMessage("Fim da análise geométrica!");
		    
		}
	    w++;
	}while(w<1);
		
	}
}
