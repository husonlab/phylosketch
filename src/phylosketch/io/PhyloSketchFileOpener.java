/*
 * PhyloSketchFileOpener.java Copyright (C) 2020. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package phylosketch.io;

import jloda.fx.util.RecentFilesManager;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.NotificationManager;
import jloda.util.Basic;
import phylosketch.util.NewWindow;
import phylosketch.window.MainWindow;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * opens a file
 * Daniel Huson, 1.2020
 */
public class PhyloSketchFileOpener implements Consumer<String> {

    @Override
    public void accept(String fileName) {
        MainWindow window = (MainWindow) MainWindowManager.getInstance().getLastFocusedMainWindow();
        if (window == null || !window.isEmpty())
            window = NewWindow.apply();

        String firstLine = Objects.requireNonNull(Basic.getFirstLineFromFile(new File(fileName))).trim().toLowerCase();
        try {
            if (firstLine.startsWith("#nexus"))
                PhyloSketchIO.open(window.getController().getContentPane(), window.getView(), new File(fileName));
            else if (firstLine.startsWith("<nex:nexml") || firstLine.startsWith("<?xml version="))
                PhyloSketchIO.importNeXML(window.getController().getContentPane(), window.getView(), new File(fileName));
            else
                PhyloSketchIO.importNewick(window.getController().getContentPane(), window.getView(), new File(fileName));

            window.getView().setFileName(fileName);
            RecentFilesManager.getInstance().insertRecentFile(fileName);

        } catch (IOException e) {
            NotificationManager.showError("Open file failed: " + e.getMessage());
        }

    }
}
