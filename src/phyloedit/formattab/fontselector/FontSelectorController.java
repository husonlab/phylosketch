/*
 *  FontSelectorController.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.formattab.fontselector;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;

/**
 * font selector controller class
 * Daniel Huson, 1.2018
 */
public class FontSelectorController {

    @FXML
    private ComboBox<String> fontFamilyComboBox;

    @FXML
    private ComboBox<String> fontStyleComboBox;

    @FXML
    private ComboBox<Integer> fontSizeComboBox;

    @FXML
    private Slider fontSizeSlider;


    public ComboBox<String> getFontFamilyComboBox() {
        return fontFamilyComboBox;
    }

    public ComboBox<String> getFontStyleComboBox() {
        return fontStyleComboBox;
    }

    public ComboBox<Integer> getFontSizeComboBox() {
        return fontSizeComboBox;
    }

    public Slider getFontSizeSlider() {
        return fontSizeSlider;
    }
}
