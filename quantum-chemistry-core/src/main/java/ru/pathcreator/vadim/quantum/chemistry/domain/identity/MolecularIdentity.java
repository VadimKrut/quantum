/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.identity;

import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Source-labelled names bound to a particular core graph; displayName remains a separate drawing caption. */
public record MolecularIdentity(
    String commonName,
    String iupacName,
    List<String> synonyms,
    String cas,
    String smiles,
    String inchi,
    String inchiKey,
    String pubchemCid,
    String source,
    Origin origin,
    Instant recordedAt,
    String structureSignature) {
  public enum Origin {
    MANUAL,
    PUBCHEM
  }

  private static final String PREFIX = "compound.identity.v1.";
  private static final Set<String> FIELDS =
      Set.of(
          "common",
          "iupac",
          "synonyms",
          "cas",
          "smiles",
          "inchi",
          "key",
          "cid",
          "source",
          "origin",
          "time",
          "signature");

  public MolecularIdentity {
    commonName = text(commonName, 2000);
    iupacName = text(iupacName, 8000);
    cas = text(cas, 32);
    smiles = text(smiles, 16000);
    inchi = text(inchi, 32000);
    inchiKey = text(inchiKey, 32);
    pubchemCid = text(pubchemCid, 20);
    source = text(source, 4000);
    synonyms =
        List.copyOf(
            new LinkedHashSet<>(
                Objects.requireNonNull(synonyms).stream()
                    .map(s -> text(s, 2000))
                    .filter(s -> !s.isBlank())
                    .toList()));
    if (synonyms.stream().anyMatch(value -> value.contains("\n"))) {
      throw new IllegalArgumentException("Один синоним должен занимать одну строку.");
    }
    if (synonyms.size() > 100) {
      throw new IllegalArgumentException("Сохраните не более 100 синонимов.");
    }
    if (!cas.isEmpty() && !validCas(cas)) {
      throw new IllegalArgumentException("Проверьте формат и контрольную цифру CAS.");
    }
    if (!inchi.isEmpty() && !inchi.startsWith("InChI=1")) {
      throw new IllegalArgumentException("InChI должен начинаться с InChI=1.");
    }
    if (!inchiKey.isEmpty() && !inchiKey.matches("[A-Z]{14}-[A-Z]{10}-[A-Z]")) {
      throw new IllegalArgumentException("Некорректный формат InChIKey.");
    }
    if (!pubchemCid.isEmpty() && !pubchemCid.matches("[1-9][0-9]{0,18}")) {
      throw new IllegalArgumentException("CID должен быть положительным целым числом.");
    }
    if (commonName.isEmpty()
        && iupacName.isEmpty()
        && synonyms.isEmpty()
        && cas.isEmpty()
        && smiles.isEmpty()
        && inchi.isEmpty()
        && inchiKey.isEmpty()) {
      throw new IllegalArgumentException("Введите название или идентификатор вещества.");
    }
    if (source.isBlank()) {
      throw new IllegalArgumentException("Укажите источник названий и идентификаторов.");
    }
    Objects.requireNonNull(origin);
    Objects.requireNonNull(recordedAt);
    if (structureSignature == null || !structureSignature.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Карточка должна быть связана с конкретной структурой.");
    }
    if (origin == Origin.PUBCHEM
        && (pubchemCid.isEmpty()
            || smiles.isEmpty()
            || !source.equals("https://pubchem.ncbi.nlm.nih.gov/compound/" + pubchemCid))) {
      throw new IllegalArgumentException(
          "Для записи PubChem требуются CID, проверенный SMILES и ссылка на запись.");
    }
  }

  private static String text(final String value, final int max) {
    final String s = Objects.requireNonNullElse(value, "").strip();
    if (s.length() > max
        || s.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
      throw new IllegalArgumentException(
          "Поле карточки слишком длинное или содержит управляющий символ.");
    }
    return s;
  }

  public static boolean validCas(final String s) {
    if (!s.matches("[1-9][0-9]{1,6}-[0-9]{2}-[0-9]")) {
      return false;
    }
    final String digits = s.replace("-", "");
    int sum = 0;
    for (int i = digits.length() - 2, weight = 1; i >= 0; i--, weight++) {
      sum += (digits.charAt(i) - '0') * weight;
    }
    return sum % 10 == digits.charAt(digits.length() - 1) - '0';
  }

  public static String signature(final Molecule molecule) {
    final String value =
        LaboratoryMaterialProperties.signature(molecule)
            + "\n"
            + molecule.metadata().attributes().getOrDefault(Stereochemistry.REVIEW_ATTRIBUTE, "")
            + relativeSignature(molecule);
    try {
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private static String relativeSignature(final Molecule molecule) {
    final var relative = MolecularRelativeStereo.read(molecule);
    if (relative.isEmpty()) {
      return "";
    }
    final List<String> groups = new ArrayList<>();
    for (final var group : relative.get().groups()) {
      final var members =
          group.members().stream().sorted(Comparator.comparing(m -> m.atom().value())).toList();
      final boolean invert =
          members.getFirst().descriptor() == MolecularRelativeStereo.Descriptor.S_STAR;
      final StringBuilder value = new StringBuilder(group.kind().name());
      for (final var member : members) {
        final int descriptor =
            member.descriptor() == MolecularRelativeStereo.Descriptor.UNKNOWN
                ? 0
                : (member.descriptor() == MolecularRelativeStereo.Descriptor.R_STAR) != invert
                    ? 1
                    : 2;
        value
            .append('|')
            .append(member.atom().value().length())
            .append(':')
            .append(member.atom().value())
            .append(':')
            .append(descriptor);
      }
      groups.add(value.toString());
    }
    Collections.sort(groups);
    return "\nrelative:" + String.join("\n", groups);
  }

  public boolean matches(final Molecule molecule) {
    return this.structureSignature.equals(signature(molecule));
  }

  public static Optional<MolecularIdentity> read(final Molecule molecule) {
    final Map<String, String> a = new LinkedHashMap<>();
    molecule
        .metadata()
        .attributes()
        .forEach(
            (k, v) -> {
              if (k.startsWith(PREFIX)) {
                a.put(k.substring(PREFIX.length()), v);
              }
            });
    if (a.isEmpty()) {
      return Optional.empty();
    }
    if (!FIELDS.containsAll(a.keySet())
        || !a.keySet().containsAll(Set.of("source", "origin", "time", "signature"))) {
      throw new IllegalArgumentException("Повреждён или неизвестен формат карточки вещества.");
    }
    try {
      return Optional.of(
          new MolecularIdentity(
              a.get("common"),
              a.get("iupac"),
              a.containsKey("synonyms")
                  ? Arrays.asList(a.get("synonyms").split("\n", -1))
                  : List.of(),
              a.get("cas"),
              a.get("smiles"),
              a.get("inchi"),
              a.get("key"),
              a.get("cid"),
              a.get("source"),
              Origin.valueOf(a.get("origin")),
              Instant.parse(a.get("time")),
              a.get("signature")));
    } catch (final RuntimeException e) {
      throw new IllegalArgumentException(
          "Не удалось прочитать карточку вещества: " + e.getMessage(), e);
    }
  }

  public static Molecule attach(
      final Molecule molecule, final String caption, final MolecularIdentity identity) {
    if (identity != null && !identity.matches(molecule)) {
      throw new IllegalArgumentException(
          "Карточка относится к другой структуре. Проверьте её заново.");
    }
    final Map<String, String> a = new LinkedHashMap<>(molecule.metadata().attributes());
    a.keySet().removeIf(k -> k.startsWith(PREFIX));
    if (identity != null) {
      final Map<String, String> values =
          Map.ofEntries(
              Map.entry("common", identity.commonName),
              Map.entry("iupac", identity.iupacName),
              Map.entry("synonyms", String.join("\n", identity.synonyms)),
              Map.entry("cas", identity.cas),
              Map.entry("smiles", identity.smiles),
              Map.entry("inchi", identity.inchi),
              Map.entry("key", identity.inchiKey),
              Map.entry("cid", identity.pubchemCid),
              Map.entry("source", identity.source),
              Map.entry("origin", identity.origin.name()),
              Map.entry("time", identity.recordedAt.toString()),
              Map.entry("signature", identity.structureSignature));
      values.forEach(
          (k, v) -> {
            if (!v.isEmpty()) {
              a.put(PREFIX + k, v);
            }
          });
    }
    return Molecule.of(
        molecule.id(),
        caption,
        molecule.atoms(),
        molecule.bonds(),
        molecule.charge(),
        molecule.spinMultiplicity(),
        molecule.stereochemistry(),
        molecule.conformation(),
        molecule.opticalRotation(),
        molecule.symmetry(),
        ChemistryMetadata.of(molecule.metadata().source(), molecule.metadata().location(), a));
  }

  public String searchableText() {
    return String.join(
        "\n",
        this.commonName,
        this.iupacName,
        String.join("\n", this.synonyms),
        this.cas,
        this.inchiKey,
        this.pubchemCid);
  }
}
