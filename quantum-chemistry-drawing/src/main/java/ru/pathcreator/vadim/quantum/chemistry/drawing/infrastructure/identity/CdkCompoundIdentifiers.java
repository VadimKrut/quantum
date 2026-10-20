/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity;

import java.util.*;
import org.openscience.cdk.aromaticity.*;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.isomorphism.*;
import org.openscience.cdk.silent.*;
import org.openscience.cdk.smiles.*;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.Stereochemistry;
import ru.pathcreator.vadim.quantum.chemistry.domain.stereo.MolecularRelativeStereo;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange.CdkStereoIdentity;

/** Disposable graph conversion. No implicit H are invented and no stereo descriptor is silently discarded. */
public final class CdkCompoundIdentifiers {
  public record Result(String smiles, boolean identityLookupAllowed, String explanation) {}

  public Result describe(final MoleculeDrawing drawing) {
    try {
      final IAtomContainer graph = project(drawing);
      final boolean stereo = graph.stereoElements().iterator().hasNext();
      return new Result(
          smiles(graph),
          true,
          stereo
              ? "Изомерический SMILES сохраняет проверенные R/S и E/Z. Справочная запись будет принята только при совпадении графа и всей заданной конфигурации."
              : "SMILES рассчитан локально из явных атомов, включая изотопы и заряд. Абсолютная стереоконфигурация в исходной структуре не задана; запись с дополнительно назначенной конфигурацией не будет принята.");
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final RuntimeException e) {
      return new Result(
          "",
          false,
          Objects.requireNonNullElse(
              e.getMessage(), "Для этой структуры точная проверка названия недоступна."));
    }
  }

  public boolean current(final MoleculeDrawing drawing, final MolecularIdentity identity) {
    if (!identity.matches(drawing.molecule())) {
      return false;
    }
    if (identity.origin() != MolecularIdentity.Origin.PUBCHEM && identity.smiles().isBlank()) {
      return true;
    }
    try {
      return matches(drawing, identity.smiles());
    } catch (final java.util.concurrent.CancellationException e) {
      throw e;
    } catch (final RuntimeException e) {
      return false;
    }
  }

  /** Without a drawing only the frozen core configuration can support a verified identity. */
  public boolean current(final Molecule molecule, final MolecularIdentity identity) {
    return current(MoleculeDrawing.of(molecule, List.of(), List.of(), List.of()), identity);
  }

  public boolean matches(final MoleculeDrawing drawing, final String candidate) {
    final IAtomContainer source = project(drawing);
    if (candidate == null
        || candidate.isBlank()
        || candidate.length() > 16000
        || candidate.codePoints().anyMatch(Character::isWhitespace)
        || candidate.contains("|")
        || candidate.contains(">")) {
      return false;
    }
    try {
      final IAtomContainer target =
          new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(candidate);
      if (target.getAtomCount() > 1000 || target.getSingleElectronCount() > 0) {
        return false;
      }
      for (final var element : target.stereoElements()) {
        if (element.getGroupInfo() != IStereoElement.GRP_ABS
            || !(element instanceof ITetrahedralChirality
                || element instanceof IDoubleBondStereochemistry)) {
          return false;
        }
      }
      normalize(target);
      final IAtomContainer compared = AtomContainerManipulator.suppressHydrogens(target);
      if (source.getAtomCount() != compared.getAtomCount()
          || source.getBondCount() != compared.getBondCount()) {
        return false;
      }
      if (stereoCount(source) != stereoCount(compared)) {
        return false;
      }
      final long deadline = System.nanoTime() + 1_000_000_000L;
      final AtomMatcher atoms =
          new AtomMatcher() {
            @Override
            public boolean matches(final IAtom a, final IAtom b) {
              guard(deadline);
              return Objects.equals(a.getAtomicNumber(), b.getAtomicNumber())
                  && Objects.equals(a.getFormalCharge(), b.getFormalCharge())
                  && Objects.equals(a.getMassNumber(), b.getMassNumber())
                  && Objects.equals(a.getImplicitHydrogenCount(), b.getImplicitHydrogenCount());
            }
          };
      final BondMatcher bonds =
          new BondMatcher() {
            @Override
            public boolean matches(final IBond a, final IBond b) {
              guard(deadline);
              return a.isAromatic() == b.isAromatic()
                  && (a.isAromatic() || a.getOrder() == b.getOrder());
            }
          };
      // CDK Pattern installs StereoMatch for a stereo query. Both directions also reject added or
      // lost assignments.
      return VentoFoggia.findIdentical(source, atoms, bonds).matches(compared)
          && VentoFoggia.findIdentical(compared, atoms, bonds).matches(source);
    } catch (final org.openscience.cdk.exception.CDKException e) {
      throw new IllegalArgumentException("Не удалось проверить SMILES найденной записи.", e);
    }
  }

  private static void guard(final long deadline) {
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException();
    }
    if (System.nanoTime() > deadline) {
      throw new IllegalArgumentException("Превышено время сопоставления структуры.");
    }
  }

  private static boolean stereoSpecified(final MoleculeDrawing drawing) {
    return MolecularRelativeStereo.present(drawing.molecule())
        || !drawing.molecule().stereochemistry().empty()
        || drawing
            .molecule()
            .metadata()
            .attributes()
            .containsKey(Stereochemistry.ORDERED_PARITY_ATTRIBUTE)
        || drawing.molecule().metadata().attributes().containsKey(Stereochemistry.REVIEW_ATTRIBUTE)
        || drawing.bonds().stream()
            .anyMatch(
                b ->
                    Set.of(
                            BondDrawingStyle.WEDGE_UP,
                            BondDrawingStyle.WEDGE_DOWN,
                            BondDrawingStyle.WAVY,
                            BondDrawingStyle.UNKNOWN)
                        .contains(b.style()));
  }

  private static int stereoCount(final IAtomContainer graph) {
    int count = 0;
    for (final var ignored : graph.stereoElements()) {
      count++;
    }
    return count;
  }

  private static IAtomContainer project(final MoleculeDrawing drawing) {
    if (!stereoSpecified(drawing)) {
      return project(drawing.molecule());
    }
    final var graph = CdkStereoIdentity.identityGraph(drawing);
    normalize(graph);
    return AtomContainerManipulator.suppressHydrogens(graph);
  }

  private static String smiles(final IAtomContainer graph) {
    try {
      return new SmilesGenerator(SmiFlavor.Isomeric).create(graph);
    } catch (final org.openscience.cdk.exception.CDKException e) {
      throw new IllegalArgumentException(
          "SMILES для этой структуры недоступен: " + e.getMessage(), e);
    }
  }

  private static IAtomContainer project(final Molecule molecule) {
    if (molecule.atomCount() == 0
        || molecule.atomCount() > 1000
        || molecule.atoms().stream().filter(a -> a.element().atomicNumber() != 1).count() > 256) {
      throw new IllegalArgumentException(
          "Идентификаторы доступны для 1–1000 атомов и не более 256 тяжёлых атомов.");
    }
    if (molecule.spinMultiplicity().value() != 1
        || molecule.atoms().stream().anyMatch(a -> a.radicalState().radical())) {
      throw new IllegalArgumentException(
          "Радикалы и заданный спин пока не поддержаны этим преобразованием SMILES.");
    }
    if (molecule.charge().value()
        != molecule.atoms().stream().mapToInt(a -> a.formalCharge().value()).sum()) {
      throw new IllegalArgumentException(
          "Сумма зарядов атомов отличается от заряда молекулы; сначала уточните структуру.");
    }
    final IAtomContainer graph = new AtomContainer();
    final Map<AtomId, IAtom> atoms = new HashMap<>();
    molecule
        .atoms()
        .forEach(
            a -> {
              final IAtom copy = new org.openscience.cdk.silent.Atom(a.element().symbol().value());
              copy.setFormalCharge(a.formalCharge().value());
              copy.setImplicitHydrogenCount(0);
              if (a.hasIsotope()) {
                copy.setMassNumber(a.isotope().massNumber());
              }
              atoms.put(a.id(), copy);
              graph.addAtom(copy);
            });
    for (final var b : molecule.bonds()) {
      final IBond.Order order =
          switch (b.type()) {
            case SINGLE, AROMATIC -> IBond.Order.SINGLE;
            case DOUBLE -> IBond.Order.DOUBLE;
            case TRIPLE -> IBond.Order.TRIPLE;
            default ->
                throw new IllegalArgumentException(
                    "SMILES не поддерживает неопределённые и координационные связи этой модели.");
          };
      final IBond copy =
          new org.openscience.cdk.silent.Bond(
              atoms.get(b.firstAtomId()), atoms.get(b.secondAtomId()), order);
      if (b.type() == BondType.AROMATIC) {
        copy.setIsAromatic(true);
        copy.getBegin().setIsAromatic(true);
        copy.getEnd().setIsAromatic(true);
      }
      graph.addBond(copy);
    }
    normalize(graph);
    return AtomContainerManipulator.suppressHydrogens(graph);
  }

  private static void normalize(final IAtomContainer graph) {
    try {
      if (java.util.stream.StreamSupport.stream(graph.bonds().spliterator(), false)
          .anyMatch(IBond::isAromatic)) {
        Kekulization.kekulize(graph);
      }
      Cycles.markRingAtomsAndBonds(graph);
      if (!Aromaticity.apply(Aromaticity.Model.Daylight, graph)) {
        throw new IllegalArgumentException("Ароматичность не определена полностью.");
      }
    } catch (final org.openscience.cdk.exception.CDKException e) {
      throw new IllegalArgumentException("Не удалось нормализовать ароматические связи.", e);
    }
  }
}
