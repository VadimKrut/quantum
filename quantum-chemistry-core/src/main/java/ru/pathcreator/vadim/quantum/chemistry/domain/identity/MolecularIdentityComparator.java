/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

public final class MolecularIdentityComparator {

  private MolecularIdentityComparator() {}

  public static MolecularComparisonResult compare(
      final Molecule first,
      final Molecule second
  ) {
    if (first == null) {
      throw new IllegalArgumentException("First molecule must not be null.");
    }
    if (second == null) {
      throw new IllegalArgumentException("Second molecule must not be null.");
    }
    final boolean sameFormula = first.formula().equals(second.formula());
    if (!sameFormula) {
      return MolecularComparisonResult.of(
          MolecularRelationshipKind.DIFFERENT_FORMULA, false, false, false, false, false);
    }
    List<int[]> mappings = MolecularIdentityComparator.findMappings(first, second, true);
    if (mappings.isEmpty()) {
      return MolecularIdentityComparator.comparisonWithoutStrictMapping(first, second);
    }
    return MolecularIdentityComparator.comparisonWithMappings(first, second, mappings);
  }

  private static MolecularComparisonResult comparisonWithMappings(
      final Molecule first, final Molecule second, final List<int[]> mappings) {
    boolean hasSameStereochemistry = false;
    boolean hasSameConformationWithSameStereo = false;
    boolean hasEnantiomeric = false;
    for (int i = 0; i < mappings.size(); ++i) {
      int[] mapping = mappings.get(i);
      final boolean sameStereochemistry =
          MolecularIdentityComparator.sameStereochemistry(first, second, mapping);
      if (sameStereochemistry) {
        hasSameStereochemistry = true;
        if (!MolecularIdentityComparator.sameConformation(first, second, mapping)) continue;
        hasSameConformationWithSameStereo = true;
        continue;
      }
      if (!MolecularIdentityComparator.enantiomeric(first, second, mapping)) continue;
      hasEnantiomeric = true;
    }
    return MolecularComparisonResult.of(
        MolecularIdentityComparator.relationshipKind(
            hasSameStereochemistry, hasEnantiomeric, hasSameConformationWithSameStereo),
        true,
        true,
        hasSameStereochemistry,
        hasEnantiomeric,
        hasSameConformationWithSameStereo);
  }

  private static MolecularComparisonResult comparisonWithoutStrictMapping(
      final Molecule first, final Molecule second) {
    final boolean sameSkeleton =
        !MolecularIdentityComparator.findMappings(first, second, false).isEmpty();
    return MolecularComparisonResult.of(
        sameSkeleton
            ? MolecularRelationshipKind.SAME_FORMULA_DIFFERENT_ELECTRONIC_STATE
            : MolecularRelationshipKind.CONSTITUTIONAL_ISOMER,
        true,
        false,
        false,
        false,
        false);
  }

  private static MolecularRelationshipKind relationshipKind(
      final boolean sameStereochemistry, final boolean enantiomeric, final boolean sameConformation) {
    if (sameStereochemistry && sameConformation) {
      return MolecularRelationshipKind.SAME;
    }
    if (sameStereochemistry) {
      return MolecularRelationshipKind.CONFORMER;
    }
    if (enantiomeric) {
      return MolecularRelationshipKind.ENANTIOMER;
    }
    return MolecularRelationshipKind.DIASTEREOMER;
  }

  private static List<int[]> findMappings(
      final Molecule first, final Molecule second, final boolean strictAtomState) {
    if (first.atomCount() != second.atomCount() || first.bondCount() != second.bondCount()) {
      return List.of();
    }
    final int atomCount = first.atomCount();
    int[] mapping = new int[atomCount];
    boolean[] used = new boolean[atomCount];
    for (int i = 0; i < atomCount; ++i) {
      mapping[i] = -1;
    }
    final ArrayList<int[]> mappings = new ArrayList<int[]>();
    MolecularIdentityComparator.collectMappings(
        first, second, strictAtomState, mapping, used, 0, mappings);
    return List.copyOf(mappings);
  }

  private static void collectMappings(
      final Molecule first,
      final Molecule second,
      final boolean strictAtomState,
      final int[] mapping,
      final boolean[] used,
      final int firstIndex,
      final List<int[]> mappings) {
    if (firstIndex == first.atomCount()) {
      mappings.add((int[]) mapping.clone());
      return;
    }
    final Atom firstAtom = first.atoms().get(firstIndex);
    for (int secondIndex = 0; secondIndex < second.atomCount(); ++secondIndex) {
      if (used[secondIndex]
          || !MolecularIdentityComparator.atomCompatible(
              firstAtom, second.atoms().get(secondIndex), strictAtomState)) continue;
      mapping[firstIndex] = secondIndex;
      used[secondIndex] = true;
      if (MolecularIdentityComparator.partialBondsCompatible(first, second, mapping, firstIndex)) {
        MolecularIdentityComparator.collectMappings(
            first, second, strictAtomState, mapping, used, firstIndex + 1, mappings);
      }
      used[secondIndex] = false;
      mapping[firstIndex] = -1;
    }
  }

  private static boolean atomCompatible(
      final Atom first,
      final Atom second,
      final boolean strictAtomState
  ) {
    if (!first.element().symbol().equals(second.element().symbol())) {
      return false;
    }
    if (!Objects.equals(first.isotope(), second.isotope())) {
      return false;
    }
    if (!strictAtomState) {
      return true;
    }
    return first.formalCharge().equals(second.formalCharge())
        && first.radicalState().equals(second.radicalState());
  }

  private static boolean partialBondsCompatible(
      final Molecule first, final Molecule second, final int[] mapping, final int newestFirstIndex) {
    for (int firstIndex = 0; firstIndex < newestFirstIndex; ++firstIndex) {
      BondType secondBondType;
      BondType firstBondType;
      if (mapping[firstIndex] < 0
          || (firstBondType =
                  MolecularIdentityComparator.bondTypeBetween(first, firstIndex, newestFirstIndex))
              == (secondBondType =
                  MolecularIdentityComparator.bondTypeBetween(
                      second, mapping[firstIndex], mapping[newestFirstIndex]))) continue;
      return false;
    }
    return true;
  }

  private static BondType bondTypeBetween(
      final Molecule molecule,
      final int firstIndex,
      final int secondIndex
  ) {
    final AtomId firstAtomId = molecule.atoms().get(firstIndex).id();
    final AtomId secondAtomId = molecule.atoms().get(secondIndex).id();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.connects(firstAtomId, secondAtomId)) continue;
      return bond.type();
    }
    return null;
  }

  private static boolean sameStereochemistry(
      final Molecule first,
      final Molecule second,
      final int[] mapping
  ) {
    return MolecularIdentityComparator.stereochemistryMatches(first, second, mapping, false);
  }

  private static boolean enantiomeric(
      final Molecule first,
      final Molecule second,
      final int[] mapping
  ) {
    if (first.stereochemistry().empty() || second.stereochemistry().empty()) {
      return false;
    }
    return MolecularIdentityComparator.stereochemistryMatches(first, second, mapping, true);
  }

  private static boolean stereochemistryMatches(
      final Molecule first, final Molecule second, final int[] mapping, final boolean inverted) {
    int i;
    final List<Stereocenter> firstCenters = first.stereochemistry().centers();
    final List<Stereocenter> secondCenters = second.stereochemistry().centers();
    if (firstCenters.size() != secondCenters.size()) {
      return false;
    }
    ArrayList<String> secondKeys = new ArrayList<String>(secondCenters.size());
    for (i = 0; i < secondCenters.size(); ++i) {
      secondKeys.add(
          MolecularIdentityComparator.stereoKey(secondCenters.get(i), null, second, false));
    }
    for (i = 0; i < firstCenters.size(); ++i) {
      final String mappedKey =
          MolecularIdentityComparator.stereoKey(firstCenters.get(i), mapping, first, inverted);
      if (mappedKey != null && MolecularIdentityComparator.removeFirst(secondKeys, mappedKey))
        continue;
      return false;
    }
    return secondKeys.isEmpty();
  }

  private static String stereoKey(
      final Stereocenter center, final int[] mapping, final Molecule mappingSource, final boolean inverted) {
    StereochemicalDescriptor descriptor =
        inverted ? MolecularIdentityComparator.inverted(center.descriptor()) : center.descriptor();
    final StereochemicalDescriptor stereochemicalDescriptor = descriptor;
    if (descriptor == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    builder.append(center.kind().name());
    builder.append('|');
    builder.append(descriptor.name());
    builder.append('|');
    MolecularIdentityComparator.appendMappedAtom(
        builder, center.primaryAtomId(), mapping, mappingSource);
    builder.append('|');
    if (center.hasSecondaryAtom()) {
      MolecularIdentityComparator.appendMappedAtom(
          builder, center.secondaryAtomId(), mapping, mappingSource);
    }
    final List<AtomId> referenceAtomIds = center.referenceAtomIds();
    for (int i = 0; i < referenceAtomIds.size(); ++i) {
      builder.append('|');
      MolecularIdentityComparator.appendMappedAtom(
          builder, referenceAtomIds.get(i), mapping, mappingSource);
    }
    return builder.toString();
  }

  private static StereochemicalDescriptor inverted(final StereochemicalDescriptor descriptor) {
    switch (descriptor) {
      case R:
        return StereochemicalDescriptor.S;
      case S:
        return StereochemicalDescriptor.R;
      case R_A:
        return StereochemicalDescriptor.S_A;
      case S_A:
        return StereochemicalDescriptor.R_A;
      case P:
        return StereochemicalDescriptor.M;
      case M:
        return StereochemicalDescriptor.P;
      case DELTA:
        return StereochemicalDescriptor.LAMBDA;
      case LAMBDA:
        return StereochemicalDescriptor.DELTA;
      case D:
        return StereochemicalDescriptor.L;
      case L:
        return StereochemicalDescriptor.D;
      case E:
      case Z:
      case CIS:
      case TRANS:
      case UNKNOWN:
        return null;
      default:
        throw new IllegalStateException("Unsupported stereochemical descriptor.");
    }
  }

  private static boolean sameConformation(
      final Molecule first,
      final Molecule second,
      final int[] mapping) {
    int i;
    final List<TorsionAngle> firstAngles = first.conformation().torsionAngles();
    final List<TorsionAngle> secondAngles = second.conformation().torsionAngles();
    if (firstAngles.size() != secondAngles.size()) {
      return false;
    }
    final ArrayList<String> secondKeys = new ArrayList<String>(secondAngles.size());
    for (i = 0; i < secondAngles.size(); ++i) {
      secondKeys.add(MolecularIdentityComparator.torsionKey(secondAngles.get(i), null, second));
    }
    for (i = 0; i < firstAngles.size(); ++i) {
      final String key = MolecularIdentityComparator.torsionKey(firstAngles.get(i), mapping, first);
      final String reversedKey =
          MolecularIdentityComparator.reversedTorsionKey(firstAngles.get(i), mapping, first);
      if (MolecularIdentityComparator.removeFirst(secondKeys, key)
          || MolecularIdentityComparator.removeFirst(secondKeys, reversedKey)) continue;
      return false;
    }
    return secondKeys.isEmpty();
  }

  private static String torsionKey(
      final TorsionAngle angle,
      final int[] mapping,
      final Molecule mappingSource
  ) {
    StringBuilder builder = new StringBuilder();
    MolecularIdentityComparator.appendMappedAtom(
        builder, angle.firstAtomId(), mapping, mappingSource);
    MolecularIdentityComparator.appendTorsionAtom(
        builder, angle.secondAtomId(), mapping, mappingSource);
    MolecularIdentityComparator.appendTorsionAtom(
        builder, angle.thirdAtomId(), mapping, mappingSource);
    MolecularIdentityComparator.appendTorsionAtom(
        builder, angle.fourthAtomId(), mapping, mappingSource);
    builder.append('|');
    builder.append(angle.degrees());
    return builder.toString();
  }

  private static String reversedTorsionKey(
      final TorsionAngle angle, final int[] mapping, final Molecule mappingSource) {
    final StringBuilder builder = new StringBuilder();
    MolecularIdentityComparator.appendMappedAtom(
        builder, angle.fourthAtomId(), mapping, mappingSource);
    MolecularIdentityComparator.appendTorsionAtom(
        builder, angle.thirdAtomId(), mapping, mappingSource);
    MolecularIdentityComparator.appendTorsionAtom(
        builder, angle.secondAtomId(), mapping, mappingSource);
    MolecularIdentityComparator.appendTorsionAtom(
        builder, angle.firstAtomId(), mapping, mappingSource);
    builder.append('|');
    builder.append(angle.degrees());
    return builder.toString();
  }

  private static void appendTorsionAtom(
      final StringBuilder builder, final AtomId atomId, final int[] mapping, final Molecule mappingSource) {
    builder.append('|');
    MolecularIdentityComparator.appendMappedAtom(builder, atomId, mapping, mappingSource);
  }

  private static void appendMappedAtom(
      final StringBuilder builder, final AtomId atomId, final int[] mapping, final Molecule mappingSource) {
    if (mapping == null) {
      builder.append(MolecularIdentityComparator.indexOfAtom(mappingSource, atomId));
    } else {
      builder.append(mapping[MolecularIdentityComparator.indexOfAtom(mappingSource, atomId)]);
    }
  }

  private static int indexOfAtom(
      final Molecule molecule,
      final AtomId atomId
  ) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).id().equals(atomId)) continue;
      return i;
    }
    throw new IllegalArgumentException("Mapped atom id is not present in source molecule.");
  }

  private static boolean removeFirst(
      final List<String> values,
      final String value
  ) {
    for (int i = 0; i < values.size(); ++i) {
      if (!values.get(i).equals(value)) continue;
      values.remove(i);
      return true;
    }
    return false;
  }
}