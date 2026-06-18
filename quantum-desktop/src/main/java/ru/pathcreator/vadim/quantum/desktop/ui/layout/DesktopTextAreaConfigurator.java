/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.layout;

import javafx.scene.control.TextArea;

/**
 * Централизует режимы редактирования и переноса строк для текстовых панелей desktop UI.
 */
public final class DesktopTextAreaConfigurator {

    public void editable(final TextArea... areas) {
        for (final TextArea area : areas) {
            area.setEditable(true);
        }
    }

    public void readonly(final TextArea... areas) {
        for (final TextArea area : areas) {
            area.setEditable(false);
        }
    }

    public void wrap(final TextArea... areas) {
        for (final TextArea area : areas) {
            area.setWrapText(true);
        }
    }

    public void noWrap(final TextArea... areas) {
        for (final TextArea area : areas) {
            area.setWrapText(false);
        }
    }
}