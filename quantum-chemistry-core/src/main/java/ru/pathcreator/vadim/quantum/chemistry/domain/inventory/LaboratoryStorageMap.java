/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.inventory;

import java.time.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;

/** Storage topology and append-only container metadata. Quantities remain in inventory/sample ledgers. */
public record LaboratoryStorageMap(List<Place> places, List<Event> events) {
  public static final LaboratoryStorageMap EMPTY = new LaboratoryStorageMap(List.of(), List.of());

  public enum Kind {
    CABINET("Шкаф / холодильник"),
    SHELF("Полка"),
    BOX("Коробка / штатив");
    private final String label;

    Kind(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum ItemKind {
    BATCH("Партия"),
    SAMPLE("Образец");
    private final String label;

    ItemKind(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public enum Action {
    MOVE("Перемещение партии"),
    OPENED("Дата вскрытия"),
    EXPIRY("Срок годности"),
    NOTE("Примечание");
    private final String label;

    Action(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public record Place(String id, String parentId, Kind kind, String label, int rows, int columns) {
    public Place {
      id = identifier(id);
      parentId = parentId == null ? null : identifier(parentId);
      Objects.requireNonNull(kind);
      label = text(label, 100, true);
      if (label.contains("/") || label.contains("\n") || label.contains("\r")) {
        throw new IllegalArgumentException(
            "Название места не должно содержать / или перевод строки.");
      }
      if (kind == Kind.BOX
          ? rows < 1 || columns < 1 || rows > 26 || columns > 24 || rows * columns > 400
          : rows != 0 || columns != 0) {
        throw new IllegalArgumentException(
            "Для коробки задайте до 400 мест (до 26 рядов и 24 столбцов); для шкафа/полки размеры не нужны.");
      }
    }
  }

  public record Slot(String placeId, int row, int column) {
    public Slot {
      placeId = identifier(placeId);
      if (row < 0 || column < 0 || (row == 0) != (column == 0)) {
        throw new IllegalArgumentException("Неверная позиция хранения.");
      }
    }
  }

  public record Item(ItemKind kind, String id) {
    public Item {
      Objects.requireNonNull(kind);
      id = text(id, 700, true);
    }
  }

  public record Event(
      String id, Item item, Instant time, Action action, Slot slot, LocalDate date, String note) {
    public Event {
      id = identifier(id);
      Objects.requireNonNull(item);
      Objects.requireNonNull(time);
      Objects.requireNonNull(action);
      note = text(note, 4000, true);
      if (action == Action.MOVE && item.kind() != ItemKind.BATCH) {
        throw new IllegalArgumentException(
            "Место образца меняется событием DETAILS в исходном опыте.");
      }
      if (action != Action.MOVE && slot != null
          || action != Action.OPENED && action != Action.EXPIRY && date != null) {
        throw new IllegalArgumentException("Данные события не соответствуют операции хранения.");
      }
    }
  }

  public record State(Slot slot, LocalDate opened, LocalDate expiry, String note) {
    public boolean expired(final LocalDate today) {
      return this.expiry != null && this.expiry.isBefore(today);
    }

    public boolean soon(final LocalDate today, final int days) {
      return this.expiry != null
          && !this.expiry.isBefore(today)
          && !this.expiry.isAfter(today.plusDays(days));
    }
  }

  public LaboratoryStorageMap {
    places = List.copyOf(places);
    events = List.copyOf(events);
    if (places.size() > 2000 || events.size() > 50000) {
      throw new IllegalArgumentException(
          "Схема хранения ограничена 2000 местами и 50000 событиями.");
    }
    final Map<String, Place> known = new HashMap<>();
    final Set<String> labels = new HashSet<>();
    long characters = 0;
    for (final var place : places) {
      if (known.putIfAbsent(place.id(), place) != null
          || !labels.add(
              Objects.toString(place.parentId(), "")
                  + "/"
                  + place.label().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException(
            "Место с таким именем или идентификатором уже существует.");
      }
      final var parent = place.parentId() == null ? null : known.get(place.parentId());
      if (place.kind() == Kind.CABINET
          ? parent != null || place.parentId() != null
          : parent == null
              || place.kind() == Kind.SHELF && parent.kind() != Kind.CABINET
              || place.kind() == Kind.BOX && parent.kind() != Kind.SHELF) {
        throw new IllegalArgumentException("Иерархия хранения: шкаф → полка → коробка.");
      }
      characters += place.label().length();
    }
    final Set<String> ids = new HashSet<>();
    final Map<Item, Instant> times = new HashMap<>();
    for (final var event : events) {
      if (!ids.add(event.id())
          || times.containsKey(event.item()) && event.time().isBefore(times.get(event.item()))) {
        throw new IllegalArgumentException("Нарушена история хранения.");
      }
      times.put(event.item(), event.time());
      if (event.slot() != null) {
        validate(event.slot(), known);
      }
      characters += event.note().length();
    }
    if (characters > 8000000) {
      throw new IllegalArgumentException("История хранения превышает допустимый размер.");
    }
  }

  public LaboratoryStorageMap append(final Place place) {
    final var next = new ArrayList<>(this.places);
    next.add(place);
    return new LaboratoryStorageMap(next, this.events);
  }

  public LaboratoryStorageMap append(final Event event) {
    final var next = new ArrayList<>(this.events);
    next.add(event);
    return new LaboratoryStorageMap(this.places, next);
  }

  public State state(final Item item) {
    Slot slot = null;
    LocalDate opened = null, expiry = null;
    String note = "";
    for (final var event : this.events) {
      if (!event.item().equals(item)) {
        continue;
      }
      switch (event.action()) {
        case MOVE -> slot = event.slot();
        case OPENED -> opened = event.date();
        case EXPIRY -> expiry = event.date();
        case NOTE -> note = event.note();
      }
    }
    return new State(slot, opened, expiry, note);
  }

  public String path(final String id) {
    final var place =
        this.places.stream()
            .filter(p -> p.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Место не найдено."));
    return place.parentId() == null
        ? place.label()
        : this.path(place.parentId()) + " / " + place.label();
  }

  public String address(final Slot slot) {
    this.validate(slot);
    return this.path(slot.placeId())
        + (slot.row() == 0
            ? ""
            : " / "
                + (char) ('A' + slot.row() - 1)
                + String.format(Locale.ROOT, "%02d", slot.column()));
  }

  public Optional<Slot> resolve(final String address) {
    for (final var p : this.places) {
      final String path = this.path(p.id());
      if (p.kind() != Kind.BOX) {
        if (path.equals(address)) {
          return Optional.of(new Slot(p.id(), 0, 0));
        }
      } else {
        final String prefix = path + " / ";
        if (address.startsWith(prefix)) {
          final String cell = address.substring(prefix.length());
          if (cell.matches("[A-Z][0-9]{2}")) {
            final int row = cell.charAt(0) - 'A' + 1, col = Integer.parseInt(cell.substring(1));
            if (row <= p.rows() && col >= 1 && col <= p.columns()) {
              return Optional.of(new Slot(p.id(), row, col));
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  public void validate(final Slot slot) {
    final Map<String, Place> places = new HashMap<>();
    this.places.forEach(p -> places.put(p.id(), p));
    validate(slot, places);
  }

  private static void validate(final Slot slot, final Map<String, Place> places) {
    final var p = places.get(slot.placeId());
    if (p == null
        || (p.kind() == Kind.BOX
            ? slot.row() < 1
                || slot.row() > p.rows()
                || slot.column() < 1
                || slot.column() > p.columns()
            : slot.row() != 0 || slot.column() != 0)) {
      throw new IllegalArgumentException("Позиция отсутствует в схеме хранения.");
    }
  }

  public static void requireAppend(
      final LaboratoryStorageMap previous, final LaboratoryStorageMap next) {
    if (next.places.size() < previous.places.size()
        || !next.places.subList(0, previous.places.size()).equals(previous.places)
        || next.events.size() < previous.events.size()
        || !next.events.subList(0, previous.events.size()).equals(previous.events)) {
      throw new IllegalArgumentException(
          "Сохранённые места и события хранения неизменяемы; добавьте новое событие.");
    }
  }

  private static String identifier(final String value) {
    final var result = IdentifierValue.requireIdentifier(value, "Идентификатор места");
    if (result.length() > 160) {
      throw new IllegalArgumentException("Слишком длинный идентификатор.");
    }
    return result;
  }

  private static String text(final String value, final int maximum, final boolean required) {
    Objects.requireNonNull(value);
    if (value.length() > maximum || required && value.isBlank()) {
      throw new IllegalArgumentException("Заполните поле хранения в допустимой длине.");
    }
    return value.strip();
  }
}
