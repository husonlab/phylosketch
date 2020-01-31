/*
 *  NodeLabelDialog.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.actions;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phyloedit.window.PhyloEditor;

import java.util.Optional;


/**
 * show the node label dialog
 * Daniel Huson, 1.2020
 */
public class NodeLabelDialog extends ContextMenu {

    public static boolean apply(Stage owner, PhyloEditor editor, Node v) {
        final int id = v.getId();
        final String oldLabel = editor.getLabel(v).getText();

        // ask for label
        TextInputDialog dialog = new TextInputDialog(oldLabel);
        dialog.initOwner(owner);
        dialog.setTitle("Node Label Input");
        dialog.setHeaderText("Set node label");
        dialog.setContentText("Please enter node label:");

        Optional<String> result = dialog.showAndWait();
        final String newLabel;
        if (result.isPresent()) {
            newLabel = result.get();
            editor.getUndoManager().doAndAdd(new UndoableRedoableCommand("Label") {
                @Override
                public void undo() {
                    editor.getLabel(editor.getGraph().searchNodeId(id)).setText(oldLabel);
                }

                @Override
                public void redo() {
                    editor.getLabel(editor.getGraph().searchNodeId(id)).setText(newLabel);
                }
            });
            return true;
        }
        return false; // canceled
    }
}
