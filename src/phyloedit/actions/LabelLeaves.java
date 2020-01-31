/*
 *  LabelLeaves.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.actions;

import javafx.stage.Stage;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Triplet;
import phyloedit.window.PhyloEditor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * label all leaves
 * Daniel Huson, 1.2020
 */
public class LabelLeaves {

    public static void labelLeavesABC(PhyloEditor editor) {
        final PhyloTree graph = editor.getGraph();

        final List<Node> leaves = sortLeaves(editor);

        final Set<String> seen = new HashSet<>();
        leaves.stream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));

        leaves.forEach(v -> editor.getLabel(v).setText(getNextLabelABC(seen)));
    }

    public static void labelLeaves123(PhyloEditor editor) {
        final PhyloTree graph = editor.getGraph();

        final List<Node> leaves = sortLeaves(editor);

        final Set<String> seen = new HashSet<>();
        leaves.stream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));

        leaves.forEach(v -> editor.getLabel(v).setText(getNextLabel123(seen)));
    }

    public static void labelLeaves(Stage owner, PhyloEditor editor) {
        final List<Node> leaves = sortLeaves(editor);

        for (Node v : leaves) {
            editor.getNodeSelection().clearAndSelect(v);
            if (!NodeLabelDialog.apply(owner, editor, v))
                break;
        }
    }

    private static List<Node> sortLeaves(PhyloEditor editor) {
        final PhyloTree graph = editor.getGraph();
        final List<Triplet<Node, Double, Double>> list = graph.getNodesAsSet().stream().filter(v -> v.getOutDegree() == 0)
                .map(v -> new Triplet<>(v, editor.getShape(v).getTranslateX(), editor.getShape(v).getTranslateY())).collect(Collectors.toList());


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
