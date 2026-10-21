#requires -Version 7.0
param(
  [Parameter(Mandatory = $true)][string]$MsiPath,
  [switch]$VerifyOnly
)

$ErrorActionPreference = 'Stop'
$packagePath = (Resolve-Path -LiteralPath $MsiPath).Path
if ([IO.Path]::GetExtension($packagePath) -ne '.msi') { throw 'Expected an MSI package.' }
$packageInstaller = New-Object -ComObject WindowsInstaller.Installer
try {
  $packageDatabase = $packageInstaller.OpenDatabase($packagePath, $(if ($VerifyOnly) { 0 } else { 1 }))

  function Read-PackageRows([string]$Query) {
    $packageView = $packageDatabase.OpenView($Query)
    $null = $packageView.Execute()
    while ($packageRecord = $packageView.Fetch()) { ,$packageRecord }
    $null = $packageView.Close()
  }

  # jpackage's recursive cleanup also removes files that were never installed by this MSI.
  # Rely on Windows Installer's owned-file/component tables and empty-directory cleanup instead.
  $packageTables = @(Read-PackageRows 'SELECT `Name` FROM `_Tables`' | ForEach-Object { $_.StringData(1) })
  if (-not $VerifyOnly) {
    foreach ($packageSequence in @('InstallExecuteSequence', 'InstallUISequence')) {
      if ($packageTables -contains $packageSequence) {
        $packageView = $packageDatabase.OpenView('UPDATE `' + $packageSequence + '` SET `Condition` = ''0'' WHERE `Action` = ''WixRemoveFoldersEx''')
        $packageView.Execute()
        $packageView.Close()
      }
    }
    if ($packageTables -contains 'WixRemoveFolderEx') {
      $packageView = $packageDatabase.OpenView('DELETE FROM `WixRemoveFolderEx`')
      $packageView.Execute()
      $packageView.Close()
    }
    $packageDatabase.Commit()
  }
  foreach ($packageSequence in @('InstallExecuteSequence', 'InstallUISequence')) {
    if ($packageTables -contains $packageSequence) {
      foreach ($packageRecord in @(Read-PackageRows ('SELECT `Condition` FROM `' + $packageSequence + '` WHERE `Action` = ''WixRemoveFoldersEx'''))) {
        if ($packageRecord.StringData(1) -ne '0') { throw 'Recursive uninstall action is enabled.' }
      }
    }
  }
  if ($packageTables -contains 'WixRemoveFolderEx') {
    if (@(Read-PackageRows 'SELECT * FROM `WixRemoveFolderEx`').Count -ne 0) { throw 'Recursive removal targets remain.' }
  }
  if ($packageTables -contains 'RemoveFile') {
    foreach ($packageRecord in @(Read-PackageRows 'SELECT `FileName` FROM `RemoveFile`')) {
      if ($packageRecord.StringData(1)) { throw 'Unexpected removal pattern; review the package before publishing.' }
    }
  }
  Write-Output 'MSI verified: removes owned files and empty folders, never recursively clears the installation directory.'
} finally {
  foreach ($packageComObject in @($packageView, $packageRecord, $packageDatabase, $packageInstaller)) {
    if ($null -ne $packageComObject -and [Runtime.InteropServices.Marshal]::IsComObject($packageComObject)) {
      $null = [Runtime.InteropServices.Marshal]::FinalReleaseComObject($packageComObject)
    }
  }
  [GC]::Collect()
  [GC]::WaitForPendingFinalizers()
}
