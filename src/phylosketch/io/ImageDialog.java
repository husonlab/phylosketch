/*
 *  Copyright (C) 2018. Daniel H. Huson
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

/*
 *  Copyright (C) 2018. Daniel H. Huson
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

import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.fx.window.NotificationManager;
import jloda.util.ProgramProperties;

import java.io.File;

/**
 * ask for an image
 * Daniel Huson, 2.2020
 */
public class ImageDialog {
    public static Image apply(Stage owner) {
        final File previousFile = new File(ProgramProperties.get("ImageFile", ""));
        final FileChooser fileChooser = new FileChooser();
        if (previousFile.getParentFile() != null)
            fileChooser.setInitialDirectory(previousFile.getParentFile());
        fileChooser.setInitialFileName(previousFile.getName());
        fileChooser.setTitle("Open Image File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image", "*.png", "*.gif", "*.tiff", "*.jpg", "*.jpeg"));

        final File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile != null) {
            ProgramProperties.put("ImageFile", selectedFile);
            try {
                return new Image(selectedFile.toURI().toURL().toString());
            } catch (Exception ex) {
                NotificationManager.showError("Load background image failed: " + ex);
            }
        }
        return null;
    }
}
