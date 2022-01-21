/*
 * ChangeEdgeShapeCommand.java Copyright (C) 2022 Daniel H. Huson
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

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.util.CollectionUtils;
import jloda.util.ProgramProperties;
import phylosketch.window.PhyloView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * change the shape of edges
 * Daniel Huson, 2.2020
 */
public class ChangeEdgeShapeCommand extends UndoableRedoableCommand {
    public enum EdgeShape {Straight, DownRight, RightDown, Reshape}

    final private Runnable undo;
    final private Runnable redo;

    /**
     * constructor
     *
     * @param view
     * @param shape
     */
    public ChangeEdgeShapeCommand(PhyloView view, Collection<Edge> edges, EdgeShape shape) {
        super("Edge Shape");

        final boolean horizontal = (shape == EdgeShape.Reshape && view.computeRootLocation().isHorizontal());

        final Map<Integer, double[]> id2oldCoordinates = new HashMap<>();
        final Map<Integer, double[]> id2newCoordinates = new HashMap<>();

        edges.forEach(e -> {
            final double[] oldCoordinates = view.getEdgeView(e).getControlCoordinates();

            final double[] newCoordinates;
            final Point2D start = new Point2D(view.getX(e.getSource()), view.getY(e.getSource()));
            final Point2D end = new Point2D(view.getX(e.getTarget()), view.getY(e.getTarget()));

            final EdgeShape shapeToUse;
            if (shape == EdgeShape.Reshape) {
                if (Math.abs(start.getX() - end.getX()) < 5 || Math.abs(start.getY() - end.getY()) < 5 || start.distance(end) < 25)
                    shapeToUse = EdgeShape.Straight;
                else if (horizontal)
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
            synchronized (id2oldCoordinates) {
                id2oldCoordinates.put(e.getId(), oldCoordinates);
                id2newCoordinates.put(e.getId(), newCoordinates);
            }
        });

        undo = () -> animateChangeShape(view, id2newCoordinates, id2oldCoordinates);

        redo = () -> animateChangeShape(view, id2oldCoordinates, id2newCoordinates);
    }

    private void animateChangeShape(PhyloView view, Map<Integer, double[]> id2oldCoordinates, Map<Integer, double[]> id2newCoordinates) {
        if (id2oldCoordinates.size() < ProgramProperties.get("AnimationLimit", 5000)) {
            final Animation animation = new Transition() {
                {
                    setCycleDuration(Duration.millis(500));
                }

                @Override
                protected void interpolate(double p) {
                    final double q = 1.0 - p;
                    for (Integer id : id2oldCoordinates.keySet()) {
						view.getEdgeView(view.getGraph().findEdgeById(id)).setControlCoordinates(CollectionUtils.weightedSum(p, id2newCoordinates.get(id), q, id2oldCoordinates.get(id)));
                    }
                }
            };
            animation.play();
        } else {
            for (Integer id : id2oldCoordinates.keySet()) {
                view.getEdgeView(view.getGraph().findEdgeById(id)).setControlCoordinates(id2newCoordinates.get(id));
            }
        }
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
