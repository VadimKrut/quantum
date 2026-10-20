/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.infrastructure.builtin;

import java.util.ArrayList;
import java.util.List;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPattern;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternCatalog;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.mechanism.ReactionMechanismPatternStep;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.MechanisticEventKind;
import ru.pathcreator.vadim.quantum.chemistry.domain.mechanism.ReactionMechanismFamily;

/**
 * ?????????? ??????? ???????? ?????????? ???????????? ? ?????????? ?????.
 */
public final class BuiltInReactionMechanismPatternCatalog {

  private BuiltInReactionMechanismPatternCatalog() {}

  public static ReactionMechanismPatternCatalog organicAndPhysicalChemistry() {
    return ReactionMechanismPatternCatalog.of(
        BuiltInReactionMechanismPatternCatalog.completeCoverage(
            List.of(
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ACID_BASE,
                    "Acid-base proton transfer",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Acid-base encounter complex",
                            "Hydrogen-bonding, ion pairing or solvent cage organization aligns donor and acceptor before proton motion.",
                            0.18,
                            List.of("acid", "base", "hydrogen bond", "ion pair", "pH window"),
                            List.of("steric blocking", "immiscible phases")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Proton transfer event",
                            "The proton moves along the organized donor-acceptor coordinate and changes the reactive microstate.",
                            0.42,
                            List.of(
                                "proton donor",
                                "proton acceptor",
                                "matched pKa",
                                "buffered condition"),
                            List.of("no proton source", "wrong pKa gap")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Solvent and counterion relaxation",
                            "Solvent, counterions and aggregate state relax around the new acid/base pair and determine follow-up reactivity.",
                            0.24,
                            List.of("polar solvent", "salt balance", "buffer", "counterion"),
                            List.of("ion-pair trapping", "phase separation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Electronic microstate redistribution",
                            "Conjugated bases or acids redistribute charge by resonance, tautomerism or coordination before the next step.",
                            0.16,
                            List.of(
                                "resonance stabilized",
                                "tautomeric acid/base",
                                "heteroatom conjugation"),
                            List.of("locked tautomer", "irreversible salt formation"))),
                    List.of("acid", "base", "pH", "proton donor", "proton acceptor", "matched pKa"),
                    List.of(
                        "no proton source",
                        "strong steric blocking",
                        "wrong protonation microstate")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN1,
                    "SN1 substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Ionizing solvation complex",
                            "Polar protic solvent, Lewis acid or neighboring stabilization organizes the substrate before C-X ionization.",
                            0.16,
                            List.of(
                                "polar protic solvent", "tertiary center", "benzylic", "allylic"),
                            List.of("nonpolar solvent", "primary center")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                            "Rate-determining leaving-group departure",
                            "The C-leaving-group bond breaks and generates a planar or contact-ion-pair carbocation-like intermediate.",
                            0.34,
                            List.of("good leaving group", "stabilized cation", "silver salt"),
                            List.of("poor leaving group", "strong backside nucleophile")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Ion pair solvent cage reorganization",
                            "Solvent and counterion reorganization controls capture face, rearrangement risk and racemization.",
                            0.18,
                            List.of(
                                "solvent separated ion pair",
                                "neighboring group participation",
                                "rearrangement"),
                            List.of("tight ion pair", "rapid elimination")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Nucleophile capture",
                            "A neutral or weak nucleophile captures the cation from either face and forms the substituted product skeleton.",
                            0.22,
                            List.of("weak nucleophile", "solvolysis", "racemization"),
                            List.of("sterically blocked cation", "competing elimination")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.DEPROTONATION,
                            "Product deprotonation or ion balance",
                            "Solvent, base or counterion neutralizes the captured adduct and closes the ionic balance.",
                            0.10,
                            List.of("solvent base", "workup", "salt formation"),
                            List.of("acid-sensitive product", "over-solvolysis"))),
                    List.of("tertiary center", "stabilized cation", "polar protic solvent"),
                    List.of("primary center", "poor leaving group")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.NUCLEOPHILIC_SUBSTITUTION_SN2,
                    "SN2 substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Backside approach organization",
                            "A strong nucleophile approaches the antibonding C-leaving-group axis under polar aprotic or phase-transfer conditions.",
                            0.18,
                            List.of(
                                "strong nucleophile", "polar aprotic solvent", "primary center"),
                            List.of("tertiary center", "crowded backside")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Concerted backside attack",
                            "Nucleophile overlap with sigma-star orbital starts C-Nu bond formation while the leaving group bond weakens.",
                            0.30,
                            List.of(
                                "backside approach",
                                "halide",
                                "sulfonate",
                                "activated nucleophile"),
                            List.of("weak nucleophile", "protonated nucleophile")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.BOND_FORMATION,
                            "Pentacoordinate transition region",
                            "The transition state has partial bonds to nucleophile and leaving group and carries inversion geometry.",
                            0.24,
                            List.of(
                                "Walden inversion",
                                "low steric hindrance",
                                "good orbital alignment"),
                            List.of("beta-branching", "neighboring elimination")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                            "Leaving-group expulsion",
                            "Leaving group departs as the new sigma bond completes and stereochemistry is inverted at the reacting center.",
                            0.20,
                            List.of("good leaving group", "inversion product", "salt formation"),
                            List.of("poor leaving group", "ion-pair trapping")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Product ion-pair separation",
                            "Counterion and solvent separate the product from the leaving group salt, affecting observed rate and isolation.",
                            0.08,
                            List.of("polar medium", "phase transfer", "soluble salt"),
                            List.of("ion-pair aggregation", "precipitation blocks turnover"))),
                    List.of("nucleophile", "leaving group", "polar aprotic solvent"),
                    List.of("tertiary center", "backside approach blocked")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.NUCLEOPHILIC_AROMATIC_SUBSTITUTION,
                    "Nucleophilic aromatic substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Substrate activation and nucleophile organization",
                            "Electron-poor aryl or heteroaryl substrate, base and nucleophile are organized before addition.",
                            0.16,
                            List.of("electron-poor aryl", "heteroaryl", "base", "nucleophile"),
                            List.of("electron-rich aromatic system", "protonated nucleophile")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Meisenheimer addition",
                            "Nucleophile adds into the activated ring and forms a sigma-adduct at the leaving-group bearing site.",
                            0.30,
                            List.of(
                                "ortho/para electron-withdrawing group",
                                "activated heterocycle",
                                "leaving group"),
                            List.of("sterically blocked ipso carbon", "weak nucleophile")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Anionic sigma-complex delocalization",
                            "Negative charge is delocalized through ring heteroatoms or withdrawing substituents.",
                            0.24,
                            List.of(
                                "nitro", "pyridine", "diazine", "electron-withdrawing substituent"),
                            List.of("no anion stabilization", "wrong protonation state")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                            "Aromaticity-restoring leaving group expulsion",
                            "Leaving group departure restores aromaticity and gives the substituted aryl or heteroaryl product.",
                            0.22,
                            List.of("chloride", "fluoride", "sulfonate", "restored aromaticity"),
                            List.of("poor leaving group", "addition-elimination not reversible")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Product neutralization",
                            "Base, solvent or counterion neutralizes the product and closes the ionic balance.",
                            0.08,
                            List.of("base", "protic workup", "salt metathesis"),
                            List.of("salt isolation", "acid-sensitive product"))),
                    List.of("electron-poor aryl/heteroaryl", "leaving group", "base"),
                    List.of("electron-rich aromatic system", "no leaving group")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ADDITION_RING_OPENING_RING_CLOSURE,
                    "ANRORC addition/ring-opening/ring-closure substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Nucleophile addition to activated aza-heterocycle",
                            "Nucleophile adds into an electron-poor heterocycle and forms an anionic sigma-adduct.",
                            0.20,
                            List.of(
                                "aza-heterocycle", "leaving group", "strong nucleophile", "base"),
                            List.of("electron-rich heterocycle", "weak nucleophile")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.RING_OPENING,
                            "Ring opening of the adduct",
                            "The sigma-adduct can open the heteroaromatic ring and expose an acyclic amidine/enamine-like intermediate.",
                            0.24,
                            List.of(
                                "diazine", "pyrimidine", "amide base", "low competing protonation"),
                            List.of("rigid fused system", "rapid direct leaving-group expulsion")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Open-chain intermediate reorganization",
                            "Charge and bond order redistribute before closure, which can change apparent substitution position.",
                            0.18,
                            List.of(
                                "ring nitrogen stabilization",
                                "anion delocalization",
                                "controlled temperature"),
                            List.of("poor anion stabilization", "irreversible fragmentation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RING_CLOSURE,
                            "Ring closure to substituted heterocycle",
                            "The rearranged intermediate closes back to a heterocyclic product channel.",
                            0.24,
                            List.of(
                                "intramolecular closure",
                                "restored aromaticity",
                                "nitrogen heterocycle"),
                            List.of("trapped open-chain intermediate", "hydrolysis")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Product neutralization and leaving-group balance",
                            "Final proton transfer and salt balance deliver the neutral or salt form of the substituted product.",
                            0.14,
                            List.of("workup", "base", "leaving-group salt"),
                            List.of("acid-sensitive product", "overreaction"))),
                    List.of(
                        "activated aza-heterocycle",
                        "leaving group",
                        "strong nucleophile",
                        "basic medium"),
                    List.of("no ring-opening pathway", "direct SNAr dominates", "hydrolysis")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ELECTROPHILIC_AROMATIC_SUBSTITUTION,
                    "Electrophilic aromatic substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Electrophile generation and pi-complex",
                            "Acid, Lewis acid or oxidant generates the electrophile and forms a weak pi-complex with the aromatic ring.",
                            0.18,
                            List.of(
                                "electrophile",
                                "Lewis acid",
                                "acidic medium",
                                "activated aromatic ring"),
                            List.of("no electrophile", "strongly deactivated ring")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ELECTROPHILIC_ATTACK,
                            "Sigma-complex formation",
                            "The aromatic pi system attacks the electrophile and forms an arenium ion at the selected regioisomeric site.",
                            0.30,
                            List.of("ortho/para director", "meta director", "electron-rich arene"),
                            List.of("sterically blocked site", "heteroatom protonation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Arenium ion delocalization",
                            "Positive charge delocalizes through the ring and substituent effects determine regioselectivity and rate.",
                            0.24,
                            List.of(
                                "resonance donation",
                                "inductive withdrawal",
                                "regioselective directing"),
                            List.of("cation destabilization", "oxidative decomposition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.DEPROTONATION,
                            "Aromaticity restoration",
                            "Base or counterion removes the ipso proton and restores aromaticity to give the substituted arene.",
                            0.20,
                            List.of("base", "counterion", "aromaticity restored"),
                            List.of("overreaction", "poly-substitution")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Acid catalyst and salt balance",
                            "Solvent and counterion regenerate catalyst or isolate salt products, affecting yield and side reactions.",
                            0.08,
                            List.of("acid regeneration", "controlled temperature", "quench"),
                            List.of("strong oxidant excess", "acid-sensitive substituent"))),
                    List.of("activated aromatic ring", "electrophile", "acidic medium"),
                    List.of("strongly deactivated ring", "oxidation-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ADDITION,
                    "Addition to multiple bond",
                    List.of(
                        MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("alkene", "alkyne", "carbonyl", "imine"),
                    List.of("no pi bond", "reversible hydration conditions")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.CARBONYL_ADDITION,
                    "Nucleophilic addition to carbonyl",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Carbonyl polarization",
                            "Lewis acid, hydrogen bonding or intrinsic C=O polarization exposes the carbonyl carbon to nucleophilic attack.",
                            0.18,
                            List.of("aldehyde", "ketone", "Lewis acid", "hydrogen bonding"),
                            List.of("sterically shielded carbonyl", "deactivated amide")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Nucleophile addition",
                            "Hydride, organometallic, amine, cyanide or heteroatom nucleophile attacks the carbonyl carbon.",
                            0.32,
                            List.of(
                                "hydride donor", "organometallic nucleophile", "cyanide", "amine"),
                            List.of("protonated nucleophile", "nucleophile quenched by solvent")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Alkoxide or carbinol intermediate stabilization",
                            "The tetrahedral intermediate redistributes charge and can coordinate metal, solvent or counterion.",
                            0.20,
                            List.of("alkoxide", "metal counterion", "tetrahedral intermediate"),
                            List.of("retro-addition", "competing enolization")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Protonation or workup",
                            "Proton source or workup converts the anionic intermediate to alcohol, hemiaminal, cyanohydrin or related product.",
                            0.22,
                            List.of("workup", "proton donor", "controlled acidity"),
                            List.of("acid-sensitive product", "dehydration")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Adduct equilibration",
                            "Solvent and additives determine whether the addition product persists or equilibrates to imine/enamine/acetal forms.",
                            0.08,
                            List.of("low water", "chelation", "temperature control"),
                            List.of("reversible addition", "water excess"))),
                    List.of("aldehyde", "ketone", "organometallic nucleophile", "hydride donor"),
                    List.of("strong steric shielding", "competing enolization")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.CARBONYL_SUBSTITUTION,
                    "Acyl substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Acyl activation",
                            "Acyl derivative is activated by leaving-group ability, protonation, Lewis acid or coupling reagent.",
                            0.18,
                            List.of(
                                "acid chloride",
                                "anhydride",
                                "activated ester",
                                "coupling reagent"),
                            List.of("amide resonance", "poor acyl activation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Tetrahedral intermediate formation",
                            "Nucleophile attacks the acyl carbon and forms a tetrahedral oxyanion or zwitterionic intermediate.",
                            0.30,
                            List.of("amine", "alcohol", "thiol", "carboxylate", "base"),
                            List.of("nucleophile protonation", "steric acyl shielding")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Intermediate collapse alignment",
                            "Oxyanion re-forms the carbonyl while the best leaving group aligns for departure.",
                            0.20,
                            List.of("oxyanion", "leaving group hierarchy", "base assistance"),
                            List.of("wrong leaving group", "stable hydrate")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                            "Leaving-group expulsion",
                            "Leaving group departs and the acyl substitution product forms with restored C=O bond order.",
                            0.22,
                            List.of(
                                "chloride",
                                "carboxylate",
                                "alcohol leaving group",
                                "amide product"),
                            List.of("leaving group reprotonation", "hydrolysis")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Product and catalyst proton balance",
                            "Base, solvent or workup neutralizes product and leaving group salt.",
                            0.10,
                            List.of("base", "workup", "salt removal"),
                            List.of("overacylation", "product hydrolysis"))),
                    List.of("acid chloride", "anhydride", "ester", "amide", "nucleophile"),
                    List.of("poor acyl leaving group", "nucleophile protonation")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.IMINE_ENAMINE_FORMATION,
                    "Imine or enamine formation",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Carbonyl-amine encounter complex",
                            "Carbonyl compound and amine form a hydrogen-bonded or acid-activated encounter complex.",
                            0.16,
                            List.of("carbonyl", "amine", "mild acid", "water removal"),
                            List.of("amine fully protonated", "excess water")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Carbinolamine formation",
                            "Amine attacks the carbonyl carbon to give a tetrahedral carbinolamine intermediate.",
                            0.28,
                            List.of("primary amine", "secondary amine", "aldehyde", "ketone"),
                            List.of("sterically blocked carbonyl", "weak amine")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Carbinolamine proton shuttle",
                            "Internal or solvent-mediated proton transfer converts hydroxyl into a better leaving group.",
                            0.20,
                            List.of(
                                "buffered acid", "protic solvent", "intramolecular proton relay"),
                            List.of("too strong acid", "no proton shuttle")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.CONDENSATION,
                            "Water elimination",
                            "Water leaves to form an iminium or imine-like unsaturated intermediate.",
                            0.24,
                            List.of("dehydrating conditions", "Dean-Stark", "molecular sieves"),
                            List.of("water excess", "hydrolysis")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Imine/enamine tautomer selection",
                            "Deprotonation or resonance reorganization selects imine, iminium or enamine product form.",
                            0.12,
                            List.of("alpha proton", "secondary amine", "base"),
                            List.of("unstable imine", "polycondensation"))),
                    List.of("carbonyl", "amine", "acid catalysis", "water removal"),
                    List.of("too strong acid", "excess water")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ENOLATE_ALDOL,
                    "Enolate aldol pathway",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.DEPROTONATION,
                            "Enolate generation",
                            "Base removes an alpha proton and forms a metal or neutral enolate equivalent.",
                            0.22,
                            List.of("alpha proton", "base", "lithium enolate", "amine base"),
                            List.of("no alpha proton", "base quenched")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "O/C enolate resonance selection",
                            "Counterion, solvent and temperature bias oxygen-bound or carbon-bound enolate character.",
                            0.16,
                            List.of("coordinating cation", "polar solvent", "low temperature"),
                            List.of("uncontrolled equilibration", "self-condensation mixture")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Carbonyl C-C bond formation",
                            "Enolate carbon attacks a second carbonyl compound and forms a beta-alkoxide intermediate.",
                            0.30,
                            List.of("aldehyde electrophile", "ketone electrophile", "cross aldol"),
                            List.of("hindered electrophile", "competing Michael acceptor")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Aldolate protonation",
                            "Alkoxide is protonated to give beta-hydroxy carbonyl product or its salt.",
                            0.18,
                            List.of("workup", "proton source", "controlled quench"),
                            List.of("retro-aldol", "over-deprotonation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.CONDENSATION,
                            "Optional dehydration",
                            "Thermal or acid/base conditions can dehydrate the aldol adduct to an enone.",
                            0.14,
                            List.of("heat", "acid", "base", "conjugated enone"),
                            List.of("beta-hydroxy product desired", "polymerization"))),
                    List.of("alpha proton", "base", "carbonyl electrophile"),
                    List.of("no alpha proton", "self-condensation mixture")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.MICHAEL_ADDITION,
                    "Conjugate Michael addition",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Donor and acceptor activation",
                            "Base or catalyst activates a soft nucleophile and aligns it with the beta-carbon of an electron-poor pi system.",
                            0.16,
                            List.of("soft nucleophile", "base", "organocatalyst", "enone"),
                            List.of("hard 1,2-addition preference", "acceptor polymerization")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.DEPROTONATION,
                            "Nucleophile softening",
                            "Malonate, nitroalkane, enolate, thiolate or amine donor is converted into its conjugate-addition form.",
                            0.20,
                            List.of("malonate", "nitroalkane", "thiolate", "enolate"),
                            List.of("donor pKa mismatch", "nucleophile over-basic")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                            "Beta-addition",
                            "The nucleophile adds to the beta position and moves electron density onto the alpha-carbonyl or related acceptor.",
                            0.30,
                            List.of(
                                "alpha,beta-unsaturated acceptor",
                                "beta carbon",
                                "conjugate acceptor"),
                            List.of("sterically blocked beta carbon", "competing direct addition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Enolate or anion delocalization",
                            "The addition intermediate delocalizes into carbonyl, nitrile, nitro or sulfone acceptor framework.",
                            0.20,
                            List.of("enolate intermediate", "nitronate", "stabilized anion"),
                            List.of("anion trapping", "reversible addition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Product protonation",
                            "Proton transfer or catalyst turnover gives the saturated conjugate-addition product.",
                            0.14,
                            List.of("proton source", "catalyst turnover", "workup"),
                            List.of("retro-Michael", "over-addition"))),
                    List.of("soft nucleophile", "alpha,beta-unsaturated acceptor", "base"),
                    List.of("hard 1,2-addition preference", "acceptor polymerization")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.REDUCTIVE_AMINATION,
                    "Reductive amination",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Carbonyl and amine encounter",
                            "Amine approaches aldehyde or ketone under pH control before C-N bond formation.",
                            0.12,
                            List.of("aldehyde", "ketone", "amine", "controlled pH"),
                            List.of("amine salt only", "steric shielding")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Carbinolamine formation",
                            "Nitrogen attacks the carbonyl carbon and forms the tetrahedral carbinolamine intermediate.",
                            0.22,
                            List.of("carbonyl", "primary amine", "secondary amine"),
                            List.of("poor nucleophilicity", "competing hydrate")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Carbinolamine dehydration setup",
                            "Proton transfers convert the hydroxyl group into a leaving group without fully protonating the amine.",
                            0.14,
                            List.of("mild acid", "water removal", "buffer"),
                            List.of("too strong acid", "excess water")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.IMINIUM_FORMATION,
                            "Imine or iminium generation",
                            "Water loss forms the imine or iminium electrophile that is ready for hydride delivery.",
                            0.20,
                            List.of("imine", "iminium", "dehydrating condition"),
                            List.of("hydrolysis", "enamine side path")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.HYDRIDE_TRANSFER,
                            "Chemoselective hydride reduction",
                            "Mild hydride donor reduces the iminium center while minimizing carbonyl over-reduction.",
                            0.24,
                            List.of("NaBH3CN", "NaBH(OAc)3", "mild hydride donor"),
                            List.of("over-reduction", "free aldehyde reduction")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            5,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Amine workup and salt balance",
                            "Final proton transfer gives the neutral or salt form of the alkylated amine product.",
                            0.08,
                            List.of("workup", "amine product", "salt balance"),
                            List.of("acid-sensitive substrate", "product salt trapped"))),
                    List.of("aldehyde", "ketone", "amine", "mild hydride donor", "controlled pH"),
                    List.of("water excess", "over-reduction", "acid-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.OLEFINATION,
                    "Carbonyl olefination",
                    List.of(
                        MechanisticEventKind.YLIDE_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.OLEFIN_FORMATION),
                    List.of("carbonyl", "ylide/phosphonate/sulfone reagent", "base", "E/Z control"),
                    List.of(
                        "enolizable carbonyl", "salt-sensitive substrate", "poor stereocontrol")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ELIMINATION_E1,
                    "E1 elimination",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Ionizing substrate activation",
                            "Solvent, heat or Lewis acid polarizes the leaving-group bond before unimolecular ionization.",
                            0.12,
                            List.of(
                                "tertiary center",
                                "benzylic center",
                                "polar protic solvent",
                                "heat"),
                            List.of("primary center", "nonpolar solvent")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                            "Carbocation-forming departure",
                            "The leaving group departs to generate a cationic or tight ion-pair intermediate.",
                            0.34,
                            List.of("good leaving group", "cation stabilization", "weak base"),
                            List.of("poor leaving group", "unstable cation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Cation reorganization and rearrangement screen",
                            "The cationic manifold relaxes and is checked for hydride or alkyl shifts before beta deprotonation.",
                            0.16,
                            List.of(
                                "rearrangement possible",
                                "solvent-separated ion pair",
                                "hyperconjugation"),
                            List.of("fragmentation", "rapid nucleophile capture")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.DEPROTONATION,
                            "Beta proton removal",
                            "Solvent or weak base removes a beta proton from the most accessible cation conformer.",
                            0.22,
                            List.of("beta hydrogen", "weak base", "Zaitsev alkene"),
                            List.of("no beta hydrogen", "steric shielding")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.BOND_FORMATION,
                            "Alkene pi-bond formation",
                            "C-H bond cleavage and charge relaxation form the alkene regioisomer distribution.",
                            0.16,
                            List.of("alkene product", "thermodynamic control", "heat"),
                            List.of("substitution competition", "polymerization"))),
                    List.of("tertiary center", "heat", "weak base", "good leaving group"),
                    List.of("unstable cation", "low temperature", "nucleophilic capture")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ELIMINATION_E2,
                    "E2 elimination",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                            "Anti-periplanar conformer selection",
                            "Rotamer or ring geometry is selected where beta C-H and C-leaving-group bonds align anti-periplanar.",
                            0.24,
                            List.of(
                                "anti-periplanar beta hydrogen",
                                "chair conformer",
                                "accessible beta site"),
                            List.of("locked syn geometry", "no beta hydrogen")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.DEPROTONATION,
                            "Concerted beta deprotonation",
                            "Strong base removes the aligned beta proton while the leaving-group bond begins to break.",
                            0.28,
                            List.of("strong base", "bulky base", "high temperature"),
                            List.of("weak base", "acidic proton elsewhere")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                            "Synchronous leaving-group departure",
                            "The leaving group departs through the same transition state instead of forming a free cation.",
                            0.20,
                            List.of(
                                "good leaving group",
                                "concerted transition state",
                                "polar aprotic solvent"),
                            List.of("poor leaving group", "SN2 competition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.ELIMINATION,
                            "Pi-bond construction",
                            "C-H cleavage, C-X cleavage and C=C formation resolve into Hofmann or Zaitsev product families.",
                            0.20,
                            List.of(
                                "alkene target", "regioselectivity", "stereospecific elimination"),
                            List.of("isomer mixture", "over-elimination")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Salt and base adduct relaxation",
                            "Base conjugate acid, leaving-group salt and alkene product relax under reaction conditions.",
                            0.08,
                            List.of("base salt", "workup", "alkene isolation"),
                            List.of("alkene isomerization", "polymerization"))),
                    List.of(
                        "strong base",
                        "anti-periplanar beta hydrogen",
                        "heat",
                        "good leaving group"),
                    List.of("no beta hydrogen", "poor leaving group", "SN2 competition")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ELECTROPHILIC_ADDITION,
                    "Electrophilic addition",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                            "Pi-bond exposure and reagent approach",
                            "Alkene or alkyne geometry exposes the pi bond to electrophile, acid, halogen or polarized reagent.",
                            0.14,
                            List.of("alkene", "alkyne", "accessible pi bond", "electrophile"),
                            List.of("steric shielding", "conjugate addition competition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ELECTROPHILIC_ATTACK,
                            "Electrophile capture",
                            "The pi bond attacks the electrophile and forms a cationic, bridged, or polarized intermediate.",
                            0.30,
                            List.of(
                                "Markovnikov control", "halonium ion", "protonation", "Lewis acid"),
                            List.of("carbocation rearrangement", "radical pathway")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Intermediate ion-pair organization",
                            "Counterion, solvent and substituents control facial approach, rearrangement risk and regioselectivity.",
                            0.16,
                            List.of(
                                "solvent cage",
                                "neighboring group participation",
                                "regioselectivity"),
                            List.of("polyaddition", "unstable cation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Nucleophile addition to intermediate",
                            "Counterion, solvent or external nucleophile captures the activated carbon framework.",
                            0.26,
                            List.of("halide", "water", "alcohol", "anti addition", "syn addition"),
                            List.of("wrong nucleophile", "over-addition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Product proton balance",
                            "Final proton transfer gives the neutral addition product and sets the observed regiochemical family.",
                            0.14,
                            List.of("neutral product", "workup", "proton shuttle"),
                            List.of("acid-sensitive product", "reversible addition"))),
                    List.of(
                        "alkene",
                        "alkyne",
                        "electrophile",
                        "Markovnikov or anti-Markovnikov control"),
                    List.of(
                        "carbocation rearrangement", "polyaddition", "competing radical pathway")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.RADICAL_SUBSTITUTION,
                    "Radical substitution",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.RADICAL_INITIATION,
                            "Radical source activation",
                            "Light, peroxide, azo initiator or redox reagent generates the first chain-carrying radical.",
                            0.16,
                            List.of("light", "peroxide", "AIBN", "halogen source"),
                            List.of("radical inhibitor", "oxygen excess")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.HYDROGEN_ATOM_TRANSFER,
                            "Selective hydrogen abstraction",
                            "The radical abstracts the weakest or most accessible C-H/X-H bond to form the substrate radical.",
                            0.24,
                            List.of("weak C-H bond", "benzylic", "allylic", "tertiary center"),
                            List.of("poor selectivity", "strong C-H bond")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.HALOGEN_ATOM_TRANSFER,
                            "Atom or group transfer",
                            "The substrate radical captures halogen or another transferable group from the reagent pool.",
                            0.24,
                            List.of("halogen source", "NBS/NCS", "selective atom donor"),
                            List.of("over-halogenation", "wrong atom donor")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RADICAL_PROPAGATION,
                            "Chain propagation regeneration",
                            "A new radical carrier is regenerated so the substitution chain can continue with measured quantum yield.",
                            0.20,
                            List.of(
                                "chain propagation",
                                "radical-compatible substrate",
                                "thermal/light drive"),
                            List.of("short chain length", "side radical trap")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.RADICAL_TERMINATION,
                            "Termination and side-product control",
                            "Radical recombination or disproportionation closes minor pathways and limits conversion.",
                            0.16,
                            List.of("controlled concentration", "dilution", "terminating workup"),
                            List.of("dimerization", "tar formation"))),
                    List.of(
                        "weak C-H bond",
                        "halogen source",
                        "light or initiator",
                        "radical-compatible substrate"),
                    List.of("poor selectivity", "radical inhibitor", "over-halogenation")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.RADICAL_CHAIN,
                    "Radical chain mechanism",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.RADICAL_INITIATION,
                            "Initiator homolysis or photolysis",
                            "Thermal, photochemical or redox initiation produces the radical carrier population.",
                            0.18,
                            List.of("radical initiator", "light", "peroxide", "redox initiation"),
                            List.of("inhibitor", "no initiation energy")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.RADICAL_ATTACK,
                            "Radical attack or abstraction",
                            "The radical carrier adds to a multiple bond or abstracts an atom to create the substrate radical.",
                            0.26,
                            List.of("weak bond", "alkene", "radical acceptor", "accessible site"),
                            List.of("steric shielding", "spin trap")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RADICAL_PROPAGATION,
                            "Propagation turnover",
                            "The substrate radical transfers atom/group or adds to another partner and regenerates the chain carrier.",
                            0.30,
                            List.of(
                                "chain carrier regeneration",
                                "high quantum yield",
                                "propagation step"),
                            List.of("oxygen-sensitive chain", "chain transfer side path")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Radical stabilization and selectivity",
                            "Spin density, captodative stabilization and conjugation determine the dominant radical site.",
                            0.12,
                            List.of(
                                "allylic radical", "benzylic radical", "captodative stabilization"),
                            List.of("delocalized regioisomers", "low site selectivity")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.RADICAL_TERMINATION,
                            "Termination competition",
                            "Combination, disproportionation, inhibitor capture or wall quenching consumes radicals.",
                            0.14,
                            List.of("termination rate", "controlled conversion", "dilution"),
                            List.of("dimerization", "runaway chain"))),
                    List.of("radical initiator", "light", "peroxide", "weak bond"),
                    List.of("radical inhibitor", "oxygen-sensitive chain")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PERICYCLIC,
                    "Pericyclic concerted mechanism",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.PERICYCLIC_CONCERTED,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("conjugated pi system", "thermal or photochemical control"),
                    List.of("broken conjugation", "forbidden orbital symmetry")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CYCLOADDITION,
                    "Cycloaddition",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.CYCLOADDITION,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("diene/dienophile", "dipole/dipolarophile", "orbital overlap"),
                    List.of("wrong stereochemical approach", "competing polymerization")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ELECTROCYCLIC,
                    "Electrocyclic ring opening or closure",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.ELECTROCYCLIC_STEP,
                        MechanisticEventKind.RING_OPENING),
                    List.of(
                        "conjugated polyene", "thermal or photochemical mode", "allowed rotation"),
                    List.of("wrong conrotatory/disrotatory mode", "geometry locked")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.SIGMATROPIC,
                    "Sigmatropic rearrangement",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.SIGMATROPIC_SHIFT,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("allylic system", "sigma bond migration", "allowed topology"),
                    List.of("poor orbital alignment", "fragmentation competition")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.REARRANGEMENT,
                    "Molecular rearrangement",
                    List.of(
                        MechanisticEventKind.REARRANGEMENT_MIGRATION,
                        MechanisticEventKind.BOND_CLEAVAGE,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("migrating group", "cation/radical/anion center"),
                    List.of("no migratory aptitude", "competing fragmentation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.TAUTOMERIZATION,
                    "Tautomerization",
                    List.of(
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.RESONANCE_REORGANIZATION,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("mobile proton", "acid/base catalysis", "heteroatom pi system"),
                    List.of("locked tautomer", "wrong protonation microstate")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.OXIDATION,
                    "Oxidation",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Oxidant-substrate encounter",
                            "The oxidant, metal-oxo reagent, electrochemical interface or oxygen-transfer species binds or approaches the substrate.",
                            0.16,
                            List.of(
                                "oxidant", "metal oxo", "positive potential", "oxygen transfer"),
                            List.of("redox-inactive substrate", "poor solubility")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ELECTRON_TRANSFER,
                            "Electron or oxygen-transfer activation",
                            "Electron density is removed from the substrate or an oxygen-transfer transition state is formed.",
                            0.28,
                            List.of("accessible HOMO", "matched redox potential", "oxygen donor"),
                            List.of("overpotential", "electron-poor substrate")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Proton-coupled redox balance",
                            "Acid/base or solvent assists charge balance after electron removal and controls selectivity.",
                            0.18,
                            List.of("PCET", "buffer", "base", "water"),
                            List.of("wrong pH", "radical cation fragmentation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.ELECTROCHEMICAL_OXIDATION,
                            "Oxidized product manifold formation",
                            "The oxidized intermediate resolves into alcohol, carbonyl, heteroatom oxide, coupling product or other ranked state.",
                            0.24,
                            List.of(
                                "positive potential",
                                "selective oxidation",
                                "functional group tolerance"),
                            List.of("overoxidation", "C-C cleavage")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Workup and redox byproduct separation",
                            "Reduced oxidant, salts and product microstates relax under workup conditions.",
                            0.14,
                            List.of("workup", "quenched oxidant", "product isolation"),
                            List.of("oxidant residue", "metal contamination"))),
                    List.of(
                        "oxidant",
                        "positive potential",
                        "oxygen transfer",
                        "matched redox potential"),
                    List.of(
                        "overoxidation",
                        "redox-inactive substrate",
                        "functional group incompatibility")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.REDUCTION,
                    "Reduction",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Reductant-substrate organization",
                            "Hydride donor, metal, cathode or electron donor approaches the reducible functional group or radical acceptor.",
                            0.16,
                            List.of(
                                "reductant",
                                "hydride source",
                                "negative potential",
                                "metal catalyst"),
                            List.of("protic quench", "substrate insoluble")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ELECTRON_TRANSFER,
                            "Electron delivery or polarization",
                            "The substrate accepts electron density through single-electron transfer, two-electron transfer or polarized hydride approach.",
                            0.26,
                            List.of("accessible LUMO", "matched reduction potential", "SET"),
                            List.of("redox-inactive substrate", "competing proton reduction")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.HYDRIDE_TRANSFER,
                            "Hydride or atom transfer",
                            "Hydride, hydrogen atom or metal-bound hydrogen is delivered to the ranked electrophilic center.",
                            0.26,
                            List.of(
                                "carbonyl", "iminium", "nitro group", "catalytic hydrogenation"),
                            List.of("over-reduction", "wrong chemoselectivity")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Protonation and charge neutralization",
                            "Proton source, solvent or workup neutralizes the reduced intermediate and sets product microstate.",
                            0.18,
                            List.of("proton source", "acid workup", "alcohol/amine product"),
                            List.of("proton source absent", "anion trapped")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Reduced product and reagent byproduct relaxation",
                            "Metal salts, borates, aluminates or electrode byproducts separate from the neutral product.",
                            0.14,
                            List.of("workup", "salt separation", "neutral product"),
                            List.of("functional group incompatibility", "metal residue"))),
                    List.of(
                        "reductant",
                        "hydride source",
                        "negative potential",
                        "matched reduction potential"),
                    List.of(
                        "functional group incompatibility",
                        "proton source absent",
                        "over-reduction")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDROGENATION,
                    "Catalytic hydrogenation",
                    List.of(
                        MechanisticEventKind.CATALYST_ASSOCIATION,
                        MechanisticEventKind.HYDROGEN_ATOM_TRANSFER,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "H2",
                        "Pd/Pt/Ni catalyst",
                        "alkene or alkyne",
                        "surface-compatible substrate"),
                    List.of(
                        "catalyst poisoning", "over-reduction", "hydrogenolysis-sensitive group")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDROBORATION_OXIDATION,
                    "Hydroboration oxidation",
                    List.of(
                        MechanisticEventKind.BORON_ADDITION,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.OXIDATIVE_WORKUP,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "alkene", "borane reagent", "anti-Markovnikov target", "oxidative workup"),
                    List.of(
                        "air/moisture sensitivity",
                        "competing reduction",
                        "steric reagent mismatch")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.EPOXIDATION,
                    "Epoxidation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.EPOXIDE_FORMATION,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of(
                        "alkene", "peracid or metal oxo reagent", "stereospecific oxygen transfer"),
                    List.of("allylic oxidation", "acid-sensitive epoxide", "overoxidation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HALOGENATION,
                    "Halogenation",
                    List.of(
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.HALOGEN_ATOM_TRANSFER,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE),
                    List.of(
                        "halogen source",
                        "activated aromatic/alkene/enolate",
                        "site-directing condition"),
                    List.of(
                        "polyhalogenation",
                        "oxidation-sensitive substrate",
                        "low regioselectivity")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.ORGANOMETALLIC_CROSS_COUPLING,
                    "Organometallic cross-coupling",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CATALYST_ASSOCIATION,
                            "Active catalyst generation",
                            "Precatalyst, ligand and base generate the low-valent active Pd/Ni/Cu species in the ligand sphere.",
                            0.14,
                            List.of("Pd/Ni/Cu catalyst", "ligand", "base", "inert atmosphere"),
                            List.of("no catalyst", "ligand mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.OXIDATIVE_ADDITION,
                            "Electrophile oxidative addition",
                            "Aryl, vinyl, benzyl or heteroaryl halide/pseudohalide inserts into the low-valent metal center.",
                            0.26,
                            List.of(
                                "aryl halide",
                                "heteroaryl halide",
                                "activated C-X bond",
                                "Ni/Pd catalyst"),
                            List.of("poor leaving group", "catalyst poisoning heteroatom")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.TRANSMETALATION,
                            "Nucleophile transmetallation",
                            "Organoboron, organozinc, organostannane, organomagnesium or related partner transfers the organic group to metal.",
                            0.24,
                            List.of("organometallic partner", "base", "boronate", "zinc reagent"),
                            List.of("no base", "protodeboronation", "hydrolysis")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.REDUCTIVE_ELIMINATION,
                            "C-C or C-heteroatom bond-forming reductive elimination",
                            "Ligand environment aligns both organic fragments and releases coupled product while reducing the metal.",
                            0.26,
                            List.of("cis fragments", "bulky ligand", "coupled product"),
                            List.of("beta-hydride risk", "homocoupling")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.CATALYST_DISSOCIATION,
                            "Catalyst turnover and side-path accounting",
                            "The active metal species is regenerated or deactivated; homocoupling, deboronation and dehalogenation are ranked.",
                            0.10,
                            List.of("turnover", "low catalyst loading", "base balance"),
                            List.of("catalyst black", "heterocycle chelation"))),
                    List.of(
                        "Pd/Ni/Cu catalyst",
                        "base",
                        "organometallic partner",
                        "aryl halide",
                        "heteroaryl halide"),
                    List.of(
                        "no catalyst",
                        "no base",
                        "beta-hydride risk",
                        "heteroatom catalyst poisoning")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ORGANOMETALLIC_INSERTION,
                    "Organometallic insertion pathway",
                    List.of(
                        MechanisticEventKind.LIGAND_EXCHANGE,
                        MechanisticEventKind.MIGRATORY_INSERTION,
                        MechanisticEventKind.BETA_HYDRIDE_ELIMINATION),
                    List.of("metal hydride/alkyl", "alkene or CO", "open coordination site"),
                    List.of("ligand saturation", "unwanted beta-hydride elimination")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.METATHESIS,
                    "Olefin metathesis",
                    List.of(
                        MechanisticEventKind.CATALYST_ASSOCIATION,
                        MechanisticEventKind.CARBENE_FORMATION,
                        MechanisticEventKind.CYCLOADDITION,
                        MechanisticEventKind.CARBENE_INSERTION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of("alkene", "metal carbene catalyst", "low coordinating impurities"),
                    List.of("catalyst poisoning", "unproductive terminal alkene isomerization")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.CATALYTIC_CYCLE,
                    "General catalytic cycle",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CATALYST_ASSOCIATION,
                            "Substrate-catalyst association",
                            "Catalyst active site, ligand sphere or surface binds the substrate and sets the turnover entry point.",
                            0.18,
                            List.of(
                                "catalyst",
                                "ligand",
                                "substrate binding",
                                "open coordination site"),
                            List.of("catalyst poisoning", "steric exclusion")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.LIGAND_EXCHANGE,
                            "Ligand or reagent exchange",
                            "Ancillary ligand, solvent, base or reagent exchange prepares the active intermediate for bond rearrangement.",
                            0.16,
                            List.of("ligand lability", "base", "solvent coordination"),
                            List.of("ligand mismatch", "inactive saturated complex")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.MIGRATORY_INSERTION,
                            "Turnover-limiting chemical step",
                            "Insertion, transfer, oxidative addition, reductive elimination or equivalent chemical step forms the product skeleton.",
                            0.34,
                            List.of(
                                "turnover-limiting step",
                                "matched substrate",
                                "productive transition state"),
                            List.of("high barrier", "off-cycle reservoir")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.CATALYST_DISSOCIATION,
                            "Product release",
                            "Product dissociates, freeing the active site or metal center for another catalytic cycle.",
                            0.20,
                            List.of("product release", "low product inhibition", "turnover"),
                            List.of("product chelation", "precipitation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.SOLVATION_REORGANIZATION,
                            "Catalyst resting-state balance",
                            "Resting states, deactivation and regeneration pathways are balanced under reaction conditions.",
                            0.12,
                            List.of("resting state", "regeneration", "stable catalyst"),
                            List.of("catalyst decomposition", "poisoning"))),
                    List.of("catalyst", "ligand", "turnover-limiting step", "active site"),
                    List.of("catalyst poisoning", "ligand mismatch", "off-cycle reservoir")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PHOTOCHEMICAL,
                    "Photochemical pathway",
                    List.of(
                        MechanisticEventKind.PHOTOCHEMICAL_EXCITATION,
                        MechanisticEventKind.TRIPLET_ENERGY_TRANSFER,
                        MechanisticEventKind.INTERSYSTEM_CROSSING,
                        MechanisticEventKind.NITRENE_FORMATION,
                        MechanisticEventKind.NITRENE_INSERTION,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.PHOTOCHEMICAL_QUENCHING),
                    List.of("light", "photosensitizer", "triplet state"),
                    List.of("no absorption overlap", "quenching")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ELECTROCHEMICAL,
                    "Electrochemical pathway",
                    List.of(
                        MechanisticEventKind.ELECTROCHEMICAL_OXIDATION,
                        MechanisticEventKind.ELECTROCHEMICAL_REDUCTION,
                        MechanisticEventKind.ELECTRON_TRANSFER,
                        MechanisticEventKind.ANODIC_COUPLING,
                        MechanisticEventKind.CATHODIC_FRAGMENTATION),
                    List.of("electrode potential", "electrolyte", "redox-active substrate"),
                    List.of("passivation", "mass transport limitation")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.HETEROCYCLE_CONSTRUCTION,
                    "Heterocycle construction",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Component organization and reactive-state selection",
                            "Heteroatom nucleophile, pi partner, carbonyl/hydrazone component or metal catalyst is organized before the productive bond-making branch.",
                            0.12,
                            List.of(
                                "heteroatom nucleophile",
                                "carbonyl partner",
                                "alkyne",
                                "hydrazone",
                                "metal catalyst"),
                            List.of(
                                "component incompatibility",
                                "wrong protonation state",
                                "poor solubility")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.CONDENSATION,
                            "Condensation or imine/hydrazone precursor formation",
                            "Carbonyl, amine, hydrazine or dicarbonyl components form the imine, enamine, hydrazone or dehydrated precursor needed for ring construction.",
                            0.16,
                            List.of(
                                "carbonyl",
                                "amine",
                                "hydrazine",
                                "dehydrating condition",
                                "water removal"),
                            List.of("water excess", "wrong tautomer", "component ratio drift")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.OXIDATIVE_ADDITION,
                            "Metal insertion or activated leaving-group entry",
                            "For Larock/Sonogashira-like heteroannulation, metal enters an aryl halide or related activated bond before migratory insertion.",
                            0.13,
                            List.of(
                                "aryl iodide",
                                "aryl bromide",
                                "palladium",
                                "copper",
                                "leaving group"),
                            List.of(
                                "catalyst poisoning",
                                "oxidative addition too slow",
                                "ligand mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                            "Pi-partner addition or cycloaddition branch",
                            "Alkyne, alkene, dipole or enone partner enters the ring-forming manifold through insertion, cycloaddition or conjugate addition.",
                            0.18,
                            List.of("alkyne", "alkene", "dipole", "dipolarophile", "enone"),
                            List.of("wrong regioselectivity", "polymerization", "steric mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.MIGRATORY_INSERTION,
                            "Regioselectivity-determining insertion or rearrangement",
                            "Substituent electronics and sterics select the major regioisomer during insertion, rearrangement, or open-chain intermediate reorganization.",
                            0.18,
                            List.of(
                                "unsymmetric alkyne",
                                "substituent bulk",
                                "electron withdrawal",
                                "regioisomer control"),
                            List.of(
                                "competing regioisomers",
                                "unmodeled directing group",
                                "mixed conformers")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            5,
                            MechanisticEventKind.RING_CLOSURE,
                            "Heterocycle ring closure",
                            "Intramolecular C-N, C-O, C-S or C-C bond formation closes the five- or six-membered heterocyclic framework.",
                            0.18,
                            List.of(
                                "intramolecular closure",
                                "heteroatom attack",
                                "aromatization path",
                                "fused ring"),
                            List.of(
                                "wrong tether length", "macrocycle competition", "fragmentation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            6,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Aromatization, tautomer and oxidation-state settling",
                            "The closed intermediate restores aromaticity or settles into the observed tautomer/oxidation state before isolation.",
                            0.15,
                            List.of(
                                "aromatization", "tautomer control", "oxidation state", "workup"),
                            List.of("tautomer mixture", "overoxidation", "product salt trapped"))),
                    List.of(
                        "bifunctional precursor",
                        "heteroatom nucleophile",
                        "alkyne or carbonyl partner",
                        "dehydrating condition",
                        "metal catalyst"),
                    List.of(
                        "wrong tether length",
                        "competing polymerization",
                        "regioisomer mixture",
                        "catalyst poisoning")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.HETEROCYCLE_FUNCTIONALIZATION,
                    "Heterocycle functionalization",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.PROTONATION,
                            "Heteroatom protonation and directing-state selection",
                            "Heterocycle protonation, metalation or N-oxide state selects the reactive site before bond editing.",
                            0.16,
                            List.of(
                                "heteroatom-directed site",
                                "activated heterocycle",
                                "N-oxide",
                                "protonation state"),
                            List.of("wrong protonation state", "multiple competing sites")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.COMPLEXATION,
                            "Regioselective reagent precomplex",
                            "Lewis acid, base, catalyst or reagent is positioned by ring heteroatoms and substituent electronics.",
                            0.16,
                            List.of("directing group", "Lewis acid", "base", "catalyst"),
                            List.of("chelating poison", "substrate salt precipitation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.ELECTROPHILIC_ATTACK,
                            "Electrophilic or metal-mediated site attack",
                            "Electrophile, organometallic fragment or activated partner forms the first bond at the selected ring site.",
                            0.22,
                            List.of(
                                "electrophile", "C-H activation", "halogenation", "metal catalyst"),
                            List.of("overfunctionalization", "wrong regioisomer")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "Complementary nucleophilic displacement branch",
                            "Electron-poor heterocycles can proceed by SNAr-like nucleophilic addition instead of electrophilic attack.",
                            0.18,
                            List.of("electron-poor heterocycle", "leaving group", "nucleophile"),
                            List.of("electron-rich ring", "poor leaving group")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.RESONANCE_REORGANIZATION,
                            "Aromaticity and tautomer restoration",
                            "Charge, tautomer and aromaticity are reorganized to the observed functionalized heteroaromatic product.",
                            0.18,
                            List.of(
                                "aromaticity restoration", "tautomer control", "ring heteroatom"),
                            List.of("tautomer mixture", "N-oxide mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            5,
                            MechanisticEventKind.DEPROTONATION,
                            "Final deprotonation or catalyst release",
                            "Base or catalyst turnover restores neutral product and releases the active reagent/catalyst state.",
                            0.10,
                            List.of("base", "catalyst turnover", "neutral product"),
                            List.of("product salt", "catalyst deactivation"))),
                    List.of("heteroatom-directed site", "activated heterocycle"),
                    List.of("multiple competing sites", "N-oxide or protonation state mismatch")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.MULTICOMPONENT_CONDENSATION,
                    "Multicomponent condensation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.CONDENSATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.RING_CLOSURE),
                    List.of(
                        "three or more components",
                        "carbonyl partner",
                        "amine or heteroatom nucleophile",
                        "water removal"),
                    List.of("component incompatibility", "polymerization", "wrong stoichiometry")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.CLICK_CYCLOADDITION,
                    "Click cycloaddition",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.COMPLEXATION,
                            "Azide-alkyne catalyst encounter",
                            "Copper, ruthenium or strain activation organizes azide and alkyne partners in the productive geometry.",
                            0.16,
                            List.of(
                                "azide",
                                "alkyne",
                                "Cu catalyst",
                                "Ru catalyst",
                                "strain activation"),
                            List.of("poor solubility", "metal-sensitive substrate")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.COPPER_ACETYLIDE_FORMATION,
                            "Metal acetylide generation",
                            "Terminal alkyne deprotonation and metal coordination form the reactive acetylide manifold.",
                            0.20,
                            List.of("terminal alkyne", "amine base", "copper acetylide"),
                            List.of("Glaser homocoupling", "no base")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.CYCLOADDITION,
                            "Concerted or stepwise triazoline formation",
                            "Azide and activated alkyne combine through a regioselective cycloaddition manifold.",
                            0.28,
                            List.of("triazole target", "regioselectivity", "dipole/dipolarophile"),
                            List.of("azide safety risk", "wrong regiochemistry")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RING_CLOSURE,
                            "Triazole ring aromatization",
                            "The five-membered ring closes and reorganizes into aromatic triazole connectivity.",
                            0.22,
                            List.of("ring closure", "aromatic triazole", "tautomer control"),
                            List.of("metal-bound product", "tautomer ambiguity")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.CATALYST_DISSOCIATION,
                            "Catalyst release and product demetalation",
                            "Metal dissociates or is removed during workup to give the isolated triazole product.",
                            0.14,
                            List.of("workup", "catalyst turnover", "demetalation"),
                            List.of("copper contamination", "product chelation"))),
                    List.of(
                        "azide",
                        "alkyne",
                        "Cu/Ru catalyst or strain activation",
                        "triazole target"),
                    List.of("metal-sensitive substrate", "azide safety risk", "poor solubility")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ORGANOCATALYSIS,
                    "Organocatalytic cycle",
                    List.of(
                        MechanisticEventKind.ENANTIOSELECTIVE_CATALYST_BINDING,
                        MechanisticEventKind.IMINIUM_FORMATION,
                        MechanisticEventKind.ENAMINE_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "organocatalyst",
                        "carbonyl substrate",
                        "enantioselective environment",
                        "acid/base cocatalyst"),
                    List.of(
                        "water sensitivity", "catalyst poisoning", "racemic background reaction")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PROTECTING_GROUP,
                    "Protection or deprotection",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.PROTECTING_GROUP_INSTALLATION,
                        MechanisticEventKind.PROTECTING_GROUP_REMOVAL,
                        MechanisticEventKind.ORTHOGONAL_DEPROTECTION),
                    List.of("labile functional group", "selective reagent", "orthogonal condition"),
                    List.of("global deprotection", "functional group incompatibility")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.POLYMERIZATION,
                    "Polymerization",
                    List.of(
                        MechanisticEventKind.RADICAL_INITIATION,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.RADICAL_TERMINATION),
                    List.of("monomer", "initiator", "chain propagation"),
                    List.of("inhibitor", "gelation risk")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.POLYMERIZATION_CHAIN_GROWTH,
                    "Chain-growth polymerization",
                    List.of(
                        MechanisticEventKind.RADICAL_INITIATION,
                        MechanisticEventKind.CHAIN_GROWTH_PROPAGATION,
                        MechanisticEventKind.RADICAL_PROPAGATION,
                        MechanisticEventKind.RADICAL_TERMINATION),
                    List.of(
                        "vinyl monomer",
                        "initiator",
                        "controlled propagation",
                        "inhibitor-free medium"),
                    List.of(
                        "oxygen inhibition",
                        "runaway exotherm",
                        "broad molecular-weight distribution")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.POLYMERIZATION_STEP_GROWTH,
                    "Step-growth polymerization",
                    List.of(
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.STEP_GROWTH_COUPLING,
                        MechanisticEventKind.CONDENSATION,
                        MechanisticEventKind.PRECIPITATION),
                    List.of(
                        "bifunctional monomers",
                        "stoichiometric balance",
                        "water or alcohol removal"),
                    List.of(
                        "monofunctional impurity", "low conversion", "viscosity-limited mixing")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.DIAZOTIZATION,
                    "Aromatic diazotization",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.DIAZONIUM_FORMATION,
                        MechanisticEventKind.SOLVATION_REORGANIZATION),
                    List.of("aniline", "nitrite source", "strong acid", "low temperature"),
                    List.of("warm diazonium salt", "electron-poor amine", "excess oxidant")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.SANDMEYER,
                    "Sandmeyer substitution",
                    List.of(
                        MechanisticEventKind.DIAZONIUM_FORMATION,
                        MechanisticEventKind.ELECTRON_TRANSFER,
                        MechanisticEventKind.ARYNE_FORMATION,
                        MechanisticEventKind.ARYNE_ADDITION,
                        MechanisticEventKind.DIAZONIUM_SUBSTITUTION,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of(
                        "aryl diazonium",
                        "Cu(I) salt",
                        "halide or cyanide source",
                        "controlled temperature"),
                    List.of(
                        "diazonium decomposition", "phenol side product", "radical side reaction")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.GRIGNARD_OR_ORGANOLITHIUM_ADDITION,
                    "Grignard or organolithium addition",
                    List.of(
                        MechanisticEventKind.METAL_HALOGEN_EXCHANGE,
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.ORGANOMETALLIC_ADDITION,
                        MechanisticEventKind.EPOXIDE_OPENING,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "organomagnesium or organolithium reagent",
                        "dry ether",
                        "carbonyl or epoxide",
                        "acid workup"),
                    List.of(
                        "water or oxygen",
                        "acidic proton",
                        "competing enolization",
                        "over-addition")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ESTERIFICATION,
                    "Esterification",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.ACYL_TRANSFER,
                        MechanisticEventKind.ESTERIFICATION,
                        MechanisticEventKind.CONDENSATION),
                    List.of(
                        "carboxylic acid or activated acyl donor",
                        "alcohol",
                        "acid catalyst",
                        "water removal"),
                    List.of(
                        "water excess", "acid-sensitive substrate", "sterically hindered alcohol")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.TRANSESTERIFICATION,
                    "Transesterification",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.ACYL_TRANSFER,
                        MechanisticEventKind.TRANSESTERIFICATION,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE),
                    List.of("ester", "alcohol", "acid/base catalyst", "removal of leaving alcohol"),
                    List.of(
                        "equilibrium limitation",
                        "transacylation mixture",
                        "base-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDROLYSIS,
                    "Hydrolysis",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.HYDROLYSIS,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("water", "acid or base", "ester/amide/acetal/nitrile", "controlled pH"),
                    List.of(
                        "over-hydrolysis", "epimerization", "water-sensitive protecting group")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.SOLVOLYSIS,
                    "Solvolysis",
                    List.of(
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                        MechanisticEventKind.SOLVATION_REORGANIZATION,
                        MechanisticEventKind.SOLVOLYSIS,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("good leaving group", "polar solvent nucleophile", "stabilized cation"),
                    List.of("rearrangement", "elimination competition", "racemization")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.FRAGMENTATION,
                    "Fragmentation",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.BOND_CLEAVAGE,
                        MechanisticEventKind.FRAGMENTATION,
                        MechanisticEventKind.RESONANCE_REORGANIZATION),
                    List.of(
                        "strained scaffold",
                        "beta leaving group",
                        "redox or base trigger",
                        "relief of strain"),
                    List.of(
                        "uncontrolled cleavage", "polyfragmentation", "competing rearrangement")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.RADICAL_CYCLIZATION,
                    "Radical cyclization",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.RADICAL_INITIATION,
                            "Radical precursor activation",
                            "Light, initiator, tin hydride, silane or redox trigger generates the first carbon or heteroatom radical.",
                            0.18,
                            List.of("radical precursor", "initiator", "light", "redox trigger"),
                            List.of("radical inhibitor", "oxygen inhibition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                            "Tether geometry selection",
                            "Substrate conformation selects 5-exo, 6-endo or competing intermolecular trapping geometry.",
                            0.14,
                            List.of("tethered alkene", "tethered alkyne", "5-exo", "6-endo"),
                            List.of("wrong tether length", "intermolecular trapping")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RADICAL_CYCLIZATION,
                            "Intramolecular radical addition",
                            "The radical adds into the tethered pi bond and forms the new ring bond.",
                            0.30,
                            List.of(
                                "alkene", "alkyne", "ring closure", "radical-compatible substrate"),
                            List.of("premature reduction", "poor orbital overlap")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RADICAL_PROPAGATION,
                            "Product radical propagation",
                            "The new radical abstracts atom or transfers electron to continue the chain and set product oxidation state.",
                            0.22,
                            List.of(
                                "hydrogen atom donor",
                                "halogen atom transfer",
                                "chain propagation"),
                            List.of("overreduction", "fragmentation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.RADICAL_TERMINATION,
                            "Termination and side-path competition",
                            "Radical chain terminates by recombination, disproportionation or trapping; side products are ranked.",
                            0.16,
                            List.of("termination", "recombination", "product radical"),
                            List.of("dimerization", "oxygen trapping"))),
                    List.of(
                        "radical precursor",
                        "tethered alkene/alkyne",
                        "5-exo or 6-endo geometry",
                        "initiator/light"),
                    List.of("premature reduction", "intermolecular trapping", "oxygen inhibition")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.C_H_ACTIVATION,
                    "C-H activation",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CATALYST_ASSOCIATION,
                            "Directing group catalyst precoordination",
                            "Transition metal binds the directing group or heteroatom and selects a proximal C-H bond.",
                            0.16,
                            List.of(
                                "directing group",
                                "transition-metal catalyst",
                                "heterocycle",
                                "site-selective C-H bond"),
                            List.of("chelating poison", "no directing group")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.DIFFUSION_LIMITED_ENCOUNTER,
                            "Coupling partner and oxidant encounter",
                            "Oxidant/base/coupling partner availability controls whether productive turnover is possible.",
                            0.10,
                            List.of("oxidant", "base", "coupling partner", "ligand"),
                            List.of("mass transport limitation", "reagent incompatibility")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.C_H_ACTIVATION,
                            "C-H cleavage",
                            "Concerted metalation-deprotonation, oxidative addition or electrophilic metalation cleaves the selected C-H bond.",
                            0.26,
                            List.of("CMD", "C-H activation", "acetate base", "site-selective"),
                            List.of("unselective activation", "acidic site mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.CYCLOMETALATION,
                            "Cyclometalated intermediate formation",
                            "A metallacycle or organometallic intermediate stores the activated aryl/heteroaryl fragment.",
                            0.18,
                            List.of("cyclometalation", "metallacycle", "aryl metal"),
                            List.of("beta-hydride risk", "ring strain")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.MIGRATORY_INSERTION,
                            "Functionalizing partner insertion or transmetalation branch",
                            "Alkene, CO, aryl, alkynyl or heteroatom partner enters the metal-carbon bond manifold.",
                            0.20,
                            List.of(
                                "alkene",
                                "CO",
                                "aryl partner",
                                "alkynyl partner",
                                "amination partner"),
                            List.of("wrong regioselectivity", "partner decomposition")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            5,
                            MechanisticEventKind.REDUCTIVE_ELIMINATION,
                            "Product-forming reductive elimination",
                            "The new C-C or C-heteroatom bond forms and the catalyst is returned through oxidation-state management.",
                            0.10,
                            List.of(
                                "reductive elimination", "catalyst turnover", "product release"),
                            List.of("catalyst deactivation", "overfunctionalization"))),
                    List.of(
                        "directing group",
                        "transition-metal catalyst",
                        "oxidant or coupling partner",
                        "site-selective C-H bond"),
                    List.of(
                        "unselective activation",
                        "catalyst deactivation",
                        "overfunctionalization")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.OZONOLYSIS,
                    "Ozonolysis",
                    List.of(
                        MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                        MechanisticEventKind.OZONIDE_FORMATION,
                        MechanisticEventKind.OZONIDE_CLEAVAGE,
                        MechanisticEventKind.OXIDATIVE_WORKUP),
                    List.of(
                        "alkene or alkyne",
                        "ozone",
                        "reductive or oxidative workup",
                        "low temperature control"),
                    List.of("overoxidation", "peroxide hazard", "ozonide instability")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.OXIDATIVE_CLEAVAGE,
                    "Oxidative cleavage",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXIDATIVE_CLEAVAGE,
                        MechanisticEventKind.BOND_CLEAVAGE,
                        MechanisticEventKind.OXYGEN_TRANSFER),
                    List.of("vicinal diol/alkene", "strong oxidant", "aqueous or polar medium"),
                    List.of("overoxidation", "sensitive heteroatom oxidation", "fragment mixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.BAEYER_VILLIGER,
                    "Baeyer-Villiger oxidation",
                    List.of(
                        MechanisticEventKind.PEROXY_ACID_ADDITION,
                        MechanisticEventKind.ALKYL_MIGRATION,
                        MechanisticEventKind.BOND_CLEAVAGE,
                        MechanisticEventKind.OXYGEN_TRANSFER),
                    List.of(
                        "ketone or aldehyde",
                        "peracid/oxidant",
                        "migratory aptitude",
                        "controlled acidity"),
                    List.of(
                        "wrong regioselectivity",
                        "epoxidation competition",
                        "acid-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.WOLFF_KISHNER_REDUCTION,
                    "Wolff-Kishner reduction",
                    List.of(
                        MechanisticEventKind.HYDRAZONE_FORMATION,
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.NITROGEN_EXTRUSION,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("aldehyde or ketone", "hydrazine", "strong base", "high temperature"),
                    List.of(
                        "base-sensitive substrate",
                        "incomplete hydrazone formation",
                        "thermal decomposition")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CLEMMENSEN_REDUCTION,
                    "Clemmensen reduction",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.ELECTRON_TRANSFER,
                        MechanisticEventKind.AMALGAM_REDUCTION,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("aldehyde or ketone", "Zn/Hg", "strong acid", "acid-stable substrate"),
                    List.of("acid-sensitive substrate", "metal poisoning", "over-reduction")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.NITRATION,
                    "Aromatic nitration",
                    List.of(
                        MechanisticEventKind.NITRONIUM_FORMATION,
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.SIGMA_COMPLEX_FORMATION,
                        MechanisticEventKind.DEPROTONATION),
                    List.of(
                        "aromatic ring", "nitrating acid mixture", "substituent directing effects"),
                    List.of("over-nitration", "oxidation", "wrong regioisomer")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.SULFONATION,
                    "Aromatic sulfonation",
                    List.of(
                        MechanisticEventKind.SULFONATING_AGENT_FORMATION,
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.SIGMA_COMPLEX_FORMATION,
                        MechanisticEventKind.DEPROTONATION),
                    List.of("aromatic ring", "SO3/sulfuric acid", "reversible temperature control"),
                    List.of("desulfonation", "polysulfonation", "acid-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.FRIEDEL_CRAFTS_ALKYLATION,
                    "Friedel-Crafts alkylation",
                    List.of(
                        MechanisticEventKind.ALKYL_CATION_EQUIVALENT_FORMATION,
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.SIGMA_COMPLEX_FORMATION,
                        MechanisticEventKind.DEPROTONATION),
                    List.of("electron-rich arene", "alkyl halide/alcohol/alkene", "Lewis acid"),
                    List.of("carbocation rearrangement", "polyalkylation", "deactivated ring")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.FRIEDEL_CRAFTS_ACYLATION,
                    "Friedel-Crafts acylation",
                    List.of(
                        MechanisticEventKind.ACYLIUM_FORMATION,
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.SIGMA_COMPLEX_FORMATION,
                        MechanisticEventKind.DEPROTONATION),
                    List.of(
                        "arene", "acyl chloride/anhydride", "Lewis acid", "acyl-stable substrate"),
                    List.of("deactivated ring", "complexation shutdown", "ortho/para mixture")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.SUZUKI_COUPLING,
                    "Suzuki-Miyaura coupling",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CATALYST_ASSOCIATION,
                            "Pd(0) ligand activation",
                            "Ligated palladium enters the catalytic cycle in a coordinatively competent low-valent state.",
                            0.12,
                            List.of("palladium catalyst", "ligand", "base"),
                            List.of("catalyst poisoning", "ligand mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.PALLADIUM_OXIDATIVE_ADDITION,
                            "Aryl or vinyl electrophile oxidative addition",
                            "Pd inserts into aryl/vinyl halide or pseudohalide to form the organopalladium intermediate.",
                            0.24,
                            List.of(
                                "aryl halide", "vinyl halide", "triflate", "oxidative addition"),
                            List.of("dehalogenation", "unreactive chloride")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.COMPLEXATION,
                            "Boronate activation by base",
                            "Base converts the organoboron partner into a transmetalation-competent boronate species.",
                            0.16,
                            List.of("organoboron reagent", "base", "boronate"),
                            List.of("protodeboronation", "base-sensitive heterocycle")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.PALLADIUM_TRANSMETALATION,
                            "Transmetalation",
                            "Organic group is transferred from boron to palladium to assemble both coupling partners on metal.",
                            0.24,
                            List.of("transmetalation", "boronic acid", "boronate ester"),
                            List.of("homocoupling", "water mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PALLADIUM_REDUCTIVE_ELIMINATION,
                            "C-C reductive elimination",
                            "The biaryl or alkenyl-aryl bond forms and Pd returns to the low-valent cycle.",
                            0.18,
                            List.of("biaryl target", "reductive elimination", "C-C bond"),
                            List.of("steric reductive-elimination barrier", "regioisomer mixture")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            5,
                            MechanisticEventKind.CATALYST_DISSOCIATION,
                            "Product release and catalyst regeneration",
                            "Product dissociates, salts are removed and the catalyst is regenerated or deactivated.",
                            0.06,
                            List.of("product release", "catalyst turnover", "workup"),
                            List.of("palladium black", "product chelation"))),
                    List.of(
                        "aryl/vinyl halide or pseudohalide",
                        "organoboron reagent",
                        "base",
                        "palladium catalyst"),
                    List.of("protodeboronation", "homocoupling", "catalyst poisoning")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HECK_COUPLING,
                    "Heck coupling",
                    List.of(
                        MechanisticEventKind.PALLADIUM_OXIDATIVE_ADDITION,
                        MechanisticEventKind.PALLADIUM_MIGRATORY_INSERTION,
                        MechanisticEventKind.BETA_HYDRIDE_ELIMINATION,
                        MechanisticEventKind.PALLADIUM_REDUCTIVE_ELIMINATION),
                    List.of("aryl/vinyl halide", "alkene", "base", "palladium catalyst"),
                    List.of(
                        "alkene isomerization", "beta-hydride side path", "poor regioselectivity")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.SONOGASHIRA_COUPLING,
                    "Sonogashira coupling",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.PALLADIUM_OXIDATIVE_ADDITION,
                            "Aryl/vinyl electrophile oxidative addition",
                            "Pd(0) inserts into the aryl or vinyl halide bond to create the electrophile-derived Pd(II) fragment.",
                            0.23,
                            List.of("aryl halide", "vinyl halide", "palladium catalyst"),
                            List.of("dehalogenation", "unreactive electrophile")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.COPPER_ACETYLIDE_FORMATION,
                            "Terminal alkyne copper acetylide formation",
                            "Amine base and copper co-catalyst generate the transmetalating alkynyl copper species.",
                            0.24,
                            List.of("terminal alkyne", "amine base", "copper catalyst"),
                            List.of("Glaser homocoupling", "copper sensitivity")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.PALLADIUM_TRANSMETALATION,
                            "Alkynyl transmetalation",
                            "The alkynyl group transfers to palladium and creates the coupled-partner Pd(II) intermediate.",
                            0.22,
                            List.of("transmetalation", "alkynyl copper", "Pd alkynyl"),
                            List.of("water/oxygen", "ligand mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.PALLADIUM_REDUCTIVE_ELIMINATION,
                            "C(sp2)-C(sp) reductive elimination",
                            "Reductive elimination forms the aryl/vinyl alkyne and regenerates Pd(0).",
                            0.20,
                            List.of("alkynyl arene", "reductive elimination", "C-C bond"),
                            List.of("steric barrier", "side dehalogenation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.CATALYST_DISSOCIATION,
                            "Catalyst release and copper cleanup",
                            "Metal-bound product is released and copper/palladium residues are separated or recycled.",
                            0.11,
                            List.of("workup", "catalyst release", "product isolation"),
                            List.of("metal contamination", "product chelation"))),
                    List.of("aryl/vinyl halide", "terminal alkyne", "amine base", "Pd/Cu catalyst"),
                    List.of("Glaser homocoupling", "copper sensitivity", "dehalogenation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.NEGISHI_COUPLING,
                    "Negishi coupling",
                    List.of(
                        MechanisticEventKind.CATALYST_ASSOCIATION,
                        MechanisticEventKind.PALLADIUM_OXIDATIVE_ADDITION,
                        MechanisticEventKind.PALLADIUM_TRANSMETALATION,
                        MechanisticEventKind.PALLADIUM_REDUCTIVE_ELIMINATION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "organozinc reagent", "aryl/vinyl halide", "palladium or nickel catalyst"),
                    List.of(
                        "air/moisture sensitivity",
                        "beta-hydride elimination",
                        "zinc reagent decomposition")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.STILLE_COUPLING,
                    "Stille coupling",
                    List.of(
                        MechanisticEventKind.CATALYST_ASSOCIATION,
                        MechanisticEventKind.PALLADIUM_OXIDATIVE_ADDITION,
                        MechanisticEventKind.PALLADIUM_TRANSMETALATION,
                        MechanisticEventKind.PALLADIUM_REDUCTIVE_ELIMINATION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "organostannane",
                        "aryl/vinyl halide",
                        "palladium catalyst",
                        "additive-controlled transmetalation"),
                    List.of("tin toxicity", "slow transmetalation", "protodestannylation")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.BUCHWALD_HARTWIG_AMINATION,
                    "Buchwald-Hartwig amination",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CATALYST_ASSOCIATION,
                            "Ligated palladium activation",
                            "Bulky electron-rich ligand prepares low-valent palladium for aryl electrophile activation.",
                            0.12,
                            List.of("bulky ligand", "palladium catalyst", "base"),
                            List.of("ligand mismatch", "catalyst poisoning")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.PALLADIUM_OXIDATIVE_ADDITION,
                            "Aryl halide oxidative addition",
                            "Pd inserts into the aryl halide or pseudohalide bond to form an aryl-Pd intermediate.",
                            0.22,
                            List.of("aryl halide", "triflate", "oxidative addition"),
                            List.of("aryl dehalogenation", "unreactive chloride")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.AMINATION_LIGAND_EXCHANGE,
                            "Amine coordination and deprotonation",
                            "Amine binds to palladium and base forms the amido-palladium species.",
                            0.26,
                            List.of("amine", "base", "amido palladium", "secondary amine"),
                            List.of("amine inhibition", "acidic salt", "sterically blocked amine")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.PALLADIUM_REDUCTIVE_ELIMINATION,
                            "C-N reductive elimination",
                            "The aryl amine C-N bond forms from the aryl-amido palladium intermediate.",
                            0.26,
                            List.of("C-N bond", "aryl amine", "reductive elimination"),
                            List.of("reductive-elimination barrier", "beta-hydride side path")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.CATALYST_DISSOCIATION,
                            "Product release and catalyst recycle",
                            "Aminated product dissociates and active palladium is regenerated or deactivated.",
                            0.14,
                            List.of("product release", "catalyst turnover", "workup"),
                            List.of("product chelation", "palladium black"))),
                    List.of("aryl halide", "amine", "base", "bulky ligand", "palladium catalyst"),
                    List.of("aryl dehalogenation", "amine inhibition", "ligand mismatch")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.DIELS_ALDER,
                    "Diels-Alder cycloaddition",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                            "s-cis diene conformer selection",
                            "The diene adopts the productive s-cis geometry and the dienophile aligns for orbital overlap.",
                            0.20,
                            List.of("diene", "s-cis diene geometry", "dienophile"),
                            List.of("wrong diene conformation", "locked trans diene")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.COMPLEXATION,
                            "Lewis acid or substituent activation",
                            "Lewis acid, pressure, solvent or substituent electronics tune the HOMO-LUMO gap and regioselectivity.",
                            0.14,
                            List.of("Lewis acid", "electron-poor dienophile", "endo control"),
                            List.of("Lewis acid poisoning", "polymerization")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.DIASTEREOSELECTIVE_APPROACH,
                            "Endo/exo approach selection",
                            "Secondary orbital interactions and sterics select the endo/exo and facial approach.",
                            0.20,
                            List.of(
                                "endo/exo control",
                                "diastereoselective approach",
                                "chiral auxiliary"),
                            List.of("low stereocontrol", "regioisomer mixture")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.CYCLOADDITION,
                            "Concerted six-electron cycloaddition",
                            "Two sigma bonds form through the pericyclic transition state without isolable ionic intermediates.",
                            0.34,
                            List.of("thermal allowed", "concerted", "orbital overlap"),
                            List.of("forbidden orbital symmetry", "retro-Diels-Alder")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.BOND_FORMATION,
                            "Cyclohexene product relaxation",
                            "The cycloadduct relaxes conformationally and competing retro or polymerization paths are ranked.",
                            0.12,
                            List.of("cycloadduct", "new sigma bonds", "product relaxation"),
                            List.of("retro-Diels-Alder", "strain-driven fragmentation"))),
                    List.of("diene", "dienophile", "s-cis diene geometry", "endo/exo control"),
                    List.of("wrong diene conformation", "polymerization", "retro-Diels-Alder")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ONE_THREE_DIPOLAR_CYCLOADDITION,
                    "1,3-dipolar cycloaddition",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.CYCLOADDITION,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("1,3-dipole", "dipolarophile", "orbital and regioselectivity control"),
                    List.of("regioisomer mixture", "dipole instability", "side oligomerization")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.BECKMANN_REARRANGEMENT,
                    "Beckmann rearrangement",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.BECKMANN_MIGRATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.TAUTOMERIZATION),
                    List.of("oxime", "acid or activating reagent", "anti migrating group"),
                    List.of("fragmentation", "wrong E/Z oxime", "over-acidic medium")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PINACOL_REARRANGEMENT,
                    "Pinacol rearrangement",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.BOND_CLEAVAGE,
                        MechanisticEventKind.PINACOL_SHIFT,
                        MechanisticEventKind.DEPROTONATION),
                    List.of(
                        "vicinal diol", "acid", "migratory aptitude", "carbocation stabilization"),
                    List.of("dehydration side product", "fragmentation", "poor migration control")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CLAISEN_REARRANGEMENT,
                    "Claisen rearrangement",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.CLAISEN_SHIFT,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of(
                        "allyl vinyl ether or equivalent",
                        "thermal activation",
                        "chair-like transition state"),
                    List.of("wrong conformer", "competing Cope shift", "thermal degradation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.COPE_REARRANGEMENT,
                    "Cope rearrangement",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.COPE_SHIFT,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of(
                        "1,5-diene", "allowed chair/boat transition state", "thermal activation"),
                    List.of("degenerate mixture", "competing fragmentation", "locked geometry")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ENZYME_LIKE,
                    "Enzyme-like stepwise catalysis",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "binding pocket model",
                        "acid/base residues",
                        "transition-state stabilization"),
                    List.of("missing environment", "wrong protonation microstate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.MANNICH_REACTION,
                    "Mannich reaction",
                    List.of(
                        MechanisticEventKind.IMINIUM_FORMATION,
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("aldehyde", "amine", "enolizable carbonyl", "acid/base balance"),
                    List.of("imine hydrolysis", "self-condensation", "over-alkylation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.STRECKER_SYNTHESIS,
                    "Strecker synthesis",
                    List.of(
                        MechanisticEventKind.IMINIUM_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.HYDROLYSIS,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "aldehyde or ketone",
                        "amine",
                        "cyanide source",
                        "controlled aqueous workup"),
                    List.of("cyanide hazard", "imine reversibility", "racemic product")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.UGI_REACTION,
                    "Ugi four-component reaction",
                    List.of(
                        MechanisticEventKind.IMINIUM_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.ACYL_TRANSFER,
                        MechanisticEventKind.REARRANGEMENT_MIGRATION),
                    List.of("carbonyl", "amine", "isocyanide", "carboxylic acid"),
                    List.of("component mismatch", "side Passerini path", "poor stereocontrol")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PASSERINI_REACTION,
                    "Passerini three-component reaction",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.ACYL_TRANSFER,
                        MechanisticEventKind.REARRANGEMENT_MIGRATION),
                    List.of("carbonyl", "isocyanide", "carboxylic acid", "aprotic medium"),
                    List.of(
                        "water sensitivity", "Ugi competition", "component stoichiometry drift")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.WITTIG_REACTION,
                    "Wittig olefination",
                    List.of(
                        MechanisticEventKind.YLIDE_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.OLEFIN_FORMATION,
                        MechanisticEventKind.BOND_CLEAVAGE),
                    List.of("aldehyde or ketone", "phosphonium ylide", "base", "E/Z control"),
                    List.of("enolizable carbonyl", "salt removal", "poor alkene selectivity")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HORNER_WADSWORTH_EMMONS,
                    "Horner-Wadsworth-Emmons olefination",
                    List.of(
                        MechanisticEventKind.YLIDE_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.OLEFIN_FORMATION,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE),
                    List.of("phosphonate", "base", "carbonyl", "E-selective context"),
                    List.of(
                        "base-sensitive substrate",
                        "ketone low reactivity",
                        "phosphate salt handling")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.MITSUNOBU_REACTION,
                    "Mitsunobu substitution",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("alcohol", "acidic nucleophile", "phosphine", "azodicarboxylate"),
                    List.of(
                        "poor nucleophile acidity",
                        "steric inversion failure",
                        "redox-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.SWERN_OXIDATION,
                    "Swern oxidation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.ELIMINATION,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "alcohol",
                        "DMSO activation",
                        "oxalyl chloride or equivalent",
                        "low temperature"),
                    List.of("odor and gas evolution", "overheating", "acid-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.DESS_MARTIN_OXIDATION,
                    "Dess-Martin oxidation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.ELIMINATION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of("alcohol", "hypervalent iodine reagent", "mild neutral conditions"),
                    List.of("oxidant incompatibility", "iodinane decomposition", "overoxidation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.DIHYDROXYLATION,
                    "Alkene dihydroxylation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.HYDROLYSIS,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("alkene", "osmium or permanganate oxidant", "syn addition target"),
                    List.of("oxidative cleavage", "overoxidation", "toxic metal oxidant")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.SHARPLESS_EPOXIDATION,
                    "Sharpless epoxidation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.ENANTIOSELECTIVE_CATALYST_BINDING,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.EPOXIDE_FORMATION),
                    List.of(
                        "allylic alcohol",
                        "titanium tartrate catalyst",
                        "peroxide",
                        "enantioselective oxygen transfer"),
                    List.of(
                        "wrong alcohol geometry", "water sensitivity", "poor enantioinduction")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ROBINSON_ANNULATION,
                    "Robinson annulation",
                    List.of(
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                        MechanisticEventKind.CONDENSATION,
                        MechanisticEventKind.RING_CLOSURE),
                    List.of("enolizable ketone", "Michael acceptor", "base", "annulation geometry"),
                    List.of("self-condensation", "polymerization", "regioisomer mixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PAUSON_KHAND_REACTION,
                    "Pauson-Khand reaction",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.MIGRATORY_INSERTION,
                        MechanisticEventKind.CYCLOADDITION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of("alkyne", "alkene", "carbon monoxide", "cobalt or metal catalyst"),
                    List.of("CO handling", "regioselectivity", "metal complex stability")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.NAZAROV_CYCLIZATION,
                    "Nazarov cyclization",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.ELECTROCYCLIC_STEP,
                        MechanisticEventKind.RING_CLOSURE,
                        MechanisticEventKind.DEPROTONATION),
                    List.of(
                        "divinyl ketone",
                        "Lewis or Bronsted acid",
                        "conrotatory electrocyclization"),
                    List.of("polymerization", "wrong cation stability", "competing rearrangement")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.FISCHER_INDOLE_SYNTHESIS,
                    "Fischer indole synthesis",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.HYDRAZONE_FORMATION,
                            "Aryl hydrazone formation",
                            "Aryl hydrazine condenses with aldehyde or ketone to form the productive hydrazone isomer.",
                            0.20,
                            List.of("aryl hydrazine", "aldehyde", "ketone", "acid"),
                            List.of("wrong hydrazone isomer", "water excess")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.PROTONATION,
                            "Acid activation of hydrazone",
                            "Protonation activates the hydrazone for the rearrangement manifold.",
                            0.14,
                            List.of("acid", "hydrazone", "protonation"),
                            List.of("acid-sensitive substituent", "overprotonation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.REARRANGEMENT_MIGRATION,
                            "Fischer rearrangement and N-N bond weakening",
                            "The aryl group migrates through the ene-hydrazine manifold while the N-N bond is prepared for cleavage.",
                            0.24,
                            List.of("aryl migration", "rearrangement", "ene-hydrazine"),
                            List.of("poor migratory aptitude", "fragmentation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RING_CLOSURE,
                            "Indole ring closure",
                            "Intramolecular C-C bond formation closes the five-membered indole ring.",
                            0.22,
                            List.of("indole target", "ring closure", "intramolecular attack"),
                            List.of("wrong regioisomer", "steric blockage")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Aromatization and ammonia release balance",
                            "Proton transfers restore aromaticity and complete product tautomer/salt state.",
                            0.20,
                            List.of("aromatization", "indole", "workup"),
                            List.of("tautomer mixture", "product protonation"))),
                    List.of("aryl hydrazine", "aldehyde or ketone", "acid", "indole target"),
                    List.of(
                        "wrong hydrazone isomer",
                        "acid-sensitive substituent",
                        "regioisomer mixture")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.SKRAUP_SYNTHESIS,
                    "Skraup quinoline synthesis",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CONDENSATION,
                            "Acrolein/enal generation and aniline condensation",
                            "Glycerol or enal source forms the alpha,beta-unsaturated electrophile that condenses with aniline.",
                            0.22,
                            List.of("aniline", "glycerol", "enal source", "acid"),
                            List.of("exotherm", "polymerization")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                            "Conjugate addition into activated enal",
                            "Aniline-derived nucleophile adds across the unsaturated aldehyde manifold.",
                            0.20,
                            List.of("conjugate addition", "enamine", "acidic medium"),
                            List.of("tar formation", "overalkylation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.RING_CLOSURE,
                            "Quinoline ring closure",
                            "Intramolecular electrophilic substitution closes the fused heteroaromatic ring system.",
                            0.24,
                            List.of("quinoline target", "ring closure", "fused heterocycle"),
                            List.of("wrong ortho site", "deactivated aniline")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.OXIDATIVE_WORKUP,
                            "Dihydroquinoline oxidation",
                            "Oxidant converts the reduced cyclized intermediate into aromatic quinoline.",
                            0.24,
                            List.of("oxidant", "aromatization", "quinoline"),
                            List.of("overoxidation", "oxidant-sensitive substituent")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.PROTON_TRANSFER,
                            "Salt neutralization and product isolation",
                            "Acid/base balance and workup control the quinoline free base or salt form.",
                            0.10,
                            List.of("workup", "quinoline salt", "neutralization"),
                            List.of("emulsion", "acid salt trapped"))),
                    List.of("aniline", "glycerol or enal source", "acid", "oxidant"),
                    List.of("exotherm", "polymerization", "overoxidation")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.COMBES_QUINOLINE_ANNULATION,
                    "Combes quinoline annulation",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CONDENSATION,
                            "Aniline beta-dicarbonyl condensation",
                            "An aniline-like nucleophile condenses with a beta-dicarbonyl or beta-diketone electrophile.",
                            0.20,
                            List.of("aniline", "beta-dicarbonyl", "beta-diketone", "imine/enamine"),
                            List.of(
                                "weak aniline nucleophilicity", "competitive carbonyl hydration")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.TAUTOMERIZATION,
                            "Iminium/enamine equilibration",
                            "Proton transfers and tautomerization set the electrophilic geometry for annulation.",
                            0.16,
                            List.of("acid", "iminium", "enamine", "tautomer control"),
                            List.of("wrong tautomer population", "basic quench")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.ELECTROPHILIC_ATTACK,
                            "Rate-limiting aryl annulation",
                            "The aryl ring closes onto the activated carbonyl-derived manifold; steric and electronic effects choose regioisomer channel.",
                            0.28,
                            List.of(
                                "electrophilic annulation",
                                "ortho aryl site",
                                "steric/electronic regioselectivity"),
                            List.of(
                                "blocked ortho site", "deactivated arene", "regioisomer mixture")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RING_CLOSURE,
                            "Quinoline ring closure",
                            "C-C bond formation creates the fused quinoline skeleton before final dehydration/aromatization.",
                            0.20,
                            List.of("quinoline", "fused heterocycle", "ring closure"),
                            List.of("open-chain imine residue", "polymerization")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.ELIMINATION,
                            "Dehydration and aromatization",
                            "Acidic dehydrating medium removes water and restores the heteroaromatic quinoline pi-system.",
                            0.16,
                            List.of("dehydrating agent", "polyphosphoric acid", "aromatization"),
                            List.of("incomplete dehydration", "acid-sensitive substituent"))),
                    List.of(
                        "aniline", "beta-dicarbonyl or beta-diketone", "acid", "dehydrating agent"),
                    List.of(
                        "ortho-site blockage", "regioisomer mixture", "acid-sensitive substrate")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.HANTZSCH_SYNTHESIS,
                    "Hantzsch heterocycle synthesis",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.CONDENSATION,
                            "Iminium/enamine component assembly",
                            "Aldehyde, ammonia or amine and beta-ketoester components enter the multicomponent condensation network.",
                            0.20,
                            List.of("beta-ketoester", "aldehyde", "ammonia", "amine"),
                            List.of("component ratio drift", "water excess")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ENAMINE_FORMATION,
                            "Enamine donor formation",
                            "One carbonyl component forms the enamine nucleophile that attacks the iminium/electrophilic partner.",
                            0.20,
                            List.of("enamine", "beta-ketoester", "ammonia"),
                            List.of("wrong tautomer", "self-condensation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "C-C bond construction",
                            "The enamine donor adds to the activated iminium or carbonyl-derived acceptor.",
                            0.22,
                            List.of("C-C bond", "iminium", "nucleophilic attack"),
                            List.of("side condensation", "steric mismatch")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RING_CLOSURE,
                            "Dihydropyridine ring closure",
                            "Intramolecular condensation closes the six-membered nitrogen heterocycle.",
                            0.22,
                            List.of("dihydropyridine", "ring closure", "heterocycle"),
                            List.of("oligomerization", "wrong stoichiometry")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.TAUTOMERIZATION,
                            "Tautomer and oxidation-state settling",
                            "The product settles into dihydropyridine or oxidized pyridine-like state depending on conditions.",
                            0.16,
                            List.of("tautomerization", "dihydropyridine", "oxidation state"),
                            List.of("oxidation state mismatch", "tautomeric mixture"))),
                    List.of(
                        "beta-ketoester",
                        "aldehyde",
                        "ammonia or amine",
                        "multicomponent condensation"),
                    List.of(
                        "component ratio drift",
                        "oxidation state mismatch",
                        "regioisomer mixture")),
                BuiltInReactionMechanismPatternCatalog.detailedPattern(
                    ReactionMechanismFamily.BIGINELLI_REACTION,
                    "Biginelli reaction",
                    List.of(
                        BuiltInReactionMechanismPatternCatalog.step(
                            0,
                            MechanisticEventKind.IMINIUM_FORMATION,
                            "Aldehyde-urea iminium activation",
                            "Aldehyde and urea/thiourea form an iminium-like electrophile under acid catalysis.",
                            0.22,
                            List.of("aldehyde", "urea", "thiourea", "acid catalysis"),
                            List.of("low conversion", "water excess")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            1,
                            MechanisticEventKind.ENAMINE_FORMATION,
                            "Beta-ketoester enol/enamine equivalent formation",
                            "The beta-ketoester component forms the nucleophilic tautomer needed for C-C bond construction.",
                            0.18,
                            List.of("beta-ketoester", "enol", "acid catalysis"),
                            List.of("wrong tautomer", "self-condensation")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            2,
                            MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                            "C-C bond formation onto iminium partner",
                            "The beta-ketoester nucleophile adds to the iminium center and forms the open-chain ureide intermediate.",
                            0.24,
                            List.of("nucleophilic attack", "iminium", "C-C bond"),
                            List.of("side condensation", "steric congestion")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            3,
                            MechanisticEventKind.RING_CLOSURE,
                            "Dihydropyrimidinone ring closure",
                            "Intramolecular urea nitrogen attack and condensation close the heterocyclic core.",
                            0.24,
                            List.of("dihydropyrimidinone", "ring closure", "heterocycle"),
                            List.of("wrong N/O cyclization", "component incompatibility")),
                        BuiltInReactionMechanismPatternCatalog.step(
                            4,
                            MechanisticEventKind.CONDENSATION,
                            "Dehydration and product tautomer settling",
                            "Water is expelled and the dihydropyrimidinone/thione tautomer distribution is resolved.",
                            0.12,
                            List.of("dehydration", "tautomer control", "product isolation"),
                            List.of("tautomeric mixture", "acid-sensitive substituent"))),
                    List.of("aldehyde", "urea/thiourea", "beta-ketoester", "acid catalysis"),
                    List.of("low conversion", "side condensation", "tautomeric mixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ACETAL_KETAL_FORMATION,
                    "Acetal or ketal formation",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                        MechanisticEventKind.CONDENSATION),
                    List.of(
                        "aldehyde/ketone", "alcohol or diol", "acid catalysis", "water removal"),
                    List.of(
                        "aqueous medium",
                        "acid-sensitive substrate",
                        "transacetalization mixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ACETAL_HYDROLYSIS,
                    "Acetal hydrolysis",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                        MechanisticEventKind.HYDROLYSIS),
                    List.of("acetal/ketal", "aqueous acid", "carbonyl regeneration"),
                    List.of("base-only conditions", "acid-labile protecting groups")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.EPOXIDE_OPENING,
                    "Epoxide ring opening",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.EPOXIDE_OPENING,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "epoxide",
                        "nucleophile",
                        "acid or base activation",
                        "regioselectivity control"),
                    List.of("competing rearrangement", "overalkylation", "wrong site selectivity")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HALOHYDRIN_FORMATION,
                    "Halohydrin formation",
                    List.of(
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "alkene",
                        "halogen electrophile",
                        "water/alcohol nucleophile",
                        "anti addition"),
                    List.of("radical halogenation", "polyhalogenation", "poor alkene control")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDROHALOGENATION,
                    "Hydrohalogenation",
                    List.of(
                        MechanisticEventKind.PROTONATION,
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("alkene/alkyne", "HX", "Markovnikov or radical anti-Markovnikov mode"),
                    List.of("rearrangement", "polyaddition", "peroxide ambiguity")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDRATION,
                    "Hydration of unsaturation",
                    List.of(
                        MechanisticEventKind.ELECTROPHILIC_ATTACK,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.TAUTOMERIZATION),
                    List.of(
                        "alkene/alkyne", "water", "acid or metal catalysis", "keto-enol control"),
                    List.of("polymerization", "carbocation rearrangement", "overhydration")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CLAISEN_CONDENSATION,
                    "Claisen condensation",
                    List.of(
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "ester",
                        "matching alkoxide base",
                        "alpha proton",
                        "beta-keto ester target"),
                    List.of("no alpha proton", "wrong alkoxide", "mixed ester scrambling")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.DIECKMANN_CONDENSATION,
                    "Dieckmann condensation",
                    List.of(
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.RING_CLOSURE,
                        MechanisticEventKind.LEAVING_GROUP_DEPARTURE),
                    List.of("diester", "intramolecular geometry", "five/six-membered ring target"),
                    List.of("strained ring", "intermolecular polymerization", "poor base match")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.KNOEVENAGEL_CONDENSATION,
                    "Knoevenagel condensation",
                    List.of(
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.CONDENSATION,
                        MechanisticEventKind.ELIMINATION),
                    List.of(
                        "aldehyde/ketone",
                        "active methylene",
                        "amine/base catalyst",
                        "water removal"),
                    List.of("self-condensation", "weak active methylene", "E/Z mixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HENRY_REACTION,
                    "Henry nitroaldol reaction",
                    List.of(
                        MechanisticEventKind.DEPROTONATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.CONDENSATION),
                    List.of("nitroalkane", "aldehyde/ketone", "base", "nitroaldol target"),
                    List.of("dehydration side path", "retro-Henry", "stereomixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CANNIZZARO_REACTION,
                    "Cannizzaro disproportionation",
                    List.of(
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.HYDRIDE_TRANSFER,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.ELECTRON_TRANSFER),
                    List.of("non-enolizable aldehyde", "strong base", "hydride transfer pair"),
                    List.of("alpha proton", "aldol competition", "mixed Cannizzaro mixture")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.BIRCH_REDUCTION,
                    "Birch reduction",
                    List.of(
                        MechanisticEventKind.ELECTRON_TRANSFER,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.ELECTRON_TRANSFER,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of(
                        "aromatic ring",
                        "alkali metal",
                        "ammonia/alcohol",
                        "substituent-controlled diene"),
                    List.of("overreduction", "metal incompatibility", "proton source mismatch")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.JONES_OXIDATION,
                    "Jones oxidation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.ELECTRON_TRANSFER,
                        MechanisticEventKind.HYDROLYSIS),
                    List.of(
                        "alcohol", "chromium(VI)", "aqueous acid", "carboxylic acid/ketone target"),
                    List.of("overoxidation", "acid sensitivity", "chromium waste")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.PCC_OXIDATION,
                    "PCC oxidation",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.OXYGEN_TRANSFER,
                        MechanisticEventKind.ELIMINATION,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of("alcohol", "pyridinium chlorochromate", "anhydrous mild oxidation"),
                    List.of("overoxidation if wet", "chromium waste", "allylic rearrangement")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDRIDE_REDUCTION,
                    "Hydride reduction",
                    List.of(
                        MechanisticEventKind.COMPLEXATION,
                        MechanisticEventKind.HYDRIDE_TRANSFER,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.HYDROLYSIS),
                    List.of(
                        "carbonyl/acyl derivative",
                        "NaBH4/LiAlH4/DIBAL",
                        "workup",
                        "chemoselectivity"),
                    List.of(
                        "overreduction", "water sensitivity", "functional group incompatibility")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CURTIUS_REARRANGEMENT,
                    "Curtius rearrangement",
                    List.of(
                        MechanisticEventKind.NITRENE_FORMATION,
                        MechanisticEventKind.REARRANGEMENT_MIGRATION,
                        MechanisticEventKind.NITROGEN_EXTRUSION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK),
                    List.of("acyl azide", "thermal activation", "isocyanate trapping"),
                    List.of(
                        "azide safety",
                        "water/alcohol trapping ambiguity",
                        "migration selectivity")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HOFMANN_REARRANGEMENT,
                    "Hofmann rearrangement",
                    List.of(
                        MechanisticEventKind.NITRENE_FORMATION,
                        MechanisticEventKind.REARRANGEMENT_MIGRATION,
                        MechanisticEventKind.NUCLEOPHILIC_ATTACK,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("primary amide", "hypohalite/base", "one-carbon loss amine target"),
                    List.of("N-halogenation side path", "sensitive substituent", "overoxidation")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.ARYNE_SUBSTITUTION,
                    "Aryne substitution",
                    List.of(
                        MechanisticEventKind.ARYNE_FORMATION,
                        MechanisticEventKind.ARYNE_ADDITION,
                        MechanisticEventKind.PROTON_TRANSFER,
                        MechanisticEventKind.RESONANCE_REORGANIZATION),
                    List.of(
                        "aryl halide/triflate",
                        "strong base",
                        "nucleophile",
                        "benzyne regioselectivity"),
                    List.of("isomer mixture", "base-sensitive substrate", "competing SNAr")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.CARBENE_CYCLOPROPANATION,
                    "Carbene cyclopropanation",
                    List.of(
                        MechanisticEventKind.CARBENE_FORMATION,
                        MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                        MechanisticEventKind.CARBENE_INSERTION,
                        MechanisticEventKind.BOND_FORMATION),
                    List.of("alkene", "diazo/haloform carbene source", "metal catalyst or base"),
                    List.of("C-H insertion", "dimerization", "stereocontrol risk")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.NITRENE_INSERTION,
                    "Nitrene insertion",
                    List.of(
                        MechanisticEventKind.NITRENE_FORMATION,
                        MechanisticEventKind.NITRENE_INSERTION,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.PROTON_TRANSFER),
                    List.of("azide/iminoiodinane", "C-H or alkene substrate", "metal catalyst"),
                    List.of("overinsertion", "aziridination competition", "nitrene safety")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.HYDROFORMYLATION,
                    "Hydroformylation",
                    List.of(
                        MechanisticEventKind.CATALYST_ASSOCIATION,
                        MechanisticEventKind.MIGRATORY_INSERTION,
                        MechanisticEventKind.BOND_FORMATION,
                        MechanisticEventKind.REDUCTIVE_ELIMINATION),
                    List.of(
                        "alkene", "CO/H2 syngas", "Rh/Co catalyst", "linear/branched selectivity"),
                    List.of("pressure handling", "isomerization", "hydrogenation side reaction")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.OLEFIN_METATHESIS,
                    "Olefin metathesis",
                    List.of(
                        MechanisticEventKind.CATALYST_ASSOCIATION,
                        MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                        MechanisticEventKind.BOND_CLEAVAGE,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "alkene",
                        "Ru/Mo carbene catalyst",
                        "ethylene removal or equilibrium control"),
                    List.of("catalyst poisoning", "E/Z mixture", "isomerization")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.RING_CLOSING_METATHESIS,
                    "Ring-closing metathesis",
                    List.of(
                        MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
                        MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
                        MechanisticEventKind.RING_CLOSURE,
                        MechanisticEventKind.CATALYST_DISSOCIATION),
                    List.of(
                        "diene",
                        "Ru/Mo carbene catalyst",
                        "ring-size feasible",
                        "dilution/equilibrium control"),
                    List.of("macrocycle dilution", "oligomerization", "catalyst poisoning")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.THIOL_ENE_CLICK,
                    "Thiol-ene click reaction",
                    List.of(
                        MechanisticEventKind.RADICAL_INITIATION,
                        MechanisticEventKind.RADICAL_ATTACK,
                        MechanisticEventKind.HYDROGEN_ATOM_TRANSFER,
                        MechanisticEventKind.RADICAL_PROPAGATION),
                    List.of(
                        "thiol", "alkene", "light/initiator", "anti-Markovnikov thioether target"),
                    List.of("oxygen inhibition", "disulfide formation", "polymerization")),
                BuiltInReactionMechanismPatternCatalog.pattern(
                    ReactionMechanismFamily.UNKNOWN,
                    "Unknown mechanism",
                    List.of(MechanisticEventKind.UNKNOWN),
                    List.of("insufficient evidence"),
                    List.of("manual review required")))));
  }

  private static List<ReactionMechanismPattern> completeCoverage(
      final List<ReactionMechanismPattern> explicitPatterns) {
    final ArrayList<ReactionMechanismPattern> patterns =
        new ArrayList<ReactionMechanismPattern>(
            explicitPatterns.size() + ReactionMechanismFamily.values().length);
    patterns.addAll(explicitPatterns);
    final ReactionMechanismFamily[] families = ReactionMechanismFamily.values();
    for (int i = 0; i < families.length; ++i) {
      if (BuiltInReactionMechanismPatternCatalog.containsFamily(patterns, families[i])) {
        continue;
      }
      patterns.add(BuiltInReactionMechanismPatternCatalog.genericPattern(families[i]));
    }
    return List.copyOf(patterns);
  }

  private static boolean containsFamily(
      final List<ReactionMechanismPattern> patterns, final ReactionMechanismFamily family) {
    for (int i = 0; i < patterns.size(); ++i) {
      if (patterns.get(i).family() == family) {
        return true;
      }
    }
    return false;
  }

  private static ReactionMechanismPattern genericPattern(final ReactionMechanismFamily family) {
    return BuiltInReactionMechanismPatternCatalog.pattern(
        family,
        BuiltInReactionMechanismPatternCatalog.displayName(family),
        BuiltInReactionMechanismPatternCatalog.genericEvents(family),
        BuiltInReactionMechanismPatternCatalog.genericApplicabilitySignals(family),
        BuiltInReactionMechanismPatternCatalog.genericRiskSignals(family));
  }

  private static String displayName(final ReactionMechanismFamily family) {
    final String raw = family.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
  }

  private static List<MechanisticEventKind> genericEvents(final ReactionMechanismFamily family) {
    if (family.name().contains("RADICAL")) {
      return List.of(
          MechanisticEventKind.RADICAL_INITIATION,
          MechanisticEventKind.RADICAL_ATTACK,
          MechanisticEventKind.RADICAL_PROPAGATION,
          MechanisticEventKind.RADICAL_TERMINATION);
    }
    if (family.name().contains("PHOTOCHEMICAL")) {
      return List.of(
          MechanisticEventKind.PHOTOCHEMICAL_EXCITATION,
          MechanisticEventKind.ELECTRON_TRANSFER,
          MechanisticEventKind.BOND_FORMATION,
          MechanisticEventKind.PHOTOCHEMICAL_QUENCHING);
    }
    if (family.name().contains("ELECTROCHEMICAL")) {
      return List.of(
          MechanisticEventKind.ELECTROCHEMICAL_OXIDATION,
          MechanisticEventKind.PROTON_TRANSFER,
          MechanisticEventKind.BOND_FORMATION,
          MechanisticEventKind.SOLVATION_REORGANIZATION);
    }
    if (family.name().contains("OXID") || family.name().contains("REDUCT")) {
      return List.of(
          MechanisticEventKind.COMPLEXATION,
          MechanisticEventKind.ELECTRON_TRANSFER,
          MechanisticEventKind.PROTON_TRANSFER);
    }
    if (family.name().contains("COUPLING")
        || family.name().contains("CATALYTIC")
        || family.name().contains("METATHESIS")) {
      return List.of(
          MechanisticEventKind.CATALYST_ASSOCIATION,
          MechanisticEventKind.BOND_FORMATION,
          MechanisticEventKind.CATALYST_DISSOCIATION);
    }
    if (family.name().contains("CYCLO")
        || family.name().contains("ANNULATION")
        || family.name().contains("HETEROCYCLE")) {
      return List.of(
          MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
          MechanisticEventKind.BOND_FORMATION,
          MechanisticEventKind.RING_CLOSURE);
    }
    if (family.name().contains("CARBONYL")
        || family.name().contains("IMINE")
        || family.name().contains("ENAMINE")
        || family.name().contains("ENOLATE")
        || family.name().contains("ALDOL")
        || family.name().contains("CONDENSATION")
        || family.name().contains("OLEFINATION")) {
      return List.of(
          MechanisticEventKind.NUCLEOPHILIC_ATTACK,
          MechanisticEventKind.CONDENSATION,
          MechanisticEventKind.PROTON_TRANSFER,
          MechanisticEventKind.ELIMINATION);
    }
    if (family.name().contains("PERICYCLIC")
        || family.name().contains("ELECTROCYCLIC")
        || family.name().contains("SIGMATROPIC")
        || family.name().contains("DIELS")) {
      return List.of(
          MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
          MechanisticEventKind.PERICYCLIC_CONCERTED,
          MechanisticEventKind.BOND_FORMATION,
          MechanisticEventKind.DIASTEREOSELECTIVE_APPROACH);
    }
    if (family.name().contains("TAUTOMER") || family.name().contains("ACID_BASE")) {
      return List.of(
          MechanisticEventKind.PROTON_TRANSFER,
          MechanisticEventKind.SOLVATION_REORGANIZATION,
          MechanisticEventKind.RESONANCE_REORGANIZATION);
    }
    if (family.name().contains("ELIMINATION")
        || family.name().contains("FRAGMENTATION")
        || family.name().contains("CLEAVAGE")) {
      return List.of(
          MechanisticEventKind.CONFORMATIONAL_REORGANIZATION,
          MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
          MechanisticEventKind.BOND_CLEAVAGE,
          MechanisticEventKind.PROTON_TRANSFER);
    }
    if (family.name().contains("PROTECTING")
        || family.name().contains("ACETAL")
        || family.name().contains("SOLVOLYSIS")
        || family.name().contains("HYDROLYSIS")) {
      return List.of(
          MechanisticEventKind.PROTON_TRANSFER,
          MechanisticEventKind.NUCLEOPHILIC_ATTACK,
          MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
          MechanisticEventKind.SOLVATION_REORGANIZATION);
    }
    if (family.name().contains("SUBSTITUTION")
        || family.name().contains("SANDMEYER")
        || family.name().contains("ARYNE")) {
      return List.of(
          MechanisticEventKind.NUCLEOPHILIC_ATTACK,
          MechanisticEventKind.LEAVING_GROUP_DEPARTURE,
          MechanisticEventKind.PROTON_TRANSFER);
    }
    if (family.name().contains("ADDITION")
        || family.name().contains("HYDRATION")
        || family.name().contains("HYDRO")) {
      return List.of(
          MechanisticEventKind.ADDITION_TO_MULTIPLE_BOND,
          MechanisticEventKind.BOND_FORMATION,
          MechanisticEventKind.PROTON_TRANSFER);
    }
    return List.of(
        MechanisticEventKind.COMPLEXATION,
        MechanisticEventKind.BOND_FORMATION,
        MechanisticEventKind.SOLVATION_REORGANIZATION);
  }

  private static List<String> genericApplicabilitySignals(final ReactionMechanismFamily family) {
    if (family.name().contains("RADICAL")) {
      return List.of(
          "radical precursor", "initiator/light/redox trigger", "radical trap or acceptor");
    }
    if (family.name().contains("PHOTOCHEMICAL")) {
      return List.of(
          "light source",
          "photosensitizer or absorbing substrate",
          "excited-state compatible partner");
    }
    if (family.name().contains("ELECTROCHEMICAL")) {
      return List.of(
          "electrode/redox window", "electrolyte or conductive medium", "proton/electron balance");
    }
    if (family.name().contains("OXID") || family.name().contains("REDUCT")) {
      return List.of(
          "redox-active substrate", "matched oxidant/reductant", "proton/electron balance");
    }
    if (family.name().contains("COUPLING")
        || family.name().contains("CATALYTIC")
        || family.name().contains("METATHESIS")) {
      return List.of(
          "catalyst present", "compatible ligand/metal cycle", "leaving group or unsaturation");
    }
    if (family.name().contains("CYCLO")
        || family.name().contains("ANNULATION")
        || family.name().contains("HETEROCYCLE")) {
      return List.of(
          "ring-forming geometry",
          "matched heteroatom/electrophile pair",
          "conformation supports closure");
    }
    if (family.name().contains("CARBONYL")
        || family.name().contains("IMINE")
        || family.name().contains("ENAMINE")
        || family.name().contains("ENOLATE")
        || family.name().contains("ALDOL")
        || family.name().contains("CONDENSATION")
        || family.name().contains("OLEFINATION")) {
      return List.of(
          "carbonyl/imine/enolate handle",
          "nucleophile or ylide partner",
          "acid/base dehydration balance");
    }
    if (family.name().contains("PERICYCLIC")
        || family.name().contains("ELECTROCYCLIC")
        || family.name().contains("SIGMATROPIC")
        || family.name().contains("DIELS")) {
      return List.of(
          "orbital symmetry compatible partner",
          "s-cis/conformation available",
          "stereochemical course retained");
    }
    if (family.name().contains("TAUTOMER") || family.name().contains("ACID_BASE")) {
      return List.of(
          "acid/base partner", "proton shuttle or solvent", "charge-stabilized tautomer/protomer");
    }
    if (family.name().contains("ELIMINATION")
        || family.name().contains("FRAGMENTATION")
        || family.name().contains("CLEAVAGE")) {
      return List.of(
          "leaving group or weak bond",
          "base/thermal/light activation",
          "anti/syn geometry or fragmentation relief");
    }
    if (family.name().contains("PROTECTING")
        || family.name().contains("ACETAL")
        || family.name().contains("SOLVOLYSIS")
        || family.name().contains("HYDROLYSIS")) {
      return List.of(
          "acid/base or solvent activation",
          "labile protecting/acetal/ester handle",
          "water/alcohol/nucleophile partner");
    }
    if (family.name().contains("SUBSTITUTION")
        || family.name().contains("SANDMEYER")
        || family.name().contains("ARYNE")) {
      return List.of(
          "leaving group present",
          "nucleophile or trapping partner present",
          "activation supports displacement");
    }
    if (family.name().contains("ADDITION")
        || family.name().contains("HYDRATION")
        || family.name().contains("HYDRO")) {
      return List.of(
          "multiple bond or electrophilic center",
          "addition partner present",
          "proton/terminating source available");
    }
    return List.of(
        "matched reactive functional groups",
        "conditions support the family",
        "electronic and steric descriptors do not block the route");
  }

  private static List<String> genericRiskSignals(final ReactionMechanismFamily family) {
    if (family.name().contains("RADICAL")) {
      return List.of(
          "oxygen inhibition", "radical dimerization", "uncontrolled chain side reaction");
    }
    if (family.name().contains("PHOTOCHEMICAL")) {
      return List.of("over-irradiation", "photodecomposition", "triplet/singlet mismatch");
    }
    if (family.name().contains("ELECTROCHEMICAL")) {
      return List.of(
          "overpotential side reaction",
          "electrode passivation",
          "competing proton reduction/oxidation");
    }
    if (family.name().contains("OXID") || family.name().contains("REDUCT")) {
      return List.of(
          "over-redox side channel", "incompatible proton activity", "air/moisture sensitivity");
    }
    if (family.name().contains("COUPLING")
        || family.name().contains("CATALYTIC")
        || family.name().contains("METATHESIS")) {
      return List.of(
          "catalyst poisoning",
          "beta-hydride or protodefunctionalization",
          "competitive homocoupling");
    }
    if (family.name().contains("CYCLO")
        || family.name().contains("ANNULATION")
        || family.name().contains("HETEROCYCLE")) {
      return List.of(
          "wrong regioisomer",
          "oligomerization or polymerization",
          "ring strain or conformer mismatch");
    }
    if (family.name().contains("CARBONYL")
        || family.name().contains("IMINE")
        || family.name().contains("ENAMINE")
        || family.name().contains("ENOLATE")
        || family.name().contains("ALDOL")
        || family.name().contains("CONDENSATION")
        || family.name().contains("OLEFINATION")) {
      return List.of(
          "self-condensation", "E/Z or tautomer mixture", "reversible dehydration/addition");
    }
    if (family.name().contains("PERICYCLIC")
        || family.name().contains("ELECTROCYCLIC")
        || family.name().contains("SIGMATROPIC")
        || family.name().contains("DIELS")) {
      return List.of(
          "wrong stereochemical mode",
          "competing regioisomer",
          "thermal/photochemical mode mismatch");
    }
    if (family.name().contains("TAUTOMER") || family.name().contains("ACID_BASE")) {
      return List.of(
          "salt formation only", "multiple protomer distribution", "medium-dependent reversal");
    }
    if (family.name().contains("ELIMINATION")
        || family.name().contains("FRAGMENTATION")
        || family.name().contains("CLEAVAGE")) {
      return List.of(
          "over-fragmentation", "substitution competition", "wrong alkene/isomer distribution");
    }
    if (family.name().contains("PROTECTING")
        || family.name().contains("ACETAL")
        || family.name().contains("SOLVOLYSIS")
        || family.name().contains("HYDROLYSIS")) {
      return List.of(
          "over-deprotection", "hydrolysis of sensitive groups", "solvent capture side product");
    }
    if (family.name().contains("SUBSTITUTION")
        || family.name().contains("SANDMEYER")
        || family.name().contains("ARYNE")) {
      return List.of(
          "elimination or hydrolysis",
          "poor leaving group",
          "competing regioisomeric substitution");
    }
    if (family.name().contains("ADDITION")
        || family.name().contains("HYDRATION")
        || family.name().contains("HYDRO")) {
      return List.of(
          "over-addition", "wrong regioselectivity", "reversible addition or protonation trap");
    }
    return List.of(
        "competing pathway with similar driving force",
        "missing condition specificity",
        "requires electronic rerank for ranked decisions");
  }

  private static ReactionMechanismPattern pattern(
      final ReactionMechanismFamily family,
      final String displayName,
      final List<MechanisticEventKind> events,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    return ReactionMechanismPattern.ofDetailed(
        family,
        displayName,
        BuiltInReactionMechanismPatternCatalog.eventAwareSteps(
            displayName, events, applicabilitySignals, riskSignals),
        applicabilitySignals,
        riskSignals);
  }

  private static List<ReactionMechanismPatternStep> eventAwareSteps(
      final String displayName,
      final List<MechanisticEventKind> events,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    final List<MechanisticEventKind> expandedEvents =
        BuiltInReactionMechanismPatternCatalog.productionDetailedEvents(events);
    final ReactionMechanismPatternStep[] steps =
        new ReactionMechanismPatternStep[expandedEvents.size()];
    final double contribution = 1.0 / expandedEvents.size();
    for (int i = 0; i < expandedEvents.size(); ++i) {
      final MechanisticEventKind event = expandedEvents.get(i);
      steps[i] =
          ReactionMechanismPatternStep.of(
              i,
              event,
              BuiltInReactionMechanismPatternCatalog.stageName(event),
              BuiltInReactionMechanismPatternCatalog.stageDescription(displayName, event),
              contribution,
              applicabilitySignals,
              riskSignals);
    }
    return List.of(steps);
  }

  private static List<MechanisticEventKind> productionDetailedEvents(
      final List<MechanisticEventKind> events) {
    if (events.size() >= 4) {
      return List.copyOf(events);
    }
    final ArrayList<MechanisticEventKind> expanded = new ArrayList<MechanisticEventKind>(4);
    expanded.addAll(events);
    while (expanded.size() < 4) {
      expanded.add(BuiltInReactionMechanismPatternCatalog.terminalEvent(expanded));
    }
    return List.copyOf(expanded);
  }

  private static MechanisticEventKind terminalEvent(final List<MechanisticEventKind> events) {
    if (events.contains(MechanisticEventKind.CATALYST_ASSOCIATION)
        && !events.contains(MechanisticEventKind.CATALYST_DISSOCIATION)) {
      return MechanisticEventKind.CATALYST_DISSOCIATION;
    }
    if (!events.contains(MechanisticEventKind.SOLVATION_REORGANIZATION)) {
      return MechanisticEventKind.SOLVATION_REORGANIZATION;
    }
    if (!events.contains(MechanisticEventKind.CONFORMATIONAL_REORGANIZATION)) {
      return MechanisticEventKind.CONFORMATIONAL_REORGANIZATION;
    }
    return MechanisticEventKind.COMPLEXATION;
  }

  private static String stageName(final MechanisticEventKind event) {
    final String raw = event.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
  }

  private static String stageDescription(
      final String displayName, final MechanisticEventKind event) {
    return BuiltInReactionMechanismPatternCatalog.stageName(event)
        + " stage participates in "
        + displayName
        + " and is evaluated against applicability, risk, condition and electronic descriptors.";
  }

  private static ReactionMechanismPattern detailedPattern(
      final ReactionMechanismFamily family,
      final String displayName,
      final List<ReactionMechanismPatternStep> steps,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    return ReactionMechanismPattern.ofDetailed(
        family, displayName, steps, applicabilitySignals, riskSignals);
  }

  private static ReactionMechanismPatternStep step(
      final int orderIndex,
      final MechanisticEventKind eventKind,
      final String stageName,
      final String description,
      final double probabilityContribution,
      final List<String> applicabilitySignals,
      final List<String> riskSignals) {
    return ReactionMechanismPatternStep.of(
        orderIndex,
        eventKind,
        stageName,
        description,
        probabilityContribution,
        applicabilitySignals,
        riskSignals);
  }
}
