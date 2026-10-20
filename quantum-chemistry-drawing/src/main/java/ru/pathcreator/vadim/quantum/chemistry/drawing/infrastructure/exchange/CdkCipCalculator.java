/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.vecmath.Point2d;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.stereo.StereoElementFactory;
import com.simolecule.centres.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;

/** Derived descriptors from explicit imported parity or the current 2D stereochemical depiction. */
public final class CdkCipCalculator {
  public enum Source {
    STORED_CONFIGURATION,
    DRAWING_2D
  }

  public record Result(MolecularCipLabels labels, List<String> warnings) {
    public Result {
      warnings = List.copyOf(warnings);
    }
  }

  public Result calculate(final MoleculeDrawing drawing, final Source source) {
    final Molecule m = drawing.molecule();
    if (MolecularRelativeStereo.present(m)) {
      final var relative = MolecularRelativeStereo.read(m).orElseThrow();
      relative.validate(m);
      throw new IllegalArgumentException(
          "Для структуры заданы относительные / рацемические группы или неизвестная абсолютная конфигурация. R*/S* — ручные относительные назначения; абсолютный CIP здесь не утверждается.");
    }
    if (m.atomCount() > 256) {
      throw new IllegalArgumentException(
          "Расчёт CIP в рабочем месте ограничен 256 явными атомами.");
    }
    try {
      final List<String> warnings = new ArrayList<>();
      final IAtomContainer graph;
      graph = configuration(drawing, source);
      if (source == Source.DRAWING_2D) {
        warnings.add(
            "Расчёт использует клинья и расположение связей текущего 2D-рисунка. Проверьте рисунок при расхождении с ранее введённой конфигурацией.");
      }
      if (Thread.currentThread().isInterrupted()) {
        throw new java.util.concurrent.CancellationException();
      }
      CdkLabeller.label(graph);
      final List<MolecularCipLabels.Label> labels = new ArrayList<>();
      int represented = 0;
      for (final var ignored : graph.stereoElements()) {
        represented++;
      }
      for (int i = 0; i < graph.getAtomCount(); i++) {
        final Object descriptor = graph.getAtom(i).getProperty(BaseMol.CIP_LABEL_KEY);
        if (accepted(descriptor)) {
          labels.add(
              new MolecularCipLabels.Label(m.atoms().get(i).id(), null, descriptor.toString()));
        }
      }
      for (final var bond : graph.bonds()) {
        final Object descriptor = bond.getProperty(BaseMol.CIP_LABEL_KEY);
        if (accepted(descriptor)) {
          labels.add(
              new MolecularCipLabels.Label(
                  m.atoms().get(graph.indexOf(bond.getBegin())).id(),
                  m.atoms().get(graph.indexOf(bond.getEnd())).id(),
                  descriptor.toString()));
        }
      }
      if (labels.isEmpty()) {
        warnings.add(
            "Дескрипторы не определены: нет однозначно заданных поддержанных стереоэлементов.");
      }
      if (labels.size() < represented) {
        warnings.add(
            "Часть заданных стереоэлементов не получила дескриптор. Отсутствие метки не означает отсутствие стереоизомерии.");
      }
      warnings.add(
          "Плоскостная и геликальная хиральность требуют отдельного задания; найденные метки не доказывают полноту стереохимического описания.");
      return new Result(
          new MolecularCipLabels(
              signature(drawing, source),
              "CIP · Centres 1.5 / CDK 2.12 · " + source.name(),
              labels),
          warnings);
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final Exception e) {
      throw new IllegalArgumentException("CIP: " + e.getMessage(), e);
    }
  }

  static IAtomContainer configuration(final MoleculeDrawing drawing, final Source source)
      throws Exception {
    final Molecule m = drawing.molecule();
    final IAtomContainer graph;
    if (source == Source.STORED_CONFIGURATION) {
      graph = CdkStructureExchange.project(drawing);
    } else {
      final var attrs = new LinkedHashMap<>(m.metadata().attributes());
      attrs.remove(Stereochemistry.ORDERED_PARITY_ATTRIBUTE);
      attrs.remove(Stereochemistry.REVIEW_ATTRIBUTE);
      attrs.remove("exchange.v1.signature");
      final Molecule base =
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
              ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), attrs));
      final var plain =
          MoleculeDrawing.of(
              base,
              drawing.atoms(),
              drawing.bonds().stream()
                  .map(
                      b ->
                          BondDrawing.of(b.firstAtomId(), b.secondAtomId(), BondDrawingStyle.PLAIN))
                  .toList(),
              drawing.manualFields());
      graph = CdkStructureExchange.project(plain);
      final Map<AtomId, Integer> indices = new HashMap<>();
      for (int i = 0; i < m.atomCount(); i++) {
        indices.put(m.atoms().get(i).id(), i);
      }
      for (final var a : drawing.atoms()) {
        graph
            .getAtom(indices.get(a.atomId()))
            .setPoint2d(new Point2d(a.point2D().x(), -a.point2D().y()));
      }
      for (final var b : drawing.bonds()) {
        final IAtom first = graph.getAtom(indices.get(b.firstAtomId())),
            second = graph.getAtom(indices.get(b.secondAtomId()));
        final IBond bond = graph.getBond(first, second);
        if (bond == null) {
          continue;
        }
        final boolean forward = bond.getBegin().equals(first);
        bond.setStereo(
            switch (b.style()) {
              case WEDGE_UP -> forward ? IBond.Stereo.UP : IBond.Stereo.UP_INVERTED;
              case WEDGE_DOWN -> forward ? IBond.Stereo.DOWN : IBond.Stereo.DOWN_INVERTED;
              case WAVY, UNKNOWN -> IBond.Stereo.UP_OR_DOWN;
              default -> IBond.Stereo.NONE;
            });
      }
      graph.setStereoElements(StereoElementFactory.using2DCoordinates(graph).createAll());
    }
    return graph;
  }

  private static boolean accepted(final Object value) {
    return value != null
        && Set.of("R", "S", "r", "s", "E", "Z", "M", "P", "m", "p", "seqCis", "seqTrans")
            .contains(value.toString());
  }

  public static String signature(final MoleculeDrawing drawing, final Source source) {
    final var m = drawing.molecule();
    final StringBuilder text =
        new StringBuilder(
            ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.signature(m));
    text.append('|')
        .append(source)
        .append('|')
        .append(
            m.metadata().attributes().getOrDefault(Stereochemistry.ORDERED_PARITY_ATTRIBUTE, ""));
    if (source == Source.DRAWING_2D) {
      drawing
          .atoms()
          .forEach(
              a ->
                  text.append('|')
                      .append(a.atomId())
                      .append(':')
                      .append(a.point2D().x())
                      .append(':')
                      .append(a.point2D().y()));
      drawing
          .bonds()
          .forEach(
              b ->
                  text.append('|')
                      .append(b.firstAtomId())
                      .append(':')
                      .append(b.secondAtomId())
                      .append(':')
                      .append(b.style()));
    }
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(text.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (final java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
