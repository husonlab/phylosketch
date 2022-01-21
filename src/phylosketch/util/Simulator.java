/*
 * Simulator.java Copyright (C) 2022 Daniel H. Huson
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

import jloda.graph.Node;
import jloda.phylo.PhyloTree;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Simulator {
    public static void main(String[] args) throws IOException {
        var size = 100000;

        var nodes = new Node[size];

        var tree = new PhyloTree();
        for (var i = 0; i < nodes.length; i++) {
            nodes[i] = tree.newNode();
        }
        tree.setRoot(nodes[0]);

        var random = new Random(666);

        // makes a tree:
        for (var i = nodes.length - 1; i > 0; i--) {
            var j = (i > 1 ? random.nextInt(i) : 0);
            tree.newEdge(nodes[j], nodes[i]);
        }
        // additional incoming edges to nodes that have degree 2
        for (var i = nodes.length - 1; i > 0; i--) {
            var v = nodes[i];
            if (v.getDegree() == 2) {
                for (var attempt = 0; attempt < i; attempt++) { // try i times to find a predecessor to which we are not already connected to
                    var j = (i > 1 ? random.nextInt(i) : 0);
                    var u = nodes[j];
                    if (!v.isAdjacent(u)) {
                        tree.newEdge(u, v);
                        break;
                    }
                }
            }
        }

        var reticulateCount = 0;
        for (var v : tree.nodes()) {
            if (v.getInDegree() > 1) {
                v.setLabel("#H" + (++reticulateCount));
            }
        }

        var leafCount = 0;
        for (var v : tree.nodes()) {
            if (v.getOutDegree() == 0)
                v.setLabel("t" + (++leafCount));
        }

        System.err.println(tree.toBracketString(false) + ";");

        try (var w = new FileWriter("/Users/huson/tmp/network-" + size + ".tre")) {
            w.write(tree.toBracketString(false) + ";\n");
        }
    }
}
