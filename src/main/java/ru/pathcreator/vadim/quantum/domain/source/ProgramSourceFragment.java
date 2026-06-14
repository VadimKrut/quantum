/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.domain.source;

import java.util.Objects;

import ru.pathcreator.vadim.quantum.domain.naming.IdentifierName;

/**
 * Сохраненный фрагмент внешнего представления, для которого в IR еще нет исполняемой семантики.
 */
public final class ProgramSourceFragment {

    /**
     * Формат исходного фрагмента.
     */
    private final IdentifierName format;

    /**
     * Род фрагмента внутри формата.
     */
    private final IdentifierName kind;

    /**
     * Исходный текст фрагмента.
     */
    private final String content;

    public ProgramSourceFragment(
        final String format,
        final String kind,
        final String content
    ) {
        if (
            content == null
            || content.isBlank()
        ) {
            throw new IllegalArgumentException("Program source fragment content must not be blank.");
        }
        this.format = IdentifierName.of(
            format,
            "Source fragment format"
        );
        this.kind = IdentifierName.of(
            kind,
            "Source fragment kind"
        );
        this.content = content;
    }

    /**
     * Возвращает формат фрагмента.
     *
     * @return формат фрагмента
     */
    public String format() {
        return format.value();
    }

    /**
     * Возвращает род фрагмента.
     *
     * @return род фрагмента
     */
    public String kind() {
        return kind.value();
    }

    /**
     * Возвращает исходный текст.
     *
     * @return исходный текст
     */
    public String content() {
        return content;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProgramSourceFragment fragment)) {
            return false;
        }
        return Objects.equals(
            format,
            fragment.format
        )
            && Objects.equals(
                kind,
                fragment.kind
            )
            && Objects.equals(
                content,
                fragment.content
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            format,
            kind,
            content
        );
    }
}