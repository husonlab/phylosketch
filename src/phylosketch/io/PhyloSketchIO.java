/*
 * PhyloSketchIO.java Copyright (C) 2021. Daniel H. Huson
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

package phylosketch.io;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.fx.control.RichTextLabel;
import jloda.fx.shapes.NodeShape;
import jloda.fx.util.FontUtils;
import jloda.graph.Edge;
import jloda.graph.Node;
import jloda.phylo.PhyloTree;
import jloda.util.Basic;
import jloda.util.FileUtils;
import jloda.util.ProgramProperties;
import jloda.util.StringUtils;
import jloda.util.parse.NexusStreamParser;
import org.xml.sax.SAXException;
import phylosketch.embed.RootedNetworkEmbedder;
import phylosketch.window.EdgeView;
import phylosketch.window.NetworkProperties;
import phylosketch.window.NodeView;
import phylosketch.window.PhyloView;
import splitstree5.core.datablocks.NetworkBlock;
import splitstree5.core.datablocks.TaxaBlock;
import splitstree5.io.imports.NeXML.handlers.NexmlTreesHandler;
import splitstree5.io.nexus.NetworkNexusInput;
import splitstree5.io.nexus.NetworkNexusOutput;
import splitstree5.io.nexus.TaxaNexusInput;
import splitstree5.io.nexus.TaxaNexusOutput;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.Map;

/**
 * Phylogenetic netwok I/O
 * Daniel Huson, 1.2020
 */
public class PhyloSketchIO {
    /**
     * save network with all coordinates
     *
     * @param selectedFile
     * @param editor
     */
    public static void save(File selectedFile, PhyloView editor) {
        final TaxaBlock taxaBlock = new TaxaBlock();

        final Map<String, Node> label2node = NetworkProperties.getLabel2Node(editor.getGraph());
        taxaBlock.addTaxaByNames(label2node.keySet());

        final PhyloTree graph = editor.getGraph();
        final NetworkBlock networkBlock = new NetworkBlock("Input", graph);
        networkBlock.setNetworkType(NetworkBlock.Type.Other);

        for (Node v : graph.nodes()) {
            final NodeView nodeView = editor.getNodeView(v);

            final NetworkBlock.NodeData nodeData = networkBlock.getNodeData(v);
            nodeData.put("x", String.format("%.2f", nodeView.getTranslateX()));
            nodeData.put("y", String.format("%.2f", nodeView.getTranslateY()));
            nodeData.put("w", String.format("%.2f", nodeView.getWidth()));
            nodeData.put("h", String.format("%.2f", nodeView.getHeight()));
            if (!Basic.equals(nodeView.getLabel().getRotate(), 0, 0.00001)) {
                nodeData.put("lr", String.format("%.0f", nodeView.getLabel().getRotate()));
            }

            nodeData.put("type", NodeShape.getCode(nodeView.getShape()));
            if (!nodeView.getShape().getFill().equals(Color.WHITE))
                nodeData.put("clr", nodeView.getShape().getFill().toString());
            final RichTextLabel label = nodeView.getLabel();
            if (label.getText().length() > 0) {
                nodeData.put("text", label.getText());
                nodeData.put("lx", String.format("%.2f", label.getLayoutX()));
                nodeData.put("ly", String.format("%.2f", label.getLayoutY()));

                if (!label.getTextFill().equals(Color.BLACK))
                    nodeData.put("lclr", label.getTextFill().toString());

                if (!label.getFont().equals(PhyloView.DefaultFont))
                    nodeData.put("font", label.getFont().getFamily() + "," + label.getFont().getStyle() + "," + label.getFont().getSize());
            }
        }

        for (Edge e : graph.edges()) {
            final EdgeView edgeView = editor.getEdgeView(e);
            final CubicCurve curve = edgeView.getCurve();
            NetworkBlock.EdgeData edgeData = networkBlock.getEdgeData(e);
            edgeData.put("type", "CC");
            edgeData.put("c1x", String.format("%.2f", curve.getControlX1()));
            edgeData.put("c1y", String.format("%.2f", curve.getControlY1()));
            edgeData.put("c2x", String.format("%.2f", curve.getControlX2()));
            edgeData.put("c2y", String.format("%.2f", curve.getControlY2()));
            edgeData.put("sw", String.format("%.2f", curve.getStrokeWidth()));
            if (!curve.getStroke().equals(Color.BLACK))
                edgeData.put("clr", curve.getStroke().toString());
            if (!edgeView.getArrowHead().isVisible())
                edgeData.put("arw", "false");
        }

        final TaxaNexusOutput taxaOutput = new TaxaNexusOutput();
        final NetworkNexusOutput networkOutput = new NetworkNexusOutput();

		networkBlock.setName(FileUtils.replaceFileSuffix(selectedFile.getName(), ""));
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

    public static void open(Pane mainPane, PhyloView editor, File selectedFile) throws IOException {
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
            final NetworkBlock.NodeData nodeData = networkBlock.getNodeData(v);
            final NodeView nodeView = editor.addNode(v, mainPane, Basic.parseDouble(nodeData.get("x")), Basic.parseDouble(nodeData.get("y")));

            if (nodeData.get("type") != null) {
				NodeShape nodeShape = StringUtils.valueOfMatchingSubsequence(NodeShape.class, nodeData.get("type"));
                if (nodeShape != null && nodeShape != editor.getNodeView(v).getNodeShape())
                    nodeView.changeShape(nodeShape);
                if (nodeShape == NodeShape.None) {
                    nodeView.setWidth(1);
                    nodeView.setHeight(1);
                } else {
                    final double w = Basic.parseDouble(nodeData.get("w"));
                    final double h = Basic.parseDouble(nodeData.get("h"));
                    if (w > 0)
                        nodeView.setWidth(w);
                    if (h > 0)
                        nodeView.setHeight(h);
                }
            }

            if (nodeData.get("clr") != null) {
                nodeView.getShape().setFill(Color.valueOf(nodeData.get("clr")));
            }


            if (nodeData.get("text") != null) {
                final RichTextLabel label = nodeView.getLabel();
                label.setText(nodeData.get("text"));

                label.setLayoutX(Basic.parseDouble(nodeData.get("lx")));
                label.setLayoutY(Basic.parseDouble(nodeData.get("ly")));

                if (nodeData.get("font") != null) {
					final String[] tokens = StringUtils.split(nodeData.get("font"), ',');
                    if (tokens.length == 3 && Basic.isDouble(tokens[2])) {
                        label.setFont(FontUtils.font(tokens[0], tokens[1], Basic.parseDouble(tokens[2])));
                    }
                }
                if (nodeData.get("lclr") != null) {
                    label.setTextFill(Color.valueOf(nodeData.get("lclr")));
                }

                if (nodeData.get("lr") != null) {
                    label.setRotate(Basic.parseDouble(nodeData.get("lr")));
                }
            }
        }

        for (Edge e : graph.edges()) {
            final NetworkBlock.EdgeData edgeData = networkBlock.getEdgeData(e);
            if (edgeData.get("type").equals("CC")) {
                editor.addEdge(e);
                final EdgeView edgeView = editor.getEdgeView(e);
                final double c1x = Basic.parseDouble(edgeData.get("c1x"));
                final double c1y = Basic.parseDouble(edgeData.get("c1y"));
                final double c2x = Basic.parseDouble(edgeData.get("c2x"));
                final double c2y = Basic.parseDouble(edgeData.get("c2y"));
                edgeView.setControlCoordinates(new double[]{c1x, c1y, c2x, c2y});

                if (edgeData.get("clr") != null) {
                    edgeView.getCurve().setStroke(Color.valueOf(edgeData.get("clr")));
                }

                if (edgeData.get("sw") != null) {
                    final double sw = Basic.parseDouble(edgeData.get("sw"));
                    if (sw > 0)
                        edgeView.getCurve().setStrokeWidth(sw);
                }
                if (edgeData.get("arw") != null) {
                    edgeView.getArrowHead().setVisible(Basic.parseBoolean(edgeData.get("arw")));
                }
            }
        }
    }

    public static void importNewick(Pane contentPane, PhyloView view, File selectedFile) throws IOException {

        final PhyloTree tree = new PhyloTree();
        try (BufferedReader r = new BufferedReader(new FileReader(selectedFile))) {
            tree.read(r, true);
        }

        final PhyloTree graph = view.getGraph();
        graph.copy(tree);

        RootedNetworkEmbedder.apply(contentPane, view, RootedNetworkEmbedder.Orientation.leftRight);
    }

    /**
     * export in extended Newick format
     *
     * @param owner
     * @param editor
     */
    public static void exportNewick(final Stage owner, PhyloView editor) {
        final File previousDir = new File(ProgramProperties.get("ExportDir", ""));
        final FileChooser fileChooser = new FileChooser();
        if (previousDir.isDirectory())
            fileChooser.setInitialDirectory(previousDir);
		fileChooser.setInitialFileName(FileUtils.replaceFileSuffix(FileUtils.getFileNameWithoutPath(editor.getFileName()), ".newick"));
		fileChooser.setTitle("Export File");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Extended Newick", "*.newick", "*.new", "*.tree", "*.tre"),
                new FileChooser.ExtensionFilter("Text", "*.txt"));
        File selectedFile = fileChooser.showSaveDialog(owner);
        if (selectedFile != null) {
            try (BufferedWriter w = new BufferedWriter(new FileWriter(selectedFile))) {
                for (Node root : NetworkProperties.findRoots(editor.getGraph())) {
                    editor.getGraph().setRoot(root);
                    editor.getGraph().write(w, false);
                    w.write(";\n");
                }
                final ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(w.toString());
                Clipboard.getSystemClipboard().setContent(clipboardContent);

                ProgramProperties.put("ExportDir", selectedFile.getParent());
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Import NeXML tree
     *
     * @param contentPane
     * @param view
     * @param selectedFile
     * @throws IOException
     */
    public static void importNeXML(Pane contentPane, PhyloView view, File selectedFile) throws IOException {

        PhyloTree tree = new PhyloTree();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser;
            saxParser = factory.newSAXParser();
            NexmlTreesHandler handler = new NexmlTreesHandler();
            saxParser.parse(selectedFile, handler);

            //for (PhyloTree t : handler.getTrees())
            //trees.getTrees().add(t);
            tree = handler.getTrees().get(0);

        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }

        final PhyloTree graph = view.getGraph();
        graph.copy(tree);

        RootedNetworkEmbedder.apply(contentPane, view, RootedNetworkEmbedder.Orientation.leftRight);
    }
}
