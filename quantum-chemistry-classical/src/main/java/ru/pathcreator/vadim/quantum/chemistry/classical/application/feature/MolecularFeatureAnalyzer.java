/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.feature;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeature;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature.MolecularFeatureSet;
import ru.pathcreator.vadim.quantum.chemistry.domain.element.ElementSymbol;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularGraph;
import ru.pathcreator.vadim.quantum.chemistry.domain.graph.MolecularRing;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** Извлекает структурные и функциональные признаки из молекулярного графа. */
public final class MolecularFeatureAnalyzer {

  public MolecularFeatureSet analyze(final Molecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Molecule must not be null.");
    }
    final ArrayList<MolecularFeature> features = new ArrayList<MolecularFeature>(32);
    final MolecularGraph graph = molecule.graph();
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.HETERO_ATOM_COUNT,
        "hetero atoms",
        MolecularFeatureAnalyzer.heteroAtomCount(molecule));
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.RING_COUNT, "rings", graph.rings().size());
    MolecularFeatureAnalyzer.addRingFeatures(features, molecule, graph);
    MolecularFeatureAnalyzer.addFunctionalGroupFeatures(features, molecule);
    MolecularFeatureAnalyzer.addAtomStateFeatures(features, molecule);
    return MolecularFeatureSet.of(features);
  }

  private static long heteroAtomCount(final Molecule molecule) {
    long count = 0L;
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (MolecularFeatureAnalyzer.isHeteroAtom(atoms.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static void addRingFeatures(
      final List<MolecularFeature> features, final Molecule molecule, final MolecularGraph graph) {
    final List<MolecularRing> rings = graph.rings();
    long heterocycleCount = 0L;
    long aromaticRingCount = 0L;
    long fiveMemberedRingCount = 0L;
    long sixMemberedRingCount = 0L;
    long nitrogenHeterocycleCount = 0L;
    long oxygenHeterocycleCount = 0L;
    long sulfurHeterocycleCount = 0L;
    long epoxideCount = 0L;
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (ring.size() == 5) {
        ++fiveMemberedRingCount;
      }
      if (ring.size() == 6) {
        ++sixMemberedRingCount;
      }
      if (MolecularFeatureAnalyzer.isEpoxideRing(molecule, ring)) {
        ++epoxideCount;
      }
      if (MolecularFeatureAnalyzer.ringHasAromaticBond(molecule, ring)) {
        ++aromaticRingCount;
      }
      if (!MolecularFeatureAnalyzer.ringHasHeteroAtom(molecule, ring)) {
        continue;
      }
      ++heterocycleCount;
      if (MolecularFeatureAnalyzer.ringContains(molecule, ring, "N")) {
        ++nitrogenHeterocycleCount;
      }
      if (MolecularFeatureAnalyzer.ringContains(molecule, ring, "O")) {
        ++oxygenHeterocycleCount;
      }
      if (MolecularFeatureAnalyzer.ringContains(molecule, ring, "S")) {
        ++sulfurHeterocycleCount;
      }
    }
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.HETEROCYCLE, "heterocycles", heterocycleCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.AROMATIC_RING, "aromatic rings", aromaticRingCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.FIVE_MEMBERED_RING,
        "five-membered rings",
        fiveMemberedRingCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.SIX_MEMBERED_RING,
        "six-membered rings",
        sixMemberedRingCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.NITROGEN_HETEROCYCLE,
        "nitrogen heterocycles",
        nitrogenHeterocycleCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.OXYGEN_HETEROCYCLE,
        "oxygen heterocycles",
        oxygenHeterocycleCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.SULFUR_HETEROCYCLE,
        "sulfur heterocycles",
        sulfurHeterocycleCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.EPOXIDE, "epoxide rings", epoxideCount);
  }

  private static void addFunctionalGroupFeatures(
      final List<MolecularFeature> features, final Molecule molecule) {
    long carbonylCount = 0L;
    long aldehydeCount = 0L;
    long ketoneCount = 0L;
    long carboxylDerivativeCount = 0L;
    long carboxylicAcidOrCarboxylateCount = 0L;
    long acylHalideCount = 0L;
    long anhydrideCount = 0L;
    long esterCount = 0L;
    long amideCount = 0L;
    long alcoholCount = 0L;
    long phenolCount = 0L;
    long hydroxylOrAlkoxyCount = 0L;
    long etherCount = 0L;
    long amineCount = 0L;
    long imineCount = 0L;
    long nitrileCount = 0L;
    long nitroCount = 0L;
    long alkeneCount = 0L;
    long alkyneCount = 0L;
    long arylHalideCount = 0L;
    long heteroarylHalideCount = 0L;
    long primaryOrSecondaryAmineCount = 0L;
    long tertiaryAmineCount = 0L;
    long benzylOrBenzylicFragmentCount = 0L;
    long thiolOrThioetherCount = 0L;
    long sulfurOxideCount = 0L;
    long boronicAcidOrBoronateCount = 0L;
    long organometallicReagentCount = 0L;
    long isocyanideCount = 0L;
    long isocyanateCount = 0L;
    long activatedAlkeneCount = 0L;
    long alphaCarbonylCHCount = 0L;
    long goodLeavingGroupCount = 0L;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (MolecularFeatureAnalyzer.isCarbonCarbonBond(molecule, bond)
          && bond.type() == BondType.DOUBLE
          && !molecule.aromaticity().isAromaticBond(bond)) {
        ++alkeneCount;
      }
      if (MolecularFeatureAnalyzer.isCarbonCarbonBond(molecule, bond)
          && bond.type() == BondType.TRIPLE) {
        ++alkyneCount;
      }
      if (MolecularFeatureAnalyzer.isCarbonHalogenBond(molecule, bond)
          && (bond.type() == BondType.SINGLE || bond.type() == BondType.AROMATIC)) {
        final AtomId carbonId = MolecularFeatureAnalyzer.carbonEndpoint(molecule, bond);
        if (carbonId != null && MolecularFeatureAnalyzer.atomInAromaticRing(molecule, carbonId)) {
          ++arylHalideCount;
        }
        if (carbonId != null
            && MolecularFeatureAnalyzer.atomInAromaticHeterocycle(molecule, carbonId)) {
          ++heteroarylHalideCount;
        }
      }
      if (MolecularFeatureAnalyzer.isCarbonNitrogenBond(molecule, bond)
          && bond.type() == BondType.DOUBLE
          && !molecule.aromaticity().isAromaticBond(bond)) {
        ++imineCount;
      }
      if (MolecularFeatureAnalyzer.isCarbonNitrogenBond(molecule, bond)
          && bond.type() == BondType.TRIPLE) {
        ++nitrileCount;
        if (MolecularFeatureAnalyzer.isIsocyanideTripleBond(molecule, bond)) {
          ++isocyanideCount;
        }
      }
      if (bond.type() == BondType.DOUBLE
          && MolecularFeatureAnalyzer.isCarbonOxygenBond(molecule, bond)) {
        ++carbonylCount;
      }
      if (MolecularFeatureAnalyzer.isCarbonCarbonBond(molecule, bond)
          && bond.type() == BondType.DOUBLE
          && !molecule.aromaticity().isAromaticBond(bond)
          && MolecularFeatureAnalyzer.alkeneHasElectronWithdrawingNeighbor(molecule, bond)) {
        ++activatedAlkeneCount;
      }
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
    }
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final String symbol = atom.element().symbol().value();
      if ("O".equals(symbol)
          && MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C") > 0L) {
        ++hydroxylOrAlkoxyCount;
        if (MolecularFeatureAnalyzer.atomHasExplicitHydrogen(molecule, atom.id())) {
          final boolean aromaticCarbonNeighbor =
              MolecularFeatureAnalyzer.hasSingleBondToAromaticCarbon(molecule, atom.id());
          if (aromaticCarbonNeighbor) {
            ++phenolCount;
          } else {
            ++alcoholCount;
          }
        }
      }
      if ("O".equals(symbol)
          && MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C") >= 2L) {
        ++etherCount;
      }
      if ("N".equals(symbol)
          && !molecule.aromaticity().isAromaticAtom(atom.id())
          && MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C") > 0L) {
        ++amineCount;
      }
      if ("N".equals(symbol) && !molecule.aromaticity().isAromaticAtom(atom.id())) {
        final long carbonNeighborCount =
            MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C");
        if (carbonNeighborCount > 0L && carbonNeighborCount <= 2L) {
          ++primaryOrSecondaryAmineCount;
        }
        if (carbonNeighborCount >= 3L) {
          ++tertiaryAmineCount;
        }
      }
      if ("S".equals(symbol)
          && MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C") > 0L) {
        ++thiolOrThioetherCount;
      }
      if ("C".equals(symbol)
          && MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, atom.id(), "O")) {
        final long carbonSingleNeighborCount =
            MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C");
        final long explicitHydrogenNeighborCount =
            MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "H");
        if (explicitHydrogenNeighborCount > 0L) {
          ++aldehydeCount;
        }
        if (carbonSingleNeighborCount >= 2L) {
          ++ketoneCount;
        }
        if (MolecularFeatureAnalyzer.hasSingleBondToTerminalHetero(molecule, atom.id(), "O")) {
          ++carboxylDerivativeCount;
          ++carboxylicAcidOrCarboxylateCount;
        }
        if (MolecularFeatureAnalyzer.hasSingleBondToHalogen(molecule, atom.id())) {
          ++carboxylDerivativeCount;
          ++acylHalideCount;
        }
        if (MolecularFeatureAnalyzer.hasSingleBondToBridge(molecule, atom.id(), "O", "C")) {
          ++carboxylDerivativeCount;
          ++esterCount;
          if (MolecularFeatureAnalyzer.hasSingleBondToCarbonylCarbonThroughBridge(
              molecule, atom.id(), "O")) {
            ++anhydrideCount;
          }
        }
        if (MolecularFeatureAnalyzer.hasSingleBondTo(molecule, atom.id(), "N")) {
          ++carboxylDerivativeCount;
          ++amideCount;
        }
      }
      if ("C".equals(symbol)
          && MolecularFeatureAnalyzer.atomHasExplicitHydrogen(molecule, atom.id())
          && MolecularFeatureAnalyzer.hasSingleBondToCarbonylCarbon(molecule, atom.id())) {
        ++alphaCarbonylCHCount;
      }
      if ("N".equals(symbol)
          && MolecularFeatureAnalyzer.oxygenNeighborCount(molecule, atom.id()) >= 2L) {
        ++nitroCount;
      }
      if ("N".equals(symbol)
          && MolecularFeatureAnalyzer.hasIsocyanateTopology(molecule, atom.id())) {
        ++isocyanateCount;
      }
      if ("S".equals(symbol)
          && MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, atom.id(), "O")) {
        ++sulfurOxideCount;
      }
      if ("C".equals(symbol)
          && MolecularFeatureAnalyzer.benzylicHeteroatomHandle(molecule, atom.id())) {
        ++benzylOrBenzylicFragmentCount;
      }
      if ("B".equals(symbol)
          && MolecularFeatureAnalyzer.oxygenNeighborCount(molecule, atom.id()) >= 2L) {
        ++boronicAcidOrBoronateCount;
      }
      if (MolecularFeatureAnalyzer.isOrganometallicAtom(symbol)
          && MolecularFeatureAnalyzer.singleNeighborCount(molecule, atom.id(), "C") > 0L) {
        ++organometallicReagentCount;
      }
    }
    goodLeavingGroupCount =
        arylHalideCount
            + heteroarylHalideCount
            + MolecularFeatureAnalyzer.sulfonylLeavingGroupCount(molecule);
    final long electronWithdrawingGroupCount =
        nitrileCount
            + nitroCount
            + carbonylCount
            + sulfurOxideCount
            + arylHalideCount
            + isocyanateCount;
    final long electronPoorHeteroareneCount =
        heterocycleCount(molecule) == 0L
            ? 0L
            : nitrogenHeterocycleLikeCount(molecule) + electronWithdrawingGroupCount;
    final long activatedNucleophileCount =
        primaryOrSecondaryAmineCount + thiolOrThioetherCount + hydroxylOrAlkoxyCount;
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.CARBONYL, "carbonyl groups", carbonylCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ALDEHYDE, "aldehyde groups", aldehydeCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.KETONE, "ketone groups", ketoneCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.CARBOXYL_DERIVATIVE,
        "carboxyl derivatives",
        carboxylDerivativeCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ACYL_HALIDE, "acyl halides", acylHalideCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ANHYDRIDE, "anhydrides", anhydrideCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.CARBOXYLIC_ACID_OR_CARBOXYLATE,
        "carboxylic acid or carboxylate groups",
        carboxylicAcidOrCarboxylateCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ESTER, "ester groups", esterCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.AMIDE, "amide groups", amideCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.HYDROXYL_OR_ALKOXY,
        "hydroxyl or alkoxy groups",
        hydroxylOrAlkoxyCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ALCOHOL, "alcohol groups", alcoholCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.PHENOL, "phenol groups", phenolCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ETHER, "ether groups", etherCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.AMINE, "amine groups", amineCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.IMINE, "imine groups", imineCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.NITRILE, "nitrile groups", nitrileCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.NITRO, "nitro groups", nitroCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ALKENE, "alkene groups", alkeneCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ALKYNE, "alkyne groups", alkyneCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ARYL_HALIDE, "aryl halide handles", arylHalideCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.HETEROARYL_HALIDE,
        "heteroaryl halide handles",
        heteroarylHalideCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.ELECTRON_WITHDRAWING_GROUP,
        "electron-withdrawing groups",
        electronWithdrawingGroupCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.ELECTRON_POOR_HETEROARENE,
        "electron-poor heteroarenes",
        electronPoorHeteroareneCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.PRIMARY_OR_SECONDARY_AMINE,
        "primary or secondary amines",
        primaryOrSecondaryAmineCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.TERTIARY_AMINE, "tertiary amines", tertiaryAmineCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.BENZYL_OR_BENZYLIC_FRAGMENT,
        "benzyl or benzylic fragments",
        benzylOrBenzylicFragmentCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.ACTIVATED_NUCLEOPHILE,
        "activated nucleophile handles",
        activatedNucleophileCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.THIOL_OR_THIOETHER,
        "thiol or thioether groups",
        thiolOrThioetherCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.SULFUR_OXIDE, "sulfur oxide groups", sulfurOxideCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.BORONIC_ACID_OR_BORONATE,
        "boronic acid or boronate handles",
        boronicAcidOrBoronateCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.ORGANOMETALLIC_REAGENT,
        "organometallic reagent handles",
        organometallicReagentCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ISOCYANIDE, "isocyanide groups", isocyanideCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ISOCYANATE, "isocyanate groups", isocyanateCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.ACTIVATED_ALKENE, "activated alkenes", activatedAlkeneCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.ALPHA_CARBONYL_C_H,
        "alpha carbonyl C-H handles",
        alphaCarbonylCHCount);
    MolecularFeatureAnalyzer.addPositive(
        features,
        MolecularFeatureKind.GOOD_LEAVING_GROUP,
        "good leaving group handles",
        goodLeavingGroupCount);
  }

  private static void addAtomStateFeatures(
      final List<MolecularFeature> features, final Molecule molecule) {
    long halogenCount = 0L;
    long chargedCenterCount = 0L;
    long radicalCenterCount = 0L;
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      final String symbol = atom.element().symbol().value();
      if ("F".equals(symbol) || "Cl".equals(symbol) || "Br".equals(symbol) || "I".equals(symbol)) {
        ++halogenCount;
      }
      if (atom.formalCharge().value() != 0) {
        ++chargedCenterCount;
      }
      if (atom.radicalState().radical()) {
        ++radicalCenterCount;
      }
    }
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.HALOGENATED, "halogen atoms", halogenCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.CHARGED_CENTER, "charged centers", chargedCenterCount);
    MolecularFeatureAnalyzer.addPositive(
        features, MolecularFeatureKind.RADICAL_CENTER, "radical centers", radicalCenterCount);
  }

  private static boolean isCarbonOxygenBond(final Molecule molecule, final Bond bond) {
    return MolecularFeatureAnalyzer.isBondBetween(molecule, bond, "C", "O");
  }

  private static boolean isCarbonNitrogenBond(final Molecule molecule, final Bond bond) {
    return MolecularFeatureAnalyzer.isBondBetween(molecule, bond, "C", "N");
  }

  private static boolean isCarbonCarbonBond(final Molecule molecule, final Bond bond) {
    return MolecularFeatureAnalyzer.isBondBetween(molecule, bond, "C", "C");
  }

  private static boolean isCarbonHalogenBond(final Molecule molecule, final Bond bond) {
    final String first = MolecularFeatureAnalyzer.atomSymbol(molecule, bond.firstAtomId()).value();
    final String second =
        MolecularFeatureAnalyzer.atomSymbol(molecule, bond.secondAtomId()).value();
    return "C".equals(first) && MolecularFeatureAnalyzer.isHalogenSymbol(second)
        || "C".equals(second) && MolecularFeatureAnalyzer.isHalogenSymbol(first);
  }

  private static AtomId carbonEndpoint(final Molecule molecule, final Bond bond) {
    if ("C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, bond.firstAtomId()).value())) {
      return bond.firstAtomId();
    }
    if ("C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, bond.secondAtomId()).value())) {
      return bond.secondAtomId();
    }
    return null;
  }

  private static boolean isBondBetween(
      final Molecule molecule,
      final Bond bond,
      final String firstExpectedSymbol,
      final String secondExpectedSymbol) {
    final String first = MolecularFeatureAnalyzer.atomSymbol(molecule, bond.firstAtomId()).value();
    final String second =
        MolecularFeatureAnalyzer.atomSymbol(molecule, bond.secondAtomId()).value();
    return firstExpectedSymbol.equals(first) && secondExpectedSymbol.equals(second)
        || firstExpectedSymbol.equals(second) && secondExpectedSymbol.equals(first);
  }

  private static boolean hasDoubleBondTo(
      final Molecule molecule, final AtomId atomId, final String neighborSymbol) {
    return MolecularFeatureAnalyzer.hasBondTo(molecule, atomId, neighborSymbol, BondType.DOUBLE);
  }

  private static boolean hasSingleBondTo(
      final Molecule molecule, final AtomId atomId, final String neighborSymbol) {
    return MolecularFeatureAnalyzer.hasBondTo(molecule, atomId, neighborSymbol, BondType.SINGLE)
        || MolecularFeatureAnalyzer.hasBondTo(molecule, atomId, neighborSymbol, BondType.AROMATIC);
  }

  private static boolean hasSingleBondToHalogen(final Molecule molecule, final AtomId atomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null) {
        continue;
      }
      if (MolecularFeatureAnalyzer.isHalogenSymbol(
          MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean atomHasExplicitHydrogen(final Molecule molecule, final AtomId atomId) {
    return MolecularFeatureAnalyzer.singleNeighborCount(molecule, atomId, "H") > 0L;
  }

  private static boolean hasSingleBondToAromaticCarbon(
      final Molecule molecule, final AtomId atomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null) {
        continue;
      }
      if ("C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())
          && MolecularFeatureAnalyzer.atomInAromaticRing(molecule, neighborId)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasBondTo(
      final Molecule molecule,
      final AtomId atomId,
      final String neighborSymbol,
      final BondType type) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != type) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null) {
        continue;
      }
      if (neighborSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasSingleBondToBridge(
      final Molecule molecule,
      final AtomId atomId,
      final String bridgeSymbol,
      final String bridgeNeighborSymbol) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId bridgeAtomId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (bridgeAtomId == null) {
        continue;
      }
      if (!bridgeSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, bridgeAtomId).value())) {
        continue;
      }
      if (MolecularFeatureAnalyzer.hasSingleBondToOtherThan(
          molecule, bridgeAtomId, atomId, bridgeNeighborSymbol)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasSingleBondToCarbonylCarbonThroughBridge(
      final Molecule molecule, final AtomId atomId, final String bridgeSymbol) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bridgeBond = bonds.get(i);
      if (bridgeBond.type() != BondType.SINGLE && bridgeBond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId bridgeAtomId = MolecularFeatureAnalyzer.otherEndpoint(bridgeBond, atomId);
      if (bridgeAtomId == null) {
        continue;
      }
      if (!bridgeSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, bridgeAtomId).value())) {
        continue;
      }
      if (MolecularFeatureAnalyzer.hasCarbonylCarbonNeighborOtherThan(
          molecule, bridgeAtomId, atomId)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasCarbonylCarbonNeighborOtherThan(
      final Molecule molecule, final AtomId atomId, final AtomId excludedAtomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null || neighborId.equals(excludedAtomId)) {
        continue;
      }
      if ("C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())
          && MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, neighborId, "O")) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasSingleBondToTerminalHetero(
      final Molecule molecule, final AtomId atomId, final String heteroSymbol) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId heteroAtomId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (heteroAtomId == null) {
        continue;
      }
      if (!heteroSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, heteroAtomId).value())) {
        continue;
      }
      if (!MolecularFeatureAnalyzer.hasSingleBondToOtherThan(molecule, heteroAtomId, atomId, "C")) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasSingleBondToOtherThan(
      final Molecule molecule,
      final AtomId atomId,
      final AtomId excludedAtomId,
      final String neighborSymbol) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null || neighborId.equals(excludedAtomId)) {
        continue;
      }
      if (neighborSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean benzylicHeteroatomHandle(final Molecule molecule, final AtomId atomId) {
    if (!"C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, atomId).value())
        || molecule.aromaticity().isAromaticAtom(atomId)) {
      return false;
    }
    boolean aromaticCarbonNeighbor = false;
    boolean heteroatomNeighbor = false;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null) {
        continue;
      }
      final String neighborSymbol =
          MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value();
      aromaticCarbonNeighbor |=
          "C".equals(neighborSymbol)
              && MolecularFeatureAnalyzer.atomInAromaticRing(molecule, neighborId);
      heteroatomNeighbor |=
          MolecularFeatureAnalyzer.isHeteroSymbol(ElementSymbol.of(neighborSymbol));
    }
    return aromaticCarbonNeighbor && heteroatomNeighbor;
  }

  private static long oxygenNeighborCount(final Molecule molecule, final AtomId atomId) {
    long count = 0L;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bonds.get(i), atomId);
      if (neighborId == null) {
        continue;
      }
      if ("O".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())) {
        ++count;
      }
    }
    return count;
  }

  private static long singleNeighborCount(
      final Molecule molecule, final AtomId atomId, final String neighborSymbol) {
    long count = 0L;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null) {
        continue;
      }
      if (neighborSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())) {
        ++count;
      }
    }
    return count;
  }

  private static AtomId otherEndpoint(final Bond bond, final AtomId atomId) {
    if (bond.firstAtomId().equals(atomId)) {
      return bond.secondAtomId();
    }
    if (bond.secondAtomId().equals(atomId)) {
      return bond.firstAtomId();
    }
    return null;
  }

  private static boolean ringHasHeteroAtom(final Molecule molecule, final MolecularRing ring) {
    final List<AtomId> atomIds = ring.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      if (MolecularFeatureAnalyzer.isHeteroSymbol(
          MolecularFeatureAnalyzer.atomSymbol(molecule, atomIds.get(i)))) {
        return true;
      }
    }
    return false;
  }

  private static boolean isEpoxideRing(final Molecule molecule, final MolecularRing ring) {
    if (ring.size() != 3) {
      return false;
    }
    return MolecularFeatureAnalyzer.ringContains(molecule, ring, "O")
        && MolecularFeatureAnalyzer.ringCarbonCount(molecule, ring) == 2L
        && MolecularFeatureAnalyzer.ringSingleBondCount(molecule, ring) == 3L;
  }

  private static long ringCarbonCount(final Molecule molecule, final MolecularRing ring) {
    long count = 0L;
    final List<AtomId> atomIds = ring.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      if ("C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, atomIds.get(i)).value())) {
        ++count;
      }
    }
    return count;
  }

  private static long ringSingleBondCount(final Molecule molecule, final MolecularRing ring) {
    long count = 0L;
    final List<AtomId> atomIds = ring.atomIds();
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() == BondType.SINGLE
          && atomIds.contains(bond.firstAtomId())
          && atomIds.contains(bond.secondAtomId())) {
        ++count;
      }
    }
    return count;
  }

  private static boolean ringContains(
      final Molecule molecule, final MolecularRing ring, final String expectedSymbol) {
    final List<AtomId> atomIds = ring.atomIds();
    for (int i = 0; i < atomIds.size(); ++i) {
      if (expectedSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, atomIds.get(i)).value())) {
        return true;
      }
    }
    return false;
  }

  private static boolean ringHasAromaticBond(final Molecule molecule, final MolecularRing ring) {
    return molecule.aromaticity().isAromaticRing(ring);
  }

  private static boolean atomInAromaticRing(final Molecule molecule, final AtomId atomId) {
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (ring.atomIds().contains(atomId)
          && MolecularFeatureAnalyzer.ringHasAromaticBond(molecule, ring)) {
        return true;
      }
    }
    return false;
  }

  private static boolean atomInAromaticHeterocycle(final Molecule molecule, final AtomId atomId) {
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      final MolecularRing ring = rings.get(i);
      if (!ring.atomIds().contains(atomId)) {
        continue;
      }
      if (MolecularFeatureAnalyzer.ringHasAromaticBond(molecule, ring)
          && MolecularFeatureAnalyzer.ringHasHeteroAtom(molecule, ring)) {
        return true;
      }
    }
    return false;
  }

  private static long heterocycleCount(final Molecule molecule) {
    long count = 0L;
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      if (MolecularFeatureAnalyzer.ringHasHeteroAtom(molecule, rings.get(i))) {
        ++count;
      }
    }
    return count;
  }

  private static long nitrogenHeterocycleLikeCount(final Molecule molecule) {
    long count = 0L;
    final List<MolecularRing> rings = molecule.graph().rings();
    for (int i = 0; i < rings.size(); ++i) {
      if (MolecularFeatureAnalyzer.ringContains(molecule, rings.get(i), "N")) {
        ++count;
      }
    }
    return count;
  }

  private static boolean isIsocyanideTripleBond(final Molecule molecule, final Bond bond) {
    if (!MolecularFeatureAnalyzer.isCarbonNitrogenBond(molecule, bond)
        || bond.type() != BondType.TRIPLE) {
      return false;
    }
    final AtomId carbonId = MolecularFeatureAnalyzer.atomIdForSymbol(molecule, bond, "C");
    final AtomId nitrogenId = MolecularFeatureAnalyzer.atomIdForSymbol(molecule, bond, "N");
    if (carbonId == null || nitrogenId == null) {
      return false;
    }
    return MolecularFeatureAnalyzer.singleNeighborCountExcluding(
            molecule, nitrogenId, carbonId, "C")
        > 0L;
  }

  private static boolean hasIsocyanateTopology(final Molecule molecule, final AtomId nitrogenId) {
    if (!"N".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, nitrogenId).value())) {
      return false;
    }
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond firstBond = bonds.get(i);
      if (firstBond.type() != BondType.SINGLE) {
        continue;
      }
      final AtomId carbonId = MolecularFeatureAnalyzer.otherEndpoint(firstBond, nitrogenId);
      if (carbonId == null
          || !"C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, carbonId).value())) {
        continue;
      }
      if (MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, carbonId, "O")) {
        return true;
      }
    }
    return false;
  }

  private static boolean alkeneHasElectronWithdrawingNeighbor(
      final Molecule molecule, final Bond alkeneBond) {
    final AtomId first = alkeneBond.firstAtomId();
    final AtomId second = alkeneBond.secondAtomId();
    return MolecularFeatureAnalyzer.hasElectronWithdrawingNeighbor(molecule, first, second)
        || MolecularFeatureAnalyzer.hasElectronWithdrawingNeighbor(molecule, second, first);
  }

  private static boolean hasElectronWithdrawingNeighbor(
      final Molecule molecule, final AtomId atomId, final AtomId excludedAtomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null || neighborId.equals(excludedAtomId)) {
        continue;
      }
      final String symbol = MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value();
      if (MolecularFeatureAnalyzer.isElectronWithdrawingAtomSymbol(symbol)) {
        return true;
      }
      if ("C".equals(symbol)
          && (MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, neighborId, "O")
              || MolecularFeatureAnalyzer.hasTripleBondTo(molecule, neighborId, "N"))) {
        return true;
      }
      if ("S".equals(symbol)
          && MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, neighborId, "O")) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasSingleBondToCarbonylCarbon(
      final Molecule molecule, final AtomId atomId) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null) {
        continue;
      }
      if ("C".equals(MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())
          && MolecularFeatureAnalyzer.hasDoubleBondTo(molecule, neighborId, "O")) {
        return true;
      }
    }
    return false;
  }

  private static long sulfonylLeavingGroupCount(final Molecule molecule) {
    long count = 0L;
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!"S".equals(atom.element().symbol().value())) {
        continue;
      }
      if (MolecularFeatureAnalyzer.oxygenNeighborCount(molecule, atom.id()) < 2L) {
        continue;
      }
      if (MolecularFeatureAnalyzer.hasSingleBondToHalogen(molecule, atom.id())
          || MolecularFeatureAnalyzer.hasSingleBondTo(molecule, atom.id(), "O")) {
        ++count;
      }
    }
    return count;
  }

  private static AtomId atomIdForSymbol(
      final Molecule molecule, final Bond bond, final String symbol) {
    if (symbol.equals(MolecularFeatureAnalyzer.atomSymbol(molecule, bond.firstAtomId()).value())) {
      return bond.firstAtomId();
    }
    if (symbol.equals(MolecularFeatureAnalyzer.atomSymbol(molecule, bond.secondAtomId()).value())) {
      return bond.secondAtomId();
    }
    return null;
  }

  private static boolean hasTripleBondTo(
      final Molecule molecule, final AtomId atomId, final String neighborSymbol) {
    return MolecularFeatureAnalyzer.hasBondTo(molecule, atomId, neighborSymbol, BondType.TRIPLE);
  }

  private static long singleNeighborCountExcluding(
      final Molecule molecule,
      final AtomId atomId,
      final AtomId excludedAtomId,
      final String neighborSymbol) {
    long count = 0L;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (bond.type() != BondType.SINGLE && bond.type() != BondType.AROMATIC) {
        continue;
      }
      final AtomId neighborId = MolecularFeatureAnalyzer.otherEndpoint(bond, atomId);
      if (neighborId == null || neighborId.equals(excludedAtomId)) {
        continue;
      }
      if (neighborSymbol.equals(
          MolecularFeatureAnalyzer.atomSymbol(molecule, neighborId).value())) {
        ++count;
      }
    }
    return count;
  }

  private static boolean isElectronWithdrawingAtomSymbol(final String symbol) {
    return "N".equals(symbol)
        || "O".equals(symbol)
        || "S".equals(symbol)
        || "P".equals(symbol)
        || MolecularFeatureAnalyzer.isHalogenSymbol(symbol);
  }

  private static boolean isOrganometallicAtom(final String symbol) {
    return "Li".equals(symbol)
        || "Mg".equals(symbol)
        || "Zn".equals(symbol)
        || "Sn".equals(symbol)
        || "Cu".equals(symbol)
        || "B".equals(symbol)
        || "Si".equals(symbol);
  }

  private static boolean isHeteroAtom(final Atom atom) {
    return MolecularFeatureAnalyzer.isHeteroSymbol(atom.element().symbol());
  }

  private static boolean isHeteroSymbol(final ElementSymbol symbol) {
    final String value = symbol.value();
    return !("C".equals(value) || "H".equals(value));
  }

  private static boolean isHalogenSymbol(final String value) {
    return "F".equals(value) || "Cl".equals(value) || "Br".equals(value) || "I".equals(value);
  }

  private static ElementSymbol atomSymbol(final Molecule molecule, final AtomId atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().equals(atomId)) {
        return atom.element().symbol();
      }
    }
    throw new IllegalArgumentException("Molecule does not contain atom id.");
  }

  private static void addPositive(
      final List<MolecularFeature> features,
      final MolecularFeatureKind kind,
      final String label,
      final long count) {
    if (count > 0L) {
      features.add(MolecularFeature.of(kind, label, count, 1.0));
    }
  }
}
