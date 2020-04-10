/*
 * RotateGraphAndLabelsCommand.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.GeometryUtilsFX;
import phylosketch.window.PhyloView;

public class RotateGraphCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public RotateGraphCommand(final PhyloView phyloView, boolean clockwise) {
        super("Rotate graph " + (clockwise ? "clockwise" : "anticlockwise"));

        undo = () -> rotateAnimated(phyloView, clockwise ? -90 : 90);
        redo = () -> rotateAnimated(phyloView, clockwise ? 90 : -90);
    }

    private void rotateAnimated(PhyloView phyloView, double alpha) {
        if (phyloView.getGraph().getNumberOfNodes() < 5000) {
            final Animation animation = new Transition() {
                double previous = 0;

                {
                    setCycleDuration(Duration.millis(500));
                }

                @Override
                protected void interpolate(double frac) {
                    double add = (frac - previous) * alpha;
                    rotate(phyloView, add);
                    previous = frac;
                }
            };
            animation.play();
        } else
            rotate(phyloView, alpha);
    }

    /**
     * rotate the graph
     *
     * @param phyloView
     * @param alpha
     */
    private void rotate(PhyloView phyloView, double alpha) {
        phyloView.getGraph().nodeParallelStream().map(phyloView::getNodeView).forEach(nv -> {
            final Point2D point = GeometryUtilsFX.rotate(nv.getTranslateX(), nv.getTranslateY(), alpha);
            nv.setTranslateX(point.getX());
            nv.setTranslateY(point.getY());
        });
        phyloView.getGraph().edgeParallelStream().map(phyloView::getEdgeView).forEach(ev -> {
            final double[] controlCoordinates = ev.getControlCoordinates();
            for (int i = 0; i < controlCoordinates.length; i += 2) {
                final Point2D point = GeometryUtilsFX.rotate(controlCoordinates[i], controlCoordinates[i + 1], alpha);
                controlCoordinates[i] = point.getX();
                controlCoordinates[i + 1] = point.getY();
            }
            ev.setControlCoordinates(controlCoordinates);
        });
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