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
import ru.pathcreator.vadim.quantum.chemistry.classical.application.feature.MolecularFeatureAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.result.ClassicalResultQualityValues;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnostic;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.diagnostic.ClassicalDiagnosticTarget;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultSection;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValue;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.result.ClassicalResultValueKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubjectKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Строит локальный feature-based прогноз IR/NMR/UV признаков для органических молекул. */
public final class MolecularSpectroscopyPredictionExecutor {

  private static final String EXCITED_STATE_BACKEND_OPTION = "excited_state_backend";
  private static final String SPECTROSCOPY_BACKEND_OPTION = "spectroscopy_backend";
  private static final String PYSCF_BACKEND = "pyscf";
  private static final String NATIVE_BACKEND = "native";

  private final MolecularFeatureAnalyzer featureAnalyzer;
  private final PyScfExcitedStateExecutor pyScfExcitedStateExecutor;
  private final NativeExcitedStateExecutor nativeExcitedStateExecutor;

  public MolecularSpectroscopyPredictionExecutor() {
    this(
        new MolecularFeatureAnalyzer(),
        new PyScfExcitedStateExecutor(),
        new NativeExcitedStateExecutor());
  }

  public MolecularSpectroscopyPredictionExecutor(final MolecularFeatureAnalyzer featureAnalyzer) {
    this(featureAnalyzer, new PyScfExcitedStateExecutor(), new NativeExcitedStateExecutor());
  }

  public MolecularSpectroscopyPredictionExecutor(
      final MolecularFeatureAnalyzer featureAnalyzer,
      final PyScfExcitedStateExecutor pyScfExcitedStateExecutor,
      final NativeExcitedStateExecutor nativeExcitedStateExecutor) {
    if (featureAnalyzer == null) {
      throw new IllegalArgumentException("Spectroscopy feature analyzer must not be null.");
    }
    if (pyScfExcitedStateExecutor == null) {
      throw new IllegalArgumentException("PySCF excited-state executor must not be null.");
    }
    if (nativeExcitedStateExecutor == null) {
      throw new IllegalArgumentException("Native excited-state executor must not be null.");
    }
    this.featureAnalyzer = featureAnalyzer;
    this.pyScfExcitedStateExecutor = pyScfExcitedStateExecutor;
    this.nativeExcitedStateExecutor = nativeExcitedStateExecutor;
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request, final List<ClassicalDiagnostic> diagnostics) {
    return this.execute(request, ClassicalCalculationKind.SPECTROSCOPY_PREDICTION, diagnostics);
  }

  public ClassicalResultSection execute(
      final ClassicalCalculationRequest request,
      final ClassicalCalculationKind kind,
      final List<ClassicalDiagnostic> diagnostics) {
    if (request == null) {
      throw new IllegalArgumentException("Spectroscopy request must not be null.");
    }
    if (diagnostics == null) {
      throw new IllegalArgumentException("Spectroscopy diagnostics must not be null.");
    }
    if (request.subject().subjectKind() != ChemistrySubjectKind.MOLECULE) {
      diagnostics.add(
          ClassicalDiagnostic.error(
              ClassicalDiagnosticCode.SUBJECT_KIND_UNSUPPORTED,
              ClassicalDiagnosticTarget.of("subject", request.subject().stableId()),
              "Spectroscopy prediction requires molecule subject."));
      return null;
    }
    final Molecule molecule = (Molecule) request.subject();
    if (MolecularSpectroscopyPredictionExecutor.pyScfBackendRequested(request)) {
      return this.pyScfExcitedStateExecutor.execute(request, molecule, kind, diagnostics);
    }
    if (kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
        || MolecularSpectroscopyPredictionExecutor.nativeBackendRequested(request)) {
      return this.nativeExcitedStateExecutor.execute(request, molecule, kind, diagnostics);
    }
    final MolecularFeatureSet features = this.featureAnalyzer.analyze(molecule);
    diagnostics.add(
        ClassicalDiagnostic.warning(
            ClassicalDiagnosticCode.APPROXIMATION_REQUIRED,
            ClassicalDiagnosticTarget.of("calculationKind", kind.name()),
            "Spectroscopy prediction uses structural-feature ranges; it is not a quantum response calculation."));
    final ArrayList<ClassicalResultValue> values = new ArrayList<ClassicalResultValue>();
    ClassicalResultQualityValues.addApproximate(
        values,
        "java_native",
        "feature_based_ir_nmr_uv_prediction",
        "spectroscopy_prediction",
        "Feature-based spectroscopy is a fast structural prediction and not an ab initio spectral calculation.");
    values.add(ClassicalResultValue.text("method", "feature_based_ir_nmr_uv_prediction"));
    values.add(ClassicalResultValue.text("model", "feature_based_ir_nmr_uv_prediction"));
    values.add(ClassicalResultValue.scalar("feature_count", features.features().size(), "count"));
    values.add(
        ClassicalResultValue.of(
            "spectroscopy_rows",
            ClassicalResultValueKind.TABLE,
            "assignment",
            MolecularSpectroscopyPredictionExecutor.spectroscopyRows(features)));
    return ClassicalResultSection.of(
        kind,
        kind == ClassicalCalculationKind.EXCITED_STATE_ANALYSIS
            ? "Feature-based excited-state analysis"
            : "Feature-based spectroscopy prediction",
        values);
  }

  private static List<String> spectroscopyRows(final MolecularFeatureSet features) {
    final ArrayList<String> rows = new ArrayList<String>(24);
    rows.add("method\tregion\tassignment\tstrength\tfeature_count");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.CARBONYL,
        "IR",
        "1650-1810 cm^-1",
        "C=O stretch",
        "strong");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.NITRILE,
        "IR",
        "2210-2260 cm^-1",
        "C≡N stretch",
        "medium-strong");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.ALKYNE,
        "IR",
        "2100-2260 cm^-1",
        "C≡C stretch",
        "weak-medium");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.HYDROXYL_OR_ALKOXY,
        "IR",
        "3200-3600 cm^-1 / 1000-1260 cm^-1",
        "O-H or C-O stretch",
        "broad/strong");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.AMINE,
        "IR",
        "3300-3500 cm^-1",
        "N-H stretch region",
        "medium");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.NITRO,
        "IR",
        "1340-1560 cm^-1",
        "NO2 asymmetric/symmetric stretch",
        "strong");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.AROMATIC_RING,
        "NMR/UV",
        "6.0-8.5 ppm / 200-300 nm",
        "aromatic proton and pi-pi* region",
        "diagnostic");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.HETEROCYCLE,
        "NMR/UV",
        "heteroaromatic shifted region",
        "heterocycle electronic anisotropy",
        "diagnostic");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.ALKENE,
        "NMR/IR",
        "4.5-6.5 ppm / 1620-1680 cm^-1",
        "C=C and vinyl proton region",
        "medium");
    MolecularSpectroscopyPredictionExecutor.addIfPresent(
        rows,
        features,
        MolecularFeatureKind.HALOGENATED,
        "NMR/MS",
        "downfield carbon shifts / isotope pattern",
        "halogen substituent signature",
        "diagnostic");
    if (rows.size() == 1) {
      rows.add("summary\tfeature-limited\tno diagnostic feature detected\tweak\t0");
    }
    return List.copyOf(rows);
  }

  private static void addIfPresent(
      final List<String> rows,
      final MolecularFeatureSet features,
      final MolecularFeatureKind kind,
      final String method,
      final String region,
      final String assignment,
      final String strength) {
    final long count = features.countOf(kind);
    if (count <= 0L) {
      return;
    }
    rows.add(method + "\t" + region + "\t" + assignment + "\t" + strength + "\t" + count);
  }

  public static boolean pyScfBackendRequested(final ClassicalCalculationRequest request) {
    return MolecularSpectroscopyPredictionExecutor.optionEquals(
            request, EXCITED_STATE_BACKEND_OPTION, PYSCF_BACKEND)
        || MolecularSpectroscopyPredictionExecutor.optionEquals(
            request, SPECTROSCOPY_BACKEND_OPTION, PYSCF_BACKEND);
  }

  private static boolean nativeBackendRequested(final ClassicalCalculationRequest request) {
    return MolecularSpectroscopyPredictionExecutor.optionEquals(
            request, EXCITED_STATE_BACKEND_OPTION, NATIVE_BACKEND)
        || MolecularSpectroscopyPredictionExecutor.optionEquals(
            request, SPECTROSCOPY_BACKEND_OPTION, NATIVE_BACKEND);
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
}
