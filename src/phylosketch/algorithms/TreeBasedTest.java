/*
 * TreeBasedTest.java Copyright (C) 2022 Daniel H. Huson
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

package phylosketch.algorithms;

import jloda.graph.EdgeSet;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.phylo.algorithms.OffspringGraphMatching;
import jloda.util.FileLineIterator;
import jloda.util.StringUtils;
import jloda.util.UsageException;
import jloda.util.progress.ProgressPercentage;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * test whether given networks are tree-based and reports the discrepancy, if not
 * Daniel Huson, 1.2020
 */
public class TreeBasedTest {

    /**
     * main program
     *
	 */
    public static void main(String[] args) throws IOException, UsageException {
        if (args.length != 1)
            throw new UsageException("Expected input file");
        if (args[0].equals("-h")) {
            System.err.println("Usage: TreeBasedTest file");
            return;
        }
        final File inputFile = new File(args[0]);

        try (FileLineIterator it = new FileLineIterator(inputFile)) {
            int count = 0;
            int treeBased = 0;
            while (it.hasNext()) {
                final String line = it.next().trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    final PhyloTree tree = new PhyloTree();
                    tree.read(new StringReader(line));

                    if (count == 0) {
                        System.err.println("Special edges " + tree.getNumberReticulateEdges() + ": "
                                           + StringUtils.toString(tree.edgeStream().filter(tree::isReticulateEdge).map(e -> "(" + e.getSource().getId() + "," + e.getTarget().getId() + ")").collect(Collectors.toList()), ","));
                        for (Node v : tree.nodes()) {
                            System.err.println("node: " + v.getId() + " children: "
                                               + StringUtils.toString(StreamSupport.stream(v.children().spliterator(), false).map(Node::getId).collect(Collectors.toList()), ",")
                                               + (tree.getLabel(v) != null ? " label: " + tree.getLabel(v) : ""));
                        }
                        System.err.println(tree.toBracketString(false));
                    }

                    System.out.println("------- Network " + (++count + " ---------"));
                    //System.out.println("Number of taxa:   " + tree.getNumberOfWorkingTaxonIds());
                    System.out.println("Number of leaves: " + tree.countLeaves());
                    System.out.println("Number of nodes:  " + tree.getNumberOfNodes());
                    System.out.println("Number of edges:  " + tree.getNumberOfEdges());

                    final EdgeSet matching = OffspringGraphMatching.compute(tree, new ProgressPercentage());

                    System.out.println("Size of matching: " + matching.size());
                    if (OffspringGraphMatching.isTreeBased(tree, matching)) {
                        treeBased++;
                        System.out.println("Network is tree-based");
                    } else {
                        System.out.println("Network is NOT tree-based, discrepancy: " + OffspringGraphMatching.discrepancy(tree, matching));
                    }
                }
            }
            if (count > 1) {
                System.out.println("Total tree-based: " + treeBased + " of " + count);
            }
        }
    }
}
