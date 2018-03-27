package com.github.jasjisdo.ctview.listener;

import com.github.jasjisdo.ctview.CTScanView;
import com.github.jasjisdo.ctview.ImageUpdateDirection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * A load button action listener which handles to load 3D-image scans and view them in specific image labels.
 */
public class LoadButtonActionListener implements ActionListener {

    private final CTScanView parentFrame;

    public LoadButtonActionListener(CTScanView parentFrame) {
        this.parentFrame = parentFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // open file chooser dialog
        JFileChooser fileChooser = parentFrame.getFileChooser();
        fileChooser.showOpenDialog(parentFrame);
        System.out.println(fileChooser.getSelectedFile());

        try {
            readAndLoadFile(fileChooser.getSelectedFile());
            showAllPreviewImages();
            showZYXImages();
            resizeAndRefreshParentFrame(1089, 276);
            parentFrame.enableControls();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    /* read and load the given file in image data container */
    private void readAndLoadFile(File file) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        parentFrame.loadData(in);
    }

    /* show all previews from loaded image data */
    private void showAllPreviewImages() {
        showPreviewImages(113, parentFrame.getzImage1(), ImageUpdateDirection.Z_AXIS);
        showPreviewImages(256, parentFrame.getyImage2(), ImageUpdateDirection.Y_AXIS);
        showPreviewImages(256, parentFrame.getxImage3(), ImageUpdateDirection.X_AXIS);
    }

    /* show Images for zyx-direction in specific label image view. (left: Z, middle: Y, right: X)*/
    private void showZYXImages() {
        parentFrame.updateImage(ImageUpdateDirection.X_AXIS);
        parentFrame.updateImage(ImageUpdateDirection.Y_AXIS);
        parentFrame.updateImage(ImageUpdateDirection.Z_AXIS);
    }

    /* resize the parent frame to new added content like image previews */
    private void resizeAndRefreshParentFrame(int width, int height) {
        SwingUtilities.invokeLater( () -> {
            parentFrame.getGridScrollPane().setPreferredSize(new Dimension(width, height));
            parentFrame.getGridPanel().validate();
            parentFrame.getGridPanel().getParent().validate();
            parentFrame.pack();
            parentFrame.setLocationRelativeTo(null);
        } );
    }

    /* render, scale and show preview images */
    private void showPreviewImages(final int maxNumOfImg, final BufferedImage imageToShow,
                                   final ImageUpdateDirection updateDirection) {

        for (int imgNr = 0; imgNr < maxNumOfImg; imgNr++) {
            final int num = imgNr;
            SwingUtilities.invokeLater( () -> {
                BufferedImage image = parentFrame.updateAxis(imageToShow, num, updateDirection);
                BufferedImage scaledImage = parentFrame.scale(128, 128, image);
                JLabel label = new JLabel(new ImageIcon(scaledImage));
                parentFrame.getGridPanel().add(label);
            } );
        }

    }

}
