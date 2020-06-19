/*
 * NodeLabelDialog.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.util;

import javafx.stage.Stage;
import jloda.fx.label.EditLabelDialog;
import jloda.graph.Node;
import phylosketch.commands.ChangeNodeLabelsCommand;
import phylosketch.window.PhyloView;

import java.util.Collections;
import java.util.Optional;


/**
 * show the node label dialog
 * Daniel Huson, 1.2020
 */
public class NodeLabelDialog {

    public static boolean apply(Stage owner, PhyloView editor, Node v) {
        final EditLabelDialog editLabelDialog = new EditLabelDialog(owner, editor.getLabel(v));
        final Optional<String> result = editLabelDialog.showAndWait();
        if (result.isPresent()) {
            final int id = v.getId();
            final String oldLabel = editor.getLabel(v).getText();
            final String newLabel = result.get();
            editor.getUndoManager().doAndAdd(new ChangeNodeLabelsCommand(editor, Collections.singletonList(new ChangeNodeLabelsCommand.Data(id, oldLabel, newLabel))));
            return true;
        } else
            return false;
    }
}
