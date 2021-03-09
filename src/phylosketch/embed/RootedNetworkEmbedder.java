/*
 * RootedNetworkEmbedder.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.embed;

import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import phylosketch.window.EdgeView;
import phylosketch.window.PhyloView;

import java.util.*;

/**
 * computes an embedding of a rooted network
 */
public class RootedNetworkEmbedder {
    public enum Orientation {leftRight, down, up, rightLeft}

    public static void apply(Pane mainPane, PhyloView view, Orientation orientation) {

        final PhyloTree graph = view.getGraph();

        final NodeArray<Node> reticulation2LSA = new NodeArray<>(graph);
        final NodeArray<List<Node>> node2LSAChildren = new NodeArray<>(graph);
        LSATree.computeLSAOrdering(graph, reticulation2LSA, node2LSAChildren);

        Optional<Node> root = graph.nodeStream().filter(v -> v.getInDegree() == 0).findFirst();
        if (root.isPresent()) {
            graph.setRoot(root.get());
            NodeIntegerArray levels = computeLevels(graph, node2LSAChildren, 1);
            final int maxLevel = Basic.max(levels.values());
            NodeDoubleArray yCoord = computeYCoordinates(graph, node2LSAChildren, graph.getRoot());

            computeCoordinatesCladogramRec(mainPane, view, graph.getRoot(), node2LSAChildren, yCoord, maxLevel, levels);
            computeEdges(view);
        }
    }

    /**
     * recursively compute node coordinates from edge angles:
     *
     * @param v Node
     */
    private static void computeCoordinatesCladogramRec(Pane mainPane, PhyloView view, Node v, NodeArray<List<Node>> node2LSAChildren, NodeDoubleArray yCoord, int maxLevel, NodeIntegerArray levels) {
        view.addNode(v, mainPane, 50 * (maxLevel + 1 - levels.getValue(v)), 50 * yCoord.getValue(v));
        for (Node w : node2LSAChildren.getValue(v)) {
            computeCoordinatesCladogramRec(mainPane, view, w, node2LSAChildren, yCoord, maxLevel, levels);
        }
    }

    /**
     * compute edges
     *
     * @param view
     */
    private static void computeEdges(PhyloView view) {
        for (Edge e : view.getGraph().edges()) {
            final Node v = e.getSource();
            final Node w = e.getTarget();

            final EdgeView edgeView = view.addEdge(e);
            final CubicCurve curve = edgeView.getCurve();

            if (view.getX(v) == view.getX(w) || view.getY(v) == view.getY(w)) {
                curve.setControlX1(0.7 * view.getX(v) + 0.3 * view.getX(w));
                curve.setControlX2(0.3 * view.getX(v) + 0.7 * view.getX(w));
                curve.setControlY1(0.7 * view.getY(v) + 0.3 * view.getY(w));
                curve.setControlY2(0.3 * view.getY(v) + 0.7 * view.getY(w));
            } else {
                curve.setControlX1(view.getX(v));
                curve.setControlY1(view.getY(w));
                curve.setControlX2(view.getX(v));
                curve.setControlY2(view.getY(w));
            }
        }
    }

    /**
     * compute the levels in the tree or network (max number of edges from node to a leaf)
     *
     * @param add
     * @return levels
     */
    private static NodeIntegerArray computeLevels(PhyloTree graph, NodeArray<List<Node>> node2GuideTreeChildren, int add) {
        NodeIntegerArray levels = new NodeIntegerArray(graph);
        computeLevelsRec(graph, node2GuideTreeChildren, graph.getRoot(), levels, add, new HashSet<>());
        return levels;
    }

    /**
     * compute node levels
     *
     * @param v
     * @param levels
     * @param add
     * @return max height
     */
    private static void computeLevelsRec(PhyloTree graph, NodeArray<List<Node>> node2GuideTreeChildren, Node v, NodeIntegerArray levels, int add, Set<Node> path) {
        path.add(v);
        int level = 0;
        Set<Node> below = new HashSet<>();
        for (Edge f = v.getFirstOutEdge(); f != null; f = v.getNextOutEdge(f)) {
            Node w = f.getTarget();
            below.add(w);
            if (levels.getValue(w) == null)
                computeLevelsRec(graph, node2GuideTreeChildren, w, levels, add, path);
            level = Math.max(level, levels.get(w) + (graph.isTransferEdge(f) ? 0 : add));
        }
        final Collection<Node> lsaChildren = node2GuideTreeChildren.getValue(v);
        if (lsaChildren != null) {
            for (Node w : lsaChildren) {
                if (!below.contains(w) && !path.contains(w)) {
                    if (levels.getValue(w) == null)
                        computeLevelsRec(graph, node2GuideTreeChildren, w, levels, add, path);
                    level = Math.max(level, levels.get(w) + add);
                }
            }
        }
        levels.set(v, level);
        path.remove(v);
    }

    /**
     * compute the y-coordinates for the parallel view
     *
     * @param root
     * @return y-coordinates
     */
    public static NodeDoubleArray computeYCoordinates(PhyloTree graph, NodeArray<List<Node>> node2LSAChildren, Node root) {
        final NodeDoubleArray yCoord = new NodeDoubleArray(graph);
        final List<Node> leafOrder = new ArrayList<>();
        computeYCoordinateOfLeavesRec(root, node2LSAChildren, 0, yCoord, leafOrder);
        if (graph.getSpecialEdges().size() > 0)
            fixSpacing(leafOrder, yCoord);
        computeYCoordinateOfInternalRec(root, node2LSAChildren, yCoord);
        return yCoord;
    }

    /**
     * recursively compute the y coordinate for a parallel or triangular diagram
     *
     * @param v
     * @param leafNumber rank of leaf in vertical ordering
     * @return index of last leaf
     */
    private static int computeYCoordinateOfLeavesRec(Node v, NodeArray<List<Node>> node2LSAChildren, int leafNumber, NodeDoubleArray yCoord, List<Node> nodeOrder) {
        List<Node> list = node2LSAChildren.getValue(v);

        if (list.size() == 0) {
            // String taxonName = tree.getLabel(v);
            yCoord.put(v, (double) ++leafNumber);
            nodeOrder.add(v);
        } else {
            for (Node w : list) {
                leafNumber = computeYCoordinateOfLeavesRec(w, node2LSAChildren, leafNumber, yCoord, nodeOrder);
            }
        }
        return leafNumber;
    }


    /**
     * recursively compute the y coordinate for the internal nodes of a parallel diagram
     *
     * @param v
     * @param yCoord
     */
    private static void computeYCoordinateOfInternalRec(Node v, NodeArray<List<Node>> node2LSAChildren, NodeDoubleArray yCoord) {
        if (v.getOutDegree() > 0) {
            double first = Double.NEGATIVE_INFINITY;
            double last = Double.NEGATIVE_INFINITY;

            for (Node w : node2LSAChildren.getValue(v)) {
                double y = yCoord.get(w);
                if (y == 0) {
                    computeYCoordinateOfInternalRec(w, node2LSAChildren, yCoord);
                    y = yCoord.get(w);
                }
                last = y;
                if (first == Double.NEGATIVE_INFINITY)
                    first = last;
            }
            yCoord.put(v, 0.5 * (last + first));
        }
    }

    /**
     * fix spacing so that space between any two true leaves is 1
     *
     * @param leafOrder
     */
    private static void fixSpacing(List<Node> leafOrder, NodeDoubleArray yCoord) {
        final Node[] nodes = leafOrder.toArray(new Node[0]);
        double leafPos = 0;
        for (int lastLeaf = -1; lastLeaf < nodes.length; ) {
            int nextLeaf = lastLeaf + 1;
            while (nextLeaf < nodes.length && nodes[nextLeaf].getOutDegree() > 0)
                nextLeaf++;
            // assign fractional positions to intermediate nodes
            int count = (nextLeaf - lastLeaf) - 1;
            if (count > 0) {
                double add = 1.0 / (count + 1); // if odd, use +2 to avoid the middle
                double value = leafPos;
                for (int i = lastLeaf + 1; i < nextLeaf; i++) {
                    value += add;
                    yCoord.put(nodes[i], value);
                }
            }
            // assign whole positions to actual leaves:
            if (nextLeaf < nodes.length) {
                yCoord.put(nodes[nextLeaf], ++leafPos);
            }
            lastLeaf = nextLeaf;
        }
    }
}
