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

package phyloedit.window;/*
 *  PhyloEditorWindowController.java Copyright (C) 2020 Daniel H. Huson
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

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import jloda.fx.control.ZoomableScrollPane;
import jloda.util.ProgramProperties;

public class PhyloEditorWindowController {

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
    private MenuItem selectAllMenuItem;

    @FXML
    private MenuItem selectNoneMenuItem;

    @FXML
    private MenuItem selectInvertMenuItem;

    @FXML
    private MenuItem selectLeavesMenuItem;

    @FXML
    private MenuItem selectTreeNodesMenuItem;

    @FXML
    private MenuItem selectReticulateNodesMenuitem;

    @FXML
    private MenuItem selectStableNodesMenuItem;

    @FXML
    private MenuItem selectVisibleNodesMenuItem;

    @FXML
    private MenuItem selectAllBelowMenuItem;

    @FXML
    private MenuItem selectAllAboveMenuItem;

    @FXML
    private MenuItem enterFullScreenMenuItem;

    @FXML
    private MenuItem labelLeavesABCMenuItem;

    @FXML
    private MenuItem labelLeaves123MenuItem;

    @FXML
    private MenuItem labelLeavesMenuItem;

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
    private Menu windowMenu;

    @FXML
    private MenuItem aboutMenuItem;

    @FXML
    private ToolBar toolBar;

    @FXML
    private Button openButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button printButton;

    @FXML
    private FlowPane statusFlowPane;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Pane mainPane;

    @FXML
    private Label selectionLabel;

    @FXML
    private Label infoLabel;

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


        zoomableScrollPane = new ZoomableScrollPane(scrollPane.getContent());
        scrollPane.setContent(null);
        borderPane.setCenter(zoomableScrollPane);
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

    public MenuItem getSelectAllMenuItem() {
        return selectAllMenuItem;
    }

    public MenuItem getSelectNoneMenuItem() {
        return selectNoneMenuItem;
    }

    public MenuItem getSelectInvertMenuItem() {
        return selectInvertMenuItem;
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

    public MenuItem getSelectStableNodesMenuItem() {
        return selectStableNodesMenuItem;
    }

    public MenuItem getSelectVisibleNodesMenuItem() {
        return selectVisibleNodesMenuItem;
    }

    public MenuItem getSelectAllBelowMenuItem() {
        return selectAllBelowMenuItem;
    }

    public MenuItem getSelectAllAboveMenuItem() {
        return selectAllAboveMenuItem;
    }

    public MenuItem getEnterFullScreenMenuItem() {
        return enterFullScreenMenuItem;
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

    public Menu getWindowMenu() {
        return windowMenu;
    }

    public MenuItem getAboutMenuItem() {
        return aboutMenuItem;
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

    public ZoomableScrollPane getScrollPane() {
        return zoomableScrollPane;
    }

    public Pane getMainPane() {
        return mainPane;
    }

    public Label getSelectionLabel() {
        return selectionLabel;
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }

    public Label getInfoLabel() {
        return infoLabel;
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

    public ZoomableScrollPane getZoomableScrollPane() {
        return zoomableScrollPane;
    }
}