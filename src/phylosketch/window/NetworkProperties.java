/*
 * NetworkProperties.java Copyright (C) 2021. Daniel H. Huson
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
import jloda.graph.*;
import jloda.graph.algorithms.CutPoints;
import jloda.phylo.PhyloTree;
import jloda.util.*;
import splitstree5.treebased.OffspringGraphMatching;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * maintains network properties
 * Daniel Huson, 1.2020
 */
public class NetworkProperties {
    private final static IntegerProperty fontSize = new SimpleIntegerProperty(-1);

    /**
     * setup network properties
     *
     * @param statusFlowPane
     * @param graphFX
     * @param <G>
     */
    public static <G extends Graph> void setup(FlowPane statusFlowPane, GraphFX<G> graphFX, BooleanProperty leafLabeledDAGProperty) {
        if (fontSize.get() < 2)
            fontSize.set(ProgramProperties.get("StatusPaneFontSize", 14));

        graphFX.getNodeList().addListener((InvalidationListener) z -> update(statusFlowPane, graphFX, leafLabeledDAGProperty));
        graphFX.getEdgeList().addListener((InvalidationListener) z -> update(statusFlowPane, graphFX, leafLabeledDAGProperty));
        graphFX.getNodeList().addListener((ListChangeListener<Node>) (z) -> {
            while (z.next()) {
                for (Node v : z.getAddedSubList()) {
                    graphFX.nodeLabelProperty(v).addListener(c -> update(statusFlowPane, graphFX, leafLabeledDAGProperty));
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

    public static <G extends Graph> void update(FlowPane statusFlowPane, GraphFX<G> graphFX, BooleanProperty leafLabeledDAGProperty) {
        if (graphFX.isNotUpdatingPropertiesAndSet()) {
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

					if (getLabel2Node(graph).size() != IteratorUtils.size(getNode2Label(graph).values()))
						Platform.runLater(() -> statusFlowPane.getChildren().add(newText("multi-labeled")));

                    progress.incrementProgress();
                    final boolean isDAG = isNonEmptyDAG(graph);
                    Platform.runLater(() -> leafLabeledDAGProperty.set(isDAG && numberOfUnlabeledLeaves == 0));
                    progress.incrementProgress();

                    if (isNonEmptyForest(graph))
                        Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree")));
                    else if (isDAG) {
                        if (numberOfUnlabeledLeaves == 0) {
                            progress.incrementProgress();

                            final EdgeSet matching = OffspringGraphMatching.compute(graph, progress);
                            if (OffspringGraphMatching.isTreeBased(graph, matching))
                                Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree-based,")));
                            else
                                Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree-based-distance: " + OffspringGraphMatching.discrepancy(graph, matching) + ",")));

                            if (isTreeChild(graph))
                                Platform.runLater(() -> statusFlowPane.getChildren().add(newText("tree-child,")));

                            if (isTemporal(graph))
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
                    graphFX.setUpdatingProperties(false);
                    for (Object object : statusFlowPane.getChildren()) {
                        if (object instanceof Shape) {
                            ((Shape) object).prefWidth(30);
                        }
                    }
                }
            });
        }
    }

    public static boolean isNonEmptyForest(Graph graph) {
        if (graph.getNumberOfNodes() == 0)
            return false;
        final NodeSet visited = new NodeSet(graph);

        final Queue<Node> queue = new LinkedList<>(findRoots(graph));
        while (queue.size() > 0) {
            final Node w = queue.remove();
            if (visited.contains(w))
                return false;
            else
                visited.add(w);
			queue.addAll(IteratorUtils.asList(w.children()));
        }
        return true;
    }

    public static boolean isNonEmptyDAG(Graph graph) {
        if (graph.getNumberOfNodes() == 0)
            return false;
        final Graph g = new Graph();
        g.copy(graph);

        while (g.getNumberOfNodes() > 0) {
            boolean found = false;
            for (Node v : g.nodes()) {
                if (v.getOutDegree() == 0) {
                    g.deleteNode(v);
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }
        return true;
    }

    public static boolean isLeafLabeled(Graph graph) {
        final Optional<Node> unlabeled = graph.nodeStream().filter(v -> v.getOutDegree() == 0 && (graph.getLabel(v) == null || (graph.getLabel(v).length() == 0))).findAny();
        return unlabeled.isEmpty();
    }

    public static List<Node> findRoots(Graph graph) {
        return graph.nodeStream().filter(v -> v.getInDegree() == 0).collect(Collectors.toList());
    }

    public static Map<String, Node> getLabel2Node(Graph graph) {
        final Map<String, Node> map = new TreeMap<>();
        for (Node v : graph.nodes()) {
            final String label = graph.getLabel(v);
            if (label != null)
                map.put(label, v);
        }
        return map;
    }

    public static NodeArray<String> getNode2Label(Graph graph) {
        final NodeArray<String> map = new NodeArray<>(graph);
        for (Node v : graph.nodes()) {
            final String label = graph.getLabel(v);
            if (label != null)
                map.put(v, label);
        }
        return map;
    }

    public static boolean isTreeChild(PhyloTree graph) {
        for (Node v : graph.nodes()) {
            if (v.getOutDegree() > 0) {
                boolean ok = false;
                for (Node w : v.children()) {
                    if (w.getInDegree() == 1) {
                        ok = true;
                        break;
                    }
                }
                if (!ok)
                    return false;
            }
        }
        return true;
    }

    public static NodeSet computeAllVisibleNodes(PhyloTree graph) {
        var result = graph.newNodeSet();

        for (var root : graph.nodeStream().filter(v -> v.getInDegree() == 0).collect(Collectors.toList())) {
            result.add(root);

            NodeArray<BitSet> leavesBelow = graph.newNodeArray();
            depthFirstDAG(root, v -> {
                if (v.getOutDegree() == 0) {
                    leavesBelow.put(v, BitSetUtils.asBitSet(v.getId()));
                    result.add(v);
                } else {
                    var set = new BitSet();
                    for (var w : v.children()) {
                        set.or(leavesBelow.get(w));
                    }
                    leavesBelow.put(v, set);
                }
            });

            for (var nodeId : BitSetUtils.members(leavesBelow.get(root))) {
                result.addAll(CutPoints.apply(graph, v -> leavesBelow.get(v).get(nodeId)));
            }
        }
        return result;
    }

    public static void depthFirstDAG(Node root, Consumer<Node> calculation) {
        for (var w : root.children()) {
            depthFirstDAG(w, calculation);
        }
        calculation.accept(root);
    }

    /**
     * determines all visible nodes that are unavoidable in any path from the root to at least one leaf
     *
     * @param graph
     * @return
     */
    public static NodeSet computeAllVisibleNodesOld(PhyloTree graph) {
        var result = graph.newNodeSet();
        if (isNonEmptyDAG(graph)) {

            var leaves = graph.nodeStream().filter(v -> v.getOutDegree() == 0).collect(Collectors.toSet());
            var stableNodes = computeAllLowestStableAncestors(graph, leaves);

            for (var root : findRoots(graph))
                computeAllVisibleNodesRec(root, stableNodes, result);
        }
        return result;
    }

    private static void computeAllVisibleNodesRec(Node v, NodeSet stableNodes, NodeSet result) {
        if (stableNodes.contains(v))
            result.add(v);

        for (var w : v.children()) {
            computeAllVisibleNodesRec(w, stableNodes, result);
            if (w.getInDegree() == 1 && (result.contains(w) || w.getOutDegree() == 0)) {
                result.add(v);
            }
        }
    }

    /**
     * determines all completely stable nodes, which are nodes that lie on all paths to all of their children
     *
     * @param graph
     * @return
     */
    public static NodeSet computeAllCompletelyStableInternal(PhyloTree graph) {
        var result = graph.newNodeSet();

        if (isNonEmptyDAG(graph)) {
            for (var root : findRoots(graph))
                computeAllCompletelyStableInternalRec(root, new HashSet<>(), new HashSet<>(), result);
        }
        return result;
    }

    /**
     * recursively determines all stable nodes
     */
    private static void computeAllCompletelyStableInternalRec(Node v, Set<Node> below, Set<Node> parentsOfBelow, NodeSet result) {
        if (v.getOutDegree() == 0) {
			below.add(v);
			parentsOfBelow.addAll(IteratorUtils.asList(v.parents()));
        } else {
            var belowV = new HashSet<Node>();
            var parentsOfBelowV = new HashSet<Node>();

            for (var w : v.children()) {
                computeAllCompletelyStableInternalRec(w, belowV, parentsOfBelowV, result);
            }
			belowV.forEach(u -> parentsOfBelowV.addAll(IteratorUtils.asList(u.parents())));
			belowV.add(v);

            if (belowV.containsAll(parentsOfBelowV)) {
                result.add(v);
            }
            below.addAll(belowV);
            parentsOfBelow.addAll(parentsOfBelowV);
        }
    }

    /**
     * determines all stable nodes for the given query set. For each node in the query set, this
     * is the set of nodes that lies on all paths to that member of the query set
     */
    public static NodeSet computeAllLowestStableAncestors(PhyloTree graph, Collection<Node> query) {
        var result = graph.newNodeSet();

        if (isNonEmptyDAG(graph)) {
            final NodeArray<Set<Node>> below = new NodeArray<>(graph);
            var remainingQuery = graph.newNodeSet();
            for (var root : findRoots(graph)) {
                labelByDescendantsRec(root, query, below);
                if (below.get(root) != null) {
                    remainingQuery.clear();
                    computeAllStableAncestorsRec(root, remainingQuery, below, result);
                }
            }
        }
        return result;
    }

    /**
     * label by all queries below
     *
     * @param v     current node
     * @param query set of query nodes
     * @param below will contain for each node, the set of query nodes on or below it
     * @return query nodes on or below v
     */
    private static Set<Node> labelByDescendantsRec(Node v, Collection<Node> query, NodeArray<Set<Node>> below) {
        Set<Node> belowV = null;

        for (var w : v.children()) {
            var belowW = labelByDescendantsRec(w, query, below);
            if (belowW != null) {
                if (belowV == null)
                    belowV = new HashSet<>();
                belowV.addAll(belowW);
            }
        }
        if (query.contains(v)) {
            if (belowV == null)
                belowV = new HashSet<>();
            belowV.add(v);
        }
        below.put(v, belowV);
        return belowV;
    }

    /**
     * recursively determines all stable nodes
     */
    private static void computeAllStableAncestorsRec(Node v, Set<Node> remainingQuery, NodeArray<Set<Node>> below, NodeSet result) {
        if (remainingQuery.size() > 0) {
            var count = new HashMap<Node, Integer>();
            for (var w : v.children()) {
                var belowW = below.get(w);
                if (belowW != null) {
                    for (var u : belowW) {
                        if (remainingQuery.contains(u)) {
                            count.merge(u, 1, Integer::sum);
                        }
                    }
                }
            }
            for (var u : count.keySet()) {
                if (count.get(u) > 1) {
                    result.add(v);
                    remainingQuery.remove(u);
                }
            }
            for (var w : v.children()) {
                computeAllStableAncestorsRec(w, remainingQuery, below, result);
            }
        }
    }

    public static boolean isTemporal(PhyloTree graph) {
        var contractedGraph = new PhyloTree(graph);

        var reticulateEdges = contractedGraph.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).collect(Collectors.toSet());

        if (reticulateEdges.size() == 0)
            return true;
        else {
            var selfEdgeEncountered = new Single<>(false);
            contractedGraph.contractEdges(reticulateEdges, selfEdgeEncountered);
            return !selfEdgeEncountered.get() && isNonEmptyDAG(contractedGraph);
        }
    }

    public static Text newText(String label) {
        var text = new Text(label);
        text.setStyle(String.format("-fx-font-size: %d;", fontSize.get()));
        return text;
    }
}
