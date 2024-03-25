/*
 * CreateEdgeCommand.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.scene.paint.Paint;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import phylosketch.view.PhyloView;

/**
 * create an edge
 * Daniel Huson, 2.2020
 */
public class CreateEdgeCommand extends UndoableRedoableCommand {
    final private Runnable undo;
    final private Runnable redo;

    private int eId;

    public CreateEdgeCommand(PhyloView phyloView, Node v, Node w) {
		this(phyloView, v, w, null, null, null);
	}

	public CreateEdgeCommand(PhyloView phyloView, Node v, Node w, Boolean arrowHeadVisible, Double strokeWidth, Paint stroke) {
        super("Create Edge");
        final PhyloTree graph = phyloView.getGraph();

        final int vId = v.getId();
        final int wId = w.getId();

        undo = () -> {
            if (eId != 0) {
                final Edge e = graph.findEdgeById(eId);
                phyloView.removeEdge(e);
                graph.deleteEdge(e);
            }
        };

        redo = () -> {
            final Node vv = graph.findNodeById(vId);
            final Node ww = graph.findNodeById(wId);
            final Edge e;
            if (eId == 0) {
                e = graph.newEdge(vv, ww);
                eId = e.getId();
            } else
                e = graph.newEdge(vv, ww, null, eId);
			var edgeView = phyloView.addEdge(e);
			if (arrowHeadVisible != null)
				edgeView.getArrowHead().setVisible(arrowHeadVisible);
			if (strokeWidth != null)
				edgeView.getCurve().setStrokeWidth(strokeWidth);
			if (stroke != null)
				edgeView.getCurve().setStroke(stroke);
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
