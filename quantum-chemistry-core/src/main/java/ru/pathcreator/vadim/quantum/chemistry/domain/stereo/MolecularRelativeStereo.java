/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.domain.stereo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.metadata.ChemistryMetadata;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryMaterialProperties;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.*;

/** Authored relative configurations and material ensemble claims; never inferred absolute CIP. */
public record MolecularRelativeStereo(String structureSignature, List<Group> groups) {
  public static final String PREFIX = "laboratory.relative-stereo.v1.";

  public enum Kind {
    RELATIVE("Относительная конфигурация"),
    RACEMIC("Рацемат · равная пара энантиомеров"),
    UNKNOWN_ABSOLUTE("Абсолютная конфигурация не установлена");
    private final String title;

    Kind(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public enum Descriptor {
    R_STAR("R*"),
    S_STAR("S*"),
    UNKNOWN("Не задано");
    private final String title;

    Descriptor(final String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public record Member(AtomId atom, Descriptor descriptor) {
    public Member {
      Objects.requireNonNull(atom);
      Objects.requireNonNull(descriptor);
    }
  }

  public record Group(
      String id,
      Kind kind,
      AtomId anchor,
      List<Member> members,
      String source,
      Instant recordedAt) {
    public Group {
      if (id == null || id.length() > 200) {
        throw new IllegalArgumentException("Некорректный номер стереогруппы.");
      }
      id = IdentifierValue.requireIdentifier(id, "Stereo group id");
      Objects.requireNonNull(kind);
      Objects.requireNonNull(anchor);
      Objects.requireNonNull(recordedAt);
      members = List.copyOf(members);
      source = Objects.requireNonNullElse(source, "").strip();
      if (source.isEmpty() || source.length() > 4000 || source.indexOf('\0') >= 0) {
        throw new IllegalArgumentException("Укажите источник назначения, до 4000 символов.");
      }
      if (members.isEmpty()
          || members.size() > 64
          || members.stream().map(Member::atom).distinct().count() != members.size()
          || members.stream().noneMatch(m -> m.atom().equals(anchor))) {
        throw new IllegalArgumentException(
            "Стереогруппа содержит 1–64 разных центра и выбранный опорный центр.");
      }
      if (kind == Kind.RELATIVE && members.size() < 2) {
        throw new IllegalArgumentException(
            "Для относительной конфигурации выберите не менее двух центров.");
      }
      if (kind != Kind.UNKNOWN_ABSOLUTE
          && members.stream().anyMatch(m -> m.descriptor() == Descriptor.UNKNOWN)) {
        throw new IllegalArgumentException("Явно назначьте R* или S* каждому центру этой группы.");
      }
      if (kind == Kind.UNKNOWN_ABSOLUTE
          && (members.size() != 1 || members.getFirst().descriptor() != Descriptor.UNKNOWN)) {
        throw new IllegalArgumentException(
            "Неустановленная абсолютная конфигурация задаётся одному центру без R*/S*. Для взаимного расположения создайте относительную группу.");
      }
    }

    @Override
    public String toString() {
      return kind + " · " + members.size() + " центр(а)";
    }
  }

  public MolecularRelativeStereo {
    if (structureSignature == null || !structureSignature.matches("[a-f0-9]{64}")) {
      throw new IllegalArgumentException("Нет подписи графа относительной стереохимии.");
    }
    groups = List.copyOf(groups);
    if (groups.size() > 64 || groups.stream().map(Group::id).distinct().count() != groups.size()) {
      throw new IllegalArgumentException("Повтор или слишком много стереогрупп.");
    }
    final Set<AtomId> used = new HashSet<>();
    for (final var group : groups) {
      for (final var member : group.members()) {
        if (!used.add(member.atom())) {
          throw new IllegalArgumentException(
              "Один центр не может входить в несколько стереогрупп.");
        }
      }
    }
  }

  public static String signature(final Molecule m) {
    return LaboratoryMaterialProperties.signature(m);
  }

  public boolean matches(final Molecule m) {
    return structureSignature.equals(signature(m));
  }

  public Set<AtomId> atoms() {
    final Set<AtomId> ids = new HashSet<>();
    groups.forEach(g -> g.members().forEach(m -> ids.add(m.atom())));
    return Set.copyOf(ids);
  }

  public void validate(final Molecule m) {
    if (!matches(m)) {
      throw new IllegalArgumentException(
          "Граф изменён после назначения относительной стереохимии. Пересоздайте или удалите устаревшие группы.");
    }
    final Set<AtomId> ids =
        m.atoms().stream().map(Atom::id).collect(java.util.stream.Collectors.toSet());
    for (final var id : atoms()) {
      if (!ids.contains(id)) {
        throw new IllegalArgumentException("Центр стереогруппы отсутствует в структуре.");
      }
      final var bonds =
          m.bonds().stream()
              .filter(b -> b.firstAtomId().equals(id) || b.secondAtomId().equals(id))
              .toList();
      if (bonds.size() != 4 || bonds.stream().anyMatch(b -> b.type() != BondType.SINGLE)) {
        throw new IllegalArgumentException(
            "Для тетраэдрического центра группы нужны четыре явные одинарные связи, включая H.");
      }
    }
  }

  public static boolean present(final Molecule m) {
    return m.metadata().attributes().keySet().stream().anyMatch(k -> k.startsWith(PREFIX));
  }

  public Molecule apply(final Molecule m) {
    validate(m);
    return attach(m, this);
  }

  public static Molecule clear(final Molecule m) {
    return attach(m, null);
  }

  private static Molecule attach(final Molecule m, final MolecularRelativeStereo value) {
    final Map<String, String> a = new LinkedHashMap<>(m.metadata().attributes());
    a.keySet().removeIf(k -> k.startsWith(PREFIX));
    if (value != null && !value.groups().isEmpty()) {
      a.put(PREFIX + "signature", value.structureSignature());
      a.put(PREFIX + "count", Integer.toString(value.groups().size()));
      for (int i = 0; i < value.groups().size(); i++) {
        final var g = value.groups().get(i);
        a.put(
            PREFIX + "group." + i,
            encode(g.id())
                + "|"
                + g.kind().name()
                + "|"
                + encode(g.anchor().value())
                + "|"
                + encode(g.source())
                + "|"
                + g.recordedAt()
                + "|"
                + g.members().stream()
                    .map(x -> encode(x.atom().value()) + ":" + x.descriptor().name())
                    .collect(java.util.stream.Collectors.joining(",")));
      }
    }
    return Molecule.of(
        m.id(),
        m.displayName(),
        m.atoms(),
        m.bonds(),
        m.charge(),
        m.spinMultiplicity(),
        m.stereochemistry(),
        m.conformation(),
        m.opticalRotation(),
        m.symmetry(),
        ChemistryMetadata.of(m.metadata().source(), m.metadata().location(), a));
  }

  public static Optional<MolecularRelativeStereo> read(final Molecule m) {
    if (!present(m)) {
      return Optional.empty();
    }
    final var a = m.metadata().attributes();
    try {
      final String raw = Objects.requireNonNull(a.get(PREFIX + "count"));
      if (raw.length() > 2) {
        throw new IllegalArgumentException();
      }
      final int count = Integer.parseInt(raw);
      if (count < 1
          || count > 64
          || a.keySet().stream().filter(k -> k.startsWith(PREFIX)).count() != count + 2) {
        throw new IllegalArgumentException();
      }
      final List<Group> groups = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        final String record = Objects.requireNonNull(a.get(PREFIX + "group." + i));
        if (record.length() > 40000) {
          throw new IllegalArgumentException();
        }
        final String[] p = record.split("\\|", -1);
        if (p.length != 6 || p[5].split(",", -1).length > 64) {
          throw new IllegalArgumentException();
        }
        final List<Member> members = new ArrayList<>();
        for (final String item : p[5].split(",", -1)) {
          final String[] q = item.split(":", -1);
          if (q.length != 2) {
            throw new IllegalArgumentException();
          }
          members.add(new Member(AtomId.of(decode(q[0], 200)), Descriptor.valueOf(q[1])));
        }
        groups.add(
            new Group(
                decode(p[0], 200),
                Kind.valueOf(p[1]),
                AtomId.of(decode(p[2], 200)),
                members,
                decode(p[3], 4000),
                Instant.parse(p[4])));
      }
      return Optional.of(new MolecularRelativeStereo(a.get(PREFIX + "signature"), groups));
    } catch (final RuntimeException ex) {
      throw new IllegalArgumentException(
          "Повреждена запись относительной стереохимии; исходные данные сохранены в QCHEM.", ex);
    }
  }

  private static String encode(final String s) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(s.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(final String s, final int max) {
    if (s.length() > max * 6) {
      throw new IllegalArgumentException();
    }
    try {
      final String value =
          StandardCharsets.UTF_8
              .newDecoder()
              .decode(java.nio.ByteBuffer.wrap(Base64.getUrlDecoder().decode(s)))
              .toString();
      if (value.length() > max) {
        throw new IllegalArgumentException();
      }
      return value;
    } catch (final java.nio.charset.CharacterCodingException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
