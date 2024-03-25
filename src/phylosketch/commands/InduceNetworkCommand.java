/*
 * DeleteNodesEdgesCommand.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import jloda.fx.shapes.NodeShape;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Edge;
import jloda.graph.Node;
import phylosketch.view.EdgeView;
import phylosketch.view.NodeView;
import phylosketch.view.PhyloView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * computes the induced network
 * Daniel Huson, 8.2023
 */
public class InduceNetworkCommand extends UndoableRedoableCommand {
	private final Runnable undo;
	private final Runnable redo;

	public InduceNetworkCommand(Pane pane, PhyloView view) {
		super("Induced Network");

		var list = new LinkedList<>(view.getNodeSelection().getSelectedItems());

		var nodeSelection = new HashSet<>(list);
		var edgeSelection = new HashSet<Edge>();

		while (!list.isEmpty()) {
			var v = list.remove();
			for (var e : v.inEdges()) {
				Node w = e.getSource();
				edgeSelection.add(e);
				if (!nodeSelection.contains(w)) {
					nodeSelection.add(w);
					list.add(w);
				}
			}
		}

		var nodesToDelete = new ArrayList<>(view.getGraph().getNodesAsList());
		nodesToDelete.removeAll(nodeSelection);
		var edgesToDelete = new ArrayList<>(view.getGraph().getEdgesAsList());
		edgesToDelete.removeAll(edgeSelection);

		var deleteCommand = new DeleteNodesEdgesCommand(pane, view, nodesToDelete, edgesToDelete);

		undo = deleteCommand::undo;
		redo = deleteCommand::redo;
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
