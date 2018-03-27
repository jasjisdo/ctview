package com.github.jasjisdo.ctview.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

/**
 * A file filter used by file chooser to select *.dms 3D-images scans.
 */
public class DmsFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        return f.isDirectory() || f.getName().endsWith(".dms");
    }

    @Override
    public String getDescription() {
        return "DMS file";
    }

}
