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
import javafx.util.Duration;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.Node;
import phylosketch.window.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * rotate node labels
 * Daniel Huson, 4.2020
 */
public class RotateLabelsCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public RotateLabelsCommand(final PhyloView phyloView, boolean clockwise) {
        super("Rotate labels " + (clockwise ? "clockwise" : "anticlockwise"));

        final ArrayList<Node> nodes = new ArrayList<>();
        if (phyloView.getNodeSelection().size() > 0)
            nodes.addAll(phyloView.getNodeSelection().getSelectedItems());
        else
            nodes.addAll(phyloView.getGraph().getNodesAsSet());

        undo = () -> rotate(phyloView, nodes, clockwise ? -90 : 90);
        redo = () -> rotateAnimated(phyloView, nodes, clockwise ? 90 : -90);
    }

    private void rotateAnimated(PhyloView phyloView, Collection<Node> nodes, double alpha) {
        if (nodes.size() < 5000) {
            final Animation animation = new Transition() {
                double previous = 0;

                {
                    setCycleDuration(Duration.millis(500));
                }

                @Override
                protected void interpolate(double frac) {
                    double add = (frac - previous) * alpha;
                    rotate(phyloView, nodes, add);
                    previous = frac;
                }
            };
            animation.play();
        } else
            rotate(phyloView, nodes, alpha);
    }

    /**
     * rotate the labels
     *
     * @param phyloView
     * @param alpha
     */
    private void rotate(PhyloView phyloView, Collection<Node> nodes, double alpha) {
        nodes.stream().map(phyloView::getNodeView).forEach(nv -> {
            nv.setLabelAngle(GeometryUtilsFX.modulo360(nv.getLabelAngle() + alpha));
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
