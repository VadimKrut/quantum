# Quantum IR

Quantum IR is a Java 25 multi-module project for describing, validating, simulating, persisting, importing, and exporting quantum programs through a project-owned intermediate representation.

The main idea is simple: the native Quantum IR is the center of the system. External formats are adapters around it, not the core design driver. A program can be created through Java APIs, stored as native JSON, inspected, simulated locally, and exported to supported text formats.

## Project Direction

The long-term goal is to grow Quantum IR into a full development studio and programming surface for quantum software. The native IR and Java DSL are intended to be the project-owned way to describe quantum programs. External formats such as OpenQASM and Quil exist for interoperability, migration, inspection, and convenience.

Future hardware support should be implemented through dedicated internal backend adapters. A backend adapter should understand the native Quantum IR directly and translate it into the real instruction set, job model, constraints, and diagnostics of a specific quantum computer or simulator. Backend-specific rules must stay outside the core domain model.

## What The Project Does

- Provides a format-independent Quantum IR core.
- Validates registers, operations, references, gate definitions, classical constructs, timing/control constructs, and metadata.
- Supports native JSON persistence for the current IR surface.
- Imports and exports OpenQASM 2, OpenQASM 3, and Quil.
- Provides a public Java API facade for common workflows.
- Provides a CLI for validation, inspection, preflight, transformation, conversion, compilation, and simulation.
- Provides a JavaFX desktop studio focused on native IR building, visualization, diagnostics, simulation preview, and export.
- Includes a local state-vector simulator intended for correctness checks and development workflows.

## Modules

- `quantum-core` - domain model and validation.
- `quantum-application` - application services, diagnostics, preflight, transformation, workflows.
- `quantum-json` - native Quantum IR JSON reader/writer.
- `quantum-openqasm2` - OpenQASM 2 integration adapter.
- `quantum-openqasm3` - OpenQASM 3 integration adapter.
- `quantum-quil` - Quil integration adapter.
- `quantum-simulation` - local simulation engine.
- `quantum-api` - stable public Java facade.
- `quantum-library` - built-in algorithm library and descriptors.
- `quantum-cli` - command-line interface.
- `quantum-desktop` - JavaFX desktop studio.

## Requirements

- Java 25.
- Maven 3.9 or newer.

Make sure the active JDK in your terminal or IDE is Java 25 before building the project.

## Build And Test

Run the full test suite:

```bash
mvn test
```

Build the desktop module and everything it needs:

```bash
mvn -pl quantum-desktop -am package
```

Run the desktop studio:

```bash
java -jar quantum-desktop/target/quantum-desktop-<version>.jar
```

Build a desktop app image with an embedded runtime:

```bash
mvn -pl quantum-desktop -am clean verify -Pdist-app-image -DskipTests
```

The generated application is placed under `quantum-desktop/target/dist/`.
This app image is a complete application directory; do not copy only the launcher executable out of it, because it depends on the sibling `runtime` and `app` directories.

Platform installers are generated with `dist-windows-exe`, `dist-windows-msi`, `dist-linux-deb`, `dist-linux-rpm`, or `dist-macos-dmg` on the corresponding operating system. Windows installer profiles require the WiX Toolset available to `jpackage`.

Pushes to `master` run the desktop distribution workflow. The workflow reads the root Maven version, publishes artifacts under the release tag `v<version>`, and refreshes the release assets when the Maven version has not changed. Increasing the root Maven version creates a new release tag and a new release.
Project release versions use `major.minor` numbers without leading zeroes, for example `1.0`, `1.1`, or `2.0`.

## Maven Packages

GitHub Packages publishes the Maven modules separately, so a downstream project can depend only on the parts it needs. For example, an application can use the public facade without depending on the desktop studio:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/VadimKrut/quantum</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>ru.pathcreator.vadim</groupId>
        <artifactId>quantum-api</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
```

Other published modules use the same group and version, for example `quantum-core`, `quantum-simulation`, `quantum-json`, `quantum-openqasm2`, `quantum-openqasm3`, `quantum-quil`, and `quantum-library`.

If Maven asks for credentials when reading GitHub Packages, configure a `github` server in the consumer project's Maven `settings.xml` with a GitHub token that can read packages.

## Supported External Formats

Current baseline integrations:

- OpenQASM 2
- OpenQASM 3
- Quil

The domain model is not intended to mirror any single external language. Each integration has its own import/export rules, diagnostics, capability checks, and lowering behavior.

## License

This project is licensed under the Mozilla Public License 2.0.

## First Version Notice

This is the first public version. Some behavior, diagnostics, documentation, UI details, or integration edge cases may still contain inaccuracies. If you find a bug, an incorrect result, unclear behavior, or a missing case, please open an issue and describe how to reproduce it. I will review it and fix the project.

---

# Quantum IR

Quantum IR - это Java 25 multi-module проект для описания, валидации, симуляции, сохранения, импорта и экспорта квантовых программ через собственное промежуточное представление проекта.

Главная идея простая: родной Quantum IR является центром системы. Внешние форматы являются адаптерами вокруг него, а не основой архитектуры. Программу можно создать через Java API, сохранить в родной JSON, проинспектировать, локально просимулировать и экспортировать в поддерживаемые текстовые форматы.

## Направление Проекта

Долгосрочная цель - вырастить Quantum IR в полноценную студию разработки и программную поверхность для квантового ПО. Родной IR и Java DSL должны быть собственным способом проекта описывать квантовые программы. Внешние форматы вроде OpenQASM и Quil нужны для совместимости, миграции, инспекции и удобства.

Будущая поддержка реального оборудования должна строиться через отдельные внутренние backend adapters. Такой adapter должен напрямую понимать родной Quantum IR и переводить его в реальные инструкции, job model, ограничения и диагностику конкретного квантового компьютера или симулятора. Специфичные правила backend не должны попадать в core domain model.

## Что Делает Проект

- Предоставляет независимое от внешних форматов ядро Quantum IR.
- Валидирует регистры, операции, ссылки, определения гейтов, классические конструкции, timing/control конструкции и metadata.
- Поддерживает native JSON persistence для текущей поверхности IR.
- Импортирует и экспортирует OpenQASM 2, OpenQASM 3 и Quil.
- Предоставляет публичный Java API facade для типовых workflow.
- Предоставляет CLI для validation, inspection, preflight, transformation, conversion, compilation и simulation.
- Предоставляет JavaFX desktop studio для построения родного IR, визуализации, диагностики, simulation preview и export.
- Включает локальный state-vector simulator для проверки корректности и разработки.

## Модули

- `quantum-core` - доменная модель и валидация.
- `quantum-application` - application services, diagnostics, preflight, transformation, workflows.
- `quantum-json` - reader/writer родного Quantum IR JSON.
- `quantum-openqasm2` - integration adapter для OpenQASM 2.
- `quantum-openqasm3` - integration adapter для OpenQASM 3.
- `quantum-quil` - integration adapter для Quil.
- `quantum-simulation` - локальный simulation engine.
- `quantum-api` - стабильный публичный Java facade.
- `quantum-library` - встроенная библиотека алгоритмов и описаний.
- `quantum-cli` - command-line interface.
- `quantum-desktop` - JavaFX desktop studio.

## Требования

- Java 25.
- Maven 3.9 или новее.

Перед сборкой убедитесь, что в терминале или IDE активен JDK 25.

## Сборка И Тесты

Запустить все тесты:

```bash
mvn test
```

Собрать desktop-модуль и его зависимости:

```bash
mvn -pl quantum-desktop -am package
```

Запустить desktop studio:

```bash
java -jar quantum-desktop/target/quantum-desktop-<version>.jar
```

Собрать desktop app image со встроенным runtime:

```bash
mvn -pl quantum-desktop -am clean verify -Pdist-app-image -DskipTests
```

Собранное приложение будет находиться в `quantum-desktop/target/dist/`.
App image - это полная папка приложения. Не копируйте из неё только launcher executable, потому что он зависит от соседних папок `runtime` и `app`.

Платформенные установщики собираются профилями `dist-windows-exe`, `dist-windows-msi`, `dist-linux-deb`, `dist-linux-rpm` или `dist-macos-dmg` на соответствующей операционной системе. Windows installer profiles требуют WiX Toolset, доступный для `jpackage`.

Push в `master` запускает desktop distribution workflow. Workflow читает версию из root Maven, публикует артефакты в release tag `v<version>` и обновляет release assets, если Maven version не изменилась. Увеличение root Maven version создаёт новый release tag и новый release.
Версии релизов проекта задаются числами в формате `major.minor` без ведущих нулей, например `1.0`, `1.1` или `2.0`.

## Maven Packages

GitHub Packages публикует Maven-модули отдельно, поэтому другой проект может подключить только нужную часть библиотеки. Например, приложение может использовать публичный facade без desktop studio:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/VadimKrut/quantum</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>ru.pathcreator.vadim</groupId>
        <artifactId>quantum-api</artifactId>
        <version>1.0</version>
    </dependency>
</dependencies>
```

Остальные опубликованные модули используют тот же group и version, например `quantum-core`, `quantum-simulation`, `quantum-json`, `quantum-openqasm2`, `quantum-openqasm3`, `quantum-quil` и `quantum-library`.

Если Maven при чтении GitHub Packages запросит доступ, настройте server с id `github` в Maven `settings.xml` проекта-потребителя и укажите GitHub token с правом чтения packages.

## Поддерживаемые Внешние Форматы

Текущие базовые интеграции:

- OpenQASM 2
- OpenQASM 3
- Quil

Доменная модель не должна копировать один конкретный внешний язык. У каждой интеграции есть свои правила import/export, diagnostics, capability checks и lowering behavior.

## Лицензия

Проект распространяется под лицензией Mozilla Public License 2.0.

## Предупреждение О Первой Версии

Это первая публичная версия. В поведении, диагностике, документации, UI или edge cases интеграций могут оставаться неточности. Если вы нашли ошибку, некорректный результат, непонятное поведение или неподдержанный случай, пожалуйста, создайте задачу и опишите, как это воспроизвести. Я всё проверю и поправлю проект.