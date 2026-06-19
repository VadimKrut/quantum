/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.method;

import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

public final class ElectronicStructureMethodName {

  private final String value;

  private ElectronicStructureMethodName(final String value) {
    this.value = value;
  }

  public static ElectronicStructureMethodName of(final String value) {
    return new ElectronicStructureMethodName(
        TextValue.requireText(value, "Electronic structure method name"));
  }

  public String value() {
    return this.value;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ElectronicStructureMethodName)) {
      return false;
    }
    final ElectronicStructureMethodName name = (ElectronicStructureMethodName) other;
    return this.value.equals(name.value);
  }

  public int hashCode() {
    return this.value.hashCode();
  }
}