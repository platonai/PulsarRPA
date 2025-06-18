#!/usr/bin/env pwsh

# Function to safely remove specific items
function Remove-ItemsSafely {
    param(
        [string]$Path,
        [string]$Description,
        [string[]]$Patterns
    )

    if (Test-Path $Path) {
        Write-Host "Clearing $Description with patterns: $($Patterns -join ', ')..." -ForegroundColor Yellow

        # Remove matching files
        try {
            $matchedFiles = Get-ChildItem -Path $Path -Recurse -Force -File |
                Where-Object {
                    $file = $_.FullName
                    $Patterns | Where-Object { $file -match $_ }
                }

            if ($matchedFiles) {
                $matchedFiles | Remove-Item -Force -ErrorAction SilentlyContinue
                Write-Host "Removed $($matchedFiles.Count) matching files from $Description" -ForegroundColor Green
            } else {
                Write-Host "No matching files found in $Description" -ForegroundColor DarkYellow
            }
        }
        catch {
            Write-Warning "Some files in $Description could not be removed: $($_.Exception.Message)"
        }

        # Remove matching empty directories
        try {
            $matchedDirs = Get-ChildItem -Path $Path -Recurse -Force -Directory |
                Where-Object {
                    $dir = $_.FullName
                    $Patterns | Where-Object { $dir -match $_ }
                } |
                Sort-Object FullName -Descending |
                Where-Object { (Get-ChildItem $_.FullName -Force | Measure-Object).Count -eq 0 }

            if ($matchedDirs) {
                $matchedDirs | Remove-Item -Force -ErrorAction SilentlyContinue
                Write-Host "Removed $($matchedDirs.Count) matching empty directories from $Description" -ForegroundColor Green
            } else {
                Write-Host "No matching empty directories found in $Description" -ForegroundColor DarkYellow
            }
        }
        catch {
            Write-Warning "Some directories in $Description could not be removed: $($_.Exception.Message)"
        }
    }
    else {
        Write-Warning "$Description path not found: $Path"
    }
}

# Define temp directories
$systemTemp = [System.IO.Path]::GetTempPath()
$userTemp = $env:LOCALAPPDATA + "\Temp"

# Define patterns to match
$patterns = @('tomcat', 'chrome', 'test', 'pkg-', '.jar', 'koltin', '.tmp', '.ps', '.ps1')

Write-Host "Starting targeted temporary file cleanup..." -ForegroundColor Cyan
Write-Host "Will only remove items containing: $($patterns -join ', ')" -ForegroundColor Cyan

# Clear System Temp with patterns
Remove-ItemsSafely -Path $systemTemp -Description "System Temp ($systemTemp)" -Patterns $patterns

# Clear User Temp with patterns
Remove-ItemsSafely -Path $userTemp -Description "User Temp ($userTemp)" -Patterns $patterns

Write-Host "`nTargeted temporary files cleanup completed successfully!" -ForegroundColor Green