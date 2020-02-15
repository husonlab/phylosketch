/*
 * NewEdgeAndNodeCommand.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Graph;
import jloda.graph.Node;
import phylosketch.window.PhyloView;

/**
 * the create Edge and Node command
 * Daniel Huson, 1.2020
 */
public class NewEdgeAndNodeCommand extends UndoableRedoableCommand {
    final private Runnable undo;
    final private Runnable redo;

    private int edgeId;
    private int wId;

    /**
     * construct
     *
     * @param pane
     * @param editor
     * @param a
     * @param b
     * @param x
     * @param y
     */
    public NewEdgeAndNodeCommand(Pane pane, PhyloView editor, Node a, final Node b, double x, double y) {
        super("Add Edge");
        final Graph graph = editor.getGraph();

        final int aId = a.getId();
        final int bId = (b != null ? b.getId() : 0);

        undo = () -> {
            if (wId > 0) {
                final Node w = graph.searchNodeId(wId);
                editor.removeNode(w);
                graph.deleteNode(w);
            } else if (edgeId > 0) {
                final Edge e = graph.searchEdgeId(edgeId);
                editor.removeEdge(e);
                graph.deleteEdge(e);
            }
        };

        redo = () -> {
            Node w;
            if (bId == 0) {
                if (wId == 0) {
                    w = graph.newNode();
                    wId = w.getId();
                } else
                    w = graph.newNode(null, wId);
                editor.addNode(w, pane, x, y);
            } else
                w = graph.searchNodeId(bId);

            final Node v = graph.searchNodeId(aId);
            if (v.getCommonEdge(w) == null && v != w) {
                Edge e;
                if (edgeId == 0) {
                    e = graph.newEdge(v, w);
                    edgeId = e.getId();
                } else {
                    e = graph.newEdge(v, w, null, edgeId);
                }
                editor.addEdge(e);
            }
            if (wId > 0) Platform.runLater(() -> editor.getNodeSelection().clearAndSelect(graph.searchNodeId(wId)));
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
