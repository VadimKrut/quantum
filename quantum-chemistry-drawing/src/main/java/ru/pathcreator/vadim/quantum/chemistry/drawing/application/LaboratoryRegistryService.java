/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.time.Instant;
import java.nio.file.Path;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.CdkCompoundIdentifiers;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereoIdentity;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Exact duplicates are reported, never silently merged; parent grouping remains explicit. */
public final class LaboratoryRegistryService {
  public record Matches(
      List<LaboratoryCompoundRegistry.Compound> exact,
      List<LaboratoryCompoundRegistry.Compound> unresolved) {
    public Matches {
      exact = List.copyOf(exact);
      unresolved = List.copyOf(unresolved);
    }
  }

  public record Registration(
      LaboratoryCompoundRegistry registry,
      LaboratoryCompoundRegistry.Compound compound,
      MoleculeDrawing drawing) {}

  public Matches matches(final LaboratoryCompoundRegistry registry, final Molecule molecule) {
    return matches(registry, molecule, null, Map.of());
  }

  public Matches matches(
      final LaboratoryCompoundRegistry registry,
      final MoleculeDrawing drawing,
      final Map<String, MoleculeDrawing> layouts) {
    return matches(registry, drawing.molecule(), drawing, layouts);
  }

  private Matches matches(
      final LaboratoryCompoundRegistry registry,
      final Molecule molecule,
      final MoleculeDrawing drawing,
      final Map<String, MoleculeDrawing> layouts) {
    final var exact = new ArrayList<LaboratoryCompoundRegistry.Compound>();
    final var unresolved = new ArrayList<LaboratoryCompoundRegistry.Compound>();
    for (final var compound : registry.compounds()) {
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException();
      }
      final var old = layouts.get(compound.id());
      final var result =
          drawing != null && old != null
              ? ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange
                  .CdkStereoIdentity.compare(drawing, old)
              : drawing != null && hasStereoDrawing(drawing)
                  ? MolecularGraphEquivalence.Result.UNRESOLVED
                  : compare(molecule, compound.molecule());
      if (result == MolecularGraphEquivalence.Result.EQUIVALENT) {
        exact.add(compound);
      } else if (result == MolecularGraphEquivalence.Result.UNRESOLVED) {
        unresolved.add(compound);
      }
    }
    return new Matches(exact, unresolved);
  }

  public Registration register(
      final LaboratoryCompoundRegistry registry,
      final Map<String, MoleculeDrawing> layouts,
      final MoleculeDrawing drawing,
      final String name,
      final List<String> aliases,
      final String source,
      final Instant at) {
    final var matches = matches(registry, drawing, layouts);
    if (!matches.exact().isEmpty()) {
      throw new IllegalArgumentException(
          "Такая точная структура уже зарегистрирована: "
              + matches.exact().getFirst().id()
              + ". Откройте её карточку и добавьте название или источник.");
    }
    if (registry.nextNumber() >= 10000000) {
      throw new IllegalArgumentException("Номера реестра закончились.");
    }
    final var independent = new MoleculeSketchService().independentCopy(drawing);
    final var copy =
        withMolecule(independent, MolecularRegistryReference.attach(independent.molecule(), null));
    final var compound =
        new LaboratoryCompoundRegistry.Compound(
            String.format(Locale.ROOT, "QC%07d", registry.nextNumber()),
            name,
            copy.molecule(),
            aliases,
            source,
            at);
    final var compounds = new ArrayList<>(registry.compounds());
    compounds.add(compound);
    return new Registration(
        new LaboratoryCompoundRegistry(registry.nextNumber() + 1, compounds, registry.relations()),
        compound,
        copy);
  }

  public record Names(List<String> values, String source) {
    public Names {
      values = List.copyOf(values);
    }
  }

  /** Returns saved names only when their stored graph/stereo evidence is still current. */
  public Names savedNames(final MoleculeDrawing drawing) {
    final var identity =
        MolecularIdentity.read(drawing.molecule())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Сначала сохраните подтверждённые названия в карточке вещества."));
    if (!new CdkCompoundIdentifiers().current(drawing, identity)) {
      throw new IllegalArgumentException(
          "Сохранённые названия относятся к прежней структуре. Проверьте карточку вещества заново.");
    }
    final var names = new ArrayList<String>();
    names.add(identity.commonName());
    names.add(identity.iupacName());
    names.addAll(identity.synonyms());
    final var values = distinctNames(names);
    if (values.isEmpty()) {
      throw new IllegalArgumentException(
          "В карточке вещества сохранены идентификаторы, но нет названий для переноса.");
    }
    return new Names(
        values,
        "Сохранённые названия · "
            + (identity.origin() == MolecularIdentity.Origin.PUBCHEM
                ? "PubChem"
                : "внесено вручную")
            + " · "
            + identity.recordedAt()
            + "\n"
            + identity.source());
  }

  /** The caller explicitly chooses this transfer; existing graph and source-card identity are checked first. */
  public MoleculeDrawing withCardNames(
      final Path file,
      final LaboratoryCompoundRegistry.Compound card,
      final MoleculeDrawing registered,
      final MoleculeDrawing participant,
      final Instant at) {
    if (registered == null || !registered.molecule().equals(card.molecule())) {
      throw new IllegalArgumentException("Структура карточки не загружена.");
    }
    requireExact(participant, registered);
    final String source =
        "Названия из карточки реестра "
            + card.id()
            + "\nРеестр: "
            + file.toAbsolutePath().normalize()
            + "\nКарточка зарегистрирована: "
            + card.registeredAt()
            + "\nИсточник карточки:\n"
            + card.source();
    return LaboratoryIdentityService.registryNames(
        participant, card.displayName(), card.aliases(), source, at);
  }

  /** Adds only reviewed names/provenance to an exact card; the graph, ID and registration date stay unchanged. */
  public LaboratoryCompoundRegistry mergeNames(
      final LaboratoryCompoundRegistry registry,
      final Map<String, MoleculeDrawing> layouts,
      final String id,
      final MoleculeDrawing source,
      final String preferred,
      final List<String> aliases,
      final String provenance) {
    final var old = registry.compound(id);
    requireExact(source, layouts.get(id));
    final var requested =
        new LaboratoryCompoundRegistry.Compound(
            old.id(), preferred, old.molecule(), aliases, provenance, old.registeredAt());
    final var names = new ArrayList<>(old.aliases());
    names.add(requested.displayName());
    names.addAll(requested.aliases());
    final var replacement =
        new LaboratoryCompoundRegistry.Compound(
            old.id(),
            old.displayName(),
            old.molecule(),
            distinctNames(names).stream()
                .filter(v -> !v.equalsIgnoreCase(old.displayName()))
                .toList(),
            appendSource(old.source(), requested.source()),
            old.registeredAt());
    return new LaboratoryCompoundRegistry(
        registry.nextNumber(),
        registry.compounds().stream().map(c -> c.id().equals(id) ? replacement : c).toList(),
        registry.relations());
  }

  public MoleculeDrawing linked(
      final Path file,
      final LaboratoryCompoundRegistry.Compound card,
      final MoleculeDrawing registered,
      final MoleculeDrawing participant) {
    if (registered == null || !registered.molecule().equals(card.molecule())) {
      throw new IllegalArgumentException("Структура карточки не загружена.");
    }
    requireExact(participant, registered);
    final var reference =
        new MolecularRegistryReference(
            file.toAbsolutePath().normalize().toString(),
            card.id(),
            card.registeredAt(),
            MolecularIdentity.signature(card.molecule()),
            MolecularIdentity.signature(participant.molecule()));
    if (MolecularRegistryReference.read(participant.molecule())
        .filter(reference::equals)
        .isPresent()) {
      return participant;
    }
    // Recorded samples/analyses can retain the previous molecule ID; metadata gets a new molecule
    // revision.
    final var revision = new MoleculeSketchService().independentCopy(participant);
    return withMolecule(
        revision, MolecularRegistryReference.attach(revision.molecule(), reference));
  }

  public boolean sameCard(
      final MolecularRegistryReference reference,
      final Path file,
      final LaboratoryCompoundRegistry.Compound card) {
    return Path.of(reference.registryPath()).equals(file.toAbsolutePath().normalize())
        && reference.compoundId().equals(card.id())
        && reference.registeredAt().equals(card.registeredAt())
        && reference.registrySignature().equals(MolecularIdentity.signature(card.molecule()));
  }

  public void requireExact(final MoleculeDrawing participant, final MoleculeDrawing registered) {
    if (registered == null) {
      throw new IllegalArgumentException("Расположение зарегистрированной структуры отсутствует.");
    }
    final var comparison = CdkStereoIdentity.compare(participant, registered);
    if (comparison != MolecularGraphEquivalence.Result.EQUIVALENT) {
      throw new IllegalArgumentException(
          comparison == MolecularGraphEquivalence.Result.UNRESOLVED
              ? "Точное соответствие структур не подтверждено. Неопределённое сравнение не позволяет связать или объединить записи."
              : "Структуры различаются. Выберите соответствующую карточку или зарегистрируйте новую структуру.");
    }
  }

  public static List<String> distinctNames(final Collection<String> values) {
    final var result = new LinkedHashMap<String, String>();
    for (final String value : values) {
      final String name = Objects.requireNonNullElse(value, "").strip();
      if (!name.isEmpty()) {
        result.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
      }
    }
    return List.copyOf(result.values());
  }

  public static String appendSource(final String old, final String added) {
    final String a = Objects.requireNonNullElse(old, "").strip(),
        b = Objects.requireNonNullElse(added, "").strip();
    return b.isEmpty() || a.equals(b) || a.endsWith("\n\n" + b)
        ? a
        : a.isEmpty() ? b : a + "\n\n" + b;
  }

  private static MoleculeDrawing withMolecule(
      final MoleculeDrawing drawing, final Molecule molecule) {
    return MoleculeDrawing.of(molecule, drawing.atoms(), drawing.bonds(), drawing.manualFields());
  }

  private static boolean hasStereoDrawing(final MoleculeDrawing drawing) {
    return !stereoStyles(drawing).isEmpty();
  }

  private static Set<String> stereoStyles(final MoleculeDrawing drawing) {
    final var styles = new HashSet<String>();
    for (final var bond : drawing.bonds()) {
      if (bond.style()
              == ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle
                  .WEDGE_UP
          || bond.style()
              == ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle
                  .WEDGE_DOWN
          || bond.style()
              == ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.BondDrawingStyle
                  .WAVY) {
        styles.add(
            bond.firstAtomId().value() + "/" + bond.secondAtomId().value() + "/" + bond.style());
      }
    }
    return styles;
  }

  public static MolecularGraphEquivalence.Result compare(
      final Molecule first, final Molecule second) {
    return MolecularGraphEquivalence.compare(first, second);
  }

  public static List<LaboratoryCompoundRegistry.Compound> search(
      final LaboratoryCompoundRegistry registry, final String query) {
    final String needle = Objects.requireNonNullElse(query, "").strip().toLowerCase(Locale.ROOT);
    return registry.compounds().stream()
        .filter(
            c ->
                (c.id()
                        + " "
                        + c.displayName()
                        + " "
                        + String.join(" ", c.aliases())
                        + " "
                        + c.source())
                    .toLowerCase(Locale.ROOT)
                    .contains(needle))
        .toList();
  }
}
