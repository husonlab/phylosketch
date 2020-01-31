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

package phyloedit.window;

import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import jloda.fx.control.ItemSelectionModel;
import jloda.fx.graph.GraphFX;
import jloda.fx.shapes.CircleShape;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.SelectionEffect;
import jloda.graph.Edge;
import jloda.graph.EdgeArray;
import jloda.graph.Node;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import jloda.util.Single;
import phyloedit.actions.MoveSelectedNodesCommand;
import phyloedit.actions.NewEdgeAndNodeCommand;
import phyloedit.actions.NodeLabelDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * editor
 * Daniel Huson, 1.2020
 */
public class PhyloEditor {
    public enum EdgeType {treeEdge, reticulateEdge}

    private final StringProperty fileName = new SimpleStringProperty("");

    private final PhyloTree graph = new PhyloTree();
    private final GraphFX<PhyloTree> graphFX = new GraphFX<>(graph);
    private final UndoManager undoManager = new UndoManager();

    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    private final ObjectProperty<Font> font = new SimpleObjectProperty<>(Font.font("Helvetica", 12));

    private final PhyloEditorWindow window;
    private final NodeArray<Pair<Shape, Label>> node2shapeAndLabel;
    private final EdgeArray<EdgeView> edge2view;

    private final ItemSelectionModel<Node> nodeSelection = new ItemSelectionModel<>();
    private final ItemSelectionModel<Edge> edgeSelection = new ItemSelectionModel<>();

    private final Group world;
    private final Group spacers;
    private final Group graphNodes;
    private final Group graphNodeLabels;
    private final Group graphEdges;
    private final Group graphEdgeLabels;

    /**
     * constructor
     */
    public PhyloEditor(PhyloEditorWindow window) {
        this.window = window;

        spacers = new Group();
        graphNodes = new Group();
        graphNodeLabels = new Group();
        graphEdges = new Group();
        graphEdgeLabels = new Group();

        this.world = new Group(spacers, graphEdges, graphNodes, graphEdgeLabels, graphNodeLabels);

        node2shapeAndLabel = new NodeArray<>(graph);
        edge2view = new EdgeArray<>(graph);

        nodeSelection.getSelectedItems().addListener((ListChangeListener<Node>) (e) -> {
            while (e.next()) {
                for (Node v : e.getAddedSubList()) {
                    if (v.getOwner() != null) {
                        getShape(v).setEffect(SelectionEffect.getInstance());
                        getLabel(v).setEffect(SelectionEffect.getInstance());
                    }

                }
                for (Node v : e.getRemoved()) {
                    if (v.getOwner() != null) {
                        getShape(v).setEffect(null);
                        getLabel(v).setEffect(null);
                    }
                }
            }
        });

        edgeSelection.getSelectedItems().addListener((ListChangeListener<Edge>) (e) -> {
            while (e.next()) {
                for (Edge edge : e.getAddedSubList()) {
                    final EdgeView edgeView = edge2view.get(edge);
                    if (edgeView != null) {
                        for (javafx.scene.Node node : edgeView.getChildren())
                            node.setEffect(SelectionEffect.getInstance());
                    }
                }
                for (Edge edge : e.getRemoved()) {
                    final EdgeView edgeView = edge2view.get(edge);
                    if (edgeView != null) {
                        for (javafx.scene.Node node : edgeView.getChildren())
                            node.setEffect(null);
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

    public void clear() {
        nodeSelection.clearSelection();
        edgeSelection.clearSelection();
        graph.clear();
        //world.getChildren().clear();
    }

    public void addNode(Pane pane, double x, double y, Node v) {
        final Shape shape;
        final Label label;

        shape = new CircleShape(10);
        shape.setStroke(Color.BLACK);
        shape.setFill(Color.WHITE);
        shape.setStrokeWidth(2);

        if (graph.getLabel(v) != null)
            label = new Label(graph.getLabel(v));
        else
            label = new Label();

        label.setFont(getFont());

        shape.setTranslateX(x);
        shape.setTranslateY(y);
        setupMouseInteraction(pane, shape, v);
        graphNodes.getChildren().add(shape);

        label.setFont(getFont());
        label.setLayoutX(10);
        label.translateXProperty().bind(shape.translateXProperty());
        label.translateYProperty().bind(shape.translateYProperty());
        graphNodeLabels.getChildren().add(label);
        setupMouseInteraction(pane, label, v);

        if (true) {  // add spacers to prevent graph from moving when molecules with long names arrive at a node that is on the boundary of the graph
            final Circle spacer = new Circle(100);
            spacer.translateXProperty().bind(shape.translateXProperty());
            spacer.translateYProperty().bind(shape.translateYProperty());
            spacer.setFill(Color.TRANSPARENT);
            spacer.setStroke(Color.TRANSPARENT);
            spacer.setMouseTransparent(true);

            spacers.getChildren().add(spacer);
        }

        label.textProperty().addListener((c, o, n) -> graph.setLabel(v, n));

        shape.setOnContextMenuRequested((c) -> {
            final MenuItem setLabel = new MenuItem("Set Label");
            setLabel.setOnAction((e) -> NodeLabelDialog.apply(window.getStage(), this, v));
            new ContextMenu(setLabel).show(window.getStage(), c.getScreenX(), c.getScreenY());
        });


        shape.setOnMouseEntered(e -> shape.setFill(Color.LIGHTGRAY));
        shape.setOnMouseExited(e -> shape.setFill(Color.WHITE));

        node2shapeAndLabel.put(v, new Pair<>(shape, label));
    }

    public void removeNode(Node v) {
        for (Edge e : v.adjacentEdges()) {
            graphEdges.getChildren().removeAll(edge2view.get(e).getChildren());
        }
        Pair<Shape, Label> pair = node2shapeAndLabel.get(v);
        if (pair != null) {
            graphNodes.getChildren().remove(pair.get1());
            graphNodeLabels.getChildren().remove(pair.get2());
        }
    }

    public void addEdge(Edge e) {
        final Shape sourceShape = node2shapeAndLabel.get(e.getSource()).getFirst();
        final Shape targetShape = node2shapeAndLabel.get(e.getTarget()).getFirst();

        final EdgeView edgeView = new EdgeView(this, e, sourceShape.translateXProperty(), sourceShape.translateYProperty(), targetShape.translateXProperty(), targetShape.translateYProperty());
        edge2view.setValue(e, edgeView);
        graphEdges.getChildren().addAll(edgeView.getChildren());
    }

    public void removeEdge(Edge e) {
        graphEdges.getChildren().removeAll(getEdgeView(e).getChildren());
    }

    enum What {moveNode, growEdge}

    /**
     * setup mouse interaction
     */
    private void setupMouseInteraction(Pane pane, javafx.scene.Node mouseTarget, Node v) {
        mouseTarget.setCursor(Cursor.CROSSHAIR);

        final double[] mouseDownPosition = new double[2];
        final double[] previousMousePosition = new double[2];
        final Map<Integer, double[]> oldControlPointLocations = new HashMap<>();
        final Map<Integer, double[]> newControlPointLocations = new HashMap<>();

        final Single<Boolean> moved = new Single<>(false);
        final Single<What> what = new Single<>(null);
        final Single<Node> target = new Single<>(null);
        final Line line = new Line();
        world.getChildren().add(line);

        mouseTarget.setOnMousePressed(c -> {
            previousMousePosition[0] = c.getSceneX();
            previousMousePosition[1] = c.getSceneY();

            oldControlPointLocations.clear();
            newControlPointLocations.clear();

            mouseDownPosition[0] = c.getSceneX();
            mouseDownPosition[1] = c.getSceneY();
            moved.set(false);
            what.set(c.isShiftDown() ? What.growEdge : What.moveNode);
            if (what.get() == What.growEdge) {
                final Shape shape = node2shapeAndLabel.get(v).get1();
                line.setStartX(shape.getTranslateX());
                line.setStartY(shape.getTranslateY());

                final Point2D location = pane.sceneToLocal(previousMousePosition[0], previousMousePosition[1]);

                line.setEndX(location.getX());
                line.setEndY(location.getY());
                line.setVisible(true);
                target.set(null);
            }
            c.consume();
        });

        mouseTarget.setOnMouseDragged(c -> {
            final double mouseX = c.getSceneX();
            final double mouseY = c.getSceneY();

            if (what.get() == What.moveNode) {
                getNodeSelection().select(v);
                final double deltaX = (mouseX - previousMousePosition[0]);
                final double deltaY = (mouseY - previousMousePosition[1]);

                for (Node u : getNodeSelection().getSelectedItems()) {
                    if (mouseTarget instanceof Shape) {
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

                        final Shape shape = getShape(u);
                        shape.setTranslateX(shape.getTranslateX() + deltaX);
                        shape.setTranslateY(shape.getTranslateY() + deltaY);
                    } else if (mouseTarget instanceof Label) {
                        final Label label = node2shapeAndLabel.get(u).get2();
                        label.setLayoutX(label.getLayoutX() + deltaX);
                        label.setLayoutY(label.getLayoutY() + deltaY);
                    }
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
                    getShape(target.get()).setFill(Color.WHITE);
                    target.set(null);
                }
                if (w != null && w != v && w != target.get()) {
                    target.set(w);
                    getShape(target.get()).setFill(Color.GRAY);

                }
            }

            moved.set(true);
            previousMousePosition[0] = c.getSceneX();
            previousMousePosition[1] = c.getSceneY();
            c.consume();
        });

        mouseTarget.setOnMouseReleased(c -> {
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
                        getShape(target.get()).setFill(Color.WHITE);

                    final double x = line.getEndX();
                    final double y = line.getEndY();

                    final Node w = findNodeIfHit(c.getScreenX(), c.getScreenY());
                    undoManager.doAndAdd(new NewEdgeAndNodeCommand(pane, this, v, w, x, y));
                }
                moved.set(false);
            }
            line.setVisible(false);
        });
    }

    public void moveNode(Node v, double x, double y) {
        final Pair<Shape, Label> pair = node2shapeAndLabel.get(v);
        pair.getFirst().setTranslateX(pair.getFirst().getTranslateX() + x);
        pair.getFirst().setTranslateY(pair.getFirst().getTranslateY() + y);
    }

    private Node findNodeIfHit(double x, double y) {
        for (Node v : graph.nodes()) {
            final Shape shape = node2shapeAndLabel.get(v).getFirst();
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

    public NodeArray<Pair<Shape, Label>> getNode2shapeAndLabel() {
        return node2shapeAndLabel;
    }

    public Shape getShape(Node v) {
        return node2shapeAndLabel.get(v).getFirst();
    }

    public Label getLabel(Node v) {
        return node2shapeAndLabel.get(v).getSecond();
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
