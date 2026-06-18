/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.library;

import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.fieldRow;
import static ru.pathcreator.vadim.quantum.desktop.ui.layout.DesktopUiNodes.section;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Строит рабочее пространство библиотеки алгоритмов без бизнес-логики генерации.
 */
public final class DesktopLibraryWorkspaceView {

    /**
     * Собирает вкладку библиотеки из готовых контролов и кнопок.
     *
     * @param builtInList список встроенных алгоритмов
     * @param userList список пользовательских .qdsl файлов
     * @param searchField поле поиска
     * @param metadataForm форма описания пользовательской записи
     * @param parameterArea параметры встроенного генератора
     * @param detailsArea описание выбранной записи
     * @param actions кнопки действий
     * @param text локализованные подписи
     * @return JavaFX node вкладки библиотеки
     */
    public Node build(
        final ListView<String> builtInList,
        final ListView<String> userList,
        final TextField searchField,
        final Node metadataForm,
        final TextArea parameterArea,
        final TextArea detailsArea,
        final Node actions,
        final DesktopLibraryWorkspaceText text
    ) {
        final VBox builtInPane = section(
            text.builtInAlgorithms(),
            fieldRow(
                text.search(),
                searchField
            ),
            builtInList
        );
        final VBox userPane = section(
            text.userLibrary(),
            userList
        );
        final VBox left = new VBox(
            12.0,
            builtInPane,
            userPane
        );
        final VBox center = new VBox(
            12.0,
            section(
                text.algorithmDetails(),
                detailsArea
            ),
            section(
                text.builtInParameters(),
                parameterArea
            )
        );
        final VBox right = new VBox(
            12.0,
            section(
                text.saveCurrentProgram(),
                metadataForm,
                actions
            )
        );
        VBox.setVgrow(
            builtInList,
            Priority.ALWAYS
        );
        VBox.setVgrow(
            userList,
            Priority.ALWAYS
        );
        VBox.setVgrow(
            detailsArea,
            Priority.ALWAYS
        );
        VBox.setVgrow(
            parameterArea,
            Priority.ALWAYS
        );
        final SplitPane splitPane = new SplitPane(
            left,
            center,
            right
        );
        splitPane.setDividerPositions(
            0.28,
            0.68
        );
        final BorderPane root = new BorderPane(splitPane);
        root.setPadding(new Insets(12.0));
        return root;
    }
}