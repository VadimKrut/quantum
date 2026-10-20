/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.workflow.domain.followup;

import java.util.List;
import java.util.Objects;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationOption;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationKind;
import ru.pathcreator.vadim.quantum.chemistry.classical.domain.calculation.ClassicalCalculationRequest;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.ChemistryHash;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/**
 * Исполняемый план последующих расчетов, полученный из workflow-рекомендаций.
 */
public final class ChemistryFollowUpCalculationPlan {

  public static final ChemistryFollowUpCalculationPlan EMPTY =
      new ChemistryFollowUpCalculationPlan("", List.of());
  private static final String OPTION_CANDIDATE_INDEX = "workflow_candidate_index";
  private static final String OPTION_RECOMMENDATION_PRIORITY = "workflow_recommendation_priority";
  private static final String OPTION_SUBJECT_SOURCE = "workflow_subject_source";

  private final String workflowJobId;
  private final List<ClassicalCalculationRequest> requests;

  private ChemistryFollowUpCalculationPlan(
      final String workflowJobId, final List<ClassicalCalculationRequest> requests) {
    this.workflowJobId = workflowJobId;
    this.requests = requests;
  }

  public static ChemistryFollowUpCalculationPlan of(
      final String workflowJobId, final List<ClassicalCalculationRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return EMPTY;
    }
    for (int i = 0; i < requests.size(); ++i) {
      if (requests.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException(
          "Chemistry follow-up calculation request must not be null.");
    }
    return new ChemistryFollowUpCalculationPlan(
        IdentifierValue.requireIdentifier(workflowJobId, "Chemistry follow-up workflow job id"),
        List.copyOf(requests));
  }

  public String workflowJobId() {
    return this.workflowJobId;
  }

  public List<ClassicalCalculationRequest> requests() {
    return this.requests;
  }

  public int requestCount() {
    return this.requests.size();
  }

  public boolean empty() {
    return this.requests.isEmpty();
  }

  public ChemistryFollowUpCalculationPlan limitedTo(final int maxRequestCount) {
    if (maxRequestCount < 0) {
      throw new IllegalArgumentException("Chemistry follow-up request limit must be non-negative.");
    }
    if (maxRequestCount == 0 || this.requests.isEmpty()) {
      return EMPTY;
    }
    if (this.requests.size() <= maxRequestCount) {
      return this;
    }
    return ChemistryFollowUpCalculationPlan.of(
        this.workflowJobId, this.requests.subList(0, maxRequestCount));
  }

  public List<ClassicalCalculationRequest> requestsForCandidate(final int candidateIndex) {
    if (candidateIndex < 0) {
      throw new IllegalArgumentException(
          "Chemistry follow-up candidate index must be non-negative.");
    }
    if (this.requests.isEmpty()) {
      return List.of();
    }
    final java.util.ArrayList<ClassicalCalculationRequest> result =
        new java.util.ArrayList<ClassicalCalculationRequest>();
    for (int i = 0; i < this.requests.size(); ++i) {
      final ClassicalCalculationRequest request = this.requests.get(i);
      if (ChemistryFollowUpCalculationPlan.optionEquals(
          request, OPTION_CANDIDATE_INDEX, Integer.toString(candidateIndex))) {
        result.add(request);
      }
    }
    return List.copyOf(result);
  }

  public List<ClassicalCalculationRequest> requestsByPriority(final String priority) {
    if (priority == null || priority.isBlank()) {
      throw new IllegalArgumentException("Chemistry follow-up priority must not be blank.");
    }
    if (this.requests.isEmpty()) {
      return List.of();
    }
    final java.util.ArrayList<ClassicalCalculationRequest> result =
        new java.util.ArrayList<ClassicalCalculationRequest>();
    for (int i = 0; i < this.requests.size(); ++i) {
      final ClassicalCalculationRequest request = this.requests.get(i);
      if (ChemistryFollowUpCalculationPlan.optionEquals(
          request, OPTION_RECOMMENDATION_PRIORITY, priority)) {
        result.add(request);
      }
    }
    return List.copyOf(result);
  }

  public ChemistryFollowUpCalculationPlan filteredByKinds(
      final List<ClassicalCalculationKind> calculationKinds) {
    if (calculationKinds == null || calculationKinds.isEmpty()) {
      throw new IllegalArgumentException(
          "Chemistry follow-up calculation kinds must not be empty.");
    }
    for (int i = 0; i < calculationKinds.size(); ++i) {
      if (calculationKinds.get(i) != null) {
        continue;
      }
      throw new IllegalArgumentException("Chemistry follow-up calculation kind must not be null.");
    }
    if (this.requests.isEmpty()) {
      return EMPTY;
    }
    final java.util.ArrayList<ClassicalCalculationRequest> result =
        new java.util.ArrayList<ClassicalCalculationRequest>();
    for (int i = 0; i < this.requests.size(); ++i) {
      final ClassicalCalculationRequest request = this.requests.get(i);
      if (ChemistryFollowUpCalculationPlan.hasAnyKind(request, calculationKinds)) {
        result.add(request);
      }
    }
    if (result.isEmpty()) {
      return EMPTY;
    }
    return ChemistryFollowUpCalculationPlan.of(this.workflowJobId, result);
  }

  public boolean hasKind(final ClassicalCalculationKind calculationKind) {
    if (calculationKind == null) {
      throw new IllegalArgumentException("Chemistry follow-up calculation kind must not be null.");
    }
    for (int i = 0; i < this.requests.size(); ++i) {
      if (this.requests.get(i).calculationKinds().contains(calculationKind)) {
        return true;
      }
    }
    return false;
  }

  public long candidateProductMoleculeRequestCount() {
    long count = 0L;
    for (int i = 0; i < this.requests.size(); ++i) {
      if (ChemistryFollowUpCalculationPlan.optionEquals(
          this.requests.get(i), OPTION_SUBJECT_SOURCE, "candidate_product_molecule")) {
        ++count;
      }
    }
    return count;
  }

  private static boolean optionEquals(
      final ClassicalCalculationRequest request, final String name, final String value) {
    final List<ClassicalCalculationOption> options = request.options();
    for (int i = 0; i < options.size(); ++i) {
      final ClassicalCalculationOption option = options.get(i);
      if (option.name().equals(name) && option.value().equals(value)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasAnyKind(
      final ClassicalCalculationRequest request,
      final List<ClassicalCalculationKind> calculationKinds) {
    final List<ClassicalCalculationKind> requestKinds = request.calculationKinds();
    for (int i = 0; i < calculationKinds.size(); ++i) {
      if (requestKinds.contains(calculationKinds.get(i))) {
        return true;
      }
    }
    return false;
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ChemistryFollowUpCalculationPlan)) {
      return false;
    }
    final ChemistryFollowUpCalculationPlan plan = (ChemistryFollowUpCalculationPlan) other;
    return Objects.equals(this.workflowJobId, plan.workflowJobId)
        && Objects.equals(this.requests, plan.requests);
  }

  public int hashCode() {
    int result = ChemistryHash.seed();
    result = ChemistryHash.include(result, this.workflowJobId);
    result = ChemistryHash.include(result, this.requests);
    return result;
  }
}
