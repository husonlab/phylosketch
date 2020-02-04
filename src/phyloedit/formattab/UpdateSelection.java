/*
 * UpdateSelection.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.formattab;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import jloda.fx.shapes.NodeShape;
import jloda.graph.Edge;
import jloda.graph.Node;
import phyloedit.window.EdgeView;
import phyloedit.window.NodeView;
import phyloedit.window.PhyloView;

public class UpdateSelection {

    public static void apply(PhyloView phyloView, FormatTabController controller) {
        {
            Font labelFont = null;
            boolean labelFontSame = true;
            Color nodeColor = null;
            boolean nodeColorSame = true;
            Color labelColor = null;
            boolean labelColorSame = true;
            Double nodeWidth = null;
            boolean nodeWidthSame = true;
            Double nodeHeight = null;
            boolean nodeHeightSame = true;
            NodeShape nodeShape = null;
            boolean nodeShapeSame = true;

            for (Node v : phyloView.getNodeSelection().getSelectedItems()) {
                final NodeView nodeView = phyloView.getNodeView(v);
                if (nodeColorSame) {
                    if (nodeColor == null)
                        nodeColor = (Color) nodeView.getShape().getFill();
                    else if (!nodeView.getShape().getFill().equals(nodeColor)) {
                        nodeColorSame = false;
                        nodeColor = null;
                    }
                }
                if (nodeShapeSame) {
                    if (nodeShape == null) {
                        nodeShape = NodeShape.valueOf(nodeView.getShape());
                    } else if (nodeShape != NodeShape.valueOf(nodeView.getShape())) {
                        nodeShapeSame = false;
                        nodeShape = null;
                    }
                }
                if (nodeHeightSame) {
                    if (nodeHeight == null) {
                        nodeHeight = nodeView.getHeight();
                    } else if (nodeHeight != nodeView.getHeight()) {
                        nodeHeightSame = false;
                        nodeHeight = null;
                    }
                }
                if (nodeWidthSame) {
                    if (nodeWidth == null) {
                        nodeWidth = nodeView.getWidth();
                    } else if (nodeWidth != nodeView.getWidth()) {
                        nodeWidthSame = false;
                        nodeWidth = null;
                    }
                }
                final Label label = nodeView.getLabel();
                if (label.getText().length() > 0) {
                    if (labelFontSame) {
                        if (labelFont == null) {
                            labelFont = label.getFont();
                        } else if (!labelFont.equals(label.getFont())) {
                            labelFontSame = false;
                            labelFont = null;
                        }
                    }
                    if (labelColorSame) {
                        if (labelColor == null) {
                            labelColor = (Color) label.getTextFill();
                        } else if (!labelColor.equals(label.getTextFill())) {
                            labelColorSame = false;
                            labelColor = null;
                        }
                    }
                }
            }

            controller.getFontSelector().setFontValue(labelFont);
            controller.getNodeShapeComboBox().setValue(nodeShape);
            controller.getNodeColorPicker().setValue(nodeColor);
            controller.getNodeHeightComboBox().setValue(nodeHeight);
            controller.getNodeWidthComboBox().setValue(nodeWidth);
            controller.getLabelColorPicker().setValue(labelColor);
        }
        {
            Color color = null;
            boolean colorSame = true;
            Double width = null;
            boolean widthSame = true;
            Boolean arrow = null;
            boolean arrowSame = true;

            for (Edge e : phyloView.getEdgeSelection().getSelectedItems()) {
                final EdgeView edgeView = phyloView.getEdgeView(e);
                if (colorSame) {
                    if (color == null) {
                        color = (Color) edgeView.getCurve().getStroke();
                    } else if (!color.equals(edgeView.getCurve().getStroke())) {
                        colorSame = false;
                        color = null;
                    }
                }
                if (widthSame) {
                    if (width == null) {
                        width = edgeView.getCurve().getStrokeWidth();
                    } else if (width != edgeView.getCurve().getStrokeWidth()) {
                        widthSame = false;
                        width = null;
                    }
                }
                if (arrowSame) {
                    if (arrow == null) {
                        arrow = edgeView.getArrowHead().isVisible();
                    } else if (arrow != edgeView.getArrowHead().isVisible()) {
                        arrowSame = false;
                        arrow = null;
                    }
                }
            }

            controller.getEdgeWidthComboBox().setValue(width);
            controller.getEdgeColorPicker().setValue(color);
            controller.getEdgeStyleCBox().setValue(arrow != null && arrow ? "Arrow" : "Line");
        }
    }

    public static void clear(FormatTabController controller) {
        controller.getFontSelector().setFontValue(null);
        controller.getNodeShapeComboBox().setValue(null);
        controller.getNodeColorPicker().setValue(null);
        controller.getNodeHeightComboBox().setValue(null);
        controller.getNodeWidthComboBox().setValue(null);
        controller.getLabelColorPicker().setValue(null);

        controller.getEdgeWidthComboBox().setValue(null);
        controller.getEdgeColorPicker().setValue(null);
        controller.getEdgeStyleCBox().setValue("Arrow");
    }
}
