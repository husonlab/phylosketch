/*
 * AlignNodesCommand.java Copyright (C) 2020 Daniel H. Huson
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

/*
 * AlignNodesCommand.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.commands;

import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phyloedit.window.NodeView;
import phyloedit.window.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * change label positions
 * daniel huson, 2.2020
 */
public class PositionNodeLabelsCommand extends UndoableRedoableCommand {
    public enum Position {Above, Below, Left, Right}

    private final Runnable undo;
    private final Runnable redo;

    public PositionNodeLabelsCommand(PhyloView view, Collection<Node> nodes, Position position) {
        super("Label Position");

        final ArrayList<Data> dataList = new ArrayList<>();

        nodes.forEach(v -> {
            final NodeView nv = view.getNodeView(v);
            switch (position) {
                case Above: {
                    dataList.add(new Data(v.getId(), nv.getLabel().getLayoutX(), -0.5 * nv.getLabel().getWidth(), nv.getLabel().getLayoutY(), -(0.5 * nv.getHeight() + nv.getLabel().getHeight() + 5)));
                    break;
                }
                case Below: {
                    dataList.add(new Data(v.getId(), nv.getLabel().getLayoutX(), -0.5 * nv.getLabel().getWidth(), nv.getLabel().getLayoutY(), +(0.5 * nv.getHeight() - 5)));
                    break;
                }
                case Left: {
                    dataList.add(new Data(v.getId(), nv.getLabel().getLayoutX(), -(0.5 * nv.getWidth() + nv.getLabel().getWidth() + 5), nv.getLabel().getLayoutY(), -0.5 * nv.getLabel().getHeight()));
                    break;
                }
                case Right: {
                    dataList.add(new Data(v.getId(), nv.getLabel().getLayoutX(), (0.5 * nv.getWidth() + 5), nv.getLabel().getLayoutY(), -0.5 * nv.getLabel().getHeight()));
                    break;
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
    }

}
