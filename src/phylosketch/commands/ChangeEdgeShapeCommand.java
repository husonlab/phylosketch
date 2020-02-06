/*
 *  Copyright (C) 2018. Daniel H. Huson
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
 */

package phylosketch.commands;

import javafx.geometry.Point2D;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import phylosketch.window.PhyloView;

public class ChangeEdgeShapeCommand extends UndoableRedoableCommand {
    public enum EdgeShape {Straight, DownRight, RightDown}

    final private Runnable undo;
    final private Runnable redo;

    public ChangeEdgeShapeCommand(PhyloView view, Edge e, EdgeShape shape) {
        super("Edge Shape");

        final int id = e.getId();
        final double[] oldCoordinates = view.getEdgeView(e).getControlCoordinates();

        final double[] newCoordinates;
        final Point2D start = new Point2D(view.getX(e.getSource()), view.getY(e.getSource()));
        final Point2D end = new Point2D(view.getX(e.getTarget()), view.getY(e.getTarget()));

        switch (shape) {
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

        undo = () -> {
            view.getEdgeView(view.getGraph().searchEdgeId(id)).setControlCoordinates(oldCoordinates);
        };

        redo = () -> {
            view.getEdgeView(view.getGraph().searchEdgeId(id)).setControlCoordinates(newCoordinates);
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
