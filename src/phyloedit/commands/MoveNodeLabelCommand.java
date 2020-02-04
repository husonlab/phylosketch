/*
 * MoveNodeLabelCommand.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.commands;

import javafx.scene.control.Label;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phyloedit.window.PhyloView;

/**
 * move node label
 * Daniel Huson, 2.2020
 */
public class MoveNodeLabelCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;


    public MoveNodeLabelCommand(PhyloView editor, Node v, double dx, double dy) {
        super("Move Node Label");
        final int id = v.getId();

        undo = () -> {
            final Label label = editor.getNodeView(editor.getGraph().searchNodeId(id)).getLabel();
            label.setLayoutX(label.getLayoutX() - dx);
            label.setLayoutY(label.getLayoutY() - dy);
        };

        redo = () -> {
            final Label label = editor.getNodeView(editor.getGraph().searchNodeId(id)).getLabel();
            label.setLayoutX(label.getLayoutX() + dx);
            label.setLayoutY(label.getLayoutY() + dy);
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