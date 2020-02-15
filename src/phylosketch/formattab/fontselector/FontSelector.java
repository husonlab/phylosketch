/*
 * FontSelector.java Copyright (C) 2020. Daniel H. Huson
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

package phylosketch.formattab.fontselector;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.util.converter.IntegerStringConverter;
import jloda.fx.util.ExtendedFXMLLoader;
import jloda.fx.util.FontUtils;
import jloda.util.ProgramProperties;

import java.util.Set;
import java.util.TreeSet;

/**
 * font selection popup
 * Daniel Huson, 1.2018
 */
public class FontSelector extends ComboBox<String> {
    private final FontSelectorController controller;
    private final Popup popup;

    private final ObjectProperty<Font> fontValue = new SimpleObjectProperty<>();

    private boolean sizeChanging = false;
    private boolean fontChanging = false;

    public FontSelector() {
        this(ProgramProperties.getDefaultFontFX());
    }

    public FontSelector(Font font) {
        fontValueProperty().addListener((c, o, n) -> {
            if (n != null)
                setValue(n.getName() + String.format(" %.0fpx", n.getSize()));
            else
                setValue(null);
        });

        final ExtendedFXMLLoader<FontSelectorController> extendedFXMLLoader = new ExtendedFXMLLoader<>(this.getClass());
        controller = extendedFXMLLoader.getController();

        controller.getFontSizeSlider().setMin(4);
        controller.getFontSizeSlider().setMax(60);

        final ComboBox<Integer> fontSizeComboBox = controller.getFontSizeComboBox();

        fontSizeComboBox.getItems().addAll(4, 6, 8, 10, 12, 14, 16, 20, 24, 32, 38, 48, 60);

        fontSizeComboBox.setConverter(new IntegerStringConverter());
        fontSizeComboBox.valueProperty().addListener((c, o, n) -> {
            if (!sizeChanging) {
                try {
                    sizeChanging = true;
                    controller.getFontSizeSlider().setValue(n);
                } finally {
                    sizeChanging = false;
                }
            }
        });

        controller.getFontSizeSlider().valueProperty().addListener((c, o, n) -> {
            if (!sizeChanging) {
                try {
                    sizeChanging = true;
                    fontSizeComboBox.setValue(Math.round(n.floatValue()));
                } finally {
                    sizeChanging = false;
                }
            }
        });
        fontSizeComboBox.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateFontValue());

        final ComboBox<String> fontFamilyComboBox = controller.getFontFamilyComboBox();

        final Set<String> families = new TreeSet<>();
        for (String family : Font.getFamilies()) {
            if (Font.font(family, 12).getFamily().equals(family))
                families.add(family);
        }

        fontFamilyComboBox.getItems().addAll(families);

        fontFamilyComboBox.getEditor().setOnKeyReleased((e) -> {
            if (e.getCode().isLetterKey() || e.getCode() == KeyCode.SPACE) {
                final String currentText = fontFamilyComboBox.getEditor().getText().toLowerCase();
                if (currentText.length() > 0) {
                    for (String item : fontFamilyComboBox.getItems()) {
                        if (item.toLowerCase().startsWith(currentText)) {
                            fontFamilyComboBox.getEditor().setText(item);
                            fontFamilyComboBox.getEditor().selectRange(currentText.length(), item.length());
                            break;
                        }
                    }
                }
            }
        });
        fontFamilyComboBox.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateFontValue());

        final ComboBox<String> fontStyleComboBox = controller.getFontStyleComboBox();

        controller.getFontStyleComboBox().getItems().addAll("Bold", "Italic", "Bold Italic", "Regular");

        fontStyleComboBox.getEditor().setOnKeyReleased((e) -> {
            if (e.getCode().isLetterKey() || e.getCode() == KeyCode.SPACE) {
                final String currentText = fontStyleComboBox.getEditor().getText().toLowerCase();
                if (currentText.length() > 0) {
                    for (String item : fontStyleComboBox.getItems()) {
                        if (item.toLowerCase().startsWith(currentText)) {
                            fontStyleComboBox.getEditor().setText(item);
                            fontStyleComboBox.getEditor().selectRange(currentText.length(), item.length());
                            break;
                        }
                    }
                }
            }
        });
        fontStyleComboBox.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateFontValue());

        popup = new Popup();
        popup.getContent().add(extendedFXMLLoader.getRoot());
        final DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.LIGHTGRAY);
        dropShadow.setOffsetX(2);
        dropShadow.setOffsetY(2);
        extendedFXMLLoader.getRoot().setEffect(dropShadow);

        setDefaultFont(font);
    }

    @Override
    public void show() {
        Event.fireEvent(this, new Event(ComboBoxBase.ON_SHOWING));
        final Point2D location = localToScreen(getLayoutX(), getLayoutY());
        popup.show(getScene().getWindow(), location.getX(), location.getY());
    }

    public void setDefaultFont(Font font) {
        if (!fontChanging) {
            try {
                fontChanging = true;
                setFontValue(font);
                controller.getFontFamilyComboBox().setValue(font.getFamily());
                controller.getFontSizeComboBox().setValue((int) Math.round(font.getSize()));
                controller.getFontStyleComboBox().setValue(font.getStyle());
            } finally {
                fontChanging = false;
            }
        }
    }

    private void updateFontValue() {
        if (!fontChanging) {
            try {
                fontChanging = true;

                final String family = controller.getFontFamilyComboBox().getSelectionModel().getSelectedItem();
                if (controller.getFontFamilyComboBox().getItems().contains(family)) {
                    final int size = Math.max(1, controller.getFontSizeComboBox().getValue());
                    final String style = controller.getFontStyleComboBox().getSelectionModel().getSelectedItem();
                    final Font font = FontUtils.font(family, style, size);
                    setFontValue(font);
                }
            } finally {
                fontChanging = false;
            }
        }
    }

    @Override
    public void hide() {
        Event.fireEvent(this, new Event(ComboBoxBase.ON_HIDING));
        popup.hide();
    }

    public Font getFontValue() {
        return fontValue.get();
    }

    public ObjectProperty<Font> fontValueProperty() {
        return fontValue;
    }

    public void setFontValue(Font fontValue) {
        this.fontValue.set(fontValue);
    }
}
