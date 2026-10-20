/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.MolecularConformation;
import ru.pathcreator.vadim.quantum.chemistry.domain.conformation.TorsionAngle;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.Coordinate3D;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.LengthUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ElementaryReactionStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismEnergyPoint;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanismPointKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEvent;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionCoordinateValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanism;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySource;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistrySourceLocation;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionMethodType;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentLoading;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentPurity;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionId;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionPhase;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionSide;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricCoefficient;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.StoichiometricEquivalent;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotation;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.OpticalRotationDirection;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereocenter;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.StereochemicalDescriptor;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.FormalCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Isotope;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MolecularCharge;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.MoleculeId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.RadicalState;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.SpinMultiplicity;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.MolecularSymmetry;
import ru.pathcreator.vadim.quantum.chemistry.domain.symmetry.PointGroupName;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentration;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Pressure;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.EnergyValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmount;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.SubstanceAmountUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.Temperature;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit;
import ru.pathcreator.vadim.quantum.chemistry.storage.application.ChemistryStorageService;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageDiagnosticCode;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.diagnostic.ChemistryStorageResult;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionEventKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionHistoryEvent;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionSnapshot;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.history.ChemistryStorageReactionTimeline;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultSectionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalResultValueRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageClassicalCoverageItemRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageComputationPlanRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageConditionSnapshotRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageDocument;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageExtension;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStoragePredictionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageQuantumProgramRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeKind;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionOutcomeRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageReactionRecordStatus;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapCandidateRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.domain.model.ChemistryStorageRoadmapRecord;
import ru.pathcreator.vadim.quantum.chemistry.storage.infrastructure.filesystem.ChemistryStorageFileRepository;

public final class ChemistryStorageRoundTripTest {

  @Test
  public void roundTripKeepsMoleculeReactionAndExtensions() {
    final Molecule reagent = ChemistryStorageRoundTripTest.chiralMolecule();
    final Molecule product = ChemistryStorageRoundTripTest.productMolecule();
    final Reaction reaction = ChemistryStorageRoundTripTest.reaction(reagent, product);
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder("project_alpha", "Alpha chemistry project")
            .metadata("owner", "lab")
            .metadata("purpose", "round-trip")
            .molecule(reagent)
            .molecule(product)
            .reaction(reaction)
            .extension(
                ChemistryStorageExtension.of(
                    "classical_task",
                    "task_1",
                    Map.of("engine", "future_java"),
                    List.of("line one", "line two with spaces")))
            .build();

    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<String> written = service.write(document);
    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertFalse(written.value().contains("{"));
    Assertions.assertFalse(written.value().contains("}"));

    final ChemistryStorageResult<ChemistryStorageDocument> read = service.read(written.value());
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(document, read.value());

    final ChemistryStorageResult<ChemistryStorageDocument> secondRead =
        service.read(service.write(read.value()).value());
    Assertions.assertTrue(secondRead.success(), secondRead.diagnostics().toString());
    Assertions.assertEquals(read.value(), secondRead.value());
  }

  @Test
  public void roundTripKeepsMechanismEventsAndReactionHistoryRecords() {
    final Molecule hydrogen = ChemistryStorageRoundTripTest.hydrogenMolecule();
    final Molecule atomicHydrogen = ChemistryStorageRoundTripTest.atomicHydrogen();
    final Reaction dissociation =
        Reaction.of(
            ReactionId.of("h2_dissociation"),
            "Hydrogen dissociation",
            ReactionSide.of(
                List.of(ReactionParticipant.of(hydrogen, StoichiometricCoefficient.ONE))),
            ReactionSide.of(
                List.of(ReactionParticipant.of(atomicHydrogen, StoichiometricCoefficient.of(2)))));
    final ElementaryReactionStep step =
        ElementaryReactionStep.of(
            "homolysis",
            dissociation,
            null,
            EnergyValue.of(436.0, EnergyUnit.KILOJOULE_PER_MOLE),
            EnergyValue.of(0.0, EnergyUnit.KILOJOULE_PER_MOLE),
            EnergyValue.of(436.0, EnergyUnit.KILOJOULE_PER_MOLE),
            List.of(
                MechanisticEvent.of(
                    "bond_cleavage",
                    0,
                    MechanisticEventKind.BOND_CLEAVAGE,
                    "H-H sigma bond cleaves homolytically.",
                    List.of("h2.h1", "h2.h2"),
                    1.0)));
    final ReactionMechanism mechanism =
        ReactionMechanism.of(
            "h2_homolysis",
            ReactionMechanismFamily.RADICAL_CHAIN,
            dissociation,
            List.of(step),
            List.of(
                MechanismEnergyPoint.of(
                    "reactants",
                    MechanismPointKind.REACTANT_COMPLEX,
                    ReactionCoordinateValue.of(0.0),
                    EnergyValue.of(0.0, EnergyUnit.KILOJOULE_PER_MOLE),
                    hydrogen.id()),
                MechanismEnergyPoint.ofMany(
                    "products",
                    MechanismPointKind.PRODUCT_COMPLEX,
                    ReactionCoordinateValue.of(1.0),
                    EnergyValue.of(436.0, EnergyUnit.KILOJOULE_PER_MOLE),
                    List.of(atomicHydrogen.id(), hydrogen.id()))));
    final ChemistryStorageReactionRecord record =
        ChemistryStorageReactionRecord.of(
            "record_0",
            dissociation,
            ChemistryStorageReactionRecordStatus.SIMULATED_CLASSICAL,
            0,
            null,
            "Reference homolysis calculation.");
    final ChemistryStorageReactionRecord observedRecord =
        ChemistryStorageReactionRecord.of(
            "record_1",
            dissociation,
            ChemistryStorageReactionRecordStatus.OBSERVED,
            1,
            61.5,
            "Observed laboratory follow-up.");
    final ChemistryStoragePredictionRecord predictionRecord =
        ChemistryStoragePredictionRecord.of(
            "prediction_0",
            "h2_prediction_request",
            1,
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
                    List.of("h2_homolysis"),
                    List.of("h2_homolysis:RADICAL_CHAIN:p=0.72:yield=64.0"),
                    List.of(
                        "MECHANISM_MATCH:0.45:homolysis path",
                        "KINETIC:0.12:barrier-compatible path"),
                    List.of(
                        "THERMODYNAMIC_FACTOR:0.21:weak H-H bond",
                        "QUANTUM_ENERGY_MODEL:0.18:VQE follow-up requested"),
                    "Quantum/classical parity candidate.")),
            List.of(
                "WARNING:-0.05:radical side products require follow-up",
                "USER_RULE:0.20:homolysis mechanism supplied by library"),
            "Prediction snapshot after mechanism analysis.");
    final ChemistryStorageComputationPlanRecord computationPlanRecord =
        ChemistryStorageComputationPlanRecord.of(
            "calculation_plan_0",
            "record_0",
            2,
            ChemistryStorageReactionRecordStatus.PLANNED,
            List.of("classical_h2_rhf", "classical_h2_barrier_scan"),
            List.of("classical_h2_rhf_result"),
            List.of("quantum_h2_vqe", "quantum_h2_qpe"),
            true,
            "Prepared but intentionally deferred.");
    final ChemistryStorageClassicalResultRecord classicalResultRecord =
        ChemistryStorageClassicalResultRecord.of(
            "classical_h2_rhf_result",
            "classical_h2_rhf",
            2,
            "SUCCEEDED",
            2,
            0,
            1,
            "APPROXIMATE",
            List.of(
                ChemistryStorageClassicalCoverageItemRecord.of(
                    "SINGLE_POINT_ENERGY", "EXACT", "RHF total energy"),
                ChemistryStorageClassicalCoverageItemRecord.of(
                    "REACTION_ENERGY", "APPROXIMATE", "Dissociation energy")),
            List.of(
                ChemistryStorageClassicalResultSectionRecord.of(
                    "TOTAL_ENERGY",
                    "RHF total energy",
                    List.of(
                        ChemistryStorageClassicalResultValueRecord.of(
                            "electronic_energy", "SCALAR", "hartree", List.of("-1.116684387")),
                        ChemistryStorageClassicalResultValueRecord.of(
                            "state_label", "TEXT", "", List.of("singlet ground state")))),
                ChemistryStorageClassicalResultSectionRecord.of(
                    "REACTION_ENERGY",
                    "Dissociation energy",
                    List.of(
                        ChemistryStorageClassicalResultValueRecord.of(
                            "delta_energy", "SCALAR", "kilojoule_per_mole", List.of("436.0"))))),
            "Portable RHF result for storage round-trip.");
    final ChemistryStorageQuantumProgramRecord vqeProgramRecord =
        ChemistryStorageQuantumProgramRecord.of(
            "quantum_h2_vqe",
            "calculation_plan_0",
            0,
            "VQE_ANSATZ",
            "QUANTUM_IR",
            "program h2_vqe\ncircuit main\nqreg q[2]\nansatz uccsd",
            "OPENQASM 3.0;\ninclude \"stdgates.inc\";\nqubit[2] q;",
            List.of("VALIDATION_OK", "EXPORT_LOWERING_REQUIRED"),
            "Prepared VQE program payload.");
    final ChemistryStorageQuantumProgramRecord qpeProgramRecord =
        ChemistryStorageQuantumProgramRecord.of(
            "quantum_h2_qpe",
            "calculation_plan_0",
            0,
            "PHASE_ESTIMATION",
            "QUANTUM_IR",
            "program h2_qpe\ncircuit phase_estimation\nqreg phase[4]\nqreg system[2]",
            null,
            List.of("VALIDATION_OK"),
            "Prepared QPE program payload.");
    final ChemistryStorageRoadmapRecord roadmapRecord =
        ChemistryStorageRoadmapRecord.of(
            "roadmap_0",
            "h2_prediction_request",
            2,
            "SUCCESS",
            4,
            4,
            List.of(
                ChemistryStorageRoadmapCandidateRecord.of(
                    0,
                    0.72,
                    64.0,
                    List.of("h"),
                    List.of("H"),
                    List.of("2*h:H"),
                    List.of("h2_homolysis"),
                    List.of("h2_homolysis:HOMOLYSIS:p=0.72:yield=64.0"),
                    List.of("TRANSITION_STATE_SEARCH", "REACTION_PATH", "HAMILTONIAN_PREPARATION"),
                    List.of("candidate_0_classical_transition_state"),
                    List.of("candidate_0/explicit-product-graph-materialization"),
                    List.of("candidate_0_quantum_hamiltonian"),
                    true,
                    true,
                    true,
                    "ready")),
            List.of("condition_sensitivity_kinetics"),
            "Persistent roadmap from prediction to classical and quantum follow-up.");
    final ChemistryStorageReactionOutcomeRecord outcomeRecord =
        ChemistryStorageReactionOutcomeRecord.of(
            "outcome_0",
            "h2_dissociation",
            0,
            ChemistryStorageReactionOutcomeKind.PREDICTED,
            List.of("h"),
            0.72,
            64.0,
            List.of("h2_homolysis"),
            List.of("classical_h2_rhf_result"),
            List.of("quantum_h2_vqe", "quantum_h2_qpe"),
            "Gas-phase dissociation at reference condition.",
            "Predicted radical pair outcome.");
    final ChemistryStorageConditionSnapshotRecord referenceConditions =
        ChemistryStorageConditionSnapshotRecord.of(
            "condition_0",
            "h2_dissociation",
            0,
            "Gas phase reference",
            ReactionConditions.of(
                Temperature.of(298.15, TemperatureUnit.KELVIN),
                Pressure.of(1.0, PressureUnit.ATMOSPHERE),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.SOLVENT,
                        "argon matrix",
                        null,
                        null,
                        StoichiometricEquivalent.of(1.0),
                        null,
                        ReactionComponentPurity.percent(99.999),
                        ReactionPhase.GAS,
                        "inert isolation environment")),
                "Reference gas-phase condition."),
            "Initial calculation condition.");
    final ChemistryStorageConditionSnapshotRecord heatedConditions =
        ChemistryStorageConditionSnapshotRecord.of(
            "condition_1",
            "h2_dissociation",
            1,
            "Heated follow-up",
            ReactionConditions.of(
                Temperature.of(650.0, TemperatureUnit.KELVIN),
                Pressure.of(0.5, PressureUnit.ATMOSPHERE),
                List.of(
                    ReactionConditionComponent.of(
                        ReactionComponentRole.CATALYST,
                        "platinum surface",
                        null,
                        null,
                        null,
                        ReactionComponentLoading.percent(2.5),
                        ReactionComponentPurity.percent(95.0),
                        ReactionPhase.SOLID,
                        "surface assisted dissociation")),
                "Observed elevated-temperature condition."),
            "User-entered laboratory condition.");
    final ChemistryStorageReactionHistoryEvent plannedEvent =
        ChemistryStorageReactionHistoryEvent.of(
            "event_0",
            "h2_dissociation",
            0,
            ChemistryStorageReactionEventKind.CLASSICAL_CALCULATION_PLANNED,
            "Classical and quantum work prepared",
            "The reaction was converted into classical result and Quantum IR work items.",
            List.of("record_0"),
            List.of("condition_0"),
            List.of("prediction_0"),
            List.of("calculation_plan_0"),
            List.of("classical_h2_rhf_result"),
            List.of("quantum_h2_vqe", "quantum_h2_qpe"));
    final ChemistryStorageReactionHistoryEvent observationEvent =
        ChemistryStorageReactionHistoryEvent.of(
            "event_1",
            "h2_dissociation",
            1,
            ChemistryStorageReactionEventKind.OBSERVATION_ADDED,
            "Observed laboratory follow-up",
            "Observed yield was attached without rerunning calculations.",
            List.of("record_1"),
            List.of("condition_1"),
            List.of(),
            List.of(),
            List.of(),
            List.of());
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder("mechanism_project", "Mechanism project")
            .molecule(hydrogen)
            .molecule(atomicHydrogen)
            .reaction(dissociation)
            .mechanism(mechanism)
            .reactionRecord(record)
            .reactionRecord(observedRecord)
            .predictionRecord(predictionRecord)
            .computationPlanRecord(computationPlanRecord)
            .classicalResultRecord(classicalResultRecord)
            .quantumProgramRecord(vqeProgramRecord)
            .quantumProgramRecord(qpeProgramRecord)
            .roadmapRecord(roadmapRecord)
            .conditionSnapshotRecord(referenceConditions)
            .conditionSnapshotRecord(heatedConditions)
            .reactionOutcomeRecord(outcomeRecord)
            .reactionHistoryEvent(plannedEvent)
            .reactionHistoryEvent(observationEvent)
            .build();
    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<String> written = service.write(document);

    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertTrue(written.value().contains("mechanism id=h2_homolysis"));
    Assertions.assertTrue(written.value().contains("event step=homolysis"));
    Assertions.assertTrue(written.value().contains("reaction-record id=record_0"));
    Assertions.assertTrue(written.value().contains("prediction-record id=prediction_0"));
    Assertions.assertTrue(
        written.value().contains("computation-plan-record id=calculation_plan_0"));
    Assertions.assertTrue(
        written.value().contains("classical-result-record id=classical_h2_rhf_result"));
    Assertions.assertTrue(written.value().contains("coverage=APPROXIMATE"));
    Assertions.assertTrue(
        written.value().contains("coverage index=0 kind=SINGLE_POINT_ENERGY status=EXACT"));
    Assertions.assertTrue(
        written.value().contains("coverage index=1 kind=REACTION_ENERGY status=APPROXIMATE"));
    Assertions.assertTrue(written.value().contains("value section=0 name=electronic_energy"));
    Assertions.assertTrue(written.value().contains("roadmap-record id=roadmap_0"));
    Assertions.assertTrue(written.value().contains("roadmap-candidate index=0"));
    Assertions.assertTrue(written.value().contains("condition-snapshot-record id=condition_0"));
    Assertions.assertTrue(written.value().contains("condition role=CATALYST"));
    Assertions.assertTrue(written.value().contains("reaction-outcome-record id=outcome_0"));
    Assertions.assertTrue(written.value().contains("reaction-history-event id=event_0"));
    Assertions.assertTrue(written.value().contains("conditionSnapshots=\"condition_0\""));
    Assertions.assertTrue(written.value().contains("classicalResults=\"classical_h2_rhf_result\""));
    Assertions.assertTrue(written.value().contains("candidate rank=0"));
    Assertions.assertTrue(
        written
            .value()
            .contains("diagnostics=\"WARNING:-0.05:radical side products require follow-up"));
    Assertions.assertTrue(
        written
            .value()
            .contains("mechanismDetails=\"h2_homolysis:RADICAL_CHAIN:p=0.72:yield=64.0\""));
    Assertions.assertTrue(written.value().contains("score=\"MECHANISM_MATCH:0.45:homolysis path"));
    Assertions.assertTrue(
        written.value().contains("evidence=\"THERMODYNAMIC_FACTOR:0.21:weak H-H bond"));

    final ChemistryStorageResult<ChemistryStorageDocument> read = service.read(written.value());
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(document, read.value());
    Assertions.assertEquals(1, read.value().mechanisms().get(0).eventCount());
    Assertions.assertEquals(
        ChemistryStorageReactionRecordStatus.SIMULATED_CLASSICAL,
        read.value().reactionRecords().get(0).status());
    final ChemistryStorageReactionTimeline timeline =
        service.reactionTimeline(read.value(), "h2_dissociation");
    Assertions.assertFalse(timeline.empty());
    Assertions.assertEquals(2, timeline.size());
    Assertions.assertEquals(2, timeline.eventCount());
    Assertions.assertEquals("record_0", timeline.first().id());
    Assertions.assertEquals("record_1", timeline.latest().id());
    Assertions.assertEquals(61.5, timeline.requireHistoryIndex(1).observedYieldPercent());
    Assertions.assertEquals(
        ChemistryStorageReactionEventKind.CLASSICAL_CALCULATION_PLANNED,
        timeline.requireEvent("event_0").kind());
    Assertions.assertEquals(
        List.of("quantum_h2_vqe", "quantum_h2_qpe"),
        timeline.requireEvent("event_0").quantumProgramIds());
    Assertions.assertEquals(
        List.of("condition_0"), timeline.requireEvent("event_0").conditionSnapshotRecordIds());
    Assertions.assertEquals(
        List.of("condition_1"), timeline.requireEvent("event_1").conditionSnapshotRecordIds());
    Assertions.assertEquals(
        ChemistryStorageReactionRecordStatus.SIMULATED_QUANTUM,
        read.value().predictionRecords().get(0).status());
    Assertions.assertEquals(
        List.of(
            "WARNING:-0.05:radical side products require follow-up",
            "USER_RULE:0.20:homolysis mechanism supplied by library"),
        read.value().predictionRecords().get(0).diagnostics());
    Assertions.assertEquals(1, read.value().reactionOutcomeRecords().size());
    Assertions.assertEquals(
        ChemistryStorageReactionOutcomeKind.PREDICTED,
        read.value().reactionOutcomeRecords().get(0).kind());
    Assertions.assertEquals(2, read.value().conditionSnapshotRecords().size());
    Assertions.assertEquals(
        "Heated follow-up", read.value().conditionSnapshotRecords().get(1).label());
    Assertions.assertEquals(
        List.of("quantum_h2_vqe", "quantum_h2_qpe"),
        read.value().reactionOutcomeRecords().get(0).quantumProgramIds());
    Assertions.assertEquals(
        List.of("h2_homolysis"),
        read.value().predictionRecords().get(0).candidates().get(0).mechanismIds());
    Assertions.assertEquals(
        List.of("H"),
        read.value().predictionRecords().get(0).candidates().get(0).productFormulaSignatures());
    Assertions.assertEquals(
        List.of("2*h:H"),
        read.value()
            .predictionRecords()
            .get(0)
            .candidates()
            .get(0)
            .productStoichiometrySignatures());
    Assertions.assertEquals(
        List.of("h2_homolysis:RADICAL_CHAIN:p=0.72:yield=64.0"),
        read.value().predictionRecords().get(0).candidates().get(0).mechanismDescriptors());
    Assertions.assertEquals(
        List.of("MECHANISM_MATCH:0.45:homolysis path", "KINETIC:0.12:barrier-compatible path"),
        read.value().predictionRecords().get(0).candidates().get(0).scoreComponents());
    Assertions.assertEquals(
        List.of(
            "THERMODYNAMIC_FACTOR:0.21:weak H-H bond",
            "QUANTUM_ENERGY_MODEL:0.18:VQE follow-up requested"),
        read.value().predictionRecords().get(0).candidates().get(0).evidence());
    Assertions.assertEquals(
        List.of("classical_h2_rhf", "classical_h2_barrier_scan"),
        read.value().computationPlanRecords().get(0).classicalPlanIds());
    Assertions.assertEquals(
        List.of("classical_h2_rhf_result"),
        read.value().computationPlanRecords().get(0).classicalResultIds());
    Assertions.assertEquals(1, read.value().classicalResultRecords().size());
    Assertions.assertEquals(classicalResultRecord, read.value().classicalResultRecords().get(0));
    Assertions.assertTrue(read.value().classicalResultRecords().get(0).hasSections());
    Assertions.assertTrue(read.value().classicalResultRecords().get(0).hasDiagnostics());
    Assertions.assertEquals(
        "RHF total energy",
        read.value().classicalResultRecords().get(0).requireSection("TOTAL_ENERGY").title());
    Assertions.assertEquals(
        "-1.116684387",
        read.value()
            .classicalResultRecords()
            .get(0)
            .requireSection("TOTAL_ENERGY")
            .requireValue("electronic_energy")
            .firstValue());
    Assertions.assertEquals(
        List.of("quantum_h2_vqe", "quantum_h2_qpe"),
        read.value().computationPlanRecords().get(0).quantumProgramIds());
    Assertions.assertEquals(2, read.value().quantumProgramRecords().size());
    Assertions.assertEquals(vqeProgramRecord, read.value().quantumProgramRecords().get(0));
    Assertions.assertTrue(read.value().quantumProgramRecords().get(0).hasNativePayload());
    Assertions.assertTrue(read.value().quantumProgramRecords().get(0).hasExportedPayload());
    Assertions.assertEquals(
        List.of("VALIDATION_OK", "EXPORT_LOWERING_REQUIRED"),
        read.value().quantumProgramRecords().get(0).diagnostics());
    Assertions.assertEquals(1, read.value().roadmapRecords().size());
    Assertions.assertEquals(roadmapRecord, read.value().roadmapRecords().get(0));
    Assertions.assertTrue(read.value().roadmapRecords().get(0).hasCandidates());
    Assertions.assertEquals(
        List.of("candidate_0_quantum_hamiltonian"),
        read.value().roadmapRecords().get(0).candidates().get(0).quantumPreparationTargets());
    Assertions.assertEquals(
        List.of("H"),
        read.value().roadmapRecords().get(0).candidates().get(0).productFormulaSignatures());
    Assertions.assertEquals(
        List.of("2*h:H"),
        read.value().roadmapRecords().get(0).candidates().get(0).productStoichiometrySignatures());
    Assertions.assertEquals(
        List.of("candidate_0/explicit-product-graph-materialization"),
        read.value().roadmapRecords().get(0).candidates().get(0).followUpActionTargets());
    Assertions.assertTrue(
        read.value().roadmapRecords().get(0).candidates().get(0).explicitProductGraph());
    Assertions.assertTrue(
        read.value().roadmapRecords().get(0).candidates().get(0).electronicRerankReady());
    Assertions.assertTrue(
        read.value().roadmapRecords().get(0).candidates().get(0).quantumWorkflowReady());
    Assertions.assertEquals(
        "ready", read.value().roadmapRecords().get(0).candidates().get(0).readinessReason());
    final ChemistryStorageReactionSnapshot plannedSnapshot =
        service.reactionSnapshot(read.value(), "h2_dissociation", 0);
    Assertions.assertEquals("record_0", plannedSnapshot.reactionRecord().id());
    Assertions.assertFalse(plannedSnapshot.hasPredictions());
    Assertions.assertTrue(plannedSnapshot.hasOutcomes());
    Assertions.assertFalse(plannedSnapshot.hasComputationPlans());
    Assertions.assertFalse(plannedSnapshot.hasRoadmaps());
    Assertions.assertFalse(plannedSnapshot.hasClassicalResults());
    Assertions.assertTrue(plannedSnapshot.hasQuantumPrograms());
    Assertions.assertTrue(plannedSnapshot.hasHistoryEvents());
    Assertions.assertEquals(0, plannedSnapshot.predictionRecords().size());
    Assertions.assertEquals(1, plannedSnapshot.conditionSnapshotRecords().size());
    Assertions.assertEquals("condition_0", plannedSnapshot.conditionSnapshotRecords().get(0).id());
    Assertions.assertTrue(plannedSnapshot.hasConditionSnapshots());
    Assertions.assertEquals(1, plannedSnapshot.outcomeRecords().size());
    Assertions.assertEquals(0, plannedSnapshot.computationPlanRecords().size());
    Assertions.assertEquals(0, plannedSnapshot.roadmapRecords().size());
    Assertions.assertEquals(0, plannedSnapshot.classicalResultRecords().size());
    Assertions.assertEquals(2, plannedSnapshot.quantumProgramRecords().size());
    Assertions.assertEquals(
        ChemistryStorageReactionEventKind.CLASSICAL_CALCULATION_PLANNED,
        plannedSnapshot.historyEvents().get(0).kind());
    final ChemistryStorageReactionSnapshot observedSnapshot =
        service.reactionSnapshot(read.value(), "h2_dissociation", 1);
    Assertions.assertEquals("record_1", observedSnapshot.reactionRecord().id());
    Assertions.assertEquals(2, observedSnapshot.historyEvents().size());
    Assertions.assertEquals(2, observedSnapshot.conditionSnapshotRecords().size());
    Assertions.assertEquals(
        TemperatureUnit.KELVIN,
        observedSnapshot.conditionSnapshotRecords().get(1).conditions().temperature().unit());
    Assertions.assertEquals(1, observedSnapshot.outcomeRecords().size());
    Assertions.assertEquals(61.5, observedSnapshot.reactionRecord().observedYieldPercent());
    Assertions.assertEquals(List.of(predictionRecord), observedSnapshot.predictionRecords());
    Assertions.assertFalse(observedSnapshot.hasComputationPlans());
    Assertions.assertFalse(observedSnapshot.hasRoadmaps());
    Assertions.assertFalse(observedSnapshot.hasClassicalResults());
    final ChemistryStorageReactionSnapshot completedSnapshot =
        service.reactionSnapshot(read.value(), "h2_dissociation", 2);
    Assertions.assertEquals(
        List.of(computationPlanRecord), completedSnapshot.computationPlanRecords());
    Assertions.assertEquals(List.of(roadmapRecord), completedSnapshot.roadmapRecords());
    Assertions.assertEquals(
        List.of(classicalResultRecord), completedSnapshot.classicalResultRecords());
    // Same history index in another reaction must not import the first reaction's artifacts.
    final Reaction other =
        Reaction.of(
            ReactionId.of("other_reaction"),
            "Other reaction",
            dissociation.reactants(),
            dissociation.products());
    final ChemistryStorageDocument multiple =
        read.value().toBuilder()
            .reaction(other)
            .reactionRecord(
                ChemistryStorageReactionRecord.of(
                    "other_record",
                    other,
                    ChemistryStorageReactionRecordStatus.PLANNED,
                    0,
                    null,
                    null))
            .build();
    final ChemistryStorageReactionSnapshot unrelated =
        service.reactionSnapshot(multiple, "other_reaction", 2);
    Assertions.assertFalse(unrelated.hasPredictions());
    Assertions.assertFalse(unrelated.hasRoadmaps());
    Assertions.assertFalse(unrelated.hasComputationPlans());
    Assertions.assertFalse(unrelated.hasClassicalResults());
    Assertions.assertFalse(unrelated.hasQuantumPrograms());
    Assertions.assertTrue(read.value().computationPlanRecords().get(0).deferredExecution());
  }

  @Test
  public void parserReturnsDiagnosticsForInvalidInput() {
    final ChemistryStorageService service = new ChemistryStorageService();
    final ChemistryStorageResult<ChemistryStorageDocument> empty = service.read("   ");
    Assertions.assertFalse(empty.success());
    Assertions.assertTrue(empty.hasErrors());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.EMPTY_INPUT, empty.diagnostics().get(0).code());

    final ChemistryStorageResult<ChemistryStorageDocument> invalid =
        service.read("QCHEM version=1\r\nmolecule id=x name=\"bad\"\r\nend");
    Assertions.assertFalse(invalid.success());
    Assertions.assertTrue(invalid.hasErrors());

    final ChemistryStorageResult<ChemistryStorageDocument> unclosedQuote =
        service.read("QCHEM version=1\r\nproject id=broken name=\"Broken\r\n");
    Assertions.assertFalse(unclosedQuote.success());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.INVALID_LINE, unclosedQuote.diagnostics().get(0).code());
  }

  @Test
  public void parserReturnsSpecificDiagnosticsForStorageContractErrors() {
    final ChemistryStorageService service = new ChemistryStorageService();
    final String missingEnd =
        "QCHEM version=1\r\n"
            + "project id=project_missing_end name=\"Missing end\"\r\n"
            + "molecule id=water name=\"Water\" charge=0 spin=1 symmetry=C1 symmetryNumber=1 "
            + "rotation=NONE rotationDegrees=null rotationWavelengthNm=null\r\n";
    final ChemistryStorageResult<ChemistryStorageDocument> missingEndResult =
        service.read(missingEnd);
    Assertions.assertFalse(missingEndResult.success());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.MISSING_END, missingEndResult.diagnostics().get(0).code());

    final String unknownReference =
        "QCHEM version=1\r\n"
            + "project id=project_unknown_ref name=\"Unknown reference\"\r\n"
            + "reaction id=r1 name=\"Broken\"\r\n"
            + "  reactant molecule=missing coefficient=1\r\n"
            + "  product molecule=missing coefficient=1\r\n"
            + "end\r\n";
    final ChemistryStorageResult<ChemistryStorageDocument> unknownReferenceResult =
        service.read(unknownReference);
    Assertions.assertFalse(unknownReferenceResult.success());
    Assertions.assertEquals(
        ChemistryStorageDiagnosticCode.UNKNOWN_MOLECULE_REFERENCE,
        unknownReferenceResult.diagnostics().get(0).code());
  }

  @Test
  public void documentRejectsDuplicateMoleculeIdsAndDetachedReactionMolecules() {
    final Molecule water = ChemistryStorageRoundTripTest.productMolecule();
    final Executable duplicateMoleculeDocumentCreation =
        new Executable() {
          public void execute() {
            ChemistryStorageDocument.builder("duplicates", "Duplicates")
                .molecule(water)
                .molecule(water)
                .build();
          }
        };
    Assertions.assertThrows(IllegalArgumentException.class, duplicateMoleculeDocumentCreation);

    final Molecule reagent = ChemistryStorageRoundTripTest.chiralMolecule();
    final Reaction detachedReaction = ChemistryStorageRoundTripTest.reaction(reagent, water);
    final Executable detachedReactionDocumentCreation =
        new Executable() {
          public void execute() {
            ChemistryStorageDocument.builder("detached", "Detached")
                .molecule(water)
                .reaction(detachedReaction)
                .build();
          }
        };
    Assertions.assertThrows(IllegalArgumentException.class, detachedReactionDocumentCreation);

    final Executable detachedHistoryEventCreation =
        new Executable() {
          public void execute() {
            ChemistryStorageDocument.builder("detached_event", "Detached event")
                .molecule(water)
                .reactionHistoryEvent(
                    ChemistryStorageReactionHistoryEvent.of(
                        "event_detached",
                        "missing_reaction",
                        0,
                        ChemistryStorageReactionEventKind.USER_EDIT,
                        "Detached",
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()))
                .build();
          }
        };
    Assertions.assertThrows(IllegalArgumentException.class, detachedHistoryEventCreation);

    final Executable detachedConditionSnapshotCreation =
        new Executable() {
          public void execute() {
            ChemistryStorageDocument.builder("detached_condition", "Detached condition")
                .molecule(water)
                .conditionSnapshotRecord(
                    ChemistryStorageConditionSnapshotRecord.of(
                        "condition_detached",
                        "missing_reaction",
                        0,
                        "Detached condition",
                        ReactionConditions.EMPTY,
                        null))
                .build();
          }
        };
    Assertions.assertThrows(IllegalArgumentException.class, detachedConditionSnapshotCreation);
  }

  @Test
  public void fileRepositoryWritesAndReadsUtf8StorageFile() throws Exception {
    final ChemistryStorageDocument document =
        ChemistryStorageDocument.builder("project_file", "File project")
            .molecule(ChemistryStorageRoundTripTest.productMolecule())
            .build();
    final Path directory = Files.createTempDirectory("qchem-storage-test");
    final Path file = directory.resolve("sample.qchem");
    final ChemistryStorageFileRepository repository = new ChemistryStorageFileRepository();

    final ChemistryStorageResult<Path> written = repository.write(file, document);
    Assertions.assertTrue(written.success(), written.diagnostics().toString());
    Assertions.assertTrue(Files.exists(file));

    final ChemistryStorageResult<ChemistryStorageDocument> read = repository.read(file);
    Assertions.assertTrue(read.success(), read.diagnostics().toString());
    Assertions.assertEquals(document, read.value());
  }

  private static Molecule chiralMolecule() {
    final AtomId c = AtomId.of("c");
    final AtomId h = AtomId.of("h");
    final AtomId cl = AtomId.of("cl");
    final AtomId br = AtomId.of("br");
    final AtomId f = AtomId.of("f");
    return Molecule.of(
        MoleculeId.of("chiral_halide"),
        "Chiral halide",
        List.of(
            ChemistryStorageRoundTripTest.atomWithMetadata(c, "C", 0.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h, "H", 1.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(cl, "Cl", 0.0, 1.0, 0.0),
            ChemistryStorageRoundTripTest.atom(br, "Br", 0.0, 0.0, 1.0),
            Atom.of(
                f,
                ElementSymbol.of("F"),
                Coordinate3D.of(-1.0, -1.0, -1.0, LengthUnit.ANGSTROM),
                FormalCharge.NEUTRAL,
                Isotope.of(19),
                RadicalState.CLOSED_SHELL,
                ChemistryMetadata.EMPTY)),
        List.of(
            Bond.of(c, h, BondType.SINGLE),
            Bond.of(c, cl, BondType.SINGLE),
            Bond.of(c, br, BondType.SINGLE),
            Bond.of(c, f, BondType.SINGLE),
            Bond.of(h, cl, BondType.SINGLE),
            Bond.of(cl, br, BondType.SINGLE),
            Bond.of(br, f, BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET,
        Stereochemistry.of(
            List.of(Stereocenter.ofTetrahedralAtom(c, StereochemicalDescriptor.R, h, cl, br, f))),
        MolecularConformation.of(List.of(TorsionAngle.of(h, cl, br, f, 60.0))),
        OpticalRotation.of(OpticalRotationDirection.DEXTROROTATORY, 12.5, null, 589.0),
        MolecularSymmetry.of(PointGroupName.C1, 1),
        ChemistryMetadata.of(
            ChemistrySource.of("manual", "unit test"),
            ChemistrySourceLocation.of(3, 7),
            Map.of("note", "metadata survives")));
  }

  private static Molecule productMolecule() {
    final AtomId o = AtomId.of("o");
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    return Molecule.of(
        MoleculeId.of("water"),
        "Water",
        List.of(
            ChemistryStorageRoundTripTest.atom(o, "O", 0.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h1, "H", 0.96, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h2, "H", -0.24, 0.93, 0.0)),
        List.of(Bond.of(o, h1, BondType.SINGLE), Bond.of(o, h2, BondType.SINGLE)),
        MolecularCharge.NEUTRAL,
        SpinMultiplicity.SINGLET);
  }

  private static Molecule hydrogenMolecule() {
    final AtomId h1 = AtomId.of("h1");
    final AtomId h2 = AtomId.of("h2");
    return Molecule.of(
        MoleculeId.of("h2"),
        "Hydrogen",
        List.of(
            ChemistryStorageRoundTripTest.atom(h1, "H", 0.0, 0.0, 0.0),
            ChemistryStorageRoundTripTest.atom(h2, "H", 0.74, 0.0, 0.0)),
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

  private static Reaction reaction(final Molecule reagent, final Molecule product) {
    return Reaction.of(
        ReactionId.of("hydrolysis"),
        "Hydrolysis storage check",
        ReactionSide.of(List.of(ReactionParticipant.of(reagent, StoichiometricCoefficient.ONE))),
        ReactionSide.of(List.of(ReactionParticipant.of(product, StoichiometricCoefficient.of(2)))),
        ReactionConditions.of(
            Temperature.of(298.15, TemperatureUnit.KELVIN),
            Pressure.of(1.0, PressureUnit.ATMOSPHERE),
            List.of(
                ReactionConditionComponent.of(
                    ReactionComponentRole.SOLVENT,
                    "water",
                    SubstanceAmount.of(1.5, SubstanceAmountUnit.MOLE),
                    MolarConcentration.of(0.1, MolarConcentrationUnit.MOLE_PER_LITER),
                    StoichiometricEquivalent.of(2.0),
                    ReactionComponentLoading.percent(5.0),
                    ReactionComponentPurity.percent(99.0),
                    ReactionPhase.AQUEOUS,
                    "condition note")),
            "reaction note"),
        ChemistryMetadata.of(null, null, Map.of("route", "test")));
  }

  private static Atom atom(
      final AtomId id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(id, ElementSymbol.of(symbol), Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM));
  }

  private static Atom atomWithMetadata(
      final AtomId id, final String symbol, final double x, final double y, final double z) {
    return Atom.of(
        id,
        ElementSymbol.of(symbol),
        Coordinate3D.of(x, y, z, LengthUnit.ANGSTROM),
        FormalCharge.NEUTRAL,
        null,
        RadicalState.CLOSED_SHELL,
        ChemistryMetadata.of(
            ChemistrySource.of("lab_book", "atom provenance"),
            ChemistrySourceLocation.of(5, 11),
            Map.of("role", "stereocenter")));
  }
}
