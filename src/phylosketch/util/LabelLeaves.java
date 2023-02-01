/*
 * LabelLeaves.java Copyright (C) 2023 Daniel H. Huson
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

package phylosketch.util;

import javafx.stage.Stage;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Pair;
import phylosketch.commands.ChangeNodeLabelsCommand;
import phylosketch.view.PhyloView;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        return sortLeaves(editor).stream().filter(v -> editor.getLabel(v).getRawText().length() == 0).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabelABC(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> labelInternalABC(PhyloView editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));
        return sortInternal(editor).stream().filter(v -> editor.getLabel(v).getRawText().length() == 0).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabelABC(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> labelLeaves123(PhyloView editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));
        return sortLeaves(editor).stream().filter(v -> editor.getLabel(v).getRawText().length() == 0).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabel123(seen))).collect(Collectors.toList());
    }

    public static List<ChangeNodeLabelsCommand.Data> labelInternal123(PhyloView editor) {
        final PhyloTree graph = editor.getGraph();

        final Set<String> seen = new HashSet<>();
        graph.nodeStream().filter(v -> graph.getLabel(v) != null).forEach(v -> seen.add(graph.getLabel(v)));
        return sortInternal(editor).stream().filter(v -> editor.getLabel(v).getRawText().length() == 0).map(v -> new ChangeNodeLabelsCommand.Data(v.getId(), editor.getLabel(v).getText(), getNextLabel123(seen))).collect(Collectors.toList());
    }

    public static void labelLeaves(Stage owner, PhyloView editor) {
        final List<Node> leaves = sortLeaves(editor);

        for (Node v : leaves) {
            editor.getNodeSelection().clearAndSelect(v);
            if (!NodeLabelDialog.apply(owner, editor, v))
                break;
        }
    }

    private static List<Node> sortLeaves(PhyloView phyloView) {
        final PhyloTree graph = phyloView.getGraph();

        final List<Pair<Node, Double>> list;
        if (phyloView.computeRootLocation().isHorizontal())
            list = graph.nodeStream().filter(v -> v.getOutDegree() == 0).map(v -> new Pair<>(v, phyloView.getNodeView(v).getTranslateY())).collect(Collectors.toList());
        else
            list = graph.nodeStream().filter(v -> v.getOutDegree() == 0).map(v -> new Pair<>(v, phyloView.getNodeView(v).getTranslateX())).collect(Collectors.toList());

        return list.stream().sorted(Comparator.comparingDouble(Pair::getSecond)).map(Pair::getFirst).collect(Collectors.toList());
    }

    private static List<Node> sortInternal(PhyloView phyloView) {
        final PhyloTree graph = phyloView.getGraph();

        final List<Pair<Node, Double>> list;
        if (phyloView.computeRootLocation().isHorizontal())
            list = graph.nodeStream().filter(v -> v.getOutDegree() > 0).map(v -> new Pair<>(v, phyloView.getNodeView(v).getTranslateY())).collect(Collectors.toList());
        else
            list = graph.nodeStream().filter(v -> v.getOutDegree() > 0).map(v -> new Pair<>(v, phyloView.getNodeView(v).getTranslateX())).collect(Collectors.toList());

        return list.stream().sorted(Comparator.comparingDouble(Pair::getSecond)).map(Pair::getFirst).collect(Collectors.toList());
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
