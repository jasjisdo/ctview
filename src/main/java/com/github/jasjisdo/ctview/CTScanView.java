package com.github.jasjisdo.ctview;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
     /////////////////////
     //  Inner Classes  //
     /////////////////////
     */

    /*
        This is the event handler for the application
    */
    private class GUIEventHandler implements ActionListener, ChangeListener {

        //Change handler (e.g. for sliders)
        public void stateChanged(ChangeEvent e) {
//            System.out.println(yslice_slider.getValue());
//            //e.g. do something to change the image here
            if (e.getSource() == zSliceSlider) {
                // update slider value
                updateZSliderValue();
            }
            if (e.getSource() == ySliceSlider) {
                // update slider value
                updateYSliderValue();
            }
            if (e.getSource() == xSliceSlider) {
                // update slider value
                updateXSliderValue();
            }
        }

        //action handlers (e.g. for buttons)
        public void actionPerformed(ActionEvent event) {
            if (event.getSource() == MIPButton) {
                //e.g. do something to change the image here
                //e.g. call MIP function
                int zValue = zSliceSlider.getValue();
                zImage1 = mip(zImage1, zValue); //(although mine is called MIP, it doesn't do MIP)

                // Update image
                imageIcon1.setIcon(new ImageIcon(zImage1));
            }
        }
    }
    /*
        This is a frame to show an image in detail and with zoom
     */
    private class DetailImageFrame extends JFrame {

        private JFrame parentFrame;
        private BufferedImage image;
        private final JLabel imageLabel;

        DetailImageFrame(final JFrame parentFrame, final BufferedImage image) throws HeadlessException {
            super("Detail Image View");

            final JFrame thisFrame = this;

            this.parentFrame = parentFrame;
            this.image = image;

            Container container = this.getContentPane();
            BorderLayout borderLayout = new BorderLayout();
            container.setLayout(borderLayout);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            JButton plus = new JButton("+");
            plus.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final ImageIcon imageIcon = (ImageIcon) imageLabel.getIcon();
                    final BufferedImage image = (BufferedImage) imageIcon.getImage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final BufferedImage scaledImage = scaleRatio(1.2f, 1.2f, image);
                            imageLabel.setIcon(new ImageIcon(scaledImage));
                            imageLabel.validate();
                        }
                    });
                }
            });
            JButton minus = new JButton("-");
            minus.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final ImageIcon imageIcon = (ImageIcon) imageLabel.getIcon();
                    final BufferedImage image = (BufferedImage) imageIcon.getImage();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            final BufferedImage scaledImage = scaleRatio(0.8f, 0.8f, image);
                            imageLabel.setIcon(new ImageIcon(scaledImage));
                            imageLabel.validate();
                        }
                    });
                }
            });
            buttonPanel.add(plus);
            buttonPanel.add(minus);
            container.add(buttonPanel, BorderLayout.NORTH);

            imageLabel = new JLabel(new ImageIcon(image));
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getViewport().add(imageLabel, null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            container.add(scrollPane, BorderLayout.CENTER);

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    super.windowOpened(e);
                    parentFrame.setEnabled(false);
                }
                @Override
                public void windowClosed(WindowEvent e) {
                    super.windowClosed(e);
                    parentFrame.setEnabled(true);
                    parentFrame.setVisible(true);
                }
            });
        }
    }

    /*
     //////////////////////////////
     //  Class Fields / Members  //
     //////////////////////////////
     */

    private JFileChooser fileChooser = new JFileChooser();

    private FileFilter filter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().endsWith(".dms");
        }

        @Override
        public String getDescription() {
            return "DMS file";
        }
    };

    private ComponentListener componentListener = new ComponentAdapter() {
        @Override
        public void componentShown(ComponentEvent e) {
            super.componentShown(e);
            loadButton.setEnabled(true); // activate load button when ui is shown.
        }
    };

    private JPanel titlePanel = new JPanel();
    private JPanel imagesPanel = new JPanel();
    private JPanel gridPanel = new JPanel();
    private JScrollPane gridScrollPane = new JScrollPane();
    private JButton loadButton; // an button to load the CThead.dms file
    private JButton MIPButton; //an example button to switch to MIP mode
    private JLabel imageIcon1; //using JLabel to display an image (check online documentation)
    private JLabel imageIcon2; //using JLabel to display an image (check online documentation)
    private JLabel imageIcon3; //using JLabel to display an image (check online documentation)
    private JSlider zSliceSlider, ySliceSlider, xSliceSlider; //sliders to step through the slices (z and y directions) (remember 113 slices in z direction 0-112)
    private BufferedImage zImage1, yImage2, xImage3; //storing the image in memory
    private short cthead[][][]; //store the 3D volume data set
    private short min, max; //min/max value in the 3D volume data set

    /*
     ///////////////////
     //  Constructor  //
     ///////////////////
     */

    private CTScanView(String title) throws HeadlessException {
        super(title);

        // add and set file filter to file chooser.
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

        titlePanel.setLayout(new FlowLayout(FlowLayout.LEADING));
        container.add(titlePanel, BorderLayout.NORTH);

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
        final JFrame thisFrame = this;
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // open file chooser dialog
                fileChooser.showOpenDialog(thisFrame);
                System.out.println(fileChooser.getSelectedFile());
                try {
                    DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileChooser.getSelectedFile())));

                    loadData(in);

                    zSliceSlider.setEnabled(true);  // activate slider only when file is loaded
                    ySliceSlider.setEnabled(true);  // activate slider only when file is loaded
                    xSliceSlider.setEnabled(true);  // activate slider only when file is loaded
                    MIPButton.setEnabled(true);     // activate button only when file is loaded

                    for (int imgNr = 0; imgNr < 113; imgNr++) {
                        final int num = imgNr;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                BufferedImage image = updateZAxis(zImage1, num);
                                BufferedImage scaledImage = scale(128, 128, image);
                                JLabel label = new JLabel(new ImageIcon(scaledImage));
                                gridPanel.add(label);
                            }
                        });
                    }

                    for (int imgNr = 0; imgNr < 256; imgNr++) {
                        final int num = imgNr;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                BufferedImage image = updateYAxis(yImage2, num);
                                BufferedImage scaledImage = scale(128, 128, image);
                                JLabel label = new JLabel(new ImageIcon(scaledImage));
                                gridPanel.add(label);
                            }
                        });
                    }

                    for (int imgNr = 0; imgNr < 256; imgNr++) {
                        final int num = imgNr;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                BufferedImage image = updateXAxis(xImage3, num);
                                BufferedImage scaledImage = scale(128, 128, image);
                                JLabel label = new JLabel(new ImageIcon(scaledImage));
                                gridPanel.add(label);
                            }
                        });
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            gridScrollPane.setPreferredSize(new Dimension(1089, 276));
                            gridPanel.validate();
                            gridPanel.getParent().validate();
                            thisFrame.pack();
                            thisFrame.setLocationRelativeTo(null);
                        }
                    });

                    updateXSliderValue();
                    updateYSliderValue();
                    updateZSliderValue();

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });

        //Zslice slider
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
        //see
        //https://docs.oracle.com/javase/7/docs/api/javax/swing/JSlider.html
        //for documentation (e.g. how to get the value, how to display vertically if you want)

        // Then our image (as a label icon)
        imageIcon1 = new JLabel(new ImageIcon(zImage1));
        imageIcon1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                BufferedImage image = (BufferedImage)((ImageIcon)imageIcon1.getIcon()).getImage();
                image = scale(512, 512, image);

                JFrame frame = new DetailImageFrame(thisFrame, image);
                frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            }
        });
        BufferedImage scaledImage2 = scale(256, 256, yImage2);
        imageIcon2 = new JLabel(new ImageIcon(scaledImage2));
        imageIcon2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                BufferedImage image = (BufferedImage)((ImageIcon)imageIcon2.getIcon()).getImage();
                image = scale(512, 512, image);

                JFrame frame = new DetailImageFrame(thisFrame, image);
                frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            }
        });
        BufferedImage scaledImage3 = scale(256, 256, xImage3);
        imageIcon3 = new JLabel(new ImageIcon(scaledImage3));
        imageIcon3.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                BufferedImage image = (BufferedImage)((ImageIcon)imageIcon3.getIcon()).getImage();
                image = scale(512, 512, image);

                JFrame frame = new DetailImageFrame(thisFrame, image);
                frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            }
        });

        // Then the invert button
        MIPButton = new JButton("MIP");

        // add elements to ui, order matters in flow layout
        titlePanel.add(loadButton);

        imagesPanel.add(imageIcon1);
        imagesPanel.add(zSliceSlider);
        imagesPanel.add(imageIcon2);
        imagesPanel.add(ySliceSlider);
        imagesPanel.add(imageIcon3);
        imagesPanel.add(xSliceSlider);
        imagesPanel.add(MIPButton);

        // Now all the handlers class
        CTScanView.GUIEventHandler handler = new CTScanView.GUIEventHandler();

        // associate appropriate handlers
        MIPButton.addActionListener(handler);
        ySliceSlider.addChangeListener(handler);
        zSliceSlider.addChangeListener(handler);
        xSliceSlider.addChangeListener(handler);

        // deactivate control on start (this ui elements will be activated when file is loaded.)
        zSliceSlider.setEnabled(false);
        ySliceSlider.setEnabled(false);
        xSliceSlider.setEnabled(false);
        MIPButton.setEnabled(false);
        loadButton.setEnabled(false); // activated when ui becomes visible.

        // ... and display everything
        pack();

        System.out.println(titlePanel.getWidth());
        System.out.println(imagesPanel.getWidth());
        System.out.println(this.getWidth());

        setLocationRelativeTo(null);
        addComponentListener(componentListener);
        setVisible(true);

    }

    /*
     ///////////////////////
     //  Private Methods  //
     ///////////////////////
     */

    private void loadData(DataInputStream in) throws IOException {

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

    private void updateZSliderValue() {
        final int zValue = zSliceSlider.getValue();

        // Update image
        // update ui in a swing ui thread. THIS IS recommended because it much more stable on most OS.
        // Cause a smoother update of the image label.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                zImage1 = updateZAxis(zImage1, zValue);
                imageIcon1.setIcon(new ImageIcon(zImage1));
            }
        });
    }

    private void updateYSliderValue() {
        final int yValue = ySliceSlider.getValue();

        // Update image
        // update ui in a swing ui thread. THIS IS recommended because it much more stable on most OS.
        // Cause a smoother update of the image label.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                yImage2 = updateYAxis(yImage2, yValue);
                // Image scaledImage2 = image2.getScaledInstance(256, 256, Image.SCALE_SMOOTH);
                BufferedImage scaledImage2 = scale(256, 256, yImage2);
                imageIcon2.setIcon(new ImageIcon(scaledImage2));
            }
        });
    }

    private void updateXSliderValue() {
        final int xValue = xSliceSlider.getValue();

        // Update image
        // update ui in a swing ui thread. THIS IS recommended because it much more stable on most OS.
        // Cause a smoother update of the image label.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                xImage3 = updateXAxis(xImage3, xValue);
                // Image scaledImage3 = image3.getScaledInstance(256, 256, Image.SCALE_SMOOTH);
                BufferedImage scaledImage3 = scale(256, 256, xImage3);
                imageIcon3.setIcon(new ImageIcon(scaledImage3));
            }
        });
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
    private BufferedImage mip(BufferedImage image, int k) {
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

    private BufferedImage updateZAxis(BufferedImage image, int k) {
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

    private BufferedImage updateYAxis(BufferedImage image, int j) {
        //Get image dimensions, and declare loop variables
        int w = image.getWidth(), h = image.getHeight();
        //Obtain pointer to data for fast processing
        byte[] data = getImageData(image);
        float col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use k for loops in z, and i for loops in x
        //as this makes the code more readable
        for (int k = 0; k < h; k++) {
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
                    data[c + 3 * i + 3 * k * w] = (byte) col;
                } // colour loop
            } // column loop
        } // row loop

        return image;
    }

    private BufferedImage updateXAxis(BufferedImage image, int i) {
        //Get image dimensions, and declare loop variables
        int w = image.getWidth(), h = image.getHeight();
        //Obtain pointer to data for fast processing
        byte[] data = getImageData(image);
        float col;
        short datum;
        //Shows how to loop through each pixel and colour
        //Try to always use k for loops in z, and j for loops in y
        //as this makes the code more readable
        for (int j = 0; j < h; j++) {
            for (int k = 0; k < w; k++) {
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
                    data[c + 3 * k + 3 * j * w] = (byte) col;
                } // colour loop
            } // column loop
        } // row loop

        return image;
    }

    private BufferedImage scale(int scaledWith, int scaledHeight, BufferedImage image) {

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

    private BufferedImage scaleRatio(float scaleX, float scaleY, BufferedImage image) {

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
     ///////////////////
     //  Main Method  //
     ///////////////////
     */

    public static void main(String[] args) {

        // init ui in a swing ui thread. THIS IS recommended because it much more stable on most OS.
        // Cause a smoother presentation of the frame.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new CTScanView("CT Scan View");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });

    }
}
