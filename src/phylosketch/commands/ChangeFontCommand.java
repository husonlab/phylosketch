/*
 * ChangeFontCommand.java Copyright (C) 2021. Daniel H. Huson
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

import javafx.scene.text.Font;
import jloda.fx.undo.UndoableRedoableCommand;
import jloda.graph.Node;
import phylosketch.window.PhyloView;

import java.util.ArrayList;
import java.util.Collection;

/**
 * change font
 * Daniel Huson, 2.2020
 */
public class ChangeFontCommand extends UndoableRedoableCommand {
    private final Runnable undo;
    private final Runnable redo;
    private final ArrayList<Data> dataList = new ArrayList<>();

    public ChangeFontCommand(PhyloView editor, Collection<Node> nodes, Font font) {
        super("Font");

        for (Node v : nodes) {
            dataList.add(new Data(v.getId(), editor.getLabel(v).getFont(), font));
        }

        undo = () -> {
            for (Data data : dataList) {
                editor.getLabel(editor.getGraph().findNodeById(data.id)).setFont(data.oldValue);

            }
        };

        redo = () -> {
            for (Data data : dataList) {
                editor.getLabel(editor.getGraph().findNodeById(data.id)).setFont(data.newValue);

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

    static class Data {
        final int id;
        final Font oldValue;
        final Font newValue;

        public Data(int id, Font oldValue, Font newValue) {
            this.id = id;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
