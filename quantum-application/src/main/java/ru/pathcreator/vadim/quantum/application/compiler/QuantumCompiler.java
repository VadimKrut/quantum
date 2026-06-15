/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.application.compiler;

import java.util.List;

import ru.pathcreator.vadim.quantum.application.inspection.ProgramInspectionResult;
import ru.pathcreator.vadim.quantum.application.inspection.QuantumProgramInspector;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightChecker;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightResult;
import ru.pathcreator.vadim.quantum.application.integration.capability.CapabilityPreflightStatus;
import ru.pathcreator.vadim.quantum.application.integration.contract.QuantumExporter;
import ru.pathcreator.vadim.quantum.application.integration.result.ExportResult;
import ru.pathcreator.vadim.quantum.application.transformation.QuantumProgramTransformer;
import ru.pathcreator.vadim.quantum.application.transformation.TransformationResult;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;
import ru.pathcreator.vadim.quantum.domain.validation.QuantumProgramValidator;
import ru.pathcreator.vadim.quantum.domain.validation.ValidationResult;

/**
 * Полный compiler pipeline от Quantum IR до target export.
 */
public final class QuantumCompiler {

    private final QuantumProgramValidator validator;
    private final QuantumProgramInspector inspector;
    private final CapabilityPreflightChecker preflightChecker;
    private final QuantumProgramTransformer transformer;

    public QuantumCompiler() {
        this(
            new QuantumProgramValidator(),
            new QuantumProgramInspector(),
            new CapabilityPreflightChecker(),
            new QuantumProgramTransformer()
        );
    }

    public QuantumCompiler(
        final QuantumProgramValidator validator,
        final QuantumProgramInspector inspector,
        final CapabilityPreflightChecker preflightChecker,
        final QuantumProgramTransformer transformer
    ) {
        if (validator == null) {
            throw new IllegalArgumentException("Quantum program validator must not be null.");
        }
        if (inspector == null) {
            throw new IllegalArgumentException("Quantum program inspector must not be null.");
        }
        if (preflightChecker == null) {
            throw new IllegalArgumentException("Capability preflight checker must not be null.");
        }
        if (transformer == null) {
            throw new IllegalArgumentException("Quantum program transformer must not be null.");
        }
        this.validator = validator;
        this.inspector = inspector;
        this.preflightChecker = preflightChecker;
        this.transformer = transformer;
    }

    public CompilerResult compile(
        final QuantumProgram program,
        final QuantumExporter exporter
    ) {
        return compile(
            program,
            exporter,
            CompilerOptions.defaults()
        );
    }

    public CompilerResult compile(
        final QuantumProgram program,
        final QuantumExporter exporter,
        final CompilerOptions options
    ) {
        if (program == null) {
            throw new IllegalArgumentException("Quantum program must not be null.");
        }
        if (exporter == null) {
            throw new IllegalArgumentException("Quantum exporter must not be null.");
        }
        if (options == null) {
            throw new IllegalArgumentException("Compiler options must not be null.");
        }
        final CompilerResult.Builder result = CompilerResult.builder(
            exporter.format(),
            program
        );
        final ValidationResult initialValidation = options.runInitialValidation()
            ? runInitialValidation(
                program,
                result
            )
            : skipValidation(
                result,
                CompilerStage.INITIAL_VALIDATION,
                "Initial validation was skipped by compiler options."
            );
        if (
            shouldStopAfterValidation(
                initialValidation,
                options
            )
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_VALIDATION
            );
        }
        final ProgramInspectionResult initialInspection = options.runInitialInspection()
            ? runInitialInspection(
                program,
                exporter,
                result
            )
            : skipInspection(
                result,
                CompilerStage.INITIAL_INSPECTION,
                "Initial inspection was skipped by compiler options."
            );
        if (
            options.stopOnWarnings()
            && initialInspection != null
            && initialInspection.diagnosticCount() > 0
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_WARNING
            );
        }
        final CapabilityPreflightResult initialPreflight = options.runInitialPreflight()
            ? runInitialPreflight(
                program,
                exporter,
                result
            )
            : skipPreflight(
                result,
                CompilerStage.INITIAL_PREFLIGHT,
                "Initial preflight was skipped by compiler options."
            );
        if (
            shouldStopAfterPreflight(
                initialPreflight,
                options
            )
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_PREFLIGHT
            );
        }
        final TransformationResult transformation = options.runTransformation()
            ? runTransformation(
                program,
                result,
                options
            )
            : skipTransformation(
                result,
                CompilerStage.TRANSFORMATION,
                "Transformation was skipped by compiler options."
            );
        if (
            options.stopOnTransformationError()
            && transformation != null
            && transformation.hasErrors()
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_TRANSFORMATION
            );
        }
        if (
            options.stopOnWarnings()
            && transformation != null
            && !transformation.diagnostics().isEmpty()
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_WARNING
            );
        }
        final QuantumProgram transformedProgram = transformation == null
            ? program
            : transformation.transformedProgram();
        result.transformedProgram(transformedProgram);
        final ValidationResult transformedValidation = options.runTransformedValidation()
            ? runTransformedValidation(
                transformedProgram,
                result
            )
            : skipValidation(
                result,
                CompilerStage.TRANSFORMED_VALIDATION,
                "Transformed validation was skipped by compiler options."
            );
        if (
            shouldStopAfterValidation(
                transformedValidation,
                options
            )
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_VALIDATION
            );
        }
        final ProgramInspectionResult transformedInspection = options.runTransformedInspection()
            ? runTransformedInspection(
                transformedProgram,
                exporter,
                result
            )
            : skipInspection(
                result,
                CompilerStage.TRANSFORMED_INSPECTION,
                "Transformed inspection was skipped by compiler options."
            );
        if (
            options.stopOnWarnings()
            && transformedInspection != null
            && transformedInspection.diagnosticCount() > 0
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_WARNING
            );
        }
        final CapabilityPreflightResult transformedPreflight = options.runTransformedPreflight()
            ? runTransformedPreflight(
                transformedProgram,
                exporter,
                result
            )
            : skipPreflight(
                result,
                CompilerStage.TRANSFORMED_PREFLIGHT,
                "Transformed preflight was skipped by compiler options."
            );
        if (
            shouldStopAfterPreflight(
                transformedPreflight,
                options
            )
        ) {
            return stop(
                result,
                CompilerResultStatus.STOPPED_ON_PREFLIGHT
            );
        }
        final ExportResult exportResult = runExport(
            transformedProgram,
            exporter,
            options,
            result
        );
        return result.status(exportResult.isSuccess()
                ? CompilerResultStatus.EXPORTED
                : CompilerResultStatus.EXPORT_FAILED)
            .build();
    }

    private ValidationResult runInitialValidation(
        final QuantumProgram program,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final ValidationResult validation = validator.validate(program);
        result.initialValidation(validation)
            .addStage(record(
                CompilerStage.INITIAL_VALIDATION,
                validation.isValid()
                    ? CompilerStageStatus.SUCCESS
                    : CompilerStageStatus.FAILED,
                started,
                validation.isValid()
                    ? "Initial validation passed."
                    : "Initial validation failed with " + validation.errorCount() + " error(s)."
            ));
        return validation;
    }

    private ProgramInspectionResult runInitialInspection(
        final QuantumProgram program,
        final QuantumExporter exporter,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final ProgramInspectionResult inspection = inspector.inspect(
            program,
            List.of(exporter.capabilityProfile())
        );
        result.initialInspection(inspection)
            .addStage(record(
                CompilerStage.INITIAL_INSPECTION,
                inspection.diagnosticCount() == 0
                    ? CompilerStageStatus.SUCCESS
                    : CompilerStageStatus.WARNING,
                started,
                "Initial inspection completed with " + inspection.diagnosticCount() + " diagnostic(s)."
            ));
        return inspection;
    }

    private CapabilityPreflightResult runInitialPreflight(
        final QuantumProgram program,
        final QuantumExporter exporter,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final CapabilityPreflightResult preflight = preflightChecker.check(
            program,
            exporter.capabilityProfile()
        );
        result.initialPreflight(preflight)
            .addStage(record(
                CompilerStage.INITIAL_PREFLIGHT,
                preflightStatus(preflight),
                started,
                "Initial preflight status is " + preflight.status() + "."
            ));
        return preflight;
    }

    private TransformationResult runTransformation(
        final QuantumProgram program,
        final CompilerResult.Builder result,
        final CompilerOptions options
    ) {
        final long started = System.nanoTime();
        final TransformationResult transformation = transformer.transform(
            program,
            options.transformationOptions()
        );
        result.transformation(transformation)
            .addStage(record(
                CompilerStage.TRANSFORMATION,
                transformation.hasErrors()
                    ? CompilerStageStatus.FAILED
                    : transformation.diagnostics().isEmpty()
                        ? CompilerStageStatus.SUCCESS
                        : CompilerStageStatus.WARNING,
                started,
                "Transformation completed with " + transformation.diagnostics().size() + " diagnostic(s)."
            ));
        return transformation;
    }

    private ValidationResult runTransformedValidation(
        final QuantumProgram program,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final ValidationResult validation = validator.validate(program);
        result.transformedValidation(validation)
            .addStage(record(
                CompilerStage.TRANSFORMED_VALIDATION,
                validation.isValid()
                    ? CompilerStageStatus.SUCCESS
                    : CompilerStageStatus.FAILED,
                started,
                validation.isValid()
                    ? "Transformed validation passed."
                    : "Transformed validation failed with " + validation.errorCount() + " error(s)."
            ));
        return validation;
    }

    private ProgramInspectionResult runTransformedInspection(
        final QuantumProgram program,
        final QuantumExporter exporter,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final ProgramInspectionResult inspection = inspector.inspect(
            program,
            List.of(exporter.capabilityProfile())
        );
        result.transformedInspection(inspection)
            .addStage(record(
                CompilerStage.TRANSFORMED_INSPECTION,
                inspection.diagnosticCount() == 0
                    ? CompilerStageStatus.SUCCESS
                    : CompilerStageStatus.WARNING,
                started,
                "Transformed inspection completed with " + inspection.diagnosticCount() + " diagnostic(s)."
            ));
        return inspection;
    }

    private CapabilityPreflightResult runTransformedPreflight(
        final QuantumProgram program,
        final QuantumExporter exporter,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final CapabilityPreflightResult preflight = preflightChecker.check(
            program,
            exporter.capabilityProfile()
        );
        result.transformedPreflight(preflight)
            .addStage(record(
                CompilerStage.TRANSFORMED_PREFLIGHT,
                preflightStatus(preflight),
                started,
                "Transformed preflight status is " + preflight.status() + "."
            ));
        return preflight;
    }

    private ExportResult runExport(
        final QuantumProgram program,
        final QuantumExporter exporter,
        final CompilerOptions options,
        final CompilerResult.Builder result
    ) {
        final long started = System.nanoTime();
        final ExportResult exportResult = exporter.exportProgram(
            program,
            options.exportOptions()
        );
        result.exportResult(exportResult)
            .addStage(record(
                CompilerStage.EXPORT,
                exportResult.isSuccess()
                    ? CompilerStageStatus.SUCCESS
                    : CompilerStageStatus.FAILED,
                started,
                exportResult.isSuccess()
                    ? "Export completed."
                    : "Export failed with " + exportResult.diagnosticCount() + " diagnostic(s)."
            ));
        return exportResult;
    }

    private static boolean shouldStopAfterValidation(
        final ValidationResult validation,
        final CompilerOptions options
    ) {
        return validation != null
            && options.stopOnValidationError()
            && !validation.isValid();
    }

    private static boolean shouldStopAfterPreflight(
        final CapabilityPreflightResult preflight,
        final CompilerOptions options
    ) {
        if (preflight == null || !options.stopOnUnsupportedTarget()) {
            return false;
        }
        return preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET
            || preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS;
    }

    private static CompilerStageStatus preflightStatus(final CapabilityPreflightResult preflight) {
        if (
            preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_BY_TARGET
            || preflight.status() == CapabilityPreflightStatus.UNSUPPORTED_WITHOUT_LOSS
        ) {
            return CompilerStageStatus.FAILED;
        }
        if (preflight.status() == CapabilityPreflightStatus.LOWERING_REQUIRED) {
            return CompilerStageStatus.WARNING;
        }
        return CompilerStageStatus.SUCCESS;
    }

    private static CompilerResult stop(
        final CompilerResult.Builder result,
        final CompilerResultStatus status
    ) {
        return result.status(status).build();
    }

    private static ValidationResult skipValidation(
        final CompilerResult.Builder result,
        final CompilerStage stage,
        final String message
    ) {
        skip(
            result,
            stage,
            message
        );
        return null;
    }

    private static ProgramInspectionResult skipInspection(
        final CompilerResult.Builder result,
        final CompilerStage stage,
        final String message
    ) {
        skip(
            result,
            stage,
            message
        );
        return null;
    }

    private static CapabilityPreflightResult skipPreflight(
        final CompilerResult.Builder result,
        final CompilerStage stage,
        final String message
    ) {
        skip(
            result,
            stage,
            message
        );
        return null;
    }

    private static TransformationResult skipTransformation(
        final CompilerResult.Builder result,
        final CompilerStage stage,
        final String message
    ) {
        skip(
            result,
            stage,
            message
        );
        return null;
    }

    private static void skip(
        final CompilerResult.Builder result,
        final CompilerStage stage,
        final String message
    ) {
        result.addStage(CompilerStageRecord.of(
            stage,
            CompilerStageStatus.SKIPPED,
            0L,
            message
        ));
    }

    private static CompilerStageRecord record(
        final CompilerStage stage,
        final CompilerStageStatus status,
        final long started,
        final String message
    ) {
        return CompilerStageRecord.of(
            stage,
            status,
            System.nanoTime() - started,
            message
        );
    }
}