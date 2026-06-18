/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.library;

/**
 * Тексты вкладки библиотеки, которые приходят из текущей локали desktop.
 */
public final class DesktopLibraryWorkspaceText {

    private final String builtInAlgorithms;
    private final String search;
    private final String userLibrary;
    private final String algorithmDetails;
    private final String builtInParameters;
    private final String saveCurrentProgram;

    /**
     * Создает набор подписей для вкладки библиотеки.
     *
     * @param builtInAlgorithms заголовок встроенной библиотеки
     * @param search подпись поиска
     * @param userLibrary заголовок пользовательской библиотеки
     * @param algorithmDetails заголовок описания алгоритма
     * @param builtInParameters заголовок параметров генератора
     * @param saveCurrentProgram заголовок сохранения текущей программы
     */
    public DesktopLibraryWorkspaceText(
        final String builtInAlgorithms,
        final String search,
        final String userLibrary,
        final String algorithmDetails,
        final String builtInParameters,
        final String saveCurrentProgram
    ) {
        this.builtInAlgorithms = requireText(
            builtInAlgorithms,
            "Built-in algorithms text"
        );
        this.search = requireText(
            search,
            "Search text"
        );
        this.userLibrary = requireText(
            userLibrary,
            "User library text"
        );
        this.algorithmDetails = requireText(
            algorithmDetails,
            "Algorithm details text"
        );
        this.builtInParameters = requireText(
            builtInParameters,
            "Built-in parameters text"
        );
        this.saveCurrentProgram = requireText(
            saveCurrentProgram,
            "Save current program text"
        );
    }

    public String builtInAlgorithms() {
        return builtInAlgorithms;
    }

    public String search() {
        return search;
    }

    public String userLibrary() {
        return userLibrary;
    }

    public String algorithmDetails() {
        return algorithmDetails;
    }

    public String builtInParameters() {
        return builtInParameters;
    }

    public String saveCurrentProgram() {
        return saveCurrentProgram;
    }

    private static String requireText(
        final String value,
        final String subject
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            throw new IllegalArgumentException(subject + " must not be blank.");
        }
        return value;
    }
}