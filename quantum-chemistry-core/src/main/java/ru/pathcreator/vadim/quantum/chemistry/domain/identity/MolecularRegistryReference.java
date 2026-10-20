/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.identity;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** An exact local registry address and frozen card identity, independent of mutable names. */
public record MolecularRegistryReference(
    String registryPath,
    String compoundId,
    Instant registeredAt,
    String registrySignature,
    String structureSignature) {
  private static final String PREFIX = "compound.registry.v1.";
  private static final Set<String> FIELDS =
      Set.of("path", "id", "at", "registrySignature", "structureSignature");

  public MolecularRegistryReference {
    if (registryPath == null
        || registryPath.isBlank()
        || registryPath.length() > 16000
        || registryPath.codePoints().anyMatch(Character::isISOControl)
        || !Path.of(registryPath).isAbsolute()
        || !Path.of(registryPath).normalize().toString().equals(registryPath)) {
      throw new IllegalArgumentException(
          "Ссылка на реестр должна содержать полный нормализованный путь.");
    }
    if (compoundId == null || !compoundId.matches("QC[0-9]{7}")) {
      throw new IllegalArgumentException("Некорректный номер карточки реестра.");
    }
    Objects.requireNonNull(registeredAt);
    for (final String signature : Arrays.asList(registrySignature, structureSignature)) {
      if (signature == null || !signature.matches("[0-9a-f]{64}")) {
        throw new IllegalArgumentException("Ссылка реестра должна хранить подписи обеих структур.");
      }
    }
  }

  /** A core graph check, not a graph-isomorphism or drawing-stereo assertion. */
  public boolean matches(final Molecule molecule) {
    return structureSignature.equals(MolecularIdentity.signature(molecule));
  }

  public static Optional<MolecularRegistryReference> read(final Molecule molecule) {
    final var values = new HashMap<String, String>();
    molecule
        .metadata()
        .attributes()
        .forEach(
            (key, value) -> {
              if (key.startsWith(PREFIX)) {
                values.put(key.substring(PREFIX.length()), value);
              }
            });
    if (values.isEmpty()) {
      return Optional.empty();
    }
    if (!values.keySet().equals(FIELDS)) {
      throw new IllegalArgumentException("Повреждена или неизвестна ссылка на карточку реестра.");
    }
    try {
      return Optional.of(
          new MolecularRegistryReference(
              values.get("path"),
              values.get("id"),
              Instant.parse(values.get("at")),
              values.get("registrySignature"),
              values.get("structureSignature")));
    } catch (final RuntimeException exception) {
      throw new IllegalArgumentException(
          "Не удалось прочитать ссылку на реестр: " + exception.getMessage(), exception);
    }
  }

  public static Molecule attach(
      final Molecule molecule, final MolecularRegistryReference reference) {
    if (reference != null && !reference.matches(molecule)) {
      throw new IllegalArgumentException("Ссылка подтверждена для другой структуры участника.");
    }
    final var values = new LinkedHashMap<>(molecule.metadata().attributes());
    values.keySet().removeIf(key -> key.startsWith(PREFIX));
    if (reference != null) {
      values.put(PREFIX + "path", reference.registryPath);
      values.put(PREFIX + "id", reference.compoundId);
      values.put(PREFIX + "at", reference.registeredAt.toString());
      values.put(PREFIX + "registrySignature", reference.registrySignature);
      values.put(PREFIX + "structureSignature", reference.structureSignature);
    }
    return Molecule.of(
        molecule.id(),
        molecule.displayName(),
        molecule.atoms(),
        molecule.bonds(),
        molecule.charge(),
        molecule.spinMultiplicity(),
        molecule.stereochemistry(),
        molecule.conformation(),
        molecule.opticalRotation(),
        molecule.symmetry(),
        ChemistryMetadata.of(molecule.metadata().source(), molecule.metadata().location(), values));
  }
}
