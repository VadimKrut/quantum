/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Запрос basis set: каноническое имя, текстовое или структурное определение. */
public final class BasisSet {

  public static final BasisSet STO_3G = new BasisSet(BasisSetName.of("STO-3G"), null, null);
  public static final BasisSet SIX_31G = new BasisSet(BasisSetName.of("6-31G"), null, null);
  private final BasisSetName name;
  private final String definitionText;
  private final BasisSetDefinition definition;

  private BasisSet(
      final BasisSetName name, final String definitionText, final BasisSetDefinition definition) {
    this.name = name;
    this.definitionText = definitionText;
    this.definition = definition;
  }

  public static BasisSet of(final BasisSetName name) {
    if (name == null) {
      throw new IllegalArgumentException("Basis set name must not be null.");
    }
    return new BasisSet(name, null, null);
  }

  public static BasisSet defined(final BasisSetName name, final String definitionText) {
    if (name == null) {
      throw new IllegalArgumentException("Basis set name must not be null.");
    }
    return new BasisSet(
        name, TextValue.requireText(definitionText, "Basis set definition text"), null);
  }

  public static BasisSet structured(final BasisSetName name, final BasisSetDefinition definition) {
    if (name == null) {
      throw new IllegalArgumentException("Basis set name must not be null.");
    }
    if (definition == null) {
      throw new IllegalArgumentException("Basis set definition must not be null.");
    }
    return new BasisSet(name, null, definition);
  }

  public BasisSetName name() {
    return this.name;
  }

  public boolean hasDefinitionText() {
    return this.definitionText != null;
  }

  public boolean hasStructuredDefinition() {
    return this.definition != null;
  }

  public String definitionText() {
    if (this.definitionText == null) {
      throw new IllegalStateException("Basis set does not contain definition text.");
    }
    return this.definitionText;
  }

  public BasisSetDefinition definition() {
    if (this.definition == null) {
      throw new IllegalStateException("Basis set does not contain structured definition.");
    }
    return this.definition;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BasisSet)) {
      return false;
    }
    final BasisSet basisSet = (BasisSet) other;
    return Objects.equals(this.name, basisSet.name)
        && Objects.equals(this.definitionText, basisSet.definitionText)
        && Objects.equals(this.definition, basisSet.definition);
  }

  public int hashCode() {
    int result = this.name.hashCode();
    result = 31 * result + Objects.hashCode(this.definitionText);
    result = 31 * result + Objects.hashCode(this.definition);
    return result;
  }
}
