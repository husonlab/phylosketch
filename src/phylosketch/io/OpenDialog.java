/*
 * OpenDialog.java Copyright (C) 2020 Daniel H. Huson
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

/*
 *  OpenDialog.java Copyright (C) 2020 Daniel H. Huson
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
 */

package phylosketch.io;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.fx.util.TextFileFilter;
import jloda.util.ProgramProperties;

import java.io.File;

/**
 * open a file
 * Daniel Huson, 2.2020
 */
public class OpenDialog {
    /**
     * open a file
     *
     * @param owner
     */
    public static void apply(final Stage owner) {

        final File previousDir = new File(ProgramProperties.get("OpenDir", ""));
        final FileChooser fileChooser = new FileChooser();
        if (previousDir.isDirectory())
            fileChooser.setInitialDirectory(previousDir);
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PhyloSketch nexus", "*.nexus", "*.nex"),
                new FileChooser.ExtensionFilter("Extended Newick", "*.newick", "*.new", "*.tree", "*.tre"),
                TextFileFilter.getInstance());
        fileChooser.setTitle("Open File");

        final File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile != null) {
            new PhyloEditorFileOpener().accept(selectedFile.getPath());
            ProgramProperties.put("OpenDir", selectedFile.getPath());
        }
    }
}
