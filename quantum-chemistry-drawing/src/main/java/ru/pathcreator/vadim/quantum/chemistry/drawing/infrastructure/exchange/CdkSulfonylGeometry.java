/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.exchange;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Corrects one known builder degeneracy; this is an initial local placement, not energy minimisation. */
final class CdkSulfonylGeometry {
  private CdkSulfonylGeometry() {}

  /** Called only for newly generated, mapped candidates with complete finite coordinates. */
  static int repair(final Molecule source, final IAtomContainer graph) {
    if (source.charge().value() != 0
        || source.spinMultiplicity().value() != 1
        || source.atoms().stream()
            .anyMatch(a -> a.formalCharge().value() != 0 || a.radicalState().radical())) {
      return 0;
    }
    int changed = 0;
    for (final IAtom sulfur : graph.atoms()) {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      if (!element(sulfur, 16) || sulfur.isAromatic() || !neutral(sulfur)) {
        continue;
      }
      final var bonds = graph.getConnectedBondsList(sulfur);
      if (bonds.size() != 4) {
        continue;
      }
      final var oxo = new ArrayList<IAtom>();
      final var fixed = new ArrayList<IAtom>();
      boolean eligible = true;
      for (final IBond bond : bonds) {
        final IAtom atom = bond.getOther(sulfur);
        if (bond.isAromatic() || !neutral(atom)) {
          eligible = false;
          break;
        }
        if (bond.getOrder() == IBond.Order.DOUBLE
            && element(atom, 8)
            && graph.getConnectedBondsCount(atom) == 1
            && !atom.isAromatic()) {
          oxo.add(atom);
        } else if (bond.getOrder() == IBond.Order.SINGLE) {
          fixed.add(atom);
        } else {
          eligible = false;
          break;
        }
      }
      // Organic sulfonyl only: exclude sulfates, sulfites, charged forms and isotope-distinguished
      // oxo ligands.
      if (!eligible
          || oxo.size() != 2
          || fixed.size() != 2
          || fixed.stream().noneMatch(a -> element(a, 6))
          || !Objects.equals(oxo.get(0).getMassNumber(), oxo.get(1).getMassNumber())) {
        continue;
      }
      if (oxo.get(0).getPoint3d().distance(oxo.get(1).getPoint3d()) >= .35) {
        continue;
      }
      final Point3d center = sulfur.getPoint3d();
      final double firstLength = center.distance(oxo.get(0).getPoint3d()),
          secondLength = center.distance(oxo.get(1).getPoint3d());
      if (firstLength < 1.1 || firstLength > 2.1 || secondLength < 1.1 || secondLength > 2.1) {
        continue;
      }
      final Vector3d u = direction(center, fixed.get(0).getPoint3d()),
          v = direction(center, fixed.get(1).getPoint3d());
      if (u == null || v == null) {
        continue;
      }
      // Do not try to rescue a collapsed or nearly linear fixed skeleton.
      final double cosine = u.dot(v);
      if (cosine < Math.cos(Math.toRadians(140)) || cosine > Math.cos(Math.toRadians(80))) {
        continue;
      }
      final Vector3d bisector = new Vector3d(u);
      bisector.add(v);
      bisector.normalize();
      bisector.negate();
      final Vector3d normal = new Vector3d();
      normal.cross(u, v);
      normal.normalize();
      // Two equivalent terminal oxygens occupy the free tetrahedral directions. Fixed branches
      // never move.
      final double along = Math.sqrt(1.0 / 3), across = Math.sqrt(2.0 / 3);
      oxo.get(0).setPoint3d(place(center, bisector, normal, along, across, firstLength));
      oxo.get(1).setPoint3d(place(center, bisector, normal, along, -across, secondLength));
      changed++;
    }
    return changed;
  }

  private static boolean element(final IAtom atom, final int number) {
    return Objects.equals(atom.getAtomicNumber(), number);
  }

  private static boolean neutral(final IAtom atom) {
    return Objects.equals(atom.getFormalCharge(), 0);
  }

  private static Vector3d direction(final Point3d from, final Point3d to) {
    final var vector = new Vector3d(to);
    vector.sub(from);
    final double length = vector.length();
    if (!Double.isFinite(length) || length < .4 || length > 3.5) {
      return null;
    }
    vector.scale(1 / length);
    return vector;
  }

  private static Point3d place(
      final Point3d center,
      final Vector3d bisector,
      final Vector3d normal,
      final double along,
      final double across,
      final double length) {
    final var point = new Point3d(center);
    point.x += length * (along * bisector.x + across * normal.x);
    point.y += length * (along * bisector.y + across * normal.y);
    point.z += length * (along * bisector.z + across * normal.z);
    return point;
  }
}
