/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.classical.application.property;

import java.util.ArrayList;
import java.util.List;

/** Хранит разобранный stdout PySCF property backend без привязки к Python runtime. */
final class PyScfPropertyProcessResult {

  private static final String RESULT_PREFIX = "QUANTUM_PYSCF_PROPERTY ";

  private final boolean successful;
  private final boolean converged;
  private final double totalEnergy;
  private final double dipoleX;
  private final double dipoleY;
  private final double dipoleZ;
  private final double dipoleNorm;
  private final double homoEnergy;
  private final double lumoEnergy;
  private final double homoLumoGap;
  private final double verticalCationEnergy;
  private final double verticalAnionEnergy;
  private final double verticalIonizationPotential;
  private final double verticalIonizationPotentialEv;
  private final double verticalElectronAffinity;
  private final double verticalElectronAffinityEv;
  private final List<String> chargeRows;
  private final String summary;

  private PyScfPropertyProcessResult(
      final boolean successful,
      final boolean converged,
      final double totalEnergy,
      final double dipoleX,
      final double dipoleY,
      final double dipoleZ,
      final double dipoleNorm,
      final double homoEnergy,
      final double lumoEnergy,
      final double homoLumoGap,
      final double verticalCationEnergy,
      final double verticalAnionEnergy,
      final double verticalIonizationPotential,
      final double verticalIonizationPotentialEv,
      final double verticalElectronAffinity,
      final double verticalElectronAffinityEv,
      final List<String> chargeRows,
      final String summary) {
    this.successful = successful;
    this.converged = converged;
    this.totalEnergy = totalEnergy;
    this.dipoleX = dipoleX;
    this.dipoleY = dipoleY;
    this.dipoleZ = dipoleZ;
    this.dipoleNorm = dipoleNorm;
    this.homoEnergy = homoEnergy;
    this.lumoEnergy = lumoEnergy;
    this.homoLumoGap = homoLumoGap;
    this.verticalCationEnergy = verticalCationEnergy;
    this.verticalAnionEnergy = verticalAnionEnergy;
    this.verticalIonizationPotential = verticalIonizationPotential;
    this.verticalIonizationPotentialEv = verticalIonizationPotentialEv;
    this.verticalElectronAffinity = verticalElectronAffinity;
    this.verticalElectronAffinityEv = verticalElectronAffinityEv;
    this.chargeRows = List.copyOf(chargeRows);
    this.summary = summary;
  }

  static PyScfPropertyProcessResult parse(final int exitCode, final String output) {
    if (exitCode != 0) {
      return PyScfPropertyProcessResult.failed(
          "PySCF property process exited with " + exitCode + ": " + output);
    }
    boolean hasEnergy = false;
    boolean hasDipole = false;
    boolean hasFrontier = false;
    boolean hasVerticalIon = false;
    boolean converged = false;
    double totalEnergy = Double.NaN;
    double dipoleX = Double.NaN;
    double dipoleY = Double.NaN;
    double dipoleZ = Double.NaN;
    double dipoleNorm = Double.NaN;
    double homoEnergy = Double.NaN;
    double lumoEnergy = Double.NaN;
    double homoLumoGap = Double.NaN;
    double verticalCationEnergy = Double.NaN;
    double verticalAnionEnergy = Double.NaN;
    double verticalIonizationPotential = Double.NaN;
    double verticalIonizationPotentialEv = Double.NaN;
    double verticalElectronAffinity = Double.NaN;
    double verticalElectronAffinityEv = Double.NaN;
    final ArrayList<String> chargeRows = new ArrayList<String>();
    final String[] lines = output.split("\\R");
    for (int i = 0; i < lines.length; ++i) {
      final String line = lines[i];
      if (!line.startsWith(RESULT_PREFIX)) {
        continue;
      }
      final String payload = line.substring(RESULT_PREFIX.length()).trim();
      final String[] parts = payload.split("\\s+");
      if (parts.length >= 3 && parts[0].equals("ENERGY")) {
        totalEnergy = Double.parseDouble(parts[1]);
        converged = Boolean.parseBoolean(parts[2]);
        hasEnergy = true;
      } else if (parts.length >= 5 && parts[0].equals("DIPOLE")) {
        dipoleX = Double.parseDouble(parts[1]);
        dipoleY = Double.parseDouble(parts[2]);
        dipoleZ = Double.parseDouble(parts[3]);
        dipoleNorm = Double.parseDouble(parts[4]);
        hasDipole = true;
      } else if (parts.length >= 4 && parts[0].equals("FRONTIER")) {
        homoEnergy = Double.parseDouble(parts[1]);
        lumoEnergy = Double.parseDouble(parts[2]);
        homoLumoGap = Double.parseDouble(parts[3]);
        hasFrontier = true;
      } else if (parts.length >= 7 && parts[0].equals("VERTICAL_ION")) {
        verticalCationEnergy = Double.parseDouble(parts[1]);
        verticalAnionEnergy = Double.parseDouble(parts[2]);
        verticalIonizationPotential = Double.parseDouble(parts[3]);
        verticalIonizationPotentialEv = Double.parseDouble(parts[4]);
        verticalElectronAffinity = Double.parseDouble(parts[5]);
        verticalElectronAffinityEv = Double.parseDouble(parts[6]);
        hasVerticalIon = true;
      } else if (parts.length >= 5 && parts[0].equals("MULLIKEN")) {
        chargeRows.add(parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4]);
      }
    }
    if (!hasEnergy || !hasDipole || !hasFrontier || !hasVerticalIon || chargeRows.isEmpty()) {
      return PyScfPropertyProcessResult.failed(
          "PySCF property output did not contain complete result markers: " + output);
    }
    return new PyScfPropertyProcessResult(
        true,
        converged,
        totalEnergy,
        dipoleX,
        dipoleY,
        dipoleZ,
        dipoleNorm,
        homoEnergy,
        lumoEnergy,
        homoLumoGap,
        verticalCationEnergy,
        verticalAnionEnergy,
        verticalIonizationPotential,
        verticalIonizationPotentialEv,
        verticalElectronAffinity,
        verticalElectronAffinityEv,
        chargeRows,
        "ok");
  }

  static PyScfPropertyProcessResult failed(final String summary) {
    return new PyScfPropertyProcessResult(
        false,
        false,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        Double.NaN,
        List.of(),
        summary);
  }

  boolean successful() {
    return this.successful;
  }

  boolean converged() {
    return this.converged;
  }

  double totalEnergy() {
    return this.totalEnergy;
  }

  double dipoleX() {
    return this.dipoleX;
  }

  double dipoleY() {
    return this.dipoleY;
  }

  double dipoleZ() {
    return this.dipoleZ;
  }

  double dipoleNorm() {
    return this.dipoleNorm;
  }

  double homoEnergy() {
    return this.homoEnergy;
  }

  double lumoEnergy() {
    return this.lumoEnergy;
  }

  double homoLumoGap() {
    return this.homoLumoGap;
  }

  double verticalCationEnergy() {
    return this.verticalCationEnergy;
  }

  double verticalAnionEnergy() {
    return this.verticalAnionEnergy;
  }

  double verticalIonizationPotential() {
    return this.verticalIonizationPotential;
  }

  double verticalIonizationPotentialEv() {
    return this.verticalIonizationPotentialEv;
  }

  double verticalElectronAffinity() {
    return this.verticalElectronAffinity;
  }

  double verticalElectronAffinityEv() {
    return this.verticalElectronAffinityEv;
  }

  List<String> chargeRows() {
    return this.chargeRows;
  }

  String summary() {
    return this.summary;
  }
}
