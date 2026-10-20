/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.ClassicalChemistryService;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationId;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalCalculationStatus;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSetName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodName;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;

public final class AbInitioReferenceRegressionTest {

  @Test
  public void hydrogenRhfAndMp2SinglePointMethodsMatchReferenceValues() {
    AbInitioReferenceRegressionTest.assertSinglePoint(
        "h2_rhf_reference",
        ElectronicStructureMethod.HARTREE_FOCK,
        BasisSet.STO_3G,
        -1.1166596958005601,
        1.0E-5);
    AbInitioReferenceRegressionTest.assertSinglePoint(
        "h2_mp2_reference",
        ElectronicStructureMethod.of(
            ElectronicStructureMethodType.PERTURBATION_THEORY,
            ElectronicStructureMethodName.of("MP2")),
        BasisSet.STO_3G,
        -1.1298409409176147,
        2.0E-5);
  }

  @Test
  public void hydrogenRhfMatchesPyscfReferenceWithSelfContained631gBasisText() {
    AbInitioReferenceRegressionTest.assertSinglePoint(
        "h2_rhf_631g_text_reference",
        ElectronicStructureMethod.HARTREE_FOCK,
        AbInitioReferenceRegressionTest.hydrogen631g(),
        -1.126726567372355,
        2.0E-5);
  }

  @Test
  public void hydrogenElectronicFiniteDifferenceGradientMatchesPyscfReference() {
    final ClassicalChemistryService service = new ClassicalChemistryService();
    final ClassicalCalculationRequest request =
        ClassicalCalculationRequest.of(
            ClassicalCalculationId.of("h2_gradient_pyscf_reference"),
            AbInitioReferenceRegressionTest.hydrogenAtDistance(0.8),
            ElectronicStructureMethod.HARTREE_FOCK,
            BasisSet.STO_3G,
            null,
            List.of(ClassicalCalculationKind.GEOMETRY_GRADIENT),
            List.of(
                ClassicalCalculationOption.text(
                    "geometry_energy_model", "electronic_structure_finite_difference"),
                ClassicalCalculationOption.floating(
                    "geometry_finite_difference_step_angstrom", 5.0E-4)),
            ChemistryMetadata.EMPTY);
    final ClassicalCalculationResult result = service.execute(request);

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics()));
    assertEquals(
        0.139429260815227, AbInitioReferenceRegressionTest.gradientZ(result, "h1"), 2.0E-5);
    assertEquals(
        -0.139429260815227, AbInitioReferenceRegressionTest.gradientZ(result, "h0"), 2.0E-5);
  }

  @Test
  public void hydrogenDftReferenceFeedsActiveSpaceAndPropertyAnalysis() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_dft_reference_downstream_analysis"),
                    AbInitioReferenceRegressionTest.hydrogen(),
                    ElectronicStructureMethod.densityFunctional("PBE"),
                    BasisSet.STO_3G,
                    ActiveSpace.of(2, 2),
                    List.of(
                        ClassicalCalculationKind.SINGLE_POINT_ENERGY,
                        ClassicalCalculationKind.HAMILTONIAN_PREPARATION,
                        ClassicalCalculationKind.ACTIVE_SPACE_SELECTION,
                        ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS),
                    List.of(),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.PARTIAL,
        result.status(),
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics()));
    assertTrue(
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics())
            .contains("explicitly marked approximate model"));
    assertEquals("DFT_KOHN_SHAM", result.requireFirstValue("method"));
    assertEquals("KOHN_SHAM_DFT", result.requireFirstValue("reference_method"));
    assertEquals("KOHN_SHAM_DFT", result.requireFirstValue("reference"));
    assertEquals(2.0, result.requireScalar("density_trace"), 1.0E-7);
    assertEquals(2.0, result.requireScalar("active_electron_count"), 1.0E-12);
    assertTrue(result.requireScalar("homo_lumo_gap") >= 0.0);
    assertTrue(result.requireScalar("total_integral_count") > 0.0);
  }

  @Test
  public void hydrogenCasscfSinglePointSelectsActiveSpaceAutomatically() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_casscf_auto_active_space"),
                    AbInitioReferenceRegressionTest.hydrogen(),
                    ElectronicStructureMethod.of(
                        ElectronicStructureMethodType.MULTI_CONFIGURATIONAL_SELF_CONSISTENT_FIELD,
                        ElectronicStructureMethodName.of("CASSCF")),
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
                    List.of(),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics()));
    assertEquals(
        "active_space_casscf_orbital_optimization", result.requireFirstValue("implemented_kernel"));
    assertTrue(result.requireScalar("active_space_orbitals") >= 1.0);
    assertTrue(
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics())
            .contains("Active space was selected automatically"));
  }

  @Test
  public void hydrogenQuantumProgramPreparationSelectsActiveSpaceAutomatically() {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of("h2_quantum_program_auto_active_space"),
                    AbInitioReferenceRegressionTest.hydrogen(),
                    ElectronicStructureMethod.HARTREE_FOCK,
                    BasisSet.STO_3G,
                    null,
                    List.of(ClassicalCalculationKind.QUANTUM_PROGRAM_PREPARATION),
                    List.of(
                        ClassicalCalculationOption.integer("active_space_max_orbitals", 2),
                        ClassicalCalculationOption.integer("active_space_min_orbitals", 1)),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics()));
    assertEquals("true", result.requireFirstValue("generation_complete"));
    assertTrue(result.requireScalar("artifact_count") > 0.0);
  }

  private static void assertSinglePoint(
      final String id,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final double expectedTotalEnergy,
      final double tolerance,
      final ClassicalCalculationOption... options) {
    final ClassicalCalculationResult result =
        new ClassicalChemistryService()
            .execute(
                ClassicalCalculationRequest.of(
                    ClassicalCalculationId.of(id),
                    AbInitioReferenceRegressionTest.hydrogen(),
                    method,
                    basisSet,
                    null,
                    List.of(ClassicalCalculationKind.SINGLE_POINT_ENERGY),
                    List.of(options),
                    ChemistryMetadata.EMPTY));

    assertEquals(
        ClassicalCalculationStatus.SUCCEEDED,
        result.status(),
        AbInitioReferenceRegressionTest.diagnosticsText(result.diagnostics().diagnostics()));
    assertTrue(result.coverageReport().exact(), result.coverageReport().items().toString());
    assertEquals(expectedTotalEnergy, result.requireScalar("total_energy"), tolerance, id);
  }

  private static Molecule hydrogen() {
    return AbInitioReferenceRegressionTest.hydrogenAtDistance(0.741848095192207);
  }

  private static Molecule hydrogenAtDistance(final double distanceAngstrom) {
    return Molecule.of(
        MoleculeId.of("reference.h2"),
        "Hydrogen reference",
        List.of(
            AbInitioReferenceRegressionTest.atom("h0", "H", 0.0, 0.0, 0.0),
            AbInitioReferenceRegressionTest.atom("h1", "H", 0.0, 0.0, distanceAngstrom)),
        List.of(Bond.of(AtomId.of("h0"), AtomId.of("h1"), BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Atom atom(
      final String id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(
        AtomId.of(id),
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.EMPTY);
  }

  private static BasisSet hydrogen631g() {
    return BasisSet.defined(
        BasisSetName.of("6-31G"),
        """
        H 0
        S 3 1.00
          18.7311370  0.03349460
           2.8253937  0.23472695
           0.6401217  0.81375733
        S 1 1.00
           0.1612778  1.00000000
        ****
        """);
  }

  private static ClassicalCalculationOption pyScfBackend() {
    return ClassicalCalculationOption.text("single_point_backend", "pyscf");
  }

  private static String diagnosticsText(final List<ClassicalDiagnostic> diagnostics) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < diagnostics.size(); ++i) {
      final ClassicalDiagnostic diagnostic = diagnostics.get(i);
      if (i > 0) {
        builder.append(" | ");
      }
      builder
          .append(diagnostic.severity())
          .append(' ')
          .append(diagnostic.code())
          .append(' ')
          .append(diagnostic.target())
          .append(": ")
          .append(diagnostic.message());
    }
    return builder.toString();
  }

  private static double gradientZ(final ClassicalCalculationResult result, final String atomId) {
    final List<String> rows = AbInitioReferenceRegressionTest.table(result, "gradient_rows");
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 9 && atomId.equals(parts[0])) {
        return Double.parseDouble(parts[7]);
      }
    }
    throw new AssertionError("Missing gradient row for " + atomId);
  }

  private static List<String> table(final ClassicalCalculationResult result, final String name) {
    for (int sectionIndex = 0; sectionIndex < result.sections().size(); ++sectionIndex) {
      final List<
              ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue>
          values = result.sections().get(sectionIndex).values();
      for (int valueIndex = 0; valueIndex < values.size(); ++valueIndex) {
        if (name.equals(values.get(valueIndex).name())) {
          return values.get(valueIndex).values();
        }
      }
    }
    throw new AssertionError("Missing result value " + name);
  }

  private static double hessianValue(
      final ClassicalCalculationResult result,
      final int firstAtomIndex,
      final int firstAxis,
      final int secondAtomIndex,
      final int secondAxis) {
    final List<String> rows = AbInitioReferenceRegressionTest.table(result, "hessian_rows");
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 5
          && Integer.parseInt(parts[0]) == firstAtomIndex
          && Integer.parseInt(parts[1]) == firstAxis
          && Integer.parseInt(parts[2]) == secondAtomIndex
          && Integer.parseInt(parts[3]) == secondAxis) {
        return Double.parseDouble(parts[4]);
      }
    }
    throw new AssertionError("Missing Hessian row.");
  }

  private static double firstFrequency(final ClassicalCalculationResult result) {
    final List<String> frequencies = result.requireValues("frequencies");
    if (frequencies.isEmpty()) {
      throw new AssertionError("Missing frequency values.");
    }
    return Double.parseDouble(frequencies.get(0));
  }

  private static double optimizedDistance(
      final ClassicalCalculationResult result,
      final String firstAtomId,
      final String secondAtomId) {
    final double[] first = AbInitioReferenceRegressionTest.optimizedCoordinate(result, firstAtomId);
    final double[] second =
        AbInitioReferenceRegressionTest.optimizedCoordinate(result, secondAtomId);
    final double dx = first[0] - second[0];
    final double dy = first[1] - second[1];
    final double dz = first[2] - second[2];
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static double[] optimizedCoordinate(
      final ClassicalCalculationResult result, final String atomId) {
    final List<String> rows =
        AbInitioReferenceRegressionTest.table(result, "optimized_coordinates");
    for (int i = 0; i < rows.size(); ++i) {
      final String[] parts = rows.get(i).split(",");
      if (parts.length >= 5 && atomId.equals(parts[0])) {
        return new double[] {
          Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4])
        };
      }
    }
    throw new AssertionError("Missing optimized coordinate for " + atomId);
  }

  private static double firstExcitedStateValue(
      final ClassicalCalculationResult result, final int column) {
    final List<String> rows = AbInitioReferenceRegressionTest.table(result, "excited_state_rows");
    if (rows.isEmpty()) {
      throw new AssertionError("Missing excited state rows.");
    }
    final String[] parts = rows.get(0).split(",");
    if (parts.length <= column) {
      throw new AssertionError("Excited state row has too few columns.");
    }
    return Double.parseDouble(parts[column]);
  }
}
