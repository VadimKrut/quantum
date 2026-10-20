/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.layout;

import java.util.*;
import javax.vecmath.Point2d;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.layout.HydrogenPlacer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.AtomContainer;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;
import ru.pathcreator.vadim.quantum.chemistry.drawing.application.MoleculeLayoutProvider;

/** Uses CDK only on a disposable display projection. No chemical result is imported from CDK. */
public final class CdkMoleculeLayout implements MoleculeLayoutProvider {
  private static final System.Logger LOGGER = System.getLogger(CdkMoleculeLayout.class.getName());

  public Optional<double[]> coordinates(final Molecule molecule, final double bondLength) {
    Objects.requireNonNull(molecule);
    if (!Double.isFinite(bondLength) || bondLength <= 0) {
      throw new IllegalArgumentException("Drawing bond length must be positive.");
    }
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Drawing layout interrupted.");
    }
    if (molecule.atomCount() == 0) {
      return Optional.of(new double[0]);
    }
    if (molecule.atomCount() > 500
        || molecule.atoms().stream().filter(a -> a.element().atomicNumber() != 1).count() > 160
        || molecule.bonds().stream()
            .anyMatch(b -> b.type() == BondType.COORDINATE || b.type() == BondType.UNKNOWN)) {
      return Optional.empty();
    }
    try {
      return Optional.of(this.project(molecule, bondLength));
    } catch (final java.util.concurrent.CancellationException exception) {
      throw exception;
    } catch (final CDKException | RuntimeException exception) {
      LOGGER.log(
          System.Logger.Level.WARNING,
          "2D layout adapter declined a graph; using the core display layout.",
          exception);
      return Optional.empty();
    }
  }

  private double[] project(final Molecule molecule, final double bondLength) throws CDKException {
    final var full = new AtomContainer();
    final Map<AtomId, IAtom> atoms = new LinkedHashMap<>();
    final Map<AtomId, List<Bond>> incident = new HashMap<>();
    for (final var atom : molecule.atoms()) {
      final var copy = new org.openscience.cdk.silent.Atom(atom.element().symbol().value());
      copy.setID(atom.id().value());
      copy.setFormalCharge(atom.formalCharge().value());
      copy.setImplicitHydrogenCount(0);
      if (atom.isotope() != null) {
        copy.setMassNumber(atom.isotope().massNumber());
      }
      full.addAtom(copy);
      atoms.put(atom.id(), copy);
      incident.put(atom.id(), new ArrayList<>());
    }
    for (final var bond : molecule.bonds()) {
      incident.get(bond.firstAtomId()).add(bond);
      incident.get(bond.secondAtomId()).add(bond);
      final var copy =
          new org.openscience.cdk.silent.Bond(
              atoms.get(bond.firstAtomId()),
              atoms.get(bond.secondAtomId()),
              switch (bond.type()) {
                case DOUBLE -> IBond.Order.DOUBLE;
                case TRIPLE -> IBond.Order.TRIPLE;
                case SINGLE, AROMATIC -> IBond.Order.SINGLE;
                default -> throw new IllegalArgumentException("Unsupported display bond.");
              });
      if (bond.type() == BondType.AROMATIC) {
        copy.setIsAromatic(true);
        copy.getBegin().setIsAromatic(true);
        copy.getEnd().setIsAromatic(true);
      }
      full.addBond(copy);
    }
    // These counts come exclusively from existing explicit H atoms, never from inferred valence.
    final Set<String> hiddenHydrogens = new HashSet<>();
    for (final var atom : molecule.atoms()) {
      final var bonds = incident.get(atom.id());
      if (atom.element().atomicNumber() != 1
          || atom.isotope() != null
          || atom.formalCharge().value() != 0
          || atom.radicalState().radical()
          || bonds.size() != 1
          || bonds.getFirst().type() != BondType.SINGLE) {
        continue;
      }
      final var bond = bonds.getFirst();
      final var parent =
          atoms.get(
              bond.firstAtomId().equals(atom.id()) ? bond.secondAtomId() : bond.firstAtomId());
      if (parent.getAtomicNumber() == 1) {
        continue;
      }
      hiddenHydrogens.add(atom.id().value());
      parent.setImplicitHydrogenCount(parent.getImplicitHydrogenCount() + 1);
    }
    final var skeleton = new AtomContainer();
    for (final var atom : full.atoms()) {
      if (!hiddenHydrogens.contains(atom.getID())) {
        skeleton.addAtom(atom);
      }
    }
    for (final var bond : full.bonds()) {
      if (!hiddenHydrogens.contains(bond.getBegin().getID())
          && !hiddenHydrogens.contains(bond.getEnd().getID())) {
        skeleton.addBond(bond);
      }
    }
    final var generator = new StructureDiagramGenerator();
    generator.generateCoordinates(skeleton);
    new HydrogenPlacer().placeHydrogens2D(full, generator.getBondLength());
    if (Thread.currentThread().isInterrupted()) {
      throw new java.util.concurrent.CancellationException("Drawing layout interrupted.");
    }
    final Map<String, Point2d> positions = new HashMap<>();
    for (final var atom : full.atoms()) {
      positions.put(atom.getID(), atom.getPoint2d());
    }
    final double[] result = new double[molecule.atomCount() * 2];
    final double scale = bondLength / generator.getBondLength();
    for (int index = 0; index < molecule.atomCount(); index++) {
      final var position = positions.get(molecule.atoms().get(index).id().value());
      if (position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y)) {
        throw new IllegalStateException("Incomplete display coordinates.");
      }
      result[index * 2] = position.x * scale;
      result[index * 2 + 1] = -position.y * scale;
    }
    return result;
  }
}
