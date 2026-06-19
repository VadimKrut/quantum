/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.storage.domain.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.TextValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.Reaction;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/**
 * Полный документ хранения химического проекта: молекулы, реакции, metadata и расширения.
 */
public final class ChemistryStorageDocument {

  private final ChemistryStorageFormatVersion version;
  private final String projectId;
  private final String displayName;
  private final Map<String, String> metadata;
  private final List<Molecule> molecules;
  private final List<Reaction> reactions;
  private final List<ChemistryStorageExtension> extensions;

  private ChemistryStorageDocument(
      final ChemistryStorageFormatVersion version,
      final String projectId,
      final String displayName,
      final Map<String, String> metadata,
      final List<Molecule> molecules,
      final List<Reaction> reactions,
      final List<ChemistryStorageExtension> extensions) {
    this.version = version;
    this.projectId = projectId;
    this.displayName = displayName;
    this.metadata = metadata;
    this.molecules = molecules;
    this.reactions = reactions;
    this.extensions = extensions;
  }

  public static Builder builder(
      final String projectId,
      final String displayName) {
    return new Builder(projectId, displayName);
  }

  public ChemistryStorageFormatVersion version() {
    return this.version;
  }

  public String projectId() {
    return this.projectId;
  }

  public String displayName() {
    return this.displayName;
  }

  public Map<String, String> metadata() {
    return this.metadata;
  }

  public List<Molecule> molecules() {
    return this.molecules;
  }

  public List<Reaction> reactions() {
    return this.reactions;
  }

  public List<ChemistryStorageExtension> extensions() {
    return this.extensions;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryStorageDocument)) {
      return false;
    }
    final ChemistryStorageDocument document = (ChemistryStorageDocument) other;
    return Objects.equals(this.version, document.version)
        && Objects.equals(this.projectId, document.projectId)
        && Objects.equals(this.displayName, document.displayName)
        && Objects.equals(this.metadata, document.metadata)
        && Objects.equals(this.molecules, document.molecules)
        && Objects.equals(this.reactions, document.reactions)
        && Objects.equals(this.extensions, document.extensions);
  }

  public int hashCode() {
    return Objects.hash(
        this.version,
        this.projectId,
        this.displayName,
        this.metadata,
        this.molecules,
        this.reactions,
        this.extensions);
  }

  /**
   * Mutable builder для удобной сборки immutable storage document.
   */
  public static final class Builder {

    private final String projectId;
    private final String displayName;
    private ChemistryStorageFormatVersion version = ChemistryStorageFormatVersion.CURRENT;
    private final LinkedHashMap<String, String> metadata = new LinkedHashMap<String, String>();
    private final ArrayList<Molecule> molecules = new ArrayList<Molecule>();
    private final ArrayList<Reaction> reactions = new ArrayList<Reaction>();
    private final ArrayList<ChemistryStorageExtension> extensions =
        new ArrayList<ChemistryStorageExtension>();

    private Builder(
        final String projectId,
        final String displayName) {
      this.projectId = IdentifierValue.requireIdentifier(projectId, "Storage project id");
      this.displayName = TextValue.requireText(displayName, "Storage project display name");
    }

    public Builder version(final ChemistryStorageFormatVersion version) {
      if (version == null) {
        throw new IllegalArgumentException("Storage format version must not be null.");
      }
      this.version = version;
      return this;
    }

    public Builder metadata(
        final String key,
        final String value) {
      this.metadata.put(
          IdentifierValue.requireIdentifier(key, "Storage metadata key"),
          TextValue.requireText(value, "Storage metadata value"));
      return this;
    }

    public Builder molecule(final Molecule molecule) {
      if (molecule == null) {
        throw new IllegalArgumentException("Storage molecule must not be null.");
      }
      this.molecules.add(molecule);
      return this;
    }

    public Builder reaction(final Reaction reaction) {
      if (reaction == null) {
        throw new IllegalArgumentException("Storage reaction must not be null.");
      }
      this.reactions.add(reaction);
      return this;
    }

    public Builder extension(final ChemistryStorageExtension extension) {
      if (extension == null) {
        throw new IllegalArgumentException("Storage extension must not be null.");
      }
      this.extensions.add(extension);
      return this;
    }

    public ChemistryStorageDocument build() {
      ChemistryStorageDocumentValidator.requireUniqueMolecules(this.molecules);
      ChemistryStorageDocumentValidator.requireUniqueReactions(this.reactions);
      ChemistryStorageDocumentValidator.requireReactionMoleculesPresent(
          this.reactions,
          this.molecules);
      return new ChemistryStorageDocument(
          this.version,
          this.projectId,
          this.displayName,
          Map.copyOf(this.metadata),
          List.copyOf(this.molecules),
          List.copyOf(this.reactions),
          List.copyOf(this.extensions));
    }
  }
}