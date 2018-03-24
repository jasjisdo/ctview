package com.github.jasjisdo.ctview.eventhandler;

import com.github.jasjisdo.ctview.CTScanView;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A class which handle ui events like button click action and slider change
 */
public class GUIEventHandler implements ActionListener, ChangeListener {

    private CTScanView ctScanView;

    /**
     * Create a new {@code GUIEventHandler} class.
     * @param ctScanView        that contains sliders for x,y,z dimension image update.
     */
    public GUIEventHandler(CTScanView ctScanView) {
        this.ctScanView = ctScanView;
    }

    public void stateChanged(ChangeEvent e) {
        if ( e.getSource() == ctScanView.getzSliceSlider() ) { ctScanView.updateZSliderValue(); }
        else if ( e.getSource() == ctScanView.getySliceSlider() ) { ctScanView.updateYSliderValue(); }
        else if ( e.getSource() == ctScanView.getxSliceSlider() ) { ctScanView.updateXSliderValue(); }
    }

    public void actionPerformed(ActionEvent event) {
        if ( event.getSource() == ctScanView.getMipButton() ) {
            int zValue = ctScanView.getzSliceSlider().getValue();
            ctScanView.setzImage1(ctScanView.mip(ctScanView.getzImage1(), zValue));
            ctScanView.getImageIcon1().setIcon(new ImageIcon(ctScanView.getzImage1()));
        }
    }

}
