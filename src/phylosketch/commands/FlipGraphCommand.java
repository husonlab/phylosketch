/*
 * RotateGraphAndLabelsCommand.java Copyright (C) 2021. Daniel H. Huson
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
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.util.Basic;
import jloda.util.Pair;
import jloda.util.ProgramProperties;
import jloda.util.Triplet;
import phylosketch.window.NodeView;
import phylosketch.window.PhyloView;

import java.util.List;
import java.util.stream.Collectors;

/**
 * flip the graph horizontally or vertically
 * Daniel Huson, 4.2020
 */
public class FlipGraphCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public FlipGraphCommand(final PhyloView view, boolean horizontally) {
        super("Flip graph " + (horizontally ? "horizontally" : "vertically"));

        final PositionNodeLabelsCommand positionNodeLabelsCommand = new PositionNodeLabelsCommand(view, Basic.asList(view.getGraph().nodes()),
                PositionNodeLabelsCommand.Position.getDefault(view.computeRootLocation().opposite(horizontally)));

        undo = () -> {
            flipAnimated(view, horizontally);
            positionNodeLabelsCommand.undo();
        };
        redo = () -> {
            flipAnimated(view, horizontally);
            positionNodeLabelsCommand.redo();
        };
    }

    /**
     * flip the graph and animate
     *
     * @param phyloView
     * @param horizontally
     */
    private void flipAnimated(PhyloView phyloView, boolean horizontally) {
        final Graph graph = phyloView.getGraph();

        final List<Triplet<Node, Double, Double>> nodeAndCoordinates = graph.nodeParallelStream().map(v ->
                new Triplet<>(v, horizontally ? phyloView.getX(v) : phyloView.getY(v), horizontally ? -phyloView.getX(v) : -phyloView.getY(v))).
                collect(Collectors.toList());

        final List<Pair<Edge, double[]>> edgeAndCoordinates = phyloView.getGraph().edgeParallelStream().map(e -> {
            final double[] controlCoordinates = phyloView.getEdgeView(e).getControlCoordinates();
            final double[] array = new double[controlCoordinates.length];
            for (int i = 0; i < controlCoordinates.length; i += 2) {
                array[i] = controlCoordinates[horizontally ? i : i + 1];
                array[i + 1] = -array[i];
            }
            return new Pair<>(e, array);
        }).collect(Collectors.toList());

        if (graph.getNumberOfNodes() < ProgramProperties.get("AnimationLimit", 5000)) {
            final Animation animation = new Transition() {
                {
                    setCycleDuration(Duration.millis(500));
                }

                @Override
                protected void interpolate(double p) {
                    flip(phyloView, nodeAndCoordinates, edgeAndCoordinates, horizontally, p);
                }
            };
            animation.play();
        } else {
            flip(phyloView, nodeAndCoordinates, edgeAndCoordinates, horizontally, 1.0);
        }
    }

    /**
     * flip the graph
     *
     * @param phyloView
     * @param node2coordinates
     * @param edge2coordinates
     * @param p
     */
    private void flip(PhyloView phyloView, List<Triplet<Node, Double, Double>> node2coordinates, List<Pair<Edge, double[]>> edge2coordinates, boolean horizontally, double p) {
        final double q = 1.0 - p;
        for (Triplet<Node, Double, Double> triplet : node2coordinates) {
            final NodeView nv = phyloView.getNodeView(triplet.getFirst());
            if (horizontally)
                nv.setTranslateX(q * triplet.getSecond() + p * triplet.getThird());
            else
                nv.setTranslateY(q * triplet.getSecond() + p * triplet.getThird());
        }
        for (Pair<Edge, double[]> pair : edge2coordinates) {
            final Edge e = pair.getFirst();
            final double[] array = pair.getSecond();
            final double[] controlCoordinates = phyloView.getEdgeView(e).getControlCoordinates();
            for (int i = 0; i < array.length; i += 2) {
                controlCoordinates[horizontally ? i : i + 1] = q * array[i] + p * array[i + 1];
            }
            phyloView.getEdgeView(e).setControlCoordinates(controlCoordinates);
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
