/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity.OpsinChemicalName;

/** Curated named graph seeds, interpreted locally; topology and atom substitution remain explicit. */
public final class LaboratoryScaffolds {
  public enum Family {
    FUSED("Конденсированные"),
    BRIDGED("Мостиковые"),
    SPIRO("Спироциклы");
    private final String title;

    Family(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public record Scaffold(String id, String title, String chemicalName, Family family) {
    @Override
    public String toString() {
      return title;
    }
  }

  public record Prepared(MoleculeDrawing drawing, List<String> notes) {
    public Prepared {
      notes = List.copyOf(notes);
    }
  }

  public static final List<Scaffold> PALETTE =
      List.of(
          new Scaffold("indole", "Индол", "indole", Family.FUSED),
          new Scaffold("indazole", "Индазол", "1H-indazole", Family.FUSED),
          new Scaffold("benzimidazole", "Бензимидазол", "1H-benzimidazole", Family.FUSED),
          new Scaffold("benzofuran", "Бензофуран", "benzofuran", Family.FUSED),
          new Scaffold("benzothiophene", "Бензотиофен", "benzothiophene", Family.FUSED),
          new Scaffold("quinoline", "Хинолин", "quinoline", Family.FUSED),
          new Scaffold("isoquinoline", "Изохинолин", "isoquinoline", Family.FUSED),
          new Scaffold("quinazoline", "Хиназолин", "quinazoline", Family.FUSED),
          new Scaffold("quinoxaline", "Хиноксалин", "quinoxaline", Family.FUSED),
          new Scaffold("purine", "Пурин", "9H-purine", Family.FUSED),
          new Scaffold("pteridine", "Птеридин", "pteridine", Family.FUSED),
          new Scaffold(
              "imidazopyridine", "Имидазо[1,2-a]пиридин", "imidazo[1,2-a]pyridine", Family.FUSED),
          new Scaffold(
              "pyrazolopyrimidine",
              "Пиразоло[1,5-a]пиримидин",
              "pyrazolo[1,5-a]pyrimidine",
              Family.FUSED),
          new Scaffold("naphthyridine", "1,8-Нафтиридин", "1,8-naphthyridine", Family.FUSED),
          new Scaffold(
              "aza_norbornane",
              "7-Азабицикло[2.2.1]гептан",
              "7-azabicyclo[2.2.1]heptane",
              Family.BRIDGED),
          new Scaffold("quinuclidine", "Хинуклидин", "1-azabicyclo[2.2.2]octane", Family.BRIDGED),
          new Scaffold(
              "tropane_parent",
              "8-Азабицикло[3.2.1]октан",
              "8-azabicyclo[3.2.1]octane",
              Family.BRIDGED),
          new Scaffold(
              "diaza_bridge",
              "3,8-Диазабицикло[3.2.1]октан",
              "3,8-diazabicyclo[3.2.1]octane",
              Family.BRIDGED),
          new Scaffold(
              "oxa_bridge",
              "2-Оксабицикло[2.2.1]гептан",
              "2-oxabicyclo[2.2.1]heptane",
              Family.BRIDGED),
          new Scaffold(
              "diazaspiro45",
              "2,8-Диазаспиро[4.5]декан",
              "2,8-diazaspiro[4.5]decane",
              Family.SPIRO),
          new Scaffold(
              "oxazaspiro45",
              "1-Окса-8-азаспиро[4.5]декан",
              "1-oxa-8-azaspiro[4.5]decane",
              Family.SPIRO),
          new Scaffold("azaspiro35", "2-Азаспиро[3.5]нонан", "2-azaspiro[3.5]nonane", Family.SPIRO),
          new Scaffold(
              "diazaspiro33",
              "2,6-Диазаспиро[3.3]гептан",
              "2,6-diazaspiro[3.3]heptane",
              Family.SPIRO),
          new Scaffold(
              "dioxaspiro44",
              "1,4-Диоксаспиро[4.4]нонан",
              "1,4-dioxaspiro[4.4]nonane",
              Family.SPIRO));

  private LaboratoryScaffolds() {}

  public static Prepared prepare(final Scaffold scaffold) {
    if (!PALETTE.contains(scaffold)) {
      throw new IllegalArgumentException("Выберите каркас из палитры.");
    }
    final var parsed = new OpsinChemicalName().parse(scaffold.chemicalName());
    final var notes = new ArrayList<>(parsed.notes());
    notes.add(
        "Каркас: "
            + scaffold.title()
            + ". Неуказанная стереохимия остаётся неуказанной; энантиомер или диастереомер автоматически не выбран.");
    return new Prepared(
        new MoleculeSketchService().rename(parsed.drawing(), scaffold.title()), notes);
  }

  public static Prepared attach(
      final MoleculeDrawing target,
      final AtomId targetAtom,
      final MoleculeDrawing scaffold,
      final AtomId scaffoldAtom,
      final boolean replaceTargetHydrogen,
      final boolean replaceScaffoldHydrogen,
      final double direction) {
    final var notes = new ArrayList<String>();
    final var first = replaceTargetHydrogen ? replaceHydrogen(target, targetAtom) : target;
    final var second = replaceScaffoldHydrogen ? replaceHydrogen(scaffold, scaffoldAtom) : scaffold;
    if (replaceTargetHydrogen) {
      notes.add(
          "В исходной структуре явно заменён один обычный терминальный H у выбранного атома.");
    }
    if (replaceScaffoldHydrogen) {
      notes.add("В каркасе явно заменён один обычный терминальный H у выбранного атома.");
    }
    final var drawing =
        MoleculeFragmentPlacement.attach(
            first, targetAtom, second, scaffoldAtom, BondType.SINGLE, direction);
    final var errors =
        new ru.pathcreator.vadim.quantum.chemistry.domain.validation.ChemistryCoreValidator()
            .validateMolecule(drawing.molecule()).diagnostics().stream()
                .filter(
                    d ->
                        d.severity()
                                == ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic
                                    .ChemistryDiagnosticSeverity.ERROR
                            || d.code()
                                == ru.pathcreator.vadim.quantum.chemistry.domain.diagnostic
                                    .ChemistryDiagnosticCode.MOLECULE_VALENCE_EXCEEDS_PROFILE)
                .toList();
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(
          "Присоединение нарушает проверку графа или типичную валентность. Выберите другую точку / замену явного H. "
              + errors.getFirst().message());
    }
    if (MoleculeFragmentPlacement.overlaps(first, drawing)) {
      notes.add(
          "Фрагменты перекрываются на рисунке. Измените направление присоединения или выполните 2D-раскладку после добавления.");
    }
    notes.add(
        "Добавляется одна явная одинарная связь. Химическая осуществимость такой структуры этим действием не подтверждается.");
    return new Prepared(drawing, notes);
  }

  public static boolean hasReplaceableHydrogen(final MoleculeDrawing drawing, final AtomId atom) {
    return hydrogen(drawing, atom) != null;
  }

  private static MoleculeDrawing replaceHydrogen(final MoleculeDrawing drawing, final AtomId atom) {
    if (drawing.molecule().stereochemistry().centers().stream()
        .anyMatch(c -> c.primaryAtomId().equals(atom) || atom.equals(c.secondaryAtomId()))) {
      throw new IllegalArgumentException(
          "Замещение у заданного стереоцентра выполните в редакторе со сверкой конфигурации.");
    }
    final AtomId hydrogen = hydrogen(drawing, atom);
    if (hydrogen == null) {
      throw new IllegalArgumentException(
          "У выбранного атома нет обычного терминального H для замены. Выберите другую точку или отключите замену H, если граф уже содержит свободную валентность.");
    }
    return new MoleculeSketchService()
        .removeAtom(MoleculeAbbreviations.expandAll(drawing), hydrogen);
  }

  private static AtomId hydrogen(final MoleculeDrawing drawing, final AtomId parent) {
    for (final var atom : drawing.molecule().atoms()) {
      if (!atom.element().symbol().value().equals("H")
          || atom.hasIsotope()
          || atom.formalCharge().value() != 0
          || atom.radicalState().radical()) {
        continue;
      }
      final var bonds =
          drawing.molecule().bonds().stream()
              .filter(b -> b.firstAtomId().equals(atom.id()) || b.secondAtomId().equals(atom.id()))
              .toList();
      if (bonds.size() == 1
          && bonds.getFirst().type() == BondType.SINGLE
          && bonds.getFirst().connects(atom.id(), parent)
          && drawing.bonds().stream()
              .anyMatch(
                  b -> b.connects(atom.id(), parent) && b.style() == BondDrawingStyle.PLAIN)) {
        return atom.id();
      }
    }
    return null;
  }
}
