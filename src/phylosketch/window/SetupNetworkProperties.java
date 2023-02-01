/*
 * SetupNetworkProperties.java Copyright (C) 2023 Daniel H. Huson
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

package phylosketch.window;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import jloda.fx.graph.GraphFX;
import jloda.fx.util.AService;
import jloda.fx.util.ProgramProperties;
import jloda.graph.EdgeSet;
import jloda.graph.Graph;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.phylo.algorithms.OffspringGraphMatching;
import jloda.phylo.algorithms.RootedNetworkProperties;
import jloda.util.IteratorUtils;

/**
 * setup network properties
 * Daniel Huson, 1.2022
 */
public class SetupNetworkProperties {
	final static IntegerProperty fontSize = new SimpleIntegerProperty(-1);

	/**
	 * setup network properties
	 */
	public static <G extends Graph> void setup(FlowPane statusFlowPane, GraphFX<G> graphFX, BooleanProperty updatingProperties, BooleanProperty leafLabeledDAGProperty) {
		if (fontSize.get() < 2)
			fontSize.set(ProgramProperties.get("StatusPaneFontSize", 14));

		graphFX.getNodeList().addListener((InvalidationListener) z -> update(statusFlowPane, graphFX, updatingProperties, leafLabeledDAGProperty));
		graphFX.getEdgeList().addListener((InvalidationListener) z -> update(statusFlowPane, graphFX, updatingProperties, leafLabeledDAGProperty));
		graphFX.getNodeList().addListener((ListChangeListener<Node>) (z) -> {
			while (z.next()) {
				for (Node v : z.getAddedSubList()) {
					graphFX.nodeLabelProperty(v).addListener(c -> update(statusFlowPane, graphFX, updatingProperties, leafLabeledDAGProperty));
				}
			}
		});

		statusFlowPane.setOnContextMenuRequested(e -> {
			final MenuItem increaseFontSizeButton = new MenuItem("Increase Font Size");
			increaseFontSizeButton.setOnAction(z -> {
				fontSize.set(fontSize.get() + 2);
				statusFlowPane.getChildren().stream().filter(c -> c instanceof Text).map(c -> (Text) c).forEach(c -> c.setStyle(String.format("-fx-font-size: %d;", fontSize.get())));
				ProgramProperties.put("StatusPaneFontSize", fontSize.get());


			});
			final MenuItem decreaseFontSizeButton = new MenuItem("Decrease Font Size");
			decreaseFontSizeButton.setOnAction(z -> {
				fontSize.set(fontSize.get() - 2);
				statusFlowPane.getChildren().stream().filter(c -> c instanceof Text).map(c -> (Text) c).forEach(c -> c.setStyle(String.format("-fx-font-size: %d;", fontSize.get())));
				ProgramProperties.put("StatusPaneFontSize", fontSize.get());
			});
			decreaseFontSizeButton.disableProperty().bind(fontSize.lessThanOrEqualTo(2));

			final ContextMenu contextMenu = new ContextMenu(increaseFontSizeButton, decreaseFontSizeButton);
			contextMenu.show(statusFlowPane, e.getScreenX(), e.getScreenY());
		});
	}

	public static <G extends Graph> void update(FlowPane statusFlowPane, GraphFX<G> graphFX, BooleanProperty updatingProperties, BooleanProperty leafLabeledDAGProperty) {
		if (!updatingProperties.get()) {
			updatingProperties.set(true);
			statusFlowPane.getChildren().clear();

			final PhyloTree graph = (PhyloTree) graphFX.getGraph();
			statusFlowPane.getChildren().add(newText("nodes: " + graph.getNumberOfNodes()));

			final int numberOfRoots = (int) graph.nodeStream().filter(v -> v.getInDegree() == 0).count();
			statusFlowPane.getChildren().add(newText("(roots: " + numberOfRoots));

			final int numberOfReticulations = (int) graph.nodeStream().filter(v -> v.getInDegree() > 1).count();
			statusFlowPane.getChildren().add(newText("reticulates: " + numberOfReticulations));

			final int numberOfLeaves = (int) graph.nodeStream().filter(v -> v.getOutDegree() == 0).count();
			statusFlowPane.getChildren().add(newText("leaves: " + numberOfLeaves + ")"));

			statusFlowPane.getChildren().add(newText("edges: " + graph.getNumberOfEdges() + "  "));

			var service = new AService<Boolean>(statusFlowPane);
			service.setCallable(() -> {
				var progress = service.getProgressListener();
				progress.setTasks("Updating", "properties");
				progress.setMaximum(10);
				progress.setProgress(0);
				final int numberOfUnlabeledLeaves = (int) graph.nodeStream().filter(v -> v.getOutDegree() == 0 && graph.getLabel(v) == null).count();
				progress.incrementProgress();

				if (numberOfLeaves > 0) {
					if (numberOfUnlabeledLeaves == 0)
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("leaf-labeled,")));
					else
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("unlabeled leaves: " + numberOfUnlabeledLeaves + ",")));

					if (RootedNetworkProperties.getLabel2Node(graph).size() != IteratorUtils.size(RootedNetworkProperties.getNode2Label(graph).values()))
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("multi-labeled")));

					progress.incrementProgress();
					final boolean isDAG = RootedNetworkProperties.isNonEmptyDAG(graph);
					Platform.runLater(() -> leafLabeledDAGProperty.set(isDAG && numberOfUnlabeledLeaves == 0));
					progress.incrementProgress();

					if (RootedNetworkProperties.isNonEmptyForest(graph))
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree")));
					else if (isDAG) {
						if (numberOfUnlabeledLeaves == 0) {
							progress.incrementProgress();

							try (EdgeSet matching = OffspringGraphMatching.compute(graph, progress)) {
								if (OffspringGraphMatching.isTreeBased(graph, matching))
									Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree-based,")));
								else
									Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree-based-distance: " + OffspringGraphMatching.discrepancy(graph, matching) + ",")));
							}

							if (RootedNetworkProperties.isTreeChild(graph))
								Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree-child,")));

							if (RootedNetworkProperties.isTemporal(graph))
								Platform.runLater(() -> statusFlowPane.getChildren().add(newText("temporal,")));
						}
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("DAG")));
					} else
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("graph")));

				}
				return true;
			});

			service.start();

			service.stateProperty().addListener((c, o, n) -> {
				if (o == Worker.State.RUNNING && n != Worker.State.RUNNING) {
					updatingProperties.set(false);
					for (var object : statusFlowPane.getChildren()) {
						if (object instanceof Shape shape) {
							shape.prefWidth(30);
						}
					}
				}
			});
		}
	}

	public static Text newText(String label) {
		var text = new Text(label);
		text.setStyle(String.format("-fx-font-size: %d;", fontSize.get()));
		return text;
	}
}
