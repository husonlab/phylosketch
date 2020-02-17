/*
 * SetImageCommand.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.commands;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import jloda.fx.undo.UndoableRedoableCommand;

/**
 * set a background image
 * Daniel Huson, 1.2020
 */
public class SetImageCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public SetImageCommand(Stage stage, Pane contentPane, Image image) {
        super(image != null ? "Set Background Image" : "Delete Background Image");

        final ImageView oldImageView;
        if (contentPane.getChildren().get(0) instanceof ImageView)
            oldImageView = (ImageView) contentPane.getChildren().get(0);
        else
            oldImageView = null;

        final ImageView newImageView;
        if (image != null) {
            newImageView = new ImageView(image);
            newImageView.setOpacity(0.5);
            newImageView.setPickOnBounds(true);
            newImageView.setPreserveRatio(true);
            newImageView.setMouseTransparent(true);
        } else
            newImageView = null;

        undo = () -> {
            if (newImageView != null) {
                contentPane.getChildren().remove(newImageView);
                newImageView.fitWidthProperty().unbind();
                newImageView.fitHeightProperty().unbind();
            }
            if (oldImageView != null) {
                contentPane.getChildren().add(0, oldImageView);
                oldImageView.fitWidthProperty().bind(stage.widthProperty().subtract(100));
                oldImageView.fitHeightProperty().bind(stage.heightProperty().subtract(100));

            }
        };

        redo = () -> {
            if (oldImageView != null) {
                contentPane.getChildren().remove(oldImageView);
                oldImageView.fitWidthProperty().unbind();
                oldImageView.fitHeightProperty().unbind();
            }
            if (newImageView != null) {
                contentPane.getChildren().add(0, newImageView);
                newImageView.fitWidthProperty().bind(stage.widthProperty().subtract(100));
                newImageView.fitHeightProperty().bind(stage.heightProperty().subtract(100));
            }
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
