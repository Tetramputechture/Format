/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package format;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Class for Bitmap image files.
 * @author Nick
 */
public class Bitmap extends Format {
    
    /**
     * logger
     */
    private static final Logger log = Logger.getLogger(Bitmap.class.getName());
    
    /**
     * Compression types of the image.
     * Either no compression, RLE-4 compression, or RLE-8 compression
     */
    public String compressionType;
    
    /**
     * Width adjusted for padding, needed for image writing.
     */
    public int cWidth;
   
    /**
     * Horizontal resolution in pixels per meter.
     */
    public int horzResolution;
    
    /**
     * Vertical resolution in pixels per meter.
     */
    public int vertResolution;
    
    /**
     * Number of colors used in image.
     */
    public int colorsUsed;
    
    /**
     * Minimum number of important colors used in image.
     * Zero means every color is important.
     */
    public int colorsImportant;
    
       /**
     * Loads a Bitmap image.
     * @param filename the name of the file to be loaded
     */
    @Override
    public void load(String filename) {
        
        // set filename
        this.filename = filename;
        
        // fetch bitmap file
        getFileByteData();

        // validate file
        checkFile();
        
        // get file info (width, height, etc)
        getHeader();
        
        // load pixels into an image and set effect
        setImage();
        
        // finally, set file info
        setInfo();
    }
    
    private void checkFile() {
        
        // signature must be 42 4D ("BM") 
        if(!(this.data[0] == (byte) 0x42 && this.data[1] == (byte) 0x4D))
            isValid = false;

        // these offsets must be 0
        for(int i = 6; i <= 9; i++) 
            if(!(this.data[i] == (byte) 0x00))
                isValid = false;

        // BITMAPINFOHEADER structure at offset 14 must be 40
        if(!(this.data[14] == (byte) 0x28))
            isValid = false;

        // check if file is not valid
        if(!isValid) 
            log.log(Level.SEVERE, 
                    String.format("File %s is not registered as a bitmap!\n", this.filename), 
                    new IllegalArgumentException());
    }
    
    private void getHeader() {
        // get width of image
        // since width size is 4 bytes, 
        // we must build a string from right to left
        String w = "";
        for(int i = 3; i >= 0; i--) {
            w += String.format("%02X", this.data[18+i]); 
        }
        this.width = hexStringToInt(w);

        // get height of image
        // since height size is 4 bytes, 
        // we must build a string from right to left
        String h = "";
        for(int i = 3; i >= 0; i--) {
            h += String.format("%02X", this.data[22+i]); 
        }
        this.height = hexStringToInt(h);
        
        // number of planes at offset 26 must be 1
        if(!(this.data[26] == (byte) 0x01))
            isValid = false;

        // get bit depth
        this.colorDepth = hexStringToInt(String.format("%02X", this.data[28]));
        
        // get width including padding
        this.cWidth = (this.width + 3) & ~3;

        // get compression type
        byte cType = this.data[30];
        switch (cType) {
            case (byte) 0x00:
                this.compressionType = "None";
                break;
            case (byte) 0x01:
                this.compressionType = "RLE-8";
                break;
            case (byte) 0x02:
                this.compressionType = "RLE-4";
                break;
            default:
                isValid = false;
        }
        
        // get horizontal resolution in pixels per meter
        // since horizontal resolution is 4 bytes,
        // we must build a string from right to left
        String hr = "";
        for(int i = 3; i >= 0; i--) {
            hr += String.format("%02X", this.data[38+i]);
        }
        this.horzResolution = hexStringToInt(hr);
        
        // get vertical resoltion in pixels per meter
        // since vertical resolution is 4 bytes,
        // we must build a string from right to left
        String vr = "";
        for(int i = 3; i >= 0; i--) {
            vr += String.format("%02X", this.data[42+i]);
        }
        this.vertResolution = hexStringToInt(vr);
        
        // get number of colors in the image
        // if bit depth is > 16, value should be 0
        this.colorsUsed = this.data[46];
        
        // get number of important colors in the image
        // if 0, all colors are important
        this.colorsImportant = this.data[50];
    }
    
    private void setImage() {
        
        this.image = new BufferedImage(this.width,
                                       this.height, 
                                       BufferedImage.TYPE_INT_RGB);
        
        // start from bottom row
        for(int y = this.height-1; y >= 0; y--) {
            for(int x = 0; x < this.width; x++) {
                
                int index = (cWidth*y + x) * 3 + 54;
                int b = this.data[index];
                int g = this.data[index+1];
                int r = this.data[index+2];
             
                // merge rgb values to single int
                int rgb = rgbToInt(r, g, b);
                
                // build image from bottom up
                if(rgb != 0)
                    this.image.setRGB(x, this.height-1-y, rgb);
            }
        }
        
        // set effect
        this.effect = Effect.NO_EFFECT;
    }
    
    private void setInfo() {
        
        this.info.add("Format: Bitmap");
        this.info.add("Size: " + Integer.toString(this.size) + " bytes");
        this.info.add("Dimensions: " + Integer.toString(this.width) + " x " + Integer.toString(this.height));
        this.info.add("Width: " + Integer.toString(this.width) + " px");
        this.info.add("Height: " + Integer.toString(this.height) + " px");
        this.info.add("Bit depth: " + Integer.toString(this.colorDepth));
        this.info.add("Compression type: " + this.compressionType);
        this.info.add("Horizontal Resolution (ppm): " + Integer.toString(this.horzResolution));
        this.info.add("Vertical Resolution (ppm): " + Integer.toString(this.vertResolution));
        
    }
    
    /**
     * Saves a Bitmap image as specified filename
     * @param path the path of the file to be saved, with extension .bmp
     */
    @Override
    public void save(String path) {
        
        // add ".bmp" onto file if not already
        if(!(path.contains(".bmp") || path.contains(".BMP")))
            path += ".bmp";
        
        File sf = new File(path);
        try {
            ImageIO.write(this.image, "BMP", sf);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
  
    public static void main(String[] args) throws FileNotFoundException {
        Bitmap img = new Bitmap();
        img.load("test2.bmp");
        img.save("testout");
    }
}
