
package phyloedit.main;
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
import phyloedit.window.PhyloEditorWindow;

import java.io.File;
import java.time.Duration;

public class PhyloEdit extends Application {
    /**
     * main
     *
     * @param args
     */
    public static void main(String[] args) {
        Basic.restoreSystemOut(System.err); // send system out to system err
        Basic.startCollectionStdErr();

        ResourceManagerFX.addResourceRoot(PhyloEdit.class, "phyloedit.resources");
        ProgramProperties.getProgramIconsFX().setAll(ResourceManagerFX.getIcons("PhyloEdit-16.png", "PhyloEdit-32.png", "PhyloEdit-48.png", "PhyloEdit-64.png", "PhyloEdit-128.png"));

        ProgramProperties.setProgramName(Version.NAME);
        ProgramProperties.setProgramVersion(Version.SHORT_DESCRIPTION);
        ProgramProperties.setProgramLicence("Copyright (C) 2020 Daniel H. Huson. This program comes with ABSOLUTELY NO WARRANTY.\n" +
                "This is free software, licensed under the terms of the GNU General Public License, Version 3.\n" +
                "Sources available at: https://github.com/husonlab/catlynet\n" +
                "Installers available at: http://software-ab.informatik.uni-tuebingen.de/download/catlynet\n");
        SplashScreen.setLabelAnchor(new Point2D(160, 20));
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
        final ArgsOptions options = new ArgsOptions(args, PhyloEdit.class, Version.NAME + " - Directed network generator");
        options.setAuthors("Daniel H. Huson, with Mike A. Steel");
        options.setLicense(ProgramProperties.getProgramLicence());
        options.setVersion(ProgramProperties.getProgramVersion());

        options.comment("Input:");

        final String defaultPropertiesFile;
        if (ProgramProperties.isMacOS())
            defaultPropertiesFile = System.getProperty("user.home") + "/Library/Preferences/PhyloEdit.def";
        else
            defaultPropertiesFile = System.getProperty("user.home") + File.separator + ".PhyloEdit.def";
        final String propertiesFile = options.getOption("-p", "propertiesFile", "Properties file", defaultPropertiesFile);
        final boolean showVersion = options.getOption("-V", "version", "Show version string", false);
        final boolean silentMode = options.getOption("-S", "silentMode", "Silent mode", false);
        ProgramExecutorService.setNumberOfCoresToUse(options.getOption("-t", "threads", "Maximum number of threads to use in a parallel algorithm (0=all available)", 0));
        options.done();

        ProgramProperties.load(propertiesFile);

        if (silentMode) {
            Basic.stopCollectingStdErr();
            Basic.hideSystemErr();
            Basic.hideSystemOut();
        }

        if (showVersion) {
            System.err.println(ProgramProperties.getProgramVersion());
            System.err.println(jloda.util.Version.getVersion(PhyloEdit.class, ProgramProperties.getProgramName()));
            System.err.println("Java version: " + System.getProperty("java.version"));
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        SplashScreen.showSplash(Duration.ofSeconds(5));
        try {
            stage.setTitle("Untitled - " + ProgramProperties.getProgramName());
            NotificationManager.setShowNotifications(true);

            final PhyloEditorWindow mainWindow = new PhyloEditorWindow();

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
}
