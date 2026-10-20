/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.util.*;
import javax.vecmath.Point3d;
import com.simolecule.centres.*;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.stereo.StereoElementFactory;
import ru.pathcreator.vadim.quantum.chemistry.domain.geometry.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing;

/** Initial candidates only. CDK's builder is not trusted to preserve stereo: coordinates are checked independently. */
public final class CdkInitialGeometry {
  public record Result(Molecule molecule, String method) {}

  private CdkInitialGeometry() {}

  public static Result prepare(final MoleculeDrawing drawing) {
    final Molecule m = drawing.molecule();
    if (m.atomCount() < 2 || m.atomCount() > 256) {
      throw new IllegalArgumentException("Для конформеров поддерживаются 2–256 явных атомов.");
    }
    if (!m.conformation().empty()) {
      throw new IllegalArgumentException(
          "Автопостроение не применяет заданные торсионные ограничения. Используйте согласованную 3D-геометрию без автоматического перестроения.");
    }
    try {
      cancelled();
      var graph = CdkStereochemicalProjection.representative(drawing);
      final var expected = stereoLabels(graph);
      if (m.atoms().stream().allMatch(a -> a.coordinate() != null)) {
        for (int i = 0; i < m.atomCount(); i++) {
          final var p = m.atoms().get(i).coordinate();
          graph
              .getAtom(i)
              .setPoint3d(
                  new Point3d(
                      p.unit().toAngstrom(p.x()),
                      p.unit().toAngstrom(p.y()),
                      p.unit().toAngstrom(p.z())));
        }
        validate(graph, expected);
        return new Result(
            m,
            "Исходные 3D-координаты; заданная стереохимия проверена по геометрии; без оптимизации. "
                + CdkStereochemicalProjection.annotation(m));
      }
      if (m.atomCount() > 128) {
        throw new IllegalArgumentException(
            "Автопостроение ограничено 128 явными атомами. Введите готовые координаты в этом окне.");
      }
      graph
          .atoms()
          .forEach(
              a -> {
                a.setPoint2d(null);
                a.setPoint3d(null);
              });
      // An unresponsive third-party builder is killed without leaving computation in the
      // application JVM.
      graph = CdkInitialGeometryWorker.build(graph);
      cancelled();
      requireMapping(m, graph);
      finiteCoordinates(graph);
      final int repairedSulfonylGroups = CdkSulfonylGeometry.repair(m, graph);
      coordinatesValid(graph);
      final var actual = geometricLabels(graph);
      boolean reflected = false;
      if (!matches(expected, actual)) {
        if (expected.keySet().stream().noneMatch(k -> k.startsWith("T:"))
            || !expected.entrySet().stream()
                .allMatch(
                    e ->
                        e.getKey().startsWith("T:")
                            ? opposite(e.getValue()).equals(actual.get(e.getKey()))
                            : e.getValue().equals(actual.get(e.getKey())))) {
          throw new IllegalArgumentException(
              "Построитель не воспроизвёл заданную стереохимию. Частичная инверсия нескольких центров и неопределимая геометрия не исправляются автоматически.");
        }
        for (final var a : graph.atoms()) {
          a.getPoint3d().x = -a.getPoint3d().x;
        }
        reflected = true;
      }
      validate(graph, expected);
      final List<Atom> atoms = new ArrayList<>();
      for (int i = 0; i < m.atomCount(); i++) {
        final var a = m.atoms().get(i);
        final var p = graph.getAtom(i).getPoint3d();
        atoms.add(
            Atom.of(
                a.id(),
                a.element().symbol(),
                Coordinate3D.of(p.x, p.y, p.z, LengthUnit.ANGSTROM),
                a.formalCharge(),
                a.isotope(),
                a.radicalState(),
                a.metadata()));
      }
      final var result =
          Molecule.of(
              m.id(),
              m.displayName(),
              atoms,
              m.bonds(),
              m.charge(),
              m.spinMultiplicity(),
              m.stereochemistry(),
              m.conformation(),
              m.opticalRotation(),
              m.symmetry(),
              m.metadata());
      return new Result(
          result,
          "Начальные координаты CDK 2.12 / MM2-шаблоны; конфигурация проверена по 3D"
              + (repairedSulfonylGroups == 0
                  ? ""
                  : "; восстановлено начальное тетраэдрическое расположение совпавших сульфонильных O, групп: "
                      + repairedSulfonylGroups)
              + (reflected ? "; весь кандидат зеркально отражён для согласования конфигурации" : "")
              + ". Длины связей приближённые; возможны стандартные значения вне набора параметров. Без оптимизации и энергии. Незаданные центры не назначаются. "
              + CdkStereochemicalProjection.annotation(m));
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException(
          "3D-кандидат не принят: "
              + e.getMessage()
              + " Можно использовать импортированную 3D-геометрию с тем же графом и конфигурацией.",
          e);
    }
  }

  /** Verifies a geometry with original atom identities; does not infer or write stereocentre declarations. */
  public static void verify(final MoleculeDrawing expected, final Molecule geometry) {
    if (!expected.molecule().conformation().empty()) {
      throw new IllegalArgumentException(
          "Проверка введённых торсионных ограничений здесь не поддержана; такую геометрию нельзя применить из этого окна.");
    }
    final Molecule m = expected.molecule();
    if (!m.bonds().equals(geometry.bonds())
        || m.atomCount() != geometry.atomCount()
        || !m.stereochemistry().equals(geometry.stereochemistry())
        || !ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.signature(m)
            .equals(
                ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.signature(
                    geometry))
        || !m.charge().equals(geometry.charge())
        || !m.spinMultiplicity().equals(geometry.spinMultiplicity())
        || !m.conformation().equals(geometry.conformation())) {
      throw new IllegalArgumentException(
          "Геометрия относится к другой структуре или стереозаписи.");
    }
    final var graph = CdkStereochemicalProjection.representative(expected);
    final var labels = stereoLabels(graph);
    for (int i = 0; i < m.atomCount(); i++) {
      final var a = m.atoms().get(i);
      final var b = geometry.atoms().get(i);
      if (!a.id().equals(b.id())
          || !a.element().equals(b.element())
          || !a.formalCharge().equals(b.formalCharge())
          || !Objects.equals(a.isotope(), b.isotope())
          || !a.radicalState().equals(b.radicalState())
          || b.coordinate() == null) {
        throw new IllegalArgumentException("Не совпадают атомы или отсутствуют координаты.");
      }
      final var p = b.coordinate();
      graph
          .getAtom(i)
          .setPoint3d(
              new Point3d(
                  p.unit().toAngstrom(p.x()),
                  p.unit().toAngstrom(p.y()),
                  p.unit().toAngstrom(p.z())));
    }
    validate(graph, labels);
  }

  /** Explicit atom-ID input avoids guessed atom mapping from external coordinate order. Units are angstrom. */
  public static Molecule readCoordinates(final MoleculeDrawing expected, final String text) {
    if (text == null || text.length() > 80000) {
      throw new IllegalArgumentException("Введите координаты, до 80000 знаков.");
    }
    final var m = expected.molecule();
    if (m.atomCount() < 2 || m.atomCount() > 256) {
      throw new IllegalArgumentException("Для конформеров поддерживаются 2–256 явных атомов.");
    }
    final Map<String, Coordinate3D> values = new HashMap<>();
    final Map<String, Atom> atoms = new HashMap<>();
    m.atoms().forEach(a -> atoms.put(a.id().value(), a));
    for (final String line : text.lines().toList()) {
      if (line.isBlank() || line.strip().startsWith("#")) {
        continue;
      }
      final String[] p = line.strip().split("\\s+");
      if (p.length != 5
          || !atoms.containsKey(p[0])
          || !atoms.get(p[0]).element().symbol().value().equals(p[1])
          || values.containsKey(p[0])) {
        throw new IllegalArgumentException(
            "Каждая строка: ID атома, элемент, X, Y, Z. Нужны все исходные ID ровно один раз и соответствующие элементы.");
      }
      try {
        values.put(
            p[0],
            Coordinate3D.of(
                Double.parseDouble(p[2].replace(',', '.')),
                Double.parseDouble(p[3].replace(',', '.')),
                Double.parseDouble(p[4].replace(',', '.')),
                LengthUnit.ANGSTROM));
      } catch (final RuntimeException ex) {
        throw new IllegalArgumentException("Некорректные координаты атома " + p[0] + ".", ex);
      }
    }
    if (values.size() != m.atomCount()) {
      throw new IllegalArgumentException(
          "Число координат не соответствует числу атомов, включая явные H.");
    }
    final var placed =
        m.atoms().stream()
            .map(
                a ->
                    Atom.of(
                        a.id(),
                        a.element().symbol(),
                        values.get(a.id().value()),
                        a.formalCharge(),
                        a.isotope(),
                        a.radicalState(),
                        a.metadata()))
            .toList();
    final var result =
        Molecule.of(
            m.id(),
            m.displayName(),
            placed,
            m.bonds(),
            m.charge(),
            m.spinMultiplicity(),
            m.stereochemistry(),
            m.conformation(),
            m.opticalRotation(),
            m.symmetry(),
            m.metadata());
    verify(expected, result);
    return result;
  }

  private static void requireMapping(final Molecule m, final IAtomContainer graph) {
    if (graph.getAtomCount() != m.atomCount() || graph.getBondCount() != m.bonds().size()) {
      throw new IllegalArgumentException("Построитель изменил состав структуры.");
    }
    for (int i = 0; i < m.atomCount(); i++) {
      final var a = m.atoms().get(i);
      final var b = graph.getAtom(i);
      if (!a.id().value().equals(b.getID())
          || a.element().atomicNumber() != b.getAtomicNumber()
          || a.formalCharge().value() != b.getFormalCharge()
          || !Objects.equals(
              a.isotope() == null ? null : a.isotope().massNumber(), b.getMassNumber())) {
        throw new IllegalArgumentException("Построитель изменил соответствие атомов.");
      }
    }
    final Map<AtomId, IAtom> atoms = new HashMap<>();
    for (int i = 0; i < m.atomCount(); i++) {
      atoms.put(m.atoms().get(i).id(), graph.getAtom(i));
    }
    for (final var b : m.bonds()) {
      final var projected = graph.getBond(atoms.get(b.firstAtomId()), atoms.get(b.secondAtomId()));
      if (projected == null
          || !(switch (b.type()) {
            case SINGLE -> projected.getOrder() == IBond.Order.SINGLE;
            case DOUBLE -> projected.getOrder() == IBond.Order.DOUBLE;
            case TRIPLE -> projected.getOrder() == IBond.Order.TRIPLE;
            case AROMATIC ->
                projected.getOrder() == IBond.Order.SINGLE
                    || projected.getOrder() == IBond.Order.DOUBLE;
            default -> false;
          })) {
        throw new IllegalArgumentException("Построитель изменил связи исходной структуры.");
      }
    }
  }

  private static void finiteCoordinates(final IAtomContainer graph) {
    for (final var a : graph.atoms()) {
      final var p = a.getPoint3d();
      if (p == null
          || !Double.isFinite(p.x)
          || !Double.isFinite(p.y)
          || !Double.isFinite(p.z)
          || Math.max(Math.abs(p.x), Math.max(Math.abs(p.y), Math.abs(p.z))) > 1000) {
        throw new IllegalArgumentException("Неполные или недопустимые координаты.");
      }
    }
  }

  private static void coordinatesValid(final IAtomContainer graph) {
    finiteCoordinates(graph);
    for (final var b : graph.bonds()) {
      final double distance = b.getBegin().getPoint3d().distance(b.getEnd().getPoint3d());
      if (distance < .4 || distance > 3.5) {
        throw new IllegalArgumentException("Геометрия содержит недопустимую длину связи.");
      }
    }
    for (int i = 0; i < graph.getAtomCount(); i++) {
      for (int j = i + 1; j < graph.getAtomCount(); j++) {
        if (graph.getAtom(i).getPoint3d().distance(graph.getAtom(j).getPoint3d()) < .35) {
          throw new IllegalArgumentException(
              "В кандидате перекрываются атомы "
                  + graph.getAtom(i).getSymbol()
                  + " ("
                  + graph.getAtom(i).getID()
                  + ") и "
                  + graph.getAtom(j).getSymbol()
                  + " ("
                  + graph.getAtom(j).getID()
                  + ").");
        }
      }
    }
  }

  private static void validate(final IAtomContainer graph, final Map<String, String> expected) {
    coordinatesValid(graph);
    if (!matches(expected, geometricLabels(graph))) {
      throw new IllegalArgumentException(
          "Координаты не подтверждают заданную конфигурацию всех стереоэлементов.");
    }
  }

  private static boolean matches(
      final Map<String, String> expected, final Map<String, String> actual) {
    return expected.entrySet().stream().allMatch(e -> e.getValue().equals(actual.get(e.getKey())));
  }

  private static String opposite(final String label) {
    return "R".equals(label) ? "S" : "S".equals(label) ? "R" : "";
  }

  private static Map<String, String> geometricLabels(final IAtomContainer graph) {
    graph.setStereoElements(StereoElementFactory.using3DCoordinates(graph).createAll());
    return stereoLabels(graph);
  }

  static Map<String, String> stereoLabels(final IAtomContainer graph) {
    cancelled();
    graph.atoms().forEach(a -> a.removeProperty(BaseMol.CIP_LABEL_KEY));
    graph.bonds().forEach(b -> b.removeProperty(BaseMol.CIP_LABEL_KEY));
    CdkLabeller.label(graph);
    final Map<String, String> result = new HashMap<>();
    for (final var element : graph.stereoElements()) {
      if (element instanceof ITetrahedralChirality t) {
        final String label =
            Objects.toString(t.getChiralAtom().getProperty(BaseMol.CIP_LABEL_KEY), "");
        if (Set.of("R", "S").contains(label)) {
          result.put("T:" + graph.indexOf(t.getChiralAtom()), label);
        }
      } else if (element instanceof IDoubleBondStereochemistry d) {
        final var b = d.getStereoBond();
        final String label = Objects.toString(b.getProperty(BaseMol.CIP_LABEL_KEY), "");
        if (Set.of("E", "Z").contains(label)) {
          final int a = graph.indexOf(b.getBegin()), z = graph.indexOf(b.getEnd());
          result.put("D:" + Math.min(a, z) + ":" + Math.max(a, z), label);
        }
      }
    }
    return result;
  }

  private static void cancelled() {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
  }
}
