/*
 * NetworkProperties.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import jloda.fx.graph.GraphFX;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.Single;
import splitstree5.treebased.OffspringGraphMatching;

import java.util.*;
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

        final int numberOfUnlabeledLeaves = (int) graph.nodeStream().filter(v -> v.getOutDegree() == 0 && graph.getLabel(v) == null).count();
        if (numberOfLeaves > 0) {
            if (numberOfUnlabeledLeaves == 0)
                statusFlowPane.getChildren().add(newText("leaf-labeled,"));
            else
                statusFlowPane.getChildren().add(newText("unlabeled leaves: " + numberOfUnlabeledLeaves + ","));

            if (getLabel2Node(graph).size() != Basic.size(getNode2Label(graph).values()))
                statusFlowPane.getChildren().add(newText("multi-labeled"));

            final boolean isDAG = isNonEmptyDAG(graph);
            leafLabeledDAGProperty.set(isDAG && numberOfUnlabeledLeaves == 0);

            if (isNonEmptyForest(graph)) {
                statusFlowPane.getChildren().add(newText("tree"));
            } else if (isDAG) {
                if (numberOfUnlabeledLeaves == 0) {
                    final EdgeSet matching = OffspringGraphMatching.compute(graph);
                    if (OffspringGraphMatching.isTreeBased(graph, matching))
                        statusFlowPane.getChildren().add(newText("tree-based,"));
                    else
                        statusFlowPane.getChildren().add(newText("tree-based-distance: " + OffspringGraphMatching.discrepancy(graph, matching) + ","));

                    if (isTreeChild(graph))
                        statusFlowPane.getChildren().add(newText("tree-child,"));

                    if (isTemporal(graph))
                        statusFlowPane.getChildren().add(newText("temporal,"));
                }
                statusFlowPane.getChildren().add(newText("DAG"));

            } else
                statusFlowPane.getChildren().add(newText("graph"));

        }

        for (Object object : statusFlowPane.getChildren()) {
            if (object instanceof Shape) {
                ((Shape) object).prefWidth(30);
            }
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
            queue.addAll(Basic.asList(w.children()));
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

    /**
     * determines all visible reticulations, which are nodes that have a tree path to a leaf or to a stable node
     *
     * @param graph
     * @return
     */
    public static NodeSet allVisibleNodes(PhyloTree graph) {
        final NodeSet result = new NodeSet(graph);
        if (isNonEmptyDAG(graph)) {

            final Set<Node> leaves = graph.nodeStream().filter(v -> v.getOutDegree() == 0).collect(Collectors.toSet());
            final NodeSet stableNodes = allLowestStableAncestors(graph, leaves);

            for (Node root : findRoots(graph))
                allVisibleNodesRec(root, stableNodes, result);
        }
        return result;
    }

    private static void allVisibleNodesRec(Node v, NodeSet stableNodes, NodeSet result) {
        if (stableNodes.contains(v))
            result.add(v);

        for (Node w : v.children()) {
            allVisibleNodesRec(w, stableNodes, result);

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
    public static NodeSet allCompletelyStableInternal(PhyloTree graph) {
        final NodeSet result = new NodeSet(graph);

        if (isNonEmptyDAG(graph)) {
            for (Node root : findRoots(graph))
                allCompletelyStableInternalRec(root, new HashSet<>(), new HashSet<>(), result);
        }
        return result;
    }

    /**
     * recursively determines all stable nodes
     */
    private static void allCompletelyStableInternalRec(Node v, Set<Node> below, Set<Node> parentsOfBelow, NodeSet result) {
        if (v.getOutDegree() == 0) {
            below.add(v);
            parentsOfBelow.addAll(Basic.asList(v.parents()));
        } else {
            final Set<Node> belowV = new HashSet<>();
            final Set<Node> parentsOfBelowV = new HashSet<>();

            for (Node w : v.children()) {
                allCompletelyStableInternalRec(w, belowV, parentsOfBelowV, result);
            }
            belowV.forEach(u -> parentsOfBelowV.addAll(Basic.asList(u.parents())));
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
    public static NodeSet allLowestStableAncestors(PhyloTree graph, Collection<Node> query) {
        final NodeSet result = new NodeSet(graph);

        if (isNonEmptyDAG(graph)) {
            final NodeArray<Set<Node>> below = new NodeArray<>(graph);
            for (Node root : findRoots(graph)) {
                labelByDescendantsRec(root, query, below);
                if (below.getValue(root) != null) {
                    final Set<Node> remainingQuery = new HashSet<>(below.getValue(root));
                    allStableAncestorsRec(root, remainingQuery, below, result);
                }
            }
        }
        return result;
    }

    /**
     * label by all queries below
     *
     * @param v
     * @param below
     * @return
     */
    private static Set<Node> labelByDescendantsRec(Node v, Collection<Node> query, NodeArray<Set<Node>> below) {
        Set<Node> belowV = null;

        for (Node w : v.children()) {
            final Set<Node> belowW = labelByDescendantsRec(w, query, below);
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
    private static void allStableAncestorsRec(Node v, Set<Node> remainingQuery, NodeArray<Set<Node>> below, NodeSet result) {
        if (remainingQuery.size() > 0) {
            final Map<Node, Integer> count = new HashMap<>();
            for (Node w : v.children()) {
                final Set<Node> belowW = below.getValue(w);
                if (belowW != null) {
                    for (Node u : belowW) {
                        if (remainingQuery.contains(u)) {
                            count.merge(u, 1, Integer::sum);
                        }
                    }
                }
            }
            for (Node u : count.keySet()) {
                if (count.get(u) > 1) {
                    result.add(v);
                    remainingQuery.remove(u);
                }
            }
            for (Node w : v.children()) {
                allStableAncestorsRec(w, remainingQuery, below, result);
            }
        }
    }

    public static boolean isTemporal(PhyloTree graph) {
        final PhyloTree contractedGraph = new PhyloTree(graph);

        final Set<Edge> reticulateEdges = contractedGraph.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).collect(Collectors.toSet());

        if (reticulateEdges.size() == 0)
            return true;
        else {
            final Single<Boolean> selfEdgeEncountered = new Single<>(false);
            contractedGraph.contractEdges(reticulateEdges, selfEdgeEncountered);
            return !selfEdgeEncountered.get() && isNonEmptyDAG(contractedGraph);
        }
    }

    public static Text newText(String label) {
        final Text text = new Text(label);
        text.setStyle(String.format("-fx-font-size: %d;", fontSize.get()));
        return text;
    }
}
