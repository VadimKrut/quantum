/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.capability;

import java.util.Objects;

/**
 * Одна проверенная возможность: что именно поддержано, частично поддержано или требует внешнего backend.
 */
public final class ChemistryCapabilityItem {

  private final String key;
  private final ChemistryCapabilityStatus status;
  private final String message;

  private ChemistryCapabilityItem(
      final String key, final ChemistryCapabilityStatus status, final String message) {
    this.key = key;
    this.status = status;
    this.message = message;
  }

  public static ChemistryCapabilityItem of(
      final String key, final ChemistryCapabilityStatus status, final String message) {
    if (key == null || key.isBlank()) {
      throw new IllegalArgumentException("Chemistry capability key must not be blank.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Chemistry capability status must not be null.");
    }
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Chemistry capability message must not be blank.");
    }
    return new ChemistryCapabilityItem(key, status, message);
  }

  public String key() {
    return this.key;
  }

  public ChemistryCapabilityStatus status() {
    return this.status;
  }

  public String message() {
    return this.message;
  }

  public boolean supported() {
    return this.status == ChemistryCapabilityStatus.SUPPORTED;
  }

  public boolean partial() {
    return this.status == ChemistryCapabilityStatus.PARTIAL;
  }

  public boolean unsupported() {
    return this.status == ChemistryCapabilityStatus.UNSUPPORTED;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryCapabilityItem)) {
      return false;
    }
    final ChemistryCapabilityItem item = (ChemistryCapabilityItem) other;
    return Objects.equals(this.key, item.key)
        && this.status == item.status
        && Objects.equals(this.message, item.message);
  }

  public int hashCode() {
    return Objects.hash(this.key, this.status, this.message);
  }
}
