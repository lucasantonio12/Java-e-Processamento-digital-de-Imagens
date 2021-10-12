package my_plugins;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

/**
 * Blue background (Alessandra Mendes) & BlackWhite background segmentation - v151019
 * 
**/
public class Blue_BlackWhite implements PlugIn {
	
	static final int R = 0, G = 1, B = 2; // indices dos componentes RGB
	
	public void run(String args) {
		
		ImagePlus imp, impGray, finalImage;
		
		// Input (blue or white/black backgrounds)
        GenericDialog gd = new GenericDialog("Inicial Setting");
        String items[] = {"Select BLUE Background image", "Select WHITE and BLACK background images"};
        gd.addRadioButtonGroup("Input Images", items, 2, 1, "Blue Background");
        gd.showDialog();
        if (gd.wasCanceled()){
            IJ.error("PlugIn canceled!");
            return;
        }
        String op = gd.getNextRadioButton(); 
        if (op.contentEquals("Select WHITE and BLACK background images")) {
            ImagePlus imDark, imSubtracted;
    		IJ.open(""); 
    		imp = IJ.getImage(); 
    		IJ.run("Set Scale...", "distance=60.5347 known=6 pixel=1 unit=mm");
    		IJ.run("8-bit"); 
    		impGray = IJ.getImage(); 
    		IJ.open(""); 
    		IJ.run("8-bit");
    		imDark = IJ.getImage(); 
    		imDark.updateAndRepaintWindow();
    		ImageCalculator ic = new ImageCalculator();
    		imSubtracted = ic.run("Subtract create", impGray, imDark);
    		imSubtracted.show();
    		imSubtracted = IJ.getImage();
    		ImageProcessor ip = imSubtracted.getProcessor();
    		ip.setAutoThreshold("Default");
    		IJ.run("Convert to Mask");
    		IJ.run("Make Binary");
    		IJ.run("Fill Holes");
    		IJ.run("Analyze Particles...", "size=3-Infinity show=Masks display clear");  
    		imSubtracted.changes = false;
    		imSubtracted.close();
    		finalImage = IJ.getImage();
    		new WaitForUserDialog("Do you confirm?").show();
        } else if(op.contentEquals("Select BLUE Background image")) {
        	ImageProcessor ip, ipGray;

        	int lin, col, i, j;
    		float maior = 0, min, max, hue = 0, sat = 0, bri = 0;
    		int[] RGB = new int[3];
    		int[] GRAY = new int[3];
    		float hsb[] = new float[3];
    		int R = 0, G = 1, B = 2; //index
    		
    		IJ.open(""); 
    		imp = IJ.getImage(); 
    		ip = imp.getProcessor();
    		col = ip.getWidth(); 
    		lin = ip.getHeight(); 
    		IJ.run("8-bit");
    		impGray = IJ.getImage(); 
    		ipGray = impGray.getProcessor();
    		
    		//flag - red edges 
    		ipGray.findEdges();
    		for (j=0; j<col; j++) { 
    			for (i=0; i<lin; i++) { 
    				ipGray.getPixel(j, i, GRAY);
    				ip.getPixel(j, i, RGB);
    				if(GRAY[0]>100){ 
    					RGB[R] = 255;
    					RGB[G] = 0;
    					RGB[B] = 0;
    					ip.putPixel(j, i, RGB); 
    				}
    			}
    		}
    		ColorProcessor cpRGB = (ColorProcessor) ip; 

    		//blue background extraction
    		for (j=0; j<col; j++) { 
    			for (i=0; i<lin; i++) { 
    				cpRGB.getPixel(j, i, RGB); 
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
    				if( ( ((RGB[B]>RGB[G])&&(RGB[B]>RGB[R]))
    						&&( (RGB[B]-RGB[R])>10) ) // se o canal azul é o mais alto e a distância entre o canal azul e vermelho é de pelo menos 10 intensidades
    						&&( ((hue >= 181)&&(hue <= 300)) //se o tom é de azul (do ciano ao azul marinho)
    						&&((sat>=20)&&(bri>=20)) ) ){ // se (saturação >= 20% e brilho >= 20%)  
    					ipGray.putPixel(j, i, 255); // background
    				}else {
    					ipGray.putPixel(j, i, 0); // foreground
    				}
    			}
    		} 
    		
    		imp.updateAndDraw(); 
    		ip.setAutoThreshold("Otsu"); 
    		IJ.run("Convert to Mask"); 
    		imp.updateAndDraw(); 
    		IJ.run("Make Binary"); 
    		IJ.run("Fill Holes"); 
    		IJ.run("Watershed"); 
    		IJ.run("Set Measurements...", "area"); 
    		// noise extraction
     		IJ.run("Analyze Particles...", "size=0-Infinity show=Nothing display");  
    		TextWindow tw = (TextWindow)WindowManager.getFrame("Results"); 
    	    ResultsTable rt = tw.getTextPanel().getResultsTable(); 
    	    float areas[] = rt.getColumn(0);
    	    for (i=0; i<areas.length; i++)
    	    	if (areas[i]>maior)
    	    		maior = areas[i];
    		IJ.run("Analyze Particles...", "size="+(maior/3)+"-Infinity show=Masks summarize");
    		finalImage = IJ.getImage();

    		// elements counting 
    		//tw = (TextWindow)WindowManager.getFrame("Summary"); 
    	    //rt = tw.getTextPanel().getResultsTable(); 
    		impGray.changes = false;
    		impGray.close(); 
        }

		IJ.selectWindow("Results"); 
		IJ.run("Close");

        //return finalImage;  // this is the final mask image

		IJ.showMessage("End of the plugin");

   }
}
