/*
 * LabelLeaves.java Copyright (C) 2020. Daniel H. Huson
 *
 * (Some code written by other authors, as named in code.)
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

import javafx.stage.Stage;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Triplet;
import phylosketch.commands.ChangeNodeLabelsCommand;
import phylosketch.window.PhyloView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * label all leaves
 * Daniel Huson, 1.2020
 */
public class LabelLeaves {

    public static List<ChangeNodeLabelsCommand.Data> labelLeavesABC(PhyloView editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));

        final List<Node> leaves = sortLeaves(editor);
        return leaves.stream().filter(v -> editor.getLabel(v).getText().length() == 0).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabelABC(seen))).collect(Collectors.toList());
    }


    public static List<ChangeNodeLabelsCommand.Data> labelLeaves123(PhyloView editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));

        final List<Node> leaves = sortLeaves(editor);
        return leaves.stream().filter(v -> editor.getLabel(v).getText().length() == 0).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabel123(seen))).collect(Collectors.toList());
    }

    public static void labelLeaves(Stage owner, PhyloView editor) {
        final List<Node> leaves = sortLeaves(editor);

        for (Node v : leaves) {
            editor.getNodeSelection().clearAndSelect(v);
            if (!NodeLabelDialog.apply(owner, editor, v))
                break;
        }
    }

    private static List<Node> sortLeaves(PhyloView editor) {
        final PhyloTree graph = editor.getGraph();
        final List<Triplet<Node, Double, Double>> list = graph.getNodesAsSet().stream().filter(v -> v.getOutDegree() == 0)
                .map(v -> new Triplet<>(v, editor.getNodeView(v).getTranslateX(), editor.getNodeView(v).getTranslateY())).collect(Collectors.toList());


        final Optional<Double> minx = list.stream().map(Triplet::getSecond).min(Double::compare);
        final Optional<Double> maxx = list.stream().map(Triplet::getSecond).max(Double::compare);

        final Optional<Double> miny = list.stream().map(Triplet::getThird).min(Double::compare);
        final Optional<Double> maxy = list.stream().map(Triplet::getThird).max(Double::compare);

        if (minx.isPresent()) {
            double dx = maxx.get() - minx.get();
            double dy = maxy.get() - miny.get();

            if (dx >= dy) {
                return list.stream().sorted(Comparator.comparingDouble(Triplet::getSecond)).map(Triplet::get1).collect(Collectors.toList());
            } else {
                return list.stream().sorted(Comparator.comparingDouble(Triplet::getThird)).map(Triplet::get1).collect(Collectors.toList());
            }

        } else
            return new ArrayList<>();
    }


    public static String getNextLabelABC(Set<String> seen) {
        int id = 0;
        String label = "A";
        while (seen.contains(label)) {
            id++;
            int letter = ('A' + (id % 26));
            int number = id / 26;
            label = (char) letter + (number > 0 ? "_" + number : "");
        }
        seen.add(label);
        return label;
    }

    public static String getNextLabel123(Set<String> seen) {
        int id = 1;
        String label = "" + id;
        while (seen.contains(label)) {
            id++;
            label = "" + id;

        }
        seen.add(label);
        return label;
    }

}
