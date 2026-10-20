/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.electronic;

/** Режим подготовки электронной задачи и Hamiltonian для дальнейших CPU/Quantum IR workflow. */
public enum ElectronicStructurePreparationMode {
  /** Полный Hamiltonian с одноэлектронными и двухэлектронными интегралами. */
  FULL_TWO_ELECTRON,
  /** Hamiltonian подготовлен внешним расчетным backend без материализации Java AO/MO tensors. */
  BACKEND_HAMILTONIAN,
  /** Приближенный Hamiltonian только с одноэлектронной частью и явной причиной деградации. */
  ONE_ELECTRON_FALLBACK,
  /** Direct Hartree-Fock через потоковый Gaussian ERI provider без полной материализации двухэлектронного tensor. */
  DIRECT_HARTREE_FOCK,
  /** Parameterized valence Hamiltonian built from molecular graph without Gaussian basis. */
  SEMI_EMPIRICAL_VALENCE
}
