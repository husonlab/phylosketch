/*
 * ChangeNodeColorCommand.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.scene.paint.Color;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phylosketch.view.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * undoable
 * Daniel Huson, 2.2020
 */
public class ChangeNodeColorCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;
    private final ArrayList<Data> dataList = new ArrayList<>();

    public ChangeNodeColorCommand(PhyloView editor, Collection<Node> nodes, Color value) {
        super("Node Color");

        for (Node v : nodes) {
            dataList.add(new Data(v.getId(), (Color) editor.getNodeView(v).getShape().getFill(), value));
        }

        undo = () -> {
            for (Data data : dataList) {
                editor.getNodeView(editor.getGraph().findNodeById(data.id)).getShape().setFill(data.oldValue);
            }
        };

        redo = () -> {
            for (Data data : dataList) {
                editor.getNodeView(editor.getGraph().findNodeById(data.id)).getShape().setFill(data.newValue);
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

    static class Data {
        final int id;
        final Color oldValue;
        final Color newValue;

        public Data(int id, Color oldValue, Color newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
