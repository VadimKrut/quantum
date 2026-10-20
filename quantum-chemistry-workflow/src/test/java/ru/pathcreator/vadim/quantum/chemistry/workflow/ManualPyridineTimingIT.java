/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.problem.ElectronicHamiltonian;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.fermion.application.ElectronicHamiltonianQubitMapper;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;

public final class ManualPyridineTimingIT {

  public static void main(final String[] arguments) {
    new ManualPyridineTimingIT().printPyridineStageTiming();
  }

  @Test
  public void printPyridineStageTiming() {
    final long start = System.nanoTime();
    final ElectronicStructurePreparationResult preparation =
        new ElectronicStructurePreparationPipeline()
            .prepare(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("manual_pyridine_timing"),
                    ManualPyridineTimingIT.pyridine(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    ActiveSpace.of(4, 4),
                    List.of(ClassicalCalculationKind.HAMILTONIAN_PREPARATION),
                    List.of(
                        ClassicalCalculationOption.integer("active_space_first_orbital", 19),
                        ClassicalCalculationOption.text("hamiltonian_backend", "pyscf")),
                    ChemistryMetadata.EMPTY));
    ManualPyridineTimingIT.printTiming("prepare", start);
    final ElectronicHamiltonian hamiltonian = preparation.hamiltonian();
    System.out.println("hamiltonian integrals=" + hamiltonian.integralCount());
    final ElectronicHamiltonianQubitMapper mapper = new ElectronicHamiltonianQubitMapper();
    for (final FermionToQubitMapping mapping : FermionToQubitMapping.values()) {
      final long mappingStart = System.nanoTime();
      mapper.mapVerified(hamiltonian, mapping, 1.0E-7);
      ManualPyridineTimingIT.printTiming(mapping.name(), mappingStart);
    }
    ManualPyridineTimingIT.printTiming("total", start);
  }

  private static void printTiming(final String label, final long start) {
    final double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
    System.out.println(label + " seconds=" + seconds);
  }

  private static Molecule pyridine() {
    return Molecule.of(
        MoleculeId.of("manual.reference.pyridine"),
        "Pyridine workflow reference",
        List.of(
            ManualPyridineTimingIT.atom("n0", "N", 1.39, 0.0, 0.0),
            ManualPyridineTimingIT.atom("c1", "C", 0.695, 1.204, 0.0),
            ManualPyridineTimingIT.atom("c2", "C", -0.695, 1.204, 0.0),
            ManualPyridineTimingIT.atom("c3", "C", -1.39, 0.0, 0.0),
            ManualPyridineTimingIT.atom("c4", "C", -0.695, -1.204, 0.0),
            ManualPyridineTimingIT.atom("c5", "C", 0.695, -1.204, 0.0),
            ManualPyridineTimingIT.atom("h1", "H", 1.24, 2.148, 0.0),
            ManualPyridineTimingIT.atom("h2", "H", -1.24, 2.148, 0.0),
            ManualPyridineTimingIT.atom("h3", "H", -2.48, 0.0, 0.0),
            ManualPyridineTimingIT.atom("h4", "H", -1.24, -2.148, 0.0),
            ManualPyridineTimingIT.atom("h5", "H", 1.24, -2.148, 0.0)),
        ManualPyridineTimingIT.pyridineBonds(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static List<Bond> pyridineBonds() {
    final List<Bond> bonds =
        new ArrayList<Bond>(
            List.of(
                Bond.of(AtomId.of("n0"), AtomId.of("c1"), BondType.AROMATIC),
                Bond.of(AtomId.of("c1"), AtomId.of("c2"), BondType.AROMATIC),
                Bond.of(AtomId.of("c2"), AtomId.of("c3"), BondType.AROMATIC),
                Bond.of(AtomId.of("c3"), AtomId.of("c4"), BondType.AROMATIC),
                Bond.of(AtomId.of("c4"), AtomId.of("c5"), BondType.AROMATIC),
                Bond.of(AtomId.of("c5"), AtomId.of("n0"), BondType.AROMATIC),
                Bond.of(AtomId.of("c1"), AtomId.of("h1"), BondType.SINGLE),
                Bond.of(AtomId.of("c2"), AtomId.of("h2"), BondType.SINGLE),
                Bond.of(AtomId.of("c3"), AtomId.of("h3"), BondType.SINGLE),
                Bond.of(AtomId.of("c4"), AtomId.of("h4"), BondType.SINGLE),
                Bond.of(AtomId.of("c5"), AtomId.of("h5"), BondType.SINGLE)));
    return List.copyOf(bonds);
  }

  private static Atom atom(
      final String id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(
        AtomId.of(id), ElementSymbol.of(symbol), Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM));
  }
}
