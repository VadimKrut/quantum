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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ru.pathcreator.vadim.quantum.application.integration.format.IntegrationFormat;
import ru.pathcreator.vadim.quantum.domain.gate.ParameterExpressionKind;

/**
 * Immutable профиль возможностей конкретного import/export adapter.
 */
public final class IntegrationCapabilityProfile {

    public static final long UNBOUNDED_QUBIT_COUNT = -1L;

    /**
     * Формат, к которому относится профиль.
     */
    private final IntegrationFormat format;

    /**
     * Человекочитаемое имя target-а.
     */
    private final String targetName;

    /**
     * Версия target-а или спецификации.
     */
    private final String targetVersion;

    /**
     * Максимальное количество qubit или UNBOUNDED_QUBIT_COUNT.
     */
    private final long maxQubitCount;

    /**
     * Набор поддерживаемых возможностей.
     */
    private final Set<IntegrationCapability> capabilities;

    /**
     * Имена native gate, которые target принимает напрямую.
     */
    private final Set<String> nativeGateNames;

    /**
     * Типы parameter expression, которые target принимает напрямую.
     */
    private final Set<ParameterExpressionKind> supportedParameterKinds;

    /**
     * Граф связности qubit target-а.
     */
    private final TargetConnectivityGraph connectivityGraph;

    /**
     * Дополнительная metadata target-а.
     */
    private final Map<String, String> metadata;

    private IntegrationCapabilityProfile(
        final IntegrationFormat format,
        final String targetName,
        final String targetVersion,
        final long maxQubitCount,
        final Set<IntegrationCapability> capabilities,
        final Set<String> nativeGateNames,
        final Set<ParameterExpressionKind> supportedParameterKinds,
        final TargetConnectivityGraph connectivityGraph,
        final Map<String, String> metadata
    ) {
        this.format = format;
        this.targetName = targetName;
        this.targetVersion = targetVersion;
        this.maxQubitCount = maxQubitCount;
        this.capabilities = capabilities;
        this.nativeGateNames = nativeGateNames;
        this.supportedParameterKinds = supportedParameterKinds;
        this.connectivityGraph = connectivityGraph;
        this.metadata = metadata;
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
        return of(
            format,
            defaultTargetName(format),
            "unspecified",
            UNBOUNDED_QUBIT_COUNT,
            capabilities,
            Set.of(),
            Set.of(),
            TargetConnectivityGraph.allToAll(),
            Map.of()
        );
    }

    /**
     * Создает полный target capability profile.
     *
     * @param format внешний формат
     * @param targetName имя target-а
     * @param targetVersion версия target-а
     * @param maxQubitCount максимальное количество qubit или UNBOUNDED_QUBIT_COUNT
     * @param capabilities поддерживаемые возможности
     * @param nativeGateNames native gate names или пустой набор без ограничения
     * @param supportedParameterKinds parameter expression kinds или пустой набор без ограничения
     * @param connectivityGraph граф связности
     * @param metadata metadata target-а
     * @return полный target capability profile
     */
    public static IntegrationCapabilityProfile of(
        final IntegrationFormat format,
        final String targetName,
        final String targetVersion,
        final long maxQubitCount,
        final Set<IntegrationCapability> capabilities,
        final Set<String> nativeGateNames,
        final Set<ParameterExpressionKind> supportedParameterKinds,
        final TargetConnectivityGraph connectivityGraph,
        final Map<String, String> metadata
    ) {
        if (format == null) {
            throw new IllegalArgumentException("Integration format must not be null.");
        }
        if (
            targetName == null
            || targetName.isBlank()
        ) {
            throw new IllegalArgumentException("Target name must not be blank.");
        }
        if (
            targetVersion == null
            || targetVersion.isBlank()
        ) {
            throw new IllegalArgumentException("Target version must not be blank.");
        }
        if (
            maxQubitCount != UNBOUNDED_QUBIT_COUNT
            && maxQubitCount <= 0
        ) {
            throw new IllegalArgumentException("Target max qubit count must be positive or UNBOUNDED_QUBIT_COUNT.");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("Integration capabilities must not be null.");
        }
        if (nativeGateNames == null) {
            throw new IllegalArgumentException("Native gate names must not be null.");
        }
        if (supportedParameterKinds == null) {
            throw new IllegalArgumentException("Supported parameter kinds must not be null.");
        }
        if (connectivityGraph == null) {
            throw new IllegalArgumentException("Target connectivity graph must not be null.");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Target metadata must not be null.");
        }
        final EnumSet<IntegrationCapability> copy = EnumSet.noneOf(IntegrationCapability.class);
        for (IntegrationCapability capability : capabilities) {
            if (capability == null) {
                throw new IllegalArgumentException("Integration capability must not be null.");
            }
            copy.add(capability);
        }
        final Set<String> gateNames = copyNativeGateNames(nativeGateNames);
        final EnumSet<ParameterExpressionKind> parameterKinds = EnumSet.noneOf(ParameterExpressionKind.class);
        for (ParameterExpressionKind kind : supportedParameterKinds) {
            if (kind == null) {
                throw new IllegalArgumentException("Supported parameter kind must not be null.");
            }
            parameterKinds.add(kind);
        }
        return new IntegrationCapabilityProfile(
            format,
            targetName,
            targetVersion,
            maxQubitCount,
            Set.copyOf(copy),
            gateNames,
            Set.copyOf(parameterKinds),
            connectivityGraph,
            copyMetadata(metadata)
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
     * Возвращает имя target-а.
     *
     * @return имя target-а
     */
    public String targetName() {
        return targetName;
    }

    /**
     * Возвращает версию target-а.
     *
     * @return версия target-а
     */
    public String targetVersion() {
        return targetVersion;
    }

    /**
     * Проверяет, задан ли hard limit по количеству qubit.
     *
     * @return true, если limit задан
     */
    public boolean hasMaxQubitCount() {
        return maxQubitCount != UNBOUNDED_QUBIT_COUNT;
    }

    /**
     * Возвращает максимальное количество qubit.
     *
     * @return максимальное количество qubit или UNBOUNDED_QUBIT_COUNT
     */
    public long maxQubitCount() {
        return maxQubitCount;
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

    /**
     * Проверяет, задан ли explicit native gate set.
     *
     * @return true, если native gate set задан
     */
    public boolean hasNativeGateSet() {
        return !nativeGateNames.isEmpty();
    }

    /**
     * Проверяет, является ли gate native для target-а.
     *
     * @param gateName имя gate
     * @return true, если gate поддержан или native gate set не задан
     */
    public boolean supportsNativeGate(final String gateName) {
        if (
            gateName == null
            || gateName.isBlank()
        ) {
            throw new IllegalArgumentException("Native gate name must not be blank.");
        }
        return nativeGateNames.isEmpty() || nativeGateNames.contains(gateName);
    }

    /**
     * Возвращает immutable native gate names.
     *
     * @return native gate names
     */
    public Set<String> nativeGateNames() {
        return nativeGateNames;
    }

    /**
     * Проверяет, задан ли explicit set поддерживаемых типов параметров.
     *
     * @return true, если set задан
     */
    public boolean hasSupportedParameterKinds() {
        return !supportedParameterKinds.isEmpty();
    }

    /**
     * Проверяет поддержку типа parameter expression.
     *
     * @param kind тип parameter expression
     * @return true, если тип поддержан или set не задан
     */
    public boolean supportsParameterKind(final ParameterExpressionKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Parameter expression kind must not be null.");
        }
        return supportedParameterKinds.isEmpty() || supportedParameterKinds.contains(kind);
    }

    /**
     * Возвращает immutable set поддерживаемых типов параметров.
     *
     * @return supported parameter kinds
     */
    public Set<ParameterExpressionKind> supportedParameterKinds() {
        return supportedParameterKinds;
    }

    /**
     * Возвращает граф связности target-а.
     *
     * @return граф связности
     */
    public TargetConnectivityGraph connectivityGraph() {
        return connectivityGraph;
    }

    /**
     * Возвращает metadata target-а.
     *
     * @return immutable metadata
     */
    public Map<String, String> metadata() {
        return metadata;
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
            && maxQubitCount == profile.maxQubitCount
            && Objects.equals(
                targetName,
                profile.targetName
            )
            && Objects.equals(
                targetVersion,
                profile.targetVersion
            )
            && Objects.equals(
                capabilities,
                profile.capabilities
            )
            && Objects.equals(
                nativeGateNames,
                profile.nativeGateNames
            )
            && Objects.equals(
                supportedParameterKinds,
                profile.supportedParameterKinds
            )
            && Objects.equals(
                connectivityGraph,
                profile.connectivityGraph
            )
            && Objects.equals(
                metadata,
                profile.metadata
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            format,
            targetName,
            targetVersion,
            maxQubitCount,
            capabilities,
            nativeGateNames,
            supportedParameterKinds,
            connectivityGraph,
            metadata
        );
    }

    private static String defaultTargetName(final IntegrationFormat format) {
        if (format == null) {
            return "unknown";
        }
        return format.name();
    }

    private static Set<String> copyNativeGateNames(final Set<String> gateNames) {
        final LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        for (String gateName : gateNames) {
            if (
                gateName == null
                || gateName.isBlank()
            ) {
                throw new IllegalArgumentException("Native gate name must not be blank.");
            }
            copy.put(
                gateName,
                gateName
            );
        }
        return Set.copyOf(copy.keySet());
    }

    private static Map<String, String> copyMetadata(final Map<String, String> metadata) {
        final LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        final String[] keys = metadata.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final String value = metadata.get(key);
            if (
                key == null
                || key.isBlank()
            ) {
                throw new IllegalArgumentException("Target metadata key must not be blank.");
            }
            if (value == null) {
                throw new IllegalArgumentException("Target metadata value must not be null.");
            }
            copy.put(
                key,
                value
            );
        }
        return Map.copyOf(copy);
    }
}