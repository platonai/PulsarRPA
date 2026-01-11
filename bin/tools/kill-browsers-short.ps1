#!/usr/bin/env pwsh

foreach ($proc in (Get-Process -Name "chrome")) {
    $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($proc.Id)").CommandLine
    if ($cmdLine -and $cmdLine -match "PULSAR_CHROME") {
        Write-Host "Killing process $($proc.Id) for browser $browser with command line: $cmdLine" -ForegroundColor Yellow
        Stop-Process -Id $proc.Id -Force
    }
}
