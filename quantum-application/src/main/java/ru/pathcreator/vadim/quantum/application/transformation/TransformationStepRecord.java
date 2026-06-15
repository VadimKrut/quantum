/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.transformation;

import java.util.Objects;

/**
 * Запись о примененном или пропущенном шаге консервативной трансформации.
 */
public final class TransformationStepRecord {

    /**
     * Имя шага трансформации.
     */
    private final TransformationStep step;

    /**
     * Человекочитаемое объяснение, почему шаг применен или пропущен.
     */
    private final String message;

    private TransformationStepRecord(
        final TransformationStep step,
        final String message
    ) {
        this.step = step;
        this.message = message;
    }

    /**
     * Создает запись шага трансформации.
     *
     * @param step шаг трансформации
     * @param message объяснение шага
     * @return запись шага
     */
    public static TransformationStepRecord of(
        final TransformationStep step,
        final String message
    ) {
        if (step == null) {
            throw new IllegalArgumentException("Transformation step must not be null.");
        }
        if (
            message == null
            || message.isBlank()
        ) {
            throw new IllegalArgumentException("Transformation step message must not be blank.");
        }
        return new TransformationStepRecord(
            step,
            message
        );
    }

    /**
     * Возвращает шаг трансформации.
     *
     * @return шаг трансформации
     */
    public TransformationStep step() {
        return step;
    }

    /**
     * Возвращает объяснение шага.
     *
     * @return объяснение шага
     */
    public String message() {
        return message;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TransformationStepRecord record)) {
            return false;
        }
        return step == record.step
            && Objects.equals(
                message,
                record.message
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            step,
            message
        );
    }
}