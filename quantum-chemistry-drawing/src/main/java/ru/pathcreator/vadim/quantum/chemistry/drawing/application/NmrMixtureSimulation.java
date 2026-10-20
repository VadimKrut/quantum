/*
 * Copyright 2026 Vadim Aleksandrovich Zaletaev
 * SPDX-License-Identifier: MPL-2.0
 */
package ru.pathcreator.vadim.quantum.chemistry.drawing.application;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Broadens explicitly assigned shifts; never predicts shifts, coupling, solvent signals or multiplets. */
public final class NmrMixtureSimulation {
  public static final int MAX_POINTS = 32768, MAX_COMPONENTS = 16, MAX_LINES = 2048;
  public static final String MODEL = "lorentzian-bin-average-v1";

  private NmrMixtureSimulation() {}

  /** One atom's assigned shift and nonnegative relative integral. Coincident shifts sum naturally. */
  public record Line(String atomId, double ppm, double integral) {
    public Line {
      atomId = text(atomId, "Идентификатор атома", 256);
      finite(ppm, "Сдвиг");
      nonnegative(integral, "Интеграл");
    }
  }

  /** Amounts are molar parts, not mass fractions or measured concentrations. */
  public record Component(String id, String name, double molarParts, List<Line> lines) {
    public Component {
      id = text(id, "Идентификатор компонента", 256);
      name = text(name, "Название компонента", 2048);
      nonnegative(molarParts, "Мольные части");
      lines = List.copyOf(lines);
      if (lines.size() > MAX_LINES)
        throw new IllegalArgumentException("Не более " + MAX_LINES + " атомных линий.");
      final Set<String> atoms = new HashSet<>();
      double sum = 0;
      for (final Line line : lines) {
        if (!atoms.add(line.atomId()))
          throw new IllegalArgumentException("Атом указан дважды в компоненте: " + line.atomId());
        sum += line.integral();
        finite(sum, "Сумма интегралов");
      }
    }
  }

  /** Bin-averaged density per ppm. Each bin is centred at firstPpm+i*stepPpm and has width |stepPpm|. */
  public record Trace(double firstPpm, double stepPpm, List<Double> intensity) {
    public Trace {
      finite(firstPpm, "Начало оси");
      finite(stepPpm, "Шаг оси");
      if (stepPpm == 0) throw new IllegalArgumentException("Шаг оси не может быть нулевым.");
      intensity = List.copyOf(intensity);
      if (intensity.size() < 2 || intensity.size() > MAX_POINTS)
        throw new IllegalArgumentException("Сетка должна содержать 2–" + MAX_POINTS + " точек.");
      finite(firstPpm + stepPpm * (intensity.size() - 1), "Конец оси");
      for (final Double value : intensity) {
        if (value == null) throw new IllegalArgumentException("Пустая интенсивность.");
        nonnegative(value, "Интенсивность");
      }
    }

    public double ppm(final int index) {
      Objects.checkIndex(index, this.intensity.size());
      return this.firstPpm + index * this.stepPpm;
    }

    /** Rectangle integration is exact for this bin-average representation, up to floating-point rounding. */
    public double area() {
      double sum = 0, compensation = 0;
      for (final double value : this.intensity) {
        final double term = value * Math.abs(this.stepPpm) - compensation;
        final double next = sum + term;
        compensation = (next - sum) - term;
        sum = next;
      }
      finite(sum, "Площадь трассы");
      return sum;
    }
  }

  /** Pure uses one mole of that component; contribution uses its normalized mole fraction. Same axis and y scale. */
  public record ComponentTrace(
      String id, String name, double moleFraction, Trace pure, Trace contribution) {
    public ComponentTrace {
      id = text(id, "Идентификатор компонента", 256);
      name = text(name, "Название компонента", 2048);
      nonnegative(moleFraction, "Мольная доля");
      if (moleFraction > 1) throw new IllegalArgumentException("Мольная доля больше единицы.");
      Objects.requireNonNull(pure);
      Objects.requireNonNull(contribution);
    }
  }

  public record Result(
      List<ComponentTrace> components,
      Trace mixture,
      double observedMHz,
      double widthHz,
      List<String> diagnostics) {
    public Result {
      components = List.copyOf(components);
      Objects.requireNonNull(mixture);
      positive(observedMHz, "Частота наблюдаемого ядра");
      positive(widthHz, "Ширина линии");
      diagnostics = List.copyOf(diagnostics);
    }

    public double lineWidthPpm() {
      return this.widthHz / this.observedMHz;
    }
  }

  public static Result simulate(
      final List<Component> components,
      final double observedMHz,
      final double widthHz,
      final double firstPpm,
      final double lastPpm,
      final int points) {
    return simulate(components, observedMHz, widthHz, firstPpm, lastPpm, points, () -> false);
  }

  public static Result simulate(
      final List<Component> input,
      final double observedMHz,
      final double widthHz,
      final double firstPpm,
      final double lastPpm,
      final int points,
      final BooleanSupplier cancelled) {
    final List<Component> components = List.copyOf(input);
    Objects.requireNonNull(cancelled);
    checkCancelled(cancelled);
    if (components.isEmpty() || components.size() > MAX_COMPONENTS)
      throw new IllegalArgumentException("Выберите 1–" + MAX_COMPONENTS + " компонентов.");
    positive(observedMHz, "Частота наблюдаемого ядра, МГц");
    positive(widthHz, "Ширина линии FWHM, Гц");
    finite(firstPpm, "Начало оси");
    finite(lastPpm, "Конец оси");
    if (points < 2 || points > MAX_POINTS)
      throw new IllegalArgumentException("Сетка должна содержать 2–" + MAX_POINTS + " точек.");
    final double step = (lastPpm - firstPpm) / (points - 1),
        bin = Math.abs(step),
        fwhm = widthHz / observedMHz,
        gamma = fwhm / 2;
    positive(bin, "Ширина бина");
    positive(gamma, "Полуширина линии в ppm");
    final double lower = Math.min(firstPpm, lastPpm) - bin / 2,
        upper = Math.max(firstPpm, lastPpm) + bin / 2;
    finite(lower, "Нижняя граница бинов");
    finite(upper, "Верхняя граница бинов");
    if (firstPpm + step == firstPpm
        || lastPpm - step == lastPpm
        || lower == Math.min(firstPpm, lastPpm)
        || upper == Math.max(firstPpm, lastPpm))
      throw new IllegalArgumentException("Диапазон слишком мал для численной точности оси.");
    final Set<String> identifiers = new HashSet<>();
    int lines = 0;
    double maximumParts = 0;
    for (final Component component : components) {
      if (!identifiers.add(component.id()))
        throw new IllegalArgumentException("Компонент указан дважды: " + component.id());
      lines += component.lines().size();
      maximumParts = Math.max(maximumParts, component.molarParts());
    }
    if (lines > MAX_LINES)
      throw new IllegalArgumentException("Суммарно не более " + MAX_LINES + " атомных линий.");
    if (maximumParts == 0)
      throw new IllegalArgumentException(
          "Хотя бы один компонент должен иметь положительные мольные части.");
    double scaledSum = 0;
    for (final Component component : components) scaledSum += component.molarParts() / maximumParts;
    final double[] mixture = new double[points];
    final List<ComponentTrace> rendered = new ArrayList<>();
    final List<String> diagnostics = new ArrayList<>();
    diagnostics.add(
        "Уширенная диаграмма заданных сдвигов: средняя плотность Лоренца в бинах; J, мультиплеты и дополнительные сигналы не добавляются.");
    diagnostics.add(
        "Интенсивности задаются введёнными относительными интегралами, а не предсказываются из структуры или условий измерения.");
    diagnostics.add(
        "Чистые компоненты показаны на один моль; вклад каждого умножен на его нормированную мольную долю. Площади за пределами оси не перенормируются.");
    if (bin > fwhm / 8)
      diagnostics.add(
          "Шаг сетки крупнее 1/8 ширины FWHM. Площади сохранены в пределах бинов, но форма узкой линии разрешена недостаточно; сузьте диапазон или увеличьте число точек.");
    for (final Component component : components) {
      checkCancelled(cancelled);
      final double fraction = (component.molarParts() / maximumParts) / scaledSum;
      final double[] pure = new double[points];
      double expectedArea = 0;
      for (final Line line : component.lines()) {
        checkCancelled(cancelled);
        expectedArea += line.integral();
        if (line.integral() == 0) continue;
        for (int i = 0; i < points; i++) {
          if ((i & 511) == 0) checkCancelled(cancelled);
          final double centre = firstPpm + i * step;
          final double probability =
              binProbability(centre - bin / 2, centre + bin / 2, line.ppm(), gamma);
          final double density = (probability * line.integral()) / bin;
          finite(density, "Плотность линии");
          pure[i] += density;
          finite(pure[i], "Суммарная плотность компонента");
        }
      }
      final List<Double> pureValues = new ArrayList<>(points),
          contributionValues = new ArrayList<>(points);
      for (int i = 0; i < points; i++) {
        final double contribution = pure[i] * fraction;
        finite(contribution, "Вклад компонента");
        mixture[i] += contribution;
        finite(mixture[i], "Интенсивность смеси");
        pureValues.add(pure[i]);
        contributionValues.add(contribution);
      }
      final Trace pureTrace = new Trace(firstPpm, step, pureValues),
          contributionTrace = new Trace(firstPpm, step, contributionValues);
      final double capturedArea = pureTrace.area();
      if (expectedArea > 0 && capturedArea < expectedArea * 0.999)
        diagnostics.add(
            "«"
                + component.name()
                + "»: часть площади линий находится вне выбранного диапазона; интеграл в окне меньше заданного.");
      if (expectedArea == 0)
        diagnostics.add(
            "«"
                + component.name()
                + "»: положительных интегралов нет; диаграмма этого компонента нулевая.");
      rendered.add(
          new ComponentTrace(
              component.id(), component.name(), fraction, pureTrace, contributionTrace));
    }
    final List<Double> mixtureValues = new ArrayList<>(points);
    for (final double value : mixture) mixtureValues.add(value);
    return new Result(
        rendered, new Trace(firstPpm, step, mixtureValues), observedMHz, widthHz, diagnostics);
  }

  private static double binProbability(
      final double lower, final double upper, final double centre, final double gamma) {
    final double low = (lower - centre) / gamma, high = (upper - centre) / gamma;
    // atan difference in a stable positive-angle form when representable; direct atan handles
    // extreme tails.
    final double numerator = high - low, denominator = 1 + low * high;
    final double angle =
        Double.isFinite(numerator) && Double.isFinite(denominator)
            ? Math.atan2(numerator, denominator)
            : Math.atan(high) - Math.atan(low);
    return Math.max(0, Math.min(1, angle / Math.PI));
  }

  private static void checkCancelled(final BooleanSupplier cancelled) {
    if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean())
      throw new CancellationException("Построение диаграммы ЯМР отменено.");
  }

  private static String text(final String value, final String name, final int maximum) {
    if (value == null || value.isBlank() || value.length() > maximum)
      throw new IllegalArgumentException(
          name + ": требуется непустой текст до " + maximum + " символов.");
    return value;
  }

  private static void finite(final double value, final String name) {
    if (!Double.isFinite(value))
      throw new IllegalArgumentException(name + ": требуется конечное число.");
  }

  private static void nonnegative(final double value, final String name) {
    finite(value, name);
    if (value < 0) throw new IllegalArgumentException(name + " не может быть отрицательным.");
  }

  private static void positive(final double value, final String name) {
    finite(value, name);
    if (value <= 0) throw new IllegalArgumentException(name + " должно быть больше нуля.");
  }
}
