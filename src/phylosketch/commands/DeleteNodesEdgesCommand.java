/*
 * DeleteNodesEdgesCommand.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import jloda.fx.shapes.NodeShape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import phylosketch.window.EdgeView;
import phylosketch.window.NodeView;
import phylosketch.window.PhyloView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * delete nodes command
 * Daniel Huson, 1.2020
 */
public class DeleteNodesEdgesCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    public DeleteNodesEdgesCommand(Pane pane, PhyloView view, Collection<Node> nodes, Collection<Edge> edges) {
        super("Delete");

        final PhyloTree graph = view.getGraph();

        final ArrayList<NodeData> nodeDataList = new ArrayList<>();

        final ArrayList<EdgeData> edgeDataList = new ArrayList<>();

        final Set<Edge> edgeSet = new HashSet<>(edges);

        for (Node v : nodes) {
            nodeDataList.add(new NodeData(v.getId(), view.getNodeView(v)));
            edgeSet.addAll(Basic.asList(v.adjacentEdges()));
        }

        for (Edge e : edgeSet) {
            edgeDataList.add(new EdgeData(e.getId(), e.getSource().getId(), e.getTarget().getId(), view.getEdgeView(e)));
        }

        undo = () -> {
            view.getNodeSelection().clearSelection();
            view.getEdgeSelection().clearSelection();

            for (NodeData data : nodeDataList) {
                final Node v = graph.newNode(null, data.id);
                data.apply(view.addNode(v, pane, data.x, data.y));
                view.getNodeSelection().select(v);
            }
            for (EdgeData data : edgeDataList) {
                final Node v = graph.findNodeById(data.sourceId);
                final Node w = graph.findNodeById(data.targetId);
                final Edge e = graph.newEdge(v, w, null, data.id);
                data.apply(view.addEdge(e));
                view.getEdgeSelection().select(e);
            }
        };

        redo = () -> {
            for (EdgeData data : edgeDataList) {
                final Edge e = graph.findEdgeById(data.id);
                view.removeEdge(e);
                graph.deleteEdge(e);
            }
            for (NodeData data : nodeDataList) {
                final Node v = graph.findNodeById(data.id);
                view.removeNode(v);
                graph.deleteNode(v);
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

    static class NodeData {
        final int id;
        final double x;
        final double y;
        final Paint fill;
        final NodeShape nodeShape;

        final String text;
        final double lx;
        final double ly;
        final Paint textFill;


        public NodeData(int id, NodeView nv) {
            this.id = id;
            this.x = nv.getTranslateX();
            this.y = nv.getTranslateY();
            this.fill = nv.getShape().getFill();
            this.nodeShape = NodeShape.valueOf(nv.getShape());
            this.text = nv.getLabel().getText();
            this.lx = nv.getLabel().getLayoutX();
            this.ly = nv.getLabel().getLayoutY();
            this.textFill = nv.getLabel().getTextFill();
        }

        public void apply(NodeView nv) {
            nv.setTranslateX(x);
            nv.setTranslateY(y);
            nv.changeShape(nodeShape);
            nv.getShape().setFill(fill);
            nv.getLabel().setText(text);
            nv.getLabel().setLayoutX(lx);
            nv.getLabel().setLayoutY(ly);
            nv.getLabel().setTextFill(textFill);
        }
    }

    static class EdgeData {
        final int id;
        final int sourceId;
        final int targetId;
        final double strokeWidth;
        final Paint stroke;
        final double[] controlCoordinates;
        final boolean arrow;

        public EdgeData(int id, int sourceId, int targetId, EdgeView edgeView) {
            this.id = id;
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.controlCoordinates = edgeView.getControlCoordinates();
            this.strokeWidth = edgeView.getCurve().getStrokeWidth();
            this.stroke = edgeView.getCurve().getStroke();
            this.arrow = edgeView.getArrowHead().isVisible();
        }

        public void apply(EdgeView edgeView) {
            edgeView.setControlCoordinates(controlCoordinates);
            edgeView.getCurve().setStrokeWidth(strokeWidth);
            edgeView.getCurve().setStroke(stroke);
            edgeView.getArrowHead().setVisible(arrow);
        }
    }
}
