#!/usr/bin/env pwsh

$ErrorActionPreference = "Stop"

$chromeExe = "$Env:ProgramFiles\Google\Chrome\Application\chrome.exe"

if (Test-Path $chromeExe) {
    Write-Host "Google Chrome is already installed. Skipping installation."
    exit 0
}

$installerUrl = "https://dl.google.com/chrome/install/latest/chrome_installer.exe"
$tempInstaller = "$env:TEMP\chrome_installer.exe"

Write-Host "Downloading Google Chrome installer..."
Invoke-WebRequest -Uri $installerUrl -OutFile $tempInstaller

Write-Host "Installing Google Chrome silently..."
Start-Process -FilePath $tempInstaller -ArgumentList "/silent /install" -Wait

Remove-Item $tempInstaller -Force

if (Test-Path $chromeExe) {
    Write-Host "Google Chrome installation completed successfully."
    exit 0
} else {
    Write-Error "Google Chrome installation failed."
    exit 1
}
