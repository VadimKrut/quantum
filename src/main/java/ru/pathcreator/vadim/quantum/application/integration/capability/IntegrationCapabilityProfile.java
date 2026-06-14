/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.integration.capability;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;

/**
 * Immutable профиль возможностей конкретного import/export adapter.
 */
public final class IntegrationCapabilityProfile {

    /**
     * Формат, к которому относится профиль.
     */
    private final IntegrationFormat format;

    /**
     * Набор поддерживаемых возможностей.
     */
    private final Set<IntegrationCapability> capabilities;

    private IntegrationCapabilityProfile(
        final IntegrationFormat format,
        final Set<IntegrationCapability> capabilities
    ) {
        this.format = format;
        this.capabilities = capabilities;
    }

    /**
     * Создает профиль возможностей.
     *
     * @param format внешний формат
     * @param capabilities поддерживаемые возможности
     * @return профиль возможностей
     */
    public static IntegrationCapabilityProfile of(
        final IntegrationFormat format,
        final Set<IntegrationCapability> capabilities
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Integration format must not be null.");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("Integration capabilities must not be null.");
        }
        final EnumSet<IntegrationCapability> copy = EnumSet.noneOf(IntegrationCapability.class);
        for (IntegrationCapability capability : capabilities) {
            if (capability == null) {
                throw new IllegalArgumentException("Integration capability must not be null.");
            }
            copy.add(capability);
        }
        return new IntegrationCapabilityProfile(
            format,
            Set.copyOf(copy)
        );
    }

    /**
     * Создает пустой профиль возможностей.
     *
     * @param format внешний формат
     * @return пустой профиль
     */
    public static IntegrationCapabilityProfile empty(final IntegrationFormat format) {
        return of(
            format,
            EnumSet.noneOf(IntegrationCapability.class)
        );
    }

    /**
     * Возвращает формат профиля.
     *
     * @return формат
     */
    public IntegrationFormat format() {
        return format;
    }

    /**
     * Проверяет поддержку возможности.
     *
     * @param capability возможность
     * @return true, если возможность поддерживается
     */
    public boolean supports(final IntegrationCapability capability) {
        if (capability == null) {
            throw new IllegalArgumentException("Integration capability must not be null.");
        }
        return capabilities.contains(capability);
    }

    /**
     * Возвращает immutable набор возможностей.
     *
     * @return возможности
     */
    public Set<IntegrationCapability> capabilities() {
        return capabilities;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IntegrationCapabilityProfile profile)) {
            return false;
        }
        return format == profile.format
            && Objects.equals(
                capabilities,
                profile.capabilities
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            format,
            capabilities
        );
    }
}