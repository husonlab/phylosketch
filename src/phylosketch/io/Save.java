/*
 * Save.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.stage.FileChooser;
import jloda.fx.util.RecentFilesManager;
import jloda.fx.util.TextFileFilter;
import jloda.util.ProgramProperties;
import phylosketch.window.MainWindow;

import java.io.File;
import java.io.IOException;

/**
 * save file
 * Daniel Huson, 1.2020
 */
public class Save {
    /**
     * save file
     *
     * @param file
     * @param window
     * @throws IOException
     */
    public static void apply(File file, MainWindow window) {
        PhyloSketchIO.save(file, window.getView());
    }

    /**
     * show save dialog
     *
     * @param window
     * @return true, if saved
     */
    public static boolean showSaveDialog(MainWindow window) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File - " + ProgramProperties.getProgramVersion());

        final File currentFile = new File(window.getView().getFileName());

        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Nexus", "*.nexus", "*.nex"),
                TextFileFilter.getInstance());


        if (!currentFile.isDirectory()) {
            fileChooser.setInitialDirectory(currentFile.getParentFile());
            fileChooser.setInitialFileName(currentFile.getName());
        } else {
            final File tmp = new File(ProgramProperties.get("SaveFileDir", ""));
            if (tmp.isDirectory()) {
                fileChooser.setInitialDirectory(tmp);
            }
        }

        final File selectedFile = fileChooser.showSaveDialog(window.getStage());

        if (selectedFile != null) {
            Save.apply(selectedFile, window);
            ProgramProperties.put("SaveFileDir", selectedFile.getParent());
            RecentFilesManager.getInstance().insertRecentFile(selectedFile.getPath());
            return true;
        } else
            return false;
    }
}
