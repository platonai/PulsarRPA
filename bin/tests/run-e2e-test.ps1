#!/usr/bin/env pwsh

# 查找包含 VERSION 文件的根目录
$AppHome = (Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome = Split-Path -Parent $AppHome
}
Set-Location $AppHome

# 启用错误终止
$ErrorActionPreference = "Stop"

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "[TEST 1/4] Running command-sse.ps1 e2e test..." -ForegroundColor Yellow
Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
& $AppHome/bin/command-sse.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] E2E test command-sse.ps1 completed successfully" -ForegroundColor Green
} else {
    Write-Host "[FAIL] E2E test command-sse.ps1 failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
Write-Host "====================================================================" -ForegroundColor Cyan

Write-Host "[TEST 2/4] Running scrape.ps1 e2e test..." -ForegroundColor Yellow
Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
& $AppHome/bin/scrape.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] E2E test scrape.ps1 completed successfully" -ForegroundColor Green
} else {
    Write-Host "[FAIL] E2E test scrape.ps1 failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
Write-Host "====================================================================" -ForegroundColor Cyan

Write-Host "[TEST 3/4] Running scrape-async.ps1 e2e test with parameters:" -ForegroundColor Yellow
Write-Host "      - Seeds file: ./bin/seeds.txt" -ForegroundColor Gray
Write-Host "      - Max concurrent tasks: 10" -ForegroundColor Gray
Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
& $AppHome/bin/scrape-async.ps1 -f ./bin/seeds.txt -m 10
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] E2E test scrape-async.ps1 completed successfully" -ForegroundColor Green
} else {
    Write-Host "[FAIL] E2E test scrape-async.ps1 failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
Write-Host "====================================================================" -ForegroundColor Cyan

Write-Host "[TEST 4/4] Running run-test-cases.ps1 e2e test" -ForegroundColor Yellow
Write-Host "--------------------------------------------------------------------" -ForegroundColor Gray
& $AppHome/bin/tests/test-curl-commands.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] E2E test run-test-cases.ps1 completed successfully" -ForegroundColor Green
} else {
    Write-Host "[FAIL] E2E test run-test-cases.ps1 failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "All e2e tests passed successfully!" -ForegroundColor Green

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host "E2E tests completed at $(Get-Date)" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan
