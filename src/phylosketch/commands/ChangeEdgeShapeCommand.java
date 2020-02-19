/*
 * ChangeEdgeShapeCommand.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.commands;

import javafx.geometry.Point2D;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import phylosketch.window.PhyloView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ChangeEdgeShapeCommand extends UndoableRedoableCommand {
    public enum EdgeShape {Straight, DownRight, RightDown, Reshape}

    final private Runnable undo;
    final private Runnable redo;

    public ChangeEdgeShapeCommand(PhyloView view, Collection<Edge> edges, EdgeShape shape) {
        super("Edge Shape");

        final boolean isLeftToRight = shape == EdgeShape.Reshape && view.isLeftToRightLayout();

        final Map<Integer, double[]> id2oldCoordinates = new HashMap<>();
        final Map<Integer, double[]> id2newCoordinates = new HashMap<>();

        for (Edge e : edges) {
            final double[] oldCoordinates = view.getEdgeView(e).getControlCoordinates();

            final double[] newCoordinates;
            final Point2D start = new Point2D(view.getX(e.getSource()), view.getY(e.getSource()));
            final Point2D end = new Point2D(view.getX(e.getTarget()), view.getY(e.getTarget()));

            final EdgeShape shapeToUse;
            if (shape == EdgeShape.Reshape) {
                if (Math.abs(start.getX() - end.getX()) < 5 || Math.abs(start.getY() - end.getY()) < 5 || start.distance(end) < 25)
                    shapeToUse = EdgeShape.Straight;
                else if (isLeftToRight)
                    shapeToUse = EdgeShape.DownRight;
                else
                    shapeToUse = EdgeShape.RightDown;
            } else shapeToUse = shape;

            switch (shapeToUse) {
                default:
                case Straight: {
                    newCoordinates = new double[]{0.7 * start.getX() + 0.3 * end.getX(), 0.7 * start.getY() + 0.3 * end.getY(), 0.3 * start.getX() + 0.7 * end.getX(), 0.3 * start.getY() + 0.7 * end.getY()};
                    break;
                }
                case RightDown: {
                    newCoordinates = new double[]{end.getX(), start.getY(), end.getX(), start.getY()};
                    break;
                }
                case DownRight: {
                    newCoordinates = new double[]{start.getX(), end.getY(), start.getX(), end.getY()};
                    break;
                }
            }
            id2oldCoordinates.put(e.getId(), oldCoordinates);
            id2newCoordinates.put(e.getId(), newCoordinates);

        }

        undo = () -> {
            for (Integer id : id2oldCoordinates.keySet()) {
                view.getEdgeView(view.getGraph().searchEdgeId(id)).setControlCoordinates(id2oldCoordinates.get(id));
            }
        };

        redo = () -> {
            for (Integer id : id2newCoordinates.keySet()) {
                view.getEdgeView(view.getGraph().searchEdgeId(id)).setControlCoordinates(id2newCoordinates.get(id));
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
