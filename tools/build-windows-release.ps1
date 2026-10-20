#requires -Version 7.0
param(
  [Parameter(Mandatory = $true)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
  [Parameter(Mandatory = $true)][string]$JdkHome,
  [Parameter(Mandatory = $true)][string]$WixDirectory,
  [Parameter(Mandatory = $true)][string]$RuntimeImage,
  [ValidateSet('msi', 'exe')][string]$PackageType = 'msi',
  [string]$SourceArchive
)

$ErrorActionPreference = 'Stop'
$releaseRoot = Split-Path -Parent $PSScriptRoot
$releaseWork = Join-Path $releaseRoot "target/windows-release-$Version"
$releaseDest = Join-Path $releaseRoot 'dist'
$releaseJar = Join-Path $releaseRoot 'quantum-desktop/target/quantum-desktop-1.0.jar'
$releaseJpackage = Join-Path $JdkHome 'bin/jpackage.exe'
$releaseRuntime = (Resolve-Path -LiteralPath $RuntimeImage).Path
$releaseWix = (Resolve-Path -LiteralPath $WixDirectory).Path
$releaseIcon = Join-Path $releaseRoot 'quantum-desktop/src/main/packaging/windows/quantum-ir-studio.ico'
$releaseInstaller = Join-Path $releaseDest "Quantum IR Studio-$Version.$PackageType"
foreach ($releaseRequired in @($releaseJar, $releaseJpackage, $releaseIcon, (Join-Path $releaseRuntime 'bin/java.exe'), (Join-Path $releaseWix 'candle.exe'), (Join-Path $releaseWix 'light.exe'))) {
  if (-not (Test-Path -LiteralPath $releaseRequired -PathType Leaf)) { throw "Missing release dependency: $releaseRequired" }
}
if (Test-Path -LiteralPath $releaseWork) { throw "Release work directory already exists: $releaseWork" }
if (Test-Path -LiteralPath $releaseInstaller) { throw "Installer already exists: $releaseInstaller" }
$releaseInput = Join-Path $releaseWork 'input'
$releaseImages = Join-Path $releaseWork 'image'
New-Item -ItemType Directory -Path $releaseInput, $releaseImages -Force | Out-Null
New-Item -ItemType Directory -Path $releaseDest -Force | Out-Null
Copy-Item -LiteralPath $releaseJar -Destination $releaseInput
Copy-Item -LiteralPath (Join-Path $releaseRoot 'LICENSE') -Destination (Join-Path $releaseInput 'LICENSE.txt')
if ($SourceArchive) {
  $releaseSource = (Resolve-Path -LiteralPath $SourceArchive).Path
  if ([IO.Path]::GetExtension($releaseSource) -ne '.zip') { throw 'Source archive must be a ZIP file' }
  Copy-Item -LiteralPath $releaseSource -Destination (Join-Path $releaseInput 'Quantum-IR-Studio-sources.zip')
}
$releaseJarHash = (Get-FileHash -LiteralPath $releaseJar -Algorithm SHA256).Hash.ToLowerInvariant()
$releasePathBefore = $env:PATH
try {
  $env:PATH = "$releaseWix;$env:PATH"
  $releaseImageArguments = @('--type', 'app-image', '--input', $releaseInput, '--dest', $releaseImages,
    '--name', 'Quantum IR Studio', '--main-jar', 'quantum-desktop-1.0.jar',
    '--main-class', 'ru.pathcreator.vadim.quantum.desktop.launcher.QuantumDesktopLauncher',
    '--app-version', $Version, '--vendor', 'Vadim Aleksandrovich Zaletaev',
    '--description', 'Local chemistry and synthesis workbench', '--icon', $releaseIcon,
    '--runtime-image', $releaseRuntime, '--java-options', '-Dfile.encoding=UTF-8',
    '--java-options', '--enable-native-access=ALL-UNNAMED')
  & $releaseJpackage @releaseImageArguments 2>&1 | Tee-Object -FilePath (Join-Path $releaseWork 'app-image.log')
  if ($LASTEXITCODE -ne 0) { throw "App image failed: $LASTEXITCODE" }
  $releaseImage = Join-Path $releaseImages 'Quantum IR Studio'
  $releaseEmbeddedJar = Join-Path $releaseImage 'app/quantum-desktop-1.0.jar'
  if ((Get-FileHash -LiteralPath $releaseEmbeddedJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $releaseJarHash) { throw 'Packaged JAR differs from the verified build' }
  $releaseInstallerArguments = @('--type', $PackageType, '--app-image', $releaseImage, '--dest', $releaseDest,
    '--name', 'Quantum IR Studio', '--app-version', $Version, '--vendor', 'Vadim Aleksandrovich Zaletaev',
    '--description', 'Local chemistry and synthesis workbench', '--win-per-user-install',
    '--win-menu', '--win-menu-group', 'Quantum IR Studio', '--win-shortcut', '--win-shortcut-prompt',
    '--win-dir-chooser', '--install-dir', 'Quantum IR Studio',
    '--win-upgrade-uuid', 'd6bc9912-fde3-59cd-a5ee-5ba989833c9c',
    '--temp', (Join-Path $releaseWork 'installer-temp'), '--verbose')
  & $releaseJpackage @releaseInstallerArguments 2>&1 | Tee-Object -FilePath (Join-Path $releaseWork 'installer.log')
  if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $releaseInstaller)) { throw "Installer failed: $LASTEXITCODE" }
  $releaseImageFiles = @(Get-ChildItem -LiteralPath $releaseImage -Recurse -File | ForEach-Object {
    @{ path = [IO.Path]::GetRelativePath($releaseImage, $_.FullName); sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(); bytes = $_.Length }
  })
  $releaseManifest = @{ version = $Version; packageType = $PackageType; installer = $releaseInstaller;
    sha256 = (Get-FileHash -LiteralPath $releaseInstaller -Algorithm SHA256).Hash.ToLowerInvariant();
    bytes = (Get-Item -LiteralPath $releaseInstaller).Length; jarSha256 = $releaseJarHash;
    appImage = $releaseImage; javaBundled = $true; perUserInstall = $true;
    upgradeUuid = 'd6bc9912-fde3-59cd-a5ee-5ba989833c9c'; signed = $false;
    appImageFiles = $releaseImageFiles; imageArguments = $releaseImageArguments; installerArguments = $releaseInstallerArguments }
  [IO.File]::WriteAllText((Join-Path $releaseDest "Quantum-IR-Studio-$Version-manifest.json"), ($releaseManifest | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
  Write-Output "READY: $releaseInstaller"
} finally {
  $env:PATH = $releasePathBefore
}
