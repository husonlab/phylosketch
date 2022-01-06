module phylosketch {
    requires transitive jloda;
    requires transitive splitstreefive;

    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires transitive javafx.fxml;
    requires transitive java.desktop;

    requires com.install4j.runtime;

    exports phylosketch.main;
    exports phylosketch.window;
    exports phylosketch.util;
    exports phylosketch.commands;

    opens phylosketch.window;
    opens phylosketch.resources.icons;
    opens phylosketch.resources.images;

    opens phylosketch.formattab;
    opens phylosketch.formattab.fontselector;
	exports phylosketch.algorithms;
}