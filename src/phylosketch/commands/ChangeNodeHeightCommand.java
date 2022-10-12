/*
 * ChangeNodeHeightCommand.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.fx.shapes.ISized;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phylosketch.view.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * undoable
 * Daniel Huson, 2.2020
 */
public class ChangeNodeHeightCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;
    private final ArrayList<Data> dataList = new ArrayList<>();

    public ChangeNodeHeightCommand(PhyloView editor, Collection<Node> nodes, double height) {
        super("Height");

        for (Node v : nodes) {
            dataList.add(new Data(v.getId(), ((ISized) editor.getNodeView(v).getShape()).getHeight(), height));
        }

        undo = () -> {
            for (Data data : dataList) {
                ISized sized = (ISized) editor.getNodeView(editor.getGraph().findNodeById(data.id)).getShape();
                sized.setSize(sized.getWidth(), data.oldValue);
            }
        };

        redo = () -> {
            for (Data data : dataList) {
                ISized sized = (ISized) editor.getNodeView(editor.getGraph().findNodeById(data.id)).getShape();
                sized.setSize(sized.getWidth(), data.newValue);
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
        final double oldValue;
        final double newValue;

        public Data(int id, double oldValue, double newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
