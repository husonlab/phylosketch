module phyloedit {
    requires transitive jloda;
    requires transitive splitstreefive;

    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires transitive javafx.fxml;
    requires transitive javafx.web;
    requires transitive java.sql;
    requires transitive java.desktop;

    requires fx.platform.utils;

    exports phyloedit.main;
    exports phyloedit.window;
    exports phyloedit.actions;

    opens phyloedit.window;

}