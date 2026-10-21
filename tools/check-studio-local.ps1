#requires -Version 7.0
param(
  [string]$JdkHome = $env:JAVA_HOME,
  [switch]$LiveReleaseCheck,
  [switch]$CaptureLayouts
)
$ErrorActionPreference = 'Stop'
if (-not $JdkHome) { throw 'Specify -JdkHome with JDK 25.' }
$auditRoot = Split-Path -Parent $PSScriptRoot
$auditJar = Join-Path $auditRoot 'quantum-desktop/target/quantum-desktop-1.0.jar'
$auditClasses = Join-Path $auditRoot 'target/studio-audit'
if (-not (Test-Path -LiteralPath $auditJar)) { throw 'Build quantum-desktop with -DskipTests first.' }
New-Item -ItemType Directory -Path $auditClasses -Force | Out-Null
& (Join-Path $JdkHome 'bin/javac.exe') -encoding UTF-8 -cp $auditJar -d $auditClasses (Join-Path $PSScriptRoot 'LaboratoryNavigationAudit.java') (Join-Path $PSScriptRoot 'LaboratoryActionAudit.java') (Join-Path $PSScriptRoot 'StudioUpdateAudit.java') (Join-Path $PSScriptRoot 'LaboratoryTutorialAudit.java')
if ($LASTEXITCODE -ne 0) { throw 'Audit compilation failed.' }
$auditClassPath = "$auditClasses$([IO.Path]::PathSeparator)$auditJar"
$auditArguments = @('--enable-native-access=ALL-UNNAMED', '-Dprism.order=sw', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-Xmx768m', '-cp', $auditClassPath)
if ($CaptureLayouts) { $auditArguments = @('-Dquantum.audit.snapshots=true') + $auditArguments }
& (Join-Path $JdkHome 'bin/java.exe') @auditArguments ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryNavigationAudit
if ($LASTEXITCODE -ne 0) { throw 'Navigation audit failed.' }
& (Join-Path $JdkHome 'bin/java.exe') @auditArguments ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryActionAudit
if ($LASTEXITCODE -ne 0) { throw 'Action audit failed.' }
[string[]]$auditLive = if ($LiveReleaseCheck) { @('--live') } else { @() }
& (Join-Path $JdkHome 'bin/java.exe') @auditArguments ru.pathcreator.vadim.quantum.desktop.update.StudioUpdateAudit @auditLive
if ($LASTEXITCODE -ne 0) { throw 'Update audit failed.' }

& (Join-Path $JdkHome 'bin/java.exe') @auditArguments ru.pathcreator.vadim.quantum.desktop.chemistry.ui.LaboratoryTutorialAudit
if ($LASTEXITCODE -ne 0) { throw 'Tutorial audit failed.' }
