/*
 *  DeleteNodesEdgesCommand.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.actions;

import javafx.scene.layout.Pane;
import jloda.fx.control.ItemSelectionModel;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeEdge;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import phyloedit.window.EdgeView;
import phyloedit.window.PhyloEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * delete nodes command
 * Daniel Huson, 1.2020
 */
public class DeleteNodesEdgesCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    private final ArrayList<OldNodeData> oldNodeData = new ArrayList<>();

    private final ArrayList<OldEdgeData> oldEdgeData = new ArrayList<>();

    public DeleteNodesEdgesCommand(Pane pane, PhyloEditor editor) {
        super("Delete");

        final PhyloTree graph = editor.getGraph();
        final ItemSelectionModel<Node> selectionModel = editor.getNodeSelection();

        final Collection<Integer> nodeIds = selectionModel.getSelectedItems().stream().map(NodeEdge::getId).collect(Collectors.toList());
        final Collection<Integer> edgeIds = selectionModel.getSelectedItems().stream().map(NodeEdge::getId).collect(Collectors.toList());

        for (Node v : graph.nodes()) {
            edgeIds.addAll(Basic.asList(v.adjacentEdges()).stream().map(NodeEdge::getId).collect(Collectors.toList()));
        }

        undo = () -> {
            selectionModel.clearSelection();
            for (OldNodeData data : oldNodeData) {
                Node v = graph.newNode(null, data.id);
                editor.addNode(pane, data.x, data.y, v);
                graph.setLabel(v, data.label);
                editor.getLabel(v).setText(data.label);
                selectionModel.select(v);
            }
            for (OldEdgeData data : oldEdgeData) {
                final Node v = graph.searchNodeId(data.sourceId);
                final Node w = graph.searchNodeId(data.targetId);
                final Edge e = graph.newEdge(v, w, null, data.edgeId);
                editor.addEdge(e);
                final EdgeView edgeView = editor.getEdgeView(e);
                edgeView.setControlCoordinates(data.controlCoordinates);
            }
        };

        redo = () -> {
            oldNodeData.clear();
            oldEdgeData.clear();

            for (int id : edgeIds) {
                final Edge e = graph.searchEdgeId(id);
                oldEdgeData.add(new OldEdgeData(e.getId(), e.getSource().getId(), e.getTarget().getId(), editor.getEdgeView(e).getControlCoordinates()));
                editor.removeEdge(e);
                graph.deleteEdge(e);
            }
            for (int id : nodeIds) {
                final Node v = graph.searchNodeId(id);
                oldNodeData.add(new OldNodeData(v.getId(), graph.getLabel(v), editor.getShape(v).getTranslateX(), editor.getShape(v).getTranslateY()));
                editor.removeNode(v);
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

    static class OldNodeData {
        int id;
        String label;
        double x;
        double y;

        public OldNodeData(int id, String label, double x, double y) {
            this.id = id;
            this.label = label;
            this.x = x;
            this.y = y;
        }
    }

    static class OldEdgeData {
        int edgeId;
        int sourceId;
        int targetId;
        double[] controlCoordinates;

        public OldEdgeData(int edgeId, int sourceId, int targetId, double[] controlCoordinates) {
            this.edgeId = edgeId;
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.controlCoordinates = controlCoordinates;
        }
    }
}
