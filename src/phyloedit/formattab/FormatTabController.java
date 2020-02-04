/*
 *  FormatTabController.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.formattab;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import jloda.fx.shapes.NodeShape;
import jloda.util.ProgramProperties;
import phyloedit.formattab.fontselector.FontSelector;

public class FormatTabController {
    @FXML
    private Pane pane;

    @FXML
    private Pane fontComboBoxPane;

    @FXML
    private ComboBoxBase<Font> fontComboBox;

    @FXML
    private ColorPicker labelColorPicker;

    @FXML
    private ColorPicker edgeColorPicker;

    @FXML
    private ColorPicker nodeColorPicker;

    @FXML
    private ComboBox<NodeShape> nodeShapeComboBox;

    @FXML
    private ComboBox<Double> edgeWidthComboBox;

    @FXML
    private ComboBox<Double> nodeWidthComboBox;

    @FXML
    private ComboBox<Double> nodeHeightComboBox;

    private FontSelector fontSelector;

    @FXML
    private ComboBox<String> edgeStyleCBox;

    @FXML
    void initialize() {
        fontComboBoxPane.getChildren().remove(fontComboBox);
        fontSelector = new FontSelector(ProgramProperties.getDefaultFontFX());
        fontComboBoxPane.getChildren().add(fontSelector);
    }

    public Pane getPane() {
        return pane;
    }

    public FontSelector getFontSelector() {
        return fontSelector;
    }

    public ColorPicker getLabelColorPicker() {
        return labelColorPicker;
    }

    public ColorPicker getEdgeColorPicker() {
        return edgeColorPicker;
    }

    public ColorPicker getNodeColorPicker() {
        return nodeColorPicker;
    }

    public ComboBox<NodeShape> getNodeShapeComboBox() {
        return nodeShapeComboBox;
    }

    public ComboBox<Double> getEdgeWidthComboBox() {
        return edgeWidthComboBox;
    }

    public ComboBox<Double> getNodeWidthComboBox() {
        return nodeWidthComboBox;
    }

    public ComboBox<Double> getNodeHeightComboBox() {
        return nodeHeightComboBox;
    }

    public ComboBox<String> getEdgeStyleCBox() {
        return edgeStyleCBox;
    }
}
