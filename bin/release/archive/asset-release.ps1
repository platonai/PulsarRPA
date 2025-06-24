#!/usr/bin/env pwsh

param(
    [string]$RemoteUser = "vincent",
    [string]$RemoteHost = "platonai.cn",
    [string]$RemoteBaseDir = "~/platonic.fun/repo/ai/platon/pulsar/"
)

# 🔍 Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

$MvnCmd = Join-Path $AppHome '.\mvnw.cmd'

$Version=(Get-Content "$AppHome/VERSION" -TotalCount 1) -replace "-SNAPSHOT", ""

# If pulsar-app/pulsar-master/target/PulsarRPA.jar exists, copy it to remote
$PulsarRPAPath = "$AppHome/pulsar-app/pulsar-master/target/PulsarRPA.jar"

if (Test-Path $PulsarRPAPath) {
    Write-Host "PulsarRPA.jar exists"
} else {
    Write-Warning "PulsarRPA.jar does not exist"

    # Build pulsar-app/pulsar-master
    Set-Location $AppHome/pulsar-app
    & $MvnCmd -pl pulsar-master clean package -DskipTests
    Set-Location $AppHome
}

$VersionedJarRemoteFullPath = "${RemoteBaseDir}PulsarRPA-${Version}.jar"
$JarRemoteFullPath = "${RemoteBaseDir}PulsarRPA.jar"
$DestinationPath = "${RemoteUser}@${RemoteHost}:$VersionedJarRemoteFullPath"

Write-Host "Copying $PulsarRPAPath to $DestinationPath..."

try {
    # Check if SSH connection works
    $testResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'echo Connection test'"
    if ($LASTEXITCODE -ne 0) {
        throw "SSH connection test failed"
    }

    # Ensure destination directory exists
    $mkdirResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'mkdir -p $RemoteBaseDir'"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create remote directory"
    }

    # Copy the file
    $copyResult = Invoke-Expression "scp $PulsarRPAPath $DestinationPath"
    if ($LASTEXITCODE -ne 0) {
        throw "SCP command failed with exit code $LASTEXITCODE"
    }

    Write-Host "File copied successfully to $DestinationPath" -ForegroundColor Green

    # Create a symbolic link to the latest version
    $linkResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'ln -sf $VersionedJarRemoteFullPath $JarRemoteFullPath'"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create symbolic link"
    }
    Write-Host "Symbolic link created successfully" -ForegroundColor Green

    # List the remote directory contents
    # e.g. ssh vincent@platonai.cn ls -l ~/platonic.fun/repo/ai/platon/pulsar/
    $listResult = Invoke-Expression "ssh ${RemoteUser}@${RemoteHost} 'ls -l $RemoteBaseDir'"

    Write-Host "Done."
    Write-Host "Assets are available at: https://static.platonai.cn/repo/ai/platon/pulsar/"
}
catch {
    Write-Error "Error: $_"
    exit 1
}
