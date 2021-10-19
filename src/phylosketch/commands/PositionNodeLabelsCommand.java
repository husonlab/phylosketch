/*
 * PositionNodeLabelsCommand.java Copyright (C) 2021. Daniel H. Huson
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

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.util.Duration;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.util.NumberUtils;
import jloda.util.ProgramProperties;
import phylosketch.window.NodeView;
import phylosketch.window.PhyloView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * change label positions
 * daniel huson, 2.2020
 */
public class PositionNodeLabelsCommand extends UndoableRedoableCommand {

    public enum Position {
        Above, Below, Left, Right, Center;

        public static Position getDefault(PhyloView.RootLocation rootLocation) {
            switch (rootLocation) {
                default:
                case Top:
                    return Below;
                case Bottom:
                    return Above;
                case Left:
                    return Right;
                case Right:
                    return Left;
            }
        }
    }

    private final Runnable undo;
    private final Runnable redo;

    /**
     * change node positions
     *
     * @param view
     * @param position
     */
    public PositionNodeLabelsCommand(PhyloView view, Collection<Node> nodes, Position position) {
        super("Label Position");

        final ArrayList<Data> dataList = new ArrayList<>();

        for (Node v : nodes) {
            final double nodeWidth = view.getNodeView(v).getWidth();
            final double nodeHeight = view.getNodeView(v).getHeight();
            final RichTextLabel label = view.getNodeView(v).getLabel();

			final boolean horizontalLabel = !(NumberUtils.equals(label.getRotate(), 90, 0.00001) || NumberUtils.equals(label.getRotate(), 270, 0.00001));

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
        }

        undo = () -> animatePositionChange(view, false, dataList);

        redo = () -> animatePositionChange(view, true, dataList);
    }

    private void animatePositionChange(PhyloView view, boolean toNew, Collection<Data> dataList) {
        if (dataList.size() < ProgramProperties.get("AnimationLimit", 5000)) {
            final Animation animation = new Transition() {
                {
                    setCycleDuration(Duration.millis(500));
                }

                @Override
                protected void interpolate(double frac) {
                    final double p = (toNew ? 1.0 - frac : frac);
                    final double q = (toNew ? frac : 1.0 - frac);

                    for (Data data : dataList) {
                        final NodeView nv = view.getNodeView(view.getGraph().findNodeById(data.id));
                        nv.getLabel().setLayoutX(p * data.oldX + q * data.newX);
                        nv.getLabel().setLayoutY(p * data.oldY + q * data.newY);
                    }
                }
            };
            animation.play();
        } else {
            for (Data data : dataList) {
                final NodeView nv = view.getNodeView(view.getGraph().findNodeById(data.id));
                nv.getLabel().setLayoutX(toNew ? data.newX : data.oldX);
                nv.getLabel().setLayoutY(toNew ? data.newY : data.oldY);
            }
        }
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
