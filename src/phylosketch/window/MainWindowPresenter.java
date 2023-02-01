/*
 * MainWindowPresenter.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package phylosketch.window;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import jloda.fx.control.ItemSelectionModel;
import jloda.fx.control.RichTextLabel;
import jloda.fx.control.ZoomableScrollPane;
import jloda.fx.find.FindToolBar;
import jloda.fx.find.GraphSearcher;
import jloda.fx.selection.rubberband.RubberBandSelection;
import jloda.fx.selection.rubberband.RubberBandSelectionHandler;
import jloda.fx.undo.UndoManager;
import jloda.fx.util.*;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.NotificationManager;
import jloda.fx.window.SplashScreen;
import jloda.fx.window.WindowGeometry;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.phylo.algorithms.RootedNetworkProperties;
import jloda.util.StringUtils;
import phylosketch.algorithms.RunNormalize;
import phylosketch.commands.*;
import phylosketch.formattab.FormatTab;
import phylosketch.io.*;
import phylosketch.util.LabelLeaves;
import phylosketch.util.NewWindow;
import phylosketch.view.PhyloView;
import splitstree5.main.CheckForUpdate;

import java.io.IOException;
import java.io.StringReader;
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
public class MainWindowPresenter {

    public static void setup(MainWindow window) {
        final MainWindowController controller = window.getController();
        final PhyloView view = window.getView();
        final PhyloTree graph = view.getGraph();
        final ItemSelectionModel<Node> nodeSelection = view.getNodeSelection();
        final ItemSelectionModel<Edge> edgeSelection = view.getEdgeSelection();
        final UndoManager undoManager = view.getUndoManager();

        final ZoomableScrollPane scrollPane = controller.getScrollPane();

        scrollPane.setLockAspectRatio(false);
        scrollPane.setRequireShiftOrControlToZoom(true);
        //scrollPane.setPannable(true);

        scrollPane.getContent().setStyle("-fx-background-color: white;");

        final Pane contentPane = controller.getContentPane();
        contentPane.getChildren().add(view.getWorld());

        scrollPane.setUpdateScaleMethod(() -> ZoomCommand.zoom(scrollPane.getZoomFactorX(), scrollPane.getZoomFactorY(), contentPane, view));

        contentPane.prefWidthProperty().bind(controller.getBorderPane().widthProperty());
        contentPane.prefHeightProperty().bind(controller.getBorderPane().heightProperty());

        final var hasBackgroundImage = new SimpleBooleanProperty(false);
        contentPane.getChildren().addListener((InvalidationListener) c -> hasBackgroundImage.set(contentPane.getChildren().size() > 0 && contentPane.getChildren().get(0) instanceof ImageView));

        controller.getInfoLabelsVBox().visibleProperty().bind(view.getGraphFX().emptyProperty());
        controller.getInfoLabelsVBox().setMouseTransparent(true);

		new RubberBandSelection(contentPane, scrollPane, view.getWorld(), RubberBandSelectionHandler.create(graph, nodeSelection,
				edgeSelection, v -> view.getNodeView(v).getShape()));

        final var updatingProperties = new SimpleBooleanProperty(false);
        final var isLeafLabeledDAG = new SimpleBooleanProperty(false);
        SetupNetworkProperties.setup(controller.getStatusFlowPane(), view.getGraphFX(), updatingProperties, isLeafLabeledDAG);

        view.getGraphFX().getEdgeList().addListener((ListChangeListener<Edge>) c -> {
            while (c.next()) {
                for (var e : c.getAddedSubList()) {
                    if (e.getTarget().getInDegree() > 1) {
                        for (var f : e.getTarget().inEdges()) {
                            graph.setReticulate(f, true);
                            graph.setWeight(f, 0);
                        }
                    }
                }
                for (var e : c.getRemoved()) {
                    if (e.getTarget().getInDegree() <= 1) {
                        for (var f : e.getTarget().inEdges()) {
                            graph.setReticulate(f, false);
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

        controller.getPrintMenuItem().setOnAction((e) -> Print.print(window.getStage(), contentPane));
        controller.getPrintMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getPrintButton().setOnAction(controller.getPrintMenuItem().getOnAction());
        controller.getPrintButton().disableProperty().bind(controller.getPrintMenuItem().disableProperty());

        controller.getQuitMenuItem().setOnAction((e) -> {
            while (MainWindowManager.getInstance().size() > 0) {
                final MainWindow aWindow = (MainWindow) MainWindowManager.getInstance().getMainWindow(MainWindowManager.getInstance().size() - 1);
                if (SaveBeforeClosingDialog.apply(aWindow) == SaveBeforeClosingDialog.Result.cancel || !MainWindowManager.getInstance().closeMainWindow(aWindow))
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
            if (view.getNodeSelection().size() > 0) {
                final List<String> labels = graph.nodeStream().map(view::getLabel).map(RichTextLabel::getText).filter(text -> text.length() > 0).collect(Collectors.toList());
                final ClipboardContent clipboardContent = new ClipboardContent();
				clipboardContent.putString(StringUtils.toString(labels, "\n"));
				Clipboard.getSystemClipboard().setContent(clipboardContent);
            } else if (graph.getNumberOfNodes() > 0) {
                final Image snapshot = contentPane.snapshot(null, null);
                final ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putImage(snapshot);
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            }
        });
        controller.getCopyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getUndoMenuItem().setOnAction(e -> undoManager.undo());
        controller.getUndoMenuItem().disableProperty().bind(undoManager.undoableProperty().not());
        controller.getUndoMenuItem().textProperty().bind(undoManager.undoNameProperty());

        controller.getUndoButton().setOnAction(e -> undoManager.undo());
        controller.getUndoButton().disableProperty().bind(undoManager.undoableProperty().not());
        controller.getUndoButton().setTooltip(new Tooltip());
        controller.getUndoButton().getTooltip().textProperty().bind(undoManager.undoNameProperty());

        controller.getRedoMenuItem().setOnAction(e -> undoManager.redo());
        controller.getRedoMenuItem().disableProperty().bind(undoManager.redoableProperty().not());
        controller.getRedoMenuItem().textProperty().bind(undoManager.redoNameProperty());

        controller.getRedoButton().setOnAction(e -> undoManager.redo());
        controller.getRedoButton().disableProperty().bind(undoManager.redoableProperty().not());
        controller.getRedoButton().setTooltip(new Tooltip());
        controller.getRedoButton().getTooltip().textProperty().bind(undoManager.redoNameProperty());

        controller.getPasteMenuItem().setOnAction(e -> {
            final Clipboard cb = Clipboard.getSystemClipboard();
            if (cb.hasImage()) {
                Image image = cb.getImage();
                undoManager.doAndAdd(new SetImageCommand(window.getStage(), controller.getContentPane(), image));
            }
        });

        controller.getPasteNewickMenuItem().setOnAction(e -> {
            final Clipboard cb = Clipboard.getSystemClipboard();
            if (cb.hasString()) {
                undoManager.clear();
                try (var reader = new StringReader(cb.getString())) {
                    PhyloSketchIO.importNewick(contentPane, view, reader);
                } catch (IOException ex) {
                    NotificationManager.showError("Paste Newick failed: " + ex);
                }
            }
        });

        controller.getNormalizationMenuItem().setOnAction(c -> RunNormalize.apply(window));
        controller.getNormalizationMenuItem().disableProperty().bind(isLeafLabeledDAG.not());

        controller.getRemoveBackgroundImageMenuItem().setOnAction(e -> undoManager.doAndAdd(new SetImageCommand(window.getStage(), controller.getContentPane(), null)));
        controller.getRemoveBackgroundImageMenuItem().disableProperty().bind(hasBackgroundImage.not());

        controller.getLoadBackgroundImageMenuItem().setOnAction(e -> {
            final Image image = ImageDialog.apply(window.getStage());
            if (image != null)
                undoManager.doAndAdd(new SetImageCommand(window.getStage(), controller.getContentPane(), image));
        });

        controller.getDeleteMenuItem().setOnAction(e ->
                undoManager.doAndAdd(new DeleteNodesEdgesCommand(contentPane, view, view.getNodeSelection().getSelectedItems(), view.getEdgeSelection().getSelectedItems())));
        controller.getDeleteMenuItem().disableProperty().bind(nodeSelection.emptyProperty().and(edgeSelection.emptyProperty()));

        controller.getDeleteLabelsMenuItem().setOnAction(e -> nodeSelection.getSelectedItems().forEach(v -> view.getNodeView(v).getLabel().setText(null)));
        controller.getDeleteLabelsMenuItem().disableProperty().bind(nodeSelection.emptyProperty());

        contentPane.setOnMousePressed(e -> {
            if (e.getClickCount() == 2) {
                final Point2D location = contentPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                undoManager.doAndAdd(new CreateNodeCommand(contentPane, view, location.getX(), location.getY()));
            } else if (e.getClickCount() == 1 && !e.isShiftDown())
                controller.getSelectNoneMenuItem().getOnAction().handle(null);
        });

        controller.getCopyNewickMenuItem().setOnAction(e -> {
            try (StringWriter w = new StringWriter()) {
                for (Node root : RootedNetworkProperties.findRoots(graph)) {
                    graph.setRoot(root);
                    graph.write(w, false);
                    w.write(";\n");
                }
                final ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(w.toString());
                Clipboard.getSystemClipboard().setContent(clipboardContent);
            } catch (IOException ignored) {
            }

        });
        controller.getCopyNewickMenuItem().disableProperty().bind(isLeafLabeledDAG.not());

        final GraphSearcher graphSearcher = new GraphSearcher(view.getGraph(), view.getNodeSelection(), (v) -> view.getLabel(v).getText(), (v, t) -> undoManager.doAndAdd(new ChangeLabelCommand(view, v, t)));
        final FindToolBar graphFindToolBar = new FindToolBar(window.getStage(), graphSearcher);

        graphSearcher.foundProperty().addListener((c, o, n) -> {
            if (n != null && view.getLabel(n) != null) {
                controller.getScrollPane().ensureVisible(view.getLabel(n));
            }
        });

        controller.getTopVBox().getChildren().add(graphFindToolBar);
        controller.getFindMenuItem().setOnAction(c -> graphFindToolBar.setShowFindToolBar(true));
        controller.getFindMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getFindAgainMenuItem().setOnAction(c -> graphFindToolBar.findAgain());
        controller.getFindAgainMenuItem().disableProperty().bind(graphFindToolBar.canFindAgainProperty().not());
        controller.getReplaceMenuItem().setOnAction(c -> graphFindToolBar.setShowReplaceToolBar(true));
        controller.getReplaceMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateGraphClockwiseMenuItem().setOnAction(c -> undoManager.doAndAdd(new RotateGraphCommand(view, true)));
        controller.getRotateGraphClockwiseMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateGraphAnticlockwiseMenuItem().setOnAction(c -> undoManager.doAndAdd(new RotateGraphCommand(view, false)));
        controller.getRotateGraphAnticlockwiseMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getFlipGraphHorizontallyMenuItem().setOnAction(c -> undoManager.doAndAdd(new FlipGraphCommand(view, true)));
        controller.getFlipGraphHorizontallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getFlipGraphVerticallyMenuItem().setOnAction(c -> undoManager.doAndAdd(new FlipGraphCommand(view, false)));
        controller.getFlipGraphVerticallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getFlipGraphHorizontallyButton().setOnAction(controller.getFlipGraphHorizontallyMenuItem().getOnAction());
        controller.getFlipGraphHorizontallyButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getFlipGraphVerticallyButton().setOnAction(controller.getFlipGraphVerticallyMenuItem().getOnAction());
        controller.getFlipGraphVerticallyButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateLabelsClockwiseMenuItem().setOnAction(c -> undoManager.doAndAdd(new RotateLabelsCommand(view, view.selectedOrAllNodes(), true)));
        controller.getRotateLabelsClockwiseMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateLabelsAnticlockwiseMenuItem().setOnAction(c -> undoManager.doAndAdd(new RotateLabelsCommand(view, view.selectedOrAllNodes(), false)));
        controller.getRotateLabelsAnticlockwiseMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateGraphClockwiseButton().setOnAction(controller.getRotateGraphClockwiseMenuItem().getOnAction());
        controller.getRotateGraphClockwiseButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateGraphAnticlockwiseButton().setOnAction(controller.getRotateGraphAnticlockwiseMenuItem().getOnAction());
        controller.getRotateGraphAnticlockwiseButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateLabelsClockwiseButton().setOnAction(controller.getRotateLabelsClockwiseMenuItem().getOnAction());
        controller.getRotateLabelsClockwiseButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getRotateLabelsAnticlockwiseButton().setOnAction(controller.getRotateLabelsAnticlockwiseMenuItem().getOnAction());
        controller.getRotateLabelsAnticlockwiseButton().disableProperty().bind(view.getGraphFX().emptyProperty());

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

        controller.getLabelInternalABCMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelInternalABC(view))));
        controller.getLabelInternalABCMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelInternal123MenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeNodeLabelsCommand(view, LabelLeaves.labelInternal123(view))));
        controller.getLabelInternal123MenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getZoomInVerticallyMenuItem().setOnAction(e -> scrollPane.zoomBy(1, 1.1));
        controller.getZoomInVerticallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getZoomOutVerticallyMenuItem().setOnAction(e -> scrollPane.zoomBy(1, 1 / 1.1));
        controller.getZoomOutVerticallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getZoomInHorizontallyMenuItem().setOnAction(e -> scrollPane.zoomBy(1.1, 1));
        controller.getZoomInHorizontallyMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
        controller.getZoomOutHorizontallyMenuItem().setOnAction(e -> scrollPane.zoomBy(1 / 1.1, 1));
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

        controller.getRemoveDiNodesMenuItem().setOnAction(c -> undoManager.doAndAdd(new RemoveDiNodesCommand(controller.getContentPane(), view, view.selectedOrAllNodes())));
        controller.getRemoveDiNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getAddDiNodesMenuItem().setOnAction(c -> undoManager.doAndAdd(SplitEdgeCommand.createAddDiNodesCommand(controller.getContentPane(), view, view.selectedOrAllEdges())));
        controller.getAddDiNodesMenuItem().disableProperty().bind(view.getEdgeSelection().emptyProperty());

        controller.getStraightenEdgesMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeEdgeShapeCommand(view, view.selectedOrAllEdges(), ChangeEdgeShapeCommand.EdgeShape.Straight)));
        controller.getStraightenEdgesMenuItem().disableProperty().bind(Bindings.isEmpty(view.getGraphFX().getEdgeList()));

        controller.getStraightenEdgesButton().setOnAction(controller.getStraightenEdgesMenuItem().getOnAction());
        controller.getStraightenEdgesButton().disableProperty().bind(controller.getStraightenEdgesMenuItem().disableProperty());

        controller.getReshapeEdgesMenuItem().setOnAction(c -> undoManager.doAndAdd(new ChangeEdgeShapeCommand(view, view.selectedOrAllEdges(), ChangeEdgeShapeCommand.EdgeShape.Reshape)));
        controller.getReshapeEdgesMenuItem().disableProperty().bind(Bindings.isEmpty(view.getGraphFX().getEdgeList()));

        controller.getReshapEdgesButton().setOnAction(controller.getReshapeEdgesMenuItem().getOnAction());
        controller.getReshapEdgesButton().disableProperty().bind(controller.getReshapeEdgesMenuItem().disableProperty());

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

        controller.getUseDarkThemeCheckMenuItem().selectedProperty().bindBidirectional(MainWindowManager.useDarkThemeProperty());
    }

    public static void setupSelect(PhyloView view, MainWindowController controller) {
        final PhyloTree graph = view.getGraph();

        final ItemSelectionModel<Node> nodeSelection = view.getNodeSelection();
        final ItemSelectionModel<Edge> edgeSelection = view.getEdgeSelection();

        controller.getSelectAllMenuItem().setOnAction(e -> {
            graph.nodes().forEach(nodeSelection::select);
            graph.edges().forEach(edgeSelection::select);
        });
        controller.getSelectAllMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectNoneMenuItem().setOnAction(e -> {
            nodeSelection.clearSelection();
            edgeSelection.clearSelection();
        });
        controller.getSelectNoneMenuItem().disableProperty().bind(nodeSelection.emptyProperty().and(edgeSelection.emptyProperty()));

        controller.getSelectInvertMenuItem().setOnAction(e -> {
            graph.nodes().forEach(nodeSelection::toggleSelection);
            graph.edges().forEach(edgeSelection::toggleSelection);
        });
        controller.getSelectInvertMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectRootsMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() == 0).forEach(nodeSelection::select));
        controller.getSelectRootsMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectLeavesMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getOutDegree() == 0).forEach(nodeSelection::select));
        controller.getSelectLeavesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectReticulateNodesMenuitem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() > 1).forEach(nodeSelection::select));
        controller.getSelectReticulateNodesMenuitem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectStableNodesMenuItem().setOnAction(e -> RootedNetworkProperties.computeAllCompletelyStableInternal(graph).forEach(nodeSelection::select));
        controller.getSelectStableNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectVisibleNodesMenuItem().setOnAction(e -> RootedNetworkProperties.computeAllVisibleNodes(graph, null).forEach(nodeSelection::select));
        controller.getSelectVisibleNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectVisibleReticulationsMenuItem().setOnAction(e -> RootedNetworkProperties.computeAllVisibleNodes(graph, null).stream().filter(v -> v.getInDegree() > 1).forEach(nodeSelection::select));
        controller.getSelectVisibleReticulationsMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectTreeNodesMenuItem().setOnAction(e -> graph.nodeStream().filter(v -> v.getInDegree() <= 1 && v.getOutDegree() > 0).forEach(nodeSelection::select));

        controller.getSelectTreeNodesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

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
        controller.getSelectAllAboveMenuItem().disableProperty().bind(nodeSelection.emptyProperty());

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
        controller.getSelectAllBelowMenuItem().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getSelectLowestStableAncestorMenuItem().setOnAction(e -> nodeSelection.selectItems(RootedNetworkProperties.computeAllLowestStableAncestors(graph, nodeSelection.getSelectedItems())));
        controller.getSelectLowestStableAncestorMenuItem().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getSelectTreeEdgesMenuItem().setOnAction(c -> graph.edgeStream().filter(e -> e.getTarget().getInDegree() <= 1).forEach(edgeSelection::select));
        controller.getSelectReticulateEdgesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getSelectReticulateEdgesMenuItem().setOnAction(c -> graph.edgeStream().filter(e -> e.getTarget().getInDegree() > 1).forEach(edgeSelection::select));
        controller.getSelectReticulateEdgesMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());
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
        controller.getAlignTopButton().disableProperty().bind(atMostOneSelected);

        controller.getAlignMiddleMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, view.selectedOrAllNodes(), AlignNodesCommand.Alignment.Middle)));
        controller.getAlignMiddleMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignMiddleButton().setOnAction(controller.getAlignMiddleMenuItem().getOnAction());
        controller.getAlignMiddleButton().disableProperty().bind(atMostOneSelected);

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
        controller.getAlignCenterButton().disableProperty().bind(atMostOneSelected);

        controller.getAlignRightMenuItem().setOnAction(c -> undoManager.doAndAdd(new AlignNodesCommand(view, nodeSelection, AlignNodesCommand.Alignment.Right)));
        controller.getAlignRightMenuItem().disableProperty().bind(atMostOneSelected);

        controller.getAlignRightButton().setOnAction(controller.getAlignRightMenuItem().getOnAction());
        controller.getAlignRightButton().disableProperty().bind(atMostOneSelected);

        controller.getDistributeHorizontallyMenuItem().setOnAction(c -> undoManager.doAndAdd(new DistributeNodesCommand(view, nodeSelection, DistributeNodesCommand.Direction.Horizontally)));
        controller.getDistributeHorizontallyMenuItem().disableProperty().bind(atMostTwoSelected);

        controller.getDistributeHorizontallyButton().setOnAction(controller.getDistributeHorizontallyMenuItem().getOnAction());
        controller.getDistributeHorizontallyButton().disableProperty().bind(atMostTwoSelected);

        controller.getDistributeVerticallyMenuItem().setOnAction(c -> undoManager.doAndAdd(new DistributeNodesCommand(view, nodeSelection, DistributeNodesCommand.Direction.Vertically)));
        controller.getDistributeVerticallyMenuItem().disableProperty().bind(atMostTwoSelected);

        controller.getDistributeVerticallyButton().setOnAction(controller.getDistributeVerticallyMenuItem().getOnAction());
        controller.getDistributeVerticallyButton().disableProperty().bind(atMostTwoSelected);
    }

    public static void setupLabelPosition(PhyloView view, MainWindowController controller) {
        final UndoManager undoManager = view.getUndoManager();

        final ObservableList<Node> nodeSelection = view.getNodeSelection().getSelectedItems();

        final BooleanProperty noneSelected = new SimpleBooleanProperty(false);
        noneSelected.bind(Bindings.size(nodeSelection).isEqualTo(0));

        controller.getLabelPositionAboveMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, view.selectedOrAllNodes(), PositionNodeLabelsCommand.Position.Above)));
        controller.getLabelPositionAboveMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelAboveButton().setOnAction(controller.getLabelPositionAboveMenuItem().getOnAction());
        controller.getLabelAboveButton().disableProperty().bind(controller.getLabelPositionAboveMenuItem().disableProperty());

        controller.getLabelPositionBelowMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, view.selectedOrAllNodes(), PositionNodeLabelsCommand.Position.Below)));
        controller.getLabelPositionBelowMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelBelowButton().setOnAction(controller.getLabelPositionBelowMenuItem().getOnAction());
        controller.getLabelBelowButton().disableProperty().bind(controller.getLabelPositionBelowMenuItem().disableProperty());

        controller.getLabelPositionLeftMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, view.selectedOrAllNodes(), PositionNodeLabelsCommand.Position.Left)));
        controller.getLabelPositionLeftMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelLeftButton().setOnAction(controller.getLabelPositionLeftMenuItem().getOnAction());
        controller.getLabelLeftButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelPositionRightMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, view.selectedOrAllNodes(), PositionNodeLabelsCommand.Position.Right)));
        controller.getLabelPositionRightMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelRightButton().setOnAction(controller.getLabelPositionRightMenuItem().getOnAction());
        controller.getLabelRightButton().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelPositionCenterMenuItem().setOnAction(c -> undoManager.doAndAdd(new PositionNodeLabelsCommand(view, view.selectedOrAllNodes(), PositionNodeLabelsCommand.Position.Center)));
        controller.getLabelPositionCenterMenuItem().disableProperty().bind(view.getGraphFX().emptyProperty());

        controller.getLabelCenterButton().setOnAction(controller.getLabelPositionCenterMenuItem().getOnAction());
        controller.getLabelCenterButton().disableProperty().bind(view.getGraphFX().emptyProperty());

    }
}
