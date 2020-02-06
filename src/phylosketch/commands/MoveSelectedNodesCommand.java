/*
 * MoveSelectedNodesCommand.java Copyright (C) 2020 Daniel H. Huson
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

package phylosketch.commands;

import javafx.collections.ObservableList;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.graph.NodeEdge;
import jloda.phylo.PhyloTree;
import phylosketch.window.PhyloView;

import java.util.HashMap;
import java.util.List;
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
     * @param dx
     * @param dy
     * @param editor
     * @param selectedItems
     * @param oldEdgeControlCoordinates0
     * @param newEdgeControlCoordinates0
     */
    public MoveSelectedNodesCommand(double dx, double dy, PhyloView editor, ObservableList<Node> selectedItems,
                                    Map<Integer, double[]> oldEdgeControlCoordinates0, Map<Integer, double[]> newEdgeControlCoordinates0) {
        super("Move");

        final PhyloTree graph = editor.getGraph();
        final Map<Integer, double[]> oldEdgeControlCoordinates = new HashMap<>(oldEdgeControlCoordinates0);
        final Map<Integer, double[]> newEdgeControlCoordinates = new HashMap<>(newEdgeControlCoordinates0);

        final List<Integer> nodeData = selectedItems.stream().map(NodeEdge::getId).collect(Collectors.toList());
        final List<Integer> edgeData = graph.edgeStream().filter(e -> oldEdgeControlCoordinates.containsKey(e.getId()) && newEdgeControlCoordinates.containsKey(e.getId()))
                .map(NodeEdge::getId).collect(Collectors.toList());


        undo = () -> {
            nodeData.forEach(id -> editor.moveNode(graph.searchNodeId(id), -dx, -dy));

            edgeData.forEach(id -> editor.getEdge2view().get(graph.searchEdgeId(id)).setControlCoordinates(oldEdgeControlCoordinates.get(id)));
        };

        redo = () -> {
            nodeData.forEach(id -> editor.moveNode(graph.searchNodeId(id), dx, dy));

            edgeData.forEach(id -> editor.getEdge2view().get(graph.searchEdgeId(id)).setControlCoordinates(newEdgeControlCoordinates.get(id)));
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
