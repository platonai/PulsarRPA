# bin\run-integration-test.ps1

$ErrorActionPreference = "Stop"

Write-Host "===================================================================="
Write-Host "[TEST 1/3] Running command-sse.ps1 integration test..."
Write-Host "--------------------------------------------------------------------"
& .\bin\command-sse.ps1
if ($LASTEXITCODE -eq 0) {
  Write-Host "[PASS] Integration test command-sse.ps1 completed successfully"
} else {
  Write-Host "[FAIL] Integration test command-sse.ps1 failed with exit code $LASTEXITCODE"
  exit 1
}
Write-Host "===================================================================="

Write-Host "[TEST 2/3] Running scrape.ps1 integration test..."
Write-Host "--------------------------------------------------------------------"
& .\bin\scrape.ps1
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
& .\bin\scrape-async.ps1 -f .\bin\seeds.txt -m 10
if ($LASTEXITCODE -eq 0) {
  Write-Host "[PASS] Integration test scrape-async.ps1 completed successfully"
} else {
  Write-Host "[FAIL] Integration test scrape-async.ps1 failed with exit code $LASTEXITCODE"
  exit 1
}
Write-Host "===================================================================="
Write-Host "All integration tests passed successfully!"