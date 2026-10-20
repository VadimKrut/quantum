/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.notebook.LaboratoryAnalyticalEvidence.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;

/** Deterministic calculations from explicit analytical observations; no instrument or purity is inferred. */
public final class LaboratoryEvidenceService {

  private static final MathContext MC = MathContext.DECIMAL128;
  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private LaboratoryEvidenceService() {}

  public record Quantitation(BigDecimal purityPercent, List<String> warnings) {
    public Quantitation {
      warnings = List.copyOf(warnings);
    }
  }

  public record Calibration(
      BigDecimal slopeAreaPerGram,
      BigDecimal intercept,
      BigDecimal rSquared,
      BigDecimal minGram,
      BigDecimal maxGram) {}

  public record ChromQuantitation(
      LaboratoryQuantity mass,
      BigDecimal purityPercent,
      Calibration calibration,
      List<String> warnings) {
    public ChromQuantitation {
      warnings = List.copyOf(warnings);
    }
  }

  public record Integrated(BigDecimal retentionMin, BigDecimal area) {}

  /** Exact immutable source, including historical corrections; never substitutes the latest record. */
  public static Optional<LaboratoryAnalysis> exactSource(
      final LaboratoryExperiment experiment, final Entry entry) {
    if (!(entry.data() instanceof Multiplets data)) {
      return Optional.empty();
    }
    final var source =
        exactSource(experiment, entry.componentId(), entry.sampleId(), entry.molecule(), data);
    if (source.isPresent() && source.get().origin() != entry.origin()) {
      throw new IllegalArgumentException(
          "Происхождение мультиплетов должно соответствовать выбранной записи 1D ЯМР.");
    }
    return source;
  }

  public static Optional<LaboratoryAnalysis> exactSource(
      final LaboratoryExperiment experiment,
      final String componentId,
      final String sampleId,
      final Molecule snapshot,
      final Multiplets data) {
    if (data.analysisId() == null) {
      return Optional.empty();
    }
    final var source =
        experiment.analyses().stream()
            .filter(a -> a.id().equals(data.analysisId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Связанная запись 1D ЯМР отсутствует: " + data.analysisId()));
    if (!(source.data() instanceof LaboratoryAnalysis.Nmr nmr)
        || nmr.trace() == null
        || !source.componentId().equals(componentId)
        || !Objects.equals(source.sampleId(), sampleId)
        || nmr.nucleus() != data.nucleus()
        || nmr.frequencyMHz() == null
        || nmr.frequencyMHz().compareTo(data.frequencyMHz()) != 0
        || !MolecularIdentity.signature(source.molecule())
            .equals(MolecularIdentity.signature(snapshot))) {
      throw new IllegalArgumentException(
          "Трасса должна относиться к той же структуре, образцу, ядру и частоте.");
    }
    return Optional.of(source);
  }

  /** BIPM ISRD: wA = IA/IS * NS/NA * MA/MS * mS/mA * wS; masses refer to the same prepared solution. */
  public static Quantitation quantitativeNmr(final QuantitativeNmr value) {
    final var result =
        value
            .sampleIntegral()
            .divide(value.standardIntegral(), MC)
            .multiply(
                BigDecimal.valueOf(value.standardNuclei())
                    .divide(BigDecimal.valueOf(value.sampleNuclei()), MC),
                MC)
            .multiply(value.sampleMolarMass().divide(value.standardMolarMass(), MC), MC)
            .multiply(
                value.standardMass().baseValue().divide(value.sampleMass().baseValue(), MC), MC)
            .multiply(value.standardPurityPercent(), MC);
    final List<String> warnings = new ArrayList<>();
    if (result.compareTo(HUNDRED) > 0) {
      warnings.add(
          "Расчётная массовая доля выше 100%. Проверьте перекрытие, интегралы, число ядер, навески и стандарт; результат не ограничен искусственно до 100%.");
    }
    warnings.add(
        "Количественная пригодность интегралов, полнота релаксации и отсутствие перекрытия проверяются по методике. Расчёт не оценивает неопределённость.");
    return new Quantitation(result, warnings);
  }

  /** Adjacent line spacings, not an automatic first-order multiplet identification or coupling fit. */
  public static List<BigDecimal> lineSpacings(
      final Multiplet multiplet, final BigDecimal frequencyMHz) {
    final var lines = multiplet.linesPpm().stream().sorted().distinct().toList();
    final List<BigDecimal> result = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      result.add(lines.get(i).subtract(lines.get(i - 1)).multiply(frequencyMHz, MC));
    }
    return List.copyOf(result);
  }

  public static Map<String, BigDecimal> areaPercent(final Chromatogram data) {
    final var sum =
        data.peaks().stream()
            .filter(ChromPeak::included)
            .map(ChromPeak::area)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    final var values = new LinkedHashMap<String, BigDecimal>();
    if (sum.signum() == 0) {
      return Map.of();
    }
    data.peaks().stream()
        .filter(ChromPeak::included)
        .forEach(p -> values.put(p.id(), p.area().multiply(HUNDRED).divide(sum, MC)));
    return Collections.unmodifiableMap(values);
  }

  /** Unweighted least squares with an intercept, area = slope * grams + intercept. */
  public static Calibration calibration(final MassCalibration input) {
    final BigDecimal count = BigDecimal.valueOf(input.points().size());
    final BigDecimal meanX =
        input.points().stream()
            .map(p -> p.mass().baseValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(count, MC);
    final BigDecimal meanY =
        input.points().stream()
            .map(CalibrationPoint::area)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(count, MC);
    BigDecimal xx = BigDecimal.ZERO, xy = BigDecimal.ZERO, yy = BigDecimal.ZERO;
    for (final var point : input.points()) {
      final var x = point.mass().baseValue().subtract(meanX);
      final var y = point.area().subtract(meanY);
      xx = xx.add(x.multiply(x, MC), MC);
      xy = xy.add(x.multiply(y, MC), MC);
      yy = yy.add(y.multiply(y, MC), MC);
    }
    if (xx.signum() == 0) {
      throw new IllegalArgumentException("Для калибровки нужны разные массы стандарта.");
    }
    final var slope = xy.divide(xx, MC);
    if (slope.signum() <= 0) {
      throw new IllegalArgumentException("Калибровка должна иметь положительный наклон отклика.");
    }
    final var intercept = meanY.subtract(slope.multiply(meanX, MC));
    final var r2 =
        yy.signum() == 0 ? BigDecimal.ZERO : xy.multiply(xy, MC).divide(xx.multiply(yy, MC), MC);
    return new Calibration(
        slope,
        intercept,
        r2,
        input.points().stream()
            .map(p -> p.mass().baseValue())
            .min(BigDecimal::compareTo)
            .orElseThrow(),
        input.points().stream()
            .map(p -> p.mass().baseValue())
            .max(BigDecimal::compareTo)
            .orElseThrow());
  }

  public static ChromQuantitation quantify(final Chromatogram data) {
    if (data.calibration() == null) {
      return null;
    }
    final var fit = calibration(data.calibration());
    final var peak =
        data.peaks().stream()
            .filter(p -> p.id().equals(data.quantifiedPeakId()))
            .findFirst()
            .orElseThrow();
    final var grams = peak.area().subtract(fit.intercept()).divide(fit.slopeAreaPerGram(), MC);
    if (grams.signum() < 0) {
      throw new IllegalArgumentException(
          "Отклик выбранного пика ниже пересечения калибровки: положительная масса не определена.");
    }
    final BigDecimal purity =
        data.representedSampleMass() == null
            ? null
            : grams.divide(data.representedSampleMass().baseValue(), MC).multiply(HUNDRED, MC);
    final List<String> warnings = new ArrayList<>();
    if (grams.compareTo(fit.minGram()) < 0 || grams.compareTo(fit.maxGram()) > 0) {
      warnings.add(
          "Отклик соответствует массе за пределами диапазона стандартов: выполнена экстраполяция.");
    }
    if (purity != null && purity.compareTo(HUNDRED) > 0) {
      warnings.add(
          "Расчётная массовая доля выше 100%: проверьте калибровку и массу вещества, представленную в анализируемой порции.");
    }
    if (data.calibration().points().size() == 2) {
      warnings.add("Две точки задают прямую, но не подтверждают линейность отклика.");
    }
    warnings.add(
        "Калибровка относится только к выбранному аналиту и той же подготовке и регистрации. Доли площадей других пиков не превращаются в массовые доли.");
    return new ChromQuantitation(
        new LaboratoryQuantity(grams, LaboratoryQuantityUnit.GRAM)
            .in(LaboratoryQuantityUnit.MILLIGRAM),
        purity,
        fit,
        warnings);
  }

  /** Trapezoidal area in response×minute above a straight line joining the explicitly selected endpoints. */
  public static Integrated integrate(
      final List<ChromPoint> trace, final BigDecimal from, final BigDecimal to) {
    if (trace.size() < 2
        || from.compareTo(to) >= 0
        || from.compareTo(trace.getFirst().timeMin()) < 0
        || to.compareTo(trace.getLast().timeMin()) > 0) {
      throw new IllegalArgumentException(
          "Границы интегрирования должны находиться внутри хроматограммы.");
    }
    final var first = interpolate(trace, from);
    final var last = interpolate(trace, to);
    final List<ChromPoint> part = new ArrayList<>();
    part.add(new ChromPoint(from, first));
    trace.stream()
        .filter(p -> p.timeMin().compareTo(from) > 0 && p.timeMin().compareTo(to) < 0)
        .forEach(part::add);
    part.add(new ChromPoint(to, last));
    final var width = to.subtract(from);
    BigDecimal area = BigDecimal.ZERO,
        max = null,
        retention = from,
        previousCorrected = BigDecimal.ZERO;
    for (int i = 0; i < part.size(); i++) {
      final var point = part.get(i);
      final var baseline =
          first.add(
              last.subtract(first).multiply(point.timeMin().subtract(from)).divide(width, MC));
      final var corrected = point.response().subtract(baseline);
      if (max == null || corrected.compareTo(max) > 0) {
        max = corrected;
        retention = point.timeMin();
      }
      if (i > 0) {
        area =
            area.add(
                corrected
                    .add(previousCorrected)
                    .multiply(point.timeMin().subtract(part.get(i - 1).timeMin()))
                    .divide(BigDecimal.valueOf(2), MC),
                MC);
      }
      previousCorrected = corrected;
    }
    if (area.signum() < 0) {
      throw new IllegalArgumentException(
          "Площадь относительно выбранной линии фона отрицательная. Измените границы; отрицательные участки не обрезаются автоматически.");
    }
    return new Integrated(retention, area);
  }

  private static BigDecimal interpolate(final List<ChromPoint> trace, final BigDecimal x) {
    for (int i = 0; i < trace.size(); i++) {
      final var p = trace.get(i);
      if (p.timeMin().equals(x) || p.timeMin().compareTo(x) == 0) {
        return p.response();
      }
      if (p.timeMin().compareTo(x) > 0 && i > 0) {
        final var a = trace.get(i - 1);
        return a.response()
            .add(
                p.response()
                    .subtract(a.response())
                    .multiply(x.subtract(a.timeMin()))
                    .divide(p.timeMin().subtract(a.timeMin()), MC));
      }
    }
    throw new IllegalArgumentException("Точка вне диапазона хроматограммы.");
  }

  public static List<String> diagnostics(final Entry entry) {
    final List<String> warnings = new ArrayList<>();
    switch (entry.data()) {
      case Correlations c -> {
        final long n =
            c.correlations().stream()
                .filter(p -> p.xAtoms().isEmpty() || p.yAtoms().isEmpty())
                .count();
        if (n > 0) {
          warnings.add("Корреляций с незавершённым назначением: " + n + ".");
        }
        if (c.correlations().isEmpty()) {
          warnings.add("Список корреляций пуст.");
        }
        if (c.experiment() == Experiment2D.HSQC) {
          for (final var p : c.correlations()) {
            if (p.xAtoms().size() == 1 && p.yAtoms().size() == 1) {
              final var x = p.xAtoms().iterator().next();
              final var y = p.yAtoms().iterator().next();
              if (x.attachedProton() && !x.atomId().equals(y.atomId())) {
                warnings.add(
                    "HSQC «"
                        + p.id()
                        + "»: выбранный H при тяжёлом атоме и назначение второй оси указывают разные атомы; проверьте корреляцию и структуру.");
              }
            }
          }
        }
      }
      case Multiplets m -> {
        final long n = m.multiplets().stream().filter(p -> p.atoms().isEmpty()).count();
        if (n > 0) {
          warnings.add("Мультиплетов без назначения: " + n + ".");
        }
        if (m.multiplets().stream().anyMatch(p -> p.linesPpm().size() > 1)) {
          warnings.add(
              "Расстояния между линиями показаны отдельно от введённых J: сложные мультиплеты требуют проверки порядка и перекрытия.");
        }
      }
      case QuantitativeNmr q -> warnings.addAll(quantitativeNmr(q).warnings());
      case Chromatogram c -> {
        warnings.add(
            "Доля площади — доля сигнала детектора; массовая чистота не определяется из неё автоматически.");
        if (c.calibration() != null) {
          warnings.addAll(quantify(c).warnings());
        }
        if (c.peaks().stream().anyMatch(p -> !p.included())) {
          warnings.add("Из суммы площадей исключены пики; основания сохранены в строках.");
        }
        if (!c.trace().isEmpty()) {
          for (final var p : c.peaks()) {
            if (p.retentionMin().compareTo(c.trace().getFirst().timeMin()) < 0
                || p.retentionMin().compareTo(c.trace().getLast().timeMin()) > 0) {
              warnings.add(
                  p.label()
                      + ": время удерживания вне сохранённой трассы; отклик в этой точке неизвестен, маркер на трассе не показан.");
            }
          }
        }
      }
      case Dossier d -> {
        if (d.conclusion() == Conclusion.PENDING) {
          warnings.add("Заключение о продукте ещё не принято.");
        }
        d.findings().stream()
            .filter(f -> !f.resolved())
            .forEach(
                f ->
                    warnings.add(
                        severityLabel(f.severity()) + " · " + f.category() + ": " + f.text()));
      }
    }
    if (entry.origin() != LaboratoryAnalysis.Origin.MEASURED) {
      warnings.add(
          "Происхождение данных: " + entry.origin() + ". Это не измерение данного образца.");
    }
    return List.copyOf(warnings);
  }

  public static List<String> dossier(final LaboratoryExperiment experiment, final Entry entry) {
    if (!(entry.data() instanceof Dossier dossier)) {
      return diagnostics(entry);
    }
    final List<String> result = new ArrayList<>(diagnostics(entry));
    for (final String id : dossier.analysisIds()) {
      final var a =
          experiment.analyses().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
      if (!LaboratoryMaterialProperties.signature(a.molecule())
          .equals(LaboratoryMaterialProperties.signature(entry.molecule()))) {
        result.add(a.title() + ": сохранённая структура отличается от структуры досье.");
      }
      if (a.origin() != LaboratoryAnalysis.Origin.MEASURED) {
        result.add(a.title() + ": данные имеют происхождение «" + a.origin() + "».");
      }
      if (a.data() instanceof LaboratoryAnalysis.Nmr n) {
        final long unassigned = n.signals().stream().filter(p -> p.atoms().isEmpty()).count();
        if (unassigned > 0) {
          result.add(a.title() + ": неназначенных сигналов " + unassigned + ".");
        }
      }
      if (a.data() instanceof LaboratoryAnalysis.Mass mass
          && mass.ppmError() != null
          && mass.ppmError().abs().compareTo(dossier.massTolerancePpm()) > 0) {
        result.add(
            a.title()
                + ": отклонение "
                + format(mass.ppmError())
                + " ppm превышает заданный допуск "
                + format(dossier.massTolerancePpm())
                + " ppm.");
      }
    }
    for (final String id : dossier.evidenceIds()) {
      final var a =
          experiment.analyticalEvidence().entries().stream()
              .filter(x -> x.id().equals(id))
              .findFirst()
              .orElseThrow();
      diagnostics(a).forEach(w -> result.add(a.title() + ": " + w));
      if (!LaboratoryMaterialProperties.signature(a.molecule())
          .equals(LaboratoryMaterialProperties.signature(entry.molecule()))) {
        result.add(a.title() + ": сохранённая структура отличается от структуры досье.");
      }
    }
    if (dossier.analysisIds().isEmpty() && dossier.evidenceIds().isEmpty()) {
      result.add("Не выбраны подтверждающие аналитические записи.");
    }
    return List.copyOf(result);
  }

  public static String dossierSummary(final LaboratoryExperiment experiment, final Entry entry) {
    if (!(entry.data() instanceof Dossier dossier)) {
      return description(entry);
    }
    final StringBuilder text =
        new StringBuilder(description(entry))
            .append("\n\nВыбрано аналитических записей: ")
            .append(dossier.analysisIds().size() + dossier.evidenceIds().size())
            .append("\n");
    int index = 0;
    for (final String id : dossier.analysisIds()) {
      final var record =
          experiment.analyses().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
      text.append("\n")
          .append(++index)
          .append(". ")
          .append(record.title())
          .append("\nДата: ")
          .append(record.recordedAt())
          .append(" · ")
          .append(record.origin())
          .append("\n");
      text.append(LaboratoryAnalysisSummary.concise(record)).append("\n");
      if (record.source().isBlank()) text.append("Источник не указан.\n");
      if (experiment.analyses().stream().anyMatch(a -> id.equals(a.correctsId()))) {
        text.append("Выбрана историческая версия; существует последующее исправление.\n");
      }
      if (record.data() instanceof LaboratoryAnalysis.Nmr nmr) {
        text.append("Назначено сигналов: ")
            .append(nmr.signals().stream().filter(s -> !s.atoms().isEmpty()).count())
            .append(" из ")
            .append(nmr.signals().size())
            .append("\n");
      }
      LaboratoryAnalysisSummary.diagnostics(record, experiment)
          .forEach(w -> text.append("• ").append(w).append("\n"));
    }
    for (final String id : dossier.evidenceIds()) {
      final var record =
          experiment.analyticalEvidence().entries().stream()
              .filter(a -> a.id().equals(id))
              .findFirst()
              .orElseThrow();
      text.append("\n")
          .append(++index)
          .append(". ")
          .append(record.title())
          .append("\nДата: ")
          .append(record.recordedAt())
          .append(" · ")
          .append(record.origin())
          .append("\n")
          .append(description(record))
          .append("\n");
      if (record.source().isBlank()) text.append("Источник не указан.\n");
      if (experiment.analyticalEvidence().entries().stream()
          .anyMatch(a -> id.equals(a.correctsId()))) {
        text.append("Выбрана историческая версия; существует последующее исправление.\n");
      }
    }
    text.append("\nРасхождения и проверка связанных записей\n");
    final var warnings = dossier(experiment, entry);
    if (warnings.isEmpty()) {
      text.append(
          "По выполненным проверкам расхождений не найдено. Это не автоматическое подтверждение идентичности продукта.\n");
    } else {
      new LinkedHashSet<>(warnings).forEach(w -> text.append("• ").append(w).append("\n"));
    }
    return text.toString();
  }

  public static String description(final Entry entry) {
    final StringBuilder out =
        new StringBuilder(entry.title())
            .append(" · ")
            .append(entry.data().method())
            .append("\n")
            .append(entry.molecule().displayName())
            .append("\n");
    switch (entry.data()) {
      case Correlations c -> {
        if (c.spectrum() != null) {
          out.append("Обработанный 2D спектр: ")
              .append(c.spectrum().sourceName())
              .append(" · ")
              .append(c.spectrum().format())
              .append(" · ")
              .append(c.spectrum().width())
              .append(" × ")
              .append(c.spectrum().height())
              .append("\nКонтрольные суммы: ")
              .append(c.spectrum().sourceHashes())
              .append("\nРегистрация: ")
              .append(c.spectrum().acquisition())
              .append("\n");
        }
        out.append("Оси: ")
            .append(c.xNucleus())
            .append(" / ")
            .append(c.yNucleus())
            .append(" · растворитель: ")
            .append(c.solvent())
            .append("\n");
        c.correlations()
            .forEach(
                p ->
                    out.append(format(p.xPpm()))
                        .append(" / ")
                        .append(format(p.yPpm()))
                        .append(" ppm · интенсивность ")
                        .append(format(p.intensity()))
                        .append(" · ")
                        .append(assignments(p.xAtoms()))
                        .append(" ↔ ")
                        .append(assignments(p.yAtoms()))
                        .append(" · ")
                        .append(p.note())
                        .append("\n"));
      }
      case Multiplets m -> {
        if (m.analysisId() != null) {
          out.append("Точная исходная запись 1D ЯМР: ").append(m.analysisId()).append("\n");
        }
        out.append("Частота: ").append(format(m.frequencyMHz())).append(" МГц\n");
        m.multiplets()
            .forEach(
                p ->
                    out.append("δ ")
                        .append(format(p.centerPpm()))
                        .append(" · ")
                        .append(p.label())
                        .append(" · интеграл ")
                        .append(format(p.integral()))
                        .append(" · J: ")
                        .append(
                            p.couplingsHz().stream()
                                .map(LaboratoryEvidenceService::format)
                                .toList())
                        .append(" Гц · линии δ: ")
                        .append(
                            p.linesPpm().stream().map(LaboratoryEvidenceService::format).toList())
                        .append(" ppm · расстояния линий: ")
                        .append(
                            lineSpacings(p, m.frequencyMHz()).stream()
                                .map(LaboratoryEvidenceService::format)
                                .toList())
                        .append(" Гц · ")
                        .append(assignments(p.atoms()))
                        .append(" · ")
                        .append(p.note())
                        .append("\n"));
      }
      case QuantitativeNmr q ->
          out.append("Массовая доля по qNMR: ")
              .append(format(quantitativeNmr(q).purityPercent()))
              .append("%\nОбразец: ")
              .append(q.sampleMass())
              .append(" · M ")
              .append(format(q.sampleMolarMass()))
              .append(" г/моль · интеграл ")
              .append(format(q.sampleIntegral()))
              .append(" · ядер ")
              .append(q.sampleNuclei())
              .append("\nСтандарт: ")
              .append(q.standardName())
              .append(" · ")
              .append(q.standardMass())
              .append(" · M ")
              .append(format(q.standardMolarMass()))
              .append(" г/моль · интеграл ")
              .append(format(q.standardIntegral()))
              .append(" · ядер ")
              .append(q.standardNuclei())
              .append(" · чистота ")
              .append(format(q.standardPurityPercent()))
              .append(
                  "%\nФормула: wA = IA/IS × NS/NA × MA/MS × mS/mA × wS. Массы приведены к одинаковым единицам.\n")
              .append(q.preparation())
              .append("\n");
      case Chromatogram c -> {
        final var areas = areaPercent(c);
        c.peaks()
            .forEach(
                p ->
                    out.append(p.label())
                        .append(" · tR ")
                        .append(format(p.retentionMin()))
                        .append(" мин · площадь ")
                        .append(format(p.area()))
                        .append(" · ")
                        .append(
                            p.included()
                                ? areas.containsKey(p.id())
                                    ? format(areas.get(p.id())) + "% площади"
                                    : "сумма площадей равна нулю"
                                : "исключён: " + p.exclusionReason())
                        .append("\n"));
        if (c.calibration() != null) {
          final var q = quantify(c);
          out.append("По калибровке: ")
              .append(q.mass())
              .append(" · R² ")
              .append(format(q.calibration().rSquared()))
              .append("\nПлощадь = ")
              .append(format(q.calibration().slopeAreaPerGram()))
              .append(" × масса в граммах + ")
              .append(format(q.calibration().intercept()))
              .append(" · невзвешенная линейная регрессия со свободным пересечением\nСтандарты: ")
              .append(c.calibration().points().size())
              .append(" · ")
              .append(c.calibration().source())
              .append("\n")
              .append(c.preparation())
              .append("\n");
          if (q.purityPercent() != null) {
            out.append("Масса представленной порции: ")
                .append(c.representedSampleMass())
                .append("\nМассовая доля по калибровке: ")
                .append(format(q.purityPercent()))
                .append("%\n");
          }
        }
        if (c.reportedPurityPercent() != null) {
          out.append("Отдельно сообщённая массовая доля: ")
              .append(format(c.reportedPurityPercent()))
              .append("% · ")
              .append(c.reportedPuritySource())
              .append("\n");
        }
      }
      case Dossier d -> {
        out.append("Заключение: ")
            .append(d.conclusion())
            .append("\n")
            .append(d.conclusionText())
            .append("\nДопуск МС: ")
            .append(format(d.massTolerancePpm()))
            .append(" ppm\n");
        d.findings()
            .forEach(
                f ->
                    out.append(f.resolved() ? "Разрешено · " : "Открыто · ")
                        .append(severityLabel(f.severity()))
                        .append(" · ")
                        .append(f.category())
                        .append(" · ")
                        .append(f.text())
                        .append("\n"));
      }
    }
    diagnostics(entry).forEach(w -> out.append("• ").append(w).append("\n"));
    if (!entry.source().isBlank()) {
      out.append("Источник: ").append(entry.source()).append("\n");
    }
    if (!entry.note().isBlank()) {
      out.append(entry.note());
    }
    return out.toString();
  }

  public static String severityLabel(final Severity severity) {
    return switch (severity) {
      case INFORMATION -> "Информация";
      case WARNING -> "Предупреждение";
      case DISCREPANCY -> "Расхождение";
    };
  }

  /** Old native/CSV keys remain accepted; localised imports are normalised to the same enum. */
  public static Severity severity(final String value) {
    return switch (value == null ? "" : value.strip().toUpperCase(Locale.ROOT)) {
      case "INFORMATION", "ИНФОРМАЦИЯ" -> Severity.INFORMATION;
      case "WARNING", "ПРЕДУПРЕЖДЕНИЕ" -> Severity.WARNING;
      case "DISCREPANCY", "РАСХОЖДЕНИЕ" -> Severity.DISCREPANCY;
      default ->
          throw new IllegalArgumentException(
              "Неверный уровень замечания. Выберите «Информация», «Предупреждение» или «Расхождение» в столбце «Уровень».");
    };
  }

  public static String format(final BigDecimal value) {
    return value == null
        ? "—"
        : value.round(new MathContext(8)).stripTrailingZeros().toPlainString();
  }

  public static String assignments(final Set<Assignment> values) {
    return String.join(
        ";",
        values.stream()
            .sorted(Comparator.comparing(a -> a.atomId().value()))
            .map(a -> (a.attachedProton() ? "H@" : "") + a.atomId().value())
            .toList());
  }

  public static Set<Assignment> assignments(final String value) {
    if (value.isBlank()) {
      return Set.of();
    }
    final Set<Assignment> result = new LinkedHashSet<>();
    for (final String token : value.split(";", -1)) {
      final String text = token.strip();
      final boolean h = text.startsWith("H@");
      if (!result.add(new Assignment(AtomId.of(h ? text.substring(2) : text), h))) {
        throw new IllegalArgumentException("Повтор назначения атома.");
      }
    }
    return Set.copyOf(result);
  }

  /** Own text files use an explicit header; imports do not guess a vendor format or column meaning. */
  public static List<List<String>> table(final String text, final List<String> header) {
    if (text.length() > 16000000) {
      throw new IllegalArgumentException("Текстовая таблица превышает 16 миллионов символов.");
    }
    final String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    if (lines.length == 0) {
      throw new IllegalArgumentException("Пустая таблица.");
    }
    final char delimiter = lines[0].contains("\t") ? '\t' : ',';
    final var actual = columns(lines[0].replace("\uFEFF", ""), delimiter);
    if (!actual.equals(header)) {
      throw new IllegalArgumentException("Ожидаемые столбцы: " + String.join(" · ", header));
    }
    final List<List<String>> result = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) {
      if (lines[i].isBlank()) {
        continue;
      }
      final var row = columns(lines[i], delimiter);
      if (row.size() != header.size()) {
        throw new IllegalArgumentException("Неверное число столбцов в строке " + (i + 1) + ".");
      }
      result.add(row);
      if (result.size() > 262144) {
        throw new IllegalArgumentException("Слишком много строк таблицы.");
      }
    }
    return List.copyOf(result);
  }

  private static List<String> columns(final String line, final char delimiter) {
    final List<String> result = new ArrayList<>();
    final StringBuilder field = new StringBuilder();
    boolean quoted = false, ended = false;
    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);
      if (c == '"') {
        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          field.append('"');
          i++;
        } else if (quoted) {
          quoted = false;
          ended = true;
        } else if (field.isEmpty() && !ended) {
          quoted = true;
        } else {
          throw new IllegalArgumentException("Некорректные кавычки CSV.");
        }
      } else if (c == delimiter && !quoted) {
        result.add(field.toString().strip());
        field.setLength(0);
        ended = false;
      } else {
        if (ended && !Character.isWhitespace(c)) {
          throw new IllegalArgumentException("Текст после закрывающей кавычки CSV.");
        }
        if (!ended) {
          field.append(c);
        }
      }
    }
    if (quoted) {
      throw new IllegalArgumentException("Многострочные кавычки в таблице не поддерживаются.");
    }
    result.add(field.toString().strip());
    return List.copyOf(result);
  }
}
