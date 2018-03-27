package com.github.jasjisdo.ctview.components;

import com.github.jasjisdo.ctview.CTScanView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

/**
 * This is a frame to show an image in detail and with zoom-in and -out buttons.
 */
public class DetailImageFrame extends JFrame {

    private final JLabel imageLabel;

    public DetailImageFrame(final CTScanView parentFrame, final BufferedImage image) throws HeadlessException {
        super("Detail Image View");

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
                SwingUtilities.invokeLater(() -> {
                    final BufferedImage scaledImage = parentFrame.scaleRatio(1.2f, 1.2f, image);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                    imageLabel.validate();
                });
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ImageIcon imageIcon = (ImageIcon) imageLabel.getIcon();
                final BufferedImage image = (BufferedImage) imageIcon.getImage();
                SwingUtilities.invokeLater(() -> {
                    final BufferedImage scaledImage = parentFrame.scaleRatio(0.8f, 0.8f, image);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                    imageLabel.validate();
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