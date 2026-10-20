/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;

/** A physical specimen with a frozen assigned structure and an append-only quantity/location history. */
public record LaboratorySample(
    String id,
    String code,
    String componentId,
    String stageId,
    String stageTitle,
    Kind kind,
    Molecule molecule,
    List<String> parents,
    List<Event> events,
    LaboratorySampleOrigin origin) {
  public LaboratorySample(
      final String id,
      final String code,
      final String componentId,
      final String stageId,
      final String stageTitle,
      final Kind kind,
      final Molecule molecule,
      final List<String> parents,
      final List<Event> events) {
    this(id, code, componentId, stageId, stageTitle, kind, molecule, parents, events, null);
  }

  public enum Kind {
    CRUDE("Сырой продукт"),
    ISOLATED("Выделенный образец"),
    FRACTION("Фракция"),
    ALIQUOT("Аликвота"),
    POOLED("Объединённый образец"),
    TRANSFERRED("Из другого опыта");
    private final String label;

    Kind(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public enum Action {
    REGISTER("Регистрация"),
    MEASURE("Уточнение остатка"),
    CONSUME("Расход"),
    DETAILS("Место и этикетка"),
    FINISH("Переработан полностью"),
    DISCARD("Утилизирован"),
    ARCHIVE("Архив"),
    RESTORE("Возвращён в работу");
    private final String label;

    Action(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public enum Status {
    AVAILABLE("В работе"),
    EXHAUSTED("Израсходован"),
    PROCESSED("Переработан"),
    DISCARDED("Утилизирован"),
    ARCHIVED("В архиве");
    private final String label;

    Status(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public record Event(
      String id,
      Instant time,
      Action action,
      LaboratoryQuantity quantity,
      String label,
      String location,
      String container,
      String note,
      String relatedSampleId) {
    public Event {
      id = IdentifierValue.requireIdentifier(id, "Sample event id");
      Objects.requireNonNull(time);
      Objects.requireNonNull(action);
      label = text(label, 240);
      location = text(location, 500);
      container = text(container, 160);
      note = text(note, 8000);
      if (relatedSampleId != null) {
        IdentifierValue.requireIdentifier(relatedSampleId, "Related sample id");
      }
      if (quantity != null
          && (quantity.value().precision() > 40
              || Math.abs((long) quantity.value().scale()) > 24)) {
        throw new IllegalArgumentException("Количество образца вне поддержанной точности.");
      }
      if (action != Action.REGISTER && note.isBlank()) {
        throw new IllegalArgumentException("Укажите основание операции с образцом.");
      }
      if ((action == Action.REGISTER || action == Action.DETAILS) && label.isBlank()) {
        throw new IllegalArgumentException("Укажите подпись образца.");
      }
      if (action == Action.MEASURE && quantity == null
          || action == Action.CONSUME && (quantity == null || quantity.value().signum() <= 0)) {
        throw new IllegalArgumentException("Укажите количество для операции.");
      }
      if (action != Action.REGISTER
          && action != Action.MEASURE
          && action != Action.CONSUME
          && quantity != null) {
        throw new IllegalArgumentException("Эта операция не содержит количества.");
      }
      if (action != Action.REGISTER
          && action != Action.DETAILS
          && (!label.isEmpty() || !location.isEmpty() || !container.isEmpty())) {
        throw new IllegalArgumentException("Место и подпись меняются отдельной операцией.");
      }
      if (relatedSampleId != null && action != Action.CONSUME) {
        throw new IllegalArgumentException("Передача образцу относится только к расходу.");
      }
    }
  }

  public record Balance(
      LaboratoryQuantity remaining,
      Status status,
      String label,
      String location,
      String container) {}

  public LaboratorySample {
    id = IdentifierValue.requireIdentifier(id, "Sample id");
    componentId = IdentifierValue.requireIdentifier(componentId, "Sample component id");
    if (stageId != null) {
      IdentifierValue.requireIdentifier(stageId, "Sample stage id");
    }
    stageTitle = text(stageTitle, 500);
    code = text(code, 96);
    if (code.isBlank() || code.contains("\n") || code.contains("\r")) {
      throw new IllegalArgumentException("Код образца должен занимать одну непустую строку.");
    }
    Objects.requireNonNull(kind);
    Objects.requireNonNull(molecule);
    parents = List.copyOf(parents);
    events = List.copyOf(events);
    if ((kind == Kind.TRANSFERRED) != (origin != null) || origin != null && !parents.isEmpty()) {
      throw new IllegalArgumentException(
          "Переданный образец требует внешнего происхождения и не содержит местных родителей.");
    }
    if (parents.size() > 100
        || new HashSet<>(parents).size() != parents.size()
        || parents.contains(id)) {
      throw new IllegalArgumentException("Неверные исходные образцы.");
    }
    parents.forEach(p -> IdentifierValue.requireIdentifier(p, "Parent sample id"));
    if (kind == Kind.ALIQUOT && parents.size() != 1
        || kind == Kind.POOLED && parents.size() < 2
        || kind == Kind.FRACTION && parents.size() != 1) {
      throw new IllegalArgumentException(
          "Не задано происхождение аликвоты, фракции или объединённого образца.");
    }
    if (events.isEmpty()
        || events.size() > 10000
        || events.getFirst().action() != Action.REGISTER) {
      throw new IllegalArgumentException(
          "История образца начинается регистрацией; предел — 10000 событий.");
    }
    final Set<String> ids = new HashSet<>();
    Instant previous = null;
    for (int i = 0; i < events.size(); i++) {
      final Event e = events.get(i);
      if (!ids.add(e.id())
          || i > 0 && e.action() == Action.REGISTER
          || previous != null && e.time().isBefore(previous)) {
        throw new IllegalArgumentException("Нарушен порядок истории образца.");
      }
      previous = e.time();
    }
    evaluate(events);
  }

  public Balance balance() {
    return evaluate(this.events);
  }

  public Instant createdAt() {
    return this.events.getFirst().time();
  }

  public LaboratorySample append(final Event event) {
    final var values = new ArrayList<>(this.events);
    values.add(event);
    return new LaboratorySample(
        this.id,
        this.code,
        this.componentId,
        this.stageId,
        this.stageTitle,
        this.kind,
        this.molecule,
        this.parents,
        values,
        this.origin);
  }

  private static Balance evaluate(final List<Event> events) {
    LaboratoryQuantity amount = null;
    Status status = Status.AVAILABLE;
    String label = "", location = "", container = "";
    for (final Event event : events) {
      switch (event.action()) {
        case REGISTER -> {
          amount = event.quantity();
          label = event.label();
          location = event.location();
          container = event.container();
        }
        case DETAILS -> {
          label = event.label();
          location = event.location();
          container = event.container();
        }
        case MEASURE -> {
          if (status != Status.AVAILABLE && status != Status.EXHAUSTED) {
            throw new IllegalArgumentException("Сначала верните образец в работу.");
          }
          amount = event.quantity();
          status = Status.AVAILABLE;
        }
        case CONSUME -> {
          if (status != Status.AVAILABLE || amount == null) {
            throw new IllegalArgumentException(
                "Для расхода нужен доступный образец с известным остатком.");
          }
          final var taken = event.quantity().in(amount.unit());
          if (taken.value().compareTo(amount.value()) > 0) {
            throw new IllegalArgumentException("Расход больше доступного остатка.");
          }
          amount = new LaboratoryQuantity(amount.value().subtract(taken.value()), amount.unit());
        }
        case FINISH, DISCARD -> {
          if (status != Status.AVAILABLE && status != Status.EXHAUSTED) {
            throw new IllegalArgumentException("Образец уже выведен из работы.");
          }
          if (amount != null) {
            amount = new LaboratoryQuantity(java.math.BigDecimal.ZERO, amount.unit());
          }
          status = event.action() == Action.FINISH ? Status.PROCESSED : Status.DISCARDED;
        }
        case ARCHIVE -> {
          if (status != Status.AVAILABLE && status != Status.EXHAUSTED) {
            throw new IllegalArgumentException("Архивировать можно образец в работе.");
          }
          status = Status.ARCHIVED;
        }
        case RESTORE -> {
          if (status == Status.AVAILABLE || status == Status.EXHAUSTED) {
            throw new IllegalArgumentException("Образец уже находится в работе.");
          }
          status = Status.AVAILABLE;
        }
      }
      if (status == Status.AVAILABLE && amount != null && amount.value().signum() == 0) {
        status = Status.EXHAUSTED;
      }
    }
    return new Balance(amount, status, label, location, container);
  }

  private static String text(final String value, final int limit) {
    Objects.requireNonNull(value);
    if (value.length() > limit) {
      throw new IllegalArgumentException("Слишком длинное поле образца.");
    }
    return value.strip();
  }
}
