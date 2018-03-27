package com.github.jasjisdo.ctview;

import com.github.jasjisdo.ctview.components.DetailImageFrame;
import com.github.jasjisdo.ctview.eventhandler.GUIEventHandler;
import com.github.jasjisdo.ctview.filefilter.DmsFileFilter;
import com.github.jasjisdo.ctview.listener.ImageIconMouseListener;
import com.github.jasjisdo.ctview.listener.LoadButtonActionListener;
import lombok.NonNull;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.*;

public class CTScanView extends JFrame {

    /*
     //////////////////////////////
     //  Class Fields / Members  //
     //////////////////////////////
     */

    private JFileChooser fileChooser = new JFileChooser();
    private JPanel gridPanel = new JPanel();
    private JScrollPane gridScrollPane = new JScrollPane();
    private JButton loadButton;             // an button to load the CThead.dms file
    private JButton mipButton;              // an example button to switch to MIP mode
    private JLabel imageIcon1;              // using JLabel to display an image (check online documentation)
    private JLabel imageIcon2;              // using JLabel to display an image (check online documentation)
    private JLabel imageIcon3;              // using JLabel to display an image (check online documentation)
    private JSlider zSliceSlider,           // sliders to step through the slices (z and y directions)
            ySliceSlider,                   // (remember 113 slices in z direction 0-112)
            xSliceSlider;
    private @NonNull BufferedImage zImage1, // storing the image in memory
            yImage2,
            xImage3;
    private short cthead[][][];             // store the 3D volume data set
    private short min, max;                 // min/max value in the 3D volume data set

    /*
     ///////////////////
     //  Constructor  //
     ///////////////////
     */

    private CTScanView(String title) throws HeadlessException {
        super(title);

        // add and set file filter to file chooser.
        FileFilter filter = new DmsFileFilter();
        this.fileChooser.addChoosableFileFilter(filter);
        this.fileChooser.setFileFilter(filter);

        // set directory that will be shown when file chooser opens.
        String workingDir = System.getProperty("user.dir");
        this.fileChooser.setCurrentDirectory(new File(workingDir));

        //Create a BufferedImage to store the image data
        zImage1 = new BufferedImage(256, 256, BufferedImage.TYPE_3BYTE_BGR); // x,y
        yImage2 = new BufferedImage(256, 112, BufferedImage.TYPE_3BYTE_BGR); // x,z
        xImage3 = new BufferedImage(112, 256, BufferedImage.TYPE_3BYTE_BGR); // z,y

        // Set up the simple GUI
        // First the container:
        Container container = getContentPane();
        BorderLayout borderLayout = new BorderLayout();
        container.setLayout(borderLayout);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        container.add(titlePanel, BorderLayout.NORTH);

        JPanel imagesPanel = new JPanel();
        imagesPanel.setLayout(new FlowLayout());
        container.add(imagesPanel, BorderLayout.CENTER);

        GridLayout thumblayout = new GridLayout(0, 8);
        thumblayout.setVgap(10);
        gridPanel.setLayout(thumblayout);

        gridScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        gridScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // gridScrollPane.setViewportBorder(new LineBorder(Color.RED));
        gridScrollPane.getViewport().add(gridPanel, null);
        gridScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        container.add(gridScrollPane, BorderLayout.SOUTH);

        loadButton = new JButton("Load file...");
        final CTScanView thisFrame = this;
        loadButton.addActionListener(new LoadButtonActionListener(thisFrame));

        // Then the invert button
        mipButton = new JButton("MIP");

        //Zslice slider
        //@see https://docs.oracle.com/javase/7/docs/api/javax/swing/JSlider.html
        zSliceSlider = new JSlider(0, 112);
        zSliceSlider.setOrientation(JSlider.VERTICAL);
        zSliceSlider.setToolTipText("zAxis");
        zSliceSlider.setMajorTickSpacing(56);
        zSliceSlider.setMinorTickSpacing(6);
        zSliceSlider.setPaintTicks(true);
        zSliceSlider.setPaintLabels(true);

        //Yslice slider
        ySliceSlider = new JSlider(0, 255);
        ySliceSlider.setOrientation(JSlider.VERTICAL);
        //Add labels (y slider as example)
        ySliceSlider.setMajorTickSpacing(50);
        ySliceSlider.setMinorTickSpacing(10);
        ySliceSlider.setPaintTicks(true);
        ySliceSlider.setPaintLabels(true);

        //Xslice slider
        xSliceSlider = new JSlider(0, 255);
        xSliceSlider.setOrientation(JSlider.VERTICAL);
        //Add labels (x slider as example)
        xSliceSlider.setMajorTickSpacing(50);
        xSliceSlider.setMinorTickSpacing(10);
        xSliceSlider.setPaintTicks(true);
        xSliceSlider.setPaintLabels(true);

        // Then the ct scan images for dimensions ZYX (as a label icons)
        // Z dimension
        imageIcon1 = new JLabel(new ImageIcon(zImage1));
        imageIcon1.addMouseListener(new ImageIconMouseListener(this, imageIcon1));

        // Y dimension
        BufferedImage scaledImage2 = scale(256, 256, yImage2);
        imageIcon2 = new JLabel(new ImageIcon(scaledImage2));
        imageIcon2.addMouseListener(new ImageIconMouseListener(this, imageIcon2));

        // X dimension
        BufferedImage scaledImage3 = scale(256, 256, xImage3);
        imageIcon3 = new JLabel(new ImageIcon(scaledImage3));
        imageIcon3.addMouseListener(new ImageIconMouseListener(this, imageIcon3));

        // add elements to ui, order matters in flow layout
        titlePanel.add(loadButton);
        titlePanel.add(mipButton);

        imagesPanel.add(imageIcon1);
        imagesPanel.add(zSliceSlider);
        imagesPanel.add(imageIcon2);
        imagesPanel.add(ySliceSlider);
        imagesPanel.add(imageIcon3);
        imagesPanel.add(xSliceSlider);

        // Now all the handlers class
        GUIEventHandler handler = new GUIEventHandler(this);

        // associate appropriate handlers
        mipButton.addActionListener(handler);
        ySliceSlider.addChangeListener(handler);
        zSliceSlider.addChangeListener(handler);
        xSliceSlider.addChangeListener(handler);

        // deactivate control on start (this ui elements will be activated when file is loaded.)
        zSliceSlider.setEnabled(false);
        ySliceSlider.setEnabled(false);
        xSliceSlider.setEnabled(false);
        mipButton.setEnabled(false);
        loadButton.setEnabled(false); // activated when ui becomes visible.

        // ... and display everything
        pack();

        System.out.println(titlePanel.getWidth());
        System.out.println(imagesPanel.getWidth());
        System.out.println(this.getWidth());

        setLocationRelativeTo(null);
        ComponentListener componentListener = new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                super.componentShown(e);
                loadButton.setEnabled(true); // activate load button when ui is shown.
            }
        };
        addComponentListener(componentListener);
        setVisible(true);

    }

    public void enableControls() {
        zSliceSlider.setEnabled(true);  // activate slider only when file is loaded
        ySliceSlider.setEnabled(true);  // activate slider only when file is loaded
        xSliceSlider.setEnabled(true);  // activate slider only when file is loaded
        mipButton.setEnabled(true);     // activate button only when file is loaded
    }

    /*
     ///////////////////////
     //  Private Methods  //
     ///////////////////////
     */

    public void loadData(DataInputStream in) throws IOException {

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE; //set to extreme values
        short read; //value read in
        int b1, b2; //data is wrong Endian (check wikipedia) for Java so we need to swap the bytes around

        cthead = new short[113][256][256]; //allocate the memory - note this is fixed for this data set
        //loop through the data reading it in
        for (int k = 0; k < 113; k++) {
            for (int j = 0; j < 256; j++) {
                for (int i = 0; i < 256; i++) {
                    //because the Endianess is wrong, it needs to be read byte at a time and swapped
                    b1 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier! <--- That is not true!)
                    b2 = ((int) in.readByte()) & 0xff; //the 0xff is because Java does not have unsigned types (C++ is so much easier! <--- That is not true!)
                    read = (short) ((b2 << 8) | b1); //and swizzle the bytes around
                    if (read < min) min = read; //update the minimum
                    if (read > max) max = read; //update the maximum
                    cthead[k][j][i] = read; //put the short into memory (in C++ you can replace all this code with one fread. <--- who cares?!)
                }
            }
        }
        System.out.println(min + " " + max); //diagnostic - for CThead this should be -1117, 2248
        //(i.e. there are 3366 levels of grey (we are trying to display on 256 levels of grey)
        //therefore histogram equalization would be a good thing
    }

    public void updateImage(@NonNull ImageUpdateDirection updateDirection) {
        if( updateDirection.ordinal() == ImageUpdateDirection.Z_AXIS.ordinal() ) {
            SwingUtilities.invokeLater( () -> {
                zImage1 = updateAxis(zImage1, zSliceSlider.getValue(), ImageUpdateDirection.Z_AXIS);
                imageIcon1.setIcon(new ImageIcon(zImage1));
            } );
        }
        else if ( updateDirection.ordinal() == ImageUpdateDirection.Y_AXIS.ordinal() ) {
            SwingUtilities.invokeLater( () -> {
                yImage2 = updateAxis(yImage2, ySliceSlider.getValue(), ImageUpdateDirection.Y_AXIS);
                BufferedImage scaledImage2 = scale(256, 256, yImage2);
                imageIcon2.setIcon(new ImageIcon(scaledImage2));
            } );
        }
        else if ( updateDirection.ordinal() == ImageUpdateDirection.X_AXIS.ordinal() ) {
            SwingUtilities.invokeLater( () -> {
                xImage3 = updateAxis(xImage3, xSliceSlider.getValue(), ImageUpdateDirection.X_AXIS);
                BufferedImage scaledImage3 = scale(256, 256, xImage3);
                imageIcon3.setIcon(new ImageIcon(scaledImage3));
            } );
        }
    }

    /*
        This function will return a pointer to an array
        of bytes which represent the image data in memory.
        Using such a pointer allows fast access to the image
        data for processing (rather than getting/setting
        individual pixels)
    */
    private static byte[] getImageData(BufferedImage image) {
        WritableRaster imageRaster = image.getRaster();
        DataBuffer dataBuffer = imageRaster.getDataBuffer();
        if (dataBuffer.getDataType() != DataBuffer.TYPE_BYTE) {
            throw new IllegalStateException("That's not of type byte");
        }
        return ((DataBufferByte) dataBuffer).getData();
    }

    /*
        This function shows how to carry out an operation on an image.
        It obtains the dimensions of the image, and then loops through
        the image carrying out the copying of a slice of data into the
		image.
    */
    public BufferedImage mip(BufferedImage image, int k) {
        //Get image dimensions, and declare loop variables
        int w = image.getWidth(), h = image.getHeight();
        //Obtain pointer to data for fast processing
        byte[] data = getImageData(image);
        float col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use j for loops in y, and i for loops in x
        //as this makes the code more readable
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                //at this point (i,j) is a single pixel in the image
                //here you would need to do something to (i,j) if the image size
                //does not match the slice size (e.g. during an image resizing operation
                //If you don't do this, your j,i could be outside the array bounds
                //In the framework, the image is 256x256 and the data set slices are 256x256
                //so I don't do anything - this also leaves you something to do for the assignment
                //datum=cthead[76][j][i]; //get values from slice 76 (change this in your assignment)
                datum = cthead[k][j][i]; //get values from slice from value of param k (change this in your assignment)
                //calculate the colour by performing a mapping from [min,max] -> [0,255]
                col = (255.0f * ((float) datum - (float) min) / ((float) (max - min)));
                for (int c = 0; c < 3; c++) {
                    //and now we are looping through the bgr components of the pixel
                    //set the colour component c of pixel (i,j)
                    data[c + 3 * i + 3 * j * w] = (byte) col;
                } // colour loop
            } // column loop
        } // row loop

        return image;
    }

    public BufferedImage updateAxis(BufferedImage image, int sliderValue, ImageUpdateDirection axisDirection) {
        final int width = image.getWidth(), height = image.getHeight();
        final byte[] data = getImageData(image);
        float color;
        short pixel;
        switch (axisDirection) {
            case Z_AXIS: {
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        pixel = cthead[sliderValue][j][i];
                        color = (255.0f * ((float) pixel - (float) min) / ((float) (max - min)));
                        for (int c = 0; c < 3; c++) {
                            data[c + 3 * i + 3 * j * width] = (byte) color;
                        }
                    }
                }
                return image;
            }
            case Y_AXIS: {
                for (int k = 0; k < height; k++) {
                    for (int i = 0; i < width; i++) {
                        pixel = cthead[k][sliderValue][i];
                        color = (255.0f * ((float) pixel - (float) min) / ((float) (max - min)));
                        for (int c = 0; c < 3; c++) {
                            data[c + 3 * i + 3 * k * width] = (byte) color;
                        }
                    }
                }
                return image;
            }
            case X_AXIS: {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < width; k++) {
                        pixel = cthead[k][j][sliderValue];
                        color = (255.0f * ((float) pixel - (float) min) / ((float) (max - min)));
                        for (int c = 0; c < 3; c++) {
                            data[c + 3 * k + 3 * j * width] = (byte) color;
                        }
                    }
                }
            }
            return image;
        }
        return image;
    }

    public BufferedImage scale(int scaledWith, int scaledHeight, BufferedImage image) {

        float width = image.getWidth(), height = image.getHeight();
        float xRatio = scaledWith / width, yRatio = scaledHeight / height;

        return scaleRatio(xRatio, yRatio, image);

    }

    private int getByte(int value, int n) {
        return (value >> (n * 8)) & 0xFF;
    }

    private float lerp(float s, float e, float t) {
        return s + (e - s) * t;
    }

    private float blerp(float c00, float c10, float c01, float c11, float tx, float ty) {
        return lerp(lerp(c00, c10, tx), lerp(c01, c11, tx), ty);
    }

    public BufferedImage scaleRatio(float scaleX, float scaleY, BufferedImage image) {

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int type = image.getType();

        int newWidth = (int) (width * scaleX);
        int newHeight = (int) (height * scaleY);
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, type);

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                float gx = (float) (x) / newWidth * (width - 1);
                float gy = (float) (y) / newHeight * (height - 1);
                int gxi = (int) (gx);
                int gyi = (int) (gy);
                int rgb = 0;
                int c00 = image.getRGB(gxi, gyi);
                int c10 = image.getRGB(gxi + 1, gyi);
                int c01 = image.getRGB(gxi, gyi + 1);
                int c11 = image.getRGB(gxi + 1, gyi + 1);
                for (int i = 0; i < 3; i++) {
                    float b00 = (float) getByte(c00, i);
                    float b10 = (float) getByte(c10, i);
                    float b01 = (float) getByte(c01, i);
                    float b11 = (float) getByte(c11, i);
                    int ble = (int) (blerp(b00, b10, b01, b11, gx - gxi, gy - gyi)) << (8 * i);
                    rgb |= ble;
                }
                newImage.setRGB(x, y, rgb);
            }
        }
        return newImage;
    }

    /*
     //////////////
     //  Getter  //
     //////////////
     */

    public JFileChooser getFileChooser() {
        return fileChooser;
    }

    public JPanel getGridPanel() {
        return gridPanel;
    }

    public JScrollPane getGridScrollPane() {
        return gridScrollPane;
    }

    public BufferedImage getzImage1() {
        return zImage1;
    }

    public BufferedImage getyImage2() {
        return yImage2;
    }

    public BufferedImage getxImage3() {
        return xImage3;
    }

    public JLabel getImageIcon1() {
        return imageIcon1;
    }


    public JLabel getImageIcon2() {
        return imageIcon2;
    }

    public JLabel getImageIcon3() {
        return imageIcon3;
    }

    public JButton getMipButton() {
        return mipButton;
    }

    public JSlider getzSliceSlider() {
        return zSliceSlider;
    }

    public JSlider getySliceSlider() {
        return ySliceSlider;
    }

    public JSlider getxSliceSlider() {
        return xSliceSlider;
    }

    /*
     //////////////
     //  Setter  //
     //////////////
     */

    public void setzImage1(BufferedImage zImage1) {
        this.zImage1 = zImage1;
    }

    public void setyImage2(BufferedImage yImage2) {
        this.yImage2 = yImage2;
    }

    public void setxImage3(BufferedImage xImage3) {
        this.xImage3 = xImage3;
    }

    /*
     ///////////////////
     //  Main Method  //
     ///////////////////
     */

    public static void main(String[] args) {

        // init ui in a swing ui thread. THIS IS recommended because it much more stable on most OS.
        // Cause a smoother presentation of the frame.
        SwingUtilities.invokeLater( () -> {
            JFrame frame = new CTScanView("CT Scan View");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        } );

    }
}
