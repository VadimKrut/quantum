/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.redox;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;

/** Redox center: вся молекула, атом, связь, кластер или делокализованная область. */
public final class RedoxCenter {

  private final String label;
  private final RedoxCenterKind kind;
  private final List<AtomId> atomIds;

  private RedoxCenter(
      final String label, final RedoxCenterKind kind, final List<AtomId> atomIds) {
    this.label = label;
    this.kind = kind;
    this.atomIds = atomIds;
  }

  public static RedoxCenter wholeMolecule(final String label) {
    return RedoxCenter.of(label, RedoxCenterKind.WHOLE_MOLECULE, List.of());
  }

  public static RedoxCenter of(
      final String label, final RedoxCenterKind kind, final List<AtomId> atomIds) {
    final String checkedLabel = TextValue.requireText(label, "Redox center label");
    if (kind == null) {
      throw new IllegalArgumentException("Redox center kind must not be null.");
    }
    return new RedoxCenter(
        checkedLabel, kind, List.copyOf(RedoxCenter.requireAtomIds(kind, atomIds)));
  }

  public String label() {
    return this.label;
  }

  public RedoxCenterKind kind() {
    return this.kind;
  }

  public List<AtomId> atomIds() {
    return this.atomIds;
  }

  private static List<AtomId> requireAtomIds(
      final RedoxCenterKind kind, final List<AtomId> atomIds) {
    if (atomIds == null) {
      throw new IllegalArgumentException("Redox center atom ids must not be null.");
    }
    RedoxCenter.validateAtomCount(kind, atomIds.size());
    for (int i = 0; i < atomIds.size(); ++i) {
      final AtomId atomId = atomIds.get(i);
      if (atomId == null) {
        throw new IllegalArgumentException("Redox center atom id must not be null.");
      }
      for (int j = i + 1; j < atomIds.size(); ++j) {
        if (!atomId.equals(atomIds.get(j))) continue;
        throw new IllegalArgumentException("Redox center atom ids must be unique.");
      }
    }
    return atomIds;
  }

  private static void validateAtomCount(
      final RedoxCenterKind kind,
      final int count
  ) {
    switch (kind) {
      case WHOLE_MOLECULE:
        {
          if (count == 0) break;
          throw new IllegalArgumentException("Whole-molecule redox center must not list atoms.");
        }
      case ATOM:
        {
          if (count == 1) break;
          throw new IllegalArgumentException("Atom redox center requires exactly one atom.");
        }
      case BOND:
        {
          if (count == 2) break;
          throw new IllegalArgumentException("Bond redox center requires exactly two atoms.");
        }
      case CLUSTER:
      case DELOCALIZED:
        {
          if (count >= 1) break;
          throw new IllegalArgumentException("Cluster or delocalized redox center requires atoms.");
        }
      default:
        {
          throw new IllegalStateException("Unsupported redox center kind.");
        }
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof RedoxCenter)) {
      return false;
    }
    final RedoxCenter center = (RedoxCenter) other;
    return Objects.equals(this.label, center.label)
        && this.kind == center.kind
        && Objects.equals(this.atomIds, center.atomIds);
  }

  public int hashCode() {
    int result = this.label.hashCode();
    result = 31 * result + this.kind.hashCode();
    result = 31 * result + this.atomIds.hashCode();
    return result;
  }
}