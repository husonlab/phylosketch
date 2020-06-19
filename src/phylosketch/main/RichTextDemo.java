/*
 * RichTextDemo.java Copyright (C) 2020. Daniel H. Huson
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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import jloda.fx.control.RichTextLabel;

public class RichTextDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        final TextField inputField = new TextField("<b>X hello</b> <i>you<sup>2</sup>!</i>");
        //inputField.setMinWidth(TextField.USE_PREF_SIZE);
        inputField.setPrefWidth(500);
        inputField.setMaxWidth(10000);

        final Button done = new Button("Done");
        done.setOnAction(z -> System.exit(0));

        final RichTextLabel label = new RichTextLabel();
        label.setFont(Font.font("Arial", FontWeight.NORMAL, FontPosture.ITALIC, 48));
        label.textProperty().bind(inputField.textProperty());

        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(new ToolBar(inputField, done));
        borderPane.setCenter(new StackPane(label));

        primaryStage.setTitle("RichTextDemo");
        primaryStage.setScene(new Scene(borderPane, 600, 600));
        primaryStage.show();
    }
}
