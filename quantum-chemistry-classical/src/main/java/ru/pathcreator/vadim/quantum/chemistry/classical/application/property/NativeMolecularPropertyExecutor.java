/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.property;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationPipeline;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic.ElectronicStructurePreparationResult;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.GaussianDipoleIntegralEngine;
import ru.pathcreator.vadim.quantum.chemistry.integrals.application.IntegralMoleculeFactory;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.tensor.DipoleIntegralMatrices;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.rhf.RhfResult;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.uhf.UhfResult;

/**
 * Считает молекулярные свойства на Java-native SCF-данных без внешнего PySCF-процесса.
 */
public final class NativeMolecularPropertyExecutor {

  private static final double HARTREE_TO_EV = 27.211386245988;
  private static final double ELECTRON_ANGSTROM_TO_DEBYE = 4.803204712570263;
  private static final double ELECTRON_BOHR_TO_DEBYE = 2.541746473;
  private final ElectronicStructurePreparationPipeline preparationPipeline;
  private final GaussianDipoleIntegralEngine dipoleIntegralEngine;

  public NativeMolecularPropertyExecutor() {
    this(new ElectronicStructurePreparationPipeline(), new GaussianDipoleIntegralEngine());
  }

  public NativeMolecularPropertyExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline) {
    this(preparationPipeline, new GaussianDipoleIntegralEngine());
  }

  public NativeMolecularPropertyExecutor(
      final ElectronicStructurePreparationPipeline preparationPipeline,
      final GaussianDipoleIntegralEngine dipoleIntegralEngine) {
    if (preparationPipeline == null) {
      throw new IllegalArgumentException(
          "Native molecular property preparation pipeline must not be null.");
    }
    if (dipoleIntegralEngine == null) {
      throw new IllegalArgumentException(
          "Native molecular property dipole integral engine must not be null.");
    }
    this.preparationPipeline = preparationPipeline;
    this.dipoleIntegralEngine = dipoleIntegralEngine;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Native molecular property request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Native molecular property diagnostics must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of(
                  "calculationKind", ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS.name()),
              "Molecular property analysis requires a molecule subject."));
      return null;
    }
    final ElectronicStructurePreparationResult preparation;
    try {
      preparation = this.preparationPipeline.prepare(request);
    } catch (final IllegalArgumentException exception) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.BASIS_SET_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("basisSet", request.basisSet().name().value()),
              exception.getMessage()));
      return null;
    }
    if (!preparation.hasRhfReference()
        && !preparation.hasRohfReference()
        && !preparation.hasUhfReference()
        && !preparation.hasKohnShamDftReference()) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.METHOD_TYPE_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("method", request.method().name().value()),
              "Native molecular property analysis requires a Java-native RHF, ROHF, UHF or Kohn-Sham DFT reference."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    diagnostics.add(
        ClassicalDiagnostic.info(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of(
                "calculationKind", ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS.name()),
            "Native molecular property analysis includes Koopmans frontier IP/EA and Mulliken"
                + " population models; AO dipole integrals are reported separately where available."));
    final double[] densityMatrix = NativeMolecularPropertyExecutor.totalDensityMatrix(preparation);
    final double[] overlap = NativeMolecularPropertyExecutor.overlapMatrix(preparation);
    final double[] atomPopulations =
        NativeMolecularPropertyExecutor.mullikenPopulations(
            preparation.basisSet(), molecule, densityMatrix, overlap);
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "scf_molecular_property_analysis",
        "molecular_property_analysis",
        "SCF-derived properties are computed from Java-native electronic data, while several observables still use Mulliken/frontier approximations.");
    values.add(ClassicalResultValue.text("method", "java_native_scf_molecular_properties"));
    values.add(ClassicalResultValue.text("backend", "java_native"));
    values.add(ClassicalResultValue.text("basis_set", preparation.basisSetName()));
    values.add(
        ClassicalResultValue.text(
            "reference", NativeMolecularPropertyExecutor.referenceName(preparation)));
    values.add(
        ClassicalResultValue.text(
            "converged", Boolean.toString(NativeMolecularPropertyExecutor.converged(preparation))));
    values.add(
        ClassicalResultValue.scalar(
            "total_energy", NativeMolecularPropertyExecutor.totalEnergy(preparation), "Hartree"));
    NativeMolecularPropertyExecutor.addFrontierValues(values, preparation);
    NativeMolecularPropertyExecutor.addMullikenValues(values, molecule, atomPopulations);
    this.addAoDipoleIntegralValues(values, preparation.basisSet(), molecule, densityMatrix);
    NativeMolecularPropertyExecutor.addDensityMomentDipoleValues(
        values, preparation.basisSet(), molecule, densityMatrix, overlap);
    NativeMolecularPropertyExecutor.addDipoleValues(values, molecule, atomPopulations);
    return ClassicalResultSection.of(
        ClassicalCalculationKind.MOLECULAR_PROPERTY_ANALYSIS,
        "Java-native SCF molecular properties",
        values);
  }

  private void addAoDipoleIntegralValues(
      final List<ClassicalResultValue> values,
      final GaussianBasisSet basisSet,
      final Molecule molecule,
      final double[] densityMatrix) {
    final IntegralMolecule integralMolecule = IntegralMoleculeFactory.fromMolecule(molecule);
    final DipoleIntegralMatrices dipoleIntegrals =
        this.dipoleIntegralEngine.evaluate(integralMolecule, basisSet);
    final double[] nuclearMoment = new double[3];
    for (int i = 0; i < integralMolecule.atoms().size(); ++i) {
      nuclearMoment[0] +=
          integralMolecule.atoms().get(i).atomicNumber() * integralMolecule.atoms().get(i).xBohr();
      nuclearMoment[1] +=
          integralMolecule.atoms().get(i).atomicNumber() * integralMolecule.atoms().get(i).yBohr();
      nuclearMoment[2] +=
          integralMolecule.atoms().get(i).atomicNumber() * integralMolecule.atoms().get(i).zBohr();
    }
    final double[] electronicMoment =
        NativeMolecularPropertyExecutor.electronicDipoleMoment(densityMatrix, dipoleIntegrals);
    final double dipoleX = (nuclearMoment[0] - electronicMoment[0]) * ELECTRON_BOHR_TO_DEBYE;
    final double dipoleY = (nuclearMoment[1] - electronicMoment[1]) * ELECTRON_BOHR_TO_DEBYE;
    final double dipoleZ = (nuclearMoment[2] - electronicMoment[2]) * ELECTRON_BOHR_TO_DEBYE;
    values.add(ClassicalResultValue.text("ab_initio_dipole_model", "ao_dipole_integral"));
    values.add(ClassicalResultValue.scalar("ab_initio_dipole_x", dipoleX, "Debye"));
    values.add(ClassicalResultValue.scalar("ab_initio_dipole_y", dipoleY, "Debye"));
    values.add(ClassicalResultValue.scalar("ab_initio_dipole_z", dipoleZ, "Debye"));
    values.add(
        ClassicalResultValue.scalar(
            "ab_initio_dipole_norm",
            Math.sqrt(dipoleX * dipoleX + dipoleY * dipoleY + dipoleZ * dipoleZ),
            "Debye"));
    values.add(
        ClassicalResultValue.scalar(
            "ao_electronic_dipole_x_au", electronicMoment[0], "electron_bohr"));
    values.add(
        ClassicalResultValue.scalar(
            "ao_electronic_dipole_y_au", electronicMoment[1], "electron_bohr"));
    values.add(
        ClassicalResultValue.scalar(
            "ao_electronic_dipole_z_au", electronicMoment[2], "electron_bohr"));
  }

  private static void addFrontierValues(
      final List<ClassicalResultValue> values,
      final ElectronicStructurePreparationResult preparation) {
    final FrontierOrbitalWindow frontier =
        preparation.hasRhfReference()
            ? NativeMolecularPropertyExecutor.rhfFrontier(
                preparation.rhfResult(), preparation.electronCount())
            : preparation.hasRohfReference()
                ? NativeMolecularPropertyExecutor.rohfFrontier(preparation)
                : preparation.hasKohnShamDftReference()
                    ? NativeMolecularPropertyExecutor.kohnShamDftFrontier(preparation)
                    : NativeMolecularPropertyExecutor.uhfFrontier(preparation.uhfResult());
    values.add(ClassicalResultValue.scalar("homo_energy", frontier.homoEnergy(), "Hartree"));
    values.add(ClassicalResultValue.scalar("lumo_energy", frontier.lumoEnergy(), "Hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "homo_lumo_gap", frontier.lumoEnergy() - frontier.homoEnergy(), "Hartree"));
    values.add(ClassicalResultValue.text("vertical_energy_model", "koopmans_frontier_estimate"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_ionization_potential", -frontier.homoEnergy(), "Hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_ionization_potential_ev", -frontier.homoEnergy() * HARTREE_TO_EV, "eV"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_electron_affinity", -frontier.lumoEnergy(), "Hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_electron_affinity_ev", -frontier.lumoEnergy() * HARTREE_TO_EV, "eV"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_cation_energy_estimate",
            NativeMolecularPropertyExecutor.totalEnergy(preparation) - frontier.homoEnergy(),
            "Hartree"));
    values.add(
        ClassicalResultValue.scalar(
            "vertical_anion_energy_estimate",
            NativeMolecularPropertyExecutor.totalEnergy(preparation) + frontier.lumoEnergy(),
            "Hartree"));
  }

  private static void addMullikenValues(
      final List<ClassicalResultValue> values,
      final Molecule molecule,
      final double[] atomPopulations) {
    final List<Atom> atoms = molecule.atoms();
    final ArrayList<String> chargeRows = new ArrayList<String>(atoms.size());
    double totalPopulation = 0.0;
    double totalCharge = 0.0;
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final double population = atomPopulations[i];
      final double charge = (double) atom.element().atomicNumber() - population;
      totalPopulation += population;
      totalCharge += charge;
      chargeRows.add(
          atom.id().value()
              + ","
              + atom.element().symbol().value()
              + ","
              + population
              + ","
              + charge);
    }
    values.add(
        ClassicalResultValue.scalar("mulliken_total_population", totalPopulation, "electron"));
    values.add(
        ClassicalResultValue.scalar("mulliken_total_charge", totalCharge, "electron_charge"));
    values.add(
        ClassicalResultValue.of(
            "mulliken_charge_rows",
            ClassicalResultValueKind.TABLE,
            "atom_id,symbol,population,charge",
            chargeRows));
  }

  private static void addDensityMomentDipoleValues(
      final List<ClassicalResultValue> values,
      final GaussianBasisSet basisSet,
      final Molecule molecule,
      final double[] densityMatrix,
      final double[] overlap) {
    final double[] nuclearMoment = new double[3];
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final Coordinate3D coordinate = atom.coordinate();
      final double charge = (double) atom.element().atomicNumber();
      nuclearMoment[0] += charge * coordinate.unit().toAngstrom(coordinate.x());
      nuclearMoment[1] += charge * coordinate.unit().toAngstrom(coordinate.y());
      nuclearMoment[2] += charge * coordinate.unit().toAngstrom(coordinate.z());
    }
    final double[] electronicMoment =
        NativeMolecularPropertyExecutor.densityOverlapCenterMoment(
            basisSet, molecule, densityMatrix, overlap);
    final double dipoleX = (nuclearMoment[0] - electronicMoment[0]) * ELECTRON_ANGSTROM_TO_DEBYE;
    final double dipoleY = (nuclearMoment[1] - electronicMoment[1]) * ELECTRON_ANGSTROM_TO_DEBYE;
    final double dipoleZ = (nuclearMoment[2] - electronicMoment[2]) * ELECTRON_ANGSTROM_TO_DEBYE;
    values.add(
        ClassicalResultValue.text(
            "density_moment_dipole_model", "ao_density_overlap_center_moment"));
    values.add(ClassicalResultValue.scalar("density_moment_dipole_x", dipoleX, "Debye"));
    values.add(ClassicalResultValue.scalar("density_moment_dipole_y", dipoleY, "Debye"));
    values.add(ClassicalResultValue.scalar("density_moment_dipole_z", dipoleZ, "Debye"));
    values.add(
        ClassicalResultValue.scalar(
            "density_moment_dipole_norm",
            Math.sqrt(dipoleX * dipoleX + dipoleY * dipoleY + dipoleZ * dipoleZ),
            "Debye"));
  }

  private static void addDipoleValues(
      final List<ClassicalResultValue> values,
      final Molecule molecule,
      final double[] atomPopulations) {
    final List<Atom> atoms = molecule.atoms();
    double dipoleX = 0.0;
    double dipoleY = 0.0;
    double dipoleZ = 0.0;
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final double charge = (double) atom.element().atomicNumber() - atomPopulations[i];
      final Coordinate3D coordinate = atom.coordinate();
      dipoleX += charge * coordinate.unit().toAngstrom(coordinate.x()) * ELECTRON_ANGSTROM_TO_DEBYE;
      dipoleY += charge * coordinate.unit().toAngstrom(coordinate.y()) * ELECTRON_ANGSTROM_TO_DEBYE;
      dipoleZ += charge * coordinate.unit().toAngstrom(coordinate.z()) * ELECTRON_ANGSTROM_TO_DEBYE;
    }
    values.add(ClassicalResultValue.text("dipole_model", "mulliken_point_charge_estimate"));
    values.add(ClassicalResultValue.scalar("dipole_x", dipoleX, "Debye"));
    values.add(ClassicalResultValue.scalar("dipole_y", dipoleY, "Debye"));
    values.add(ClassicalResultValue.scalar("dipole_z", dipoleZ, "Debye"));
    values.add(
        ClassicalResultValue.scalar(
            "dipole_norm",
            Math.sqrt(dipoleX * dipoleX + dipoleY * dipoleY + dipoleZ * dipoleZ),
            "Debye"));
  }

  private static double[] densityOverlapCenterMoment(
      final GaussianBasisSet basisSet,
      final Molecule molecule,
      final double[] densityMatrix,
      final double[] overlap) {
    final int orbitalCount = basisSet.orbitalCount();
    final double[] moment = new double[3];
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    final List<Atom> atoms = molecule.atoms();
    for (int row = 0; row < orbitalCount; ++row) {
      final Coordinate3D rowCoordinate = atoms.get(orbitals.get(row).atomIndex()).coordinate();
      final double rowX = rowCoordinate.unit().toAngstrom(rowCoordinate.x());
      final double rowY = rowCoordinate.unit().toAngstrom(rowCoordinate.y());
      final double rowZ = rowCoordinate.unit().toAngstrom(rowCoordinate.z());
      final int rowOffset = row * orbitalCount;
      for (int column = 0; column < orbitalCount; ++column) {
        final Coordinate3D columnCoordinate =
            atoms.get(orbitals.get(column).atomIndex()).coordinate();
        final double populationWeight =
            densityMatrix[rowOffset + column] * overlap[column * orbitalCount + row];
        moment[0] +=
            populationWeight
                * 0.5
                * (rowX + columnCoordinate.unit().toAngstrom(columnCoordinate.x()));
        moment[1] +=
            populationWeight
                * 0.5
                * (rowY + columnCoordinate.unit().toAngstrom(columnCoordinate.y()));
        moment[2] +=
            populationWeight
                * 0.5
                * (rowZ + columnCoordinate.unit().toAngstrom(columnCoordinate.z()));
      }
    }
    return moment;
  }

  private static double[] electronicDipoleMoment(
      final double[] densityMatrix, final DipoleIntegralMatrices dipoleIntegrals) {
    final int orbitalCount = dipoleIntegrals.orbitalCount();
    if (densityMatrix == null
        || densityMatrix.length != Math.multiplyExact(orbitalCount, orbitalCount)) {
      throw new IllegalArgumentException("Density matrix shape must match dipole integrals.");
    }
    final double[] moment = new double[3];
    for (int row = 0; row < orbitalCount; ++row) {
      final int rowOffset = row * orbitalCount;
      for (int column = 0; column < orbitalCount; ++column) {
        final double density = densityMatrix[rowOffset + column];
        moment[0] += density * dipoleIntegrals.valueAt(0, row, column);
        moment[1] += density * dipoleIntegrals.valueAt(1, row, column);
        moment[2] += density * dipoleIntegrals.valueAt(2, row, column);
      }
    }
    return moment;
  }

  private static double[] mullikenPopulations(
      final GaussianBasisSet basisSet,
      final Molecule molecule,
      final double[] densityMatrix,
      final double[] overlap) {
    final int atomCount = molecule.atomCount();
    final int orbitalCount = basisSet.orbitalCount();
    final List<ContractedGaussianOrbital> orbitals = basisSet.orbitals();
    final double[] populations = new double[atomCount];
    for (int row = 0; row < orbitalCount; ++row) {
      final int atomIndex = orbitals.get(row).atomIndex();
      if (atomIndex >= atomCount) {
        throw new IllegalStateException("Basis orbital atom index is outside molecule atom range.");
      }
      double orbitalPopulation = 0.0;
      final int rowOffset = row * orbitalCount;
      for (int column = 0; column < orbitalCount; ++column) {
        orbitalPopulation +=
            densityMatrix[rowOffset + column] * overlap[column * orbitalCount + row];
      }
      populations[atomIndex] += orbitalPopulation;
    }
    return populations;
  }

  private static double[] totalDensityMatrix(
      final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasRhfReference()) {
      return preparation.rhfResult().densityMatrixCopy();
    }
    if (preparation.hasRohfReference()) {
      final double[] alpha = preparation.rohfResult().alphaDensityMatrixCopy();
      final double[] beta = preparation.rohfResult().betaDensityMatrixCopy();
      final double[] total = new double[alpha.length];
      for (int i = 0; i < total.length; ++i) {
        total[i] = alpha[i] + beta[i];
      }
      return total;
    }
    if (preparation.hasKohnShamDftReference()) {
      return preparation.kohnShamDftResult().densityMatrixCopy();
    }
    final UhfResult uhfResult = preparation.uhfResult();
    final double[] alpha = uhfResult.alphaDensityMatrixCopy();
    final double[] beta = uhfResult.betaDensityMatrixCopy();
    final double[] total = new double[alpha.length];
    for (int i = 0; i < total.length; ++i) {
      total[i] = alpha[i] + beta[i];
    }
    return total;
  }

  private static double[] overlapMatrix(final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasAtomicOrbitalTensors()) {
      return preparation.atomicOrbitalTensors().overlapCopy();
    }
    if (preparation.hasOneElectronMatrices()) {
      return preparation.oneElectronMatrices().overlapCopy();
    }
    throw new IllegalStateException(
        "Native molecular property analysis requires AO overlap matrix.");
  }

  private static FrontierOrbitalWindow rhfFrontier(
      final RhfResult rhfResult, final int electronCount) {
    final double[] energies = rhfResult.orbitalEnergiesCopy();
    final int occupiedCount = Math.max(1, Math.floorDiv(electronCount, 2));
    final int homoIndex = Math.min(occupiedCount - 1, energies.length - 1);
    final int lumoIndex = Math.min(homoIndex + 1, energies.length - 1);
    return new FrontierOrbitalWindow(energies[homoIndex], energies[lumoIndex]);
  }

  private static FrontierOrbitalWindow uhfFrontier(final UhfResult uhfResult) {
    final double[] alphaEnergies = uhfResult.alphaOrbitalEnergiesCopy();
    final double[] betaEnergies = uhfResult.betaOrbitalEnergiesCopy();
    final double alphaHomo =
        NativeMolecularPropertyExecutor.occupiedEnergy(
            alphaEnergies, uhfResult.alphaElectronCount());
    final double betaHomo =
        NativeMolecularPropertyExecutor.occupiedEnergy(betaEnergies, uhfResult.betaElectronCount());
    final double alphaLumo =
        NativeMolecularPropertyExecutor.virtualEnergy(
            alphaEnergies, uhfResult.alphaElectronCount());
    final double betaLumo =
        NativeMolecularPropertyExecutor.virtualEnergy(betaEnergies, uhfResult.betaElectronCount());
    return new FrontierOrbitalWindow(Math.max(alphaHomo, betaHomo), Math.min(alphaLumo, betaLumo));
  }

  private static FrontierOrbitalWindow rohfFrontier(
      final ElectronicStructurePreparationResult preparation) {
    final double[] energies = preparation.rohfResult().orbitalEnergiesCopy();
    final int homoIndex =
        Math.max(
            0, Math.min(preparation.rohfResult().alphaElectronCount() - 1, energies.length - 1));
    final int lumoIndex = Math.min(homoIndex + 1, energies.length - 1);
    return new FrontierOrbitalWindow(energies[homoIndex], energies[lumoIndex]);
  }

  private static FrontierOrbitalWindow kohnShamDftFrontier(
      final ElectronicStructurePreparationResult preparation) {
    final double[] energies = preparation.kohnShamDftResult().orbitalEnergiesCopy();
    final int occupiedCount = Math.max(1, Math.floorDiv(preparation.electronCount(), 2));
    final int homoIndex = Math.min(occupiedCount - 1, energies.length - 1);
    final int lumoIndex = Math.min(homoIndex + 1, energies.length - 1);
    return new FrontierOrbitalWindow(energies[homoIndex], energies[lumoIndex]);
  }

  private static double occupiedEnergy(final double[] energies, final int electronCount) {
    final int index = Math.max(0, Math.min(electronCount - 1, energies.length - 1));
    return energies[index];
  }

  private static double virtualEnergy(final double[] energies, final int electronCount) {
    final int index = Math.max(0, Math.min(electronCount, energies.length - 1));
    return energies[index];
  }

  private static boolean converged(final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasRhfReference()) {
      return preparation.rhfResult().converged();
    }
    if (preparation.hasRohfReference()) {
      return preparation.rohfResult().converged();
    }
    if (preparation.hasKohnShamDftReference()) {
      return preparation.kohnShamDftResult().converged();
    }
    return preparation.uhfResult().converged();
  }

  private static double totalEnergy(final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasRhfReference()) {
      return preparation.rhfResult().totalEnergy();
    }
    if (preparation.hasRohfReference()) {
      return preparation.rohfResult().totalEnergy();
    }
    if (preparation.hasKohnShamDftReference()) {
      return preparation.kohnShamDftResult().totalEnergy();
    }
    return preparation.uhfResult().totalEnergy();
  }

  private static String referenceName(final ElectronicStructurePreparationResult preparation) {
    if (preparation.hasRhfReference()) {
      return "RHF";
    }
    if (preparation.hasRohfReference()) {
      return "ROHF";
    }
    if (preparation.hasKohnShamDftReference()) {
      return "KOHN_SHAM_DFT";
    }
    return "UHF";
  }

  private static final class FrontierOrbitalWindow {

    private final double homoEnergy;
    private final double lumoEnergy;

    private FrontierOrbitalWindow(final double homoEnergy, final double lumoEnergy) {
      this.homoEnergy = homoEnergy;
      this.lumoEnergy = lumoEnergy;
    }

    private double homoEnergy() {
      return this.homoEnergy;
    }

    private double lumoEnergy() {
      return this.lumoEnergy;
    }
  }
}
