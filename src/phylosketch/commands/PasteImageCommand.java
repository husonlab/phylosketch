/*
 * PasteImageCommand.java Copyright (C) 2020 Daniel H. Huson
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
 *  PasteImageCommand.java Copyright (C) 2020 Daniel H. Huson
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

package phylosketch.commands;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import jloda.fx.undo.UndoableRedoableCommand;
import phylosketch.window.MainWindowController;

/**
 * paste a background image
 * Daniel Huson, 1.2020
 */
public class PasteImageCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public PasteImageCommand(Stage stage, MainWindowController controller, Image image) {
        super("Paste Image");

        final ImageView prevView;
        if (controller.getMainPane().getChildren().get(0) instanceof ImageView)
            prevView = (ImageView) controller.getMainPane().getChildren().get(0);
        else
            prevView = null;

        final ImageView newView = new ImageView(image);
        newView.setOpacity(0.5);
        newView.setPickOnBounds(true);
        newView.setPreserveRatio(true);
        newView.setMouseTransparent(true);


        undo = () -> {
            controller.getMainPane().getChildren().remove(newView);
            newView.fitWidthProperty().unbind();
            if (prevView != null) {
                controller.getMainPane().getChildren().add(0, prevView);
                prevView.fitWidthProperty().bind(controller.getToolBar().widthProperty().subtract(50));
            }
        };

        redo = () -> {
            if (prevView != null) {
                controller.getMainPane().getChildren().remove(prevView);
                prevView.fitWidthProperty().unbind();
            }
            controller.getMainPane().getChildren().add(0, newView);
            newView.fitWidthProperty().bind(controller.getToolBar().widthProperty().subtract(50));
            //newView.setFitWidth(100);
        };
    }

    @Override
    public void undo() {
        undo.run();
    }

    @Override
    public void redo() {
        redo.run();
    }
}
