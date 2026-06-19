/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.metadata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ChemistryMetadata {

  public static final ChemistryMetadata EMPTY = new ChemistryMetadata(null, null, Map.of());
  private final ChemistrySource source;
  private final ChemistrySourceLocation location;
  private final Map<String, String> attributes;

  private ChemistryMetadata(
      final ChemistrySource source, final ChemistrySourceLocation location, final Map<String, String> attributes) {
    this.source = source;
    this.location = location;
    this.attributes = attributes;
  }

  public static ChemistryMetadata of(
      final ChemistrySource source, final ChemistrySourceLocation location, final Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      if (source == null && location == null) {
        return EMPTY;
      }
      return new ChemistryMetadata(source, location, Map.of());
    }
    final LinkedHashMap<String, String> checkedAttributes = new LinkedHashMap<String, String>();
    for (final Map.Entry<String, String> entry : attributes.entrySet()) {
      checkedAttributes.put(
          TextValue.requireText(entry.getKey(), "Metadata attribute key"),
          TextValue.requireText(entry.getValue(), "Metadata attribute value"));
    }
    return new ChemistryMetadata(source, location, Map.copyOf(checkedAttributes));
  }

  public ChemistrySource source() {
    return this.source;
  }

  public boolean hasSource() {
    return this.source != null;
  }

  public ChemistrySourceLocation location() {
    return this.location;
  }

  public boolean hasLocation() {
    return this.location != null;
  }

  public Map<String, String> attributes() {
    return this.attributes;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryMetadata)) {
      return false;
    }
    final ChemistryMetadata metadata = (ChemistryMetadata) other;
    return Objects.equals(this.source, metadata.source)
        && Objects.equals(this.location, metadata.location)
        && Objects.equals(this.attributes, metadata.attributes);
  }

  public int hashCode() {
    return Objects.hash(this.source, this.location, this.attributes);
  }
}