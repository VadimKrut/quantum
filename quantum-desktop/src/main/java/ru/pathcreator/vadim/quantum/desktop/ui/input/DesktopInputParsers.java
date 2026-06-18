/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.desktop.ui.input;

/**
 * Нормализует текстовые значения из desktop-controls в безопасные числовые настройки.
 */
public final class DesktopInputParsers {

    private DesktopInputParsers() {
    }

    public static int positiveIntegerOrOne(final String text) {
        try {
            return Math.max(
                1,
                Integer.parseInt(text)
            );
        } catch (final RuntimeException exception) {
            return 1;
        }
    }

    public static long positiveLongOrDefault(
        final String text,
        final long defaultValue
    ) {
        try {
            return Math.max(
                1L,
                Long.parseLong(text)
            );
        } catch (final RuntimeException exception) {
            return defaultValue;
        }
    }

    public static double doubleOrDefault(
        final String text,
        final double defaultValue
    ) {
        try {
            return Double.parseDouble(text);
        } catch (final RuntimeException exception) {
            return defaultValue;
        }
    }

    public static String exceptionMessage(final Throwable exception) {
        final String message = exception.getMessage();
        return message == null
            ? exception.getClass().getSimpleName()
            : message;
    }
}