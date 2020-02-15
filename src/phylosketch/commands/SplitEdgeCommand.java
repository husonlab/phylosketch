/*
 * SplitEdgeCommand.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import phylosketch.window.EdgeView;
import phylosketch.window.PhyloView;

/**
 * split edge command
 * Daniel Huson, 2.2020
 */
public class SplitEdgeCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    private int newNodeId = 0;

    /**
     * constructor
     *
     * @param pane
     * @param view
     * @param e
     * @param location
     */
    public SplitEdgeCommand(Pane pane, PhyloView view, Edge e, Point2D location) {
        super("Split Edge");

        final PhyloTree graph = view.getGraph();

        final int oldEdgeId = e.getId();
        final double[] oldEdgeCoordinates = view.getEdgeView(e).getControlCoordinates();
        final double oldEdgeWidth = view.getEdgeView(e).getCurve().getStrokeWidth();
        final Paint oldEdgePaint = view.getEdgeView(e).getCurve().getStroke();

        final int sourceId = e.getSource().getId();
        final int targetId = e.getTarget().getId();

        undo = () -> {
            if (newNodeId > 0) {
                final Node v = graph.searchNodeId(newNodeId);
                view.removeNode(v);
                graph.deleteNode(v);

                final Edge oldEdge = graph.newEdge(graph.searchNodeId(sourceId), graph.searchNodeId(targetId), null, oldEdgeId);
                final EdgeView ev = view.addEdge(oldEdge);
                ev.setControlCoordinates(oldEdgeCoordinates);
                ev.getCurve().setStrokeWidth(oldEdgeWidth);
                ev.getCurve().setStroke(oldEdgePaint);
            }
        };

        redo = () -> {
            final Edge oldEdge = graph.searchEdgeId(oldEdgeId);
            view.removeEdge(oldEdge);
            graph.deleteEdge(oldEdge);

            Node newNode;
            if (newNodeId == 0) {
                newNode = graph.newNode();
                newNodeId = newNode.getId();
            } else
                newNode = graph.newNode(null, newNodeId);
            view.addNode(newNode, pane, location.getX(), location.getY());

            final Edge e1 = graph.newEdge(graph.searchNodeId(sourceId), newNode);
            final EdgeView ev1 = view.addEdge(e1);
            ev1.getCurve().setStrokeWidth(oldEdgeWidth);
            ev1.getCurve().setStroke(oldEdgePaint);

            final Edge e2 = graph.newEdge(newNode, graph.searchNodeId(targetId));
            final EdgeView ev2 = view.addEdge(e2);
            ev2.getCurve().setStrokeWidth(oldEdgeWidth);
            ev2.getCurve().setStroke(oldEdgePaint);
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
