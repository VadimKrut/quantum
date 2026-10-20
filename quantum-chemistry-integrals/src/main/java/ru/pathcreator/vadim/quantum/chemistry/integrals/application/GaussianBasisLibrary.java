/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.integrals.application;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.ContractedGaussianOrbital;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianAngularMomentum;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianBasisSet;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianPrimitive;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.basis.GaussianShellType;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralAtom;
import ru.pathcreator.vadim.quantum.chemistry.integrals.domain.molecule.IntegralMolecule;

/**
 * Встроенные Gaussian basis definitions для Java-native pipeline без внешнего basis repository.
 */
public final class GaussianBasisLibrary {

  public static final String GENERATED_EVEN_TEMPERED_NAME = "AUTO-EVEN-TEMPERED";
  public static final String GENERATED_COMPACT_EVEN_TEMPERED_NAME = "AUTO-EVEN-TEMPERED-COMPACT";

  private GaussianBasisLibrary() {}

  public static GaussianBasisSet sto3g(final IntegralMolecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("STO-3G molecule must not be null.");
    }
    final String unsupportedReason = GaussianBasisLibrary.unsupportedSto3gReason(molecule);
    if (unsupportedReason != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    final List<IntegralAtom> atoms = molecule.atoms();
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(atoms.size() * 5);
    for (int i = 0; i < atoms.size(); ++i) {
      final IntegralAtom atom = atoms.get(i);
      GaussianBasisLibrary.addSto3gAtomOrbitals(atom.atomicNumber(), i, orbitals);
    }
    return GaussianBasisSet.of("STO-3G", orbitals);
  }

  public static GaussianBasisSet six31g(final IntegralMolecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("6-31G molecule must not be null.");
    }
    final String unsupportedReason = GaussianBasisLibrary.unsupportedSix31gReason(molecule);
    if (unsupportedReason != null) {
      throw new IllegalArgumentException(unsupportedReason);
    }
    final List<IntegralAtom> atoms = molecule.atoms();
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(atoms.size() * 9);
    for (int i = 0; i < atoms.size(); ++i) {
      final IntegralAtom atom = atoms.get(i);
      GaussianBasisLibrary.addSix31gAtomOrbitals(atom.atomicNumber(), i, orbitals);
    }
    return GaussianBasisSet.of("6-31G", orbitals);
  }

  public static GaussianBasisSet generatedEvenTempered(final IntegralMolecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("Generated even-tempered molecule must not be null.");
    }
    final List<IntegralAtom> atoms = molecule.atoms();
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(atoms.size() * 18);
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      GaussianBasisLibrary.addGeneratedEvenTemperedAtomOrbitals(
          atoms.get(atomIndex).atomicNumber(), atomIndex, orbitals);
    }
    return GaussianBasisSet.of(GENERATED_EVEN_TEMPERED_NAME, orbitals);
  }

  public static GaussianBasisSet generatedCompactEvenTempered(final IntegralMolecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException(
          "Generated compact even-tempered molecule must not be null.");
    }
    final List<IntegralAtom> atoms = molecule.atoms();
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(atoms.size() * 8);
    for (int atomIndex = 0; atomIndex < atoms.size(); ++atomIndex) {
      GaussianBasisLibrary.addGeneratedCompactEvenTemperedAtomOrbitals(
          atoms.get(atomIndex).atomicNumber(), atomIndex, orbitals);
    }
    return GaussianBasisSet.of(GENERATED_COMPACT_EVEN_TEMPERED_NAME, orbitals);
  }

  public static boolean generatedEvenTemperedName(final String basisName) {
    return basisName != null
        && (GENERATED_EVEN_TEMPERED_NAME.equalsIgnoreCase(basisName)
            || "AUTO-GAUSSIAN".equalsIgnoreCase(basisName)
            || "EVEN-TEMPERED".equalsIgnoreCase(basisName)
            || "ETB".equalsIgnoreCase(basisName));
  }

  public static boolean generatedCompactEvenTemperedName(final String basisName) {
    return basisName != null
        && (GENERATED_COMPACT_EVEN_TEMPERED_NAME.equalsIgnoreCase(basisName)
            || "AUTO-COMPACT-GAUSSIAN".equalsIgnoreCase(basisName)
            || "COMPACT-EVEN-TEMPERED".equalsIgnoreCase(basisName)
            || "CETB".equalsIgnoreCase(basisName));
  }

  public static String unsupportedSto3gReason(final IntegralMolecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("STO-3G molecule must not be null.");
    }
    final List<IntegralAtom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (GaussianBasisLibrary.supportsSto3g(atoms.get(i).atomicNumber())) {
        continue;
      }
      return "Built-in STO-3G Java basis currently contains verified shells for H through Ne.";
    }
    return null;
  }

  public static String unsupportedSix31gReason(final IntegralMolecule molecule) {
    if (molecule == null) {
      throw new IllegalArgumentException("6-31G molecule must not be null.");
    }
    final List<IntegralAtom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      if (GaussianBasisLibrary.supportsSix31g(atoms.get(i).atomicNumber())) {
        continue;
      }
      return "Built-in 6-31G Java basis currently contains verified shells for H, C, N, O, F, P, S and Cl.";
    }
    return null;
  }

  public static boolean supportsSto3g(final int atomicNumber) {
    return atomicNumber >= 1 && atomicNumber <= 10;
  }

  public static boolean supportsSix31g(final int atomicNumber) {
    return atomicNumber == 1
        || atomicNumber == 6
        || atomicNumber == 7
        || atomicNumber == 8
        || atomicNumber == 9
        || atomicNumber == 15
        || atomicNumber == 16
        || atomicNumber == 17;
  }

  private static void addGeneratedEvenTemperedAtomOrbitals(
      final int atomicNumber,
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    if (atomicNumber <= 2) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 2, atomicNumber, orbitals);
      return;
    }
    if (atomicNumber <= 10) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 3, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.P, 2, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.D, 1, atomicNumber, orbitals);
      return;
    }
    if (atomicNumber <= 18) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 4, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.P, 3, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.D, 1, atomicNumber, orbitals);
      return;
    }
    if (atomicNumber <= 36) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 5, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.P, 4, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.D, 2, atomicNumber, orbitals);
      return;
    }
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.S, 6, atomicNumber, orbitals);
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.P, 5, atomicNumber, orbitals);
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.D, 3, atomicNumber, orbitals);
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.F, 1, atomicNumber, orbitals);
  }

  private static void addGeneratedCompactEvenTemperedAtomOrbitals(
      final int atomicNumber,
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    if (atomicNumber <= 2) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 1, atomicNumber, orbitals);
      return;
    }
    if (atomicNumber <= 10) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 2, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.P, 1, atomicNumber, orbitals);
      return;
    }
    if (atomicNumber <= 36) {
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.S, 2, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.P, 2, atomicNumber, orbitals);
      GaussianBasisLibrary.addGeneratedShellSeries(
          atomIndex, GaussianShellType.D, 1, atomicNumber, orbitals);
      return;
    }
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.S, 3, atomicNumber, orbitals);
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.P, 2, atomicNumber, orbitals);
    GaussianBasisLibrary.addGeneratedShellSeries(
        atomIndex, GaussianShellType.D, 1, atomicNumber, orbitals);
  }

  private static void addGeneratedShellSeries(
      final int atomIndex,
      final GaussianShellType shellType,
      final int shellCount,
      final int atomicNumber,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    final double baseExponent =
        Math.max(
            0.045,
            0.045 * Math.pow((double) atomicNumber, 0.85) / (1.0 + shellType.angularMomentum()));
    final double ratio = 2.35 + 0.1 * Math.min(shellType.angularMomentum(), 4);
    for (int shellIndex = 0; shellIndex < shellCount; ++shellIndex) {
      final double exponent = baseExponent * Math.pow(ratio, shellCount - shellIndex - 1);
      GaussianBasisLibrary.addShell(
          atomIndex, shellType, new double[] {exponent}, new double[] {1.0}, orbitals);
    }
  }

  private static void addSto3gAtomOrbitals(
      final int atomicNumber,
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    switch (atomicNumber) {
      case 1:
        orbitals.add(
            ContractedGaussianOrbital.of(
                atomIndex,
                GaussianAngularMomentum.S,
                GaussianBasisLibrary.normalizedPrimitives(
                    new double[] {3.42525091, 0.62391373, 0.16885540},
                    new double[] {0.1543289673, 0.5353281423, 0.4446345422},
                    GaussianAngularMomentum.S)));
        return;
      case 2:
        orbitals.add(
            ContractedGaussianOrbital.of(
                atomIndex,
                GaussianAngularMomentum.S,
                GaussianBasisLibrary.normalizedPrimitives(
                    new double[] {6.362421394, 1.158922999, 0.3136497915},
                    new double[] {0.1543289673, 0.5353281423, 0.4446345422},
                    GaussianAngularMomentum.S)));
        return;
      case 3:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {16.11957475, 2.936200663, 0.7946504870},
            new double[] {0.6362897469, 0.1478600533, 0.04808867840});
        return;
      case 4:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {30.16787069, 5.495115306, 1.487192653},
            new double[] {1.314833110, 0.3055389383, 0.09937074560});
        return;
      case 5:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {48.79111318, 8.887362172, 2.405267040},
            new double[] {2.236956142, 0.5198204999, 0.1690617600});
        return;
      case 6:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {71.61683735, 13.04509632, 3.530512160},
            new double[] {2.941249355, 0.6834830964, 0.2222899159});
        return;
      case 7:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {99.10616896, 18.05231239, 4.885660238},
            new double[] {3.780455879, 0.8784966449, 0.2857143744});
        return;
      case 8:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {130.7093214, 23.80886605, 6.443608313},
            new double[] {5.033151319, 1.169596125, 0.3803889600});
        return;
      case 9:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {166.6791340, 30.36081233, 8.216820672},
            new double[] {6.464803249, 1.502281245, 0.4885884864});
        return;
      case 10:
        GaussianBasisLibrary.addFirstRowSto3g(
            atomIndex,
            orbitals,
            new double[] {207.0156070, 37.70815124, 10.20529731},
            new double[] {8.246315120, 1.916266291, 0.6232292721});
        return;
    }
    throw new IllegalArgumentException(
        "Built-in STO-3G Java basis currently contains verified shells for H through Ne.");
  }

  private static void addFirstRowSto3g(
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals,
      final double[] coreExponents,
      final double[] valenceExponents) {
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                coreExponents,
                new double[] {0.1543289673, 0.5353281423, 0.4446345422},
                GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                valenceExponents,
                new double[] {-0.09996722919, 0.3995128261, 0.7001154689},
                GaussianAngularMomentum.S)));
    GaussianBasisLibrary.addShell(
        atomIndex,
        GaussianShellType.P,
        valenceExponents,
        new double[] {0.1559162750, 0.6076837186, 0.3919573931},
        orbitals);
  }

  private static void addSix31gAtomOrbitals(
      final int atomicNumber,
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    switch (atomicNumber) {
      case 1:
        GaussianBasisLibrary.addSplitValenceHydrogenSix31g(atomIndex, orbitals);
        return;
      case 6:
        GaussianBasisLibrary.addSplitValenceFirstRowSix31g(
            atomIndex,
            orbitals,
            new double[] {3047.5249, 457.36951, 103.94869, 29.210155, 9.286663, 3.163927},
            new double[] {
              0.0018347001629433386,
              0.014037301246680397,
              0.06884260611404756,
              0.23218442062075614,
              0.46794134155879313,
              0.36231203217764585
            },
            new double[] {7.8682724, 1.8812885, 0.5442493},
            new double[] {-0.11933240861517204, -0.16085421161282776, 1.1434564825515419},
            new double[] {0.06899909558499257, 0.31642397975315173, 0.7443082523743546},
            0.1687144);
        return;
      case 7:
        GaussianBasisLibrary.addSplitValenceFirstRowSix31g(
            atomIndex,
            orbitals,
            new double[] {4173.511, 627.4579, 142.9021, 40.23433, 12.82021, 4.390437},
            new double[] {
              0.0018347994430301013,
              0.013994995751692975,
              0.06858697917980464,
              0.23224092950117381,
              0.46906985760961933,
              0.36045489058067104
            },
            new double[] {11.626358, 2.71628, 0.772218},
            new double[] {-0.11496102288211311, -0.16911803366165226, 1.14585222807313},
            new double[] {0.06758002324803634, 0.32390711142648276, 0.7408952548735407},
            0.2120313);
        return;
      case 8:
        GaussianBasisLibrary.addSplitValenceFirstRowSix31g(
            atomIndex,
            orbitals,
            new double[] {5484.6717, 825.23495, 188.04696, 52.9645, 16.89757, 5.7996353},
            new double[] {
              0.0018310998055270163,
              0.013950098518421949,
              0.06844509273075047,
              0.23271427528444968,
              0.47019295006289363,
              0.3585208619230904
            },
            new double[] {15.539616, 3.5999336, 1.0137618},
            new double[] {-0.11077750382508463, -0.14802630511126472, 1.1307670390447473},
            new double[] {0.0708742995433115, 0.3397527978107552, 0.7271585953144516},
            0.2700058);
        return;
      case 9:
        GaussianBasisLibrary.addSplitValenceFirstRowSix31g(
            atomIndex,
            orbitals,
            new double[] {7001.71309, 1051.36609, 239.28569, 67.3974453, 21.5199573, 7.4031013},
            new double[] {
              0.0018196169009262592,
              0.013916079607083853,
              0.06840532453482111,
              0.23318576011870107,
              0.4712674392398944,
              0.3566185461815334
            },
            new double[] {20.8479528, 4.80830834, 1.34406986},
            new double[] {-0.10850697510915827, -0.14645165814733072, 1.1286885811354632},
            new double[] {0.07162872423722153, 0.34591210269682787, 0.7224699563667966},
            0.358151393);
        return;
      case 15:
        GaussianBasisLibrary.addThirdPeriodSix31g(
            atomIndex,
            orbitals,
            new double[] {19413.3, 2909.42, 661.364, 185.759, 59.1943, 20.031},
            new double[] {
              0.0018515989228099592,
              0.014206191735376345,
              0.06999945927696898,
              0.2400788603312228,
              0.4847617179840145,
              0.3351998049934642
            },
            new double[] {339.478, 81.0101, 25.878, 9.45221, 3.66566, 1.46746},
            new double[] {
              -0.0027821701045196066,
              -0.036049901354310264,
              -0.11663100438155334,
              0.09683280363778136,
              0.6144180230822445,
              0.40379801516974456
            },
            new double[] {2.15623, 0.748997, 0.283145},
            new double[] {-0.25292411386309183, 0.03285184467761387, 1.0812547617831043},
            0.0998317,
            new double[] {
              0.004564616190607359,
              0.033693571881087174,
              0.1397548833678009,
              0.33936171678625926,
              0.45092062368496416,
              0.2385858008886276
            },
            new double[] {-0.01776531273343096, 0.2740581964334192, 0.7854215629572301});
        return;
      case 16:
        GaussianBasisLibrary.addThirdPeriodSix31g(
            atomIndex,
            orbitals,
            new double[] {21917.1, 3301.49, 754.146, 212.711, 67.9896, 23.0515},
            new double[] {
              0.0018690012083962711,
              0.014230009200363263,
              0.06969604506173702,
              0.23848715419304523,
              0.4833073124806724,
              0.33807421858071746
            },
            new double[] {423.735, 100.71, 32.1599, 11.8079, 4.6311, 1.87025},
            new double[] {
              -0.0023767004951015867,
              -0.03169300660211831,
              -0.11331702360559875,
              0.05609001168437246,
              0.5922551233754325,
              0.455006094784446
            },
            new double[] {2.61584, 0.922167, 0.341287},
            new double[] {-0.250373114158728, 0.06695676310130426, 1.0545062690675562},
            0.117167,
            new double[] {
              0.004061000321288588,
              0.030681002427346758,
              0.13045201032079265,
              0.32720502588703093,
              0.45285103582759384,
              0.25604202025692507
            },
            new double[] {-0.014510992854050261, 0.3102628472108192, 0.7544826284544418});
        return;
      case 17:
        GaussianBasisLibrary.addThirdPeriodSix31g(
            atomIndex,
            orbitals,
            new double[] {25180.1, 3780.35, 860.474, 242.145, 77.3349, 26.247},
            new double[] {
              0.001833000311787486,
              0.014034002387138887,
              0.06909701175318053,
              0.23745204038983206,
              0.48303408216255134,
              0.3398560578084276
            },
            new double[] {491.765, 116.984, 37.4153, 13.7834, 5.45215, 2.22588},
            new double[] {
              -0.0022974019889148967,
              -0.03071402658985468,
              -0.11252809741821863,
              0.045016038971442934,
              0.5893535102171851,
              0.4652064027401164
            },
            new double[] {3.18649, 1.14427, 0.420377},
            new double[] {-0.25183128804589827, 0.06158931501194785, 1.0601854225489431},
            0.142657,
            new double[] {
              0.003989400547775424,
              0.030318004162895505,
              0.12988001783352685,
              0.3279510450302045,
              0.45352706227275896,
              0.25215403462269115
            },
            new double[] {-0.014299012714249678, 0.3235722877106929, 0.7435076611045274});
        return;
    }
    throw new IllegalArgumentException(
        "Built-in 6-31G Java basis currently contains verified shells for H, C, N, O, F, P, S and Cl.");
  }

  private static void addSplitValenceHydrogenSix31g(
      final int atomIndex, final ArrayList<ContractedGaussianOrbital> orbitals) {
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                new double[] {18.731137, 2.8253937, 0.6401217},
                new double[] {0.033494599523090078, 0.2347269466578609, 0.8137573184133876},
                GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                new double[] {0.1612778},
                new double[] {1.0000000000000002},
                GaussianAngularMomentum.S)));
  }

  private static void addSplitValenceFirstRowSix31g(
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals,
      final double[] coreExponents,
      final double[] coreCoefficients,
      final double[] valenceExponents,
      final double[] valenceSCoefficients,
      final double[] valencePCoefficients,
      final double diffuseExponent) {
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                coreExponents, coreCoefficients, GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                valenceExponents, valenceSCoefficients, GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                new double[] {diffuseExponent}, new double[] {1.0}, GaussianAngularMomentum.S)));
    GaussianBasisLibrary.addShell(
        atomIndex, GaussianShellType.P, valenceExponents, valencePCoefficients, orbitals);
    GaussianBasisLibrary.addShell(
        atomIndex,
        GaussianShellType.P,
        new double[] {diffuseExponent},
        new double[] {1.0},
        orbitals);
  }

  private static void addThirdPeriodSix31g(
      final int atomIndex,
      final ArrayList<ContractedGaussianOrbital> orbitals,
      final double[] coreSExponents,
      final double[] coreSCoefficients,
      final double[] innerValenceExponents,
      final double[] innerValenceSCoefficients,
      final double[] outerValenceExponents,
      final double[] outerValenceSCoefficients,
      final double diffuseExponent,
      final double[] innerValencePCoefficients,
      final double[] outerValencePCoefficients) {
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                coreSExponents, coreSCoefficients, GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                innerValenceExponents, innerValenceSCoefficients, GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                outerValenceExponents, outerValenceSCoefficients, GaussianAngularMomentum.S)));
    orbitals.add(
        ContractedGaussianOrbital.of(
            atomIndex,
            GaussianAngularMomentum.S,
            GaussianBasisLibrary.normalizedPrimitives(
                new double[] {diffuseExponent}, new double[] {1.0}, GaussianAngularMomentum.S)));
    GaussianBasisLibrary.addShell(
        atomIndex, GaussianShellType.P, innerValenceExponents, innerValencePCoefficients, orbitals);
    GaussianBasisLibrary.addShell(
        atomIndex, GaussianShellType.P, outerValenceExponents, outerValencePCoefficients, orbitals);
    GaussianBasisLibrary.addShell(
        atomIndex,
        GaussianShellType.P,
        new double[] {diffuseExponent},
        new double[] {1.0},
        orbitals);
  }

  public static List<ContractedGaussianOrbital> cartesianShellOrbitals(
      final int atomIndex,
      final GaussianShellType shellType,
      final double[] exponents,
      final double[] contractionCoefficients) {
    final ArrayList<ContractedGaussianOrbital> orbitals =
        new ArrayList<ContractedGaussianOrbital>(shellType.cartesianFunctionCount());
    GaussianBasisLibrary.addShell(
        atomIndex, shellType, exponents, contractionCoefficients, orbitals);
    return List.copyOf(orbitals);
  }

  private static void addShell(
      final int atomIndex,
      final GaussianShellType shellType,
      final double[] exponents,
      final double[] contractionCoefficients,
      final ArrayList<ContractedGaussianOrbital> orbitals) {
    final List<GaussianAngularMomentum> angularMomenta =
        GaussianAngularMomentum.cartesianShell(shellType);
    for (int i = 0; i < angularMomenta.size(); ++i) {
      final GaussianAngularMomentum angularMomentum = angularMomenta.get(i);
      orbitals.add(
          ContractedGaussianOrbital.of(
              atomIndex,
              angularMomentum,
              GaussianBasisLibrary.normalizedPrimitives(
                  exponents, contractionCoefficients, angularMomentum)));
    }
  }

  public static List<GaussianPrimitive> normalizedPrimitives(
      final double[] exponents,
      final double[] contractionCoefficients,
      final GaussianAngularMomentum angularMomentum) {
    if (exponents.length != contractionCoefficients.length) {
      throw new IllegalArgumentException("Primitive exponent and coefficient counts must match.");
    }
    final ArrayList<GaussianPrimitive> primitives =
        new ArrayList<GaussianPrimitive>(exponents.length);
    for (int i = 0; i < exponents.length; ++i) {
      final double exponent = exponents[i];
      final double normalization =
          GaussianBasisLibrary.primitiveNormalization(exponent, angularMomentum);
      primitives.add(GaussianPrimitive.of(exponent, contractionCoefficients[i] * normalization));
    }
    return List.copyOf(primitives);
  }

  private static double primitiveNormalization(
      final double exponent, final GaussianAngularMomentum angularMomentum) {
    final double sNormalization = Math.pow(2.0 * exponent / Math.PI, 0.75);
    if (angularMomentum.sType()) {
      return sNormalization;
    }
    GaussianShellType.ofAngularMomentum(angularMomentum.totalPower());
    final double axisFactor =
        GaussianBasisLibrary.oddDoubleFactorial(2 * angularMomentum.xPower() - 1)
            * GaussianBasisLibrary.oddDoubleFactorial(2 * angularMomentum.yPower() - 1)
            * GaussianBasisLibrary.oddDoubleFactorial(2 * angularMomentum.zPower() - 1);
    return sNormalization
        * Math.sqrt(Math.pow(4.0 * exponent, angularMomentum.totalPower()) / axisFactor);
  }

  private static int oddDoubleFactorial(final int value) {
    int result = 1;
    for (int current = value; current > 1; current -= 2) {
      result *= current;
    }
    return result;
  }
}
