<#
.SYNOPSIS
  Normalize placement of `import ai.platon.pulsar.common.logPrintln` in Kotlin test files.

.DESCRIPTION
  - Recursively scans all Kotlin files under any `src/test` directory.
  - Detects use of unqualified `logPrintln(...)` (not fully-qualified), and ensures there is
    exactly one `import ai.platon.pulsar.common.logPrintln` placed BEFORE the first existing
    import line in the header. If there are no imports, inserts after package, after @file: annotation,
    or at the top accordingly.
  - Removes any misplaced or duplicate `import ai.platon.pulsar.common.logPrintln` lines found
    elsewhere in the file to avoid trailing or duplicate imports.
  - If a wildcard `import ai.platon.pulsar.common.*` exists, no specific import is inserted, but any
    duplicate specific imports are removed.

.PARAMETER Root
  The root directory to scan. Defaults to repository root based on script location.

.PARAMETER DryRun
  If set, only prints files that would be modified.

.EXAMPLE
  # Preview changes
  powershell -ExecutionPolicy Bypass -File bin\tools\import-logprintln.ps1 -DryRun

.EXAMPLE
  # Apply changes in place
  powershell -ExecutionPolicy Bypass -File bin\tools\import-logprintln.ps1

.NOTES
  - Designed for Windows PowerShell and PowerShell Core.
  - Operates idempotently; safe to run multiple times.
#>
[CmdletBinding()]
param(
  [string]$Root,
  [switch]$DryRun
)

# Resolve repo root robustly
if (-not $Root -or $Root.Trim() -eq '') {
  $scriptPath = $null
  try { $scriptPath = $MyInvocation.MyCommand.Path } catch {}
  $scriptDir = $null
  if ($scriptPath) { $scriptDir = Split-Path -Parent $scriptPath }
  if (-not $scriptDir -or $scriptDir.Trim() -eq '') { $scriptDir = $PSScriptRoot }
  if (-not $scriptDir -or $scriptDir.Trim() -eq '') { $scriptDir = (Get-Location).Path }
  try { $Root = Resolve-Path -LiteralPath (Join-Path $scriptDir '..\..') } catch { $Root = $scriptDir }
}

Write-Output "Scanning for Kotlin test files under: $Root"

# Collect target files: Kotlin sources under any src/test directory
$ktFiles = Get-ChildItem -Path $Root -Recurse -File -Include *.kt,*.kts |
  Where-Object { $_.FullName -match "[\\/]src[\\/]test[\\/]" }

if (-not $ktFiles) {
  Write-Output "No Kotlin test files found."
  return
}

# Regexes
$usesLogPrintlnUnqualified = [regex]::new("(?s)(^|[^A-Za-z0-9_\.:])logPrintln\s*\(")
$usesFullyQualified = [regex]::new("ai\\.platon\\.pulsar\\.common\\.logPrintln\s*\(")
$hasSpecificImportLine = [regex]::new("(?m)^\s*import\s+ai\\.platon\\.pulsar\\.common\\.logPrintln\s*$")
$hasWildcardImport = [regex]::new("(?m)^\s*import\s+ai\\.platon\\.pulsar\\.common\\\.\*\b")
$importLine = 'import ai.platon.pulsar.common.logPrintln'

# Helpers to strip comments and strings for safer detection
function Remove-KotlinCommentsAndStrings {
  param([string]$s)
  if (-not $s) { return $s }
  # Remove block comments /* ... */
  $s = [regex]::Replace($s, '(?s)/\*.*?\*/', '')
  # Remove line comments // ... end
  $s = [regex]::Replace($s, '(?m)//.*$', '')
  # Remove raw triple-quoted strings """ ... """
  $s = [regex]::Replace($s, '(?s)""".*?"""', '""""""')
  # Remove regular double-quoted strings "..." with escapes
  $s = [regex]::Replace($s, '(?s)"(?:\\.|[^"\\])*"', '""')
  # Remove char literals 'a' (incl. escapes)
  $s = [regex]::Replace($s, "'(?:\\.|[^'\\])'", "''")
  return $s
}

$updatedCount = 0
$skippedCount = 0

foreach ($f in $ktFiles) {
  try { $text = Get-Content -LiteralPath $f.FullName -Raw } catch { Write-Warning "Failed to read $($f.FullName): $_"; continue }

  $codeOnly = Remove-KotlinCommentsAndStrings -s $text

  $needsWork = $false
  $usesUnqual = $usesLogPrintlnUnqualified.IsMatch($codeOnly)
  $usesFqn = $usesFullyQualified.IsMatch($codeOnly)
  $hasWildcard = $hasWildcardImport.IsMatch($codeOnly)
  $hasSpecific = $hasSpecificImportLine.IsMatch($codeOnly)

  # Decide whether to process this file
  # We process when:
  #  - Unqualified calls exist, or
  #  - Specific import already exists (to de-duplicate/move if misplaced)
  if ($usesUnqual -or $hasSpecific) {
    $needsWork = $true
  }
  if (-not $needsWork) { $skippedCount++; continue }

  # Build lines and remove all specific import lines to prevent duplicates/misplacement
  $lines = $text -split "\r?\n", -1
  $filtered = New-Object System.Collections.Generic.List[string]
  foreach ($ln in $lines) {
    if ($ln -match '^\s*import\s+ai\.platon\.pulsar\.common\.logPrintln\s*$') { continue }
    $filtered.Add($ln)
  }
  $lines = $filtered.ToArray()

  # If wildcard import exists, we only needed to remove duplicates; write back if changed
  if ($hasWildcard) {
    $newText1 = [string]::Join([Environment]::NewLine, $lines)
    if ($newText1 -ne $text) {
      if ($DryRun) { Write-Output "[DRY] Would cleanup duplicates (wildcard present): $($f.FullName)" }
      else {
        try { Set-Content -LiteralPath $f.FullName -Value $newText1 }
        catch { Write-Warning "Failed to update $($f.FullName): $_" }
        Write-Output "Updated: $($f.FullName)"
        $updatedCount++
      }
    } else { $skippedCount++ }
    continue
  }

  # If only FQN is used and no unqualified call and no specific import originally, nothing to do
  if (-not $usesUnqual -and -not $hasSpecific) { $skippedCount++; continue }

  # Find insertion anchors in header: first import, package, last @file: annotation
  $pkgIdx = -1
  $firstImportIdx = -1
  $lastFileAnnoIdx = -1
  $inBlockComment = $false
  for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $trim = $line.Trim()

    if ($inBlockComment) { if ($trim -match '.*\*/') { $inBlockComment = $false }; continue }
    if ($trim -match '^/\*') { $inBlockComment = $true; if ($trim -match '\*/') { $inBlockComment = $false }; continue }
    if ($trim -match '^//') { continue }
    if ($trim -eq '') { continue }

    if ($trim -match '^@file:') { $lastFileAnnoIdx = $i; continue }
    if ($trim -match '^package\s+') { if ($pkgIdx -eq -1) { $pkgIdx = $i }; continue }
    if ($trim -match '^import\s+') { if ($firstImportIdx -eq -1) { $firstImportIdx = $i }; continue }

    break
  }

  # Insert the single specific import at the canonical place
  $newLines = $null
  if ($firstImportIdx -ge 0) {
    $newLines = @()
    if ($firstImportIdx -gt 0) { $newLines += $lines[0..($firstImportIdx - 1)] }
    $newLines += $importLine
    $newLines += $lines[$firstImportIdx..($lines.Length - 1)]
  } elseif ($pkgIdx -ge 0) {
    $newLines = @()
    $newLines += $lines[0..$pkgIdx]
    $newLines += $importLine
    if ($pkgIdx + 1 -lt $lines.Length) { $newLines += $lines[($pkgIdx + 1)..($lines.Length - 1)] }
  } elseif ($lastFileAnnoIdx -ge 0) {
    $newLines = @()
    $newLines += $lines[0..$lastFileAnnoIdx]
    $newLines += $importLine
    if ($lastFileAnnoIdx + 1 -lt $lines.Length) { $newLines += $lines[($lastFileAnnoIdx + 1)..($lines.Length - 1)] }
  } else {
    $newLines = @($importLine) + $lines
  }

  $newText = [string]::Join([Environment]::NewLine, $newLines)

  if ($DryRun) {
    if ($newText -ne $text) { Write-Output "[DRY] Would update: $($f.FullName)" } else { $skippedCount++ }
  } else {
    try {
      Set-Content -LiteralPath $f.FullName -Value $newText
      Write-Output "Updated: $($f.FullName)"
      $updatedCount++
    } catch {
      Write-Warning "Failed to update $($f.FullName): $_"
    }
  }
}

Write-Output "Done. Updated: $updatedCount, Skipped: $skippedCount, Total scanned: $($ktFiles.Count)"
