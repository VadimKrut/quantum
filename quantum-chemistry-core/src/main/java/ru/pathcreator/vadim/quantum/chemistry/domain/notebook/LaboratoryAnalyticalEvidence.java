/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package ru.pathcreator.vadim.quantum.chemistry.domain.notebook;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import ru.pathcreator.vadim.quantum.chemistry.domain.common.IdentifierValue;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.AtomId;
import ru.pathcreator.vadim.quantum.chemistry.domain.structure.Molecule;
import ru.pathcreator.vadim.quantum.chemistry.domain.identity.MolecularIdentity;
import ru.pathcreator.vadim.quantum.chemistry.domain.spectroscopy.NmrSpectrum2D;

/** Explicit advanced analytical observations and decisions with immutable structure snapshots. */
public record LaboratoryAnalyticalEvidence(List<Entry> entries) {

  public static final LaboratoryAnalyticalEvidence EMPTY =
      new LaboratoryAnalyticalEvidence(List.of());

  public enum Experiment2D {
    HSQC,
    HMBC,
    COSY,
    NOESY,
    ROESY
  }

  public enum Severity {
    INFORMATION,
    WARNING,
    DISCREPANCY
  }

  public enum Conclusion {
    PENDING("Заключение ещё не принято"),
    ACCEPTED("Соответствие подтверждено пользователем"),
    REJECTED("Соответствие отклонено пользователем");
    private final String label;

    Conclusion(String label) {
      this.label = label;
    }

    public String toString() {
      return this.label;
    }
  }

  public sealed interface Data
      permits Correlations, Multiplets, QuantitativeNmr, Chromatogram, Dossier {
    String method();
  }

  /** attachedProton means a proton assigned to the indicated heavy atom, not an explicit atom in the graph. */
  public record Assignment(AtomId atomId, boolean attachedProton) {
    public Assignment {
      Objects.requireNonNull(atomId);
    }
  }

  public record Correlation(
      String id,
      BigDecimal xPpm,
      BigDecimal yPpm,
      BigDecimal intensity,
      Set<Assignment> xAtoms,
      Set<Assignment> yAtoms,
      String note) {
    public Correlation {
      id = identifier(id);
      xPpm = number(xPpm);
      yPpm = number(yPpm);
      intensity = optional(intensity);
      xAtoms = Set.copyOf(xAtoms);
      yAtoms = Set.copyOf(yAtoms);
      note = text(note, 2000);
    }
  }

  public record Correlations(
      Experiment2D experiment,
      LaboratoryAnalysis.Nucleus xNucleus,
      LaboratoryAnalysis.Nucleus yNucleus,
      String solvent,
      List<Correlation> correlations,
      NmrSpectrum2D spectrum)
      implements Data {
    public Correlations(
        final Experiment2D experiment,
        final LaboratoryAnalysis.Nucleus xNucleus,
        final LaboratoryAnalysis.Nucleus yNucleus,
        final String solvent,
        final List<Correlation> correlations) {
      this(experiment, xNucleus, yNucleus, solvent, correlations, null);
    }

    public Correlations {
      if (spectrum != null
          && (spectrum.xNucleus() != xNucleus || spectrum.yNucleus() != yNucleus)) {
        throw new IllegalArgumentException(
            "Ядра осей должны соответствовать исходному 2D спектру.");
      }
      Objects.requireNonNull(experiment);
      Objects.requireNonNull(xNucleus);
      Objects.requireNonNull(yNucleus);
      solvent = text(solvent, 500);
      correlations = List.copyOf(correlations);
      rows(correlations.size());
      unique(correlations.stream().map(Correlation::id).toList());
      if (xNucleus != LaboratoryAnalysis.Nucleus.H1
          || (experiment == Experiment2D.COSY
                  || experiment == Experiment2D.NOESY
                  || experiment == Experiment2D.ROESY)
              && yNucleus != LaboratoryAnalysis.Nucleus.H1
          || (experiment == Experiment2D.HSQC || experiment == Experiment2D.HMBC)
              && yNucleus != LaboratoryAnalysis.Nucleus.C13
              && yNucleus != LaboratoryAnalysis.Nucleus.N15) {
        throw new IllegalArgumentException(
            "Для COSY/NOESY/ROESY задайте оси ¹H/¹H, для HSQC/HMBC — ¹H/¹³C или ¹H/¹⁵N.");
      }
    }

    public String method() {
      return this.experiment + " · корреляции 2D";
    }
  }

  public record Multiplet(
      String id,
      BigDecimal centerPpm,
      BigDecimal integral,
      List<BigDecimal> linesPpm,
      List<BigDecimal> couplingsHz,
      Set<Assignment> atoms,
      String label,
      String note) {
    public Multiplet {
      id = identifier(id);
      centerPpm = number(centerPpm);
      integral = integral == null ? null : nonnegative(integral);
      linesPpm = linesPpm.stream().map(LaboratoryAnalyticalEvidence::number).toList();
      couplingsHz = couplingsHz.stream().map(LaboratoryAnalyticalEvidence::positive).toList();
      atoms = Set.copyOf(atoms);
      label = text(label, 100);
      note = text(note, 2000);
      if (linesPpm.size() > 1000 || couplingsHz.size() > 100) {
        throw new IllegalArgumentException("Слишком много линий или констант мультиплета.");
      }
    }
  }

  public record Multiplets(
      LaboratoryAnalysis.Nucleus nucleus,
      BigDecimal frequencyMHz,
      List<Multiplet> multiplets,
      String analysisId)
      implements Data {
    public Multiplets(
        final LaboratoryAnalysis.Nucleus nucleus,
        final BigDecimal frequencyMHz,
        final List<Multiplet> multiplets) {
      this(nucleus, frequencyMHz, multiplets, null);
    }

    public Multiplets {
      analysisId = analysisId == null ? null : identifier(analysisId);
      Objects.requireNonNull(nucleus);
      frequencyMHz = positive(frequencyMHz);
      multiplets = List.copyOf(multiplets);
      rows(multiplets.size());
      unique(multiplets.stream().map(Multiplet::id).toList());
    }

    public String method() {
      return this.nucleus + " · мультиплеты и J";
    }
  }

  public record QuantitativeNmr(
      String standardName,
      BigDecimal sampleIntegral,
      int sampleNuclei,
      BigDecimal sampleMolarMass,
      LaboratoryQuantity sampleMass,
      BigDecimal standardIntegral,
      int standardNuclei,
      BigDecimal standardMolarMass,
      LaboratoryQuantity standardMass,
      BigDecimal standardPurityPercent,
      String preparation)
      implements Data {
    public QuantitativeNmr {
      standardName = required(standardName, 500);
      sampleIntegral = positive(sampleIntegral);
      standardIntegral = positive(standardIntegral);
      sampleMolarMass = positive(sampleMolarMass);
      standardMolarMass = positive(standardMolarMass);
      mass(sampleMass);
      mass(standardMass);
      standardPurityPercent = percent(standardPurityPercent, false);
      preparation = required(preparation, 8000);
      if (sampleNuclei < 1
          || sampleNuclei > 10000
          || standardNuclei < 1
          || standardNuclei > 10000) {
        throw new IllegalArgumentException(
            "Число резонирующих ядер в каждом интеграле должно быть от 1 до 10000.");
      }
    }

    public String method() {
      return "qNMR · внутренний стандарт";
    }
  }

  public record ChromPoint(BigDecimal timeMin, BigDecimal response) {
    public ChromPoint {
      timeMin = nonnegative(timeMin);
      response = number(response);
    }
  }

  public record ChromPeak(
      String id,
      String label,
      BigDecimal retentionMin,
      BigDecimal fromMin,
      BigDecimal toMin,
      BigDecimal area,
      boolean included,
      String exclusionReason) {
    public ChromPeak {
      id = identifier(id);
      label = required(label, 500);
      retentionMin = nonnegative(retentionMin);
      fromMin = nonnegative(fromMin);
      toMin = nonnegative(toMin);
      area = nonnegative(area);
      exclusionReason = text(exclusionReason, 2000);
      if (fromMin.compareTo(retentionMin) > 0
          || retentionMin.compareTo(toMin) > 0
          || fromMin.compareTo(toMin) >= 0) {
        throw new IllegalArgumentException(
            "Границы пика должны охватывать его время удерживания и иметь ненулевую ширину.");
      }
      if (!included && exclusionReason.isBlank()) {
        throw new IllegalArgumentException("Укажите причину исключения пика из суммы площадей.");
      }
    }
  }

  public record CalibrationPoint(LaboratoryQuantity mass, BigDecimal area) {
    public CalibrationPoint {
      Objects.requireNonNull(mass);
      nonnegative(mass.value());
      if (!mass.unit().dimension().equals("mass")) {
        throw new IllegalArgumentException("Калибровочная точка требует единицу массы.");
      }
      area = nonnegative(area);
    }
  }

  public record MassCalibration(List<CalibrationPoint> points, String source) {
    public MassCalibration {
      points = List.copyOf(points);
      source = required(source, 4000);
      if (points.size() < 2
          || points.size() > 1000
          || points.stream().map(p -> p.mass().baseValue().stripTrailingZeros()).distinct().count()
              < 2) {
        throw new IllegalArgumentException(
            "Для линейной калибровки нужны минимум две разные массы стандарта.");
      }
    }
  }

  public record Chromatogram(
      String technique,
      String detector,
      List<ChromPoint> trace,
      List<ChromPeak> peaks,
      MassCalibration calibration,
      String quantifiedPeakId,
      LaboratoryQuantity representedSampleMass,
      BigDecimal reportedPurityPercent,
      String reportedPuritySource,
      String preparation)
      implements Data {
    public Chromatogram {
      technique = required(technique, 500);
      detector = required(detector, 500);
      trace = List.copyOf(trace);
      peaks = List.copyOf(peaks);
      rows(peaks.size());
      unique(peaks.stream().map(ChromPeak::id).toList());
      if (trace.size() > 262144 || trace.size() == 1) {
        throw new IllegalArgumentException("Хроматограмма содержит 0 или от 2 до 262144 точек.");
      }
      BigDecimal previous = null;
      for (final var point : trace) {
        if (previous != null && previous.compareTo(point.timeMin()) >= 0) {
          throw new IllegalArgumentException("Время хроматограммы должно строго возрастать.");
        }
        previous = point.timeMin();
      }
      if (quantifiedPeakId != null) {
        quantifiedPeakId = identifier(quantifiedPeakId);
        final String selected = quantifiedPeakId;
        if (peaks.stream().noneMatch(p -> p.id().equals(selected))) {
          throw new IllegalArgumentException("Количественно определяемый пик отсутствует.");
        }
      }
      if (calibration != null && quantifiedPeakId == null) {
        throw new IllegalArgumentException("Выберите один пик, для которого построена калибровка.");
      }
      if (representedSampleMass != null) {
        mass(representedSampleMass);
        if (calibration == null) {
          throw new IllegalArgumentException(
              "Масса анализируемой порции требует калибровки выбранного пика.");
        }
      }
      reportedPurityPercent =
          reportedPurityPercent == null ? null : percent(reportedPurityPercent, true);
      reportedPuritySource = text(reportedPuritySource, 4000);
      preparation = text(preparation, 8000);
      if (reportedPurityPercent != null && reportedPuritySource.isBlank()) {
        throw new IllegalArgumentException(
            "Для отдельно сообщённой массовой доли укажите источник и метод.");
      }
      if (calibration != null && preparation.isBlank()) {
        throw new IllegalArgumentException(
            "Опишите подготовку порции и основание единиц калибровки.");
      }
    }

    public String method() {
      return this.technique + " · хроматография";
    }
  }

  public record Finding(String category, Severity severity, String text, boolean resolved) {
    public Finding {
      category = required(category, 500);
      Objects.requireNonNull(severity);
      text = required(text, 8000);
    }
  }

  public record Dossier(
      List<String> analysisIds,
      List<String> evidenceIds,
      BigDecimal massTolerancePpm,
      List<Finding> findings,
      Conclusion conclusion,
      String conclusionText)
      implements Data {
    public Dossier {
      analysisIds = ids(analysisIds);
      evidenceIds = ids(evidenceIds);
      massTolerancePpm = positive(massTolerancePpm);
      findings = List.copyOf(findings);
      rows(findings.size());
      Objects.requireNonNull(conclusion);
      conclusionText = text(conclusionText, 10000);
      if (conclusion != Conclusion.PENDING && conclusionText.isBlank()) {
        throw new IllegalArgumentException("Зафиксируйте основание своего заключения о продукте.");
      }
    }

    public String method() {
      return "Подтверждение продукта";
    }
  }

  public record Entry(
      String id,
      String componentId,
      String sampleId,
      String title,
      Instant recordedAt,
      LaboratoryAnalysis.Origin origin,
      String source,
      String note,
      Molecule molecule,
      List<String> attachmentIds,
      String correctsId,
      Data data) {
    public Entry {
      id = identifier(id);
      componentId = identifier(componentId);
      sampleId = sampleId == null ? null : identifier(sampleId);
      title = required(title, 500);
      Objects.requireNonNull(recordedAt);
      Objects.requireNonNull(origin);
      source = text(source, 8000);
      note = text(note, 10000);
      Objects.requireNonNull(molecule);
      attachmentIds = ids(attachmentIds);
      correctsId = correctsId == null ? null : identifier(correctsId);
      Objects.requireNonNull(data);
      if (origin != LaboratoryAnalysis.Origin.MEASURED && source.isBlank()) {
        throw new IllegalArgumentException(
            "Укажите литературный источник или метод моделирования.");
      }
      if (data instanceof Correlations value) {
        for (final var row : value.correlations()) {
          assignments(molecule, row.xAtoms(), value.xNucleus());
          assignments(molecule, row.yAtoms(), value.yNucleus());
        }
      }
      if (data instanceof Multiplets value) {
        for (final var row : value.multiplets()) {
          assignments(molecule, row.atoms(), value.nucleus());
        }
      }
    }
  }

  public LaboratoryAnalyticalEvidence {
    entries = List.copyOf(entries);
    if (entries.size() > 2000) {
      throw new IllegalArgumentException(
          "Не более 2000 дополнительных аналитических записей в опыте.");
    }
    final Map<String, Entry> known = new LinkedHashMap<>();
    final Set<String> corrected = new HashSet<>();
    long points = 0, characters = 0, cells2D = 0;
    final Set<NmrSpectrum2D> spectra2D = new HashSet<>();
    for (final var entry : entries) {
      if (known.containsKey(entry.id())) {
        throw new IllegalArgumentException("Повтор идентификатора аналитической записи.");
      }
      if (entry.correctsId() != null) {
        final var old = known.get(entry.correctsId());
        if (old == null
            || !corrected.add(old.id())
            || !old.componentId().equals(entry.componentId())
            || !Objects.equals(old.sampleId(), entry.sampleId())
            || old.data().getClass() != entry.data().getClass()
            || !LaboratoryMaterialProperties.signature(old.molecule())
                .equals(LaboratoryMaterialProperties.signature(entry.molecule()))
            || entry.recordedAt().isBefore(old.recordedAt())) {
          throw new IllegalArgumentException(
              "Исправление должно продолжать последнюю запись того же анализа, образца и структуры.");
        }
      }
      if (entry.data() instanceof Dossier dossier) {
        for (final String id : dossier.evidenceIds()) {
          final var related = known.get(id);
          if (related == null
              || related.data() instanceof Dossier
              || !related.componentId().equals(entry.componentId())
              || !Objects.equals(related.sampleId(), entry.sampleId())) {
            throw new IllegalArgumentException(
                "Досье ссылается на отсутствующий анализ или другой образец.");
          }
        }
      }
      if (entry.data() instanceof Correlations c
          && c.spectrum() != null
          && spectra2D.add(c.spectrum())) {
        cells2D += c.spectrum().values().size();
        characters +=
            c.spectrum().format().length()
                + c.spectrum().sourceName().length()
                + c.spectrum().acquisition().length();
        if (cells2D > NmrSpectrum2D.MAX_CELLS) {
          throw new IllegalArgumentException(
              "В опыте не более 4 194 304 точек уникальных 2D спектров, включая историю.");
        }
      }
      points += entry.data() instanceof Chromatogram c ? c.trace().size() : 0;
      characters += entry.title().length() + entry.source().length() + entry.note().length();
      if (points > 524288 || characters > 10000000) {
        throw new IllegalArgumentException("Превышен общий объём аналитических данных опыта.");
      }
      known.put(entry.id(), entry);
    }
  }

  public List<Entry> latest() {
    final Set<String> corrected = new HashSet<>();
    this.entries.forEach(
        e -> {
          if (e.correctsId() != null) {
            corrected.add(e.correctsId());
          }
        });
    return this.entries.stream().filter(e -> !corrected.contains(e.id())).toList();
  }

  public LaboratoryAnalyticalEvidence append(final Entry entry) {
    final var values = new ArrayList<>(this.entries);
    values.add(entry);
    return new LaboratoryAnalyticalEvidence(values);
  }

  public static void requireAppend(
      final LaboratoryAnalyticalEvidence previous, final LaboratoryAnalyticalEvidence next) {
    if (next.entries.size() < previous.entries.size()
        || !next.entries.subList(0, previous.entries.size()).equals(previous.entries)) {
      throw new IllegalArgumentException(
          "Аналитическую историю нельзя удалять или менять: добавьте исправление.");
    }
  }

  public static LaboratoryAnalyticalEvidence merge(
      final LaboratoryAnalyticalEvidence previous, final LaboratoryAnalyticalEvidence current) {
    final var result = new LinkedHashMap<String, Entry>();
    previous.entries.forEach(e -> result.put(e.id(), e));
    for (final var e : current.entries) {
      final var old = result.putIfAbsent(e.id(), e);
      if (old != null && !old.equals(e)) {
        throw new IllegalArgumentException("Версия содержит изменённую аналитическую запись.");
      }
    }
    return new LaboratoryAnalyticalEvidence(
        result.values().stream().sorted(Comparator.comparing(Entry::recordedAt)).toList());
  }

  public void validateLinks(
      final List<LaboratorySample> samples,
      final List<LaboratoryAnalysis> analyses,
      final List<LaboratoryAttachment> attachments) {
    final Set<String> assets = new HashSet<>();
    attachments.forEach(a -> assets.add(a.id()));
    for (final var entry : this.entries) {
      if (!assets.containsAll(entry.attachmentIds())) {
        throw new IllegalArgumentException(
            "Аналитическая запись ссылается на отсутствующее вложение.");
      }
      if (entry.sampleId() != null) {
        final var sample =
            samples.stream()
                .filter(s -> s.id().equals(entry.sampleId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException("Отсутствует образец аналитической записи."));
        if (!sample.componentId().equals(entry.componentId())
            || !LaboratoryMaterialProperties.signature(sample.molecule())
                .equals(LaboratoryMaterialProperties.signature(entry.molecule()))) {
          throw new IllegalArgumentException(
              "Аналитическая структура не соответствует выбранному образцу.");
        }
      }
      if (entry.data() instanceof Multiplets multiplets && multiplets.analysisId() != null) {
        final var analysis =
            analyses.stream()
                .filter(a -> a.id().equals(multiplets.analysisId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Исходный ЯМР-анализ мультиплетов отсутствует."));
        if (!(analysis.data() instanceof LaboratoryAnalysis.Nmr nmr) || nmr.trace() == null) {
          throw new IllegalArgumentException(
              "Связанные мультиплеты требуют сохранённый ЯМР-анализ с исходной трассой.");
        }
        if (!analysis.componentId().equals(entry.componentId())
            || !Objects.equals(analysis.sampleId(), entry.sampleId())
            || !MolecularIdentity.signature(analysis.molecule())
                .equals(MolecularIdentity.signature(entry.molecule()))) {
          throw new IllegalArgumentException(
              "Исходный ЯМР-анализ относится к другому компоненту, образцу или снимку структуры.");
        }
        if (nmr.nucleus() != multiplets.nucleus()
            || nmr.frequencyMHz() == null
            || nmr.frequencyMHz().compareTo(multiplets.frequencyMHz()) != 0) {
          throw new IllegalArgumentException(
              "Ядро и частота мультиплетов должны совпадать с исходным ЯМР-анализом.");
        }
        if (analysis.origin() != entry.origin()) {
          throw new IllegalArgumentException(
              "Происхождение мультиплетов должно совпадать с происхождением исходного ЯМР-анализа.");
        }
      }
      if (entry.data() instanceof Dossier dossier) {
        for (final String id : dossier.analysisIds()) {
          final var analysis =
              analyses.stream()
                  .filter(a -> a.id().equals(id))
                  .findFirst()
                  .orElseThrow(
                      () -> new IllegalArgumentException("В досье отсутствует исходный анализ."));
          if (!analysis.componentId().equals(entry.componentId())
              || !Objects.equals(analysis.sampleId(), entry.sampleId())) {
            throw new IllegalArgumentException("Досье содержит анализ другого образца.");
          }
        }
      }
    }
  }

  private static void assignments(
      final Molecule molecule,
      final Set<Assignment> assignments,
      final LaboratoryAnalysis.Nucleus nucleus) {
    if (assignments.size() > 1000) {
      throw new IllegalArgumentException("Слишком много назначений одного сигнала.");
    }
    for (final var assignment : assignments) {
      final var atom =
          molecule.atoms().stream()
              .filter(a -> a.id().equals(assignment.atomId()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Назначенный атом отсутствует в сохранённой структуре."));
      if (assignment.attachedProton()
          ? nucleus != LaboratoryAnalysis.Nucleus.H1 || atom.element().atomicNumber() == 1
          : atom.element().atomicNumber() != nucleus.element()
              || atom.isotope() != null && atom.isotope().massNumber() != nucleus.isotope()) {
        throw new IllegalArgumentException("Назначение атома не соответствует ядру выбранной оси.");
      }
    }
  }

  private static void rows(final int count) {
    if (count > 10000) {
      throw new IllegalArgumentException("Не более 10000 строк в одном анализе.");
    }
  }

  private static List<String> ids(final List<String> values) {
    final var result = values.stream().map(LaboratoryAnalyticalEvidence::identifier).toList();
    rows(result.size());
    unique(result);
    return result;
  }

  private static void unique(final List<String> values) {
    if (new HashSet<>(values).size() != values.size()) {
      throw new IllegalArgumentException("Повтор идентификатора строки или ссылки.");
    }
  }

  private static String identifier(final String value) {
    final String id = IdentifierValue.requireIdentifier(value, "Analytical evidence identifier");
    if (id.length() > 200) {
      throw new IllegalArgumentException("Слишком длинный идентификатор анализа.");
    }
    return id;
  }

  private static String text(final String value, final int max) {
    Objects.requireNonNull(value);
    if (value.length() > max) {
      throw new IllegalArgumentException("Слишком длинное поле аналитической записи.");
    }
    return value;
  }

  private static String required(final String value, final int max) {
    final String result = text(value, max).strip();
    if (result.isBlank()) {
      throw new IllegalArgumentException("Заполните обязательное текстовое поле анализа.");
    }
    return result;
  }

  private static BigDecimal number(final BigDecimal value) {
    Objects.requireNonNull(value);
    if (value.precision() > 40 || Math.abs((long) value.scale()) > 40) {
      throw new IllegalArgumentException("Число анализа вне поддержанной точности.");
    }
    return value.stripTrailingZeros();
  }

  private static BigDecimal optional(final BigDecimal value) {
    return value == null ? null : number(value);
  }

  private static BigDecimal nonnegative(final BigDecimal value) {
    final var n = number(value);
    if (n.signum() < 0) {
      throw new IllegalArgumentException("Значение не может быть отрицательным.");
    }
    return n;
  }

  private static BigDecimal positive(final BigDecimal value) {
    final var n = number(value);
    if (n.signum() <= 0) {
      throw new IllegalArgumentException("Значение должно быть положительным.");
    }
    return n;
  }

  private static BigDecimal percent(final BigDecimal value, final boolean zero) {
    final var n = zero ? nonnegative(value) : positive(value);
    if (n.compareTo(new BigDecimal("100")) > 0) {
      throw new IllegalArgumentException("Массовая доля должна быть не больше 100%.");
    }
    return n;
  }

  private static void mass(final LaboratoryQuantity value) {
    Objects.requireNonNull(value);
    positive(value.value());
    if (!value.unit().dimension().equals("mass")) {
      throw new IllegalArgumentException(
          "Введите массу с единицей массы; перевод из объёма автоматически не выполняется.");
    }
  }
}
