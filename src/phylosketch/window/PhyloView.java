/*
 *  Copyright (C) 2018. Daniel H. Huson
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

/*
 *  Copyright (C) 2018. Daniel H. Huson
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

/*
 *  Copyright (C) 2018. Daniel H. Huson
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

package phylosketch.window;

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import jloda.fx.control.ItemSelectionModel;
import jloda.fx.graph.GraphFX;
import jloda.fx.shapes.NodeShape;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.SelectionEffect;
import jloda.graph.*;
import jloda.phylo.PhyloTree;
import jloda.util.Single;
import phylosketch.commands.MoveNodeLabelCommand;
import phylosketch.commands.MoveSelectedNodesCommand;
import phylosketch.commands.NewEdgeAndNodeCommand;
import phylosketch.util.EdgeContextMenu;
import phylosketch.util.NodeLabelDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * editor
 * Daniel Huson, 1.2020
 */
public class PhyloView {
    private final StringProperty fileName = new SimpleStringProperty("");

    private final PhyloTree graph = new PhyloTree();
    private final GraphFX<PhyloTree> graphFX = new GraphFX<>(graph);
    private final UndoManager undoManager = new UndoManager();

    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    public static final Font DefaultFont = Font.font("Arial", 12);
    private final ObjectProperty<Font> font = new SimpleObjectProperty<>(DefaultFont);

    private final MainWindow window;
    //private final NodeArray<Pair<Shape, Label>> node2shapeAndLabel;
    private final NodeArray<NodeView> node2view;
    private final EdgeArray<EdgeView> edge2view;

    private final ItemSelectionModel<Node> nodeSelection = new ItemSelectionModel<>();
    private final ItemSelectionModel<Edge> edgeSelection = new ItemSelectionModel<>();

    private final Group world;
    private final Group graphNodes;
    private final Group graphNodeLabels;
    private final Group graphEdges;

    /**
     * constructor
     */
    public PhyloView(MainWindow window) {
        this.window = window;

        graphNodes = new Group();
        graphNodeLabels = new Group();
        graphEdges = new Group();
        Group graphEdgeLabels = new Group();

        this.world = new Group(graphEdges, graphNodes, graphEdgeLabels, graphNodeLabels);

        node2view = new NodeArray<>(graph);
        edge2view = new EdgeArray<>(graph);

        nodeSelection.getSelectedItems().addListener((ListChangeListener<Node>) (e) -> {
            while (e.next()) {
                for (Node v : e.getAddedSubList()) {
                    try {
                        getNodeView(v).getShapeGroup().setEffect(SelectionEffect.getInstance());
                        getLabel(v).setEffect(SelectionEffect.getInstance());
                    } catch (NotOwnerException ignored) {
                    }

                }
                for (Node v : e.getRemoved()) {
                    try {
                        getNodeView(v).getShapeGroup().setEffect(null);
                        getLabel(v).setEffect(null);
                    } catch (NotOwnerException ignored) {
                    }
                }
            }
        });

        edgeSelection.getSelectedItems().addListener((ListChangeListener<Edge>) (e) -> {
            while (e.next()) {
                for (Edge edge : e.getAddedSubList()) {
                    try {
                        final EdgeView edgeView = edge2view.get(edge);
                        if (edgeView != null) {
                            for (javafx.scene.Node node : edgeView.getChildren())
                                node.setEffect(SelectionEffect.getInstance());
                        }
                    } catch (NotOwnerException ignored) {
                    }
                }
                for (Edge edge : e.getRemoved()) {
                    try {
                        final EdgeView edgeView = edge2view.get(edge);
                        if (edgeView != null) {
                            for (javafx.scene.Node node : edgeView.getChildren())
                                node.setEffect(null);
                        }
                    } catch (NotOwnerException ignored) {
                    }
                }
            }
        });

        graphFX.getNodeList().addListener((ListChangeListener<Node>) c -> {
            while (c.next()) {
                nodeSelection.getSelectedItems().removeAll(c.getRemoved());
            }
        });

        graphFX.getEdgeList().addListener((ListChangeListener<Edge>) c -> {
            while (c.next()) {
                edgeSelection.getSelectedItems().removeAll(c.getRemoved());
            }
        });

        undoManager.undoableProperty().addListener(c -> dirty.set(true));
    }

    public NodeView addNode(Node v, Pane pane, double x, double y) {
        final NodeView nodeView = new NodeView(getFont(), x, y);

        graphNodes.getChildren().add(nodeView.getShapeGroup());
        node2view.put(v, nodeView);
        setupMouseInteraction(pane, v);

        if (graph.getLabel(v) != null)
            nodeView.getLabel().setText(graph.getLabel(v));
        nodeView.getLabel().textProperty().addListener((c, o, n) -> graph.setLabel(v, n));
        graphNodeLabels.getChildren().add(nodeView.getLabel());

        nodeView.getShapeGroup().setOnContextMenuRequested(c -> {
            final MenuItem setLabel = new MenuItem("Set Label");
            setLabel.setOnAction((e) -> NodeLabelDialog.apply(window.getStage(), this, v));
            new ContextMenu(setLabel, new SeparatorMenuItem()).show(window.getStage(), c.getScreenX(), c.getScreenY());
        });
        return nodeView;
    }

    public void changeNodeShape(Node v, NodeShape nodeShape) {
        node2view.get(v).changeShape(nodeShape);
    }

    public void removeNode(Node v) {
        for (Edge e : v.adjacentEdges()) {
            final EdgeView edgeView = getEdgeView(e);
            if (edgeView != null)
                graphEdges.getChildren().removeAll(edgeView.getChildren());
        }
        final NodeView nodeView = node2view.get(v);
        if (nodeView != null) {
            graphNodes.getChildren().remove(nodeView.getShapeGroup());
            graphNodeLabels.getChildren().remove(nodeView.getLabel());
        }
    }

    public EdgeView addEdge(Edge e) {
        final NodeView sourceView = node2view.get(e.getSource());
        final NodeView targetView = node2view.get(e.getTarget());

        final EdgeView edgeView = new EdgeView(this, e, sourceView.translateXProperty(), sourceView.translateYProperty(), targetView.translateXProperty(), targetView.translateYProperty());
        edge2view.setValue(e, edgeView);

        EdgeContextMenu.setup(window.getController().getMainPane(), this, e);

        graphEdges.getChildren().addAll(edgeView.getChildren());
        return edgeView;
    }

    public void removeEdge(Edge e) {
        graphEdges.getChildren().removeAll(getEdgeView(e).getChildren());
    }

    enum What {moveNode, growEdge}

    /**
     * setup mouse interaction
     */
    private void setupMouseInteraction(Pane pane, Node v) {
        final NodeView nodeView = getNodeView(v);

        nodeView.getShapeGroup().setCursor(Cursor.CROSSHAIR);

        final double[] mouseDownPosition = new double[2];
        final double[] previousMousePosition = new double[2];
        final Map<Integer, double[]> oldControlPointLocations = new HashMap<>();
        final Map<Integer, double[]> newControlPointLocations = new HashMap<>();

        final Single<Boolean> moved = new Single<>(false);
        final Single<What> what = new Single<>(null);
        final Single<Node> target = new Single<>(null);
        final Line line = new Line();

        world.getChildren().add(line);

        nodeView.getShapeGroup().setOnMousePressed(c -> {
            mouseDownPosition[0] = previousMousePosition[0] = c.getSceneX();
            mouseDownPosition[1] = previousMousePosition[1] = c.getSceneY();
            moved.set(false);

            oldControlPointLocations.clear();
            newControlPointLocations.clear();

            what.set(c.isShiftDown() ? What.growEdge : What.moveNode);
            if (what.get() == What.growEdge) {
                line.setStartX(nodeView.getTranslateX());
                line.setStartY(nodeView.getTranslateY());
                nodeView.getShapeGroup().setCursor(Cursor.CLOSED_HAND);

                final Point2D location = pane.sceneToLocal(previousMousePosition[0], previousMousePosition[1]);

                line.setEndX(location.getX());
                line.setEndY(location.getY());
                line.setVisible(true);
                target.set(null);
            }
            c.consume();
        });

        nodeView.getShapeGroup().setOnMouseDragged(c -> {
            final double mouseX = c.getSceneX();
            final double mouseY = c.getSceneY();

            if (what.get() == What.moveNode) {
                getNodeSelection().select(v);
                final double deltaX = (mouseX - previousMousePosition[0]);
                final double deltaY = (mouseY - previousMousePosition[1]);

                for (Node u : getNodeSelection().getSelectedItems()) {
                    {
                            final double deltaXReshapeEdge = (mouseX - previousMousePosition[0]);
                            final double deltaYReshapeEdge = (mouseY - previousMousePosition[1]);

                            for (Edge e : u.outEdges()) {
                                final EdgeView edgeView = edge2view.get(e);

                                if (!oldControlPointLocations.containsKey(e.getId())) {
                                    oldControlPointLocations.put(e.getId(), edgeView.getControlCoordinates());
                                }
                                edgeView.startMoved(deltaXReshapeEdge, deltaYReshapeEdge);
                                newControlPointLocations.put(e.getId(), edgeView.getControlCoordinates());
                            }
                        for (Edge e : u.inEdges()) {
                            final EdgeView edgeView = edge2view.get(e);
                            if (!oldControlPointLocations.containsKey(e.getId())) {
                                oldControlPointLocations.put(e.getId(), edgeView.getControlCoordinates());
                            }
                            edgeView.endMoved(deltaXReshapeEdge, deltaYReshapeEdge);
                            newControlPointLocations.put(e.getId(), edgeView.getControlCoordinates());
                        }
                    }

                    final NodeView nodeViewU = getNodeView(u);
                    nodeViewU.setTranslateX(nodeViewU.getTranslateX() + deltaX);
                    nodeViewU.setTranslateY(nodeViewU.getTranslateY() + deltaY);
                }
            }
            if (what.get() == What.growEdge) {
                getNodeSelection().clearSelection();
                getNodeSelection().select(v);

                final Point2D location = pane.sceneToLocal(mouseX, mouseY);
                line.setEndX(location.getX());
                line.setEndY(location.getY());

                final Node w = findNodeIfHit(c.getScreenX(), c.getScreenY());
                if ((w == null || w == v || w != target.get()) && target.get() != null) {
                    getNodeView(target.get()).getShape().setFill(Color.WHITE);
                    target.set(null);
                }
                if (w != null && w != v && w != target.get()) {
                    target.set(w);
                    getNodeView(target.get()).getShape().setFill(Color.GRAY);
                }
            }

            moved.set(true);
            previousMousePosition[0] = c.getSceneX();
            previousMousePosition[1] = c.getSceneY();
            c.consume();
        });

        nodeView.getShapeGroup().setOnMouseReleased(c -> {
            if (!moved.get()) {
                if (!c.isShiftDown()) {
                    nodeSelection.clearSelection();
                    edgeSelection.clearSelection();
                    nodeSelection.select(v);
                } else {
                    if (nodeSelection.getSelectedItems().contains(v))
                        nodeSelection.clearSelection(v);
                    else
                        nodeSelection.select(v);
                }
            } else {
                if (what.get() == What.moveNode) {
                    // yes, add, not doAndAdd()
                    final double dx = previousMousePosition[0] - mouseDownPosition[0];
                    final double dy = previousMousePosition[1] - mouseDownPosition[1];
                    undoManager.add(new MoveSelectedNodesCommand(dx, dy, this,
                            nodeSelection.getSelectedItems(), oldControlPointLocations, newControlPointLocations));

                } else if (what.get() == What.growEdge) {
                    if (target.get() != null)
                        getNodeView(target.get()).getShape().setFill(Color.WHITE);

                    final double x = line.getEndX();
                    final double y = line.getEndY();

                    final Node w = findNodeIfHit(c.getScreenX(), c.getScreenY());
                    undoManager.doAndAdd(new NewEdgeAndNodeCommand(pane, this, v, w, x, y));
                    nodeView.getShapeGroup().setCursor(Cursor.CROSSHAIR);
                }
                moved.set(false);
            }
            line.setVisible(false);
        });

        nodeView.getLabel().setOnMousePressed(c -> {
            mouseDownPosition[0] = previousMousePosition[0] = c.getSceneX();
            mouseDownPosition[1] = previousMousePosition[1] = c.getSceneY();
            moved.set(false);
        });

        nodeView.getLabel().setOnMouseDragged(c -> {
            moved.set(true);

            final double mouseX = c.getSceneX();
            final double mouseY = c.getSceneY();

            final Label label = nodeView.getLabel();
            label.setLayoutX(label.getLayoutX() + (mouseX - previousMousePosition[0]));
            label.setLayoutY(label.getLayoutY() + (mouseY - previousMousePosition[1]));

            previousMousePosition[0] = mouseX;
            previousMousePosition[1] = mouseY;
            c.consume();
        });

        nodeView.getLabel().setOnMouseReleased(c -> {
            if (!moved.get()) {
                if (!c.isShiftDown()) {
                    nodeSelection.clearSelection();
                    edgeSelection.clearSelection();
                    nodeSelection.select(v);
                } else {
                    if (nodeSelection.getSelectedItems().contains(v))
                        nodeSelection.clearSelection(v);
                    else
                        nodeSelection.select(v);
                }
            } else {
                final double mouseX = c.getSceneX();
                final double mouseY = c.getSceneY();
                undoManager.add(new MoveNodeLabelCommand(this, v, mouseX - mouseDownPosition[0], mouseY - mouseDownPosition[1]));
            }
        });
    }

    public void moveNode(Node v, double x, double y) {
        final NodeView nodeView = node2view.get(v);
        nodeView.setTranslateX(nodeView.getTranslateX() + x);
        nodeView.setTranslateY(nodeView.getTranslateY() + y);
    }

    private Node findNodeIfHit(double x, double y) {
        for (Node v : graph.nodes()) {
            final Shape shape = node2view.get(v).getShape();
            if (shape.contains(shape.screenToLocal(x, y)))
                return v;
        }
        return null;
    }

    public Font getFont() {
        return font.get();
    }

    public ObjectProperty<Font> fontProperty() {
        return font;
    }

    public void setFont(Font font) {
        this.font.set(font);
    }

    public Group getWorld() {
        return world;
    }

    public PhyloTree getGraph() {
        return graph;
    }

    public ItemSelectionModel<Node> getNodeSelection() {
        return nodeSelection;
    }

    public NodeArray<NodeView> getNode2View() {
        return node2view;
    }

    public NodeView getNodeView(Node v) {
        return node2view.get(v);
    }

    public double getX(Node v) {
        return getNodeView(v).getTranslateX();
    }

    public double getY(Node v) {
        return getNodeView(v).getTranslateY();
    }

    public Label getLabel(Node v) {
        return node2view.get(v).getLabel();
    }

    public EdgeArray<EdgeView> getEdge2view() {
        return edge2view;
    }

    public EdgeView getEdgeView(Edge e) {
        return edge2view.get(e);
    }

    public CubicCurve getCurve(Edge e) {
        return edge2view.get(e).getCurve();
    }

    public ItemSelectionModel<Edge> getEdgeSelection() {
        return edgeSelection;
    }

    public GraphFX<PhyloTree> getGraphFX() {
        return graphFX;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public String getFileName() {
        return fileName.get();
    }

    public StringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty.set(dirty);
    }
}
