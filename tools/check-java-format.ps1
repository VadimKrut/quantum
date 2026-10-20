#requires -Version 7.0
param(
  [string]$BaseRef = 'HEAD',
  [string]$FormatterJar,
  [switch]$Fix
)

$ErrorActionPreference = 'Stop'
$formatRoot = Split-Path -Parent $PSScriptRoot
$formatVersion = '1.37.0'
$formatHash = '834b2a0c38cb774953322a84b5ca3f2f40dd3156650b3cd44d3b744345962f7a'
$formatJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java).Source }

if (-not $FormatterJar) {
  $formatCache = Join-Path $formatRoot 'target/format-tools'
  New-Item -ItemType Directory -Path $formatCache -Force | Out-Null
  $FormatterJar = Join-Path $formatCache "google-java-format-$formatVersion-all-deps.jar"
  if (-not (Test-Path -LiteralPath $FormatterJar)) {
    Invoke-WebRequest -Uri "https://github.com/google/google-java-format/releases/download/v$formatVersion/google-java-format-$formatVersion-all-deps.jar" -OutFile $FormatterJar
  }
}
if ((Get-FileHash -LiteralPath $FormatterJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $formatHash) {
  throw 'Formatter checksum mismatch.'
}
$FormatterJar = (Resolve-Path -LiteralPath $FormatterJar).Path

Push-Location $formatRoot
try {
  $formatChanged = @(& git -c core.autocrlf=false diff --name-only --diff-filter=ACMR $BaseRef -- '*.java')
  if ($LASTEXITCODE -ne 0) { throw 'Cannot compare with the requested Git reference.' }
  $formatNew = @(& git ls-files --others --exclude-standard -- '*.java')
  if ($LASTEXITCODE -ne 0) { throw 'Cannot list new Java files.' }
  $formatFiles = @($formatChanged + $formatNew | Sort-Object -Unique | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf })
  [string[]]$formatMode = if ($Fix) { '--replace' } else { '--dry-run', '--set-exit-if-changed' }
  $formatOptions = @('--skip-sorting-imports', '--skip-removing-unused-imports', '--skip-javadoc-formatting', '--skip-reflowing-long-strings', '--skip-reordering-modifiers')
  for ($formatIndex = 0; $formatIndex -lt $formatFiles.Count; $formatIndex += 25) {
    $formatEnd = [Math]::Min($formatIndex + 24, $formatFiles.Count - 1)
    $formatArguments = @('-jar', $FormatterJar) + $formatOptions + $formatMode + @($formatFiles[$formatIndex..$formatEnd])
    & $formatJava @formatArguments
    if ($LASTEXITCODE -ne 0) { throw 'Java formatting check failed. Run with -Fix to apply the formatter.' }
  }
  Write-Output "Java format: $($formatFiles.Count) changed/new files checked."
} finally {
  Pop-Location
}
