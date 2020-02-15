/*
 * ControlBindings.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.window;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import jloda.fx.control.ItemSelectionModel;
import jloda.fx.find.FindToolBar;
import jloda.fx.find.GraphSearcher;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.BasicFX;
import jloda.fx.util.Print;
import jloda.fx.util.RecentFilesManager;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.SplashScreen;
import jloda.fx.window.WindowGeometry;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.FileOpenManager;
import jloda.util.ProgramProperties;
import phylosketch.commands.*;
import phylosketch.formattab.FormatTab;
import phylosketch.io.*;
import phylosketch.util.LabelLeaves;
import phylosketch.util.NewWindow;
import phylosketch.util.RubberBandSelectionHandler;
import splitstree5.gui.utils.CheckForUpdate;
import splitstree5.gui.utils.RubberBandSelection;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * setup control bindings
 * Daniel Huson, 1.2020
 */
public class ControlBindings {

    public static void setup(MainWindow window) {
        final MainWindowController controller = window.getController();
        final PhyloView view = window.getView();
        final PhyloTree graph = view.getGraph();
        final ItemSelectionModel<Node> nodeSelection = view.getNodeSelection();
        final ItemSelectionModel<Edge> edgeSelection = view.getEdgeSelection();
        final UndoManager undoManager = view.getUndoManager();
        final Pane mainPane = controller.getMainPane();

        final BooleanProperty hasBackgroundImage = new SimpleBooleanProperty(false);
        controller.getMainPane().getChildren().addListener((InvalidationListener) c -> {
            hasBackgroundImage.set(controller.getMainPane().getChildren().size() > 0 && controller.getMainPane().getChildren().get(0) instanceof ImageView);
        });

        mainPane.getChildren().add(view.getWorld());

        controller.getInfoLabelsVBox().visibleProperty().bind(view.getGraphFX().emptyProperty());
        controller.getInfoLabelsVBox().setMouseTransparent(true);

        mainPane.prefWidthProperty().bind(controller.getBorderPane().widthProperty());
        mainPane.prefHeightProperty().bind(controller.getBorderPane().heightProperty());

        controller.getScrollPane().setLockAspectRatio(false);
        controller.getScrollPane().setRequireShiftOrControlToZoom(true);
        controller.getScrollPane().setUpdateScaleMethod(() -> ZoomCommand.zoom(controller.getScrollPane().getZoomFactorX(), controller.getScrollPane().getZoomFactorY(), mainPane, view));

        new RubberBandSelection(mainPane, controller.getScrollPane(), view.getWorld(), RubberBandSelectionHandler.create(graph, nodeSelection,
                edgeSelection, v -> view.getNodeView(v).getShape(), view::getCurve));

        final BooleanProperty isLeafLabeledDAG = new SimpleBooleanProperty(false);
        NetworkProperties.setup(controller.getStatusFlowPane(), view.getGraphFX(), isLeafLabeledDAG);

        view.getGraphFX().getEdgeList().addListener((ListChangeListener<Edge>) c -> {
            while (c.next()) {
                for (Edge e : c.getAddedSubList()) {
                    if (e.getTarget().getInDegree() > 1) {
                        for (Edge f : e.getTarget().inEdges()) {
                            graph.setSpecial(f, true);
                            graph.setWeight(f, 0);
                        }
                    }
                }
                for (Edge e : c.getRemoved()) {
                    if (e.getTarget().getInDegree() <= 1) {
                        for (Edge f : e.getTarget().inEdges()) {
                            graph.setSpecial(f, false);
                            graph.setWeight(f, 1);
                        }
                    }
                }
            }
        });

        controller.getSelectionLabel().setText("");
        nodeSelection.getSelectedItems().addListener((InvalidationListener) c -> {
            if (nodeSelection.size() > 0 || edgeSelection.size() > 0)
                controller.getSelectionLabel().setText(String.format("Selected %d nodes and %d edges",
                        nodeSelection.size(), edgeSelection.size()));
            else
                controller.getSelectionLabel().setText("");
        });
        edgeSelection.getSelectedItems().addListener((InvalidationListener) c -> {
            if (nodeSelection.size() > 0 || edgeSelection.size() > 0)
                controller.getSelectionLabel().setText(String.format("Selected %d node(s) and %d edge(s)",
                        nodeSelection.size(), edgeSelection.size()));
            else
                controller.getSelectionLabel().setText("");
        });

        controller.getNewMenuItem().setOnAction(e -> NewWindow.apply());

        controller.getOpenMenuItem().setOnAction(e -> OpenDialog.apply(window.getStage()));
        controller.getOpenButton().setOnAction(controller.getOpenMenuItem().getOnAction());

        controller.getImportMenuItem().setOnAction(e -> ImportDialog.apply(window.getStage()));

        controller.getSaveAsMenuItem().setOnAction(e -> Save.showSaveDialog(window));
        controller.getSaveAsMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getSaveButton().setOnAction(controller.getSaveAsMenuItem().getOnAction());
        controller.getSaveButton().disableProperty().bind(controller.getSaveAsMenuItem().disableProperty());

        controller.getExportMenuItem().setOnAction(e -> PhyloSketchIO.exportNewick(window.getStage(), view));
        controller.getExportMenuItem().disableProperty().bind(isLeafLabeledDAG.not());
        controller.getExportButton().setOnAction(controller.getExportMenuItem().getOnAction());
        controller.getExportButton().disableProperty().bind(controller.getExportMenuItem().disableProperty());

        controller.getPageSetupMenuItem().setOnAction((e) -> Print.showPageLayout(window.getStage()));

        controller.getPrintMenuItem().setOnAction((e) -> Print.print(window.getStage(), mainPane));
        controller.getPrintMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getPrintButton().setOnAction(controller.getPrintMenuItem().getOnAction());
        controller.getPrintButton().disableProperty().bind(controller.getPrintMenuItem().disableProperty());

        controller.getQuitMenuItem().setOnAction((e) -> {
            while (MainWindowManager.getInstance().size() > 0) {
                final MainWindow aWindow = (MainWindow) MainWindowManager.getInstance().getMainWindow(MainWindowManager.getInstance().size() - 1);
                if (SaveBeforeClosingDialog.apply(aWindow) == SaveBeforeClosingDialog.Result.cancel)
                    break;
            }
        });

        window.getStage().setOnCloseRequest((e) -> {
            controller.getCloseMenuItem().getOnAction().handle(null);
            e.consume();
        });

        controller.getCloseMenuItem().setOnAction(e -> {
            if (SaveBeforeClosingDialog.apply(window) != SaveBeforeClosingDialog.Result.cancel) {
                ProgramProperties.put("WindowGeometry", (new WindowGeometry(window.getStage())).toString());
                MainWindowManager.getInstance().closeMainWindow(window);
            }
        });

        controller.getCopyMenuItem().setOnAction(e -> {
            if (view.getNodeSelection().getSelectedItems().size() > 0) {
                final List<String> labels = graph.nodeStream().map(view::getLabel).filter(a -> a.getText().length() > 0).map(Labeled::getText).collect(Collectors.toList());
                final ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(Basic.toString(labels, "\n"));
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            } else if (graph.getNumberOfNodes() > 0) {
                final Image snapshot = controller.getMainPane().snapshot(null, null);
                final ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putImage(snapshot);
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            }
        });
        controller.getCopyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getUndoMenuItem().setOnAction(e -> undoManager.undo());
        controller.getUndoMenuItem().disableProperty().bind(undoManager.undoableProperty().not());
        controller.getUndoMenuItem().textProperty().bind(undoManager.undoNameProperty());
        controller.getRedoMenuItem().setOnAction(e -> undoManager.redo());
        controller.getRedoMenuItem().disableProperty().bind(undoManager.redoableProperty().not());
        controller.getRedoMenuItem().textProperty().bind(undoManager.redoNameProperty());


        controller.getPasteMenuItem().setOnAction(e -> {
            final Clipboard cb = Clipboard.getSystemClipboard();
            if (cb.hasImage()) {
                Image image = cb.getImage();
                undoManager.doAndAdd(new SetImageCommand(window.getStage(), controller, image));
            }
        });

        controller.getRemoveBackgroundImageMenuItem().setOnAction(e -> undoManager.doAndAdd(new SetImageCommand(window.getStage(), controller, null)));
        controller.getRemoveBackgroundImageMenuItem().disableProperty().bind(hasBackgroundImage.not());

        controller.getLoadBackgroundImageMenuItem().setOnAction(e -> {
            final Image image = ImageDialog.apply(window.getStage());
            if (image != null)
                undoManager.doAndAdd(new SetImageCommand(window.getStage(), controller, image));
        });

        controller.getDeleteMenuItem().setOnAction(e ->
                undoManager.doAndAdd(new DeleteNodesEdgesCommand(mainPane, view)));
        controller.getDeleteMenuItem().disableProperty().bind(nodeSelection.sizeProperty().isEqualTo(0).and(edgeSelection.sizeProperty().isEqualTo(0)));

        mainPane.setOnMousePressed((e) -> {
            if (e.getClickCount() == 2) {
                final Point2D location = mainPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                undoManager.doAndAdd(new CreateNodeCommand(mainPane, view, location.getX(), location.getY()));
            } else if (e.getClickCount() == 1 && !e.isShiftDown())
                controller.getSelectNoneMenuItem().getOnAction().handle(null);
        });


        controller.getCopyNewickMenuItem().setOnAction(e -> {
            try (StringWriter w = new StringWriter()) {
                final Node root = NetworkProperties.findRoot(graph);
                if (root != null) {
                    graph.setRoot(root);
                    graph.write(w, false);
                    final ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(w.toString() + ";");
                    Clipboard.getSystemClipboard().setContent(clipboardContent);
                }
            } catch (IOException ignored) {
            }

        });
        controller.getCopyNewickMenuItem().disableProperty().bind(isLeafLabeledDAG.not());

        final FindToolBar graphFindToolBar = new FindToolBar(new GraphSearcher(window.getController().getScrollPane(), view.getGraph(), view.getNodeSelection(), view::getLabel, (v, t) -> undoManager.doAndAdd(new ChangeLabelCommand(view, v, t))));
        controller.getTopVBox().getChildren().add(graphFindToolBar);
        controller.getFindMenuItem().setOnAction(c -> graphFindToolBar.setShowFindToolBar(true));
        controller.getFindMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getFindAgainMenuItem().setOnAction(c -> graphFindToolBar.findAgain());
        controller.getFindAgainMenuItem().disableProperty().bind(graphFindToolBar.canFindAgainProperty().not());
        controller.getReplaceMenuItem().setOnAction(c -> graphFindToolBar.setShowReplaceToolBar(true));
        controller.getReplaceMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelLeavesABCMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelLeavesABC(view))));
        controller.getLabelLeavesABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelABCButton().setOnAction(controller.getLabelLeavesABCMenuItem().getOnAction());
        controller.getLabelABCButton().disableProperty().bind(controller.getLabelLeavesABCMenuItem().disableProperty());

        controller.getLabelLeaves123MenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelLeaves123(view))));
        controller.getLabelLeaves123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabel123Button().setOnAction(controller.getLabelLeaves123MenuItem().getOnAction());
        controller.getLabel123Button().disableProperty().bind(controller.getLabelLeaves123MenuItem().disableProperty());

        controller.getLabelLeavesMenuItem().setOnAction(c -> LabelLeaves.labelLeaves(window.getStage(), view));
        controller.getLabelLeavesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getZoomInVerticallyMenuItem().setOnAction(e -> controller.getScrollPane().zoomBy(1, 1.1));
        controller.getZoomInVerticallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getZoomOutVerticallyMenuItem().setOnAction(e -> controller.getScrollPane().zoomBy(1, 1 / 1.1));
        controller.getZoomOutVerticallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getZoomInHorizontallyMenuItem().setOnAction(e -> controller.getScrollPane().zoomBy(1.1, 1));
        controller.getZoomInHorizontallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getZoomOutHorizontallyMenuItem().setOnAction(e -> controller.getScrollPane().zoomBy(1 / 1.1, 1));
        controller.getZoomOutHorizontallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getIncreaseFontSizeMenuItem().setOnAction(c -> {
            view.setFont(Font.font(view.getFont().getName(), view.getFont().getSize() + 2));
            final Stream<Node> stream = (nodeSelection.size() > 0 ? nodeSelection.getSelectedItems().stream() : graph.nodeStream());
            stream.map(view::getLabel).forEach(a -> a.setFont(Font.font(a.getFont().getName(), a.getFont().getSize() + 2)));
        });
        controller.getIncreaseFontSizeMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getDecreaseFontSizeMenuItem().setOnAction(c -> {
            if (view.getFont().getSize() - 2 >= 4) {
                view.setFont(Font.font(view.getFont().getName(), view.getFont().getSize() - 2));
                final Stream<Node> stream = (nodeSelection.size() > 0 ? nodeSelection.getSelectedItems().stream() : graph.nodeStream());
                stream.filter(v -> view.getLabel(v).getFont().getSize() >= 6).map(view::getLabel)
                        .forEach(a -> a.setFont(Font.font(a.getFont().getName(), a.getFont().getSize() - 2)));
            }
        });
        controller.getDecreaseFontSizeMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());


        BasicFX.setupFullScreenMenuSupport(window.getStage(), controller.getEnterFullScreenMenuItem());

        RecentFilesManager.getInstance().setFileOpener(FileOpenManager.getFileOpener());
        RecentFilesManager.getInstance().setupMenu(controller.getRecentMenu());

        controller.getStraightenEdgesMenuItem().setOnAction(c -> undoManager.doAndAdd(new StraightenEdgesCommand(view, view.getEdgeSelection().getSelectedItems())));
        controller.getStraightenEdgesMenuItem().disableProperty().bind(Bindings.isEmpty(view.getEdgeSelection().getSelectedItems()));

        controller.getAboutMenuItem().setOnAction((e) -> SplashScreen.showSplash(Duration.ofMinutes(2)));

        controller.getCheckForUpdatesMenuItem().setOnAction((e) -> CheckForUpdate.apply());
        MainWindowManager.getInstance().changedProperty().addListener((c, o, n) -> controller.getCheckForUpdatesMenuItem().disableProperty().set(MainWindowManager.getInstance().size() > 1
                || (MainWindowManager.getInstance().size() == 1 && !MainWindowManager.getInstance().getMainWindow(0).isEmpty())));


        setupSelect(view, controller);
        setupAlign(view, controller);
        setupLabelPosition(view, controller);

        controller.getFormatTitledPane().setAnimated(false);
        controller.getFormatTitledPane().expandedProperty().addListener((c, o, n) -> controller.getFormatBorderPane().setVisible(n));

        controller.getFormatBorderPane().setCenter(new FormatTab(window));
        controller.getFormatTitledPane().setExpanded(false);

    }

    public static void setupSelect(PhyloView editor, MainWindowController controller) {
        final PhyloTree graph = editor.getGraph();

        final ItemSelectionModel<Node> nodeSelection = editor.getNodeSelection();
        final ItemSelectionModel<Edge> edgeSelection = editor.getEdgeSelection();

        controller.getSelectAllMenuItem().setOnAction(e -> {
            graph.nodes().forEach(nodeSelection::select);
            graph.edges().forEach(edgeSelection::select);
        });
        controller.getSelectAllMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectNoneMenuItem().setOnAction(e -> {
            nodeSelection.clearSelection();
            edgeSelection.clearSelection();
        });
        controller.getSelectNoneMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()).and(Bindings.isEmpty(edgeSelection.getSelectedItems())));

        controller.getSelectInvertMenuItem().setOnAction(e -> {
            graph.nodes().forEach(nodeSelection::toggleSelection);
            graph.edges().forEach(edgeSelection::toggleSelection);
        });
        controller.getSelectInvertMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectLeavesMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getOutDegree() == 0).forEach(nodeSelection::select));

        controller.getSelectLeavesMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectReticulateNodesMenuitem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() > 1).forEach(nodeSelection::select));
        controller.getSelectReticulateNodesMenuitem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectStableNodesMenuItem().setOnAction(e -> NetworkProperties.allStableInternal(graph).forEach(nodeSelection::select));
        controller.getSelectStableNodesMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectVisibleNodesMenuItem().setOnAction(e -> NetworkProperties.allVisibleReticulations(graph).forEach(nodeSelection::select));
        controller.getSelectVisibleNodesMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectTreeNodesMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() <= 1 && v.getOutDegree() > 0).forEach(nodeSelection::select));

        controller.getSelectTreeNodesMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectAllAboveMenuItem().setOnAction(c -> {
            final Queue<Node> list = new LinkedList<>(nodeSelection.getSelectedItems());

            while (list.size() > 0) {
                Node v = list.remove();
                for (Edge e : v.inEdges()) {
                    Node w = e.getSource();
                    edgeSelection.select(e);
                    if (!nodeSelection.isSelected(w)) {
                        nodeSelection.select(w);
                        list.add(w);
                    }
                }
            }
        });
        controller.getSelectAllAboveMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()));

        controller.getSelectAllBelowMenuItem().setOnAction(c -> {
            final Queue<Node> list = new LinkedList<>(nodeSelection.getSelectedItems());

            while (list.size() > 0) {
                Node v = list.remove();
                for (Edge e : v.outEdges()) {
                    Node w = e.getTarget();
                    edgeSelection.select(e);
                    if (!nodeSelection.isSelected(w)) {
                        nodeSelection.select(w);
                        list.add(w);
                    }
                }
            }
        });
        controller.getSelectAllBelowMenuItem().disableProperty().bind(Bindings.isEmpty(nodeSelection.getSelectedItems()));

        controller.getSelectTreeEdgesMenuItem().setOnAction(c -> graph.edgeStream().filter(e -> e.getTarget().getInDegree() <= 1).forEach(edgeSelection::select));
        controller.getSelectReticulateEdgesMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

        controller.getSelectReticulateEdgesMenuItem().setOnAction(c -> graph.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).forEach(edgeSelection::select));
        controller.getSelectReticulateEdgesMenuItem().disableProperty().bind(editor.getGraphFX().emptyProperty());

    }

    public static void setupAlign(PhyloView view, MainWindowController controller) {
        final UndoManager undoManager = view.getUndoManager();

        final ObservableList<Node> nodeSelection = view.getNodeSelection().getSelectedItems();

        final BooleanProperty atMostOneSelected = new SimpleBooleanProperty(false);
        atMostOneSelected.bind(Bindings.size(nodeSelection).lessThanOrEqualTo(1));

        final BooleanProperty atMostTwoSelected = new SimpleBooleanProperty(false);
        atMostTwoSelected.bind(Bindings.size(nodeSelection).lessThanOrEqualTo(2));

        controller.getAlignTopMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Top)));
        controller.getAlignTopMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignTopButton().setOnAction(controller.getAlignTopMenuItem().getOnAction());
        controller.getAlignTopButton().disableProperty().bind(controller.getAlignTopMenuItem().disableProperty());

        controller.getAlignMiddleMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Middle)));
        controller.getAlignMiddleMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignMiddleButton().setOnAction(controller.getAlignMiddleMenuItem().getOnAction());
        controller.getAlignMiddleButton().disableProperty().bind(controller.getAlignMiddleMenuItem().disableProperty());

        controller.getAlignBottomMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Bottom)));
        controller.getAlignBottomMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignBottomButton().setOnAction(controller.getAlignBottomMenuItem().getOnAction());
        controller.getAlignBottomButton().disableProperty().bind(controller.getAlignBottomMenuItem().disableProperty());

        controller.getAlignLeftMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Left)));
        controller.getAlignLeftMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignLeftButton().setOnAction(controller.getAlignLeftMenuItem().getOnAction());
        controller.getAlignLeftButton().disableProperty().bind(controller.getAlignLeftMenuItem().disableProperty());

        controller.getAlignCenterMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Center)));
        controller.getAlignCenterMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignCenterButton().setOnAction(controller.getAlignCenterMenuItem().getOnAction());
        controller.getAlignCenterButton().disableProperty().bind(controller.getAlignCenterMenuItem().disableProperty());

        controller.getAlignRightMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Right)));
        controller.getAlignRightMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignRightButton().setOnAction(controller.getAlignRightMenuItem().getOnAction());
        controller.getAlignRightButton().disableProperty().bind(controller.getAlignRightMenuItem().disableProperty());

        controller.getDistributeHorizontallyMenuItem().setOnAction(c -> undoManager.doAndAdd(new DistributeNodesCommand(view, nodeSelection, DistributeNodesCommand.Direction.Horizontally)));
        controller.getDistributeHorizontallyMenuItem().disableProperty().bind(atMostTwoSelected);

        controller.getDistributeHorizontallyButton().setOnAction(controller.getDistributeHorizontallyMenuItem().getOnAction());
        controller.getDistributeHorizontallyButton().disableProperty().bind(controller.getDistributeHorizontallyMenuItem().disableProperty());

        controller.getDistributeVerticallyMenuItem().setOnAction(c -> undoManager.doAndAdd(new DistributeNodesCommand(view, nodeSelection, DistributeNodesCommand.Direction.Vertically)));
        controller.getDistributeVerticallyMenuItem().disableProperty().bind(atMostTwoSelected);

        controller.getDistributeVerticallyButton().setOnAction(controller.getDistributeVerticallyMenuItem().getOnAction());
        controller.getDistributeVerticallyButton().disableProperty().bind(controller.getDistributeVerticallyMenuItem().disableProperty());
    }

    public static void setupLabelPosition(PhyloView view, MainWindowController controller) {
        final UndoManager undoManager = view.getUndoManager();

        final ObservableList<Node> nodeSelection = view.getNodeSelection().getSelectedItems();

        final BooleanProperty noneSelected = new SimpleBooleanProperty(false);
        noneSelected.bind(Bindings.size(nodeSelection).isEqualTo(0));

        controller.getLabelPositionAboveMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, nodeSelection, PositionNodeLabelsCommand.Position.Above)));
        controller.getLabelPositionAboveMenuItem().disableProperty().bind(noneSelected);

        controller.getLabelAboveButton().setOnAction(controller.getLabelPositionAboveMenuItem().getOnAction());
        controller.getLabelAboveButton().disableProperty().bind(controller.getLabelPositionAboveMenuItem().disableProperty());

        controller.getLabelPositionBelowMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, nodeSelection, PositionNodeLabelsCommand.Position.Below)));
        controller.getLabelPositionBelowMenuItem().disableProperty().bind(noneSelected);

        controller.getLabelBelowButton().setOnAction(controller.getLabelPositionBelowMenuItem().getOnAction());
        controller.getLabelBelowButton().disableProperty().bind(controller.getLabelPositionBelowMenuItem().disableProperty());

        controller.getLabelPositionLeftMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, nodeSelection, PositionNodeLabelsCommand.Position.Left)));
        controller.getLabelPositionLeftMenuItem().disableProperty().bind(noneSelected);

        controller.getLabelLeftButton().setOnAction(controller.getLabelPositionLeftMenuItem().getOnAction());
        controller.getLabelLeftButton().disableProperty().bind(controller.getLabelPositionLeftMenuItem().disableProperty());

        controller.getLabelPositionRightMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, nodeSelection, PositionNodeLabelsCommand.Position.Right)));
        controller.getLabelPositionRightMenuItem().disableProperty().bind(noneSelected);

        controller.getLabelRightButton().setOnAction(controller.getLabelPositionRightMenuItem().getOnAction());
        controller.getLabelRightButton().disableProperty().bind(controller.getLabelPositionRightMenuItem().disableProperty());


        controller.getLabelPositionCenterMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, nodeSelection, PositionNodeLabelsCommand.Position.Center)));
        controller.getLabelPositionCenterMenuItem().disableProperty().bind(noneSelected);

        controller.getLabelCenterButton().setOnAction(controller.getLabelPositionCenterMenuItem().getOnAction());
        controller.getLabelCenterButton().disableProperty().bind(controller.getLabelPositionCenterMenuItem().disableProperty());

    }
}
