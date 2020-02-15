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

package phylosketch.window;/*
 *  NetworkProperties.java Copyright (C) 2020 Daniel H. Huson
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

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import jloda.fx.graph.GraphFX;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import splitstree5.treebased.OffspringGraphMatching;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * maintains network properties
 * Daniel Huson, 1.2020
 */
public class NetworkProperties {
    /**
     * setup network properties
     *
     * @param statusFlowPane
     * @param graphFX
     * @param <G>
     */
    public static <G extends Graph> void setup(FlowPane statusFlowPane, GraphFX<G> graphFX, BooleanProperty leafLabeledDAGProperty) {

        graphFX.getNodeList().addListener((InvalidationListener) z -> update(statusFlowPane, graphFX, leafLabeledDAGProperty));
        graphFX.getEdgeList().addListener((InvalidationListener) z -> update(statusFlowPane, graphFX, leafLabeledDAGProperty));
        graphFX.getNodeList().addListener((ListChangeListener<Node>) (z) -> {
            while (z.next()) {
                for (Node v : z.getAddedSubList()) {
                    graphFX.nodeLabelProperty(v).addListener(c -> update(statusFlowPane, graphFX, leafLabeledDAGProperty));
                }
            }
        });
    }

    public static <G extends Graph> void update(FlowPane statusFlowPane, GraphFX<G> graphFX, BooleanProperty leafLabeledDAGProperty) {
        statusFlowPane.getChildren().clear();

        final PhyloTree graph = (PhyloTree) graphFX.getGraph();
        statusFlowPane.getChildren().add(newText("nodes: " + graph.getNumberOfNodes()));
        statusFlowPane.getChildren().add(newText("edges: " + graph.getNumberOfEdges()));
        final boolean isRootedDag = isRootedDAG(graph);
        statusFlowPane.getChildren().add(newText("rooted DAG: " + isRootedDag));
        final boolean isLeafLabeled = isLeafLabeled(graph);
        statusFlowPane.getChildren().add(newText("leaf-labeled: " + isLeafLabeled));
        leafLabeledDAGProperty.set(isRootedDag && isLeafLabeled);

        if (getLabel2Node(graph).size() != Basic.size(getNode2Label(graph).values()))
            statusFlowPane.getChildren().add(newText("multi-labeled"));

        if (isRootedDag && isLeafLabeled) {
            final EdgeSet matching = OffspringGraphMatching.compute(graph);
            if (OffspringGraphMatching.isTreeBased(graph, matching))
                statusFlowPane.getChildren().add(newText("tree-based: true"));
            else
                statusFlowPane.getChildren().add(newText("tree-based: +" + OffspringGraphMatching.discrepancy(graph, matching)));

            statusFlowPane.getChildren().add(newText("tree-child: " + isTreeChild(graph)));

            statusFlowPane.getChildren().add(newText("temporal: " + isTemporal(graph)));
        }

        for (Object object : statusFlowPane.getChildren()) {
            if (object instanceof Shape) {
                ((Shape) object).prefWidth(30);
            }
        }
    }

    public static boolean isRootedDAG(Graph graph) {
        if (findRoot(graph) == null)
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
        for (Node v : graph.nodes()) {
            if (v.getOutDegree() == 0 && (graph.getLabel(v) == null || (graph.getLabel(v).length() == 0)))
                return false;
        }
        return true;
    }

    public static Node findRoot(Graph graph) {
        for (Node v : graph.nodes()) {
            if (v.getInDegree() == 0)
                return v;
        }
        return null;
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
     * determines all stable nodes, which are nodes that lie on all paths to all of their children
     *
     * @param graph
     * @return
     */
    public static NodeSet allStableInternal(PhyloTree graph) {
        final NodeSet result = new NodeSet(graph);

        if (isRootedDAG(graph)) {
            final Node root = findRoot(graph);
            if (root != null)
                allStableInternalRec(root, new HashSet<>(), new HashSet<>(), result);
        }
        return result;
    }

    /**
     * determines all visible reticulations, which are nodes that have a tree path to a leaf or stable node
     *
     * @param graph
     * @return
     */
    public static NodeSet allVisibleReticulations(PhyloTree graph) {
        final NodeSet result = new NodeSet(graph);
        if (isRootedDAG(graph)) {
            final Node root = findRoot(graph);
            if (root != null)
                allVisibleReticulationsRec(root, allStableInternal(graph), result);
        }
        result.setAll(result.stream().filter(v -> v.getInDegree() > 1).collect(Collectors.toList()));
        return result;
    }

    private static void allVisibleReticulationsRec(Node v, NodeSet stableNodes, NodeSet result) {
        if (stableNodes.contains(v))
            result.add(v);

        for (Node w : v.children()) {
            allVisibleReticulationsRec(w, stableNodes, result);

            if (w.getInDegree() == 1 && (result.contains(w) || w.getOutDegree() == 0)) {
                result.add(v);
            }
        }
    }

    /**
     * recursively determines all stable nodes
     *
     * @param v
     * @param below
     * @param parentsOfBelow
     * @param result
     */
    private static void allStableInternalRec(Node v, Set<Node> below, Set<Node> parentsOfBelow, NodeSet result) {

        if (v.getOutDegree() == 0) {
            below.add(v);
            parentsOfBelow.addAll(Basic.asList(v.parents()));
        } else {
            final Set<Node> belowV = new HashSet<>();
            final Set<Node> parentsOfBelowV = new HashSet<>();

            for (Node w : v.children()) {
                allStableInternalRec(w, belowV, parentsOfBelowV, result);
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

    public static boolean isTemporal(PhyloTree graph) {
        final PhyloTree contractedGraph = new PhyloTree(graph);

        final Set<Edge> reticulateEdges = contractedGraph.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).collect(Collectors.toSet());

        if (reticulateEdges.size() == 0)
            return true;
        else {
            contractedGraph.contractEdges(reticulateEdges);
            return isRootedDAG(contractedGraph);
        }
    }

    public static Text newText(String label) {
        final Text text = new Text(label);
        text.setStyle("-fx-font-size: 18;");
        return text;
    }
}
