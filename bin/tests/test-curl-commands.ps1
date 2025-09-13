#!/usr/bin/env pwsh

# Test script for curl commands from README.md
# All curl commands are explicitly listed below for easy maintenance
# Author: Auto-generated for platonai
# Date: 2025-06-11 17:29:59

# =========================
# CURL 命令与描述变量定义
# =========================

# System Health Checks (Quick tests first)
$CURL_DESC_HEALTH_CHECK = "Health Check Endpoint"
$CURL_CMD_HEALTH_CHECK = 'curl -X GET "http://localhost:8182/actuator/health"'

$CURL_DESC_QUERY_PARAMS = "Query Parameters Test"
$CURL_CMD_QUERY_PARAMS = 'curl -X GET "http://localhost:8182/actuator/health?details=true"'

$CURL_DESC_WEBUI = "WebUI Command Interface"
$CURL_CMD_WEBUI = @'
curl -X GET "http://localhost:8182/command.html"
'@

$CURL_DESC_CUSTOM_HEADERS = "Custom Headers Test"
$CURL_CMD_CUSTOM_HEADERS = @'
curl -X GET "http://localhost:8182/actuator/health" -H "Accept: application/json" -H "User-Agent: Browser4-Test-Suite/1.0"
'@

# Simple Data Extraction Tests
$CURL_DESC_SIMPLE_LOAD = "Simple Page Load Test"
$CURL_CMD_SIMPLE_LOAD = @'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_base_uri(dom) as url,
dom_first_text(dom, 'title') as page_title
from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N', 'body');
"
'@

$CURL_DESC_HTML_PARSE = "HTML Parsing Test"
$CURL_CMD_HTML_PARSE = @'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, 'h1') as heading,
dom_all_texts(dom, 'p') as paragraphs
from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N', 'body');
"
'@

$CURL_DESC_COMPLEX_XSQL = "Complex X-SQL Query"
$CURL_CMD_COMPLEX_XSQL = @'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
dom_first_text(dom, 'title') as page_title,
dom_first_text(dom, 'h1,h2') as main_heading,
dom_base_uri(dom) as base_url
from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N', 'body');
"
'@

$CURL_DESC_FORM_DATA = "Form Data Test"
$CURL_CMD_FORM_DATA = @'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select dom_first_text(dom, 'title') as title from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N', 'body');
"
'@

# Advanced API Tests (Longer running)
$CURL_DESC_PLAIN_API = "Plain Text Command API - Amazon Product"
$CURL_CMD_PLAIN_API = @'
curl -X POST "http://localhost:8182/api/commands/plain" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0FFTT2J6N

After browser launch: clear browser cookies.
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
"
'@

$CURL_DESC_JSON_API = "JSON Command API - Amazon Product"
$CURL_CMD_JSON_API = @'
curl -X POST "http://localhost:8182/api/commands" -H "Content-Type: application/json" -d '{
"url": "https://www.amazon.com/dp/B0FFTT2J6N",
"onBrowserLaunchedActions": [
  "clear browser cookies",
  "navigate to the home page",
  "click a random link"
],
"onPageReadyActions": ["click #title", "scroll to the middle"],
"pageSummaryPrompt": "Provide a brief introduction of this product.",
"dataExtractionRules": "product name, price, and ratings",
"uriExtractionRules": "all links containing /dp/ on the page"
}'
'@

$CURL_DESC_XSQL_LLM = "X-SQL API - LLM Data Extraction"
$CURL_CMD_XSQL_LLM = @'
curl -X POST "http://localhost:8182/api/x/e" -H "Content-Type: text/plain" -d "
select
llm_extract(dom, 'product name, price, ratings') as llm_extracted_data,
dom_base_uri(dom) as url,
dom_first_text(dom, '#productTitle') as title,
dom_first_slim_html(dom, 'img:expr(width > 400)') as img
from load_and_select('https://www.amazon.com/dp/B0FFTT2J6N', 'body');
"
'@

$CURL_DESC_ASYNC_MODE = "Async Command Mode Test"
$CURL_CMD_ASYNC_MODE = @'
curl -X POST "http://localhost:8182/api/commands/plain?mode=async" -H "Content-Type: text/plain" -d "
Go to https://www.amazon.com/dp/B0FFTT2J6N

Extract the page title and all text content.
"
'@

# 系统测试优先的命令数组
$CURL_COMMANDS = @(
  @{Desc = $CURL_DESC_HEALTH_CHECK; Cmd = $CURL_CMD_HEALTH_CHECK}
  @{Desc = $CURL_DESC_QUERY_PARAMS; Cmd = $CURL_CMD_QUERY_PARAMS}
  @{Desc = $CURL_DESC_WEBUI; Cmd = $CURL_CMD_WEBUI}
  @{Desc = $CURL_DESC_CUSTOM_HEADERS; Cmd = $CURL_CMD_CUSTOM_HEADERS}
  @{Desc = $CURL_DESC_SIMPLE_LOAD; Cmd = $CURL_CMD_SIMPLE_LOAD}
  @{Desc = $CURL_DESC_HTML_PARSE; Cmd = $CURL_CMD_HTML_PARSE}
  @{Desc = $CURL_DESC_COMPLEX_XSQL; Cmd = $CURL_CMD_COMPLEX_XSQL}
  @{Desc = $CURL_DESC_FORM_DATA; Cmd = $CURL_CMD_FORM_DATA}
  @{Desc = $CURL_DESC_ASYNC_MODE; Cmd = $CURL_CMD_ASYNC_MODE}
  @{Desc = $CURL_DESC_PLAIN_API; Cmd = $CURL_CMD_PLAIN_API}
  @{Desc = $CURL_DESC_JSON_API; Cmd = $CURL_CMD_JSON_API}
  @{Desc = $CURL_DESC_XSQL_LLM; Cmd = $CURL_CMD_XSQL_LLM}
)

# =============================================================================
# SECTION: GLOBAL CONFIGURATION AND INITIALIZATION
# =============================================================================

$DEFAULT_BASE_URL = "http://localhost:8182"
$TEST_RESULTS_DIR = "./target/test-results"
$TIMESTAMP = (Get-Date -Format "yyyyMMdd_HHmmss")
$LOG_FILE = "${TEST_RESULTS_DIR}/curl_tests_${TIMESTAMP}.log"

# 颜色定义
function Write-ColorText {
  param (
    [Parameter(Mandatory=$true)] [string]$Text,
    [string]$Color = "White"
  )
  Write-Host $Text -ForegroundColor $Color -NoNewline
}

# 默认选项
$PULSAR_BASE_URL = $DEFAULT_BASE_URL
$TIMEOUT_SECONDS = 120
$FAST_MODE = $false
$SKIP_SERVER_CHECK = $false
$VERBOSE_MODE = $false
$USER_NAME = "platonai"

# 计数器
$TOTAL_TESTS = 0
$PASSED_TESTS = 0
$FAILED_TESTS = 0
$SKIPPED_TESTS = 0

# 确保结果目录存在
New-Item -ItemType Directory -Path $TEST_RESULTS_DIR -Force | Out-Null

# =============================================================================
# SECTION: UTILITY FUNCTIONS
# =============================================================================

function Log {
  param(
    [string]$Message
  )
  $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
  $logMessage = "[$timestamp] $Message"
  Write-Host $logMessage
  Add-Content -Path $LOG_FILE -Value $logMessage
}

function VLog {
  param(
    [string]$Message
  )
  if ($VERBOSE_MODE) {
    Log "[VERBOSE] $Message" -Color "Cyan"
  }
}

function Show-Progress {
  param(
    [int]$Current,
    [int]$Total
  )
  $percent = [math]::Floor(($Current * 100) / $Total)
  $filled = [math]::Floor($percent / 2)
  $empty = 50 - $filled

  Write-Host "`r[PROGRESS] [" -ForegroundColor Blue -NoNewline
  Write-Host ($filled -gt 0 ? ("=" * $filled) : "") -NoNewline
  Write-Host ($empty -gt 0 ? ("-" * $empty) : "") -NoNewline
  Write-Host "] ${percent}% ($Current/$Total)" -NoNewline
}

function Substitute-Urls {
  param(
    [string]$Command
  )
  return $Command -replace "http://localhost:8182", $PULSAR_BASE_URL
}

function Check-Server {
  Log "[INFO] Checking Browser4 server at $PULSAR_BASE_URL..." -ForegroundColor Blue

  try {
    $response = Invoke-WebRequest -Uri "$PULSAR_BASE_URL/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
      Log "[SUCCESS] Browser4 server is healthy and responding" -ForegroundColor Green
      return $true
    }
  } catch {}

  try {
    $response = Invoke-WebRequest -Uri "$PULSAR_BASE_URL/" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
      Log "[WARNING] Server responding but health check endpoint unavailable" -ForegroundColor Yellow
      return $true
    }
  } catch {}

  Log "[ERROR] Browser4 server not accessible at $PULSAR_BASE_URL" -ForegroundColor Red
  Log "[HINT] Start Browser4 with:" -ForegroundColor Cyan
  Log "    java -DDEEPSEEK_API_KEY=`${DEEPSEEK_API_KEY} -jar Browser4.jar" -ForegroundColor White
  return $false
}

# =============================================================================
# SECTION: TEST EXECUTION FUNCTIONS
# =============================================================================

function Run-CurlTest {
  param(
    [string]$TestName,
    [string]$CurlCommand,
    [int]$TestNumber
  )

  $script:TOTAL_TESTS++
  Log ""
  Log "[TEST $TestNumber/$($CURL_COMMANDS.Count)] $TestName" -ForegroundColor Purple

  # 显示命令预览
  if ($VERBOSE_MODE) {
    Log "[COMMAND]" -ForegroundColor Cyan
    Write-Host $CurlCommand
  } else {
    $shortCmd = $CurlCommand.Substring(0, [Math]::Min(200, $CurlCommand.Length)).Replace("`n", " ").Replace("`r", "")
    Log "[COMMAND] $shortCmd..." -ForegroundColor Cyan
  }

  # 替换URL
  $finalCommand = Substitute-Urls $CurlCommand
  $responseFile = [System.IO.Path]::GetTempFileName()
  $errorFile = [System.IO.Path]::GetTempFileName()
  $metaFile = [System.IO.Path]::GetTempFileName()

  $fullCommand = "$finalCommand --max-time $TIMEOUT_SECONDS -w '%{http_code}`n%{time_total}`n%{size_download}`n%{url_effective}' -o `"$responseFile`" -s"
  VLog "Executing: $($fullCommand.Substring(0, [Math]::Min(200, $fullCommand.Length)))..."

  # 执行命令
  $startTime = Get-Date
  $success = $false

  try {
    Invoke-Expression $fullCommand > $metaFile 2> $errorFile
    $success = $LASTEXITCODE -eq 0
  } catch {
    $success = $false
  }

  $endTime = Get-Date
  $duration = [math]::Round(($endTime - $startTime).TotalSeconds)

  if ($success) {
    $httpStatus = "000"
    $timeTotal = "0.000"
    $sizeDownload = "0"
    $urlEffective = "N/A"

    if (Test-Path $metaFile) {
      $metaContent = Get-Content $metaFile -ErrorAction SilentlyContinue
      if ($metaContent -and $metaContent.Count -ge 1) {
        $httpStatus = $metaContent[0]
        if ($metaContent.Count -ge 2) { $timeTotal = $metaContent[1] }
        if ($metaContent.Count -ge 3) { $sizeDownload = $metaContent[2] }
        if ($metaContent.Count -ge 4) { $urlEffective = $metaContent[3] }
      }
    }

    Log "[RESPONSE] Status: $httpStatus | Time: ${timeTotal}s | Size: ${sizeDownload}B | Duration: ${duration}s" -ForegroundColor Blue

    # 检查成功
    if ($httpStatus -match "^[23][0-9][0-9]$") {
      Log "[PASS] ✅ Test completed successfully" -ForegroundColor Green
      $script:PASSED_TESTS++
      Copy-Item -Path $responseFile -Destination "$TEST_RESULTS_DIR/test_${TestNumber}_success.json" -ErrorAction SilentlyContinue

      if ([int]$sizeDownload -gt 0 -and [int]$sizeDownload -lt 3000) {
        $preview = (Get-Content -Path $responseFile -Raw -ErrorAction SilentlyContinue).Substring(0, [Math]::Min(500, (Get-Content -Path $responseFile -Raw -ErrorAction SilentlyContinue).Length))
        if ($preview -and $preview -ne " ") {
          $preview = $preview -replace "[\r\n]", "" -replace "\s+", " "
          Log "[PREVIEW] $preview..." -ForegroundColor Cyan
        }
      } elseif ([int]$sizeDownload -gt 3000) {
        Log "[INFO] Large response (${sizeDownload}B) saved to results directory" -ForegroundColor Cyan
      }
    } else {
      Log "[FAIL] ❌ HTTP Status: $httpStatus" -ForegroundColor Red
      $script:FAILED_TESTS++
      Copy-Item -Path $responseFile -Destination "$TEST_RESULTS_DIR/test_${TestNumber}_error_${httpStatus}.txt" -ErrorAction SilentlyContinue

      if (Test-Path $responseFile) {
        $errorContent = Get-Content -Path $responseFile -Raw -ErrorAction SilentlyContinue
        if ($errorContent) {
          $errorPreview = $errorContent.Substring(0, [Math]::Min(200, $errorContent.Length)) -replace "[\r\n]", ""
          Log "[ERROR RESPONSE] $errorPreview" -ForegroundColor Red
        }
      }

      if (Test-Path $errorFile) {
        $curlError = Get-Content -Path $errorFile -Raw -ErrorAction SilentlyContinue
        if ($curlError) {
          $curlErrorPreview = $curlError.Substring(0, [Math]::Min(200, $curlError.Length)) -replace "[\r\n]", ""
          Log "[CURL ERROR] $curlErrorPreview" -ForegroundColor Red
        }
      }
    }
  } else {
    Log "[FAIL] ❌ Command execution failed" -ForegroundColor Red
    $script:FAILED_TESTS++

    @"
Command: $finalCommand
Error output:
$(Get-Content $errorFile -Raw -ErrorAction SilentlyContinue)
"@ | Out-File -FilePath "$TEST_RESULTS_DIR/test_${TestNumber}_exec_error.txt"

    if (Test-Path $errorFile) {
      $execError = Get-Content -Path $errorFile -Raw -ErrorAction SilentlyContinue
      if ($execError) {
        $execErrorPreview = $execError.Substring(0, [Math]::Min(200, $execError.Length)) -replace "[\r\n]", ""
        Log "[EXECUTION ERROR] $execErrorPreview" -ForegroundColor Red
      }
    }
  }

  Remove-Item -Path $responseFile, $errorFile, $metaFile -Force -ErrorAction SilentlyContinue
  if ($TestNumber -lt $CURL_COMMANDS.Count) { Show-Progress $TOTAL_TESTS $CURL_COMMANDS.Count }
}

function Run-AllTests {
  Log "[INFO] Starting test execution..." -ForegroundColor Blue
  Log "[INFO] Total commands to test: $($CURL_COMMANDS.Count)" -ForegroundColor Blue

  $testCounter = 0
  foreach ($commandEntry in $CURL_COMMANDS) {
    $testCounter++
    Run-CurlTest $commandEntry.Desc $commandEntry.Cmd $testCounter
    if (-not $FAST_MODE) { Start-Sleep -Seconds 1 }
  }

  Write-Host ""
}

function Print-Summary {
  Log ""
  Log "=============================================="
  Log "[FINAL SUMMARY] Test Results" -ForegroundColor Blue
  Log "=============================================="
  Log "Test Session: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Blue
  Log "User: $USER_NAME" -ForegroundColor Blue
  Log "Server: $PULSAR_BASE_URL" -ForegroundColor Blue
  Log "Total Commands: $($CURL_COMMANDS.Count)" -ForegroundColor Blue
  Log "Tests Executed: $TOTAL_TESTS" -ForegroundColor Blue
  Log "Passed: $PASSED_TESTS" -ForegroundColor Green
  Log "Failed: $FAILED_TESTS" -ForegroundColor Red
  Log "Skipped: $SKIPPED_TESTS" -ForegroundColor Yellow

  if ($TOTAL_TESTS -gt 0) {
    $successRate = [math]::Round(($PASSED_TESTS * 100) / $TOTAL_TESTS)
    Log "Success Rate: $successRate%" -ForegroundColor Blue
  }

  Log "Log File: $LOG_FILE" -ForegroundColor Blue
  Log "Results Directory: $TEST_RESULTS_DIR" -ForegroundColor Blue
  Log "=============================================="

  if ($TOTAL_TESTS -eq 0) {
    Log "[INFO] No tests were executed" -ForegroundColor Yellow
    exit 0
  } elseif ($FAILED_TESTS -eq 0) {
    Log "[SUCCESS] All tests passed! 🎉" -ForegroundColor Green
    exit 0
  } else {
    Log "[PARTIAL SUCCESS] Some tests failed. Check logs for details." -ForegroundColor Yellow
    exit 1
  }
}

function Usage {
  @"
Usage: $($MyInvocation.MyCommand.Name) [OPTIONS]

Test curl commands from README.md against Browser4 server.

OPTIONS:
-u, --url URL         Browser4 base URL (default: $DEFAULT_BASE_URL)
-f, --fast            Fast mode - minimal delays between tests
-s, --skip-server     Skip server connectivity check
-t, --timeout SEC     Request timeout in seconds (default: 120)
-v, --verbose         Enable verbose output
-h, --help            Show this help message

EXAMPLES:
$($MyInvocation.MyCommand.Name)                              # Run all tests with defaults
$($MyInvocation.MyCommand.Name) -u http://localhost:8080     # Use custom server URL
$($MyInvocation.MyCommand.Name) -f -t 60                     # Fast mode with 60s timeout
$($MyInvocation.MyCommand.Name) -s -v                        # Skip server check with verbose output

REQUIREMENTS:
- curl command available
- Browser4 server running (unless --skip-server)

UPDATING COMMANDS:
Edit the CURL_COMMANDS array to add/modify tests.
"@
}

# =============================================================================
# SECTION: MAIN EXECUTION LOGIC
# =============================================================================

function Parse-Args {
  param(
    [string[]]$Arguments
  )

  $i = 0
  while ($i -lt $Arguments.Count) {
    switch ($Arguments[$i]) {
      "-u" { $script:PULSAR_BASE_URL = $Arguments[$i+1]; $i += 2 }
      "--url" { $script:PULSAR_BASE_URL = $Arguments[$i+1]; $i += 2 }
      "-f" { $script:FAST_MODE = $true; $i++ }
      "--fast" { $script:FAST_MODE = $true; $i++ }
      "-s" { $script:SKIP_SERVER_CHECK = $true; $i++ }
      "--skip-server" { $script:SKIP_SERVER_CHECK = $true; $i++ }
      "-t" { $script:TIMEOUT_SECONDS = $Arguments[$i+1]; $i += 2 }
      "--timeout" { $script:TIMEOUT_SECONDS = $Arguments[$i+1]; $i += 2 }
      "-v" { $script:VERBOSE_MODE = $true; $i++ }
      "--verbose" { $script:VERBOSE_MODE = $true; $i++ }
      "-h" { Usage; exit 0 }
      "--help" { Usage; exit 0 }
      default {
        Write-Host "Unknown option: $($Arguments[$i])"
        Usage
        exit 1
      }
    }
  }
}

function Main {
  Log "[INFO] Browser4 Curl Command Test Suite" -ForegroundColor Blue
  Log "[INFO] User: $USER_NAME" -ForegroundColor Blue
  Log "[INFO] Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Blue
  Log "[INFO] Server URL: $PULSAR_BASE_URL" -ForegroundColor Blue
  Log "[INFO] Timeout: ${TIMEOUT_SECONDS}s" -ForegroundColor Blue
  Log "[INFO] Fast Mode: $FAST_MODE" -ForegroundColor Blue
  Log "[INFO] Verbose Mode: $VERBOSE_MODE" -ForegroundColor Blue

  if (-not (Get-Command "curl" -ErrorAction SilentlyContinue)) {
    Log "[ERROR] curl command not found. Please install curl." -ForegroundColor Red
    exit 1
  }

  if (-not $SKIP_SERVER_CHECK) {
    if (-not (Check-Server)) {
      Log "[WARNING] Use --skip-server to bypass server check" -ForegroundColor Yellow
      exit 1
    }
  }

  Run-AllTests
  Print-Summary
}

# 让脚本支持 Ctrl+C 中断
try {
  Parse-Args $args
  Main
} catch {
  if ($_.Exception.Message -match "ScriptHalted|OperationStopped") {
    Log "`n[INFO] Tests interrupted by user" -ForegroundColor Yellow
    exit 130
  } else {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
  }
}