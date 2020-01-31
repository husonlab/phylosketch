/*
 *  MoveSelectedNodesCommand.java Copyright (C) 2020 Daniel H. Huson
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

import javafx.collections.ObservableList;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.graph.NodeEdge;
import jloda.phylo.PhyloTree;
import phyloedit.window.EdgeView;
import phyloedit.window.PhyloEditor;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * move all selected nodes
 * Daniel Huson, 1.2020
 */
public class MoveSelectedNodesCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    /**
     * constructor
     *
     * @param x
     * @param y
     * @param editor
     * @param selectedItems
     * @param oldEdgeControlCoordinates
     * @param newEdgeControlCoordinates
     */
    public MoveSelectedNodesCommand(double x, double y, PhyloEditor editor, ObservableList<Node> selectedItems,
                                    Map<Integer, double[]> oldEdgeControlCoordinates, Map<Integer, double[]> newEdgeControlCoordinates) {
        super("Move");

        final PhyloTree graph = editor.getGraph();
        final Collection<Integer> nodeIds = selectedItems.stream().map(NodeEdge::getId).collect(Collectors.toList());

        undo = () -> {
            for (int id : nodeIds) {
                Node v = graph.searchNodeId(id);
                editor.moveNode(v, -x, -y);
            }
            for (int id : oldEdgeControlCoordinates.keySet()) {
                EdgeView edgeView = editor.getEdge2view().get(graph.searchEdgeId(id));
                edgeView.setControlCoordinates(oldEdgeControlCoordinates.get(id));
            }
        };
        redo = () -> {
            for (int id : nodeIds) {
                Node v = graph.searchNodeId(id);
                editor.moveNode(v, x, y);
            }
            for (int id : newEdgeControlCoordinates.keySet()) {
                EdgeView edgeView = editor.getEdge2view().get(graph.searchEdgeId(id));
                edgeView.setControlCoordinates(newEdgeControlCoordinates.get(id));
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
