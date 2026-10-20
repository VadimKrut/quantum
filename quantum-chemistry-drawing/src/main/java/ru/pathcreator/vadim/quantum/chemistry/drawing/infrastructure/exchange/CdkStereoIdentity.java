/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.util.*;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.stereo.DoubleBondStereochemistry;
import org.openscience.cdk.stereo.TetrahedralChirality;
import com.simolecule.centres.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraphEquivalence;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Identity comparison checks depiction and declarations separately; inconsistent evidence is unresolved. */
public final class CdkStereoIdentity {
  private CdkStereoIdentity() {}

  /** Disposable absolute stereo graph for exact public-identity lookup; never invents E/Z from ordinary layout. */
  public static IAtomContainer identityGraph(final MoleculeDrawing drawing) {
    final Molecule m = drawing.molecule();
    if (MolecularRelativeStereo.present(m)) {
      throw new IllegalArgumentException(
          "Для структуры заданы относительные / рацемические группы или неизвестная абсолютная конфигурация. PubChem не подтверждает соответствие этих групп по обычному SMILES; автоматическая привязка названия отключена. Сохраните назначение в QCHEM и укажите имя с источником вручную.");
    }
    if (m.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)) {
      throw new IllegalArgumentException(
          "Стереохимия изменена и требует проверки перед поиском названия.");
    }
    if (drawing.bonds().stream()
        .anyMatch(
            b -> b.style() == BondDrawingStyle.WAVY || b.style() == BondDrawingStyle.UNKNOWN)) {
      throw new IllegalArgumentException(
          "Неопределённая стереосвязь не позволяет подтвердить точное имя. Уточните конфигурацию или введите имя с источником вручную.");
    }
    if (m.atomCount() > 256) {
      throw new IllegalArgumentException("Стереопроверка названия ограничена 256 явными атомами.");
    }
    try {
      final var attributes = new LinkedHashMap<>(m.metadata().attributes());
      attributes.remove(Stereochemistry.ORDERED_PARITY_ATTRIBUTE);
      attributes.remove("exchange.v1.signature");
      final var plainMolecule =
          Molecule.of(
              m.id(),
              m.displayName(),
              m.atoms(),
              m.bonds(),
              m.charge(),
              m.spinMultiplicity(),
              Stereochemistry.EMPTY,
              m.conformation(),
              m.opticalRotation(),
              m.symmetry(),
              ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), attributes));
      final var plain =
          MoleculeDrawing.of(
              plainMolecule,
              drawing.atoms(),
              drawing.bonds().stream()
                  .map(
                      b ->
                          BondDrawing.of(b.firstAtomId(), b.secondAtomId(), BondDrawingStyle.PLAIN))
                  .toList(),
              drawing.manualFields());
      final IAtomContainer stored = CdkStructureExchange.project(plain);
      final Map<AtomId, IAtom> atoms = new HashMap<>();
      for (int i = 0; i < m.atomCount(); i++) {
        atoms.put(m.atoms().get(i).id(), stored.getAtom(i));
      }
      final var parity = new HashMap<>(MolecularStereoParity.read(m));
      if (!parity.isEmpty()
          && !ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.signature(m)
              .equals(m.metadata().attributes().get("exchange.v1.signature"))) {
        throw new IllegalArgumentException(
            "Импортированная стереоконфигурация устарела. Перепроверьте структуру перед поиском.");
      }
      final Map<String, String> requested = new HashMap<>();
      for (final var center : m.stereochemistry().centers()) {
        final var descriptor = center.descriptor();
        if (center.kind() == StereocenterKind.TETRAHEDRAL_ATOM) {
          final var ordered = parity.remove(center.primaryAtomId());
          if (ordered == null
              && descriptor != StereochemicalDescriptor.R
              && descriptor != StereochemicalDescriptor.S) {
            throw new IllegalArgumentException(
                "Для поиска требуется проверяемая абсолютная R/S-конфигурация или сохранённая тетраэдрическая чётность.");
          }
          final var element =
              new TetrahedralChirality(
                  atoms.get(center.primaryAtomId()),
                  center.referenceAtomIds().stream().map(atoms::get).toArray(IAtom[]::new),
                  ordered != null && ordered.sign() < 0
                      ? ITetrahedralChirality.Stereo.ANTI_CLOCKWISE
                      : ITetrahedralChirality.Stereo.CLOCKWISE);
          stored.addStereoElement(element);
          if (descriptor == StereochemicalDescriptor.R
              || descriptor == StereochemicalDescriptor.S) {
            requested.put(stereoKey(stored, element), descriptor.name());
          }
        } else if (center.kind() == StereocenterKind.DOUBLE_BOND
            && Set.of(
                    StereochemicalDescriptor.CIS,
                    StereochemicalDescriptor.TRANS,
                    StereochemicalDescriptor.E,
                    StereochemicalDescriptor.Z)
                .contains(descriptor)) {
          final IAtom a = atoms.get(center.primaryAtomId()),
              b = atoms.get(center.secondaryAtomId());
          final var element =
              new DoubleBondStereochemistry(
                  stored.getBond(a, b),
                  new IBond[] {
                    stored.getBond(a, atoms.get(center.referenceAtomIds().get(0))),
                    stored.getBond(b, atoms.get(center.referenceAtomIds().get(1)))
                  },
                  descriptor == StereochemicalDescriptor.TRANS
                      ? IDoubleBondStereochemistry.Conformation.OPPOSITE
                      : IDoubleBondStereochemistry.Conformation.TOGETHER);
          stored.addStereoElement(element);
          if (descriptor == StereochemicalDescriptor.E
              || descriptor == StereochemicalDescriptor.Z) {
            requested.put(stereoKey(stored, element), descriptor.name());
          }
        } else {
          throw new IllegalArgumentException(
              "Этот тип стереозаписи не поддержан проверкой названия. Укажите имя с источником вручную.");
        }
      }
      if (!parity.isEmpty()) {
        throw new IllegalArgumentException("Чётность не соответствует стереоцентрам структуры.");
      }
      final var initial = absoluteLabels(stored);
      final List<IStereoElement> corrected = new ArrayList<>();
      for (final var element : stored.stereoElements()) {
        final String key = stereoKey(stored, element), wanted = requested.get(key);
        if (wanted == null || wanted.equals(initial.get(key))) {
          corrected.add(element);
        } else if (element instanceof ITetrahedralChirality t) {
          corrected.add(
              new TetrahedralChirality(
                  t.getChiralAtom(),
                  t.getLigands(),
                  t.getStereo() == ITetrahedralChirality.Stereo.CLOCKWISE
                      ? ITetrahedralChirality.Stereo.ANTI_CLOCKWISE
                      : ITetrahedralChirality.Stereo.CLOCKWISE));
        } else if (element instanceof IDoubleBondStereochemistry d) {
          corrected.add(
              new DoubleBondStereochemistry(
                  d.getStereoBond(),
                  d.getBonds(),
                  d.getStereo() == IDoubleBondStereochemistry.Conformation.TOGETHER
                      ? IDoubleBondStereochemistry.Conformation.OPPOSITE
                      : IDoubleBondStereochemistry.Conformation.TOGETHER));
        }
      }
      stored.setStereoElements(corrected);
      final var storedLabels = absoluteLabels(stored);
      if (!requested.entrySet().stream()
          .allMatch(e -> e.getValue().equals(storedLabels.get(e.getKey())))) {
        throw new IllegalArgumentException(
            "Не удалось подтвердить введённые абсолютные дескрипторы.");
      }
      final boolean wedges =
          drawing.bonds().stream()
              .anyMatch(
                  b ->
                      b.style() == BondDrawingStyle.WEDGE_UP
                          || b.style() == BondDrawingStyle.WEDGE_DOWN);
      final boolean doubleBonds = storedLabels.keySet().stream().anyMatch(k -> k.startsWith("D:"));
      if (!wedges && (!doubleBonds || drawing.atoms().isEmpty())) {
        return stored;
      }
      final IAtomContainer depicted =
          CdkCipCalculator.configuration(drawing, CdkCipCalculator.Source.DRAWING_2D);
      final List<IStereoElement> explicit = new ArrayList<>();
      for (final var element : depicted.stereoElements()) {
        if (wedges && element instanceof ITetrahedralChirality
            || element instanceof IDoubleBondStereochemistry
                && storedLabels.containsKey(stereoKey(depicted, element))) {
          explicit.add(element);
        }
      }
      depicted.setStereoElements(explicit);
      final var depictedLabels = absoluteLabels(depicted);
      if (wedges && depictedLabels.keySet().stream().noneMatch(k -> k.startsWith("T:"))
          || !storedLabels.entrySet().stream()
              .filter(e -> wedges || e.getKey().startsWith("D:"))
              .allMatch(e -> e.getValue().equals(depictedLabels.get(e.getKey())))) {
        throw new IllegalArgumentException(
            "Клинья или расположение заместителей двойной связи расходятся с сохранённой конфигурацией либо неоднозначны. Названия не применены.");
      }
      return wedges ? depicted : stored;
    } catch (final java.util.concurrent.CancellationException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new IllegalArgumentException(
          "Не удалось проверить стереохимию для названия: " + ex.getMessage(), ex);
    }
  }

  private static String stereoKey(final IAtomContainer graph, final IStereoElement<?, ?> element) {
    if (element instanceof ITetrahedralChirality t) {
      return "T:" + graph.indexOf(t.getChiralAtom());
    }
    if (element instanceof IDoubleBondStereochemistry d) {
      final int a = graph.indexOf(d.getStereoBond().getBegin()),
          b = graph.indexOf(d.getStereoBond().getEnd());
      return "D:" + Math.min(a, b) + ":" + Math.max(a, b);
    }
    throw new IllegalArgumentException(
        "Поддержаны только обычные тетраэдрические R/S и двойные связи E/Z.");
  }

  private static Map<String, String> absoluteLabels(final IAtomContainer graph) {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
    graph.atoms().forEach(a -> a.removeProperty(BaseMol.CIP_LABEL_KEY));
    graph.bonds().forEach(b -> b.removeProperty(BaseMol.CIP_LABEL_KEY));
    CdkLabeller.label(graph);
    final Map<String, String> labels = new HashMap<>();
    for (final var element : graph.stereoElements()) {
      final Object raw =
          element instanceof ITetrahedralChirality t
              ? t.getChiralAtom().getProperty(BaseMol.CIP_LABEL_KEY)
              : element instanceof IDoubleBondStereochemistry d
                  ? d.getStereoBond().getProperty(BaseMol.CIP_LABEL_KEY)
                  : null;
      final String label = Objects.toString(raw, "");
      if (element.getGroupInfo() != IStereoElement.GRP_ABS
          || !Set.of("R", "S", "E", "Z").contains(label)) {
        throw new IllegalArgumentException(
            "Абсолютная конфигурация не подтверждена поддержанными правилами CIP.");
      }
      labels.put(stereoKey(graph, element), label);
    }
    return labels;
  }

  public static MolecularGraphEquivalence.Result compare(
      final MoleculeDrawing a, final MoleculeDrawing b) {
    try {
      return MolecularGraphEquivalence.compare(verified(a), verified(b));
    } catch (final java.util.concurrent.CancellationException ex) {
      throw ex;
    } catch (final Exception ex) {
      return MolecularGraphEquivalence.Result.UNRESOLVED;
    }
  }

  public static String signature(final MoleculeDrawing drawing) {
    try {
      final String identity =
          "drawing-stereo-identity-v1/"
              + LaboratoryMaterialProperties.signature(verified(drawing))
              + "/"
              + relativeRecord(drawing.molecule());
      return HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (final RuntimeException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new IllegalArgumentException(
          "Не удалось подтвердить стереохимию рисунка: " + ex.getMessage(), ex);
    }
  }

  private static String relativeRecord(final Molecule m) {
    return MolecularRelativeStereo.read(m)
        .map(
            r ->
                r.groups().stream()
                    .map(g -> g.kind() + ":" + g.members())
                    .sorted()
                    .toList()
                    .toString())
        .orElse("");
  }

  /** Read-only projection shared by identity and search; atom IDs and the source drawing remain unchanged. */
  public static Molecule verifiedMolecule(final MoleculeDrawing drawing) {
    try {
      return verified(Objects.requireNonNull(drawing));
    } catch (final java.util.concurrent.CancellationException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new IllegalArgumentException("Конфигурация не сопоставлена: " + ex.getMessage(), ex);
    }
  }

  private static Molecule verified(final MoleculeDrawing drawing) throws Exception {
    final var m = drawing.molecule();
    if (m.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)) {
      throw new IllegalArgumentException("Стереохимия требует проверки.");
    }
    if (MolecularRelativeStereo.present(m)) {
      MolecularRelativeStereo.read(m).orElseThrow().validate(m);
      return m;
    }
    if (drawing.bonds().stream()
        .anyMatch(
            b -> b.style() == BondDrawingStyle.WAVY || b.style() == BondDrawingStyle.UNKNOWN)) {
      throw new IllegalArgumentException("Неопределённая стереосвязь.");
    }
    final boolean depicted =
        drawing.bonds().stream()
            .anyMatch(
                b ->
                    b.style() == BondDrawingStyle.WEDGE_UP
                        || b.style() == BondDrawingStyle.WEDGE_DOWN);
    final Molecule stored =
        m.metadata().attributes().containsKey(Stereochemistry.ORDERED_PARITY_ATTRIBUTE)
            ? normalized(drawing, CdkCipCalculator.Source.STORED_CONFIGURATION)
            : m;
    if (!depicted) {
      return stored;
    }
    final Molecule fromDrawing = normalized(drawing, CdkCipCalculator.Source.DRAWING_2D);
    if (fromDrawing.stereochemistry().empty()) {
      throw new IllegalArgumentException("Клинья не определяют поддержанную конфигурацию.");
    }
    if (!stored.stereochemistry().empty()
        && MolecularGraphEquivalence.compare(stored, fromDrawing)
            != MolecularGraphEquivalence.Result.EQUIVALENT) {
      throw new IllegalArgumentException(
          "Клинья рисунка расходятся с сохранённой конфигурацией или не позволяют её проверить.");
    }
    return fromDrawing;
  }

  private static Molecule normalized(
      final MoleculeDrawing drawing, final CdkCipCalculator.Source source) throws Exception {
    final Molecule m = drawing.molecule();
    if (m.atomCount() > 256) {
      throw new IllegalArgumentException("Проверка стереорисунка ограничена 256 явными атомами.");
    }
    final var graph = CdkCipCalculator.configuration(drawing, source);
    CdkLabeller.label(graph);
    final List<Stereocenter> centers = new ArrayList<>();
    for (final var element : graph.stereoElements()) {
      if (element instanceof ITetrahedralChirality t) {
        final String label =
            Objects.toString(t.getChiralAtom().getProperty(BaseMol.CIP_LABEL_KEY), "");
        if (!Set.of("R", "S").contains(label)) {
          throw new IllegalArgumentException("Тетраэдрическая конфигурация не подтверждена.");
        }
        final var refs = Arrays.stream(t.getLigands()).map(a -> id(m, graph, a)).toList();
        centers.add(
            Stereocenter.ofTetrahedralAtom(
                id(m, graph, t.getChiralAtom()),
                StereochemicalDescriptor.valueOf(label),
                refs.get(0),
                refs.get(1),
                refs.get(2),
                refs.get(3)));
      } else if (element instanceof IDoubleBondStereochemistry d) {
        final IBond bond = d.getStereoBond();
        final String label = Objects.toString(bond.getProperty(BaseMol.CIP_LABEL_KEY), "");
        if (!Set.of("E", "Z").contains(label)) {
          throw new IllegalArgumentException("Конфигурация двойной связи не подтверждена.");
        }
        centers.add(
            Stereocenter.ofDoubleBond(
                id(m, graph, bond.getBegin()),
                id(m, graph, bond.getEnd()),
                StereochemicalDescriptor.valueOf(label),
                id(m, graph, d.getBonds()[0].getOther(bond.getBegin())),
                id(m, graph, d.getBonds()[1].getOther(bond.getEnd()))));
      } else {
        throw new IllegalArgumentException("Этот тип стереохимии требует отдельной проверки.");
      }
    }
    final var a = new LinkedHashMap<>(m.metadata().attributes());
    a.remove(Stereochemistry.ORDERED_PARITY_ATTRIBUTE);
    a.remove("exchange.v1.signature");
    return Molecule.of(
        m.id(),
        m.displayName(),
        m.atoms(),
        m.bonds(),
        m.charge(),
        m.spinMultiplicity(),
        Stereochemistry.of(centers),
        m.conformation(),
        m.opticalRotation(),
        m.symmetry(),
        ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), a));
  }

  private static AtomId id(final Molecule m, final IAtomContainer graph, final IAtom atom) {
    return m.atoms().get(graph.indexOf(atom)).id();
  }
}
