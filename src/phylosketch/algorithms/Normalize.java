/*
 * Normalize.java Copyright (C) 2021. Daniel H. Huson
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
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.IteratorUtils;
import jloda.util.Pair;
import phylosketch.commands.ChangeEdgeShapeCommand;
import phylosketch.util.NewWindow;
import phylosketch.window.MainWindow;
import phylosketch.window.NetworkProperties;
import phylosketch.window.PhyloView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * computes the normalization of a rooted phylogenetic network
 * Daniel Huson, 3.2020
 */
public class Normalize {
    static public void apply(PhyloView phyloView, PhyloTree target, NodeArray<Point2D> coordinates) {
        var start = System.currentTimeMillis();
        var source = phyloView.getGraph();

        var visibleAndLeaves = NetworkProperties.computeAllVisibleNodes(source);
        visibleAndLeaves.addAll(source.nodeStream().filter(v -> v.getOutDegree() == 0).collect(Collectors.toList()));

        final NodeArray<Node> src2tar = new NodeArray<>(source);

        var sourceRoot = source.nodeStream().filter(v -> v.getInDegree() == 0).findAny();
        if (sourceRoot.isEmpty())
            return;

        // setup new nodes:
        for (var s : visibleAndLeaves) {
            var t = target.newNode();
            src2tar.put(s, t);
            if (sourceRoot.get() == s)
                target.setRoot(t);
            coordinates.put(t, new Point2D(phyloView.getNodeView(s).getTranslateX(), phyloView.getNodeView(s).getTranslateY()));
            target.setLabel(t, source.getLabel(s));
        }

        final NodeArray<Collection<Node>> visibleAndLeavesBelow = source.newNodeArray();

        computeAllBelowRec(sourceRoot.get(), visibleAndLeaves, visibleAndLeavesBelow);

        // create full graph:
        for (var vs : visibleAndLeaves) {
            var vt = src2tar.get(vs);
            for (var ws : visibleAndLeavesBelow.get(vs)) {
                var wt = src2tar.get(ws);
                target.newEdge(vt, wt);
            }
        }


        if (true) {
            NodeArray<Set<Node>> parents = target.newNodeArray();
            for (var v : target.nodes()) {
                parents.put(v, IteratorUtils.asSet(v.parents()));
            }

            for (var e : target.edges()) {
                for (var f : e.getTarget().inEdges()) {
                    if (f != e && parents.get(f.getSource()).contains(e.getSource())) {
                        target.deleteEdge(e);
                        break;
                    }
                }
            }
            //var edgesToDelete = target.edgeStream().filter(isReducible).collect(Collectors.toList());
            //edgesToDelete.forEach(target::deleteEdge);
        } else {
            // transitive reduction:
            var edgesToDelete = new HashSet<Edge>();
            for (var x : target.nodes()) {
                for (var z : x.children()) {
                    for (var y : x.children()) {
                        if (y.isChild(z)) {
                            edgesToDelete.add(x.getCommonEdge(z));
                            break;
                        }
                    }
                }
            }
            edgesToDelete.forEach(target::deleteEdge);
        }

        // remove digons:

        var nodesToRemove = target.nodeStream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1).collect(Collectors.toSet());

        for (var v : nodesToRemove) {
            target.delDivertex(v);
        }

        var srcNodeLabels = source.nodeStream().map(source::getLabel).filter(s -> s != null && s.length() > 0).collect(Collectors.toList());
        var targetNodeLabels = target.nodeStream().map(target::getLabel).filter(s -> s != null && s.length() > 0).collect(Collectors.toSet());

        var lost = srcNodeLabels.stream().filter(s -> !targetNodeLabels.contains(s)).count();
        if (lost > 0)
            NotificationManager.showInformation("Number of labeled internal nodes removed: " + lost);

        var finish = System.currentTimeMillis();

        var sourceReticulations = source.nodeStream().filter(v -> v.getInDegree() > 1).count();
        var targetReticulations = target.nodeStream().filter(v -> v.getInDegree() > 1).count();

        System.err.printf("Network with %,d nodes, %,d edges and %,d reticulations -> normalization with %,d nodes, %,d edges and %,d reticulations (time: %ds)%n",
                source.getNumberOfNodes(), source.getNumberOfEdges(), sourceReticulations, target.getNumberOfNodes(), target.getNumberOfEdges(), targetReticulations,
                ((finish - start) / 1000));
    }

    /**
     * collect all visible nodes below a given node
     *
     * @param v            the current node
     * @param visible      the set of all visible or leaf nodes
     * @param visibleBelow the mapping of v to all below
     * @return the set of all below v
     */
    private static Set<Node> computeAllBelowRec(Node v, Set<Node> visible, NodeArray<Collection<Node>> visibleBelow) {
        var set = new HashSet<Node>();

        for (var w : v.children()) {
            set.addAll(computeAllBelowRec(w, visible, visibleBelow));
            if (visible.contains(w))
                set.add(w);
        }
        visibleBelow.put(v, set);
        return set;
    }

    public static void runNormalize(MainWindow mainWindow) {
        var newWindow = NewWindow.apply();

        newWindow.getView().setFileName(Basic.replaceFileSuffix(mainWindow.getView().getFileName(), "-normalized.sptree5"));

        final AService<Pair<PhyloTree, NodeArray<Point2D>>> service = new AService<>(newWindow.getController().getStatusFlowPane());

        service.setCallable(() -> {
            var phyloTree = new PhyloTree();
            var coordinates = new NodeArray<Point2D>(phyloTree);
            Normalize.apply(mainWindow.getView(), phyloTree, coordinates);
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

            view.getUndoManager().doAndAdd(new ChangeEdgeShapeCommand(view, Basic.asList(view.getGraph().edges()), ChangeEdgeShapeCommand.EdgeShape.Reshape));

            view.setDirty(true);
        });

        service.start();
    }
}
