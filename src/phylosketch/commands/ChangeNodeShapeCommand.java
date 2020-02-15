/*
 * ChangeNodeShapeCommand.java Copyright (C) 2020. Daniel H. Huson
 *
 * (Some code written by other authors, as named in code.)
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

import jloda.fx.shapes.NodeShape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phylosketch.window.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * change font
 * Daniel Huson, 2.2020
 */
public class ChangeNodeShapeCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;
    private final ArrayList<Data> dataList = new ArrayList<>();

    public ChangeNodeShapeCommand(PhyloView editor, Collection<Node> nodes, NodeShape nodeShape) {
        super("Shape");

        for (Node v : nodes) {
            dataList.add(new Data(v.getId(), NodeShape.valueOf(editor.getNodeView(v).getShape()), nodeShape));
        }

        undo = () -> {
            for (Data data : dataList) {
                editor.changeNodeShape(editor.getGraph().searchNodeId(data.id), data.oldValue);

            }
        };

        redo = () -> {
            for (Data data : dataList) {
                editor.changeNodeShape(editor.getGraph().searchNodeId(data.id), data.newValue);
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
        final NodeShape oldValue;
        final NodeShape newValue;

        public Data(int id, NodeShape oldValue, NodeShape newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
