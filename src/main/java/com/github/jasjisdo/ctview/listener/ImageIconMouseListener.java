package com.github.jasjisdo.ctview.listener;

import com.github.jasjisdo.ctview.CTScanView;
import com.github.jasjisdo.ctview.components.DetailImageFrame;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

/**
 * image icon mouse listener which opens the detail view of the presented image on click.
 */
public class ImageIconMouseListener extends MouseAdapter {

    private final CTScanView parentFrame;
    private final JLabel imageIcon;

    public ImageIconMouseListener(CTScanView parentFrame, JLabel imageIcon) {
        this.parentFrame = parentFrame;
        this.imageIcon = imageIcon;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        BufferedImage image = (BufferedImage) ((ImageIcon) imageIcon.getIcon()).getImage();
        open(prepareDetailFrame(parentFrame.scale(512, 512, image)));
    }

    /* prepares a detail image frame which gets disposed after it is closed again */
    private DetailImageFrame prepareDetailFrame(BufferedImage image) {
        DetailImageFrame frame = new DetailImageFrame(parentFrame, image);
        frame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        frame.pack();
        return frame;
    }

    /* opens a detail image frame which show the given image */
    private void open(DetailImageFrame frame) {
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
