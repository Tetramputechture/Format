/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package format;

import java.io.FileNotFoundException;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import org.apache.commons.io.FileUtils;


/**
 * An abstract class that extends various image formats.
 * @author Nick
 */
public abstract class Format {
    
    /**
     * logger
     */
    private static final Logger log = Logger.getLogger(Format.class.getName());
    
    /**
     * Holds the image data.
     */
    public BufferedImage image;
    
    /**
     * Holds file info.
     */
    public List<String> info;
    
    /**
     * Filename of the image.
     */
    public String filename;
    
    /**
     * File data.
     */
    public byte[] data;
    
    /**
     * Size of image data in bytes.
     */
    public int size;
    
    /**
     * If the file is valid or not.
     */
    boolean isValid;
    
    /**
     * Image width.
     */
    public int width;
    
    /**
     * Image height.
     */
    public int height;
    
    /**
     * Color depth (bit depth).
     */
    public int colorDepth;
    
    /**
     * Various image effects.
     */
    public enum Effect {
        /**
         * No effect.
         */
        NO_EFFECT,
        /**
         * Black & white effect.
         */
        NOIR
    }
    
    public Effect effect;
    
    /**
     * Converts a hex string to an integer.
     * @param str The hex string to be converted.
     * @return The decimal representation of the hex string.
     */
    public static int hexStringToInt(String str) {
        return Integer.parseInt(str, 16);
    }
    
    /**
     * Converts an rgb value to a single integer pixel.
     * @param r The red value to be converted.
     * @param g The green value to be converted.
     * @param b The blue value to be converted.
     * @return The integer value of the rgb value.
     */
    public static int rgbToInt(int r, int g, int b) {
        return ((r&0x0ff)<<16)|((g&0x0ff)<<8)|(b&0x0ff); 
    }
    
    /**
     * Converts a single byte into a String.
     * @param b The byte to be converted.
     * @return The String representation of the byte.
     */
    public static String byteToString(byte b) {
        return String.format("%02X", b);
    }
    
    /**
     * Fetches the file, and its byte data
     */
    public void getFileByteData() {
        
        File file = new File(this.filename);
        
        // get header byteData in bytes
        try {
            this.data = FileUtils.readFileToByteArray(file);
        } catch (IOException ex) {
            log.log(Level.SEVERE, 
                    String.format("File %s not found!", this.filename), 
                    ex);
        }
        
        // set image size
        this.size = this.data.length;
        
        // image is valid until proven invalid
        this.isValid = true;
        
        // make ArrayList of strings for file info
        this.info = new ArrayList<>();
    }
    
    /**
     * Loads an image from this format.
     * @param filename the name of the file to be loaded
     * @throws FileNotFoundException
     */
    public abstract void load(String filename) throws FileNotFoundException;
    
    /**
     * Saves an image from this format.
     * @param filename the name of the file to be saved
     */
    public abstract void save(String filename);
    
    public void setEffect(Effect effect) {
        switch(effect) {
            case NO_EFFECT:
                System.out.println("No effect applied!\n");
                break;
            case NOIR:
                System.out.println("Noir effect applied!\n");
                break;
            default:
                System.out.println("Invalid effect!\n");
        }
    }

}
