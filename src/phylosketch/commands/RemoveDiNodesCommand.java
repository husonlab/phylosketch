/*
 * RemoveDiNodesCommand.java Copyright (C) 2023 Daniel H. Huson
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

import javafx.scene.layout.Pane;
import jloda.fx.control.RichTextLabel;
import jloda.fx.undo.CompositeCommand;
import jloda.graph.Node;
import phylosketch.view.PhyloView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * remove di-nodes command
 * Daniel Huson, 1.2020
 */
public class RemoveDiNodesCommand extends CompositeCommand {

    public RemoveDiNodesCommand(Pane pane, PhyloView view, Collection<Node> nodes) {
        super("Remove Di Nodes");

		var diNodes = nodes.stream().filter(v -> v.getInDegree() == 1 && v.getOutDegree() == 1 && (view.getGraph().getLabel(v) == null || RichTextLabel.getRawText(view.getGraph().getLabel(v)).isBlank()))
                .collect(Collectors.toList());

		if (!diNodes.isEmpty()) {
            add(new DeleteNodesEdgesCommand(pane, view, diNodes, Collections.emptyList()));

			{
				var set = new HashSet<>(diNodes);
				for (var v : diNodes) {
                    if (set.contains(v)) {
						var a = v;
                        while (set.contains(a)) {
                            if (a != v)
                                set.remove(a);
                            a = a.getFirstInEdge().getOpposite(a);
                        }
						var b = v;
						var e = b.getFirstInEdge();
                        while (set.contains(b)) {
                            if (b != v)
                                set.remove(b);
                            b = b.getFirstOutEdge().getOpposite(b);
							e = b.getFirstInEdge();
                        }
						if (a != v && b != v && a != b) {
							var edgeView = view.getEdgeView(e);
							add(new CreateEdgeCommand(view, a, b, edgeView.getArrowHead().isVisible(), edgeView.getCurve().getStrokeWidth(), edgeView.getCurve().getStroke()));
							//add(new CreateEdgeCommand(view, a,b));
						}
                    }
                }
            }
        }
    }
}
