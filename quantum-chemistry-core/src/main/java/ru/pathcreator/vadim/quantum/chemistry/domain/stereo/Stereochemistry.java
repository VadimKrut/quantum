/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.stereo;

import java.util.List;
import java.util.Objects;

public final class Stereochemistry {

  public static final Stereochemistry EMPTY = new Stereochemistry(List.of());
  private final List<Stereocenter> centers;

  private Stereochemistry(final List<Stereocenter> centers) {
    this.centers = centers;
  }

  public static Stereochemistry of(final List<Stereocenter> centers) {
    if (centers == null || centers.isEmpty()) {
      return EMPTY;
    }
    for (int i = 0; i < centers.size(); ++i) {
      Stereocenter center = centers.get(i);
      if (center == null) {
        throw new IllegalArgumentException("Stereocenter must not be null.");
      }
      for (int j = i + 1; j < centers.size(); ++j) {
        if (center.equals(centers.get(j))) {
          throw new IllegalArgumentException("Stereochemistry contains duplicate stereocenter.");
        }
        if (!center.sameLocusAs(centers.get(j))) continue;
        throw new IllegalArgumentException(
            "Stereochemistry contains conflicting stereocenters for one locus.");
      }
    }
    return new Stereochemistry(List.copyOf(centers));
  }

  public List<Stereocenter> centers() {
    return this.centers;
  }

  public boolean empty() {
    return this.centers.isEmpty();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Stereochemistry)) {
      return false;
    }
    final Stereochemistry stereochemistry = (Stereochemistry) other;
    return Objects.equals(this.centers, stereochemistry.centers);
  }

  public int hashCode() {
    return this.centers.hashCode();
  }
}