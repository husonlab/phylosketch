/*
 * ChangeLabelCommand.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.commands;

import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phylosketch.view.PhyloView;

/**
 * change a node label
 * Daniel Huson, 2.2020
 */
public class ChangeLabelCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public ChangeLabelCommand(PhyloView view, Node v, String newText) {
        super("Change Label");

        final String oldText = view.getLabel(v).getText();
        final int id = v.getId();

        undo = () -> view.getLabel(view.getGraph().findNodeById(id)).setText(oldText);

		redo = () -> view.getLabel(view.getGraph().findNodeById(id)).setText(newText);
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
