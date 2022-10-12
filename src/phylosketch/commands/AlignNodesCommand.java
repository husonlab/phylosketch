/*
 * AlignNodesCommand.java Copyright (C) 2022 Daniel H. Huson
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

package phylosketch.commands;

import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.util.Duration;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import jloda.util.ProgramProperties;
import phylosketch.view.NodeView;
import phylosketch.view.PhyloView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * align nodes command
 * daniel huson, 2.2020
 */
public class AlignNodesCommand extends UndoableRedoableCommand {
    public enum Alignment {Top, Middle, Bottom, Left, Center, Right}

    private final Runnable undo;
    private final Runnable redo;

    public AlignNodesCommand(PhyloView view, Collection<Node> nodes, Alignment alignment) {
        super("Node Alignment");

        final ArrayList<Data> dataList = new ArrayList<>();

        final Optional<Double> minX = nodes.stream().map(v -> view.getNodeView(v).getTranslateX()).min(Double::compare);
        final Optional<Double> maxX = nodes.stream().map(v -> view.getNodeView(v).getTranslateX()).max(Double::compare);
        final Optional<Double> minY = nodes.stream().map(v -> view.getNodeView(v).getTranslateY()).min(Double::compare);
        final Optional<Double> maxY = nodes.stream().map(v -> view.getNodeView(v).getTranslateY()).max(Double::compare);

        if (minX.isPresent() && maxX.isPresent() && minY.isPresent() && maxY.isPresent()) {
            switch (alignment) {
                case Top: {
                    nodes.forEach(v -> dataList.add(new Data(v.getId(), view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY(), 0, minY.get() - view.getNodeView(v).getTranslateY())));
                    break;
                }
                case Middle: {
                    final double middle = (0.5 * (minY.get() + maxY.get()));
                    nodes.forEach(v -> dataList.add(new Data(v.getId(), view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY(), 0, middle - view.getNodeView(v).getTranslateY())));
                    break;
                }
                case Bottom: {
                    nodes.forEach(v -> dataList.add(new Data(v.getId(), view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY(), 0, maxY.get() - view.getNodeView(v).getTranslateY())));

                    break;
                }
                case Left: {
                    nodes.forEach(v -> dataList.add(new Data(v.getId(), view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY(), minX.get() - view.getNodeView(v).getTranslateX(), 0)));
                    break;
                }
                case Center: {
                    final double center = (0.5 * (minX.get() + maxX.get()));
                    nodes.forEach(v -> dataList.add(new Data(v.getId(), view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY(), center - view.getNodeView(v).getTranslateX(), 0)));
                    break;
                }
                case Right: {
                    nodes.forEach(v -> dataList.add(new Data(v.getId(), view.getNodeView(v).getTranslateX(), view.getNodeView(v).getTranslateY(), maxX.get() - view.getNodeView(v).getTranslateX(), 0)));
                    break;
                }
            }
        }

        undo = () -> animateAlign(view, dataList, true);

        redo = () -> animateAlign(view, dataList, false);
    }

    private void animateAlign(PhyloView view, ArrayList<Data> dataList, boolean back) {
        if (dataList.size() < ProgramProperties.get("AnimationLimit", 5000)) {
            final Animation animation = new Transition() {
                {
                    setCycleDuration(Duration.millis(500));
                }

                @Override
                protected void interpolate(double p) {
                    final double q = 1.0 - p;
                    for (Data data : dataList) {
                        final NodeView nv = view.getNodeView(view.getGraph().findNodeById(data.id));
                        nv.setTranslateX((back ? p : q) * data.oldX + (back ? q : p) * data.newX);
                        nv.setTranslateY((back ? p : q) * data.oldY + (back ? q : p) * data.newY);
                    }

                }
            };
            animation.play();
        } else {
            for (Data data : dataList) {
                final NodeView nv = view.getNodeView(view.getGraph().findNodeById(data.id));
                nv.setTranslateX(back ? data.oldX : data.newX);
                nv.setTranslateY(back ? data.oldY : data.newY);
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
        final double oldY;
        final double newX;
        final double newY;

        public Data(int id, double oldX, double oldY, double dx, double dy) {
            this.id = id;
            this.oldX = oldX;
            this.oldY = oldY;
            this.newX = oldX + dx;
            this.newY = oldY + dy;
        }
    }
}
