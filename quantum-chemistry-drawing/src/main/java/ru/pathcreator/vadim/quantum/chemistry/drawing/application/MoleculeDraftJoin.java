/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Joining two 2D drafts creates a new molecular identity, without inferring a reaction. */
public final class MoleculeDraftJoin {
  private MoleculeDraftJoin() {}

  public static MoleculeDrawing join(
      final MoleculeDrawing first,
      final AtomId from,
      final MoleculeDrawing second,
      final AtomId to,
      final BondType type) {
    check(first);
    check(second);
    final boolean managed = MoleculeSkeletonHydrogens.managed(first.molecule());
    if (managed != MoleculeSkeletonHydrogens.managed(second.molecule())) {
      throw new IllegalArgumentException(
          "Сначала выберите каждый компонент и выполните «Вид → Автоматический учёт H для выбранной структуры», затем соедините их. Ручные H сохранятся.");
    }
    if (first.molecule().atoms().stream().noneMatch(atom -> atom.id().equals(from))
        || second.molecule().atoms().stream().noneMatch(atom -> atom.id().equals(to))) {
      throw new IllegalArgumentException("Атом соединения отсутствует в структуре.");
    }
    final LinkedHashMap<AtomId, AtomId> remap = new LinkedHashMap<>();
    second
        .molecule()
        .atoms()
        .forEach(atom -> remap.put(atom.id(), AtomId.of("a_" + UUID.randomUUID())));
    final ArrayList<Atom> atoms = new ArrayList<>(first.molecule().atoms());
    second
        .molecule()
        .atoms()
        .forEach(
            atom ->
                atoms.add(
                    Atom.of(
                        remap.get(atom.id()),
                        atom.element().symbol(),
                        null,
                        atom.formalCharge(),
                        atom.isotope(),
                        atom.radicalState(),
                        atom.metadata())));
    final ArrayList<Bond> bonds = new ArrayList<>(first.molecule().bonds());
    second
        .molecule()
        .bonds()
        .forEach(
            bond ->
                bonds.add(
                    Bond.of(
                        remap.get(bond.firstAtomId()),
                        remap.get(bond.secondAtomId()),
                        bond.type())));
    bonds.add(Bond.of(from, remap.get(to), type));
    final ArrayList<AtomDrawing> positions = new ArrayList<>(first.atoms());
    second
        .atoms()
        .forEach(
            atom ->
                positions.add(
                    AtomDrawing.of(
                        remap.get(atom.atomId()),
                        atom.point2D(),
                        atom.point3D(),
                        atom.labelVisible())));
    final ArrayList<BondDrawing> styles = new ArrayList<>(first.bonds());
    second
        .bonds()
        .forEach(
            bond ->
                styles.add(
                    BondDrawing.of(
                        remap.get(bond.firstAtomId()),
                        remap.get(bond.secondAtomId()),
                        bond.style())));
    styles.add(BondDrawing.of(from, remap.get(to), BondDrawingStyle.PLAIN));
    final LinkedHashMap<String, String> provenance = new LinkedHashMap<>();
    provenance.put("laboratory.join.first.id", first.moleculeId().value());
    provenance.put("laboratory.join.second.id", second.moleculeId().value());
    provenance.put("laboratory.join.first.name", first.molecule().displayName());
    provenance.put("laboratory.join.second.name", second.molecule().displayName());
    first
        .molecule()
        .metadata()
        .attributes()
        .forEach((key, value) -> provenance.put("laboratory.join.first.metadata." + key, value));
    second
        .molecule()
        .metadata()
        .attributes()
        .forEach((key, value) -> provenance.put("laboratory.join.second.metadata." + key, value));
    final Molecule molecule =
        Molecule.of(
            MoleculeId.of("m_" + UUID.randomUUID()),
            "Соединённая структура",
            atoms,
            bonds,
            MolecularCharge.of(
                Math.addExact(
                    first.molecule().charge().value(), second.molecule().charge().value())),
            SpinMultiplicity.SINGLET,
            ChemistryMetadata.of(null, null, provenance));
    // Preserve the user's placement. Rearrangement of a diagram is a separate drawing operation.
    final var result =
        ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkRelativeStereoJoin
            .preserve(
                first, second, MoleculeDrawing.of(molecule, positions, styles, List.of()), remap);
    return managed ? MoleculeSkeletonHydrogens.normalize(result) : result;
  }

  private static void check(final MoleculeDrawing drawing) {
    final Molecule molecule = drawing.molecule();
    if (molecule.atoms().stream().anyMatch(atom -> atom.coordinate() != null)
        || !molecule.stereochemistry().empty()
        || !molecule.conformation().empty()
        || !molecule.opticalRotation().equals(OpticalRotation.UNKNOWN)
        || !molecule.symmetry().equals(MolecularSymmetry.C1)
        || !molecule.spinMultiplicity().equals(SpinMultiplicity.SINGLET)
        || molecule.metadata().hasSource()
        || molecule.metadata().hasLocation()
        || !drawing.manualFields().isEmpty()) {
      throw new IllegalArgumentException(
          "Соединение доступно для 2D-черновиков без заданной 3D-геометрии, стереохимии и специальных свойств. Исходные структуры сохранены.");
    }
  }
}
