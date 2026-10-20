/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.util.*;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.stereo.TetrahedralChirality;
import javax.vecmath.Point2d;
import org.openscience.cdk.stereo.StereoElementFactory;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Remap authored groups while retaining local tetrahedral orientation, never an absolute material claim. */
public final class CdkRelativeStereoJoin {
  private CdkRelativeStereoJoin() {}

  private record Source(
      MoleculeDrawing drawing,
      MolecularRelativeStereo relative,
      Map<AtomId, AtomId> ids,
      IAtomContainer representative,
      Map<String, String> labels) {}

  public static MoleculeDrawing preserve(
      final MoleculeDrawing first,
      final MoleculeDrawing second,
      final MoleculeDrawing joined,
      final Map<AtomId, AtomId> secondIds) {
    if (!MolecularRelativeStereo.present(first.molecule())
        && !MolecularRelativeStereo.present(second.molecule())) {
      return joined;
    }
    try {
      final Map<AtomId, AtomId> firstIds = new LinkedHashMap<>();
      first.molecule().atoms().forEach(a -> firstIds.put(a.id(), a.id()));
      final List<Source> sources = new ArrayList<>();
      source(first, firstIds).ifPresent(sources::add);
      source(second, secondIds).ifPresent(sources::add);
      if (sources.stream().mapToInt(s -> s.relative().groups().size()).sum() > 64) {
        throw new IllegalArgumentException("После соединения получилось бы больше 64 стереогрупп.");
      }
      final Molecule target = joined.molecule();
      final IAtomContainer graph = plainGraph(joined);
      final Map<AtomId, IAtom> atoms = atoms(target, graph);
      for (final var source : sources) {
        for (final var center : source.relative().atoms()) {
          final AtomId mapped = required(source.ids(), center);
          final Set<AtomId> before =
              neighbors(source.drawing().molecule(), center).stream()
                  .map(id -> required(source.ids(), id))
                  .collect(java.util.stream.Collectors.toSet());
          if (!before.equals(new HashSet<>(neighbors(target, mapped)))) {
            throw new IllegalArgumentException(
                "Соединение меняет непосредственные заместители центра стереогруппы. Сначала задайте новый центр явно; исходные группы сохранены.");
          }
        }
        if (source.representative() == null) {
          continue;
        }
        for (final var element : source.representative().stereoElements()) {
          if (!(element instanceof ITetrahedralChirality tetrahedron)) {
            continue;
          }
          final AtomId center =
              source
                  .drawing()
                  .molecule()
                  .atoms()
                  .get(source.representative().indexOf(tetrahedron.getChiralAtom()))
                  .id();
          final Set<AtomId> before =
              neighbors(source.drawing().molecule(), center).stream()
                  .map(id -> required(source.ids(), id))
                  .collect(java.util.stream.Collectors.toSet());
          if (before.size() != 4
              || !before.equals(new HashSet<>(neighbors(target, required(source.ids(), center))))
              || Arrays.stream(tetrahedron.getLigands())
                  .anyMatch(a -> a.equals(tetrahedron.getChiralAtom()))) {
            throw new IllegalArgumentException(
                "Перенос клиньев требует тех же четырёх явных заместителей; соединение не должно менять сам стереоцентр.");
          }
          final IAtom[] ligands =
              Arrays.stream(tetrahedron.getLigands())
                  .map(
                      a ->
                          source
                              .drawing()
                              .molecule()
                              .atoms()
                              .get(source.representative().indexOf(a))
                              .id())
                  .map(id -> atoms.get(required(source.ids(), id)))
                  .toArray(IAtom[]::new);
          graph.addStereoElement(
              new TetrahedralChirality(
                  atoms.get(required(source.ids(), center)), ligands, tetrahedron.getStereo()));
        }
      }
      final Map<String, String> labels = CdkInitialGeometry.stereoLabels(graph);
      final List<MolecularRelativeStereo.Group> groups = new ArrayList<>();
      final Set<String> used = new HashSet<>();
      for (final var source : sources) {
        for (final var group : source.relative().groups()) {
          final List<MolecularRelativeStereo.Member> members = new ArrayList<>();
          for (final var member : group.members()) {
            final AtomId mapped = required(source.ids(), member.atom());
            var descriptor = member.descriptor();
            if (descriptor != MolecularRelativeStereo.Descriptor.UNKNOWN) {
              final String before =
                  source.labels().get("T:" + index(source.drawing().molecule(), member.atom()));
              final String after = labels.get("T:" + index(target, mapped));
              if (!Set.of("R", "S").contains(Objects.toString(before, ""))
                  || !Set.of("R", "S").contains(Objects.toString(after, ""))) {
                throw new IllegalArgumentException(
                    "Относительный центр после соединения не получил проверяемую конфигурацию. Группы не отброшены; исходные структуры сохранены.");
              }
              if (!before.equals(after)) {
                descriptor =
                    descriptor == MolecularRelativeStereo.Descriptor.R_STAR
                        ? MolecularRelativeStereo.Descriptor.S_STAR
                        : MolecularRelativeStereo.Descriptor.R_STAR;
              }
            }
            members.add(new MolecularRelativeStereo.Member(mapped, descriptor));
          }
          String id = group.id();
          if (!used.add(id)) {
            do {
              id = "join_" + UUID.randomUUID();
            } while (!used.add(id));
          }
          groups.add(
              new MolecularRelativeStereo.Group(
                  id,
                  group.kind(),
                  required(source.ids(), group.anchor()),
                  members,
                  group.source(),
                  group.recordedAt()));
        }
      }
      final Molecule result =
          new MolecularRelativeStereo(MolecularRelativeStereo.signature(target), groups)
              .apply(target);
      return MoleculeDrawing.of(result, joined.atoms(), joined.bonds(), joined.manualFields());
    } catch (final java.util.concurrent.CancellationException ex) {
      throw ex;
    } catch (final Exception ex) {
      throw new IllegalArgumentException(
          "Не удалось сохранить относительную стереохимию при соединении: " + ex.getMessage(), ex);
    }
  }

  private static Optional<Source> source(
      final MoleculeDrawing drawing, final Map<AtomId, AtomId> ids) throws Exception {
    final var optional = MolecularRelativeStereo.read(drawing.molecule());
    final boolean wedges =
        drawing.bonds().stream()
            .anyMatch(
                b ->
                    b.style() == BondDrawingStyle.WEDGE_UP
                        || b.style() == BondDrawingStyle.WEDGE_DOWN);
    if (optional.isEmpty() && !wedges) {
      return Optional.empty();
    }
    final var relative =
        optional.orElseGet(
            () ->
                new MolecularRelativeStereo(
                    MolecularRelativeStereo.signature(drawing.molecule()), List.of()));
    relative.validate(drawing.molecule());
    final Molecule m = MolecularRelativeStereo.clear(drawing.molecule());
    if (m.atomCount() > 256) {
      throw new IllegalArgumentException("Проверка переноса R*/S* ограничена 256 явными атомами.");
    }
    final var clean =
        MoleculeDrawing.of(m, drawing.atoms(), drawing.bonds(), drawing.manualFields());
    final IAtomContainer depictedGraph = wedges ? depictedGraph(clean) : null;
    final Map<String, String> depicted =
        depictedGraph == null ? Map.of() : CdkInitialGeometry.stereoLabels(depictedGraph);
    if (depictedGraph != null) {
      for (final var element : depictedGraph.stereoElements()) {
        if (!depicted.containsKey(
            "T:" + depictedGraph.indexOf(((ITetrahedralChirality) element).getChiralAtom()))) {
          throw new IllegalArgumentException(
              "Часть клиньев не получила проверяемую тетраэдрическую конфигурацию.");
        }
      }
      if (depicted.isEmpty()) {
        throw new IllegalArgumentException(
            "Клинья черновика не определяют проверяемую конфигурацию.");
      }
    }
    final Map<AtomId, String> wanted = new LinkedHashMap<>();
    for (final var group : relative.groups()) {
      if (group.kind() == MolecularRelativeStereo.Kind.UNKNOWN_ABSOLUTE) {
        continue;
      }
      Boolean inverted = null;
      for (final var member : group.members()) {
        final String shown = depicted.get("T:" + index(m, member.atom()));
        if (shown == null) {
          continue;
        }
        final boolean opposite =
            !shown.equals(
                member.descriptor() == MolecularRelativeStereo.Descriptor.R_STAR ? "R" : "S");
        if (inverted != null && inverted != opposite) {
          throw new IllegalArgumentException(
              "Клинья не согласованы с относительной конфигурацией группы.");
        }
        inverted = opposite;
      }
      for (final var member : group.members()) {
        boolean r = member.descriptor() == MolecularRelativeStereo.Descriptor.R_STAR;
        if (Boolean.TRUE.equals(inverted)) {
          r = !r;
        }
        wanted.put(member.atom(), r ? "R" : "S");
      }
    }
    final var graph = plainGraph(clean);
    final var atoms = atoms(m, graph);
    for (final var center : wanted.keySet()) {
      graph.addStereoElement(
          new TetrahedralChirality(
              atoms.get(center),
              neighbors(m, center).stream().map(atoms::get).toArray(IAtom[]::new),
              ITetrahedralChirality.Stereo.CLOCKWISE));
    }
    if (depictedGraph != null) {
      for (final var element : depictedGraph.stereoElements()) {
        final var tetrahedron = (ITetrahedralChirality) element;
        final AtomId center =
            m.atoms().get(depictedGraph.indexOf(tetrahedron.getChiralAtom())).id();
        if (relative.atoms().contains(center)) {
          continue;
        }
        final IAtom[] ligands =
            Arrays.stream(tetrahedron.getLigands())
                .map(a -> atoms.get(m.atoms().get(depictedGraph.indexOf(a)).id()))
                .toArray(IAtom[]::new);
        graph.addStereoElement(
            new TetrahedralChirality(atoms.get(center), ligands, tetrahedron.getStereo()));
      }
    }
    final var initial = CdkInitialGeometry.stereoLabels(graph);
    final List<IStereoElement> adjusted = new ArrayList<>();
    for (final var element : graph.stereoElements()) {
      final var tetrahedron = (ITetrahedralChirality) element;
      final int index = graph.indexOf(tetrahedron.getChiralAtom());
      final String label = initial.get("T:" + index);
      if (label == null) {
        throw new IllegalArgumentException(
            "Относительный центр не получил проверяемую R*/S*-конфигурацию.");
      }
      final String target = wanted.get(m.atoms().get(index).id());
      adjusted.add(
          target == null || target.equals(label)
              ? tetrahedron
              : new TetrahedralChirality(
                  tetrahedron.getChiralAtom(),
                  tetrahedron.getLigands(),
                  ITetrahedralChirality.Stereo.ANTI_CLOCKWISE));
    }
    graph.setStereoElements(adjusted);
    final var checked = CdkInitialGeometry.stereoLabels(graph);
    if (wanted.entrySet().stream()
        .anyMatch(e -> !e.getValue().equals(checked.get("T:" + index(m, e.getKey()))))) {
      throw new IllegalArgumentException(
          "Не удалось проверить представитель относительной группы.");
    }
    return Optional.of(new Source(drawing, relative, Map.copyOf(ids), graph, checked));
  }

  /** Exact explicit connectivity only: incomplete peripheral draft valences are not silently filled with H. */
  private static IAtomContainer plainGraph(final MoleculeDrawing drawing) throws Exception {
    final Molecule m = drawing.molecule();
    if (m.atomCount() > 256) {
      throw new IllegalArgumentException(
          "Проверка переноса стереогрупп ограничена 256 явными атомами.");
    }
    if (m.atoms().stream().anyMatch(a -> a.radicalState().radical())
        || m.metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)) {
      throw new IllegalArgumentException(
          "Радикалы или непроверенная стереохимия требуют отдельной проверки перед переносом группы.");
    }
    final IAtomContainer graph = new org.openscience.cdk.silent.AtomContainer();
    final Map<AtomId, IAtom> atoms = new LinkedHashMap<>();
    for (final var atom : m.atoms()) {
      final IAtom copy = new org.openscience.cdk.silent.Atom(atom.element().symbol().value());
      copy.setFormalCharge(atom.formalCharge().value());
      copy.setImplicitHydrogenCount(0);
      if (atom.hasIsotope()) {
        copy.setMassNumber(atom.isotope().massNumber());
      }
      copy.setID(atom.id().value());
      graph.addAtom(copy);
      atoms.put(atom.id(), copy);
    }
    for (final var bond : m.bonds()) {
      final IBond.Order order =
          switch (bond.type()) {
            case SINGLE, AROMATIC -> IBond.Order.SINGLE;
            case DOUBLE -> IBond.Order.DOUBLE;
            case TRIPLE -> IBond.Order.TRIPLE;
            default ->
                throw new IllegalArgumentException(
                    "Перенос групп с координационными или неизвестными связями пока не поддержан.");
          };
      final IBond copy =
          new org.openscience.cdk.silent.Bond(
              atoms.get(bond.firstAtomId()), atoms.get(bond.secondAtomId()), order);
      if (bond.type() == BondType.AROMATIC) {
        copy.setIsAromatic(true);
        copy.getBegin().setIsAromatic(true);
        copy.getEnd().setIsAromatic(true);
      }
      graph.addBond(copy);
    }
    if (m.bonds().stream().anyMatch(b -> b.type() == BondType.AROMATIC)) {
      org.openscience.cdk.aromaticity.Kekulization.kekulize(graph);
    }
    return graph;
  }

  private static IAtomContainer depictedGraph(final MoleculeDrawing drawing) throws Exception {
    final var graph = plainGraph(drawing);
    final var atoms = atoms(drawing.molecule(), graph);
    for (final var atom : drawing.atoms()) {
      atoms.get(atom.atomId()).setPoint2d(new Point2d(atom.point2D().x(), -atom.point2D().y()));
    }
    for (final var bond : drawing.bonds()) {
      final var first = atoms.get(bond.firstAtomId());
      final var second = atoms.get(bond.secondAtomId());
      final var target = graph.getBond(first, second);
      final boolean forward = target.getBegin().equals(first);
      target.setStereo(
          switch (bond.style()) {
            case WEDGE_UP -> forward ? IBond.Stereo.UP : IBond.Stereo.UP_INVERTED;
            case WEDGE_DOWN -> forward ? IBond.Stereo.DOWN : IBond.Stereo.DOWN_INVERTED;
            case WAVY, UNKNOWN -> IBond.Stereo.UP_OR_DOWN;
            default -> IBond.Stereo.NONE;
          });
    }
    graph.setStereoElements(
        StereoElementFactory.using2DCoordinates(graph).createAll().stream()
            .filter(e -> e instanceof ITetrahedralChirality)
            .toList());
    return graph;
  }

  private static Map<AtomId, IAtom> atoms(final Molecule molecule, final IAtomContainer graph) {
    final Map<AtomId, IAtom> result = new HashMap<>();
    for (int i = 0; i < molecule.atomCount(); i++) {
      result.put(molecule.atoms().get(i).id(), graph.getAtom(i));
    }
    return result;
  }

  private static AtomId required(final Map<AtomId, AtomId> ids, final AtomId id) {
    return Objects.requireNonNull(ids.get(id), "Нет отображения атома стереогруппы.");
  }

  private static int index(final Molecule m, final AtomId id) {
    for (int i = 0; i < m.atomCount(); i++) {
      if (m.atoms().get(i).id().equals(id)) {
        return i;
      }
    }
    throw new IllegalArgumentException("Атом стереогруппы отсутствует.");
  }

  private static List<AtomId> neighbors(final Molecule m, final AtomId id) {
    return m.bonds().stream()
        .filter(b -> b.firstAtomId().equals(id) || b.secondAtomId().equals(id))
        .map(b -> b.firstAtomId().equals(id) ? b.secondAtomId() : b.firstAtomId())
        .toList();
  }
}
