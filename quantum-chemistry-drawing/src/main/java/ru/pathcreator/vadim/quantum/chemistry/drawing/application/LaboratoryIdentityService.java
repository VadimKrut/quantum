/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.CdkCompoundIdentifiers;

/** Explicit name claims stay graph-bound; parsing and registry curation do not prove preferred IUPAC names. */
public final class LaboratoryIdentityService {
  private LaboratoryIdentityService() {}

  public static MoleculeDrawing enteredName(
      final MoleculeDrawing drawing, final String name, final String source) {
    final var identity =
        new MolecularIdentity(
            "",
            "",
            List.of(name),
            "",
            "",
            "",
            "",
            "",
            source,
            MolecularIdentity.Origin.MANUAL,
            Instant.now(),
            MolecularIdentity.signature(drawing.molecule()));
    return attach(drawing, identity);
  }

  public static MoleculeDrawing registryNames(
      final MoleculeDrawing drawing,
      final String preferred,
      final List<String> aliases,
      final String source,
      final Instant at) {
    final var old =
        MolecularIdentity.read(drawing.molecule())
            .filter(identity -> new CdkCompoundIdentifiers().current(drawing, identity))
            .orElse(null);
    final var names = new ArrayList<>(aliases);
    if (old != null) {
      names.add(old.commonName());
      names.addAll(old.synonyms());
    }
    final String iupac = old == null ? "" : old.iupacName();
    final var synonyms =
        LaboratoryRegistryService.distinctNames(names).stream()
            .filter(
                value ->
                    !value.equalsIgnoreCase(preferred.strip()) && !value.equalsIgnoreCase(iupac))
            .toList();
    final String provenance =
        old == null ? source : LaboratoryRegistryService.appendSource(old.source(), source);
    if (synonyms.size() > 100 || provenance.length() > 4000) {
      throw new IllegalArgumentException(
          "Полные названия или источник не помещаются в карточку вещества. Ничего не добавлено. Можно отключить перенос названий и добавить только структуру со ссылкой на реестр; исходная карточка сохранена полностью.");
    }
    final var identity =
        new MolecularIdentity(
            preferred,
            iupac,
            synonyms,
            old == null ? "" : old.cas(),
            old == null ? "" : old.smiles(),
            old == null ? "" : old.inchi(),
            old == null ? "" : old.inchiKey(),
            old == null ? "" : old.pubchemCid(),
            provenance,
            MolecularIdentity.Origin.MANUAL,
            at,
            MolecularIdentity.signature(drawing.molecule()));
    return attach(drawing, identity);
  }

  /** Keep entered names and their source without marking those manual claims as PubChem-verified. */
  public static MolecularIdentity preserveEnteredNames(
      final MoleculeDrawing drawing,
      final MolecularIdentity found,
      final String commonName,
      final List<String> enteredSynonyms,
      final String source) {
    Objects.requireNonNull(found, "found");
    if (found.origin() != MolecularIdentity.Origin.PUBCHEM
        || !new CdkCompoundIdentifiers().current(drawing, found)) {
      throw new IllegalArgumentException(
          "Название из PubChem не подтверждено для текущей структуры.");
    }
    if (found.iupacName().isBlank()) {
      throw new IllegalArgumentException(
          "В найденной карточке PubChem нет систематического названия IUPAC. Введённые сведения не изменены.");
    }
    final String entered = Objects.requireNonNullElse(commonName, "").strip();
    final String enteredSource = Objects.requireNonNullElse(source, "").strip();
    final var previousNames =
        LaboratoryRegistryService.distinctNames(
            Objects.requireNonNull(enteredSynonyms, "enteredSynonyms"));
    if (previousNames.size() > 100) {
      throw new IllegalArgumentException(
          "Сохраните не более 100 введённых синонимов. Результат поиска не заменил прежний ввод.");
    }
    final boolean sameNames =
        (entered.isBlank() || entered.equals(found.commonName()))
            && found.synonyms().containsAll(previousNames);
    if (sameNames && (enteredSource.isBlank() || enteredSource.equals(found.source()))) {
      return found;
    }
    final String preferred = entered.isBlank() ? found.commonName() : entered;
    final String provenance =
        LaboratoryRegistryService.appendSource(
            enteredSource.isBlank() ? "Сохранены названия, введённые автором." : enteredSource,
            "PubChem (IUPAC и идентификаторы):\n" + found.source());
    final List<String> aliases = new ArrayList<>(previousNames);
    if (!found.commonName().isBlank()) {
      aliases.add(found.commonName());
    }
    aliases.addAll(found.synonyms());
    final var synonyms =
        LaboratoryRegistryService.distinctNames(aliases).stream()
            .filter(name -> !name.equalsIgnoreCase(preferred))
            .limit(100)
            .toList();
    return new MolecularIdentity(
        preferred,
        found.iupacName(),
        synonyms,
        found.cas(),
        found.smiles(),
        found.inchi(),
        found.inchiKey(),
        found.pubchemCid(),
        provenance,
        MolecularIdentity.Origin.MANUAL,
        found.recordedAt(),
        found.structureSignature());
  }

  /** A supplied structural string is locally comparable even when its accompanying names are manual claims. */
  public static void requireConsistentSmiles(final MoleculeDrawing drawing, final String smiles) {
    if (smiles == null || smiles.isBlank()) {
      return;
    }
    final boolean matches;
    try {
      matches = new CdkCompoundIdentifiers().matches(drawing, smiles.strip());
    } catch (final RuntimeException failure) {
      throw new IllegalArgumentException(
          "SMILES не удалось проверить для текущей структуры: "
              + Objects.requireNonNullElse(failure.getMessage(), "неподдерживаемое представление")
              + ". Исправьте или очистите поле SMILES; ручные названия и источник можно сохранить отдельно.",
          failure);
    }
    if (!matches) {
      throw new IllegalArgumentException(
          "SMILES не соответствует текущему графу или заданной конфигурации. Исправьте или очистите поле SMILES; введённые названия и источник сохранены в форме.");
    }
  }

  private static MoleculeDrawing attach(
      final MoleculeDrawing drawing, final MolecularIdentity identity) {
    return MoleculeDrawing.of(
        MolecularIdentity.attach(drawing.molecule(), drawing.molecule().displayName(), identity),
        drawing.atoms(),
        drawing.bonds(),
        drawing.manualFields());
  }
}
