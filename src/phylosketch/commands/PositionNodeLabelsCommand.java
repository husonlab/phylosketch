/*
 * PositionNodeLabelsCommand.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.commands;

import javafx.scene.control.Label;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.util.Basic;
import phylosketch.window.NodeView;
import phylosketch.window.PhyloView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * change label positions
 * daniel huson, 2.2020
 */
public class PositionNodeLabelsCommand extends UndoableRedoableCommand {
    public enum Position {Above, Below, Left, Right, Center}

    private final Runnable undo;
    private final Runnable redo;

    /**
     * change node positions
     *
     * @param view
     * @param position
     */
    public PositionNodeLabelsCommand(PhyloView view, Position position) {
        super("Label Position");

        final Stream<Node> nodes;
        if (view.getNodeSelection().size() > 0)
            nodes = view.getNodeSelection().getSelectedItems().stream();
        else
            nodes = view.getGraph().nodeStream();

        final ArrayList<Data> dataList = new ArrayList<>();

        nodes.forEach(v -> {
            final double nodeWidth = view.getNodeView(v).getWidth();
            final double nodeHeight = view.getNodeView(v).getHeight();
            final Label label = view.getNodeView(v).getLabel();
            
            final boolean horizontalLabel = !(Basic.equals(label.getRotate(), 90, 0.00001) || Basic.equals(label.getRotate(), 270, 0.00001));

            if (horizontalLabel) {

                switch (position) {
                    case Above: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -0.5 * label.getWidth(), label.getLayoutY(), -(0.5 * label.getHeight() + label.getHeight() + 5)));
                        break;
                    }
                    case Below: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -0.5 * label.getWidth(), label.getLayoutY(), +(0.5 * nodeHeight + 5)));
                        break;
                    }
                    case Left: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -(0.5 * nodeWidth + label.getWidth() + 5), label.getLayoutY(), -0.5 * label.getHeight()));
                        break;
                    }
                    case Right: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), (0.5 * nodeWidth + 5), label.getLayoutY(), -0.5 * label.getHeight()));
                        break;
                    }
                    case Center: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -0.5 * label.getWidth(), label.getLayoutY(), -0.5 * label.getHeight()));
                        break;
                    }
                }
            } else {
                switch (position) {
                    case Above: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -0.5 * label.getWidth(), label.getLayoutY(), -(8 + nodeHeight + 0.5 * label.getWidth())));
                        break;
                    }
                    case Below: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -0.5 * label.getWidth(), label.getLayoutY(), 5 + 0.5 * label.getWidth()));
                        break;
                    }
                    case Left: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -(0.5 * nodeWidth + label.getWidth() + 8) + 0.5 * label.getWidth(), label.getLayoutY(), -0.5 * label.getHeight()));
                        break;
                    }
                    case Right: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), 0.5 * nodeWidth + 8 - 0.5 * label.getWidth(), label.getLayoutY(), -0.5 * label.getHeight()));
                        break;
                    }
                    case Center: {
                        dataList.add(new Data(v.getId(), label.getLayoutX(), -0.5 * label.getWidth(), label.getLayoutY(), -0.5 * label.getHeight()));
                        break;
                    }
                }
            }
        });

        undo = () -> {
            for (Data data : dataList) {
                final NodeView nv = view.getNodeView(view.getGraph().searchNodeId(data.id));
                nv.getLabel().setLayoutX(data.oldX);
                nv.getLabel().setLayoutY(data.oldY);
            }
        };

        redo = () -> {
            for (Data data : dataList) {
                final NodeView nv = view.getNodeView(view.getGraph().searchNodeId(data.id));
                nv.getLabel().setLayoutX(data.newX);
                nv.getLabel().setLayoutY(data.newY);
            }
        };
    }

    @Override
    public void undo() {
        undo.run();
    }

    @Override
    public void redo() {
        redo.run();
    }

    private static class Data {
        final int id;
        final double oldX;
        final double newX;
        final double oldY;
        final double newY;

        public Data(int id, double oldX, double newX, double oldY, double newY) {
            this.id = id;
            this.oldX = oldX;
            this.newX = newX;
            this.oldY = oldY;
            this.newY = newY;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Data)) return false;
            Data data = (Data) o;
            return id == data.id &&
                    Double.compare(data.oldX, oldX) == 0 &&
                    Double.compare(data.newX, newX) == 0 &&
                    Double.compare(data.oldY, oldY) == 0 &&
                    Double.compare(data.newY, newY) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, oldX, newX, oldY, newY);
        }
    }
}
