/*
 * RunNormalize.java Copyright (C) 2022. Daniel H. Huson
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

package phylosketch.algorithms;

import javafx.geometry.Point2D;
import jloda.fx.util.AService;
import jloda.fx.window.NotificationManager;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.FileUtils;
import jloda.util.IteratorUtils;
import jloda.util.Pair;
import phylosketch.commands.ChangeEdgeShapeCommand;
import phylosketch.util.NewWindow;
import phylosketch.window.MainWindow;

/**
 * run the normalization algorithm
 * Daniel Huson, 1.2022
 */
public class RunNormalize {
	public static void apply(MainWindow mainWindow) {
		var newWindow = NewWindow.apply();

		newWindow.getView().setFileName(FileUtils.replaceFileSuffix(mainWindow.getView().getFileName(), "-normalized.sptree5"));

		final AService<Pair<PhyloTree, NodeArray<Point2D>>> service = new AService<>(newWindow.getController().getStatusFlowPane());

		service.setCallable(() -> {
			var phyloTree = new PhyloTree();
			var coordinates = new NodeArray<Point2D>(phyloTree);
			var view = mainWindow.getView();
			Normalize.apply(view.getGraph(), v -> new Point2D(view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY()),
					phyloTree, coordinates::put);
			return new Pair<>(phyloTree, coordinates);
		});

		service.setOnFailed(c -> NotificationManager.showError("Normalization failed: " + service.getException()));

		service.setOnSucceeded(c -> {
			var view = newWindow.getView();
			var graph = service.getValue().getFirst();
			var coordinates = service.getValue().getSecond();
			NodeArray<Node> old2new = graph.newNodeArray();
			view.getGraph().copy(graph, old2new, null);

			graph.nodes().forEach(v -> view.addNode(old2new.get(v), newWindow.getController().getContentPane(), coordinates.get(v).getX(), coordinates.get(v).getY()));
			view.getGraph().edges().forEach(view::addEdge);

			view.getUndoManager().doAndAdd(new ChangeEdgeShapeCommand(view, IteratorUtils.asList(view.getGraph().edges()), ChangeEdgeShapeCommand.EdgeShape.Reshape));

			view.setDirty(true);
		});

		service.start();
	}
}
