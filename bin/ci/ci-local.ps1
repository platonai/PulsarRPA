#!/usr/bin/env pwsh

# Define a parameter for intervalSeconds with a default value of 60
param (
    [int]$intervalSeconds = 60
)

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# Configuration
$repoPath = $AppHome                                  # 你的 Git 仓库路径
$buildScript = "$AppHome/bin/build.ps1"               # 你的构建脚本

# Enter the repository directory
Set-Location -Path $repoPath

# Function to get the current HEAD hash
function Get-HeadHash {
    return (git rev-parse HEAD)
}

# Function to run the build script
function Run-BuildScript {
    if (Test-Path $buildScript) {
        Write-Output "[INFO] Running $buildScript..."

        # Test the pulsar-tests module first
        Write-Output "[INFO] Testing pulsar-tests module..."
        $testResult = & "$buildScript" -clean -pl :pulsar-tests
        $testResult = & "$buildScript" -test -pl :pulsar-tests

        # Check if the pulsar-tests module had any failed tests
        if ($LASTEXITCODE -eq 0) {
            Write-Output "[INFO] pulsar-tests module passed. Testing all modules..."
            & "$buildScript" -clean -test
        } else {
            Write-Output "[ERROR] pulsar-tests module failed. Skipping testing of all other modules."
        }
    } else {
        Write-Output "[ERROR] $buildScript not found in $repoPath"
    }
}

# First run: Always pull and build
Write-Output "[INFO] First run: Pulling latest changes and running build script..."
git pull
Run-BuildScript

# Get the current HEAD hash after first run
$lastHash = Get-HeadHash

# Main loop
while ($true) {
    Write-Output "[INFO] Checking for updates at $(Get-Date)"

    # Perform git pull
    git pull

    # Get the new HEAD hash
    $newHash = Get-HeadHash

    # Compare hashes
    if ($newHash -ne $lastHash) {
        Write-Output "[INFO] New updates detected (Old: $lastHash, New: $newHash)"

        # Run build script if updates are detected
        Run-BuildScript

        # Update the last hash
        $lastHash = $newHash
    } else {
        Write-Output "[INFO] No updates detected."
    }

    # Wait for the next check
    Start-Sleep -Seconds $intervalSeconds
}
