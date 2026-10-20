/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.template;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Стабильный идентификатор именованного шаблона органической реакции. */
public final class OrganicReactionTemplateId {

  private final String value;

  private OrganicReactionTemplateId(final String value) {
    this.value = value;
  }

  public static OrganicReactionTemplateId of(final String value) {
    return new OrganicReactionTemplateId(
        IdentifierValue.requireIdentifier(value, "Organic reaction template id"));
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrganicReactionTemplateId)) {
      return false;
    }
    final OrganicReactionTemplateId id = (OrganicReactionTemplateId) other;
    return Objects.equals(this.value, id.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }
}
