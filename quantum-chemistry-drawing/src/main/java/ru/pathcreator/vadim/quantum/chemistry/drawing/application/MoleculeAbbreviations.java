/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Native manual-field adapter for a typed, strictly display-only abbreviation projection. */
public final class MoleculeAbbreviations {
  public static final String PREFIX = "display.abbreviation.v1.";
  public static final List<String> SUGGESTED_LABELS =
      List.of("Me", "Et", "iPr", "tBu", "Ph", "Bn", "Boc", "Cbz", "Ts", "Ac", "Fmoc", "TBS");

  public record Projection(
      Map<AtomId, MoleculeAbbreviation> anchors, Set<AtomId> hidden, List<String> diagnostics) {
    public Projection {
      anchors = Map.copyOf(anchors);
      hidden = Set.copyOf(hidden);
      diagnostics = List.copyOf(diagnostics);
    }
  }

  private static final Map<MoleculeDrawing, Projection> CACHE = new IdentityHashMap<>();
  private static final ArrayDeque<MoleculeDrawing> CACHE_ORDER = new ArrayDeque<>();

  private MoleculeAbbreviations() {}

  public static Projection projection(final MoleculeDrawing drawing) {
    synchronized (CACHE) {
      final var cached = CACHE.get(drawing);
      if (cached != null) {
        return cached;
      }
    }
    final var result = read(drawing);
    synchronized (CACHE) {
      if (!CACHE.containsKey(drawing)) {
        while (CACHE_ORDER.size() >= 64) {
          CACHE.remove(CACHE_ORDER.removeFirst());
        }
        CACHE_ORDER.addLast(drawing);
        CACHE.put(drawing, result);
      }
    }
    return result;
  }

  public static List<MoleculeAbbreviation> groups(final MoleculeDrawing drawing) {
    return projection(drawing).anchors().values().stream()
        .sorted(Comparator.comparing(MoleculeAbbreviation::id))
        .toList();
  }

  public static Set<AtomId> selection(final MoleculeDrawing drawing, final Set<AtomId> selected) {
    final var result = new LinkedHashSet<>(selected);
    for (final var group : groups(drawing)) {
      if (!Collections.disjoint(group.atoms(), selected)) {
        result.addAll(group.atoms());
      }
    }
    return Set.copyOf(result);
  }

  public static MoleculeDrawing collapse(
      final MoleculeDrawing drawing, final Set<AtomId> selected, final String label) {
    final var atoms = new LinkedHashSet<>(selection(drawing, selected));
    for (final var atom : drawing.molecule().atoms()) {
      if (!atom.element().symbol().value().equals("H")) {
        continue;
      }
      final var bonds =
          drawing.molecule().bonds().stream()
              .filter(b -> b.firstAtomId().equals(atom.id()) || b.secondAtomId().equals(atom.id()))
              .toList();
      if (bonds.size() == 1
          && bonds.getFirst().type() == BondType.SINGLE
          && (atoms.contains(bonds.getFirst().firstAtomId())
              || atoms.contains(bonds.getFirst().secondAtomId()))) {
        atoms.add(atom.id());
      }
    }
    final AtomId anchor = validate(drawing, atoms);
    final var previous = groups(drawing);
    final var retained =
        previous.stream().filter(g -> Collections.disjoint(g.atoms(), atoms)).toList();
    if (retained.size() >= 64) {
      throw new IllegalArgumentException("В одной структуре можно свернуть до 64 групп.");
    }
    final var group =
        new MoleculeAbbreviation(
            "group_" + UUID.randomUUID(),
            label,
            anchor,
            atoms.stream().sorted(Comparator.comparing(AtomId::value)).toList(),
            signature(drawing));
    final var manual =
        new ArrayList<>(
            drawing.manualFields().stream().filter(f -> !f.key().startsWith(PREFIX)).toList());
    for (final var old : retained) {
      manual.add(field(old));
    }
    manual.add(field(group));
    return MoleculeDrawing.of(drawing.molecule(), drawing.atoms(), drawing.bonds(), manual);
  }

  public static MoleculeDrawing expand(final MoleculeDrawing drawing, final AtomId anchor) {
    final var group = projection(drawing).anchors().get(anchor);
    if (group == null) {
      throw new IllegalArgumentException("Выберите подпись свёрнутой группы.");
    }
    return MoleculeDrawing.of(
        drawing.molecule(),
        drawing.atoms(),
        drawing.bonds(),
        drawing.manualFields().stream().filter(f -> !f.key().equals(PREFIX + group.id())).toList());
  }

  public static MoleculeDrawing expandAll(final MoleculeDrawing drawing) {
    return MoleculeDrawing.of(
        drawing.molecule(),
        drawing.atoms(),
        drawing.bonds(),
        drawing.manualFields().stream().filter(f -> !f.key().startsWith(PREFIX)).toList());
  }

  private static Projection read(final MoleculeDrawing drawing) {
    final var records =
        drawing.manualFields().stream().filter(f -> f.key().startsWith(PREFIX)).toList();
    if (records.isEmpty()) {
      return new Projection(Map.of(), Set.of(), List.of());
    }
    if (records.size() > 64) {
      return new Projection(
          Map.of(), Set.of(), List.of("Слишком много сокращённых групп: показан полный граф."));
    }
    final String signature = signature(drawing);
    final var anchors = new LinkedHashMap<AtomId, MoleculeAbbreviation>();
    final var used = new HashSet<AtomId>();
    final var hidden = new HashSet<AtomId>();
    final var diagnostics = new ArrayList<String>();
    for (final var record : records) {
      try {
        if (record.feature() != ChemistryDrawingFeature.MOLECULE_STRUCTURE
            || record.value().length() > 150000) {
          throw new IllegalArgumentException("Неверное поле отображения группы.");
        }
        final String[] parts = record.value().split("\\|", -1);
        if (parts.length != 4) {
          throw new IllegalArgumentException("Неверная версия сокращённой группы.");
        }
        final var group =
            new MoleculeAbbreviation(
                record.key().substring(PREFIX.length()),
                decode(parts[0]),
                AtomId.of(decode(parts[1])),
                Arrays.stream(parts[3].split(",", -1))
                    .map(MoleculeAbbreviations::decode)
                    .map(AtomId::of)
                    .toList(),
                parts[2]);
        if (!signature.equals(group.structureSignature())) {
          throw new IllegalArgumentException(
              "После изменения графа сокращение «"
                  + group.label()
                  + "» раскрыто; сверьте и сверните заново.");
        }
        if (!Collections.disjoint(used, group.atoms())
            || !validate(drawing, Set.copyOf(group.atoms())).equals(group.anchor())) {
          throw new IllegalArgumentException("Пересекающиеся или повреждённые группы раскрыты.");
        }
        used.addAll(group.atoms());
        hidden.addAll(group.atoms());
        hidden.remove(group.anchor());
        anchors.put(group.anchor(), group);
      } catch (final IllegalArgumentException exception) {
        diagnostics.add(Objects.toString(exception.getMessage(), "Неверная группа раскрыта."));
      }
    }
    return new Projection(anchors, hidden, diagnostics);
  }

  private static AtomId validate(final MoleculeDrawing drawing, final Set<AtomId> selected) {
    if (selected.isEmpty() || selected.size() > 500) {
      throw new IllegalArgumentException("Выделите связный фрагмент до 500 атомов.");
    }
    final var molecule = drawing.molecule();
    final var atoms =
        molecule.atoms().stream().collect(java.util.stream.Collectors.toMap(Atom::id, a -> a));
    if (!atoms.keySet().containsAll(selected)) {
      throw new IllegalArgumentException("Выделение содержит отсутствующие атомы.");
    }
    for (final var id : selected) {
      final var atom = atoms.get(id);
      if (atom.hasIsotope() || atom.formalCharge().value() != 0 || atom.radicalState().radical()) {
        throw new IllegalArgumentException(
            "Изотоп, заряд или радикал внутри группы должен оставаться явно видимым.");
      }
    }
    if (ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo.read(molecule)
        .map(r -> !Collections.disjoint(r.atoms(), selected))
        .orElse(false)) {
      throw new IllegalArgumentException(
          "Центр относительной / рацемической группы должен оставаться видимым.");
    }
    if (molecule.stereochemistry().centers().stream()
        .anyMatch(
            c ->
                selected.contains(c.primaryAtomId())
                    || c.hasSecondaryAtom() && selected.contains(c.secondaryAtomId()))) {
      throw new IllegalArgumentException("Стереоцентр внутри выбранного фрагмента оставьте явным.");
    }
    if (drawing.bonds().stream()
        .anyMatch(
            b ->
                selected.contains(b.firstAtomId())
                    && selected.contains(b.secondAtomId())
                    && b.style() != BondDrawingStyle.PLAIN)) {
      throw new IllegalArgumentException(
          "Связь с клином, штрихом или особым стилем внутри группы должна оставаться видимой.");
    }
    final var boundary =
        molecule.bonds().stream()
            .filter(b -> selected.contains(b.firstAtomId()) != selected.contains(b.secondAtomId()))
            .toList();
    if (boundary.size() != 1 || boundary.getFirst().type() != BondType.SINGLE) {
      throw new IllegalArgumentException(
          "Сокращение требует ровно одной внешней одинарной связи. Выделите всю терминальную группу.");
    }
    final var first = boundary.getFirst();
    final AtomId anchor =
        selected.contains(first.firstAtomId()) ? first.firstAtomId() : first.secondAtomId();
    if (atoms.get(anchor).element().symbol().value().equals("H")) {
      throw new IllegalArgumentException("Точка сокращённой группы должна быть тяжёлым атомом.");
    }
    final var seen = new HashSet<AtomId>();
    final var queue = new ArrayDeque<AtomId>();
    queue.add(anchor);
    while (!queue.isEmpty()) {
      final var id = queue.removeFirst();
      if (!seen.add(id)) {
        continue;
      }
      for (final var bond : molecule.bonds()) {
        final AtomId other =
            bond.firstAtomId().equals(id)
                ? bond.secondAtomId()
                : bond.secondAtomId().equals(id) ? bond.firstAtomId() : null;
        if (other != null && selected.contains(other) && !seen.contains(other)) {
          queue.add(other);
        }
      }
    }
    if (!seen.equals(selected)) {
      throw new IllegalArgumentException(
          "Выделенные атомы должны образовывать одну связанную группу.");
    }
    return anchor;
  }

  private static ManualDrawingField field(final MoleculeAbbreviation group) {
    return ManualDrawingField.of(
        ChemistryDrawingFeature.MOLECULE_STRUCTURE,
        PREFIX + group.id(),
        encode(group.label())
            + "|"
            + encode(group.anchor().value())
            + "|"
            + group.structureSignature()
            + "|"
            + group.atoms().stream()
                .map(a -> encode(a.value()))
                .collect(java.util.stream.Collectors.joining(",")));
  }

  private static String signature(final MoleculeDrawing drawing) {
    final String styles =
        drawing.bonds().stream()
            .filter(b -> b.style() != BondDrawingStyle.PLAIN)
            .map(b -> b.firstAtomId().value() + "/" + b.secondAtomId().value() + "/" + b.style())
            .sorted()
            .collect(java.util.stream.Collectors.joining(";"));
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(
                      (LaboratoryMaterialProperties.signature(drawing.molecule()) + "/" + styles)
                          .getBytes(StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static String encode(final String text) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(text.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(final String value) {
    if (value.length() > 1000) {
      throw new IllegalArgumentException("Поле группы слишком длинное.");
    }
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .decode(java.nio.ByteBuffer.wrap(Base64.getUrlDecoder().decode(value)))
          .toString();
    } catch (final java.nio.charset.CharacterCodingException exception) {
      throw new IllegalArgumentException("Неверный UTF-8 группы.", exception);
    }
  }
}
