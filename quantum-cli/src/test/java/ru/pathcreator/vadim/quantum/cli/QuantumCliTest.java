/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.pathcreator.vadim.quantum.api.Quantum;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
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
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalCoverageItemRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.domain.bit.ClassicalBit;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalAssignment;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalComparisonOperator;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalExpression;
import ru.pathcreator.vadim.quantum.domain.classical.ClassicalPredicate;
import ru.pathcreator.vadim.quantum.domain.gate.StandardGate;
import ru.pathcreator.vadim.quantum.domain.model.QuantumCircuit;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.operation.ClassicalCondition;
import ru.pathcreator.vadim.quantum.domain.operation.GateOperation;
import ru.pathcreator.vadim.quantum.domain.operation.OperationBlock;
import ru.pathcreator.vadim.quantum.domain.operation.SymbolicForLoopOperation;
import ru.pathcreator.vadim.quantum.domain.register.ClassicalRegister;
import ru.pathcreator.vadim.quantum.domain.register.QuantumRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantumCliTest {

  @TempDir private Path tempDir;

  @Test
  void validatesOpenQasm2InJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result = run("validate", "--input", source.toString(), "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"valid\" : true"));
  }

  @Test
  void inspectsOpenQasm2InTextMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result = run("inspect", "--input", source.toString());

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("circuits=1"));
    assertTrue(result.stdout().contains("operations=4"));
  }

  @Test
  void preflightsTargetFormat() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run("preflight", "--input", source.toString(), "--output-format", "openqasm3");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("Preflight: EXPORTABLE"));
  }

  @Test
  void estimatesResourcesInJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result = run("resources", "--input", source.toString(), "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"estimatedStateVectorBytes\" : 64"));
    assertTrue(result.stdout().contains("\"localSimulationFeasible\" : true"));
    assertTrue(result.stdout().contains("\"twoQubitGateCount\" : 1"));
  }

  @Test
  void printsCircuitTimelineInJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result = run("circuit", "--input", source.toString(), "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"quantumWires\""));
    assertTrue(result.stdout().contains("\"label\" : \"cx\""));
    assertTrue(result.stdout().contains("\"classicalWires\""));
  }

  @Test
  void compilesAndWritesTargetFile() throws Exception {
    final Path source = writeBellOpenQasm2();
    final Path output = tempDir.resolve("bell.qasm3");

    final CliRunResult result =
        run(
            "compile",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--output",
            output.toString());

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(Files.readString(output).contains("OPENQASM 3.0"));
  }

  @Test
  void compileFastModeSkipsOptionalCompilerStages() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "compile",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--fast",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"status\" : \"EXPORTED\""));
    assertTrue(result.stdout().contains("\"status\" : \"SKIPPED\""));
    assertTrue(result.stdout().contains("\"stage\" : \"INITIAL_VALIDATION\""));
  }

  @Test
  void convertsExternalProgramToNativeJson() throws Exception {
    final Path source = writeBellOpenQasm2();
    final Path output = tempDir.resolve("bell.quantum.json");

    final CliRunResult result =
        run(
            "convert",
            "--input",
            source.toString(),
            "--output-format",
            "json",
            "--output",
            output.toString());

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(Files.readString(output).contains("\"computationModel\""));
  }

  @Test
  void simulatesBellProgram() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "simulate",
            "--input",
            source.toString(),
            "--shots",
            "128",
            "--seed",
            "9",
            "--state-vector",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"shots\" : 128"));
    assertTrue(result.stdout().contains("\"counts\""));
    assertTrue(result.stdout().contains("\"stateVectorSize\""));
  }

  @Test
  void simulatesNativeJsonWithClassicalControlSurface() throws Exception {
    final Path source = writeClassicalControlNativeJson();

    final CliRunResult result =
        run(
            "simulate",
            "--input",
            source.toString(),
            "--input-format",
            "json",
            "--format",
            "json",
            "--shots",
            "64",
            "--seed",
            "7");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"success\" : true"));
    assertTrue(result.stdout().contains("\"1\" : 64"));
  }

  @Test
  void runsProductWorkflowInJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "workflow",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "64",
            "--seed",
            "3",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"workflow\""));
    assertTrue(result.stdout().contains("\"status\" : \"COMPLETED\""));
    assertTrue(result.stdout().contains("\"backendDryRun\""));
    assertTrue(result.stdout().contains("\"compile\""));
    assertTrue(result.stdout().contains("\"resources\""));
  }

  @Test
  void runsBenchmarkInJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "benchmark",
            "--input",
            source.toString(),
            "--output-format",
            "openqasm3",
            "--warmup",
            "0",
            "--iterations",
            "1",
            "--shots",
            "32",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"benchmark\""));
    assertTrue(result.stdout().contains("\"stage\" : \"import\""));
    assertTrue(result.stdout().contains("\"stage\" : \"workflow\""));
    assertTrue(result.stdout().contains("\"totalAverageNanos\""));
  }

  @Test
  void runsCompatibilityMatrixInJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "compatibility",
            "--input",
            source.toString(),
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"compatibility\""));
    assertTrue(result.stdout().contains("\"targets\""));
    assertTrue(result.stdout().contains("\"targetFormat\" : \"OPENQASM_3\""));
    assertTrue(result.stdout().contains("\"checks\""));
  }

  @Test
  void verifiesCrossFormatRoundTripsInJsonMode() throws Exception {
    final Path source = writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "verify-cross-format",
            "--input",
            source.toString(),
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"verify-cross-format\""));
    assertTrue(result.stdout().contains("\"simulationEquivalent\" : true"));
    assertTrue(result.stdout().contains("\"targetFormat\" : \"OPENQASM_3\""));
  }

  @Test
  void runsCorpusRegressionForDirectoryInJsonMode() throws Exception {
    writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "regress-corpus",
            "--input",
            tempDir.toString(),
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"regress-corpus\""));
    assertTrue(result.stdout().contains("\"caseCount\" : 1"));
    assertTrue(result.stdout().contains("\"failureCount\" : 0"));
    assertTrue(result.stdout().contains("\"crossFormat\""));
  }

  @Test
  void runsReleaseReadinessForDirectoryInJsonMode() throws Exception {
    writeBellOpenQasm2();

    final CliRunResult result =
        run(
            "release-readiness",
            "--input",
            tempDir.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"release-readiness\""));
    assertTrue(result.stdout().contains("\"status\" : \"READY\""));
    assertTrue(result.stdout().contains("\"corpusRegression\""));
    assertTrue(result.stdout().contains("\"benchmark\""));
  }

  @Test
  void printsTargetProfile() {
    final CliRunResult result =
        run("target-profile", "--output-format", "quil", "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"targetName\""));
    assertTrue(result.stdout().contains("\"nativeGates\""));
  }

  @Test
  void printsProductDoctorReport() throws Exception {
    final Path project = createCompleteProductProject();

    final CliRunResult result = run("doctor", "--input", project.toString(), "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"doctor\""));
    assertTrue(result.stdout().contains("\"status\" : \"HEALTHY\""));
    assertTrue(result.stdout().contains("\"failedCheckCount\" : 0"));
  }

  @Test
  void printsProductAuditReport() throws Exception {
    final Path project = createCompleteProductProject();
    final Path corpus = tempDir.resolve("corpus");
    Files.createDirectories(corpus);
    Files.writeString(corpus.resolve("bell.qasm"), bellOpenQasm2Source());

    final CliRunResult result =
        run(
            "product-audit",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"product-audit\""));
    assertTrue(result.stdout().contains("\"status\" : \"READY\""));
    assertTrue(result.stdout().contains("\"doctor\""));
    assertTrue(result.stdout().contains("\"readiness\""));
  }

  @Test
  void writesProductReportBundle() throws Exception {
    final Path project = createCompleteProductProject();
    final Path corpus = tempDir.resolve("report-corpus");
    final Path output = tempDir.resolve("report-output");
    Files.createDirectories(corpus);
    Files.writeString(corpus.resolve("bell.qasm"), bellOpenQasm2Source());

    final CliRunResult result =
        run(
            "product-report",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output",
            output.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"product-report\""));
    assertTrue(Files.isRegularFile(output.resolve("product-audit.json")));
    assertTrue(Files.isRegularFile(output.resolve("summary.md")));
    assertTrue(Files.isRegularFile(output.resolve("manifest.properties")));
    assertTrue(Files.readString(output.resolve("summary.md")).contains("Quantum Product Report"));
    assertTrue(Files.readString(output.resolve("manifest.properties")).contains("summarySha256="));
  }

  @Test
  void writesProductDistributionBundle() throws Exception {
    final Path project = createCompleteProductProject();
    final Path corpus = tempDir.resolve("distribution-corpus");
    final Path output = tempDir.resolve("distribution-output");
    Files.createDirectories(corpus);
    Files.writeString(corpus.resolve("bell.qasm"), bellOpenQasm2Source());

    final CliRunResult result =
        run(
            "product-distribution",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output",
            output.toString(),
            "--output-format",
            "openqasm3",
            "--shots",
            "32",
            "--seed",
            "4",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"product-distribution\""));
    assertTrue(result.stdout().contains("\"archivePath\""));
    assertTrue(result.stdout().contains("\"archiveSha256\""));
    assertTrue(Files.isRegularFile(output.resolve("README.md")));
    assertTrue(Files.isRegularFile(tempDir.resolve("distribution-output.zip")));
    assertTrue(Files.isRegularFile(output.resolve("manifest.properties")));
    assertTrue(Files.isRegularFile(output.resolve("lib").resolve("quantum-cli-test.jar")));
    assertTrue(Files.isRegularFile(output.resolve("report").resolve("product-audit.json")));
    assertTrue(
        Files.readString(output.resolve("manifest.properties"))
            .contains("format=quantum-product-distribution"));
    final CliRunResult verify =
        run("product-verify-distribution", "--input", output.toString(), "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, verify.exitCode());
    assertTrue(verify.stdout().contains("\"command\" : \"product-verify-distribution\""));
    assertTrue(verify.stdout().contains("\"success\" : true"));
  }

  @Test
  void searchesChemistryLibraryInJsonMode() {
    final CliRunResult result =
        run(
            "chemistry-library",
            "--query",
            "acetone",
            "--kind",
            "molecule",
            "--limit",
            "5",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-library\""));
    assertTrue(result.stdout().contains("\"count\" : 1"));
    assertTrue(result.stdout().contains("\"id\" : \"molecule.acetone\""));
    assertTrue(result.stdout().contains("\"moleculeCount\" : 1"));
  }

  @Test
  void searchesChemistryLibraryByRussianTag() {
    final CliRunResult result =
        run("chemistry-library", "--query", "ацетон", "--kind", "molecule", "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"count\" : 1"));
    assertTrue(result.stdout().contains("\"ацетон\""));
  }

  @Test
  void searchesChemistryLibraryForRadicalBuildingBlock() {
    final CliRunResult result =
        run("chemistry-library", "--query", "radical", "--kind", "molecule", "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"id\" : \"molecule.hydrogen_radical\""));
    assertTrue(result.stdout().contains("\"radical\""));
  }

  @Test
  void searchesChemistryLibraryForHeteroarylAminationBuildingBlocks() {
    final CliRunResult substrate =
        run(
            "chemistry-library",
            "--query",
            "chloro nicotinonitrile",
            "--kind",
            "molecule",
            "--format",
            "json");
    final CliRunResult amine =
        run(
            "chemistry-library",
            "--query",
            "benzylamine",
            "--kind",
            "molecule",
            "--format",
            "json");

    assertEquals(
        QuantumCli.EXIT_SUCCESS, substrate.exitCode(), substrate.stdout() + substrate.stderr());
    assertEquals(QuantumCli.EXIT_SUCCESS, amine.exitCode(), amine.stdout() + amine.stderr());
    assertTrue(
        substrate
            .stdout()
            .contains("molecule.two_chloro_four_six_dimethyl_three_pyridinecarbonitrile"));
    assertTrue(amine.stdout().contains("molecule.benzylamine"));
  }

  @Test
  void predictsHeteroarylAminationFromLibraryReactants() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.two_chloro_four_six_dimethyl_three_pyridinecarbonitrile",
            "--reactant-entry",
            "molecule.benzylamine",
            "--domain",
            "heterocycle_functionalization",
            "--temperature-celsius",
            "120.0",
            "--pressure-atm",
            "1.0",
            "--duration-hours",
            "3.0",
            "--condition-component",
            "solvent:PEG4000:70:g:solution:70 g PEG4000",
            "--condition-component",
            "reactant:2-Chloro-4,6-dimethyl-3-pyridinecarbonitrile:10:g:solid:10 g substrate",
            "--condition-component",
            "reactant:Benzylamine:6.5:g:liquid:6.5 g benzylamine",
            "--electronic-rerank",
            "--electronic-rerank-max-candidates",
            "3",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"candidateCount\""));
    assertTrue(result.stdout().contains("NUCLEOPHILIC_AROMATIC_SUBSTITUTION"));
    assertTrue(result.stdout().contains("classicalCalculation"));
  }

  @Test
  void predictsOrganicReactionFromChemistryLibraryEntries() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.acetone",
            "--candidate-entry",
            "molecule.acetone",
            "--domain",
            "unknown",
            "--temperature-celsius",
            "25.0",
            "--duration-hours",
            "1.5",
            "--solvent",
            "ethanol",
            "--catalyst",
            "none",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-predict\""));
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"candidateCount\" : 1"));
    assertTrue(result.stdout().contains("\"expectedYieldPercent\""));
    assertTrue(result.stdout().contains("\"recommendationPlan\""));
    assertTrue(result.stdout().contains("\"computationRoadmap\""));
    assertTrue(result.stdout().contains("\"readyForFollowUpWork\" : true"));
    assertTrue(result.stdout().contains("\"quantumPreparationTargets\""));
  }

  @Test
  void predictsOrganicReactionWithRepeatedSeedCandidates() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.ethanol",
            "--candidate-entry",
            "molecule.ethanol",
            "--candidate-entry",
            "molecule.dimethyl_ether",
            "--domain",
            "unknown",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-predict\""));
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"candidateCount\" : 2"));
    assertTrue(result.stdout().contains("\"readyForFollowUpWork\" : true"));
  }

  @Test
  void predictsOrganicReactionWithMultiProductSeedCandidateSide() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.methane",
            "--reactant-entry",
            "molecule.oxygen",
            "--reactant-coefficient",
            "1",
            "--reactant-coefficient",
            "2",
            "--candidate-side-entry",
            "molecule.carbon_dioxide",
            "--candidate-side-entry",
            "molecule.water",
            "--candidate-side-coefficient",
            "1",
            "--candidate-side-coefficient",
            "2",
            "--domain",
            "unknown",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-predict\""));
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"candidateCount\" : 1"));
    assertTrue(result.stdout().contains("\"readyForFollowUpWork\" : true"));
  }

  @Test
  void passesCalculationOptionsIntoChemistryPredictionWorkflow() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.acetone",
            "--candidate-entry",
            "molecule.acetone",
            "--domain",
            "unknown",
            "--temperature-celsius",
            "25.0",
            "--duration-hours",
            "1.5",
            "--solvent",
            "ethanol",
            "--catalyst",
            "none",
            "--calculation-option",
            "advanced_prediction_electronic_rerank=true",
            "--calculation-option",
            "advanced_prediction_apply_calibration_priors=false",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-predict\""));
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"classicalCalculation\""));
    assertTrue(result.stdout().contains("ELECTRONIC_FACTOR"));
  }

  @Test
  void usesPublicElectronicRerankFlagForChemistryPredictionWorkflow() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.acetone",
            "--candidate-entry",
            "molecule.acetone",
            "--domain",
            "unknown",
            "--electronic-rerank",
            "--electronic-rerank-max-candidates",
            "1",
            "--electronic-rerank-max-atoms",
            "12",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-predict\""));
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"classicalCalculation\""));
    assertTrue(result.stdout().contains("ELECTRONIC_FACTOR"));
  }

  @Test
  void predictsGeneratedCandidateAndPreparesQuantumWorkflowFromLibraryReactant() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.hydrogen_radical",
            "--reactant-coefficient",
            "2",
            "--domain",
            "unknown",
            "--quantum-preparation",
            "--quantum-workflow-vqe",
            "--quantum-preparation-max-candidates",
            "1",
            "--quantum-preparation-max-participants",
            "1",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-predict\""));
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"candidateCount\" : 1"));
    assertTrue(result.stdout().contains("\"classicalCalculation\""));
    assertTrue(result.stdout().contains("candidate_quantum_workflow_preparation"));
    assertTrue(result.stdout().contains("vqe_status"));
    assertTrue(result.stdout().contains("CONVERGED"));
    assertTrue(result.stdout().contains("prepared"));
    assertTrue(result.stdout().contains("true"));
  }

  @Test
  void predictsGeneratedCandidateFromRepeatedReactantEntries() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.hydrogen_radical",
            "--reactant-entry",
            "molecule.hydrogen_radical",
            "--domain",
            "unknown",
            "--quantum-preparation",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"status\" : \"SUCCESS\""));
    assertTrue(result.stdout().contains("\"candidateCount\" : 1"));
    assertTrue(result.stdout().contains("\"classicalCalculation\""));
    assertTrue(result.stdout().contains("candidate_quantum_workflow_preparation"));
  }

  @Test
  void runsChemistryWorkflowFromLibraryEntryWithClassicalQuantumAndStoragePayloads()
      throws Exception {
    final CliRunResult result =
        run(
            "chemistry-workflow",
            "--subject-entry",
            "molecule.water",
            "--candidate-entry",
            "molecule.water",
            "--domain",
            "unknown",
            "--temperature-celsius",
            "25.0",
            "--duration-hours",
            "1.0",
            "--solvent",
            "water",
            "--mode",
            "full_quantum_preparation",
            "--method",
            "hartree-fock",
            "--basis",
            "STO-3G",
            "--mapping",
            "jordan_wigner",
            "--calculation-kinds",
            "hamiltonian_preparation",
            "--active-electrons",
            "2",
            "--active-orbitals",
            "2",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode(), result.stdout() + result.stderr());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-workflow\""));
    final com.fasterxml.jackson.databind.JsonNode payload =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.stdout());
    assertEquals("PARTIAL", payload.path("status").asText());
    assertEquals(false, payload.path("success").asBoolean());
    assertEquals(0, payload.path("stageSummary").path("failed").asInt(-1));
    assertEquals("SUCCEEDED", payload.path("classicalResult").path("status").asText());
    assertTrue(payload.path("quantumProgramPackage").path("artifactCount").asInt() > 0);
    assertTrue(
        java.util.stream.StreamSupport.stream(payload.path("stages").spliterator(), false)
            .anyMatch(
                stage ->
                    stage.path("stage").asText().equals("COMPUTATION_PARITY_PLANNING")
                        && stage.path("status").asText().equals("WARNING")));
    assertTrue(result.stdout().contains("\"classicalResult\""));
    assertTrue(result.stdout().contains("\"stageSummary\""));
    assertTrue(result.stdout().contains("\"completed\""));
    assertTrue(result.stdout().contains("\"skipped\""));
    assertTrue(result.stdout().contains("\"quantumProgramPackage\""));
    assertTrue(result.stdout().contains("\"artifactCount\""));
    assertTrue(result.stdout().contains("\"computationParity\""));
    assertTrue(result.stdout().contains("\"storageDocument\""));
    assertTrue(result.stdout().contains("\"molecules\" : 1"));
    assertTrue(result.stdout().contains("\"prediction\""));
  }

  @Test
  void reportsChemistryCalibrationCoverageForEmpiricalReadiness() {
    final CliRunResult result = run("chemistry-calibration-coverage", "--format", "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-calibration-coverage\""));
    assertTrue(result.stdout().contains("\"externallyVerifiedExactObservationCount\""));
    assertTrue(result.stdout().contains("\"classPriorObservationCount\""));
    assertTrue(result.stdout().contains("\"sourceTracedPriorObservationCount\""));
    assertTrue(result.stdout().contains("\"productionReadyForEmpiricalPrediction\""));
    assertTrue(result.stdout().contains("\"sourceTracedPriorObservationIds\""));
    assertTrue(result.stdout().contains("\"domainCounts\""));
    assertTrue(result.stdout().contains("\"mechanismFamilyCounts\""));
  }

  @Test
  void rejectsChemistryWorkflowHamiltonianWithoutActiveSpace() {
    final CliRunResult result =
        run(
            "chemistry-workflow",
            "--subject-entry",
            "molecule.water",
            "--calculation-kinds",
            "hamiltonian_preparation");

    assertEquals(QuantumCli.EXIT_USAGE_ERROR, result.exitCode());
    assertTrue(result.stderr().contains("requires --active-electrons and --active-orbitals"));
  }

  @Test
  void rejectsChemistryWorkflowVqeWithoutActiveSpace() {
    final CliRunResult result =
        run(
            "chemistry-workflow",
            "--subject-entry",
            "molecule.water",
            "--calculation-kinds",
            "quantum_vqe_optimization");

    assertEquals(QuantumCli.EXIT_USAGE_ERROR, result.exitCode());
    assertTrue(result.stderr().contains("requires --active-electrons and --active-orbitals"));
  }

  @Test
  void summarizesChemistryStorageWithRoadmapTimelineAndSnapshot() throws Exception {
    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<String> written = service.write(chemistryStorageDocument());
    assertTrue(written.success(), written.diagnostics().toString());
    final Path source = tempDir.resolve("chemistry.qchem");
    Files.writeString(source, written.value());

    final CliRunResult result =
        run(
            "chemistry-storage-summary",
            "--input",
            source.toString(),
            "--reaction-id",
            "h2_dissociation",
            "--history-index",
            "0",
            "--format",
            "json");

    assertEquals(QuantumCli.EXIT_SUCCESS, result.exitCode());
    assertTrue(result.stdout().contains("\"command\" : \"chemistry-storage-summary\""));
    assertTrue(result.stdout().contains("\"projectId\" : \"cli_storage_project\""));
    assertTrue(result.stdout().contains("\"roadmapRecords\""));
    assertTrue(result.stdout().contains("\"classicalResultRecords\""));
    assertTrue(result.stdout().contains("\"classicalResults\" : 1"));
    assertTrue(result.stdout().contains("\"electronic_energy\""));
    assertTrue(result.stdout().contains("\"-1.116684387\""));
    assertTrue(result.stdout().contains("\"candidate_0_quantum_hamiltonian\""));
    assertTrue(result.stdout().contains("\"timeline\""));
    assertTrue(result.stdout().contains("\"snapshot\""));
    assertTrue(result.stdout().contains("\"historyIndex\" : 0"));
    assertTrue(result.stdout().contains("\"quantumProgramIds\""));
  }

  @Test
  void rejectsUnknownChemistryDomain() {
    final CliRunResult result =
        run(
            "chemistry-predict",
            "--reactant-entry",
            "molecule.acetone",
            "--domain",
            "not-a-domain");

    assertEquals(QuantumCli.EXIT_USAGE_ERROR, result.exitCode());
    assertTrue(result.stderr().contains("Unsupported chemistry --domain value"));
  }

  @Test
  void detectsTamperedDistributionBundle() throws Exception {
    final Path project = createCompleteProductProject();
    final Path corpus = tempDir.resolve("tampered-distribution-corpus");
    final Path output = tempDir.resolve("tampered-distribution-output");
    Files.createDirectories(corpus);
    Files.writeString(corpus.resolve("bell.qasm"), bellOpenQasm2Source());
    final CliRunResult distribution =
        run(
            "product-distribution",
            "--input",
            corpus.toString(),
            "--project-root",
            project.toString(),
            "--output",
            output.toString(),
            "--output-format",
            "openqasm3",
            "--format",
            "json");
    assertEquals(QuantumCli.EXIT_SUCCESS, distribution.exitCode());
    Files.writeString(output.resolve("README.md"), "tampered");

    final CliRunResult verify =
        run("product-verify-distribution", "--input", output.toString(), "--format", "json");

    assertEquals(QuantumCli.EXIT_WORKFLOW_ERROR, verify.exitCode());
    assertTrue(verify.stdout().contains("MANIFEST_FILE_SIZE_MISMATCH"));
  }

  @Test
  void returnsUsageErrorForMissingInput() {
    final CliRunResult result = run("validate");

    assertEquals(QuantumCli.EXIT_USAGE_ERROR, result.exitCode());
    assertTrue(result.stderr().contains("Missing required option"));
  }

  private Path writeBellOpenQasm2() throws Exception {
    final Path source = tempDir.resolve("bell.qasm");
    Files.writeString(source, bellOpenQasm2Source());
    return source;
  }

  private static String bellOpenQasm2Source() {
    return """
            OPENQASM 2.0;
            include "qelib1.inc";
            qreg q[2];
            creg c[2];
            h q[0];
            cx q[0],q[1];
            measure q[0] -> c[0];
            measure q[1] -> c[1];
            """
        .trim();
  }

  private Path createCompleteProductProject() throws Exception {
    final Path project = tempDir.resolve("product");
    Files.createDirectories(project);
    Files.writeString(project.resolve("pom.xml"), pomWithModules());
    Files.writeString(project.resolve("LICENSE"), "MPL-2.0");
    Files.writeString(project.resolve(".gitignore"), "/target/\ndocs/\n/tools/\n.idea/\n");
    createDirectories(
        project,
        new String[] {
          "quantum-core",
          "quantum-simulation",
          "quantum-application",
          "quantum-json",
          "quantum-openqasm2",
          "quantum-openqasm3",
          "quantum-quil",
          "quantum-api",
          "quantum-cli",
          "quantum-desktop"
        });
    createFiles(
        project,
        new String[] {
          "smoke-corpus/README.md",
          "smoke-corpus/openqasm2/bell.qasm",
          "smoke-corpus/openqasm3/ghz.qasm",
          "smoke-corpus/quil/bell.quil",
          "quantum-cli/target/quantum-cli-test.jar",
          "quantum-desktop/target/quantum-desktop-test.jar"
        });
    return project;
  }

  private Path writeClassicalControlNativeJson() throws Exception {
    final QuantumProgram program = QuantumProgram.gateBased();
    final QuantumCircuit circuit = program.createCircuit("classical_control");
    final QuantumRegister q = circuit.createQuantumRegister("q", 1);
    final ClassicalRegister c = circuit.createClassicalRegister("c", 1);
    final ClassicalBit bit = c.get(0);
    circuit.assign(
        new ClassicalAssignment(ClassicalExpression.bit(bit), ClassicalExpression.integer(1L)));
    circuit.classicallyControlled(
        ClassicalPredicate.compare(
            ClassicalExpression.bit(bit),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1L)),
        GateOperation.of(StandardGate.X, q.get(0)));
    circuit.controlled(
        ClassicalCondition.equalTo(c, 1L), GateOperation.of(StandardGate.X, q.get(0)));
    circuit.conditionalBlock(
        ClassicalPredicate.compare(
            ClassicalExpression.bit(bit),
            ClassicalComparisonOperator.EQUAL,
            ClassicalExpression.integer(1L)),
        OperationBlock.of(GateOperation.of(StandardGate.X, q.get(0))),
        null);
    circuit.symbolicForLoop(
        new SymbolicForLoopOperation(
            "j",
            "int",
            ClassicalExpression.integer(0L),
            ClassicalExpression.integer(1L),
            ClassicalExpression.integer(1L),
            OperationBlock.of(
                GateOperation.of(StandardGate.X, q.get(0)),
                GateOperation.of(StandardGate.X, q.get(0)))));
    circuit.measure(q.get(0), bit);
    final Path source = tempDir.resolve("classical-control.quantum.json");
    Files.writeString(source, Quantum.writeJson(program).content());
    return source;
  }

  private static void createDirectories(final Path project, final String[] directories)
      throws Exception {
    for (int index = 0; index < directories.length; index++) {
      Files.createDirectories(project.resolve(directories[index]));
    }
  }

  private static void createFiles(final Path project, final String[] paths) throws Exception {
    for (int index = 0; index < paths.length; index++) {
      final Path path = project.resolve(paths[index]);
      Files.createDirectories(path.getParent());
      Files.writeString(path, "x");
    }
  }

  private static String pomWithModules() {
    return """
            <project>
                <modules>
                    <module>quantum-core</module>
                    <module>quantum-simulation</module>
                    <module>quantum-application</module>
                    <module>quantum-json</module>
                    <module>quantum-openqasm2</module>
                    <module>quantum-openqasm3</module>
                    <module>quantum-quil</module>
                    <module>quantum-api</module>
                    <module>quantum-cli</module>
                    <module>quantum-desktop</module>
                </modules>
            </project>
            """;
  }

  private static ChemistryStorageDocument chemistryStorageDocument() {
    final Molecule hydrogen = hydrogenMolecule();
    final Molecule atomicHydrogen = atomicHydrogen();
    final Reaction dissociation =
        Reaction.of(
            ReactionId.of("h2_dissociation"),
            "Hydrogen dissociation",
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(atomicHydrogen, StoichiometricCoefficient.of(2)))));
    final ChemistryStorageReactionRecord record =
        ChemistryStorageReactionRecord.of(
            "record_0",
            dissociation,
            ChemistryStorageReactionRecordStatus.PLANNED,
            0,
            null,
            "CLI storage summary fixture.");
    final ChemistryStoragePredictionRecord prediction =
        ChemistryStoragePredictionRecord.of(
            "prediction_0",
            "h2_prediction_request",
            0,
            ReactionPredictionMethodType.HYBRID,
            ChemistryStorageReactionRecordStatus.SIMULATED_QUANTUM,
            List.of(
                ChemistryStoragePredictionCandidateRecord.of(
                    0,
                    List.of("h"),
                    List.of("H"),
                    List.of("2*h:H"),
                    0.72,
                    64.0,
                    List.of(),
                    List.of("homolytic hydrogen dissociation"),
                    List.of("MECHANISM_MATCH:0.45:homolysis path"),
                    List.of("USER_RULE:0.2:CLI fixture mechanism evidence"),
                    "CLI prediction candidate.")),
            List.of("WARNING:-0.05:CLI fixture requires follow-up"),
            "CLI prediction snapshot.");
    final ChemistryStorageComputationPlanRecord plan =
        ChemistryStorageComputationPlanRecord.of(
            "calculation_plan_0",
            "record_0",
            0,
            ChemistryStorageReactionRecordStatus.PLANNED,
            List.of("classical_h2_rhf"),
            List.of("classical_h2_rhf_result"),
            List.of("quantum_h2_vqe"),
            true,
            "Prepared classical and quantum follow-up.");
    final ChemistryStorageClassicalResultRecord classicalResult =
        ChemistryStorageClassicalResultRecord.of(
            "classical_h2_rhf_result",
            "classical_h2_rhf",
            0,
            "SUCCEEDED",
            1,
            0,
            1,
            "EXACT",
            List.of(
                ChemistryStorageClassicalCoverageItemRecord.of(
                    "SINGLE_POINT_ENERGY", "EXACT", "RHF total energy")),
            List.of(
                ChemistryStorageClassicalResultSectionRecord.of(
                    "TOTAL_ENERGY",
                    "RHF total energy",
                    List.of(
                        ChemistryStorageClassicalResultValueRecord.of(
                            "electronic_energy", "SCALAR", "hartree", List.of("-1.116684387")),
                        ChemistryStorageClassicalResultValueRecord.of(
                            "reference", "TEXT", "", List.of("cli fixture"))))),
            "Portable classical result in CLI storage summary fixture.");
    final ChemistryStorageRoadmapRecord roadmap =
        ChemistryStorageRoadmapRecord.of(
            "roadmap_0",
            "h2_prediction_request",
            0,
            "SUCCESS",
            2,
            2,
            List.of(
                ChemistryStorageRoadmapCandidateRecord.of(
                    0,
                    0.72,
                    64.0,
                    List.of("h"),
                    List.of(),
                    List.of("GEOMETRY_OPTIMIZATION", "HAMILTONIAN_PREPARATION"),
                    List.of("candidate_0_quantum_hamiltonian"))),
            List.of("condition_sensitivity_kinetics"),
            "Persistent roadmap from prediction to computation.");
    final ChemistryStorageReactionHistoryEvent event =
        ChemistryStorageReactionHistoryEvent.of(
            "event_0",
            "h2_dissociation",
            0,
            ChemistryStorageReactionEventKind.CLASSICAL_CALCULATION_PLANNED,
            "Computation planned",
            "The reaction was converted into classical and quantum work items.",
            List.of("record_0"),
            List.of(),
            List.of("prediction_0"),
            List.of("calculation_plan_0"),
            List.of("classical_h2_rhf_result"),
            List.of("quantum_h2_vqe"));
    final QuantumProgram fixtureProgram = QuantumProgram.gateBased();
    fixtureProgram.createCircuit("storage_fixture").createQuantumRegister("q", 2);
    final var quantumProgram =
        ru.pathcreator.vadim.quantum.chemistry.storage.domain.model
            .ChemistryStorageQuantumProgramRecord.of(
            "quantum_h2_vqe",
            "calculation_plan_0",
            0,
            "VQE_ANSATZ",
            "QUANTUM_IR",
            Quantum.writeJson(fixtureProgram).content(),
            null,
            List.of("FIXTURE_ONLY"),
            "Storage-reference fixture; no optimized state or energy is asserted.");
    return ChemistryStorageDocument.builder("cli_storage_project", "CLI storage project")
        .molecule(hydrogen)
        .molecule(atomicHydrogen)
        .reaction(dissociation)
        .reactionRecord(record)
        .predictionRecord(prediction)
        .computationPlanRecord(plan)
        .classicalResultRecord(classicalResult)
        .quantumProgramRecord(quantumProgram)
        .roadmapRecord(roadmap)
        .reactionHistoryEvent(event)
        .build();
  }

  private static Molecule hydrogenMolecule() {
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    return Molecule.of(
        MoleculeId.of("h2"),
        "Hydrogen",
        List.of(atom(h1, "H", 0.0, 0.0, 0.0), atom(h2, "H", 0.74, 0.0, 0.0)),
        List.of(Bond.of(h1, h2, BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule atomicHydrogen() {
    return Molecule.of(
        MoleculeId.of("h"),
        "Hydrogen atom",
        List.of(
            Atom.of(
                AtomId.of("h"),
                ElementSymbol.of("H"),
                Coordinate3D.of(0.0, 0.0, 0.0, LengthUnit.ANGSTROM),
                FormalCharge.NEUTRAL,
                null,
                RadicalState.of(1),
                ChemistryMetadata.EMPTY)),
        List.of(),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.of(2));
  }

  private static Atom atom(
      final AtomId id, final String element, final double x, final double y, final double z) {
    return Atom.of(
        id,
        ElementSymbol.of(element),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.EMPTY);
  }

  private static CliRunResult run(final String... args) {
    final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    final int exitCode =
        QuantumCli.run(
            args,
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));
    return new CliRunResult(
        exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
  }

  private record CliRunResult(int exitCode, String stdout, String stderr) {}
}
