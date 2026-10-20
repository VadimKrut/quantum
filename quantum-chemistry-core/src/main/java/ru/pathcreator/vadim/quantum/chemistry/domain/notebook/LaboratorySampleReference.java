/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Stable physical-sample locator, independent of filenames and editable labels. */
public record LaboratorySampleReference(String experimentId, String sampleId) {
  public LaboratorySampleReference {
    experimentId = IdentifierValue.requireIdentifier(experimentId, "Sample experiment id");
    sampleId = IdentifierValue.requireIdentifier(sampleId, "Sample id");
  }

  public String token() {
    try {
      return "QCS1-"
          + HexFormat.of()
              .withUpperCase()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(
                          (this.experimentId + "\0" + this.sampleId)
                              .getBytes(StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  public static boolean isToken(final String text) {
    return text != null && text.matches("QCS1-[0-9A-F]{64}");
  }
}
