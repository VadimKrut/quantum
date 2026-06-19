/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.reaction;

import java.util.List;
import java.util.Objects;

public final class ReactionSide {

  private final List<ReactionParticipant> participants;
  private final ReactionSideSignature signature;

  private ReactionSide(
      final List<ReactionParticipant> participants, final ReactionSideSignature signature) {
    this.participants = participants;
    this.signature = signature;
  }

  public static ReactionSide of(final List<ReactionParticipant> participants) {
    if (participants == null || participants.isEmpty()) {
      throw new IllegalArgumentException("Reaction side participants must not be empty.");
    }
    for (int i = 0; i < participants.size(); ++i) {
      if (participants.get(i) != null) continue;
      throw new IllegalArgumentException("Reaction participant must not be null.");
    }
    final List<ReactionParticipant> checkedParticipants = List.copyOf(participants);
    final ReactionSideSignature checkedSignature =
        ReactionSideSignature.fromParticipants(checkedParticipants);
    return new ReactionSide(checkedParticipants, checkedSignature);
  }

  public List<ReactionParticipant> participants() {
    return this.participants;
  }

  public ReactionSideSignature signature() {
    return signature;
  }

  public boolean sameParticipantsAs(final ReactionSide other) {
    return other != null && signature.sameParticipantsAs(other.signature);
  }

  public int participantCount() {
    return this.participants.size();
  }

  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ReactionSide)) {
      return false;
    }
    final ReactionSide side = (ReactionSide) other;
    return Objects.equals(this.participants, side.participants);
  }

  public int hashCode() {
    return this.participants.hashCode();
  }
}