/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;

/** Content-addressed attachment; the hash is also its safe on-disk name. */
public record LaboratoryAttachment(String id, String name, String sha256, long size) {
  public LaboratoryAttachment {
    id = IdentifierValue.requireIdentifier(id, "Attachment id");
    name = TextValue.requireText(name, "Attachment name");
    Objects.requireNonNull(sha256, "Attachment hash");
    if (!sha256.matches("[a-f0-9]{64}") || size < 0) {
      throw new IllegalArgumentException("Invalid attachment fingerprint or size.");
    }
  }

  public String toString() {
    return this.name + " (" + this.size + " байт)";
  }
}
