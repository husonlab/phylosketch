/*
 * ChangeEdgeStyleCommand.java Copyright (C) 2022 Daniel H. Huson
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
import jloda.graph.Edge;
import phylosketch.view.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * undoable
 * Daniel Huson, 2.2020
 */
public class ChangeEdgeStyleCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;
    private final ArrayList<Data> dataList = new ArrayList<>();

    public ChangeEdgeStyleCommand(PhyloView editor, Collection<Edge> edges, boolean value) {
        super("Edge Style");

        for (Edge e : edges) {
            dataList.add(new Data(e.getId(), editor.getEdgeView(e).getArrowHead().isVisible(), value));
        }

        undo = () -> {
            for (Data data : dataList) {
                editor.getEdgeView(editor.getGraph().findEdgeById(data.id)).getArrowHead().setVisible(data.oldValue);
            }
        };

        redo = () -> {
            for (Data data : dataList) {
                editor.getEdgeView(editor.getGraph().findEdgeById(data.id)).getArrowHead().setVisible(data.newValue);
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
        final boolean oldValue;
        final boolean newValue;

        public Data(int id, boolean oldValue, boolean newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
