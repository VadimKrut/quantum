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
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

public final class Stereocenter {

  private final StereocenterKind kind;
  private final StereochemicalDescriptor descriptor;
  private final AtomId primaryAtomId;
  private final AtomId secondaryAtomId;
  private final List<AtomId> referenceAtomIds;

  private Stereocenter(
      final StereocenterKind kind,
      final StereochemicalDescriptor descriptor,
      final AtomId primaryAtomId,
      final AtomId secondaryAtomId,
      final List<AtomId> referenceAtomIds) {
    this.kind = kind;
    this.descriptor = descriptor;
    this.primaryAtomId = primaryAtomId;
    this.secondaryAtomId = secondaryAtomId;
    this.referenceAtomIds = referenceAtomIds;
  }

  public static Stereocenter ofTetrahedralAtom(
      final AtomId centerAtomId,
      final StereochemicalDescriptor descriptor,
      final AtomId firstLigandAtomId,
      final AtomId secondLigandAtomId,
      final AtomId thirdLigandAtomId,
      final AtomId fourthLigandAtomId) {
    return Stereocenter.of(
        StereocenterKind.TETRAHEDRAL_ATOM,
        descriptor,
        centerAtomId,
        null,
        List.of(firstLigandAtomId, secondLigandAtomId, thirdLigandAtomId, fourthLigandAtomId));
  }

  public static Stereocenter ofDoubleBond(
      final AtomId firstAtomId,
      final AtomId secondAtomId,
      final StereochemicalDescriptor descriptor,
      final AtomId firstSideReferenceAtomId,
      final AtomId secondSideReferenceAtomId) {
    return Stereocenter.of(
        StereocenterKind.DOUBLE_BOND,
        descriptor,
        firstAtomId,
        secondAtomId,
        List.of(firstSideReferenceAtomId, secondSideReferenceAtomId));
  }

  public static Stereocenter ofAxial(
      final AtomId firstAxisAtomId,
      final AtomId secondAxisAtomId,
      final StereochemicalDescriptor descriptor,
      final AtomId firstSideReferenceAtomId,
      final AtomId secondSideReferenceAtomId) {
    return Stereocenter.of(
        StereocenterKind.AXIAL,
        descriptor,
        firstAxisAtomId,
        secondAxisAtomId,
        List.of(firstSideReferenceAtomId, secondSideReferenceAtomId));
  }

  public static Stereocenter ofHelical(
      final AtomId firstReferenceAtomId,
      final AtomId lastReferenceAtomId,
      final StereochemicalDescriptor descriptor,
      final List<AtomId> helixReferenceAtomIds) {
    return Stereocenter.of(
        StereocenterKind.HELICAL,
        descriptor,
        firstReferenceAtomId,
        lastReferenceAtomId,
        helixReferenceAtomIds);
  }

  public static Stereocenter ofPlanar(
      final AtomId planeAtomId,
      final StereochemicalDescriptor descriptor,
      final AtomId firstReferenceAtomId,
      final AtomId secondReferenceAtomId,
      final AtomId thirdReferenceAtomId) {
    return Stereocenter.of(
        StereocenterKind.PLANAR,
        descriptor,
        planeAtomId,
        null,
        List.of(firstReferenceAtomId, secondReferenceAtomId, thirdReferenceAtomId));
  }

  public static Stereocenter of(
      final StereocenterKind kind,
      final StereochemicalDescriptor descriptor,
      final AtomId primaryAtomId,
      final AtomId secondaryAtomId,
      final List<AtomId> referenceAtomIds) {
    if (kind == null) {
      throw new IllegalArgumentException("Stereocenter kind must not be null.");
    }
    if (descriptor == null) {
      throw new IllegalArgumentException("Stereochemical descriptor must not be null.");
    }
    if (primaryAtomId == null) {
      throw new IllegalArgumentException("Stereocenter primary atom id must not be null.");
    }
    if (kind == StereocenterKind.DOUBLE_BOND && secondaryAtomId == null) {
      throw new IllegalArgumentException("Double-bond stereocenter requires secondary atom id.");
    }
    if ((kind == StereocenterKind.AXIAL || kind == StereocenterKind.HELICAL)
        && secondaryAtomId == null) {
      throw new IllegalArgumentException(
          "Axial or helical stereocenter requires secondary atom id.");
    }
    if (secondaryAtomId != null && primaryAtomId.equals(secondaryAtomId)) {
      throw new IllegalArgumentException("Stereocenter atoms must be different.");
    }
    return new Stereocenter(
        kind,
        descriptor,
        primaryAtomId,
        secondaryAtomId,
        Stereocenter.requireReferenceAtomIds(
            kind, descriptor, primaryAtomId, secondaryAtomId, referenceAtomIds));
  }

  public StereocenterKind kind() {
    return this.kind;
  }

  public StereochemicalDescriptor descriptor() {
    return this.descriptor;
  }

  public AtomId primaryAtomId() {
    return this.primaryAtomId;
  }

  public AtomId secondaryAtomId() {
    return this.secondaryAtomId;
  }

  public List<AtomId> referenceAtomIds() {
    return this.referenceAtomIds;
  }

  public boolean hasSecondaryAtom() {
    return this.secondaryAtomId != null;
  }

  public boolean references(final AtomId atomId) {
    if (atomId == null) {
      return false;
    }
    for (int i = 0; i < this.referenceAtomIds.size(); ++i) {
      if (!this.referenceAtomIds.get(i).equals(atomId)) continue;
      return true;
    }
    return this.primaryAtomId.equals(atomId)
        || this.secondaryAtomId != null && this.secondaryAtomId.equals(atomId);
  }

  public boolean sameLocusAs(final Stereocenter other) {
    if (other == null || this.kind != other.kind) {
      return false;
    }
    switch (this.kind) {
      case TETRAHEDRAL_ATOM:
      case PLANAR:
        return this.primaryAtomId.equals(other.primaryAtomId);
      case DOUBLE_BOND:
      case AXIAL:
        return this.sameTwoAtomLocus(other);
      case HELICAL:
        return this.primaryAtomId.equals(other.primaryAtomId)
            && Objects.equals(this.secondaryAtomId, other.secondaryAtomId);
      default:
        throw new IllegalStateException("Unsupported stereocenter kind.");
    }
  }

  private static List<AtomId> requireReferenceAtomIds(
      final StereocenterKind kind,
      final StereochemicalDescriptor descriptor,
      final AtomId primaryAtomId,
      final AtomId secondaryAtomId,
      final List<AtomId> referenceAtomIds) {
    if (referenceAtomIds == null) {
      throw new IllegalArgumentException("Stereocenter reference atoms must not be null.");
    }
    if (referenceAtomIds.size() != Stereocenter.requiredReferenceCount(kind)) {
      throw new IllegalArgumentException("Stereocenter reference atom count does not match kind.");
    }
    Stereocenter.validateDescriptor(kind, descriptor);
    for (int i = 0; i < referenceAtomIds.size(); ++i) {
      AtomId referenceAtomId = referenceAtomIds.get(i);
      if (referenceAtomId == null) {
        throw new IllegalArgumentException("Stereocenter reference atom id must not be null.");
      }
      if (referenceAtomId.equals(primaryAtomId) || referenceAtomId.equals(secondaryAtomId)) {
        throw new IllegalArgumentException(
            "Stereocenter reference atoms must not repeat center atoms.");
      }
      for (int j = i + 1; j < referenceAtomIds.size(); ++j) {
        if (!referenceAtomId.equals(referenceAtomIds.get(j))) continue;
        throw new IllegalArgumentException("Stereocenter reference atoms must be unique.");
      }
    }
    return List.copyOf(referenceAtomIds);
  }

  private static int requiredReferenceCount(final StereocenterKind kind) {
    switch (kind) {
      case TETRAHEDRAL_ATOM:
        return 4;
      case DOUBLE_BOND:
      case AXIAL:
        return 2;
      case PLANAR:
      case HELICAL:
        return 3;
      default:
        throw new IllegalStateException("Unsupported stereocenter kind.");
    }
  }

  private static void validateDescriptor(
      final StereocenterKind kind, final StereochemicalDescriptor descriptor) {
    final boolean valid =
        switch (kind) {
          case TETRAHEDRAL_ATOM ->
              descriptor == StereochemicalDescriptor.R
                  || descriptor == StereochemicalDescriptor.S
                  || descriptor == StereochemicalDescriptor.D
                  || descriptor == StereochemicalDescriptor.L
                  || descriptor == StereochemicalDescriptor.UNKNOWN;
          case DOUBLE_BOND ->
              descriptor == StereochemicalDescriptor.E
                  || descriptor == StereochemicalDescriptor.Z
                  || descriptor == StereochemicalDescriptor.CIS
                  || descriptor == StereochemicalDescriptor.TRANS
                  || descriptor == StereochemicalDescriptor.UNKNOWN;
          case AXIAL ->
              descriptor == StereochemicalDescriptor.R_A
                  || descriptor == StereochemicalDescriptor.S_A
                  || descriptor == StereochemicalDescriptor.P
                  || descriptor == StereochemicalDescriptor.M
                  || descriptor == StereochemicalDescriptor.UNKNOWN;
          case HELICAL ->
              descriptor == StereochemicalDescriptor.P
                  || descriptor == StereochemicalDescriptor.M
                  || descriptor == StereochemicalDescriptor.DELTA
                  || descriptor == StereochemicalDescriptor.LAMBDA
                  || descriptor == StereochemicalDescriptor.UNKNOWN;
          case PLANAR ->
              descriptor == StereochemicalDescriptor.R
                  || descriptor == StereochemicalDescriptor.S
                  || descriptor == StereochemicalDescriptor.UNKNOWN;
        };
    if (!valid) {
      throw new IllegalArgumentException(
          "Stereochemical descriptor is not valid for stereocenter kind.");
    }
  }

  private boolean sameTwoAtomLocus(final Stereocenter other) {
    return this.primaryAtomId.equals(other.primaryAtomId)
            && Objects.equals(this.secondaryAtomId, other.secondaryAtomId)
        || this.primaryAtomId.equals(other.secondaryAtomId)
            && Objects.equals(this.secondaryAtomId, other.primaryAtomId);
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Stereocenter)) {
      return false;
    }
    final Stereocenter stereocenter = (Stereocenter) other;
    return this.kind == stereocenter.kind
        && this.descriptor == stereocenter.descriptor
        && Objects.equals(this.primaryAtomId, stereocenter.primaryAtomId)
        && Objects.equals(this.secondaryAtomId, stereocenter.secondaryAtomId)
        && Objects.equals(this.referenceAtomIds, stereocenter.referenceAtomIds);
  }

  public int hashCode() {
    return Objects.hash(
        new Object[] {
          this.kind,
          this.descriptor,
          this.primaryAtomId,
          this.secondaryAtomId,
          this.referenceAtomIds
        });
  }
}