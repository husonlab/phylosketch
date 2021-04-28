/*
 * MainWindow.java Copyright (C) 2021. Daniel H. Huson
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

package phylosketch.window;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jloda.fx.util.ExtendedFXMLLoader;
import jloda.fx.util.MemoryUsage;
import jloda.fx.window.IMainWindow;
import jloda.fx.window.MainWindowManager;
import jloda.util.Basic;
import jloda.util.FileOpenManager;
import jloda.util.ProgramProperties;
import phylosketch.io.PhyloSketchFileOpener;

import java.util.Arrays;

/**
 * the phylo editor main window
 * Daniel Huson, 1.2020
 */
public class MainWindow implements IMainWindow {
    private final Parent root;
    private final MainWindowController controller;
    private final PhyloView view = new PhyloView(this);
    private Stage stage;

    public MainWindow() {
        Platform.setImplicitExit(false);

        {
            final ExtendedFXMLLoader<MainWindowController> extendedFXMLLoader = new ExtendedFXMLLoader<>(this.getClass());
            root = extendedFXMLLoader.getRoot();
            controller = extendedFXMLLoader.getController();
        }

        final MemoryUsage memoryUsage = MemoryUsage.getInstance();
        controller.getMemoryUsageLabel().textProperty().bind(memoryUsage.memoryUsageStringProperty());

        FileOpenManager.setExtensions(Arrays.asList(new FileChooser.ExtensionFilter("Nexus", "*.nexus", "*.nex"),
                new FileChooser.ExtensionFilter("All", "*.*")));
        FileOpenManager.setFileOpener(new PhyloSketchFileOpener());
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public IMainWindow createNew() {
        return new MainWindow();
    }

    @Override
    public void show(Stage stage0, double screenX, double screenY, double width, double height) {
        if (stage == null)
            stage = new Stage();
        this.stage = stage0;
        stage.getIcons().addAll(ProgramProperties.getProgramIconsFX());

        final Scene scene = new Scene(root, width, height);

        stage.setScene(scene);
        stage.sizeToScene();
        stage.setX(screenX);
        stage.setY(screenY);

        getStage().titleProperty().addListener((e) -> MainWindowManager.getInstance().fireChanged());

        ControlBindings.setup(this);

        view.fileNameProperty().addListener(c -> stage.setTitle(Basic.getFileNameWithoutPath(view.getFileName()) + (view.isDirty() ? "*" : "")
                + " - " + ProgramProperties.getProgramName()));

        view.dirtyProperty().addListener(c -> stage.setTitle(Basic.getFileNameWithoutPath(view.getFileName()) + (view.isDirty() ? "*" : "")
                + " - " + ProgramProperties.getProgramName()));

        view.setFileName("Untitled.nexus");

        stage.show();
    }

    @Override
    public boolean isEmpty() {
        return view.getGraph().getNumberOfNodes() == 0;
    }

    @Override
    public void close() {
        stage.hide();
    }


    public MainWindowController getController() {
        return controller;
    }

    public PhyloView getView() {
        return view;
    }
}
