/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

final class ClassicalCalculationBackendSelectionTest {

  @Test
  void defaultSinglePointEnergyUsesJavaNativeBackend() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("native_single_point_backend_probe"),
                    ClassicalCalculationBackendSelectionTest.hydrogenMolecule(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY)));

    Assertions.assertEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertEquals("RHF", result.requireFirstValue("method"));
    Assertions.assertTrue(result.hasValue("preparation_mode"));
    Assertions.assertFalse(result.hasValue("backend"));
    Assertions.assertFalse(result.hasValue("backend_contract"));
  }

  @Test
  void explicitSinglePointPyScfBackendDoesNotFallbackSilentlyToNativeBackend() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("explicit_pyscf_single_point_backend_probe"),
                    ClassicalCalculationBackendSelectionTest.hydrogenMolecule(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
                    List.of(
                        ClassicalCalculationOption.text("single_point_backend", "pyscf"),
                        ClassicalCalculationOption.text(
                            "pyscf_command", "definitely_missing_pyscf_command")),
                    ChemistryMetadata.EMPTY));

    Assertions.assertNotEquals(ClassicalCalculationStatus.SUCCEEDED, result.status());
    Assertions.assertFalse(result.hasValue("method"));
    Assertions.assertTrue(result.diagnostics().hasWarnings());
    Assertions.assertTrue(
        result.diagnostics().diagnostics().stream()
            .anyMatch(
                diagnostic ->
                    diagnostic.code() == ClassicalDiagnosticCode.RESULT_FAILED
                        && diagnostic.message().contains("PySCF backend failed")),
        result.diagnostics().diagnostics().toString());
  }

  @Test
  void defaultMolecularPropertyAnalysisUsesJavaNativeBackend() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("native_property_backend_probe"),
                    ClassicalCalculationBackendSelectionTest.hydrogenMolecule(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    List.of(ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS)));

    Assertions.assertEquals("java_native", result.requireFirstValue("backend"));
    Assertions.assertFalse(result.hasValue("pyscf_success"));
  }

  @Test
  void defaultMethodBenchmarkUsesJavaNativeBackend() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("native_method_benchmark_backend_probe"),
                    ClassicalCalculationBackendSelectionTest.hydrogenMolecule(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    List.of(ClassicalCalculationKind.METHOD_BENCHMARK)));

    Assertions.assertEquals("java_native", result.requireFirstValue("backend"));
    Assertions.assertFalse(result.hasValue("pyscf_benchmark_rows"));
  }

  private static Molecule hydrogenMolecule() {
    return Molecule.of(
        MoleculeId.of("h2_backend_probe"),
        "Hydrogen backend probe",
        List.of(
            Atom.of(
                AtomId.of("h0"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM)),
            Atom.of(
                AtomId.of("h1"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.74, LengthUnit.ANGSTROM))),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }
}
