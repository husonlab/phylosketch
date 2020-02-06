/*
 * EdgeContextMenu.java Copyright (C) 2020 Daniel H. Huson
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
 * EdgeContextMenu.java Copyright (C) 2020 Daniel H. Huson
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

package phylosketch.util;

import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve;
import jloda.graph.Edge;
import phylosketch.commands.SplitEdgeCommand;
import phylosketch.window.EdgeView;
import phylosketch.window.PhyloView;

/**
 * edge context menu
 * Daniel Huson, 2.020
 */
public class EdgeContextMenu {
    public static void setup(Pane pane, PhyloView view, Edge e) {

        final EdgeView ev = view.getEdgeView(e);
        final CubicCurve curve = ev.getCurve();

        curve.setOnContextMenuRequested(c -> {
            final Point2D screenLocation = new Point2D(c.getScreenX(), c.getScreenY());
            Point2D locationLocation = pane.screenToLocal(screenLocation);

            final MenuItem split = new MenuItem("Split");
            split.setOnAction(s -> view.getUndoManager().doAndAdd(new SplitEdgeCommand(pane, view, e, locationLocation)));
            final ContextMenu menu = new ContextMenu();
            menu.getItems().add(split);
            menu.show(pane, screenLocation.getX(), screenLocation.getY());
        });
    }
}
