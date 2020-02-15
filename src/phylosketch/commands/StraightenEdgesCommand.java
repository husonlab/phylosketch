/*
 * StraightenEdgesCommand.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.collections.ObservableList;
import javafx.scene.shape.CubicCurve;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import phylosketch.window.EdgeView;
import phylosketch.window.PhyloView;

import java.util.ArrayList;

/**
 * straighten selected edges
 * Daniel Huson, 2.2020
 */
public class StraightenEdgesCommand extends UndoableRedoableCommand {

    private final Runnable undo;
    private final Runnable redo;

    public StraightenEdgesCommand(PhyloView view, ObservableList<Edge> edges) {
        super("Straighten Edges");

        final ArrayList<Data> dataList = new ArrayList<>();

        for (Edge e : edges) {
            final EdgeView edgeView = view.getEdgeView(e);
            final CubicCurve curve = edgeView.getCurve();
            dataList.add(new Data(e.getId(), edgeView.getControlCoordinates(),
                    new double[]{0.7 * curve.getStartX() + 0.3 * curve.getEndX(), 0.7 * curve.getStartY() + 0.3 * curve.getEndY(),
                            0.3 * curve.getStartX() + 0.7 * curve.getEndX(), 0.3 * curve.getStartY() + 0.7 * curve.getEndY()}));
        }

        undo = () -> dataList.forEach(d -> view.getEdgeView(view.getGraph().searchEdgeId(d.id)).setControlCoordinates(d.oldValue));

        redo = () -> dataList.forEach(d -> view.getEdgeView(view.getGraph().searchEdgeId(d.id)).setControlCoordinates(d.newValue));

    }

    @Override
    public void undo() {
        undo.run();

    }

    @Override
    public void redo() {
        redo.run();
    }

    private static class Data {
        final int id;
        final double[] oldValue;
        final double[] newValue;

        public Data(int id, double[] oldValue, double[] newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
