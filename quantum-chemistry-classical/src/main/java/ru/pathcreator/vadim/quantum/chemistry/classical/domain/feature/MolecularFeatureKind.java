/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.domain.feature;

/** Тип структурного или функционального признака, извлеченного из молекулярного графа. */
public enum MolecularFeatureKind {
  HETERO_ATOM_COUNT,
  RING_COUNT,
  HETEROCYCLE,
  AROMATIC_RING,
  FIVE_MEMBERED_RING,
  SIX_MEMBERED_RING,
  NITROGEN_HETEROCYCLE,
  OXYGEN_HETEROCYCLE,
  SULFUR_HETEROCYCLE,
  CARBONYL,
  CARBOXYL_DERIVATIVE,
  CARBOXYLIC_ACID_OR_CARBOXYLATE,
  ESTER,
  AMIDE,
  HYDROXYL_OR_ALKOXY,
  ETHER,
  EPOXIDE,
  AMINE,
  IMINE,
  NITRILE,
  NITRO,
  ALKENE,
  ALKYNE,
  ARYL_HALIDE,
  HETEROARYL_HALIDE,
  ELECTRON_WITHDRAWING_GROUP,
  ELECTRON_POOR_HETEROARENE,
  PRIMARY_OR_SECONDARY_AMINE,
  TERTIARY_AMINE,
  BENZYL_OR_BENZYLIC_FRAGMENT,
  ACTIVATED_NUCLEOPHILE,
  THIOL_OR_THIOETHER,
  SULFUR_OXIDE,
  HALOGENATED,
  CHARGED_CENTER,
  RADICAL_CENTER,
  ALDEHYDE,
  KETONE,
  ACYL_HALIDE,
  ANHYDRIDE,
  ALCOHOL,
  PHENOL,
  BORONIC_ACID_OR_BORONATE,
  ORGANOMETALLIC_REAGENT,
  ISOCYANIDE,
  ISOCYANATE,
  ACTIVATED_ALKENE,
  ALPHA_CARBONYL_C_H,
  GOOD_LEAVING_GROUP;
}
