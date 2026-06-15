/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.backend;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ru.pathcreator.vadim.quantum.application.integration.capability.IntegrationCapabilityProfile;

public final class BackendDescriptor {

    private final String backendId;
    private final String displayName;
    private final String version;
    private final IntegrationCapabilityProfile targetProfile;
    private final Set<BackendCapability> capabilities;
    private final Map<String, String> metadata;

    private BackendDescriptor(
        final String backendId,
        final String displayName,
        final String version,
        final IntegrationCapabilityProfile targetProfile,
        final Set<BackendCapability> capabilities,
        final Map<String, String> metadata
    ) {
        this.backendId = validateText(
            backendId,
            "Backend id"
        );
        this.displayName = validateText(
            displayName,
            "Backend display name"
        );
        this.version = validateText(
            version,
            "Backend version"
        );
        if (targetProfile == null) {
            throw new IllegalArgumentException("Backend target profile must not be null.");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("Backend capabilities must not be null.");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Backend metadata must not be null.");
        }
        final EnumSet<BackendCapability> copiedCapabilities = EnumSet.noneOf(BackendCapability.class);
        for (BackendCapability capability : capabilities) {
            if (capability == null) {
                throw new IllegalArgumentException("Backend capability must not be null.");
            }
            copiedCapabilities.add(capability);
        }
        this.targetProfile = targetProfile;
        this.capabilities = Set.copyOf(copiedCapabilities);
        this.metadata = copyMetadata(metadata);
    }

    public static BackendDescriptor of(
        final String backendId,
        final String displayName,
        final String version,
        final IntegrationCapabilityProfile targetProfile,
        final Set<BackendCapability> capabilities,
        final Map<String, String> metadata
    ) {
        return new BackendDescriptor(
            backendId,
            displayName,
            version,
            targetProfile,
            capabilities,
            metadata
        );
    }

    public String backendId() {
        return backendId;
    }

    public String displayName() {
        return displayName;
    }

    public String version() {
        return version;
    }

    public IntegrationCapabilityProfile targetProfile() {
        return targetProfile;
    }

    public Set<BackendCapability> capabilities() {
        return capabilities;
    }

    public boolean supports(final BackendCapability capability) {
        return capabilities.contains(capability);
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    private static String validateText(
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

    private static Map<String, String> copyMetadata(final Map<String, String> metadata) {
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String key : metadata.keySet()) {
            final String value = metadata.get(key);
            if (
                key == null
                || key.isBlank()
            ) {
                throw new IllegalArgumentException("Backend metadata key must not be blank.");
            }
            if (value == null) {
                throw new IllegalArgumentException("Backend metadata value must not be null.");
            }
            result.put(
                key,
                value
            );
        }
        return Map.copyOf(result);
    }
}