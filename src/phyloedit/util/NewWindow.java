/*
 *  NewWindow.java Copyright (C) 2020 Daniel H. Huson
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

package phyloedit.util;

import jloda.fx.window.MainWindowManager;
import phyloedit.window.PhyloEditorWindow;

/**
 * create a new window
 * Daniel Huson, 7.2019
 */
public class NewWindow {
    /**
     * create a new window
     *
     * @return new window
     */
    public static PhyloEditorWindow apply() {
        return (PhyloEditorWindow) MainWindowManager.getInstance().createAndShowWindow(false);
    }
}
