module phyloedit {
    requires transitive jloda;
    requires transitive splitstreefive;

    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires transitive javafx.fxml;
    requires transitive java.desktop;

    requires fx.platform.utils;
    requires com.install4j.runtime;

    exports phyloedit.main;
    exports phyloedit.window;
    exports phyloedit.util;
    exports phyloedit.commands;

    opens phyloedit.window;
    opens phyloedit.resources.icons;
    opens phyloedit.resources.images;

    opens phyloedit.formattab;
    opens phyloedit.formattab.fontselector;


}