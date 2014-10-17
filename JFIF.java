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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Class for JPEG File Interchange Format image files.
 * @author Nick
 */
public class JFIF extends Format {
    
    /**
     * logger
     */
    private static final Logger log = Logger.getLogger(JFIF.class.getName());
    
    /**
     * File version
     */
    public String version;
    
    /**
     * Units for pixel density
     */
    public String units;
    
    /**
     * X density
     */
    public int xDensity;
    
    /**
     * Y density
     */
    public int yDensity;
    
    /**
     * Thumbnail width
     * 0 - no thumbnail
     */
    public int thumbWidth;
    
    /**
     * Thumbnail height
     * 0 - no thumbnail
     */
    public int thumbHeight;
    
    /**
     * Thumbnail image
     */
    public BufferedImage thumbnail;
    
    /**
     * File comment
     */
    public String comment;
    
    public int rInterval;
    
    public int componentCount;
    
    public int yHSF;
    public int yVSF;
    public int yQT;
    
    public int cbHSF;
    public int cbVSF;  
    public int cbQT;
    
    public int crHSF;
    public int crVSF;
    public int crQT;
    
    public int maxHSF;
    public int maxVSF;
    
    public int[][] huffValues;
    public int[][] huffCodeLengths;
    public int[][] huffCodeTables;
    
    public static float C2 = (float) (2.0 * Math.cos(Math.PI/8));
    public static float C4 = (float) (2.0 * Math.cos(2 * Math.PI/8));
    public static float C6 = (float) (2.0 * Math.cos(3 * Math.PI/8));
    public static float Q = C2 - C6;
    public static float R = C2 + C6;
    
    
    @Override
    public void load(String filename) throws FileNotFoundException {
        
        // set filename
        this.filename = filename;
        System.out.println("Filename: " + filename);
        
        // get file data
        getFileByteData();

        // validate file
        checkFile();
        
        System.out.println("Size: " + this.size + " bytes");
        
        // Since file has been validated, SOI index is 0
        
        // get file info
        getSOI();
               
        // get file comments
        getCOM();
        
        // get quantization tables
        getDQT();
        
        // get restart interval
        getDRI();
        
        // get huffman tables
        getDHT();
        
        // get Start of Frame info (width, height, etc)
        getSOF();
        
        // get scan info and set image
        getSOS();
        
        // Since file has been validated, 
        // EOI index is always the last marker (0xFFD9)
        
        // finally, set file info
        setInfo();
    }
    
    // ********* / / / / / /*********
    // ********* / / / / / /*********
    // ********* FILE CHECK *********
    // ********* / / / / / /*********
    // ********* / / / / / /*********
    
    private void checkFile() {

        // Start Of Image (SOI) must be FFD8 and the next marker must be FF
        if(!(this.data[0] == (byte) 0xFF && 
             this.data[1] == (byte) 0xD8 &&
             this.data[2] == (byte) 0xFF)) 
            
            this.isValid = false;
        
        
        // check if file is not valid
        if(!isValid) 
            log.log(Level.SEVERE, 
                    String.format("ERROR: File %s is not registered as a JFIF!\n", this.filename), 
                    new IllegalArgumentException());
    }
    
    // ********* / / / / / *********
    // ********* / / / / / *********
    // ********* JFIF INFO *********
    // ********* / / / / / *********
    // ********* / / / / / *********
    
    private void getSOI() {
        
        System.out.println("\n{{{ APP0 Marker }}}\n");
        
        // Since file has been validated, JFIF index is 2
        int soiIndex = 2;
        
        // Get SOI  length
        String l = byteToString(this.data[soiIndex+2]) 
                    + byteToString(this.data[soiIndex+3]); 
        int soiLength = hexStringToInt(l);
        
        System.out.println("Length: " + soiLength);
        
        // Get identifier (should be "JFIF")
        String identifier = "";
        for(int i = 0; i < 5; i++) {
            identifier += (char)this.data[soiIndex+4+i];
        }
        System.out.println("Identifier: " + identifier);
        
        // Get JPEG this.version
        this.version = Byte.toString(this.data[soiIndex+9]) + "." // major revision
                     + Byte.toString(this.data[soiIndex+10]);     // minor revision
        
        System.out.println("Version: " + this.version);
        
        // Get this.units for X and Y densities
        // 0 - No this.units, X and Y specify pixel aspect ratio
        // 1 - X and Y are in dots per inch
        // 2 - X and Y are in dots per cm
        switch(this.data[soiIndex+11]) {
            case (byte) 0x01:
                this.units = " dpi";
                break;
            case (byte) 0x02:
                this.units = " dpcm";
                break;
            default:
                this.units = "";
        }
        
        // Get X density
        this.xDensity = (this.data[soiIndex+12] & 0xFF) + (this.data[soiIndex+13] & 0xFF);
        System.out.println("X Density: " + this.xDensity + this.units);
        
        // Get Y density
        this.yDensity = (this.data[soiIndex+14] & 0xFF) + (this.data[soiIndex+15] & 0xFF);
        System.out.println("Y Density: " + this.yDensity + this.units);
        
        // Get thumbnail width 
        this.thumbWidth = this.data[soiIndex+16];
        
        // Get thumbnail height
        this.thumbHeight = this.data[soiIndex+17];
       
        if(!(this.thumbWidth == 0 || this.thumbHeight == 0)) {
            
            // load thumbnail pixels into thumbnail
            this.thumbnail = new BufferedImage(this.width,
                                       this.height, 
                                       BufferedImage.TYPE_INT_RGB);
            
            // Get RGB pixel array for thumbnail
            for(int y = 0; y < thumbHeight; y++) {
                for(int x = 0; x < thumbWidth; x++) {
                    
                    int index = (thumbWidth*y + x) * 3 + 18;
                    int r = this.data[index];
                    int g = this.data[index+1];
                    int b = this.data[index+2];

                    // merge rgb values to single int
                    int rgb = rgbToInt(r, g, b);

                    this.thumbnail.setRGB(x, y, rgb);
                }
            }
        }
    }
    
    // ********* / / / / / / /  *********
    // ********* / / / / / / /  *********
    // *********    COMMENTS    *********
    // ********* / / / / / / /  *********
    // ********* / / / / / / /  *********    
    
    private void getCOM() {
        
        System.out.println("\n{{{ Comments }}}\n");
        
        // Read until Comment marker (0xFFFE)
        int comIndex = 0;
        while(!(this.data[comIndex]   == (byte) 0xFF && 
                this.data[comIndex+1] == (byte) 0xFE)) {
            comIndex++;
        }
     
        //System.out.println("COM marker at offset " + comIndex);
        
        // Get comment length
        String l = byteToString(this.data[comIndex+2]) 
                    + byteToString(this.data[comIndex+3]); 
        int comLength = hexStringToInt(l);
        
        System.out.println("Length: " + comLength);
        
        // Convert comment to string
        this.comment = "";
        for(int i = 0; i < comLength-2; i++) {
            this.comment += (char)this.data[comIndex+4+i];
        }
        
        System.out.println("Comment: " + this.comment);
    }
    
    // ********* / / / / / / /  *********
    // ********* / / / / / / /  *********
    // ********* START OF FRAME *********
    // ********* / / / / / / /  *********
    // ********* / / / / / / /  *********
    
    private void getSOF() {
        
        System.out.println("\n\n{{{ Start Of Frame }}}\n");
        
        // Read until Start of Frame marker (0xFFC0)
        int sofIndex = 0;
        while(!(this.data[sofIndex]   == (byte) 0xFF && 
                this.data[sofIndex+1] == (byte) 0xC0)) {
            sofIndex++;
        }
       
        //System.out.println("SOF0 marker at offset " + sofIndex);
        String l = byteToString(this.data[sofIndex+2]) 
                    + byteToString(this.data[sofIndex+3]); 
        int sofLength = hexStringToInt(l);
        
        System.out.println("Length: " + sofLength);
        
        // Get color depth
        this.colorDepth = this.data[sofIndex+4];
        
        System.out.println("Color depth: " + this.colorDepth);
        
        // Get image height
        String h = byteToString(this.data[sofIndex+5]) + byteToString(this.data[sofIndex+6]);
        this.height = hexStringToInt(h);
        System.out.println("Height: " + this.height);
        
        // Get image width
        String w = byteToString(this.data[sofIndex+7]) + byteToString(this.data[sofIndex+8]); 
        this.width = hexStringToInt(w);
        System.out.println("Width: " + this.width);
        
        // Get component count
        this.componentCount = this.data[sofIndex+9];
        
        System.out.println("Number of components: " + this.componentCount);

        // Each component has 3 bytes associated with it:
        // - Component ID
        // - H and V sampling factors - H is first four bits and V is second four bits
        // Quantization table number  
        
        /////////////////
        // Y component //
        /////////////////
        
        int comp = this.data[sofIndex+10];
        
        // Split HV byte into two 4 bit sections
        String yHV = byteToString(this.data[sofIndex+11]);
        this.yHSF = Character.getNumericValue(yHV.charAt(0));
        this.yVSF = Character.getNumericValue(yHV.charAt(1));
        
        // Get quantization table number
        this.yQT = this.data[sofIndex+12];
        
        System.out.printf("Component %s\n "
                        + "Horizontal Frequency: %s\n"
                        + " Vertical Frequency: %s\n"
                        + " Quantization Table: %s\n", comp, yHSF, yVSF, yQT);
        
        if(componentCount == 3) {
            
            //////////////////
            // Cb component //
            //////////////////
            
            comp = this.data[sofIndex+13];
            
            String cbHV = byteToString(this.data[sofIndex+14]); 
            this.cbHSF = Character.getNumericValue(cbHV.charAt(0));
            this.cbVSF = Character.getNumericValue(cbHV.charAt(1));

            this.cbQT = this.data[sofIndex+15];
            
            System.out.printf("Component %s\n "
                            + "Horizontal Frequency: %s\n"
                            + " Vertical Frequency: %s\n"
                            + " Quantization Table: %s\n", comp, cbHSF, cbVSF, cbQT);         
            
            //////////////////
            // Cr component //
            //////////////////
            
            comp = this.data[sofIndex+16];
            
            String crHV = byteToString(this.data[sofIndex+17]);
            this.crHSF = Character.getNumericValue(crHV.charAt(0));
            this.crVSF = Character.getNumericValue(crHV.charAt(1));
        
            this.crQT = this.data[sofIndex+16];
            
            System.out.printf("Component %s\n "
                            + "Horizontal Frequency: %s\n"
                            + " Vertical Frequency: %s\n"
                            + " Quantization Table: %s\n", comp, crHSF, crVSF, crQT);  
        }
        
        // Find max of all sampling freqs
        this.maxHSF = Math.max(yHSF, Math.max(cbHSF, crHSF));
        this.maxVSF = Math.max(yVSF, Math.max(cbVSF, crVSF));
    }
   
    // ********* / / / / / / / *********
    // ********* / / / / / / / *********
    // ********* HUFFMAN TABLE *********
    // ********* / / / / / / / *********
    // ********* / / / / / / / *********
    
    private void getDHT() {
        
        System.out.println("\n{{{ Define Huffman Table }}}\n");
        
        // Read until DHT marker (0xFFC4)
        int dhtIndex = 0;
        while(!(this.data[dhtIndex]   == (byte) 0xFF && 
                this.data[dhtIndex+1] == (byte) 0xC4)) {
            dhtIndex++;
        }
        
        //System.out.println("DHT marker at offset: " + dhtIndex);
        
        // Get DHT length
        String l = byteToString(this.data[dhtIndex+2]) 
                    + byteToString(this.data[dhtIndex+3]); 
        int dhtLength = hexStringToInt(l);
        
        System.out.println("Length: " + dhtLength);
        
        // initialize arrays
        int[][] huffCodeCounts = new int[4][16];
        
        this.huffCodeLengths = new int[4][256];
        this.huffValues = new int[4][256];    
        this.huffCodeTables = new int[4][256];
        
        // make counter for number of tables
        // every time a table is read, subtract its length from this value
        // when this value is 0, all tables have been read
        int tCount = dhtLength - 2;
        
        // start 4 bytes after marker
        int tOffset = dhtIndex + 4;

        while(tCount > 0) {
            
            // table number
            int n = 0;
        
            // Get the table index and class
            String tByte = byteToString(this.data[tOffset]);
            int tIndex = Character.getNumericValue(tByte.charAt(1));
            String tClass;
            if(Character.getNumericValue(tByte.charAt(0)) == 0)
                tClass = "DC";
            else
                tClass = "AC";

            System.out.println("\n\nTable Index: " + tIndex);
            System.out.println("Table Class: " + tClass);

            int tValueCount = 0;

            // get codes from 1-16
            //System.out.print(" Code Counts: ");
            for(int i = 0; i < 16; i++) {
                huffCodeCounts[n][i] = this.data[tOffset+1+i];
                //System.out.print(this.huffCodeCounts[n][i] + " ");
                tValueCount += this.data[tOffset+1+i];
            }
            
            // get max count length
            List c = Arrays.asList(ArrayUtils.toObject(huffCodeCounts[n]));
            int maxLength = (int) Collections.max(c);
            
            // turn code counts to lengths
            countsToLengths(huffCodeCounts[n],
                            maxLength,
                            this.huffCodeLengths[n]);

            // get the code values
            // System.out.print("\n Code Values:\n");
            for(int i = 0; i < tValueCount; i++) {
                this.huffValues[n][i] = this.data[tOffset+16+i] & 0xFF;
            }
            
            System.out.println(" Code Lengths: ");
            for(int i = 0; i < tValueCount; i++) {
                System.out.print(huffCodeLengths[n][i] + " ");
                if((i+1) % 16 == 0 && i > 0) {
                    System.out.println();
                }
            }
            
            // generate Huffman table
            generateHuffmanCodes(tValueCount,
                                 huffCodeLengths[n],
                                 this.huffCodeTables[n]);
            
            System.out.println("\n Codes: " );
            for(int i = 0; i < tValueCount; i++) {
                System.out.print(this.huffCodeTables[n][i] + " ");
                if((i+1) % 16 == 0 && i > 0) {
                    System.out.println();
                }
            }
            
            tCount -= 17 + tValueCount;
            tOffset += 17 + tValueCount;
            n++;
        }
        
    }
    
    private static void countsToLengths(int[] counts, 
                                int maxLength,
                                int[] codeLengths) {
        int index = 0;
        for(int i = 0; i < 16; i++) {
            for(int j = 0; j < counts[i]; j++) {
                codeLengths[index] = i;
                index++;
            }
        }
    }
    
    private static void generateHuffmanCodes(int numCodes,
                                      int[] codeLengths,
                                      int codes[]) {
        int huffmanCodeCounter = 0;
        int codeLengthCounter = 0;
        for(int index = 0; index < numCodes; index++) {
            
            while(codeLengths[index] > codeLengthCounter) {
                huffmanCodeCounter <<= 1;
                codeLengthCounter++;
            }
            
            codes[index] = huffmanCodeCounter;
            huffmanCodeCounter++;
        }
    }
    
    private void decodeHT() {
        // TO DO
    }
    
    /**
     * Fast Inverse Discrete Cosine Transform on an 8x8 matrix
     * Algorithm credit: https://vsr.informatik.tu-chemnitz.de/~jan/MPEG/HTML/IDCT.html
     */
    
    private void IDCT8x8(final float[] matrix) {
        
        float a2, a3, a4, tmp1, tmp2, a5, a6, a7; // B1
        float tmp4, b2, neg_b4, b5, b6; // M
        float tmp3, n0, n1, n2, n3, neg_n5; // A1
        float m3, m4, m5, m6, neg_m7; // A2
        
        for(int i = 0; i < 8; i++) {
            
            // B1
            a2 = matrix[8 * i + 2] - matrix[8 * i + 6];
            a3 = matrix[8 * i + 2] + matrix[8 * i + 6];
            a4 = matrix[8 * i + 5] - matrix[8 * i + 3];
            tmp1 = matrix[8 * i + 1] + matrix[8 * i + 7];
            tmp2 = matrix[8 * i + 3] + matrix[8 * i + 5];
            a5 = tmp1 - tmp2;
            a6 = matrix[8 * i + 1] - matrix[8 * i + 7];
            a7 = tmp1 + tmp2;
            
            // M
            b2 = a2 * C4;
            tmp4 = C6 * (a4 + a6);
            neg_b4 = Q * a4 + tmp4;
            b5 = a5 * C4;
            b6 = R * a6 - tmp4;
            
            // A1
            tmp3 = b6 - a7;
            n0 = tmp3 - b5;
            n1 = matrix[8 * i] - matrix[8 * i + 4];
            n2 = b2 - a3;
            n3 = matrix[8 * i] + matrix[8 * i + 4];
            neg_n5 = neg_b4;
            
            // A2
            m3 = n1 + n2;
            m4 = n3 + a3;
            m5 = n1 - n2;
            m6 = n3 - a3;
            neg_m7 = neg_n5 + n0;
            
            // peform operation on matrix
            matrix[8 * i] = m4 + a7;
            matrix[8 * i + 1] = m3 + tmp3;
            matrix[8 * i + 2] = m5 - n0;
            matrix[8 * i + 3] = m6 + neg_m7;
            matrix[8 * i + 4] = m6 - neg_m7;
            matrix[8 * i + 5] = m5 + n0;
            matrix[8 * i + 6] = m3 - tmp3;
            matrix[8 * i + 7] = m4 - a7;
        }
        
        for(int i = 0; i < 8; i++) {
            // B1
            a2 = matrix[16 + i] - matrix[48 + i];
            a3 = matrix[16 + i] + matrix[48 + i];
            a4 = matrix[40 + i] - matrix[24 + i];
            tmp1 = matrix[8 + i] + matrix[56 + i];
            tmp2 = matrix[8 + i] + matrix[40 + i];
            a5 = tmp1 - tmp2;
            a6 = matrix[8 + i] - matrix[56 + i];
            a7 = tmp1 + tmp2;
            
            // M
            b2 = a2 * C4;
            tmp4 = C6 * (a4 + a6);
            neg_b4 = Q * a4 + tmp4;
            b5 = a5 * C4;
            b6 = R * a6 - tmp4;
            
            // A1
            tmp3 = b6 - a7;
            n0 = tmp3 - b5;
            n1 = matrix[i] - matrix[32 + i];
            n2 = b2 - a3;
            n3 = matrix[i] + matrix[32 + i];
            neg_n5 = neg_b4;
            
            // A2
            m3 = n1 + n2;
            m4 = n3 + a3;
            m5 = n1 - n2;
            m6 = n3 - a3;
            neg_m7 = neg_n5 + n0;
            
            // peform operation on matrix
            matrix[i] = m4 + a7;
            matrix[8 + i] = m3 + tmp3;
            matrix[16 * i] = m5 - n0;
            matrix[24 * i] = m6 + neg_m7;
            matrix[32 * i] = m6 - neg_m7;
            matrix[40 * i] = m5 + n0;
            matrix[48 * i] = m3 - tmp3;
            matrix[56 * i] = m4 - a7;
        }
    }
    
    // ********* / / / / / / / / / / / / / *********
    // ********* / / / / / / / / / / / / / *********
    // ********* / / QUANTIZATION TABLE / /*********
    // ********* / / / / / / / / / / / / / *********
    // ********* / / / / / / / / / / / / / *********
    
    private void getDRI() {
        
        System.out.println("\n{{{ Define Restart Interval }}}\n");
        
        // Read until Define Restart Interval (0xFFDD)
        // Optional, so check if file even has it 
        
        int driIndex = 0;
        while(driIndex < this.size && (!(this.data[driIndex] == (byte) 0xFF && 
                this.data[driIndex+1] == (byte) 0xDD))) {
            driIndex++;
        }
        
        if(driIndex == this.size) {
            System.out.println("No Define Restart Interval Marker");
            this.rInterval = 0;
            return;
        }
        
        // length is always 4
        System.out.println("Length: 4");
        
        // Get restart interval
        String i = byteToString(this.data[driIndex+4]) 
                    + byteToString(this.data[driIndex+5]); 
        this.rInterval = hexStringToInt(i);
        
        System.out.println("Restart Interval: " + this.rInterval);
    }
    
    private void getDQT() {
        
        System.out.println("\n{{{ Define Quantization Table }}}\n");
        
        // Read until Define Quantisation Table(s) marker (0xFFDB)
        int dqtIndex = 0;
        while(!(this.data[dqtIndex]   == (byte) 0xFF && 
                this.data[dqtIndex+1] == (byte) 0xDB)) {
            dqtIndex++;
        }
        
        //System.out.println("DQT marker at offset " + dqtIndex);
                
        // Get DQT length
        String l = byteToString(this.data[dqtIndex+2]) 
                    + byteToString(this.data[dqtIndex+3]); 
        int dqtLength = hexStringToInt(l);
        
        System.out.println("Length: " + dqtLength);
        
        // Un zig-zag the tables
        
        
        // Get first table info
        /*
        String firstTableByte = byteToString(this.data[dqtIndex+2]);
        int firstTableIndex = Character.getNumericValue(firstTableByte.charAt(0));
        int firstTablePrecision = Character.getNumericValue(firstTableByte.charAt(1));
        
        System.out.println("Table Index: " + firstTableIndex);
        System.out.println("Table Precision: " + firstTablePrecision);
        System.out.println("Table Values: TO DO");
        
        // Get second table info
        String secondTableByte = byteToString(this.data[dqtIndex+69]);
        System.out.println(secondTableByte);
        int secondTableIndex = Character.getNumericValue(secondTableByte.charAt(0));
        int secondTablePrecision = Character.getNumericValue(secondTableByte.charAt(1));
        
        System.out.println("Table Index: " + secondTableIndex);
        System.out.println("Table Precision: " + secondTablePrecision);
        System.out.println("Table Values: TO DO");*/
        
    }
    
    // ********* / / / / / / / *********
    // ********* / / / / / / / *********
    // ********* START OF SCAN *********
    // ********* / / / / / / / *********
    // ********* / / / / / / / *********
    
    private void getSOS() {
        
        System.out.println("\n{{{ Start Of Scan }}}\n");
        
        // Read until Start Of Scan marker (0xFFDA)
        int sosIndex = 0;
        while(!(this.data[sosIndex]   == (byte) 0xFF && 
                this.data[sosIndex+1] == (byte) 0xDA)) {
            sosIndex++;
        }
        
        //System.out.println("SOS marker at offset " + sosIndex);
        
        String l = byteToString(this.data[sosIndex+2]) 
                    + byteToString(this.data[sosIndex+3]); 
        int sosLength = hexStringToInt(l);
        
        System.out.println("Length: " + sosLength);
        
        int scanCount = this.data[sosIndex+4];
        
        System.out.println("Scan count: " + scanCount);
        
        for(int i = 0; i < scanCount*2; i += 2) {
            
            // get component identifier
            int cID = this.data[sosIndex+5+i];
            System.out.println(" Component ID: " + cID);
            
            // get what tables are used for the identifier
            String tType = byteToString(this.data[sosIndex+6+i]);
            int dcTType = Character.getNumericValue(tType.charAt(0));
            int acTType = Character.getNumericValue(tType.charAt(1));
            System.out.println("  AC Entropy Table: " + acTType);
            System.out.println("  DC Entropy Table: " + dcTType);
        }
        
        int ssStart = this.data[sosIndex+sosLength-1];
        System.out.println("Spectral Selection Start: " + ssStart);
        
        int ssEnd = this.data[sosIndex+sosLength];
        System.out.println("Spectral Selection End: " + ssEnd);
        
        String sa = byteToString(this.data[sosIndex+sosLength+1]);
        int saHigh = Character.getNumericValue(sa.charAt(0));
        int saLow = Character.getNumericValue(sa.charAt(1));
        
        System.out.println("Successive Approximation High: " + saHigh);
        System.out.println("Successive Approximation Low: " + saLow);
        
        // Decode scan data
        
        // Get size of data units for components
        int yPixelsX = 8 * (this.maxHSF / this.yHSF);
        int yPixelsY = 8 * (this.maxVSF / this.yVSF);
        
        System.out.println("Pixels per Y component (X): " + yPixelsX);
        System.out.println("Pixels per Y component (Y): " + yPixelsY);

        
        if(this.componentCount == 3) {
            int cbPixelsX = 8 * (this.maxHSF / this.cbHSF);
            int cbPixelsY = 8 * (this.maxVSF / this.cbVSF);
            
            System.out.println("Pixels per Cb component (X): " + cbPixelsX);
            System.out.println("Pixels per Cb component (Y): " + cbPixelsY);
            
            int crPixelsX = 8 * (this.maxHSF / this.crHSF);
            int crPixelsY = 8 * (this.maxVSF / this.crVSF);  
            
            System.out.println("Pixels per Cr component (X): " + crPixelsX);
            System.out.println("Pixels per Cr component (Y): " + crPixelsY);
        }
        
        int mcusx, mcusy;
        int mcuCount;
        
        // Get total number of MCUs
        if(this.componentCount == 1) {
            mcusx = (this.width + yPixelsX - 1) / yPixelsX;
            mcusy = (this.height + yPixelsY - 1) / yPixelsY; 
        }
        
        else {
            int c = 8 * this.maxHSF;
            mcusx = (this.width + c - 1) / c;
            mcusy = (this.height + c - 1) / c;
        }
        
        mcuCount = mcusx * mcusy;
        
        // get number of data units per MCU
        int dataUnitsPerMCU = (this.yHSF * this.yVSF) + 
                              (this.cbHSF * this.cbVSF) +
                              (this.crHSF * this.crVSF);
        
//        // loop through each MCU
//        for(int m = 0; m < mcuCount; m++) {
//            
//            // loop through each component in the scan
//            for(int i = 0; i < this.yVSF; i++) {
//                for(int j = 0; j < this.yVSF; j++) {
//                    decodeDataUnit(
//                }
//            }
//        }
            
        
        
    }
    
    private void setInfo() {
        
        this.info.add("Format: JPEG, this.version " + this.version);
        this.info.add("Size: " + Integer.toString(this.size) + " bytes");
        this.info.add("Dimensions: " + Integer.toString(this.width) + " x " + Integer.toString(this.height));
        this.info.add("Width: " + Integer.toString(this.width) + " px");
        this.info.add("Height: " + Integer.toString(this.height) + " px");
        this.info.add("Bit depth: " + Integer.toString(this.colorDepth));
        this.info.add("X Density: " + this.xDensity + this.units);
        this.info.add("Y Density: " + this.yDensity + this.units);
        this.info.add("Comments: " + this.comment);
    }

    @Override
    public void save(String path) {
        if(!(filename.contains(".jpg") || filename.contains(".JPG")))
            filename += ".jpg";
        File f = new File(path);
        try {
            ImageIO.write(this.image, "JPG", f);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        JFIF img = new JFIF();
        img.load("jpgtest.jpg");
        //img.save("testout");
    }
}
