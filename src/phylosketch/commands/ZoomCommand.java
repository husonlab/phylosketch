/*
 * ZoomCommand.java Copyright (C) 2020. Daniel H. Huson
 *
 * (Some code written by other authors, as named in code.)
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

package phylosketch.commands;

import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.Triplet;
import phylosketch.window.NodeView;
import phylosketch.window.PhyloView;

import java.util.ArrayList;

/**
 * zoom command
 */
public class ZoomCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;

    private final ArrayList<Triplet<Integer, Double, Double>> oldNodes = new ArrayList<>();
    private final ArrayList<Triplet<Integer, Double, Double>> newNodes = new ArrayList<>();

    private final ArrayList<Pair<Integer, double[]>> oldEdges = new ArrayList<>();
    private final ArrayList<Pair<Integer, double[]>> newEdges = new ArrayList<>();

    public ZoomCommand(double zoomFactorX, double zoomFactorY, Pane mainPane, PhyloView editor) {
        super("Zoom");

        final PhyloTree graph = editor.getGraph();

        for (Node v : graph.nodes()) {
            oldNodes.add(new Triplet<>(v.getId(), editor.getNodeView(v).getTranslateX(), editor.getNodeView(v).getTranslateY()));
            newNodes.add(new Triplet<>(v.getId(), zoomFactorX * editor.getNodeView(v).getTranslateX(), zoomFactorY * editor.getNodeView(v).getTranslateY()));
        }
        undo = () -> {
            oldNodes.forEach(t -> {
                final NodeView nodeView = editor.getNodeView(graph.searchNodeId(t.getFirst()));
                nodeView.setTranslateX(t.getSecond());
                nodeView.setTranslateY(t.getThird());
            });
            if (mainPane.getChildren().size() > 0 && mainPane.getChildren().get(0) instanceof ImageView) {
                ImageView imageView = (ImageView) mainPane.getChildren().get(0);
                imageView.setFitWidth(1 / zoomFactorX * imageView.getFitWidth());
                imageView.setFitHeight(1 / zoomFactorY * imageView.getFitHeight());
            }
        };

        redo = () ->
                newNodes.forEach(t -> {
                    final NodeView nodeView = editor.getNodeView(graph.searchNodeId(t.getFirst()));
                    nodeView.setTranslateX(t.getSecond());
                    nodeView.setTranslateY(t.getThird());
                });
        if (mainPane.getChildren().size() > 0 && mainPane.getChildren().get(0) instanceof ImageView) {
            ImageView imageView = (ImageView) mainPane.getChildren().get(0);
            imageView.setFitWidth(zoomFactorX * imageView.getFitWidth());
            imageView.setFitHeight(zoomFactorY * imageView.getFitHeight());
        }
    }

    @Override
    public void undo() {
        undo.run();
    }

    @Override
    public void redo() {
        redo.run();
    }

    public static void zoom(double zoomFactorX, double zoomFactorY, Pane mainPane, PhyloView editor) {
        if (mainPane.getChildren().size() > 0 && mainPane.getChildren().get(0) instanceof ImageView) {
            ImageView imageView = (ImageView) mainPane.getChildren().get(0);
            //imageView.setFitWidth(zoomFactorX*imageView.getFitWidth());
            //imageView.setFitHeight(zoomFactorY*imageView.getFitHeight());
        }

        editor.getGraph().nodes().forEach(v -> {
            final NodeView nodeView = editor.getNodeView(v);
            nodeView.setTranslateX(zoomFactorX * nodeView.getTranslateX());
            nodeView.setTranslateY(zoomFactorY * nodeView.getTranslateY());
        });
        editor.getGraph().edges().forEach(e -> {
            final CubicCurve cubicCurve = editor.getEdgeView(e).getCurve();
            cubicCurve.setControlX1(zoomFactorX * cubicCurve.getControlX1());
            cubicCurve.setControlY1(zoomFactorY * cubicCurve.getControlY1());
            cubicCurve.setControlX2(zoomFactorX * cubicCurve.getControlX2());
            cubicCurve.setControlY2(zoomFactorY * cubicCurve.getControlY2());
        });
    }

}
