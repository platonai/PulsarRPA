# bin\run-integration-test.ps1

# Find the first parent directory containing the VERSION file
$AppHome=(Get-Item -Path $MyInvocation.MyCommand.Path).Directory
while ($AppHome -ne $null -and !(Test-Path "$AppHome/VERSION")) {
    $AppHome=$AppHome.Parent
}
cd $AppHome

$ErrorActionPreference = "Stop"

Write-Host "===================================================================="
Write-Host "[TEST 1/3] Running command-sse.ps1 integration test..."
Write-Host "--------------------------------------------------------------------"
& $AppHome\bin\command-sse.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] Integration test command-sse.ps1 completed successfully"
} else {
    Write-Host "[FAIL] Integration test command-sse.ps1 failed with exit code $LASTEXITCODE"
    exit 1
}
Write-Host "===================================================================="

Write-Host "[TEST 2/3] Running scrape.ps1 integration test..."
Write-Host "--------------------------------------------------------------------"
& $AppHome\bin\scrape.ps1
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] Integration test scrape.ps1 completed successfully"
} else {
    Write-Host "[FAIL] Integration test scrape.ps1 failed with exit code $LASTEXITCODE"
    exit 1
}
Write-Host "===================================================================="

Write-Host "[TEST 3/3] Running scrape-async.ps1 integration test with parameters:"
Write-Host "      - Seeds file: .\bin\seeds.txt"
Write-Host "      - Max concurrent tasks: 10"
Write-Host "--------------------------------------------------------------------"
& $AppHome\bin\scrape-async.ps1 -f $AppHome\bin\seeds.txt -m 10
if ($LASTEXITCODE -eq 0) {
    Write-Host "[PASS] Integration test scrape-async.ps1 completed successfully"
} else {
    Write-Host "[FAIL] Integration test scrape-async.ps1 failed with exit code $LASTEXITCODE"
    exit 1
}
Write-Host "===================================================================="
Write-Host "All integration tests passed successfully!"