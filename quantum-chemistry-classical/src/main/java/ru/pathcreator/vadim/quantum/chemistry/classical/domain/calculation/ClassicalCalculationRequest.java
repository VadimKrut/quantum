/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.experiment.ChemistrySubject;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ActiveSpace;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.BasisSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.method.ElectronicStructureMethod;

/**
 * Полная постановка классического расчета: объект химии, метод, basis, активное пространство,
 * задачи и настройки.
 */
public final class ClassicalCalculationRequest {

  private final ClassicalCalculationId id;
  private final ChemistrySubject subject;
  private final ElectronicStructureMethod method;
  private final BasisSet basisSet;
  private final ActiveSpace activeSpace;
  private final List<ClassicalCalculationKind> calculationKinds;
  private final List<ClassicalCalculationOption> options;
  private final ChemistryMetadata metadata;

  private ClassicalCalculationRequest(
      final ClassicalCalculationId id,
      final ChemistrySubject subject,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final List<ClassicalCalculationKind> calculationKinds,
      final List<ClassicalCalculationOption> options,
      final ChemistryMetadata metadata
  ) {
    this.id = id;
    this.subject = subject;
    this.method = method;
    this.basisSet = basisSet;
    this.activeSpace = activeSpace;
    this.calculationKinds = calculationKinds;
    this.options = options;
    this.metadata = metadata;
  }

  public static ClassicalCalculationRequest of(
      final ClassicalCalculationId id,
      final ChemistrySubject subject,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final List<ClassicalCalculationKind> calculationKinds
  ) {
    return ClassicalCalculationRequest.of(
        id,
        subject,
        method,
        basisSet,
        null,
        calculationKinds,
        List.of(),
        ChemistryMetadata.EMPTY);
  }

  public static ClassicalCalculationRequest of(
      final ClassicalCalculationId id,
      final ChemistrySubject subject,
      final ElectronicStructureMethod method,
      final BasisSet basisSet,
      final ActiveSpace activeSpace,
      final List<ClassicalCalculationKind> calculationKinds,
      final List<ClassicalCalculationOption> options,
      final ChemistryMetadata metadata
  ) {
    if (id == null) {
      throw new IllegalArgumentException("Classical calculation request id must not be null.");
    }
    if (subject == null) {
      throw new IllegalArgumentException("Classical calculation subject must not be null.");
    }
    if (method == null) {
      throw new IllegalArgumentException("Classical calculation method must not be null.");
    }
    if (basisSet == null) {
      throw new IllegalArgumentException("Classical calculation basis set must not be null.");
    }
    final List<ClassicalCalculationKind> checkedKinds =
        List.copyOf(ClassicalCalculationRequest.requireKinds(calculationKinds));
    final List<ClassicalCalculationOption> checkedOptions =
        List.copyOf(ClassicalCalculationRequest.requireOptions(options));
    final ChemistryMetadata checkedMetadata = metadata == null ? ChemistryMetadata.EMPTY : metadata;
    return new ClassicalCalculationRequest(
        id,
        subject,
        method,
        basisSet,
        activeSpace,
        checkedKinds,
        checkedOptions,
        checkedMetadata);
  }

  public ClassicalCalculationId id() {
    return this.id;
  }

  public ChemistrySubject subject() {
    return this.subject;
  }

  public ElectronicStructureMethod method() {
    return this.method;
  }

  public BasisSet basisSet() {
    return this.basisSet;
  }

  public ActiveSpace activeSpace() {
    return this.activeSpace;
  }

  public boolean hasActiveSpace() {
    return this.activeSpace != null;
  }

  public List<ClassicalCalculationKind> calculationKinds() {
    return this.calculationKinds;
  }

  public List<ClassicalCalculationOption> options() {
    return this.options;
  }

  public ChemistryMetadata metadata() {
    return this.metadata;
  }

  private static List<ClassicalCalculationKind> requireKinds(
      final List<ClassicalCalculationKind> kinds
  ) {
    if (kinds == null || kinds.isEmpty()) {
      throw new IllegalArgumentException("Classical calculation kind list must not be empty.");
    }
    final HashSet<ClassicalCalculationKind> uniqueKinds = new HashSet<ClassicalCalculationKind>();
    for (int i = 0; i < kinds.size(); ++i) {
      final ClassicalCalculationKind kind = kinds.get(i);
      if (kind == null) {
        throw new IllegalArgumentException("Classical calculation kind must not be null.");
      }
      if (uniqueKinds.add(kind)) {
        continue;
      }
      throw new IllegalArgumentException("Classical calculation kind list contains duplicate kind.");
    }
    return kinds;
  }

  private static List<ClassicalCalculationOption> requireOptions(
      final List<ClassicalCalculationOption> options
  ) {
    if (options == null) {
      return List.of();
    }
    final HashSet<String> optionNames = new HashSet<String>();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option == null) {
        throw new IllegalArgumentException("Classical calculation option must not be null.");
      }
      if (optionNames.add(option.name())) {
        continue;
      }
      throw new IllegalArgumentException("Classical calculation options contain duplicate name.");
    }
    return options;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ClassicalCalculationRequest)) {
      return false;
    }
    final ClassicalCalculationRequest request = (ClassicalCalculationRequest) other;
    return Objects.equals(this.id, request.id)
        && Objects.equals(this.subject, request.subject)
        && Objects.equals(this.method, request.method)
        && Objects.equals(this.basisSet, request.basisSet)
        && Objects.equals(this.activeSpace, request.activeSpace)
        && Objects.equals(this.calculationKinds, request.calculationKinds)
        && Objects.equals(this.options, request.options)
        && Objects.equals(this.metadata, request.metadata);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.id);
    result = ChemistryHash.include(result, this.subject);
    result = ChemistryHash.include(result, this.method);
    result = ChemistryHash.include(result, this.basisSet);
    result = ChemistryHash.include(result, this.activeSpace);
    result = ChemistryHash.include(result, this.calculationKinds);
    result = ChemistryHash.include(result, this.options);
    result = ChemistryHash.include(result, this.metadata);
    return result;
  }
}