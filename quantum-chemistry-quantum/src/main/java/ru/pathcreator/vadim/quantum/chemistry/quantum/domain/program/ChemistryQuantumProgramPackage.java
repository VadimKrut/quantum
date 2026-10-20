/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.quantum.domain.program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import ru.pathcreator.vadim.quantum.chemistry.fermion.domain.mapping.FermionToQubitMapping;
import ru.pathcreator.vadim.quantum.domain.model.QuantumProgram;

/**
 * ?????? ????? ?????????????? Quantum IR ???????? ? ?????? ???????? ?????????? ??? ????? chemistry-??????.
 */
public final class ChemistryQuantumProgramPackage {

  public static final ChemistryQuantumProgramPackage EMPTY =
      new ChemistryQuantumProgramPackage(
          "", null, 0, 0, ChemistryQuantumPreparationProvenance.UNKNOWN, List.of(), List.of());

  private final String sourceId;
  private final FermionToQubitMapping requestedMapping;
  private final int spinOrbitalCount;
  private final int electronCount;
  private final ChemistryQuantumPreparationProvenance provenance;
  private final List<ChemistryQuantumProgramArtifact> artifacts;
  private final List<ChemistryQuantumProgramSkip> skippedArtifacts;

  private ChemistryQuantumProgramPackage(
      final String sourceId,
      final FermionToQubitMapping requestedMapping,
      final int spinOrbitalCount,
      final int electronCount,
      final ChemistryQuantumPreparationProvenance provenance,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skippedArtifacts) {
    this.sourceId = sourceId;
    this.requestedMapping = requestedMapping;
    this.spinOrbitalCount = spinOrbitalCount;
    this.electronCount = electronCount;
    this.provenance = provenance;
    this.artifacts = artifacts;
    this.skippedArtifacts = skippedArtifacts;
  }

  public static ChemistryQuantumProgramPackage of(
      final String sourceId,
      final FermionToQubitMapping requestedMapping,
      final int spinOrbitalCount,
      final int electronCount,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skippedArtifacts) {
    return ChemistryQuantumProgramPackage.of(
        sourceId,
        requestedMapping,
        spinOrbitalCount,
        electronCount,
        ChemistryQuantumPreparationProvenance.UNKNOWN,
        artifacts,
        skippedArtifacts);
  }

  public static ChemistryQuantumProgramPackage of(
      final String sourceId,
      final FermionToQubitMapping requestedMapping,
      final int spinOrbitalCount,
      final int electronCount,
      final ChemistryQuantumPreparationProvenance provenance,
      final List<ChemistryQuantumProgramArtifact> artifacts,
      final List<ChemistryQuantumProgramSkip> skippedArtifacts) {
    if (sourceId == null || sourceId.isBlank()) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package source id must not be blank.");
    }
    if (requestedMapping == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package requested mapping must not be null.");
    }
    if (spinOrbitalCount <= 0) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package spin orbital count must be positive.");
    }
    if (electronCount < 0) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package electron count must not be negative.");
    }
    if (provenance == null) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package provenance must not be null.");
    }
    ChemistryQuantumProgramPackage.requireConsistentProvenance(
        requestedMapping, spinOrbitalCount, electronCount, provenance);
    final List<ChemistryQuantumProgramArtifact> checkedArtifacts =
        ChemistryQuantumProgramPackage.checkedArtifacts(artifacts);
    return new ChemistryQuantumProgramPackage(
        sourceId,
        requestedMapping,
        spinOrbitalCount,
        electronCount,
        provenance,
        checkedArtifacts,
        ChemistryQuantumProgramPackage.checkedSkips(skippedArtifacts));
  }

  public String sourceId() {
    return this.sourceId;
  }

  public FermionToQubitMapping requestedMapping() {
    return this.requestedMapping;
  }

  public int spinOrbitalCount() {
    return this.spinOrbitalCount;
  }

  public int electronCount() {
    return this.electronCount;
  }

  public ChemistryQuantumPreparationProvenance provenance() {
    return this.provenance;
  }

  public List<ChemistryQuantumProgramArtifact> artifacts() {
    return this.artifacts;
  }

  public List<ChemistryQuantumProgramSkip> skippedArtifacts() {
    return this.skippedArtifacts;
  }

  public int artifactCount() {
    return this.artifacts.size();
  }

  public int skippedArtifactCount() {
    return this.skippedArtifacts.size();
  }

  public boolean empty() {
    return this.artifacts.isEmpty() && this.skippedArtifacts.isEmpty();
  }

  public boolean hasArtifacts() {
    return !this.artifacts.isEmpty();
  }

  public boolean generationComplete() {
    return !this.artifacts.isEmpty() && this.skippedArtifacts.isEmpty();
  }

  public boolean hasArtifact(final ChemistryQuantumProgramKind kind) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    return this.artifactIndex(kind) >= 0;
  }

  public boolean hasSkip(final ChemistryQuantumProgramKind kind) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    return this.skipIndex(kind) >= 0;
  }

  public ChemistryQuantumProgramArtifact requireArtifact(final ChemistryQuantumProgramKind kind) {
    final int index = this.artifactIndex(kind);
    if (index < 0) {
      throw new IllegalStateException(
          "Chemistry quantum program package does not contain artifact " + kind + ".");
    }
    return this.artifacts.get(index);
  }

  public ChemistryQuantumProgramArtifact artifactOrNull(final ChemistryQuantumProgramKind kind) {
    final int index = this.artifactIndex(kind);
    return index < 0 ? null : this.artifacts.get(index);
  }

  public ChemistryQuantumProgramSkip skipOrNull(final ChemistryQuantumProgramKind kind) {
    final int index = this.skipIndex(kind);
    return index < 0 ? null : this.skippedArtifacts.get(index);
  }

  public String skipReasonOrEmpty(final ChemistryQuantumProgramKind kind) {
    final ChemistryQuantumProgramSkip skip = this.skipOrNull(kind);
    return skip == null ? "" : skip.reason();
  }

  public ChemistryQuantumProgramArtifact artifactOrNull(
      final ChemistryQuantumProgramKind kind, final FermionToQubitMapping mapping) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    for (int i = 0; i < this.artifacts.size(); ++i) {
      final ChemistryQuantumProgramArtifact artifact = this.artifacts.get(i);
      if (artifact.kind() == kind && artifact.mapping() == mapping) {
        return artifact;
      }
    }
    return null;
  }

  public boolean hasArtifact(
      final ChemistryQuantumProgramKind kind, final FermionToQubitMapping mapping) {
    return this.artifactOrNull(kind, mapping) != null;
  }

  public QuantumProgram primaryProgram() {
    if (this.artifacts.isEmpty()) {
      throw new IllegalStateException(
          "Chemistry quantum program package does not contain programs.");
    }
    return this.artifacts.get(0).program();
  }

  public List<String> artifactIds() {
    final ArrayList<String> ids = new ArrayList<String>(this.artifacts.size());
    for (int i = 0; i < this.artifacts.size(); ++i) {
      ids.add(this.artifacts.get(i).id());
    }
    return List.copyOf(ids);
  }

  public long totalOperationCount() {
    long count = 0L;
    for (int i = 0; i < this.artifacts.size(); ++i) {
      count += this.artifacts.get(i).operationCount();
    }
    return count;
  }

  public int artifactCount(final ChemistryQuantumProgramKind kind) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    int count = 0;
    for (int i = 0; i < this.artifacts.size(); ++i) {
      if (this.artifacts.get(i).kind() == kind) {
        ++count;
      }
    }
    return count;
  }

  public List<ChemistryQuantumProgramArtifact> artifacts(final ChemistryQuantumProgramKind kind) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    final ArrayList<ChemistryQuantumProgramArtifact> found =
        new ArrayList<ChemistryQuantumProgramArtifact>();
    for (int i = 0; i < this.artifacts.size(); ++i) {
      if (this.artifacts.get(i).kind() == kind) {
        found.add(this.artifacts.get(i));
      }
    }
    return List.copyOf(found);
  }

  private int artifactIndex(final ChemistryQuantumProgramKind kind) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    for (int i = 0; i < this.artifacts.size(); ++i) {
      if (this.artifacts.get(i).kind() == kind) {
        return i;
      }
    }
    return -1;
  }

  private int skipIndex(final ChemistryQuantumProgramKind kind) {
    ChemistryQuantumProgramPackage.requireKind(kind);
    for (int i = 0; i < this.skippedArtifacts.size(); ++i) {
      if (this.skippedArtifacts.get(i).kind() == kind) {
        return i;
      }
    }
    return -1;
  }

  private static List<ChemistryQuantumProgramArtifact> checkedArtifacts(
      final List<ChemistryQuantumProgramArtifact> artifacts) {
    if (artifacts == null) {
      return List.of();
    }
    for (int i = 0; i < artifacts.size(); ++i) {
      if (artifacts.get(i) == null) {
        throw new IllegalArgumentException("Chemistry quantum program artifact must not be null.");
      }
    }
    ChemistryQuantumProgramPackage.requireUniqueArtifactIds(artifacts);
    return List.copyOf(artifacts);
  }

  private static List<ChemistryQuantumProgramSkip> checkedSkips(
      final List<ChemistryQuantumProgramSkip> skippedArtifacts) {
    if (skippedArtifacts == null) {
      return List.of();
    }
    for (int i = 0; i < skippedArtifacts.size(); ++i) {
      if (skippedArtifacts.get(i) == null) {
        throw new IllegalArgumentException("Chemistry quantum program skip must not be null.");
      }
    }
    ChemistryQuantumProgramPackage.requireUniqueSkipIds(skippedArtifacts);
    return List.copyOf(skippedArtifacts);
  }

  private static void requireConsistentProvenance(
      final FermionToQubitMapping requestedMapping,
      final int spinOrbitalCount,
      final int electronCount,
      final ChemistryQuantumPreparationProvenance provenance) {
    if (!provenance.known()) {
      return;
    }
    if (provenance.electronCount() != electronCount) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package electron count must match known provenance.");
    }
    if (provenance.spinOrbitalCount() != spinOrbitalCount) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package spin orbital count must match known provenance.");
    }
    if (provenance.requestedMapping() != null
        && provenance.requestedMapping() != requestedMapping) {
      throw new IllegalArgumentException(
          "Chemistry quantum program package requested mapping must match known provenance.");
    }
  }

  private static void requireUniqueArtifactIds(
      final List<ChemistryQuantumProgramArtifact> artifacts) {
    final Set<String> ids = new HashSet<String>(artifacts.size());
    for (int i = 0; i < artifacts.size(); ++i) {
      if (!ids.add(artifacts.get(i).id())) {
        throw new IllegalArgumentException("Chemistry quantum program artifact id must be unique.");
      }
    }
  }

  private static void requireUniqueSkipIds(
      final List<ChemistryQuantumProgramSkip> skippedArtifacts) {
    final Set<String> ids = new HashSet<String>(skippedArtifacts.size());
    for (int i = 0; i < skippedArtifacts.size(); ++i) {
      if (!ids.add(skippedArtifacts.get(i).id())) {
        throw new IllegalArgumentException("Chemistry quantum program skip id must be unique.");
      }
    }
  }

  private static void requireKind(final ChemistryQuantumProgramKind kind) {
    if (kind == null) {
      throw new IllegalArgumentException("Chemistry quantum program kind must not be null.");
    }
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryQuantumProgramPackage)) {
      return false;
    }
    final ChemistryQuantumProgramPackage pack = (ChemistryQuantumProgramPackage) other;
    return this.spinOrbitalCount == pack.spinOrbitalCount
        && this.electronCount == pack.electronCount
        && Objects.equals(this.sourceId, pack.sourceId)
        && this.requestedMapping == pack.requestedMapping
        && Objects.equals(this.provenance, pack.provenance)
        && Objects.equals(this.artifacts, pack.artifacts)
        && Objects.equals(this.skippedArtifacts, pack.skippedArtifacts);
  }

  public int hashCode() {
    return Objects.hash(
        this.sourceId,
        this.requestedMapping,
        this.spinOrbitalCount,
        this.electronCount,
        this.provenance,
        this.artifacts,
        this.skippedArtifacts);
  }
}
