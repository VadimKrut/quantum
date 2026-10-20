/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.spectroscopy;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.energy.SinglePointEnergyExecutor;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.BondedForceFieldEnergyModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.ElectronicStructureFiniteDifferenceEnergyModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryCoordinateSupport;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryEnergyModel;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryEnergyModelKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.geometry.GeometryOptimizationOptions;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOptionKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.scf.application.JacobiSymmetricEigenSolver;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.DenseSquareMatrix;
import ru.pathcreator.vadim.quantum.chemistry.scf.domain.matrix.SymmetricEigenDecomposition;

/**
 * Строит приближенную harmonic frequency и thermodynamics сводку по связям
 * молекулы без внешнего quantum-chemistry backend.
 */
public final class HarmonicFrequencyThermodynamicsExecutor {

  private static final String TEMPERATURE_KELVIN_OPTION = "thermodynamics_temperature_kelvin";
  private static final String HESSIAN_BACKEND_OPTION = "hessian_backend";
  private static final String FREQUENCY_BACKEND_OPTION = "frequency_backend";
  private static final String VIBRATIONAL_BACKEND_OPTION = "vibrational_backend";
  private static final String PYSCF_BACKEND = "pyscf";
  private static final double SPEED_OF_LIGHT_CM_PER_SECOND = 2.99792458E10;
  private static final double ATOMIC_MASS_UNIT_KG = 1.66053906660E-27;
  private static final double NEWTON_PER_METER_PER_MDYNE_PER_ANGSTROM = 100.0;
  private static final double WAVENUMBER_TO_KJ_PER_MOLE = 0.0119626565638697;
  private static final double HARTREE_PER_ANGSTROM_SQUARED_TO_NEWTON_PER_METER = 43.596246321;
  private static final double ROOM_TEMPERATURE_KELVIN = 298.15;
  private static final double GAS_CONSTANT_KJ_PER_MOLE_KELVIN = 0.00831446261815324;

  private final PyScfHessianFrequencyExecutor pyScfHessianFrequencyExecutor;
  private final SinglePointEnergyExecutor singlePointEnergyExecutor;
  private final BondedForceFieldEnergyModel forceFieldEnergyModel;
  private final JacobiSymmetricEigenSolver eigenSolver;

  public HarmonicFrequencyThermodynamicsExecutor() {
    this(
        new PyScfHessianFrequencyExecutor(),
        new SinglePointEnergyExecutor(),
        new BondedForceFieldEnergyModel(),
        new JacobiSymmetricEigenSolver());
  }

  public HarmonicFrequencyThermodynamicsExecutor(
      final SinglePointEnergyExecutor singlePointEnergyExecutor) {
    this(
        new PyScfHessianFrequencyExecutor(),
        singlePointEnergyExecutor,
        new BondedForceFieldEnergyModel(),
        new JacobiSymmetricEigenSolver());
  }

  public HarmonicFrequencyThermodynamicsExecutor(
      final PyScfHessianFrequencyExecutor pyScfHessianFrequencyExecutor,
      final SinglePointEnergyExecutor singlePointEnergyExecutor,
      final BondedForceFieldEnergyModel forceFieldEnergyModel,
      final JacobiSymmetricEigenSolver eigenSolver) {
    if (pyScfHessianFrequencyExecutor == null) {
      throw new IllegalArgumentException("PySCF Hessian/frequency executor must not be null.");
    }
    if (singlePointEnergyExecutor == null) {
      throw new IllegalArgumentException("Frequency single-point executor must not be null.");
    }
    if (forceFieldEnergyModel == null) {
      throw new IllegalArgumentException("Frequency force-field model must not be null.");
    }
    if (eigenSolver == null) {
      throw new IllegalArgumentException("Frequency eigen solver must not be null.");
    }
    this.pyScfHessianFrequencyExecutor = pyScfHessianFrequencyExecutor;
    this.singlePointEnergyExecutor = singlePointEnergyExecutor;
    this.forceFieldEnergyModel = forceFieldEnergyModel;
    this.eigenSolver = eigenSolver;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Frequency request must not be null.");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Frequency calculation kind must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Frequency diagnostics target must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Frequency and molecular thermodynamics require molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    final String coordinateError =
        HarmonicFrequencyThermodynamicsExecutor.coordinateError(molecule, kind);
    if (coordinateError != null) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.GEOMETRY_REQUIRED,
              ClassicalDiagnosticTarget.of("molecule", molecule.id().value()),
              coordinateError));
      return null;
    }
    if (HarmonicFrequencyThermodynamicsExecutor.pyScfBackendRequested(request)) {
      return this.pyScfHessianFrequencyExecutor.execute(request, molecule, kind, diagnostics);
    }
    final GeometryOptimizationOptions options =
        GeometryOptimizationOptions.fromOptions(request.options());
    final GeometryEnergyModel energyModel = this.energyModel(request, options);
    if (kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
        || !"bonded_force_field".equals(energyModel.name())) {
      return this.finiteDifferenceHessianSection(
          request, molecule, kind, energyModel, options, diagnostics);
    }
    final double[] frequencies = this.bondFrequenciesWavenumber(molecule);
    final double zeroPointEnergy = this.zeroPointEnergyKilojoulePerMole(frequencies);
    final double temperatureKelvin =
        HarmonicFrequencyThermodynamicsExecutor.temperatureKelvin(request.options());
    final double thermalCorrection =
        HarmonicFrequencyThermodynamicsExecutor.thermalCorrectionKilojoulePerMole(
            molecule, temperatureKelvin);
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "Local frequency/thermodynamics uses bonded harmonic force-field approximation, not ab initio Hessian."));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "bonded_harmonic_force_field",
        "frequency_thermodynamic_analysis",
        "Bonded harmonic force-field analysis is a local approximation, not an ab initio Hessian.");
    values.add(ClassicalResultValue.text("method", "bonded_harmonic_force_field"));
    values.add(ClassicalResultValue.scalar("mode_count", frequencies.length, "count"));
    values.add(ClassicalResultValue.scalar("temperature", temperatureKelvin, "K"));
    values.add(ClassicalResultValue.scalar("zero_point_energy", zeroPointEnergy, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar("thermal_correction_enthalpy", thermalCorrection, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar(
            "estimated_enthalpy_correction", zeroPointEnergy + thermalCorrection, "kJ/mol"));
    values.add(
        ClassicalResultValue.of(
            "frequencies",
            ClassicalResultValueKind.VECTOR,
            "cm^-1",
            HarmonicFrequencyThermodynamicsExecutor.frequencyValues(frequencies)));
    return ClassicalResultSection.of(
        kind,
        kind == ClassicalCalculationKind.FREQUENCY_ANALYSIS
            ? "Bonded harmonic frequency analysis"
            : "Bonded harmonic thermodynamics",
        values);
  }

  private ClassicalResultSection finiteDifferenceHessianSection(
      final ClassicalCalculationRequest request,
      final Molecule molecule,
      final ClassicalCalculationKind kind,
      final GeometryEnergyModel energyModel,
      final GeometryOptimizationOptions options,
      final List<ClassicalDiagnostic> diagnostics) {
    final double[] coordinates = GeometryCoordinateSupport.coordinatesAngstrom(molecule);
    final double[] hessian =
        this.finiteDifferenceHessian(
            molecule, coordinates, energyModel, options.finiteDifferenceStepAngstrom());
    final double[] frequencies = this.hessianFrequenciesWavenumber(molecule, hessian, energyModel);
    final ModeClassification modeClassification =
        HarmonicFrequencyThermodynamicsExecutor.modeClassification(frequencies);
    final double zeroPointEnergy = this.zeroPointEnergyKilojoulePerMole(frequencies);
    final double temperatureKelvin =
        HarmonicFrequencyThermodynamicsExecutor.temperatureKelvin(request.options());
    final double thermalCorrection =
        HarmonicFrequencyThermodynamicsExecutor.thermalCorrectionKilojoulePerMole(
            molecule, temperatureKelvin);
    if ("bonded_force_field".equals(energyModel.name())) {
      diagnostics.add(
          ClassicalDiagnostic.warning(
              ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
              ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
              "Hessian/frequency analysis uses bonded force-field second derivatives."));
    }
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    HarmonicFrequencyThermodynamicsExecutor.addFiniteDifferenceQualityValues(values, energyModel);
    values.add(
        ClassicalResultValue.text("method", energyModel.name() + "_finite_difference_hessian"));
    values.add(
        ClassicalResultValue.text("model", energyModel.name() + "_finite_difference_hessian"));
    values.add(ClassicalResultValue.text("hessian_model", energyModel.name()));
    values.add(
        ClassicalResultValue.text(
            "stationary_point_kind", modeClassification.stationaryPointKind()));
    values.add(ClassicalResultValue.scalar("coordinate_count", coordinates.length, "count"));
    values.add(ClassicalResultValue.scalar("vibrational_mode_count", frequencies.length, "count"));
    values.add(
        ClassicalResultValue.scalar(
            "real_mode_count", modeClassification.realModeCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "imaginary_mode_count", modeClassification.imaginaryModeCount(), "count"));
    values.add(
        ClassicalResultValue.scalar(
            "projected_translation_rotation_count",
            Math.max(0, coordinates.length - frequencies.length),
            "count"));
    values.add(
        ClassicalResultValue.scalar(
            "finite_difference_step", options.finiteDifferenceStepAngstrom(), "angstrom"));
    values.add(
        ClassicalResultValue.scalar(
            "energy", energyModel.energy(molecule, coordinates), energyModel.energyUnit()));
    values.add(ClassicalResultValue.scalar("mode_count", frequencies.length, "count"));
    values.add(ClassicalResultValue.scalar("temperature", temperatureKelvin, "K"));
    values.add(ClassicalResultValue.scalar("zero_point_energy", zeroPointEnergy, "kJ/mol"));
    values.add(
        ClassicalResultValue.scalar("thermal_correction_enthalpy", thermalCorrection, "kJ/mol"));
    values.add(
        ClassicalResultValue.of(
            "hessian_rows",
            ClassicalResultValueKind.TABLE,
            energyModel.energyUnit() + "/angstrom^2",
            HarmonicFrequencyThermodynamicsExecutor.hessianRows(hessian, coordinates.length)));
    values.add(
        ClassicalResultValue.of(
            "frequencies",
            ClassicalResultValueKind.VECTOR,
            "cm^-1",
            HarmonicFrequencyThermodynamicsExecutor.frequencyValues(frequencies)));
    values.add(
        ClassicalResultValue.of(
            "frequency_modes",
            ClassicalResultValueKind.TABLE,
            "cm^-1",
            HarmonicFrequencyThermodynamicsExecutor.frequencyModeRows(frequencies)));
    return ClassicalResultSection.of(
        kind,
        kind == ClassicalCalculationKind.HESSIAN_ANALYSIS
            ? "Java-native finite-difference Hessian"
            : "Java-native finite-difference harmonic frequencies",
        values);
  }

  private static void addFiniteDifferenceQualityValues(
      final List<ClassicalResultValue> values, final GeometryEnergyModel energyModel) {
    if ("bonded_force_field".equals(energyModel.name())) {
      ClassicalResultQualityValues.addApproximate(
          values,
          "java_native",
          "bonded_force_field_finite_difference_hessian",
          "hessian_frequency_analysis",
          "Finite-difference Hessian is based on a bonded force-field potential.");
      return;
    }
    ClassicalResultQualityValues.addExact(
        values,
        "java_native",
        "electronic_structure_finite_difference_hessian",
        "hessian_frequency_analysis");
  }

  private GeometryEnergyModel energyModel(
      final ClassicalCalculationRequest request, final GeometryOptimizationOptions options) {
    final Molecule molecule = (Molecule) request.subject();
    if (options.energyModelKind() == GeometryEnergyModelKind.ELECTRONIC_STRUCTURE_FINITE_DIFFERENCE
        || (options.energyModelKind() == GeometryEnergyModelKind.AUTO
            && molecule.atomCount() <= options.autoElectronicMaxAtoms())) {
      return new ElectronicStructureFiniteDifferenceEnergyModel(
          request, this.singlePointEnergyExecutor);
    }
    return this.forceFieldEnergyModel;
  }

  private double[] finiteDifferenceHessian(
      final Molecule molecule,
      final double[] coordinates,
      final GeometryEnergyModel energyModel,
      final double stepAngstrom) {
    final int dimension = coordinates.length;
    final double[] hessian = new double[Math.multiplyExact(dimension, dimension)];
    final double[] shifted = coordinates.clone();
    final double referenceEnergy = energyModel.energy(molecule, coordinates);
    for (int row = 0; row < dimension; ++row) {
      final double original = shifted[row];
      shifted[row] = original + stepAngstrom;
      final double plusEnergy = energyModel.energy(molecule, shifted);
      shifted[row] = original - stepAngstrom;
      final double minusEnergy = energyModel.energy(molecule, shifted);
      shifted[row] = original;
      hessian[row * dimension + row] =
          (plusEnergy - 2.0 * referenceEnergy + minusEnergy) / (stepAngstrom * stepAngstrom);
      for (int column = row + 1; column < dimension; ++column) {
        final double secondOriginal = shifted[column];
        shifted[row] = original + stepAngstrom;
        shifted[column] = secondOriginal + stepAngstrom;
        final double plusPlus = energyModel.energy(molecule, shifted);
        shifted[column] = secondOriginal - stepAngstrom;
        final double plusMinus = energyModel.energy(molecule, shifted);
        shifted[row] = original - stepAngstrom;
        final double minusMinus = energyModel.energy(molecule, shifted);
        shifted[column] = secondOriginal + stepAngstrom;
        final double minusPlus = energyModel.energy(molecule, shifted);
        shifted[row] = original;
        shifted[column] = secondOriginal;
        final double value =
            (plusPlus - plusMinus - minusPlus + minusMinus) / (4.0 * stepAngstrom * stepAngstrom);
        hessian[row * dimension + column] = value;
        hessian[column * dimension + row] = value;
      }
    }
    return hessian;
  }

  private double[] hessianFrequenciesWavenumber(
      final Molecule molecule, final double[] hessian, final GeometryEnergyModel energyModel) {
    final int dimension = molecule.atomCount() * 3;
    final double[] massWeighted = new double[hessian.length];
    final List<Atom> atoms = molecule.atoms();
    final double unitScale =
        "hartree".equals(energyModel.energyUnit())
            ? HARTREE_PER_ANGSTROM_SQUARED_TO_NEWTON_PER_METER
            : 1.0;
    for (int row = 0; row < dimension; ++row) {
      final double rowMassKg = atoms.get(row / 3).element().atomicMass() * ATOMIC_MASS_UNIT_KG;
      for (int column = 0; column < dimension; ++column) {
        final double columnMassKg =
            atoms.get(column / 3).element().atomicMass() * ATOMIC_MASS_UNIT_KG;
        massWeighted[row * dimension + column] =
            hessian[row * dimension + column] * unitScale / Math.sqrt(rowMassKg * columnMassKg);
      }
    }
    final SymmetricEigenDecomposition decomposition =
        this.eigenSolver.solve(DenseSquareMatrix.of(dimension, massWeighted));
    final int vibrationalModeCount = Math.max(1, dimension - (molecule.atomCount() > 2 ? 6 : 5));
    final double[] frequencies = new double[Math.min(vibrationalModeCount, dimension)];
    int cursor = 0;
    for (int i = 0; i < dimension && cursor < frequencies.length; ++i) {
      final double eigenvalue = decomposition.eigenvalue(i);
      if (eigenvalue >= -1.0E-6) {
        continue;
      }
      frequencies[cursor] =
          -HarmonicFrequencyThermodynamicsExecutor.wavenumberFromCurvature(eigenvalue);
      ++cursor;
    }
    for (int i = dimension - 1; i >= 0 && cursor < frequencies.length; --i) {
      final double eigenvalue = decomposition.eigenvalue(i);
      if (eigenvalue <= 1.0E-6) {
        continue;
      }
      frequencies[cursor] =
          HarmonicFrequencyThermodynamicsExecutor.wavenumberFromCurvature(eigenvalue);
      ++cursor;
    }
    return frequencies;
  }

  private static double wavenumberFromCurvature(final double eigenvalue) {
    return Math.sqrt(Math.abs(eigenvalue)) / (2.0 * Math.PI * SPEED_OF_LIGHT_CM_PER_SECOND);
  }

  private double[] bondFrequenciesWavenumber(final Molecule molecule) {
    final List<Bond> bonds = molecule.bonds();
    final double[] frequencies = new double[bonds.size()];
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Atom first =
          HarmonicFrequencyThermodynamicsExecutor.atomById(molecule, bond.firstAtomId());
      final Atom second =
          HarmonicFrequencyThermodynamicsExecutor.atomById(molecule, bond.secondAtomId());
      final double reducedMassKg =
          HarmonicFrequencyThermodynamicsExecutor.reducedMassAtomicUnit(first, second)
              * ATOMIC_MASS_UNIT_KG;
      final double forceConstant =
          HarmonicFrequencyThermodynamicsExecutor.forceConstantMillidynePerAngstrom(bond.type())
              * NEWTON_PER_METER_PER_MDYNE_PER_ANGSTROM;
      frequencies[i] =
          (1.0 / (2.0 * Math.PI * SPEED_OF_LIGHT_CM_PER_SECOND))
              * Math.sqrt(forceConstant / reducedMassKg);
    }
    return frequencies;
  }

  private double zeroPointEnergyKilojoulePerMole(final double[] frequencies) {
    double result = 0.0;
    for (int i = 0; i < frequencies.length; ++i) {
      result += 0.5 * frequencies[i] * WAVENUMBER_TO_KJ_PER_MOLE;
    }
    return result;
  }

  private static double thermalCorrectionKilojoulePerMole(
      final Molecule molecule, final double temperatureKelvin) {
    return GAS_CONSTANT_KJ_PER_MOLE_KELVIN
        * temperatureKelvin
        * Math.max(1.0, 3.0 * (double) molecule.atomCount() - 6.0);
  }

  private static double temperatureKelvin(final List<ClassicalCalculationOption> options) {
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (!TEMPERATURE_KELVIN_OPTION.equals(option.name())) {
        continue;
      }
      if (option.kind() != ClassicalCalculationOptionKind.DOUBLE) {
        throw new IllegalArgumentException(
            "Thermodynamics temperature option must have DOUBLE kind.");
      }
      final double temperatureKelvin = Double.parseDouble(option.value());
      if (!Double.isFinite(temperatureKelvin) || temperatureKelvin <= 0.0) {
        throw new IllegalArgumentException(
            "Thermodynamics temperature must be finite and positive.");
      }
      return temperatureKelvin;
    }
    return ROOM_TEMPERATURE_KELVIN;
  }

  private static double reducedMassAtomicUnit(final Atom first, final Atom second) {
    final double firstMass = first.element().atomicMass();
    final double secondMass = second.element().atomicMass();
    return firstMass * secondMass / (firstMass + secondMass);
  }

  private static double forceConstantMillidynePerAngstrom(final BondType bondType) {
    if (bondType == BondType.TRIPLE) {
      return 15.6;
    }
    if (bondType == BondType.DOUBLE) {
      return 10.1;
    }
    if (bondType == BondType.AROMATIC) {
      return 8.2;
    }
    return 5.1;
  }

  private static List<String> frequencyValues(final double[] frequencies) {
    final ArrayList<String> values = new ArrayList<String>(frequencies.length);
    for (int i = 0; i < frequencies.length; ++i) {
      values.add(Double.toString(frequencies[i]));
    }
    return values;
  }

  private static List<String> frequencyModeRows(final double[] frequencies) {
    final ArrayList<String> rows = new ArrayList<String>(frequencies.length + 1);
    rows.add("index|kind|signed_wavenumber_cm^-1|absolute_wavenumber_cm^-1");
    for (int i = 0; i < frequencies.length; ++i) {
      final double frequency = frequencies[i];
      rows.add(
          i
              + "|"
              + (frequency < 0.0 ? "imaginary" : "real")
              + "|"
              + frequency
              + "|"
              + Math.abs(frequency));
    }
    return List.copyOf(rows);
  }

  private static ModeClassification modeClassification(final double[] frequencies) {
    int realModeCount = 0;
    int imaginaryModeCount = 0;
    for (int i = 0; i < frequencies.length; ++i) {
      if (frequencies[i] < 0.0) {
        ++imaginaryModeCount;
      } else {
        ++realModeCount;
      }
    }
    String stationaryPointKind = "minimum";
    if (imaginaryModeCount == 1) {
      stationaryPointKind = "first_order_saddle";
    } else if (imaginaryModeCount > 1) {
      stationaryPointKind = "higher_order_saddle";
    }
    return ModeClassification.of(realModeCount, imaginaryModeCount, stationaryPointKind);
  }

  private static List<String> hessianRows(final double[] hessian, final int dimension) {
    final ArrayList<String> rows = new ArrayList<String>(hessian.length);
    for (int row = 0; row < dimension; ++row) {
      for (int column = 0; column < dimension; ++column) {
        rows.add(row + "," + column + "," + hessian[row * dimension + column]);
      }
    }
    return List.copyOf(rows);
  }

  private static Atom atomById(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (atoms.get(i).id().equals(atomId)) {
        return atoms.get(i);
      }
    }
    throw new IllegalArgumentException("Bond references missing atom " + atomId.value() + ".");
  }

  private static String coordinateError(
      final Molecule molecule, final ClassicalCalculationKind kind) {
    if (kind != ClassicalCalculationKind.HESSIAN_ANALYSIS && molecule.bondCount() == 0) {
      return "Frequency analysis requires at least one bond.";
    }
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (!atoms.get(i).hasCoordinate()) {
        return "Frequency analysis requires coordinates for atom "
            + atoms.get(i).id().value()
            + ".";
      }
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      if (!bonds.get(i).type().hasDefinedOrder()) {
        return "Frequency analysis requires defined bond orders; unknown bond found.";
      }
    }
    return null;
  }

  public static boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    return HarmonicFrequencyThermodynamicsExecutor.optionEquals(
            request, HESSIAN_BACKEND_OPTION, PYSCF_BACKEND)
        || HarmonicFrequencyThermodynamicsExecutor.optionEquals(
            request, FREQUENCY_BACKEND_OPTION, PYSCF_BACKEND)
        || HarmonicFrequencyThermodynamicsExecutor.optionEquals(
            request, VIBRATIONAL_BACKEND_OPTION, PYSCF_BACKEND);
  }

  private static boolean optionEquals(
      final ClassicalCalculationRequest request, final String name, final String expectedValue) {
    for (int i = 0; i < request.options().size(); ++i) {
      if (!request.options().get(i).name().equals(name)) {
        continue;
      }
      return request.options().get(i).value().equalsIgnoreCase(expectedValue);
    }
    return false;
  }

  private static final class ModeClassification {

    private final int realModeCount;
    private final int imaginaryModeCount;
    private final String stationaryPointKind;

    private ModeClassification(
        final int realModeCount, final int imaginaryModeCount, final String stationaryPointKind) {
      this.realModeCount = realModeCount;
      this.imaginaryModeCount = imaginaryModeCount;
      this.stationaryPointKind = stationaryPointKind;
    }

    static ModeClassification of(
        final int realModeCount, final int imaginaryModeCount, final String stationaryPointKind) {
      return new ModeClassification(realModeCount, imaginaryModeCount, stationaryPointKind);
    }

    int realModeCount() {
      return this.realModeCount;
    }

    int imaginaryModeCount() {
      return this.imaginaryModeCount;
    }

    String stationaryPointKind() {
      return this.stationaryPointKind;
    }
  }
}
