/*
 * MainWindowController.java Copyright (C) 2020. Daniel H. Huson
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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jloda.fx.control.ZoomableScrollPane;
import jloda.fx.window.IMainWindow;
import jloda.fx.window.MainWindowManager;
import jloda.util.ProgramProperties;

import java.util.ArrayList;

public class MainWindowController {

    @FXML
    private BorderPane borderPane;

    @FXML
    private VBox topVBox;

    @FXML
    private MenuBar menuBar;

    @FXML
    private Menu fileMenu;

    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private Menu recentMenu;

    @FXML
    private MenuItem saveAsMenuItem;

    @FXML
    private MenuItem importMenuItem;

    @FXML
    private MenuItem exportMenuItem;

    @FXML
    private MenuItem printMenuItem;

    @FXML
    private MenuItem pageSetupMenuItem;

    @FXML
    private MenuItem closeMenuItem;

    @FXML
    private MenuItem quitMenuItem;

    @FXML
    private Menu editMenu;

    @FXML
    private MenuItem undoMenuItem;

    @FXML
    private MenuItem redoMenuItem;

    @FXML
    private MenuItem cutMenuItem;

    @FXML
    private MenuItem copyMenuItem;

    @FXML
    private MenuItem copyNewickMenuItem;

    @FXML
    private MenuItem pasteMenuItem;

    @FXML
    private MenuItem deleteMenuItem;

    @FXML
    private MenuItem findMenuItem;

    @FXML
    private MenuItem findAgainMenuItem;

    @FXML
    private MenuItem replaceMenuItem;

    @FXML
    private MenuItem selectAllMenuItem;

    @FXML
    private MenuItem selectNoneMenuItem;

    @FXML
    private MenuItem selectInvertMenuItem;

    @FXML
    private MenuItem selectTreeEdgesMenuItem;

    @FXML
    private MenuItem selectReticulateEdgesMenuItem;

    @FXML
    private MenuItem selectRootsMenuItem;

    @FXML
    private MenuItem selectLeavesMenuItem;

    @FXML
    private MenuItem selectTreeNodesMenuItem;

    @FXML
    private MenuItem selectReticulateNodesMenuitem;

    @FXML
    private MenuItem selectVisibleNodesMenuItem;

    @FXML
    private MenuItem selectVisibleReticulationsMenuItem;

    @FXML
    private MenuItem selectStableNodesMenuItem;

    @FXML
    private MenuItem selectAllBelowMenuItem;

    @FXML
    private MenuItem selectAllAboveMenuItem;

    @FXML
    private MenuItem labelLeavesABCMenuItem;

    @FXML
    private MenuItem labelLeaves123MenuItem;

    @FXML
    private MenuItem labelLeavesMenuItem;

    @FXML
    private MenuItem labelInternalABCMenuItem;

    @FXML
    private MenuItem labelInternal123MenuItem;
    @FXML
    private MenuItem alignLeftMenuItem;

    @FXML
    private MenuItem alignCenterMenuItem;

    @FXML
    private MenuItem alignRightMenuItem;

    @FXML
    private MenuItem alignTopMenuItem;

    @FXML
    private MenuItem alignMiddleMenuItem;

    @FXML
    private MenuItem alignBottomMenuItem;

    @FXML
    private MenuItem distributeHorizontallyMenuItem;

    @FXML
    private MenuItem distributeVerticallyMenuItem;

    @FXML
    private MenuItem labelPositionLeftMenuItem;

    @FXML
    private MenuItem labelPositionRightMenuItem;

    @FXML
    private MenuItem labelPositionCenterMenuItem;


    @FXML
    private MenuItem labelPositionAboveMenuItem;

    @FXML
    private MenuItem labelPositionBelowMenuItem;

    @FXML
    private MenuItem removeDiNodesMenuItem;

    @FXML
    private MenuItem addDiNodesMenuItem;

    @FXML
    private MenuItem straightenEdgesMenuItem;

    @FXML
    private MenuItem reshapeEdgesMenuItem;

    @FXML
    private MenuItem increaseFontSizeMenuItem;

    @FXML
    private MenuItem decreaseFontSizeMenuItem;

    @FXML
    private MenuItem zoomInVerticallyMenuItem;

    @FXML
    private MenuItem zoomOutVerticallyMenuItem;

    @FXML
    private MenuItem zoomInHorizontallyMenuItem;

    @FXML
    private MenuItem zoomOutHorizontallyMenuItem;

    @FXML
    private MenuItem enterFullScreenMenuItem;

    @FXML
    private Menu windowMenu;

    @FXML
    private MenuItem aboutMenuItem;

    @FXML
    private MenuItem checkForUpdatesMenuItem;


    @FXML
    private FlowPane statusFlowPane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Pane contentPane;

    @FXML
    private ToolBar toolBar;

    @FXML
    private Button openButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button exportButton;

    @FXML
    private MenuItem loadBackgroundImageMenuItem;

    @FXML
    private MenuItem removeBackgroundImageMenuItem;

    @FXML
    private Button printButton;

    @FXML
    private TitledPane formatTitledPane;

    @FXML
    private VBox infoLabelsVBox;

    @FXML
    private BorderPane formatBorderPane;

    @FXML
    private Label selectionLabel;

    @FXML
    private MenuItem alignLeftButton;

    @FXML
    private MenuItem alignCenterButton;

    @FXML
    private MenuItem alignRightButton;

    @FXML
    private MenuItem alignTopButton;

    @FXML
    private MenuItem alignMiddleButton;

    @FXML
    private MenuItem alignBottomButton;

    @FXML
    private MenuItem distributeHorizontallyButton;

    @FXML
    private MenuItem distributeVerticallyButton;

    @FXML
    private MenuItem labelLeftButton;

    @FXML
    private MenuItem labelRightButton;

    @FXML
    private MenuItem labelCenterButton;


    @FXML
    private MenuItem labelAboveButton;

    @FXML
    private MenuItem labelBelowButton;

    @FXML
    private Button labelABCButton;

    @FXML
    private Button label123Button;

    private ZoomableScrollPane zoomableScrollPane;

    @FXML
    void initialize() {
        increaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("+", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));
        decreaseFontSizeMenuItem.setAccelerator(new KeyCharacterCombination("-", KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_ANY));

        if (ProgramProperties.isMacOS()) {
            getMenuBar().setUseSystemMenuBar(true);
            fileMenu.getItems().remove(getQuitMenuItem());
            // windowMenu.getItems().remove(getAboutMenuItem());
            //editMenu.getItems().remove(getPreferencesMenuItem());
        }

        final ArrayList<MenuItem> originalWindowMenuItems = new ArrayList<>(windowMenu.getItems());

        final InvalidationListener invalidationListener = observable -> {
            windowMenu.getItems().setAll(originalWindowMenuItems);
            int count = 0;
            for (IMainWindow mainWindow : MainWindowManager.getInstance().getMainWindows()) {
                if (mainWindow.getStage() != null) {
                    final String title = mainWindow.getStage().getTitle();
                    if (title != null) {
                        final MenuItem menuItem = new MenuItem(title.replaceAll("- " + ProgramProperties.getProgramName(), ""));
                        menuItem.setOnAction((e) -> mainWindow.getStage().toFront());
                        menuItem.setAccelerator(new KeyCharacterCombination("" + (++count), KeyCombination.SHORTCUT_DOWN));
                        windowMenu.getItems().add(menuItem);
                    }
                }
                if (MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow) != null) {
                    for (Stage auxStage : MainWindowManager.getInstance().getAuxiliaryWindows(mainWindow)) {
                        final String title = auxStage.getTitle();
                        if (title != null) {
                            final MenuItem menuItem = new MenuItem(title.replaceAll("- " + ProgramProperties.getProgramName(), ""));
                            menuItem.setOnAction((e) -> auxStage.toFront());
                            windowMenu.getItems().add(menuItem);
                        }
                    }
                }
            }
        };
        MainWindowManager.getInstance().changedProperty().addListener(invalidationListener);
        invalidationListener.invalidated(null);

        scrollPane.setContent(null);
        zoomableScrollPane = new ZoomableScrollPane(contentPane);
        ((BorderPane) scrollPane.getParent()).setCenter(zoomableScrollPane);
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }

    public VBox getTopVBox() {
        return topVBox;
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }

    public Menu getFileMenu() {
        return fileMenu;
    }

    public MenuItem getNewMenuItem() {
        return newMenuItem;
    }

    public MenuItem getOpenMenuItem() {
        return openMenuItem;
    }

    public MenuItem getImportMenuItem() {
        return importMenuItem;
    }

    public Menu getRecentMenu() {
        return recentMenu;
    }

    public MenuItem getSaveAsMenuItem() {
        return saveAsMenuItem;
    }

    public MenuItem getExportMenuItem() {
        return exportMenuItem;
    }

    public MenuItem getPrintMenuItem() {
        return printMenuItem;
    }

    public MenuItem getPageSetupMenuItem() {
        return pageSetupMenuItem;
    }

    public MenuItem getCloseMenuItem() {
        return closeMenuItem;
    }

    public MenuItem getQuitMenuItem() {
        return quitMenuItem;
    }

    public Menu getEditMenu() {
        return editMenu;
    }

    public MenuItem getUndoMenuItem() {
        return undoMenuItem;
    }

    public MenuItem getRedoMenuItem() {
        return redoMenuItem;
    }

    public MenuItem getCutMenuItem() {
        return cutMenuItem;
    }

    public MenuItem getCopyMenuItem() {
        return copyMenuItem;
    }

    public MenuItem getCopyNewickMenuItem() {
        return copyNewickMenuItem;
    }

    public MenuItem getPasteMenuItem() {
        return pasteMenuItem;
    }

    public MenuItem getDeleteMenuItem() {
        return deleteMenuItem;
    }

    public MenuItem getFindMenuItem() {
        return findMenuItem;
    }

    public MenuItem getFindAgainMenuItem() {
        return findAgainMenuItem;
    }

    public MenuItem getReplaceMenuItem() {
        return replaceMenuItem;
    }

    public MenuItem getSelectAllMenuItem() {
        return selectAllMenuItem;
    }

    public MenuItem getSelectNoneMenuItem() {
        return selectNoneMenuItem;
    }

    public MenuItem getSelectInvertMenuItem() {
        return selectInvertMenuItem;
    }

    public MenuItem getSelectTreeEdgesMenuItem() {
        return selectTreeEdgesMenuItem;
    }

    public MenuItem getSelectReticulateEdgesMenuItem() {
        return selectReticulateEdgesMenuItem;
    }

    public MenuItem getSelectRootsMenuItem() {
        return selectRootsMenuItem;
    }

    public MenuItem getSelectLeavesMenuItem() {
        return selectLeavesMenuItem;
    }

    public MenuItem getSelectTreeNodesMenuItem() {
        return selectTreeNodesMenuItem;
    }

    public MenuItem getSelectReticulateNodesMenuitem() {
        return selectReticulateNodesMenuitem;
    }

    public MenuItem getSelectVisibleNodesMenuItem() {
        return selectVisibleNodesMenuItem;
    }

    public MenuItem getSelectVisibleReticulationsMenuItem() {
        return selectVisibleReticulationsMenuItem;
    }

    public MenuItem getSelectStableNodesMenuItem() {
        return selectStableNodesMenuItem;
    }

    public MenuItem getSelectAllBelowMenuItem() {
        return selectAllBelowMenuItem;
    }

    public MenuItem getSelectAllAboveMenuItem() {
        return selectAllAboveMenuItem;
    }

    public MenuItem getLabelLeavesABCMenuItem() {
        return labelLeavesABCMenuItem;
    }

    public MenuItem getLabelLeaves123MenuItem() {
        return labelLeaves123MenuItem;
    }

    public MenuItem getLabelLeavesMenuItem() {
        return labelLeavesMenuItem;
    }

    public MenuItem getLabelInternalABCMenuItem() {
        return labelInternalABCMenuItem;
    }

    public MenuItem getLabelInternal123MenuItem() {
        return labelInternal123MenuItem;
    }

    public MenuItem getIncreaseFontSizeMenuItem() {
        return increaseFontSizeMenuItem;
    }

    public MenuItem getDecreaseFontSizeMenuItem() {
        return decreaseFontSizeMenuItem;
    }

    public MenuItem getZoomInVerticallyMenuItem() {
        return zoomInVerticallyMenuItem;
    }

    public MenuItem getZoomOutVerticallyMenuItem() {
        return zoomOutVerticallyMenuItem;
    }

    public MenuItem getZoomInHorizontallyMenuItem() {
        return zoomInHorizontallyMenuItem;
    }

    public MenuItem getZoomOutHorizontallyMenuItem() {
        return zoomOutHorizontallyMenuItem;
    }

    public MenuItem getEnterFullScreenMenuItem() {
        return enterFullScreenMenuItem;
    }

    public Menu getWindowMenu() {
        return windowMenu;
    }

    public MenuItem getAboutMenuItem() {
        return aboutMenuItem;
    }

    public MenuItem getCheckForUpdatesMenuItem() {
        return checkForUpdatesMenuItem;
    }

    public ToolBar getToolBar() {
        return toolBar;
    }

    public Button getOpenButton() {
        return openButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getExportButton() {
        return exportButton;
    }

    public Button getPrintButton() {
        return printButton;
    }

    public FlowPane getStatusFlowPane() {
        return statusFlowPane;
    }

    public Label getSelectionLabel() {
        return selectionLabel;
    }

    public VBox getInfoLabelsVBox() {
        return infoLabelsVBox;
    }

    public ZoomableScrollPane getScrollPane() {
        return zoomableScrollPane;
    }

    public TitledPane getFormatTitledPane() {
        return formatTitledPane;
    }

    public BorderPane getFormatBorderPane() {
        return formatBorderPane;
    }

    public MenuItem getAlignLeftMenuItem() {
        return alignLeftMenuItem;
    }

    public MenuItem getAlignCenterMenuItem() {
        return alignCenterMenuItem;
    }

    public MenuItem getAlignRightMenuItem() {
        return alignRightMenuItem;
    }

    public MenuItem getAlignTopMenuItem() {
        return alignTopMenuItem;
    }

    public MenuItem getAlignMiddleMenuItem() {
        return alignMiddleMenuItem;
    }

    public MenuItem getAlignBottomMenuItem() {
        return alignBottomMenuItem;
    }

    public MenuItem getDistributeHorizontallyMenuItem() {
        return distributeHorizontallyMenuItem;
    }

    public MenuItem getDistributeVerticallyMenuItem() {
        return distributeVerticallyMenuItem;
    }

    public MenuItem getLabelPositionLeftMenuItem() {
        return labelPositionLeftMenuItem;
    }

    public MenuItem getLabelPositionRightMenuItem() {
        return labelPositionRightMenuItem;
    }

    public MenuItem getLabelPositionCenterMenuItem() {
        return labelPositionCenterMenuItem;
    }

    public MenuItem getLabelPositionAboveMenuItem() {
        return labelPositionAboveMenuItem;
    }

    public MenuItem getLabelPositionBelowMenuItem() {
        return labelPositionBelowMenuItem;
    }

    public MenuItem getRemoveDiNodesMenuItem() {
        return removeDiNodesMenuItem;
    }

    public MenuItem getAddDiNodesMenuItem() {
        return addDiNodesMenuItem;
    }

    public MenuItem getStraightenEdgesMenuItem() {
        return straightenEdgesMenuItem;
    }

    public MenuItem getReshapeEdgesMenuItem() {
        return reshapeEdgesMenuItem;
    }

    public MenuItem getAlignLeftButton() {
        return alignLeftButton;
    }

    public MenuItem getAlignCenterButton() {
        return alignCenterButton;
    }

    public MenuItem getAlignRightButton() {
        return alignRightButton;
    }


    public MenuItem getAlignTopButton() {
        return alignTopButton;
    }

    public MenuItem getAlignMiddleButton() {
        return alignMiddleButton;
    }

    public MenuItem getAlignBottomButton() {
        return alignBottomButton;
    }

    public MenuItem getDistributeHorizontallyButton() {
        return distributeHorizontallyButton;
    }

    public MenuItem getDistributeVerticallyButton() {
        return distributeVerticallyButton;
    }

    public MenuItem getLabelLeftButton() {
        return labelLeftButton;
    }

    public MenuItem getLabelRightButton() {
        return labelRightButton;
    }

    public MenuItem getLabelCenterButton() {
        return labelCenterButton;
    }

    public MenuItem getLabelAboveButton() {
        return labelAboveButton;
    }

    public MenuItem getLabelBelowButton() {
        return labelBelowButton;
    }

    public MenuItem getLoadBackgroundImageMenuItem() {
        return loadBackgroundImageMenuItem;
    }

    public MenuItem getRemoveBackgroundImageMenuItem() {
        return removeBackgroundImageMenuItem;
    }

    public Button getLabelABCButton() {
        return labelABCButton;
    }

    public Button getLabel123Button() {
        return label123Button;
    }

    public Pane getContentPane() {
        return contentPane;
    }
}