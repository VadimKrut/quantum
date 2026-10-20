/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.*;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.reaction.*;

/** A read-only report projection. The same core arithmetic supplies the workbench and exports. */
public final class LaboratoryExperimentReport {
  public record Options(
      boolean structures,
      boolean quantities,
      boolean procedure,
      boolean analyses,
      boolean journal,
      boolean attachments,
      boolean diagnostics,
      boolean superseded,
      boolean strict,
      Map<String, String> references) {
    public Options {
      references = Map.copyOf(references);
    }

    public static Options defaults() {
      return new Options(true, true, true, true, false, true, false, false, false, Map.of());
    }
  }

  public sealed interface Block permits Heading, Paragraph, Table, Figure {}

  public record Heading(String text, int level) implements Block {}

  public record Paragraph(String text, boolean subdued) implements Block {}

  public record Table(List<String> headers, List<List<String>> rows) implements Block {
    public Table {
      headers = List.copyOf(headers);
      rows = rows.stream().map(List::copyOf).toList();
      if (headers.isEmpty()) {
        throw new IllegalArgumentException("Не заданы столбцы отчёта.");
      }
      for (final var row : rows) {
        if (row.size() != headers.size()) {
          throw new IllegalArgumentException("Неверные столбцы отчёта.");
        }
      }
    }
  }

  public record Figure(String key, String caption) implements Block {}

  public record Document(String title, String experimentId, List<Block> blocks) {
    public Document {
      blocks = List.copyOf(blocks);
    }
  }

  private LaboratoryExperimentReport() {}

  public static Document create(final LaboratoryExperiment experiment, final Options options) {
    return create(experiment, options, Map.of());
  }

  public static Document create(
      final ru.pathcreator.vadim.quantum.chemistry.drawing.domain.notebook.LaboratoryDrawing
          drawing,
      final Options options) {
    return create(drawing.experiment(), options, drawing.layouts());
  }

  private static Document create(
      final LaboratoryExperiment experiment,
      final Options options,
      final Map<String, ru.pathcreator.vadim.quantum.chemistry.drawing.domain.model.MoleculeDrawing>
          layouts) {
    final Map<String, BigDecimal> structuralMasses = new LinkedHashMap<>();
    experiment
        .components()
        .forEach(
            c -> {
              final var mass = LaboratoryPlanStructureMasses.molarMass(c.molecule());
              if (mass != null) {
                structuralMasses.put(c.id(), mass);
              }
            });
    final List<Block> blocks = new ArrayList<>();
    final var details = new LaboratoryExperimentReportDetails(experiment, options, blocks);
    if (experiment.stages().isEmpty()
        && !conditions(experiment.conditions()).equals("Не указаны.")) {
      blocks.add(new Heading("Условия", 1));
      blocks.add(new Paragraph(conditions(experiment.conditions()), false));
    }
    final Map<String, LaboratoryComponent> components = new LinkedHashMap<>();
    experiment.components().forEach(c -> components.put(c.id(), c));
    if (experiment.stages().isEmpty()) {
      blocks.add(new Paragraph("Стадии реакции не заданы.", true));
    }
    int index = 0;
    for (final var stage : experiment.stages()) {
      blocks.add(
          new Heading((experiment.stages().size() > 1 ? ++index + ". " : "") + stage.title(), 1));
      if (!stage.classification().reactionNames().isEmpty()) {
        blocks.add(
            new Paragraph(
                "Название реакции: "
                    + stage.classification().reactionNames().stream()
                        .map(LaboratoryReactionClassification.Term::text)
                        .collect(java.util.stream.Collectors.joining(" · ")),
                false));
      }
      if (!stage.classification().mechanisms().isEmpty()) {
        blocks.add(
            new Paragraph(
                "Указанные механизмы: "
                    + stage.classification().mechanisms().stream()
                        .map(LaboratoryReactionClassification.Term::text)
                        .collect(java.util.stream.Collectors.joining(" · ")),
                false));
      }
      if (!stage.classification().note().isBlank()) {
        blocks.add(
            new Paragraph("Примечание к классификации: " + stage.classification().note(), false));
      }
      if (options.structures()) {
        blocks.add(new Figure("stage:" + stage.id(), ""));
      }
      final String stageConditions = conditions(stage.conditions());
      if (!stageConditions.equals("Не указаны.")) {
        blocks.add(new Heading("Условия", 2));
        blocks.add(new Paragraph(stageConditions, false));
      }
      final var auxiliaries =
          stage.participants().stream()
              .filter(
                  p ->
                      p.role() != ReactionComponentRole.REACTANT
                          && p.role() != ReactionComponentRole.PRODUCT)
              .map(
                  p ->
                      role(p.role())
                          + ": "
                          + components.get(p.componentId()).molecule().displayName())
              .toList();
      if (!auxiliaries.isEmpty()) {
        blocks.add(new Paragraph(String.join("\n", auxiliaries), false));
      }
      if (options.quantities()) {
        LaboratoryReportQuantities.append(experiment, stage, options, structuralMasses, blocks);
      }
    }
    if (options.structures() || options.quantities()) {
      final Set<String> assigned = new HashSet<>();
      experiment
          .stages()
          .forEach(s -> s.participants().forEach(p -> assigned.add(p.componentId())));
      for (final var component : experiment.components()) {
        if (!assigned.contains(component.id())) {
          blocks.add(new Heading(component.molecule().displayName() + " · вне стадий", 2));
          if (options.structures()) {
            blocks.add(
                new Figure(
                    "component:" + component.id(), component.molecule().formula().hillNotation()));
          }
          if (options.quantities()) {
            blocks.add(
                new Paragraph(
                    role(component.role())
                        + " · "
                        + component.evidence()
                        + "\nПлан: "
                        + quantity(component.planned())
                        + "; "
                        + component.evidence().quantityLabel()
                        + ": "
                        + quantity(component.actual()),
                    false));
          }
        }
      }
    }
    if (options.diagnostics()) {
      final List<Block> identityBlocks = new ArrayList<>();
      for (final var component : experiment.components()) {
        try {
          final var saved =
              ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity.read(
                  component.molecule());
          if (saved.isEmpty()) {
            continue;
          }
          final var identity = saved.get();
          identityBlocks.add(new Heading(component.molecule().displayName(), 2));
          final var identifiers =
              new ru.pathcreator.vadim.quantum.chemistry.drawing.infrastructure.identity
                  .CdkCompoundIdentifiers();
          final var layout = layouts.get(component.id());
          if (!(layout == null
              ? identifiers.current(component.molecule(), identity)
              : identifiers.current(layout, identity))) {
            identityBlocks.add(
                new Paragraph(
                    "Сохранённая карточка не подтверждена для текущей структуры и конфигурации. Названия и идентификаторы требуют перепроверки.",
                    true));
            continue;
          }
          final List<List<String>> rows = new ArrayList<>();
          if (!identity.commonName().isBlank()) {
            rows.add(List.of("Распространённое название", identity.commonName()));
          }
          if (!identity.iupacName().isBlank()) {
            rows.add(List.of("IUPAC", identity.iupacName()));
          }
          if (!identity.cas().isBlank()) {
            rows.add(List.of("CAS", identity.cas()));
          }
          if (!identity.inchiKey().isBlank()) {
            rows.add(List.of("InChIKey", identity.inchiKey()));
          }
          if (!identity.pubchemCid().isBlank()) {
            rows.add(List.of("PubChem CID", identity.pubchemCid()));
          }
          identityBlocks.add(new Table(List.of("Название / идентификатор", "Значение"), rows));
          identityBlocks.add(
              new Paragraph(
                  "Полные синонимы и структурные строки доступны в карточке вещества в QCHEM.",
                  true));
          identityBlocks.add(
              new Paragraph(
                  (identity.origin()
                              == ru.pathcreator.vadim.quantum.chemistry.domain.identity
                                  .MolecularIdentity.Origin.PUBCHEM
                          ? "PubChem; граф и вся заданная поддержанная стереоконфигурация сопоставлены."
                          : "Ручной ввод; соответствие названию проверяет автор.")
                      + "\nИсточник: "
                      + identity.source()
                      + "\nЗаписано: "
                      + identity.recordedAt(),
                  true));
        } catch (final IllegalArgumentException e) {
          identityBlocks.add(
              new Paragraph(
                  "Карточка «"
                      + component.molecule().displayName()
                      + "» не прочитана: "
                      + e.getMessage(),
                  true));
        }
      }
      if (!identityBlocks.isEmpty()) {
        blocks.add(new Heading("Названия и идентификаторы веществ", 1));
        blocks.addAll(identityBlocks);
      }
    }
    if (options.procedure()) {
      if (!experiment.notes().isBlank()) {
        blocks.add(new Heading("Примечания", 1));
        blocks.add(new Paragraph(experiment.notes(), false));
      }
      details.procedure();
      details.operations();
      if (!options.journal()) {
        final var observations =
            experiment.journal().stream()
                .filter(v -> v.kind() != LaboratoryEntry.Kind.ACTION && !v.text().isBlank())
                .toList();
        if (!observations.isEmpty()) {
          blocks.add(new Heading("Наблюдения", 1));
          observations.forEach(v -> blocks.add(new Paragraph(v.text(), false)));
        }
      }
    }
    if (options.quantities() || options.analyses()) {
      details.outcomes();
    }

    if (options.quantities() && !experiment.samples().isEmpty()) {
      blocks.add(new Heading("Образцы и фракции", 1));
      if (options.diagnostics()) {
        blocks.add(
            new Paragraph(
                "Остатки относятся к физическим образцам. Они не заменяют записанное количество продукта и расчёт выхода реакции. Структура образца — сохранённая привязка; чистота не выводится из наличия образца.",
                true));
      }
      final List<List<String>> sampleRows = new ArrayList<>();
      for (final var sample : experiment.samples()) {
        final var b = sample.balance();
        sampleRows.add(
            List.of(
                sample.code() + "\n" + b.label(),
                sample.kind().toString(),
                b.remaining() == null ? "Не измерен" : b.remaining().toString(),
                b.status().toString(),
                b.location() + (b.container().isBlank() ? "" : " · " + b.container())));
      }
      blocks.add(new Table(List.of("Образец", "Тип", "Остаток", "Статус", "Хранение"), sampleRows));
      for (final var sample : experiment.samples()) {
        final String parents =
            String.join(
                ", ",
                sample.parents().stream()
                    .map(
                        id ->
                            experiment.samples().stream()
                                .filter(s -> s.id().equals(id))
                                .findFirst()
                                .orElseThrow()
                                .code())
                    .toList());
        blocks.add(
            new Paragraph(
                sample.code()
                    + " ← "
                    + (parents.isEmpty() ? sample.molecule().displayName() : parents)
                    + (sample.stageTitle().isBlank() ? "" : " · " + sample.stageTitle()),
                true));
        if (options.journal()) {
          for (final var e : sample.events()) {
            blocks.add(
                new Paragraph(
                    sample.code()
                        + " · "
                        + e.time()
                        + " · "
                        + e.action()
                        + (e.quantity() == null ? "" : " · " + e.quantity())
                        + (e.label().isBlank()
                            ? ""
                            : "\n" + e.label() + " · " + e.location() + " · " + e.container())
                        + "\n"
                        + e.note(),
                    true));
          }
        }
      }
    }
    if (options.analyses()
        && (!experiment.analyses().isEmpty()
            || !experiment.analyticalEvidence().entries().isEmpty())) {
      blocks.add(new Heading("Аналитические результаты", 1));
      final Set<String> corrected = new HashSet<>();
      experiment.analyses().stream()
          .map(LaboratoryAnalysis::correctsId)
          .filter(Objects::nonNull)
          .forEach(corrected::add);
      if (options.diagnostics()) {
        blocks.add(
            new Paragraph(
                options.superseded()
                    ? "Включена история аналитических исправлений."
                    : "Показаны записи без последующего исправления. Исходные версии сохраняются в QCHEM.",
                true));
      }
      if (experiment.analyses().isEmpty() && experiment.analyticalEvidence().entries().isEmpty()) {
        blocks.add(new Paragraph("Результаты не внесены.", true));
      }
      for (final var analysis : experiment.analyses()) {
        if (options.superseded() || !corrected.contains(analysis.id())) {
          blocks.add(new Heading(analysis.title(), 2));
          String analyticalText = LaboratoryAnalysisSummary.text(analysis);
          if (analysis.sampleId() != null) {
            final var sample =
                experiment.samples().stream()
                    .filter(s -> s.id().equals(analysis.sampleId()))
                    .findFirst()
                    .orElseThrow();
            blocks.add(
                new Paragraph(
                    "Связанный образец: " + sample.code() + " · " + sample.balance().label(),
                    true));
          }
          if (analysis.data() instanceof LaboratoryAnalysis.Mass m) {
            analyticalText =
                analyticalText.replace(
                    "calcd m/z " + exact(m.calculatedMz()) + ", found " + exact(m.foundMz()),
                    "calcd m/z " + mass(m.calculatedMz()) + ", found " + mass(m.foundMz()));
          }
          blocks.add(new Paragraph(analyticalText, false));
          if (options.diagnostics()) {
            blocks.add(
                new Paragraph(
                    "Записано: "
                        + analysis.recordedAt()
                        + " · ID: "
                        + analysis.id()
                        + (analysis.correctsId() == null
                            ? ""
                            : "\nИсправление записи: " + analysis.correctsId())
                        + (corrected.contains(analysis.id())
                            ? "\nЕсть последующее исправление."
                            : ""),
                    true));
          }
          if (options.structures()) {
            blocks.add(
                new Figure(
                    "analysis:" + analysis.id(),
                    analysis.data() instanceof LaboratoryAnalysis.Nmr n && n.trace() != null
                        ? "Спектр ЯМР"
                        : "Структура и введённые аналитические данные"));
          }
          if (analysis.data() instanceof LaboratoryAnalysis.Mass mass
              && !mass.calculationSource().isBlank()) {
            blocks.add(new Paragraph("Источник расчётного m/z: " + mass.calculationSource(), true));
          }
          if (analysis.data() instanceof LaboratoryAnalysis.Nmr nmr) {
            final List<List<String>> rows = new ArrayList<>();
            int signal = 0;
            for (final var s : nmr.signals()) {
              rows.add(
                  List.of(
                      Integer.toString(++signal),
                      number(s.from())
                          + (s.from().compareTo(s.to()) == 0 ? "" : " - " + number(s.to())),
                      s.multiplicity(),
                      number(s.integral()),
                      s.atoms().isEmpty()
                          ? "не назначено"
                          : s.atoms().stream()
                              .map(a -> a.value())
                              .sorted()
                              .collect(java.util.stream.Collectors.joining(", "))));
            }
            if (options.diagnostics()) {
              blocks.add(
                  new Paragraph(
                      "Интегралы в таблице до нормировки; множитель: "
                          + number(nmr.integralScale())
                          + ". Назначения используют идентификаторы атомов сохранённого снимка.",
                      true));
            }
            blocks.add(
                new Table(
                    List.of("Сигнал", "δ, ppm", "Мультиплет", "Интеграл", "Атомы снимка"), rows));
          }
          if (options.diagnostics()) {
            blocks.add(
                new Paragraph(
                    String.join("\n", LaboratoryAnalysisSummary.diagnostics(analysis, experiment)),
                    true));
          }
          final List<String> names =
              analysis.attachmentIds().stream()
                  .map(
                      id ->
                          experiment.attachments().stream()
                              .filter(a -> a.id().equals(id))
                              .findFirst()
                              .orElseThrow()
                              .name())
                  .toList();
          if (!names.isEmpty()) {
            blocks.add(
                new Paragraph(
                    names.isEmpty()
                        ? analysis.data() instanceof LaboratoryAnalysis.Nmr n && n.trace() != null
                            ? "Числовые исходные данные сохранены в QCHEM. Полный каталог прибора отдельно не приложен."
                            : "Исходные файлы не связаны."
                        : "Связанные файлы: " + String.join("; ", names),
                    true));
          }
        }
      }
      details.advanced();
    }
    if (options.journal()) {
      blocks.add(new Heading("Журнал опыта", 1));
      if (experiment.journal().isEmpty()) {
        blocks.add(new Paragraph("Записи отсутствуют.", true));
      }
      for (final var entry : experiment.journal()) {
        blocks.add(new Heading(entry.kind() + " · " + entry.recordedAt(), 2));
        blocks.add(new Paragraph(entry.text(), false));
        blocks.add(
            new Paragraph(
                "ID: "
                    + entry.id()
                    + (entry.correctsId() == null ? "" : "; исправляет: " + entry.correctsId()),
                true));
      }
    }
    if (options.attachments() && !experiment.attachments().isEmpty()) {
      blocks.add(new Heading("Исходные файлы", 1));
      if (options.diagnostics()) {
        blocks.add(
            new Paragraph(
                "Перечень вложений, без самих файлов. Для переноса исходных данных используйте пакет опыта QCHEMPACK. Наличие файлов на диске этим отчётом не проверяется.",
                true));
      }
      if (options.diagnostics()) {
        blocks.add(
            new Table(
                List.of("Название", "Байт", "SHA-256"),
                experiment.attachments().stream()
                    .map(a -> List.of(a.name(), Long.toString(a.size()), a.sha256()))
                    .toList()));
      } else {
        blocks.add(
            new Paragraph(
                String.join(
                    "\n",
                    experiment.attachments().stream().map(LaboratoryAttachment::name).toList()),
                false));
      }
    }
    return new Document(experiment.title(), experiment.id(), blocks);
  }

  public static String conditions(final ReactionConditions c) {
    final List<String> lines = new ArrayList<>();
    if (c.temperature() != null) {
      lines.add(
          "Температура: "
              + LaboratoryReportQuantities.decimal(BigDecimal.valueOf(c.temperature().value()))
              + (c.temperature().unit()
                      == ru.pathcreator.vadim.quantum.chemistry.domain.unit.TemperatureUnit.CELSIUS
                  ? " °C"
                  : " K"));
    }
    if (c.duration() != null) {
      lines.add(
          "Продолжительность: "
              + LaboratoryReportQuantities.decimal(BigDecimal.valueOf(c.duration().value()))
              + " "
              + switch (c.duration().unit()) {
                case SECOND -> "с";
                case MINUTE -> "мин";
                case HOUR -> "ч";
                case DAY -> "сут";
              });
    }
    if (c.pressure() != null) {
      lines.add(
          "Давление: "
              + LaboratoryReportQuantities.decimal(BigDecimal.valueOf(c.pressure().value()))
              + " "
              + switch (c.pressure().unit()) {
                case PASCAL -> "Па";
                case ATMOSPHERE -> "атм";
                case BAR -> "бар";
              });
    }
    for (final var item : c.components()) {
      lines.add(
          role(item.role())
              + ": "
              + item.name()
              + (item.amount() == null
                  ? ""
                  : "; количество = "
                      + item.amount().value()
                      + " "
                      + switch (item.amount().unit()) {
                        case MOLE -> "моль";
                        case MILLIMOLE -> "ммоль";
                        case MICROMOLE -> "мкмоль";
                        case KILOGRAM -> "кг";
                        case GRAM -> "г";
                        case MILLIGRAM -> "мг";
                        case MICROGRAM -> "мкг";
                      })
              + (item.concentration() == null
                  ? ""
                  : "; c = "
                      + item.concentration().value()
                      + " "
                      + switch (item.concentration().unit()) {
                        case MOLE_PER_LITER -> "моль/л";
                        case MILLIMOLE_PER_LITER -> "ммоль/л";
                        case MICROMOLE_PER_LITER -> "мкмоль/л";
                      })
              + (item.equivalent() == null ? "" : "; экв. = " + item.equivalent().value())
              + (item.loading() == null ? "" : "; загрузка = " + item.loading().percent() + " %")
              + (item.purity() == null ? "" : "; чистота = " + item.purity().percent() + " %")
              + (item.phase() == null || item.phase() == ReactionPhase.UNKNOWN
                  ? ""
                  : "; фаза: "
                      + switch (item.phase()) {
                        case SOLID -> "твёрдая";
                        case LIQUID -> "жидкая";
                        case GAS -> "газ";
                        case SOLUTION -> "раствор";
                        case AQUEOUS -> "водный раствор";
                        case MELT -> "расплав";
                        case UNKNOWN -> "не указана";
                      })
              + (item.note() == null ? "" : "; " + item.note()));
    }
    for (final var feature : c.features()) {
      lines.add(
          featureName(feature.kind())
              + ": "
              + Objects.toString(feature.value(), "")
              + (feature.note() == null ? "" : " · " + feature.note()));
    }
    if (c.note() != null) {
      lines.add(c.note());
    }
    return lines.isEmpty() ? "Не указаны." : String.join("\n", lines);
  }

  public static String number(final BigDecimal n) {
    if (n == null) {
      return "—";
    }
    final BigDecimal rounded = n.round(new MathContext(8, RoundingMode.HALF_UP));
    return (n.compareTo(rounded) == 0 ? "" : "≈ ")
        + (Math.abs(rounded.scale()) > 12
            ? rounded.toEngineeringString()
            : rounded.stripTrailingZeros().toPlainString());
  }

  private static String exact(final BigDecimal n) {
    return n == null ? "—" : n.stripTrailingZeros().toPlainString();
  }

  private static BigDecimal mmol(final BigDecimal n) {
    return n == null ? null : n.movePointRight(3);
  }

  private static String quantity(final LaboratoryQuantity q) {
    return q == null ? "не указано" : number(q.value()) + " " + q.unit();
  }

  private static String mass(final BigDecimal n) {
    if (n == null) {
      return "—";
    }
    final BigDecimal rounded = n.setScale(5, RoundingMode.HALF_UP);
    if (rounded.signum() == 0 && n.signum() != 0) {
      return number(n);
    }
    return (rounded.compareTo(n) == 0 ? "" : "≈ ") + rounded.toPlainString();
  }

  public static String role(final ReactionComponentRole role) {
    return switch (role) {
      case REACTANT -> "Исходник";
      case PRODUCT -> "Продукт";
      case SOLVENT -> "Растворитель";
      case CATALYST -> "Катализатор";
      case ADDITIVE -> "Добавка";
      case ACID -> "Кислота";
      case BASE -> "Основание";
      case LIGAND -> "Лиганд";
      case ATMOSPHERE -> "Атмосфера";
      case WORKUP -> "Обработка";
      case OTHER -> "Другое";
    };
  }

  private static String featureName(final ReactionConditionFeatureKind kind) {
    return switch (kind) {
      case INERT_ATMOSPHERE -> "Инертная атмосфера";
      case AIR_SENSITIVE -> "Чувствительность к воздуху";
      case ANHYDROUS -> "Безводные условия";
      case CATALYST_PRESENT -> "Катализатор";
      case DEHYDRATING_AGENT -> "Водоотнимающий агент";
      case ACIDIC_MEDIUM -> "Кислая среда";
      case BASIC_MEDIUM -> "Основная среда";
      case OXIDIZING_MEDIUM -> "Окислительная среда";
      case REDUCING_MEDIUM -> "Восстановительная среда";
      case HYDROGENATION -> "Гидрирование";
      case PHOTOCHEMICAL -> "Фотохимия";
      case ELECTROCHEMICAL -> "Электрохимия";
      case MICROWAVE -> "Микроволновый нагрев";
      case SEALED_VESSEL -> "Закрытый сосуд";
      case FLOW_REACTOR -> "Проточный реактор";
      case REFLUX -> "Кипячение с обратным холодильником";
      case COOLING -> "Охлаждение";
      case HEATING -> "Нагрев";
      case ADDITION_RATE -> "Скорость добавления";
      case WORKUP -> "Обработка";
      case PURIFICATION -> "Очистка";
      case CUSTOM -> "Другие условия";
    };
  }
}
