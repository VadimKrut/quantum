/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryAnalyzer;
import ru.pathcreator.vadim.quantum.chemistry.classical.application.stoichiometry.ReactionStoichiometryProfile;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.OrganicReactionDomain;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionEvidenceKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.prediction.ReactionPredictionRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionComponentRole;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionComponent;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditionFeatureKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionConditions;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.ReactionParticipant;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Atom;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Bond;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.BondType;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.MolarConcentrationUnit;
import ru.pathcreator.vadim.quantum.chemistry.domain.unit.PressureUnit;

/**
 * Классифицирует условия реакции в объяснимые эффекты для organic prediction.
 */
public final class ReactionConditionEffectAnalyzer {

  private static final double COOLING_KELVIN = 283.15;
  private static final double HEATING_KELVIN = 323.15;
  private static final double HIGH_TEMPERATURE_KELVIN = 373.15;
  private static final double ROOM_TEMPERATURE_KELVIN = 298.15;
  private static final double ATMOSPHERIC_PRESSURE_ATM = 1.0;
  private static final double PRESSURIZED_REACTION_ATM = 2.0;
  private static final double DILUTE_CONCENTRATION_MOLAR = 0.03;
  private static final double CONCENTRATED_CONCENTRATION_MOLAR = 1.0;
  private static final double HIGH_THERMAL_DOSE_KELVIN_HOUR = 240.0;
  private final ReactionStoichiometryAnalyzer stoichiometryAnalyzer;

  public ReactionConditionEffectAnalyzer() {
    this.stoichiometryAnalyzer = new ReactionStoichiometryAnalyzer();
  }

  public ReactionConditionEffectProfile analyze(final ReactionPredictionRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Reaction prediction request must not be null.");
    }
    final ArrayList<ReactionConditionEffect> effects = new ArrayList<ReactionConditionEffect>(16);
    final ReactionConditions conditions = request.conditions();
    if (conditions == null || conditions.empty()) {
      return ReactionConditionEffectProfile.EMPTY;
    }
    this.addSolventEffects(conditions, effects);
    this.addCatalystEffects(conditions, effects);
    this.addNucleophileAndBaseIdentityEffects(conditions, effects);
    this.addReactantNucleophileEffects(request, effects);
    this.addReactantRecipeStoichiometryEffects(request, effects);
    this.addAcidBaseEffects(conditions, effects);
    this.addActivationModeEffects(conditions, effects);
    this.addNoteDerivedEffects(conditions, effects);
    this.addTemperatureEffects(conditions, effects);
    this.addDurationEffects(conditions, effects);
    this.addThermalDoseEffects(conditions, effects);
    this.addThermalMassTransferEffects(conditions, effects);
    this.addPressureEffects(conditions, effects);
    this.addAtmosphereEffects(conditions, effects);
    this.addProcessControlEffects(conditions, effects);
    this.addQuantitativeComponentEffects(conditions, effects);
    this.addDomainEffects(request, effects);
    return ReactionConditionEffectProfile.of(effects);
  }

  private void addQuantitativeComponentEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    final List<ReactionConditionComponent> components = conditions.components();
    boolean quantified = false;
    boolean excessReagent = false;
    boolean catalyticLoading = false;
    boolean purityLimitation = false;
    boolean concentrationRegime = false;
    boolean diluteRegime = false;
    boolean concentratedRegime = false;
    boolean massQuantified = false;
    double totalGramScale = 0.0;
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      quantified |=
          component.hasAmount()
              || component.hasEquivalent()
              || component.hasLoading()
              || component.hasPurity()
              || component.hasConcentration();
      if (component.hasEquivalent() && component.equivalent().value() >= 1.15) {
        excessReagent |=
            component.role() == ReactionComponentRole.REACTANT
                || component.role() == ReactionComponentRole.BASE
                || component.role() == ReactionComponentRole.ADDITIVE
                || component.role() == ReactionComponentRole.OTHER;
      }
      if (component.hasLoading()
          && component.role() == ReactionComponentRole.CATALYST
          && component.loading().percent() <= 30.0) {
        catalyticLoading = true;
      }
      if (component.hasPurity() && component.purity().percent() < 95.0) {
        purityLimitation = true;
      }
      if (component.hasConcentration()) {
        concentrationRegime = true;
        final double molar =
            ReactionConditionEffectAnalyzer.molarConcentration(
                component.concentration().value(), component.concentration().unit());
        diluteRegime |= molar <= DILUTE_CONCENTRATION_MOLAR;
        concentratedRegime |= molar >= CONCENTRATED_CONCENTRATION_MOLAR;
      }
      if (component.hasAmount() && component.amount().massQuantity()) {
        massQuantified = true;
        totalGramScale += component.amount().gramValue();
      }
    }
    if (quantified) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.QUANTIFIED_STOICHIOMETRY_CONTROL,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Quantified amount/equivalent/loading/purity metadata enables condition-aware pathway ranking.",
              0.030,
              false));
    }
    if (excessReagent) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.EXCESS_REAGENT_DRIVING_FORCE,
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Excess base/additive/reagent equivalents can drive conversion and suppress incomplete substitution channels.",
              0.045,
              false));
    }
    if (massQuantified) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.MASS_QUANTIFIED_REACTION_SCALE,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              totalGramScale >= 50.0
                  ? "Reaction recipe contains gram-scale mass quantities; heat transfer, mixing and solvent-load effects are represented explicitly."
                  : "Reaction recipe contains mass quantities, enabling scale-aware condition interpretation alongside molar equivalents.",
              totalGramScale >= 50.0 ? 0.020 : 0.012,
              false));
    }
    if (catalyticLoading) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.CATALYST_LOADING_WINDOW,
              ReactionPredictionEvidenceKind.CATALYST_EFFECT,
              "Catalyst loading is explicitly quantified in a catalytic rather than stoichiometric range.",
              0.035,
              false));
    }
    if (purityLimitation) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.PURITY_LIMITATION,
              ReactionPredictionEvidenceKind.WARNING,
              "At least one component has sub-95% purity, increasing side-product and yield-window uncertainty.",
              -0.055,
              true));
    }
    if (concentrationRegime) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.CONCENTRATION_REGIME,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Explicit concentration data constrains bimolecular collision frequency and dilution-sensitive selectivity.",
              0.025,
              false));
    }
    if (diluteRegime) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.DILUTE_CONDITION_REGIME,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Dilute condition regime suppresses bimolecular collision frequency and can favor intramolecular, unimolecular or slow pathways.",
              -0.030,
              true));
    }
    if (concentratedRegime) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.CONCENTRATED_CONDITION_REGIME,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Concentrated condition regime raises bimolecular encounter frequency and side-reaction competition.",
              0.035,
              false));
    }
  }

  private void addReactantNucleophileEffects(
      final ReactionPredictionRequest request, final List<ReactionConditionEffect> effects) {
    final List<ReactionParticipant> participants = request.reactants().participants();
    boolean primaryOrSecondaryAmine = false;
    for (int i = 0; i < participants.size(); ++i) {
      final Molecule molecule = participants.get(i).molecule();
      if (ReactionConditionEffectAnalyzer.hasNitrogenHydrogenBond(molecule)
          || ReactionConditionEffectAnalyzer.amineLikeName(
              molecule.displayName().toLowerCase(Locale.ROOT))
          || ReactionConditionEffectAnalyzer.hasNeutralAmineNitrogenEnvironment(molecule)) {
        primaryOrSecondaryAmine = true;
        break;
      }
    }
    if (primaryOrSecondaryAmine) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.REACTANT_AMINE_NUCLEOPHILE,
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Reactant identity or molecular graph contains an amine-like nitrogen nucleophile source.",
              0.070,
              false));
    }
  }

  private void addReactantRecipeStoichiometryEffects(
      final ReactionPredictionRequest request, final List<ReactionConditionEffect> effects) {
    final ReactionStoichiometryProfile profile = this.stoichiometryAnalyzer.analyze(request);
    if (profile.quantifiedReactantCount() == 0) {
      return;
    }
    if (profile.unmatchedReactantCount() > 0) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.MASS_RECIPE_REACTANT_UNMATCHED,
              ReactionPredictionEvidenceKind.WARNING,
              "Some mass-quantified reactant recipe inputs could not be matched to molecular graph participants, so stoichiometric prediction is partially underdetermined.",
              -0.045,
              true));
    }
    if (!profile.hasMatchedQuantities()) {
      return;
    }
    effects.add(
        this.effect(
            ReactionConditionEffectKind.MASS_TO_MOLE_STOICHIOMETRY,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Matched reactant gram-scale recipe inputs were converted through molecular formula masses into mole-equivalent ratios.",
            profile.matchedReactantCount() >= 2 ? 0.032 : 0.018,
            false));
    if (profile.hasEquivalentExcess(1.15)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.EXCESS_REAGENT_DRIVING_FORCE,
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Mass-derived mole-equivalent ratios show reagent excess that can drive conversion or change side-channel competition.",
              0.038,
              false));
    }
  }

  private static String normalizedName(final String value) {
    if (value == null) {
      return "";
    }
    final String lower = value.toLowerCase(Locale.ROOT);
    final StringBuilder normalized = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); ++i) {
      final char current = lower.charAt(i);
      if (Character.isLetterOrDigit(current)) {
        normalized.append(current);
      }
    }
    return normalized.toString();
  }

  private static boolean hasNeutralAmineNitrogenEnvironment(final Molecule molecule) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (!atom.element().symbol().value().equals("N")) {
        continue;
      }
      final NitrogenEnvironment environment =
          ReactionConditionEffectAnalyzer.nitrogenEnvironment(molecule, atom);
      if (environment.singleCarbonBondCount > 0
          && environment.aromaticBondCount == 0
          && environment.heavyAtomBondOrder <= 3.0) {
        return true;
      }
    }
    return false;
  }

  private static NitrogenEnvironment nitrogenEnvironment(
      final Molecule molecule, final Atom nitrogen) {
    int singleCarbonBondCount = 0;
    int aromaticBondCount = 0;
    double heavyAtomBondOrder = 0.0;
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      if (!bond.firstAtomId().equals(nitrogen.id()) && !bond.secondAtomId().equals(nitrogen.id())) {
        continue;
      }
      final Atom neighbor =
          ReactionConditionEffectAnalyzer.atom(
              molecule,
              bond.firstAtomId().equals(nitrogen.id())
                  ? bond.secondAtomId().value()
                  : bond.firstAtomId().value());
      final String symbol = neighbor.element().symbol().value();
      if (!symbol.equals("H")) {
        heavyAtomBondOrder +=
            molecule.aromaticity().isAromaticBond(bond)
                ? 1.5
                : ReactionConditionEffectAnalyzer.bondOrder(bond.type());
      }
      if (symbol.equals("C")
          && bond.type() == BondType.SINGLE
          && !molecule.aromaticity().isAromaticBond(bond)) {
        ++singleCarbonBondCount;
      }
      if (molecule.aromaticity().isAromaticBond(bond)) {
        ++aromaticBondCount;
      }
    }
    return new NitrogenEnvironment(singleCarbonBondCount, aromaticBondCount, heavyAtomBondOrder);
  }

  private static double bondOrder(final BondType type) {
    if (type == BondType.SINGLE) {
      return 1.0;
    }
    if (type == BondType.DOUBLE) {
      return 2.0;
    }
    if (type == BondType.TRIPLE) {
      return 3.0;
    }
    if (type == BondType.AROMATIC) {
      return 1.5;
    }
    return 1.0;
  }

  private static boolean hasNitrogenHydrogenBond(final Molecule molecule) {
    final List<Bond> bonds = molecule.bonds();
    for (int i = 0; i < bonds.size(); ++i) {
      final Bond bond = bonds.get(i);
      final Atom first = ReactionConditionEffectAnalyzer.atom(molecule, bond.firstAtomId().value());
      final Atom second =
          ReactionConditionEffectAnalyzer.atom(molecule, bond.secondAtomId().value());
      if (ReactionConditionEffectAnalyzer.hasElements(first, second, "N", "H")
          || ReactionConditionEffectAnalyzer.hasElements(first, second, "H", "N")) {
        return true;
      }
    }
    return false;
  }

  private static Atom atom(final Molecule molecule, final String atomId) {
    final List<Atom> atoms = molecule.atoms();
    for (int i = 0; i < atoms.size(); ++i) {
      final Atom atom = atoms.get(i);
      if (atom.id().value().equals(atomId)) {
        return atom;
      }
    }
    throw new IllegalArgumentException("Molecule bond references unknown atom: " + atomId + ".");
  }

  private static boolean hasElements(
      final Atom first, final Atom second, final String firstElement, final String secondElement) {
    return first.element().symbol().value().equals(firstElement)
        && second.element().symbol().value().equals(secondElement);
  }

  private static boolean amineLikeName(final String name) {
    return ReactionConditionEffectAnalyzer.containsAnyToken(
        name,
        "amine",
        "amino",
        "anilin",
        "morpholin",
        "piperidin",
        "pyrrolidin",
        "ammonia",
        "nh3",
        "hydraz",
        "guanid",
        "azetidin",
        "piperazin",
        "imidazol",
        "triazol");
  }

  private static boolean containsAnyToken(final String name, final String... tokens) {
    for (int i = 0; i < tokens.length; ++i) {
      if (name.contains(tokens[i])) {
        return true;
      }
    }
    return false;
  }

  private void addNucleophileAndBaseIdentityEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    final List<ReactionConditionComponent> components = conditions.components();
    boolean amineNucleophile = false;
    boolean weakNucleophile = false;
    boolean strongBase = false;
    boolean bulkyBase = false;
    boolean inorganicBase = false;
    for (int i = 0; i < components.size(); ++i) {
      final ReactionConditionComponent component = components.get(i);
      final String name = component.name().toLowerCase(Locale.ROOT);
      final ReactionComponentRole role = component.role();
      if (role == ReactionComponentRole.BASE
          || role == ReactionComponentRole.ADDITIVE
          || role == ReactionComponentRole.OTHER) {
        strongBase |=
            this.containsAny(
                name,
                "nah",
                "sodium hydride",
                "sodium amide",
                "nanh2",
                "nan h2",
                "nah2",
                "lda",
                "lihmds",
                "nahmds",
                "khmds",
                "tert-butoxide",
                "tbuok",
                "potassium tert-butoxide",
                "dbu",
                "dbn");
        bulkyBase |=
            this.containsAny(
                name,
                "dipea",
                "hunig",
                "hünig",
                "triethylamine",
                "tea",
                "tert-butoxide",
                "tbuok",
                "dbu",
                "dbn",
                "lutidine",
                "collidine");
        inorganicBase |=
            this.containsAny(
                name,
                "k2co3",
                "potassium carbonate",
                "cs2co3",
                "cesium carbonate",
                "na2co3",
                "sodium carbonate",
                "nahco3",
                "sodium bicarbonate",
                "phosphate",
                "koh",
                "naoh");
      }
      if (role == ReactionComponentRole.ADDITIVE
          || role == ReactionComponentRole.BASE
          || role == ReactionComponentRole.OTHER) {
        amineNucleophile |= ReactionConditionEffectAnalyzer.amineLikeName(name);
        weakNucleophile |=
            this.containsAny(
                name,
                "water",
                "h2o",
                "methanol",
                "ethanol",
                "isopropanol",
                "tert-butanol",
                "t-buoh",
                "phenol",
                "acetate");
      }
    }
    if (amineNucleophile) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.AMINE_NUCLEOPHILE_SOURCE,
              ReactionPredictionEvidenceKind.FUNCTIONAL_GROUP_MATCH,
              "Amine-like reagent identity supplies a concrete nitrogen nucleophile for substitution or amination.",
              0.075,
              false));
    }
    if (weakNucleophile) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.WEAK_NUCLEOPHILE_SOURCE,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Water/alcohol/phenol-like component supplies a weaker O-centered nucleophile or solvolysis competitor.",
              0.025,
              false));
    }
    if (strongBase) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.STRONG_BASE_PROMOTION,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Strong base identity supports deprotonation-driven nucleophilicity, elimination or anionic pathways.",
              0.065,
              false));
    }
    if (bulkyBase) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.BULKY_BASE_SELECTIVITY,
              ReactionPredictionEvidenceKind.STERIC_FACTOR,
              "Bulky base identity favors less direct substitution and can shift selectivity toward elimination or hindered channels.",
              -0.035,
              false));
    }
    if (inorganicBase) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.INORGANIC_BASE_BUFFERING,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Inorganic carbonate/hydroxide base supplies buffered basic activation for heteroaryl substitution.",
              0.040,
              false));
    }
  }

  private void addSolventEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    final List<ReactionConditionComponent> solvents =
        conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    for (int i = 0; i < solvents.size(); ++i) {
      final String solvent = solvents.get(i).name().toLowerCase(Locale.ROOT);
      if (this.containsAny(
          solvent,
          "dmf",
          "dmso",
          "nmp",
          "acetonitrile",
          "mecn",
          "thf",
          "dioxane",
          "peg",
          "polyethylene glycol",
          "poly(ethylene glycol)",
          "glyme",
          "diglyme",
          "triglyme",
          "polar aprotic",
          "aprotic solvent class")) {
        effects.add(
            this.effect(
                ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC,
                ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                "Polar aprotic or polyether solvent supports nucleophilic substitution, heteroaryl activation and cross-coupling workflows.",
                0.045,
                false));
        if (this.polyetherOrPolyolMedium(solvent)) {
          effects.add(
              this.effect(
                  ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM,
                  ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                  "Viscous polyether/polyol medium changes ion pairing, mass transfer and local nucleophile/base activity without using outcome lookup.",
                  0.030,
                  false));
          effects.add(
              this.effect(
                  ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION,
                  ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                  "Polyether-rich medium can solvate cations and create phase-transfer-like activation for anionic or heteroatom nucleophiles.",
                  0.035,
                  false));
        }
      } else if (this.containsAny(
          solvent,
          "methanol",
          "ethanol",
          "propanol",
          "water",
          "h2o",
          "t-buoh",
          "polar protic",
          "protic solvent class")) {
        effects.add(
            this.effect(
                ReactionConditionEffectKind.SOLVENT_POLAR_PROTIC,
                ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                "Polar protic solvent can stabilize ionic intermediates but may suppress moisture-sensitive routes.",
                0.015,
                false));
      } else if (this.containsAny(
          solvent,
          "hexane",
          "heptane",
          "toluene",
          "benzene",
          "xylene",
          "nonpolar",
          "nonpolar solvent class")) {
        effects.add(
            this.effect(
                ReactionConditionEffectKind.SOLVENT_NONPOLAR,
                ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                "Nonpolar solvent favors hydrophobic substrates and thermal organic transformations.",
                0.01,
                false));
      }
      if (this.containsAny(
          solvent, "dcm", "dichloromethane", "chloroform", "dce", "chlorobenzene")) {
        effects.add(
            this.effect(
                ReactionConditionEffectKind.SOLVENT_HALOGENATED,
                ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
                "Halogenated solvent is represented for electrophilic, Lewis-acid and low-temperature contexts.",
                0.02,
                false));
      }
      if (this.containsAny(
          solvent,
          "dmf",
          "dmso",
          "nmp",
          "xylene",
          "toluene",
          "dioxane",
          "peg",
          "polyethylene glycol",
          "poly(ethylene glycol)",
          "glyme",
          "diglyme",
          "triglyme",
          "high boiling solvent class")) {
        effects.add(
            this.effect(
                ReactionConditionEffectKind.SOLVENT_HIGH_BOILING,
                ReactionPredictionEvidenceKind.KINETIC_FACTOR,
                "High-boiling solvent supports extended heating or slow heterocycle functionalization.",
                0.02,
                false));
      }
    }
  }

  private boolean polyetherOrPolyolMedium(final String solvent) {
    return this.containsAny(
        solvent,
        "peg",
        "polyethylene glycol",
        "poly(ethylene glycol)",
        "polyether",
        "glyme",
        "diglyme",
        "triglyme",
        "tetraglyme",
        "crown ether",
        "cryptand");
  }

  private void addCatalystEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    this.addMetalCatalystEffect(
        conditions,
        effects,
        ReactionConditionEffectKind.PALLADIUM_CATALYSIS,
        0.08,
        "Palladium catalyst signal supports cross-coupling and heteroaryl functionalization.",
        "pd",
        "palladium");
    this.addMetalCatalystEffect(
        conditions,
        effects,
        ReactionConditionEffectKind.COPPER_CATALYSIS,
        0.06,
        "Copper catalyst signal supports Ullmann/Sonogashira-like heterocycle functionalization contexts.",
        "cui",
        "cu",
        "copper");
    this.addMetalCatalystEffect(
        conditions,
        effects,
        ReactionConditionEffectKind.NICKEL_CATALYSIS,
        0.05,
        "Nickel catalyst signal supports cross-electrophile and challenging coupling contexts.",
        "ni",
        "nickel");
  }

  private void addMetalCatalystEffect(
      final ReactionConditions conditions,
      final List<ReactionConditionEffect> effects,
      final ReactionConditionEffectKind kind,
      final double score,
      final String description,
      final String... markers) {
    final List<ReactionConditionComponent> catalysts =
        conditions.componentsByRole(ReactionComponentRole.CATALYST);
    for (int i = 0; i < catalysts.size(); ++i) {
      final String catalyst = catalysts.get(i).name().toLowerCase(Locale.ROOT);
      if (!this.containsAny(catalyst, markers)) {
        continue;
      }
      effects.add(
          this.effect(
              kind, ReactionPredictionEvidenceKind.CATALYST_EFFECT, description, score, false));
      return;
    }
  }

  private void addAcidBaseEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        && conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.CONTRADICTORY_ACID_BASE,
              ReactionPredictionEvidenceKind.WARNING,
              "Acidic and basic condition signals are both present; mechanism and product ranking are ambiguous.",
              -0.18,
              true));
      return;
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ACIDIC_MEDIUM)
        || !conditions.componentsByRole(ReactionComponentRole.ACID).isEmpty()) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.BRONSTED_ACID_ACTIVATION,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Acidic condition activates electrophiles, proton transfers or deprotection pathways.",
              0.035,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.BASIC_MEDIUM)
        || !conditions.componentsByRole(ReactionComponentRole.BASE).isEmpty()) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.BASE_PROMOTION,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Basic condition supports deprotonation, nucleophilicity or elimination pathways.",
              0.04,
              false));
    }
    if (this.hasLewisAcidLikeComponent(conditions)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.LEWIS_ACID_ACTIVATION,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Lewis-acid-like component supports carbonyl, imine or heteroatom activation.",
              0.035,
              false));
    }
  }

  private boolean hasLewisAcidLikeComponent(final ReactionConditions conditions) {
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final String name = components.get(i).name().toLowerCase(Locale.ROOT);
      if (this.containsAny(name, "bf3", "alcl3", "zncl2", "ticl4", "sncl4", "fecl3")) {
        return true;
      }
    }
    return false;
  }

  private void addActivationModeEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    boolean photochemical = false;
    boolean electrochemical = false;
    boolean oxidant = false;
    boolean reductant = false;
    boolean hydrogenation = false;
    boolean radicalInitiator = false;
    boolean oxygenTrap = false;
    final List<ReactionConditionComponent> components = conditions.components();
    for (int i = 0; i < components.size(); ++i) {
      final String name = components.get(i).name().toLowerCase(Locale.ROOT);
      photochemical |=
          this.containsAny(
              name, "hv", "light", "led", "photoredox", "irradiation", "blue led", "uv");
      electrochemical |=
          this.containsAny(
              name,
              "electrochemical",
              "electrolysis",
              "constant current",
              "constant potential",
              "anode",
              "cathode");
      oxidant |=
          this.containsAny(
              name,
              "oxidant",
              "o2",
              "oxygen",
              "air",
              "peroxide",
              "tb hp",
              "tbhp",
              "mcpba",
              "ddq",
              "persulfate",
              "k2s2o8",
              "selectfluor",
              "nbs",
              "ncs",
              "iodosobenzene");
      reductant |=
          this.containsAny(
              name,
              "reductant",
              "zinc",
              "zn",
              "tin",
              "sncl2",
              "sodium borohydride",
              "nabh4",
              "lialh4",
              "hantzsch ester",
              "silane",
              "hydrosilane",
              "formate");
      hydrogenation |=
          this.containsAny(
              name, "hydrogen", "h2", "palladium", "platinum", "raney", "nickel", "pd/c", "pt/c");
      radicalInitiator |=
          this.containsAny(
              name,
              "aibn",
              "benzoyl peroxide",
              "bpo",
              "dtbp",
              "di-tert-butyl peroxide",
              "persulfate",
              "k2s2o8",
              "photoredox",
              "eosin",
              "rose bengal",
              "ir photocatalyst",
              "ru(bpy)",
              "radical initiator");
      oxygenTrap |=
          this.containsAny(
              name, "oxygen", "o2", "air", "tempo", "bht", "galvinoxyl", "hydroquinone");
    }
    final String note = conditions.note() == null ? "" : conditions.note().toLowerCase(Locale.ROOT);
    photochemical |= note.contains("photo");
    electrochemical |= note.contains("electro");
    oxidant |= conditions.hasFeature(ReactionConditionFeatureKind.OXIDIZING_MEDIUM);
    reductant |= conditions.hasFeature(ReactionConditionFeatureKind.REDUCING_MEDIUM);
    hydrogenation |= conditions.hasFeature(ReactionConditionFeatureKind.HYDROGENATION);
    photochemical |= conditions.hasFeature(ReactionConditionFeatureKind.PHOTOCHEMICAL);
    electrochemical |= conditions.hasFeature(ReactionConditionFeatureKind.ELECTROCHEMICAL);
    if (photochemical) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.PHOTOCHEMICAL_ACTIVATION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Photochemical activation enables excited-state, photoredox or radical-chain initiation modes.",
              0.065,
              false));
    }
    if (electrochemical) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.ELECTROCHEMICAL_ACTIVATION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Electrochemical activation enables single-electron oxidation/reduction and electrode-controlled radical modes.",
              0.060,
              false));
    }
    if (oxidant) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.OXIDANT_PRESENT,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Oxidant signal raises the weight of oxidative or single-electron initiation pathways.",
              0.045,
              false));
    }
    if (reductant) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.REDUCTANT_PRESENT,
              ReactionPredictionEvidenceKind.ELECTRONIC_FACTOR,
              "Reductant signal raises the weight of reductive activation and radical-anion pathways.",
              0.040,
              false));
    }
    if (hydrogenation) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.HYDROGENATION_CATALYSIS,
              ReactionPredictionEvidenceKind.CATALYST_EFFECT,
              "Hydrogenation signal supports catalytic hydrogen transfer, nitro/nitrile/imine reduction and alkene/alkyne saturation pathways.",
              0.055,
              false));
    }
    if (radicalInitiator) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.RADICAL_INITIATOR_PRESENT,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Radical initiator or photoredox catalyst signal directly supports radical-chain candidate mechanisms.",
              0.070,
              false));
    }
    if (oxygenTrap) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.OXYGEN_RADICAL_TRAP,
              ReactionPredictionEvidenceKind.WARNING,
              "Oxygen or radical-trap signal can intercept carbon radicals and alter the product distribution.",
              -0.050,
              true));
    }
  }

  private void addNoteDerivedEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    final String note = conditions.note() == null ? "" : conditions.note().toLowerCase(Locale.ROOT);
    if (note.isBlank()) {
      return;
    }
    boolean derived = false;
    if (this.containsAny(
        note,
        "peg",
        "polyethylene glycol",
        "poly(ethylene glycol)",
        "polyether",
        "glyme",
        "diglyme",
        "triglyme")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.SOLVENT_POLAR_APROTIC,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Free-text condition note contains polyether/aprotic medium signal.",
              0.035,
              false));
      effects.add(
          this.effect(
              ReactionConditionEffectKind.VISCOUS_POLYETHER_MEDIUM,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Free-text condition note contains viscous polyether medium signal that affects mass transfer and ion pairing.",
              0.025,
              false));
      effects.add(
          this.effect(
              ReactionConditionEffectKind.PHASE_TRANSFER_LIKE_SOLVATION,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Free-text polyether medium signal supports phase-transfer-like cation solvation.",
              0.025,
              false));
    }
    if (this.containsAny(
        note,
        "120 c",
        "120°c",
        "120 degrees",
        "reflux",
        "heated",
        "heating",
        "oil bath",
        "thermal")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.THERMAL_ACTIVATION,
              ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
              "Free-text condition note contains heating/high-temperature signal.",
              0.040,
              false));
    }
    if (this.containsAny(note, "0 c", "0°c", "ice bath", "cooled", "cooling", "low temperature")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.LOW_TEMPERATURE_SELECTIVITY,
              ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
              "Free-text condition note contains cooling/low-temperature selectivity signal.",
              0.030,
              false));
    }
    if (this.containsAny(note, "3 h", "3h", "hour", "hours", "overnight", "multi-hour")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Free-text condition note contains sustained reaction-time signal.",
              0.020,
              false));
    }
    if (this.containsAny(note, "argon", "nitrogen", "n2", "inert", "anhydrous", "dry")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.INERT_DRY_CONTROL,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Free-text condition note contains inert or dry control signal.",
              0.035,
              false));
    }
    if (this.containsAny(note, "air", "moisture", "wet", "open flask")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.AIR_MOISTURE_RISK,
              ReactionPredictionEvidenceKind.WARNING,
              "Free-text condition note contains air/moisture exposure signal.",
              -0.055,
              true));
    }
    if (this.containsAny(note, "atmospheric pressure", "1 atm", "ambient pressure")) {
      derived = true;
      effects.add(
          this.effect(
              ReactionConditionEffectKind.ATMOSPHERIC_REACTION_REGIME,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Free-text condition note contains atmospheric-pressure signal.",
              0.008,
              false));
    }
    if (derived) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.NOTE_DERIVED_CONDITION_SIGNAL,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Free-text condition note was normalized into generic physical condition signals; no product outcome lookup was used.",
              0.010,
              false));
    }
  }

  private void addTemperatureEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.hasFeature(ReactionConditionFeatureKind.COOLING)
        && conditions.hasFeature(ReactionConditionFeatureKind.HEATING)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.CONTRADICTORY_THERMAL_MODE,
              ReactionPredictionEvidenceKind.WARNING,
              "Heating and cooling are both present; temperature control should be resolved before trusting ranking.",
              -0.16,
              true));
      return;
    }
    if (conditions.temperature() == null) {
      return;
    }
    final double kelvin = conditions.temperature().kelvin();
    if (kelvin <= COOLING_KELVIN || conditions.hasFeature(ReactionConditionFeatureKind.COOLING)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.LOW_TEMPERATURE_SELECTIVITY,
              ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
              "Low temperature favors kinetic selectivity and suppresses side reactions.",
              0.035,
              false));
    } else if (kelvin >= HEATING_KELVIN
        || conditions.hasFeature(ReactionConditionFeatureKind.HEATING)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.THERMAL_ACTIVATION,
              ReactionPredictionEvidenceKind.TEMPERATURE_EFFECT,
              kelvin >= HIGH_TEMPERATURE_KELVIN
                  ? "High temperature provides strong thermal activation for slow heterocycle transformations."
                  : "Heating provides thermal activation for slower organic transformations.",
              kelvin >= HIGH_TEMPERATURE_KELVIN ? 0.055 : 0.04,
              false));
    }
  }

  private void addDurationEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (!conditions.hasDuration()) {
      return;
    }
    final double hours = conditions.duration().hours();
    if (hours <= 0.5) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.SHORT_TIME_KINETIC_CONTROL,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Short reaction time emphasizes kinetic products and fast conversions.",
              0.025,
              false));
    } else if (hours >= 1.5 && hours < 12.0) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.SUSTAINED_TIME_CONVERSION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Multi-hour reaction time supports sustained conversion without treating an observed product as a lookup result.",
              0.022,
              false));
    } else if (hours >= 12.0) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.EXTENDED_TIME_CONVERSION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Extended reaction time supports slow conversion but may increase side-product risk.",
              hours > 72.0 ? 0.005 : 0.025,
              hours > 72.0));
    }
  }

  private void addThermalDoseEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.temperature() == null || !conditions.hasDuration()) {
      return;
    }
    final double excessKelvin =
        Math.max(0.0, conditions.temperature().kelvin() - ROOM_TEMPERATURE_KELVIN);
    final double thermalDose = excessKelvin * conditions.duration().hours();
    if (thermalDose < HIGH_THERMAL_DOSE_KELVIN_HOUR) {
      return;
    }
    effects.add(
        this.effect(
            ReactionConditionEffectKind.THERMAL_DOSE_REGIME,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Combined temperature-time thermal dose is high enough to promote slow conversion and thermally accessible side channels.",
            thermalDose >= HIGH_THERMAL_DOSE_KELVIN_HOUR * 2.0 ? 0.055 : 0.040,
            false));
  }

  private void addThermalMassTransferEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.temperature() == null || !conditions.hasDuration()) {
      return;
    }
    if (!this.polyetherMediumPresent(conditions)) {
      return;
    }
    final double excessKelvin =
        Math.max(0.0, conditions.temperature().kelvin() - ROOM_TEMPERATURE_KELVIN);
    final double thermalDose = excessKelvin * conditions.duration().hours();
    if (thermalDose < HIGH_THERMAL_DOSE_KELVIN_HOUR) {
      return;
    }
    effects.add(
        this.effect(
            ReactionConditionEffectKind.THERMAL_MASS_TRANSFER_WINDOW,
            ReactionPredictionEvidenceKind.KINETIC_FACTOR,
            "Sustained heating in a viscous polyether/polyol medium creates a coupled mass-transfer and thermal activation window for slow heterocycle substitutions.",
            thermalDose >= HIGH_THERMAL_DOSE_KELVIN_HOUR * 2.0 ? 0.050 : 0.035,
            false));
  }

  private void addPressureEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.pressure() == null) {
      return;
    }
    final double atmosphere =
        ReactionConditionEffectAnalyzer.atmospherePressure(
            conditions.pressure().value(), conditions.pressure().unit());
    if (atmosphere >= PRESSURIZED_REACTION_ATM) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.PRESSURIZED_REACTION_REGIME,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Pressurized reaction regime changes gas participation, volatile reagent retention and activation volume sensitivity.",
              0.030,
              false));
      return;
    }
    if (Math.abs(atmosphere - ATMOSPHERIC_PRESSURE_ATM) <= 0.10) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.ATMOSPHERIC_REACTION_REGIME,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Atmospheric pressure is explicitly declared, reducing ambiguity for volatile or gas-sensitive pathways.",
              0.010,
              false));
    }
  }

  private void addAtmosphereEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        && conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.INERT_DRY_CONTROL,
              ReactionPredictionEvidenceKind.SOLVENT_EFFECT,
              "Inert and anhydrous condition supports air- or moisture-sensitive heterocycle chemistry.",
              0.045,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.AIR_SENSITIVE)
        && (!conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
            || !conditions.hasFeature(ReactionConditionFeatureKind.INERT_ATMOSPHERE))) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.AIR_MOISTURE_RISK,
              ReactionPredictionEvidenceKind.WARNING,
              "Air-sensitive condition lacks complete inert/dry protection.",
              -0.12,
              true));
    }
  }

  private void addProcessControlEffects(
      final ReactionConditions conditions, final List<ReactionConditionEffect> effects) {
    if (conditions.hasFeature(ReactionConditionFeatureKind.MICROWAVE)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.MICROWAVE_ACCELERATION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Microwave heating is declared, so fast polar-medium heating and shortened activation windows are represented separately from conventional thermal dose.",
              0.030,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.SEALED_VESSEL)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.SEALED_VESSEL_RETENTION,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Sealed vessel mode retains volatile reagents and pressure-sensitive components, changing conversion and side-channel risk.",
              0.020,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.FLOW_REACTOR)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.FLOW_REACTOR_CONTROL,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Flow-reactor mode narrows residence-time and heat-transfer distributions compared with batch execution.",
              0.035,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.ADDITION_RATE)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.ADDITION_RATE_SELECTIVITY,
              ReactionPredictionEvidenceKind.KINETIC_FACTOR,
              "Controlled addition rate is declared, so local concentration and exotherm-sensitive selectivity are not treated as unconstrained batch conditions.",
              0.028,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.WORKUP)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.WORKUP_PRODUCT_STABILITY,
              ReactionPredictionEvidenceKind.THERMODYNAMIC_FACTOR,
              "Workup is explicitly represented, allowing product stability and quench-sensitive pathways to be separated from formation probability.",
              0.018,
              false));
    }
    if (conditions.hasFeature(ReactionConditionFeatureKind.PURIFICATION)) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.PURIFICATION_OBSERVABILITY,
              ReactionPredictionEvidenceKind.WARNING,
              "Purification is declared, so isolated product distribution may differ from crude reaction distribution.",
              -0.018,
              true));
    }
  }

  private void addDomainEffects(
      final ReactionPredictionRequest request, final List<ReactionConditionEffect> effects) {
    if (!request.hasDomain(OrganicReactionDomain.HETEROCYCLE_SYNTHESIS)
        && !request.hasDomain(OrganicReactionDomain.HETEROCYCLE_FUNCTIONALIZATION)) {
      return;
    }
    final ReactionConditions conditions = request.conditions();
    if (conditions.hasFeature(ReactionConditionFeatureKind.ANHYDROUS)
        || conditions.hasFeature(ReactionConditionFeatureKind.HEATING)
        || ReactionConditionEffectAnalyzer.heatedNumerically(conditions)
        || conditions.hasFeature(ReactionConditionFeatureKind.CATALYST_PRESENT)
        || !conditions.componentsByRole(ReactionComponentRole.CATALYST).isEmpty()) {
      effects.add(
          this.effect(
              ReactionConditionEffectKind.HETEROCYCLE_COMPATIBILITY,
              ReactionPredictionEvidenceKind.HETEROCYCLE_PATTERN,
              "Condition set contains activation or control signals common in heterocycle synthesis and functionalization.",
              0.05,
              false));
    }
  }

  private static boolean heatedNumerically(final ReactionConditions conditions) {
    return conditions.temperature() != null && conditions.temperature().kelvin() >= HEATING_KELVIN;
  }

  private boolean polyetherMediumPresent(final ReactionConditions conditions) {
    final List<ReactionConditionComponent> solvents =
        conditions.componentsByRole(ReactionComponentRole.SOLVENT);
    for (int i = 0; i < solvents.size(); ++i) {
      if (this.polyetherOrPolyolMedium(solvents.get(i).name().toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private static double atmospherePressure(final double value, final PressureUnit unit) {
    if (unit == PressureUnit.PASCAL) {
      return value / 101325.0;
    }
    if (unit == PressureUnit.BAR) {
      return value / 1.01325;
    }
    return value;
  }

  private static double molarConcentration(final double value, final MolarConcentrationUnit unit) {
    if (unit == MolarConcentrationUnit.MILLIMOLE_PER_LITER) {
      return value / 1000.0;
    }
    if (unit == MolarConcentrationUnit.MICROMOLE_PER_LITER) {
      return value / 1000000.0;
    }
    return value;
  }

  private ReactionConditionEffect effect(
      final ReactionConditionEffectKind kind,
      final ReactionPredictionEvidenceKind evidenceKind,
      final String description,
      final double score,
      final boolean risk) {
    return ReactionConditionEffect.of(kind, evidenceKind, description, score, risk);
  }

  private boolean containsAny(final String text, final String... markers) {
    for (int i = 0; i < markers.length; ++i) {
      if (text.contains(markers[i])) {
        return true;
      }
    }
    return false;
  }

  private static final class NitrogenEnvironment {

    private final int singleCarbonBondCount;
    private final int aromaticBondCount;
    private final double heavyAtomBondOrder;

    private NitrogenEnvironment(
        final int singleCarbonBondCount,
        final int aromaticBondCount,
        final double heavyAtomBondOrder) {
      this.singleCarbonBondCount = singleCarbonBondCount;
      this.aromaticBondCount = aromaticBondCount;
      this.heavyAtomBondOrder = heavyAtomBondOrder;
    }
  }
}
