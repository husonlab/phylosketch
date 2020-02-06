/*
 *  FormatTab.java Copyright (C) 2020 Daniel H. Huson
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

package phylosketch.formattab;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.converter.DoubleStringConverter;
import jloda.fx.control.ItemSelectionModel;
import jloda.fx.shapes.NodeShape;
import jloda.fx.util.ExtendedFXMLLoader;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.util.Single;
import phylosketch.commands.*;
import phylosketch.window.MainWindow;
import phylosketch.window.PhyloView;

import java.io.IOException;

/**
 * style tab for setting fonts etc
 * Daniel Huson, 1.2018
 */
public class FormatTab extends StackPane {
    private final FormatTabController controller;

    /**
     * constructor
     *
     * @param window
     * @throws IOException
     */
    public FormatTab(MainWindow window) {
        {
            final ExtendedFXMLLoader<FormatTabController> extendedFXMLLoader = new ExtendedFXMLLoader<>(this.getClass());
            controller = extendedFXMLLoader.getController();
            getChildren().add(extendedFXMLLoader.getRoot());
        }
        //setStyle("-fx-effect: dropshadow;");

        controller.getPane().setOnMouseClicked(c -> controller.getPane().requestFocus());

        controller.getNodeShapeComboBox().getItems().addAll(NodeShape.values());
        controller.getNodeWidthComboBox().getItems().addAll(1.0, 3.0, 5.0, 10.0, 20.0, 40.0, 80.0);
        controller.getNodeHeightComboBox().getItems().addAll(1.0, 3.0, 5.0, 10.0, 20.0, 40.0, 80.0);
        controller.getEdgeWidthComboBox().getItems().addAll(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 20.0);

        controller.getNodeColorPicker().setValue(Color.WHITE);
        controller.getLabelColorPicker().setValue(Color.BLACK);

        controller.getNodeWidthComboBox().setConverter(new DoubleStringConverter());
        controller.getNodeWidthComboBox().setValue(1.0);

        controller.getNodeHeightComboBox().setConverter(new DoubleStringConverter());
        controller.getNodeHeightComboBox().setValue(1.0);

        controller.getEdgeWidthComboBox().setConverter(new DoubleStringConverter());
        controller.getEdgeWidthComboBox().setValue(1.0);
        controller.getEdgeColorPicker().setValue(Color.BLACK);

        final PhyloView phyloView = window.getView();

        final ItemSelectionModel<Node> nodeSelection = phyloView.getNodeSelection();
        final ItemSelectionModel<Edge> edgeSelection = phyloView.getEdgeSelection();

        final Single<Integer> updating = new Single<>(0);

        controller.getPane().setOnMouseEntered(c -> {
            updating.set(updating.get() + 1);
            try {
                UpdateSelection.apply(phyloView, controller);
            } finally {
                updating.set(updating.get() - 1);
            }
        });
        controller.getPane().setOnMouseExited(c -> {
            updating.set(updating.get() + 1);
            try {
                UpdateSelection.clear(controller);
            } finally {
                updating.set(updating.get() - 1);
            }
        });

        controller.getEdgeStyleCBox().getItems().addAll("Line", "Arrow");

        controller.getFontSelector().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0) {
                phyloView.getUndoManager().doAndAdd(new ChangeFontCommand(phyloView, nodeSelection.getSelectedItems(), controller.getFontSelector().getFontValue()));
            }
        });

        controller.getNodeShapeComboBox().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeNodeShapeCommand(phyloView, nodeSelection.getSelectedItems(), n));

        });
        controller.getNodeShapeComboBox().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getNodeHeightComboBox().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeNodeHeightCommand(phyloView, nodeSelection.getSelectedItems(), n));
        });
        controller.getNodeHeightComboBox().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getNodeWidthComboBox().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeNodeWidthCommand(phyloView, nodeSelection.getSelectedItems(), n));
        });
        controller.getNodeWidthComboBox().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getNodeColorPicker().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeNodeColorCommand(phyloView, nodeSelection.getSelectedItems(), n));

        });
        controller.getNodeColorPicker().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getLabelColorPicker().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeLabelColorCommand(phyloView, nodeSelection.getSelectedItems(), n));
        });
        controller.getLabelColorPicker().disableProperty().bind(nodeSelection.emptyProperty());

        controller.getEdgeWidthComboBox().valueProperty().addListener((c, o, n) ->
        {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeEdgeWidthCommand(phyloView, edgeSelection.getSelectedItems(), n));
        });
        controller.getEdgeWidthComboBox().disableProperty().bind(edgeSelection.emptyProperty());

        controller.getEdgeColorPicker().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeEdgeColorCommand(phyloView, edgeSelection.getSelectedItems(), n));
        });
        controller.getEdgeColorPicker().disableProperty().bind(edgeSelection.emptyProperty());

        controller.getEdgeStyleCBox().valueProperty().addListener((c, o, n) -> {
            if (updating.get() == 0)
                phyloView.getUndoManager().doAndAdd(new ChangeEdgeStyleCommand(phyloView, edgeSelection.getSelectedItems(), n.equals("Arrow")));
        });
        controller.getEdgeStyleCBox().disableProperty().bind(edgeSelection.emptyProperty());
    }
}
