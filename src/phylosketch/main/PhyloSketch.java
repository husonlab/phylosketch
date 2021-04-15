
/*
 * PhyloSketch.java Copyright (C) 2021. Daniel H. Huson
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

package phylosketch.main;

import com.briksoftware.javafx.platform.osx.OSXIntegration;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import jloda.fx.util.ArgsOptions;
import jloda.fx.util.ProgramExecutorService;
import jloda.fx.util.ResourceManagerFX;
import jloda.fx.window.MainWindowManager;
import jloda.fx.window.NotificationManager;
import jloda.fx.window.SplashScreen;
import jloda.fx.window.WindowGeometry;
import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.UsageException;
import phylosketch.window.MainWindow;
import splitstree5.main.CheckForUpdate;

import java.io.File;
import java.time.Duration;

/**
 * phylogenetic tree and network sketch program
 * Daniel Huson, 1.2020
 */
public class PhyloSketch extends Application {
    public static boolean enableExperimental = false;

    /**
     * main
     *
     * @param args
     */
    public static void main(String[] args) {
        ProgramProperties.setUseGUI(true);
        Basic.restoreSystemOut(System.err); // send system out to system err
        Basic.startCollectionStdErr();

        ResourceManagerFX.addResourceRoot(PhyloSketch.class, "phylosketch.resources");
        ProgramProperties.getProgramIconsFX().setAll(ResourceManagerFX.getIcons("PhyloSketch-16.png", "PhyloSketch-32.png", "PhyloSketch-48.png", "PhyloSketch-64.png", "PhyloSketch-128.png"));

        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);

        ProgramProperties.setProgramLicence("Copyright (C) 2021 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.\n" +
                "This is free software, licensed under the terms of the GNU General Public License, Version 3.\n" +
                "Sources available at: https://github.com/husonlab/phylosketch\n");

        CheckForUpdate.programURL = "http://software-ab.informatik.uni-tuebingen.de/download/phylosketch";
        CheckForUpdate.applicationId = "1691242391";

        SplashScreen.setLabelAnchor(new Point2D(230, 10));
        SplashScreen.setVersionString(ProgramProperties.getProgramVersion());
        SplashScreen.setImageResourceName("splash.png");


        try {
            parseArguments(args);
        } catch (Throwable th) {
            //catch any exceptions and the like that propagate up to the top level
            if (!th.getMessage().startsWith("Help")) {
                System.err.println("Fatal error:" + "\n" + th.toString());
                Basic.caught(th);
            }
            System.exit(1);
        }

        launch(args);

    }

    protected static void parseArguments(String[] args) throws CanceledException, UsageException {
        final ArgsOptions options = new ArgsOptions(args, PhyloSketch.class, Version.NAME + " - Phylogenetic tree and network sketch program");
        options.setAuthors("Daniel H. Huson, with Mike A. Steel");
        options.setLicense(ProgramProperties.getProgramLicence());
        options.setVersion(ProgramProperties.getProgramVersion());

        options.comment("Input:");

        final String defaultPropertiesFile;
        if (ProgramProperties.isMacOS())
            defaultPropertiesFile = System.getProperty("user.home") + "/Library/Preferences/PhyloSketch.def";
        else
            defaultPropertiesFile = System.getProperty("user.home") + File.separator + ".PhyloSketch.def";
        final String propertiesFile = options.getOption("-p", "propertiesFile", "Properties file", defaultPropertiesFile);
        final boolean showVersion = options.getOption("-V", "version", "Show version string", false);
        final boolean silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-t", "threads", "Maximum number of threads to use in a parallel algorithm (0=all available)", 0));

        enableExperimental = options.getOption("-x", "experimental", "Enable experimental features", false);

        options.done();

        ProgramProperties.load(propertiesFile);

        if (silentMode) {
            Basic.stopCollectingStdErr();
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }

        if (showVersion) {
            System.err.println(ProgramProperties.getProgramVersion());
            System.err.println(jloda.util.Version.getVersion(PhyloSketch.class, ProgramProperties.getProgramName()));
            System.err.println("Java version: " + System.getProperty("java.version"));
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        SplashScreen.showSplash(Duration.ofSeconds(5));
        try {
            stage.setTitle("Untitled - " + ProgramProperties.getProgramName());
            NotificationManager.setShowNotifications(true);

            final MainWindow mainWindow = new MainWindow();

            final WindowGeometry windowGeometry = new WindowGeometry(ProgramProperties.get("WindowGeometry", "50 50 800 800"));

            mainWindow.show(stage, windowGeometry.getX(), windowGeometry.getY(), windowGeometry.getWidth(), windowGeometry.getHeight());

            // setup about and preferences menu for apple:
            if (false) {
                OSXIntegration.init();
                OSXIntegration.populateAppleMenu(() -> SplashScreen.showSplash(Duration.ofMinutes(1)), () -> System.err.println("Preferences"));

                // open files by double-click under Mac OS: // untested
                OSXIntegration.setOpenFilesHandler(files -> {
                    for (File file : files) {
                        System.err.println("Open file " + file + ": not implemented");
                    }
                });
            }
            MainWindowManager.getInstance().addMainWindow(mainWindow);

        } catch (Exception ex) {
            Basic.caught(ex);
            throw ex;
        }
    }

    @Override
    public void stop() throws Exception {
        ProgramProperties.store();
        System.exit(0);
    }
}
