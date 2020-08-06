/*
 * Normalize.java Copyright (C) 2020. Daniel H. Huson
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
import jloda.util.Pair;
import phylosketch.commands.ChangeEdgeShapeCommand;
import phylosketch.util.NewWindow;
import phylosketch.window.MainWindow;
import phylosketch.window.NetworkProperties;
import phylosketch.window.PhyloView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * computes the normalization of a rooted phylogenetic network
 * Daniel Huson, 3.2020
 */
public class Normalize {
    static public void apply(PhyloView phyloView, PhyloTree target, NodeArray<Point2D> coordinates) {
        final PhyloTree source = phyloView.getGraph();

        final Set<Node> visibleAndLeaves = Basic.asSet(NetworkProperties.allVisibleNodes(source));
        visibleAndLeaves.addAll(source.nodeStream().filter(v -> v.getOutDegree() == 0).collect(Collectors.toList()));

        final NodeArray<Node> src2tar = new NodeArray<>(source);
        final NodeArray<Node> tar2src = new NodeArray<>(target);

        final List<String> srcNodeLabels = source.nodeStream().map(source::getLabel).filter(s -> s != null && s.length() > 0).collect(Collectors.toList());

        final Optional<Node> sourceRoot = source.nodeStream().filter(v -> v.getInDegree() == 0).findAny();
        if (sourceRoot.isEmpty())
            return;

        // setup new nodes:
        for (Node s : visibleAndLeaves) {
            final Node t = target.newNode();
            src2tar.put(s, t);
            tar2src.put(t, s);
            if (sourceRoot.get() == s)
                target.setRoot(t);
            coordinates.put(t, new Point2D(phyloView.getNodeView(s).getTranslateX(), phyloView.getNodeView(s).getTranslateY()));
            target.setLabel(t, source.getLabel(s));
        }

        final NodeArray<Collection<Node>> visibleBelow = new NodeArray<>(source);

        allBelowRec(sourceRoot.get(), visibleAndLeaves, visibleBelow);

        // create full graph:
        for (Node vs : visibleAndLeaves) {
            final Node vt = src2tar.get(vs);
            for (Node ws : visibleBelow.get(vs)) {
                final Node wt = src2tar.get(ws);
                target.newEdge(vt, wt);
            }
        }

        // transitive reduction:
        final Collection<Edge> edgesToDelete = new HashSet<>();
        for (Node x : target.nodes()) {
            for (Node z : x.children()) {
                for (Node y : x.children()) {
                    if (y.isChild(z)) {
                        edgesToDelete.add(x.getCommonEdge(z));
                        break;
                    }
                }
            }
        }
        edgesToDelete.forEach(target::deleteEdge);

        // remove digons:

        final Set<Node> nodesToRemove = target.nodeStream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1).collect(Collectors.toSet());

        for (Node v : nodesToRemove) {
            target.delDivertex(v);
        }

        final Set<String> targetNodeLabels = target.nodeStream().map(target::getLabel).filter(s -> s != null && s.length() > 0).collect(Collectors.toSet());
        final long lost = srcNodeLabels.stream().filter(s -> !targetNodeLabels.contains(s)).count();
        if (lost > 0)
            NotificationManager.showWarning("Number of labeled internal nodes removed: " + lost);
    }

    /**
     * collect all visible nodes below a given node
     *
     * @param v
     * @param visible
     * @param visibleBelow
     * @return all below
     */
    private static Set<Node> allBelowRec(Node v, Set<Node> visible, NodeArray<Collection<Node>> visibleBelow) {
        final Set<Node> set = new HashSet<>();

        for (Node w : v.children()) {
            set.addAll(allBelowRec(w, visible, visibleBelow));
            if (visible.contains(w))
                set.add(w);
        }
        visibleBelow.put(v, set);
        return set;
    }

    public static void runNormalize(MainWindow mainWindow) {
        final MainWindow newWindow = NewWindow.apply();

        newWindow.getView().setFileName(Basic.replaceFileSuffix(mainWindow.getView().getFileName(), "-normalized.sptree5"));

        final AService<Pair<PhyloTree, NodeArray<Point2D>>> service = new AService<>(newWindow.getController().getStatusFlowPane());

        service.setCallable(() -> {
            final PhyloTree phyloTree = new PhyloTree();
            final NodeArray<Point2D> coordinates = new NodeArray<>(phyloTree);
            Normalize.apply(mainWindow.getView(), phyloTree, coordinates);
            return new Pair<>(phyloTree, coordinates);
        });

        service.setOnFailed(c -> {
            NotificationManager.showError("Normalization failed: " + service.getException());
        });

        service.setOnSucceeded(c -> {
            final PhyloView view = newWindow.getView();
            final PhyloTree graph = service.getValue().getFirst();
            final NodeArray<Point2D> coordinates = service.getValue().getSecond();

            final NodeArray<Node> old2new = new NodeArray<>(graph);
            view.getGraph().copy(graph, old2new, null);

            graph.nodes().forEach(v -> view.addNode(old2new.get(v), newWindow.getController().getContentPane(), coordinates.get(v).getX(), coordinates.get(v).getY()));
            view.getGraph().edges().forEach(view::addEdge);

            view.getUndoManager().doAndAdd(new ChangeEdgeShapeCommand(view, Basic.asList(view.getGraph().edges()), ChangeEdgeShapeCommand.EdgeShape.Reshape));

            view.setDirty(true);
        });

        service.start();
    }
}
