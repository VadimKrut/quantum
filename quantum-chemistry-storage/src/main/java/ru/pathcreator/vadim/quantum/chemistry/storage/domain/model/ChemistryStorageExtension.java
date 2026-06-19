/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/**
 * Расширяемый блок хранения для объектов, которые пока не имеют typed-codec.
 */
public final class ChemistryStorageExtension {

  private final String kind;
  private final String id;
  private final Map<String, String> properties;
  private final List<String> bodyLines;

  private ChemistryStorageExtension(
      final String kind,
      final String id,
      final Map<String, String> properties,
      final List<String> bodyLines) {
    this.kind = kind;
    this.id = id;
    this.properties = properties;
    this.bodyLines = bodyLines;
  }

  public static ChemistryStorageExtension of(
      final String kind,
      final String id,
      final Map<String, String> properties,
      final List<String> bodyLines) {
    final String checkedKind = IdentifierValue.requireIdentifier(kind, "Storage extension kind");
    final String checkedId = IdentifierValue.requireIdentifier(id, "Storage extension id");
    if (properties == null) {
      throw new IllegalArgumentException("Storage extension properties must not be null.");
    }
    if (bodyLines == null) {
      throw new IllegalArgumentException("Storage extension body lines must not be null.");
    }
    for (final Map.Entry<String, String> entry : properties.entrySet()) {
      IdentifierValue.requireIdentifier(entry.getKey(), "Storage extension property key");
      TextValue.requireText(entry.getValue(), "Storage extension property value");
    }
    for (int i = 0; i < bodyLines.size(); ++i) {
      TextValue.requireText(bodyLines.get(i), "Storage extension body line");
    }
    return new ChemistryStorageExtension(
        checkedKind,
        checkedId,
        Map.copyOf(properties),
        List.copyOf(bodyLines));
  }

  public String kind() {
    return this.kind;
  }

  public String id() {
    return this.id;
  }

  public Map<String, String> properties() {
    return this.properties;
  }

  public List<String> bodyLines() {
    return this.bodyLines;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageExtension)) {
      return false;
    }
    final ChemistryStorageExtension extension = (ChemistryStorageExtension) other;
    return Objects.equals(this.kind, extension.kind)
        && Objects.equals(this.id, extension.id)
        && Objects.equals(this.properties, extension.properties)
        && Objects.equals(this.bodyLines, extension.bodyLines);
  }

  public int hashCode() {
    return Objects.hash(this.kind, this.id, this.properties, this.bodyLines);
  }
}