/*
 * EdgeShapeCommand.java Copyright (C) 2022 Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.shape.CubicCurve;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.util.Pair;
import phylosketch.view.PhyloView;

/**
 * change edge shape command
 * Daniel Huson, 1.2020
 */
public class EdgeShapeCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public EdgeShapeCommand(PhyloView phyloView, Pair<Edge, Integer> edgeAndControlId, Point2D delta) {
        super("Edge Shape");
        final int id = edgeAndControlId.getFirst().getId();
        final int controlId = edgeAndControlId.getSecond();

        undo = () -> {
            final CubicCurve curve = phyloView.getCurve(phyloView.getGraph().findEdgeById(id));
            if (controlId == 1) {
                curve.setControlX1(curve.getControlX1() - delta.getX());
                curve.setControlY1(curve.getControlY1() - delta.getY());
            } else {
                curve.setControlX2(curve.getControlX2() - delta.getX());
                curve.setControlY2(curve.getControlY2() - delta.getY());
            }
        };
        redo = () -> {
            final CubicCurve curve = phyloView.getCurve(phyloView.getGraph().findEdgeById(id));
            if (controlId == 1) {
                curve.setControlX1(curve.getControlX1() + delta.getX());
                curve.setControlY1(curve.getControlY1() + delta.getY());
            } else {
                curve.setControlX2(curve.getControlX2() + delta.getX());
                curve.setControlY2(curve.getControlY2() + delta.getY());
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
