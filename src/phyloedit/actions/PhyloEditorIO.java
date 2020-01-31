/*
 *  PhyloEditorIO.java Copyright (C) 2020 Daniel H. Huson
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

import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Shape;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import phyloedit.window.EdgeView;
import phyloedit.window.NetworkProperties;
import phyloedit.window.PhyloEditor;
import splitstree5.core.datablocks.NetworkBlock;
import splitstree5.core.datablocks.TaxaBlock;
import splitstree5.io.nexus.NetworkNexusInput;
import splitstree5.io.nexus.NetworkNexusOutput;
import splitstree5.io.nexus.TaxaNexusInput;
import splitstree5.io.nexus.TaxaNexusOutput;

import java.io.*;
import java.util.Map;

/**
 * Phylogenetic netwok I/O
 * Daniel Huson, 1.2020
 */
public class PhyloEditorIO {
    /**
     * save network with all coordinates
     *
     * @param selectedFile
     * @param editor
     */
    public static void save(File selectedFile, PhyloEditor editor) {
        final TaxaBlock taxaBlock = new TaxaBlock();

        final Map<String, Node> label2node = NetworkProperties.getLabel2Node(editor.getGraph());
        taxaBlock.addTaxaByNames(label2node.keySet());

        final PhyloTree graph = editor.getGraph();
        final NetworkBlock networkBlock = new NetworkBlock("Input", graph);
        networkBlock.setNetworkType(NetworkBlock.Type.Other);

        for (Node v : graph.nodes()) {
            final Shape shape = editor.getShape(v);
            final Label label = editor.getLabel(v);
            NetworkBlock.NodeData nodeData = networkBlock.getNodeData(v);

            nodeData.put("x", String.format("%.2f", shape.getTranslateX()));
            nodeData.put("y", String.format("%.2f", shape.getTranslateY()));
        }

        for (Edge e : graph.edges()) {
            final EdgeView edgeView = editor.getEdgeView(e);
            final CubicCurve curve = edgeView.getCurve();
            NetworkBlock.EdgeData edgeData = networkBlock.getEdgeData(e);
            edgeData.put("type", "cubic");
            edgeData.put("c1x", String.format("%.2f", curve.getControlX1()));
            edgeData.put("c1y", String.format("%.2f", curve.getControlY1()));
            edgeData.put("c2x", String.format("%.2f", curve.getControlX2()));
            edgeData.put("c2y", String.format("%.2f", curve.getControlY2()));
        }

        final TaxaNexusOutput taxaOutput = new TaxaNexusOutput();
        final NetworkNexusOutput networkOutput = new NetworkNexusOutput();

        networkBlock.setName(Basic.replaceFileSuffix(selectedFile.getName(), ""));
        try (BufferedWriter w = new BufferedWriter(new FileWriter(selectedFile))) {
            w.write("#nexus [SplitsTree5 compatible]\n\n");
            taxaOutput.write(w, taxaBlock);
            networkOutput.write(w, taxaBlock, networkBlock);
        } catch (IOException e) {
            Basic.caught(e);
        }
        ProgramProperties.put("SaveDir", selectedFile.getParent());
        editor.setDirty(false);
        editor.setFileName(selectedFile.getPath());
    }

    public static void open(Pane mainPane, PhyloEditor editor, File selectedFile) throws IOException {
        final PhyloTree graph = editor.getGraph();
        graph.clear();

        final TaxaBlock taxaBlock = new TaxaBlock();
        final NetworkBlock networkBlock = new NetworkBlock("Untitled", graph);

        final TaxaNexusInput taxaInput = new TaxaNexusInput();
        final NetworkNexusInput networkInput = new NetworkNexusInput();

        try (NexusStreamParser np = new NexusStreamParser(new FileReader(selectedFile))) {
            taxaInput.parse(np, taxaBlock);
            networkInput.parse(np, taxaBlock, networkBlock);
        }

        for (Node v : graph.nodes()) {
            final double x = Basic.parseDouble(networkBlock.getNodeData(v).get("x"));
            final double y = Basic.parseDouble(networkBlock.getNodeData(v).get("y"));
            editor.addNode(mainPane, x, y, v);
        }

        for (Edge e : graph.edges()) {
            final NetworkBlock.EdgeData edgeData = networkBlock.getEdgeData(e);
            if (edgeData.get("type").equals("cubic")) {
                editor.addEdge(e);
                final EdgeView edgeView = editor.getEdgeView(e);
                final double c1x = Basic.parseDouble(edgeData.get("c1x"));
                final double c1y = Basic.parseDouble(edgeData.get("c1y"));
                final double c2x = Basic.parseDouble(edgeData.get("c2x"));
                final double c2y = Basic.parseDouble(edgeData.get("c2y"));
                edgeView.setControlCoordinates(new double[]{c1x, c1y, c2x, c2y});
            }
        }

        ProgramProperties.put("OpenDir", selectedFile.getParent());
    }

    /**
     * export in extended Newick format
     *
     * @param owner
     * @param editor
     */
    public static void exportNewick(final Stage owner, PhyloEditor editor) {
        final File previousDir = new File(ProgramProperties.get("ExportDir", ""));
        final FileChooser fileChooser = new FileChooser();
        if (previousDir.isDirectory())
            fileChooser.setInitialDirectory(previousDir);
        fileChooser.setInitialFileName(Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(editor.getFileName()), ".newick"));
        fileChooser.setTitle("Export File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Extended Newick", "*.newick", "*.new", "*.tree", "*.tre"),
                new FileChooser.ExtensionFilter("Text", "*.txt"));
        File selectedFile = fileChooser.showSaveDialog(owner);
        if (selectedFile != null) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(selectedFile))) {
                final Node root = NetworkProperties.findRoot(editor.getGraph());
                if (root != null) {
                    editor.getGraph().setRoot(root);
                    editor.getGraph().write(w, false);
                    final ClipboardContent clipboardContent = new ClipboardContent();
                    clipboardContent.putString(w.toString() + ";");
                    Clipboard.getSystemClipboard().setContent(clipboardContent);
                }
                ProgramProperties.put("ExportDir", selectedFile.getParent());
            } catch (IOException ignored) {
            }
        }
    }
}
